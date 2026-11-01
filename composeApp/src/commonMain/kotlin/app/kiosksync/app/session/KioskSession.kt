package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.routes.activeRole
import app.kiosksync.feature.kiosk.presentation.KioskAction
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingUiState
import app.kiosksync.feature.projection.domain.ProjectionController
import org.koin.compose.koinInject

@Composable
internal fun BindKioskPolicy(
    route: AppRoute,
    pairing: PairingUiState,
    lock: KioskState,
    kioskViewModel: KioskViewModel,
) {
    val session = koinInject<AppSession>()
    val projectionController = koinInject<ProjectionController>()
    LaunchedEffect(route) {
        session.updateActiveRole(activeRole(route))
    }
    // Only a paired Screen with a blocking password is a kiosk. The pairing
    // screen and the waiting-for-password screen remain usable.
    LaunchedEffect(route, pairing.pairing.isPaired, lock.hasPassword) {
        when (route) {
            AppRoute.ReceiverHome -> {
                if (pairing.pairing.isPaired && lock.hasPassword) {
                    kioskViewModel.onAction(KioskAction.Lock(force = true))
                } else {
                    kioskViewModel.onAction(KioskAction.Release)
                }
            }

            AppRoute.RoleSelection -> kioskViewModel.onAction(KioskAction.Release)
            AppRoute.EmitterHome -> {
                if (!projectionController.isProjecting) {
                    kioskViewModel.onAction(KioskAction.Release)
                }
            }
        }
    }
}
