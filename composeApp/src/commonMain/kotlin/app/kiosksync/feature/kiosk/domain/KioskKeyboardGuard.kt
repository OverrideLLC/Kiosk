package app.kiosksync.feature.kiosk.domain

/**
 * Platform keyboard guard. On Windows it installs a low-level keyboard hook
 * that swallows every key while the kiosk is locked. On other systems it is a
 * no-op so development builds stay usable.
 */
interface KioskKeyboardGuard {
    fun start()
    fun stop()
}
