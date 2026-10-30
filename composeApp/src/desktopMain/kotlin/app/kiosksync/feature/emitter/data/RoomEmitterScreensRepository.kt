package app.kiosksync.feature.emitter.data

import app.kiosksync.core.EmitterPairedDevice
import app.kiosksync.feature.emitter.domain.EmitterScreensRepository
import app.kiosksync.core.security.SecretProtector
import app.kiosksync.feature.pairing.data.PairedDeviceDao
import app.kiosksync.feature.pairing.data.PairedDeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext



internal class RoomEmitterScreensRepository(
    private val dao: PairedDeviceDao,
    private val secretProtector: SecretProtector,
) : EmitterScreensRepository {
    override suspend fun loadPairedScreens(): List<EmitterPairedDevice> = withContext(Dispatchers.IO) {
        dao.getPairedDevices(PairedDeviceDao.PAIR_KIND_EMITTER).map { entity ->
            EmitterPairedDevice(
                deviceId = entity.deviceId,
                deviceName = entity.deviceName,
                token = secretProtector.unprotect(entity.tokenCiphertext),
                host = entity.host,
                port = entity.port,
            )
        }
    }

    override suspend fun savePairedScreens(devices: List<EmitterPairedDevice>) = withContext(Dispatchers.IO) {
        dao.replacePairedDevices(PairedDeviceDao.PAIR_KIND_EMITTER, devices.map(::toEntity))
    }

    override suspend fun addPairedScreen(device: EmitterPairedDevice) = withContext(Dispatchers.IO) {
        dao.upsertPairedDevice(toEntity(device))
    }

    override suspend fun removePairedScreen(deviceId: String) = withContext(Dispatchers.IO) {
        dao.deletePairedDevice(PairedDeviceDao.PAIR_KIND_EMITTER, deviceId)
    }

    override suspend fun clearPairedScreens() = withContext(Dispatchers.IO) {
        dao.deletePairedDevices(PairedDeviceDao.PAIR_KIND_EMITTER)
    }

    private fun toEntity(device: EmitterPairedDevice) = PairedDeviceEntity(
        kind = PairedDeviceDao.PAIR_KIND_EMITTER,
        deviceId = device.deviceId,
        deviceName = device.deviceName,
        tokenCiphertext = secretProtector.protect(device.token),
        host = device.host,
        port = device.port,
    )
}
