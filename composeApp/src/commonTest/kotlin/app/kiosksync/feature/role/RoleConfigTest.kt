package app.kiosksync.feature.role

import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.feature.role.domain.AppConfig
import app.kiosksync.feature.role.presentation.RoleAction
import app.kiosksync.feature.role.presentation.RoleEffect
import app.kiosksync.feature.role.presentation.RoleReducer
import app.kiosksync.feature.role.presentation.RoleStateHolder
import app.kiosksync.feature.role.presentation.RoleUiState
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlin.test.assertTrue

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
    fun testRoleStateHolderFlow() {
        val logger = NoOpLogger()
        val stateHolder = RoleStateHolder(logger)

        // Initial state
        assertNull(stateHolder.uiState.value.selectedRole)
        assertFalse(stateHolder.uiState.value.showChangeRoleDialog)

        // Select Role
        stateHolder.selectRole(AppRole.RECEIVER)
        assertEquals(AppRole.RECEIVER, stateHolder.uiState.value.selectedRole)

        // Confirm Role
        val config = stateHolder.confirmRole("dev-1", "Screen1", 8765)
        assertNotNull(config)
        assertEquals(AppRole.RECEIVER, config.role)

        // Request Change Role
        stateHolder.requestChangeRole()
        assertTrue(stateHolder.uiState.value.showChangeRoleDialog)

        // Dismiss dialog
        stateHolder.dismissChangeRoleDialog()
        assertFalse(stateHolder.uiState.value.showChangeRoleDialog)

        // Confirm change role
        stateHolder.confirmChangeRole()
        assertNull(stateHolder.uiState.value.selectedRole)
    }

    @Test
    fun roleReducerChangesSelectionAndChangeDialogState() {
        val selected = RoleReducer.reduce(RoleUiState(), RoleAction.SelectRole(AppRole.EMITTER))
        assertEquals(AppRole.EMITTER, selected.selectedRole)

        val opened = RoleReducer.reduce(selected, RoleAction.RequestChangeRole)
        assertTrue(opened.showChangeRoleDialog)

        val dismissed = RoleReducer.reduce(opened, RoleAction.DismissChangeRoleDialog)
        assertFalse(dismissed.showChangeRoleDialog)

        assertEquals(RoleUiState(), RoleReducer.reduce(dismissed, RoleAction.ConfirmChangeRole))
    }

    @Test
    fun confirmingRoleEmitsSingleOneShotEffect() = runBlocking {
        val holder = RoleStateHolder(NoOpLogger(), initialRole = AppRole.RECEIVER)
        holder.dispatch(
            RoleAction.ConfirmRole(
                deviceId = "device-1",
                deviceName = "Pantalla",
                port = 8765,
                theme = AppTheme.DARK,
                reduceMotion = false
            )
        )

        assertEquals(
            RoleEffect.RoleConfirmed(AppConfig(AppRole.RECEIVER, "device-1", "Pantalla", 8765)),
            holder.effects.first()
        )
    }
}
