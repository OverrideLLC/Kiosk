package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.kiosksync.app.di.rememberInjected
import app.kiosksync.app.shell.Screen
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.guide.presentation.GuideAction
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.kiosk.presentation.KioskAction
import app.kiosksync.feature.kiosk.presentation.KioskState
import app.kiosksync.feature.kiosk.presentation.KioskViewModel
import app.kiosksync.feature.pairing.presentation.PairingAction
import app.kiosksync.feature.pairing.presentation.PairingEffect
import app.kiosksync.feature.pairing.presentation.PairingUiState
import app.kiosksync.feature.pairing.presentation.PairingViewModel
import app.kiosksync.feature.playback.domain.ReceiverPlaybackSyncBridge
import app.kiosksync.feature.playback.domain.ReceiverPlaylistBridge
import app.kiosksync.feature.playback.presentation.PlaybackHost
import app.kiosksync.feature.playback.presentation.PlaybackState
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder
import app.kiosksync.feature.receiver.presentation.ReceiverOptionsDialog
import app.kiosksync.feature.receiver.presentation.ReceiverUnpairDialog
import app.kiosksync.feature.role.presentation.ChangeRoleDialog
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

internal class ReceiverDialogs {
    var showOptions by mutableStateOf(false)
    var showUnpairConfirm by mutableStateOf(false)
    var showChangeRoleConfirm by mutableStateOf(false)

    fun dismissAll() {
        showOptions = false
        showUnpairConfirm = false
        showChangeRoleConfirm = false
    }
}

@Composable
internal fun rememberReceiverPlayback(
    scope: CoroutineScope,
    screen: Screen,
): PlaybackStateHolder {
    val holder = rememberInjected<PlaybackStateHolder> {
        parametersOf(PlaybackHost(scope, trackStatus = true, loadFromStorage = true))
    }
    val playlistBridge = koinInject<ReceiverPlaylistBridge>()
    val playbackSyncBridge = koinInject<ReceiverPlaybackSyncBridge>()
    DisposableEffect(holder) {
        playlistBridge.setListener { playlist ->
            holder.updatePlaylist(playlist)
        }
        playbackSyncBridge.setListener { delayMs ->
            holder.scheduleRestart(delayMs)
        }
        onDispose {
            playlistBridge.setListener(null)
            playbackSyncBridge.setListener(null)
            holder.stop()
        }
    }
    LaunchedEffect(screen) {
        if (screen is Screen.ReceiverHome) holder.start() else holder.stop()
    }
    return holder
}

@Composable
internal fun BindReceiverPairing(pairingViewModel: PairingViewModel, screen: Screen) {
    DisposableEffect(pairingViewModel, screen) {
        val receiverConfig = (screen as? Screen.ReceiverHome)?.config
        if (receiverConfig != null) {
            pairingViewModel.onAction(PairingAction.StartReceiver(receiverConfig))
        }
        onDispose {
            if (receiverConfig != null) {
                pairingViewModel.onAction(PairingAction.StopReceiver)
            }
        }
    }
}

@Composable
internal fun BindReceiverGuide(
    guideViewModel: GuideViewModel,
    screen: Screen,
    pairing: PairingUiState,
    lock: KioskState,
    playback: PlaybackState,
) {
    LaunchedEffect(screen, pairing.pairing.isPaired, lock.hasPassword, playback.hasItems) {
        if (screen is Screen.ReceiverHome) {
            guideViewModel.onAction(
                GuideAction.ReportReceiverProgress(
                    paired = pairing.pairing.isPaired,
                    hasPassword = lock.hasPassword,
                    hasContent = playback.hasItems,
                ),
            )
        }
    }
}

