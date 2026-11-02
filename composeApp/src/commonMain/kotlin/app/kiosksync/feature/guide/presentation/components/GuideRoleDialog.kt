package app.kiosksync.feature.guide.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.resources.Res
import app.kiosksync.resources.guide_role_body
import app.kiosksync.resources.guide_role_hint
import app.kiosksync.resources.guide_role_title
import app.kiosksync.resources.guide_skip
import app.kiosksync.resources.guide_title
import app.kiosksync.resources.role_emitter_title
import app.kiosksync.resources.role_receiver_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GuideRoleDialog(
    onChooseRole: (AppRole) -> Unit,
    onSkip: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    AppDialog(
        title = stringResource(Res.string.guide_title),
        icon = AppIcon.HELP,
        onDismissRequest = onSkip,
        content = {
            Text(
                text = stringResource(Res.string.guide_role_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = stringResource(Res.string.guide_role_body),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(
                    text = stringResource(Res.string.role_emitter_title),
                    onClick = { onChooseRole(AppRole.EMITTER) },
                    icon = AppIcon.CAST,
                    size = AppButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = stringResource(Res.string.role_receiver_title),
                    onClick = { onChooseRole(AppRole.RECEIVER) },
                    icon = AppIcon.TV,
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Space.sm))
            Text(
                text = stringResource(Res.string.guide_role_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        },
        actions = {
            AppButton(
                text = stringResource(Res.string.guide_skip),
                onClick = onSkip,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
        },
    )
}
