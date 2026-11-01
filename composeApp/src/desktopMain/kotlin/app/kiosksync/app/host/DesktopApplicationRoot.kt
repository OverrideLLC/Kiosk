package app.kiosksync.app.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import app.kiosksync.app.di.startDesktopKoin
import app.kiosksync.core.common.app.APP_NAME
import app.kiosksync.core.common.app.AppArgs
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.ui.Strings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.stopKoin
import java.io.File

@Composable
internal fun DesktopApplicationRoot(
    appArgs: AppArgs,
    dataDir: File,
    logger: AppLogger,
    onExit: () -> Unit,
) {
    var retryNonce by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(retryNonce) {
        ready = false
        failed = false
        runCatching { stopKoin() }
        try {
            withContext(Dispatchers.IO) {
                startDesktopKoin(appArgs, dataDir, logger)
            }
            ready = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Desktop application startup failed", error)
            failed = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { stopKoin() } }
    }

    if (ready) {
        DesktopWindowHost(onExit)
    } else {
        Window(
            onCloseRequest = onExit,
            title = APP_NAME,
            state = rememberWindowState(width = 560.dp, height = 360.dp),
            resizable = false,
        ) {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (failed) {
                        Text(Strings.STARTUP_ERROR, style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = { retryNonce++ },
                            modifier = Modifier.padding(top = 20.dp),
                        ) {
                            Text(Strings.STARTUP_RETRY)
                        }
                    } else {
                        CircularProgressIndicator()
                        Text(
                            text = Strings.STARTUP_LOADING,
                            modifier = Modifier.padding(top = 20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

