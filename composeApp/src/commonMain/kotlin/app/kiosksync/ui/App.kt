package app.kiosksync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kiosksync.AppContainer
import app.kiosksync.core.APP_NAME
import app.kiosksync.core.Strings

@Composable
fun App(container: AppContainer) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E1E1E)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = APP_NAME,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Strings.SKELETON_READY,
                        fontSize = 16.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Data dir: ${container.dataDirPath}",
                        fontSize = 13.sp,
                        color = Color(0xFF777777)
                    )
                    if (container.args.role != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rol forzado: ${container.args.role}",
                            fontSize = 13.sp,
                            color = Color(0xFF88AA88)
                        )
                    }
                    if (container.args.port != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Puerto: ${container.args.port}",
                            fontSize = 13.sp,
                            color = Color(0xFF88AA88)
                        )
                    }
                    if (container.args.windowed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Modo: Ventana (--windowed)",
                            fontSize = 13.sp,
                            color = Color(0xFF88AA88)
                        )
                    }
                }
            }
        }
    }
}
