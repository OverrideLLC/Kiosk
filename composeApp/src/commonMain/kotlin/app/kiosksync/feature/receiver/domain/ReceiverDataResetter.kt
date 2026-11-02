package app.kiosksync.feature.receiver.domain

/** Destructive Receiver data reset, called only after an explicit role-change confirmation. */
interface ReceiverDataResetter {
    suspend fun clearReceiverData()
}
