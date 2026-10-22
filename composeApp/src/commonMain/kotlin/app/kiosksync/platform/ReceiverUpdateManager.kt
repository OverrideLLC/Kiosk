package app.kiosksync.platform

import app.kiosksync.core.APP_VERSION
import kotlinx.coroutines.flow.StateFlow

data class ReceiverUpdateUi(
    val version: String? = null,
    val currentVersion: String = APP_VERSION,
    val forced: Boolean = false,
    val applying: Boolean = false,
    val error: String? = null
) {
    val isVisible: Boolean get() = version != null
}

/**
 * Receives a packaged update, validates and stages it, and lets the screen
 * accept or reject it. The desktop implementation performs the actual install
 * after the process exits.
 */
interface ReceiverUpdateManager {
    val uiState: StateFlow<ReceiverUpdateUi>
    val skippedVersion: String?

    /** Returns whether the update is forced. */
    suspend fun stagePackage(version: String, sha256: String, bytes: ByteArray): Result<Boolean>
    fun acceptUpdate(): Boolean
    fun declineUpdate()
    fun dismissError()
}
