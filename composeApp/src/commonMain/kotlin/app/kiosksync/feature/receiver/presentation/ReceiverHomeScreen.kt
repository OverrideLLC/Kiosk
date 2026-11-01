package app.kiosksync.feature.receiver.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.app.navigation.AppDestinationContext
import app.kiosksync.feature.pairing.presentation.PairingRoot
import app.kiosksync.feature.playback.presentation.CarouselRoot
import org.koin.compose.koinInject

@Composable
fun ReceiverHomeScreen() {
    val destination = koinInject<AppDestinationContext>()
    val pairingViewModel = checkNotNull(destination.pairingViewModel)
    val playback = checkNotNull(destination.playback)
    val pairingState by pairingViewModel.state.collectAsState()
    when {
        !pairingState.pairing.isPaired -> PairingRoot(viewModel = pairingViewModel)
        !destination.hasKioskPassword -> WaitingForPasswordScreen()
        else -> CarouselRoot(holder = playback)
    }
}