@Composable
internal fun BindReceiverAdmin(
    screen: Screen,
    pairingViewModel: PairingViewModel,
    pairing: PairingUiState,
    lock: KioskState,
    updateVisible: Boolean,
    dialogs: ReceiverDialogs,
    kioskViewModel: KioskViewModel,
    guideViewModel: GuideViewModel,
    registerEscHandler: ((() -> Unit)?) -> Unit,
    registerUnpairHandler: ((() -> Unit)?) -> Unit,
) {
    pairingViewModel.updateHost { dialogs.showChangeRoleConfirm = true }
    var wasLocked by remember { mutableStateOf(lock.isLocked) }

    LaunchedEffect(pairingViewModel) {
        pairingViewModel.effects.collect { effect ->
            when (effect) {
                PairingEffect.Unpaired -> dialogs.showUnpairConfirm = false
            }
        }
    }
    LaunchedEffect(pairingViewModel, lock.isLocked) {
        pairingViewModel.onAction(PairingAction.AdminActionsChanged(!lock.isLocked))
    }
    LaunchedEffect(lock.isLocked) {
        val previous = wasLocked
        wasLocked = lock.isLocked
        if (lock.isLocked) {
            dialogs.showOptions = false
            dialogs.showUnpairConfirm = false
            if (screen is Screen.ReceiverHome) guideViewModel.onAction(GuideAction.Stop)
        } else if (previous && screen is Screen.ReceiverHome) {
            dialogs.showOptions = true
        }
    }
    LaunchedEffect(updateVisible) {
        if (updateVisible) {
            dialogs.showOptions = false
            dialogs.showUnpairConfirm = false
            relockReceiver(screen, pairing, lock, kioskViewModel)
        }
    }
    LaunchedEffect(screen, pairing.pairing.isPaired) {
        if (screen is Screen.ReceiverHome) {
            registerEscHandler {
                if (kioskViewModel.state.value.isLocked) return@registerEscHandler
                dialogs.showOptions = true
            }
        } else {
            registerEscHandler(null)
        }
    }
    LaunchedEffect(screen, pairing.pairing.isPaired) {
        if (screen is Screen.ReceiverHome && pairing.pairing.isPaired) {
            registerUnpairHandler { dialogs.showUnpairConfirm = true }
        } else {
            registerUnpairHandler(null)
        }
    }
}

@Composable
internal fun ReceiverAdminDialogs(
    screen: Screen,
    pairing: PairingUiState,
    lock: KioskState,
    dialogs: ReceiverDialogs,
    roleResetting: Boolean,
    roleResetFailed: Boolean,
    pairingViewModel: PairingViewModel,
    kioskViewModel: KioskViewModel,
    guideViewModel: GuideViewModel,
    onExitRequested: () -> Unit,
    onResetRole: () -> Unit,
) {
    fun closeOptions() {
        dialogs.showOptions = false
        relockReceiver(screen, pairing, lock, kioskViewModel)
    }

    if (dialogs.showOptions) {
        ReceiverOptionsDialog(
            isReceiver = screen is Screen.ReceiverHome,
            onOpenGuide = {
                dialogs.showOptions = false
                guideViewModel.onAction(GuideAction.Start(AppRole.RECEIVER))
            },
            onDismiss = { closeOptions() },
            onChangeRole = {
                dialogs.showOptions = false
                dialogs.showChangeRoleConfirm = true
            },
            onExit = {
                dialogs.showOptions = false
                onExitRequested()
            },
            onRelock = { closeOptions() },
        )
    }
    if (dialogs.showUnpairConfirm) {
        ReceiverUnpairDialog(
            onDismiss = {
                dialogs.showUnpairConfirm = false
                relockReceiver(screen, pairing, lock, kioskViewModel)
            },
            onConfirm = { pairingViewModel.onAction(PairingAction.Unpair) },
        )
    }
    if (dialogs.showChangeRoleConfirm) {
        ChangeRoleDialog(
            isResetting = roleResetting,
            hasError = roleResetFailed,
            onDismiss = {
                if (!roleResetting) {
                    dialogs.showChangeRoleConfirm = false
                    relockReceiver(screen, pairing, lock, kioskViewModel)
                }
            },
            onConfirm = onResetRole,
        )
    }
}

internal fun relockReceiver(
    screen: Screen,
    pairing: PairingUiState,
    lock: KioskState,
    kioskViewModel: KioskViewModel,
) {
    if (screen is Screen.ReceiverHome && pairing.pairing.isPaired && lock.hasPassword) {
        kioskViewModel.onAction(KioskAction.Lock(force = true))
    }
}
