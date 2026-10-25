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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.kiosksync.core.Strings
import app.kiosksync.feature.pairing.domain.PairingUiState
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppLoadingIndicator
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.components.OverrideLogo
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space

/**
 * Receiver pairing screen, designed to be read from several metres: a huge
 * grouped code, one instruction, the renewal countdown and a clear lockout
 * message. Pure black background regardless of the selected theme.
 */
@Composable
fun PairingCodeScreen(
    state: PairingUiState,
    serviceError: String?,
    onOpenGuide: () -> Unit = {},
    onExitRequested: () -> Unit,
    onChangeRoleRequested: () -> Unit,
    showAdminActions: Boolean = true,
    receiverIp: String? = null,
    receiverPort: Int? = null,
) {
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
                text = Strings.RECEIVER_PAIRING_TITLE,
                style = KioskDisplayType.screenTitle(),
                color = KioskScreenColors.content,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Space.xl))

            val displayedCode = formatPairingCode(state.currentCode)
            Text(
                text = displayedCode,
                style = KioskDisplayType.pairingCode(),
                color = if (state.isLocked) {
                    KioskScreenColors.disabled
                } else {
                    KioskScreenColors.content
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    contentDescription = "${Strings.PAIRING_CODE_LABEL}: $displayedCode"
                },
            )

            Spacer(Modifier.height(Space.lg))

            Text(
                text = Strings.ENTER_CODE_IN_CONTROLLER,
                style = KioskDisplayType.screenCaption(),
                color = KioskScreenColors.muted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Space.lg))

            if (state.isLocked) {
                LockoutMessage(
                    title = Strings.RECEIVER_LOCKED_TITLE,
                    message = Strings.receiverLockedMessage(state.lockRemainingSeconds),
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
                        text = Strings.receiverCodeRenewsIn(
                            formatCountdown(state.codeExpiresInSeconds)
                        ),
                        style = KioskDisplayType.screenCaption(),
                        color = KioskScreenColors.subtle,
                    )
                }
            }

            if (receiverPort != null) {
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
                            text = if (receiverIp != null) {
                                Strings.RECEIVER_IP_HINT
                            } else {
                                Strings.RECEIVER_IP_UNAVAILABLE
                            },
                            style = KioskDisplayType.screenCaption(),
                            color = KioskScreenColors.muted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Space.xs))
                        if (receiverIp != null) {
                            SelectionContainer {
                                Text(
                                    text = Strings.receiverAddress(receiverIp, receiverPort),
                                    style = KioskDisplayType.screenBody(),
                                    color = KioskScreenColors.content,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            Text(
                                text = "${Strings.RECEIVER_PORT_LABEL}: $receiverPort",
                                style = KioskDisplayType.screenBody(),
                                color = KioskScreenColors.content,
                            )
                        }
                    }
                }
            }

            if (serviceError != null) {
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
                        text = serviceError,
                        style = KioskDisplayType.screenCaption(),
                        color = KioskScreenColors.error,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }

        if (showAdminActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Space.lg),
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                AppButton(
                    text = Strings.GUIDE_BUTTON,
                    onClick = onOpenGuide,
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.HELP,
                )
                AppButton(
                    text = Strings.CHANGE_ROLE,
                    onClick = onChangeRoleRequested,
                    variant = AppButtonVariant.TEXT,
                    size = AppButtonSize.SMALL,
                    icon = AppIcon.SWAP_HORIZ,
                )
                AppButton(
                    text = Strings.EXIT_APP,
                    onClick = onExitRequested,
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
            text = Strings.APP_TITLE,
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
