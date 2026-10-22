package app.kiosksync.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.playlist.PlaylistLibrary
import app.kiosksync.feature.role.AppConfig
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class FileLocalStorage(
    private val dataDir: File,
    private val logger: AppLogger
) : LocalStorage {

    private val configFile = File(dataDir, "config.json")
    private val pairedFile = File(dataDir, "paired.json")
    private val playlistFile = File(dataDir, "playlist.json")
    private val playlistsFile = File(dataDir, "playlists.json")
    private val lockFile = File(dataDir, "lock.json")
    private val mediaDir = File(dataDir, "media")
    private val skippedUpdateFile = File(dataDir, "skipped-update.txt")
    private val updatesDir = File(dataDir, "updates")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        dataDir.mkdirs()
        mediaDir.mkdirs()
    }

    override fun loadConfig(): AppConfig? {
        if (!configFile.exists()) {
            return null
        }
        return try {
            val content = configFile.readText(Charsets.UTF_8)
            json.decodeFromString<AppConfig>(content)
        } catch (e: Exception) {
            logger.error("Error reading config.json, will reconfigure", e)
            null
        }
    }

    override fun saveConfig(config: AppConfig) {
        try {
            dataDir.mkdirs()
            val content = json.encodeToString(AppConfig.serializer(), config)
            configFile.writeText(content, Charsets.UTF_8)
            logger.info("Configuration saved successfully to ${configFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Error saving config.json", e)
        }
    }

    override fun clearConfig() {
        try {
            if (configFile.exists()) {
                configFile.delete()
                logger.info("Deleted config.json")
            }
            clearPairing()
            clearKioskLock()
        } catch (e: Exception) {
            logger.error("Error clearing configuration", e)
        }
    }

    override fun clearPairing() {
        try {
            if (pairedFile.exists()) {
                pairedFile.delete()
                logger.info("Deleted paired.json")
            }
        } catch (e: Exception) {
            logger.error("Error clearing pairing", e)
        }
    }

    override fun clearReceiverData() {
        try {
            deleteFile(playlistFile)
            deleteFile(File(dataDir, "playlist.json.tmp"))
            deleteFile(lockFile)
            deleteFile(File(dataDir, "lock.json.tmp"))
            deleteFile(pairedFile)
            deleteFile(skippedUpdateFile)
            if (updatesDir.exists()) {
                updatesDir.deleteRecursively()
                logger.info("Removed staged update files")
            }
            val mediaCount = mediaDir.listFiles()?.count { it.isFile && it.delete() } ?: 0
            logger.info(
                "Receiver data cleared: content, password and pairing " +
                    "($mediaCount media file(s) removed)"
            )
        } catch (e: Exception) {
            logger.error("Error clearing receiver data", e)
        }
    }

    private fun deleteFile(file: File) {
        if (!file.exists()) return
        if (file.delete()) {
            logger.info("Deleted ${file.name}")
        } else {
            logger.warn("Could not delete ${file.name}")
        }
    }

    override fun loadReceiverPairedInfo(): app.kiosksync.core.ReceiverPairedInfo? {
        if (!pairedFile.exists()) return null
        return try {
            val content = pairedFile.readText(Charsets.UTF_8).trim()
            if (content.startsWith("{")) {
                json.decodeFromString<app.kiosksync.core.ReceiverPairedInfo>(content)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Error loading receiver paired.json", e)
            null
        }
    }

    override fun saveReceiverPairedInfo(info: app.kiosksync.core.ReceiverPairedInfo) {
        try {
            dataDir.mkdirs()
            val content = json.encodeToString(app.kiosksync.core.ReceiverPairedInfo.serializer(), info)
            pairedFile.writeText(content, Charsets.UTF_8)
            logger.info("Saved receiver pairing to ${pairedFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Error saving receiver paired.json", e)
        }
    }

    override fun loadEmitterPairedDevices(): List<app.kiosksync.core.EmitterPairedDevice> {
        if (!pairedFile.exists()) return emptyList()
        return try {
            val content = pairedFile.readText(Charsets.UTF_8).trim()
            if (content.startsWith("[")) {
                json.decodeFromString<List<app.kiosksync.core.EmitterPairedDevice>>(content)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error loading emitter paired.json", e)
            emptyList()
        }
    }

    override fun saveEmitterPairedDevices(devices: List<app.kiosksync.core.EmitterPairedDevice>) {
        try {
            dataDir.mkdirs()
            val content = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(app.kiosksync.core.EmitterPairedDevice.serializer()),
                devices
            )
            pairedFile.writeText(content, Charsets.UTF_8)
            logger.info("Saved emitter paired devices list (${devices.size} items)")
        } catch (e: Exception) {
            logger.error("Error saving emitter paired devices", e)
        }
    }

    override fun addEmitterPairedDevice(device: app.kiosksync.core.EmitterPairedDevice) {
        val current = loadEmitterPairedDevices().filterNot { it.deviceId == device.deviceId }
        saveEmitterPairedDevices(current + device)
    }

    override fun removeEmitterPairedDevice(deviceId: String) {
        val current = loadEmitterPairedDevices().filterNot { it.deviceId == deviceId }
        saveEmitterPairedDevices(current)
    }

    override fun loadPlaylist(): Playlist? {
        if (!playlistFile.exists()) {
            return null
        }
        return try {
            val content = playlistFile.readText(Charsets.UTF_8)
            json.decodeFromString<Playlist>(content)
        } catch (e: Exception) {
            logger.error("Error reading playlist.json", e)
            null
        }
    }

    override fun savePlaylist(playlist: Playlist) {
        try {
            dataDir.mkdirs()
            val content = json.encodeToString(Playlist.serializer(), playlist)
            val tempFile = File(dataDir, "playlist.json.tmp")
            tempFile.writeText(content, Charsets.UTF_8)
            try {
                Files.move(
                    tempFile.toPath(),
                    playlistFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                // Some filesystems do not support atomic moves. Replace in place instead.
                Files.move(
                    tempFile.toPath(),
                    playlistFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            logger.info("Playlist saved with version ${playlist.version} (${playlist.items.size} items)")
        } catch (e: Exception) {
            logger.error("Error saving playlist.json", e)
        }
    }

    override fun loadPlaylistLibrary(): PlaylistLibrary? {
        if (!playlistsFile.exists()) return null
        return try {
            json.decodeFromString<PlaylistLibrary>(playlistsFile.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            logger.error("Error reading playlists.json", e)
            null
        }
    }

    override fun savePlaylistLibrary(library: PlaylistLibrary) {
        try {
            dataDir.mkdirs()
            val content = json.encodeToString(PlaylistLibrary.serializer(), library)
            writeAtomically(playlistsFile, content)
            logger.info(
                "Playlist library saved with ${library.playlists.size} playlists " +
                    "(active='${library.activePlaylist()?.name.orEmpty()}')"
            )
        } catch (e: Exception) {
            logger.error("Error saving playlists.json", e)
        }
    }

    override fun loadKioskLock(): KioskLockConfig? {
        if (!lockFile.exists()) return null
        return try {
            json.decodeFromString<KioskLockConfig>(lockFile.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            logger.error("Error reading lock.json", e)
            null
        }
    }

    override fun saveKioskLock(config: KioskLockConfig) {
        try {
            dataDir.mkdirs()
            writeAtomically(lockFile, json.encodeToString(KioskLockConfig.serializer(), config))
            logger.info("Kiosk lock configuration saved")
        } catch (e: Exception) {
            logger.error("Error saving lock.json", e)
        }
    }

    override fun clearKioskLock() {
        try {
            if (lockFile.exists()) {
                lockFile.delete()
                logger.info("Deleted lock.json")
            }
        } catch (e: Exception) {
            logger.error("Error deleting lock.json", e)
        }
    }

    override fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap? {
        val targetFile = findMediaFile(mediaId, fileName)
        if (targetFile == null || !targetFile.exists()) {
            logger.error("Media file not found for mediaId='$mediaId', fileName='$fileName'")
            return null
        }

        if (targetFile.length() > MAX_IMAGE_SIZE_BYTES) {
            logger.error("Media file ${targetFile.name} exceeds max allowed size of 25 MB (${targetFile.length()} bytes)")
            return null
        }

        return try {
            val bytes = targetFile.readBytes()
            val skiaImage = Image.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            logger.error("Exception decoding image file ${targetFile.name}", e)
            null
        }
    }

    override fun importMedia(sourcePath: String): MediaImportResult {
        val source = File(sourcePath)
        if (!source.isFile || !source.canRead()) {
            logger.warn("Cannot read selected image '$sourcePath'")
            return MediaImportResult.Failure(MediaImportError.UNREADABLE)
        }
        if (source.length() > MAX_IMAGE_SIZE_BYTES) {
            logger.warn("Selected image '$sourcePath' exceeds 25 MB")
            return MediaImportResult.Failure(MediaImportError.TOO_LARGE)
        }

        val bytes = try {
            source.readBytes()
        } catch (e: Exception) {
            logger.error("Error reading selected image '$sourcePath'", e)
            return MediaImportResult.Failure(MediaImportError.UNREADABLE)
        }
        if (bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) {
            return MediaImportResult.Failure(MediaImportError.TOO_LARGE)
        }

        val format = ImageFormat.detect(bytes)
        if (format == null) {
            logger.warn("Selected file '$sourcePath' is not JPG, PNG or WEBP")
            return MediaImportResult.Failure(MediaImportError.UNSUPPORTED_FORMAT)
        }

        val mediaId = UUID.randomUUID().toString()
        val target = writeMediaFile(mediaId, format, bytes)
            ?: return MediaImportResult.Failure(MediaImportError.UNREADABLE)

        return MediaImportResult.Success(
            StoredMedia(
                mediaId = mediaId,
                fileName = source.name,
                sha256 = Sha256.hex(bytes),
                sizeBytes = target.length()
            )
        )
    }

    override fun getMediaBytes(mediaId: String, fileName: String): ByteArray? {
        val file = findMediaFile(mediaId, fileName)
        if (file == null || !file.isFile) {
            logger.warn("Cannot read local media for mediaId='$mediaId'")
            return null
        }
        if (file.length() > MAX_IMAGE_SIZE_BYTES) {
            logger.warn("Local media '${file.name}' exceeds 25 MB")
            return null
        }
        return try {
            file.readBytes()
        } catch (e: Exception) {
            logger.error("Error reading local media '${file.name}'", e)
            null
        }
    }

    override fun listStoredMedia(): List<StoredMedia> {
        val files = mediaDir.listFiles() ?: return emptyList()
        return files.mapNotNull { file ->
            if (!file.isFile) return@mapNotNull null
            val extension = file.extension.lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) return@mapNotNull null
            val mediaId = file.nameWithoutExtension
            if (mediaId.isBlank()) return@mapNotNull null
            val hash = try {
                Sha256.hex(file)
            } catch (e: Exception) {
                logger.error("Error hashing media file '${file.name}'", e)
                return@mapNotNull null
            }
            StoredMedia(
                mediaId = mediaId,
                fileName = file.name,
                sha256 = hash,
                sizeBytes = file.length()
            )
        }
    }

    override fun writeReceivedMedia(mediaId: String, format: ImageFormat, bytes: ByteArray): StoredMedia? {
        val target = writeMediaFile(mediaId, format, bytes) ?: return null
        return StoredMedia(
            mediaId = mediaId,
            fileName = target.name,
            sha256 = Sha256.hex(bytes),
            sizeBytes = target.length()
        )
    }

    override fun deleteStoredMedia(mediaId: String): Boolean {
        val files = mediaDir.listFiles() ?: return false
        var deleted = false
        files.filter { it.isFile && it.nameWithoutExtension == mediaId }.forEach { file ->
            if (file.delete()) {
                deleted = true
                logger.info("Deleted orphan media '${file.name}'")
            } else {
                logger.warn("Could not delete orphan media '${file.name}'")
            }
        }
        return deleted
    }

    private fun writeAtomically(target: File, content: String) {
        val tempFile = File(target.parentFile, "${target.name}.tmp")
        tempFile.writeText(content, Charsets.UTF_8)
        try {
            Files.move(
                tempFile.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(
                tempFile.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun findMediaFile(mediaId: String, fileName: String): File? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val safeFileName = fileName.takeIf {
            it.isNotBlank() &&
                it != "." &&
                it != ".." &&
                it.none { char -> char in "/\\:*?\"<>|" || char.code < 32 }
        }
        val candidates = buildList {
            if (extension in SUPPORTED_EXTENSIONS) {
                add(File(mediaDir, "$mediaId.$extension"))
            }
            if (safeFileName != null) {
                add(File(mediaDir, safeFileName))
            }
            add(File(mediaDir, mediaId))
        }
        candidates.firstOrNull { it.isFile }?.let { return it }

        return mediaDir.listFiles()?.firstOrNull {
            it.isFile && it.nameWithoutExtension == mediaId
        }
    }

    private fun writeMediaFile(mediaId: String, format: ImageFormat, bytes: ByteArray): File? {
        val target = File(mediaDir, "$mediaId.${format.extension}")
        val temp = File(mediaDir, "$mediaId.${format.extension}.part")
        return try {
            mediaDir.mkdirs()
            temp.writeBytes(bytes)
            mediaDir.listFiles()
                ?.filter { it.isFile && it.nameWithoutExtension == mediaId && it != temp }
                ?.forEach { it.delete() }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                if (!temp.renameTo(target)) {
                    throw IllegalStateException("Could not move temporary media file into place")
                }
            }
            target
        } catch (e: Exception) {
            logger.error("Error saving media file for mediaId='$mediaId'", e)
            runCatching { temp.delete() }
            null
        }
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
