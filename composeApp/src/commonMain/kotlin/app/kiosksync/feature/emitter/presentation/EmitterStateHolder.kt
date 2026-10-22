package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.DEFAULT_PORT
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.STATUS_FAILURE_THRESHOLD
import app.kiosksync.core.STATUS_POLL_INTERVAL_MS
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.kiosk.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.projection.ProjectionController
import app.kiosksync.platform.DeviceDiscovery
import app.kiosksync.platform.EmitterClient
import app.kiosksync.platform.MediaRepository
import app.kiosksync.platform.UpdatePackageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmitterStateHolder(
    private val discovery: DeviceDiscovery,
    private val client: EmitterClient,
    private val screensRepository: EmitterScreensRepository,
    private val kioskLockRepository: KioskLockRepository,
    private val mediaRepository: MediaRepository,
    private val logger: AppLogger,
    private val emitterId: String,
    private val emitterName: String,
    private val scope: CoroutineScope,
    private val serviceScope: CoroutineScope,
    private val kioskLockManager: KioskLockManager,
    private val projectionController: ProjectionController,
    passwordHasher: (String) -> String,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val updatePackageProvider: UpdatePackageProvider? = null
) {
    private val _uiState = MutableStateFlow(EmitterUiState())
    val uiState: StateFlow<EmitterUiState> = _uiState.asStateFlow()

    private var discovered: List<DiscoveredDevice> = emptyList()
    private val manualDevices = mutableMapOf<String, DiscoveredDevice>()
    private var storedDevices: List<EmitterPairedDevice> = emptyList()
    private val onlineStatus = mutableMapOf<String, Boolean>()
    private val failedChecks = mutableMapOf<String, Int>()
    private var selectedIds = emptySet<String>()
    private var started = false
    private var staleCleanupJob: Job? = null

    /**
     * Screens paired in this session. mDNS takes a moment to report them as
     * paired, so they are exempt from the "forget unpaired screens" cleanup
     * until the network confirms them.
     */
    private val justPairedIds = mutableSetOf<String>()
    private val jobs = mutableListOf<Job>()

    private val updateCoordinator = EmitterUpdateCoordinator(
        uiState = _uiState,
        client = client,
        logger = logger,
        emitterId = emitterId,
        scope = scope,
        backgroundDispatcher = backgroundDispatcher,
        updatePackageProvider = updatePackageProvider,
        getStoredDevices = { storedDevices },
        targetFor = ::targetFor
    )

    private val deliveryCoordinator = EmitterDeliveryCoordinator(
        uiState = _uiState,
        client = client,
        mediaRepository = mediaRepository,
        logger = logger,
        emitterId = emitterId,
        scope = scope,
        backgroundDispatcher = backgroundDispatcher,
        getStoredDevices = { storedDevices },
        targetFor = ::targetFor,
    )

    private val securityCoordinator = EmitterSecurityCoordinator(
        uiState = _uiState,
        repository = kioskLockRepository,
        scope = scope,
        backgroundDispatcher = backgroundDispatcher,
        logger = logger,
        kioskLockManager = kioskLockManager,
        passwordHasher = passwordHasher,
        pushLockToAllPaired = ::pushLockToAllPaired,
        onPasswordConfirmed = { dialog, config ->
            when (dialog.action) {
                EmitterPendingAction.SEND -> dialog.playlist?.let { deliveryCoordinator.send(it, config) }
                EmitterPendingAction.PROJECT -> dialog.playlist?.let { startProjection(it, config) }
                EmitterPendingAction.CONFIGURE -> pushLockToAllPaired(config)
                EmitterPendingAction.PAIR_LOCK -> dialog.deviceId?.let { pushLockToDevice(it, config) }
            }
        },
    )

    private val pairingCoordinator = EmitterPairingCoordinator(
        uiState = _uiState,
        client = client,
        screensRepository = screensRepository,
        kioskLockRepository = kioskLockRepository,
        logger = logger,
        emitterId = emitterId,
        emitterName = emitterName,
        scope = scope,
        backgroundDispatcher = backgroundDispatcher,
        getStoredDevices = { storedDevices },
        setStoredDevices = { storedDevices = it },
        targetFor = ::targetFor,
        onPaired = { device ->
            failedChecks[device.deviceId] = 0
            onlineStatus[device.deviceId] = true
            selectedIds = selectedIds + device.deviceId
            justPairedIds += device.deviceId
            device.appVersion?.let {
                updateCoordinator.updateDeviceInfo(device.deviceId, it, skippedVersion = null)
            }
            updateCoordinator.forgetSkippedUpdate(device.deviceId)
        },
        onUnpaired = { deviceId ->
            onlineStatus.remove(deviceId)
            failedChecks.remove(deviceId)
            selectedIds = selectedIds - deviceId
            updateCoordinator.clearUpdateState(deviceId)
        },
        openPairLockDialog = { deviceId ->
            securityCoordinator.openPasswordDialog(
                action = EmitterPendingAction.PAIR_LOCK,
                playlist = null,
                deviceId = deviceId,
            )
        },
        pushLockToDevice = ::pushLockToDevice,
        rebuild = ::rebuild,
    )

    private val manualIpCoordinator = EmitterManualIpCoordinator(
        uiState = _uiState,
        client = client,
        logger = logger,
        scope = scope,
        backgroundDispatcher = backgroundDispatcher,
        onDeviceInfo = updateCoordinator::updateDeviceInfo,
        onDeviceAdded = { device ->
            manualDevices[device.deviceId] = device
            rebuild()
        },
        parseHostPort = ::parseHostPort,
    )

    /** Accepts typed UI actions; transient state changes pass through a pure reducer. */
    fun dispatch(action: EmitterAction) {
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
            is EmitterAction.RequestUpdate -> updateCoordinator.requestUpdate(action.deviceId)
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
            EmitterAction.ConfirmPair -> confirmPair()
            EmitterAction.ConfirmUnpair -> confirmUnpair()
            EmitterAction.ConfirmManualIp -> confirmManualIp()
        }
    }

    fun start() {
        if (started) return
        started = true
        logger.info(
            "Automatic updates: " +
                if (updatePackageProvider?.isAvailable() == true) {
                    "enabled (package $APP_VERSION available)"
                } else {
                    "unavailable (controller is not running from a packaged install)"
                }
        )

        jobs += scope.launch {
            try {
                storedDevices = withContext(backgroundDispatcher) { screensRepository.loadPairedScreens() }
                logger.info("Emitter '$emitterName' ($emitterId) started with ${storedDevices.size} paired screens")
                val existingLock = withContext(backgroundDispatcher) { kioskLockRepository.load() }
                    ?.takeIf { it.enabled && it.passwordHash != null }
                if (existingLock != null && storedDevices.isNotEmpty()) pushLockToAllPaired(existingLock)
                rebuild()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not load Controller local data", error)
                _uiState.update { it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true) }
            }
        }
        jobs += scope.launch {
            discovery.discoveredDevices.collect { devices ->
                updateDiscoveredDevices(devices)
            }
        }
        jobs += scope.launch { pollStatuses() }

        // mDNS initialization can block; the serial service worker keeps it off
        // the UI and ordered with the receiver server lifecycle.
        serviceScope.launch { discovery.startDiscovery() }
        rebuild()
    }

    fun stop() {
        started = false
        jobs.forEach { it.cancel() }
        jobs.clear()
        staleCleanupJob?.cancel()
        staleCleanupJob = null
        updateCoordinator.stop()
        serviceScope.launch { discovery.stopDiscovery() }
        logger.info("Emitter discovery stopped")
    }

    /**
     * Manual "scan again" from the screens panel. Forces a fresh mDNS browse so
     * a screen that just appeared shows up without waiting for its next
     * announcement.
     */
    fun refreshDevices() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        logger.info("Manual screen scan requested")
        serviceScope.launch { discovery.refresh() }
        scope.launch {
            delay(1500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleSelection(deviceId: String) {
        val device = _uiState.value.devices.firstOrNull { it.deviceId == deviceId } ?: return
        if (!device.isPairedToThisEmitter) return
        selectedIds = if (deviceId in selectedIds) selectedIds - deviceId else selectedIds + deviceId
        rebuild()
    }

    fun openPairDialog(device: EmitterDeviceUi) {
        _uiState.update {
            it.copy(
                pairingDevice = device,
                pairingCode = "",
                pairingError = null,
                isPairing = false
            )
        }
    }

    fun dismissPairDialog() {
        if (_uiState.value.isPairing) return
        _uiState.update {
            it.copy(pairingDevice = null, pairingCode = "", pairingError = null)
        }
    }

    fun updatePairingCode(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(pairingCode = digits, pairingError = null) }
    }

    fun confirmPair() = pairingCoordinator.confirmPair()

    fun requestUnpair(device: EmitterDeviceUi) {
        _uiState.update { it.copy(unpairDevice = device) }
    }

    fun dismissUnpairDialog() {
        _uiState.update { it.copy(unpairDevice = null) }
    }

    fun confirmUnpair() = pairingCoordinator.confirmUnpair()

    fun openManualIpDialog() = manualIpCoordinator.openDialog()

    fun dismissManualIpDialog() = manualIpCoordinator.dismissDialog()

    fun updateManualIp(value: String) = manualIpCoordinator.updateInput(value)

    fun confirmManualIp() = manualIpCoordinator.confirm()

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, statusIsError = false) }
    }

    fun sendToSelected(playlist: Playlist) {
        val state = _uiState.value
        if (state.isSending || state.isSyncing) return
        if (playlist.items.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = Strings.SEND_EMPTY_PLAYLIST, statusIsError = true)
            }
            return
        }

        val selected = state.devices.filter { it.isSelected && it.isPairedToThisEmitter }
        if (selected.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = Strings.SEND_NO_SCREENS, statusIsError = true)
            }
            return
        }

        scope.launch {
            val lock = try {
                withContext(backgroundDispatcher) { kioskLockRepository.load() }
                    ?.takeIf { it.enabled && it.passwordHash != null }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not load kiosk security configuration", error)
                _uiState.update { it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true) }
                return@launch
            }
            if (lock == null) securityCoordinator.openPasswordDialog(EmitterPendingAction.SEND, playlist)
            else deliveryCoordinator.send(playlist, lock)
        }
    }

    fun synchronizePlayback() = deliveryCoordinator.synchronizePlayback()

    fun requestProjection(playlist: Playlist) {
        if (_uiState.value.isSending || _uiState.value.isSyncing || projectionController.isProjecting) return
        if (playlist.items.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = Strings.PROJECT_NO_PLAYLIST, statusIsError = true)
            }
            return
        }

        scope.launch {
            val lock = try {
                withContext(backgroundDispatcher) { kioskLockRepository.load() }
                    ?.takeIf { it.enabled && it.passwordHash != null }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not load kiosk security configuration", error)
                _uiState.update { it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true) }
                return@launch
            }
            if (lock == null) securityCoordinator.openPasswordDialog(EmitterPendingAction.PROJECT, playlist)
            else startProjection(playlist, lock)
        }
    }

    fun openSecurityDialog() = securityCoordinator.openSecurityDialog()

    fun dismissSecurityDialog() {
        _uiState.update { it.copy(securityDialog = null) }
    }

    fun confirmPassword() = securityCoordinator.confirmPassword()

    private fun startProjection(playlist: Playlist, lock: KioskLockConfig) {
        kioskLockManager.setConfig(lock)
        projectionController.start(playlist)
        // The controller is also on display: lock it with the same password.
        // `force` keeps the lock even in --windowed debug mode, where the
        // native hook is disabled and Compose feeds the key events instead.
        kioskLockManager.lock(force = true)
        logger.info("Projection started with playlist '${playlist.name}'")
    }

    private suspend fun pollStatuses() {
        while (scope.isActive) {
            for (device in storedDevices) {
                val target = targetFor(device)
                if (target == null) {
                    registerFailure(device.deviceId)
                    continue
                }
                val statusResult = withContext(backgroundDispatcher) {
                    client.getStatus(target.first, target.second, emitterId, device.token)
                }
                statusResult
                    .onSuccess { status ->
                        failedChecks[device.deviceId] = 0
                        onlineStatus[device.deviceId] = true
                        updateCoordinator.updateDeviceInfo(
                            device.deviceId,
                            status.appVersion,
                            status.skippedUpdateVersion
                        )
                    }
                    .onFailure {
                        registerFailure(device.deviceId)
                    }
            }
            rebuild()
            delay(STATUS_POLL_INTERVAL_MS)
        }
    }

    private fun registerFailure(deviceId: String) {
        val failures = (failedChecks[deviceId] ?: 0) + 1
        failedChecks[deviceId] = failures
        if (failures >= STATUS_FAILURE_THRESHOLD) {
            onlineStatus[deviceId] = false
        }
    }

    private fun targetFor(device: EmitterPairedDevice): Pair<String, Int>? {
        val current = allDiscovered().firstOrNull { it.deviceId == device.deviceId }
        val host = current?.host ?: device.host
        val port = current?.port ?: device.port
        return if (!host.isNullOrBlank() && port != null) host to port else null
    }

    /**
     * Pushes the kiosk lock to a single paired screen. Runs in the background:
     * a screen that is offline stays "waiting for password" and will receive it
     * on the next attempt (pairing again or a password change).
     */
    private fun pushLockToDevice(deviceId: String, lock: KioskLockConfig) {
        val stored = storedDevices.firstOrNull { it.deviceId == deviceId } ?: return
        val target = targetFor(stored) ?: return
        scope.launch {
            val result = withContext(backgroundDispatcher) {
                client.putLock(target.first, target.second, emitterId, stored.token, lock)
            }
            result
                .onSuccess { logger.info("Kiosk lock sent to '${stored.deviceName}'") }
                .onFailure {
                    logger.warn("Could not send kiosk lock to '${stored.deviceName}': ${it.message}")
                }
        }
    }

    private fun pushLockToAllPaired(lock: KioskLockConfig) {
        storedDevices.forEach { pushLockToDevice(it.deviceId, lock) }
    }

    private fun allDiscovered(): List<DiscoveredDevice> =
        discovered + manualDevices.values.filter { manual -> discovered.none { it.deviceId == manual.deviceId } }

    private suspend fun updateDiscoveredDevices(devices: List<DiscoveredDevice>) {
        discovered = devices
        val staleIds = devices.asSequence()
            .filter { it.deviceId in storedDevices.map(EmitterPairedDevice::deviceId) }
            .filter { !it.paired && it.deviceId !in justPairedIds }
            .map(DiscoveredDevice::deviceId)
            .toList()
        if (staleIds.isNotEmpty()) {
            try {
                storedDevices = withContext(backgroundDispatcher) {
                    for (deviceId in staleIds) screensRepository.removePairedScreen(deviceId)
                    screensRepository.loadPairedScreens()
                }
                staleIds.forEach { id ->
                    onlineStatus.remove(id)
                    failedChecks.remove(id)
                    selectedIds = selectedIds - id
                    updateCoordinator.clearUpdateState(id)
                }
                logger.info("Forgot ${staleIds.size} screen(s) that are no longer paired: $staleIds")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not remove screens that are no longer paired", error)
            }
        }

        devices.asSequence()
            .filter(DiscoveredDevice::paired)
            .forEach { justPairedIds.remove(it.deviceId) }
        rebuild()
    }

    private fun rebuild() {
        val discoveredById = allDiscovered().associateBy { it.deviceId }
        val storedById = storedDevices.associateBy { it.deviceId }

        val devices = (storedById.keys + discoveredById.keys).mapNotNull { id ->
            val stored = storedById[id]
            val live = discoveredById[id]
            when {
                stored != null -> EmitterDeviceUi(
                    deviceId = id,
                    deviceName = live?.deviceName ?: stored.deviceName,
                    host = live?.host ?: stored.host.orEmpty(),
                    port = live?.port ?: stored.port ?: DEFAULT_PORT,
                    isPairedToThisEmitter = true,
                    isPairedToAnotherEmitter = false,
                    isOnline = onlineStatus[id] ?: false,
                    isSelected = id in selectedIds,
                    appVersion = updateCoordinator.appVersionFor(id) ?: live?.appVersion
                )

                live != null -> EmitterDeviceUi(
                    deviceId = id,
                    deviceName = live.deviceName,
                    host = live.host,
                    port = live.port,
                    isPairedToThisEmitter = false,
                    isPairedToAnotherEmitter = live.paired,
                    isOnline = false,
                    isSelected = false,
                    appVersion = live.appVersion ?: updateCoordinator.appVersionFor(id)
                )

                else -> null
            }
        }.sortedBy { it.deviceName.lowercase() }

        _uiState.update {
            it.copy(devices = devices, isSearching = false)
        }
        updateCoordinator.maybeAutoUpdateAll()
    }

    companion object {
        fun parseHostPort(input: String): Pair<String, Int>? {
            val trimmed = input.trim()
                .removePrefix("http://")
                .removePrefix("https://")
            val separator = trimmed.lastIndexOf(':')
            if (separator <= 0) return null
            val host = trimmed.substring(0, separator).trim().trim('[', ']')
            val port = trimmed.substring(separator + 1).trim().toIntOrNull() ?: return null
            if (host.isBlank() || port !in 1..65535) return null
            return host to port
        }
    }
}
