package app.kiosksync.feature.role

import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppRole
import app.kiosksync.platform.LocalStorage
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeLocalStorage : LocalStorage {
    var savedConfig: AppConfig? = null
    var clearConfigCalled = false
    var clearPairingCalled = false

    override fun loadConfig(): AppConfig? = savedConfig

    override fun saveConfig(config: AppConfig) {
        savedConfig = config
    }

    override fun clearConfig() {
        clearConfigCalled = true
        savedConfig = null
        clearPairing()
    }

    override fun clearPairing() {
        clearPairingCalled = true
    }
}

class NoOpLogger : AppLogger {
    override fun info(message: String) {}
    override fun warn(message: String, throwable: Throwable?) {}
    override fun error(message: String, throwable: Throwable?) {}
    override fun debug(message: String) {}
}

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
    fun testRoleStateHolderFlow() {
        val fakeStorage = FakeLocalStorage()
        val logger = NoOpLogger()
        val stateHolder = RoleStateHolder(fakeStorage, logger)

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
        assertEquals(config, fakeStorage.savedConfig)

        // Request Change Role
        stateHolder.requestChangeRole()
        assertTrue(stateHolder.uiState.value.showChangeRoleDialog)

        // Dismiss dialog
        stateHolder.dismissChangeRoleDialog()
        assertFalse(stateHolder.uiState.value.showChangeRoleDialog)

        // Confirm change role
        stateHolder.confirmChangeRole()
        assertTrue(fakeStorage.clearConfigCalled)
        assertNull(fakeStorage.savedConfig)
        assertNull(stateHolder.uiState.value.selectedRole)
    }
}
