@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.kiosksync.core.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.kiosksync.core.Strings
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppStepper
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.core.ui.theme.Space



@Composable
internal fun FieldSection() {
    var name by remember { mutableStateOf("Promociones de octubre") }
    var invalidName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("kiosco-2026") }
    var passwordVisible by remember { mutableStateOf(false) }

    CatalogSection(Strings.DESIGN_SECTION_FIELDS) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = Strings.DESIGN_NAME_LABEL,
                    helper = Strings.DESIGN_NAME_HELPER,
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = invalidName,
                    onValueChange = { invalidName = it },
                    label = Strings.DESIGN_NAME_LABEL,
                    error = Strings.DESIGN_NAME_ERROR,
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = Strings.DESIGN_PASSWORD_LABEL,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        AppIconButton(
                            icon = if (passwordVisible) AppIcon.VISIBILITY_OFF else AppIcon.VISIBILITY,
                            contentDescription = if (passwordVisible) {
                                Strings.DESIGN_HIDE_PASSWORD
                            } else {
                                Strings.DESIGN_SHOW_PASSWORD
                            },
                            onClick = { passwordVisible = !passwordVisible },
                            size = AppIconButtonSize.SMALL,
                        )
                    },
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = {},
                    label = Strings.DESIGN_NAME_LABEL,
                    enabled = false,
                )
            }
        }
    }
}

@Composable
internal fun StepperSection() {
    var duration by remember { mutableIntStateOf(10) }
    var invalidDuration by remember { mutableIntStateOf(1) }

    CatalogSection(Strings.DESIGN_SECTION_STEPPER) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            AppStepper(
                value = duration,
                onValueChange = { duration = it },
                label = Strings.DESIGN_DURATION,
                range = 3..600,
                unit = Strings.DESIGN_DURATION_UNIT,
                helper = Strings.DESIGN_DURATION_HELP,
                modifier = Modifier.width(260.dp),
            )
            AppStepper(
                value = invalidDuration,
                onValueChange = { invalidDuration = it },
                label = Strings.DESIGN_DURATION,
                range = 3..600,
                unit = Strings.DESIGN_DURATION_UNIT,
                error = Strings.DESIGN_DURATION_ERROR,
                modifier = Modifier.width(260.dp),
            )
        }
    }
}
