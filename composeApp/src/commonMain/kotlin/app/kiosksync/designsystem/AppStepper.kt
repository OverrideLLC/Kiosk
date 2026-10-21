package app.kiosksync.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiosksync.core.Strings

/**
 * Numeric field with a stepper. Used for the default and per-image duration.
 * Arrow buttons clamp automatically; text input is validated by the caller so
 * the error message can explain the allowed range.
 */
@Composable
fun AppStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    range: IntRange = 1..600,
    unit: String? = null,
    enabled: Boolean = true,
    helper: String? = null,
    error: String? = null,
) {
    val colors = KioskColors.scheme
    val extra = KioskColors.extra
    val borderColor = if (error != null) colors.error else colors.outlineVariant

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.xs))
        }
        Surface(
            shape = KioskShape.chip,
            color = if (enabled) colors.surfaceContainerHigh else extra.disabledContainer,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier
                    .height(Sizes.fieldHeight)
                    .padding(horizontal = Space.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xxs),
            ) {
                AppIconButton(
                    icon = AppIcon.REMOVE,
                    contentDescription = Strings.STEPPER_DECREASE,
                    onClick = { onValueChange((value - 1).coerceIn(range)) },
                    enabled = enabled && value > range.first,
                    variant = AppIconButtonVariant.STANDARD,
                    size = AppIconButtonSize.SMALL,
                )
                BasicTextField(
                    value = value.toString(),
                    onValueChange = { text ->
                        val digits = text.filter { it.isDigit() }
                        if (digits.isEmpty()) {
                            onValueChange(range.first)
                        } else {
                            digits.toIntOrNull()?.let { onValueChange(it.coerceIn(0, 9999)) }
                        }
                    },
                    modifier = Modifier.width(64.dp),
                    enabled = enabled,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = if (enabled) colors.onSurface else extra.disabledContent,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (unit != null) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                AppIconButton(
                    icon = AppIcon.ADD,
                    contentDescription = Strings.STEPPER_INCREASE,
                    onClick = { onValueChange((value + 1).coerceIn(range)) },
                    enabled = enabled && value < range.last,
                    variant = AppIconButtonVariant.STANDARD,
                    size = AppIconButtonSize.SMALL,
                )
            }
        }
        val support = error ?: helper
        if (support != null) {
            Spacer(Modifier.height(Space.xxs))
            Text(
                text = support,
                style = MaterialTheme.typography.labelMedium,
                color = if (error != null) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
