package app.kiosksync.feature.emitter.presentation.viewmodel

import app.kiosksync.feature.emitter.presentation.state.DeviceUpdateStatus
import app.kiosksync.feature.emitter.presentation.state.DeviceUpdateUi
import app.kiosksync.feature.emitter.presentation.state.EmitterUiState

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppVersionPolicy
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.Strings
import app.kiosksync.core.UpdateUrgency
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.update.domain.UpdatePackageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class EmitterUpdateCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val client: EmitterClient,
    private val logger: AppLogger,
    private val emitterId: String,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val updatePackageProvider: UpdatePackageProvider?,
    private val getStoredDevices: () -> List<EmitterPairedDevice>,
    private val targetFor: (EmitterPairedDevice) -> Pair<String, Int>?,
) {
    private val deviceVersions = mutableMapOf<String, String>()
    private val skippedUpdateVersions = mutableMapOf<String, String?>()
    private val sentUpdateVersions = mutableMapOf<String, String>()
    private val updateSentAt = mutableMapOf<String, TimeMark>()
    private val updateRetryAt = mutableMapOf<String, TimeMark>()
    private val updateJobs = mutableMapOf<String, Job>()
    private val updateUploadMutex = Mutex()
    private val unavailableUpdateNotice = mutableSetOf<String>()

    fun appVersionFor(deviceId: String): String? = deviceVersions[deviceId]

    fun updateDeviceInfo(deviceId: String, appVersion: String, skippedVersion: String?) {
        deviceVersions[deviceId] = appVersion
        skippedUpdateVersions[deviceId] = skippedVersion
    }

    fun forgetSkippedUpdate(deviceId: String) {
        skippedUpdateVersions.remove(deviceId)
    }

    fun stop() {
        updateJobs.values.forEach(Job::cancel)
        updateJobs.clear()
    }

    fun clearUpdateState(deviceId: String) {
        updateJobs.remove(deviceId)?.cancel()
        sentUpdateVersions.remove(deviceId)
        updateSentAt.remove(deviceId)
        updateRetryAt.remove(deviceId)
        skippedUpdateVersions.remove(deviceId)
        deviceVersions.remove(deviceId)
        unavailableUpdateNotice.remove(deviceId)
        uiState.update { it.copy(updateByDevice = it.updateByDevice - deviceId) }
    }

    /**
     * Manual retry from the controller card. It ignores a previous "stay on
     * this version" decision and the automatic retry delay.
     */
    fun requestUpdate(deviceId: String) {
        maybeAutoUpdate(deviceId, manual = true)
    }

    fun maybeAutoUpdateAll() {
        uiState.value.devices
            .filter { it.isPairedToThisEmitter && it.isOnline && it.appVersion != null }
            .forEach { maybeAutoUpdate(it.deviceId, manual = false) }
    }

    private fun maybeAutoUpdate(deviceId: String, manual: Boolean) {
        val device = uiState.value.devices.firstOrNull { it.deviceId == deviceId } ?: return
        if (!device.isPairedToThisEmitter) return
        val screenVersion = device.appVersion ?: return
        val plan = AppVersionPolicy.plan(APP_VERSION, screenVersion)
        if (plan.urgency == UpdateUrgency.NONE) {
            clearFinishedUpdate(deviceId)
            return
        }

        if (!manual &&
            plan.urgency == UpdateUrgency.OPTIONAL &&
            skippedUpdateVersions[deviceId] == plan.targetVersion
        ) {
            sentUpdateVersions.remove(deviceId)
            updateSentAt.remove(deviceId)
            setDeviceUpdate(
                deviceId,
                DeviceUpdateUi(
                    status = DeviceUpdateStatus.SKIPPED,
                    version = plan.targetVersion,
                    message = Strings.updateSkipped(screenVersion)
                )
            )
            return
        }

        if (!manual && sentUpdateVersions[deviceId] == plan.targetVersion) {
            // Give the screen time to decide and restart. If it is still on an
            // old version after this window, the install probably failed, so
            // allow the next automatic attempt.
            val sentAt = updateSentAt[deviceId]
            if (sentAt == null || sentAt.elapsedNow() < 15.minutes) return
            sentUpdateVersions.remove(deviceId)
            updateSentAt.remove(deviceId)
        }
        if (updateJobs[deviceId]?.isActive == true) return
        val retry = updateRetryAt[deviceId]
        if (!manual && retry != null && retry.elapsedNow() < 60.seconds) return

        val provider = updatePackageProvider
        if (provider == null || !provider.isAvailable()) {
            setDeviceUpdate(
                deviceId,
                DeviceUpdateUi(
                    status = DeviceUpdateStatus.UNAVAILABLE,
                    version = plan.targetVersion,
                    forced = plan.urgency == UpdateUrgency.FORCED,
                    message = Strings.UPDATE_SEND_UNAVAILABLE
                )
            )
            if (unavailableUpdateNotice.add(deviceId)) {
                logger.warn(
                    "Screen '${device.deviceName}' is on $screenVersion but this controller " +
                        "has no packaged update available"
                )
            }
            return
        }
        unavailableUpdateNotice.remove(deviceId)

        val stored = getStoredDevices().firstOrNull { it.deviceId == deviceId } ?: return
        val target = targetFor(stored) ?: return
        sentUpdateVersions[deviceId] = plan.targetVersion

        val updateJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                updateUploadMutex.withLock {
                    setDeviceUpdate(
                        deviceId,
                        DeviceUpdateUi(
                            status = DeviceUpdateStatus.PREPARING,
                            version = plan.targetVersion,
                            forced = plan.urgency == UpdateUrgency.FORCED
                        )
                    )
                    val update = withContext(backgroundDispatcher) {
                        provider.getPackage()
                    }.getOrElse { error ->
                        throw error
                    }

                    setDeviceUpdate(
                        deviceId,
                        DeviceUpdateUi(
                            status = DeviceUpdateStatus.UPLOADING,
                            version = update.version,
                            forced = plan.urgency == UpdateUrgency.FORCED
                        )
                    )
                    val result = withContext(backgroundDispatcher) {
                        client.uploadUpdate(
                            host = target.first,
                            port = target.second,
                            emitterId = emitterId,
                            token = stored.token,
                            version = update.version,
                            sha256 = update.sha256,
                            bytes = update.bytes
                        )
                    }
                    result.fold(
                        onSuccess = { response ->
                            sentUpdateVersions[deviceId] = response.version
                            updateSentAt[deviceId] = TimeSource.Monotonic.markNow()
                            setDeviceUpdate(
                                deviceId,
                                DeviceUpdateUi(
                                    status = DeviceUpdateStatus.AWAITING_CONFIRMATION,
                                    version = response.version,
                                    forced = response.forced
                                )
                            )
                            logger.info(
                                "Update ${response.version} sent to '${stored.deviceName}' " +
                                    "(forced=${response.forced})"
                            )
                        },
                        onFailure = { error ->
                            sentUpdateVersions.remove(deviceId)
                            updateSentAt.remove(deviceId)
                            updateRetryAt[deviceId] = TimeSource.Monotonic.markNow()
                            setDeviceUpdate(
                                deviceId,
                                DeviceUpdateUi(
                                    status = DeviceUpdateStatus.FAILED,
                                    version = plan.targetVersion,
                                    forced = plan.urgency == UpdateUrgency.FORCED,
                                    message = Strings.UPDATE_SEND_FAILED
                                )
                            )
                            logger.warn(
                                "Could not send update ${plan.targetVersion} to " +
                                    "'${stored.deviceName}': ${error.message}"
                            )
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                sentUpdateVersions.remove(deviceId)
                updateSentAt.remove(deviceId)
                throw cancelled
            } catch (error: Exception) {
                sentUpdateVersions.remove(deviceId)
                updateSentAt.remove(deviceId)
                updateRetryAt[deviceId] = TimeSource.Monotonic.markNow()
                setDeviceUpdate(
                    deviceId,
                    DeviceUpdateUi(
                        status = DeviceUpdateStatus.FAILED,
                        version = plan.targetVersion,
                        forced = plan.urgency == UpdateUrgency.FORCED,
                        message = Strings.UPDATE_SEND_FAILED
                    )
                )
                logger.warn(
                    "Update ${plan.targetVersion} for '${stored.deviceName}' failed: " +
                        "${error.message}"
                )
            } finally {
                updateJobs.remove(deviceId)
            }
        }
        updateJobs[deviceId] = updateJob
        updateJob.start()
    }

    private fun setDeviceUpdate(deviceId: String, update: DeviceUpdateUi) {
        uiState.update {
            it.copy(updateByDevice = it.updateByDevice + (deviceId to update))
        }
    }

    private fun clearFinishedUpdate(deviceId: String) {
        sentUpdateVersions.remove(deviceId)
        updateSentAt.remove(deviceId)
        updateRetryAt.remove(deviceId)
        unavailableUpdateNotice.remove(deviceId)
        skippedUpdateVersions[deviceId]?.let { skipped ->
            if (AppVersionPolicy.plan(APP_VERSION, skipped).urgency == UpdateUrgency.NONE) {
                skippedUpdateVersions.remove(deviceId)
            }
        }
        uiState.update { state ->
            val current = state.updateByDevice[deviceId] ?: return@update state
            if (current.status == DeviceUpdateStatus.PREPARING ||
                current.status == DeviceUpdateStatus.UPLOADING
            ) {
                return@update state
            }
            state.copy(updateByDevice = state.updateByDevice - deviceId)
        }
    }

}
