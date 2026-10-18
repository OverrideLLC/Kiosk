package app.kiosksync.platform

import androidx.compose.ui.graphics.ImageBitmap
import app.kiosksync.feature.playlist.Playlist
import app.kiosksync.feature.role.AppConfig

interface LocalStorage {
    fun loadConfig(): AppConfig?
    fun saveConfig(config: AppConfig)
    fun clearConfig()
    fun clearPairing()

    // Playlist and media
    fun loadPlaylist(): Playlist?
    fun savePlaylist(playlist: Playlist)
    fun loadMediaBitmap(mediaId: String, fileName: String): ImageBitmap?
}
