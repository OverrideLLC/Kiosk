package app.kiosksync.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.kiosksync.AppContainer
import app.kiosksync.core.Strings
import app.kiosksync.feature.pairing.PairingCodeScreen
import app.kiosksync.feature.pairing.PairingManager
import app.kiosksync.feature.playback.CarouselScreen
import app.kiosksync.feature.playback.PlaybackStateHolder
import app.kiosksync.feature.role.AppConfig

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
    var boundPort by remember(config.deviceId) { mutableStateOf(config.port) }
    var serviceError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(config.deviceId, config.port) {
        val port = try {
            container.receiverServer.start(config.port, config.deviceId, container.deviceName)
        } catch (error: Exception) {
            container.logger.error("Could not start the receiver server", error)
            serviceError = Strings.SERVER_START_ERROR
            config.port
        }
        boundPort = port
        container.discovery.registerService(
            deviceName = container.deviceName,
            port = port,
            deviceId = config.deviceId,
            isPaired = pairingManager.uiState.value.isPaired
        )
        onDispose {
            container.discovery.unregisterService()
            container.receiverServer.stop()
        }
    }

    LaunchedEffect(pairingState.isPaired, boundPort) {
        container.discovery.registerService(
            deviceName = container.deviceName,
            port = boundPort,
            deviceId = config.deviceId,
            isPaired = pairingState.isPaired
        )
    }

    if (pairingState.isPaired) {
        CarouselScreen(
            stateHolder = playbackStateHolder,
            onExitRequested = onExitRequested,
            onChangeRoleRequested = onChangeRoleRequested
        )
    } else {
        PairingCodeScreen(
            pairingManager = pairingManager,
            serviceError = serviceError,
            onExitRequested = onExitRequested,
            onChangeRoleRequested = onChangeRoleRequested
        )
    }
}
