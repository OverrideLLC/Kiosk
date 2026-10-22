package app.kiosksync.feature.kiosk

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space

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
fun KioskUnlockOverlay(state: KioskLockUiState) {
    if (!state.isLocked) return
    if (!state.showUnlockHint && state.message == null && state.lockoutRemainingSec == 0) return

    val colors = KioskColors.scheme

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
                    text = Strings.KIOSK_LOCKED_TITLE,
                    style = KioskDisplayType.screenCaption(),
                    color = colors.onSurface,
                )
            }

            if (state.lockoutRemainingSec > 0) {
                Text(
                    text = Strings.kioskLockoutSeconds(state.lockoutRemainingSec),
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
                        text = Strings.KIOSK_UNLOCK_HINT,
                        style = KioskDisplayType.screenCaption(),
                        color = colors.onSurfaceVariant,
                    )
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = KioskDisplayType.screenCaption(),
                        color = colors.error,
                    )
                }

                Text(
                    text = Strings.KIOSK_RECOVERY_HINT,
                    style = KioskDisplayType.screenCaption(),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TypedDots(count: Int, color: Color) {
    Row(
        modifier = Modifier.semantics {
            contentDescription = Strings.kioskTypedCharacters(count)
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
