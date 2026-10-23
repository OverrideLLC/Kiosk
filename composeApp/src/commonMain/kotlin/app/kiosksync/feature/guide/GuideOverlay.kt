package app.kiosksync.feature.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kiosksync.core.AppRole
import app.kiosksync.core.Strings
import app.kiosksync.designsystem.AppButton
import app.kiosksync.designsystem.AppButtonSize
import app.kiosksync.designsystem.AppButtonVariant
import app.kiosksync.designsystem.AppDialog
import app.kiosksync.designsystem.AppIcon
import app.kiosksync.designsystem.AppIconButton
import app.kiosksync.designsystem.AppIconButtonSize
import app.kiosksync.designsystem.KioskColors
import app.kiosksync.designsystem.KioskShape
import app.kiosksync.designsystem.Space

private data class GuideStepContent(
    val title: String,
    val body: String,
    val icon: AppIcon,
)

/**
 * Renders the guide on top of the real application. The role questions is a
 * dialog because it changes the whole window; the other steps are a compact
 * card that leaves the real screens usable while the person follows it.
 */
@Composable
fun GuideOverlay(
    state: GuideUiState,
    onChooseRole: (AppRole) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onAction: (GuideAction) -> Unit,
    onFinish: () -> Unit,
    onRestart: () -> Unit,
) {
    if (!state.isActive) return

    if (state.step == GuideStep.ROLE) {
        GuideRoleDialog(
            onChooseRole = onChooseRole,
            onSkip = onSkip,
        )
        return
    }

    GuideCoachCard(
        state = state,
        onNext = onNext,
        onBack = onBack,
        onSkip = onSkip,
        onAction = onAction,
        onFinish = onFinish,
        onRestart = onRestart,
    )
}

@Composable
private fun GuideRoleDialog(
    onChooseRole: (AppRole) -> Unit,
    onSkip: () -> Unit,
) {
    val colors = KioskColors.scheme

    AppDialog(
        title = Strings.GUIDE_TITLE,
        icon = AppIcon.HELP,
        onDismissRequest = onSkip,
        content = {
            Text(
                text = Strings.GUIDE_ROLE_TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Strings.GUIDE_ROLE_BODY,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(
                    text = Strings.ROLE_EMITTER_TITLE,
                    onClick = { onChooseRole(AppRole.EMITTER) },
                    icon = AppIcon.CAST,
                    size = AppButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = Strings.ROLE_RECEIVER_TITLE,
                    onClick = { onChooseRole(AppRole.RECEIVER) },
                    icon = AppIcon.TV,
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Space.sm))
            Text(
                text = Strings.GUIDE_ROLE_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        },
        actions = {
            AppButton(
                text = Strings.GUIDE_SKIP,
                onClick = onSkip,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
        },
    )
}

@Composable
private fun GuideCoachCard(
    state: GuideUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onAction: (GuideAction) -> Unit,
    onFinish: () -> Unit,
    onRestart: () -> Unit,
) {
    val colors = KioskColors.scheme
    val content = guideContent(state)
    val progress = guideProgress(state)
    val isDone = state.step == GuideStep.EMITTER_DONE || state.step == GuideStep.RECEIVER_DONE
    val cardInteraction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = Space.lg,
                    end = Space.lg,
                    top = Space.md,
                    // Leave the Controller send bar and the Screen admin actions
                    // visible and clickable below the guide.
                    bottom = 112.dp,
                )
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                // Keep clicks on the card from reaching the real screen below.
                // Child buttons still receive their own clicks.
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
            shape = KioskShape.card,
            color = colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.55f)),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(Space.md)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            icon = content.icon,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.size(Space.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        if (progress != null) {
                            Text(
                                text = Strings.guideProgress(progress.first, progress.second),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                            )
                        }
                        Text(
                            text = content.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                    }
                    AppIconButton(
                        icon = AppIcon.CLOSE,
                        contentDescription = Strings.GUIDE_SKIP,
                        onClick = onSkip,
                        size = AppIconButtonSize.SMALL,
                    )
                }

                Spacer(Modifier.height(Space.sm))
                Text(
                    text = content.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.md))

                if (isDone) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppButton(
                            text = Strings.GUIDE_RESTART,
                            onClick = onRestart,
                            variant = AppButtonVariant.OUTLINED,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.REFRESH,
                        )
                        AppButton(
                            text = Strings.GUIDE_FINISH,
                            onClick = onFinish,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.CHECK,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (canGoBack(state.step)) {
                            AppButton(
                                text = Strings.GUIDE_BACK,
                                onClick = onBack,
                                variant = AppButtonVariant.TEXT,
                                size = AppButtonSize.SMALL,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        AppButton(
                            text = Strings.GUIDE_SKIP,
                            onClick = onSkip,
                            variant = AppButtonVariant.TEXT,
                            size = AppButtonSize.SMALL,
                        )
                        guideActionButton(state, onAction)
                        AppButton(
                            text = Strings.GUIDE_NEXT,
                            onClick = onNext,
                            variant = if (hasGuideAction(state.step)) {
                                AppButtonVariant.OUTLINED
                            } else {
                                AppButtonVariant.FILLED
                            },
                            size = AppButtonSize.SMALL,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small helper with the primary action for steps where the guide can operate
 * the real screen. It is optional: every step still has a "Siguiente" button.
 */
@Composable
private fun guideActionButton(
    state: GuideUiState,
    onAction: (GuideAction) -> Unit,
) {
    when (state.step) {
        GuideStep.EMITTER_PLAYLIST -> AppButton(
            text = Strings.GUIDE_ACTION_ADD_IMAGES,
            onClick = { onAction(GuideAction.ADD_IMAGES) },
            variant = AppButtonVariant.TONAL,
            size = AppButtonSize.SMALL,
            icon = AppIcon.ADD_PHOTO_ALTERNATE,
        )

        GuideStep.EMITTER_SCREENS -> AppButton(
            text = Strings.GUIDE_ACTION_REFRESH,
            onClick = { onAction(GuideAction.REFRESH_DEVICES) },
            variant = AppButtonVariant.TONAL,
            size = AppButtonSize.SMALL,
            icon = AppIcon.REFRESH,
        )

        GuideStep.EMITTER_SEND -> AppButton(
            text = Strings.GUIDE_ACTION_SEND,
            onClick = { onAction(GuideAction.SEND) },
            variant = AppButtonVariant.FILLED,
            size = AppButtonSize.SMALL,
            icon = AppIcon.SEND,
            enabled = state.playlistItems > 0 && state.selectedScreens > 0,
            disabledReason = if (state.playlistItems == 0) {
                Strings.GUIDE_SEND_NEEDS_PLAYLIST
            } else {
                Strings.GUIDE_SEND_NEEDS_SCREEN
            },
        )

        else -> Unit
    }
}

private fun hasGuideAction(step: GuideStep): Boolean = when (step) {
    GuideStep.EMITTER_PLAYLIST,
    GuideStep.EMITTER_SCREENS,
    GuideStep.EMITTER_SEND -> true

    else -> false
}

private fun guideContent(state: GuideUiState): GuideStepContent = when (state.step) {
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

private fun guideProgress(state: GuideUiState): Pair<Int, Int>? {
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

private fun canGoBack(step: GuideStep): Boolean = when (step) {
    GuideStep.ROLE,
    GuideStep.EMITTER_PLAYLIST,
    GuideStep.EMITTER_DONE,
    GuideStep.RECEIVER_CODE,
    GuideStep.RECEIVER_DONE -> false

    else -> true
}
