package app.kiosksync.feature.emitter.presentation

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.AppCommands
import app.kiosksync.app.session.EmitterWorkspace
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
internal val emitterPresentationModule = module {
    viewModel {
        val session = get<AppSession>()
        val commands = get<AppCommands>()
        EmitterViewModel(
            service = get(),
            kioskLockConfigurationUseCases = get(),
            logger = get(),
            emitterId = checkNotNull(session.deviceId) { "Device id is not published" },
            emitterName = session.deviceName,
            kioskLockManager = get(),
            projectionController = get(),
            onExit = { commands.onExit() },
            onRoleReset = { commands.onRoleReset() },
            backgroundDispatcher = get(AppQualifiers.io),
        )
    }
    navigation<AppRoute.EmitterHome> { EmitterWorkspace() }
}
