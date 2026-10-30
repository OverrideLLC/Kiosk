package app.kiosksync.feature.pairing.data

import app.kiosksync.core.ReceiverPairedInfo
import app.kiosksync.feature.pairing.domain.PairingRepository
import app.kiosksync.core.security.SecretProtector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



internal class RoomPairingRepository(
    private val dao: PairedDeviceDao,
    private val secretProtector: SecretProtector,
) : PairingRepository {
    override suspend fun loadPairedEmitter(): ReceiverPairedInfo? = withContext(Dispatchers.IO) {
        dao.getPairedDevices(PairedDeviceDao.PAIR_KIND_RECEIVER).firstOrNull()?.let { entity ->
            ReceiverPairedInfo(
                emitterId = entity.deviceId,
                emitterName = entity.deviceName,
                token = secretProtector.unprotect(entity.tokenCiphertext),
            )
        }
    }

    override suspend fun savePairedEmitter(info: ReceiverPairedInfo) = withContext(Dispatchers.IO) {
        dao.replacePairedDevices(
            PairedDeviceDao.PAIR_KIND_RECEIVER,
            listOf(info.toEntity(secretProtector)),
        )
    }

    override suspend fun clearPairedEmitter() = withContext(Dispatchers.IO) {
        dao.deletePairedDevices(PairedDeviceDao.PAIR_KIND_RECEIVER)
    }

    private fun ReceiverPairedInfo.toEntity(protector: SecretProtector) = PairedDeviceEntity(
        kind = PairedDeviceDao.PAIR_KIND_RECEIVER,
        deviceId = emitterId,
        deviceName = emitterName,
        tokenCiphertext = protector.protect(token),
        host = null,
        port = null,
    )
}
