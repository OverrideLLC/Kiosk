package app.kiosksync.platform

interface ReceiverServer {
    val boundPort: Int
    fun start(port: Int, deviceId: String, deviceName: String): Int
    fun stop()
}
