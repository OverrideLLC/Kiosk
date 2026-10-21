package app.kiosksync.platform

import app.kiosksync.core.AppLogger
import app.kiosksync.feature.kiosk.KioskKeyEvent
import app.kiosksync.feature.kiosk.KioskKeyKind
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * WH_KEYBOARD_LL hook. While the kiosk is locked every key event (down and up)
 * is swallowed and forwarded to the unlock state machine. When unlocked the
 * hook only intercepts Super/Win + V to toggle the controller's projection.
 */
class WindowsKioskKeyboardGuard(
    private val isLocked: () -> Boolean,
    private val onKey: (KioskKeyEvent) -> Unit,
    private val onProjectionToggle: () -> Unit,
    private val logger: AppLogger
) : KioskKeyboardGuard {

    private val user32 = User32.INSTANCE
    private val kernel32 = Kernel32.INSTANCE

    @Volatile
    private var running = false

    @Volatile
    private var threadId: Int = 0

    @Volatile
    private var hook: WinUser.HHOOK? = null

    @Volatile
    private var shiftDown = false

    @Volatile
    private var ctrlDown = false

    @Volatile
    private var winDown = false

    private var hookProc: WinUser.LowLevelKeyboardProc? = null
    private var thread: Thread? = null

    override fun start() {
        if (thread != null) return
        running = true
        val ready = CountDownLatch(1)
        thread = thread(name = "KioskSync-keyboard-hook", isDaemon = true) {
            try {
                installHook(ready)
            } catch (error: Throwable) {
                logger.error("Could not install the Windows keyboard hook", error)
            } finally {
                ready.countDown()
            }
        }
        if (!ready.await(3, TimeUnit.SECONDS)) {
            logger.warn("Windows keyboard hook did not start within 3 seconds")
        }
    }

    override fun stop() {
        running = false
        hook?.let { handle ->
            runCatching { user32.UnhookWindowsHookEx(handle) }
        }
        hook = null
        if (threadId != 0) {
            runCatching {
                user32.PostThreadMessage(
                    threadId,
                    WinUser.WM_QUIT,
                    WinDef.WPARAM(0),
                    WinDef.LPARAM(0)
                )
            }
        }
        thread?.join(1500)
        thread = null
        threadId = 0
    }

    private fun installHook(ready: CountDownLatch) {
        val proc = object : WinUser.LowLevelKeyboardProc {
            override fun callback(
                nCode: Int,
                wParam: WinDef.WPARAM,
                info: WinUser.KBDLLHOOKSTRUCT
            ): WinDef.LRESULT {
                if (nCode < 0) {
                    return user32.CallNextHookEx(
                        hook,
                        nCode,
                        wParam,
                        WinDef.LPARAM(Pointer.nativeValue(info.pointer))
                    )
                }
                return handleKeyEvent(wParam, info)
            }
        }
        hookProc = proc
        threadId = kernel32.GetCurrentThreadId()

        val module = kernel32.GetModuleHandle(null)
        val installed = user32.SetWindowsHookEx(
            WinUser.WH_KEYBOARD_LL,
            proc,
            module,
            0
        )
        if (installed == null) {
            val errorCode = kernel32.GetLastError()
            logger.error("SetWindowsHookEx(WH_KEYBOARD_LL) failed with error $errorCode", null)
            running = false
            return
        }
        hook = installed
        logger.info("Windows low-level keyboard hook installed")

        ready.countDown()

        val message = WinUser.MSG()
        while (running) {
            val result = user32.GetMessage(message, null, 0, 0)
            if (result <= 0) break
            user32.TranslateMessage(message)
            user32.DispatchMessage(message)
        }
        logger.info("Windows low-level keyboard hook stopped")
    }

    private fun handleKeyEvent(
        wParam: WinDef.WPARAM,
        info: WinUser.KBDLLHOOKSTRUCT
    ): WinDef.LRESULT {
        val message = wParam.toInt()
        val isDown = message == WinUser.WM_KEYDOWN || message == WinUser.WM_SYSKEYDOWN
        val isUp = message == WinUser.WM_KEYUP || message == WinUser.WM_SYSKEYUP
        val vk = info.vkCode

        if (vk == VK_SHIFT || vk == VK_LSHIFT || vk == VK_RSHIFT) {
            shiftDown = isDown
        }
        if (vk == VK_CONTROL || vk == VK_LCONTROL || vk == VK_RCONTROL) {
            ctrlDown = isDown
        }
        if (vk == VK_LWIN || vk == VK_RWIN) {
            winDown = isDown
        }

        if (isLocked()) {
            if (isDown) {
                onKey(mapKey(vk, info))
            }
            // Swallow both key down and key up so the OS never sees the key.
            return WinDef.LRESULT(1)
        }

        // Super/Win + V only while one of our windows is focused. Swallowing
        // the Win key itself prevents the Start menu from opening first.
        if (isOurWindowFocused()) {
            if (vk == VK_LWIN || vk == VK_RWIN) {
                return WinDef.LRESULT(1)
            }
            if (isDown && winDown && vk == VK_V) {
                onProjectionToggle()
                return WinDef.LRESULT(1)
            }
        }

        if (!isDown && !isUp) {
            return WinDef.LRESULT(0)
        }
        return user32.CallNextHookEx(
            hook,
            0,
            wParam,
            WinDef.LPARAM(Pointer.nativeValue(info.pointer))
        )
    }

    private fun mapKey(vk: Int, info: WinUser.KBDLLHOOKSTRUCT): KioskKeyEvent {
        return when (vk) {
            VK_SHIFT, VK_LSHIFT, VK_RSHIFT -> KioskKeyEvent(KioskKeyKind.SHIFT)
            VK_CONTROL, VK_LCONTROL, VK_RCONTROL -> KioskKeyEvent(KioskKeyKind.CTRL)
            VK_RETURN -> KioskKeyEvent(KioskKeyKind.ENTER)
            VK_BACK -> KioskKeyEvent(KioskKeyKind.BACKSPACE)
            else -> {
                val char = charFor(info)
                if (char != null && (char.isLetterOrDigit())) {
                    KioskKeyEvent(KioskKeyKind.CHARACTER, char)
                } else {
                    KioskKeyEvent(KioskKeyKind.OTHER)
                }
            }
        }
    }

    private fun charFor(info: WinUser.KBDLLHOOKSTRUCT): Char? {
        val keyboardState = ByteArray(256)
        user32.GetKeyboardState(keyboardState)
        if (shiftDown) keyboardState[VK_SHIFT] = 0x80.toByte()
        if (ctrlDown) keyboardState[VK_CONTROL] = 0x80.toByte()

        val buffer = CharArray(8)
        val converted = user32.ToUnicodeEx(
            info.vkCode,
            info.scanCode,
            keyboardState,
            buffer,
            buffer.size,
            0x04,
            user32.GetKeyboardLayout(0)
        )
        if (converted == 1 && buffer[0].code >= 32) {
            return buffer[0]
        }
        return mapVirtualKey(info.vkCode)
    }

    private fun mapVirtualKey(vk: Int): Char? = when {
        vk in VK_0..VK_9 -> if (shiftDown) {
            SHIFTED_DIGITS[vk - VK_0]
        } else {
            ('0' + (vk - VK_0))
        }

        vk in VK_A..VK_Z -> {
            val lower = ('a' + (vk - VK_A))
            if (shiftDown) lower.uppercaseChar() else lower
        }

        vk in VK_NUMPAD_0..VK_NUMPAD_9 -> ('0' + (vk - VK_NUMPAD_0))
        vk == VK_SPACE -> ' '
        else -> null
    }

    private fun isOurWindowFocused(): Boolean {
        val foreground = user32.GetForegroundWindow() ?: return false
        val processId = IntByReference()
        user32.GetWindowThreadProcessId(foreground, processId)
        return processId.value == kernel32.GetCurrentProcessId()
    }

    private companion object {
        const val VK_BACK = 0x08
        const val VK_RETURN = 0x0D
        const val VK_SHIFT = 0x10
        const val VK_CONTROL = 0x11
        const val VK_SPACE = 0x20
        const val VK_0 = 0x30
        const val VK_9 = 0x39
        const val VK_A = 0x41
        const val VK_Z = 0x5A
        const val VK_LWIN = 0x5B
        const val VK_RWIN = 0x5C
        const val VK_NUMPAD_0 = 0x60
        const val VK_NUMPAD_9 = 0x69
        const val VK_LSHIFT = 0xA0
        const val VK_RSHIFT = 0xA1
        const val VK_LCONTROL = 0xA2
        const val VK_RCONTROL = 0xA3
        const val VK_V = 0x56

        val SHIFTED_DIGITS = charArrayOf(')', '!', '@', '#', '$', '%', '^', '&', '*', '(')
    }
}
