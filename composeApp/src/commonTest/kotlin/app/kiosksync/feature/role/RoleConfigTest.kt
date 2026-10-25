package app.kiosksync.feature.role

import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.feature.role.presentation.RoleAction
import app.kiosksync.feature.role.presentation.RoleEffect
import app.kiosksync.feature.role.presentation.RoleViewModel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class RoleConfigTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun testAppConfigSerialization() {
        val config = AppConfig(
            role = AppRole.EMITTER,
            deviceId = "test-device-uuid-1234",
            deviceName = "MainPC",
            port = 8765
        )
        val encoded = json.encodeToString(AppConfig.serializer(), config)
        val decoded = json.decodeFromString<AppConfig>(encoded)

        assertEquals(config.role, decoded.role)
        assertEquals(config.deviceId, decoded.deviceId)
        assertEquals(config.deviceName, decoded.deviceName)
        assertEquals(config.port, decoded.port)
    }

    @Test
    fun testThemePreferenceSerialization() {
        val config = AppConfig(
            role = AppRole.RECEIVER,
            deviceId = "device",
            deviceName = "Screen",
            theme = AppTheme.LIGHT
        )
        val decoded = json.decodeFromString<AppConfig>(
            json.encodeToString(AppConfig.serializer(), config)
        )

        assertEquals(AppTheme.LIGHT, decoded.theme)
    }

    @Test
    fun testLegacyConfigWithoutThemeDefaultsToDark() {
        val legacy = """
            {
              "role": "EMITTER",
              "deviceId": "device",
              "deviceName": "Controller",
              "port": 8765
            }
        """.trimIndent()
        val decoded = json.decodeFromString<AppConfig>(legacy)

        assertEquals(AppTheme.DARK, decoded.theme)
    }

    @Test
    fun roleViewModelEmitsConfirmedRoleAndResetsSelection() = runBlocking {
        val viewModel = RoleViewModel(NoOpLogger())

        assertNull(viewModel.state.value.selectedRole)

        viewModel.onAction(RoleAction.SelectRole(AppRole.RECEIVER))
        assertEquals(AppRole.RECEIVER, viewModel.state.value.selectedRole)

        viewModel.onAction(RoleAction.ConfirmRole)
        assertEquals(RoleEffect.RoleConfirmed(AppRole.RECEIVER), viewModel.effects.first())

        viewModel.onAction(RoleAction.ResetSelection)
        assertNull(viewModel.state.value.selectedRole)
    }
}
