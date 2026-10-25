package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.EmitterUpdateState
import app.kiosksync.feature.playlist.domain.Playlist

enum class EmitterPanel {
    SCREENS,
    PLAYLIST,
}

data class EmitterDeviceUi(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val isPairedToThisEmitter: Boolean,
    val isPairedToAnotherEmitter: Boolean,
    val isOnline: Boolean,
    val isSelected: Boolean,
    val appVersion: String? = null
)

enum class SyncUiStatus {
    PENDING,
    UPLOADING,
    APPLYING,
    SUCCESS,
    FAILED
}

data class DeviceSyncUi(
    val status: SyncUiStatus,
    val uploaded: Int = 0,
    val total: Int = 0,
    val version: Int = 0,
    val message: String? = null
)

enum class EmitterPendingAction {
    SEND,
    PROJECT,
    CONFIGURE,
    PAIR_LOCK
}

data class KioskPasswordDialogUi(
    val action: EmitterPendingAction,
    val playlist: Playlist? = null,
    val deviceId: String? = null,
    val draft: String = "",
    val confirmDraft: String = "",
    val error: String? = null
)

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

data class EmitterUiState(
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
