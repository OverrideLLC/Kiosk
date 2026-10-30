package app.kiosksync.local

import app.kiosksync.core.AppLogger
import app.kiosksync.core.security.SecretProtector
import app.kiosksync.feature.emitter.data.RoomEmitterScreensRepository
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.kiosk.data.RoomKioskLockRepository
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.media.data.FileMediaStore
import app.kiosksync.feature.media.data.RoomMediaRepository
import app.kiosksync.feature.media.domain.MediaRepository
import app.kiosksync.feature.pairing.data.RoomPairingRepository
import app.kiosksync.feature.pairing.domain.PairingRepository
import app.kiosksync.feature.playlist.data.RoomPlaylistRepository
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.feature.receiver.data.RoomReceiverDataResetter
import app.kiosksync.feature.receiver.domain.ReceiverDataResetter
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
    private val mediaStore = FileMediaStore(dataDir, logger)

    val pairing: PairingRepository = RoomPairingRepository(database.pairedDeviceDao(), secretProtector)
    val emitterScreens: EmitterScreensRepository = RoomEmitterScreensRepository(database.pairedDeviceDao(), secretProtector)
    val playlists: PlaylistRepository = RoomPlaylistRepository(database.playlistDao())
    val kioskLock: KioskLockRepository = RoomKioskLockRepository(database.kioskLockDao(), secretProtector)
    val media: MediaRepository = RoomMediaRepository(database.mediaDao(), mediaStore, logger)
    val receiverDataResetter: ReceiverDataResetter =
        RoomReceiverDataResetter(database.receiverDataDao(), mediaStore, dataDir)

    fun close() = database.close()
}
