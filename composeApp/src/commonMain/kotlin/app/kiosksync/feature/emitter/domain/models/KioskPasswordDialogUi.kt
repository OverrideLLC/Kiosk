package app.kiosksync.feature.emitter.domain.models

import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.core.ui.UiText

data class KioskPasswordDialogUi(
    val action: EmitterPendingAction,
    val playlist: Playlist? = null,
    val deviceId: String? = null,
    val draft: String = "",
    val confirmDraft: String = "",
    val error: UiText? = null
)
