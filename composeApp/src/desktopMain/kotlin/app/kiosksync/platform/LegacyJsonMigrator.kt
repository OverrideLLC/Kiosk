package app.kiosksync.platform

import app.kiosksync.core.AppLogger
import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistLibrary
import app.kiosksync.platform.db.KioskDao
import app.kiosksync.platform.db.KioskDatabase
import app.kiosksync.platform.db.KioskLockEntity
import app.kiosksync.platform.db.LegacyRoomData
import app.kiosksync.platform.db.MediaEntity
import app.kiosksync.platform.db.PairedDeviceEntity
import app.kiosksync.platform.db.PlaylistEntity
import app.kiosksync.platform.db.PlaylistItemEntity
import app.kiosksync.platform.secret.SecretProtector
import java.io.File
import java.io.IOException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Imports the old structured JSON as one Room transaction and keeps recoverable backups. */
class LegacyJsonMigrator(
    private val dataDir: File,
    private val database: KioskDatabase,
    private val mediaStore: FileMediaStore,
    private val secretProtector: SecretProtector,
    private val logger: AppLogger
) {
    private val dao = database.kioskDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun migrate() {
        if (dao.getMetadata(KioskDao.LEGACY_MIGRATION_KEY) == KioskDao.LEGACY_MIGRATION_COMPLETE) {
            deleteLegacySources()
            return
        }

        val legacyFiles = LEGACY_FILE_NAMES.map { File(dataDir, it) }.filter(File::isFile)
        legacyFiles.forEach(::backup)

        val playlists = mutableListOf<PlaylistEntity>()
        val items = mutableListOf<PlaylistItemEntity>()

        File(dataDir, "playlist.json").takeIf(File::isFile)?.let { file ->
            val playlist = decode<Playlist>(file)
            appendPlaylist(
                playlist = playlist,
                playlistKey = KioskDao.RECEIVER_PLAYLIST_KEY,
                owner = KioskDao.OWNER_RECEIVER,
                position = 0,
                active = true,
                playlists = playlists,
                items = items
            )
        }

        File(dataDir, "playlists.json").takeIf(File::isFile)?.let { file ->
            val library = decode<PlaylistLibrary>(file)
            val activeId = library.activePlaylistId ?: library.playlists.firstOrNull()?.id
            library.playlists.forEachIndexed { index, playlist ->
                appendPlaylist(
                    playlist = playlist,
                    playlistKey = playlist.id,
                    owner = KioskDao.OWNER_EMITTER,
                    position = index,
                    active = playlist.id == activeId,
                    playlists = playlists,
                    items = items
                )
            }
        }

        val pairedDevices = File(dataDir, "paired.json").takeIf(File::isFile)?.let(::decodePairing).orEmpty()
        val kioskLock = File(dataDir, "lock.json").takeIf(File::isFile)?.let { file ->
            decode<KioskLockConfig>(file).toEntity()
        }
        val media = mediaStore.listFiles().map { MediaEntity(it.mediaId, it.fileName, it.sha256, it.sizeBytes) }

        // Import is idempotent. Keep the marker separate so a failed source cleanup can be
        // retried without leaving an unencrypted legacy secret beside the database.
        dao.importLegacy(LegacyRoomData(playlists, items, pairedDevices, kioskLock, media))
        deleteLegacySources()
        dao.completeLegacyMigration()
        logger.info("Legacy persistence imported into Room (${legacyFiles.size} JSON file(s), ${media.size} media row(s))")
    }

    private fun deleteLegacySources() {
        LEGACY_FILE_NAMES.map { File(dataDir, it) }.filter(File::isFile).forEach { file ->
            if (!file.delete()) throw IOException("Could not remove plaintext legacy file '${file.name}' after backup")
        }
    }

    private fun decodePairing(file: File): List<PairedDeviceEntity> {
        val contents = file.readText(Charsets.UTF_8).trim()
        return when {
            contents.startsWith("[") -> json.decodeFromString(ListSerializer(EmitterPairedDevice.serializer()), contents)
                .map(::toEntity)
            contents.startsWith("{") -> listOf(json.decodeFromString<ReceiverPairedInfo>(contents).toEntity())
            else -> error("paired.json has an unsupported format")
        }
    }

    private fun toEntity(device: EmitterPairedDevice) = PairedDeviceEntity(
        kind = KioskDao.PAIR_KIND_EMITTER,
        deviceId = device.deviceId,
        deviceName = device.deviceName,
        tokenCiphertext = secretProtector.protect(device.token),
        host = device.host,
        port = device.port
    )

    private fun ReceiverPairedInfo.toEntity() = PairedDeviceEntity(
        kind = KioskDao.PAIR_KIND_RECEIVER,
        deviceId = emitterId,
        deviceName = emitterName,
        tokenCiphertext = secretProtector.protect(token),
        host = null,
        port = null
    )

    private fun KioskLockConfig.toEntity() = KioskLockEntity(
        passwordHashCiphertext = passwordHash?.let(secretProtector::protect),
        passwordLength = passwordLength,
        enabled = enabled,
        unlockSequence = unlockSequence.joinToString(","),
        recoveryHashCiphertext = recoveryHash?.let(secretProtector::protect),
        recoveryLength = recoveryLength
    )

    private fun appendPlaylist(
        playlist: Playlist,
        playlistKey: String,
        owner: String,
        position: Int,
        active: Boolean,
        playlists: MutableList<PlaylistEntity>,
        items: MutableList<PlaylistItemEntity>
    ) {
        playlists += PlaylistEntity(
            playlistKey = playlistKey,
            owner = owner,
            domainId = playlist.id,
            name = playlist.name,
            version = playlist.version,
            defaultDurationSec = playlist.defaultDurationSec,
            position = position,
            active = active
        )
        playlist.items.forEachIndexed { index, item ->
            items += PlaylistItemEntity(
                playlistKey = playlistKey,
                position = index,
                mediaId = item.mediaId,
                fileName = item.fileName,
                sha256 = item.sha256,
                durationSec = item.durationSec
            )
        }
    }

    private inline fun <reified T> decode(file: File): T = try {
        json.decodeFromString<T>(file.readText(Charsets.UTF_8))
    } catch (error: Exception) {
        throw IllegalArgumentException("Could not parse ${file.name}; original data was preserved", error)
    }

    private fun backup(source: File) {
        val legacyDir = File(dataDir, "legacy").apply { mkdirs() }
        val secretFile = source.name in SECRET_FILE_NAMES
        val target = File(legacyDir, "${source.name}.room-v1${if (secretFile) ".dpapi" else ""}")
        if (target.exists()) return
        if (secretFile) {
            target.writeText(secretProtector.protect(source.readText(Charsets.UTF_8)), Charsets.UTF_8)
        } else {
            source.copyTo(target, overwrite = false)
        }
    }

    private companion object {
        val LEGACY_FILE_NAMES = listOf("paired.json", "playlist.json", "playlists.json", "lock.json")
        val SECRET_FILE_NAMES = setOf("paired.json", "lock.json")
    }
}
