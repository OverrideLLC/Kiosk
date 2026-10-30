package app.kiosksync.feature.playback.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.Motion
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.feature.playback.presentation.components.DefaultIdleCarousel
import org.jetbrains.compose.ui.tooling.preview.Preview

private data class DisplayedImage(
    val contentKey: String,
    val bitmap: ImageBitmap
)

/** Soft fade between images, as specified by the Screen role. */
private const val CROSSFADE_DURATION_MS = 500

/**
 * Fullscreen playback. Pure black, image fitted, no chrome. When there is no
 * playlist (or every image is unreadable) the branded idle carousel plays and
 * a discreet caption explains that content is pending.
 */
@Composable
fun CarouselRoot(holder: PlaybackStateHolder) {
    val state by holder.state.collectAsState()
    CarouselScreen(state = state)
}

@Composable
fun CarouselScreen(state: PlaybackState) {
    val displayedImage = state.currentItem?.let { item ->
        state.currentBitmap?.let { bitmap -> DisplayedImage(item.sha256, bitmap) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KioskScreenColors.background),
        contentAlignment = Alignment.Center,
    ) {
        if (state.hasItems && displayedImage != null) {
            Crossfade(
                targetState = displayedImage,
                animationSpec = Motion.fade(CROSSFADE_DURATION_MS),
            ) { image ->
                Image(
                    bitmap = image.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            DefaultIdleCarousel()
            Text(
                text = Strings.WAITING_CONTENT,
                style = KioskDisplayType.screenCaption(),
                color = KioskScreenColors.subtle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Space.xl),
            )
        }
    }
}

@Preview
@Composable
private fun CarouselScreenPreview() {
    KioskTheme {
        CarouselScreen(state = PlaybackState())
    }
}
