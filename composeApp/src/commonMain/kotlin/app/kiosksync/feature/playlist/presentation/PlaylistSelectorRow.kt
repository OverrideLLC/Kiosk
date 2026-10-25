package app.kiosksync.feature.playlist.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppDropdownMenu
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppMenuItem
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space

@Composable
internal fun PlaylistSelectorRow(
    state: PlaylistEditorUiState,
    onAction: (PlaylistEditorAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Strings.PLAYLIST_SELECTOR_LABEL,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xxs))
            Box {
                Surface(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = KioskShape.field,
                    color = colors.surface,
                    contentColor = colors.onSurface,
                    border = BorderStroke(Sizes.hairline, colors.outline),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizes.buttonSmall)
                            .padding(horizontal = Space.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(
                            icon = AppIcon.PLAYLIST_PLAY,
                            contentDescription = null,
                            size = Sizes.iconSmall,
                            tint = colors.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            text = state.playlist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        AppIcon(
                            icon = AppIcon.EXPAND_MORE,
                            contentDescription = null,
                            size = Sizes.iconCompact,
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
                AppDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    items = state.playlists.map { playlist ->
                        val active = playlist.id == state.activePlaylistId
                        AppMenuItem(
                            label = playlist.name,
                            icon = if (active) AppIcon.CHECK else null,
                            onClick = { onAction(PlaylistEditorAction.SelectPlaylist(playlist.id)) },
                        )
                    },
                )
            }
        }
        Spacer(Modifier.width(Space.xs))
        AppIconButton(
            icon = AppIcon.ADD,
            contentDescription = Strings.PLAYLIST_NEW,
            onClick = { onAction(PlaylistEditorAction.OpenCreateDialog) },
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.PLAYLIST_NEW,
        )
        AppIconButton(
            icon = AppIcon.EDIT,
            contentDescription = Strings.PLAYLIST_RENAME,
            onClick = { onAction(PlaylistEditorAction.OpenRenameDialog) },
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.PLAYLIST_RENAME,
        )
        AppIconButton(
            icon = AppIcon.DELETE,
            contentDescription = Strings.PLAYLIST_DELETE,
            onClick = { onAction(PlaylistEditorAction.RequestDeletePlaylist) },
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.PLAYLIST_DELETE,
            tint = colors.error,
        )
    }
}

