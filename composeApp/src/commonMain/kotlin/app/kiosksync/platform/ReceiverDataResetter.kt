package app.kiosksync.platform

/** Destructive Receiver data reset, called only after an explicit role-change confirmation. */
interface ReceiverDataResetter {
    suspend fun clearReceiverData()
}
