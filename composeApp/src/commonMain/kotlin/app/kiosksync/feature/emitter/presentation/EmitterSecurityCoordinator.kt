package app.kiosksync.feature.emitter.presentation

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.MAX_RECOVERY_PHRASE_LENGTH
import app.kiosksync.core.Strings
import app.kiosksync.core.isValidKioskPassword
import app.kiosksync.core.isValidRecoveryPhrase
import app.kiosksync.core.isValidUnlockSequence
import app.kiosksync.feature.kiosk.KioskLockManager
import app.kiosksync.feature.playlist.domain.Playlist
import app.kiosksync.platform.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Owns controller-side kiosk password and recovery configuration workflows. */
internal class EmitterSecurityCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val storage: LocalStorage,
    private val logger: AppLogger,
    private val kioskLockManager: KioskLockManager,
    private val passwordHasher: (String) -> String,
    private val pushLockToAllPaired: (KioskLockConfig) -> Unit,
    private val onPasswordConfirmed: (KioskPasswordDialogUi, KioskLockConfig) -> Unit,
) {
    fun openSecurityDialog() {
        val config = storage.loadKioskLock()
        uiState.update {
            it.copy(
                securityDialog = SecurityDialogUi(
                    sequence = config?.unlockSequence
                        ?.takeIf(::isValidUnlockSequence)
                        ?: DEFAULT_UNLOCK_SEQUENCE,
                    hasCustomRecovery = config?.recoveryHash != null,
                ),
            )
        }
    }

    fun openChangePasswordDialog() {
        uiState.update { it.copy(securityDialog = null) }
        openPasswordDialog(EmitterPendingAction.CONFIGURE, null)
    }

    fun openPasswordDialog(
        action: EmitterPendingAction,
        playlist: Playlist?,
        deviceId: String? = null,
    ) {
        uiState.update {
            it.copy(
                passwordDialog = KioskPasswordDialogUi(
                    action = action,
                    playlist = playlist,
                    deviceId = deviceId,
                ),
            )
        }
    }

    fun saveUnlockSequence() {
        val dialog = uiState.value.securityDialog ?: return
        if (!isValidUnlockSequence(dialog.sequence)) {
            uiState.update {
                it.copy(
                    securityDialog = dialog.copy(sequenceError = Strings.SECURITY_SEQUENCE_INVALID),
                )
            }
            return
        }

        val previous = storage.loadKioskLock()
        val config = (previous ?: KioskLockConfig()).copy(
            enabled = true,
            unlockSequence = dialog.sequence,
        )
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        uiState.update {
            it.copy(
                hasKioskPassword = config.passwordHash != null,
                securityDialog = it.securityDialog?.copy(sequenceError = null),
                statusMessage = Strings.SECURITY_SEQUENCE_SAVED,
                statusIsError = false,
            )
        }
        logger.info("Kiosk unlock sequence updated (${config.unlockSequence.size} keys)")
        // Do not overwrite a screen's password with an unprotected lock.
        if (config.passwordHash != null) pushLockToAllPaired(config)
    }

    fun saveRecoveryPhrase() {
        val dialog = uiState.value.securityDialog ?: return
        if (!dialog.recoveryAccepted) {
            updateRecoveryError(dialog, Strings.SECURITY_RECOVERY_MUST_ACCEPT)
            return
        }

        val phrase = dialog.recoveryDraft
        if (!isValidRecoveryPhrase(phrase)) {
            updateRecoveryError(dialog, Strings.SECURITY_RECOVERY_INVALID)
            return
        }
        if (phrase != dialog.recoveryConfirmDraft) {
            updateRecoveryError(dialog, Strings.SECURITY_RECOVERY_MISMATCH)
            return
        }

        val previous = storage.loadKioskLock() ?: KioskLockConfig()
        val config = previous.copy(
            enabled = true,
            recoveryHash = passwordHasher(phrase),
            recoveryLength = phrase.length,
        )
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        uiState.update {
            it.copy(
                hasKioskPassword = config.passwordHash != null,
                securityDialog = it.securityDialog?.copy(
                    hasCustomRecovery = true,
                    showRecoveryEditor = false,
                    recoveryAccepted = false,
                    recoveryDraft = "",
                    recoveryConfirmDraft = "",
                    recoveryError = null,
                ),
                statusMessage = Strings.SECURITY_RECOVERY_SAVED,
                statusIsError = false,
            )
        }
        logger.info("Custom kiosk recovery phrase configured")
        if (config.passwordHash != null) pushLockToAllPaired(config)
    }

    fun resetRecoveryPhrase() {
        val previous = storage.loadKioskLock() ?: return
        val config = previous.copy(recoveryHash = null, recoveryLength = 0)
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        uiState.update {
            it.copy(
                securityDialog = it.securityDialog?.copy(
                    hasCustomRecovery = false,
                    showRecoveryEditor = false,
                    recoveryAccepted = false,
                    recoveryDraft = "",
                    recoveryConfirmDraft = "",
                    recoveryError = null,
                ),
                statusMessage = Strings.SECURITY_RECOVERY_RESET_DONE,
                statusIsError = false,
            )
        }
        logger.info("Kiosk recovery phrase restored to the universal default")
        if (config.passwordHash != null) pushLockToAllPaired(config)
    }

    fun confirmPassword() {
        val dialog = uiState.value.passwordDialog ?: return
        val password = dialog.draft
        if (!isValidKioskPassword(password)) {
            uiState.update {
                it.copy(passwordDialog = dialog.copy(error = Strings.KIOSK_PASSWORD_INVALID))
            }
            return
        }
        if (password != dialog.confirmDraft) {
            uiState.update {
                it.copy(passwordDialog = dialog.copy(error = Strings.KIOSK_PASSWORD_MISMATCH))
            }
            return
        }

        val previous = storage.loadKioskLock()
        val config = KioskLockConfig(
            passwordHash = passwordHasher(password),
            passwordLength = password.length,
            unlockSequence = previous?.unlockSequence
                ?.takeIf(::isValidUnlockSequence)
                ?: DEFAULT_UNLOCK_SEQUENCE,
            recoveryHash = previous?.recoveryHash,
            recoveryLength = previous?.recoveryLength ?: 0,
        )
        storage.saveKioskLock(config)
        kioskLockManager.setConfig(config)
        uiState.update {
            it.copy(
                passwordDialog = null,
                securityDialog = null,
                hasKioskPassword = true,
                statusMessage = Strings.KIOSK_PASSWORD_SAVED,
                statusIsError = false,
            )
        }
        logger.info("Kiosk password configured from the controller")
        onPasswordConfirmed(dialog, config)
    }

    private fun updateRecoveryError(dialog: SecurityDialogUi, error: String) {
        uiState.update { it.copy(securityDialog = dialog.copy(recoveryError = error)) }
    }
}
