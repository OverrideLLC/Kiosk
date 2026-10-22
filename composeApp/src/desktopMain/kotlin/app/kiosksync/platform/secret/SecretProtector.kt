package app.kiosksync.platform.secret

interface SecretProtector {
    fun protect(plainText: String): String
    fun unprotect(cipherText: String): String
}
