package app.kiosksync.feature.pairing.presentation

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.AppSession
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

internal class PairingViewModelHost(
    val scope: CoroutineScope,
    val onExit: () -> Unit,
    val onOpenGuide: () -> Unit,
)

internal val pairingPresentationModule = module {
    factory { (host: PairingViewModelHost) ->
        val session = get<AppSession>()
        PairingViewModel(
            pairingManager = get(),
            receiverServer = get(),
            discovery = get(),
            logger = get(),
            scope = host.scope,
            serviceDispatcher = get(AppQualifiers.serviceDispatcher),
            deviceName = session.deviceName,
            localIpAddress = session.localIpAddress,
            onExit = host.onExit,
            onOpenGuide = host.onOpenGuide,
        )
    }
}
