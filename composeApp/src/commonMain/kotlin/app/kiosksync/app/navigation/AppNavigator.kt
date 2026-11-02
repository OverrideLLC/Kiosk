package app.kiosksync.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.SessionNavigator
import app.kiosksync.core.common.app.AppRole
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Single navigator for the app. Role, Controller and Screen are opened here.
 * None of them keeps a second stack: panels, pairing and playback are not routes.
 * Desktop has no reflection overload of [rememberNavBackStack], so this class
 * also registers the three route keys.
 */
internal class AppNavigator(
    private val session: SessionNavigator,
) {
    fun attach(stack: NavBackStack<NavKey>) = session.attach(stack)

    fun backStack(): NavBackStack<NavKey> = session.backStack()

    val current: AppRoute
        get() = session.current

    fun back() = session.back()

    fun start(savedRole: AppRole?): AppRoute = when (savedRole) {
        null -> AppRoute.RoleSelection
        AppRole.EMITTER -> AppRoute.EmitterHome
        AppRole.RECEIVER -> AppRoute.ReceiverHome
    }

    fun confirm(savedRole: AppRole) {
        when (savedRole) {
            AppRole.EMITTER -> showEmitter()
            AppRole.RECEIVER -> showReceiver()
        }
    }

    fun showRoleSelection() = session.replace(AppRoute.RoleSelection)

    /** Opens the Controller. Panels inside it are state, not routes. */
    fun showEmitter() = session.replace(AppRoute.EmitterHome)

    /** Opens the Screen. Pairing and playback stay inside this destination. */
    fun showReceiver() = session.replace(AppRoute.ReceiverHome)

    companion object {
        fun savedState(): SavedStateConfiguration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(AppRoute.RoleSelection::class, AppRoute.RoleSelection.serializer())
                    subclass(AppRoute.EmitterHome::class, AppRoute.EmitterHome.serializer())
                    subclass(AppRoute.ReceiverHome::class, AppRoute.ReceiverHome.serializer())
                }
            }
        }
    }
}

@Composable
internal fun rememberAppBackStack(start: AppRoute): NavBackStack<NavKey> {
    val savedState = remember { AppNavigator.savedState() }
    return rememberNavBackStack(savedState, start)
}
