package app.kiosksync.feature.pairing

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppVersionPolicy
import app.kiosksync.core.DEFAULT_PORT
import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH
import app.kiosksync.core.MAX_UNLOCK_SEQUENCE_LENGTH
import app.kiosksync.core.PairError
import app.kiosksync.core.PairRejectedException
import app.kiosksync.core.PLAYBACK_SYNC_DELAY_MS
import app.kiosksync.core.PlaylistRejectedException
import app.kiosksync.core.STATUS_FAILURE_THRESHOLD
import app.kiosksync.core.STATUS_POLL_INTERVAL_MS
import app.kiosksync.core.Strings
import app.kiosksync.core.UpdateRejectedException
import app.kiosksync.core.UpdateUrgency
import app.kiosksync.core.isValidKioskPassword
import app.kiosksync.core.isValidRecoveryPhrase
import app.kiosksync.core.isValidUnlockSequence
import app.kiosksync.feature.kiosk.KioskLockManager
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.projection.ProjectionController
import app.kiosksync.feature.sync.EmitterSyncService
import app.kiosksync.feature.sync.LocalMediaMissingException
import app.kiosksync.platform.DeviceDiscovery
import app.kiosksync.platform.EmitterClient
import app.kiosksync.platform.LocalStorage
import app.kiosksync.platform.UpdatePackageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

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

enum class DeviceUpdateStatus {
    PREPARING,
    UPLOADING,
    AWAITING_CONFIRMATION,
    APPLYING,
    SKIPPED,
    FAILED,
    UNAVAILABLE
}

data class DeviceUpdateUi(
    val status: DeviceUpdateStatus,
    val version: String,
    val forced: Boolean = false,
    val message: String? = null
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
    val updateByDevice: Map<String, DeviceUpdateUi> = emptyMap(),
    val isSending: Boolean = false,
    val isSyncing: Boolean = false,
    val passwordDialog: KioskPasswordDialogUi? = null,
    val securityDialog: SecurityDialogUi? = null,
    val hasKioskPassword: Boolean = false,
    val isRefreshing: Boolean = false
)

