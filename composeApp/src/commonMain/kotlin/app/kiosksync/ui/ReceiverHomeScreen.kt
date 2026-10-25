package app.kiosksync.ui

import androidx.compose.runtime.Composable
import app.kiosksync.feature.pairing.presentation.PairingAction
import app.kiosksync.feature.pairing.presentation.PairingCodeScreen
import app.kiosksync.feature.playback.presentation.CarouselScreen
import app.kiosksync.feature.playback.presentation.PlaybackUiState
import app.kiosksync.feature.pairing.presentation.PairingUiState

@Composable
fun ReceiverHomeScreen(
    pairingState: PairingUiState,
    playbackState: PlaybackUiState,
    hasKioskPassword: Boolean,
    onPairingAction: (PairingAction) -> Unit,
) {
    when {
        !pairingState.pairing.isPaired -> PairingCodeScreen(
            state = pairingState,
            onAction = onPairingAction,
        )

        !hasKioskPassword -> WaitingForPasswordScreen()

        else -> CarouselScreen(state = playbackState)
    }
}
