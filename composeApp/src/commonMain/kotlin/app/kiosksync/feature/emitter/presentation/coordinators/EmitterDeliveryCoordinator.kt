package app.kiosksync.feature.emitter.presentation.coordinators

import app.kiosksync.core.common.kiosk.KioskLockConfig
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
import app.kiosksync.resources.Res
import app.kiosksync.resources.playback_sync_no_screens
import app.kiosksync.resources.send_device_success
import app.kiosksync.resources.send_device_up_to_date
import app.kiosksync.resources.send_error_local_media
import app.kiosksync.resources.send_error_missing
import app.kiosksync.resources.send_error_network
import app.kiosksync.resources.send_no_screens
import app.kiosksync.core.ui.UiText
import app.kiosksync.core.ui.sendCompletedText
import app.kiosksync.core.ui.playbackSyncCompletedText

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
                it.copy(statusMessage = UiText(Res.string.playback_sync_no_screens), statusIsError = true)
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
                                statusMessage = playbackSyncCompletedText(
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
                                statusMessage = playbackSyncCompletedText(0, state.devices.count { d -> d.isPairedToThisEmitter }),
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
                        statusMessage = playbackSyncCompletedText(0, state.devices.count { d -> d.isPairedToThisEmitter }),
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
                it.copy(statusMessage = UiText(Res.string.send_no_screens), statusIsError = true)
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
                        statusMessage = sendCompletedText(results.size - failures, failures),
                        statusIsError = failures > 0,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                selected.forEach { device ->
                    updateDeviceSync(device.deviceId) {
                        DeviceSyncUi(SyncUiStatus.FAILED, message = UiText(Res.string.send_error_network))
                    }
                }
                uiState.update {
                    it.copy(
                        isSending = false,
                        statusMessage = sendCompletedText(0, selected.size),
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
                            UiText(Res.string.send_device_up_to_date)
                        } else {
                            UiText(Res.string.send_device_success)
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

    private fun syncErrorMessage(error: Throwable): UiText = when (error) {
        is PlaylistRejectedException -> UiText(Res.string.send_error_missing)
        is EmitterLocalMediaMissingException -> UiText(Res.string.send_error_local_media)
        else -> UiText(Res.string.send_error_network)
    }
}
