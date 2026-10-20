package app.kiosksync.core

/**
 * Image formats accepted by the MVP. Detection uses the file signature, not
 * only the extension, so a renamed file cannot bypass validation.
 */
enum class ImageFormat(val extension: String) {
    JPEG("jpg"),
    PNG("png"),
    WEBP("webp");

    companion object {
        fun detect(bytes: ByteArray): ImageFormat? {
            if (bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()
            ) {
                return JPEG
            }

            val pngSignature = byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            )
            if (bytes.size >= pngSignature.size && pngSignature.indices.all { bytes[it] == pngSignature[it] }) {
                return PNG
            }

            if (bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte()
            ) {
                return WEBP
            }

            return null
        }
    }
}
