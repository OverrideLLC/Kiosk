package app.kiosksync.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Expressive three-dot wave used while waiting (discovery, pairing, sending).
 * With reduced motion enabled the dots stay still.
 */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 10.dp,
) {
    val reduceMotion = LocalReduceMotion.current
    val phase = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "loadingWave")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "loadingWavePhase",
        )
        value
    }

    Canvas(
        modifier = modifier.size(width = dotSize * 5, height = dotSize * 2),
    ) {
        val radius = dotSize.toPx() / 2f
        val gap = radius * 3.2f
        val centerY = size.height / 2f
        for (index in 0..2) {
            val offsetY = if (reduceMotion) 0f else sin(phase + index * 0.9f) * radius * 0.9f
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(
                    x = size.width / 2f + (index - 1) * gap,
                    y = centerY + offsetY,
                ),
            )
        }
    }
}

/** Determinate progress with an optional label and percentage. */
@Composable
fun AppLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = KioskColors.scheme
    val clamped = progress.coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Space.xs))
                Text(
                    text = "${(clamped * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Space.xs))
        }
        LinearProgressIndicator(
            progress = { clamped },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(KioskShape.chip),
            color = colors.primary,
            trackColor = colors.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
    }
}

/** Circular indeterminate spinner for inline use. */
@Composable
fun AppCircularProgress(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val colors = KioskColors.scheme
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = colors.primary,
        trackColor = colors.surfaceContainerHighest,
        strokeWidth = 4.dp,
    )
}
