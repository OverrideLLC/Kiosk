package app.kiosksync.feature.update.domain

import app.kiosksync.core.APP_VERSION
import kotlinx.coroutines.flow.StateFlow

enum class ReceiverUpdateError {
    INSTALLATION_NOT_SUPPORTED,
    INSTALL_FAILED,
}

data class ReceiverUpdateState(
    val version: String? = null,
    val currentVersion: String = APP_VERSION,
    val forced: Boolean = false,
    val applying: Boolean = false,
    val error: ReceiverUpdateError? = null,
) {
    val isVisible: Boolean get() = version != null
}

/** Receives a packaged update, validates it, stages it, and delegates installation. */
interface ReceiverUpdateManager {
    val state: StateFlow<ReceiverUpdateState>
    val skippedVersion: String?

    /** Returns whether the update is forced. */
    suspend fun stagePackage(version: String, sha256: String, bytes: ByteArray): Result<Boolean>
    fun acceptUpdate(): Boolean
    fun declineUpdate()
    fun dismissError()
}
