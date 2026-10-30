package app.kiosksync.feature.update.presentation

import app.kiosksync.core.APP_VERSION

data class ReceiverUpdateState(
    val version: String? = null,
    val currentVersion: String = APP_VERSION,
    val forced: Boolean = false,
    val applying: Boolean = false,
    val errorMessage: String? = null,
) {
    val isVisible: Boolean get() = version != null
}
