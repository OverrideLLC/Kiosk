package app.kiosksync.feature.media.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.kiosksync.core.AppLogger
import app.kiosksync.core.ImageFormat
import app.kiosksync.core.MAX_IMAGE_SIZE_BYTES
import app.kiosksync.feature.media.domain.MediaImportError
import app.kiosksync.feature.media.domain.MediaImportResult
import app.kiosksync.feature.media.domain.StoredMedia
import app.kiosksync.core.security.Sha256
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import org.jetbrains.skia.Image

/** Owns image bytes on disk; Room stores only their metadata. */
class FileMediaStore(
    private val dataDir: File,
    private val logger: AppLogger
) {
    private val mediaDir = File(dataDir, "media").apply { mkdirs() }

    fun loadBitmap(mediaId: String, fileName: String): ImageBitmap? {
        val file = findMediaFile(mediaId, fileName)
        if (file == null || file.length() > MAX_IMAGE_SIZE_BYTES) return null
        return try {
            Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
        } catch (error: Exception) {
            logger.error("Could not decode media '${file.name}'", error)
            null
        }
    }

    fun importMedia(sourcePath: String): MediaImportResult {
        val source = File(sourcePath)
        if (!source.isFile || !source.canRead()) return MediaImportResult.Failure(MediaImportError.UNREADABLE)
        if (source.length() > MAX_IMAGE_SIZE_BYTES) return MediaImportResult.Failure(MediaImportError.TOO_LARGE)
        val bytes = try {
            source.readBytes()
        } catch (error: Exception) {
            logger.error("Could not read selected media '$sourcePath'", error)
            return MediaImportResult.Failure(MediaImportError.UNREADABLE)
        }
        if (bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) return MediaImportResult.Failure(MediaImportError.TOO_LARGE)
        val format = ImageFormat.detect(bytes) ?: return MediaImportResult.Failure(MediaImportError.UNSUPPORTED_FORMAT)
        val mediaId = UUID.randomUUID().toString()
        val target = writeMediaFile(mediaId, format, bytes)
            ?: return MediaImportResult.Failure(MediaImportError.UNREADABLE)
        return MediaImportResult.Success(
            StoredMedia(mediaId, source.name, Sha256.hex(bytes), target.length())
        )
    }

    fun getBytes(mediaId: String, fileName: String): ByteArray? {
        val file = findMediaFile(mediaId, fileName) ?: return null
        if (file.length() > MAX_IMAGE_SIZE_BYTES) return null
        return runCatching { file.readBytes() }
            .onFailure { logger.error("Could not read stored media '${file.name}'", it) }
            .getOrNull()
    }

    fun listFiles(): List<StoredMedia> = mediaDir.listFiles().orEmpty().mapNotNull { file ->
        if (!file.isFile || file.length() > MAX_IMAGE_SIZE_BYTES) return@mapNotNull null
        if (file.extension.lowercase() !in SUPPORTED_EXTENSIONS) return@mapNotNull null
        val mediaId = file.nameWithoutExtension.takeIf(String::isNotBlank) ?: return@mapNotNull null
        runCatching { StoredMedia(mediaId, file.name, Sha256.hex(file), file.length()) }
            .onFailure { logger.error("Could not hash stored media '${file.name}'", it) }
            .getOrNull()
    }

    fun contains(mediaId: String): Boolean = mediaDir.listFiles().orEmpty().any {
        it.isFile && it.nameWithoutExtension == mediaId
    }

    fun writeReceived(mediaId: String, format: ImageFormat, bytes: ByteArray): StoredMedia? {
        if (!MEDIA_ID_REGEX.matches(mediaId) || bytes.size.toLong() > MAX_IMAGE_SIZE_BYTES) return null
        val file = writeMediaFile(mediaId, format, bytes) ?: return null
        return StoredMedia(mediaId, file.name, Sha256.hex(bytes), file.length())
    }

    fun delete(mediaId: String): Boolean {
        var deleted = false
        mediaDir.listFiles().orEmpty()
            .filter { it.isFile && it.nameWithoutExtension == mediaId }
            .forEach { file ->
                if (file.delete()) deleted = true
                else logger.warn("Could not delete media '${file.name}'")
            }
        return deleted
    }

    fun clear() {
        mediaDir.listFiles().orEmpty().filter(File::isFile).forEach { file ->
            if (!file.delete()) logger.warn("Could not delete media '${file.name}' during screen reset")
        }
    }

    private fun findMediaFile(mediaId: String, fileName: String): File? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val safeFileName = fileName.takeIf {
            it.isNotBlank() && it != "." && it != ".." &&
                it.none { char -> char in "/\\:*?\"<>|" || char.code < 32 }
        }
        val candidates = buildList {
            if (extension in SUPPORTED_EXTENSIONS) add(File(mediaDir, "$mediaId.$extension"))
            if (safeFileName != null) add(File(mediaDir, safeFileName))
            add(File(mediaDir, mediaId))
        }
        return candidates.firstOrNull(File::isFile)
            ?: mediaDir.listFiles().orEmpty().firstOrNull { it.isFile && it.nameWithoutExtension == mediaId }
    }

    private fun writeMediaFile(mediaId: String, format: ImageFormat, bytes: ByteArray): File? {
        if (!MEDIA_ID_REGEX.matches(mediaId)) return null
        val target = File(mediaDir, "$mediaId.${format.extension}")
        val temp = File(mediaDir, ".$mediaId.${UUID.randomUUID()}.part")
        return try {
            temp.writeBytes(bytes)
            try {
                Files.move(
                    temp.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            target
        } catch (error: Exception) {
            logger.error("Could not write media '$mediaId'", error)
            temp.delete()
            null
        }
    }

    private companion object {
        val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        val MEDIA_ID_REGEX = Regex("^[A-Za-z0-9_-]{1,100}$")
    }
}
