package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.AppRole
import app.kiosksync.core.Strings
import app.kiosksync.feature.role.AppConfig
import app.kiosksync.feature.role.RoleStateHolder

@Composable
fun RoleHomeScreen(
    config: AppConfig,
    stateHolder: RoleStateHolder,
    onRoleReset: () -> Unit,
    onExitRequested: () -> Unit = {}
) {
    val uiState by stateHolder.uiState.collectAsState()

    val roleName = if (config.role == AppRole.EMITTER) Strings.ROLE_EMITTER_TITLE else Strings.ROLE_RECEIVER_TITLE
    val roleDesc = if (config.role == AppRole.EMITTER) Strings.ROLE_EMITTER_DESC else Strings.ROLE_RECEIVER_DESC
    val badgeColor = if (config.role == AppRole.EMITTER) Color(0xFF3D7AF5) else Color(0xFF00A86B)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = APP_NAME,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .border(1.dp, badgeColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Rol activo: $roleName",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = roleDesc,
                fontSize = 14.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Details box
            Column(
                modifier = Modifier
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                InfoRow(label = "Equipo:", value = config.deviceName)
                InfoRow(label = "ID de dispositivo:", value = config.deviceId)
                if (config.role == AppRole.RECEIVER) {
                    InfoRow(label = "Puerto local:", value = config.port.toString())
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { stateHolder.requestChangeRole() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE57373)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = Strings.CHANGE_ROLE,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onExitRequested,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF333333),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = Strings.EXIT_APP,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Confirmation Dialog
        if (uiState.showChangeRoleDialog) {
            AlertDialog(
                onDismissRequest = { stateHolder.dismissChangeRoleDialog() },
                title = {
                    Text(
                        text = Strings.CHANGE_ROLE,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = Strings.CHANGE_ROLE_CONFIRM,
                        color = Color(0xFFCCCCCC)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            stateHolder.confirmChangeRole()
                            onRoleReset()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sí, cambiar de rol")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { stateHolder.dismissChangeRoleDialog() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancelar")
                    }
                },
                backgroundColor = Color(0xFF242424)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.width(360.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
