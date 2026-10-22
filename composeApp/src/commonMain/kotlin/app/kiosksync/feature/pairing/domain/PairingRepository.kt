package app.kiosksync.feature.pairing.domain

import app.kiosksync.core.ReceiverPairedInfo

/** Persistence for the single Emitter paired with this Receiver. */
interface PairingRepository {
    suspend fun loadPairedEmitter(): ReceiverPairedInfo?
    suspend fun savePairedEmitter(info: ReceiverPairedInfo)
    suspend fun clearPairedEmitter()
}
