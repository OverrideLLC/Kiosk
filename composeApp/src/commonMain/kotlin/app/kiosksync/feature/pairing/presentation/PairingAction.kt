package app.kiosksync.feature.pairing.presentation

import app.kiosksync.feature.role.domain.AppConfig

sealed interface PairingAction {
    data class StartReceiver(val config: AppConfig) : PairingAction
    data object StopReceiver : PairingAction
    data class AdminActionsChanged(val visible: Boolean) : PairingAction
    data object Unpair : PairingAction
    data object OpenGuide : PairingAction
    data object ExitRequested : PairingAction
    data object ChangeRoleRequested : PairingAction
}

sealed interface PairingEffect {
    data object Unpaired : PairingEffect
}
