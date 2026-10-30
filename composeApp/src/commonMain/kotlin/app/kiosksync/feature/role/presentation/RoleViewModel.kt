package app.kiosksync.feature.role.presentation

import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Owns role selection state and emits the selected role when the person confirms. */
class RoleViewModel(
    private val logger: AppLogger,
    initialRole: AppRole? = null,
    private val onExit: () -> Unit = {},
    private val onOpenGuide: () -> Unit = {},
) {
    private val _state = MutableStateFlow(RoleState(selectedRole = initialRole))
    val state: StateFlow<RoleState> = _state.asStateFlow()

    private val effectChannel = Channel<RoleEffect>(Channel.BUFFERED)
    val effects: Flow<RoleEffect> = effectChannel.receiveAsFlow()

    fun onAction(action: RoleAction) {
        when (action) {
            is RoleAction.SelectRole -> _state.update { it.copy(selectedRole = action.role) }
            RoleAction.ConfirmRole -> _state.value.selectedRole?.let { role ->
                logger.info("Role confirmed: $role")
                effectChannel.trySend(RoleEffect.RoleConfirmed(role))
            }
            RoleAction.ResetSelection -> _state.value = RoleState()
            RoleAction.Exit -> onExit()
            RoleAction.OpenGuide -> onOpenGuide()
        }
    }
}
