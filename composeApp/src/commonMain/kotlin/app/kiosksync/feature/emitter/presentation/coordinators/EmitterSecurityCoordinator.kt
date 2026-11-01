package app.kiosksync.feature.emitter.presentation.coordinators

import app.kiosksync.feature.emitter.domain.models.EmitterPendingAction
import app.kiosksync.feature.emitter.presentation.EmitterState
import app.kiosksync.feature.emitter.domain.models.KioskPasswordDialogUi
import app.kiosksync.feature.emitter.domain.models.SecurityDialogUi

import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.common.kiosk.DEFAULT_UNLOCK_SEQUENCE
import app.kiosksync.core.common.kiosk.KioskLockConfig
import app.kiosksync.core.common.kiosk.isValidKioskPassword
import app.kiosksync.core.common.kiosk.isValidRecoveryPhrase
import app.kiosksync.core.common.kiosk.isValidUnlockSequence
import app.kiosksync.feature.kiosk.domain.KioskLockConfigurationUseCases
import app.kiosksync.feature.playlist.domain.Playlist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.kiosksync.resources.Res
import app.kiosksync.resources.kiosk_password_invalid
import app.kiosksync.resources.kiosk_password_mismatch
import app.kiosksync.resources.kiosk_password_saved
import app.kiosksync.resources.security_recovery_invalid
import app.kiosksync.resources.security_recovery_mismatch
import app.kiosksync.resources.security_recovery_must_accept
import app.kiosksync.resources.security_recovery_reset_done
import app.kiosksync.resources.security_recovery_saved
import app.kiosksync.resources.security_sequence_invalid
import app.kiosksync.resources.security_sequence_saved
import app.kiosksync.resources.security_storage_error
import app.kiosksync.core.ui.UiText

/** Owns controller-side kiosk password and recovery configuration workflows. */
internal class EmitterSecurityCoordinator(
    private val uiState: MutableStateFlow<EmitterState>,
    private val loadConfiguration: suspend () -> KioskLockConfig?,
    private val scope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: AppLogger,
    private val configurationUseCases: KioskLockConfigurationUseCases,
    private val pushLockToAllPaired: (KioskLockConfig) -> Unit,
    private val onPasswordConfirmed: (KioskPasswordDialogUi, KioskLockConfig) -> Unit,
) {
    fun openSecurityDialog() {
        launchStorage {
            val config = loadConfiguration()
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
                it.copy(securityDialog = dialog.copy(sequenceError = UiText(Res.string.security_sequence_invalid)))
            }
            return
        }
        launchStorage {
            val config = configurationUseCases.saveUnlockSequence(dialog.sequence)
            uiState.update {
                it.copy(
                    hasKioskPassword = config.passwordHash != null,
                    securityDialog = it.securityDialog?.copy(sequenceError = null),
                    statusMessage = UiText(Res.string.security_sequence_saved),
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
            updateRecoveryError(dialog, UiText(Res.string.security_recovery_must_accept))
            return
        }
        val phrase = dialog.recoveryDraft
        if (!isValidRecoveryPhrase(phrase)) {
            updateRecoveryError(dialog, UiText(Res.string.security_recovery_invalid))
            return
        }
        if (phrase != dialog.recoveryConfirmDraft) {
            updateRecoveryError(dialog, UiText(Res.string.security_recovery_mismatch))
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
                    statusMessage = UiText(Res.string.security_recovery_saved),
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
                    statusMessage = UiText(Res.string.security_recovery_reset_done),
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
                it.copy(passwordDialog = dialog.copy(error = UiText(Res.string.kiosk_password_invalid)))
            }
            return
        }
        if (password != dialog.confirmDraft) {
            uiState.update {
                it.copy(passwordDialog = dialog.copy(error = UiText(Res.string.kiosk_password_mismatch)))
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
                    statusMessage = UiText(Res.string.kiosk_password_saved),
                    statusIsError = false,
                )
            }
            logger.info("Kiosk password configured from the controller")
            onPasswordConfirmed(dialog, config)
        }
    }

    private fun updateRecoveryError(dialog: SecurityDialogUi, error: UiText) {
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
                    it.copy(statusMessage = UiText(Res.string.security_storage_error), statusIsError = true)
                }
            }
        }
    }
}
