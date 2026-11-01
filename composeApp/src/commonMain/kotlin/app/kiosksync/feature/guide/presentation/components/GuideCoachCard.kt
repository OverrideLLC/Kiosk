package app.kiosksync.feature.guide.presentation.components

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
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideEffect
import app.kiosksync.feature.guide.presentation.GuideState
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.resources.Res
import app.kiosksync.resources.guide_action_add_images
import app.kiosksync.resources.guide_action_refresh
import app.kiosksync.resources.guide_action_send
import app.kiosksync.resources.guide_back
import app.kiosksync.resources.guide_finish
import app.kiosksync.resources.guide_next
import app.kiosksync.resources.guide_progress
import app.kiosksync.resources.guide_restart
import app.kiosksync.resources.guide_send_needs_playlist
import app.kiosksync.resources.guide_send_needs_screen
import app.kiosksync.resources.guide_skip
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GuideCoachCard(
    state: GuideState,
    onAction: (GuideAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
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
                                text = stringResource(Res.string.guide_progress, progress.first, progress.second),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                            )
                        }
                        Text(
                            text = stringResource(content.title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                    }
                    AppIconButton(
                        icon = AppIcon.CLOSE,
                        contentDescription = stringResource(Res.string.guide_skip),
                        onClick = { onAction(GuideAction.Stop) },
                        size = AppIconButtonSize.SMALL,
                    )
                }

                Spacer(Modifier.height(Space.sm))
                Text(
                    text = stringResource(content.body),
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
                            text = stringResource(Res.string.guide_restart),
                            onClick = { onAction(GuideAction.Restart) },
                            variant = AppButtonVariant.OUTLINED,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.REFRESH,
                        )
                        AppButton(
                            text = stringResource(Res.string.guide_finish),
                            onClick = { onAction(GuideAction.Stop) },
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
                                text = stringResource(Res.string.guide_back),
                                onClick = { onAction(GuideAction.Back) },
                                variant = AppButtonVariant.TEXT,
                                size = AppButtonSize.SMALL,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        AppButton(
                            text = stringResource(Res.string.guide_skip),
                            onClick = { onAction(GuideAction.Stop) },
                            variant = AppButtonVariant.TEXT,
                            size = AppButtonSize.SMALL,
                        )
                        GuideActionButton(state = state, onAction = onAction)
                        AppButton(
                            text = stringResource(Res.string.guide_next),
                            onClick = { onAction(GuideAction.Next) },
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

@Composable
private fun GuideActionButton(
    state: GuideState,
    onAction: (GuideAction) -> Unit,
) {
    when (state.step) {
        GuideStep.EMITTER_PLAYLIST -> AppButton(
            text = stringResource(Res.string.guide_action_add_images),
            onClick = { onAction(GuideAction.RequestEffect(GuideEffect.AddImages)) },
            variant = AppButtonVariant.TONAL,
            size = AppButtonSize.SMALL,
            icon = AppIcon.ADD_PHOTO_ALTERNATE,
        )

        GuideStep.EMITTER_SCREENS -> AppButton(
            text = stringResource(Res.string.guide_action_refresh),
            onClick = { onAction(GuideAction.RequestEffect(GuideEffect.RefreshDevices)) },
            variant = AppButtonVariant.TONAL,
            size = AppButtonSize.SMALL,
            icon = AppIcon.REFRESH,
        )

        GuideStep.EMITTER_SEND -> AppButton(
            text = stringResource(Res.string.guide_action_send),
            onClick = { onAction(GuideAction.RequestEffect(GuideEffect.Send)) },
            variant = AppButtonVariant.FILLED,
            size = AppButtonSize.SMALL,
            icon = AppIcon.SEND,
            enabled = state.playlistItems > 0 && state.selectedScreens > 0,
            disabledReason = if (state.playlistItems == 0) {
                stringResource(Res.string.guide_send_needs_playlist)
            } else {
                stringResource(Res.string.guide_send_needs_screen)
            },
        )

        else -> Unit
    }
}
