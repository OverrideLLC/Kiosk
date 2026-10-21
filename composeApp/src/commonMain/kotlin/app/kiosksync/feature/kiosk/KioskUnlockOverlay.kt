package app.kiosksync.feature.kiosk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.Strings

/**
 * Small overlay shown while the keyboard is locked. It doubles as on-screen key
 * feedback so the person can see that the chord and the password are being
 * registered. The carousel keeps playing behind it.
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
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Strings.KIOSK_CHORD_LABEL,
                    color = Color(0xFF888888),
                    fontSize = 10.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChordKey(label = "Shift", done = state.chordStage >= 1)
                    ChordKey(label = "Shift", done = state.chordStage >= 2)
                    ChordKey(label = "Ctrl", done = state.chordStage >= 3)
                }

                Text(
                    text = if (state.chordStage >= 3) {
                        if (state.typedLength == 0) {
                            Strings.KIOSK_TYPING_PASSWORD
                        } else {
                            "•".repeat(state.typedLength.coerceAtMost(24))
                        }
                    } else {
                        Strings.KIOSK_KEYS_ACTIVE
                    },
                    color = if (state.chordStage >= 3) Color(0xFF9EC1FF) else Color(0xFF666666),
                    fontSize = if (state.chordStage >= 3) 14.sp else 10.sp
                )

                state.lastKey?.let { key ->
                    Text(
                        text = Strings.kioskLastKey(key),
                        color = Color(0xFF777777),
                        fontSize = 10.sp
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

@Composable
private fun ChordKey(label: String, done: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (done) Color(0xFF2E7D32) else Color(0xFF2A2A2A))
            .border(
                width = 1.dp,
                color = if (done) Color(0xFF66BB6A) else Color(0xFF555555),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (done) Color.White else Color(0xFF999999),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
