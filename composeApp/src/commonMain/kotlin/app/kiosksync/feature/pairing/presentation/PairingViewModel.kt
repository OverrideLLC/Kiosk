package app.kiosksync.feature.pairing.presentation

import app.kiosksync.core.AppLogger
import app.kiosksync.core.Strings
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.receiver.domain.ReceiverServer
import app.kiosksync.feature.role.domain.AppConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Coordinates the Receiver pairing lifecycle and exposes a screen-ready MVI state. */
class PairingViewModel(
    private val pairingManager: PairingManager,
    private val receiverServer: ReceiverServer,
    private val discovery: DeviceDiscovery,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val serviceDispatcher: CoroutineDispatcher,
    private val deviceName: String,
    localIpAddress: String?,
) {
    private val _state = MutableStateFlow(PairingUiState(receiverIp = localIpAddress))
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    private val effectChannel = Channel<PairingEffect>(Channel.BUFFERED)
    val effects: Flow<PairingEffect> = effectChannel.receiveAsFlow()

    private val activeConfig = MutableStateFlow<AppConfig?>(null)
    private var startJob: Job? = null
    private var tickerJob: Job? = null

    init {
        _state.update { it.copy(pairing = pairingManager.state.value) }
        scope.launch {
            pairingManager.state.collect { pairing ->
                _state.update { it.copy(pairing = pairing) }
                updateDiscovery(pairing.isPaired)
            }
        }
    }

    fun onAction(action: PairingAction) {
        when (action) {
            is PairingAction.StartReceiver -> startReceiver(action.config)
            PairingAction.StopReceiver -> stopReceiver()
            is PairingAction.AdminActionsChanged ->
                _state.update { it.copy(showAdminActions = action.visible) }
            PairingAction.Unpair -> unpair()
            PairingAction.OpenGuide -> effectChannel.trySend(PairingEffect.OpenGuide)
            PairingAction.ExitRequested -> effectChannel.trySend(PairingEffect.ExitRequested)
            PairingAction.ChangeRoleRequested -> effectChannel.trySend(PairingEffect.ChangeRoleRequested)
        }
    }

    private fun unpair() {
        scope.launch {
            try {
                pairingManager.unpair()
                effectChannel.send(PairingEffect.Unpaired)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not unlink the Receiver", error)
            }
        }
    }

    private fun startReceiver(config: AppConfig) {
        if (
            activeConfig.value == config &&
            (startJob?.isActive == true || _state.value.receiverPort != null)
        ) return
        activeConfig.value = config
        startJob?.cancel()
        tickerJob?.cancel()
        _state.update { it.copy(receiverPort = null, serviceError = null) }
        startJob = scope.launch {
            try {
                pairingManager.initialize()
                tickerJob = launch {
                    while (isActive && !pairingManager.state.value.isPaired) {
                        pairingManager.tick()
                        delay(1_000)
                    }
                }
                val port = withContext(serviceDispatcher) {
                    receiverServer.start(config.port, config.deviceId, deviceName)
                }
                _state.update { it.copy(receiverPort = port, serviceError = null) }
                updateDiscovery(pairingManager.state.value.isPaired)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not start the receiver server", error)
                _state.update { it.copy(serviceError = Strings.SERVER_START_ERROR) }
            }
        }
    }

    private fun stopReceiver() {
        activeConfig.value = null
        startJob?.cancel()
        startJob = null
        tickerJob?.cancel()
        tickerJob = null
        _state.update { it.copy(receiverPort = null, serviceError = null) }
        scope.launch {
            withContext(serviceDispatcher) {
                runCatching { discovery.unregisterService() }
                    .onFailure { logger.warn("Could not unregister Receiver service", it) }
                runCatching { receiverServer.stop() }
                    .onFailure { logger.warn("Could not stop Receiver server", it) }
            }
        }
    }

    private suspend fun updateDiscovery(isPaired: Boolean) {
        val config = activeConfig.value ?: return
        val port = _state.value.receiverPort ?: return
        try {
            withContext(serviceDispatcher) {
                if (activeConfig.value != config || _state.value.receiverPort != port) return@withContext
                discovery.registerService(
                    deviceName = deviceName,
                    port = port,
                    deviceId = config.deviceId,
                    isPaired = isPaired,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.warn("Could not advertise the Receiver over mDNS", error)
        }
    }
}
