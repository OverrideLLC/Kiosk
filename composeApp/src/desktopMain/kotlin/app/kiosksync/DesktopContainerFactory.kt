package app.kiosksync

import app.kiosksync.core.AppArgs
import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import app.kiosksync.data.local.RoomRepositories
import app.kiosksync.data.local.migration.LegacyJsonMigrator
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.kiosk.domain.KioskKeyEvent
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.playback.domain.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.domain.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.domain.ReceiverStatusTracker
import app.kiosksync.feature.projection.presentation.ProjectionController
import app.kiosksync.feature.role.data.DataStoreAppConfigStore
import app.kiosksync.platform.FileMediaStore
import app.kiosksync.platform.FileReceiverUpdateManager
import app.kiosksync.platform.JmDnsDiscovery
import app.kiosksync.platform.KtorEmitterClient
import app.kiosksync.platform.KtorReceiverServer
import app.kiosksync.platform.LocalUpdatePackageProvider
import app.kiosksync.platform.Sha256
import app.kiosksync.platform.createKioskKeyboardGuard
import app.kiosksync.platform.createKioskMouseGuard
import app.kiosksync.data.local.room.KioskDatabase
import app.kiosksync.data.local.room.buildKioskDatabase
import app.kiosksync.platform.detectLocalIpv4Address
import app.kiosksync.platform.secret.SecretProtectorFactory
import java.io.File
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

suspend fun createDesktopAppContainer(
    appArgs: AppArgs,
    dataDir: File,
    logger: AppLogger,
): AppContainer {
    val configStore = DataStoreAppConfigStore(dataDir, logger)
    var database: KioskDatabase? = null
    var repositories: RoomRepositories? = null
    var serviceDispatcher: kotlinx.coroutines.ExecutorCoroutineDispatcher? = null

    try {
        val existingConfig = configStore.initialize()
        val activeRole = appArgs.role ?: existingConfig?.role
        val secretProtector = SecretProtectorFactory.create(logger)
        val createdDatabase = buildKioskDatabase(dataDir)
        database = createdDatabase
        val createdRepositories = RoomRepositories(dataDir, logger, createdDatabase, secretProtector)
        repositories = createdRepositories

        try {
            withContext(Dispatchers.IO) {
                LegacyJsonMigrator(
                    dataDir = dataDir,
                    database = createdDatabase,
                    mediaStore = FileMediaStore(dataDir, logger),
                    secretProtector = secretProtector,
                    logger = logger,
                ).migrate()
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            logger.error("Legacy JSON migration did not complete; original files were retained", error)
        }

        File(dataDir, "update-restart.marker").takeIf(File::isFile)?.delete()
        val deviceName = resolveDeviceName()
        val localIpAddress = withContext(Dispatchers.IO) { detectLocalIpv4Address() }
        logger.info("Detected local IPv4 address: ${localIpAddress ?: "none"}")

        val pairingManager = PairingManager(
            repository = createdRepositories.pairing,
            logger = logger,
        )
        // Load any migrated Receiver pairing even when the role selector is shown;
        // the user can choose Receiver without restarting this process.
        pairingManager.initialize()

        val passwordHasher: (String) -> String = { input ->
            Sha256.hex(input.toByteArray(Charsets.UTF_8))
        }
        val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
        val kioskLockAvailable = isWindows && !appArgs.windowed
        val kioskLockManager = KioskLockManager(
            logger = logger,
            passwordHasher = passwordHasher,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            platformLockAvailable = kioskLockAvailable,
        )
        kioskLockManager.setConfig(createdRepositories.kioskLock.load())
        if (activeRole == AppRole.RECEIVER) kioskLockManager.lock(force = true)

        val projectionController = ProjectionController()
        val keyboardGuard = createKioskKeyboardGuard(
            enabled = kioskLockAvailable,
            isLocked = { kioskLockManager.uiState.value.isLocked },
            onKey = { kioskLockManager.onKey(it) },
            onProjectionToggle = { projectionController.requestToggle() },
            logger = logger,
        )
        val mouseGuard = createKioskMouseGuard(
            enabled = kioskLockAvailable,
            isLocked = { kioskLockManager.uiState.value.isLocked },
            logger = logger,
        )

        val statusTracker = ReceiverStatusTracker()
        val playlistBridge = ReceiverPlaylistBridge()
        val playbackSyncBridge = ReceiverPlaybackSyncBridge()
        val discovery = JmDnsDiscovery(logger)
        val receiverUpdateManager = FileReceiverUpdateManager(dataDir, logger)
        val receiverServer = KtorReceiverServer(
            pairingManager = pairingManager,
            logger = logger,
            playlistRepository = createdRepositories.playlists,
            mediaRepository = createdRepositories.media,
            kioskLockRepository = createdRepositories.kioskLock,
            playlistBridge = playlistBridge,
            playbackSyncBridge = playbackSyncBridge,
            kioskLockManager = kioskLockManager,
            statusProvider = { statusTracker.snapshot() },
            updateManager = receiverUpdateManager,
        )
        val emitterClient = KtorEmitterClient(logger)
        val updatePackageProvider = LocalUpdatePackageProvider(dataDir, logger)
        val pickImageFiles: suspend () -> List<String> = {
            DesktopFilePicker.pickImages(logger)
        }

        val createdServiceDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "OverrideKiosk-services").apply { isDaemon = true }
        }.asCoroutineDispatcher()
        serviceDispatcher = createdServiceDispatcher

        return AppContainer(
            args = appArgs,
            dataDirPath = dataDir.absolutePath,
            logger = logger,
            screensRepository = createdRepositories.emitterScreens,
            playlistRepository = createdRepositories.playlists,
            kioskLockRepository = createdRepositories.kioskLock,
            mediaRepository = createdRepositories.media,
            receiverDataResetter = createdRepositories.receiverDataResetter,
            configStore = configStore,
            initialConfig = existingConfig,
            deviceName = deviceName,
            pairingManager = pairingManager,
            discovery = discovery,
            receiverServer = receiverServer,
            receiverUpdateManager = receiverUpdateManager,
            emitterClient = emitterClient,
            updatePackageProvider = updatePackageProvider,
            receiverStatusTracker = statusTracker,
            receiverPlaylistBridge = playlistBridge,
            receiverPlaybackSyncBridge = playbackSyncBridge,
            localIpAddress = localIpAddress,
            kioskLockManager = kioskLockManager,
            kioskKeyboardGuard = keyboardGuard,
            kioskMouseGuard = mouseGuard,
            projectionController = projectionController,
            passwordHasher = passwordHasher,
            newDeviceId = { UUID.randomUUID().toString() },
            pickImageFiles = pickImageFiles,
            closePersistence = { createdRepositories.close() },
            closeServiceDispatcher = { createdServiceDispatcher.close() },
            backgroundDispatcher = Dispatchers.IO,
            serviceDispatcher = createdServiceDispatcher,
        )
    } catch (error: Throwable) {
        configStore.close()
        repositories?.close() ?: database?.close()
        serviceDispatcher?.close()
        throw error
    }
}

private fun resolveDeviceName(): String = try {
    System.getenv("COMPUTERNAME") ?: InetAddress.getLocalHost().hostName
} catch (_: Exception) {
    System.getenv("COMPUTERNAME") ?: "WindowsPC"
}

