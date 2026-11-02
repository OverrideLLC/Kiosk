package app.kiosksync.core.security

import java.util.Base64

/** Encoding-only test/development adapter; never use it for packaged Windows installs. */
object DevelopmentSecretProtector : SecretProtector {
    override fun protect(plainText: String): String =
        PREFIX + Base64.getEncoder().encodeToString(plainText.toByteArray(Charsets.UTF_8))

    override fun unprotect(cipherText: String): String {
        require(cipherText.startsWith(PREFIX)) { "Unsupported protected secret format" }
        return String(Base64.getDecoder().decode(cipherText.removePrefix(PREFIX)), Charsets.UTF_8)
    }

    private const val PREFIX = "dev-only:v1:"
}
