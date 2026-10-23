package app.kiosksync.feature.emitter.viewmodel

import app.kiosksync.feature.emitter.state.DeviceSyncUi
import app.kiosksync.feature.emitter.state.EmitterUiState
import app.kiosksync.feature.emitter.state.SyncUiStatus

import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.PLAYBACK_SYNC_DELAY_MS
import app.kiosksync.core.PlaylistRejectedException
import app.kiosksync.core.Strings
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.sync.domain.EmitterSyncService
import app.kiosksync.feature.sync.domain.LocalMediaMissingException
import app.kiosksync.platform.EmitterClient
import app.kiosksync.platform.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Coordinates playlist delivery and playback synchronization for selected screens. */
internal class EmitterDeliveryCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val client: EmitterClient,
    mediaRepository: MediaRepository,
    private val logger: AppLogger,
    private val emitterId: String,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val getStoredDevices: () -> List<EmitterPairedDevice>,
    private val targetFor: (EmitterPairedDevice) -> Pair<String, Int>?,
) {
    private val syncService = EmitterSyncService(client, mediaRepository, logger)

    fun synchronizePlayback() {
        val state = uiState.value
        if (state.isSending || state.isSyncing) return

        val paired = state.devices.filter { it.isPairedToThisEmitter }
        if (paired.isEmpty()) {
            uiState.update {
                it.copy(statusMessage = Strings.PLAYBACK_SYNC_NO_SCREENS, statusIsError = true)
            }
            return
        }

        val storedDevices = getStoredDevices()
        val targets = paired.mapNotNull { device ->
            val stored = storedDevices.firstOrNull { it.deviceId == device.deviceId }
                ?: return@mapNotNull null
            val target = targetFor(stored) ?: return@mapNotNull null
            stored to target
        }
        val unresolved = paired.size - targets.size

        uiState.update {
            it.copy(isSyncing = true, statusMessage = null, statusIsError = false)
        }

        scope.launch {
            val results = targets.map { (stored, target) ->
                async(backgroundDispatcher) {
                    client.synchronizePlayback(
                        host = target.first,
                        port = target.second,
                        emitterId = emitterId,
                        token = stored.token,
                        delayMs = PLAYBACK_SYNC_DELAY_MS,
                    ).onFailure { error ->
                        logger.warn(
                            "Could not synchronise playback on '${stored.deviceName}': ${error.message}"
                        )
                    }.isSuccess
                }
            }.awaitAll()

            val success = results.count { it }
            val failure = results.size - success + unresolved
            uiState.update {
                it.copy(
                    isSyncing = false,
                    statusMessage = Strings.playbackSyncCompleted(success, failure),
                    statusIsError = failure > 0,
                )
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

        scope.launch {
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

            var successCount = 0
            var failureCount = 0
            val storedDevices = getStoredDevices()

            for (device in selected) {
                val stored = storedDevices.firstOrNull { it.deviceId == device.deviceId }
                if (stored == null) {
                    updateDeviceSync(device.deviceId) {
                        DeviceSyncUi(SyncUiStatus.FAILED, message = Strings.SEND_ERROR_NETWORK)
                    }
                    failureCount++
                    continue
                }

                val target = targetFor(stored)
                if (target == null) {
                    updateDeviceSync(device.deviceId) {
                        DeviceSyncUi(SyncUiStatus.FAILED, message = Strings.SEND_ERROR_NETWORK)
                    }
                    failureCount++
                    continue
                }

                updateDeviceSync(device.deviceId) { DeviceSyncUi(SyncUiStatus.UPLOADING) }
                val syncResult = withContext(backgroundDispatcher) {
                    syncService.sync(
                        device = stored,
                        host = target.first,
                        port = target.second,
                        emitterId = emitterId,
                        playlist = playlist,
                        lock = lock,
                        onUploadProgress = { progress ->
                            updateDeviceSync(device.deviceId) {
                                DeviceSyncUi(
                                    status = SyncUiStatus.UPLOADING,
                                    uploaded = progress.uploaded,
                                    total = progress.total,
                                )
                            }
                        },
                        onApplying = {
                            updateDeviceSync(device.deviceId) {
                                it.copy(status = SyncUiStatus.APPLYING)
                            }
                        },
                    )
                }

                syncResult.fold(
                    onSuccess = { summary ->
                        successCount++
                        updateDeviceSync(device.deviceId) {
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
                        failureCount++
                        logger.warn(
                            "Could not send playlist to '${device.deviceName}': ${error.message}"
                        )
                        updateDeviceSync(device.deviceId) {
                            DeviceSyncUi(
                                status = SyncUiStatus.FAILED,
                                message = syncErrorMessage(error),
                            )
                        }
                    },
                )
            }

            uiState.update {
                it.copy(
                    isSending = false,
                    statusMessage = Strings.sendCompleted(successCount, failureCount),
                    statusIsError = failureCount > 0,
                )
            }
        }
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
        is LocalMediaMissingException -> Strings.SEND_ERROR_LOCAL_MEDIA
        else -> Strings.SEND_ERROR_NETWORK
    }
}
