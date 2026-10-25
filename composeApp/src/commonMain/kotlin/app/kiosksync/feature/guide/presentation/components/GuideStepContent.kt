package app.kiosksync.feature.guide.presentation.components

import app.kiosksync.core.AppRole
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.feature.guide.presentation.GuideUiState
import app.kiosksync.feature.guide.domain.GuideStep

internal data class GuideStepContent(
    val title: String,
    val body: String,
    val icon: AppIcon,
)

internal fun guideContent(state: GuideUiState): GuideStepContent = when (state.step) {
    GuideStep.EMITTER_PLAYLIST -> GuideStepContent(
        title = Strings.GUIDE_EMITTER_PLAYLIST_TITLE,
        body = Strings.GUIDE_EMITTER_PLAYLIST_BODY,
        icon = AppIcon.ADD_PHOTO_ALTERNATE,
    )

    GuideStep.EMITTER_SCREENS -> GuideStepContent(
        title = Strings.GUIDE_EMITTER_SCREENS_TITLE,
        body = Strings.GUIDE_EMITTER_SCREENS_BODY,
        icon = AppIcon.CAST,
    )

    GuideStep.EMITTER_PASSWORD -> GuideStepContent(
        title = Strings.GUIDE_EMITTER_PASSWORD_TITLE,
        body = Strings.GUIDE_EMITTER_PASSWORD_BODY,
        icon = AppIcon.LOCK,
    )

    GuideStep.EMITTER_SEND -> GuideStepContent(
        title = Strings.GUIDE_EMITTER_SEND_TITLE,
        body = Strings.GUIDE_EMITTER_SEND_BODY,
        icon = AppIcon.SEND,
    )

    GuideStep.EMITTER_DONE -> GuideStepContent(
        title = Strings.GUIDE_EMITTER_DONE_TITLE,
        body = Strings.GUIDE_EMITTER_DONE_BODY,
        icon = AppIcon.CHECK_CIRCLE,
    )

    GuideStep.RECEIVER_CODE -> GuideStepContent(
        title = Strings.GUIDE_RECEIVER_CODE_TITLE,
        body = Strings.GUIDE_RECEIVER_CODE_BODY,
        icon = AppIcon.TV,
    )

    GuideStep.RECEIVER_PASSWORD -> GuideStepContent(
        title = Strings.GUIDE_RECEIVER_PASSWORD_TITLE,
        body = Strings.GUIDE_RECEIVER_PASSWORD_BODY,
        icon = AppIcon.LOCK,
    )

    GuideStep.RECEIVER_CONTENT -> GuideStepContent(
        title = Strings.GUIDE_RECEIVER_CONTENT_TITLE,
        body = Strings.GUIDE_RECEIVER_CONTENT_BODY,
        icon = AppIcon.PLAYLIST_PLAY,
    )

    GuideStep.RECEIVER_DONE -> GuideStepContent(
        title = Strings.GUIDE_RECEIVER_DONE_TITLE,
        body = Strings.GUIDE_RECEIVER_DONE_BODY,
        icon = AppIcon.CHECK_CIRCLE,
    )

    GuideStep.ROLE -> GuideStepContent(
        title = Strings.GUIDE_ROLE_TITLE,
        body = Strings.GUIDE_ROLE_BODY,
        icon = AppIcon.HELP,
    )
}

internal fun guideProgress(state: GuideUiState): Pair<Int, Int>? {
    val steps = when (state.role) {
        AppRole.EMITTER -> listOf(
            GuideStep.EMITTER_PLAYLIST,
            GuideStep.EMITTER_SCREENS,
            GuideStep.EMITTER_PASSWORD,
            GuideStep.EMITTER_SEND,
        )

        AppRole.RECEIVER -> listOf(
            GuideStep.RECEIVER_CODE,
            GuideStep.RECEIVER_PASSWORD,
            GuideStep.RECEIVER_CONTENT,
        )

        null -> return null
    }

    if (state.step == GuideStep.EMITTER_DONE || state.step == GuideStep.RECEIVER_DONE) {
        return steps.size to steps.size
    }
    val index = steps.indexOf(state.step)
    return if (index >= 0) (index + 1) to steps.size else null
}

internal fun canGoBack(step: GuideStep): Boolean = when (step) {
    GuideStep.ROLE,
    GuideStep.EMITTER_PLAYLIST,
    GuideStep.EMITTER_DONE,
    GuideStep.RECEIVER_CODE,
    GuideStep.RECEIVER_DONE -> false

    else -> true
}

internal fun hasGuideAction(step: GuideStep): Boolean = when (step) {
    GuideStep.EMITTER_PLAYLIST,
    GuideStep.EMITTER_SCREENS,
    GuideStep.EMITTER_SEND -> true

    else -> false
}
