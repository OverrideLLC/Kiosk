package app.kiosksync.feature.update.data

import app.kiosksync.NoOpLogger
import app.kiosksync.data.security.Sha256
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class FileReceiverUpdateManagerTest {

    @Test
    fun stagesAValidOptionalPackage() = runBlocking {
        withTempInstall { installDir, dataDir ->
            val manager = manager(dataDir, installDir)
            val bytes = packageBytes(
                version = "1.1.0",
                extraEntries = emptyMap()
            )

            val result = manager.stagePackage("1.1.0", Sha256.hex(bytes), bytes)

            assertFalse(result.getOrThrow())
            assertEquals("1.1.0", manager.state.value.version)
            assertFalse(manager.state.value.forced)
            assertTrue(File(dataDir, "updates/staged-1.1.0/app/app.jar").isFile)
        }
    }

    @Test
    fun differentMajorIsMarkedAsForced() = runBlocking {
        withTempInstall { installDir, dataDir ->
            val manager = manager(dataDir, installDir)
            val bytes = packageBytes(version = "2.0.0")

            val forced = manager.stagePackage("2.0.0", Sha256.hex(bytes), bytes).getOrThrow()

            assertTrue(forced)
            assertTrue(manager.state.value.forced)
        }
    }

    @Test
    fun sameVersionIsRejected() = runBlocking {
        withTempInstall { installDir, dataDir ->
            val manager = manager(dataDir, installDir)
            val bytes = packageBytes(version = "1.0.0")

            assertTrue(
                manager.stagePackage("1.0.0", Sha256.hex(bytes), bytes).isFailure
            )
            assertNull(manager.state.value.version)
        }
    }

    @Test
    fun rejectsTraversalAndAbsolutePaths() = runBlocking {
        withTempInstall { installDir, dataDir ->
            val manager = manager(dataDir, installDir)
            val traversal = packageBytes(
                version = "1.1.0",
                extraEntries = mapOf("app\\..\\..\\evil.txt" to "bad")
            )
            assertTrue(
                manager.stagePackage("1.1.0", Sha256.hex(traversal), traversal).isFailure
            )
            assertFalse(File(dataDir.parentFile, "evil.txt").exists())

            val absolute = packageBytes(
                version = "1.1.0",
                extraEntries = mapOf("/evil.txt" to "bad")
            )
            assertTrue(
                manager.stagePackage("1.1.0", Sha256.hex(absolute), absolute).isFailure
            )
            assertFalse(File(dataDir.parentFile, "evil.txt").exists())

            val outsideApp = packageBytes(
                version = "1.1.0",
                extraEntries = mapOf("other/evil.txt" to "bad")
            )
            assertTrue(
                manager.stagePackage("1.1.0", Sha256.hex(outsideApp), outsideApp).isFailure
            )
        }
    }

    @Test
    fun requiresManifestAndMatchingVersion() = runBlocking {
        withTempInstall { installDir, dataDir ->
            val manager = manager(dataDir, installDir)
            val noManifest = packageBytes(version = "1.1.0", includeManifest = false)
            assertTrue(
                manager.stagePackage("1.1.0", Sha256.hex(noManifest), noManifest).isFailure
            )

            val mismatch = packageBytes(version = "1.2.0")
            assertTrue(
                manager.stagePackage("1.1.0", Sha256.hex(mismatch), mismatch).isFailure
            )
        }
    }

    @Test
    fun remembersSkippedOptionalVersion() = runBlocking {
        withTempInstall { installDir, dataDir ->
            val manager = manager(dataDir, installDir)
            val bytes = packageBytes(version = "1.1.0")
            assertTrue(manager.stagePackage("1.1.0", Sha256.hex(bytes), bytes).isSuccess)

            manager.declineUpdate()

            assertNull(manager.state.value.version)
            assertEquals("1.1.0", manager.skippedVersion)
            assertTrue(File(dataDir, "skipped-update.txt").isFile)

            val restarted = manager(dataDir, installDir)
            assertEquals("1.1.0", restarted.skippedVersion)
        }
    }

    @Test
    fun packagedPayloadCanBeStagedByADifferentMajorScreen() = runBlocking {
        withTempInstall { installDir, dataDir ->
            val provider = LocalUpdatePackageProvider(
                dataDir = dataDir,
                logger = NoOpLogger(),
                appDir = installDir
            )
            val update = provider.getPackage().getOrThrow()
            assertTrue(update.bytes.isNotEmpty())
            assertEquals(64, update.sha256.length)

            val receiver = FileReceiverUpdateManager(
                dataDir = dataDir,
                logger = NoOpLogger(),
                currentVersion = "0.9.0",
                appDir = installDir
            )
            val forced = receiver.stagePackage(
                update.version,
                update.sha256,
                update.bytes
            ).getOrThrow()

            assertTrue(forced)
            assertTrue(receiver.state.value.forced)
            assertTrue(
                File(dataDir, "updates/staged-${update.version}/app/app.jar").isFile
            )
        }
    }

    private fun manager(dataDir: File, installDir: File) = FileReceiverUpdateManager(
        dataDir = dataDir,
        logger = NoOpLogger(),
        currentVersion = "1.0.0",
        appDir = installDir
    )

    private suspend fun withTempInstall(block: suspend (installDir: File, dataDir: File) -> Unit) {
        val root = createTempDirectory("override-update-test").toFile()
        try {
            val installDir = File(root, "install/app").apply { mkdirs() }
            File(installDir, "OverrideKiosk.cfg").writeText("mainClass=app.kiosksync.MainKt")
            File(installDir, "app.jar").writeText("old payload")
            val dataDir = File(root, "data").apply { mkdirs() }
            block(installDir, dataDir)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun packageBytes(
        version: String,
        includeManifest: Boolean = true,
        extraEntries: Map<String, String> = emptyMap()
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            if (includeManifest) {
                zip.putNextEntry(ZipEntry("update.json"))
                zip.write("""{"version":"$version","format":1}""".toByteArray())
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("app/"))
            zip.closeEntry()
            writeEntry(zip, "app/OverrideKiosk.cfg", "mainClass=app.kiosksync.MainKt")
            writeEntry(zip, "app/app.jar", "new payload $version")
            extraEntries.forEach { (name, content) -> writeEntry(zip, name, content) }
        }
        return output.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}
