package app.kiosksync.feature.emitter.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.core.AppTheme
import app.kiosksync.feature.emitter.presentation.EmitterViewModel
import app.kiosksync.feature.guide.domain.GuideManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import androidx.compose.ui.Modifier

@Composable
fun EmitterRoute(
    viewModel: EmitterViewModel,
    playlist: Playlist,
    onAddImages: () -> Unit,
    playlistPanel: @Composable (
        modifier: Modifier,
        fillHeight: Boolean,
        onRequestRemove: (PlaylistItem) -> Unit,
    ) -> Unit,
    playlistDialogs: @Composable (
        removeTarget: PlaylistItem?,
        onDismissRemove: () -> Unit,
    ) -> Unit,
    guideManager: GuideManager,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit,
    theme: AppTheme = AppTheme.DARK,
    reduceMotion: Boolean = false,
    onThemeChange: (AppTheme) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    EmitterScreen(
        state = state,
        onAction = viewModel::onAction,
        playlist = playlist,
        onAddImages = onAddImages,
        playlistPanel = playlistPanel,
        playlistDialogs = playlistDialogs,
        guideManager = guideManager,
        onExitRequested = onExitRequested,
        onRoleReset = onRoleReset,
        theme = theme,
        reduceMotion = reduceMotion,
        onThemeChange = onThemeChange,
        onReduceMotionChange = onReduceMotionChange,
    )
}
