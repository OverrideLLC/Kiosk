package app.kiosksync.feature.receiver.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import app.kiosksync.feature.kiosk.data.KioskLockEntity
import app.kiosksync.feature.pairing.data.PairedDeviceDao
import app.kiosksync.feature.playlist.data.PlaylistDao

@Dao
interface ReceiverDataDao {
    @Transaction
    suspend fun clearReceiverData() {
        deletePlaylists(PlaylistDao.OWNER_RECEIVER)
        deletePairedDevices(PairedDeviceDao.PAIR_KIND_RECEIVER)
        deleteKioskLock(KioskLockEntity.SINGLETON_ID)
        deleteAllMedia()
    }

    @Query("DELETE FROM playlists WHERE owner = :owner")
    suspend fun deletePlaylists(owner: String)

    @Query("DELETE FROM paired_devices WHERE kind = :kind")
    suspend fun deletePairedDevices(kind: String)

    @Query("DELETE FROM kiosk_lock WHERE id = :id")
    suspend fun deleteKioskLock(id: Int)

    @Query("DELETE FROM media")
    suspend fun deleteAllMedia()
}
