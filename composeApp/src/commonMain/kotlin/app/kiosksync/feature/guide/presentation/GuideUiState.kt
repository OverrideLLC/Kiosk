package app.kiosksync.feature.guide.presentation

import app.kiosksync.core.AppRole
import app.kiosksync.feature.guide.domain.GuideStep

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
