package app.kiosksync.feature.emitter.data

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.DEFAULT_PORT
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.PLAYBACK_SYNC_DELAY_MS
import app.kiosksync.core.STATUS_FAILURE_THRESHOLD
import app.kiosksync.core.STATUS_POLL_INTERVAL_MS
import app.kiosksync.feature.emitter.domain.AddReceiverByAddressUseCase
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.emitter.domain.EmitterDeliveryResult
import app.kiosksync.feature.emitter.domain.EmitterDeliveryProgress
import app.kiosksync.feature.emitter.domain.EmitterDeliverySummary
import app.kiosksync.feature.emitter.domain.EmitterLocalMediaMissingException
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.emitter.domain.EmitterServiceDevice
import app.kiosksync.feature.emitter.domain.EmitterServiceState
import app.kiosksync.feature.emitter.domain.EmitterUpdateState
import app.kiosksync.feature.emitter.domain.PairReceiverRequest
import app.kiosksync.feature.emitter.domain.PairReceiverResult
import app.kiosksync.feature.emitter.domain.ReceiverAddressDetails
import app.kiosksync.feature.emitter.domain.UnpairReceiverResult
import app.kiosksync.feature.emitter.domain.PairReceiverUseCase
import app.kiosksync.feature.emitter.domain.UnpairReceiverUseCase
import app.kiosksync.feature.emitter.domain.PlaybackSyncResult
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.sync.domain.EmitterSyncRepository
import app.kiosksync.feature.sync.domain.LocalMediaMissingException
import app.kiosksync.feature.update.domain.UpdatePackageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Coordinates Controller discovery, paired-screen storage and remote operations. */
internal class EmitterServiceImpl(
    private val discovery: DeviceDiscovery,
    private val client: EmitterClient,
    private val screensRepository: EmitterScreensRepository,
    private val kioskLockRepository: KioskLockRepository,
    private val syncRepository: EmitterSyncRepository,
    private val logger: AppLogger,
    private val serviceScope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val updatePackageProvider: UpdatePackageProvider,
) : EmitterService {
    private val runtimeScope = CoroutineScope(SupervisorJob() + backgroundDispatcher)
    private val stateLock = Any()
    private val _state = MutableStateFlow(EmitterServiceState())
    override val state: StateFlow<EmitterServiceState> = _state.asStateFlow()
    private val updateRunner = EmitterUpdateRunner(
        client = client,
        updatePackageProvider = updatePackageProvider,
        logger = logger,
        scope = runtimeScope,
        backgroundDispatcher = backgroundDispatcher,
        currentDevice = { deviceId -> _state.value.devices.firstOrNull { it.deviceId == deviceId } },
        currentTarget = ::updateTarget,
        emitterId = { synchronized(stateLock) { runtimeEmitterId } },
        setUpdate = ::setUpdate,
    )

    private var discovered: List<DiscoveredDevice> = emptyList()
    private val manualDevices = mutableMapOf<String, DiscoveredDevice>()
    private var storedDevices: List<EmitterPairedDevice> = emptyList()
    private val onlineStatus = mutableMapOf<String, Boolean>()
    private val failedChecks = mutableMapOf<String, Int>()
    private val justPairedIds = mutableSetOf<String>()
    private var runtimeJob: Job? = null
    private var started = false
    private var runtimeEmitterId = ""
    private var runtimeEmitterName = ""

    private val deviceVersions = mutableMapOf<String, String>()
    override fun start(emitterId: String, emitterName: String) {
        synchronized(stateLock) {
            if (started) return
            started = true
            runtimeEmitterId = emitterId
            runtimeEmitterName = emitterName
        }
        logger.info(
            "Automatic updates: " + if (updatePackageProvider.isAvailable()) {
                "enabled (package $APP_VERSION available)"
            } else {
                "unavailable (controller is not running from a packaged install)"
            }
        )
        runtimeJob = runtimeScope.launch {
            try {
                val paired = withContext(backgroundDispatcher) { screensRepository.loadPairedScreens() }
                synchronized(stateLock) { storedDevices = paired }
                logger.info("Emitter '$runtimeEmitterName' ($runtimeEmitterId) started with ${paired.size} paired screens")
                loadExistingLockAndPush(runtimeEmitterId)
                rebuild()
                coroutineScope {
                    launch {
                        discovery.discoveredDevices.collect { devices -> updateDiscoveredDevices(devices) }
                    }
                    launch { pollStatuses() }
                    kotlinx.coroutines.awaitCancellation()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not start Controller data service", error)
            }
        }
        serviceScope.launch { discovery.startDiscovery() }
    }

    override fun stop() {
        synchronized(stateLock) {
            if (!started) return
            started = false
            runtimeEmitterId = ""
            runtimeEmitterName = ""
        }
        runtimeJob?.cancel()
        runtimeJob = null
        updateRunner.stop()
        serviceScope.launch { discovery.stopDiscovery() }
        logger.info("Emitter discovery stopped")
    }

    override fun refresh() {
        logger.info("Manual screen scan requested")
        serviceScope.launch { discovery.refresh() }
    }

    override suspend fun addReceiver(address: String): Result<EmitterServiceDevice> = try {
        val details = withContext(backgroundDispatcher) { AddReceiverByAddressUseCase(client)(address) }
        synchronized(stateLock) { manualDevices[details.device.deviceId] = details.device }
        rebuild()
        Result.success(details.toServiceDevice())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        logger.warn("Manual screen address was rejected: ${error.message}")
        Result.failure(error)
    }

    override suspend fun pair(
        deviceId: String,
        code: String,
        emitterId: String,
        emitterName: String,
    ): Result<PairReceiverResult> = try {
        val device = _state.value.devices.firstOrNull { it.deviceId == deviceId }
                ?: throw IllegalStateException("Screen '$deviceId' is no longer available")
        val result = withContext(backgroundDispatcher) {
            PairReceiverUseCase(client, screensRepository)(
                PairReceiverRequest(
                    deviceId = device.deviceId,
                    deviceName = device.deviceName,
                    host = device.host,
                    port = device.port,
                    emitterId = emitterId,
                    emitterName = emitterName,
                    code = code,
                ),
            )
        }
        synchronized(stateLock) {
            storedDevices = result.pairedDevices
            failedChecks[deviceId] = 0
            onlineStatus[deviceId] = true
            justPairedIds += deviceId
            device.appVersion?.let { deviceVersions[deviceId] = it }
        }
        updateRunner.forget(deviceId)
        rebuild()
        Result.success(result)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        logger.warn("Pairing failed for '$deviceId': ${error.message}")
        Result.failure(error)
    }

    override suspend fun unpair(deviceId: String, emitterId: String): Result<UnpairReceiverResult> = try {
        val stored = synchronized(stateLock) { storedDevices.firstOrNull { it.deviceId == deviceId } }
            ?: return Result.success(UnpairReceiverResult(pairedDevicesSnapshot(), remoteUnpairSucceeded = false))
        val target = targetFor(stored)
        val result = withContext(backgroundDispatcher) {
            UnpairReceiverUseCase(client, screensRepository)(
                stored,
                target?.first,
                target?.second,
                emitterId,
            )
        }
        synchronized(stateLock) {
            storedDevices = result.pairedDevices
            onlineStatus.remove(deviceId)
            failedChecks.remove(deviceId)
            deviceVersions.remove(deviceId)
        }
        updateRunner.forget(deviceId)
        rebuild()
        Result.success(result)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        logger.error("Could not remove pairing for '$deviceId'", error)
        Result.failure(error)
    }

    override suspend fun loadKioskLock(): Result<KioskLockConfig?> = try {
        val config = withContext(backgroundDispatcher) { kioskLockRepository.load() }
        Result.success(config?.takeIf { it.enabled && it.passwordHash != null })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        logger.error("Could not load kiosk security configuration", error)
        Result.failure(error)
    }

    override fun sendLockToDevice(deviceId: String, emitterId: String, lock: KioskLockConfig) {
        val stored = synchronized(stateLock) { storedDevices.firstOrNull { it.deviceId == deviceId } } ?: return
        val target = targetFor(stored) ?: return
        runtimeScope.launch {
            client.putLock(target.first, target.second, emitterId, stored.token, lock)
                .onSuccess { logger.info("Kiosk lock sent to '${stored.deviceName}'") }
                .onFailure { logger.warn("Could not send kiosk lock to '${stored.deviceName}': ${it.message}") }
        }
    }

    override fun sendLockToAllPaired(emitterId: String, lock: KioskLockConfig) {
        pairedDevicesSnapshot().forEach { sendLockToDevice(it.deviceId, emitterId, lock) }
    }

    override suspend fun deliver(
        emitterId: String,
        deviceIds: List<String>,
        playlist: Playlist,
        lock: KioskLockConfig,
        onProgress: (deviceId: String, progress: EmitterDeliveryProgress) -> Unit,
        onApplying: (deviceId: String) -> Unit,
    ): List<EmitterDeliveryResult> {
        val paired = pairedDevicesSnapshot().associateBy(EmitterPairedDevice::deviceId)
        return deviceIds.map { deviceId ->
            val stored = paired[deviceId]
            val target = stored?.let(::targetFor)
            val result = if (stored == null || target == null) {
                Result.failure(IllegalStateException("No current address for paired screen '$deviceId'"))
            } else {
                withContext(backgroundDispatcher) {
                    syncRepository.sync(
                        device = stored,
                        host = target.first,
                        port = target.second,
                        emitterId = emitterId,
                        playlist = playlist,
                        lock = lock,
                        onUploadProgress = {
                            onProgress(deviceId, EmitterDeliveryProgress(it.uploaded, it.total))
                        },
                        onApplying = { onApplying(deviceId) },
                    )
                }.map { summary ->
                    EmitterDeliverySummary(
                        appliedVersion = summary.appliedVersion,
                        uploadedCount = summary.uploadedCount,
                    )
                }.recoverCatching { error ->
                    if (error is LocalMediaMissingException) {
                        throw EmitterLocalMediaMissingException(error)
                    }
                    throw error
                }
            }
            result.onFailure { logger.warn("Could not send playlist to '$deviceId': ${it.message}") }
            EmitterDeliveryResult(deviceId, result)
        }
    }

    override suspend fun synchronizePlayback(emitterId: String): Result<PlaybackSyncResult> = try {
        val paired = pairedDevicesSnapshot()
        val targets = paired.mapNotNull { device -> targetFor(device)?.let { device to it } }
        val unresolved = paired.size - targets.size
        val outcomes = coroutineScope {
            targets.map { (device, target) ->
                async(backgroundDispatcher) {
                    client.synchronizePlayback(
                        host = target.first,
                        port = target.second,
                        emitterId = emitterId,
                        token = device.token,
                        delayMs = PLAYBACK_SYNC_DELAY_MS,
                    ).onFailure {
                        logger.warn("Could not synchronise playback on '${device.deviceName}': ${it.message}")
                    }.isSuccess
                }
            }.awaitAll()
        }
        val succeeded = outcomes.count { it }
        Result.success(PlaybackSyncResult(succeeded, outcomes.size - succeeded + unresolved))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        logger.warn("Could not synchronise playback: ${error.message}")
        Result.failure(error)
    }

    override fun requestUpdate(deviceId: String, emitterId: String) {
        synchronized(stateLock) { runtimeEmitterId = emitterId }
        updateRunner.request(deviceId, manual = true)
    }

    private suspend fun loadExistingLockAndPush(emitterId: String) {
        val lock = try {
            withContext(backgroundDispatcher) { kioskLockRepository.load() }
                ?.takeIf { it.enabled && it.passwordHash != null }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Could not load Controller kiosk lock", error)
            null
        }
        if (lock != null && pairedDevicesSnapshot().isNotEmpty()) sendLockToAllPaired(emitterId, lock)
    }

    private suspend fun pollStatuses() {
        while (runtimeScope.isActive) {
            for (device in pairedDevicesSnapshot()) {
                val target = targetFor(device)
                if (target == null) {
                    registerFailure(device.deviceId)
                    continue
                }
                val result = withContext(backgroundDispatcher) {
                    client.getStatus(target.first, target.second, runtimeEmitterId, device.token)
                }
                result.onSuccess { status ->
                    synchronized(stateLock) {
                        failedChecks[device.deviceId] = 0
                        onlineStatus[device.deviceId] = true
                        deviceVersions[device.deviceId] = status.appVersion
                    }
                    updateRunner.recordSkippedVersion(device.deviceId, status.skippedUpdateVersion)
                    updateRunner.request(device.deviceId, manual = false)
                }.onFailure { registerFailure(device.deviceId) }
            }
            rebuild()
            delay(STATUS_POLL_INTERVAL_MS)
        }
    }

    private fun registerFailure(deviceId: String) {
        synchronized(stateLock) {
            val failures = (failedChecks[deviceId] ?: 0) + 1
            failedChecks[deviceId] = failures
            if (failures >= STATUS_FAILURE_THRESHOLD) onlineStatus[deviceId] = false
        }
    }

    private suspend fun updateDiscoveredDevices(devices: List<DiscoveredDevice>) {
        val staleIds = synchronized(stateLock) {
            discovered = devices
            val storedIds = storedDevices.mapTo(mutableSetOf(), EmitterPairedDevice::deviceId)
            devices.asSequence()
                .filter { it.deviceId in storedIds }
                .filter { !it.paired && it.deviceId !in justPairedIds }
                .map(DiscoveredDevice::deviceId)
                .toList()
        }
        if (staleIds.isNotEmpty()) {
            try {
                val paired = withContext(backgroundDispatcher) {
                    for (deviceId in staleIds) {
                        screensRepository.removePairedScreen(deviceId)
                    }
                    screensRepository.loadPairedScreens()
                }
                synchronized(stateLock) {
                    storedDevices = paired
                    staleIds.forEach { id ->
                        onlineStatus.remove(id)
                        failedChecks.remove(id)
                    }
                }
                staleIds.forEach { id ->
                    synchronized(stateLock) { deviceVersions.remove(id) }
                    updateRunner.forget(id)
                }
                logger.info("Forgot ${staleIds.size} screen(s) that are no longer paired: $staleIds")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not remove screens that are no longer paired", error)
            }
        }
        synchronized(stateLock) {
            devices.asSequence().filter(DiscoveredDevice::paired).forEach { justPairedIds.remove(it.deviceId) }
        }
        rebuild()
    }

    private fun rebuild() {
        val devices = synchronized(stateLock) {
            val discoveredById = allDiscoveredLocked().associateBy(DiscoveredDevice::deviceId)
            val storedById = storedDevices.associateBy(EmitterPairedDevice::deviceId)
            (storedById.keys + discoveredById.keys).mapNotNull { id ->
                val stored = storedById[id]
                val live = discoveredById[id]
                when {
                    stored != null -> EmitterServiceDevice(
                        deviceId = id,
                        deviceName = live?.deviceName ?: stored.deviceName,
                        host = live?.host ?: stored.host.orEmpty(),
                        port = live?.port ?: stored.port ?: DEFAULT_PORT,
                        pairedToThisEmitter = true,
                        pairedToAnotherEmitter = false,
                        isOnline = onlineStatus[id] ?: false,
                        appVersion = deviceVersions[id] ?: live?.appVersion,
                    )

                    live != null -> EmitterServiceDevice(
                        deviceId = id,
                        deviceName = live.deviceName,
                        host = live.host,
                        port = live.port,
                        pairedToThisEmitter = false,
                        pairedToAnotherEmitter = live.paired,
                        isOnline = false,
                        appVersion = live.appVersion ?: deviceVersions[id],
                    )

                    else -> null
                }
            }.sortedBy { it.deviceName.lowercase() }
        }
        _state.update { it.copy(devices = devices, isSearching = false) }
        updateRunner.requestOnlineDevices(devices)
    }

    private fun allDiscoveredLocked(): List<DiscoveredDevice> =
        discovered + manualDevices.values.filter { manual -> discovered.none { it.deviceId == manual.deviceId } }

    private fun targetFor(device: EmitterPairedDevice): Pair<String, Int>? {
        val current = _state.value.devices.firstOrNull { it.deviceId == device.deviceId }
        val host = current?.host ?: device.host
        val port = current?.port ?: device.port
        return if (!host.isNullOrBlank() && port != null) host to port else null
    }

    private fun updateTarget(deviceId: String): EmitterUpdateTarget? {
        val stored = pairedDevicesSnapshot().firstOrNull { it.deviceId == deviceId } ?: return null
        val target = targetFor(stored) ?: return null
        return EmitterUpdateTarget(stored, target.first, target.second)
    }

    private fun pairedDevicesSnapshot(): List<EmitterPairedDevice> =
        synchronized(stateLock) { storedDevices.toList() }

    private fun ReceiverAddressDetails.toServiceDevice(): EmitterServiceDevice = EmitterServiceDevice(
        deviceId = device.deviceId,
        deviceName = device.deviceName,
        host = device.host,
        port = device.port,
        pairedToThisEmitter = false,
        pairedToAnotherEmitter = device.paired,
        isOnline = false,
        appVersion = device.appVersion,
    )

    private fun setUpdate(deviceId: String, update: EmitterUpdateState?) {
        _state.update { current ->
            val updates = if (update == null) current.updateByDevice - deviceId
            else current.updateByDevice + (deviceId to update)
            current.copy(updateByDevice = updates)
        }
    }
}
