package app.kiosksync.feature.emitter.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space

@Composable
internal fun SendPanel(
    hint: String,
    isSending: Boolean,
    isSyncing: Boolean,
    canSend: Boolean,
    canProject: Boolean,
    canSync: Boolean,
    sendDisabledReason: String?,
    syncDisabledReason: String?,
    onSend: () -> Unit,
    onProject: () -> Unit,
    onSync: () -> Unit,
) {
    val colors = KioskColors.scheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KioskShape.card,
        color = colors.surfaceContainerLow,
        border = BorderStroke(Sizes.hairline, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Space.md,
                vertical = Space.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.SEND_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Strings.PROJECT_SHORTCUT_HINT,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = Strings.PLAYBACK_SYNC_HINT,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Space.sm))
            AppButton(
                text = Strings.PROJECT_BUTTON,
                onClick = onProject,
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.MEDIUM,
                icon = AppIcon.PRESENT_TO_ALL,
                enabled = canProject,
                disabledReason = if (canProject) null else Strings.PROJECT_NO_PLAYLIST,
            )
            Spacer(Modifier.width(Space.xs))
            AppButton(
                text = if (isSyncing) Strings.PLAYBACK_SYNCING else Strings.PLAYBACK_SYNC_BUTTON,
                onClick = onSync,
                variant = AppButtonVariant.TONAL,
                size = AppButtonSize.MEDIUM,
                icon = AppIcon.SYNC,
                enabled = canSync,
                loading = isSyncing,
                disabledReason = syncDisabledReason,
            )
            Spacer(Modifier.width(Space.xs))
            AppButton(
                text = if (isSending) Strings.SENDING else Strings.SEND_BUTTON,
                onClick = onSend,
                variant = AppButtonVariant.FILLED,
                size = AppButtonSize.MEDIUM,
                icon = AppIcon.SEND,
                enabled = canSend,
                loading = isSending,
                disabledReason = sendDisabledReason,
            )
        }
    }
}
