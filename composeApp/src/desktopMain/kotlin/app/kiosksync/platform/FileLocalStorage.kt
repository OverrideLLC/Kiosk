package app.kiosksync.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.role.AppConfig
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import java.io.File

class FileLocalStorage(
    private val dataDir: File,
    private val logger: AppLogger
) : LocalStorage {

    private val configFile = File(dataDir, "config.json")
    private val pairedFile = File(dataDir, "paired.json")
    private val playlistFile = File(dataDir, "playlist.json")
    private val mediaDir = File(dataDir, "media")

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
            playlistFile.writeText(content, Charsets.UTF_8)
            logger.info("Playlist saved with version ${playlist.version} (${playlist.items.size} items)")
        } catch (e: Exception) {
            logger.error("Error saving playlist.json", e)
        }
    }

    override fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val validExtensions = setOf("jpg", "jpeg", "png", "webp")
        if (extension.isNotEmpty() && extension !in validExtensions) {
            logger.error("Unsupported image format for file '$fileName'. Supported: JPG, PNG, WEBP.")
            return null
        }

        // Try candidate file names in mediaDir
        val candidates = listOf(
            File(mediaDir, if (extension.isNotEmpty()) "$mediaId.$extension" else mediaId),
            File(mediaDir, fileName),
            File(mediaDir, mediaId)
        )

        val targetFile = candidates.firstOrNull { it.exists() }
            ?: mediaDir.listFiles()?.firstOrNull { it.name.startsWith(mediaId) }

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
}
