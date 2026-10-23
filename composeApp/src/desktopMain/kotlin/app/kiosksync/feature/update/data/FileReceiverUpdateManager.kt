package app.kiosksync.feature.update.data

import app.kiosksync.core.APP_EXECUTABLE_NAME
import app.kiosksync.core.APP_PAYLOAD_DIR_NAME
import app.kiosksync.core.APP_VERSION
import app.kiosksync.core.AppLogger
import app.kiosksync.core.AppVersionPolicy
import app.kiosksync.core.MAX_UPDATE_EXTRACTED_BYTES
import app.kiosksync.core.Strings
import app.kiosksync.core.UPDATE_PACKAGE_FORMAT
import app.kiosksync.core.UpdatePackageManifest
import app.kiosksync.core.UpdateUrgency
import app.kiosksync.core.isValidAppVersion
import app.kiosksync.data.security.Sha256
import app.kiosksync.feature.update.domain.ReceiverUpdateManager
import app.kiosksync.feature.update.domain.ReceiverUpdateUi
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Validates, stages and applies a full application-payload update.
 *
 * The running jars are locked by Windows, so the replacement is delegated to a
 * detached PowerShell helper that waits for this process to exit and then uses
 * robocopy to mirror the staged `app/` folder over the installed one.
 */
class FileReceiverUpdateManager(
    private val dataDir: File,
    private val logger: AppLogger,
    private val currentVersion: String = APP_VERSION,
    private val appDir: File? = ApplicationInfo.currentAppDir()
) : ReceiverUpdateManager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val updatesDir = File(dataDir, "updates").apply { mkdirs() }
    private val skippedFile = File(dataDir, "skipped-update.txt")
    private val restartMarker = File(dataDir, "update-restart.marker")

    private val _uiState = MutableStateFlow(ReceiverUpdateUi(currentVersion = currentVersion))
    override val uiState: StateFlow<ReceiverUpdateUi> = _uiState.asStateFlow()

    @Volatile
    private var skipped: String? = loadSkippedVersion()

    override val skippedVersion: String? get() = skipped

    @Volatile
    private var pendingStageDir: File? = null

    @Volatile
    private var pendingVersion: String? = null

    @Volatile
    private var pendingForced: Boolean = false

    override suspend fun stagePackage(
        version: String,
        sha256: String,
        bytes: ByteArray
    ): Result<Boolean> {
        if (!isValidAppVersion(version)) {
            return Result.failure(IllegalArgumentException("invalid_version"))
        }
        val targetAppDir = appDir
        if (targetAppDir == null || !targetAppDir.isDirectory) {
            return Result.failure(IllegalStateException(Strings.UPDATE_NOT_SUPPORTED))
        }
        // The manager's current version is the "screen" side of the policy;
        // the incoming package is the controller side.
        val plan = AppVersionPolicy.plan(version, currentVersion)
        if (plan.urgency == UpdateUrgency.NONE) {
            return Result.failure(IllegalStateException("version_is_not_newer"))
        }
        if (bytes.size.toLong() > MAX_UPDATE_PACKAGE_BYTES_LOCAL) {
            return Result.failure(IllegalArgumentException("package_too_large"))
        }
        val actualHash = Sha256.hex(bytes)
        if (!actualHash.equals(sha256, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("hash_mismatch"))
        }

        return withContext(Dispatchers.IO) {
            val tempZip = File(updatesDir, "incoming-${System.nanoTime()}.zip")
            val stageRoot = File(updatesDir, "staged-$version")
            try {
                updatesDir.mkdirs()
                if (stageRoot.exists()) stageRoot.deleteRecursively()
                stageRoot.mkdirs()
                tempZip.writeBytes(bytes)
                extractPackage(tempZip, stageRoot, version)

                updatesDir.listFiles()
                    ?.filter { it != stageRoot && it.name.startsWith("staged-") }
                    ?.forEach { it.deleteRecursively() }

                pendingStageDir = stageRoot
                pendingVersion = version
                pendingForced = plan.urgency == UpdateUrgency.FORCED
                _uiState.value = ReceiverUpdateUi(
                    version = version,
                    currentVersion = currentVersion,
                    forced = pendingForced
                )
                logger.info(
                    "Update $version staged at ${stageRoot.absolutePath} " +
                        "(forced=$pendingForced)"
                )
                Result.success(pendingForced)
            } catch (error: Exception) {
                logger.warn("Could not stage update $version", error)
                runCatching { stageRoot.deleteRecursively() }
                Result.failure(error)
            } finally {
                runCatching { tempZip.delete() }
            }
        }
    }

    override fun acceptUpdate(): Boolean {
        val state = _uiState.value
        val version = state.version ?: return false
        if (state.applying) return false

        val stageRoot = pendingStageDir
        val installDir = appDir?.takeIf { it.isDirectory }
        val executable = installDir?.parentFile?.resolve(APP_EXECUTABLE_NAME)
            ?.takeIf { it.isFile }
        if (stageRoot == null || installDir == null || executable == null) {
            logger.warn("Cannot apply update $version: packaged installation not found")
            _uiState.update { it.copy(error = Strings.UPDATE_NOT_SUPPORTED) }
            return false
        }

        return try {
            val logsDir = File(dataDir, "logs").apply { mkdirs() }
            val logFile = File(logsDir, "update.log")
            val script = File(updatesDir, "apply-update-$version.ps1")
            if (script.exists()) script.delete()

            restartMarker.writeText(version, Charsets.UTF_8)
            script.writeText(
                "\uFEFF" + buildPowerShellUpdater(
                    processId = ProcessHandle.current().pid(),
                    stageRoot = stageRoot,
                    installDir = installDir,
                    executable = executable,
                    marker = restartMarker,
                    logFile = logFile
                ),
                Charsets.UTF_8
            )

            val process = ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-WindowStyle",
                "Hidden",
                "-File",
                script.absolutePath
            )
                .directory(dataDir)
                .start()

            _uiState.update { it.copy(applying = true, error = null) }
            logger.info(
                "Update helper started for $version " +
                    "(powershell pid=${process.pid()}, app pid=${ProcessHandle.current().pid()})"
            )
            true
        } catch (error: Exception) {
            logger.error("Could not start the update helper", error)
            runCatching { restartMarker.delete() }
            _uiState.update { it.copy(error = Strings.UPDATE_INSTALL_ERROR) }
            false
        }
    }

    override fun declineUpdate() {
        val state = _uiState.value
        val version = state.version ?: return
        if (state.forced) return
        skipped = version
        runCatching {
            skippedFile.writeText(version, Charsets.UTF_8)
        }.onFailure {
            logger.warn("Could not save the skipped update version", it)
        }
        deleteStagedFiles()
        _uiState.value = ReceiverUpdateUi(currentVersion = currentVersion)
        logger.info("User skipped optional update $version")
    }

    override fun dismissError() {
        _uiState.update { if (it.error != null) it.copy(error = null) else it }
    }

    private fun extractPackage(
        zipFile: File,
        stageRoot: File,
        expectedVersion: String
    ) {
        var manifest: UpdatePackageManifest? = null
        var payloadFiles = 0
        var extractedBytes = 0L
        val rootPath = stageRoot.canonicalFile.toPath().normalize()

        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val rawName = entry.name.replace('\\', '/')
                val cleanName = if (entry.isDirectory) rawName.trimEnd('/') else rawName
                if (cleanName.isBlank() || cleanName.startsWith('/') ||
                    cleanName.contains('\u0000') || cleanName.contains(':')
                ) {
                    throw IllegalArgumentException("unsafe_entry")
                }
                val parts = cleanName.split('/')
                if (parts.any { part ->
                        part.isEmpty() || part == "." || part == ".." ||
                            part.any { it in "*?\"<>|" || it.code < 32 }
                    }
                ) {
                    throw IllegalArgumentException("unsafe_entry")
                }
                val normalized = parts.joinToString("/")
                if (normalized != "update.json" &&
                    normalized != APP_PAYLOAD_DIR_NAME &&
                    !normalized.startsWith("$APP_PAYLOAD_DIR_NAME/")
                ) {
                    throw IllegalArgumentException("unexpected_entry")
                }

                val target = File(stageRoot, normalized).canonicalFile
                val targetPath = target.toPath().normalize()
                if (targetPath != rootPath && !targetPath.startsWith(rootPath)) {
                    throw IllegalArgumentException("entry_outside_stage")
                }

                if (entry.isDirectory) {
                    target.mkdirs()
                    continue
                }

                if (entry.size > MAX_UPDATE_EXTRACTED_BYTES) {
                    throw IllegalArgumentException("package_too_large")
                }
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            extractedBytes += read
                            if (extractedBytes > MAX_UPDATE_EXTRACTED_BYTES) {
                                throw IllegalArgumentException("package_too_large")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }

                if (normalized == "update.json") {
                    manifest = runCatching {
                        json.decodeFromString<UpdatePackageManifest>(
                            target.readText(Charsets.UTF_8)
                        )
                    }.getOrElse {
                        throw IllegalArgumentException("invalid_manifest", it)
                    }
                }
                if (normalized.startsWith("$APP_PAYLOAD_DIR_NAME/")) {
                    payloadFiles++
                }
            }
        }

        val parsed = manifest
            ?: throw IllegalArgumentException("missing_manifest")
        if (parsed.version != expectedVersion) {
            throw IllegalArgumentException("manifest_version_mismatch")
        }
        if (parsed.format != UPDATE_PACKAGE_FORMAT) {
            throw IllegalArgumentException("unsupported_package_format")
        }

        val payload = File(stageRoot, APP_PAYLOAD_DIR_NAME)
        if (!payload.isDirectory || payloadFiles == 0) {
            throw IllegalArgumentException("missing_app_payload")
        }
        val hasConfig = payload.listFiles()
            ?.any { it.isFile && it.extension.equals("cfg", true) } == true
        val hasJar = payload.walkTopDown().any { it.isFile && it.extension.equals("jar", true) }
        if (!hasConfig || !hasJar) {
            throw IllegalArgumentException("incomplete_app_payload")
        }
    }

    private fun deleteStagedFiles() {
        pendingStageDir = null
        pendingVersion = null
        pendingForced = false
        updatesDir.listFiles()
            ?.filter { it.name.startsWith("staged-") }
            ?.forEach { it.deleteRecursively() }
    }

    private fun loadSkippedVersion(): String? {
        if (!skippedFile.isFile) return null
        return runCatching {
            skippedFile.readText(Charsets.UTF_8).trim().takeIf { isValidAppVersion(it) }
        }.getOrNull()
    }

    private fun buildPowerShellUpdater(
        processId: Long,
        stageRoot: File,
        installDir: File,
        executable: File,
        marker: File,
        logFile: File
    ): String {
        fun quote(value: String): String = value.replace("'", "''")
        return """
            ${'$'}ErrorActionPreference = 'Continue'
            ${'$'}targetPid = $processId
            ${'$'}stage = '${quote(stageRoot.absolutePath)}'
            ${'$'}target = '${quote(installDir.absolutePath)}'
            ${'$'}exe = '${quote(executable.absolutePath)}'
            ${'$'}marker = '${quote(marker.absolutePath)}'
            ${'$'}log = '${quote(logFile.absolutePath)}'

            function Write-UpdateLog([string]${'$'}message) {
                ${'$'}line = '[{0}] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), ${'$'}message
                Add-Content -LiteralPath ${'$'}log -Value ${'$'}line -Encoding UTF8 -ErrorAction SilentlyContinue
            }

            Write-UpdateLog "Waiting for Override Kiosk process ${'$'}targetPid"
            while (Get-Process -Id ${'$'}targetPid -ErrorAction SilentlyContinue) {
                Start-Sleep -Milliseconds 400
            }
            Start-Sleep -Seconds 2

            Write-UpdateLog "Replacing application payload"
            & robocopy (Join-Path ${'$'}stage 'app') ${'$'}target /MIR /R:3 /W:2 /NP /NJH /NJS /NDL 2>&1 |
                ForEach-Object { Write-UpdateLog ([string]${'$'}_) }
            ${'$'}code = ${'$'}LASTEXITCODE

            if (${'$'}code -ge 8) {
                Write-UpdateLog "Update failed: robocopy exit code ${'$'}code"
                Remove-Item -LiteralPath ${'$'}marker -Force -ErrorAction SilentlyContinue
                try {
                    Start-Process -FilePath ${'$'}exe -WorkingDirectory (Split-Path -Parent ${'$'}exe)
                } catch {
                    Write-UpdateLog "Could not restart the application: ${'$'}(${ '$' }_.Exception.Message)"
                }
                exit 1
            }

            Write-UpdateLog "Update applied successfully"
            try {
                Start-Process -FilePath ${'$'}exe -WorkingDirectory (Split-Path -Parent ${'$'}exe)
            } catch {
                Write-UpdateLog "Could not restart the application: ${'$'}(${ '$' }_.Exception.Message)"
            }
            Remove-Item -LiteralPath ${'$'}stage -Recurse -Force -ErrorAction SilentlyContinue
            Remove-Item -LiteralPath ${'$'}MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue
            exit 0
        """.trimIndent()
    }

    private companion object {
        const val MAX_UPDATE_PACKAGE_BYTES_LOCAL = 512L * 1024 * 1024
    }
}
