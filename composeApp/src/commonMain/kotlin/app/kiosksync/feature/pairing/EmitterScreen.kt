package app.kiosksync.feature.pairing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.Strings

@Composable
fun EmitterScreen(
    stateHolder: EmitterStateHolder,
    onExitRequested: () -> Unit,
    onRoleReset: () -> Unit
) {
    val state by stateHolder.uiState.collectAsState()
    var showChangeRoleConfirm by remember { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(24.dp))

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
                    fontSize = 12.sp,
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
                Text(Strings.ADD_BY_IP, fontSize = 13.sp)
            }
        }

        state.statusMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = Color(0xFF8BC34A),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { stateHolder.dismissStatusMessage() },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAAAAAA))
                ) {
                    Text(Strings.CLOSE_BUTTON, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.isSearching) Strings.DISCOVERY_SEARCHING else Strings.DISCOVERY_EMPTY,
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.devices, key = { it.deviceId }) { device ->
                    DeviceCard(
                        device = device,
                        onToggleSelection = { stateHolder.toggleSelection(device.deviceId) },
                        onPair = { stateHolder.openPairDialog(device) },
                        onUnpair = { stateHolder.requestUnpair(device) }
                    )
                }
            }
        }
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
private fun DeviceCard(
    device: EmitterDeviceUi,
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${device.host}:${device.port}",
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = statusText, color = statusColor, fontSize = 12.sp)
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
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onUnpair,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Strings.UNPAIR_BUTTON, fontSize = 12.sp)
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
                    Text(Strings.PAIR_BUTTON, fontSize = 12.sp)
                }
            }
        }
    }
}
