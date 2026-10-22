package app.kiosksync.platform

import app.kiosksync.core.APP_NAME
import app.kiosksync.core.APP_PAYLOAD_DIR_NAME
import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.UPDATE_PACKAGE_FORMAT
import app.kiosksync.core.UpdatePackageManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Packages the controller's own `app/` payload once and caches it in the data
 * folder. The bundled runtime and the launcher exe are not included: updating
 * the application payload is enough and keeps the transfer small.
 */
class LocalUpdatePackageProvider(
    private val dataDir: File,
    private val logger: AppLogger,
    private val appDir: File? = ApplicationInfo.currentAppDir()
) : UpdatePackageProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val buildMutex = Mutex()

    @Volatile
    private var cachedPackage: UpdatePackage? = null

    override fun isAvailable(): Boolean = appDir?.isDirectory == true

    override suspend fun getPackage(): Result<UpdatePackage> = buildMutex.withLock {
        cachedPackage?.let { return@withLock Result.success(it) }
        withContext(Dispatchers.IO) {
            runCatching { buildPackage() }
                .onSuccess { update ->
                    cachedPackage = update
                    logger.info(
                        "Update package ready: ${update.version} " +
                            "(${update.bytes.size} bytes, sha256=${update.sha256})"
                    )
                }
                .onFailure {
                    logger.warn("Could not build the update package: ${it.message}")
                }
        }
    }

    private fun buildPackage(): UpdatePackage {
        val source = appDir?.takeIf { it.isDirectory }
            ?: error("This build does not have a packaged app/ payload")
        val updatesDir = File(dataDir, "updates").apply { mkdirs() }
        val cacheFile = File(
            updatesDir,
            "$APP_NAME-${APP_VERSION}-${source.lastModified()}-$APP_PAYLOAD_DIR_NAME.zip"
                .replace(' ', '-')
        )

        if (!isValidCache(cacheFile)) {
            updatesDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith("-$APP_PAYLOAD_DIR_NAME.zip") }
                ?.forEach { old -> old.delete() }
            val tempFile = File(updatesDir, "${cacheFile.name}.tmp")
            if (tempFile.exists()) tempFile.delete()
            writePackage(source, tempFile)
            runCatching {
                Files.move(
                    tempFile.toPath(),
                    cacheFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            }.onFailure {
                Files.move(
                    tempFile.toPath(),
                    cacheFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        val hash = Sha256.hex(cacheFile)
        return UpdatePackage(
            version = APP_VERSION,
            sha256 = hash,
            bytes = cacheFile.readBytes()
        )
    }

    private fun writePackage(source: File, output: File) {
        val manifest = json.encodeToString(
            UpdatePackageManifest.serializer(),
            UpdatePackageManifest(version = APP_VERSION, format = UPDATE_PACKAGE_FORMAT)
        )
        ZipOutputStream(BufferedOutputStream(output.outputStream())).use { zip ->
            // Jars are already compressed. Fast compression keeps the package
            // creation quick without inflating the transfer too much.
            zip.setLevel(Deflater.BEST_SPEED)
            zip.putNextEntry(ZipEntry("update.json"))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            source.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val relative = file.relativeTo(source).invariantSeparatorsPath
                    val entry = ZipEntry("$APP_PAYLOAD_DIR_NAME/$relative")
                    entry.time = file.lastModified()
                    zip.putNextEntry(entry)
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
        }
        logger.info("Created update package ${output.name} (${output.length()} bytes)")
    }

    private fun isValidCache(file: File): Boolean {
        if (!file.isFile || file.length() == 0L) return false
        return runCatching {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("update.json") ?: return@use false
                val text = zip.getInputStream(entry)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val manifest = json.decodeFromString<UpdatePackageManifest>(text)
                val entries = zip.entries()
                var hasPayload = false
                while (entries.hasMoreElements()) {
                    val next = entries.nextElement()
                    if (!next.isDirectory && next.name.startsWith("$APP_PAYLOAD_DIR_NAME/")) {
                        hasPayload = true
                        break
                    }
                }
                manifest.version == APP_VERSION &&
                    manifest.format == UPDATE_PACKAGE_FORMAT &&
                    hasPayload
            }
        }.getOrDefault(false)
    }
}
