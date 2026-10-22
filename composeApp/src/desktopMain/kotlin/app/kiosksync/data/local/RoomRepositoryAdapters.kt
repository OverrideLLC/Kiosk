package app.kiosksync.data.local

import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.pairing.domain.PairingRepository
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.domain.PlaylistLibrary
import app.kiosksync.feature.playlist.domain.PlaylistRepository
import app.kiosksync.platform.FileMediaStore
import app.kiosksync.platform.MediaImportError
import app.kiosksync.platform.MediaImportResult
import app.kiosksync.platform.MediaRepository
import app.kiosksync.platform.ReceiverDataResetter
import app.kiosksync.platform.StoredMedia
import app.kiosksync.platform.db.KioskDao
import app.kiosksync.platform.db.KioskLockEntity
import app.kiosksync.platform.db.MediaEntity
import app.kiosksync.platform.db.PairedDeviceEntity
import app.kiosksync.platform.db.PlaylistEntity
import app.kiosksync.platform.db.PlaylistItemEntity
import app.kiosksync.platform.secret.SecretProtector
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class RoomPairingRepository(
    private val dao: KioskDao,
    private val secretProtector: SecretProtector,
) : PairingRepository {
    override suspend fun loadPairedEmitter(): ReceiverPairedInfo? = withContext(Dispatchers.IO) {
        dao.getPairedDevices(KioskDao.PAIR_KIND_RECEIVER).firstOrNull()?.let { entity ->
            ReceiverPairedInfo(
                emitterId = entity.deviceId,
                emitterName = entity.deviceName,
                token = secretProtector.unprotect(entity.tokenCiphertext),
            )
        }
    }

    override suspend fun savePairedEmitter(info: ReceiverPairedInfo) = withContext(Dispatchers.IO) {
        dao.replacePairedDevices(
            KioskDao.PAIR_KIND_RECEIVER,
            listOf(info.toEntity(secretProtector)),
        )
    }

    override suspend fun clearPairedEmitter() = withContext(Dispatchers.IO) {
        dao.deletePairedDevices(KioskDao.PAIR_KIND_RECEIVER)
    }

    private fun ReceiverPairedInfo.toEntity(protector: SecretProtector) = PairedDeviceEntity(
        kind = KioskDao.PAIR_KIND_RECEIVER,
        deviceId = emitterId,
        deviceName = emitterName,
        tokenCiphertext = protector.protect(token),
        host = null,
        port = null,
    )
}

internal class RoomEmitterScreensRepository(
    private val dao: KioskDao,
    private val secretProtector: SecretProtector,
) : EmitterScreensRepository {
    override suspend fun loadPairedScreens(): List<EmitterPairedDevice> = withContext(Dispatchers.IO) {
        dao.getPairedDevices(KioskDao.PAIR_KIND_EMITTER).map { entity ->
            EmitterPairedDevice(
                deviceId = entity.deviceId,
                deviceName = entity.deviceName,
                token = secretProtector.unprotect(entity.tokenCiphertext),
                host = entity.host,
                port = entity.port,
            )
        }
    }

    override suspend fun savePairedScreens(devices: List<EmitterPairedDevice>) = withContext(Dispatchers.IO) {
        dao.replacePairedDevices(KioskDao.PAIR_KIND_EMITTER, devices.map(::toEntity))
    }

    override suspend fun addPairedScreen(device: EmitterPairedDevice) = withContext(Dispatchers.IO) {
        dao.upsertPairedDevice(toEntity(device))
    }

    override suspend fun removePairedScreen(deviceId: String) = withContext(Dispatchers.IO) {
        dao.deletePairedDevice(KioskDao.PAIR_KIND_EMITTER, deviceId)
    }

    override suspend fun clearPairedScreens() = withContext(Dispatchers.IO) {
        dao.deletePairedDevices(KioskDao.PAIR_KIND_EMITTER)
    }

    private fun toEntity(device: EmitterPairedDevice) = PairedDeviceEntity(
        kind = KioskDao.PAIR_KIND_EMITTER,
        deviceId = device.deviceId,
        deviceName = device.deviceName,
        tokenCiphertext = secretProtector.protect(device.token),
        host = device.host,
        port = device.port,
    )
}

