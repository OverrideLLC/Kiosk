package app.kiosksync.feature.playback.di

import app.kiosksync.feature.playback.domain.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.domain.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.domain.ReceiverStatusTracker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val playbackModule = module {
    singleOf(::ReceiverStatusTracker)
    singleOf(::ReceiverPlaylistBridge)
    singleOf(::ReceiverPlaybackSyncBridge)
}
