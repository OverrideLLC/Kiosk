package app.kiosksync.feature.emitter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.kiosksync.core.AppTheme
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.components.OverrideFooter
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.feature.playlist.presentation.PlaylistEditorAction
import app.kiosksync.feature.playlist.presentation.PlaylistEditorEffect
import app.kiosksync.feature.playlist.presentation.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.domain.PlaylistItem
import app.kiosksync.feature.playlist.presentation.PlaylistPanel
import app.kiosksync.platform.MediaRepository
import kotlinx.coroutines.CancellationException

@Composable
fun EmitterScreen(
    stateHolder: EmitterStateHolder,
    playlistStateHolder: PlaylistEditorStateHolder,
    mediaRepository: MediaRepository,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit,
    theme: AppTheme = AppTheme.DARK,
    reduceMotion: Boolean = false,
    onThemeChange: (AppTheme) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {},
    pickImageFiles: suspend () -> List<String> = { emptyList() },
) {
    val state by stateHolder.uiState.collectAsState()
    val editorState by playlistStateHolder.uiState.collectAsState()
    var showChangeRoleConfirm by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<PlaylistItem?>(null) }
    // Below the wide breakpoint the two panels share the window as tabs so
    // neither list gets clipped by a fixed height.
    var compactTab by remember { mutableStateOf(0) }

    LaunchedEffect(playlistStateHolder, pickImageFiles) {
        playlistStateHolder.effects.collect { effect ->
            when (effect) {
                PlaylistEditorEffect.SelectImageFiles -> try {
                    playlistStateHolder.dispatch(
                        PlaylistEditorAction.ImagesSelected(pickImageFiles())
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    playlistStateHolder.dispatch(PlaylistEditorAction.ImagePickerFailed(error))
                }
            }
        }
    }

    val colors = KioskColors.scheme
    val selectedCount = state.devices.count { it.isSelected && it.isPairedToThisEmitter }
    val sendHint = when {
        editorState.playlist.items.isEmpty() -> Strings.SEND_EMPTY_PLAYLIST
        selectedCount == 0 -> Strings.SEND_NO_SCREENS
        else -> Strings.selectedScreens(selectedCount)
    }
    val pairedCount = state.devices.count { it.isPairedToThisEmitter }
    val canSend = !state.isSending &&
        !state.isSyncing &&
        selectedCount > 0 &&
        editorState.playlist.items.isNotEmpty()
    val canProject = !state.isSending &&
        !state.isSyncing &&
        editorState.playlist.items.isNotEmpty()
    val canSync = !state.isSending && !state.isSyncing && pairedCount > 0

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        val wide = maxWidth >= Sizes.wideLayoutBreakpoint

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Space.lg, vertical = Space.md),
        ) {
            ControllerHeader(
                hasPassword = state.hasKioskPassword,
                onOpenSecurity = { stateHolder.dispatch(EmitterAction.OpenSecurityDialog) },
                onOpenSettings = { showSettings = true },
                onChangeRole = { showChangeRoleConfirm = true },
                onExit = onExitRequested,
            )

            state.statusMessage?.let { message ->
                Spacer(Modifier.height(Space.sm))
                StatusBanner(
                    message = message,
                    isError = state.statusIsError,
                    onDismiss = { stateHolder.dispatch(EmitterAction.DismissStatusMessage) },
                )
            }

            Spacer(Modifier.height(Space.md))

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    ScreensPanel(
                        stateHolder = stateHolder,
                        state = state,
                        fillHeight = true,
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    )
                    Spacer(Modifier.width(Space.lg))
                    PlaylistPanel(
                        stateHolder = playlistStateHolder,
                        state = editorState,
                        mediaRepository = mediaRepository,
                        fillHeight = true,
                        onRequestRemove = { removeTarget = it },
                        modifier = Modifier.weight(0.58f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    AppButtonGroup(
                        options = listOf(
                            Strings.EMITTER_SCREENS_TITLE,
                            Strings.PLAYLIST_TITLE,
                        ),
                        selectedIndex = compactTab,
                        onSelect = { compactTab = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Space.sm))
                    if (compactTab == 0) {
                        ScreensPanel(
                            stateHolder = stateHolder,
                            state = state,
                            fillHeight = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PlaylistPanel(
                            stateHolder = playlistStateHolder,
                            state = editorState,
                            mediaRepository = mediaRepository,
                            fillHeight = true,
                            onRequestRemove = { removeTarget = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Space.md))

            SendPanel(
                hint = sendHint,
                isSending = state.isSending,
                isSyncing = state.isSyncing,
                canSend = canSend,
                canProject = canProject,
                canSync = canSync,
                sendDisabledReason = if (canSend) null else sendHint,
                syncDisabledReason = if (canSync) null else Strings.PLAYBACK_SYNC_NO_SCREENS,
                onSend = {
                    stateHolder.dispatch(EmitterAction.SendToSelected(playlistStateHolder.currentPlaylist()))
                },
                onProject = {
                    stateHolder.dispatch(EmitterAction.RequestProjection(playlistStateHolder.currentPlaylist()))
                },
                onSync = { stateHolder.dispatch(EmitterAction.SynchronizePlayback) },
            )

            Spacer(Modifier.height(Space.xs))
            OverrideFooter()
        }
    }

    if (showSettings) {
        SettingsDialog(
            theme = theme,
            reduceMotion = reduceMotion,
            onThemeChange = onThemeChange,
            onReduceMotionChange = onReduceMotionChange,
            onDismiss = { showSettings = false },
        )
    }

    state.passwordDialog?.let { dialog ->
        KioskPasswordDialog(
            dialog = dialog,
            onDraftChange = { stateHolder.dispatch(EmitterAction.UpdatePasswordDraft(it)) },
            onConfirmDraftChange = { stateHolder.dispatch(EmitterAction.UpdatePasswordConfirmDraft(it)) },
            onConfirm = { stateHolder.dispatch(EmitterAction.ConfirmPassword) },
            onDismiss = { stateHolder.dispatch(EmitterAction.DismissPasswordDialog) },
        )
    }

    state.securityDialog?.let { dialog ->
        SecurityDialog(
            dialog = dialog,
            hasPassword = state.hasKioskPassword,
            onAddKey = { stateHolder.dispatch(EmitterAction.AppendUnlockKey(it)) },
            onRemoveLast = { stateHolder.dispatch(EmitterAction.RemoveLastUnlockKey) },
            onResetSequence = { stateHolder.dispatch(EmitterAction.ResetUnlockSequence) },
            onSaveSequence = { stateHolder.dispatch(EmitterAction.SaveUnlockSequence) },
            onToggleRecovery = { stateHolder.dispatch(EmitterAction.ToggleRecoveryEditor) },
            onAcceptedChange = { stateHolder.dispatch(EmitterAction.SetRecoveryAccepted(it)) },
            onRecoveryChange = { stateHolder.dispatch(EmitterAction.UpdateRecoveryDraft(it)) },
            onRecoveryConfirmChange = { stateHolder.dispatch(EmitterAction.UpdateRecoveryConfirmDraft(it)) },
            onSaveRecovery = { stateHolder.dispatch(EmitterAction.SaveRecoveryPhrase) },
            onResetRecovery = { stateHolder.dispatch(EmitterAction.ResetRecoveryPhrase) },
            onChangePassword = { stateHolder.dispatch(EmitterAction.OpenChangePasswordDialog) },
            onDismiss = { stateHolder.dispatch(EmitterAction.DismissSecurityDialog) },
        )
    }

    editorState.nameDialogMode?.let { mode ->
        PlaylistNameDialog(
            mode = mode,
            draft = editorState.nameDraft,
            error = editorState.nameError,
            onDraftChange = { playlistStateHolder.dispatch(PlaylistEditorAction.UpdateNameDraft(it)) },
            onConfirm = { playlistStateHolder.dispatch(PlaylistEditorAction.ConfirmNameDialog) },
            onDismiss = { playlistStateHolder.dispatch(PlaylistEditorAction.DismissNameDialog) },
        )
    }

    if (editorState.showDeleteDialog) {
        AppDialog(
            title = Strings.PLAYLIST_DELETE_TITLE,
            icon = AppIcon.DELETE,
            onDismissRequest = { playlistStateHolder.dispatch(PlaylistEditorAction.DismissDeleteDialog) },
            content = {
                Text(
                    text = "${Strings.PLAYLIST_DELETE_CONFIRM}\n\n${editorState.playlist.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { playlistStateHolder.dispatch(PlaylistEditorAction.DismissDeleteDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.PLAYLIST_DELETE,
                    onClick = { playlistStateHolder.dispatch(PlaylistEditorAction.ConfirmDeletePlaylist) },
                    variant = AppButtonVariant.DANGER,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.DELETE,
                )
            },
        )
    }

    if (showChangeRoleConfirm) {
        AppDialog(
            title = Strings.CHANGE_ROLE,
            icon = AppIcon.SWAP_HORIZ,
            onDismissRequest = { showChangeRoleConfirm = false },
            content = {
                Text(
                    text = Strings.CHANGE_ROLE_CONFIRM,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { showChangeRoleConfirm = false },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.CHANGE_ROLE,
                    onClick = {
                        showChangeRoleConfirm = false
                        onRoleReset()
                    },
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.SWAP_HORIZ,
                )
            },
        )
    }

    state.pairingDevice?.let { device ->
        PairScreenDialog(
            device = device,
            code = state.pairingCode,
            error = state.pairingError,
            isPairing = state.isPairing,
            onCodeChange = { stateHolder.dispatch(EmitterAction.UpdatePairingCode(it)) },
            onConfirm = { stateHolder.dispatch(EmitterAction.ConfirmPair) },
            onDismiss = { stateHolder.dispatch(EmitterAction.DismissPairDialog) },
        )
    }

    state.unpairDevice?.let { device ->
        AppDialog(
            title = Strings.UNPAIR_DIALOG_TITLE,
            icon = AppIcon.LINK,
            onDismissRequest = { stateHolder.dispatch(EmitterAction.DismissUnpairDialog) },
            content = {
                Text(
                    text = "${Strings.UNPAIR_DIALOG_CONFIRM}\n\n${device.deviceName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { stateHolder.dispatch(EmitterAction.DismissUnpairDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.UNPAIR_BUTTON,
                    onClick = { stateHolder.dispatch(EmitterAction.ConfirmUnpair) },
                    variant = AppButtonVariant.DANGER,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.LINK,
                )
            },
        )
    }

    if (state.showManualIpDialog) {
        ManualIpDialog(
            value = state.manualIpInput,
            error = state.manualIpError,
            isLoading = state.isManualIpLoading,
            onValueChange = { stateHolder.dispatch(EmitterAction.UpdateManualIp(it)) },
            onConfirm = { stateHolder.dispatch(EmitterAction.ConfirmManualIp) },
            onDismiss = { stateHolder.dispatch(EmitterAction.DismissManualIpDialog) },
        )
    }

    removeTarget?.let { item ->
        AppDialog(
            title = Strings.REMOVE_ITEM_TITLE,
            icon = AppIcon.DELETE,
            onDismissRequest = { removeTarget = null },
            content = {
                Text(
                    text = "${Strings.REMOVE_ITEM_CONFIRM}\n\n${item.fileName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            },
            actions = {
                AppButton(
                    text = Strings.CANCEL_BUTTON,
                    onClick = { removeTarget = null },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.REMOVE_ITEM,
                    onClick = {
                        val index = editorState.playlist.items.indexOfFirst { it.mediaId == item.mediaId }
                        if (index >= 0) playlistStateHolder.dispatch(PlaylistEditorAction.RemoveItem(index))
                        removeTarget = null
                    },
                    variant = AppButtonVariant.DANGER,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.DELETE,
                )
            },
        )
    }
}
