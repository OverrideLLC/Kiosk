package app.kiosksync.feature.kiosk.presentation

import app.kiosksync.feature.kiosk.domain.KioskLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Presentation boundary for kiosk actions; the domain manager owns key handling and timers. */
class KioskViewModel(
    private val manager: KioskLockManager,
    scope: CoroutineScope,
) {
    val state: StateFlow<KioskState> = manager.state
        .map { it.toState() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = manager.state.value.toState(),
        )

    fun onAction(action: KioskAction) {
        when (action) {
            is KioskAction.Lock -> manager.lock(force = action.force)
            KioskAction.Release -> manager.release()
            is KioskAction.KeyPressed -> manager.onKey(action.event)
        }
    }
}
