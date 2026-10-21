package app.kiosksync.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.kiosksync.AppContainer
import app.kiosksync.core.Strings
import app.kiosksync.feature.pairing.PairingCodeScreen
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.CarouselScreen
import app.kiosksync.feature.playback.PlaybackStateHolder
import app.kiosksync.feature.role.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ReceiverRuntimeUi(
    val port: Int,
    val isReady: Boolean = false,
    val error: String? = null
)

@Composable
fun ReceiverHomeScreen(
    config: AppConfig,
    container: AppContainer,
    pairingManager: PairingManager,
    playbackStateHolder: PlaybackStateHolder,
    onExitRequested: () -> Unit,
    onChangeRoleRequested: () -> Unit
) {
    val pairingState by pairingManager.uiState.collectAsState()
    val lockState by container.kioskLockManager.uiState.collectAsState()
    val runtimeState = remember(config.deviceId, config.port) {
        MutableStateFlow(ReceiverRuntimeUi(port = config.port))
    }
    val runtime by runtimeState.collectAsState()

    // Starting Ktor and mDNS can block for a while. Keep both operations on the
    // serial service worker so the UI appears instantly and start/stop never race.
    DisposableEffect(config.deviceId, config.port) {
        container.serviceScope.launch {
            try {
                val port = container.receiverServer.start(
                    port = config.port,
                    deviceId = config.deviceId,
                    deviceName = container.deviceName
                )
                runtimeState.value = ReceiverRuntimeUi(port = port, isReady = true)
            } catch (error: Exception) {
                container.logger.error("Could not start the receiver server", error)
                runtimeState.value = ReceiverRuntimeUi(
                    port = config.port,
                    error = Strings.SERVER_START_ERROR
                )
            }
        }

        onDispose {
            container.serviceScope.launch {
                runCatching { container.discovery.unregisterService() }
                runCatching { container.receiverServer.stop() }
            }
        }
    }

    LaunchedEffect(pairingState.isPaired, runtime.port, runtime.isReady) {
        if (!runtime.isReady) return@LaunchedEffect
        withContext(container.serviceDispatcher) {
            container.discovery.registerService(
                deviceName = container.deviceName,
                port = runtime.port,
                deviceId = config.deviceId,
                isPaired = pairingState.isPaired
            )
        }
    }

    when {
        !pairingState.isPaired -> PairingCodeScreen(
            pairingManager = pairingManager,
            serviceError = runtime.error,
            onExitRequested = onExitRequested,
            onChangeRoleRequested = onChangeRoleRequested,
            showAdminActions = !lockState.isLocked
        )

        // A paired screen stays inert until it receives its blocking password
        // from the controller; only then does the carousel start.
        !lockState.hasPassword -> WaitingForPasswordScreen()

        else -> CarouselScreen(
            stateHolder = playbackStateHolder
        )
    }
}
