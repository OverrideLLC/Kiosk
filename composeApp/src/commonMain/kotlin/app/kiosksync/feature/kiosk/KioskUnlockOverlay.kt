package app.kiosksync.feature.kiosk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.Strings

/**
 * Small, unobtrusive overlay shown while the keyboard is locked and the user
 * starts the unlock chord. The carousel keeps playing behind it.
 */
@Composable
fun KioskUnlockOverlay(state: KioskLockUiState) {
    if (!state.isLocked) return
    if (!state.showUnlockHint && state.message == null && state.lockoutRemainingSec == 0) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .background(Color(0xE61A1A1A), RoundedCornerShape(12.dp))
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = Strings.KIOSK_LOCKED_TITLE,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Strings.KIOSK_UNLOCK_HINT,
                color = Color(0xFFBBBBBB),
                fontSize = 12.sp
            )
            if (state.lockoutRemainingSec > 0) {
                Text(
                    text = Strings.kioskLockoutSeconds(state.lockoutRemainingSec),
                    color = Color(0xFFE57373),
                    fontSize = 12.sp
                )
            } else {
                if (state.typedLength > 0) {
                    Text(
                        text = "•".repeat(state.typedLength.coerceAtMost(32)),
                        color = Color(0xFF9EC1FF),
                        fontSize = 14.sp
                    )
                }
                state.message?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFE57373),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = Strings.KIOSK_RECOVERY_HINT,
                    color = Color(0xFF777777),
                    fontSize = 10.sp
                )
            }
        }
    }
}
