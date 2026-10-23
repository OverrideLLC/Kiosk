package app.kiosksync.feature.emitter.presentation.viewmodel

import app.kiosksync.feature.emitter.presentation.state.EmitterPendingAction
import app.kiosksync.feature.emitter.presentation.state.EmitterUiState
import app.kiosksync.feature.emitter.presentation.state.KioskPasswordDialogUi
import app.kiosksync.feature.emitter.presentation.state.SecurityDialogUi

import app.kiosksync.core.AppLogger
import app.kiosksync.core.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.KioskLockConfig
import app.kiosksync.core.Strings
import app.kiosksync.core.isValidKioskPassword
import app.kiosksync.core.isValidRecoveryPhrase
import app.kiosksync.core.isValidUnlockSequence
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.kiosk.domain.KioskLockRepository
import app.kiosksync.feature.playlist.domain.Playlist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns controller-side kiosk password and recovery configuration workflows. */
internal class EmitterSecurityCoordinator(
    private val uiState: MutableStateFlow<EmitterUiState>,
    private val repository: KioskLockRepository,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: AppLogger,
    private val configurationUseCases: KioskLockConfigurationUseCases,
    private val pushLockToAllPaired: (KioskLockConfig) -> Unit,
    private val onPasswordConfirmed: (KioskPasswordDialogUi, KioskLockConfig) -> Unit,
) {
    fun openSecurityDialog() {
        launchStorage {
            val config = repository.load()
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
                it.copy(securityDialog = dialog.copy(sequenceError = Strings.SECURITY_SEQUENCE_INVALID))
            }
            return
        }
        launchStorage {
            val config = configurationUseCases.saveUnlockSequence(dialog.sequence)
            uiState.update {
                it.copy(
                    hasKioskPassword = config.passwordHash != null,
                    securityDialog = it.securityDialog?.copy(sequenceError = null),
                    statusMessage = Strings.SECURITY_SEQUENCE_SAVED,
                    statusIsError = false,
                )
            }
            logger.info("Kiosk unlock sequence updated (${config.unlockSequence.size} keys)")
            if (config.passwordHash != null) pushLockToAllPaired(config)
        }
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
        launchStorage {
            val config = configurationUseCases.saveRecoveryPhrase(phrase)
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
    }

    fun resetRecoveryPhrase() {
        launchStorage {
            val config = configurationUseCases.resetRecoveryPhrase() ?: return@launchStorage
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
        launchStorage {
            val config = configurationUseCases.savePassword(password)
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
    }

    private fun updateRecoveryError(dialog: SecurityDialogUi, error: String) {
        uiState.update { it.copy(securityDialog = dialog.copy(recoveryError = error)) }
    }

    private fun launchStorage(block: suspend () -> Unit) {
        scope.launch {
            try {
                withContext(backgroundDispatcher) { block() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Could not persist kiosk security configuration", error)
                uiState.update {
                    it.copy(statusMessage = Strings.SECURITY_STORAGE_ERROR, statusIsError = true)
                }
            }
        }
    }
}
