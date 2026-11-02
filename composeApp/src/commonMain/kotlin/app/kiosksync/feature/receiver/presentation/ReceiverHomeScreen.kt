package app.kiosksync.feature.receiver.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingRoot
import app.kiosksync.feature.pairing.presentation.PairingViewModel
import app.kiosksync.feature.playback.presentation.CarouselRoot

@Composable
fun ReceiverHomeScreen() {
    val pairing by sessionViewModel<PairingViewModel>().state.collectAsState()
    val lock by sessionViewModel<KioskViewModel>().state.collectAsState()
    when {
        !pairing.pairing.isPaired -> PairingRoot()
        !lock.hasPassword -> WaitingForPasswordScreen()
        else -> CarouselRoot()
    }
}
