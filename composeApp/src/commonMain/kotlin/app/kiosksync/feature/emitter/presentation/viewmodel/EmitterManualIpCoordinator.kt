package app.kiosksync.feature.emitter.presentation.viewmodel

import app.kiosksync.core.AppLogger
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.AddReceiverByAddressUseCase
import app.kiosksync.feature.emitter.domain.ReceiverAddressDetails
import app.kiosksync.feature.emitter.domain.ReceiverAddressException
import app.kiosksync.feature.emitter.domain.ReceiverAddressFailure
import app.kiosksync.feature.emitter.presentation.state.EmitterUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Adapts the manual-address use case result to the MVI state. */
internal class EmitterManualIpCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val addReceiverByAddress: AddReceiverByAddressUseCase,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val onDeviceAdded: (ReceiverAddressDetails) -> Unit,
) {
    fun confirm() {
        val state = uiState.value
        if (state.isManualIpLoading) return

        scope.launch {
            uiState.update { it.copy(isManualIpLoading = true, manualIpError = null) }
            try {
                val receiver = addReceiverByAddress(state.manualIpInput)
                onDeviceAdded(receiver)
                logger.info("Screen '${receiver.device.deviceName}' added manually at ${receiver.device.host}:${receiver.device.port}")
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
                logger.warn("Manual screen address was rejected: ${error.message}")
                uiState.update {
                    it.copy(isManualIpLoading = false, manualIpError = message)
                }
            }
        }
    }
}
