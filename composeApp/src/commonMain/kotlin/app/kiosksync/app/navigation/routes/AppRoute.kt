package app.kiosksync.app.navigation.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Session roots. The route names the area only; playlist, theme and device
 * configuration stay in [app.kiosksync.feature.role.domain.AppConfig].
 * Emitter panels, dialogs, the guide and the kiosk lock are not routes.
 */
@Serializable
sealed interface AppRoute : NavKey {
    /** First screen when this computer has no saved role. */
    @Serializable
    data object RoleSelection : AppRoute

    /** Controller workspace. Panels inside it are state, not routes. */
    @Serializable
    data object EmitterHome : AppRoute

    /** Screen that pairs and plays the local carousel. */
    @Serializable
    data object ReceiverHome : AppRoute
}
