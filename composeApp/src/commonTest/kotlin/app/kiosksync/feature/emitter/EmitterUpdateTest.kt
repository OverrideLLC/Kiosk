package app.kiosksync.feature.emitter

import app.kiosksync.FakeRepositories
import app.kiosksync.FakeUpdatePackageProvider
import app.kiosksync.NoOpLogger
import app.kiosksync.feature.emitter.presentation.EmitterViewModel
import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.DiscoveredDevice
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.emitter.data.EmitterServiceImpl
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.emitter.domain.EmitterUpdateStatus
import app.kiosksync.feature.emitter.domain.PlaylistApplyResult
import app.kiosksync.feature.emitter.domain.ReceiverInfo
import app.kiosksync.feature.emitter.domain.ReceiverPairingCredential
import app.kiosksync.feature.emitter.domain.ReceiverStatus
import app.kiosksync.feature.emitter.domain.RemoteSyncState
import app.kiosksync.feature.emitter.domain.UpdateUploadResult
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.projection.presentation.ProjectionController
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
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
        val service = EmitterServiceImpl(
            discovery = discovery,
            client = client,
            screensRepository = storage,
            kioskLockRepository = storage,
            syncRepository = storage,
            logger = NoOpLogger(),
            serviceScope = scope,
            backgroundDispatcher = Dispatchers.Unconfined,
            updatePackageProvider = FakeUpdatePackageProvider(
                UpdatePackage(
                    version = APP_VERSION,
                    sha256 = "a".repeat(64),
                    bytes = byteArrayOf(1, 2, 3)
                )
            ),
        )
        val lockManager = KioskLockManager(
            logger = NoOpLogger(),
            passwordHasher = { it },
            scope = scope,
            platformLockAvailable = false,
        )
        val holder = EmitterViewModel(
            service = service,
            kioskLockConfigurationUseCases = KioskLockConfigurationUseCases(
                repository = storage,
                manager = lockManager,
                passwordHasher = { it },
            ),
            logger = NoOpLogger(),
            emitterId = "tx-1",
            emitterName = "Caja",
            scope = scope,
            kioskLockManager = lockManager,
            projectionController = ProjectionController(),
            backgroundDispatcher = Dispatchers.Unconfined,
        )
        try {
            holder.start()

            assertEquals(listOf(APP_VERSION), client.uploadedVersions)
            assertEquals(
                EmitterUpdateStatus.AWAITING_CONFIRMATION,
                holder.state.value.updateByDevice["rx-1"]?.status
            )
            assertTrue(holder.state.value.devices.single().appVersion == "0.9.0")
        } finally {
            holder.stop()
            scope.cancel()
        }
    }
}

internal fun createEmitterViewModelForActionTests(scope: CoroutineScope): EmitterViewModel {
    val storage = FakeRepositories()
    val service = EmitterServiceImpl(
        discovery = FixedDiscovery(emptyList()),
        client = RecordingEmitterClient(screenVersion = APP_VERSION),
        screensRepository = storage,
        kioskLockRepository = storage,
        syncRepository = storage,
        logger = NoOpLogger(),
        serviceScope = scope,
        backgroundDispatcher = Dispatchers.Unconfined,
        updatePackageProvider = FakeUpdatePackageProvider(
            UpdatePackage(version = APP_VERSION, sha256 = "a".repeat(64), bytes = byteArrayOf(1))
        ),
    )
    val lockManager = KioskLockManager(
        logger = NoOpLogger(),
        passwordHasher = { it },
        scope = scope,
        platformLockAvailable = false,
    )
    return EmitterViewModel(
        service = service,
        kioskLockConfigurationUseCases = KioskLockConfigurationUseCases(
            repository = storage,
            manager = lockManager,
            passwordHasher = { it },
        ),
        logger = NoOpLogger(),
        emitterId = "tx-1",
        emitterName = "Caja",
        scope = scope,
        kioskLockManager = lockManager,
        projectionController = ProjectionController(),
        backgroundDispatcher = Dispatchers.Unconfined,
    )
}

internal class FixedDiscovery(devices: List<DiscoveredDevice>) : DeviceDiscovery {
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

internal class RecordingEmitterClient(
    private val screenVersion: String
) : EmitterClient {
    val uploadedVersions = mutableListOf<String>()

    override suspend fun getInfo(host: String, port: Int): Result<ReceiverInfo> =
        Result.failure(UnsupportedOperationException())

    override suspend fun pair(
        host: String,
        port: Int,
        emitterId: String,
        emitterName: String,
        code: String
    ): Result<ReceiverPairingCredential> = Result.failure(UnsupportedOperationException())

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
    ): Result<ReceiverStatus> = Result.success(
        ReceiverStatus(appVersion = screenVersion, skippedUpdateVersion = null)
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
    ): Result<RemoteSyncState> = Result.success(RemoteSyncState(playlistVersion = 0, media = emptyList()))

    override suspend fun uploadMedia(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        mediaId: String,
        sha256: String,
        bytes: ByteArray
    ): Result<Unit> = Result.failure(UnsupportedOperationException())

    override suspend fun uploadUpdate(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        version: String,
        sha256: String,
        bytes: ByteArray
    ): Result<UpdateUploadResult> {
        uploadedVersions += version
        return Result.success(
            UpdateUploadResult(
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
    ): Result<PlaylistApplyResult> = Result.success(
        PlaylistApplyResult(applied = true, version = 0, missingMediaIds = emptyList())
    )

    override suspend fun putLock(
        host: String,
        port: Int,
        emitterId: String,
        token: String,
        lock: KioskLockConfig
    ): Result<Boolean> = Result.success(true)

    override fun close() {}
}
