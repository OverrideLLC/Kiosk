package app.kiosksync.feature.playback.presentation

import app.kiosksync.app.di.AppQualifiers
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal class PlaybackHost(
    val scope: CoroutineScope,
    val trackStatus: Boolean,
    val loadFromStorage: Boolean,
)

internal val playbackPresentationModule = module {
    viewModel {
        PlaybackStateHolder(
            playlistRepository = get(),
            mediaRepository = get(),
            logger = get(),
            statusTracker = get(),
            backgroundDispatcher = get(AppQualifiers.io),
            loadFromStorage = true,
        )
    }
    factory(named("projectionPlayback")) { (host: PlaybackHost) ->
        PlaybackStateHolder(
            playlistRepository = get(),
            mediaRepository = get(),
            logger = get(),
            parentScope = host.scope,
            statusTracker = if (host.trackStatus) get() else null,
            backgroundDispatcher = get(AppQualifiers.io),
            loadFromStorage = host.loadFromStorage,
        )
    }
}
