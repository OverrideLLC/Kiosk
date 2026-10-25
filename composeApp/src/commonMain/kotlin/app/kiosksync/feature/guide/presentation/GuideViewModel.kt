package app.kiosksync.feature.guide.presentation

import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import app.kiosksync.feature.guide.domain.GuideStep
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Owns the guide's multi-step UI flow and adapts progress reports to its state. */
class GuideViewModel(private val logger: AppLogger) {
    private val _uiState = MutableStateFlow(GuideUiState())
    val state: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<GuideEffect>(Channel.BUFFERED)
    val effects: Flow<GuideEffect> = effectChannel.receiveAsFlow()

    fun onAction(action: GuideAction) {
        when (action) {
            is GuideAction.Start -> if (action.role == null) start() else start(action.role)
            GuideAction.Stop -> stop()
            is GuideAction.RoleConfirmed -> onRoleConfirmed(action.role)
            is GuideAction.ReportEmitterProgress -> updateEmitter(
                action.playlistItems,
                action.pairedScreens,
                action.selectedScreens,
                action.hasPassword,
                action.sendSucceeded,
            )
            is GuideAction.ReportReceiverProgress -> updateReceiver(
                action.paired,
                action.hasPassword,
                action.hasContent,
            )
            GuideAction.Next -> next()
            GuideAction.Back -> back()
            GuideAction.Restart -> restart()
            is GuideAction.RequestEffect -> requestEffect(action.effect)
        }
    }

    /** Starts at the role question. Used on the role screen and from its button. */
    private fun start() {
        _uiState.update {
            it.copy(
                isActive = true,
                role = null,
                step = GuideStep.ROLE,
                autoAdvanceEnabled = true,
            )
        }
        logger.info("Interactive guide opened at role selection")
    }

    /**
     * Starts on an already known role. The first step is inferred from the
     * current snapshot, so opening the guide from Settings takes the person to
     * what still needs attention instead of always starting at the beginning.
     */
    private fun start(role: AppRole) {
        _uiState.update { state ->
            state.copy(
                isActive = true,
                role = role,
                step = initialStep(role, state),
                autoAdvanceEnabled = true,
            )
        }
        logger.info("Interactive guide opened for role $role")
    }

    private fun stop() {
        if (!_uiState.value.isActive) return
        _uiState.update { it.copy(isActive = false) }
        logger.info("Interactive guide closed")
    }

    /**
     * Called when the role screen confirms the role. The snapshot of the chosen
     * role is reset deliberately: this is a new demo, so the guide starts at
     * the first step and lets the real screen report what is already done.
     */
    private fun onRoleConfirmed(role: AppRole) {
        _uiState.update { state ->
            if (!state.isActive) return@update state
            val fresh = when (role) {
                AppRole.EMITTER -> state.copy(
                    playlistItems = 0,
                    pairedScreens = 0,
                    selectedScreens = 0,
                    hasPassword = false,
                    sendSucceeded = false,
                )

                AppRole.RECEIVER -> state.copy(
                    receiverPaired = false,
                    receiverHasPassword = false,
                    receiverHasContent = false,
                )
            }
            state.copy(
                role = role,
                step = initialStep(role, fresh),
                autoAdvanceEnabled = true,
                playlistItems = fresh.playlistItems,
                pairedScreens = fresh.pairedScreens,
                selectedScreens = fresh.selectedScreens,
                hasPassword = fresh.hasPassword,
                sendSucceeded = fresh.sendSucceeded,
                receiverPaired = fresh.receiverPaired,
                receiverHasPassword = fresh.receiverHasPassword,
                receiverHasContent = fresh.receiverHasContent,
            )
        }
    }

    private fun updateEmitter(
        playlistItems: Int,
        pairedScreens: Int,
        selectedScreens: Int,
        hasPassword: Boolean,
        sendSucceeded: Boolean,
    ) {
        _uiState.update { state ->
            val updated = state.copy(
                playlistItems = playlistItems,
                pairedScreens = pairedScreens,
                selectedScreens = selectedScreens,
                hasPassword = hasPassword,
                sendSucceeded = sendSucceeded,
            )
            if (
                !updated.isActive ||
                updated.role != AppRole.EMITTER ||
                !updated.autoAdvanceEnabled
            ) {
                updated
            } else {
                updated.copy(step = advanceEmitter(updated.step, updated))
            }
        }
    }

    private fun updateReceiver(
        paired: Boolean,
        hasPassword: Boolean,
        hasContent: Boolean,
    ) {
        _uiState.update { state ->
            val updated = state.copy(
                receiverPaired = paired,
                receiverHasPassword = hasPassword,
                receiverHasContent = hasContent,
            )
            if (
                !updated.isActive ||
                updated.role != AppRole.RECEIVER ||
                !updated.autoAdvanceEnabled
            ) {
                updated
            } else {
                updated.copy(step = advanceReceiver(updated.step, updated))
            }
        }
    }

    private fun next() {
        _uiState.update { state ->
            if (!state.isActive) return@update state
            state.copy(step = nextStep(state))
        }
    }

    private fun back() {
        _uiState.update { state ->
            if (!state.isActive) return@update state
            // Going back is a manual review. Do not let the next state report
            // pull the card forward again.
            state.copy(
                step = previousStep(state),
                autoAdvanceEnabled = false,
            )
        }
    }

