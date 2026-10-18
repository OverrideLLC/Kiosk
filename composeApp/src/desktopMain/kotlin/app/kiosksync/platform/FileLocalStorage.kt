package app.kiosksync.platform

import app.kiosksync.core.AppLogger
import app.kiosksync.feature.role.AppConfig
import kotlinx.serialization.json.Json
import java.io.File

class FileLocalStorage(
    private val dataDir: File,
    private val logger: AppLogger
) : LocalStorage {

    private val configFile = File(dataDir, "config.json")
    private val pairedFile = File(dataDir, "paired.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        dataDir.mkdirs()
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
}
