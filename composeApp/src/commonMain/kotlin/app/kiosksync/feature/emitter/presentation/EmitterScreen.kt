package app.kiosksync.feature.emitter.presentation

import app.kiosksync.feature.emitter.presentation.actions.EmitterAction
import app.kiosksync.feature.emitter.presentation.components.*
import app.kiosksync.feature.emitter.presentation.EmitterUiState
import app.kiosksync.feature.emitter.presentation.SyncUiStatus

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.kiosksync.core.AppRole
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
import app.kiosksync.feature.guide.domain.GuideAction
import app.kiosksync.feature.guide.domain.GuideManager
import app.kiosksync.feature.guide.domain.GuideStep
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.feature.playlist.domain.PlaylistItem

@Composable
fun EmitterScreen(
    state: EmitterUiState,
    onAction: (EmitterAction) -> Unit,
    playlist: Playlist,
    onAddImages: () -> Unit,
    playlistPanel: @Composable (
        modifier: Modifier,
        fillHeight: Boolean,
        onRequestRemove: (PlaylistItem) -> Unit,
    ) -> Unit,
    playlistDialogs: @Composable (
        removeTarget: PlaylistItem?,
        onDismissRemove: () -> Unit,
    ) -> Unit,
    guideManager: GuideManager,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit,
    theme: AppTheme = AppTheme.DARK,
    reduceMotion: Boolean = false,
    onThemeChange: (AppTheme) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {},
) {
    val guideState by guideManager.uiState.collectAsState()
    val currentPlaylist by rememberUpdatedState(playlist)
    val currentOnAction by rememberUpdatedState(onAction)
    val currentOnAddImages by rememberUpdatedState(onAddImages)
    var showChangeRoleConfirm by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<PlaylistItem?>(null) }
    // Below the wide breakpoint the two panels share the window as tabs so
    // neither list gets clipped by a fixed height.
    var compactTab by remember { mutableStateOf(0) }

    val colors = KioskColors.scheme
    val selectedCount = state.devices.count { it.isSelected && it.isPairedToThisEmitter }
    val sendHint = when {
        playlist.items.isEmpty() -> Strings.SEND_EMPTY_PLAYLIST
        selectedCount == 0 -> Strings.SEND_NO_SCREENS
        else -> Strings.selectedScreens(selectedCount)
    }
    val pairedCount = state.devices.count { it.isPairedToThisEmitter }
    val selectedIds = state.devices
        .filter { it.isSelected && it.isPairedToThisEmitter }
        .map { it.deviceId }
        .toSet()
    val selectedSendSucceeded = selectedIds.isNotEmpty() &&
        selectedIds.all { deviceId -> state.syncByDevice[deviceId]?.status == SyncUiStatus.SUCCESS }
    val canSend = !state.isSending &&
        !state.isSyncing &&
        selectedCount > 0 &&
        playlist.items.isNotEmpty()
    val canProject = !state.isSending &&
        !state.isSyncing &&
        playlist.items.isNotEmpty()
    val canSync = !state.isSending && !state.isSyncing && pairedCount > 0

    LaunchedEffect(guideState.step) {
        when (guideState.step) {
            GuideStep.EMITTER_PLAYLIST -> compactTab = 1
            GuideStep.EMITTER_SCREENS,
            GuideStep.EMITTER_PASSWORD,
            GuideStep.EMITTER_SEND -> compactTab = 0

            else -> Unit
        }
    }

    LaunchedEffect(
        playlist.items.size,
        pairedCount,
        selectedCount,
        state.hasKioskPassword,
        selectedSendSucceeded,
    ) {
        guideManager.updateEmitter(
            playlistItems = playlist.items.size,
            pairedScreens = pairedCount,
            selectedScreens = selectedCount,
            hasPassword = state.hasKioskPassword,
            sendSucceeded = selectedSendSucceeded,
        )
    }

    LaunchedEffect(guideState.step, guideState.isActive, pairedCount, selectedCount) {
        if (
            guideState.isActive &&
            guideState.role == AppRole.EMITTER &&
            guideState.step == GuideStep.EMITTER_SEND &&
            selectedCount == 0
        ) {
            state.devices
                .firstOrNull { it.isPairedToThisEmitter && !it.isSelected }
                ?.let { onAction(EmitterAction.ToggleSelection(it.deviceId)) }
        }
    }

    LaunchedEffect(guideManager) {
        guideManager.actions.collect { action ->
            when (action) {
                GuideAction.ADD_IMAGES -> currentOnAddImages()
                GuideAction.REFRESH_DEVICES -> currentOnAction(EmitterAction.RefreshDevices)
                GuideAction.SEND -> currentOnAction(
                    EmitterAction.SendToSelected(currentPlaylist)
                )
            }
        }
    }

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
                onOpenSecurity = { onAction(EmitterAction.OpenSecurityDialog) },
                onOpenSettings = { showSettings = true },
                onChangeRole = { showChangeRoleConfirm = true },
                onExit = onExitRequested,
            )

            state.statusMessage?.let { message ->
                Spacer(Modifier.height(Space.sm))
                StatusBanner(
                    message = message,
                    isError = state.statusIsError,
                    onDismiss = { onAction(EmitterAction.DismissStatusMessage) },
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
                        onAction = onAction,
                        state = state,
                        fillHeight = true,
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    )
                    Spacer(Modifier.width(Space.lg))
                    playlistPanel(
                        Modifier.weight(0.58f).fillMaxHeight(),
                        true,
                    ) { removeTarget = it }
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
                            onAction = onAction,
                            state = state,
                            fillHeight = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        playlistPanel(Modifier.fillMaxSize(), true) { removeTarget = it }
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
                    onAction(EmitterAction.SendToSelected(playlist))
                },
                onProject = {
                    onAction(EmitterAction.RequestProjection(playlist))
                },
                onSync = { onAction(EmitterAction.SynchronizePlayback) },
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
            onOpenGuide = {
                showSettings = false
                guideManager.start(AppRole.EMITTER)
            },
            onDismiss = { showSettings = false },
        )
    }

    state.passwordDialog?.let { dialog ->
        KioskPasswordDialog(
            dialog = dialog,
            onDraftChange = { onAction(EmitterAction.UpdatePasswordDraft(it)) },
            onConfirmDraftChange = { onAction(EmitterAction.UpdatePasswordConfirmDraft(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmPassword) },
            onDismiss = { onAction(EmitterAction.DismissPasswordDialog) },
        )
    }

    state.securityDialog?.let { dialog ->
        SecurityDialog(
            dialog = dialog,
            hasPassword = state.hasKioskPassword,
            onAddKey = { onAction(EmitterAction.AppendUnlockKey(it)) },
            onRemoveLast = { onAction(EmitterAction.RemoveLastUnlockKey) },
            onResetSequence = { onAction(EmitterAction.ResetUnlockSequence) },
            onSaveSequence = { onAction(EmitterAction.SaveUnlockSequence) },
            onToggleRecovery = { onAction(EmitterAction.ToggleRecoveryEditor) },
            onAcceptedChange = { onAction(EmitterAction.SetRecoveryAccepted(it)) },
            onRecoveryChange = { onAction(EmitterAction.UpdateRecoveryDraft(it)) },
            onRecoveryConfirmChange = { onAction(EmitterAction.UpdateRecoveryConfirmDraft(it)) },
            onSaveRecovery = { onAction(EmitterAction.SaveRecoveryPhrase) },
            onResetRecovery = { onAction(EmitterAction.ResetRecoveryPhrase) },
            onChangePassword = { onAction(EmitterAction.OpenChangePasswordDialog) },
            onDismiss = { onAction(EmitterAction.DismissSecurityDialog) },
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
            onCodeChange = { onAction(EmitterAction.UpdatePairingCode(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmPair) },
            onDismiss = { onAction(EmitterAction.DismissPairDialog) },
        )
    }

    state.unpairDevice?.let { device ->
        AppDialog(
            title = Strings.UNPAIR_DIALOG_TITLE,
            icon = AppIcon.LINK,
            onDismissRequest = { onAction(EmitterAction.DismissUnpairDialog) },
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
                    onClick = { onAction(EmitterAction.DismissUnpairDialog) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.UNPAIR_BUTTON,
                    onClick = { onAction(EmitterAction.ConfirmUnpair) },
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
            onValueChange = { onAction(EmitterAction.UpdateManualIp(it)) },
            onConfirm = { onAction(EmitterAction.ConfirmManualIp) },
            onDismiss = { onAction(EmitterAction.DismissManualIpDialog) },
        )
    }

    playlistDialogs(removeTarget) { removeTarget = null }
}
