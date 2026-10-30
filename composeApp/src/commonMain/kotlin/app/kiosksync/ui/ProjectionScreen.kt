package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskScreen
import app.kiosksync.feature.playback.presentation.CarouselScreen
import app.kiosksync.feature.playback.presentation.PlaybackState

/** Stateless full-screen rendering for the controller's projected playlist. */
@Composable
fun ProjectionScreen(
    state: PlaybackState,
    lockState: KioskState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KioskScreenColors.background),
    ) {
        CarouselScreen(state = state)
        KioskScreen(state = lockState)
    }
}