internal class RoomPlaylistRepository(private val dao: KioskDao) : PlaylistRepository {
    override fun observeReceiverPlaylist(): Flow<Playlist?> = combine(
        dao.observePlaylists(KioskDao.OWNER_RECEIVER),
        dao.observePlaylistItems(KioskDao.RECEIVER_PLAYLIST_KEY),
    ) { playlists, items ->
        playlists.firstOrNull { it.playlistKey == KioskDao.RECEIVER_PLAYLIST_KEY }
            ?.let { it.toPlaylist(items) }
    }

    override fun observeEmitterPlaylistLibrary(): Flow<PlaylistLibrary?> = combine(
        dao.observePlaylists(KioskDao.OWNER_EMITTER),
        dao.observeAllPlaylistItems(),
    ) { entities, items ->
        entities.toPlaylistLibrary(items)
    }

    override suspend fun loadReceiverPlaylist(): Playlist? = withContext(Dispatchers.IO) {
        dao.getPlaylist(KioskDao.RECEIVER_PLAYLIST_KEY)?.let { entity ->
            entity.toPlaylist(dao.getPlaylistItems(entity.playlistKey))
        }
    }

    override suspend fun saveReceiverPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        val entity = playlist.toEntity(
            playlistKey = KioskDao.RECEIVER_PLAYLIST_KEY,
            owner = KioskDao.OWNER_RECEIVER,
            position = 0,
            active = true,
        )
        dao.savePlaylist(entity, playlist.toItemEntities(entity.playlistKey))
    }

    override suspend fun loadEmitterPlaylistLibrary(): PlaylistLibrary? = withContext(Dispatchers.IO) {
        val entities = dao.getPlaylists(KioskDao.OWNER_EMITTER)
        val rows = entities.flatMap { entity -> dao.getPlaylistItems(entity.playlistKey) }
        entities.toPlaylistLibrary(rows)
    }

    override suspend fun saveEmitterPlaylistLibrary(library: PlaylistLibrary) = withContext(Dispatchers.IO) {
        val activeId = library.activePlaylistId ?: library.playlists.firstOrNull()?.id
        val entities = library.playlists.mapIndexed { index, playlist ->
            playlist.toEntity(
                playlistKey = playlist.id,
                owner = KioskDao.OWNER_EMITTER,
                position = index,
                active = playlist.id == activeId,
            )
        }
        val items = library.playlists.flatMap { playlist -> playlist.toItemEntities(playlist.id) }
        dao.savePlaylistLibrary(entities, items)
    }

    private fun Playlist.toEntity(
        playlistKey: String,
        owner: String,
        position: Int,
        active: Boolean,
    ) = PlaylistEntity(
        playlistKey = playlistKey,
        owner = owner,
        domainId = id,
        name = name,
        version = version,
        defaultDurationSec = defaultDurationSec,
        position = position,
        active = active,
    )

    private fun Playlist.toItemEntities(playlistKey: String) = items.mapIndexed { index, item ->
        PlaylistItemEntity(playlistKey, index, item.mediaId, item.fileName, item.sha256, item.durationSec)
    }

    private fun PlaylistEntity.toPlaylist(rows: List<PlaylistItemEntity>) = Playlist(
        id = domainId,
        name = name,
        version = version,
        defaultDurationSec = defaultDurationSec,
        items = rows.asSequence()
            .filter { it.playlistKey == playlistKey }
            .sortedBy(PlaylistItemEntity::position)
            .map { PlaylistItem(it.mediaId, it.fileName, it.sha256, it.durationSec) }
            .toList(),
    )

    private fun List<PlaylistEntity>.toPlaylistLibrary(items: List<PlaylistItemEntity>): PlaylistLibrary? {
        if (isEmpty()) return null
        val playlists = sortedBy(PlaylistEntity::position).map { entity -> entity.toPlaylist(items) }
        val activeId = firstOrNull(PlaylistEntity::active)?.domainId ?: playlists.firstOrNull()?.id
        return PlaylistLibrary(playlists, activeId)
    }
}

