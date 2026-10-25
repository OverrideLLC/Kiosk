package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.AppLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.emitter.domain.EmitterServiceDevice
import app.kiosksync.feature.emitter.domain.EmitterServiceState
import app.kiosksync.feature.emitter.domain.EmitterUpdateStatus
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterDeliveryCoordinator
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterManualIpCoordinator
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterPairingCoordinator
import app.kiosksync.feature.emitter.presentation.coordinators.EmitterSecurityCoordinator
import app.kiosksync.feature.emitter.presentation.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterReducer
import app.kiosksync.feature.emitter.presentation.DeviceUpdateStatus
import app.kiosksync.feature.emitter.presentation.DeviceUpdateUi
import app.kiosksync.feature.emitter.presentation.EmitterDeviceUi
import app.kiosksync.feature.emitter.presentation.EmitterPendingAction
import app.kiosksync.feature.emitter.presentation.EmitterUiState
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.projection.domain.ProjectionService
import kotlinx.coroutines.CancellationException
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
    private val kioskLockConfigurationUseCases: KioskLockConfigurationUseCases,
    private val logger: AppLogger,
    private val emitterId: String,
    private val emitterName: String,
    private val scope: CoroutineScope,
    private val kioskLockManager: KioskLockManager,
    private val projectionController: ProjectionService,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _uiState = MutableStateFlow(EmitterUiState())
    val state: StateFlow<EmitterUiState> = _uiState.asStateFlow()

    private var serviceState = EmitterServiceState()
    private val selectedIds = mutableSetOf<String>()
    private val jobs = mutableListOf<Job>()
    private var started = false

    private val updateByDevice: Map<String, DeviceUpdateUi>
        get() = serviceState.updateByDevice.mapValues { (deviceId, update) ->
            val deviceVersion = serviceState.devices.firstOrNull { it.deviceId == deviceId }?.appVersion
            DeviceUpdateUi(
                status = when (update.status) {
                    EmitterUpdateStatus.PREPARING -> DeviceUpdateStatus.PREPARING
                    EmitterUpdateStatus.UPLOADING -> DeviceUpdateStatus.UPLOADING
                    EmitterUpdateStatus.AWAITING_CONFIRMATION -> DeviceUpdateStatus.AWAITING_CONFIRMATION
                    EmitterUpdateStatus.APPLYING -> DeviceUpdateStatus.APPLYING
                    EmitterUpdateStatus.SKIPPED -> DeviceUpdateStatus.SKIPPED
                    EmitterUpdateStatus.FAILED -> DeviceUpdateStatus.FAILED
                    EmitterUpdateStatus.UNAVAILABLE -> DeviceUpdateStatus.UNAVAILABLE
                },
                version = update.version,
                forced = update.forced,
                message = when (update.status) {
                    EmitterUpdateStatus.SKIPPED -> deviceVersion?.let(Strings::updateSkipped)
                    EmitterUpdateStatus.FAILED -> Strings.UPDATE_SEND_FAILED
                    EmitterUpdateStatus.UNAVAILABLE -> Strings.UPDATE_SEND_UNAVAILABLE
                    else -> null
                },
            )
        }

    private val deliveryCoordinator = EmitterDeliveryCoordinator(
        uiState = _uiState,
        service = service,
        emitterId = emitterId,
        scope = scope,
    )

    private val securityCoordinator = EmitterSecurityCoordinator(
        uiState = _uiState,
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
        uiState = _uiState,
        service = service,
        logger = logger,
        emitterId = emitterId,
        emitterName = emitterName,
        scope = scope,
        onPaired = { device ->
            selectedIds += device.deviceId
            rebuild()
        },
        onUnpaired = { deviceId ->
            selectedIds -= deviceId
            _uiState.update { it.copy(syncByDevice = it.syncByDevice - deviceId) }
            rebuild()
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
        uiState = _uiState,
        service = service,
        scope = scope,
    )

    fun onAction(action: EmitterAction) {
        if (action == EmitterAction.DismissPairDialog && _uiState.value.isPairing) return
        if (action == EmitterAction.DismissManualIpDialog && _uiState.value.isManualIpLoading) return
        _uiState.update { EmitterReducer.reduce(it, action) }
        when (action) {
            EmitterAction.RefreshDevices -> refreshDevices()
            EmitterAction.OpenManualIp,
            EmitterAction.DismissManualIpDialog,
            is EmitterAction.UpdateManualIp,
            EmitterAction.DismissStatusMessage,
            is EmitterAction.OpenPairDialog,
            EmitterAction.DismissPairDialog,
            is EmitterAction.UpdatePairingCode,
            is EmitterAction.RequestUnpair,
            EmitterAction.DismissUnpairDialog,
            is EmitterAction.UpdatePasswordDraft,
            is EmitterAction.UpdatePasswordConfirmDraft,
            EmitterAction.DismissPasswordDialog,
            EmitterAction.ToggleRecoveryEditor,
            is EmitterAction.SetRecoveryAccepted,
            is EmitterAction.UpdateRecoveryDraft,
            is EmitterAction.UpdateRecoveryConfirmDraft,
            EmitterAction.DismissSecurityDialog -> Unit

            is EmitterAction.ToggleSelection -> toggleSelection(action.deviceId)
            is EmitterAction.RequestUpdate -> service.requestUpdate(action.deviceId, emitterId)
            EmitterAction.OpenSecurityDialog -> securityCoordinator.openSecurityDialog()
            is EmitterAction.SendToSelected -> sendToSelected(action.playlist)
            is EmitterAction.RequestProjection -> requestProjection(action.playlist)
            EmitterAction.SynchronizePlayback -> deliveryCoordinator.synchronizePlayback()
            EmitterAction.ConfirmPassword -> securityCoordinator.confirmPassword()
            is EmitterAction.AppendUnlockKey,
            EmitterAction.RemoveLastUnlockKey,
            EmitterAction.ResetUnlockSequence -> Unit

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
                serviceState = newState
                val pairedIds = newState.devices.filter { it.pairedToThisEmitter }.mapTo(mutableSetOf()) { it.deviceId }
                selectedIds.retainAll(pairedIds)
                rebuild()
            }
        }
        jobs += scope.launch {
            service.loadKioskLock().fold(
                onSuccess = { lock -> _uiState.update { it.copy(hasKioskPassword = lock != null) } },
                onFailure = {
                    _uiState.update {
                        it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true)
                    }
                },
            )
        }
    }

    fun stop() {
        started = false
        jobs.forEach(Job::cancel)
        jobs.clear()
        service.stop()
    }

    private fun refreshDevices() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        service.refresh()
        scope.launch {
            delay(1_500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun toggleSelection(deviceId: String) {
        val device = _uiState.value.devices.firstOrNull { it.deviceId == deviceId } ?: return
        if (!device.isPairedToThisEmitter) return
        if (!selectedIds.add(deviceId)) selectedIds.remove(deviceId)
        rebuild()
    }

    private fun sendToSelected(playlist: Playlist) {
        val state = _uiState.value
        if (state.isSending || state.isSyncing) return
        if (playlist.items.isEmpty()) {
            _uiState.update { it.copy(statusMessage = Strings.SEND_EMPTY_PLAYLIST, statusIsError = true) }
            return
        }
        if (state.devices.none { it.isSelected && it.isPairedToThisEmitter }) {
            _uiState.update { it.copy(statusMessage = Strings.SEND_NO_SCREENS, statusIsError = true) }
            return
        }

        scope.launch {
            service.loadKioskLock().fold(
                onSuccess = { lock ->
                    if (lock == null) securityCoordinator.openPasswordDialog(EmitterPendingAction.SEND, playlist)
                    else deliveryCoordinator.send(playlist, lock)
                },
                onFailure = {
                    _uiState.update { it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true) }
                },
            )
        }
    }

    private fun requestProjection(playlist: Playlist) {
        if (_uiState.value.isSending || _uiState.value.isSyncing || projectionController.isProjecting) return
        if (playlist.items.isEmpty()) {
            _uiState.update { it.copy(statusMessage = Strings.PROJECT_NO_PLAYLIST, statusIsError = true) }
            return
        }

        scope.launch {
            service.loadKioskLock().fold(
                onSuccess = { lock ->
                    if (lock == null) securityCoordinator.openPasswordDialog(EmitterPendingAction.PROJECT, playlist)
                    else startProjection(playlist, lock)
                },
                onFailure = {
                    _uiState.update { it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true) }
                },
            )
        }
    }

    private fun startProjection(playlist: Playlist, lock: KioskLockConfig) {
        kioskLockManager.setConfig(lock)
        projectionController.start(playlist)
        kioskLockManager.lock(force = true)
        logger.info("Projection started with playlist '${playlist.name}'")
    }

    private fun rebuild() {
        val devices = serviceState.devices.map { it.toUiDevice() }.sortedBy { it.deviceName.lowercase() }
        _uiState.update {
            it.copy(
                devices = devices,
                isSearching = serviceState.isSearching,
                updateByDevice = updateByDevice,
            )
        }
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