class EmitterStateHolder(
    private val discovery: DeviceDiscovery,
    private val client: EmitterClient,
    private val storage: LocalStorage,
    private val logger: AppLogger,
    private val emitterId: String,
    private val emitterName: String,
    private val scope: CoroutineScope,
    private val serviceScope: CoroutineScope,
    private val kioskLockManager: KioskLockManager,
    private val projectionController: ProjectionController,
    private val passwordHasher: (String) -> String,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val updatePackageProvider: UpdatePackageProvider? = null
) {
    private val _uiState = MutableStateFlow(
        EmitterUiState(hasKioskPassword = storage.loadKioskLock()?.passwordHash != null)
    )
    val uiState: StateFlow<EmitterUiState> = _uiState.asStateFlow()

    private val syncService = EmitterSyncService(client, storage, logger)

    private var discovered: List<DiscoveredDevice> = emptyList()
    private val manualDevices = mutableMapOf<String, DiscoveredDevice>()
    private var storedDevices: List<EmitterPairedDevice> = emptyList()
    private val onlineStatus = mutableMapOf<String, Boolean>()
    private val failedChecks = mutableMapOf<String, Int>()
    private var selectedIds = emptySet<String>()

    /**
     * Screens paired in this session. mDNS takes a moment to report them as
     * paired, so they are exempt from the "forget unpaired screens" cleanup
     * until the network confirms them.
     */
    private val justPairedIds = mutableSetOf<String>()
    private val jobs = mutableListOf<Job>()

    private val deviceVersions = mutableMapOf<String, String>()
    private val skippedUpdateVersions = mutableMapOf<String, String?>()
    private val sentUpdateVersions = mutableMapOf<String, String>()
    private val updateSentAt = mutableMapOf<String, TimeMark>()
    private val updateRetryAt = mutableMapOf<String, TimeMark>()
    private val updateJobs = mutableMapOf<String, Job>()
    private val updateUploadMutex = Mutex()
    private val unavailableUpdateNotice = mutableSetOf<String>()

    fun start() {
        if (jobs.isNotEmpty()) return
        storedDevices = storage.loadEmitterPairedDevices()
        logger.info("Emitter '$emitterName' ($emitterId) started with ${storedDevices.size} paired screens")
        logger.info(
            "Automatic updates: " +
                if (updatePackageProvider?.isAvailable() == true) {
                    "enabled (package $APP_VERSION available)"
                } else {
                    "unavailable (controller is not running from a packaged install)"
                }
        )

        // Make sure already-paired screens have the current lock. Screens that
        // were offline receive it as soon as they are reachable again.
        val existingLock = storage.loadKioskLock()
            ?.takeIf { it.enabled && it.passwordHash != null }
        if (existingLock != null && storedDevices.isNotEmpty()) {
            pushLockToAllPaired(existingLock)
        }

        jobs += scope.launch {
            discovery.discoveredDevices.collect { devices ->
                discovered = devices
                rebuild()
            }
        }
        jobs += scope.launch { pollStatuses() }

        // mDNS initialization can block; the serial service worker keeps it off
        // the UI and ordered with the receiver server lifecycle.
        serviceScope.launch { discovery.startDiscovery() }
        rebuild()
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        updateJobs.values.forEach { it.cancel() }
        updateJobs.clear()
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

    fun confirmPair() {
        val state = _uiState.value
        val device = state.pairingDevice ?: return
        if (state.pairingCode.length != 6 || state.isPairing) return

        scope.launch {
            _uiState.update { it.copy(isPairing = true, pairingError = null) }
            val result = withContext(backgroundDispatcher) {
                client.pair(
                    host = device.host,
                    port = device.port,
                    emitterId = emitterId,
                    emitterName = emitterName,
                    code = state.pairingCode
                )
            }
            result.fold(
                onSuccess = { response ->
                    storage.addEmitterPairedDevice(
                        EmitterPairedDevice(
                            deviceId = device.deviceId,
                            deviceName = device.deviceName,
                            token = response.token,
                            host = device.host,
                            port = device.port
                        )
                    )
                    storedDevices = storage.loadEmitterPairedDevices()
                    failedChecks[device.deviceId] = 0
                    onlineStatus[device.deviceId] = true
                    selectedIds = selectedIds + device.deviceId
                    justPairedIds += device.deviceId
                    device.appVersion?.let { deviceVersions[device.deviceId] = it }
                    skippedUpdateVersions.remove(device.deviceId)
                    logger.info("Screen '${device.deviceName}' paired successfully")
                    val existingLock = storage.loadKioskLock()
            ?.takeIf { it.enabled && it.passwordHash != null }
                    _uiState.update {
                        it.copy(
                            pairingDevice = null,
                            pairingCode = "",
                            pairingError = null,
                            isPairing = false,
                            statusMessage = Strings.PAIR_SUCCESS,
                            statusIsError = false,
                            // The freshly paired screen waits until it receives a
                            // password. If the controller has none yet, ask for it
                            // right away so the screen can lock itself.
                            passwordDialog = if (existingLock == null) {
                                KioskPasswordDialogUi(
                                    action = EmitterPendingAction.PAIR_LOCK,
                                    deviceId = device.deviceId
                                )
                            } else {
                                null
                            }
                        )
                    }
                    rebuild()
                    if (existingLock != null) {
                        pushLockToDevice(device.deviceId, existingLock)
                    }
                },
                onFailure = { error ->
                    logger.warn("Pairing failed for '${device.deviceName}': ${error.message}")
                    _uiState.update {
                        it.copy(isPairing = false, pairingError = mapPairError(error))
                    }
                }
            )
        }
    }

    fun requestUnpair(device: EmitterDeviceUi) {
        _uiState.update { it.copy(unpairDevice = device) }
    }

    fun dismissUnpairDialog() {
        _uiState.update { it.copy(unpairDevice = null) }
    }

    fun confirmUnpair() {
        val device = _uiState.value.unpairDevice ?: return
        val storedDevice = storedDevices.firstOrNull { it.deviceId == device.deviceId }
        if (storedDevice == null) {
            _uiState.update { it.copy(unpairDevice = null) }
            return
        }

        scope.launch {
            _uiState.update { it.copy(unpairDevice = null) }
            val target = targetFor(storedDevice)

            // Remove it locally first. Unlinking must work even when the screen
            // is powered off or unreachable, which is the common case when a
            // screen is being retired.
            storage.removeEmitterPairedDevice(device.deviceId)
            storedDevices = storage.loadEmitterPairedDevices()
            onlineStatus.remove(device.deviceId)
            failedChecks.remove(device.deviceId)
            selectedIds = selectedIds - device.deviceId
            clearUpdateState(device.deviceId)
            logger.info("Screen '${device.deviceName}' removed from this controller")
            _uiState.update {
                it.copy(
                    statusMessage = Strings.UNPAIR_SUCCESS,
                    statusIsError = false,
                    syncByDevice = it.syncByDevice - device.deviceId
                )
            }
            rebuild()

            // Best effort: if the screen is online, tell it to forget the token
            // too. A failure here does not undo the local removal.
            if (target != null) {
                val result = withContext(backgroundDispatcher) {
                    client.unpair(target.first, target.second, emitterId, storedDevice.token)
                }
                result.onSuccess {
                    logger.info("Screen '${device.deviceName}' also unpaired remotely")
                }.onFailure { error ->
                    logger.warn(
                        "Screen '${device.deviceName}' removed locally; " +
                            "remote unpair failed: ${error.message}"
                    )
                }
            }
        }
    }

    fun openManualIpDialog() {
        _uiState.update {
            it.copy(
                showManualIpDialog = true,
                manualIpInput = "",
                manualIpError = null,
                isManualIpLoading = false
            )
        }
    }

    fun dismissManualIpDialog() {
        if (_uiState.value.isManualIpLoading) return
        _uiState.update {
            it.copy(showManualIpDialog = false, manualIpInput = "", manualIpError = null)
        }
    }

    fun updateManualIp(value: String) {
        _uiState.update { it.copy(manualIpInput = value, manualIpError = null) }
    }

    fun confirmManualIp() {
        val state = _uiState.value
        if (state.isManualIpLoading) return
        val parsed = parseHostPort(state.manualIpInput)
        if (parsed == null) {
            _uiState.update { it.copy(manualIpError = Strings.MANUAL_IP_INVALID) }
            return
        }

        val (host, port) = parsed
        scope.launch {
            _uiState.update { it.copy(isManualIpLoading = true, manualIpError = null) }
            val result = withContext(backgroundDispatcher) {
                client.getInfo(host, port)
            }
            result.fold(
                onSuccess = { info ->
                    if (info.role != "receiver") {
                        _uiState.update {
                            it.copy(
                                isManualIpLoading = false,
                                manualIpError = Strings.MANUAL_IP_NOT_RECEIVER
                            )
                        }
                        return@fold
                    }
                    manualDevices[info.deviceId] = DiscoveredDevice(
                        deviceId = info.deviceId,
                        deviceName = info.deviceName,
                        host = host,
                        port = port,
                        paired = info.paired,
                        appVersion = info.appVersion
                    )
                    deviceVersions[info.deviceId] = info.appVersion
                    skippedUpdateVersions[info.deviceId] = info.skippedUpdateVersion
                    logger.info("Screen '${info.deviceName}' added manually at $host:$port")
                    _uiState.update {
                        it.copy(
                            showManualIpDialog = false,
                            manualIpInput = "",
                            manualIpError = null,
                            isManualIpLoading = false
                        )
                    }
                    rebuild()
                },
                onFailure = { error ->
                    logger.warn("Manual screen $host:$port unreachable: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isManualIpLoading = false,
                            manualIpError = Strings.MANUAL_IP_UNREACHABLE
                        )
                    }
                }
            )
        }
    }

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

        val lock = storage.loadKioskLock()
            ?.takeIf { it.enabled && it.passwordHash != null }
        if (lock == null) {
            openPasswordDialog(EmitterPendingAction.SEND, playlist)
            return
        }
        performSend(playlist, lock)
    }

    /**
     * Best-effort playback synchronisation: every paired screen receives the
     * same relative delay and restarts its carousel when that delay expires.
     * Relative delays avoid depending on the clocks of the different PCs.
     */
    fun synchronizePlayback() {
        val state = _uiState.value
        if (state.isSending || state.isSyncing) return

        val paired = state.devices.filter { it.isPairedToThisEmitter }
        if (paired.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = Strings.PLAYBACK_SYNC_NO_SCREENS, statusIsError = true)
            }
            return
        }

        val targets = paired.mapNotNull { device ->
            val stored = storedDevices.firstOrNull { it.deviceId == device.deviceId }
                ?: return@mapNotNull null
            val target = targetFor(stored) ?: return@mapNotNull null
            stored to target
        }
        val unresolved = paired.size - targets.size

        _uiState.update {
            it.copy(
                isSyncing = true,
                statusMessage = null,
                statusIsError = false
            )
        }

        scope.launch {
            val results = targets.map { (stored, target) ->
                async(backgroundDispatcher) {
                    client.synchronizePlayback(
                        host = target.first,
                        port = target.second,
                        emitterId = emitterId,
                        token = stored.token,
                        delayMs = PLAYBACK_SYNC_DELAY_MS
                    ).onFailure { error ->
                        logger.warn(
                            "Could not synchronise playback on '${stored.deviceName}': ${error.message}"
                        )
                    }.isSuccess
                }
            }.awaitAll()

            val success = results.count { it }
            val failure = results.size - success + unresolved
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    statusMessage = Strings.playbackSyncCompleted(success, failure),
                    statusIsError = failure > 0
                )
            }
        }
    }

    fun requestProjection(playlist: Playlist) {
        if (_uiState.value.isSending || _uiState.value.isSyncing || projectionController.isProjecting) return
        if (playlist.items.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = Strings.PROJECT_NO_PLAYLIST, statusIsError = true)
            }
            return
        }

        val lock = storage.loadKioskLock()
            ?.takeIf { it.enabled && it.passwordHash != null }
        if (lock == null) {
            openPasswordDialog(EmitterPendingAction.PROJECT, playlist)
            return
        }
        startProjection(playlist, lock)
    }

    fun openChangePasswordDialog() {
        _uiState.update { it.copy(securityDialog = null) }
        openPasswordDialog(EmitterPendingAction.CONFIGURE, null)
    }

    fun openSecurityDialog() {
        val config = storage.loadKioskLock()
        _uiState.update {
            it.copy(
                securityDialog = SecurityDialogUi(
                    sequence = config?.unlockSequence
                        ?.takeIf { keys -> isValidUnlockSequence(keys) }
                        ?: DEFAULT_UNLOCK_SEQUENCE,
                    hasCustomRecovery = config?.recoveryHash != null
                )
            )
        }
    }

    fun dismissSecurityDialog() {
        _uiState.update { it.copy(securityDialog = null) }
    }

    fun appendUnlockKey(key: String) {
        _uiState.update { state ->
            val dialog = state.securityDialog ?: return@update state
            if (dialog.sequence.size >= MAX_UNLOCK_SEQUENCE_LENGTH) return@update state
            state.copy(
                securityDialog = dialog.copy(
                    sequence = dialog.sequence + key,
                    sequenceError = null
                )
            )
        }
    }

    fun removeLastUnlockKey() {
        _uiState.update { state ->
            val dialog = state.securityDialog ?: return@update state
            if (dialog.sequence.isEmpty()) return@update state
            state.copy(
                securityDialog = dialog.copy(
                    sequence = dialog.sequence.dropLast(1),
                    sequenceError = null
                )
            )
        }
    }

    fun resetUnlockSequence() {
        _uiState.update { state ->
            val dialog = state.securityDialog ?: return@update state
            state.copy(
                securityDialog = dialog.copy(
                    sequence = DEFAULT_UNLOCK_SEQUENCE,
                    sequenceError = null
                )
            )
        }
    }

    fun saveUnlockSequence() {
        val dialog = _uiState.value.securityDialog ?: return
        if (!isValidUnlockSequence(dialog.sequence)) {
            _uiState.update {
                it.copy(
                    securityDialog = dialog.copy(
                        sequenceError = Strings.SECURITY_SEQUENCE_INVALID
                    )
                )
            }
            return
        }

        val previous = storage.loadKioskLock()
        val config = (previous ?: KioskLockConfig()).copy(
            enabled = true,
            unlockSequence = dialog.sequence
        )
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        _uiState.update {
            it.copy(
                hasKioskPassword = config.passwordHash != null,
                securityDialog = it.securityDialog?.copy(sequenceError = null),
                statusMessage = Strings.SECURITY_SEQUENCE_SAVED,
                statusIsError = false
            )
        }
        logger.info("Kiosk unlock sequence updated (${config.unlockSequence.size} keys)")
        // A screen only receives a lock that includes a password; otherwise the
        // push would erase its own password.
        if (config.passwordHash != null) pushLockToAllPaired(config)
    }

    fun toggleRecoveryEditor() {
        _uiState.update { state ->
            val dialog = state.securityDialog ?: return@update state
            state.copy(
                securityDialog = dialog.copy(
                    showRecoveryEditor = !dialog.showRecoveryEditor,
                    recoveryError = null
                )
            )
        }
    }

    fun setRecoveryAccepted(accepted: Boolean) {
        _uiState.update { state ->
            val dialog = state.securityDialog ?: return@update state
            state.copy(
                securityDialog = dialog.copy(
                    recoveryAccepted = accepted,
                    recoveryError = null
                )
            )
        }
    }

    fun updateRecoveryDraft(value: String) {
        _uiState.update { state ->
            val dialog = state.securityDialog ?: return@update state
            state.copy(
                securityDialog = dialog.copy(
                    recoveryDraft = value.take(MAX_RECOVERY_PHRASE_LENGTH),
                    recoveryError = null
                )
            )
        }
    }

    fun updateRecoveryConfirmDraft(value: String) {
        _uiState.update { state ->
            val dialog = state.securityDialog ?: return@update state
            state.copy(
                securityDialog = dialog.copy(
                    recoveryConfirmDraft = value.take(MAX_RECOVERY_PHRASE_LENGTH),
                    recoveryError = null
                )
            )
        }
    }

    fun saveRecoveryPhrase() {
        val dialog = _uiState.value.securityDialog ?: return
        if (!dialog.recoveryAccepted) {
            _uiState.update {
                it.copy(
                    securityDialog = dialog.copy(
                        recoveryError = Strings.SECURITY_RECOVERY_MUST_ACCEPT
                    )
                )
            }
            return
        }
        val phrase = dialog.recoveryDraft
        if (!isValidRecoveryPhrase(phrase)) {
            _uiState.update {
                it.copy(
                    securityDialog = dialog.copy(
                        recoveryError = Strings.SECURITY_RECOVERY_INVALID
                    )
                )
            }
            return
        }
        if (phrase != dialog.recoveryConfirmDraft) {
            _uiState.update {
                it.copy(
                    securityDialog = dialog.copy(
                        recoveryError = Strings.SECURITY_RECOVERY_MISMATCH
                    )
                )
            }
            return
        }

        val previous = storage.loadKioskLock() ?: KioskLockConfig()
        val config = previous.copy(
            enabled = true,
            recoveryHash = passwordHasher(phrase),
            recoveryLength = phrase.length
        )
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        _uiState.update {
            it.copy(
                hasKioskPassword = config.passwordHash != null,
                securityDialog = it.securityDialog?.copy(
                    hasCustomRecovery = true,
                    showRecoveryEditor = false,
                    recoveryAccepted = false,
                    recoveryDraft = "",
                    recoveryConfirmDraft = "",
                    recoveryError = null
                ),
                statusMessage = Strings.SECURITY_RECOVERY_SAVED,
                statusIsError = false
            )
        }
        logger.info("Custom kiosk recovery phrase configured")
        if (config.passwordHash != null) pushLockToAllPaired(config)
    }

    fun resetRecoveryPhrase() {
        val previous = storage.loadKioskLock() ?: return
        val config = previous.copy(recoveryHash = null, recoveryLength = 0)
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        _uiState.update {
            it.copy(
                securityDialog = it.securityDialog?.copy(
                    hasCustomRecovery = false,
                    showRecoveryEditor = false,
                    recoveryAccepted = false,
                    recoveryDraft = "",
                    recoveryConfirmDraft = "",
                    recoveryError = null
                ),
                statusMessage = Strings.SECURITY_RECOVERY_RESET_DONE,
                statusIsError = false
            )
        }
        logger.info("Kiosk recovery phrase restored to the universal default")
        if (config.passwordHash != null) pushLockToAllPaired(config)
    }

    fun updatePasswordDraft(value: String) {
        _uiState.update { state ->
            val dialog = state.passwordDialog ?: return@update state
            state.copy(
                passwordDialog = dialog.copy(
                    draft = value.take(64),
                    error = null
                )
            )
        }
    }

    fun updatePasswordConfirmDraft(value: String) {
        _uiState.update { state ->
            val dialog = state.passwordDialog ?: return@update state
            state.copy(
                passwordDialog = dialog.copy(
                    confirmDraft = value.take(64),
                    error = null
                )
            )
        }
    }

    fun dismissPasswordDialog() {
        _uiState.update { it.copy(passwordDialog = null) }
    }

    fun confirmPassword() {
        val state = _uiState.value
        val dialog = state.passwordDialog ?: return
        val password = dialog.draft
        if (!isValidKioskPassword(password)) {
            _uiState.update {
                it.copy(
                    passwordDialog = dialog.copy(error = Strings.KIOSK_PASSWORD_INVALID)
                )
            }
            return
        }
        if (password != dialog.confirmDraft) {
            _uiState.update {
                it.copy(
                    passwordDialog = dialog.copy(error = Strings.KIOSK_PASSWORD_MISMATCH)
                )
            }
            return
        }

        val previousLock = storage.loadKioskLock()
        val config = KioskLockConfig(
            passwordHash = passwordHasher(password),
            passwordLength = password.length,
            unlockSequence = previousLock?.unlockSequence
                ?.takeIf { keys -> isValidUnlockSequence(keys) }
                ?: DEFAULT_UNLOCK_SEQUENCE,
            recoveryHash = previousLock?.recoveryHash,
            recoveryLength = previousLock?.recoveryLength ?: 0
        )
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        _uiState.update {
            it.copy(
                passwordDialog = null,
                securityDialog = null,
                hasKioskPassword = true,
                statusMessage = Strings.KIOSK_PASSWORD_SAVED,
                statusIsError = false
            )
        }
        logger.info("Kiosk password configured from the controller")

        when (dialog.action) {
            EmitterPendingAction.SEND -> dialog.playlist?.let { performSend(it, config) }
            EmitterPendingAction.PROJECT -> dialog.playlist?.let { startProjection(it, config) }
            // Changing the password reaches every paired screen immediately.
            EmitterPendingAction.CONFIGURE -> pushLockToAllPaired(config)
            // The screen that just paired was waiting for this password.
            EmitterPendingAction.PAIR_LOCK -> dialog.deviceId?.let { pushLockToDevice(it, config) }
        }
    }

    private fun openPasswordDialog(
        action: EmitterPendingAction,
        playlist: Playlist?,
        deviceId: String? = null
    ) {
        _uiState.update {
            it.copy(
                passwordDialog = KioskPasswordDialogUi(
                    action = action,
                    playlist = playlist,
                    deviceId = deviceId
                )
            )
        }
    }

    private fun startProjection(playlist: Playlist, lock: KioskLockConfig) {
        kioskLockManager.setConfig(lock)
        projectionController.start(playlist)
        // The controller is also on display: lock it with the same password.
        // `force` keeps the lock even when the native hook is disabled and
        // Compose feeds the key events instead.
        kioskLockManager.lock(force = true)
        logger.info("Projection started with playlist '${playlist.name}'")
    }

    private fun performSend(playlist: Playlist, lock: KioskLockConfig) {
        val selected = _uiState.value.devices.filter { it.isSelected && it.isPairedToThisEmitter }
        if (selected.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = Strings.SEND_NO_SCREENS, statusIsError = true)
            }
            return
        }

        scope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    statusMessage = null,
                    statusIsError = false,
                    syncByDevice = selected.associate { device ->
                        device.deviceId to DeviceSyncUi(SyncUiStatus.PENDING)
                    }
                )
            }

            var successCount = 0
            var failureCount = 0

            for (device in selected) {
                val stored = storedDevices.firstOrNull { it.deviceId == device.deviceId }
                if (stored == null) {
                    updateDeviceSync(device.deviceId) {
                        DeviceSyncUi(SyncUiStatus.FAILED, message = Strings.SEND_ERROR_NETWORK)
                    }
                    failureCount++
                    continue
                }

                val target = targetFor(stored)
                if (target == null) {
                    updateDeviceSync(device.deviceId) {
                        DeviceSyncUi(SyncUiStatus.FAILED, message = Strings.SEND_ERROR_NETWORK)
                    }
                    failureCount++
                    continue
                }

                updateDeviceSync(device.deviceId) {
                    DeviceSyncUi(SyncUiStatus.UPLOADING)
                }

                val syncResult = withContext(backgroundDispatcher) {
                    syncService.sync(
                        device = stored,
                        host = target.first,
                        port = target.second,
                        emitterId = emitterId,
                        playlist = playlist,
                        lock = lock,
                        onUploadProgress = { progress ->
                            updateDeviceSync(device.deviceId) {
                                DeviceSyncUi(
                                    status = SyncUiStatus.UPLOADING,
                                    uploaded = progress.uploaded,
                                    total = progress.total
                                )
                            }
                        },
                        onApplying = {
                            updateDeviceSync(device.deviceId) {
                                it.copy(status = SyncUiStatus.APPLYING)
                            }
                        }
                    )
                }
                syncResult.fold(
                    onSuccess = { summary ->
                        successCount++
                        updateDeviceSync(device.deviceId) {
                            DeviceSyncUi(
                                status = SyncUiStatus.SUCCESS,
                                version = summary.appliedVersion,
                                message = if (summary.uploadedCount == 0) {
                                    Strings.SEND_DEVICE_UP_TO_DATE
                                } else {
                                    Strings.SEND_DEVICE_SUCCESS
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        failureCount++
                        logger.warn(
                            "Could not send playlist to '${device.deviceName}': ${error.message}"
                        )
                        updateDeviceSync(device.deviceId) {
                            DeviceSyncUi(
                                status = SyncUiStatus.FAILED,
                                message = syncErrorMessage(error)
                            )
                        }
                    }
                )
            }

            _uiState.update {
                it.copy(
                    isSending = false,
                    statusMessage = Strings.sendCompleted(successCount, failureCount),
                    statusIsError = failureCount > 0
                )
            }
        }
    }

    private fun updateDeviceSync(deviceId: String, transform: (DeviceSyncUi) -> DeviceSyncUi) {
        _uiState.update { state ->
            val current = state.syncByDevice[deviceId] ?: DeviceSyncUi(SyncUiStatus.PENDING)
            state.copy(syncByDevice = state.syncByDevice + (deviceId to transform(current)))
        }
    }

    private fun syncErrorMessage(error: Throwable): String = when (error) {
        is PlaylistRejectedException -> Strings.SEND_ERROR_MISSING
        is LocalMediaMissingException -> Strings.SEND_ERROR_LOCAL_MEDIA
        else -> Strings.SEND_ERROR_NETWORK
    }

    /**
     * The Screen returns a human-readable reason when it cannot stage the
     * update (for example, it is not running from a packaged install). Raw
     * protocol codes use underscores, so they stay hidden behind the generic
     * message.
     */
    private fun updateErrorMessage(error: Throwable): String {
        val reason = (error as? UpdateRejectedException)?.reason?.trim()
        return if (!reason.isNullOrBlank() && reason.none { it == '_' }) {
            reason
        } else {
            Strings.UPDATE_SEND_FAILED
        }
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
                        deviceVersions[device.deviceId] = status.appVersion
                        skippedUpdateVersions[device.deviceId] = status.skippedUpdateVersion
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

    private fun rebuild() {
        val discoveredById = allDiscovered().associateBy { it.deviceId }
        var storedById = storedDevices.associateBy { it.deviceId }

        // A screen always starts blank and forgets its pairing. When the network
        // reports a stored screen as unpaired, drop the stale pairing so the user
        // can pair it again with a fresh code instead of being stuck with a token
        // the screen no longer knows.
        val staleIds = discoveredById.values
            .filter { live -> live.deviceId in storedById.keys }
            .filter { live -> !live.paired && live.deviceId !in justPairedIds }
            .map { it.deviceId }

        if (staleIds.isNotEmpty()) {
            staleIds.forEach { storage.removeEmitterPairedDevice(it) }
            storedDevices = storage.loadEmitterPairedDevices()
            storedById = storedDevices.associateBy { it.deviceId }
            staleIds.forEach { id ->
                onlineStatus.remove(id)
                failedChecks.remove(id)
                selectedIds = selectedIds - id
                clearUpdateState(id)
            }
            logger.info("Forgot ${staleIds.size} screen(s) that are no longer paired: $staleIds")
        }

        // Once mDNS confirms the pairing, the exemption is no longer needed.
        discoveredById.values
            .filter { it.paired }
            .forEach { justPairedIds.remove(it.deviceId) }

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
                    appVersion = deviceVersions[id] ?: live?.appVersion
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
                    appVersion = live.appVersion ?: deviceVersions[id]
                )

                else -> null
            }
        }.sortedBy { it.deviceName.lowercase() }

        _uiState.update {
            it.copy(devices = devices, isSearching = false)
        }
        maybeAutoUpdateAll()
    }

    /**
     * Manual retry from the controller card. It ignores a previous "stay on
     * this version" decision and the automatic retry delay.
     */
    fun requestUpdate(deviceId: String) {
        maybeAutoUpdate(deviceId, manual = true)
    }

    private fun maybeAutoUpdateAll() {
        _uiState.value.devices
            .filter { it.isPairedToThisEmitter && it.isOnline && it.appVersion != null }
            .forEach { maybeAutoUpdate(it.deviceId, manual = false) }
    }

    private fun maybeAutoUpdate(deviceId: String, manual: Boolean) {
        val device = _uiState.value.devices.firstOrNull { it.deviceId == deviceId } ?: return
        if (!device.isPairedToThisEmitter) return
        val screenVersion = device.appVersion ?: return
        val plan = AppVersionPolicy.plan(APP_VERSION, screenVersion)
        if (plan.urgency == UpdateUrgency.NONE) {
            clearFinishedUpdate(deviceId)
            return
        }

        if (!manual &&
            plan.urgency == UpdateUrgency.OPTIONAL &&
            skippedUpdateVersions[deviceId] == plan.targetVersion
        ) {
            sentUpdateVersions.remove(deviceId)
            updateSentAt.remove(deviceId)
            setDeviceUpdate(
                deviceId,
                DeviceUpdateUi(
                    status = DeviceUpdateStatus.SKIPPED,
                    version = plan.targetVersion,
                    message = Strings.updateSkipped(screenVersion)
                )
            )
            return
        }

        if (!manual && sentUpdateVersions[deviceId] == plan.targetVersion) {
            // Give the screen time to decide and restart. If it is still on an
            // old version after this window, the install probably failed, so
            // allow the next automatic attempt.
            val sentAt = updateSentAt[deviceId]
            if (sentAt == null || sentAt.elapsedNow() < 15.minutes) return
            sentUpdateVersions.remove(deviceId)
            updateSentAt.remove(deviceId)
        }
        if (updateJobs[deviceId]?.isActive == true) return
        val retry = updateRetryAt[deviceId]
        if (!manual && retry != null && retry.elapsedNow() < 60.seconds) return

        val provider = updatePackageProvider
        if (provider == null || !provider.isAvailable()) {
            setDeviceUpdate(
                deviceId,
                DeviceUpdateUi(
                    status = DeviceUpdateStatus.UNAVAILABLE,
                    version = plan.targetVersion,
                    forced = plan.urgency == UpdateUrgency.FORCED,
                    message = Strings.UPDATE_SEND_UNAVAILABLE
                )
            )
            if (unavailableUpdateNotice.add(deviceId)) {
                logger.warn(
                    "Screen '${device.deviceName}' is on $screenVersion but this controller " +
                        "has no packaged update available"
                )
            }
            return
        }
        unavailableUpdateNotice.remove(deviceId)

        val stored = storedDevices.firstOrNull { it.deviceId == deviceId } ?: return
        val target = targetFor(stored) ?: return
        sentUpdateVersions[deviceId] = plan.targetVersion

        val updateJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                updateUploadMutex.withLock {
                    setDeviceUpdate(
                        deviceId,
                        DeviceUpdateUi(
                            status = DeviceUpdateStatus.PREPARING,
                            version = plan.targetVersion,
                            forced = plan.urgency == UpdateUrgency.FORCED
                        )
                    )
                    val update = withContext(backgroundDispatcher) {
                        provider.getPackage()
                    }.getOrElse { error ->
                        throw error
                    }

                    setDeviceUpdate(
                        deviceId,
                        DeviceUpdateUi(
                            status = DeviceUpdateStatus.UPLOADING,
                            version = update.version,
                            forced = plan.urgency == UpdateUrgency.FORCED
                        )
                    )
                    val result = withContext(backgroundDispatcher) {
                        client.uploadUpdate(
                            host = target.first,
                            port = target.second,
                            emitterId = emitterId,
                            token = stored.token,
                            version = update.version,
                            sha256 = update.sha256,
                            bytes = update.bytes
                        )
                    }
                    result.fold(
                        onSuccess = { response ->
                            sentUpdateVersions[deviceId] = response.version
                            updateSentAt[deviceId] = TimeSource.Monotonic.markNow()
                            setDeviceUpdate(
                                deviceId,
                                DeviceUpdateUi(
                                    status = DeviceUpdateStatus.AWAITING_CONFIRMATION,
                                    version = response.version,
                                    forced = response.forced
                                )
                            )
                            logger.info(
                                "Update ${response.version} sent to '${stored.deviceName}' " +
                                    "(forced=${response.forced})"
                            )
                        },
                        onFailure = { error ->
                            sentUpdateVersions.remove(deviceId)
                            updateSentAt.remove(deviceId)
                            updateRetryAt[deviceId] = TimeSource.Monotonic.markNow()
                            setDeviceUpdate(
                                deviceId,
                                DeviceUpdateUi(
                                    status = DeviceUpdateStatus.FAILED,
                                    version = plan.targetVersion,
                                    forced = plan.urgency == UpdateUrgency.FORCED,
                                    message = updateErrorMessage(error)
                                )
                            )
                            logger.warn(
                                "Could not send update ${plan.targetVersion} to " +
                                    "'${stored.deviceName}': ${error.message}"
                            )
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                sentUpdateVersions.remove(deviceId)
                updateSentAt.remove(deviceId)
                throw cancelled
            } catch (error: Exception) {
                sentUpdateVersions.remove(deviceId)
                updateSentAt.remove(deviceId)
                updateRetryAt[deviceId] = TimeSource.Monotonic.markNow()
                setDeviceUpdate(
                    deviceId,
                    DeviceUpdateUi(
                        status = DeviceUpdateStatus.FAILED,
                        version = plan.targetVersion,
                        forced = plan.urgency == UpdateUrgency.FORCED,
                        message = updateErrorMessage(error)
                    )
                )
                logger.warn(
                    "Update ${plan.targetVersion} for '${stored.deviceName}' failed: " +
                        "${error.message}"
                )
            } finally {
                updateJobs.remove(deviceId)
            }
        }
        updateJobs[deviceId] = updateJob
        updateJob.start()
    }

    private fun setDeviceUpdate(deviceId: String, update: DeviceUpdateUi) {
        _uiState.update {
            it.copy(updateByDevice = it.updateByDevice + (deviceId to update))
        }
    }

    private fun clearFinishedUpdate(deviceId: String) {
        sentUpdateVersions.remove(deviceId)
        updateSentAt.remove(deviceId)
        updateRetryAt.remove(deviceId)
        unavailableUpdateNotice.remove(deviceId)
        skippedUpdateVersions[deviceId]?.let { skipped ->
            if (AppVersionPolicy.plan(APP_VERSION, skipped).urgency == UpdateUrgency.NONE) {
                skippedUpdateVersions.remove(deviceId)
            }
        }
        _uiState.update { state ->
            val current = state.updateByDevice[deviceId] ?: return@update state
            if (current.status == DeviceUpdateStatus.PREPARING ||
                current.status == DeviceUpdateStatus.UPLOADING
            ) {
                return@update state
            }
            state.copy(updateByDevice = state.updateByDevice - deviceId)
        }
    }

    private fun clearUpdateState(deviceId: String) {
        updateJobs.remove(deviceId)?.cancel()
        sentUpdateVersions.remove(deviceId)
        updateSentAt.remove(deviceId)
        updateRetryAt.remove(deviceId)
        skippedUpdateVersions.remove(deviceId)
        deviceVersions.remove(deviceId)
        unavailableUpdateNotice.remove(deviceId)
        _uiState.update { it.copy(updateByDevice = it.updateByDevice - deviceId) }
    }

    private fun mapPairError(error: Throwable): String {
        if (error !is PairRejectedException) return Strings.PAIR_ERROR_NETWORK
        return when (error.error) {
            PairError.INVALID_CODE -> buildString {
                append(Strings.PAIR_ERROR_INVALID_CODE)
                error.remainingAttempts?.let { remaining ->
                    append(' ')
                    append(Strings.PAIR_ERROR_REMAINING_ATTEMPTS)
                    append(' ')
                    append(remaining)
                }
            }

            PairError.LOCKED_OUT -> Strings.PAIR_ERROR_LOCKED
            PairError.ALREADY_PAIRED -> Strings.PAIR_ERROR_ALREADY_PAIRED
        }
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
