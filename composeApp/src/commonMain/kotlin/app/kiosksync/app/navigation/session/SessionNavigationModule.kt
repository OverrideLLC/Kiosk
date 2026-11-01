package app.kiosksync.app.navigation.session

import app.kiosksync.app.navigation.emitter.EmitterHomeNavigator
import app.kiosksync.app.navigation.receiver.ReceiverHomeNavigator
import app.kiosksync.app.navigation.role.RoleNavigator
import app.kiosksync.app.shell.AppPreferencesRef
import org.koin.dsl.module

internal val sessionNavigationModule = module {
    single { SessionNavigator() }
    single { EmitterHomeNavigator(get()) }
    single { ReceiverHomeNavigator(get()) }
    single { RoleNavigator(get(), get(), get()) }
    single { AppCommands() }
    single { AppPreferencesRef() }
}
