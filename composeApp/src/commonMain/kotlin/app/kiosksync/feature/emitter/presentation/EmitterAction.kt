package app.kiosksync.feature.emitter.presentation

import app.kiosksync.feature.emitter.domain.models.EmitterDeviceUi
import app.kiosksync.feature.emitter.domain.models.EmitterPanel
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
    data object OpenSettings : EmitterAction
    data object DismissSettings : EmitterAction
    data object RequestRoleReset : EmitterAction
    data object DismissRoleReset : EmitterAction
    data object ConfirmRoleReset : EmitterAction
    data object Exit : EmitterAction
    data class SelectPanel(val panel: EmitterPanel) : EmitterAction
    data class SetProjectionActive(val active: Boolean) : EmitterAction
    data class PlaylistChanged(val playlist: Playlist) : EmitterAction
    data object SendToSelected : EmitterAction
    data object RequestProjection : EmitterAction
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
