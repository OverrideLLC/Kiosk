package app.kiosksync.feature.emitter.domain

import app.kiosksync.core.EmitterPairedDevice
import kotlinx.coroutines.CancellationException

data class PairReceiverRequest(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val emitterId: String,
    val emitterName: String,
    val code: String,
)

data class PairReceiverResult(
    val pairedDevice: EmitterPairedDevice,
    val pairedDevices: List<EmitterPairedDevice>,
)

data class UnpairReceiverResult(
    val pairedDevices: List<EmitterPairedDevice>,
    val remoteUnpairSucceeded: Boolean,
)

/** Performs the remote handshake and persists the returned credential locally. */
class PairReceiverUseCase(
    private val client: EmitterClient,
    private val screensRepository: EmitterScreensRepository,
) {
    suspend operator fun invoke(request: PairReceiverRequest): PairReceiverResult {
        val response = client.pair(
            host = request.host,
            port = request.port,
            emitterId = request.emitterId,
            emitterName = request.emitterName,
            code = request.code,
        ).getOrElse { error ->
            if (error is CancellationException) throw error
            throw error
        }

        val pairedDevice = EmitterPairedDevice(
            deviceId = request.deviceId,
            deviceName = request.deviceName,
            token = response.token,
            host = request.host,
            port = request.port,
        )
        try {
            screensRepository.addPairedScreen(pairedDevice)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw PairReceiverPersistenceException(error)
        }
        val pairedDevices = try {
            screensRepository.loadPairedScreens()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw PairReceiverPersistenceException(error)
        }
        return PairReceiverResult(
            pairedDevice = pairedDevice,
            pairedDevices = pairedDevices,
        )
    }
}

class PairReceiverPersistenceException(cause: Throwable) :
    Exception("Could not persist the Receiver pairing", cause)

/** Removes the local pairing first and notifies the Receiver best-effort. */
class UnpairReceiverUseCase(
    private val client: EmitterClient,
    private val screensRepository: EmitterScreensRepository,
) {
    suspend operator fun invoke(
        device: EmitterPairedDevice,
        host: String?,
        port: Int?,
        emitterId: String,
    ): UnpairReceiverResult {
        screensRepository.removePairedScreen(device.deviceId)
        val pairedDevices = screensRepository.loadPairedScreens()
        if (host == null || port == null) {
            return UnpairReceiverResult(pairedDevices, remoteUnpairSucceeded = false)
        }

        val remoteResult = client.unpair(host, port, emitterId, device.token)
        remoteResult.exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
        }
        return UnpairReceiverResult(
            pairedDevices = pairedDevices,
            remoteUnpairSucceeded = remoteResult.getOrDefault(false),
        )
    }
}
