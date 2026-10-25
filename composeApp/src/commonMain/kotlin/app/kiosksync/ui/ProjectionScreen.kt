package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.feature.kiosk.presentation.KioskUiState
import app.kiosksync.feature.kiosk.presentation.KioskUnlockOverlay
import app.kiosksync.feature.playback.presentation.CarouselScreen
import app.kiosksync.feature.playback.presentation.PlaybackUiState

/** Stateless full-screen rendering for the controller's projected playlist. */
@Composable
fun ProjectionScreen(
    state: PlaybackUiState,
    lockState: KioskUiState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KioskScreenColors.background),
    ) {
        CarouselScreen(state = state)
        KioskUnlockOverlay(lockState)
    }
}
