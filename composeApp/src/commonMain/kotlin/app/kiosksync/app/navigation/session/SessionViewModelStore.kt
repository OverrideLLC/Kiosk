package app.kiosksync.app.navigation.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.viewmodel.koinViewModel

/**
 * Window store for ViewModels shared across destinations (role, guide, pairing,
 * playback, kiosk). Navigation entries override [LocalViewModelStoreOwner]; this
 * local stays on the window so those models are not created twice.
 */
internal val LocalSessionViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("Session ViewModelStoreOwner is missing")
}

@Composable
internal inline fun <reified T : ViewModel> sessionViewModel(): T {
    val owner = LocalSessionViewModelStoreOwner.current
        ?: LocalViewModelStoreOwner.current
        ?: error("Session ViewModelStoreOwner is missing")
    return koinViewModel(viewModelStoreOwner = owner)
}
