package app.kiosksync.feature.emitter.presentation.components

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
import app.kiosksync.feature.emitter.presentation.EmitterAction
import app.kiosksync.feature.emitter.presentation.EmitterState
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.resources.Res
import app.kiosksync.resources.playback_sync_button
import app.kiosksync.resources.playback_sync_hint
import app.kiosksync.resources.playback_sync_no_screens
import app.kiosksync.resources.playback_syncing
import app.kiosksync.resources.project_button
import app.kiosksync.resources.project_no_playlist
import app.kiosksync.resources.project_shortcut_hint
import app.kiosksync.resources.send_button
import app.kiosksync.resources.send_title
import app.kiosksync.resources.sending
import org.jetbrains.compose.resources.stringResource
import app.kiosksync.core.ui.resolve

@Composable
internal fun SendPanel(
    state: EmitterState,
    onAction: (EmitterAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

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
                    text = stringResource(Res.string.send_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = state.sendHint.resolve(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.project_shortcut_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.playback_sync_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Space.sm))
            AppButton(
                text = stringResource(Res.string.project_button),
                onClick = { onAction(EmitterAction.RequestProjection) },
                variant = AppButtonVariant.OUTLINED,
                size = AppButtonSize.MEDIUM,
                icon = AppIcon.PRESENT_TO_ALL,
                enabled = state.canProject,
                disabledReason = if (state.canProject) null else stringResource(Res.string.project_no_playlist),
            )
            Spacer(Modifier.width(Space.xs))
            AppButton(
                text = if (state.isSyncing) stringResource(Res.string.playback_syncing) else stringResource(Res.string.playback_sync_button),
                onClick = { onAction(EmitterAction.SynchronizePlayback) },
                variant = AppButtonVariant.TONAL,
                size = AppButtonSize.MEDIUM,
                icon = AppIcon.SYNC,
                enabled = state.canSync,
                loading = state.isSyncing,
                disabledReason = if (state.canSync) null else stringResource(Res.string.playback_sync_no_screens),
            )
            Spacer(Modifier.width(Space.xs))
            AppButton(
                text = if (state.isSending) stringResource(Res.string.sending) else stringResource(Res.string.send_button),
                onClick = { onAction(EmitterAction.SendToSelected) },
                variant = AppButtonVariant.FILLED,
                size = AppButtonSize.MEDIUM,
                icon = AppIcon.SEND,
                enabled = state.canSend,
                loading = state.isSending,
                disabledReason = if (state.canSend) null else state.sendHint.resolve(),
            )
        }
    }
}
