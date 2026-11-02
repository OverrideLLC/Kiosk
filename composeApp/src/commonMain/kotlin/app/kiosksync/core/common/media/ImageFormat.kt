package app.kiosksync.core.common.media

/**
 * Image formats accepted by the MVP. Detection uses the file signature, not
 * only the extension, so a renamed file cannot bypass validation.
 */
enum class ImageFormat(val extension: String) {
    JPEG("jpg"),
    PNG("png"),
    WEBP("webp");

    companion object {
        fun detect(bytes: ByteArray): ImageFormat? = when {
            bytes.startsWith(JPEG_SIGNATURE) -> JPEG
            bytes.startsWith(PNG_SIGNATURE) -> PNG
            bytes.startsWith(RIFF_SIGNATURE) && bytes.startsWith(WEBP_SIGNATURE, offset = 8) -> WEBP
            else -> null
        }
    }
}

private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val RIFF_SIGNATURE = "RIFF".encodeToByteArray()
private val WEBP_SIGNATURE = "WEBP".encodeToByteArray()

private fun ByteArray.startsWith(signature: ByteArray, offset: Int = 0): Boolean =
    size >= offset + signature.size && signature.indices.all { index -> this[offset + index] == signature[index] }
