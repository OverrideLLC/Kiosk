package app.kiosksync.feature.emitter.domain.models

import app.kiosksync.feature.playlist.domain.Playlist

data class KioskPasswordDialogUi(
    val action: EmitterPendingAction,
    val playlist: Playlist? = null,
    val deviceId: String? = null,
    val draft: String = "",
    val confirmDraft: String = "",
    val error: String? = null
)
