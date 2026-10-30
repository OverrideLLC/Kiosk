package app.kiosksync.feature.kiosk.data

import app.kiosksync.core.KioskLockConfig
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.core.security.SecretProtector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext



internal class RoomKioskLockRepository(
    private val dao: KioskLockDao,
    private val secretProtector: SecretProtector,
) : KioskLockRepository {
    override val config: Flow<KioskLockConfig?> = dao.observeKioskLock(KioskLockEntity.SINGLETON_ID)
        .map { entity -> entity?.toDomain(secretProtector) }

    override suspend fun load(): KioskLockConfig? = withContext(Dispatchers.IO) {
        dao.getKioskLock(KioskLockEntity.SINGLETON_ID)?.toDomain(secretProtector)
    }

    override suspend fun save(config: KioskLockConfig) = withContext(Dispatchers.IO) {
        dao.upsertKioskLock(config.toEntity(secretProtector))
    }

    override suspend fun clearLock() = withContext(Dispatchers.IO) {
        dao.deleteKioskLock(KioskLockEntity.SINGLETON_ID)
    }

    private fun KioskLockConfig.toEntity(protector: SecretProtector) = KioskLockEntity(
        passwordHashCiphertext = passwordHash?.let(protector::protect),
        passwordLength = passwordLength,
        enabled = enabled,
        unlockSequence = unlockSequence.joinToString(","),
        recoveryHashCiphertext = recoveryHash?.let(protector::protect),
        recoveryLength = recoveryLength,
    )

    private fun KioskLockEntity.toDomain(protector: SecretProtector) = KioskLockConfig(
        passwordHash = passwordHashCiphertext?.let(protector::unprotect),
        passwordLength = passwordLength,
        enabled = enabled,
        unlockSequence = unlockSequence.split(',').filter(String::isNotBlank),
        recoveryHash = recoveryHashCiphertext?.let(protector::unprotect),
        recoveryLength = recoveryLength,
    )
}
