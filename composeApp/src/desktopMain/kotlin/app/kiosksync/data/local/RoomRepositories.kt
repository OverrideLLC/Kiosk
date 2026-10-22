package app.kiosksync.data.local

import app.kiosksync.core.AppLogger
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.pairing.domain.PairingRepository
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.platform.FileMediaStore
import app.kiosksync.platform.MediaRepository
import app.kiosksync.platform.ReceiverDataResetter
import app.kiosksync.data.local.room.KioskDatabase
import app.kiosksync.platform.secret.SecretProtector
import java.io.File

/**
 * Composition bundle for focused repositories sharing one database and media directory.
 * The bundle is not itself a repository; each capability has a separate implementation.
 */
class RoomRepositories(
    dataDir: File,
    logger: AppLogger,
    private val database: KioskDatabase,
    secretProtector: SecretProtector,
) {
    private val dao = database.kioskDao()
    private val mediaStore = FileMediaStore(dataDir, logger)

    val pairing: PairingRepository = RoomPairingRepository(dao, secretProtector)
    val emitterScreens: EmitterScreensRepository = RoomEmitterScreensRepository(dao, secretProtector)
    val playlists: PlaylistRepository = RoomPlaylistRepository(dao)
    val kioskLock: KioskLockRepository = RoomKioskLockRepository(dao, secretProtector)
    val media: MediaRepository = RoomMediaRepository(dao, mediaStore, logger)
    val receiverDataResetter: ReceiverDataResetter =
        RoomReceiverDataResetter(dao, mediaStore, dataDir)

    fun close() = database.close()
}

