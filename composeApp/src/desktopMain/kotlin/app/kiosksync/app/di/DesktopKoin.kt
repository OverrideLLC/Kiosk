package app.kiosksync.app.di

import app.kiosksync.core.common.app.AppArgs
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.feature.emitter.di.emitterModule
import app.kiosksync.feature.kiosk.di.kioskModule
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.media.data.FileMediaStore
import app.kiosksync.feature.pairing.data.detectLocalIpv4Address
import app.kiosksync.feature.pairing.di.pairingModule
import app.kiosksync.feature.pairing.domain.PairingManager
import app.kiosksync.feature.persistence.data.LegacyJsonMigrator
import app.kiosksync.feature.persistence.data.RoomRepositories
import app.kiosksync.feature.persistence.data.persistenceModule
import app.kiosksync.feature.playback.di.playbackModule
import app.kiosksync.feature.projection.di.projectionModule
import app.kiosksync.feature.receiver.di.receiverModule
import app.kiosksync.feature.role.domain.AppConfigStore
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

private fun desktopAppModule(
    args: AppArgs,
    dataDir: File,
    logger: AppLogger,
    closeables: BootstrapCloseables,
) = module {
    includes(
        platformModule(args, dataDir, logger, closeables),
        persistenceModule(closeables),
        playbackModule,
        projectionModule,
        pairingModule,
        kioskModule,
        receiverModule,
        emitterModule(closeables),
        presentationModule,
    )
    single {
        val repositories = get<RoomRepositories>()
        get<CoroutineDispatcher>(AppQualifiers.serviceDispatcher)
        val dispatcher = closeables.serviceDispatcher
            ?: error("Service dispatcher was not created")
        AppShutdown(
            logger = get(),
            keyboardGuard = get(),
            mouseGuard = get(),
            emitterService = get(),
            discovery = get(),
            receiverServer = get(),
            emitterClient = get(),
            configStore = get(),
            closePersistence = { repositories.close() },
            closeServiceDispatcher = { dispatcher.close() },
            serviceScope = get(AppQualifiers.serviceScope),
        )
    }
}

suspend fun startDesktopKoin(args: AppArgs, dataDir: File, logger: AppLogger) {
    val closeables = BootstrapCloseables()
    startKoin {
        modules(desktopAppModule(args, dataDir, logger, closeables))
    }
    val koin = GlobalContext.get()
    try {
        val config = koin.get<AppConfigStore>().initialize()
        koin.get<RoomRepositories>()
        try {
            withContext(Dispatchers.IO) {
                LegacyJsonMigrator(
                    dataDir = koin.get(),
                    database = koin.get(),
                    mediaStore = FileMediaStore(koin.get(), koin.get()),
                    secretProtector = koin.get(),
                    logger = koin.get(),
                ).migrate()
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            logger.error("Legacy JSON migration did not complete; original files were retained", error)
        }
        File(dataDir, "update-restart.marker").takeIf(File::isFile)?.delete()
        val address = withContext(Dispatchers.IO) { detectLocalIpv4Address() }
        logger.info("Detected local IPv4 address: ${address ?: "none"}")
        koin.get<AppSession>().publishStartup(config, address)
        koin.get<PairingManager>().initialize()
        koin.get<KioskLockManager>().setConfig(koin.get<KioskLockRepository>().load())
        koin.get<AppShutdown>()
    } catch (error: Throwable) {
        closeables.close()
        runCatching { stopKoin() }
        throw error
    }
}
