package app.kiosksync.feature.receiver.presentation

import app.kiosksync.app.navigation.routes.AppRoute
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
internal val receiverNavigationModule = module {
    navigation<AppRoute.ReceiverHome> { ReceiverHomeScreen() }
}
