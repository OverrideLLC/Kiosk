package app.kiosksync.feature.pairing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppTheme
import app.kiosksync.core.MAX_DURATION_SEC
import app.kiosksync.core.MIN_DURATION_SEC
import app.kiosksync.core.Strings
import app.kiosksync.core.formatUnlockSequence
import app.kiosksync.designsystem.AppButton
import app.kiosksync.designsystem.AppButtonGroup
import app.kiosksync.designsystem.AppButtonSize
import app.kiosksync.designsystem.AppButtonVariant
import app.kiosksync.designsystem.AppDialog
import app.kiosksync.designsystem.AppDropdownMenu
import app.kiosksync.designsystem.AppEmptyState
import app.kiosksync.designsystem.AppIcon
import app.kiosksync.designsystem.AppIconButton
import app.kiosksync.designsystem.AppIconButtonSize
import app.kiosksync.designsystem.AppLinearProgress
import app.kiosksync.designsystem.AppLoadingIndicator
import app.kiosksync.designsystem.AppMenuItem
import app.kiosksync.designsystem.AppStatusChip
import app.kiosksync.designsystem.AppStatusTone
import app.kiosksync.designsystem.AppStepper
import app.kiosksync.designsystem.AppTextField
import app.kiosksync.designsystem.KioskColors
import app.kiosksync.designsystem.KioskScreenColors
import app.kiosksync.designsystem.KioskShape
import app.kiosksync.designsystem.Sizes
import app.kiosksync.designsystem.Space
import app.kiosksync.feature.playlist.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.PlaylistEditorUiState
import app.kiosksync.feature.playlist.PlaylistItem
import app.kiosksync.feature.playlist.PlaylistNameDialogMode
import app.kiosksync.platform.LocalStorage

/**
 * Controller dashboard. One window, two panels (screens + playlist) and a
 * persistent send bar. Every colour, shape, spacing and animation comes from
 * the design system; the functional events are the same as in the previous UI.
 */
