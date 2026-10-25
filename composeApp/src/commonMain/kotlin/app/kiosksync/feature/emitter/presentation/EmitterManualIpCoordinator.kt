package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.ReceiverAddressException
import app.kiosksync.feature.emitter.domain.ReceiverAddressFailure
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.emitter.presentation.EmitterUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Adapts the manual-address use case result to the MVI state. */
internal class EmitterManualIpCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val service: EmitterService,
    private val scope: CoroutineScope,
) {
    fun confirm() {
        val state = uiState.value
        if (state.isManualIpLoading) return

        scope.launch {
            uiState.update { it.copy(isManualIpLoading = true, manualIpError = null) }
            try {
                service.addReceiver(state.manualIpInput).getOrThrow()
                uiState.update {
                    it.copy(
                        showManualIpDialog = false,
                        manualIpInput = "",
                        manualIpError = null,
                        isManualIpLoading = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val reason = (error as? ReceiverAddressException)?.reason
                val message = when (reason) {
                    ReceiverAddressFailure.INVALID_ADDRESS -> Strings.MANUAL_IP_INVALID
                    ReceiverAddressFailure.NOT_A_RECEIVER -> Strings.MANUAL_IP_NOT_RECEIVER
                    ReceiverAddressFailure.UNREACHABLE, null -> Strings.MANUAL_IP_UNREACHABLE
                }
                uiState.update {
                    it.copy(isManualIpLoading = false, manualIpError = message)
                }
            }
        }
    }
}
