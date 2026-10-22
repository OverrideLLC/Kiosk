package app.kiosksync.feature.emitter.domain

import app.kiosksync.core.EmitterPairedDevice

/** Local credentials and addresses for Screens paired by this Emitter. */
interface EmitterScreensRepository {
    suspend fun loadPairedScreens(): List<EmitterPairedDevice>
    suspend fun savePairedScreens(devices: List<EmitterPairedDevice>)
    suspend fun addPairedScreen(device: EmitterPairedDevice)
    suspend fun removePairedScreen(deviceId: String)
    suspend fun clearPairedScreens()
}
