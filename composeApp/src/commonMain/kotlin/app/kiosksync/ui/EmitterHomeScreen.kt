package app.kiosksync.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.kiosksync.AppContainer
import app.kiosksync.feature.pairing.EmitterScreen
import app.kiosksync.feature.pairing.EmitterStateHolder
import app.kiosksync.feature.playlist.PlaylistEditorStateHolder
import app.kiosksync.feature.role.AppConfig

@Composable
fun EmitterHomeScreen(
    config: AppConfig,
    container: AppContainer,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val stateHolder = remember(config.deviceId) {
        EmitterStateHolder(
            discovery = container.discovery,
            client = container.emitterClient,
            storage = container.localStorage,
            logger = container.logger,
            emitterId = config.deviceId,
            emitterName = container.deviceName,
            scope = scope,
            serviceScope = container.serviceScope,
            kioskLockManager = container.kioskLockManager,
            projectionController = container.projectionController,
            passwordHasher = container.passwordHasher,
            backgroundDispatcher = container.backgroundDispatcher
        )
    }

    val playlistStateHolder = remember(config.deviceId) {
        PlaylistEditorStateHolder(
            storage = container.localStorage,
            logger = container.logger,
            scope = scope,
            pickImageFiles = container.pickImageFiles,
            newPlaylistId = container.newDeviceId
        )
    }

    LaunchedEffect(stateHolder, playlistStateHolder) {
        container.projectionController.toggleRequests.collect {
            stateHolder.requestProjection(playlistStateHolder.currentPlaylist())
        }
    }

    DisposableEffect(stateHolder) {
        stateHolder.start()
        onDispose { stateHolder.stop() }
    }

    EmitterScreen(
        stateHolder = stateHolder,
        playlistStateHolder = playlistStateHolder,
        storage = container.localStorage,
        onExitRequested = onExitRequested,
        onRoleReset = onRoleReset
    )
}
