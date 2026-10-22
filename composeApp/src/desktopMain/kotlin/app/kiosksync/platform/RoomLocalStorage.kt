package app.kiosksync.platform

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistItem
import app.kiosksync.feature.playlist.PlaylistLibrary
import app.kiosksync.platform.db.KioskDao
import app.kiosksync.platform.db.KioskDatabase
import app.kiosksync.platform.db.KioskLockEntity
import app.kiosksync.platform.db.MediaEntity
import app.kiosksync.platform.db.PairedDeviceEntity
import app.kiosksync.platform.db.PlaylistEntity
import app.kiosksync.platform.db.PlaylistItemEntity
import app.kiosksync.platform.secret.SecretProtector
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Compatibility adapter while presentation moves to asynchronous repositories. */
class RoomLocalStorage(
    private val dataDir: File,
    private val logger: AppLogger,
    private val database: KioskDatabase,
    private val secretProtector: SecretProtector
) : LocalStorage {
    private val dao = database.kioskDao()
    private val mediaStore = FileMediaStore(dataDir, logger)
    private val updatesDir = File(dataDir, "updates")

    override fun close() = database.close()

    override fun clearPairing() = write("Could not clear pairings") {
        dao.deletePairedDevices(KioskDao.PAIR_KIND_EMITTER)
        dao.deletePairedDevices(KioskDao.PAIR_KIND_RECEIVER)
    }

    override fun clearReceiverData() = write("Could not clear Screen data") {
        dao.clearReceiverData()
        mediaStore.clear()
        File(dataDir, "playlist.json.tmp").delete()
        File(dataDir, "lock.json.tmp").delete()
        File(dataDir, "skipped-update.txt").delete()
        updatesDir.deleteRecursively()
    }

    override fun loadReceiverPairedInfo(): ReceiverPairedInfo? = readOrNull("Could not load receiver pairing") {
        dao.getPairedDevices(KioskDao.PAIR_KIND_RECEIVER).firstOrNull()?.let { entity ->
            ReceiverPairedInfo(
                emitterId = entity.deviceId,
                emitterName = entity.deviceName,
                token = secretProtector.unprotect(entity.tokenCiphertext)
            )
        }
    }

    override fun saveReceiverPairedInfo(info: ReceiverPairedInfo) = write("Could not save receiver pairing") {
        dao.deletePairedDevices(KioskDao.PAIR_KIND_RECEIVER)
        dao.upsertPairedDevice(
            PairedDeviceEntity(
                kind = KioskDao.PAIR_KIND_RECEIVER,
                deviceId = info.emitterId,
                deviceName = info.emitterName,
                tokenCiphertext = secretProtector.protect(info.token),
                host = null,
                port = null
            )
        )
    }

    override fun loadEmitterPairedDevices(): List<EmitterPairedDevice> = readOrDefault(
        "Could not load paired Screens",
        emptyList()
    ) {
        dao.getPairedDevices(KioskDao.PAIR_KIND_EMITTER).map { entity ->
            EmitterPairedDevice(
                deviceId = entity.deviceId,
                deviceName = entity.deviceName,
                token = secretProtector.unprotect(entity.tokenCiphertext),
                host = entity.host,
                port = entity.port
            )
        }
    }

    override fun saveEmitterPairedDevices(devices: List<EmitterPairedDevice>) = write("Could not save paired Screens") {
        dao.deletePairedDevices(KioskDao.PAIR_KIND_EMITTER)
        dao.upsertPairedDevices(devices.map(::toPairedEntity))
    }

    override fun addEmitterPairedDevice(device: EmitterPairedDevice) = write("Could not add paired Screen") {
        dao.upsertPairedDevice(toPairedEntity(device))
    }

    override fun removeEmitterPairedDevice(deviceId: String) = write("Could not remove paired Screen") {
        dao.deletePairedDevice(KioskDao.PAIR_KIND_EMITTER, deviceId)
    }

    override fun loadPlaylist(): Playlist? = readOrNull("Could not load active playlist") {
        dao.getPlaylist(KioskDao.RECEIVER_PLAYLIST_KEY)?.let { toPlaylist(it) }
    }

    override fun savePlaylist(playlist: Playlist) = write("Could not save active playlist") {
        val entity = playlist.toEntity(
            playlistKey = KioskDao.RECEIVER_PLAYLIST_KEY,
            owner = KioskDao.OWNER_RECEIVER,
            position = 0,
            active = true
        )
        dao.savePlaylist(entity, playlist.toItemEntities(entity.playlistKey))
    }

    override fun loadPlaylistLibrary(): PlaylistLibrary? = readOrNull("Could not load playlist library") {
        val rows = dao.getPlaylists(KioskDao.OWNER_EMITTER)
        if (rows.isEmpty()) return@readOrNull null
        val playlists = rows.map { toPlaylist(it) }
        PlaylistLibrary(
            playlists = playlists,
            activePlaylistId = rows.firstOrNull { it.active }?.domainId ?: playlists.firstOrNull()?.id
        )
    }

    override fun savePlaylistLibrary(library: PlaylistLibrary) = write("Could not save playlist library") {
        val activeId = library.activePlaylistId ?: library.playlists.firstOrNull()?.id
        val entities = library.playlists.mapIndexed { index, playlist ->
            playlist.toEntity(
                playlistKey = playlist.id,
                owner = KioskDao.OWNER_EMITTER,
                position = index,
                active = playlist.id == activeId
            )
        }
        val items = library.playlists.flatMap { playlist -> playlist.toItemEntities(playlist.id) }
        dao.savePlaylistLibrary(entities, items)
    }

    override fun loadKioskLock(): KioskLockConfig? = readOrNull("Could not load kiosk lock configuration") {
        dao.getKioskLock(KioskLockEntity.SINGLETON_ID)?.let(::toKioskLock)
    }

    override fun saveKioskLock(config: KioskLockConfig) = write("Could not save kiosk lock configuration") {
        dao.upsertKioskLock(config.toEntity())
    }

    override fun clearKioskLock() = write("Could not clear kiosk lock configuration") {
        dao.deleteKioskLock(KioskLockEntity.SINGLETON_ID)
    }

    override fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap? = mediaStore.loadBitmap(mediaId, fileName)

    override fun importMedia(sourcePath: String): MediaImportResult {
        val result = mediaStore.importMedia(sourcePath)
        if (result is MediaImportResult.Success) {
            val media = result.media
            try {
                blocking {
                    dao.upsertMediaItem(MediaEntity(media.mediaId, media.fileName, media.sha256, media.sizeBytes))
                }
            } catch (error: Exception) {
                logger.error("Could not save imported media metadata", error)
                mediaStore.delete(media.mediaId)
                return MediaImportResult.Failure(MediaImportError.UNREADABLE)
            }
        }
        return result
    }

    override fun getMediaBytes(mediaId: String, fileName: String): ByteArray? = mediaStore.getBytes(mediaId, fileName)

    override fun listStoredMedia(): List<StoredMedia> = readOrDefault("Could not list media", emptyList()) {
        dao.getMedia().mapNotNull { row ->
            if (!mediaStore.contains(row.mediaId)) {
                dao.deleteMedia(row.mediaId)
                null
            } else {
                StoredMedia(row.mediaId, row.fileName, row.sha256, row.sizeBytes)
            }
        }
    }

    override fun writeReceivedMedia(mediaId: String, format: ImageFormat, bytes: ByteArray): StoredMedia? {
        if (bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) return null
        val stored = mediaStore.writeReceived(mediaId, format, bytes) ?: return null
        val saved = readOrDefault("Could not save received media metadata", false) {
            dao.upsertMediaItem(MediaEntity(stored.mediaId, stored.fileName, stored.sha256, stored.sizeBytes))
            true
        }
        if (!saved) mediaStore.delete(mediaId)
        return stored.takeIf { saved }
    }

    override fun deleteStoredMedia(mediaId: String): Boolean {
        val deleted = mediaStore.delete(mediaId)
        write("Could not delete media metadata") { dao.deleteMedia(mediaId) }
        return deleted
    }

    private fun toPairedEntity(device: EmitterPairedDevice) = PairedDeviceEntity(
        kind = KioskDao.PAIR_KIND_EMITTER,
        deviceId = device.deviceId,
        deviceName = device.deviceName,
        tokenCiphertext = secretProtector.protect(device.token),
        host = device.host,
        port = device.port
    )

    private fun Playlist.toEntity(
        playlistKey: String,
        owner: String,
        position: Int,
        active: Boolean
    ) = PlaylistEntity(
        playlistKey = playlistKey,
        owner = owner,
        domainId = id,
        name = name,
        version = version,
        defaultDurationSec = defaultDurationSec,
        position = position,
        active = active
    )

    private fun Playlist.toItemEntities(playlistKey: String) = items.mapIndexed { index, item ->
        PlaylistItemEntity(playlistKey, index, item.mediaId, item.fileName, item.sha256, item.durationSec)
    }

    private suspend fun toPlaylist(entity: PlaylistEntity): Playlist {
        val items = dao.getPlaylistItems(entity.playlistKey).map { row ->
            PlaylistItem(row.mediaId, row.fileName, row.sha256, row.durationSec)
        }
        return Playlist(entity.domainId, entity.name, entity.version, entity.defaultDurationSec, items)
    }

    private fun KioskLockConfig.toEntity() = KioskLockEntity(
        passwordHashCiphertext = passwordHash?.let(secretProtector::protect),
        passwordLength = passwordLength,
        enabled = enabled,
        unlockSequence = unlockSequence.joinToString(","),
        recoveryHashCiphertext = recoveryHash?.let(secretProtector::protect),
        recoveryLength = recoveryLength
    )

    private fun toKioskLock(entity: KioskLockEntity) = KioskLockConfig(
        passwordHash = entity.passwordHashCiphertext?.let(secretProtector::unprotect),
        passwordLength = entity.passwordLength,
        enabled = entity.enabled,
        unlockSequence = entity.unlockSequence.split(',').filter(String::isNotBlank),
        recoveryHash = entity.recoveryHashCiphertext?.let(secretProtector::unprotect),
        recoveryLength = entity.recoveryLength
    )

    private fun <T> blocking(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    private fun <T> readOrNull(label: String, block: suspend () -> T?): T? = try {
        blocking(block)
    } catch (error: Exception) {
        logger.error(label, error)
        null
    }

    private fun <T> readOrDefault(label: String, fallback: T, block: suspend () -> T): T = try {
        blocking(block)
    } catch (error: Exception) {
        logger.error(label, error)
        fallback
    }

    private fun write(label: String, block: suspend () -> Unit) {
        try {
            blocking(block)
        } catch (error: Exception) {
            logger.error(label, error)
        }
    }
}
