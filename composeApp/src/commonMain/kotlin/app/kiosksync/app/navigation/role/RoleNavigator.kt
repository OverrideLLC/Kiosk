package app.kiosksync.app.navigation.role

import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.emitter.EmitterHomeNavigator
import app.kiosksync.app.navigation.receiver.ReceiverHomeNavigator
import app.kiosksync.app.navigation.session.SessionNavigator
import app.kiosksync.core.common.app.AppRole

/** Role selection and the jump from a confirmed role to its home. */
internal class RoleNavigator(
    private val session: SessionNavigator,
    private val emitter: EmitterHomeNavigator,
    private val receiver: ReceiverHomeNavigator,
) {
    val route: AppRoute = AppRoute.RoleSelection

    fun start(role: AppRole?): AppRoute = when (role) {
        null -> route
        AppRole.EMITTER -> emitter.route
        AppRole.RECEIVER -> receiver.route
    }

    fun show() = session.replace(route)

    fun confirm(role: AppRole) {
        when (role) {
            AppRole.EMITTER -> emitter.show()
            AppRole.RECEIVER -> receiver.show()
        }
    }
}
