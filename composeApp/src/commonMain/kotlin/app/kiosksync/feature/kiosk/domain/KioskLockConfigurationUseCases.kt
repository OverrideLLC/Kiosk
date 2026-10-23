package app.kiosksync.feature.kiosk.domain

import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.isValidKioskPassword
import app.kiosksync.core.isValidRecoveryPhrase
import app.kiosksync.core.isValidUnlockSequence

/** Business operations for saving the Controller's kiosk lock configuration. */
class KioskLockConfigurationUseCases(
    private val repository: KioskLockRepository,
    private val manager: KioskLockManager,
    private val passwordHasher: (String) -> String,
) {
    suspend fun saveUnlockSequence(sequence: List<String>): KioskLockConfig {
        require(isValidUnlockSequence(sequence)) { "Invalid kiosk unlock sequence" }
        val config = (repository.load() ?: KioskLockConfig()).copy(
            enabled = true,
            unlockSequence = sequence,
        )
        repository.save(config)
        manager.setConfig(config)
        return config
    }

    suspend fun saveRecoveryPhrase(phrase: String): KioskLockConfig {
        require(isValidRecoveryPhrase(phrase)) { "Invalid kiosk recovery phrase" }
        val config = (repository.load() ?: KioskLockConfig()).copy(
            enabled = true,
            recoveryHash = passwordHasher(phrase),
            recoveryLength = phrase.length,
        )
        repository.save(config)
        manager.setConfig(config)
        return config
    }

    suspend fun resetRecoveryPhrase(): KioskLockConfig? {
        val previous = repository.load() ?: return null
        val config = previous.copy(recoveryHash = null, recoveryLength = 0)
        repository.save(config)
        manager.setConfig(config)
        return config
    }

    suspend fun savePassword(password: String): KioskLockConfig {
        require(isValidKioskPassword(password)) { "Invalid kiosk password" }
        val previous = repository.load()
        val config = KioskLockConfig(
            passwordHash = passwordHasher(password),
            passwordLength = password.length,
            unlockSequence = previous?.unlockSequence
                ?.takeIf(::isValidUnlockSequence)
                ?: DEFAULT_UNLOCK_SEQUENCE,
            recoveryHash = previous?.recoveryHash,
            recoveryLength = previous?.recoveryLength ?: 0,
        )
        repository.save(config)
        manager.setConfig(config)
        return config
    }
}
