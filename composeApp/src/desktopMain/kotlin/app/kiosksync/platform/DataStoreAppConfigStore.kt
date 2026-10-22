package app.kiosksync.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.feature.role.AppConfig
import app.kiosksync.feature.role.AppConfigStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Single-process Preferences DataStore owned by the desktop application bootstrap. */
class DataStoreAppConfigStore(
    private val dataDir: File,
    private val logger: AppLogger,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AppConfigStore {
    private val legacyConfigFile = File(dataDir, "config.json")
    private val legacyBackupFile = File(dataDir, "legacy/config.json")
    private val migrationMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val dataStore: DataStore<Preferences>

    init {
        dataDir.mkdirs()
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(dataDir, DATA_STORE_FILE_NAME) }
        )
    }

    override val config: Flow<AppConfig?> = dataStore.data.map(::decodeConfig)

    override suspend fun initialize(): AppConfig? = migrationMutex.withLock {
        val current = dataStore.data.first()
        decodeConfig(current)?.let { return@withLock it }

        if (current[CONFIG_MIGRATION_COMPLETE] == true) return@withLock null
        if (!legacyConfigFile.isFile) {
            dataStore.edit { it[CONFIG_MIGRATION_COMPLETE] = true }
            return@withLock null
        }

        val legacyConfig = try {
            json.decodeFromString<AppConfig>(legacyConfigFile.readText(Charsets.UTF_8))
        } catch (error: Exception) {
            logger.error("Could not import config.json; keeping the original file for recovery", error)
            return@withLock null
        }

        try {
            legacyBackupFile.parentFile?.mkdirs()
            if (!legacyBackupFile.exists()) {
                legacyConfigFile.copyTo(legacyBackupFile, overwrite = false)
            }
            dataStore.edit { preferences ->
                encodeConfig(preferences, legacyConfig)
                preferences[CONFIG_MIGRATION_COMPLETE] = true
            }
            legacyConfigFile.delete()
            logger.info("Migrated config.json to DataStore; backup saved to ${legacyBackupFile.absolutePath}")
            legacyConfig
        } catch (error: Exception) {
            logger.error("Could not finish config.json migration; source and backup are preserved", error)
            null
        }
    }

    override suspend fun save(config: AppConfig) {
        dataStore.edit { preferences -> encodeConfig(preferences, config) }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ROLE)
            preferences.remove(DEVICE_ID)
            preferences.remove(DEVICE_NAME)
            preferences.remove(PORT)
            preferences.remove(THEME)
            preferences.remove(REDUCE_MOTION)
        }
    }

    private fun decodeConfig(preferences: Preferences): AppConfig? {
        val role = preferences[ROLE]?.let { value ->
            AppRole.entries.firstOrNull { it.name == value }
        } ?: return null
        val deviceId = preferences[DEVICE_ID]?.takeIf(String::isNotBlank) ?: return null
        val deviceName = preferences[DEVICE_NAME]?.takeIf(String::isNotBlank) ?: return null
        val theme = preferences[THEME]?.let { value ->
            AppTheme.entries.firstOrNull { it.name == value }
        } ?: AppTheme.DARK

        return AppConfig(
            role = role,
            deviceId = deviceId,
            deviceName = deviceName,
            port = preferences[PORT] ?: app.kiosksync.core.DEFAULT_PORT,
            theme = theme,
            reduceMotion = preferences[REDUCE_MOTION] ?: false
        )
    }

    private fun encodeConfig(preferences: androidx.datastore.preferences.core.MutablePreferences, config: AppConfig) {
        preferences[ROLE] = config.role.name
        preferences[DEVICE_ID] = config.deviceId
        preferences[DEVICE_NAME] = config.deviceName
        preferences[PORT] = config.port
        preferences[THEME] = config.theme.name
        preferences[REDUCE_MOTION] = config.reduceMotion
    }

    private companion object {
        const val DATA_STORE_FILE_NAME = "app-config.preferences_pb"
        val ROLE = stringPreferencesKey("role")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val PORT = intPreferencesKey("port")
        val THEME = stringPreferencesKey("theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val CONFIG_MIGRATION_COMPLETE = booleanPreferencesKey("legacy_config_migration_complete")
    }
}
