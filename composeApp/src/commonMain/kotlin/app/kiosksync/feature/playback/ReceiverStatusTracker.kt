package app.kiosksync.feature.playback

import app.kiosksync.core.ReceiverStatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared snapshot of what the receiver is playing. The Ktor server serves it
 * through GET /status and PlaybackStateHolder keeps it up to date.
 */
class ReceiverStatusTracker {
    private val _status = MutableStateFlow(ReceiverStatusResponse(playlistVersion = 0))
    val status: StateFlow<ReceiverStatusResponse> = _status.asStateFlow()

    fun update(playlistVersion: Int, mediaId: String?, fileName: String?) {
        _status.value = ReceiverStatusResponse(
            playlistVersion = playlistVersion,
            currentMediaId = mediaId,
            currentFileName = fileName
        )
    }

    fun snapshot(): ReceiverStatusResponse = _status.value
}
