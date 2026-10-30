package app.kiosksync.feature.receiver.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppLoadingIndicator
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Shown after pairing while the screen still has no blocking password. The
 * controller sends it as soon as the person configures (or already has) a
 * password, and then the carousel takes over.
 */
@Composable
fun WaitingForPasswordScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KioskScreenColors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Sizes.contentMaxWidth)
                .padding(Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppLoadingIndicator(
                color = KioskScreenColors.muted,
                dotSize = Sizes.loadingDotLarge,
            )
            Spacer(Modifier.height(Space.xl))
            Text(
                text = Strings.RECEIVER_WAITING_PASSWORD_TITLE,
                style = KioskDisplayType.screenTitle(),
                color = KioskScreenColors.content,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.md))
            Text(
                text = Strings.RECEIVER_WAITING_PASSWORD_MESSAGE,
                style = KioskDisplayType.screenCaption(),
                color = KioskScreenColors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun WaitingForPasswordScreenPreview() {
    KioskTheme {
        WaitingForPasswordScreen()
    }
}
