package app.kiosksync.app.navigation.routes

import app.kiosksync.core.common.app.AppRole

internal fun activeRole(route: AppRoute): AppRole? = when (route) {
    AppRoute.EmitterHome -> AppRole.EMITTER
    AppRoute.ReceiverHome -> AppRole.RECEIVER
    AppRoute.RoleSelection -> null
}
