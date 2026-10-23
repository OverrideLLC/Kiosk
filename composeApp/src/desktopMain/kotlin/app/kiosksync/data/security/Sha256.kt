package app.kiosksync.data.security

import java.io.File
import java.security.MessageDigest

object Sha256 {
    fun hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).toHex()
    }

    fun hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String {
        val output = StringBuilder(size * 2)
        for (byte in this) {
            output.append(HEX[(byte.toInt() ushr 4) and 0x0F])
            output.append(HEX[byte.toInt() and 0x0F])
        }
        return output.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
