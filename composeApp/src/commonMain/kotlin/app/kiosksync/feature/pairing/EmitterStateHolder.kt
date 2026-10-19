package app.kiosksync.feature.pairing

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DEFAULT_PORT
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.PairError
import app.kiosksync.core.PairRejectedException
import app.kiosksync.core.STATUS_FAILURE_THRESHOLD
import app.kiosksync.core.STATUS_POLL_INTERVAL_MS
import app.kiosksync.core.Strings
import app.kiosksync.platform.DeviceDiscovery
import app.kiosksync.platform.EmitterClient
import app.kiosksync.platform.LocalStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EmitterDeviceUi(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val isPairedToThisEmitter: Boolean,
    val isPairedToAnotherEmitter: Boolean,
    val isOnline: Boolean,
    val isSelected: Boolean
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
    val statusMessage: String? = null
)

class EmitterStateHolder(
    private val discovery: DeviceDiscovery,
    private val client: EmitterClient,
    private val storage: LocalStorage,
    private val logger: AppLogger,
    private val emitterId: String,
    private val emitterName: String,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(EmitterUiState())
    val uiState: StateFlow<EmitterUiState> = _uiState.asStateFlow()

    private var discovered: List<DiscoveredDevice> = emptyList()
    private val manualDevices = mutableMapOf<String, DiscoveredDevice>()
    private var storedDevices: List<EmitterPairedDevice> = emptyList()
    private val onlineStatus = mutableMapOf<String, Boolean>()
    private val failedChecks = mutableMapOf<String, Int>()
    private var selectedIds = emptySet<String>()
    private val jobs = mutableListOf<Job>()

    fun start() {
        if (jobs.isNotEmpty()) return
        storedDevices = storage.loadEmitterPairedDevices()
        logger.info("Emitter '$emitterName' ($emitterId) started with ${storedDevices.size} paired screens")

        jobs += scope.launch {
            discovery.discoveredDevices.collect { devices ->
                discovered = devices
                rebuild()
            }
        }
        jobs += scope.launch { pollStatuses() }

        discovery.startDiscovery()
        rebuild()
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        discovery.stopDiscovery()
        logger.info("Emitter discovery stopped")
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
            client.pair(
                host = device.host,
                port = device.port,
                emitterId = emitterId,
                emitterName = emitterName,
                code = state.pairingCode
            ).fold(
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
                    logger.info("Screen '${device.deviceName}' paired successfully")
                    _uiState.update {
                        it.copy(
                            pairingDevice = null,
                            pairingCode = "",
                            pairingError = null,
                            isPairing = false,
                            statusMessage = Strings.PAIR_SUCCESS
                        )
                    }
                    rebuild()
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
            if (target == null) {
                _uiState.update { it.copy(statusMessage = Strings.UNPAIR_ERROR) }
                return@launch
            }
            client.unpair(target.first, target.second, emitterId, storedDevice.token).fold(
                onSuccess = {
                    storage.removeEmitterPairedDevice(device.deviceId)
                    storedDevices = storage.loadEmitterPairedDevices()
                    onlineStatus.remove(device.deviceId)
                    failedChecks.remove(device.deviceId)
                    selectedIds = selectedIds - device.deviceId
                    logger.info("Screen '${device.deviceName}' unpaired")
                    _uiState.update { it.copy(statusMessage = Strings.UNPAIR_SUCCESS) }
                    rebuild()
                },
                onFailure = { error ->
                    logger.warn("Could not unpair '${device.deviceName}': ${error.message}")
                    _uiState.update { it.copy(statusMessage = Strings.UNPAIR_ERROR) }
                }
            )
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
            client.getInfo(host, port).fold(
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
                        paired = info.paired
                    )
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
        _uiState.update { it.copy(statusMessage = null) }
    }

    private suspend fun pollStatuses() {
        while (scope.isActive) {
            for (device in storedDevices) {
                val target = targetFor(device)
                if (target == null) {
                    registerFailure(device.deviceId)
                    continue
                }
                client.getStatus(target.first, target.second, emitterId, device.token)
                    .onSuccess {
                        failedChecks[device.deviceId] = 0
                        onlineStatus[device.deviceId] = true
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

    private fun allDiscovered(): List<DiscoveredDevice> =
        discovered + manualDevices.values.filter { manual -> discovered.none { it.deviceId == manual.deviceId } }

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
                    isSelected = id in selectedIds
                )

                live != null -> EmitterDeviceUi(
                    deviceId = id,
                    deviceName = live.deviceName,
                    host = live.host,
                    port = live.port,
                    isPairedToThisEmitter = false,
                    isPairedToAnotherEmitter = live.paired,
                    isOnline = false,
                    isSelected = false
                )

                else -> null
            }
        }.sortedBy { it.deviceName.lowercase() }

        _uiState.update {
            it.copy(devices = devices, isSearching = false)
        }
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
