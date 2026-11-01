package app.kiosksync.app.navigation

import androidx.navigation3.runtime.NavKey
import app.kiosksync.core.common.app.AppRole
import kotlinx.serialization.Serializable

/**
 * Session roots. The route names the area only; playlist, theme and device
 * configuration stay in [app.kiosksync.feature.role.domain.AppConfig].
 * Emitter panels, dialogs, the guide and the kiosk lock are not routes.
 */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object RoleSelection : AppRoute

    @Serializable
    data object EmitterHome : AppRoute

    @Serializable
    data object ReceiverHome : AppRoute
}

internal fun activeRole(route: AppRoute): AppRole? = when (route) {
    AppRoute.EmitterHome -> AppRole.EMITTER
    AppRoute.ReceiverHome -> AppRole.RECEIVER
    AppRoute.RoleSelection -> null
}
