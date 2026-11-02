@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.kiosksync.core.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.kiosksync.core.common.app.AppTheme
import app.kiosksync.core.ui.components.AppButton
import app.kiosksync.core.ui.components.AppCircularProgress
import app.kiosksync.core.ui.components.AppButtonGroup
import app.kiosksync.core.ui.components.AppButtonSize
import app.kiosksync.core.ui.components.AppButtonVariant
import app.kiosksync.core.ui.components.AppDialog
import app.kiosksync.core.ui.components.AppDropdownMenu
import app.kiosksync.core.ui.components.AppEmptyState
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppIconButtonVariant
import app.kiosksync.core.ui.components.AppLinearProgress
import app.kiosksync.core.ui.components.AppListItem
import app.kiosksync.core.ui.components.AppLoadingIndicator
import app.kiosksync.core.ui.components.AppMenuItem
import app.kiosksync.core.ui.components.AppSelectableCard
import app.kiosksync.core.ui.components.AppSnackbarHost
import app.kiosksync.core.ui.components.AppStatusChip
import app.kiosksync.core.ui.components.AppStatusTone
import app.kiosksync.core.ui.components.AppStepper
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.core.ui.components.AppTooltip
import app.kiosksync.core.ui.theme.KioskColors
import app.kiosksync.core.ui.theme.KioskDisplayType
import app.kiosksync.core.ui.theme.KioskScreenColors
import app.kiosksync.core.ui.theme.KioskShape
import app.kiosksync.core.ui.theme.KioskShapes
import app.kiosksync.core.ui.theme.KioskTheme
import app.kiosksync.core.ui.theme.Sizes
import app.kiosksync.core.ui.theme.Space
import kotlinx.coroutines.launch
import app.kiosksync.resources.Res
import app.kiosksync.resources.design_add_images
import app.kiosksync.resources.design_add_ip
import app.kiosksync.resources.design_cancel
import app.kiosksync.resources.design_dialog_text
import app.kiosksync.resources.design_dialog_title
import app.kiosksync.resources.design_empty_playlist_desc
import app.kiosksync.resources.design_empty_playlist_title
import app.kiosksync.resources.design_empty_screens_desc
import app.kiosksync.resources.design_empty_screens_title
import app.kiosksync.resources.design_menu_delete
import app.kiosksync.resources.design_menu_new
import app.kiosksync.resources.design_menu_rename
import app.kiosksync.resources.design_name_label
import app.kiosksync.resources.design_progress_label
import app.kiosksync.resources.design_save
import app.kiosksync.resources.design_section_empty
import app.kiosksync.resources.design_section_feedback
import app.kiosksync.resources.design_section_loading
import app.kiosksync.resources.design_show_dialog
import app.kiosksync.resources.design_show_menu
import app.kiosksync.resources.design_show_snackbar
import app.kiosksync.resources.design_tooltip_target
import app.kiosksync.resources.design_tooltip_text
import app.kiosksync.resources.waiting_content
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FeedbackSection(
    showSnackbar: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    CatalogSection(stringResource(Res.string.design_section_feedback)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            AppButton(
                text = stringResource(Res.string.design_show_dialog),
                onClick = { showDialog = true },
                variant = AppButtonVariant.OUTLINED,
                icon = AppIcon.EDIT,
            )
            Box {
                AppButton(
                    text = stringResource(Res.string.design_show_menu),
                    onClick = { showMenu = true },
                    variant = AppButtonVariant.OUTLINED,
                    icon = AppIcon.MORE_VERT,
                )
                AppDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = listOf(
                        AppMenuItem(
                            label = stringResource(Res.string.design_menu_new),
                            onClick = {},
                            icon = AppIcon.PLAYLIST_ADD,
                        ),
                        AppMenuItem(
                            label = stringResource(Res.string.design_menu_rename),
                            onClick = {},
                            icon = AppIcon.EDIT,
                        ),
                        AppMenuItem(
                            label = stringResource(Res.string.design_menu_delete),
                            onClick = {},
                            icon = AppIcon.DELETE,
                            destructive = true,
                        ),
                    ),
                )
            }
            AppButton(
                text = stringResource(Res.string.design_show_snackbar),
                onClick = showSnackbar,
                variant = AppButtonVariant.TONAL,
                icon = AppIcon.INFO,
            )
            AppTooltip(stringResource(Res.string.design_tooltip_text)) {
                AppButton(
                    text = stringResource(Res.string.design_tooltip_target),
                    onClick = {},
                    variant = AppButtonVariant.TEXT,
                    icon = AppIcon.HELP,
                )
            }
        }
    }

    if (showDialog) {
        AppDialog(
            title = stringResource(Res.string.design_dialog_title),
            onDismissRequest = { showDialog = false },
            icon = AppIcon.PLAYLIST_ADD,
            actions = {
                AppButton(
                    text = stringResource(Res.string.design_cancel),
                    onClick = { showDialog = false },
                    variant = AppButtonVariant.TEXT,
                )
                AppButton(
                    text = stringResource(Res.string.design_save),
                    onClick = { showDialog = false },
                    icon = AppIcon.CHECK,
                )
            },
            content = {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.design_name_label),
                    helper = stringResource(Res.string.design_dialog_text),
                )
            },
        )
    }
}

@Composable
internal fun LoadingSection() {
    val colors = KioskColors.scheme

    CatalogSection(stringResource(Res.string.design_section_loading)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                AppLoadingIndicator()
                Text(
                    text = stringResource(Res.string.waiting_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            AppCircularProgress()
            AppLinearProgress(
                progress = 0.42f,
                label = stringResource(Res.string.design_progress_label),
                modifier = Modifier.width(360.dp),
            )
        }
    }
}

@Composable
internal fun EmptySection() {
    CatalogSection(stringResource(Res.string.design_section_empty)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Surface(
                shape = KioskShape.card,
                color = KioskColors.scheme.surfaceContainerLow,
                modifier = Modifier.width(460.dp),
            ) {
                AppEmptyState(
                    icon = AppIcon.TV,
                    title = stringResource(Res.string.design_empty_screens_title),
                    description = stringResource(Res.string.design_empty_screens_desc),
                    action = {
                        AppButton(
                            text = stringResource(Res.string.design_add_ip),
                            onClick = {},
                            icon = AppIcon.ADD_LINK,
                        )
                    },
                )
            }
            Surface(
                shape = KioskShape.card,
                color = KioskColors.scheme.surfaceContainerLow,
                modifier = Modifier.width(460.dp),
            ) {
                AppEmptyState(
                    icon = AppIcon.PHOTO_LIBRARY,
                    title = stringResource(Res.string.design_empty_playlist_title),
                    description = stringResource(Res.string.design_empty_playlist_desc),
                    action = {
                        AppButton(
                            text = stringResource(Res.string.design_add_images),
                            onClick = {},
                            icon = AppIcon.ADD_PHOTO_ALTERNATE,
                        )
                    },
                )
            }
        }
    }
}
