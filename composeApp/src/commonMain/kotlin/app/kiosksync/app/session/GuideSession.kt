package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.AppRoute
import androidx.compose.runtime.LaunchedEffect
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideRoot
import app.kiosksync.feature.guide.presentation.GuideState
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingUiState
import app.kiosksync.feature.role.presentation.RoleAction
import app.kiosksync.feature.role.presentation.RoleViewModel
import org.koin.compose.koinInject

@Composable
internal fun BindGuide(
    guideViewModel: GuideViewModel,
    roleViewModel: RoleViewModel,
    route: () -> AppRoute,
    pairing: () -> PairingUiState,
    lock: () -> KioskState,
    kioskViewModel: KioskViewModel,
) {
    val session = koinInject<AppSession>()
    LaunchedEffect(Unit) {
        if (session.initialConfig == null && session.args.role == null) {
            guideViewModel.onAction(GuideAction.Start())
        }
    }
    guideViewModel.updateHost(
        onChooseRole = { role ->
            roleViewModel.onAction(RoleAction.SelectRole(role))
            roleViewModel.onAction(RoleAction.ConfirmRole)
        },
        onStopped = { relockReceiver(route(), pairing(), lock(), kioskViewModel) },
    )
}

@Composable
internal fun AppGuideOverlay(
    guideViewModel: GuideViewModel,
    guideState: GuideState,
    route: AppRoute,
    locked: Boolean,
    updateVisible: Boolean,
) {
    if (guideState.isActive && !locked && !updateVisible && guideState.matches(route)) {
        GuideRoot(viewModel = guideViewModel)
    }
}

private fun GuideState.matches(route: AppRoute): Boolean = when (step) {
    GuideStep.ROLE -> route is AppRoute.RoleSelection
    else -> when (role) {
        AppRole.EMITTER -> route is AppRoute.EmitterHome
        AppRole.RECEIVER -> route is AppRoute.ReceiverHome
        null -> false
    }
}
