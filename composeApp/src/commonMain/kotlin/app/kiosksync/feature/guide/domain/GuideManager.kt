package app.kiosksync.feature.guide.domain

import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Steps of the guided demo. The step only says what the person should do next;
 * the real screens report their state and [GuideManager] moves forward on its
 * own as soon as that state proves the step is complete.
 */
enum class GuideStep {
    ROLE,
    EMITTER_PLAYLIST,
    EMITTER_SCREENS,
    EMITTER_PASSWORD,
    EMITTER_SEND,
    EMITTER_DONE,
    RECEIVER_CODE,
    RECEIVER_PASSWORD,
    RECEIVER_CONTENT,
    RECEIVER_DONE,
}

/** Actions the guide can trigger on the real Controller screen. */
enum class GuideAction {
    ADD_IMAGES,
    REFRESH_DEVICES,
    SEND,
}

data class GuideUiState(
    val isActive: Boolean = false,
    val role: AppRole? = null,
    val step: GuideStep = GuideStep.ROLE,
    /**
     * Auto-advance stays on for a normal run. It turns off when the person uses
     * "Repetir", so a demo that is already configured does not jump to the end
     * as soon as an old successful send is observed.
     */
    val autoAdvanceEnabled: Boolean = true,
    // Snapshot reported by the Controller.
    val playlistItems: Int = 0,
    val pairedScreens: Int = 0,
    val selectedScreens: Int = 0,
    val hasPassword: Boolean = false,
    val sendSucceeded: Boolean = false,
    // Snapshot reported by the Screen.
    val receiverPaired: Boolean = false,
    val receiverHasPassword: Boolean = false,
    val receiverHasContent: Boolean = false,
)

/**
 * Small state machine for the interactive guide. It intentionally lives in
 * commonMain: it has no UI and no platform dependencies, so it can be covered
 * by plain tests. The screens render it and feed it the same state they already
 * use for their normal work.
 */
class GuideManager(private val logger: AppLogger) {
    private val _uiState = MutableStateFlow(GuideUiState())
    val uiState: StateFlow<GuideUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<GuideAction>(extraBufferCapacity = 1)
    val actions: SharedFlow<GuideAction> = _actions.asSharedFlow()

    /** Starts at the role question. Used on the role screen and from its button. */
    fun start() {
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
    fun start(role: AppRole) {
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

    fun stop() {
        if (!_uiState.value.isActive) return
        _uiState.update { it.copy(isActive = false) }
        logger.info("Interactive guide closed")
    }

    /**
     * Called when the role screen confirms the role. The snapshot of the chosen
     * role is reset deliberately: this is a new demo, so the guide starts at
     * the first step and lets the real screen report what is already done.
     */
    fun onRoleConfirmed(role: AppRole) {
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

    fun updateEmitter(
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

    fun updateReceiver(
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

    fun next() {
        _uiState.update { state ->
            if (!state.isActive) return@update state
            state.copy(step = nextStep(state))
        }
    }

    fun back() {
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
    fun restart() {
        _uiState.update { state ->
            val role = state.role ?: return@update state
            state.copy(
                isActive = true,
                step = firstStep(role),
                autoAdvanceEnabled = false,
            )
        }
    }

    fun requestAction(action: GuideAction) {
        if (!_uiState.value.isActive) return
        _actions.tryEmit(action)
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
