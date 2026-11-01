package app.kiosksync.app.navigation.receiver

import androidx.navigation3.runtime.NavKey
import app.kiosksync.app.navigation.routes.AppRoute
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass

/** Saved-state key for the Screen. Pairing and playback are not entries. */
internal class ReceiverHomeBackStack {
    fun register(into: PolymorphicModuleBuilder<NavKey>) {
        into.subclass(AppRoute.ReceiverHome::class, AppRoute.ReceiverHome.serializer())
    }
}
