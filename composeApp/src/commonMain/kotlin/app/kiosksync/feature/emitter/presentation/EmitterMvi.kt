package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH
import app.kiosksync.core.MAX_UNLOCK_SEQUENCE_LENGTH
import app.kiosksync.feature.playlist.domain.Playlist

sealed interface EmitterAction {
    data object RefreshDevices : EmitterAction
    data object OpenManualIp : EmitterAction
    data class ToggleSelection(val deviceId: String) : EmitterAction
    data class OpenPairDialog(val device: EmitterDeviceUi) : EmitterAction
    data class RequestUnpair(val device: EmitterDeviceUi) : EmitterAction
    data class RequestUpdate(val deviceId: String) : EmitterAction
    data object OpenSecurityDialog : EmitterAction
    data object DismissStatusMessage : EmitterAction
    data class SendToSelected(val playlist: Playlist) : EmitterAction
    data class RequestProjection(val playlist: Playlist) : EmitterAction
    data object SynchronizePlayback : EmitterAction
    data class UpdatePasswordDraft(val value: String) : EmitterAction
    data class UpdatePasswordConfirmDraft(val value: String) : EmitterAction
    data object ConfirmPassword : EmitterAction
    data object DismissPasswordDialog : EmitterAction
    data class AppendUnlockKey(val key: String) : EmitterAction
    data object RemoveLastUnlockKey : EmitterAction
    data object ResetUnlockSequence : EmitterAction
    data object SaveUnlockSequence : EmitterAction
    data object ToggleRecoveryEditor : EmitterAction
    data class SetRecoveryAccepted(val accepted: Boolean) : EmitterAction
    data class UpdateRecoveryDraft(val value: String) : EmitterAction
    data class UpdateRecoveryConfirmDraft(val value: String) : EmitterAction
    data object SaveRecoveryPhrase : EmitterAction
    data object ResetRecoveryPhrase : EmitterAction
    data object OpenChangePasswordDialog : EmitterAction
    data object DismissSecurityDialog : EmitterAction
    data class UpdatePairingCode(val value: String) : EmitterAction
    data object ConfirmPair : EmitterAction
    data object DismissPairDialog : EmitterAction
    data object DismissUnpairDialog : EmitterAction
    data object ConfirmUnpair : EmitterAction
    data class UpdateManualIp(val value: String) : EmitterAction
    data object ConfirmManualIp : EmitterAction
    data object DismissManualIpDialog : EmitterAction
}

object EmitterReducer {
    fun reduce(state: EmitterUiState, action: EmitterAction): EmitterUiState = when (action) {
        is EmitterAction.OpenPairDialog -> state.copy(
            pairingDevice = action.device,
            pairingCode = "",
            pairingError = null
        )
        EmitterAction.DismissPairDialog -> state.copy(
            pairingDevice = null,
            pairingCode = "",
            pairingError = null
        )
        is EmitterAction.UpdatePairingCode -> state.copy(
            pairingCode = action.value.filter(Char::isDigit).take(6),
            pairingError = null
        )
        is EmitterAction.RequestUnpair -> state.copy(unpairDevice = action.device)
        EmitterAction.DismissUnpairDialog -> state.copy(unpairDevice = null)
        EmitterAction.OpenManualIp -> state.copy(
            showManualIpDialog = true,
            manualIpInput = "",
            manualIpError = null
        )
        EmitterAction.DismissManualIpDialog -> state.copy(
            showManualIpDialog = false,
            manualIpInput = "",
            manualIpError = null
        )
        is EmitterAction.UpdateManualIp -> state.copy(manualIpInput = action.value, manualIpError = null)
        EmitterAction.DismissStatusMessage -> state.copy(statusMessage = null, statusIsError = false)
        is EmitterAction.UpdatePasswordDraft -> state.copy(
            passwordDialog = state.passwordDialog?.copy(draft = action.value.take(64), error = null)
        )
        is EmitterAction.UpdatePasswordConfirmDraft -> state.copy(
            passwordDialog = state.passwordDialog?.copy(confirmDraft = action.value.take(64), error = null)
        )
        EmitterAction.DismissPasswordDialog -> state.copy(passwordDialog = null)
        is EmitterAction.AppendUnlockKey -> state.copy(
            securityDialog = state.securityDialog?.let { dialog ->
                if (dialog.sequence.size >= MAX_UNLOCK_SEQUENCE_LENGTH) dialog
                else dialog.copy(sequence = dialog.sequence + action.key, sequenceError = null)
            }
        )
        EmitterAction.RemoveLastUnlockKey -> state.copy(
            securityDialog = state.securityDialog?.let { dialog ->
                if (dialog.sequence.isEmpty()) dialog
                else dialog.copy(sequence = dialog.sequence.dropLast(1), sequenceError = null)
            }
        )
        EmitterAction.ResetUnlockSequence -> state.copy(
            securityDialog = state.securityDialog?.copy(
                sequence = DEFAULT_UNLOCK_SEQUENCE,
                sequenceError = null
            )
        )
        EmitterAction.ToggleRecoveryEditor -> state.copy(
            securityDialog = state.securityDialog?.let { dialog ->
                dialog.copy(
                    showRecoveryEditor = !dialog.showRecoveryEditor,
                    recoveryError = null
                )
            }
        )
        is EmitterAction.SetRecoveryAccepted -> state.copy(
            securityDialog = state.securityDialog?.copy(
                recoveryAccepted = action.accepted,
                recoveryError = null
            )
        )
        is EmitterAction.UpdateRecoveryDraft -> state.copy(
            securityDialog = state.securityDialog?.copy(
                recoveryDraft = action.value.take(MAX_RECOVERY_PHRASE_LENGTH),
                recoveryError = null
            )
        )
        is EmitterAction.UpdateRecoveryConfirmDraft -> state.copy(
            securityDialog = state.securityDialog?.copy(
                recoveryConfirmDraft = action.value.take(MAX_RECOVERY_PHRASE_LENGTH),
                recoveryError = null
            )
        )
        EmitterAction.DismissSecurityDialog -> state.copy(securityDialog = null)
        is EmitterAction.RefreshDevices,
        is EmitterAction.ToggleSelection,
        is EmitterAction.RequestUpdate,
        EmitterAction.OpenSecurityDialog,
        is EmitterAction.SendToSelected,
        is EmitterAction.RequestProjection,
        EmitterAction.SynchronizePlayback,
        EmitterAction.ConfirmPassword,
        EmitterAction.SaveUnlockSequence,
        EmitterAction.SaveRecoveryPhrase,
        EmitterAction.ResetRecoveryPhrase,
        EmitterAction.OpenChangePasswordDialog,
        EmitterAction.ConfirmPair,
        EmitterAction.ConfirmUnpair,
        EmitterAction.ConfirmManualIp -> state
    }
}
