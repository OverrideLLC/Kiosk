package app.kiosksync.feature.playback

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.Strings
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Default animation played on a screen that has no playlist yet.
 *
 * It rotates through five full-screen animated slides (aurora, waves, orbs,
 * diagonal stripes and rings), one effect per slide, with a long soft
 * crossfade so the transition between them looks clean. The waiting message
 * stays on top of every slide.
 */
@Composable
fun DefaultIdleCarousel(modifier: Modifier = Modifier) {
    var slide by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(IDLE_SLIDE_DURATION_MS)
            slide = (slide + 1) % IDLE_SLIDE_COUNT
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04050A))
    ) {
        Crossfade(
            targetState = slide,
            animationSpec = tween(
                durationMillis = IDLE_CROSSFADE_MS,
                easing = LinearEasing
            ),
            label = "idle-slide"
        ) { index ->
            when (index) {
                0 -> AuroraSlide()
                1 -> WavesSlide()
                2 -> OrbsSlide()
                3 -> StripesSlide()
                else -> RingsSlide()
            }
        }

        WaitingMessage(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private const val IDLE_SLIDE_COUNT = 5
private const val IDLE_SLIDE_DURATION_MS = 7_500L
private const val IDLE_CROSSFADE_MS = 1_600

// ---------------------------------------------------------------------------
// Message overlay
// ---------------------------------------------------------------------------

@Composable
private fun WaitingMessage(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "idle-message")
    val pulse = transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle-message-pulse"
    )

    Column(
        modifier = modifier.padding(horizontal = 56.dp),
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

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

@Composable
private fun slidePhase(durationMillis: Int): Float {
    val transition = rememberInfiniteTransition(label = "slide-phase")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slide-phase-value"
    )
    return phase.value
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

// ---------------------------------------------------------------------------
// Slide 1: aurora
// ---------------------------------------------------------------------------

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

@Composable
private fun AuroraSlide() {
    val phase = slidePhase(20_000)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawAurora(phase)
        drawCarouselStrip(phase)
        drawVignette()
    }
}

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

// ---------------------------------------------------------------------------
// Slide 2: waves
// ---------------------------------------------------------------------------

private val WAVE_COLORS = listOf(
    Color(0xFF2E5BFF),
    Color(0xFF00D4FF),
    Color(0xFF8A2BE2),
    Color(0xFF00FFC2)
)

@Composable
private fun WavesSlide() {
    val phase = slidePhase(18_000)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawWaves(phase)
        drawVignette()
    }
}

private fun DrawScope.drawWaves(phase: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF06101F), Color(0xFF05070F), Color(0xFF0B0716))
        )
    )

    val t = phase * 2f * PI.toFloat()
    val points = 64

    for (i in 0 until 4) {
        val color = WAVE_COLORS[i % WAVE_COLORS.size]
        val baseY = size.height * (0.30f + i * 0.15f)
        val amplitude = size.height * (0.05f + i * 0.012f)
        val frequency = 1.1f + i * 0.35f
        val path = Path()
        var step = 0
        while (step <= points) {
            val x = size.width * step / points
            val y = baseY + sin(
                (step.toFloat() / points) * frequency * 2f * PI.toFloat() +
                    t * (1f + i * 0.18f)
            ) * amplitude
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            step++
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.42f),
                    color.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                startY = baseY - amplitude,
                endY = size.height
            ),
            blendMode = BlendMode.Screen
        )
    }
}

// ---------------------------------------------------------------------------
// Slide 3: floating orbs
// ---------------------------------------------------------------------------

private val ORB_COLORS = listOf(
    Color(0xFF4FC3F7),
    Color(0xFFB388FF),
    Color(0xFFFF80AB),
    Color(0xFF69F0AE)
)

@Composable
private fun OrbsSlide() {
    val phase = slidePhase(16_000)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF141C42), Color(0xFF05060D)),
                center = center,
                radius = size.maxDimension * 0.75f
            )
        )
        drawOrbs(phase)
        drawVignette()
    }
}

private fun DrawScope.drawOrbs(phase: Float) {
    val t = phase * 2f * PI.toFloat()
    val count = 7

    for (i in 0 until count) {
        val angle = t + i * (2f * PI.toFloat() / count)
        val drift = 0.72f + 0.28f * sin(t * 0.6f + i)
        val cx = size.width * (0.5f + 0.34f * cos(angle) * drift)
        val cy = size.height * (0.5f + 0.30f * sin(angle * 1.25f + i))
        val radius = size.minDimension * (0.055f + 0.03f * (1f + sin(t * 1.4f + i)) / 2f)
        val color = ORB_COLORS[i % ORB_COLORS.size]
        val centerOffset = Offset(cx, cy)

        drawBlob(centerOffset, radius * 3.4f, color, 0.45f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), color.copy(alpha = 0f)),
                center = centerOffset,
                radius = radius
            ),
            radius = radius,
            center = centerOffset,
            blendMode = BlendMode.Screen
        )
    }
}

// ---------------------------------------------------------------------------
// Slide 4: diagonal stripes
// ---------------------------------------------------------------------------

private val STRIPE_COLORS = listOf(
    Color(0xFF3D7AF5),
    Color(0xFF7C4DFF),
    Color(0xFF00D4FF),
    Color(0xFFFF2E93)
)

@Composable
private fun StripesSlide() {
    val phase = slidePhase(22_000)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF070B18), Color(0xFF0E0A20), Color(0xFF06070F))
            )
        )
        drawStripes(phase)
        drawVignette()
    }
}

private fun DrawScope.drawStripes(phase: Float) {
    val stripe = size.minDimension * 0.11f
    val gap = stripe * 1.45f
    val total = stripe + gap
    val offset = phase * total * 4f
    val diagonal = size.maxDimension * 2f

    rotate(degrees = -22f, pivot = center) {
        var x = -diagonal + (offset % total)
        var i = 0
        while (x < diagonal) {
            val color = STRIPE_COLORS[i % STRIPE_COLORS.size]
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0f),
                        color.copy(alpha = 0.40f),
                        color.copy(alpha = 0f)
                    ),
                    startX = x,
                    endX = x + stripe
                ),
                topLeft = Offset(x, -diagonal),
                size = Size(stripe, diagonal * 2f),
                blendMode = BlendMode.Screen
            )
            x += total
            i++
        }
    }
}

// ---------------------------------------------------------------------------
// Slide 5: concentric rings
// ---------------------------------------------------------------------------

private val RING_COLORS = listOf(
    Color(0xFF4FC3F7),
    Color(0xFFB388FF),
    Color(0xFFFF80AB)
)

@Composable
private fun RingsSlide() {
    val phase = slidePhase(14_000)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0C1230), Color(0xFF04050A)),
                center = center,
                radius = size.maxDimension * 0.8f
            )
        )
        drawRings(phase)
        drawVignette()
    }
}

private fun DrawScope.drawRings(phase: Float) {
    val maxRadius = size.maxDimension * 0.78f
    val base = phase * maxRadius
    val ringCount = 7

    for (i in 0 until ringCount) {
        val radius = (base + i * (maxRadius / ringCount)) % maxRadius
        val alpha = (1f - radius / maxRadius) * 0.55f
        val color = RING_COLORS[i % RING_COLORS.size]
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = size.minDimension * 0.012f),
            blendMode = BlendMode.Screen
        )
    }
}
