package app.kiosksync.feature.emitter.presentation

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.AppSession
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

internal class EmitterViewModelHost(
    val deviceId: String,
    val scope: CoroutineScope,
    val onExit: () -> Unit,
    val onRoleReset: () -> Unit,
)

internal val emitterPresentationModule = module {
    factory { (host: EmitterViewModelHost) ->
        val session = get<AppSession>()
        EmitterViewModel(
            service = get(),
            kioskLockConfigurationUseCases = get(),
            logger = get(),
            emitterId = host.deviceId,
            emitterName = session.deviceName,
            scope = host.scope,
            kioskLockManager = get(),
            projectionController = get(),
            onExit = host.onExit,
            onRoleReset = host.onRoleReset,
            backgroundDispatcher = get(AppQualifiers.io),
        )
    }
}
