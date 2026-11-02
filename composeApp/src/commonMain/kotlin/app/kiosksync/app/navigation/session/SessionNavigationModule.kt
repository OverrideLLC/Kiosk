package app.kiosksync.app.navigation.session

import app.kiosksync.app.navigation.AppNavigator
import org.koin.dsl.module

internal val sessionNavigationModule = module {
    single { SessionNavigator() }
    single { AppNavigator(get()) }
    single { AppCommands() }
}
