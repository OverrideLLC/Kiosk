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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppSelectableCard
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.components.OverrideFooter
import app.kiosksync.core.ui.components.OverrideLogo
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import org.jetbrains.compose.ui.tooling.preview.Preview
import app.kiosksync.resources.Res
import app.kiosksync.resources.app_tagline
import app.kiosksync.resources.app_title
import app.kiosksync.resources.exit_app
import app.kiosksync.resources.guide_button
import app.kiosksync.resources.role_change_hint
import app.kiosksync.resources.role_confirm_button
import app.kiosksync.resources.role_emitter_desc
import app.kiosksync.resources.role_emitter_title
import app.kiosksync.resources.role_receiver_desc
import app.kiosksync.resources.role_receiver_title
import app.kiosksync.resources.role_select_first
import app.kiosksync.resources.role_selection_subtitle
import app.kiosksync.resources.role_selection_title
import org.jetbrains.compose.resources.stringResource

/**
 * First-run role choice. Two large cards, arrow-key navigation and a single
 * primary action. Every colour, size and shape comes from the design system.
 */
@Composable
fun RoleRoot(viewModel: RoleViewModel = sessionViewModel()) {
    val state by viewModel.state.collectAsState()
    RoleSelectionScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun RoleSelectionScreen(
    state: RoleState,
    onAction: (RoleAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val selectedRole = state.selectedRole

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
                BrandHeader(onAction = onAction)

                Spacer(Modifier.height(Space.huge))

                Text(
                    text = stringResource(Res.string.role_selection_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = stringResource(Res.string.role_selection_subtitle),
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
                                    onAction(RoleAction.SelectRole(AppRole.EMITTER))
                                    true
                                }

                                Key.DirectionRight -> {
                                    onAction(RoleAction.SelectRole(AppRole.RECEIVER))
                                    true
                                }

                                else -> false
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(Space.lg),
                ) {
                    AppSelectableCard(
                        title = stringResource(Res.string.role_emitter_title),
                        description = stringResource(Res.string.role_emitter_desc),
                        icon = AppIcon.CAST,
                        selected = selectedRole == AppRole.EMITTER,
                        onClick = { onAction(RoleAction.SelectRole(AppRole.EMITTER)) },
                        modifier = Modifier.weight(1f).height(Sizes.roleCardHeight),
                    )
                    AppSelectableCard(
                        title = stringResource(Res.string.role_receiver_title),
                        description = stringResource(Res.string.role_receiver_desc),
                        icon = AppIcon.TV,
                        selected = selectedRole == AppRole.RECEIVER,
                        onClick = { onAction(RoleAction.SelectRole(AppRole.RECEIVER)) },
                        modifier = Modifier.weight(1f).height(Sizes.roleCardHeight),
                    )
                }

                Spacer(Modifier.height(Space.xl))

                AppButton(
                    text = stringResource(Res.string.role_confirm_button),
                    onClick = { onAction(RoleAction.ConfirmRole) },
                    enabled = selectedRole != null,
                    disabledReason = stringResource(Res.string.role_select_first),
                    size = AppButtonSize.LARGE,
                    icon = AppIcon.PLAY_ARROW,
                    modifier = Modifier.widthIn(min = Sizes.roleActionMinWidth, max = Sizes.roleActionMaxWidth),
                )

                Spacer(Modifier.height(Space.xs))
                Text(
                    text = if (selectedRole == null) {
                        stringResource(Res.string.role_select_first)
                    } else {
                        stringResource(Res.string.role_change_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )

            }
        }

        OverrideFooter(Modifier.padding(bottom = Space.md))
    }
}

@Preview
@Composable
private fun RoleSelectionScreenPreview() {
    KioskTheme {
        RoleSelectionScreen(
            state = RoleState(),
            onAction = {},
        )
    }
}

@Composable
private fun BrandHeader(
    onAction: (RoleAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

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
                text = stringResource(Res.string.app_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(Res.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        AppButton(
            text = stringResource(Res.string.guide_button),
            onClick = { onAction(RoleAction.OpenGuide) },
            variant = AppButtonVariant.TEXT,
            size = AppButtonSize.SMALL,
            icon = AppIcon.HELP,
        )
        AppButton(
            text = stringResource(Res.string.exit_app),
            onClick = { onAction(RoleAction.Exit) },
            variant = AppButtonVariant.TEXT,
            size = AppButtonSize.SMALL,
            icon = AppIcon.LOGOUT,
        )
    }
}
