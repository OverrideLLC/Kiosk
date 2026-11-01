package app.kiosksync.feature.pairing.presentation

import app.kiosksync.feature.pairing.domain.PairingState
import app.kiosksync.core.ui.UiText

data class PairingUiState(
    val pairing: PairingState = PairingState(),
    val serviceError: UiText? = null,
    val receiverIp: String? = null,
    val receiverPort: Int? = null,
    val showAdminActions: Boolean = true,
)