    /** Replays the guide for the current role from its first step. */
    private fun restart() {
        _uiState.update { state ->
            val role = state.role ?: return@update state
            state.copy(
                isActive = true,
                step = firstStep(role),
                autoAdvanceEnabled = false,
            )
        }
    }

    private fun requestEffect(effect: GuideEffect) {
        if (!_uiState.value.isActive) return
        effectChannel.trySend(effect)
    }

    private fun initialStep(role: AppRole, state: GuideUiState): GuideStep = when (role) {
        AppRole.EMITTER -> when {
            state.playlistItems == 0 -> GuideStep.EMITTER_PLAYLIST
            state.pairedScreens == 0 -> GuideStep.EMITTER_SCREENS
            !state.hasPassword -> GuideStep.EMITTER_PASSWORD
            !state.sendSucceeded -> GuideStep.EMITTER_SEND
            else -> GuideStep.EMITTER_DONE
        }

        AppRole.RECEIVER -> when {
            !state.receiverPaired -> GuideStep.RECEIVER_CODE
            !state.receiverHasPassword -> GuideStep.RECEIVER_PASSWORD
            !state.receiverHasContent -> GuideStep.RECEIVER_CONTENT
            else -> GuideStep.RECEIVER_DONE
        }
    }

    private fun firstStep(role: AppRole): GuideStep = when (role) {
        AppRole.EMITTER -> GuideStep.EMITTER_PLAYLIST
        AppRole.RECEIVER -> GuideStep.RECEIVER_CODE
    }

    /**
     * Keeps moving while the next condition is also true, so a screen that
     * already has content can skip several steps in a single report.
     */
    private fun advanceEmitter(step: GuideStep, state: GuideUiState): GuideStep {
        var current = step
        while (true) {
            val next = when (current) {
                GuideStep.EMITTER_PLAYLIST ->
                    if (state.playlistItems > 0) GuideStep.EMITTER_SCREENS else null

                GuideStep.EMITTER_SCREENS ->
                    if (state.pairedScreens > 0) {
                        if (state.hasPassword) GuideStep.EMITTER_SEND else GuideStep.EMITTER_PASSWORD
                    } else {
                        null
                    }

                GuideStep.EMITTER_PASSWORD ->
                    if (state.hasPassword) GuideStep.EMITTER_SEND else null

                GuideStep.EMITTER_SEND ->
                    if (state.sendSucceeded) GuideStep.EMITTER_DONE else null

                else -> null
            }
            if (next == null) return current
            current = next
        }
    }

    private fun advanceReceiver(step: GuideStep, state: GuideUiState): GuideStep {
        var current = step
        while (true) {
            val next = when (current) {
                GuideStep.RECEIVER_CODE ->
                    if (state.receiverPaired) GuideStep.RECEIVER_PASSWORD else null

                GuideStep.RECEIVER_PASSWORD ->
                    if (state.receiverHasPassword) GuideStep.RECEIVER_CONTENT else null

                GuideStep.RECEIVER_CONTENT ->
                    if (state.receiverHasContent) GuideStep.RECEIVER_DONE else null

                else -> null
            }
            if (next == null) return current
            current = next
        }
    }

    private fun nextStep(state: GuideUiState): GuideStep = when (state.step) {
        GuideStep.ROLE -> GuideStep.ROLE
        GuideStep.EMITTER_PLAYLIST -> GuideStep.EMITTER_SCREENS
        GuideStep.EMITTER_SCREENS ->
            if (state.hasPassword) GuideStep.EMITTER_SEND else GuideStep.EMITTER_PASSWORD
        GuideStep.EMITTER_PASSWORD -> GuideStep.EMITTER_SEND
        GuideStep.EMITTER_SEND -> GuideStep.EMITTER_DONE
        GuideStep.EMITTER_DONE -> GuideStep.EMITTER_DONE
        GuideStep.RECEIVER_CODE -> GuideStep.RECEIVER_PASSWORD
        GuideStep.RECEIVER_PASSWORD -> GuideStep.RECEIVER_CONTENT
        GuideStep.RECEIVER_CONTENT -> GuideStep.RECEIVER_DONE
        GuideStep.RECEIVER_DONE -> GuideStep.RECEIVER_DONE
    }

    private fun previousStep(state: GuideUiState): GuideStep = when (state.step) {
        GuideStep.ROLE -> GuideStep.ROLE
        GuideStep.EMITTER_PLAYLIST -> GuideStep.EMITTER_PLAYLIST
        GuideStep.EMITTER_SCREENS -> GuideStep.EMITTER_PLAYLIST
        GuideStep.EMITTER_PASSWORD -> GuideStep.EMITTER_SCREENS
        GuideStep.EMITTER_SEND ->
            if (state.hasPassword) GuideStep.EMITTER_SCREENS else GuideStep.EMITTER_PASSWORD
        GuideStep.EMITTER_DONE -> GuideStep.EMITTER_SEND
        GuideStep.RECEIVER_CODE -> GuideStep.RECEIVER_CODE
        GuideStep.RECEIVER_PASSWORD -> GuideStep.RECEIVER_CODE
        GuideStep.RECEIVER_CONTENT -> GuideStep.RECEIVER_PASSWORD
        GuideStep.RECEIVER_DONE -> GuideStep.RECEIVER_CONTENT
    }
}
