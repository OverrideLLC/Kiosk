package app.kiosksync.feature.emitter.presentation.coordinators

import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.Strings
import app.kiosksync.feature.emitter.domain.EmitterDeliveryResult
import app.kiosksync.feature.emitter.domain.EmitterLocalMediaMissingException
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.emitter.domain.PlaylistRejectedException
import app.kiosksync.feature.emitter.domain.models.DeviceSyncUi
import app.kiosksync.feature.emitter.presentation.EmitterState
import app.kiosksync.feature.emitter.domain.models.SyncUiStatus
import app.kiosksync.feature.playlist.domain.Playlist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Maps Controller service outcomes to the editor's delivery and sync UI state. */
internal class EmitterDeliveryCoordinator(
    private val uiState: MutableStateFlow<EmitterState>,
    private val service: EmitterService,
    private val emitterId: String,
    private val scope: CoroutineScope,
) {
    fun synchronizePlayback() {
        val state = uiState.value
        if (state.isSending || state.isSyncing) return
        if (state.devices.none { it.isPairedToThisEmitter }) {
            uiState.update {
                it.copy(statusMessage = Strings.PLAYBACK_SYNC_NO_SCREENS, statusIsError = true)
            }
            return
        }

        uiState.update { it.copy(isSyncing = true, statusMessage = null, statusIsError = false) }
        scope.launch {
            try {
                service.synchronizePlayback(emitterId).fold(
                    onSuccess = { result ->
                        uiState.update {
                            it.copy(
                                isSyncing = false,
                                statusMessage = Strings.playbackSyncCompleted(
                                    result.successCount,
                                    result.failureCount,
                                ),
                                statusIsError = result.failureCount > 0,
                            )
                        }
                    },
                    onFailure = {
                        uiState.update {
                            it.copy(
                                isSyncing = false,
                                statusMessage = Strings.playbackSyncCompleted(0, state.devices.count { d -> d.isPairedToThisEmitter }),
                                statusIsError = true,
                            )
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                uiState.update {
                    it.copy(
                        isSyncing = false,
                        statusMessage = Strings.playbackSyncCompleted(0, state.devices.count { d -> d.isPairedToThisEmitter }),
                        statusIsError = true,
                    )
                }
            }
        }
    }

    fun send(playlist: Playlist, lock: KioskLockConfig) {
        val selected = uiState.value.devices.filter { it.isSelected && it.isPairedToThisEmitter }
        if (selected.isEmpty()) {
            uiState.update {
                it.copy(statusMessage = Strings.SEND_NO_SCREENS, statusIsError = true)
            }
            return
        }

        uiState.update {
            it.copy(
                isSending = true,
                statusMessage = null,
                statusIsError = false,
                syncByDevice = selected.associate { device ->
                    device.deviceId to DeviceSyncUi(SyncUiStatus.PENDING)
                },
            )
        }
        scope.launch {
            try {
                val results = service.deliver(
                    emitterId = emitterId,
                    deviceIds = selected.map { it.deviceId },
                    playlist = playlist,
                    lock = lock,
                    onProgress = { deviceId, progress ->
                        updateDeviceSync(deviceId) {
                            DeviceSyncUi(
                                status = SyncUiStatus.UPLOADING,
                                uploaded = progress.uploaded,
                                total = progress.total,
                            )
                        }
                    },
                    onApplying = { deviceId ->
                        updateDeviceSync(deviceId) { it.copy(status = SyncUiStatus.APPLYING) }
                    },
                )
                results.forEach(::applyResult)
                val failures = results.count { it.result.isFailure }
                uiState.update {
                    it.copy(
                        isSending = false,
                        statusMessage = Strings.sendCompleted(results.size - failures, failures),
                        statusIsError = failures > 0,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                selected.forEach { device ->
                    updateDeviceSync(device.deviceId) {
                        DeviceSyncUi(SyncUiStatus.FAILED, message = Strings.SEND_ERROR_NETWORK)
                    }
                }
                uiState.update {
                    it.copy(
                        isSending = false,
                        statusMessage = Strings.sendCompleted(0, selected.size),
                        statusIsError = true,
                    )
                }
            }
        }
    }

    private fun applyResult(result: EmitterDeliveryResult) {
        result.result.fold(
            onSuccess = { summary ->
                updateDeviceSync(result.deviceId) {
                    DeviceSyncUi(
                        status = SyncUiStatus.SUCCESS,
                        version = summary.appliedVersion,
                        message = if (summary.uploadedCount == 0) {
                            Strings.SEND_DEVICE_UP_TO_DATE
                        } else {
                            Strings.SEND_DEVICE_SUCCESS
                        },
                    )
                }
            },
            onFailure = { error ->
                updateDeviceSync(result.deviceId) {
                    DeviceSyncUi(SyncUiStatus.FAILED, message = syncErrorMessage(error))
                }
            },
        )
    }

    private fun updateDeviceSync(
        deviceId: String,
        transform: (DeviceSyncUi) -> DeviceSyncUi,
    ) {
        uiState.update { state ->
            val current = state.syncByDevice[deviceId] ?: DeviceSyncUi(SyncUiStatus.PENDING)
            state.copy(syncByDevice = state.syncByDevice + (deviceId to transform(current)))
        }
    }

    private fun syncErrorMessage(error: Throwable): String = when (error) {
        is PlaylistRejectedException -> Strings.SEND_ERROR_MISSING
        is EmitterLocalMediaMissingException -> Strings.SEND_ERROR_LOCAL_MEDIA
        else -> Strings.SEND_ERROR_NETWORK
    }
}
