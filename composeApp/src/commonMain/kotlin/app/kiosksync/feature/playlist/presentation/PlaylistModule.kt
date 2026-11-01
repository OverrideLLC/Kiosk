package app.kiosksync.feature.playlist.presentation

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.DeviceIdFactory
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val playlistPresentationModule = module {
    viewModel {
        val ids = get<DeviceIdFactory>()
        PlaylistEditorViewModel(
            playlistRepository = get(),
            mediaRepository = get(),
            logger = get(),
            newPlaylistId = { ids.create() },
            ioDispatcher = get(AppQualifiers.io),
        )
    }
}
