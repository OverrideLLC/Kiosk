package app.kiosksync.feature.playlist.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppTextField

@Composable
fun PlaylistEditorDialogs(
    state: PlaylistEditorUiState,
    onAction: (PlaylistEditorAction) -> Unit,
) {
    state.nameDialogMode?.let { mode ->
        PlaylistNameDialog(
            mode = mode,
            draft = state.nameDraft,
            error = state.nameError,
            onDraftChange = { onAction(PlaylistEditorAction.UpdateNameDraft(it)) },
            onConfirm = { onAction(PlaylistEditorAction.ConfirmNameDialog) },
            onDismiss = { onAction(PlaylistEditorAction.DismissNameDialog) },
        )
    }

    if (state.showDeleteDialog) {
        AppDialog(
            title = Strings.PLAYLIST_DELETE_TITLE,
            icon = AppIcon.DELETE,
            onDismissRequest = { onAction(PlaylistEditorAction.DismissDeleteDialog) },
            content = {
                Text(
                    text = "${Strings.PLAYLIST_DELETE_CONFIRM}\n\n${state.playlist.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { onAction(PlaylistEditorAction.DismissDeleteDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.PLAYLIST_DELETE,
                    onClick = { onAction(PlaylistEditorAction.ConfirmDeletePlaylist) },
                    variant = AppButtonVariant.DANGER,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.DELETE,
                )
            },
        )
    }

    state.removeItemId?.let { mediaId ->
        val item = state.playlist.items.firstOrNull { it.mediaId == mediaId }
        if (item != null) {
            AppDialog(
                title = Strings.REMOVE_ITEM_TITLE,
                icon = AppIcon.DELETE,
                onDismissRequest = { onAction(PlaylistEditorAction.DismissRemoveItem) },
                content = {
                    Text(
                        text = "${Strings.REMOVE_ITEM_CONFIRM}\n\n${item.fileName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                actions = {
                    AppButton(
                        text = Strings.CANCEL_BUTTON,
                        onClick = { onAction(PlaylistEditorAction.DismissRemoveItem) },
                        variant = AppButtonVariant.TEXT,
                        size = AppButtonSize.SMALL,
                    )
                    AppButton(
                        text = Strings.REMOVE_ITEM,
                        onClick = { onAction(PlaylistEditorAction.ConfirmRemoveItem) },
                        variant = AppButtonVariant.DANGER,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.DELETE,
                    )
                },
            )
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    mode: PlaylistNameDialogMode,
    draft: String,
    error: String?,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val creating = mode == PlaylistNameDialogMode.CREATE
    AppDialog(
        title = if (creating) Strings.PLAYLIST_NEW_TITLE else Strings.PLAYLIST_RENAME_TITLE,
        icon = if (creating) AppIcon.PLAYLIST_ADD else AppIcon.EDIT,
        onDismissRequest = onDismiss,
        content = {
            AppTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = Strings.PLAYLIST_NAME_FIELD,
                placeholder = Strings.PLAYLIST_NAME_HINT,
                error = error,
            )
        },
        actions = {
            AppButton(
                text = Strings.CANCEL_BUTTON,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
            AppButton(
                text = Strings.SAVE_BUTTON,
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.CHECK,
            )
        },
    )
}
