package app.kiosksync.feature.update.presentation.viewmodel

import app.kiosksync.core.Strings
import app.kiosksync.feature.update.domain.ReceiverUpdateError
import app.kiosksync.feature.update.domain.ReceiverUpdateManager
import app.kiosksync.feature.update.domain.ReceiverUpdateState
import app.kiosksync.feature.update.presentation.actions.ReceiverUpdateAction
import app.kiosksync.feature.update.presentation.actions.ReceiverUpdateEffect
import app.kiosksync.feature.update.presentation.state.ReceiverUpdateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Adapts update manager state and commands to the Receiver update MVI contract. */
class ReceiverUpdateViewModel(
    private val manager: ReceiverUpdateManager,
    scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(manager.state.value.toUiState())
    val state: StateFlow<ReceiverUpdateUiState> = _state.asStateFlow()

    private val effectChannel = Channel<ReceiverUpdateEffect>(Channel.BUFFERED)
    val effects: Flow<ReceiverUpdateEffect> = effectChannel.receiveAsFlow()

    init {
        scope.launch {
            manager.state.collect { updateState ->
                _state.value = updateState.toUiState()
            }
        }
    }

    fun onAction(action: ReceiverUpdateAction) {
        when (action) {
            ReceiverUpdateAction.Accept -> {
                if (manager.acceptUpdate()) {
                    effectChannel.trySend(ReceiverUpdateEffect.ExitRequested)
                }
            }

            ReceiverUpdateAction.Decline -> manager.declineUpdate()
            ReceiverUpdateAction.DismissError -> manager.dismissError()
        }
    }

    private fun ReceiverUpdateState.toUiState() = ReceiverUpdateUiState(
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
