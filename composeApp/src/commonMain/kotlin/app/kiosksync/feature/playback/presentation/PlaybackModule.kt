package app.kiosksync.feature.playback.presentation

import app.kiosksync.app.di.AppQualifiers
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

internal class PlaybackHost(
    val scope: CoroutineScope,
    val trackStatus: Boolean,
    val loadFromStorage: Boolean,
)

internal val playbackPresentationModule = module {
    factory { (host: PlaybackHost) ->
        PlaybackStateHolder(
            playlistRepository = get(),
            mediaRepository = get(),
            logger = get(),
            scope = host.scope,
            statusTracker = if (host.trackStatus) get() else null,
            backgroundDispatcher = get(AppQualifiers.io),
            loadFromStorage = host.loadFromStorage,
        )
    }
}
