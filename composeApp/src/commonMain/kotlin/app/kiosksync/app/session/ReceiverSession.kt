package app.kiosksync.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.kiosksync.app.navigation.routes.AppRoute
import app.kiosksync.app.navigation.session.sessionViewModel
import app.kiosksync.core.common.app.AppRole
import app.kiosksync.feature.role.domain.AppConfig
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
import app.kiosksync.feature.playback.presentation.PlaybackState
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder
import app.kiosksync.feature.receiver.presentation.ReceiverOptionsDialog
import app.kiosksync.feature.receiver.presentation.ReceiverUnpairDialog
import app.kiosksync.feature.role.presentation.ChangeRoleDialog
import org.koin.compose.koinInject

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
    route: AppRoute,
): PlaybackStateHolder {
    val holder = sessionViewModel<PlaybackStateHolder>()
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
    LaunchedEffect(route) {
        if (route is AppRoute.ReceiverHome) holder.start() else holder.stop()
    }
    return holder
}

@Composable
internal fun BindReceiverPairing(
    pairingViewModel: PairingViewModel,
    route: AppRoute,
    config: AppConfig?,
) {
    DisposableEffect(pairingViewModel, route) {
        val receiverConfig = if (route is AppRoute.ReceiverHome) config else null
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
    route: AppRoute,
    pairing: PairingUiState,
    lock: KioskState,
    playback: PlaybackState,
) {
    LaunchedEffect(route, pairing.pairing.isPaired, lock.hasPassword, playback.hasItems) {
        if (route is AppRoute.ReceiverHome) {
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
    route: AppRoute,
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
            if (route is AppRoute.ReceiverHome) guideViewModel.onAction(GuideAction.Stop)
        } else if (previous && route is AppRoute.ReceiverHome) {
            dialogs.showOptions = true
        }
    }
    LaunchedEffect(updateVisible) {
        if (updateVisible) {
            dialogs.showOptions = false
            dialogs.showUnpairConfirm = false
            relockReceiver(route, pairing, lock, kioskViewModel)
        }
    }
    LaunchedEffect(route, pairing.pairing.isPaired) {
        if (route is AppRoute.ReceiverHome) {
            registerEscHandler {
                if (kioskViewModel.state.value.isLocked) return@registerEscHandler
                dialogs.showOptions = true
            }
        } else {
            registerEscHandler(null)
        }
    }
    LaunchedEffect(route, pairing.pairing.isPaired) {
        if (route is AppRoute.ReceiverHome && pairing.pairing.isPaired) {
            registerUnpairHandler { dialogs.showUnpairConfirm = true }
        } else {
            registerUnpairHandler(null)
        }
    }
}

@Composable
internal fun ReceiverAdminDialogs(
    route: AppRoute,
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
        relockReceiver(route, pairing, lock, kioskViewModel)
    }

    if (dialogs.showOptions) {
        ReceiverOptionsDialog(
            isReceiver = route is AppRoute.ReceiverHome,
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
                relockReceiver(route, pairing, lock, kioskViewModel)
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
                    relockReceiver(route, pairing, lock, kioskViewModel)
                }
            },
            onConfirm = onResetRole,
        )
    }
}

internal fun relockReceiver(
    route: AppRoute,
    pairing: PairingUiState,
    lock: KioskState,
    kioskViewModel: KioskViewModel,
) {
    if (route is AppRoute.ReceiverHome && pairing.pairing.isPaired && lock.hasPassword) {
        kioskViewModel.onAction(KioskAction.Lock(force = true))
    }
}
