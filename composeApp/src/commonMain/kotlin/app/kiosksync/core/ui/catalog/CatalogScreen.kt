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
import app.kiosksync.core.AppTheme
import app.kiosksync.core.Strings
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

/**
 * Development-only gallery of every token and component. It is opened with
 * `--ui-catalog` and never part of a user flow.
 */
@Composable
fun CatalogScreen() {
    var theme by remember { mutableStateOf(AppTheme.DARK) }
    var reduceMotion by remember { mutableStateOf(false) }

    KioskTheme(theme = theme, reduceMotion = reduceMotion) {
        CatalogContent(
            theme = theme,
            reduceMotion = reduceMotion,
            onThemeChange = { theme = it },
            onReduceMotionChange = { reduceMotion = it },
        )
    }
}

@Composable
fun CatalogContent(
    theme: AppTheme,
    reduceMotion: Boolean,
    onThemeChange: (AppTheme) -> Unit = {},
    onReduceMotionChange: (Boolean) -> Unit = {},
) {
    val colors = KioskColors.scheme
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CatalogTopBar(
                theme = theme,
                reduceMotion = reduceMotion,
                onThemeChange = onThemeChange,
                onReduceMotionChange = onReduceMotionChange,
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.xxl),
                ) {
                    ColorSection()
                    TypographySection()
                    DisplaySection()
                    ShapeSection()
                    ButtonSection()
                    ButtonGroupSection()
                    IconSection()
                    FieldSection()
                    StepperSection()
                    ChipSection()
                    CardSection()
                    ListSection()
                    FeedbackSection(showSnackbar = {
                        scope.launch {
                            hostState.showSnackbar(
                                message = Strings.DESIGN_SNACKBAR_MESSAGE,
                                actionLabel = Strings.DESIGN_UNDO,
                                withDismissAction = true,
                            )
                        }
                    })
                    LoadingSection()
                    EmptySection()
                    Spacer(Modifier.height(Space.huge))
                }
            }
        }
        AppSnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Space.lg),
        )
    }
}

@Composable
private fun CatalogTopBar(
    theme: AppTheme,
    reduceMotion: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
) {
    val colors = KioskColors.scheme

    Surface(color = colors.surfaceContainerLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.APP_TITLE,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = Strings.DESIGN_TITLE,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = Strings.DESIGN_SUBTITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            AppButtonGroup(
                options = listOf(Strings.DESIGN_THEME_DARK, Strings.DESIGN_THEME_LIGHT),
                selectedIndex = if (theme == AppTheme.DARK) 0 else 1,
                onSelect = { index ->
                    onThemeChange(if (index == 0) AppTheme.DARK else AppTheme.LIGHT)
                },
                modifier = Modifier.width(240.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Text(
                    text = Strings.DESIGN_REDUCE_MOTION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Switch(
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChange,
                )
            }
        }
    }
}
