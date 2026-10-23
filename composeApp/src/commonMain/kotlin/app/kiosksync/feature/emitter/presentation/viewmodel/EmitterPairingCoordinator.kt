package app.kiosksync.feature.emitter.presentation.viewmodel

import app.kiosksync.feature.emitter.presentation.state.EmitterDeviceUi
import app.kiosksync.feature.emitter.presentation.state.EmitterPendingAction
import app.kiosksync.feature.emitter.presentation.state.EmitterUiState

import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.PairError
import app.kiosksync.core.PairRejectedException
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.PairReceiverRequest
import app.kiosksync.feature.emitter.domain.PairReceiverPersistenceException
import app.kiosksync.feature.emitter.domain.PairReceiverUseCase
import app.kiosksync.feature.emitter.domain.UnpairReceiverUseCase
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the asynchronous pairing and unpairing workflows for discovered screens. */
internal class EmitterPairingCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val pairReceiver: PairReceiverUseCase,
    private val unpairReceiver: UnpairReceiverUseCase,
    private val kioskLockRepository: KioskLockRepository,
    private val logger: AppLogger,
    private val emitterId: String,
    private val emitterName: String,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val getStoredDevices: () -> List<EmitterPairedDevice>,
    private val setStoredDevices: (List<EmitterPairedDevice>) -> Unit,
    private val targetFor: (EmitterPairedDevice) -> Pair<String, Int>?,
    private val onPaired: (EmitterDeviceUi) -> Unit,
    private val onUnpaired: (String) -> Unit,
    private val openPairLockDialog: (String) -> Unit,
    private val pushLockToDevice: (String, KioskLockConfig) -> Unit,
    private val rebuild: () -> Unit,
) {
    fun confirmPair() {
        val state = uiState.value
        val device = state.pairingDevice ?: return
        if (state.pairingCode.length != 6 || state.isPairing) return

        scope.launch {
            uiState.update { it.copy(isPairing = true, pairingError = null) }
            try {
                val result = withContext(backgroundDispatcher) {
                    pairReceiver(
                        PairReceiverRequest(
                            deviceId = device.deviceId,
                            deviceName = device.deviceName,
                            host = device.host,
                            port = device.port,
                            emitterId = emitterId,
                            emitterName = emitterName,
                            code = state.pairingCode,
                        ),
                    )
                }
                setStoredDevices(result.pairedDevices)
                onPaired(device)
                logger.info("Screen '${device.deviceName}' paired successfully")
                val existingLock = try {
                    withContext(backgroundDispatcher) { kioskLockRepository.load() }
                        ?.takeIf { it.enabled && it.passwordHash != null }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.error("Could not load kiosk security configuration after pairing", error)
                    uiState.update {
                        it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true)
                    }
                    null
                }
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
                rebuild()
                if (existingLock != null) pushLockToDevice(device.deviceId, existingLock)
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
        val storedDevice = getStoredDevices().firstOrNull { it.deviceId == device.deviceId }
        if (storedDevice == null) {
            uiState.update { it.copy(unpairDevice = null) }
            return
        }

        scope.launch {
            uiState.update { it.copy(unpairDevice = null) }
            val target = targetFor(storedDevice)
            try {
                val result = withContext(backgroundDispatcher) {
                    unpairReceiver(storedDevice, target?.first, target?.second, emitterId)
                }
                setStoredDevices(result.pairedDevices)
                if (target != null && result.remoteUnpairSucceeded) {
                    logger.info("Screen '${device.deviceName}' also unpaired remotely")
                } else if (target != null) {
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
            rebuild()
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
