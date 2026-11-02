package app.kiosksync.app.host

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.core.common.app.APP_NAME
import app.kiosksync.core.ui.catalog.CatalogScreen
import app.kiosksync.resources.Res
import app.kiosksync.resources.window_ui_catalog
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UiCatalogWindow(onExit: () -> Unit) {
    Window(
        onCloseRequest = onExit,
        title = stringResource(Res.string.window_ui_catalog, APP_NAME),
        state = rememberWindowState(width = 1280.dp, height = 900.dp),
    ) {
        CatalogScreen()
    }
}

