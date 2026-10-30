package app.kiosksync.core.security

import app.kiosksync.core.AppLogger

object SecretProtectorFactory {
    fun create(logger: AppLogger, osName: String = System.getProperty("os.name").orEmpty()): SecretProtector =
        if (osName.startsWith("Windows", ignoreCase = true)) {
            WindowsDpapiSecretProtector()
        } else {
            logger.warn("Using the development-only local secret protector outside Windows")
            DevelopmentSecretProtector
        }
}
