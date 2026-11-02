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
import app.kiosksync.core.ui.components.AppIcon
import app.kiosksync.core.ui.components.AppIconButton
import app.kiosksync.core.ui.components.AppIconButtonSize
import app.kiosksync.core.ui.components.AppStepper
import app.kiosksync.core.ui.components.AppTextField
import app.kiosksync.core.ui.theme.Space
import app.kiosksync.resources.Res
import app.kiosksync.resources.design_duration
import app.kiosksync.resources.design_duration_error
import app.kiosksync.resources.design_duration_help
import app.kiosksync.resources.design_duration_unit
import app.kiosksync.resources.design_hide_password
import app.kiosksync.resources.design_name_error
import app.kiosksync.resources.design_name_helper
import app.kiosksync.resources.design_name_label
import app.kiosksync.resources.design_password_label
import app.kiosksync.resources.design_sample_playlist_name
import app.kiosksync.resources.design_section_fields
import app.kiosksync.resources.design_section_stepper
import app.kiosksync.resources.design_show_password
import org.jetbrains.compose.resources.stringResource



@Composable
internal fun FieldSection() {
    val samplePlaylistName = stringResource(Res.string.design_sample_playlist_name)
    var name by remember(samplePlaylistName) { mutableStateOf(samplePlaylistName) }
    var invalidName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("kiosco-2026") }
    var passwordVisible by remember { mutableStateOf(false) }

    CatalogSection(stringResource(Res.string.design_section_fields)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.design_name_label),
                    helper = stringResource(Res.string.design_name_helper),
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = invalidName,
                    onValueChange = { invalidName = it },
                    label = stringResource(Res.string.design_name_label),
                    error = stringResource(Res.string.design_name_error),
                )
            }
            Box(modifier = Modifier.width(300.dp)) {
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(Res.string.design_password_label),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        AppIconButton(
                            icon = if (passwordVisible) AppIcon.VISIBILITY_OFF else AppIcon.VISIBILITY,
                            contentDescription = if (passwordVisible) {
                                stringResource(Res.string.design_hide_password)
                            } else {
                                stringResource(Res.string.design_show_password)
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
                    label = stringResource(Res.string.design_name_label),
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

    CatalogSection(stringResource(Res.string.design_section_stepper)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            AppStepper(
                value = duration,
                onValueChange = { duration = it },
                label = stringResource(Res.string.design_duration),
                range = 3..600,
                unit = stringResource(Res.string.design_duration_unit),
                helper = stringResource(Res.string.design_duration_help),
                modifier = Modifier.width(260.dp),
            )
            AppStepper(
                value = invalidDuration,
                onValueChange = { invalidDuration = it },
                label = stringResource(Res.string.design_duration),
                range = 3..600,
                unit = stringResource(Res.string.design_duration_unit),
                error = stringResource(Res.string.design_duration_error),
                modifier = Modifier.width(260.dp),
            )
        }
    }
}
