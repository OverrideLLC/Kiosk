package app.kiosksync.platform

interface ReceiverServer {
    val boundPort: Int
    fun start(port: Int): Int
    fun stop()
}
