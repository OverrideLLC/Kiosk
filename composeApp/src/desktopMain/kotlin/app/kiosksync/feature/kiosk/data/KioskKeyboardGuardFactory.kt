package app.kiosksync.feature.kiosk.data

import app.kiosksync.core.AppLogger
import app.kiosksync.feature.kiosk.domain.KioskKeyboardGuard
import app.kiosksync.feature.kiosk.domain.KioskKeyEvent

private object NoopKioskKeyboardGuard : KioskKeyboardGuard {
    override fun start() = Unit
    override fun stop() = Unit
}

/**
 * Windows uses a low-level keyboard hook (WH_KEYBOARD_LL) so the kiosk can
 * swallow Alt+Tab, the Windows key, Alt+F4, Ctrl+Esc and similar shortcuts.
 * Ctrl+Alt+Del is handled by Windows itself and cannot be intercepted.
 */
fun createKioskKeyboardGuard(
    enabled: Boolean,
    isLocked: () -> Boolean,
    onKey: (KioskKeyEvent) -> Unit,
    onProjectionToggle: () -> Unit,
    logger: AppLogger
): KioskKeyboardGuard {
    if (!enabled) return NoopKioskKeyboardGuard

    val osName = System.getProperty("os.name").orEmpty()
    return if (osName.startsWith("Windows", ignoreCase = true)) {
        WindowsKioskKeyboardGuard(
            isLocked = isLocked,
            onKey = onKey,
            onProjectionToggle = onProjectionToggle,
            logger = logger
        )
    } else {
        logger.info("Kiosk keyboard hook is only available on Windows; running without it")
        NoopKioskKeyboardGuard
    }
}
