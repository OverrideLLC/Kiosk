package app.kiosksync.core.security

interface SecretProtector {
    fun protect(plainText: String): String
    fun unprotect(cipherText: String): String
}
