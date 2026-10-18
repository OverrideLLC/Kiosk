package app.kiosksync.ui

import app.kiosksync.feature.role.AppConfig

sealed interface Screen {
    data object RoleSelection : Screen
    data class EmitterHome(val config: AppConfig) : Screen
    data class ReceiverHome(val config: AppConfig) : Screen
}
