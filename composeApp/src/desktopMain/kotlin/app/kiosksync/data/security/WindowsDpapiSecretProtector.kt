package app.kiosksync.data.security

import com.sun.jna.platform.win32.Crypt32Util
import java.util.Base64

class WindowsDpapiSecretProtector : SecretProtector {
    override fun protect(plainText: String): String =
        DPAPI_PREFIX + Base64.getEncoder().encodeToString(
            Crypt32Util.cryptProtectData(plainText.toByteArray(Charsets.UTF_8))
        )

    override fun unprotect(cipherText: String): String {
        require(cipherText.startsWith(DPAPI_PREFIX)) { "Unsupported protected secret format" }
        val encrypted = Base64.getDecoder().decode(cipherText.removePrefix(DPAPI_PREFIX))
        return String(Crypt32Util.cryptUnprotectData(encrypted), Charsets.UTF_8)
    }

    private companion object {
        const val DPAPI_PREFIX = "dpapi:v1:"
    }
}
