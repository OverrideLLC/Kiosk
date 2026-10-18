package app.kiosksync.feature.role

import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import app.kiosksync.platform.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RoleUiState(
    val selectedRole: AppRole? = null,
    val showChangeRoleDialog: Boolean = false
)

class RoleStateHolder(
    private val localStorage: LocalStorage,
    private val logger: AppLogger,
    initialRole: AppRole? = null
) {
    private val _uiState = MutableStateFlow(RoleUiState(selectedRole = initialRole))
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()

    fun selectRole(role: AppRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun confirmRole(deviceId: String, deviceName: String, port: Int): AppConfig? {
        val role = _uiState.value.selectedRole ?: return null
        val config = AppConfig(
            role = role,
            deviceId = deviceId,
            deviceName = deviceName,
            port = port
        )
        localStorage.saveConfig(config)
        logger.info("Role confirmed and saved: $role for device $deviceId ($deviceName)")
        return config
    }

    fun requestChangeRole() {
        _uiState.update { it.copy(showChangeRoleDialog = true) }
    }

    fun dismissChangeRoleDialog() {
        _uiState.update { it.copy(showChangeRoleDialog = false) }
    }

    fun confirmChangeRole(): Boolean {
        logger.info("Resetting role configuration upon user confirmation")
        localStorage.clearConfig()
        _uiState.update { RoleUiState(selectedRole = null, showChangeRoleDialog = false) }
        return true
    }
}
