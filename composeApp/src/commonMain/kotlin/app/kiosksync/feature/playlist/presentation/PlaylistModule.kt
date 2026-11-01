package app.kiosksync.feature.playlist.presentation

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.DeviceIdFactory
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

internal class PlaylistEditorHost(
    val scope: CoroutineScope,
)

internal val playlistPresentationModule = module {
    factory { (host: PlaylistEditorHost) ->
        val ids = get<DeviceIdFactory>()
        PlaylistEditorViewModel(
            playlistRepository = get(),
            mediaRepository = get(),
            logger = get(),
            scope = host.scope,
            newPlaylistId = { ids.create() },
            ioDispatcher = get(AppQualifiers.io),
        )
    }
}
