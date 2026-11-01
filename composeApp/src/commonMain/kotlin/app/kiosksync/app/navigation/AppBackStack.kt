package app.kiosksync.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Desktop has no reflection overload of [rememberNavBackStack]. The three
 * session roots are the only [NavKey] subtypes.
 */
private fun PolymorphicModuleBuilder<NavKey>.registerSessionRoutes() {
    subclass(AppRoute.RoleSelection::class, AppRoute.RoleSelection.serializer())
    subclass(AppRoute.EmitterHome::class, AppRoute.EmitterHome.serializer())
    subclass(AppRoute.ReceiverHome::class, AppRoute.ReceiverHome.serializer())
}

internal fun appRouteSavedState(): SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { registerSessionRoutes() }
    }
}

@Composable
internal fun rememberAppBackStack(start: AppRoute): NavBackStack<NavKey> {
    val savedState = remember { appRouteSavedState() }
    return rememberNavBackStack(savedState, start)
}
