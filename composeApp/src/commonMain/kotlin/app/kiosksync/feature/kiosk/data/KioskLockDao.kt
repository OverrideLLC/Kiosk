package app.kiosksync.feature.kiosk.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface KioskLockDao {
    @Query("SELECT * FROM kiosk_lock WHERE id = :id LIMIT 1")
    suspend fun getKioskLock(id: Int): KioskLockEntity?

    @Query("SELECT * FROM kiosk_lock WHERE id = :id LIMIT 1")
    fun observeKioskLock(id: Int): Flow<KioskLockEntity?>

    @Upsert
    suspend fun upsertKioskLock(lock: KioskLockEntity)

    @Query("DELETE FROM kiosk_lock WHERE id = :id")
    suspend fun deleteKioskLock(id: Int)
}
