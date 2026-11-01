package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.shell.Screen
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
    screen: () -> Screen,
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
        onStopped = { relockReceiver(screen(), pairing(), lock(), kioskViewModel) },
    )
}

@Composable
internal fun AppGuideOverlay(
    guideViewModel: GuideViewModel,
    guideState: GuideState,
    screen: Screen,
    locked: Boolean,
    updateVisible: Boolean,
) {
    if (guideState.isActive && !locked && !updateVisible && guideState.matches(screen)) {
        GuideRoot(viewModel = guideViewModel)
    }
}

private fun GuideState.matches(screen: Screen): Boolean = when (step) {
    GuideStep.ROLE -> screen is Screen.RoleSelection
    else -> when (role) {
        AppRole.EMITTER -> screen is Screen.EmitterHome
        AppRole.RECEIVER -> screen is Screen.ReceiverHome
        null -> false
    }
}
