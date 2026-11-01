package app.kiosksync.app.navigation

import androidx.compose.runtime.Composable
import app.kiosksync.app.session.EmitterWorkspace
import app.kiosksync.feature.receiver.presentation.ReceiverHomeScreen
import app.kiosksync.feature.role.presentation.RoleRoot
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.scope.Scope
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val appNavigationModule = module {
    single { AppDestinationContext() }
    navigation<AppRoute.RoleSelection> { RoleDestination(this) }
    navigation<AppRoute.EmitterHome> { EmitterWorkspace() }
    navigation<AppRoute.ReceiverHome> { ReceiverHomeScreen() }
}

@Composable
private fun RoleDestination(scope: Scope) {
    val destination = scope.get<AppDestinationContext>()
    RoleRoot(viewModel = checkNotNull(destination.role).viewModel)
}
