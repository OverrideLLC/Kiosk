package app.kiosksync.app.di

import app.kiosksync.app.navigation.session.sessionNavigationModule
import app.kiosksync.feature.emitter.presentation.emitterPresentationModule
import app.kiosksync.feature.guide.presentation.guidePresentationModule
import app.kiosksync.feature.kiosk.presentation.kioskPresentationModule
import app.kiosksync.feature.pairing.presentation.pairingPresentationModule
import app.kiosksync.feature.playback.presentation.playbackPresentationModule
import app.kiosksync.feature.playlist.presentation.playlistPresentationModule
import app.kiosksync.feature.receiver.presentation.receiverNavigationModule
import app.kiosksync.feature.role.presentation.rolePresentationModule
import app.kiosksync.feature.update.presentation.updatePresentationModule
import org.koin.dsl.module

val presentationModule = module {
    includes(
        guidePresentationModule,
        rolePresentationModule,
        pairingPresentationModule,
        emitterPresentationModule,
        playlistPresentationModule,
        playbackPresentationModule,
        kioskPresentationModule,
        updatePresentationModule,
        receiverNavigationModule,
        sessionNavigationModule,
    )
}