@Composable
fun EmitterScreen(
    stateHolder: EmitterStateHolder,
    playlistStateHolder: PlaylistEditorStateHolder,
    storage: LocalStorage,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit,
    theme: AppTheme = AppTheme.DARK,
    reduceMotion: Boolean = false,
    onThemeChange: (AppTheme) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {},
) {
    val state by stateHolder.uiState.collectAsState()
    val editorState by playlistStateHolder.uiState.collectAsState()
    var showChangeRoleConfirm by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<PlaylistItem?>(null) }
    // Below the wide breakpoint the two panels share the window as tabs so
    // neither list gets clipped by a fixed height.
    var compactTab by remember { mutableStateOf(0) }

    val colors = KioskColors.scheme
    val selectedCount = state.devices.count { it.isSelected && it.isPairedToThisEmitter }
    val sendHint = when {
        editorState.playlist.items.isEmpty() -> Strings.SEND_EMPTY_PLAYLIST
        selectedCount == 0 -> Strings.SEND_NO_SCREENS
        else -> Strings.selectedScreens(selectedCount)
    }
    val canSend = !state.isSending &&
        selectedCount > 0 &&
        editorState.playlist.items.isNotEmpty()
    val canProject = !state.isSending && editorState.playlist.items.isNotEmpty()

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
                onOpenSecurity = { stateHolder.openSecurityDialog() },
                onOpenSettings = { showSettings = true },
                onChangeRole = { showChangeRoleConfirm = true },
                onExit = onExitRequested,
            )

            state.statusMessage?.let { message ->
                Spacer(Modifier.height(Space.sm))
                StatusBanner(
                    message = message,
                    isError = state.statusIsError,
                    onDismiss = { stateHolder.dismissStatusMessage() },
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
                        storage = storage,
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
                            storage = storage,
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
                canSend = canSend,
                canProject = canProject,
                sendDisabledReason = if (canSend) null else sendHint,
                onSend = {
                    stateHolder.sendToSelected(playlistStateHolder.currentPlaylist())
                },
                onProject = {
                    stateHolder.requestProjection(playlistStateHolder.currentPlaylist())
                },
            )
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
            onDraftChange = { stateHolder.updatePasswordDraft(it) },
            onConfirmDraftChange = { stateHolder.updatePasswordConfirmDraft(it) },
            onConfirm = { stateHolder.confirmPassword() },
            onDismiss = { stateHolder.dismissPasswordDialog() },
        )
    }

    state.securityDialog?.let { dialog ->
        SecurityDialog(
            dialog = dialog,
            hasPassword = state.hasKioskPassword,
            onAddKey = { stateHolder.appendUnlockKey(it) },
            onRemoveLast = { stateHolder.removeLastUnlockKey() },
            onResetSequence = { stateHolder.resetUnlockSequence() },
            onSaveSequence = { stateHolder.saveUnlockSequence() },
            onToggleRecovery = { stateHolder.toggleRecoveryEditor() },
            onAcceptedChange = { stateHolder.setRecoveryAccepted(it) },
            onRecoveryChange = { stateHolder.updateRecoveryDraft(it) },
            onRecoveryConfirmChange = { stateHolder.updateRecoveryConfirmDraft(it) },
            onSaveRecovery = { stateHolder.saveRecoveryPhrase() },
            onResetRecovery = { stateHolder.resetRecoveryPhrase() },
            onChangePassword = { stateHolder.openChangePasswordDialog() },
            onDismiss = { stateHolder.dismissSecurityDialog() },
        )
    }

    editorState.nameDialogMode?.let { mode ->
        PlaylistNameDialog(
            mode = mode,
            draft = editorState.nameDraft,
            error = editorState.nameError,
            onDraftChange = { playlistStateHolder.updateNameDraft(it) },
            onConfirm = { playlistStateHolder.confirmNameDialog() },
            onDismiss = { playlistStateHolder.dismissNameDialog() },
        )
    }

    if (editorState.showDeleteDialog) {
        AppDialog(
            title = Strings.PLAYLIST_DELETE_TITLE,
            icon = AppIcon.DELETE,
            onDismissRequest = { playlistStateHolder.dismissDeletePlaylist() },
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
                    onClick = { playlistStateHolder.dismissDeletePlaylist() },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.PLAYLIST_DELETE,
                    onClick = { playlistStateHolder.confirmDeletePlaylist() },
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
            onCodeChange = { stateHolder.updatePairingCode(it) },
            onConfirm = { stateHolder.confirmPair() },
            onDismiss = { stateHolder.dismissPairDialog() },
        )
    }

    state.unpairDevice?.let { device ->
        AppDialog(
            title = Strings.UNPAIR_DIALOG_TITLE,
            icon = AppIcon.LINK,
            onDismissRequest = { stateHolder.dismissUnpairDialog() },
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
                    onClick = { stateHolder.dismissUnpairDialog() },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                )
                AppButton(
                    text = Strings.UNPAIR_BUTTON,
                    onClick = { stateHolder.confirmUnpair() },
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
            onValueChange = { stateHolder.updateManualIp(it) },
            onConfirm = { stateHolder.confirmManualIp() },
            onDismiss = { stateHolder.dismissManualIpDialog() },
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
                        if (index >= 0) playlistStateHolder.removeItem(index)
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

// ---------------------------------------------------------------------------
// Chrome
// ---------------------------------------------------------------------------

@Composable
private fun ControllerHeader(
    hasPassword: Boolean,
    onOpenSecurity: () -> Unit,
    onOpenSettings: () -> Unit,
    onChangeRole: () -> Unit,
    onExit: () -> Unit,
) {
    val colors = KioskColors.scheme
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.avatar)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                icon = AppIcon.CAST,
                contentDescription = null,
                size = Sizes.iconCompact,
                tint = colors.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(Space.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = APP_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                )
                Spacer(Modifier.width(Space.xs))
                AppStatusChip(
                    label = Strings.ROLE_EMITTER_TITLE,
                    tone = AppStatusTone.PRIMARY,
                    icon = AppIcon.CAST,
                )
            }
            Text(
                text = Strings.APP_TAGLINE,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AppStatusChip(
            label = if (hasPassword) {
                Strings.SECURITY_STATUS_PROTECTED
            } else {
                Strings.SECURITY_STATUS_NONE
            },
            tone = if (hasPassword) AppStatusTone.SUCCESS else AppStatusTone.NEUTRAL,
            icon = if (hasPassword) AppIcon.LOCK else AppIcon.LOCK_OPEN,
        )
        Spacer(Modifier.width(Space.xs))

        Box {
            AppIconButton(
                icon = AppIcon.MORE_VERT,
                contentDescription = Strings.MORE_OPTIONS,
                onClick = { menuExpanded = true },
                tooltip = Strings.MORE_OPTIONS,
            )
            AppDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                items = listOf(
                    AppMenuItem(
                        label = Strings.KIOSK_SECURITY_BUTTON,
                        icon = AppIcon.LOCK,
                        onClick = onOpenSecurity,
                    ),
                    AppMenuItem(
                        label = Strings.SETTINGS_TITLE,
                        icon = AppIcon.SETTINGS,
                        onClick = onOpenSettings,
                    ),
                    AppMenuItem(
                        label = Strings.CHANGE_ROLE,
                        icon = AppIcon.SWAP_HORIZ,
                        onClick = onChangeRole,
                    ),
                    AppMenuItem(
                        label = Strings.EXIT_APP,
                        icon = AppIcon.LOGOUT,
                        onClick = onExit,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val container = if (isError) colors.errorContainer else extra.successContainer
    val content = if (isError) colors.onErrorContainer else extra.onSuccessContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KioskShape.small,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = if (isError) AppIcon.ERROR else AppIcon.CHECK_CIRCLE,
                contentDescription = null,
                size = Sizes.iconCompact,
                tint = content,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f),
            )
            AppIconButton(
                icon = AppIcon.CLOSE,
                contentDescription = Strings.CLOSE_BUTTON,
                onClick = onDismiss,
                size = AppIconButtonSize.SMALL,
                tint = content,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Screens panel
// ---------------------------------------------------------------------------

@Composable
private fun ScreensPanel(
    stateHolder: EmitterStateHolder,
    state: EmitterUiState,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = KioskColors.scheme

    Column(modifier = modifier) {
        PanelHeader(
            title = Strings.EMITTER_SCREENS_TITLE,
            subtitle = Strings.EMITTER_SCREENS_SUBTITLE,
            actions = {
                AppIconButton(
                    icon = AppIcon.REFRESH,
                    contentDescription = Strings.DISCOVERY_REFRESH,
                    onClick = { stateHolder.refreshDevices() },
                    enabled = !state.isRefreshing,
                    tooltip = if (state.isRefreshing) {
                        Strings.DISCOVERY_REFRESHING
                    } else {
                        Strings.DISCOVERY_REFRESH
                    },
                )
                AppButton(
                    text = Strings.ADD_BY_IP,
                    onClick = { stateHolder.openManualIpDialog() },
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.ADD_LINK,
                )
            },
        )

        Spacer(Modifier.height(Space.sm))

        if (state.devices.isEmpty()) {
            val listModifier = if (fillHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().heightIn(min = Sizes.emptyStateMinHeight)
            }
            Box(
                modifier = listModifier,
                contentAlignment = Alignment.Center,
            ) {
                if (state.isSearching) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.md),
                    ) {
                        AppLoadingIndicator(color = colors.primary)
                        Text(
                            text = Strings.DISCOVERY_SEARCHING,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                } else {
                    AppEmptyState(
                        icon = AppIcon.TV,
                        title = Strings.EMPTY_SCREENS_TITLE,
                        description = Strings.DISCOVERY_EMPTY,
                        action = {
                            AppButton(
                                text = Strings.ADD_BY_IP,
                                onClick = { stateHolder.openManualIpDialog() },
                                variant = AppButtonVariant.TONAL,
                                size = AppButtonSize.SMALL,
                                icon = AppIcon.ADD_LINK,
                            )
                        },
                    )
                }
            }
        } else {
            val listModifier = if (fillHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().heightIn(max = Sizes.stackedListMaxHeight)
            }
            LazyColumn(
                modifier = listModifier,
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                itemsIndexed(
                    items = state.devices,
                    key = { _, device -> device.deviceId },
                ) { _, device ->
                    DeviceCard(
                        device = device,
                        sync = state.syncByDevice[device.deviceId],
                        onToggleSelection = { stateHolder.toggleSelection(device.deviceId) },
                        onPair = { stateHolder.openPairDialog(device) },
                        onUnpair = { stateHolder.requestUnpair(device) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit,
) {
    val colors = KioskColors.scheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Space.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xxs),
        ) {
            actions()
        }
    }
}

@Composable
private fun DeviceCard(
    device: EmitterDeviceUi,
    sync: DeviceSyncUi?,
    onToggleSelection: () -> Unit,
    onPair: () -> Unit,
    onUnpair: () -> Unit,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val selected = device.isSelected && device.isPairedToThisEmitter

    val statusLabel: String
    val statusTone: AppStatusTone
    val statusIcon: AppIcon
    when {
        device.isPairedToThisEmitter && device.isOnline -> {
            statusLabel = Strings.STATUS_ONLINE
            statusTone = AppStatusTone.SUCCESS
            statusIcon = AppIcon.WIFI
        }

        device.isPairedToThisEmitter -> {
            statusLabel = Strings.STATUS_OFFLINE
            statusTone = AppStatusTone.WARNING
            statusIcon = AppIcon.WIFI_OFF
        }

        device.isPairedToAnotherEmitter -> {
            statusLabel = Strings.PAIRED_OTHER_CONTROLLER
            statusTone = AppStatusTone.WARNING
            statusIcon = AppIcon.LINK
        }

        else -> {
            statusLabel = Strings.STATUS_UNPAIRED
            statusTone = AppStatusTone.NEUTRAL
            statusIcon = AppIcon.LINK
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KioskShape.card,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) Sizes.selectionBorder else Sizes.hairline,
            color = if (selected) colors.primary else colors.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Sizes.iconBubbleSmall)
                        .clip(CircleShape)
                        .background(
                            if (selected) colors.primary else colors.surfaceContainerHigh,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        icon = AppIcon.TV,
                        contentDescription = null,
                        size = Sizes.iconCompact,
                        tint = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Space.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${device.host}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    )
                }
                if (device.isPairedToThisEmitter) {
                    Checkbox(
                        checked = device.isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.semantics {
                            contentDescription = Strings.selectScreen(device.deviceName)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.primary,
                            uncheckedColor = colors.outline,
                            checkmarkColor = colors.onPrimary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(Space.xs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppStatusChip(
                    label = statusLabel,
                    tone = statusTone,
                    icon = statusIcon,
                )
                Spacer(Modifier.weight(1f))
                when {
                    device.isPairedToThisEmitter -> {
                        AppButton(
                            text = Strings.UNPAIR_BUTTON,
                            onClick = onUnpair,
                            variant = AppButtonVariant.TEXT,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.LINK,
                        )
                    }

                    !device.isPairedToAnotherEmitter -> {
                        AppButton(
                            text = Strings.PAIR_BUTTON,
                            onClick = onPair,
                            variant = AppButtonVariant.TONAL,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.ADD_LINK,
                        )
                    }
                }
            }

            sync?.let {
                Spacer(Modifier.height(Space.xs))
                SyncStatusRow(it)
            }
        }
    }
}

@Composable
private fun SyncStatusRow(sync: DeviceSyncUi) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra

    when (sync.status) {
        SyncUiStatus.PENDING -> Text(
            text = Strings.SEND_DEVICE_PENDING,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        SyncUiStatus.UPLOADING -> {
            AppLinearProgress(
                progress = if (sync.total > 0) {
                    sync.uploaded.toFloat() / sync.total.toFloat()
                } else {
                    0f
                },
                label = if (sync.total > 0) {
                    Strings.syncUploading(sync.uploaded, sync.total)
                } else {
                    Strings.SEND_DEVICE_UPLOADING
                },
            )
        }

        SyncUiStatus.APPLYING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppLoadingIndicator(
                color = colors.primary,
                dotSize = Sizes.loadingDotSmall,
            )
            Text(
                text = Strings.SEND_DEVICE_APPLYING,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        SyncUiStatus.SUCCESS -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.CHECK_CIRCLE,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = extra.success,
            )
            Text(
                text = "${sync.message ?: Strings.SEND_DEVICE_SUCCESS} (v${sync.version})",
                style = MaterialTheme.typography.bodySmall,
                color = extra.success,
            )
        }

        SyncUiStatus.FAILED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.ERROR,
                contentDescription = null,
                size = Sizes.iconSmall,
                tint = colors.error,
            )
            Text(
                text = sync.message ?: Strings.SEND_DEVICE_FAILED,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Playlist panel
// ---------------------------------------------------------------------------

@Composable
private fun PlaylistPanel(
    stateHolder: PlaylistEditorStateHolder,
    state: PlaylistEditorUiState,
    storage: LocalStorage,
    fillHeight: Boolean,
    onRequestRemove: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra

    Column(modifier = modifier) {
        PanelHeader(
            title = Strings.PLAYLIST_TITLE,
            subtitle = Strings.PLAYLIST_SUBTITLE,
            actions = {
                AppButton(
                    text = if (state.isImporting) Strings.ADDING_IMAGES else Strings.ADD_IMAGES,
                    onClick = { stateHolder.addImages() },
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.ADD_PHOTO_ALTERNATE,
                    loading = state.isImporting,
                    enabled = !state.isImporting,
                )
            },
        )

        Spacer(Modifier.height(Space.sm))

        PlaylistSelectorRow(state = state, stateHolder = stateHolder)

        Spacer(Modifier.height(Space.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.DEFAULT_DURATION,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = Strings.DURATION_HINT,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Space.sm))
            AppStepper(
                value = state.defaultDurationDraft.toIntOrNull()
                    ?: state.playlist.defaultDurationSec,
                onValueChange = { stateHolder.updateDefaultDuration(it.toString()) },
                range = MIN_DURATION_SEC..MAX_DURATION_SEC,
                unit = Strings.SECONDS_SUFFIX,
                modifier = Modifier.width(Sizes.stepperWidth),
            )
        }

        state.message?.let { message ->
            Spacer(Modifier.height(Space.xs))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = KioskShape.small,
                color = if (state.messageIsError) colors.errorContainer else extra.successContainer,
                contentColor = if (state.messageIsError) {
                    colors.onErrorContainer
                } else {
                    extra.onSuccessContainer
                },
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Space.sm,
                        vertical = Space.xs,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    AppIcon(
                        icon = if (state.messageIsError) AppIcon.ERROR else AppIcon.CHECK_CIRCLE,
                        contentDescription = null,
                        size = Sizes.iconSmall,
                        tint = if (state.messageIsError) {
                            colors.onErrorContainer
                        } else {
                            extra.onSuccessContainer
                        },
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    AppIconButton(
                        icon = AppIcon.CLOSE,
                        contentDescription = Strings.CLOSE_BUTTON,
                        onClick = { stateHolder.dismissMessage() },
                        size = AppIconButtonSize.SMALL,
                        tint = if (state.messageIsError) {
                            colors.onErrorContainer
                        } else {
                            extra.onSuccessContainer
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.xs))

        if (state.playlist.items.isEmpty()) {
            val listModifier = if (fillHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().heightIn(min = Sizes.emptyStateMinHeight)
            }
            Box(
                modifier = listModifier,
                contentAlignment = Alignment.Center,
            ) {
                AppEmptyState(
                    icon = AppIcon.PHOTO_LIBRARY,
                    title = Strings.EMPTY_PLAYLIST_TITLE,
                    description = Strings.PLAYLIST_EMPTY,
                    action = {
                        AppButton(
                            text = Strings.ADD_IMAGES,
                            onClick = { stateHolder.addImages() },
                            variant = AppButtonVariant.TONAL,
                            size = AppButtonSize.SMALL,
                            icon = AppIcon.ADD_PHOTO_ALTERNATE,
                            enabled = !state.isImporting,
                        )
                    },
                )
            }
        } else {
            val listModifier = if (fillHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().heightIn(max = Sizes.stackedListMaxHeight)
            }
            val lastIndex = state.playlist.items.lastIndex
            LazyColumn(
                modifier = listModifier,
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                itemsIndexed(
                    items = state.playlist.items,
                    key = { _, item -> item.mediaId },
                ) { index, item ->
                    PlaylistItemRow(
                        item = item,
                        index = index,
                        lastIndex = lastIndex,
                        defaultDurationSec = state.playlist.defaultDurationSec,
                        durationDraft = state.durationDrafts[item.mediaId],
                        storage = storage,
                        onDurationChange = {
                            stateHolder.updateItemDuration(item.mediaId, it)
                        },
                        onMoveUp = { stateHolder.moveUp(index) },
                        onMoveDown = { stateHolder.moveDown(index) },
                        onRemove = { onRequestRemove(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistSelectorRow(
    state: PlaylistEditorUiState,
    stateHolder: PlaylistEditorStateHolder,
) {
    val colors = KioskColors.scheme
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
                            onClick = { stateHolder.selectPlaylist(playlist.id) },
                        )
                    },
                )
            }
        }
        Spacer(Modifier.width(Space.xs))
        AppIconButton(
            icon = AppIcon.ADD,
            contentDescription = Strings.PLAYLIST_NEW,
            onClick = { stateHolder.openCreatePlaylistDialog() },
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.PLAYLIST_NEW,
        )
        AppIconButton(
            icon = AppIcon.EDIT,
            contentDescription = Strings.PLAYLIST_RENAME,
            onClick = { stateHolder.openRenamePlaylistDialog() },
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.PLAYLIST_RENAME,
        )
        AppIconButton(
            icon = AppIcon.DELETE,
            contentDescription = Strings.PLAYLIST_DELETE,
            onClick = { stateHolder.requestDeletePlaylist() },
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.PLAYLIST_DELETE,
            tint = colors.error,
        )
    }
}

@Composable
private fun PlaylistItemRow(
    item: PlaylistItem,
    index: Int,
    lastIndex: Int,
    defaultDurationSec: Int,
    durationDraft: String?,
    storage: LocalStorage,
    onDurationChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = KioskColors.scheme
    val bitmap = remember(item.mediaId) {
        storage.loadMediaBitmap(item.mediaId, item.fileName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.playlistRowMinHeight)
            .clip(KioskShape.small)
            .background(colors.surfaceContainerLow)
            .padding(Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = Sizes.listThumbnailWidth,
                    height = Sizes.listThumbnailHeight,
                )
                .clip(KioskShape.thumbnail)
                .background(KioskScreenColors.background),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                AppIcon(
                    icon = AppIcon.IMAGE,
                    contentDescription = null,
                    size = Sizes.iconCompact,
                    tint = colors.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.durationSec == null) {
                    "${Strings.ITEM_DEFAULT_DURATION} · $defaultDurationSec${Strings.SECONDS_SUFFIX}"
                } else {
                    "${item.durationSec}${Strings.SECONDS_SUFFIX}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        AppTextField(
            value = durationDraft ?: item.durationSec?.toString().orEmpty(),
            onValueChange = onDurationChange,
            modifier = Modifier.width(Sizes.durationFieldWidth),
            placeholder = Strings.DURATION_FIELD_HINT,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        AppIconButton(
            icon = AppIcon.ARROW_UPWARD,
            contentDescription = Strings.MOVE_UP,
            onClick = onMoveUp,
            enabled = index > 0,
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.MOVE_UP,
        )
        AppIconButton(
            icon = AppIcon.ARROW_DOWNWARD,
            contentDescription = Strings.MOVE_DOWN,
            onClick = onMoveDown,
            enabled = index < lastIndex,
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.MOVE_DOWN,
        )
        AppIconButton(
            icon = AppIcon.DELETE,
            contentDescription = Strings.REMOVE_ITEM,
            onClick = onRemove,
            size = AppIconButtonSize.SMALL,
            tooltip = Strings.REMOVE_ITEM,
            tint = colors.error,
        )
    }
}

// ---------------------------------------------------------------------------
// Send bar
// ---------------------------------------------------------------------------

@Composable
private fun SendPanel(
    hint: String,
    isSending: Boolean,
    canSend: Boolean,
    canProject: Boolean,
    sendDisabledReason: String?,
    onSend: () -> Unit,
    onProject: () -> Unit,
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

// ---------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------

@Composable
private fun SettingsDialog(
    theme: AppTheme,
    reduceMotion: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KioskColors.scheme

    AppDialog(
        title = Strings.SETTINGS_TITLE,
        icon = AppIcon.SETTINGS,
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = Strings.SETTINGS_THEME,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            AppButtonGroup(
                options = listOf(Strings.THEME_DARK, Strings.THEME_LIGHT),
                selectedIndex = if (theme == AppTheme.DARK) 0 else 1,
                onSelect = { index ->
                    onThemeChange(if (index == 0) AppTheme.DARK else AppTheme.LIGHT)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Strings.SETTINGS_THEME_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(Space.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Strings.SETTINGS_REDUCE_MOTION,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                    Text(
                        text = Strings.SETTINGS_REDUCE_MOTION_HINT,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Space.md))
                Switch(
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.outline,
                        uncheckedTrackColor = colors.surfaceContainerHighest,
                    ),
                )
            }
        },
        actions = {
            AppButton(
                text = Strings.CLOSE_BUTTON,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
        },
    )
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

@Composable
private fun PairScreenDialog(
    device: EmitterDeviceUi,
    code: String,
    error: String?,
    isPairing: Boolean,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KioskColors.scheme

    AppDialog(
        title = "${Strings.PAIR_DIALOG_TITLE}: ${device.deviceName}",
        icon = AppIcon.ADD_LINK,
        onDismissRequest = onDismiss,
        dismissible = !isPairing,
        content = {
            Text(
                text = Strings.PAIR_DIALOG_INSTRUCTION,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            AppTextField(
                value = code,
                onValueChange = onCodeChange,
                label = Strings.PAIR_CODE_FIELD,
                placeholder = Strings.PAIR_CODE_PLACEHOLDER,
                error = error,
                helper = if (error == null) Strings.PAIR_CODE_REQUIRED else null,
                enabled = !isPairing,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        actions = {
            AppButton(
                text = Strings.PAIR_DIALOG_CANCEL,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isPairing,
            )
            AppButton(
                text = if (isPairing) Strings.PAIRING_IN_PROGRESS else Strings.PAIR_DIALOG_CONFIRM,
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.LINK,
                enabled = code.length == 6,
                loading = isPairing,
                disabledReason = Strings.PAIR_CODE_REQUIRED,
            )
        },
    )
}

@Composable
private fun ManualIpDialog(
    value: String,
    error: String?,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KioskColors.scheme

    AppDialog(
        title = Strings.MANUAL_IP_DIALOG_TITLE,
        icon = AppIcon.WIFI,
        onDismissRequest = onDismiss,
        dismissible = !isLoading,
        content = {
            Text(
                text = Strings.MANUAL_IP_DIALOG_INSTRUCTION,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.md))
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                label = Strings.MANUAL_IP_FIELD,
                placeholder = Strings.MANUAL_IP_HINT,
                error = error,
                enabled = !isLoading,
            )
        },
        actions = {
            AppButton(
                text = Strings.CANCEL_BUTTON,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
                enabled = !isLoading,
            )
            AppButton(
                text = if (isLoading) Strings.PAIRING_IN_PROGRESS else Strings.ADD_BUTTON,
                onClick = onConfirm,
                size = AppButtonSize.SMALL,
                icon = AppIcon.ADD,
                enabled = !isLoading,
                loading = isLoading,
            )
        },
    )
}

@Composable
private fun KioskPasswordDialog(
    dialog: KioskPasswordDialogUi,
    onDraftChange: (String) -> Unit,
    onConfirmDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val title = when (dialog.action) {
        EmitterPendingAction.CONFIGURE -> Strings.KIOSK_PASSWORD_CHANGE_TITLE
        EmitterPendingAction.PAIR_LOCK -> Strings.KIOSK_PASSWORD_PAIR_TITLE
        else -> Strings.KIOSK_PASSWORD_TITLE
    }

    AppDialog(
        title = title,
        icon = AppIcon.LOCK,
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = Sizes.dialogBodyMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = Strings.KIOSK_PASSWORD_INSTRUCTION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = Strings.KIOSK_PASSWORD_RULE,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = Strings.KIOSK_DEFAULT_SEQUENCE_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )

                Spacer(Modifier.height(Space.md))
                AppTextField(
                    value = dialog.draft,
                    onValueChange = onDraftChange,
                    label = Strings.KIOSK_PASSWORD_FIELD,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(Space.sm))
                AppTextField(
                    value = dialog.confirmDraft,
                    onValueChange = onConfirmDraftChange,
                    label = Strings.KIOSK_PASSWORD_CONFIRM_FIELD,
                    visualTransformation = PasswordVisualTransformation(),
                )
                dialog.error?.let {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                    )
                }

                Spacer(Modifier.height(Space.md))
                WarningNote(
                    title = Strings.KIOSK_LOCK_ACTIVE_HINT,
                    body = buildString {
                        if (dialog.action == EmitterPendingAction.CONFIGURE) {
                            append(Strings.KIOSK_PASSWORD_APPLIES_NOW)
                        }
                        if (dialog.action == EmitterPendingAction.PAIR_LOCK) {
                            if (isNotEmpty()) append('\n')
                            append(Strings.KIOSK_PASSWORD_PAIR_HINT)
                        }
                        if (isNotEmpty()) append('\n')
                        append(Strings.KIOSK_UNIVERSAL_HINT)
                    },
                )
            }
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
                icon = AppIcon.LOCK,
            )
        },
    )
}

@Composable
private fun SecurityDialog(
    dialog: SecurityDialogUi,
    hasPassword: Boolean,
    onAddKey: (String) -> Unit,
    onRemoveLast: () -> Unit,
    onResetSequence: () -> Unit,
    onSaveSequence: () -> Unit,
    onToggleRecovery: () -> Unit,
    onAcceptedChange: (Boolean) -> Unit,
    onRecoveryChange: (String) -> Unit,
    onRecoveryConfirmChange: (String) -> Unit,
    onSaveRecovery: () -> Unit,
    onResetRecovery: () -> Unit,
    onChangePassword: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    var tab by remember { mutableStateOf(0) }

    AppDialog(
        title = Strings.SECURITY_TITLE,
        icon = AppIcon.LOCK,
        onDismissRequest = onDismiss,
        maxWidth = Sizes.dialogWideMaxWidth,
        content = {
            AppButtonGroup(
                options = listOf(
                    Strings.SECURITY_TAB_PASSWORD,
                    Strings.SECURITY_TAB_SEQUENCE,
                    Strings.SECURITY_TAB_RECOVERY,
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.md))

            when (tab) {
                0 -> {
                    Text(
                        text = if (hasPassword) {
                            Strings.SECURITY_PASSWORD_SECTION_HINT
                        } else {
                            "${Strings.SECURITY_NO_PASSWORD} ${Strings.SECURITY_PASSWORD_SECTION_HINT}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.md))
                    AppButton(
                        text = Strings.SECURITY_CHANGE_PASSWORD,
                        onClick = onChangePassword,
                        variant = AppButtonVariant.TONAL,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.LOCK,
                    )
                }

                1 -> {
                    Text(
                        text = Strings.SECURITY_SEQUENCE_HINT,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = Strings.SECURITY_SEQUENCE_CURRENT,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = if (dialog.sequence.isEmpty()) {
                            Strings.SECURITY_SEQUENCE_EMPTY
                        } else {
                            formatUnlockSequence(dialog.sequence)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = Strings.SECURITY_SEQUENCE_ADD,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        listOf("SHIFT", "CTRL", "ALT").forEach { key ->
                            AppButton(
                                text = formatUnlockSequence(listOf(key)),
                                onClick = { onAddKey(key) },
                                variant = AppButtonVariant.TONAL,
                                size = AppButtonSize.SMALL,
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        AppButton(
                            text = Strings.SECURITY_SEQUENCE_REMOVE,
                            onClick = onRemoveLast,
                            variant = AppButtonVariant.OUTLINED,
                            size = AppButtonSize.SMALL,
                            enabled = dialog.sequence.isNotEmpty(),
                        )
                        AppButton(
                            text = Strings.SECURITY_SEQUENCE_RESET,
                            onClick = onResetSequence,
                            variant = AppButtonVariant.TEXT,
                            size = AppButtonSize.SMALL,
                        )
                    }
                    Text(
                        text = Strings.SECURITY_SEQUENCE_MIN_HINT,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    dialog.sequenceError?.let {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                        )
                    }
                    Spacer(Modifier.height(Space.sm))
                    AppButton(
                        text = Strings.SECURITY_SEQUENCE_SAVE,
                        onClick = onSaveSequence,
                        size = AppButtonSize.SMALL,
                        icon = AppIcon.CHECK,
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .heightIn(max = Sizes.dialogBodyMaxHeight)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = if (dialog.hasCustomRecovery) {
                                Strings.SECURITY_RECOVERY_CUSTOM
                            } else {
                                Strings.SECURITY_RECOVERY_DEFAULT
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dialog.hasCustomRecovery) {
                                extra.warning
                            } else {
                                colors.onSurfaceVariant
                            },
                        )
                        Spacer(Modifier.height(Space.sm))

                        if (!dialog.showRecoveryEditor) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                AppButton(
                                    text = Strings.SECURITY_RECOVERY_CHANGE,
                                    onClick = onToggleRecovery,
                                    variant = AppButtonVariant.TONAL,
                                    size = AppButtonSize.SMALL,
                                    icon = AppIcon.EDIT,
                                )
                                if (dialog.hasCustomRecovery) {
                                    AppButton(
                                        text = Strings.SECURITY_RECOVERY_RESET,
                                        onClick = onResetRecovery,
                                        variant = AppButtonVariant.TEXT,
                                        size = AppButtonSize.SMALL,
                                        icon = AppIcon.UNDO,
                                    )
                                }
                            }
                        } else {
                            WarningNote(
                                title = Strings.SECURITY_RECOVERY_WARNING_TITLE,
                                body = Strings.SECURITY_RECOVERY_WARNING,
                            )
                            Spacer(Modifier.height(Space.sm))
                            Row(
                                modifier = Modifier.clickable {
                                    onAcceptedChange(!dialog.recoveryAccepted)
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = dialog.recoveryAccepted,
                                    onCheckedChange = { onAcceptedChange(it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = extra.warning,
                                        uncheckedColor = colors.outline,
                                        checkmarkColor = extra.onWarning,
                                    ),
                                )
                                Text(
                                    text = Strings.SECURITY_RECOVERY_ACCEPT,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(Space.sm))
                            AppTextField(
                                value = dialog.recoveryDraft,
                                onValueChange = onRecoveryChange,
                                label = Strings.SECURITY_RECOVERY_FIELD,
                                enabled = dialog.recoveryAccepted,
                                visualTransformation = PasswordVisualTransformation(),
                            )
                            Spacer(Modifier.height(Space.sm))
                            AppTextField(
                                value = dialog.recoveryConfirmDraft,
                                onValueChange = onRecoveryConfirmChange,
                                label = Strings.SECURITY_RECOVERY_CONFIRM_FIELD,
                                enabled = dialog.recoveryAccepted,
                                visualTransformation = PasswordVisualTransformation(),
                                error = dialog.recoveryError,
                            )
                            Text(
                                text = Strings.SECURITY_RECOVERY_RULE,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Space.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                AppButton(
                                    text = Strings.SECURITY_RECOVERY_SAVE,
                                    onClick = onSaveRecovery,
                                    size = AppButtonSize.SMALL,
                                    icon = AppIcon.LOCK,
                                    enabled = dialog.recoveryAccepted,
                                    disabledReason = Strings.SECURITY_RECOVERY_MUST_ACCEPT,
                                )
                                AppButton(
                                    text = Strings.CANCEL_BUTTON,
                                    onClick = onToggleRecovery,
                                    variant = AppButtonVariant.TEXT,
                                    size = AppButtonSize.SMALL,
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            AppButton(
                text = Strings.SECURITY_CLOSE,
                onClick = onDismiss,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
        },
    )
}

@Composable
private fun WarningNote(title: String, body: String) {
    val extra = KioskColors.extra

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KioskShape.small,
        color = extra.warningContainer,
        contentColor = extra.onWarningContainer,
    ) {
        Row(
            modifier = Modifier.padding(Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            AppIcon(
                icon = AppIcon.WARNING,
                contentDescription = null,
                size = Sizes.iconCompact,
                tint = extra.onWarningContainer,
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = extra.onWarningContainer,
                )
                Spacer(Modifier.height(Space.xxs))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.onWarningContainer,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = KioskColors.scheme.onSurface,
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        thickness = Sizes.securityDivider,
        color = KioskColors.scheme.outlineVariant,
    )
}
