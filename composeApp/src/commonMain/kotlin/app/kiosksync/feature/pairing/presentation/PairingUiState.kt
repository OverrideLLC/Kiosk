package app.kiosksync.feature.pairing.presentation

import app.kiosksync.feature.pairing.domain.PairingState

data class PairingUiState(
    val pairing: PairingState = PairingState(),
    val serviceError: String? = null,
    val receiverIp: String? = null,
    val receiverPort: Int? = null,
    val showAdminActions: Boolean = true,
)
