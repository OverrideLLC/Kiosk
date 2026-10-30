package app.kiosksync.feature.update.presentation

import app.kiosksync.core.Strings
import app.kiosksync.feature.update.domain.ReceiverUpdateError
import app.kiosksync.feature.update.domain.ReceiverUpdateManager
import app.kiosksync.feature.update.domain.ReceiverUpdateState as DomainUpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Adapts update manager state and commands to the Receiver update MVI contract. */
class ReceiverUpdateViewModel(
    private val manager: ReceiverUpdateManager,
    scope: CoroutineScope,
    private val onExit: () -> Unit = {},
) {
    private val _state = MutableStateFlow(manager.state.value.toState())
    val state: StateFlow<ReceiverUpdateState> = _state.asStateFlow()

    init {
        scope.launch {
            manager.state.collect { updateState ->
                _state.value = updateState.toState()
            }
        }
    }

    fun onAction(action: ReceiverUpdateAction) {
        when (action) {
            ReceiverUpdateAction.Accept -> {
                if (manager.acceptUpdate()) onExit()
            }

            ReceiverUpdateAction.Decline -> manager.declineUpdate()
            ReceiverUpdateAction.DismissError -> manager.dismissError()
        }
    }

    private fun DomainUpdateState.toState() = ReceiverUpdateState(
        version = version,
        currentVersion = currentVersion,
        forced = forced,
        applying = applying,
        errorMessage = when (error) {
            ReceiverUpdateError.INSTALLATION_NOT_SUPPORTED -> Strings.UPDATE_NOT_SUPPORTED
            ReceiverUpdateError.INSTALL_FAILED -> Strings.UPDATE_INSTALL_ERROR
            null -> null
        },
    )
}
