package app.kiosksync.feature.role.presentation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.core.Strings
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppSelectableCard
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.components.OverrideFooter
import app.kiosksync.core.ui.components.OverrideLogo
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space

/**
 * First-run role choice. Two large cards, arrow-key navigation and a single
 * primary action. Every colour, size and shape comes from the design system.
 */
@Composable
fun RoleSelectionScreen(
    stateHolder: RoleStateHolder,
    onRoleConfirmed: (AppConfig) -> Unit,
    deviceId: String,
    deviceName: String,
    port: Int,
    onExitRequested: () -> Unit = {},
    theme: AppTheme = AppTheme.DARK,
    reduceMotion: Boolean = false,
    onOpenGuide: () -> Unit = {}
) {
    val uiState by stateHolder.uiState.collectAsState()
    val currentOnRoleConfirmed by rememberUpdatedState(onRoleConfirmed)
    LaunchedEffect(stateHolder) {
        stateHolder.effects.collect { effect ->
            when (effect) {
                is RoleEffect.RoleConfirmed -> currentOnRoleConfirmed(effect.config)
            }
        }
    }
    val colors = KioskColors.scheme
    val selectedRole = uiState.selectedRole

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.xl, vertical = Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxWidth()) {
                BrandHeader(
                    onExitRequested = onExitRequested,
                    onOpenGuide = onOpenGuide
                )

                Spacer(Modifier.height(Space.huge))

                Text(
                    text = Strings.ROLE_SELECTION_TITLE,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = Strings.ROLE_SELECTION_SUBTITLE,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )

                Spacer(Modifier.height(Space.xl))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    stateHolder.dispatch(RoleAction.SelectRole(AppRole.EMITTER))
                                    true
                                }

                                Key.DirectionRight -> {
                                    stateHolder.dispatch(RoleAction.SelectRole(AppRole.RECEIVER))
                                    true
                                }

                                else -> false
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(Space.lg),
                ) {
                    AppSelectableCard(
                        title = Strings.ROLE_EMITTER_TITLE,
                        description = Strings.ROLE_EMITTER_DESC,
                        icon = AppIcon.CAST,
                        selected = selectedRole == AppRole.EMITTER,
                        onClick = { stateHolder.dispatch(RoleAction.SelectRole(AppRole.EMITTER)) },
                        modifier = Modifier.weight(1f).height(Sizes.roleCardHeight),
                    )
                    AppSelectableCard(
                        title = Strings.ROLE_RECEIVER_TITLE,
                        description = Strings.ROLE_RECEIVER_DESC,
                        icon = AppIcon.TV,
                        selected = selectedRole == AppRole.RECEIVER,
                        onClick = { stateHolder.dispatch(RoleAction.SelectRole(AppRole.RECEIVER)) },
                        modifier = Modifier.weight(1f).height(Sizes.roleCardHeight),
                    )
                }

                Spacer(Modifier.height(Space.xl))

                AppButton(
                    text = Strings.ROLE_CONFIRM_BUTTON,
                    onClick = {
                        stateHolder.dispatch(RoleAction.ConfirmRole(
                            deviceId = deviceId,
                            deviceName = deviceName,
                            port = port,
                            theme = theme,
                            reduceMotion = reduceMotion,
                        ))
                    },
                    enabled = selectedRole != null,
                    disabledReason = Strings.ROLE_SELECT_FIRST,
                    size = AppButtonSize.LARGE,
                    icon = AppIcon.PLAY_ARROW,
                    modifier = Modifier.widthIn(min = Sizes.roleActionMinWidth, max = Sizes.roleActionMaxWidth),
                )

                Spacer(Modifier.height(Space.xs))
                Text(
                    text = if (selectedRole == null) {
                        Strings.ROLE_SELECT_FIRST
                    } else {
                        Strings.ROLE_CHANGE_HINT
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )

            }
        }

        OverrideFooter(Modifier.padding(bottom = Space.md))
    }
}

@Composable
private fun BrandHeader(
    onExitRequested: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val colors = KioskColors.scheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.avatar)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            OverrideLogo(
                size = Sizes.iconCompact,
                tint = colors.onPrimaryContainer,
            )
        }
        Spacer(Modifier.size(Space.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Strings.APP_TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Text(
                text = Strings.APP_TAGLINE,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        AppButton(
            text = Strings.GUIDE_BUTTON,
            onClick = onOpenGuide,
            variant = AppButtonVariant.TEXT,
            size = AppButtonSize.SMALL,
            icon = AppIcon.HELP,
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
