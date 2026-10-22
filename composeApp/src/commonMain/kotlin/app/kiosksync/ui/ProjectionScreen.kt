package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.kiosksync.AppContainer
import app.kiosksync.feature.kiosk.KioskLockUiState
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.feature.kiosk.KioskUnlockOverlay
import app.kiosksync.feature.playback.CarouselScreen
import app.kiosksync.feature.playback.PlaybackStateHolder
import app.kiosksync.feature.playlist.Playlist

/**
 * Fullscreen, borderless projection of the controller's active playlist.
 * The playback holder works in memory: it never reads the receiver's
 * playlist.json, so projection and receiving cannot interfere.
 */
@Composable
fun ProjectionScreen(
    playlist: Playlist,
    container: AppContainer,
    lockState: KioskLockUiState
) {
    val scope = rememberCoroutineScope()
    val stateHolder = remember(playlist.id, playlist.version) {
        PlaybackStateHolder(
            localStorage = container.localStorage,
            logger = container.logger,
            scope = scope,
            statusTracker = null,
            backgroundDispatcher = container.backgroundDispatcher,
            loadFromStorage = false
        )
    }

    LaunchedEffect(stateHolder) {
        stateHolder.updatePlaylist(playlist)
        stateHolder.start()
    }

    DisposableEffect(stateHolder) {
        onDispose { stateHolder.stop() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KioskScreenColors.background)
    ) {
        CarouselScreen(stateHolder = stateHolder)
        KioskUnlockOverlay(lockState)
    }
}
