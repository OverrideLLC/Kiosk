package app.kiosksync.feature.guide.presentation

import app.kiosksync.core.AppRole

sealed interface GuideAction {
    data class Start(val role: AppRole? = null) : GuideAction
    data object Stop : GuideAction
    data class RoleConfirmed(val role: AppRole) : GuideAction
    data class ReportEmitterProgress(
        val playlistItems: Int,
        val pairedScreens: Int,
        val selectedScreens: Int,
        val hasPassword: Boolean,
        val sendSucceeded: Boolean,
    ) : GuideAction
    data class ReportReceiverProgress(
        val paired: Boolean,
        val hasPassword: Boolean,
        val hasContent: Boolean,
    ) : GuideAction
    data object Next : GuideAction
    data object Back : GuideAction
    data object Restart : GuideAction
    data class RequestEffect(val effect: GuideEffect) : GuideAction
}

sealed interface GuideEffect {
    data object AddImages : GuideEffect
    data object RefreshDevices : GuideEffect
    data object Send : GuideEffect
}
