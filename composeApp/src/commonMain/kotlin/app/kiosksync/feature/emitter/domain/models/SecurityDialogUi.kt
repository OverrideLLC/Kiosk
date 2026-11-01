package app.kiosksync.feature.emitter.domain.models

import app.kiosksync.core.common.kiosk.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.ui.UiText

data class SecurityDialogUi(
    val sequence: List<String> = DEFAULT_UNLOCK_SEQUENCE,
    val sequenceError: UiText? = null,
    val hasCustomRecovery: Boolean = false,
    val showRecoveryEditor: Boolean = false,
    val recoveryAccepted: Boolean = false,
    val recoveryDraft: String = "",
    val recoveryConfirmDraft: String = "",
    val recoveryError: UiText? = null,
)
