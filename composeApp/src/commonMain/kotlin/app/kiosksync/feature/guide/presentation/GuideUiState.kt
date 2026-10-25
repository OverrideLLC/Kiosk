package app.kiosksync.feature.guide.presentation

import app.kiosksync.core.AppRole

enum class GuideStep {
    ROLE,
    EMITTER_PLAYLIST,
    EMITTER_SCREENS,
    EMITTER_PASSWORD,
    EMITTER_SEND,
    EMITTER_DONE,
    RECEIVER_CODE,
    RECEIVER_PASSWORD,
    RECEIVER_CONTENT,
    RECEIVER_DONE,
}

data class GuideUiState(
    val isActive: Boolean = false,
    val role: AppRole? = null,
    val step: GuideStep = GuideStep.ROLE,
    val autoAdvanceEnabled: Boolean = true,
    val playlistItems: Int = 0,
    val pairedScreens: Int = 0,
    val selectedScreens: Int = 0,
    val hasPassword: Boolean = false,
    val sendSucceeded: Boolean = false,
    val receiverPaired: Boolean = false,
    val receiverHasPassword: Boolean = false,
    val receiverHasContent: Boolean = false,
)
