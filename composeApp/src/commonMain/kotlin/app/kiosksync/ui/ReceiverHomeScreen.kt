package app.kiosksync.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.feature.pairing.presentation.PairingRoot
import app.kiosksync.feature.pairing.presentation.PairingViewModel
import app.kiosksync.feature.playback.presentation.CarouselRoot
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder
import app.kiosksync.feature.receiver.presentation.WaitingForPasswordScreen

@Composable
fun ReceiverHomeScreen(
    pairingViewModel: PairingViewModel,
    playbackStateHolder: PlaybackStateHolder,
    hasKioskPassword: Boolean,
) {
    val pairingState by pairingViewModel.state.collectAsState()
    when {
        !pairingState.pairing.isPaired -> PairingRoot(viewModel = pairingViewModel)
        !hasKioskPassword -> WaitingForPasswordScreen()
        else -> CarouselRoot(holder = playbackStateHolder)
    }
}
