package app.kiosksync.feature.playlist.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.feature.playlist.domain.models.PlaylistNameDialogMode
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorState
import app.kiosksync.resources.Res
import app.kiosksync.resources.cancel_button
import app.kiosksync.resources.playlist_delete
import app.kiosksync.resources.playlist_delete_confirm
import app.kiosksync.resources.playlist_delete_title
import app.kiosksync.resources.playlist_name_field
import app.kiosksync.resources.playlist_name_hint
import app.kiosksync.resources.playlist_new_title
import app.kiosksync.resources.playlist_rename_title
import app.kiosksync.resources.remove_item
import app.kiosksync.resources.remove_item_confirm
import app.kiosksync.resources.remove_item_title
import app.kiosksync.resources.save_button
import org.jetbrains.compose.resources.stringResource
import app.kiosksync.core.ui.resolve

@Composable
fun PlaylistEditorDialogs(
    state: PlaylistEditorState,
    onAction: (PlaylistEditorAction) -> Unit,
) {
    state.nameDialogMode?.let { mode ->
        PlaylistNameDialog(
            mode = mode,
            draft = state.nameDraft,
            error = state.nameError?.resolve(),
            onDraftChange = { onAction(PlaylistEditorAction.UpdateNameDraft(it)) },
            onConfirm = { onAction(PlaylistEditorAction.ConfirmNameDialog) },
            onDismiss = { onAction(PlaylistEditorAction.DismissNameDialog) },
        )
    }

    if (state.showDeleteDialog) {
        AppDialog(
            title = stringResource(Res.string.playlist_delete_title),
            icon = AppIcon.DELETE,
            onDismissRequest = { onAction(PlaylistEditorAction.DismissDeleteDialog) },
            content = {
                Text(
                    text = "${stringResource(Res.string.playlist_delete_confirm)}\n\n${state.playlist.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = stringResource(Res.string.cancel_button),
                    onClick = { onAction(PlaylistEditorAction.DismissDeleteDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = stringResource(Res.string.playlist_delete),
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
                title = stringResource(Res.string.remove_item_title),
                icon = AppIcon.DELETE,
                onDismissRequest = { onAction(PlaylistEditorAction.DismissRemoveItem) },
                content = {
                    Text(
                        text = "${stringResource(Res.string.remove_item_confirm)}\n\n${item.fileName}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                actions = {
                    AppButton(
                        text = stringResource(Res.string.cancel_button),
                        onClick = { onAction(PlaylistEditorAction.DismissRemoveItem) },
                        variant = AppButtonVariant.TEXT,
                        size = AppButtonSize.SMALL,
                    )
                    AppButton(
                        text = stringResource(Res.string.remove_item),
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
        title = if (creating) stringResource(Res.string.playlist_new_title) else stringResource(Res.string.playlist_rename_title),
        icon = if (creating) AppIcon.PLAYLIST_ADD else AppIcon.EDIT,
        onDismissRequest = onDismiss,
        content = {
            AppTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = stringResource(Res.string.playlist_name_field),
                placeholder = stringResource(Res.string.playlist_name_hint),
                error = error,
            )
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.cancel_button),
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
            AppButton(
                text = stringResource(Res.string.save_button),
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.CHECK,
            )
        },
    )
}
