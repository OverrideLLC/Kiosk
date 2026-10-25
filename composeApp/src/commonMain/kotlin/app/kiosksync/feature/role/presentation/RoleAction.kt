package app.kiosksync.feature.role.presentation

import app.kiosksync.core.AppRole

sealed interface RoleAction {
    data class SelectRole(val role: AppRole) : RoleAction
    data object ConfirmRole : RoleAction
    data object ResetSelection : RoleAction
}
