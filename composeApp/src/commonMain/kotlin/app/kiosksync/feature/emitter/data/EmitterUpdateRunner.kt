package app.kiosksync.feature.emitter.data

import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppVersionPolicy
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.UpdateUrgency
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.emitter.domain.EmitterServiceDevice
import app.kiosksync.feature.emitter.domain.EmitterUpdateState
import app.kiosksync.feature.emitter.domain.EmitterUpdateStatus
import app.kiosksync.feature.update.domain.UpdatePackageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal data class EmitterUpdateTarget(
    val device: EmitterPairedDevice,
    val host: String,
    val port: Int,
)

/** Owns update decisions, retry timing, and package uploads for paired Receivers. */
internal class EmitterUpdateRunner(
    private val client: EmitterClient,
    private val updatePackageProvider: UpdatePackageProvider,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val currentDevice: (String) -> EmitterServiceDevice?,
    private val currentTarget: (String) -> EmitterUpdateTarget?,
    private val emitterId: () -> String,
    private val setUpdate: (String, EmitterUpdateState?) -> Unit,
) {
    private val lock = Any()
    private val uploadMutex = Mutex()
    private val skippedVersions = mutableMapOf<String, String?>()
    private val sentVersions = mutableMapOf<String, String>()
    private val sentAt = mutableMapOf<String, TimeMark>()
    private val retryAt = mutableMapOf<String, TimeMark>()
    private val jobs = mutableMapOf<String, Job>()
    private val unavailableNotices = mutableSetOf<String>()

    fun recordSkippedVersion(deviceId: String, version: String?) {
        synchronized(lock) { skippedVersions[deviceId] = version }
    }

    fun request(deviceId: String, manual: Boolean) {
        val device = currentDevice(deviceId) ?: return
        if (!device.pairedToThisEmitter) return
        val screenVersion = device.appVersion ?: return
        val plan = AppVersionPolicy.plan(APP_VERSION, screenVersion)
        if (plan.urgency == UpdateUrgency.NONE) {
            clearFinished(deviceId)
            return
        }

        synchronized(lock) {
            if (!manual && plan.urgency == UpdateUrgency.OPTIONAL && skippedVersions[deviceId] == plan.targetVersion) {
                sentVersions.remove(deviceId)
                sentAt.remove(deviceId)
                setUpdate(deviceId, EmitterUpdateState(EmitterUpdateStatus.SKIPPED, plan.targetVersion))
                return
            }
            if (!manual && sentVersions[deviceId] == plan.targetVersion) {
                val lastSent = sentAt[deviceId]
                if (lastSent == null || lastSent.elapsedNow() < 15.minutes) return
                sentVersions.remove(deviceId)
                sentAt.remove(deviceId)
            }
            if (jobs[deviceId]?.isActive == true) return
            val lastRetry = retryAt[deviceId]
            if (!manual && lastRetry != null && lastRetry.elapsedNow() < 60.seconds) return
        }

        if (!updatePackageProvider.isAvailable()) {
            setUpdate(
                deviceId,
                EmitterUpdateState(
                    status = EmitterUpdateStatus.UNAVAILABLE,
                    version = plan.targetVersion,
                    forced = plan.urgency == UpdateUrgency.FORCED,
                ),
            )
            synchronized(lock) {
                if (unavailableNotices.add(deviceId)) {
                    logger.warn("Screen '${device.deviceName}' is on $screenVersion but this Controller has no packaged update available")
                }
            }
            return
        }

        val target = currentTarget(deviceId) ?: return
        synchronized(lock) {
            unavailableNotices.remove(deviceId)
            if (jobs[deviceId]?.isActive == true) return
            sentVersions[deviceId] = plan.targetVersion
            jobs[deviceId] = scope.launch {
                try {
                    uploadMutex.withLock {
                        setUpdate(
                            deviceId,
                            EmitterUpdateState(
                                EmitterUpdateStatus.PREPARING,
                                plan.targetVersion,
                                plan.urgency == UpdateUrgency.FORCED,
                            ),
                        )
                        val update = withContext(backgroundDispatcher) {
                            updatePackageProvider.getPackage().getOrThrow()
                        }
                        setUpdate(
                            deviceId,
                            EmitterUpdateState(
                                EmitterUpdateStatus.UPLOADING,
                                update.version,
                                plan.urgency == UpdateUrgency.FORCED,
                            ),
                        )
                        val upload = withContext(backgroundDispatcher) {
                            client.uploadUpdate(
                                host = target.host,
                                port = target.port,
                                emitterId = emitterId(),
                                token = target.device.token,
                                version = update.version,
                                sha256 = update.sha256,
                                bytes = update.bytes,
                            )
                        }.getOrThrow()
                        synchronized(lock) {
                            sentVersions[deviceId] = upload.version
                            sentAt[deviceId] = TimeSource.Monotonic.markNow()
                        }
                        setUpdate(
                            deviceId,
                            EmitterUpdateState(
                                EmitterUpdateStatus.AWAITING_CONFIRMATION,
                                upload.version,
                                upload.forced,
                            ),
                        )
                        logger.info("Update ${upload.version} sent to '${target.device.deviceName}' (forced=${upload.forced})")
                    }
                } catch (cancelled: CancellationException) {
                    synchronized(lock) {
                        sentVersions.remove(deviceId)
                        sentAt.remove(deviceId)
                    }
                    throw cancelled
                } catch (error: Exception) {
                    synchronized(lock) {
                        sentVersions.remove(deviceId)
                        sentAt.remove(deviceId)
                        retryAt[deviceId] = TimeSource.Monotonic.markNow()
                    }
                    setUpdate(
                        deviceId,
                        EmitterUpdateState(
                            EmitterUpdateStatus.FAILED,
                            plan.targetVersion,
                            plan.urgency == UpdateUrgency.FORCED,
                        ),
                    )
                    logger.warn("Update ${plan.targetVersion} for '${target.device.deviceName}' failed: ${error.message}")
                } finally {
                    synchronized(lock) { jobs.remove(deviceId) }
                }
            }
        }
    }

    fun requestOnlineDevices(devices: List<EmitterServiceDevice>) {
        devices.filter { it.pairedToThisEmitter && it.isOnline && it.appVersion != null }
            .forEach { request(it.deviceId, manual = false) }
    }

    fun forget(deviceId: String) {
        synchronized(lock) {
            jobs.remove(deviceId)?.cancel()
            sentVersions.remove(deviceId)
            sentAt.remove(deviceId)
            retryAt.remove(deviceId)
            skippedVersions.remove(deviceId)
            unavailableNotices.remove(deviceId)
        }
        setUpdate(deviceId, null)
    }

    fun stop() {
        synchronized(lock) {
            jobs.values.forEach(Job::cancel)
            jobs.clear()
        }
    }

    private fun clearFinished(deviceId: String) {
        val shouldClear = synchronized(lock) {
            sentVersions.remove(deviceId)
            sentAt.remove(deviceId)
            retryAt.remove(deviceId)
            unavailableNotices.remove(deviceId)
            skippedVersions[deviceId]?.let { skipped ->
                if (AppVersionPolicy.plan(APP_VERSION, skipped).urgency == UpdateUrgency.NONE) {
                    skippedVersions.remove(deviceId)
                }
            }
            jobs[deviceId]?.isActive != true
        }
        if (shouldClear) setUpdate(deviceId, null)
    }
}
