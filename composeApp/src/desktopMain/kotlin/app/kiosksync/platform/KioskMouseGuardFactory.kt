package app.kiosksync.platform

import app.kiosksync.core.AppLogger

private object NoopKioskMouseGuard : KioskMouseGuard {
    override fun start() = Unit
    override fun stop() = Unit
    override fun setLocked(locked: Boolean) = Unit
}

/**
 * Windows uses a low-level mouse hook (WH_MOUSE_LL) so the kiosk can swallow
 * pointer movement, clicks, wheel and touchpad taps while locked. On other
 * systems the guard is a no-op; `--windowed` also disables it for debugging.
 */
fun createKioskMouseGuard(
    enabled: Boolean,
    isLocked: () -> Boolean,
    logger: AppLogger
): KioskMouseGuard {
    if (!enabled) return NoopKioskMouseGuard

    val osName = System.getProperty("os.name").orEmpty()
    return if (osName.startsWith("Windows", ignoreCase = true)) {
        WindowsKioskMouseGuard(
            isLocked = isLocked,
            logger = logger
        )
    } else {
        logger.info("Kiosk mouse hook is only available on Windows; running without it")
        NoopKioskMouseGuard
    }
}
