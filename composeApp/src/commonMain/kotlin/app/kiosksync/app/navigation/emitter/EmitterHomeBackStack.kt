package app.kiosksync.app.navigation.emitter

import androidx.navigation3.runtime.NavKey
import app.kiosksync.app.navigation.routes.AppRoute
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass

/** Saved-state key for the Controller. Panels do not add entries. */
internal class EmitterHomeBackStack {
    fun register(into: PolymorphicModuleBuilder<NavKey>) {
        into.subclass(AppRoute.EmitterHome::class, AppRoute.EmitterHome.serializer())
    }
}
