package app.kiosksync.feature.pairing.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices WHERE kind = :kind AND deviceId = :deviceId LIMIT 1")
    suspend fun getPairedDevice(kind: String, deviceId: String): PairedDeviceEntity?

    @Query("SELECT * FROM paired_devices WHERE kind = :kind ORDER BY deviceName COLLATE NOCASE")
    suspend fun getPairedDevices(kind: String): List<PairedDeviceEntity>

    @Upsert
    suspend fun upsertPairedDevice(device: PairedDeviceEntity)

    @Upsert
    suspend fun upsertPairedDevices(devices: List<PairedDeviceEntity>)

    @Query("DELETE FROM paired_devices WHERE kind = :kind")
    suspend fun deletePairedDevices(kind: String)

    @Query("DELETE FROM paired_devices WHERE kind = :kind AND deviceId = :deviceId")
    suspend fun deletePairedDevice(kind: String, deviceId: String)

    @Transaction
    suspend fun replacePairedDevices(kind: String, devices: List<PairedDeviceEntity>) {
        deletePairedDevices(kind)
        if (devices.isNotEmpty()) upsertPairedDevices(devices)
    }

    companion object {
        const val PAIR_KIND_EMITTER = "emitter"
        const val PAIR_KIND_RECEIVER = "receiver"
    }
}
