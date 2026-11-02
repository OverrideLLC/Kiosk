package app.kiosksync.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.di.DeviceIdFactory
import app.kiosksync.app.navigation.AppNavHost
import app.kiosksync.app.navigation.AppNavigator
import app.kiosksync.app.navigation.rememberAppBackStack
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.AppCommands
import app.kiosksync.app.session.BindKioskPolicy
import app.kiosksync.app.session.GuideHost
import app.kiosksync.app.session.ReceiverDialogs
import app.kiosksync.app.session.ReceiverHost
import app.kiosksync.app.session.rememberRoleSession
import app.kiosksync.app.shell.AppPreferences
import app.kiosksync.app.shell.LocalAppPreferences
import app.kiosksync.app.shell.PersistForcedRole
import app.kiosksync.app.shell.startupConfig
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.common.discovery.DEFAULT_PORT
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.kiosk.presentation.KioskRoot
import app.kiosksync.feature.role.domain.AppConfigStore
import app.kiosksync.feature.update.presentation.ReceiverUpdateRoot
import app.kiosksync.feature.update.presentation.ReceiverUpdateViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.getKoin

@Composable
fun App(
    receiverUpdateViewModel: ReceiverUpdateViewModel,
    onExitRequested: () -> Unit = {},
    registerEscHandler: ((() -> Unit)?) -> Unit = {},
    registerUnpairHandler: ((() -> Unit)?) -> Unit = {},
) {
    val window = rememberAppWindow()
    val navigator = window.navigator
    val preferences = window.preferences
    val commands = window.commands
    val startRoute = remember(window) { navigator.start(preferences.current?.role) }
    val backStack = rememberAppBackStack(startRoute)
    navigator.attach(backStack)
    val route = navigator.current
    val dialogs = remember { ReceiverDialogs() }
    commands.onExit = onExitRequested
    val role = rememberRoleSession(
        scope = window.scope,
        preferences = preferences,
        dialogs = dialogs,
        deviceId = window.deviceId,
        port = window.port,
        onRoleConfirmed = { navigator.confirm(it.role) },
    )
    commands.onRoleReset = { role.reset(navigator.current) { navigator.showRoleSelection() } }

    PersistForcedRole(preferences, window.session.args.role)
    BindKioskPolicy(route)

    CompositionLocalProvider(LocalAppPreferences provides preferences) {
        KioskTheme(theme = preferences.theme, reduceMotion = preferences.reduceMotion) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(navigator)
                    KioskRoot()
                    GuideHost(route = route, updateViewModel = receiverUpdateViewModel)
                    if (route is AppRoute.ReceiverHome) {
                        ReceiverUpdateRoot(viewModel = receiverUpdateViewModel)
                    }
                }
            }
            ReceiverHost(
                route = route,
                config = preferences.current,
                dialogs = dialogs,
                roleResetting = role.isResetting,
                roleResetFailed = role.failed,
                updateViewModel = receiverUpdateViewModel,
                registerEscHandler = registerEscHandler,
                registerUnpairHandler = registerUnpairHandler,
                onExitRequested = onExitRequested,
                onResetRole = { commands.onRoleReset() },
            )
        }
    }
}

private class AppWindow(
    val scope: CoroutineScope,
    val session: AppSession,
    val deviceId: String,
    val port: Int,
    val preferences: AppPreferences,
    val navigator: AppNavigator,
    val commands: AppCommands,
)

@Composable
private fun rememberAppWindow(): AppWindow {
    val scope = rememberCoroutineScope()
    val koin = getKoin()
    return remember(scope) {
        val session = koin.get<AppSession>()
        val deviceId = session.initialConfig?.deviceId ?: koin.get<DeviceIdFactory>().create()
        session.publishDeviceId(deviceId)
        val port = session.args.port ?: session.initialConfig?.port ?: DEFAULT_PORT
        AppWindow(
            scope = scope,
            session = session,
            deviceId = deviceId,
            port = port,
            preferences = AppPreferences(
                initial = startupConfig(session, deviceId, port),
                configStore = koin.get(),
                logger = koin.get(),
                scope = scope,
            ),
            navigator = koin.get(),
            commands = koin.get(),
        )
    }
}
