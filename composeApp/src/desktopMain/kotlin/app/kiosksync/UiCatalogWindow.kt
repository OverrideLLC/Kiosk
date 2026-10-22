package app.kiosksync

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.ui.catalog.CatalogScreen

@Composable
internal fun UiCatalogWindow(onExit: () -> Unit) {
    Window(
        onCloseRequest = onExit,
        title = "$APP_NAME · Catálogo de UI",
        state = rememberWindowState(width = 1280.dp, height = 900.dp),
    ) {
        CatalogScreen()
    }
}

