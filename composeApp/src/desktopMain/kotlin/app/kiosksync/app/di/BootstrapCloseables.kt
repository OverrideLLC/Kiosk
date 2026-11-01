package app.kiosksync.app.di

import app.kiosksync.feature.emitter.domain.EmitterClient
import app.kiosksync.feature.persistence.data.KioskDatabase
import app.kiosksync.feature.persistence.data.RoomRepositories
import app.kiosksync.feature.role.domain.AppConfigStore
import kotlinx.coroutines.ExecutorCoroutineDispatcher

/**
 * Resources created during startup that must be closed if [startDesktopKoin] fails
 * before [AppShutdown] owns them. Normal shutdown does not use this object.
 */
internal class BootstrapCloseables {
    var configStore: AppConfigStore? = null
    var database: KioskDatabase? = null
    var repositories: RoomRepositories? = null
    var emitterClient: EmitterClient? = null
    var serviceDispatcher: ExecutorCoroutineDispatcher? = null

    fun close() {
        runCatching { configStore?.close() }
        runCatching { emitterClient?.close() }
        if (repositories != null) {
            runCatching { repositories?.close() }
        } else {
            runCatching { database?.close() }
        }
        runCatching { serviceDispatcher?.close() }
    }
}
