package app.kiosksync.feature.emitter.presentation.coordinators

import app.kiosksync.feature.emitter.domain.ReceiverAddressException
import app.kiosksync.feature.emitter.domain.ReceiverAddressFailure
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.emitter.presentation.EmitterState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import app.kiosksync.resources.Res
import app.kiosksync.resources.manual_ip_invalid
import app.kiosksync.resources.manual_ip_not_receiver
import app.kiosksync.resources.manual_ip_unreachable
import app.kiosksync.core.ui.UiText

/** Adapts the manual-address use case result to the MVI state. */
internal class EmitterManualIpCoordinator(
    private val uiState: MutableStateFlow<EmitterState>,
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
                    ReceiverAddressFailure.INVALID_ADDRESS -> UiText(Res.string.manual_ip_invalid)
                    ReceiverAddressFailure.NOT_A_RECEIVER -> UiText(Res.string.manual_ip_not_receiver)
                    ReceiverAddressFailure.UNREACHABLE, null -> UiText(Res.string.manual_ip_unreachable)
                }
                uiState.update {
                    it.copy(isManualIpLoading = false, manualIpError = message)
                }
            }
        }
    }
}
