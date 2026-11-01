package app.kiosksync.feature.receiver.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingRoot
import app.kiosksync.feature.pairing.presentation.PairingViewModel
import app.kiosksync.feature.playback.presentation.CarouselRoot
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder

@Composable
fun ReceiverHomeScreen() {
    val pairingViewModel = sessionViewModel<PairingViewModel>()
    val playback = sessionViewModel<PlaybackStateHolder>()
    val lock by sessionViewModel<KioskViewModel>().state.collectAsState()
    val pairingState by pairingViewModel.state.collectAsState()
    when {
        !pairingState.pairing.isPaired -> PairingRoot(viewModel = pairingViewModel)
        !lock.hasPassword -> WaitingForPasswordScreen()
        else -> CarouselRoot(holder = playback)
    }
}
