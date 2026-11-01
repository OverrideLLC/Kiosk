package app.kiosksync.feature.kiosk.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.feature.kiosk.domain.KioskLockFeedback
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import org.jetbrains.compose.ui.tooling.preview.Preview
import app.kiosksync.resources.Res
import app.kiosksync.resources.kiosk_attempts_left
import app.kiosksync.resources.kiosk_locked_out
import app.kiosksync.resources.kiosk_locked_title
import app.kiosksync.resources.kiosk_lockout_seconds
import app.kiosksync.resources.kiosk_recovery_hint
import app.kiosksync.resources.kiosk_typed_characters
import app.kiosksync.resources.kiosk_unlock_hint
import app.kiosksync.resources.kiosk_wrong_password
import org.jetbrains.compose.resources.stringResource

/**
 * Small overlay shown while the keyboard is locked. The carousel keeps playing
 * behind it.
 *
 * It never reveals the unlock sequence: it only appears once the person starts
 * the correct sequence and shows neutral feedback (one dot per typed character,
 * wrong-password and lockout messages). The default sequence is documented in
 * the project manual and shown in the controller's "Seguridad" dialog.
 */
@Composable
fun KioskRoot(viewModel: KioskViewModel = sessionViewModel()) {
    val state by viewModel.state.collectAsState()
    KioskScreen(state = state)
}

@Composable
fun KioskScreen(state: KioskState) {
    if (!state.isLocked) return
    if (!state.showUnlockHint && state.feedback == null && state.lockoutRemainingSec == 0) return

    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(Space.lg)
                .widthIn(max = Sizes.overlayMaxWidth)
                .clip(KioskShape.card)
                .background(colors.surfaceContainerHigh)
                .border(Sizes.hairline, colors.outlineVariant, KioskShape.card)
                .padding(horizontal = Space.lg, vertical = Space.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                AppIcon(
                    icon = AppIcon.LOCK,
                    contentDescription = null,
                    size = Sizes.iconCompact,
                    tint = colors.primary,
                )
                Text(
                    text = stringResource(Res.string.kiosk_locked_title),
                    style = KioskDisplayType.screenCaption(),
                    color = colors.onSurface,
                )
            }

            if (state.lockoutRemainingSec > 0) {
                Text(
                    text = stringResource(Res.string.kiosk_lockout_seconds, state.lockoutRemainingSec),
                    style = KioskDisplayType.screenCaption(),
                    color = colors.error,
                )
            } else {
                if (state.typedLength > 0) {
                    TypedDots(
                        count = state.typedLength,
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(Space.xxs))
                } else {
                    Text(
                        text = stringResource(Res.string.kiosk_unlock_hint),
                        style = KioskDisplayType.screenCaption(),
                        color = colors.onSurfaceVariant,
                    )
                }

                state.feedback?.let { feedback ->
                    Text(
                        text = feedbackText(feedback),
                        style = KioskDisplayType.screenCaption(),
                        color = colors.error,
                    )
                }

                Text(
                    text = stringResource(Res.string.kiosk_recovery_hint),
                    style = KioskDisplayType.screenCaption(),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun feedbackText(feedback: KioskLockFeedback): String = when (feedback) {
    is KioskLockFeedback.IncorrectPassword ->
        "${stringResource(Res.string.kiosk_wrong_password)} ${stringResource(Res.string.kiosk_attempts_left)} ${feedback.attemptsRemaining}"

    KioskLockFeedback.LockedOut -> stringResource(Res.string.kiosk_locked_out)
}

@Composable
private fun TypedDots(count: Int, color: Color) {
    val description = stringResource(Res.string.kiosk_typed_characters, count)
    Row(
        modifier = Modifier.semantics {
            contentDescription = description
        },
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        repeat(count.coerceIn(1, 16)) {
            Box(
                modifier = Modifier
                    .size(Sizes.passwordDot)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Preview
@Composable
private fun KioskScreenPreview() {
    KioskTheme {
        KioskScreen(
            state = KioskState(isLocked = true, showUnlockHint = true, typedLength = 2),
        )
    }
}
