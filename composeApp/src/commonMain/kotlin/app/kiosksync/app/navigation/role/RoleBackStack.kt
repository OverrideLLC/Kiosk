package app.kiosksync.app.navigation.role

import androidx.navigation3.runtime.NavKey
import app.kiosksync.app.navigation.routes.AppRoute
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass

/** Saved-state key for role selection. The live stack is the session stack. */
internal class RoleBackStack {
    fun register(into: PolymorphicModuleBuilder<NavKey>) {
        into.subclass(AppRoute.RoleSelection::class, AppRoute.RoleSelection.serializer())
    }
}
