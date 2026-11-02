package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.AppCommands
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideRoot
import app.kiosksync.feature.guide.presentation.GuideState
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingUiState
import app.kiosksync.feature.pairing.presentation.PairingViewModel
import app.kiosksync.feature.role.presentation.RoleAction
import app.kiosksync.feature.role.presentation.RoleViewModel
import app.kiosksync.feature.update.presentation.ReceiverUpdateViewModel
import org.koin.compose.koinInject

@Composable
internal fun GuideHost(
    route: AppRoute,
    updateViewModel: ReceiverUpdateViewModel,
) {
    val guideViewModel = sessionViewModel<GuideViewModel>()
    val roleViewModel = sessionViewModel<RoleViewModel>()
    val kioskViewModel = sessionViewModel<KioskViewModel>()
    val pairingViewModel = sessionViewModel<PairingViewModel>()
    val commands = koinInject<AppCommands>()
    val guideState by guideViewModel.state.collectAsState()
    val pairingState by pairingViewModel.state.collectAsState()
    val lockState by kioskViewModel.state.collectAsState()
    val updateState by updateViewModel.state.collectAsState()
    commands.onOpenGuide = { guideViewModel.onAction(GuideAction.Start()) }
    commands.onOpenReceiverGuide = { guideViewModel.onAction(GuideAction.Start(AppRole.RECEIVER)) }
    BindGuide(
        guideViewModel = guideViewModel,
        roleViewModel = roleViewModel,
        route = { route },
        pairing = { pairingState },
        lock = { lockState },
        kioskViewModel = kioskViewModel,
    )
    AppGuideOverlay(
        guideViewModel = guideViewModel,
        guideState = guideState,
        route = route,
        locked = lockState.isLocked,
        updateVisible = updateState.isVisible,
    )
}

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
