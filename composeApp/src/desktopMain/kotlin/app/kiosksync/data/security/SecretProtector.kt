package app.kiosksync.data.security

interface SecretProtector {
    fun protect(plainText: String): String
    fun unprotect(cipherText: String): String
}
