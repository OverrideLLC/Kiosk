package app.kiosksync.feature.update.presentation

import app.kiosksync.core.common.app.APP_VERSION
import app.kiosksync.core.ui.UiText
data class ReceiverUpdateState(
    val version: String? = null,
    val currentVersion: String = APP_VERSION,
    val forced: Boolean = false,
    val applying: Boolean = false,
    val errorMessage: UiText? = null,
) {
    val isVisible: Boolean get() = version != null
}
