package app.kiosksync.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import app.kiosksync.app.session.RoleSession
import app.kiosksync.app.shell.AppPreferences
import app.kiosksync.feature.guide.presentation.GuideViewModel
import app.kiosksync.feature.pairing.presentation.PairingViewModel
import app.kiosksync.feature.playback.presentation.PlaybackStateHolder

/**
 * Session objects created with the window. Navigation entries resolve this
 * from Koin instead of closing over [app.kiosksync.app.App].
 */
internal class AppDestinationContext {
    var backStack: MutableList<NavKey>? = null
    var role by mutableStateOf<RoleSession?>(null)
    var preferences by mutableStateOf<AppPreferences?>(null)
    var guideViewModel by mutableStateOf<GuideViewModel?>(null)
    var pairingViewModel by mutableStateOf<PairingViewModel?>(null)
    var playback by mutableStateOf<PlaybackStateHolder?>(null)
    var hasKioskPassword by mutableStateOf(false)
    var onExitRequested: () -> Unit = {}

    fun replace(route: AppRoute) {
        replaceRoute(checkNotNull(backStack), route)
    }

    fun resetRole() {
        val stack = checkNotNull(backStack)
        checkNotNull(role).reset(currentRoute(stack), ::replace)
    }
}
