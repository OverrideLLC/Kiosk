package app.kiosksync.platform

import app.kiosksync.NoOpLogger
import app.kiosksync.core.AppRole
import app.kiosksync.core.AppTheme
import app.kiosksync.feature.role.AppConfig
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DataStoreAppConfigStoreTest {
    private lateinit var root: File
    private val scopes = mutableListOf<CoroutineScope>()

    @BeforeTest
    fun setUp() {
        root = File(System.getProperty("java.io.tmpdir"), "kiosk-config-${System.nanoTime()}")
        root.mkdirs()
    }

    @AfterTest
    fun tearDown() {
        scopes.forEach { it.cancel() }
        root.deleteRecursively()
    }

    @Test
    fun migratesLegacyConfigOnceAndPreservesBackup() = runBlocking {
        val dataDir = File(root, "migration").apply { mkdirs() }
        val legacyContents = """
            {"role":"EMITTER","deviceId":"device-1","deviceName":"Control","port":8765}
        """.trimIndent()
        val legacy = File(dataDir, "config.json").apply { writeText(legacyContents) }
        val expected = AppConfig(
            role = AppRole.EMITTER,
            deviceId = "device-1",
            deviceName = "Control"
        )
        val store = createStore(dataDir)

        assertEquals(expected, store.initialize())
        assertEquals(expected, store.initialize())
        assertEquals(expected, store.config.first())
        assertFalse(legacy.exists())
        assertEquals(legacyContents, File(dataDir, "legacy/config.json").readText())

        scopes.last().cancel()
        val reopened = createStore(dataDir)
        assertEquals(expected, reopened.config.first())
    }

    @Test
    fun savesAndClearsConfigAndIsolatesDataDirectories() = runBlocking {
        val first = createStore(File(root, "first"))
        val second = createStore(File(root, "second"))
        val config = AppConfig(
            role = AppRole.RECEIVER,
            deviceId = "device-2",
            deviceName = "Screen",
            theme = AppTheme.LIGHT,
            reduceMotion = true
        )

        assertNull(first.initialize())
        assertNull(second.initialize())
        first.save(config)

        assertEquals(config, first.config.first())
        assertNull(second.config.first())
        first.clear()
        assertNull(first.config.first())
    }

    @Test
    fun invalidLegacyConfigIsKeptForRecovery() = runBlocking {
        val dataDir = File(root, "invalid").apply { mkdirs() }
        val legacy = File(dataDir, "config.json").apply { writeText("{not-json") }
        val store = createStore(dataDir)

        assertNull(store.initialize())
        assertEquals("{not-json", legacy.readText())
        assertFalse(File(dataDir, "legacy/config.json").exists())
    }

    private fun createStore(dataDir: File): DataStoreAppConfigStore {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return DataStoreAppConfigStore(dataDir, NoOpLogger(), scope)
    }
}
