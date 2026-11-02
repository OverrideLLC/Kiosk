package app.kiosksync.feature.receiver.di

import app.kiosksync.feature.playback.domain.ReceiverStatusTracker
import app.kiosksync.feature.receiver.data.KtorReceiverServer
import app.kiosksync.feature.receiver.domain.ReceiverServer
import org.koin.dsl.module

internal val receiverModule = module {
    single<ReceiverServer> {
        val tracker = get<ReceiverStatusTracker>()
        KtorReceiverServer(
            pairingManager = get(),
            logger = get(),
            playlistRepository = get(),
            mediaRepository = get(),
            kioskLockRepository = get(),
            playlistBridge = get(),
            playbackSyncBridge = get(),
            kioskLockManager = get(),
            statusProvider = { tracker.snapshot() },
            updateManager = get(),
        )
    }
}
