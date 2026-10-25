package app.kiosksync.ui

import androidx.compose.runtime.Composable
import app.kiosksync.feature.receiver.presentation.WaitingForPasswordScreen
import app.kiosksync.feature.pairing.presentation.PairingAction
import app.kiosksync.feature.pairing.presentation.PairingCodeScreen
import app.kiosksync.feature.pairing.presentation.PairingUiState
import app.kiosksync.feature.playback.presentation.screen.CarouselScreen
import app.kiosksync.feature.playback.presentation.state.PlaybackUiState

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
