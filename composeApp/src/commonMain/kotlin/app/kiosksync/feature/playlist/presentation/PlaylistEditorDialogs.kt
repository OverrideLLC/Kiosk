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
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.feature.playlist.domain.PlaylistItem

@Composable
fun PlaylistEditorDialogs(
    stateHolder: PlaylistEditorStateHolder,
    state: PlaylistEditorUiState,
    removeTarget: PlaylistItem?,
    onDismissRemove: () -> Unit,
) {
    state.nameDialogMode?.let { mode ->
        PlaylistNameDialog(
            mode = mode,
            draft = state.nameDraft,
            error = state.nameError,
            onDraftChange = { stateHolder.dispatch(PlaylistEditorAction.UpdateNameDraft(it)) },
            onConfirm = { stateHolder.dispatch(PlaylistEditorAction.ConfirmNameDialog) },
            onDismiss = { stateHolder.dispatch(PlaylistEditorAction.DismissNameDialog) },
        )
    }

    if (state.showDeleteDialog) {
        AppDialog(
            title = Strings.PLAYLIST_DELETE_TITLE,
            icon = AppIcon.DELETE,
            onDismissRequest = { stateHolder.dispatch(PlaylistEditorAction.DismissDeleteDialog) },
            content = {
                Text(
                    text = "${Strings.PLAYLIST_DELETE_CONFIRM}\n\n${state.playlist.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KioskColors.scheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { stateHolder.dispatch(PlaylistEditorAction.DismissDeleteDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.PLAYLIST_DELETE,
                    onClick = { stateHolder.dispatch(PlaylistEditorAction.ConfirmDeletePlaylist) },
                    variant = AppButtonVariant.DANGER,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.DELETE,
                )
            },
        )
    }

    removeTarget?.let { item ->
        AppDialog(
            title = Strings.REMOVE_ITEM_TITLE,
            icon = AppIcon.DELETE,
            onDismissRequest = onDismissRemove,
            content = {
                Text(
                    text = "${Strings.REMOVE_ITEM_CONFIRM}\n\n${item.fileName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KioskColors.scheme.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = onDismissRemove,
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.REMOVE_ITEM,
                    onClick = {
                        val index = state.playlist.items.indexOfFirst { it.mediaId == item.mediaId }
                        if (index >= 0) stateHolder.dispatch(PlaylistEditorAction.RemoveItem(index))
                        onDismissRemove()
                    },
                    variant = AppButtonVariant.DANGER,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.DELETE,
                )
            },
        )
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
