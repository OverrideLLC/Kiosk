package app.kiosksync.feature.playback

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.core.Strings

private data class DisplayedImage(
    val contentKey: String,
    val bitmap: ImageBitmap
)

/** Long, soft fade so image changes look clean instead of abrupt. */
private const val CROSSFADE_DURATION_MS = 1100

@Composable
fun CarouselScreen(
    stateHolder: PlaybackStateHolder,
    onExitRequested: () -> Unit,
    onChangeRoleRequested: () -> Unit
) {
    val uiState by stateHolder.uiState.collectAsState()

    val displayedImage = uiState.currentItem?.let { item ->
        uiState.currentBitmap?.let { bitmap -> DisplayedImage(item.sha256, bitmap) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.hasItems && displayedImage != null) {
            Crossfade(
                targetState = displayedImage,
                animationSpec = tween(
                    durationMillis = CROSSFADE_DURATION_MS,
                    easing = FastOutSlowInEasing
                )
            ) { image ->
                Image(
                    bitmap = image.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            // No playlist yet (or every image is unreadable): play the default
            // animated carousel so the screen never looks dead.
            DefaultIdleCarousel()
        }

        // Exit / Options Dialog (opened via Esc or call)
        if (uiState.isExitDialogOpen) {
            AlertDialog(
                onDismissRequest = { stateHolder.dismissExitDialog() },
                title = {
                    Text(
                        text = "Opciones de Pantalla",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "¿Deseas salir del modo quiosco o cambiar de rol?",
                            color = Color(0xFFCCCCCC)
                        )
                        if (uiState.currentItem != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Reproduciendo: ${uiState.currentItem?.fileName} (${uiState.currentIndex + 1}/${uiState.playlist?.items?.size ?: 0})",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Row {
                        OutlinedButton(
                            onClick = {
                                stateHolder.dismissExitDialog()
                                onChangeRoleRequested()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE57373)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(Strings.CHANGE_ROLE)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                stateHolder.dismissExitDialog()
                                onExitRequested()
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Salir de la app")
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { stateHolder.dismissExitDialog() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Continuar")
                    }
                },
                backgroundColor = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
