package app.kiosksync.app.shell

import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.role.domain.AppConfig

sealed interface Screen {
    data object RoleSelection : Screen
    data class EmitterHome(val config: AppConfig) : Screen
    data class ReceiverHome(val config: AppConfig) : Screen
}

internal fun activeRole(screen: Screen): AppRole? = when (screen) {
    is Screen.EmitterHome -> AppRole.EMITTER
    is Screen.ReceiverHome -> AppRole.RECEIVER
    Screen.RoleSelection -> null
}
