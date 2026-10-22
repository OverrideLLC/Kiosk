package app.kiosksync.feature.role

import kotlinx.coroutines.flow.Flow

/** Persistence contract for small, device-wide application preferences. */
interface AppConfigStore {
    val config: Flow<AppConfig?>

    /** Imports the legacy config.json once when DataStore has no saved role. */
    suspend fun initialize(): AppConfig?

    suspend fun save(config: AppConfig)

    suspend fun clear()

    fun close() = Unit
}
