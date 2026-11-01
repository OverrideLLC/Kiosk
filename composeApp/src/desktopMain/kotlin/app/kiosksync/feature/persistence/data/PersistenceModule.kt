package app.kiosksync.feature.persistence.data

import app.kiosksync.app.di.BootstrapCloseables
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.media.domain.MediaRepository
import app.kiosksync.feature.pairing.domain.PairingRepository
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.feature.receiver.domain.ReceiverDataResetter
import app.kiosksync.feature.role.data.DataStoreAppConfigStore
import app.kiosksync.feature.role.domain.AppConfigStore
import org.koin.dsl.module

internal fun persistenceModule(closeables: BootstrapCloseables) = module {
    single<AppConfigStore> {
        val store = DataStoreAppConfigStore(get(), get())
        closeables.configStore = store
        store
    }
    single {
        val database = buildKioskDatabase(get())
        closeables.database = database
        database
    }
    single {
        val repositories = RoomRepositories(get(), get(), get(), get())
        closeables.repositories = repositories
        repositories
    }
    single<PairingRepository> { get<RoomRepositories>().pairing }
    single<EmitterScreensRepository> { get<RoomRepositories>().emitterScreens }
    single<PlaylistRepository> { get<RoomRepositories>().playlists }
    single<KioskLockRepository> { get<RoomRepositories>().kioskLock }
    single<MediaRepository> { get<RoomRepositories>().media }
    single<ReceiverDataResetter> { get<RoomRepositories>().receiverDataResetter }
}
