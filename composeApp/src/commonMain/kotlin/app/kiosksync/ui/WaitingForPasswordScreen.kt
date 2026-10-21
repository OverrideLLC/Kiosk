package app.kiosksync.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.Strings

/**
 * Shown after pairing while the screen still has no blocking password. The
 * controller sends it as soon as the person configures (or already has) a
 * password, and then the carousel takes over.
 */
@Composable
fun WaitingForPasswordScreen() {
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF3D7AF5).copy(alpha = pulse),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = Strings.RECEIVER_WAITING_PASSWORD_TITLE,
                color = Color.White.copy(alpha = pulse),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Strings.RECEIVER_WAITING_PASSWORD_MESSAGE,
                color = Color(0xFFAAAAAA),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
