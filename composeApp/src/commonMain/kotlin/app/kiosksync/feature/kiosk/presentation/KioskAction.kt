package app.kiosksync.feature.kiosk.presentation

import app.kiosksync.feature.kiosk.domain.KioskKeyEvent

sealed interface KioskAction {
    data class Lock(val force: Boolean = false) : KioskAction
    data object Release : KioskAction
    data class KeyPressed(val event: KioskKeyEvent) : KioskAction
}
