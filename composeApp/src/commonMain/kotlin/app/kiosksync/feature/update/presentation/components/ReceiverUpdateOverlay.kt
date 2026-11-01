package app.kiosksync.feature.update.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppLoadingIndicator
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.feature.update.presentation.ReceiverUpdateAction
import app.kiosksync.feature.update.presentation.ReceiverUpdateState
import app.kiosksync.resources.Res
import app.kiosksync.resources.security_close
import app.kiosksync.resources.update_applying
import app.kiosksync.resources.update_available
import app.kiosksync.resources.update_forced
import app.kiosksync.resources.update_forced_title
import app.kiosksync.resources.update_later
import app.kiosksync.resources.update_now
import app.kiosksync.resources.update_title
import org.jetbrains.compose.resources.stringResource
import app.kiosksync.core.ui.resolve

/**
 * Update prompt shown inside the receiver's fullscreen window. Keeping it in
 * the same window avoids z-order problems with the always-on-top kiosk window.
 */
@Composable
fun ReceiverUpdateOverlay(
    state: ReceiverUpdateState,
    onAction: (ReceiverUpdateAction) -> Unit,
) {
    val version = state.version ?: return
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.84f))
            .padding(Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(),
            shape = KioskShape.dialog,
            color = colors.surfaceContainerHigh,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(
                    icon = AppIcon.UPLOAD,
                    contentDescription = null,
                    size = Sizes.iconLarge,
                    tint = colors.primary,
                )
                Spacer(Modifier.height(Space.md))
                Text(
                    text = if (state.forced) {
                        stringResource(Res.string.update_forced_title)
                    } else {
                        stringResource(Res.string.update_title)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = if (state.forced) {
                        stringResource(Res.string.update_forced, version)
                    } else {
                        stringResource(Res.string.update_available, version, state.currentVersion)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.lg))

                if (state.applying) {
                    AppLoadingIndicator(color = colors.primary)
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = stringResource(Res.string.update_applying),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage.resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Space.sm))
                        AppButton(
                            text = stringResource(Res.string.security_close),
                            onClick = { onAction(ReceiverUpdateAction.DismissError) },
                            variant = AppButtonVariant.TEXT,
                            size = AppButtonSize.SMALL,
                        )
                        Spacer(Modifier.height(Space.md))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!state.forced) {
                            AppButton(
                                text = stringResource(Res.string.update_later),
                                onClick = { onAction(ReceiverUpdateAction.Decline) },
                                variant = AppButtonVariant.OUTLINED,
                                size = AppButtonSize.LARGE,
                            )
                            Spacer(Modifier.width(Space.xs))
                        }
                        AppButton(
                            text = stringResource(Res.string.update_now),
                            onClick = { onAction(ReceiverUpdateAction.Accept) },
                            variant = AppButtonVariant.FILLED,
                            size = AppButtonSize.LARGE,
                            icon = AppIcon.UPLOAD,
                        )
                    }
                }
            }
        }
    }
}
