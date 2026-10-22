package app.kiosksync.feature.role

import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

data class RoleUiState(
    val selectedRole: AppRole? = null,
    val showChangeRoleDialog: Boolean = false
)

sealed interface RoleAction {
    data class SelectRole(val role: AppRole) : RoleAction
    data class ConfirmRole(
        val deviceId: String,
        val deviceName: String,
        val port: Int,
        val theme: AppTheme,
        val reduceMotion: Boolean
    ) : RoleAction
    data object RequestChangeRole : RoleAction
    data object DismissChangeRoleDialog : RoleAction
    data object ConfirmChangeRole : RoleAction
}

sealed interface RoleEffect {
    data class RoleConfirmed(val config: AppConfig) : RoleEffect
}

object RoleReducer {
    fun reduce(state: RoleUiState, action: RoleAction): RoleUiState = when (action) {
        is RoleAction.SelectRole -> state.copy(selectedRole = action.role)
        RoleAction.RequestChangeRole -> state.copy(showChangeRoleDialog = true)
        RoleAction.DismissChangeRoleDialog -> state.copy(showChangeRoleDialog = false)
        RoleAction.ConfirmChangeRole -> RoleUiState()
        is RoleAction.ConfirmRole -> state
    }
}

class RoleStateHolder(
    private val logger: AppLogger,
    initialRole: AppRole? = null
) {
    private val _uiState = MutableStateFlow(RoleUiState(selectedRole = initialRole))
    val uiState: StateFlow<RoleUiState> = _uiState.asStateFlow()
    private val effectChannel = Channel<RoleEffect>(Channel.BUFFERED)
    val effects: Flow<RoleEffect> = effectChannel.receiveAsFlow()

    fun dispatch(action: RoleAction) {
        _uiState.update { RoleReducer.reduce(it, action) }
        when (action) {
            is RoleAction.ConfirmRole -> buildConfig(action)?.let { config ->
                logger.info("Role confirmed for device ${config.deviceId} (${config.deviceName}): ${config.role}")
                effectChannel.trySend(RoleEffect.RoleConfirmed(config))
            }
            RoleAction.ConfirmChangeRole -> logger.info("Resetting role configuration upon user confirmation")
            is RoleAction.SelectRole,
            RoleAction.RequestChangeRole,
            RoleAction.DismissChangeRoleDialog -> Unit
        }
    }

    fun selectRole(role: AppRole) {
        dispatch(RoleAction.SelectRole(role))
    }

    fun confirmRole(
        deviceId: String,
        deviceName: String,
        port: Int,
        theme: AppTheme = AppTheme.DARK,
        reduceMotion: Boolean = false
    ): AppConfig? {
        val config = buildConfig(deviceId, deviceName, port, theme, reduceMotion) ?: return null
        logger.info("Role confirmed for device ${config.deviceId} (${config.deviceName}): ${config.role}")
        return config
    }

    fun requestChangeRole() {
        dispatch(RoleAction.RequestChangeRole)
    }

    fun dismissChangeRoleDialog() {
        dispatch(RoleAction.DismissChangeRoleDialog)
    }

    fun confirmChangeRole(): Boolean {
        dispatch(RoleAction.ConfirmChangeRole)
        return true
    }

    private fun buildConfig(action: RoleAction.ConfirmRole): AppConfig? =
        buildConfig(action.deviceId, action.deviceName, action.port, action.theme, action.reduceMotion)

    private fun buildConfig(
        deviceId: String,
        deviceName: String,
        port: Int,
        theme: AppTheme,
        reduceMotion: Boolean
    ): AppConfig? {
        val role = _uiState.value.selectedRole ?: return null
        return AppConfig(role, deviceId, deviceName, port, theme, reduceMotion)
    }
}
