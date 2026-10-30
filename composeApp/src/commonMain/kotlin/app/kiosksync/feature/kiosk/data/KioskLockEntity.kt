package app.kiosksync.feature.kiosk.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "kiosk_lock")
data class KioskLockEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val passwordHashCiphertext: String?,
    val passwordLength: Int,
    val enabled: Boolean,
    val unlockSequence: String,
    val recoveryHashCiphertext: String?,
    val recoveryLength: Int
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
