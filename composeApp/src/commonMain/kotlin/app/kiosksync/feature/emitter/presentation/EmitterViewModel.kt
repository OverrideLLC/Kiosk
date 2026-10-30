package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.AppLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.emitter.domain.EmitterServiceDevice
import app.kiosksync.feature.emitter.domain.EmitterServiceState
import app.kiosksync.feature.emitter.domain.models.EmitterDeviceUi
import app.kiosksync.feature.emitter.domain.models.EmitterPendingAction
import app.kiosksync.feature.emitter.domain.models.KioskPasswordDialogUi
import app.kiosksync.feature.emitter.domain.models.SecurityDialogUi
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterDeliveryCoordinator
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterManualIpCoordinator
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterPairingCoordinator
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterSecurityCoordinator
import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH
import app.kiosksync.core.MAX_UNLOCK_SEQUENCE_LENGTH
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.projection.domain.ProjectionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds Controller presentation state and delegates operational work to the Emitter service. */
class EmitterViewModel(
    private val service: EmitterService,
    kioskLockConfigurationUseCases: KioskLockConfigurationUseCases,
    private val logger: AppLogger,
    private val emitterId: String,
    private val emitterName: String,
    private val scope: CoroutineScope,
    private val kioskLockManager: KioskLockManager,
    private val projectionController: ProjectionService,
    private val onExit: () -> Unit = {},
    private val onRoleReset: () -> Unit = {},
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _state = MutableStateFlow(EmitterState())
    val state: StateFlow<EmitterState> = _state.asStateFlow()

    private val selectedIds = mutableSetOf<String>()
    private val jobs = mutableListOf<Job>()
    private var started = false

    private val deliveryCoordinator = EmitterDeliveryCoordinator(
        uiState = _state,
        service = service,
        emitterId = emitterId,
        scope = scope,
    )

    private val securityCoordinator = EmitterSecurityCoordinator(
        uiState = _state,
        loadConfiguration = { kioskLockConfigurationUseCases.load() },
        scope = scope,
        backgroundDispatcher = backgroundDispatcher,
        logger = logger,
        configurationUseCases = kioskLockConfigurationUseCases,
        pushLockToAllPaired = { lock -> service.sendLockToAllPaired(emitterId, lock) },
        onPasswordConfirmed = { dialog, config ->
            when (dialog.action) {
                EmitterPendingAction.SEND -> dialog.playlist?.let { deliveryCoordinator.send(it, config) }
                EmitterPendingAction.PROJECT -> dialog.playlist?.let { startProjection(it, config) }
                EmitterPendingAction.CONFIGURE -> service.sendLockToAllPaired(emitterId, config)
                EmitterPendingAction.PAIR_LOCK -> dialog.deviceId?.let {
                    service.sendLockToDevice(it, emitterId, config)
                }
            }
        },
    )

    private val pairingCoordinator = EmitterPairingCoordinator(
        uiState = _state,
        service = service,
        logger = logger,
        emitterId = emitterId,
        emitterName = emitterName,
        scope = scope,
        onPaired = { device ->
            selectedIds += device.deviceId
            rebuild(service.state.value)
        },
        onUnpaired = { deviceId ->
            selectedIds -= deviceId
            _state.update { it.copy(syncByDevice = it.syncByDevice - deviceId) }
            rebuild(service.state.value)
        },
        openPairLockDialog = { deviceId ->
            securityCoordinator.openPasswordDialog(
                action = EmitterPendingAction.PAIR_LOCK,
                playlist = null,
                deviceId = deviceId,
            )
        },
    )

    private val manualIpCoordinator = EmitterManualIpCoordinator(
        uiState = _state,
        service = service,
        scope = scope,
    )

    fun onAction(action: EmitterAction) {
        if (action == EmitterAction.DismissPairDialog && _state.value.isPairing) return
        if (action == EmitterAction.DismissManualIpDialog && _state.value.isManualIpLoading) return
        when (action) {
            EmitterAction.RefreshDevices -> refreshDevices()
            EmitterAction.OpenManualIp -> _state.update {
                it.copy(showManualIpDialog = true, manualIpInput = "", manualIpError = null)
            }
            EmitterAction.DismissManualIpDialog -> _state.update {
                it.copy(showManualIpDialog = false, manualIpInput = "", manualIpError = null)
            }
            is EmitterAction.UpdateManualIp -> _state.update {
                it.copy(manualIpInput = action.value, manualIpError = null)
            }
            EmitterAction.DismissStatusMessage -> clearStatus()
            EmitterAction.OpenSettings -> _state.update { it.copy(isSettingsOpen = true) }
            EmitterAction.DismissSettings -> _state.update { it.copy(isSettingsOpen = false) }
            EmitterAction.RequestRoleReset -> _state.update { it.copy(isRoleResetConfirmationOpen = true) }
            EmitterAction.DismissRoleReset -> _state.update { it.copy(isRoleResetConfirmationOpen = false) }
            EmitterAction.ConfirmRoleReset -> {
                _state.update { it.copy(isRoleResetConfirmationOpen = false) }
                onRoleReset()
            }
            EmitterAction.Exit -> onExit()
            is EmitterAction.SelectPanel -> _state.update { it.copy(selectedPanel = action.panel) }
            is EmitterAction.SetProjectionActive -> _state.update { it.copy(isProjecting = action.active) }
            is EmitterAction.PlaylistChanged -> _state.update { it.copy(playlist = action.playlist) }
            is EmitterAction.OpenPairDialog -> _state.update {
                it.copy(pairingDevice = action.device, pairingCode = "", pairingError = null)
            }
            EmitterAction.DismissPairDialog -> _state.update {
                it.copy(pairingDevice = null, pairingCode = "", pairingError = null)
            }
            is EmitterAction.UpdatePairingCode -> _state.update {
                it.copy(pairingCode = action.value.filter(Char::isDigit).take(6), pairingError = null)
            }
            is EmitterAction.RequestUnpair -> _state.update { it.copy(unpairDevice = action.device) }
            EmitterAction.DismissUnpairDialog -> _state.update { it.copy(unpairDevice = null) }
            is EmitterAction.UpdatePasswordDraft -> updatePassword {
                it.copy(draft = action.value.take(64), error = null)
            }
            is EmitterAction.UpdatePasswordConfirmDraft -> updatePassword {
                it.copy(confirmDraft = action.value.take(64), error = null)
            }
            EmitterAction.DismissPasswordDialog -> _state.update { it.copy(passwordDialog = null) }
            is EmitterAction.AppendUnlockKey -> updateSecurity { dialog ->
                if (dialog.sequence.size >= MAX_UNLOCK_SEQUENCE_LENGTH) dialog
                else dialog.copy(sequence = dialog.sequence + action.key, sequenceError = null)
            }
            EmitterAction.RemoveLastUnlockKey -> updateSecurity { dialog ->
                if (dialog.sequence.isEmpty()) dialog
                else dialog.copy(sequence = dialog.sequence.dropLast(1), sequenceError = null)
            }
            EmitterAction.ResetUnlockSequence -> updateSecurity {
                it.copy(sequence = DEFAULT_UNLOCK_SEQUENCE, sequenceError = null)
            }
            is EmitterAction.ToggleSelection -> toggleSelection(action.deviceId)
            is EmitterAction.RequestUpdate -> service.requestUpdate(action.deviceId, emitterId)
            EmitterAction.OpenSecurityDialog -> securityCoordinator.openSecurityDialog()
            EmitterAction.DismissSecurityDialog -> _state.update { it.copy(securityDialog = null) }
            EmitterAction.ToggleRecoveryEditor -> updateSecurity { dialog ->
                dialog.copy(showRecoveryEditor = !dialog.showRecoveryEditor, recoveryError = null)
            }
            is EmitterAction.SetRecoveryAccepted -> updateSecurity {
                it.copy(recoveryAccepted = action.accepted, recoveryError = null)
            }
            is EmitterAction.UpdateRecoveryDraft -> updateSecurity {
                it.copy(recoveryDraft = action.value.take(MAX_RECOVERY_PHRASE_LENGTH), recoveryError = null)
            }
            is EmitterAction.UpdateRecoveryConfirmDraft -> updateSecurity {
                it.copy(
                    recoveryConfirmDraft = action.value.take(MAX_RECOVERY_PHRASE_LENGTH),
                    recoveryError = null,
                )
            }

            EmitterAction.SendToSelected -> sendToSelected(_state.value.playlist)
            EmitterAction.RequestProjection -> requestProjection(_state.value.playlist)
            EmitterAction.SynchronizePlayback -> deliveryCoordinator.synchronizePlayback()
            EmitterAction.ConfirmPassword -> securityCoordinator.confirmPassword()
            EmitterAction.SaveUnlockSequence -> securityCoordinator.saveUnlockSequence()
            EmitterAction.SaveRecoveryPhrase -> securityCoordinator.saveRecoveryPhrase()
            EmitterAction.ResetRecoveryPhrase -> securityCoordinator.resetRecoveryPhrase()
            EmitterAction.OpenChangePasswordDialog -> securityCoordinator.openChangePasswordDialog()
            EmitterAction.ConfirmPair -> pairingCoordinator.confirmPair()
            EmitterAction.ConfirmUnpair -> pairingCoordinator.confirmUnpair()
            EmitterAction.ConfirmManualIp -> manualIpCoordinator.confirm()
        }
    }

    fun start() {
        if (started) return
        started = true
        service.start(emitterId, emitterName)
        jobs += scope.launch {
            service.state.collect { newState ->
                val pairedIds = newState.devices.filter { it.pairedToThisEmitter }.mapTo(mutableSetOf()) { it.deviceId }
                selectedIds.retainAll(pairedIds)
                rebuild(newState)
            }
        }
        jobs += scope.launch {
            loadLock { lock -> _state.update { it.copy(hasKioskPassword = lock != null) } }
        }
    }

    fun stop() {
        started = false
        jobs.forEach(Job::cancel)
        jobs.clear()
        service.stop()
    }

    private fun refreshDevices() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        service.refresh()
        scope.launch {
            delay(1_500)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun toggleSelection(deviceId: String) {
        val device = _state.value.devices.firstOrNull { it.deviceId == deviceId } ?: return
        if (!device.isPairedToThisEmitter) return
        if (!selectedIds.add(deviceId)) selectedIds.remove(deviceId)
        rebuild(service.state.value)
    }

    private fun sendToSelected(playlist: Playlist) {
        val state = _state.value
        if (state.isSending || state.isSyncing) return
        if (playlist.items.isEmpty()) {
            showStatus(Strings.SEND_EMPTY_PLAYLIST)
            return
        }
        if (state.devices.none { it.isSelected && it.isPairedToThisEmitter }) {
            showStatus(Strings.SEND_NO_SCREENS)
            return
        }

        withKioskLock(EmitterPendingAction.SEND, playlist) { lock ->
            deliveryCoordinator.send(playlist, lock)
        }
    }

    private fun requestProjection(playlist: Playlist) {
        if (_state.value.isSending || _state.value.isSyncing || projectionController.isProjecting) return
        if (playlist.items.isEmpty()) {
            showStatus(Strings.PROJECT_NO_PLAYLIST)
            return
        }

        withKioskLock(EmitterPendingAction.PROJECT, playlist) { lock ->
            startProjection(playlist, lock)
        }
    }

    private fun withKioskLock(
        action: EmitterPendingAction,
        playlist: Playlist,
        onLockAvailable: (KioskLockConfig) -> Unit,
    ) {
        scope.launch {
            loadLock { lock ->
                if (lock == null) securityCoordinator.openPasswordDialog(action, playlist)
                else onLockAvailable(lock)
            }
        }
    }

    private suspend fun loadLock(onSuccess: (KioskLockConfig?) -> Unit) {
        service.loadKioskLock().fold(
            onSuccess = onSuccess,
            onFailure = { showStatus(Strings.SECURITY_STORAGE_ERROR) },
        )
    }

    private fun startProjection(playlist: Playlist, lock: KioskLockConfig) {
        kioskLockManager.setConfig(lock)
        projectionController.start(playlist)
        kioskLockManager.lock(force = true)
        logger.info("Projection started with playlist '${playlist.name}'")
    }

    private fun rebuild(serviceState: EmitterServiceState) {
        val devices = serviceState.devices.map { it.toUiDevice() }.sortedBy { it.deviceName.lowercase() }
        _state.update {
            it.copy(
                devices = devices,
                isSearching = serviceState.isSearching,
                updateByDevice = serviceState.updateByDevice,
            )
        }
    }

    private fun updatePassword(transform: (KioskPasswordDialogUi) -> KioskPasswordDialogUi) {
        _state.update { state ->
            state.copy(passwordDialog = state.passwordDialog?.let(transform))
        }
    }

    private fun updateSecurity(transform: (SecurityDialogUi) -> SecurityDialogUi) {
        _state.update { state ->
            state.copy(securityDialog = state.securityDialog?.let(transform))
        }
    }

    private fun showStatus(message: String) {
        _state.update { it.copy(statusMessage = message, statusIsError = true) }
    }

    private fun clearStatus() {
        _state.update { it.copy(statusMessage = null, statusIsError = false) }
    }

    private fun EmitterServiceDevice.toUiDevice() = EmitterDeviceUi(
        deviceId = deviceId,
        deviceName = deviceName,
        host = host,
        port = port,
        isPairedToThisEmitter = pairedToThisEmitter,
        isPairedToAnotherEmitter = pairedToAnotherEmitter,
        isOnline = isOnline,
        isSelected = deviceId in selectedIds,
        appVersion = appVersion,
    )
}
