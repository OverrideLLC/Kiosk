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
import app.kiosksync.app.navigation.AppNavHost
import app.kiosksync.app.navigation.role.RoleNavigator
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.AppCommands
import app.kiosksync.app.navigation.session.SessionNavigator
import app.kiosksync.app.navigation.session.rememberSessionBackStack
import app.kiosksync.app.navigation.session.sessionViewModel
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
import app.kiosksync.app.shell.AppPreferencesRef
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
import app.kiosksync.feature.role.domain.AppConfigStore
import app.kiosksync.feature.update.presentation.ReceiverUpdateRoot
import app.kiosksync.feature.update.presentation.ReceiverUpdateViewModel
import org.koin.compose.koinInject

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
    session.publishDeviceId(deviceId)
    val initialConfig = remember { startupConfig(session, deviceId, port) }
    val roleNavigator = koinInject<RoleNavigator>()
    val sessionNavigator = koinInject<SessionNavigator>()
    val backStack = rememberSessionBackStack(roleNavigator.start(initialConfig?.role))
    sessionNavigator.attach(backStack)
    val route = sessionNavigator.current
    val commands = koinInject<AppCommands>()
    val preferences = remember {
        AppPreferences(
            initial = initialConfig,
            configStore = configStore,
            logger = logger,
            scope = scope,
        )
    }
    koinInject<AppPreferencesRef>().value = preferences
    val dialogs = remember { ReceiverDialogs() }
    val guideViewModel = sessionViewModel<GuideViewModel>()
    val pairingViewModel = sessionViewModel<PairingViewModel>()
    commands.onExit = onExitRequested
    commands.onOpenGuide = { guideViewModel.onAction(GuideAction.Start()) }
    commands.onOpenReceiverGuide = { guideViewModel.onAction(GuideAction.Start(AppRole.RECEIVER)) }
    val role = rememberRoleSession(
        scope = scope,
        guideViewModel = guideViewModel,
        kioskViewModel = kioskViewModel,
        preferences = preferences,
        dialogs = dialogs,
        deviceId = deviceId,
        port = port,
        onRoleConfirmed = { roleNavigator.confirm(it.role) },
    )
    commands.onRoleReset = { role.reset(sessionNavigator.current) { roleNavigator.show() } }
    val playback = rememberReceiverPlayback(route)
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
        route = { sessionNavigator.current },
        pairing = { pairingState },
        lock = { lockState },
        kioskViewModel = kioskViewModel,
    )
    BindKioskPolicy(route, pairingState, lockState, kioskViewModel)

    KioskTheme(theme = preferences.theme, reduceMotion = preferences.reduceMotion) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost()
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
            onResetRole = { commands.onRoleReset() },
        )
    }
}
