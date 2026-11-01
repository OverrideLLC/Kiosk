package app.kiosksync.feature.role.presentation

import app.kiosksync.app.di.AppSession
import org.koin.dsl.module

internal class RoleViewModelHost(
    val onExit: () -> Unit,
    val onOpenGuide: () -> Unit,
)

internal val rolePresentationModule = module {
    factory { (host: RoleViewModelHost) ->
        val session = get<AppSession>()
        RoleViewModel(
            logger = get(),
            initialRole = session.args.role ?: session.initialConfig?.role,
            onExit = host.onExit,
            onOpenGuide = host.onOpenGuide,
        )
    }
}
