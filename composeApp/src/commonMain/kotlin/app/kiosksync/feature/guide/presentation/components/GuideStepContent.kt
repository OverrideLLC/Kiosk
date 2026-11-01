package app.kiosksync.feature.guide.presentation.components

import app.kiosksync.core.common.app.AppRole
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.feature.guide.presentation.GuideState
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.resources.Res
import app.kiosksync.resources.guide_emitter_done_body
import app.kiosksync.resources.guide_emitter_done_title
import app.kiosksync.resources.guide_emitter_password_body
import app.kiosksync.resources.guide_emitter_password_title
import app.kiosksync.resources.guide_emitter_playlist_body
import app.kiosksync.resources.guide_emitter_playlist_title
import app.kiosksync.resources.guide_emitter_screens_body
import app.kiosksync.resources.guide_emitter_screens_title
import app.kiosksync.resources.guide_emitter_send_body
import app.kiosksync.resources.guide_emitter_send_title
import app.kiosksync.resources.guide_receiver_code_body
import app.kiosksync.resources.guide_receiver_code_title
import app.kiosksync.resources.guide_receiver_content_body
import app.kiosksync.resources.guide_receiver_content_title
import app.kiosksync.resources.guide_receiver_done_body
import app.kiosksync.resources.guide_receiver_done_title
import app.kiosksync.resources.guide_receiver_password_body
import app.kiosksync.resources.guide_receiver_password_title
import app.kiosksync.resources.guide_role_body
import app.kiosksync.resources.guide_role_title
import org.jetbrains.compose.resources.StringResource

internal data class GuideStepContent(
    val title: StringResource,
    val body: StringResource,
    val icon: AppIcon,
)

internal fun guideContent(state: GuideState): GuideStepContent = when (state.step) {
    GuideStep.EMITTER_PLAYLIST -> GuideStepContent(
        title = Res.string.guide_emitter_playlist_title,
        body = Res.string.guide_emitter_playlist_body,
        icon = AppIcon.ADD_PHOTO_ALTERNATE,
    )

    GuideStep.EMITTER_SCREENS -> GuideStepContent(
        title = Res.string.guide_emitter_screens_title,
        body = Res.string.guide_emitter_screens_body,
        icon = AppIcon.CAST,
    )

    GuideStep.EMITTER_PASSWORD -> GuideStepContent(
        title = Res.string.guide_emitter_password_title,
        body = Res.string.guide_emitter_password_body,
        icon = AppIcon.LOCK,
    )

    GuideStep.EMITTER_SEND -> GuideStepContent(
        title = Res.string.guide_emitter_send_title,
        body = Res.string.guide_emitter_send_body,
        icon = AppIcon.SEND,
    )

    GuideStep.EMITTER_DONE -> GuideStepContent(
        title = Res.string.guide_emitter_done_title,
        body = Res.string.guide_emitter_done_body,
        icon = AppIcon.CHECK_CIRCLE,
    )

    GuideStep.RECEIVER_CODE -> GuideStepContent(
        title = Res.string.guide_receiver_code_title,
        body = Res.string.guide_receiver_code_body,
        icon = AppIcon.TV,
    )

    GuideStep.RECEIVER_PASSWORD -> GuideStepContent(
        title = Res.string.guide_receiver_password_title,
        body = Res.string.guide_receiver_password_body,
        icon = AppIcon.LOCK,
    )

    GuideStep.RECEIVER_CONTENT -> GuideStepContent(
        title = Res.string.guide_receiver_content_title,
        body = Res.string.guide_receiver_content_body,
        icon = AppIcon.PLAYLIST_PLAY,
    )

    GuideStep.RECEIVER_DONE -> GuideStepContent(
        title = Res.string.guide_receiver_done_title,
        body = Res.string.guide_receiver_done_body,
        icon = AppIcon.CHECK_CIRCLE,
    )

    GuideStep.ROLE -> GuideStepContent(
        title = Res.string.guide_role_title,
        body = Res.string.guide_role_body,
        icon = AppIcon.HELP,
    )
}

internal fun guideProgress(state: GuideState): Pair<Int, Int>? {
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
