package app.kiosksync.feature.playback

import app.kiosksync.feature.playlist.Playlist

/**
 * Connects the Ktor server (which receives a new playlist) with the playback
 * loop, without making either side depend on the other.
 */
class ReceiverPlaylistBridge {
    private var listener: ((Playlist) -> Unit)? = null
    private var pending: Playlist? = null

    fun setListener(newListener: ((Playlist) -> Unit)?) {
        listener = newListener
        val queued = pending
        if (newListener != null && queued != null) {
            pending = null
            newListener(queued)
        }
    }

    fun apply(playlist: Playlist) {
        val current = listener
        if (current != null) {
            current(playlist)
        } else {
            pending = playlist
        }
    }
}
