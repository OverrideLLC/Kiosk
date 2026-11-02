package app.kiosksync.feature.pairing.presentation

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.navigation.session.AppCommands
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val pairingPresentationModule = module {
    viewModel {
        val session = get<AppSession>()
        val commands = get<AppCommands>()
        PairingViewModel(
            pairingManager = get(),
            receiverServer = get(),
            discovery = get(),
            logger = get(),
            serviceDispatcher = get(AppQualifiers.serviceDispatcher),
            deviceName = session.deviceName,
            localIpAddress = session.localIpAddress,
            onExit = { commands.onExit() },
            onOpenGuide = { commands.onOpenReceiverGuide() },
        )
    }
}
