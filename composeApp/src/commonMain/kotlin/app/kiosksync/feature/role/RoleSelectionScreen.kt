package app.kiosksync.feature.role

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
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
import app.kiosksync.core.AppRole
import app.kiosksync.core.Strings

@Composable
fun RoleSelectionScreen(
    stateHolder: RoleStateHolder,
    onRoleConfirmed: (AppConfig) -> Unit,
    deviceId: String,
    deviceName: String,
    port: Int,
    onExitRequested: () -> Unit = {}
) {
    val uiState by stateHolder.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = Strings.ROLE_SELECTION_TITLE,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = Strings.ROLE_SELECTION_SUBTITLE,
            fontSize = 15.sp,
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
        ) {
            RoleCard(
                title = Strings.ROLE_EMITTER_TITLE,
                description = Strings.ROLE_EMITTER_DESC,
                role = AppRole.EMITTER,
                isSelected = uiState.selectedRole == AppRole.EMITTER,
                onSelect = { stateHolder.selectRole(AppRole.EMITTER) },
                modifier = Modifier.weight(1f).height(180.dp)
            )

            RoleCard(
                title = Strings.ROLE_RECEIVER_TITLE,
                description = Strings.ROLE_RECEIVER_DESC,
                role = AppRole.RECEIVER,
                isSelected = uiState.selectedRole == AppRole.RECEIVER,
                onSelect = { stateHolder.selectRole(AppRole.RECEIVER) },
                modifier = Modifier.weight(1f).height(180.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                val config = stateHolder.confirmRole(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    port = port
                )
                if (config != null) {
                    onRoleConfirmed(config)
                }
            },
            enabled = uiState.selectedRole != null,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF3D7AF5),
                disabledBackgroundColor = Color(0xFF2A2A2A),
                contentColor = Color.White,
                disabledContentColor = Color(0xFF666666)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(48.dp).width(260.dp)
        ) {
            Text(
                text = Strings.ROLE_CONFIRM_BUTTON,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onExitRequested,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFAAAAAA)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(40.dp).width(260.dp)
        ) {
            Text(
                text = Strings.EXIT_APP,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    role: AppRole,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFF3D7AF5) else Color(0xFF333333)
    val cardBg = if (isSelected) Color(0xFF232B3B) else Color(0xFF1E1E1E)

    Card(
        backgroundColor = cardBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = if (isSelected) 6.dp else 2.dp,
        modifier = modifier.clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF75A6FF) else Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFFBBBBBB),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