internal class RoomKioskLockRepository(
    private val dao: KioskDao,
    private val secretProtector: SecretProtector,
) : KioskLockRepository {
    override val config: Flow<KioskLockConfig?> = dao.observeKioskLock(KioskLockEntity.SINGLETON_ID)
        .map { entity -> entity?.toDomain(secretProtector) }

    override suspend fun load(): KioskLockConfig? = withContext(Dispatchers.IO) {
        dao.getKioskLock(KioskLockEntity.SINGLETON_ID)?.toDomain(secretProtector)
    }

    override suspend fun save(config: KioskLockConfig) = withContext(Dispatchers.IO) {
        dao.upsertKioskLock(config.toEntity(secretProtector))
    }

    override suspend fun clearLock() = withContext(Dispatchers.IO) {
        dao.deleteKioskLock(KioskLockEntity.SINGLETON_ID)
    }

    private fun KioskLockConfig.toEntity(protector: SecretProtector) = KioskLockEntity(
        passwordHashCiphertext = passwordHash?.let(protector::protect),
        passwordLength = passwordLength,
        enabled = enabled,
        unlockSequence = unlockSequence.joinToString(","),
        recoveryHashCiphertext = recoveryHash?.let(protector::protect),
        recoveryLength = recoveryLength,
    )

    private fun KioskLockEntity.toDomain(protector: SecretProtector) = KioskLockConfig(
        passwordHash = passwordHashCiphertext?.let(protector::unprotect),
        passwordLength = passwordLength,
        enabled = enabled,
        unlockSequence = unlockSequence.split(',').filter(String::isNotBlank),
        recoveryHash = recoveryHashCiphertext?.let(protector::unprotect),
        recoveryLength = recoveryLength,
    )
}

internal class RoomMediaRepository(
    private val dao: KioskDao,
    private val mediaStore: FileMediaStore,
    private val logger: AppLogger,
) : MediaRepository {
    override suspend fun loadBitmap(mediaId: String, fileName: String) = withContext(Dispatchers.IO) {
        mediaStore.loadBitmap(mediaId, fileName)
    }

    override suspend fun import(sourcePath: String): MediaImportResult = withContext(Dispatchers.IO) {
        val result = mediaStore.importMedia(sourcePath)
        if (result !is MediaImportResult.Success) return@withContext result
        try {
            dao.upsertMediaItem(result.media.toEntity())
            result
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logger.error("Could not save imported media metadata", error)
            mediaStore.delete(result.media.mediaId)
            MediaImportResult.Failure(MediaImportError.UNREADABLE)
        }
    }

    override suspend fun getBytes(mediaId: String, fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        mediaStore.getBytes(mediaId, fileName)
    }

    override suspend fun listStored(): List<StoredMedia> = withContext(Dispatchers.IO) {
        val files = mediaStore.listFiles().associateBy(StoredMedia::mediaId)
        dao.getMedia().mapNotNull { entity ->
            files[entity.mediaId]?.let { StoredMedia(entity.mediaId, entity.fileName, entity.sha256, entity.sizeBytes) }
                ?: run {
                    dao.deleteMedia(entity.mediaId)
                    null
                }
        }
    }

    override suspend fun writeReceived(
        mediaId: String,
        format: ImageFormat,
        bytes: ByteArray,
    ): StoredMedia? = withContext(Dispatchers.IO) {
        if (bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) return@withContext null
        val stored = mediaStore.writeReceived(mediaId, format, bytes) ?: return@withContext null
        try {
            dao.upsertMediaItem(stored.toEntity())
            stored
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            logger.error("Could not save received media metadata", error)
            mediaStore.delete(mediaId)
            null
        }
    }

    override suspend fun delete(mediaId: String): Boolean = withContext(Dispatchers.IO) {
        val deleted = mediaStore.delete(mediaId)
        dao.deleteMedia(mediaId)
        deleted
    }

    override suspend fun clearMedia() = withContext(Dispatchers.IO) {
        mediaStore.clear()
        dao.deleteAllMedia()
    }

    private fun StoredMedia.toEntity() = MediaEntity(mediaId, fileName, sha256, sizeBytes)
}

internal class RoomReceiverDataResetter(
    private val dao: KioskDao,
    private val mediaStore: FileMediaStore,
    private val dataDir: File,
) : ReceiverDataResetter {
    override suspend fun clearReceiverData() = withContext(Dispatchers.IO) {
        dao.clearReceiverData()
        mediaStore.clear()
        File(dataDir, "playlist.json.tmp").delete()
        File(dataDir, "lock.json.tmp").delete()
        File(dataDir, "skipped-update.txt").delete()
        File(dataDir, "updates").deleteRecursively()
        Unit
    }
}

