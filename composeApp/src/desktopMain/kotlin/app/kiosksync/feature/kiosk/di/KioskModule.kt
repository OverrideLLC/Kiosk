package app.kiosksync.feature.kiosk.di

import app.kiosksync.app.di.AppQualifiers
import app.kiosksync.app.di.AppSession
import app.kiosksync.app.di.PasswordDigest
import app.kiosksync.feature.kiosk.data.createKioskKeyboardGuard
import app.kiosksync.feature.kiosk.data.createKioskMouseGuard
import app.kiosksync.feature.kiosk.domain.KioskKeyboardGuard
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.kiosk.domain.KioskLockManager
import app.kiosksync.feature.kiosk.domain.KioskMouseGuard
import app.kiosksync.feature.projection.domain.ProjectionController
import org.koin.dsl.module

internal val kioskModule = module {
    single {
        val digest = get<PasswordDigest>()
        KioskLockManager(
            logger = get(),
            passwordHasher = { digest.hash(it) },
            scope = get(AppQualifiers.kioskScope),
            platformLockAvailable = get<AppSession>().platformLockAvailable,
        )
    }
    single {
        val digest = get<PasswordDigest>()
        KioskLockConfigurationUseCases(
            repository = get(),
            manager = get(),
            passwordHasher = { digest.hash(it) },
        )
    }
    single<KioskKeyboardGuard> {
        val manager = get<KioskLockManager>()
        val projection = get<ProjectionController>()
        createKioskKeyboardGuard(
            enabled = get<AppSession>().platformLockAvailable,
            isLocked = { manager.state.value.isLocked },
            onKey = { manager.onKey(it) },
            onProjectionToggle = { projection.requestToggle() },
            logger = get(),
        )
    }
    single<KioskMouseGuard> {
        val manager = get<KioskLockManager>()
        createKioskMouseGuard(
            enabled = get<AppSession>().platformLockAvailable,
            isLocked = { manager.state.value.isLocked },
            logger = get(),
        )
    }
}
