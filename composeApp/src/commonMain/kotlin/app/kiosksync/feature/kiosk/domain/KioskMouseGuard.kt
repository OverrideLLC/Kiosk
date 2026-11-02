package app.kiosksync.feature.kiosk.domain

/**
 * Platform mouse/touchpad guard. On Windows it installs a low-level mouse hook
 * that swallows every pointer event while the kiosk is locked. On other systems
 * it is a no-op so development builds stay usable.
 */
interface KioskMouseGuard {
    fun start()
    fun stop()

    /**
     * Called when the kiosk lock changes. Locking captures the pointer at its
     * current position; unlocking releases it so dialogs (for example the
     * screen options card) can be used with the mouse or touchpad.
     */
    fun setLocked(locked: Boolean)
}
