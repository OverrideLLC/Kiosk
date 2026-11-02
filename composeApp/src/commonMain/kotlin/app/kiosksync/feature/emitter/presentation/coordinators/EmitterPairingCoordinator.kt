package app.kiosksync.feature.emitter.presentation.coordinators

import app.kiosksync.feature.emitter.domain.models.EmitterDeviceUi
import app.kiosksync.feature.emitter.domain.models.EmitterPendingAction
import app.kiosksync.feature.emitter.presentation.EmitterState

import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.common.kiosk.KioskLockConfig
import app.kiosksync.feature.emitter.domain.PairError
import app.kiosksync.feature.emitter.domain.PairRejectedException
import app.kiosksync.feature.emitter.domain.PairReceiverPersistenceException
import app.kiosksync.feature.emitter.domain.EmitterService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import app.kiosksync.resources.Res
import app.kiosksync.resources.pair_error_already_paired
import app.kiosksync.resources.pair_error_locked
import app.kiosksync.resources.pair_error_network
import app.kiosksync.resources.pair_success
import app.kiosksync.resources.security_storage_error
import app.kiosksync.resources.unpair_error
import app.kiosksync.resources.unpair_success
import app.kiosksync.core.ui.UiText
import app.kiosksync.core.ui.pairInvalidCodeText

/** Owns the asynchronous pairing and unpairing workflows for discovered screens. */
internal class EmitterPairingCoordinator(
    private val uiState: MutableStateFlow<EmitterState>,
    private val service: EmitterService,
    private val logger: AppLogger,
    private val emitterId: String,
    private val emitterName: String,
    private val scope: CoroutineScope,
    private val onPaired: (EmitterDeviceUi) -> Unit,
    private val onUnpaired: (String) -> Unit,
    private val openPairLockDialog: (String) -> Unit,
) {
    fun confirmPair() {
        val state = uiState.value
        val device = state.pairingDevice ?: return
        if (state.pairingCode.length != 6 || state.isPairing) return

        scope.launch {
            uiState.update { it.copy(isPairing = true, pairingError = null) }
            try {
                service.pair(
                    deviceId = device.deviceId,
                    code = state.pairingCode,
                    emitterId = emitterId,
                    emitterName = emitterName,
                ).getOrThrow()
                onPaired(device)
                logger.info("Screen '${device.deviceName}' paired successfully")
                val existingLock = service.loadKioskLock().fold(
                    onSuccess = { it },
                    onFailure = { error ->
                    logger.error("Could not load kiosk security configuration after pairing", error)
                    uiState.update {
                        it.copy(statusMessage = UiText(Res.string.security_storage_error), statusIsError = true)
                    }
                    null
                    },
                )
                if (existingLock == null) openPairLockDialog(device.deviceId)

                uiState.update {
                    it.copy(
                        pairingDevice = null,
                        pairingCode = "",
                        pairingError = null,
                        isPairing = false,
                        statusMessage = UiText(Res.string.pair_success),
                        statusIsError = false,
                    )
                }
                if (existingLock != null) service.sendLockToDevice(device.deviceId, emitterId, existingLock)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.warn("Pairing failed for '${device.deviceName}': ${error.message}")
                uiState.update {
                    it.copy(
                        isPairing = false,
                        pairingError = when (error) {
                            is PairRejectedException -> mapPairError(error)
                            is PairReceiverPersistenceException -> UiText(Res.string.security_storage_error)
                            else -> UiText(Res.string.pair_error_network)
                        },
                    )
                }
            }
        }
    }

    fun confirmUnpair() {
        val device = uiState.value.unpairDevice ?: return
        scope.launch {
            uiState.update { it.copy(unpairDevice = null) }
            try {
                val result = service.unpair(device.deviceId, emitterId).getOrThrow()
                if (result.remoteUnpairSucceeded) {
                    logger.info("Screen '${device.deviceName}' also unpaired remotely")
                } else {
                    logger.warn(
                        "Screen '${device.deviceName}' removed locally; remote unpair failed"
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not remove pairing for '${device.deviceName}'", error)
                uiState.update { it.copy(statusMessage = UiText(Res.string.unpair_error), statusIsError = true) }
                return@launch
            }

            onUnpaired(device.deviceId)
            uiState.update {
                it.copy(
                    statusMessage = UiText(Res.string.unpair_success),
                    statusIsError = false,
                    syncByDevice = it.syncByDevice - device.deviceId,
                )
            }
            logger.info("Screen '${device.deviceName}' removed from this Controller")
        }
    }

    private fun mapPairError(error: Throwable): UiText {
        if (error !is PairRejectedException) return UiText(Res.string.pair_error_network)
        return when (error.error) {
            PairError.INVALID_CODE -> pairInvalidCodeText(error.remainingAttempts)

            PairError.LOCKED_OUT -> UiText(Res.string.pair_error_locked)
            PairError.ALREADY_PAIRED -> UiText(Res.string.pair_error_already_paired)
        }
    }
}
