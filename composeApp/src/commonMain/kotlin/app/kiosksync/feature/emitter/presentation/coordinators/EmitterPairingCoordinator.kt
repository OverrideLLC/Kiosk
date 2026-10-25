package app.kiosksync.feature.emitter.presentation.coordinators

import app.kiosksync.feature.emitter.presentation.EmitterDeviceUi
import app.kiosksync.feature.emitter.presentation.EmitterPendingAction
import app.kiosksync.feature.emitter.presentation.EmitterUiState

import app.kiosksync.core.AppLogger
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.emitter.domain.PairError
import app.kiosksync.feature.emitter.domain.PairRejectedException
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.PairReceiverPersistenceException
import app.kiosksync.feature.emitter.domain.EmitterService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns the asynchronous pairing and unpairing workflows for discovered screens. */
internal class EmitterPairingCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
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
                        it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true)
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
                        statusMessage = Strings.PAIR_SUCCESS,
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
                            is PairReceiverPersistenceException -> Strings.SECURITY_STORAGE_ERROR
                            else -> Strings.PAIR_ERROR_NETWORK
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
                uiState.update { it.copy(statusMessage = Strings.UNPAIR_ERROR, statusIsError = true) }
                return@launch
            }

            onUnpaired(device.deviceId)
            uiState.update {
                it.copy(
                    statusMessage = Strings.UNPAIR_SUCCESS,
                    statusIsError = false,
                    syncByDevice = it.syncByDevice - device.deviceId,
                )
            }
            logger.info("Screen '${device.deviceName}' removed from this Controller")
        }
    }

    private fun mapPairError(error: Throwable): String {
        if (error !is PairRejectedException) return Strings.PAIR_ERROR_NETWORK
        return when (error.error) {
            PairError.INVALID_CODE -> buildString {
                append(Strings.PAIR_ERROR_INVALID_CODE)
                error.remainingAttempts?.let { remaining ->
                    append(' ')
                    append(Strings.PAIR_ERROR_REMAINING_ATTEMPTS)
                    append(' ')
                    append(remaining)
                }
            }

            PairError.LOCKED_OUT -> Strings.PAIR_ERROR_LOCKED
            PairError.ALREADY_PAIRED -> Strings.PAIR_ERROR_ALREADY_PAIRED
        }
    }
}
