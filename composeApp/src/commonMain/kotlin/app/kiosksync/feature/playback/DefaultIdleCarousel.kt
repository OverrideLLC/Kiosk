package app.kiosksync.feature.playback

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.Strings
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Default animation played on a screen that has no playlist yet. It replaces
 * the former plain "waiting" text with a moving aurora of gradients and a
 * scrolling row of gradient cards, so an idle screen still looks alive.
 */
@Composable
fun DefaultIdleCarousel(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulse = transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04050A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = phase.value
            drawAurora(progress)
            drawCarouselStrip(progress)
            drawVignette()
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = Strings.WAITING_PLAYLIST_TITLE,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7FB4FF),
                            Color(0xFFC7A6FF),
                            Color(0xFFFF9AD5)
                        )
                    ),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.WAITING_PLAYLIST_MESSAGE,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.40f + 0.35f * pulse.value
                }
            )
        }
    }
}

private val BLOB_COLORS = listOf(
    Color(0xFF2E5BFF),
    Color(0xFF8A2BE2),
    Color(0xFF00D4FF),
    Color(0xFFFF2E93)
)

private val CARD_COLORS = listOf(
    Color(0xFF3D7AF5),
    Color(0xFF7C4DFF),
    Color(0xFF00B8D4),
    Color(0xFFE040FB),
    Color(0xFF18FFFF),
    Color(0xFF536DFE)
)

private fun DrawScope.drawAurora(phase: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A1024),
                Color(0xFF05060D),
                Color(0xFF0C0716)
            )
        )
    )

    val t = phase * 2f * PI.toFloat()
    val w = size.width
    val h = size.height
    val r = size.minDimension

    // Soft glow behind the title keeps it readable over the moving colors.
    drawBlob(Offset(w * 0.5f, h * 0.48f), r * 0.42f, Color(0xFF16224A), 0.55f)

    drawBlob(
        Offset(w * 0.22f + cos(t) * w * 0.06f, h * 0.30f + sin(t * 1.3f) * h * 0.10f),
        r * 0.58f, BLOB_COLORS[0], 0.75f
    )
    drawBlob(
        Offset(w * 0.80f + cos(t * 0.9f + 2f) * w * 0.07f, h * 0.24f + sin(t * 0.8f + 1f) * h * 0.10f),
        r * 0.52f, BLOB_COLORS[1], 0.70f
    )
    drawBlob(
        Offset(w * 0.55f + sin(t * 1.1f) * w * 0.08f, h * 0.86f + cos(t * 1.2f) * h * 0.08f),
        r * 0.62f, BLOB_COLORS[2], 0.55f
    )
    drawBlob(
        Offset(w * 0.10f + sin(t * 0.7f + 1f) * w * 0.05f, h * 0.78f),
        r * 0.46f, BLOB_COLORS[3], 0.50f
    )
}

private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center,
        blendMode = BlendMode.Screen
    )
}

/**
 * A slowly moving row of gradient cards that evokes the carousel itself. The
 * scroll wraps seamlessly: the strip length is a multiple of the color cycle,
 * so the pattern at the end of the loop matches the start.
 */
private fun DrawScope.drawCarouselStrip(phase: Float) {
    val cycle = CARD_COLORS.size
    val cardW = size.width * 0.16f
    val cardH = size.height * 0.24f
    val gap = size.width * 0.028f
    val step = cardW + gap

    val needed = ceil((size.width + 2f * step) / step).toInt() + 1
    val slots = max(cycle, ((needed + cycle - 1) / cycle) * cycle)
    val total = slots * step

    val startX = -phase * total
    val y = size.height * 0.64f

    var k = 0
    var x = startX
    while (x < size.width + step) {
        if (x + cardW > 0f && x < size.width) {
            val color = CARD_COLORS[k % cycle]
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.05f)),
                    start = Offset(x, y),
                    end = Offset(x + cardW, y + cardH)
                ),
                topLeft = Offset(x, y),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(cardH * 0.16f),
                alpha = 0.72f,
                blendMode = BlendMode.Screen
            )
        }
        x += step
        k++
    }
}

private fun DrawScope.drawVignette() {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xAA02030A)),
            center = center,
            radius = size.maxDimension * 0.78f
        )
    )
}
