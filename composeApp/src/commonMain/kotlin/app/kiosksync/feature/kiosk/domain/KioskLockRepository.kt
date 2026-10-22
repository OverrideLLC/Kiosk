package app.kiosksync.feature.kiosk.domain

import app.kiosksync.core.KioskLockConfig
import kotlinx.coroutines.flow.Flow

/** Persistence contract for the locally configured kiosk lock. */
interface KioskLockRepository {
    val config: Flow<KioskLockConfig?>
    suspend fun load(): KioskLockConfig?
    suspend fun save(config: KioskLockConfig)
    suspend fun clearLock()
}
