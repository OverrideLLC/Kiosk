package app.kiosksync.feature.pairing.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppLoadingIndicator
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.components.OverrideLogo
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import org.jetbrains.compose.ui.tooling.preview.Preview
import app.kiosksync.resources.Res
import app.kiosksync.resources.app_title
import app.kiosksync.resources.change_role
import app.kiosksync.resources.enter_code_in_controller
import app.kiosksync.resources.exit_app
import app.kiosksync.resources.guide_button
import app.kiosksync.resources.pairing_code_label
import app.kiosksync.resources.receiver_code_renews_in_value
import app.kiosksync.resources.receiver_ip_hint
import app.kiosksync.resources.receiver_ip_unavailable
import app.kiosksync.resources.receiver_locked_countdown
import app.kiosksync.resources.receiver_locked_title
import app.kiosksync.resources.receiver_pairing_title
import app.kiosksync.resources.receiver_port_label
import org.jetbrains.compose.resources.stringResource
import app.kiosksync.core.ui.resolve

/**
 * Receiver pairing screen, designed to be read from several metres: a huge
 * grouped code, one instruction, the renewal countdown and a clear lockout
 * message. Pure black background regardless of the selected theme.
 */
@Composable
fun PairingRoot(viewModel: PairingViewModel = sessionViewModel()) {
    val state by viewModel.state.collectAsState()
    PairingCodeScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun PairingCodeScreen(
    state: PairingUiState,
    onAction: (PairingAction) -> Unit,
) {
    val pairing = state.pairing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KioskScreenColors.background),
    ) {
        BrandMark(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Space.xl),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = Sizes.contentMaxWidth)
                .padding(horizontal = Space.xl, vertical = Space.huge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.receiver_pairing_title),
                style = KioskDisplayType.screenTitle(),
                color = KioskScreenColors.content,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Space.xl))

            val displayedCode = formatPairingCode(pairing.currentCode)
            val codeDescription = "${stringResource(Res.string.pairing_code_label)}: $displayedCode"
            Text(
                text = displayedCode,
                style = KioskDisplayType.pairingCode(),
                color = if (pairing.isLocked) {
                    KioskScreenColors.disabled
                } else {
                    KioskScreenColors.content
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    contentDescription = codeDescription
                },
            )

            Spacer(Modifier.height(Space.lg))

            Text(
                text = stringResource(Res.string.enter_code_in_controller),
                style = KioskDisplayType.screenCaption(),
                color = KioskScreenColors.muted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Space.lg))

            if (pairing.isLocked) {
                LockoutMessage(
                    title = stringResource(Res.string.receiver_locked_title),
                    message = stringResource(Res.string.receiver_locked_countdown, pairing.lockRemainingSeconds),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    AppLoadingIndicator(
                        color = KioskScreenColors.subtle,
                        dotSize = Sizes.loadingDot,
                    )
                    Text(
                        text = stringResource(
                            Res.string.receiver_code_renews_in_value,
                            formatCountdown(pairing.codeExpiresInSeconds),
                        ),
                        style = KioskDisplayType.screenCaption(),
                        color = KioskScreenColors.subtle,
                    )
                }
            }

            if (state.receiverPort != null) {
                Spacer(Modifier.height(Space.xl))
                Surface(
                    color = KioskScreenColors.surfaceSubtle,
                    shape = KioskShape.card,
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = Space.xl,
                            vertical = Space.md,
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (state.receiverIp != null) {
                                stringResource(Res.string.receiver_ip_hint)
                            } else {
                                stringResource(Res.string.receiver_ip_unavailable)
                            },
                            style = KioskDisplayType.screenCaption(),
                            color = KioskScreenColors.muted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Space.xs))
                        if (state.receiverIp != null) {
                            SelectionContainer {
                                Text(
                                    text = "${state.receiverIp}:${state.receiverPort}",
                                    style = KioskDisplayType.screenBody(),
                                    color = KioskScreenColors.content,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            Text(
                                text = "${stringResource(Res.string.receiver_port_label)}: ${state.receiverPort}",
                                style = KioskDisplayType.screenBody(),
                                color = KioskScreenColors.content,
                            )
                        }
                    }
                }
            }

            if (state.serviceError != null) {
                Spacer(Modifier.height(Space.lg))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    AppIcon(
                        icon = AppIcon.ERROR,
                        contentDescription = null,
                        size = Sizes.iconLarge,
                        tint = KioskScreenColors.error,
                    )
                    Text(
                        text = state.serviceError.resolve(),
                        style = KioskDisplayType.screenCaption(),
                        color = KioskScreenColors.error,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }

        if (state.showAdminActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                AppButton(
                    text = stringResource(Res.string.guide_button),
                    onClick = { onAction(PairingAction.OpenGuide) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.HELP,
                )
                AppButton(
                    text = stringResource(Res.string.change_role),
                    onClick = { onAction(PairingAction.ChangeRoleRequested) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.SWAP_HORIZ,
                )
                AppButton(
                    text = stringResource(Res.string.exit_app),
                    onClick = { onAction(PairingAction.ExitRequested) },
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.LOGOUT,
                )
            }
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.minTouchTarget)
                .clip(CircleShape)
                .background(KioskScreenColors.surfaceSubtle),
            contentAlignment = Alignment.Center,
        ) {
            OverrideLogo(
                size = Sizes.iconCompact,
                tint = KioskScreenColors.muted,
            )
        }
        Text(
            text = stringResource(Res.string.app_title),
            style = MaterialTheme.typography.titleMedium,
            color = KioskScreenColors.subtle,
        )
    }
}

@Composable
private fun LockoutMessage(title: String, message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        AppIcon(
            icon = AppIcon.LOCK,
            contentDescription = null,
            size = Sizes.iconLarge,
            tint = KioskScreenColors.error,
        )
        Column {
            Text(
                text = title,
                style = KioskDisplayType.screenBody(),
                color = KioskScreenColors.error,
            )
            Text(
                text = message,
                style = KioskDisplayType.screenCaption(),
                color = KioskScreenColors.error,
            )
        }
    }
}

@Preview
@Composable
private fun PairingCodeScreenPreview() {
    KioskTheme {
        PairingCodeScreen(
            state = PairingUiState(),
            onAction = {},
        )
    }
}
