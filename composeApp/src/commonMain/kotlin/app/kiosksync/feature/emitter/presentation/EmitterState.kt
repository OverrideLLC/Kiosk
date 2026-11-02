package app.kiosksync.feature.emitter.presentation

import app.kiosksync.feature.emitter.domain.EmitterUpdateState
import app.kiosksync.feature.emitter.domain.models.DeviceSyncUi
import app.kiosksync.feature.emitter.domain.models.EmitterDeviceUi
import app.kiosksync.feature.emitter.domain.models.EmitterPanel
import app.kiosksync.feature.emitter.domain.models.KioskPasswordDialogUi
import app.kiosksync.feature.emitter.domain.models.SecurityDialogUi
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.resources.Res
import app.kiosksync.resources.send_empty_playlist
import app.kiosksync.resources.send_no_screens
import app.kiosksync.core.ui.UiText
import app.kiosksync.core.ui.selectedScreensText

data class EmitterState(
    val playlist: Playlist = Playlist(),
    val devices: List<EmitterDeviceUi> = emptyList(),
    val isSearching: Boolean = true,
    val pairingDevice: EmitterDeviceUi? = null,
    val pairingCode: String = "",
    val pairingError: UiText? = null,
    val isPairing: Boolean = false,
    val unpairDevice: EmitterDeviceUi? = null,
    val showManualIpDialog: Boolean = false,
    val manualIpInput: String = "",
    val manualIpError: UiText? = null,
    val isManualIpLoading: Boolean = false,
    val statusMessage: UiText? = null,
    val statusIsError: Boolean = false,
    val syncByDevice: Map<String, DeviceSyncUi> = emptyMap(),
    val updateByDevice: Map<String, EmitterUpdateState> = emptyMap(),
    val isSending: Boolean = false,
    val isSyncing: Boolean = false,
    val passwordDialog: KioskPasswordDialogUi? = null,
    val securityDialog: SecurityDialogUi? = null,
    val hasKioskPassword: Boolean = false,
    val isRefreshing: Boolean = false,
    val isProjecting: Boolean = false,
    val selectedPanel: EmitterPanel = EmitterPanel.SCREENS,
    val isSettingsOpen: Boolean = false,
    val isRoleResetConfirmationOpen: Boolean = false,
) {
    val selectedCount: Int
        get() = devices.count { it.isSelected && it.isPairedToThisEmitter }

    val pairedCount: Int
        get() = devices.count { it.isPairedToThisEmitter }

    val sendHint: UiText
        get() = when {
            playlist.items.isEmpty() -> UiText(Res.string.send_empty_playlist)
            selectedCount == 0 -> UiText(Res.string.send_no_screens)
            else -> selectedScreensText(selectedCount)
        }

    val canSend: Boolean
        get() = !isSending && !isSyncing && selectedCount > 0 && playlist.items.isNotEmpty()

    val canProject: Boolean
        get() = !isSending && !isSyncing && !isProjecting && playlist.items.isNotEmpty()

    val canSync: Boolean
        get() = !isSending && !isSyncing && pairedCount > 0
}
