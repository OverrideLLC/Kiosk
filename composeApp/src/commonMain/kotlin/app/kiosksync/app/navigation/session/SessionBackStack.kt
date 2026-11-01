package app.kiosksync.app.navigation.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import app.kiosksync.app.navigation.emitter.EmitterHomeBackStack
import app.kiosksync.app.navigation.receiver.ReceiverHomeBackStack
import app.kiosksync.app.navigation.role.RoleBackStack
import app.kiosksync.app.navigation.routes.AppRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * One live stack. Each area registers its own key; homes do not keep a second
 * stack because they do not push routes.
 *
 * Desktop has no reflection overload of [rememberNavBackStack].
 */
internal class SessionBackStack(
    private val role: RoleBackStack = RoleBackStack(),
    private val emitter: EmitterHomeBackStack = EmitterHomeBackStack(),
    private val receiver: ReceiverHomeBackStack = ReceiverHomeBackStack(),
) {
    fun savedState(): SavedStateConfiguration = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                role.register(this)
                emitter.register(this)
                receiver.register(this)
            }
        }
    }
}

@Composable
internal fun rememberSessionBackStack(start: AppRoute): NavBackStack<NavKey> {
    val savedState = remember { SessionBackStack().savedState() }
    return rememberNavBackStack(savedState, start)
}
