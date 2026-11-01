package app.kiosksync.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.di.DeviceIdFactory
import app.kiosksync.app.di.rememberInjected
import app.kiosksync.app.navigation.AppDestinationContext
import app.kiosksync.app.navigation.AppNavHost
import app.kiosksync.app.navigation.AppRoute
import app.kiosksync.app.navigation.currentRoute
import app.kiosksync.app.navigation.rememberAppBackStack
import app.kiosksync.app.navigation.startupRoute
import app.kiosksync.app.session.AppGuideOverlay
import app.kiosksync.app.session.BindGuide
import app.kiosksync.app.session.BindKioskPolicy
import app.kiosksync.app.session.BindReceiverAdmin
import app.kiosksync.app.session.BindReceiverGuide
import app.kiosksync.app.session.BindReceiverPairing
import app.kiosksync.app.session.ReceiverAdminDialogs
import app.kiosksync.app.session.ReceiverDialogs
import app.kiosksync.app.session.rememberReceiverPlayback
import app.kiosksync.app.session.rememberRoleSession
import app.kiosksync.app.shell.AppPreferences
import app.kiosksync.app.shell.PersistForcedRole
import app.kiosksync.app.shell.startupConfig
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.core.common.discovery.DEFAULT_PORT
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.kiosk.presentation.KioskRoot
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingViewModel
import app.kiosksync.feature.pairing.presentation.PairingViewModelHost
import app.kiosksync.feature.role.domain.AppConfigStore
import app.kiosksync.feature.update.presentation.ReceiverUpdateRoot
import app.kiosksync.feature.update.presentation.ReceiverUpdateViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun App(
    kioskViewModel: KioskViewModel,
    receiverUpdateViewModel: ReceiverUpdateViewModel,
    onExitRequested: () -> Unit = {},
    registerEscHandler: ((() -> Unit)?) -> Unit = {},
    registerUnpairHandler: ((() -> Unit)?) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val session = koinInject<AppSession>()
    val deviceIds = koinInject<DeviceIdFactory>()
    val configStore = koinInject<AppConfigStore>()
    val logger = koinInject<AppLogger>()
    val existingConfig = session.initialConfig
    val deviceId = remember { existingConfig?.deviceId ?: deviceIds.create() }
    val port = session.args.port ?: existingConfig?.port ?: DEFAULT_PORT
    val initialConfig = remember { startupConfig(session, deviceId, port) }
    val backStack = rememberAppBackStack(startupRoute(initialConfig?.role))
    val route = currentRoute(backStack)
    val destination = koinInject<AppDestinationContext>()
    destination.backStack = backStack
    val preferences = remember {
        AppPreferences(
            initial = initialConfig,
            configStore = configStore,
            logger = logger,
            scope = scope,
        )
    }
    val dialogs = remember { ReceiverDialogs() }
    val guideViewModel = rememberInjected<GuideViewModel>()
    val pairingViewModel = rememberInjected<PairingViewModel>(scope) {
        parametersOf(
            PairingViewModelHost(
                scope = scope,
                onExit = onExitRequested,
                onOpenGuide = { guideViewModel.onAction(GuideAction.Start(AppRole.RECEIVER)) },
            ),
        )
    }
    val role = rememberRoleSession(
        scope = scope,
        guideViewModel = guideViewModel,
        kioskViewModel = kioskViewModel,
        preferences = preferences,
        dialogs = dialogs,
        deviceId = deviceId,
        port = port,
        onExitRequested = onExitRequested,
        onRoleConfirmed = { destination.replace(startupRoute(it.role)) },
    )
    val playback = rememberReceiverPlayback(scope, route)
    val guideState by guideViewModel.state.collectAsState()
    val pairingState by pairingViewModel.state.collectAsState()
    val lockState by kioskViewModel.state.collectAsState()
    val playbackState by playback.state.collectAsState()
    val updateState by receiverUpdateViewModel.state.collectAsState()

    PersistForcedRole(preferences.current)
    BindReceiverPairing(pairingViewModel, route, preferences.current)
    BindReceiverGuide(guideViewModel, route, pairingState, lockState, playbackState)
    BindReceiverAdmin(
        route = route,
        pairingViewModel = pairingViewModel,
        pairing = pairingState,
        lock = lockState,
        updateVisible = updateState.isVisible,
        dialogs = dialogs,
        kioskViewModel = kioskViewModel,
        guideViewModel = guideViewModel,
        registerEscHandler = registerEscHandler,
        registerUnpairHandler = registerUnpairHandler,
    )
    BindGuide(
        guideViewModel = guideViewModel,
        roleViewModel = role.viewModel,
        route = { currentRoute(backStack) },
        pairing = { pairingState },
        lock = { lockState },
        kioskViewModel = kioskViewModel,
    )
    BindKioskPolicy(route, pairingState, lockState, kioskViewModel)
    destination.role = role
    destination.preferences = preferences
    destination.guideViewModel = guideViewModel
    destination.pairingViewModel = pairingViewModel
    destination.playback = playback
    destination.hasKioskPassword = lockState.hasPassword
    destination.onExitRequested = onExitRequested

    KioskTheme(theme = preferences.theme, reduceMotion = preferences.reduceMotion) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(backStack = backStack)
                KioskRoot(viewModel = kioskViewModel)
                AppGuideOverlay(
                    guideViewModel = guideViewModel,
                    guideState = guideState,
                    route = route,
                    locked = lockState.isLocked,
                    updateVisible = updateState.isVisible,
                )
                if (route is AppRoute.ReceiverHome) {
                    ReceiverUpdateRoot(viewModel = receiverUpdateViewModel)
                }
            }
        }
        ReceiverAdminDialogs(
            route = route,
            pairing = pairingState,
            lock = lockState,
            dialogs = dialogs,
            roleResetting = role.isResetting,
            roleResetFailed = role.failed,
            pairingViewModel = pairingViewModel,
            kioskViewModel = kioskViewModel,
            guideViewModel = guideViewModel,
            onExitRequested = onExitRequested,
            onResetRole = destination::resetRole,
        )
    }
}
