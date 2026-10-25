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
import app.kiosksync.core.AppRole
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.theme.Space

@Composable
internal fun GuideRoleDialog(
    onChooseRole: (AppRole) -> Unit,
    onSkip: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    AppDialog(
        title = Strings.GUIDE_TITLE,
        icon = AppIcon.HELP,
        onDismissRequest = onSkip,
        content = {
            Text(
                text = Strings.GUIDE_ROLE_TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                text = Strings.GUIDE_ROLE_BODY,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppButton(
                    text = Strings.ROLE_EMITTER_TITLE,
                    onClick = { onChooseRole(AppRole.EMITTER) },
                    icon = AppIcon.CAST,
                    size = AppButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    text = Strings.ROLE_RECEIVER_TITLE,
                    onClick = { onChooseRole(AppRole.RECEIVER) },
                    icon = AppIcon.TV,
                    variant = AppButtonVariant.TONAL,
                    size = AppButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Space.sm))
            Text(
                text = Strings.GUIDE_ROLE_HINT,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        },
        actions = {
            AppButton(
                text = Strings.GUIDE_SKIP,
                onClick = onSkip,
                variant = AppButtonVariant.TEXT,
                size = AppButtonSize.SMALL,
            )
        },
    )
}
