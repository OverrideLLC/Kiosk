package app.kiosksync.feature.emitter

import app.kiosksync.FakeRepositories
import app.kiosksync.FakeUpdatePackageProvider
import app.kiosksync.NoOpLogger
import app.kiosksync.feature.emitter.presentation.state.DeviceUpdateStatus
import app.kiosksync.feature.emitter.presentation.viewmodel.EmitterViewModel
import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.DeviceInfo
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MediaUploadResponse
import app.kiosksync.core.PairResponse
import app.kiosksync.core.PlaylistApplyResponse
import app.kiosksync.core.ReceiverStatusResponse
import app.kiosksync.core.SyncStateResponse
import app.kiosksync.core.UpdateUploadResponse
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.projection.presentation.ProjectionController
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.update.domain.UpdatePackage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

class EmitterUpdateTest {

    @Test
    fun sendsUpdateAutomaticallyToAnIncompatibleScreen() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val storage = FakeRepositories().apply {
            emitterPairedDevices = listOf(
                EmitterPairedDevice(
                    deviceId = "rx-1",
                    deviceName = "Pantalla",
                    token = "token",
                    host = "127.0.0.1",
                    port = 8765
                )
            )
        }
        val discovery = FixedDiscovery(
            listOf(
                DiscoveredDevice(
                    deviceId = "rx-1",
                    deviceName = "Pantalla",
                    host = "127.0.0.1",
                    port = 8765,
                    paired = true,
                    appVersion = "0.9.0"
                )
            )
        )
        val client = RecordingEmitterClient(screenVersion = "0.9.0")
        val holder = EmitterViewModel(
            discovery = discovery,
            client = client,
            screensRepository = storage,
            kioskLockRepository = storage,
            syncRepository = storage,
            logger = NoOpLogger(),
            emitterId = "tx-1",
            emitterName = "Caja",
            scope = scope,
            serviceScope = scope,
            kioskLockManager = KioskLockManager(
                logger = NoOpLogger(),
                passwordHasher = { it },
                scope = scope,
                platformLockAvailable = false
            ),
            projectionController = ProjectionController(),
            passwordHasher = { it },
            backgroundDispatcher = Dispatchers.Unconfined,
            updatePackageProvider = FakeUpdatePackageProvider(
                UpdatePackage(
                    version = APP_VERSION,
                    sha256 = "a".repeat(64),
                    bytes = byteArrayOf(1, 2, 3)
                )
            )
        )
        try {
            holder.start()

            assertEquals(listOf(APP_VERSION), client.uploadedVersions)
            assertEquals(
                DeviceUpdateStatus.AWAITING_CONFIRMATION,
                holder.state.value.updateByDevice["rx-1"]?.status
            )
            assertTrue(holder.state.value.devices.single().appVersion == "0.9.0")
        } finally {
            holder.stop()
            scope.cancel()
        }
    }
}

private class FixedDiscovery(devices: List<DiscoveredDevice>) : DeviceDiscovery {
    private val state = MutableStateFlow(devices)
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = state
    override fun startDiscovery() {}
    override fun stopDiscovery() {}
    override fun refresh() {}
    override fun registerService(
        deviceName: String,
        port: Int,
        deviceId: String,
        isPaired: Boolean
    ) {}
    override fun unregisterService() {}
}

private class RecordingEmitterClient(
    private val screenVersion: String
) : EmitterClient {
    val uploadedVersions = mutableListOf<String>()

    override suspend fun getInfo(host: String, port: Int): Result<DeviceInfo> =
        Result.failure(UnsupportedOperationException())

    override suspend fun pair(
        host: String,
        port: Int,
        emitterId: String,
        emitterName: String,
        code: String
    ): Result<PairResponse> = Result.failure(UnsupportedOperationException())

    override suspend fun unpair(
        host: String,
        port: Int,
        emitterId: String,
        token: String
    ): Result<Boolean> = Result.success(true)

    override suspend fun getStatus(
        host: String,
        port: Int,
        emitterId: String,
        token: String
    ): Result<ReceiverStatusResponse> = Result.success(
        ReceiverStatusResponse(
            playlistVersion = 0,
            appVersion = screenVersion
        )
    )

    override suspend fun synchronizePlayback(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        delayMs: Long
    ): Result<Boolean> = Result.success(true)

    override suspend fun getSyncState(
        host: String,
        port: Int,
        emitterId: String,
        token: String
    ): Result<SyncStateResponse> = Result.success(SyncStateResponse())

    override suspend fun uploadMedia(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        mediaId: String,
        sha256: String,
        bytes: ByteArray
    ): Result<MediaUploadResponse> = Result.failure(UnsupportedOperationException())

    override suspend fun uploadUpdate(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        version: String,
        sha256: String,
        bytes: ByteArray
    ): Result<UpdateUploadResponse> {
        uploadedVersions += version
        return Result.success(
            UpdateUploadResponse(
                accepted = true,
                version = version,
                forced = true
            )
        )
    }

    override suspend fun putPlaylist(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        playlist: Playlist,
        lock: KioskLockConfig?
    ): Result<PlaylistApplyResponse> = Result.success(PlaylistApplyResponse(applied = true))

    override suspend fun putLock(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        lock: KioskLockConfig
    ): Result<Boolean> = Result.success(true)

    override fun close() {}
}
