package app.kiosksync.feature.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.Strings
import kotlinx.coroutines.delay

@Composable
fun PairingCodeScreen(
    pairingManager: PairingManager,
    serviceError: String?,
    onExitRequested: () -> Unit,
    onChangeRoleRequested: () -> Unit
) {
    val state by pairingManager.uiState.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            pairingManager.tick()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = Strings.RECEIVER_PAIRING_TITLE,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = state.currentCode,
                color = if (state.isLocked) Color(0xFF666666) else Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 10.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = Strings.ENTER_CODE_IN_CONTROLLER,
                color = Color(0xFFBBBBBB),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLocked) {
                Text(
                    text = Strings.RECEIVER_LOCKED_TITLE,
                    color = Color(0xFFE57373),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Strings.RECEIVER_LOCKED_MESSAGE} (${state.lockRemainingSeconds}s)",
                    color = Color(0xFFE57373),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "${Strings.RECEIVER_CODE_RENEWS_IN} ${formatCountdown(state.codeExpiresInSeconds)}",
                    color = Color(0xFF777777),
                    fontSize = 13.sp
                )
            }

            serviceError?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = Color(0xFFE57373),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onChangeRoleRequested,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF555555)),
                modifier = Modifier.height(34.dp)
            ) {
                Text(Strings.CHANGE_ROLE, fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = onExitRequested,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF555555)),
                modifier = Modifier.height(34.dp)
            ) {
                Text(Strings.EXIT_APP, fontSize = 11.sp)
            }
        }
    }
}

private fun formatCountdown(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
