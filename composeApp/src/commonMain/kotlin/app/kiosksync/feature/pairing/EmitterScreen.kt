package app.kiosksync.feature.pairing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.Strings
import app.kiosksync.feature.playlist.PlaylistEditorStateHolder
import app.kiosksync.feature.playlist.PlaylistEditorUiState
import app.kiosksync.feature.playlist.PlaylistItem
import app.kiosksync.feature.playlist.PlaylistNameDialogMode
import app.kiosksync.platform.LocalStorage

@Composable
fun EmitterScreen(
    stateHolder: EmitterStateHolder,
    playlistStateHolder: PlaylistEditorStateHolder,
    storage: LocalStorage,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit
) {
    val state by stateHolder.uiState.collectAsState()
    val editorState by playlistStateHolder.uiState.collectAsState()
    var showChangeRoleConfirm by remember { mutableStateOf(false) }

    val selectedCount = state.devices.count { it.isSelected && it.isPairedToThisEmitter }
    val sendHint = when {
        editorState.playlist.items.isEmpty() -> Strings.SEND_EMPTY_PLAYLIST
        selectedCount == 0 -> Strings.SEND_NO_SCREENS
        else -> Strings.selectedScreens(selectedCount)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = APP_NAME,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = Strings.ROLE_EMITTER_TITLE,
                fontSize = 13.sp,
                color = Color(0xFF75A6FF),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (state.hasKioskPassword) "🔒 ${Strings.KIOSK_SECURITY_BUTTON}" else Strings.KIOSK_SECURITY_BUTTON,
                color = Color(0xFF9EC1FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { stateHolder.openChangePasswordDialog() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9EC1FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(Strings.KIOSK_SECURITY_BUTTON, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = { showChangeRoleConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(Strings.CHANGE_ROLE, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = onExitRequested,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(Strings.EXIT_APP, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        state.statusMessage?.let { message ->
            StatusBanner(
                message = message,
                isError = state.statusIsError,
                onDismiss = { stateHolder.dismissStatusMessage() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ScreensPanel(
                stateHolder = stateHolder,
                state = state,
                modifier = Modifier
                    .weight(0.43f)
                    .fillMaxHeight()
            )
            Spacer(modifier = Modifier.width(18.dp))
            PlaylistPanel(
                stateHolder = playlistStateHolder,
                state = editorState,
                storage = storage,
                modifier = Modifier
                    .weight(0.57f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SendPanel(
            hint = sendHint,
            isSending = state.isSending,
            canSend = !state.isSending &&
                selectedCount > 0 &&
                editorState.playlist.items.isNotEmpty(),
            canProject = !state.isSending && editorState.playlist.items.isNotEmpty(),
            onSend = {
                stateHolder.sendToSelected(playlistStateHolder.currentPlaylist())
            },
            onProject = {
                stateHolder.requestProjection(playlistStateHolder.currentPlaylist())
            }
        )
    }

    state.passwordDialog?.let { dialog ->
        KioskPasswordDialog(
            dialog = dialog,
            onDraftChange = { stateHolder.updatePasswordDraft(it) },
            onConfirmDraftChange = { stateHolder.updatePasswordConfirmDraft(it) },
            onConfirm = { stateHolder.confirmPassword() },
            onDismiss = { stateHolder.dismissPasswordDialog() }
        )
    }

    editorState.nameDialogMode?.let { mode ->
        PlaylistNameDialog(
            mode = mode,
            draft = editorState.nameDraft,
            error = editorState.nameError,
            onDraftChange = { playlistStateHolder.updateNameDraft(it) },
            onConfirm = { playlistStateHolder.confirmNameDialog() },
            onDismiss = { playlistStateHolder.dismissNameDialog() }
        )
    }

    if (editorState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { playlistStateHolder.dismissDeletePlaylist() },
            title = {
                Text(Strings.PLAYLIST_DELETE_TITLE, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "${Strings.PLAYLIST_DELETE_CONFIRM}\n\n${editorState.playlist.name}",
                    color = Color(0xFFCCCCCC)
                )
            },
            confirmButton = {
                Button(
                    onClick = { playlistStateHolder.confirmDeletePlaylist() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(Strings.PLAYLIST_DELETE)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { playlistStateHolder.dismissDeletePlaylist() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.CANCEL_BUTTON)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }

    if (showChangeRoleConfirm) {
        AlertDialog(
            onDismissRequest = { showChangeRoleConfirm = false },
            title = {
                Text(Strings.CHANGE_ROLE, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(Strings.CHANGE_ROLE_CONFIRM, color = Color(0xFFCCCCCC))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChangeRoleConfirm = false
                        onRoleReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(Strings.CHANGE_ROLE)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showChangeRoleConfirm = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.CANCEL_BUTTON)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }

    state.pairingDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { stateHolder.dismissPairDialog() },
            title = {
                Text(
                    text = "${Strings.PAIR_DIALOG_TITLE}: ${device.deviceName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(Strings.PAIR_DIALOG_INSTRUCTION, color = Color(0xFFCCCCCC))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.pairingCode,
                        onValueChange = { stateHolder.updatePairingCode(it) },
                        label = { Text(Strings.PAIR_CODE_FIELD) },
                        singleLine = true,
                        enabled = !state.isPairing
                    )
                    state.pairingError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = Color(0xFFE57373), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { stateHolder.confirmPair() },
                    enabled = state.pairingCode.length == 6 && !state.isPairing,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3D7AF5),
                        disabledBackgroundColor = Color(0xFF2A2A2A),
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF777777)
                    )
                ) {
                    Text(if (state.isPairing) Strings.PAIRING_IN_PROGRESS else Strings.PAIR_DIALOG_CONFIRM)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { stateHolder.dismissPairDialog() },
                    enabled = !state.isPairing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.PAIR_DIALOG_CANCEL)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }

    state.unpairDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { stateHolder.dismissUnpairDialog() },
            title = {
                Text(Strings.UNPAIR_DIALOG_TITLE, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "${Strings.UNPAIR_DIALOG_CONFIRM}\n\n${device.deviceName}",
                    color = Color(0xFFCCCCCC)
                )
            },
            confirmButton = {
                Button(
                    onClick = { stateHolder.confirmUnpair() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(Strings.UNPAIR_BUTTON)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { stateHolder.dismissUnpairDialog() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.CANCEL_BUTTON)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }

    if (state.showManualIpDialog) {
        AlertDialog(
            onDismissRequest = { stateHolder.dismissManualIpDialog() },
            title = {
                Text(Strings.MANUAL_IP_DIALOG_TITLE, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(Strings.MANUAL_IP_DIALOG_INSTRUCTION, color = Color(0xFFCCCCCC))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.manualIpInput,
                        onValueChange = { stateHolder.updateManualIp(it) },
                        label = { Text(Strings.MANUAL_IP_FIELD) },
                        placeholder = { Text(Strings.MANUAL_IP_HINT, color = Color(0xFF777777)) },
                        singleLine = true,
                        enabled = !state.isManualIpLoading
                    )
                    state.manualIpError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = Color(0xFFE57373), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { stateHolder.confirmManualIp() },
                    enabled = !state.isManualIpLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3D7AF5),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (state.isManualIpLoading) Strings.PAIRING_IN_PROGRESS else Strings.ADD_BUTTON)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { stateHolder.dismissManualIpDialog() },
                    enabled = !state.isManualIpLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(Strings.CANCEL_BUTTON)
                }
            },
            backgroundColor = Color(0xFF242424)
        )
    }
}

@Composable
private fun StatusBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = if (isError) Color(0xFFE57373) else Color(0xFF8BC34A),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA))
        ) {
            Text(Strings.CLOSE_BUTTON, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ScreensPanel(
    stateHolder: EmitterStateHolder,
    state: EmitterUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.EMITTER_SCREENS_TITLE,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = Strings.EMITTER_SCREENS_SUBTITLE,
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }
            Button(
                onClick = { stateHolder.openManualIpDialog() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2D3A52),
                    contentColor = Color(0xFF9EC1FF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(Strings.ADD_BY_IP, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (state.devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.isSearching) Strings.DISCOVERY_SEARCHING else Strings.DISCOVERY_EMPTY,
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.devices, key = { it.deviceId }) { device ->
                    DeviceCard(
                        device = device,
                        sync = state.syncByDevice[device.deviceId],
                        onToggleSelection = { stateHolder.toggleSelection(device.deviceId) },
                        onPair = { stateHolder.openPairDialog(device) },
                        onUnpair = { stateHolder.requestUnpair(device) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPanel(
    stateHolder: PlaylistEditorStateHolder,
    state: PlaylistEditorUiState,
    storage: LocalStorage,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.PLAYLIST_TITLE,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = Strings.PLAYLIST_SUBTITLE,
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }
            Button(
                onClick = { stateHolder.addImages() },
                enabled = !state.isImporting,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3D7AF5),
                    disabledBackgroundColor = Color(0xFF2A2A2A),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF777777)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (state.isImporting) Strings.ADDING_IMAGES else Strings.ADD_IMAGES,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlaylistSelectorRow(
            state = state,
            stateHolder = stateHolder
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.DEFAULT_DURATION,
                fontSize = 12.sp,
                color = Color(0xFFBBBBBB)
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = state.defaultDurationDraft,
                onValueChange = { stateHolder.updateDefaultDuration(it) },
                modifier = Modifier.width(62.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = Strings.SECONDS_SUFFIX,
                fontSize = 11.sp,
                color = Color(0xFF999999)
            )
        }

        Text(
            text = Strings.DURATION_HINT,
            fontSize = 10.sp,
            color = Color(0xFF777777)
        )

        state.message?.let { message ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = if (state.messageIsError) Color(0xFFE57373) else Color(0xFF8BC34A),
                    modifier = Modifier.weight(1f)
                )
                SmallButton("×", onClick = { stateHolder.dismissMessage() })
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (state.playlist.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.PLAYLIST_EMPTY,
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
            }
        } else {
            val lastIndex = state.playlist.items.lastIndex
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    items = state.playlist.items,
                    key = { _, item -> item.mediaId }
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
                        onRemove = { stateHolder.removeItem(index) }
                    )
                }
            }
        }
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
    onRemove: () -> Unit
) {
    val bitmap = remember(item.mediaId) {
        storage.loadMediaBitmap(item.mediaId, item.fileName)
    }

    Card(
        backgroundColor = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("IMG", color = Color(0xFF555555), fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.durationSec == null) {
                        "${Strings.ITEM_DEFAULT_DURATION} (${defaultDurationSec}${Strings.SECONDS_SUFFIX})"
                    } else {
                        "${item.durationSec}${Strings.SECONDS_SUFFIX}"
                    },
                    color = Color(0xFF888888),
                    fontSize = 10.sp
                )
            }

            OutlinedTextField(
                value = durationDraft ?: item.durationSec?.toString().orEmpty(),
                onValueChange = onDurationChange,
                modifier = Modifier.width(56.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                placeholder = {
                    Text(
                        text = Strings.DURATION_FIELD_HINT,
                        color = Color(0xFF666666),
                        fontSize = 10.sp
                    )
                }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallButton(
                    label = "↑",
                    enabled = index > 0,
                    onClick = onMoveUp
                )
                SmallButton(
                    label = "↓",
                    enabled = index < lastIndex,
                    onClick = onMoveDown
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            SmallButton(
                label = "×",
                color = Color(0xFFE57373),
                onClick = onRemove
            )
        }
    }
}

@Composable
private fun SmallButton(
    label: String,
    enabled: Boolean = true,
    color: Color = Color(0xFF9EC1FF),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) Color(0xFF2D3A52) else Color(0xFF242424))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) color else Color(0xFF555555),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlaylistSelectorRow(
    state: PlaylistEditorUiState,
    stateHolder: PlaylistEditorStateHolder
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Strings.PLAYLIST_SELECTOR_LABEL,
            fontSize = 11.sp,
            color = Color(0xFF999999)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9EC1FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${state.playlist.name}  ▾",
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                state.playlists.forEach { playlist ->
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            stateHolder.selectPlaylist(playlist.id)
                        }
                    ) {
                        Text(
                            text = if (playlist.id == state.activePlaylistId) {
                                "● ${playlist.name}"
                            } else {
                                playlist.name
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        SmallButton(label = "+", onClick = { stateHolder.openCreatePlaylistDialog() })
        Spacer(modifier = Modifier.width(4.dp))
        SmallButton(label = "✎", onClick = { stateHolder.openRenamePlaylistDialog() })
        Spacer(modifier = Modifier.width(4.dp))
        SmallButton(
            label = "×",
            color = Color(0xFFE57373),
            onClick = { stateHolder.requestDeletePlaylist() }
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
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (mode == PlaylistNameDialogMode.CREATE) {
                    Strings.PLAYLIST_NEW_TITLE
                } else {
                    Strings.PLAYLIST_RENAME_TITLE
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    label = { Text(Strings.PLAYLIST_NAME_FIELD) },
                    placeholder = { Text(Strings.PLAYLIST_NAME_HINT, color = Color(0xFF777777)) },
                    singleLine = true
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color(0xFFE57373), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3D7AF5),
                    contentColor = Color.White
                )
            ) {
                Text(Strings.SAVE_BUTTON)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(Strings.CANCEL_BUTTON)
            }
        },
        backgroundColor = Color(0xFF242424)
    )
}

@Composable
private fun KioskPasswordDialog(
    dialog: KioskPasswordDialogUi,
    onDraftChange: (String) -> Unit,
    onConfirmDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (dialog.action) {
        EmitterPendingAction.CONFIGURE -> Strings.KIOSK_PASSWORD_CHANGE_TITLE
        EmitterPendingAction.PAIR_LOCK -> Strings.KIOSK_PASSWORD_PAIR_TITLE
        else -> Strings.KIOSK_PASSWORD_TITLE
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(Strings.KIOSK_PASSWORD_INSTRUCTION, color = Color(0xFFCCCCCC), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(Strings.KIOSK_PASSWORD_RULE, color = Color(0xFF999999), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = dialog.draft,
                    onValueChange = onDraftChange,
                    label = { Text(Strings.KIOSK_PASSWORD_FIELD) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dialog.confirmDraft,
                    onValueChange = onConfirmDraftChange,
                    label = { Text(Strings.KIOSK_PASSWORD_CONFIRM_FIELD) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                dialog.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color(0xFFE57373), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(Strings.KIOSK_LOCK_ACTIVE_HINT, color = Color(0xFFFFB74D), fontSize = 11.sp)
                if (dialog.action == EmitterPendingAction.CONFIGURE) {
                    Text(
                        Strings.KIOSK_PASSWORD_APPLIES_NOW,
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp
                    )
                }
                if (dialog.action == EmitterPendingAction.PAIR_LOCK) {
                    Text(
                        Strings.KIOSK_PASSWORD_PAIR_HINT,
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp
                    )
                }
                Text(Strings.KIOSK_UNIVERSAL_HINT, color = Color(0xFF777777), fontSize = 10.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3D7AF5),
                    contentColor = Color.White
                )
            ) {
                Text(Strings.SAVE_BUTTON)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(Strings.CANCEL_BUTTON)
            }
        },
        backgroundColor = Color(0xFF242424)
    )
}

@Composable
private fun SendPanel(
    hint: String,
    isSending: Boolean,
    canSend: Boolean,
    canProject: Boolean,
    onSend: () -> Unit,
    onProject: () -> Unit
) {
    Card(
        backgroundColor = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.SEND_TITLE,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = hint,
                    color = Color(0xFF999999),
                    fontSize = 11.sp
                )
                Text(
                    text = Strings.PROJECT_SHORTCUT_HINT,
                    color = Color(0xFF666666),
                    fontSize = 10.sp
                )
            }
            OutlinedButton(
                onClick = onProject,
                enabled = canProject,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF9EC1FF),
                    disabledContentColor = Color(0xFF555555)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(Strings.PROJECT_BUTTON, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSend,
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF3D7AF5),
                    disabledBackgroundColor = Color(0xFF2A2A2A),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF777777)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isSending) Strings.SENDING else Strings.SEND_BUTTON,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: EmitterDeviceUi,
    sync: DeviceSyncUi?,
    onToggleSelection: () -> Unit,
    onPair: () -> Unit,
    onUnpair: () -> Unit
) {
    val statusText: String
    val statusColor: Color
    when {
        device.isPairedToThisEmitter && device.isOnline -> {
            statusText = Strings.STATUS_ONLINE
            statusColor = Color(0xFF66BB6A)
        }

        device.isPairedToThisEmitter -> {
            statusText = Strings.STATUS_OFFLINE
            statusColor = Color(0xFFE57373)
        }

        device.isPairedToAnotherEmitter -> {
            statusText = Strings.PAIRED_OTHER_CONTROLLER
            statusColor = Color(0xFFFFB74D)
        }

        else -> {
            statusText = Strings.STATUS_UNPAIRED
            statusColor = Color(0xFF999999)
        }
    }

    Card(
        backgroundColor = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${device.host}:${device.port}",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = statusText, color = statusColor, fontSize = 11.sp)
                sync?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    SyncStatusRow(it)
                }
            }

            if (device.isPairedToThisEmitter) {
                Checkbox(
                    checked = device.isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF3D7AF5),
                        uncheckedColor = Color(0xFF666666)
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedButton(
                    onClick = onUnpair,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Strings.UNPAIR_BUTTON, fontSize = 11.sp)
                }
            } else if (!device.isPairedToAnotherEmitter) {
                Button(
                    onClick = onPair,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3D7AF5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Strings.PAIR_BUTTON, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SyncStatusRow(sync: DeviceSyncUi) {
    when (sync.status) {
        SyncUiStatus.PENDING -> Text(
            text = Strings.SEND_DEVICE_PENDING,
            color = Color(0xFF999999),
            fontSize = 10.sp
        )

        SyncUiStatus.UPLOADING -> Column {
            Text(
                text = if (sync.total > 0) {
                    Strings.syncUploading(sync.uploaded, sync.total)
                } else {
                    Strings.SEND_DEVICE_UPLOADING
                },
                color = Color(0xFF9EC1FF),
                fontSize = 10.sp
            )
            if (sync.total > 0) {
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = sync.uploaded.toFloat() / sync.total.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF3D7AF5),
                    backgroundColor = Color(0xFF2A2A2A)
                )
            }
        }

        SyncUiStatus.APPLYING -> Text(
            text = Strings.SEND_DEVICE_APPLYING,
            color = Color(0xFF9EC1FF),
            fontSize = 10.sp
        )

        SyncUiStatus.SUCCESS -> Text(
            text = "${sync.message ?: Strings.SEND_DEVICE_SUCCESS} (v${sync.version})",
            color = Color(0xFF66BB6A),
            fontSize = 10.sp
        )

        SyncUiStatus.FAILED -> Text(
            text = sync.message ?: Strings.SEND_DEVICE_FAILED,
            color = Color(0xFFE57373),
            fontSize = 10.sp
        )
    }
}
