package app.kiosksync.feature.emitter.domain.models

import app.kiosksync.core.ui.UiText

data class DeviceSyncUi(
    val status: SyncUiStatus,
    val uploaded: Int = 0,
    val total: Int = 0,
    val version: Int = 0,
    val message: UiText? = null
)
