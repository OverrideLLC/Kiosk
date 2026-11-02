package app.kiosksync.feature.receiver.domain

interface ReceiverServer {
    val boundPort: Int
    fun start(port: Int, deviceId: String, deviceName: String): Int
    fun stop()
}
