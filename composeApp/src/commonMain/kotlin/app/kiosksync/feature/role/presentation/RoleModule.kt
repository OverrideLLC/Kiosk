package app.kiosksync.feature.role.presentation

import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.AppCommands
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
internal val rolePresentationModule = module {
    viewModel {
        val session = get<AppSession>()
        val commands = get<AppCommands>()
        RoleViewModel(
            logger = get(),
            initialRole = session.args.role ?: session.initialConfig?.role,
            onExit = { commands.onExit() },
            onOpenGuide = { commands.onOpenGuide() },
        )
    }
    navigation<AppRoute.RoleSelection> {
        RoleRoot()
    }
}
