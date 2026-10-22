package app.kiosksync.feature.playback

/**
 * Connects the HTTP endpoint that receives a playback-synchronisation command
 * with the playback loop. The server can call this from a background thread;
 * the listener is installed by the receiver UI and returns whether the
 * restart could be scheduled.
 */
class ReceiverPlaybackSyncBridge {
    private var listener: ((Long) -> Boolean)? = null

    fun setListener(newListener: ((Long) -> Boolean)?) {
        listener = newListener
    }

    fun scheduleRestart(delayMs: Long): Boolean = listener?.invoke(delayMs) ?: false
}
