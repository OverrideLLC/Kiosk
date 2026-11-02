package app.kiosksync.feature.emitter.di

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.BootstrapCloseables
import app.kiosksync.feature.emitter.data.EmitterServiceImpl
import app.kiosksync.feature.emitter.data.KtorEmitterClient
import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.emitter.domain.EmitterService
import app.kiosksync.feature.sync.data.EmitterSyncRepositoryImpl
import app.kiosksync.feature.sync.domain.EmitterSyncRepository
import app.kiosksync.feature.update.data.FileReceiverUpdateManager
import app.kiosksync.feature.update.data.LocalUpdatePackageProvider
import app.kiosksync.feature.update.domain.ReceiverUpdateManager
import app.kiosksync.feature.update.domain.UpdatePackageProvider
import org.koin.dsl.module

internal fun emitterModule(closeables: BootstrapCloseables) = module {
    single<ReceiverUpdateManager> { FileReceiverUpdateManager(get(), get()) }
    single<EmitterClient> {
        val client = KtorEmitterClient(get())
        closeables.emitterClient = client
        client
    }
    single<EmitterSyncRepository> {
        EmitterSyncRepositoryImpl(client = get(), mediaRepository = get(), logger = get())
    }
    single<UpdatePackageProvider> { LocalUpdatePackageProvider(get(), get()) }
    single<EmitterService> {
        EmitterServiceImpl(
            discovery = get(),
            client = get(),
            screensRepository = get(),
            kioskLockRepository = get(),
            syncRepository = get(),
            logger = get(),
            serviceScope = get(AppQualifiers.serviceScope),
            backgroundDispatcher = get(AppQualifiers.io),
            updatePackageProvider = get(),
        )
    }
}
