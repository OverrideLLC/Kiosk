package app.kiosksync.feature.emitter.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.kiosksync.AppContainer
import app.kiosksync.core.AppTheme
import app.kiosksync.feature.emitter.presentation.actions.EmitterAction
import app.kiosksync.feature.emitter.presentation.viewmodel.EmitterViewModel
import app.kiosksync.feature.guide.domain.GuideManager
import app.kiosksync.feature.playlist.presentation.PlaylistEditorStateHolder
import app.kiosksync.feature.role.domain.AppConfig

@Composable
fun EmitterRoute(
    config: AppConfig,
    container: AppContainer,
    guideManager: GuideManager,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit,
    theme: AppTheme = AppTheme.DARK,
    reduceMotion: Boolean = false,
    onThemeChange: (AppTheme) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(config.deviceId) {
        EmitterViewModel(
            discovery = container.discovery,
            client = container.emitterClient,
            screensRepository = container.screensRepository,
            kioskLockRepository = container.kioskLockRepository,
            syncRepository = container.emitterSyncRepository,
            logger = container.logger,
            emitterId = config.deviceId,
            emitterName = container.deviceName,
            scope = scope,
            serviceScope = container.serviceScope,
            kioskLockManager = container.kioskLockManager,
            projectionController = container.projectionController,
            passwordHasher = container.passwordHasher,
            backgroundDispatcher = container.backgroundDispatcher,
            updatePackageProvider = container.updatePackageProvider
        )
    }
    val state by viewModel.state.collectAsState()

    val playlistStateHolder = remember(config.deviceId) {
        PlaylistEditorStateHolder(
            playlistRepository = container.playlistRepository,
            mediaRepository = container.mediaRepository,
            logger = container.logger,
            scope = scope,
            newPlaylistId = container.newDeviceId
        )
    }

    LaunchedEffect(viewModel, playlistStateHolder) {
        container.projectionController.toggleRequests.collect {
            viewModel.onAction(EmitterAction.RequestProjection(playlistStateHolder.currentPlaylist()))
        }
    }

    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    EmitterScreen(
        state = state,
        onAction = viewModel::onAction,
        playlistStateHolder = playlistStateHolder,
        mediaRepository = container.mediaRepository,
        guideManager = guideManager,
        onExitRequested = onExitRequested,
        onRoleReset = onRoleReset,
        theme = theme,
        reduceMotion = reduceMotion,
        onThemeChange = onThemeChange,
        onReduceMotionChange = onReduceMotionChange,
        pickImageFiles = container.pickImageFiles
    )
}
