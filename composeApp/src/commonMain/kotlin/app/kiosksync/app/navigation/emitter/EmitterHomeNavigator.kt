package app.kiosksync.app.navigation.emitter

import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.SessionNavigator

/** Opens the Controller. It has no nested stack: panels are not routes. */
internal class EmitterHomeNavigator(
    private val session: SessionNavigator,
) {
    val route: AppRoute = AppRoute.EmitterHome

    fun show() = session.replace(route)
}
