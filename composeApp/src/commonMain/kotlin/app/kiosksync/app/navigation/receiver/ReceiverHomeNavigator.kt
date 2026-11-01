package app.kiosksync.app.navigation.receiver

import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.SessionNavigator

/** Opens the Screen. Pairing and playback stay inside this destination. */
internal class ReceiverHomeNavigator(
    private val session: SessionNavigator,
) {
    val route: AppRoute = AppRoute.ReceiverHome

    fun show() = session.replace(route)
}
