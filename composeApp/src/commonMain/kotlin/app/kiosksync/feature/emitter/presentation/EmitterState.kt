package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.EmitterUpdateState
import app.kiosksync.feature.emitter.domain.models.DeviceSyncUi
import app.kiosksync.feature.emitter.domain.models.EmitterDeviceUi
import app.kiosksync.feature.emitter.domain.models.EmitterPanel
import app.kiosksync.feature.emitter.domain.models.KioskPasswordDialogUi
import app.kiosksync.feature.emitter.domain.models.SecurityDialogUi
import app.kiosksync.feature.playlist.domain.Playlist

data class EmitterState(
    val playlist: Playlist = Playlist(),
    val devices: List<EmitterDeviceUi> = emptyList(),
    val isSearching: Boolean = true,
    val pairingDevice: EmitterDeviceUi? = null,
    val pairingCode: String = "",
    val pairingError: String? = null,
    val isPairing: Boolean = false,
    val unpairDevice: EmitterDeviceUi? = null,
    val showManualIpDialog: Boolean = false,
    val manualIpInput: String = "",
    val manualIpError: String? = null,
    val isManualIpLoading: Boolean = false,
    val statusMessage: String? = null,
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

    val sendHint: String
        get() = when {
            playlist.items.isEmpty() -> Strings.SEND_EMPTY_PLAYLIST
            selectedCount == 0 -> Strings.SEND_NO_SCREENS
            else -> Strings.selectedScreens(selectedCount)
        }

    val canSend: Boolean
        get() = !isSending && !isSyncing && selectedCount > 0 && playlist.items.isNotEmpty()

    val canProject: Boolean
        get() = !isSending && !isSyncing && !isProjecting && playlist.items.isNotEmpty()

    val canSync: Boolean
        get() = !isSending && !isSyncing && pairedCount > 0
}
