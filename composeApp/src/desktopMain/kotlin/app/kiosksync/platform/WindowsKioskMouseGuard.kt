package app.kiosksync.platform

import app.kiosksync.core.AppLogger
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.W32APIOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * WH_MOUSE_LL hook. While the kiosk is locked every mouse event is swallowed:
 * movement, clicks, wheel and the synthetic events produced by most touchpads.
 * This prevents the pointer from reaching the taskbar, another window or the
 * window chrome while a fullscreen screen is locked.
 *
 * Windows precision-touchpad gestures handled directly by the shell (three or
 * four fingers) are outside this hook; see docs/KNOWN_LIMITATIONS.md.
 */
class WindowsKioskMouseGuard(
    private val isLocked: () -> Boolean,
    private val logger: AppLogger
) : KioskMouseGuard {

    private val user32 = User32.INSTANCE
    private val extendedUser32 = ExtendedUser32.INSTANCE
    private val kernel32 = Kernel32.INSTANCE
    private val clipRect = WinDef.RECT()
    private val shutdownHook = Thread {
        runCatching { extendedUser32.ClipCursor(null) }
    }

    @Volatile
    private var running = false

    /**
     * Whether the hook should swallow mouse events. It is normally true while
     * the kiosk is locked, but the update prompt temporarily calls
     * [setLocked] with false so the user can click "Actualizar ahora" without
     * unlocking the Screen. Keeping this separate from [isLocked] is
     * important: the kiosk remains logically locked while the update decision
     * is shown.
     */
    @Volatile
    private var swallowEvents = false

    @Volatile
    private var threadId: Int = 0

    @Volatile
    private var hook: WinUser.HHOOK? = null

    @Volatile
    private var firstEventLogged = false

    @Volatile
    private var clipLogged = false

    @Volatile
    private var anchorSet = false

    @Volatile
    private var clipActive = false

    @Volatile
    private var anchorX = 0

    @Volatile
    private var anchorY = 0

    private var hookProc: WinUser.LowLevelMouseProc? = null
    private var thread: Thread? = null

    override fun start() {
        if (thread != null) return
        if (isLocked()) setLocked(true)
        running = true
        runCatching { Runtime.getRuntime().addShutdownHook(shutdownHook) }
        val ready = CountDownLatch(1)
        thread = thread(name = "OverrideKiosk-mouse-hook", isDaemon = true) {
            try {
                installHook(ready)
            } catch (error: Throwable) {
                logger.error("Could not install the Windows mouse hook", error)
            } finally {
                ready.countDown()
            }
        }
        if (!ready.await(3, TimeUnit.SECONDS)) {
            logger.warn("Windows mouse hook did not start within 3 seconds")
        }
    }

    override fun setLocked(locked: Boolean) {
        swallowEvents = locked
        if (locked) {
            if (anchorSet && clipActive) return
            if (!anchorSet) {
                val point = WinDef.POINT()
                if (!user32.GetCursorPos(point)) return
                anchorX = point.x
                anchorY = point.y
                updateClipRect()
                anchorSet = true
            }
            val clipResult = clipCursor(clipRect)
            clipActive = clipResult
            logger.info(
                "Kiosk pointer captured at $anchorX,$anchorY " +
                    "(ClipCursor=$clipResult)"
            )
        } else if (anchorSet || clipActive) {
            anchorSet = false
            clipActive = false
            clipCursor(null)
            logger.info("Kiosk pointer released")
        }
    }

    override fun stop() {
        running = false
        swallowEvents = false
        anchorSet = false
        clipActive = false
        clipCursor(null)
        runCatching { Runtime.getRuntime().removeShutdownHook(shutdownHook) }
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
        val proc = object : WinUser.LowLevelMouseProc {
            override fun callback(
                nCode: Int,
                wParam: WinDef.WPARAM,
                info: WinUser.MSLLHOOKSTRUCT
            ): WinDef.LRESULT {
                if (nCode < 0) {
                    return user32.CallNextHookEx(
                        hook,
                        nCode,
                        wParam,
                        WinDef.LPARAM(Pointer.nativeValue(info.pointer))
                    )
                }
                if (!firstEventLogged) {
                    firstEventLogged = true
                    logger.info(
                        "Windows mouse hook received its first event " +
                            "(message=${wParam.toInt()}, locked=${isLocked()}, " +
                            "swallow=$swallowEvents)"
                    )
                }
                if (swallowEvents) {
                    // Swallow the event before Windows delivers it to any window.
                    // Some touchpad drivers and synthetic events can still move the
                    // pointer before the hook runs; confine the cursor to a single
                    // pixel as a second barrier.
                    if (!anchorSet) {
                        anchorX = info.pt.x
                        anchorY = info.pt.y
                        updateClipRect()
                        anchorSet = true
                    }
                    val clipResult = clipCursor(clipRect)
                    clipActive = clipResult
                    if (!clipLogged) {
                        clipLogged = true
                        logger.info(
                            "ClipCursor(first) returned $clipResult " +
                                "for rect=${clipRect.left},${clipRect.top},${clipRect.right},${clipRect.bottom}"
                        )
                    }
                    return WinDef.LRESULT(1)
                }
                if (anchorSet || clipActive) {
                    anchorSet = false
                    clipActive = false
                    clipCursor(null)
                }
                return user32.CallNextHookEx(
                    hook,
                    nCode,
                    wParam,
                    WinDef.LPARAM(Pointer.nativeValue(info.pointer))
                )
            }
        }
        hookProc = proc
        threadId = kernel32.GetCurrentThreadId()

        val module = kernel32.GetModuleHandle(null)
        val installed = user32.SetWindowsHookEx(
            WinUser.WH_MOUSE_LL,
            proc,
            module,
            0
        )
        if (installed == null) {
            val errorCode = kernel32.GetLastError()
            logger.error("SetWindowsHookEx(WH_MOUSE_LL) failed with error $errorCode", null)
            running = false
            return
        }
        hook = installed
        logger.info("Windows low-level mouse hook installed")

        ready.countDown()

        val message = WinUser.MSG()
        while (running) {
            val result = user32.GetMessage(message, null, 0, 0)
            if (result <= 0) break
            user32.TranslateMessage(message)
            user32.DispatchMessage(message)
        }
        logger.info("Windows low-level mouse hook stopped")
    }

    private fun updateClipRect() {
        clipRect.left = anchorX
        clipRect.top = anchorY
        clipRect.right = anchorX + 1
        clipRect.bottom = anchorY + 1
    }

    private fun clipCursor(rect: WinDef.RECT?): Boolean =
        runCatching { extendedUser32.ClipCursor(rect) }
            .onFailure { logger.error("ClipCursor failed", it) }
            .getOrDefault(false)

    private interface ExtendedUser32 : Library {
        fun ClipCursor(rect: WinDef.RECT?): Boolean

        companion object {
            val INSTANCE: ExtendedUser32 = Native.load(
                "user32",
                ExtendedUser32::class.java,
                W32APIOptions.DEFAULT_OPTIONS
            )
        }
    }
}
