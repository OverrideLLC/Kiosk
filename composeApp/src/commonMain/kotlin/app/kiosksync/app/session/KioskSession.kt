package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.shell.Screen
import app.kiosksync.app.shell.activeRole
import androidx.compose.runtime.LaunchedEffect
import app.kiosksync.feature.kiosk.presentation.KioskAction
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingUiState
import app.kiosksync.feature.projection.domain.ProjectionController
import org.koin.compose.koinInject

@Composable
internal fun BindKioskPolicy(
    screen: Screen,
    pairing: PairingUiState,
    lock: KioskState,
    kioskViewModel: KioskViewModel,
) {
    val session = koinInject<AppSession>()
    val projectionController = koinInject<ProjectionController>()
    LaunchedEffect(screen) {
        session.updateActiveRole(activeRole(screen))
    }
    // Only a paired Screen with a blocking password is a kiosk. The pairing
    // screen and the waiting-for-password screen remain usable.
    LaunchedEffect(screen, pairing.pairing.isPaired, lock.hasPassword) {
        when (screen) {
            is Screen.ReceiverHome -> {
                if (pairing.pairing.isPaired && lock.hasPassword) {
                    kioskViewModel.onAction(KioskAction.Lock(force = true))
                } else {
                    kioskViewModel.onAction(KioskAction.Release)
                }
            }

            Screen.RoleSelection -> kioskViewModel.onAction(KioskAction.Release)
            is Screen.EmitterHome -> {
                if (!projectionController.isProjecting) {
                    kioskViewModel.onAction(KioskAction.Release)
                }
            }
        }
    }
}
