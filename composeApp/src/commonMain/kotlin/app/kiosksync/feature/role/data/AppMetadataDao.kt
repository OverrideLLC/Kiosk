package app.kiosksync.feature.role.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface AppMetadataDao {
    @Query("SELECT value FROM app_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: AppMetadataEntity)

    companion object {
        const val LEGACY_MIGRATION_KEY = "legacy-json-migration-v1"
        const val LEGACY_MIGRATION_COMPLETE = "complete"
    }
}
