package app.kiosksync.feature.emitter.domain.models

import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE

data class SecurityDialogUi(
    val sequence: List<String> = DEFAULT_UNLOCK_SEQUENCE,
    val sequenceError: String? = null,
    val hasCustomRecovery: Boolean = false,
    val showRecoveryEditor: Boolean = false,
    val recoveryAccepted: Boolean = false,
    val recoveryDraft: String = "",
    val recoveryConfirmDraft: String = "",
    val recoveryError: String? = null
)
