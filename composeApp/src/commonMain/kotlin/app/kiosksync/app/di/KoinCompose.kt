package app.kiosksync.app.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.getKoin
import org.koin.core.parameter.ParametersDefinition

@Composable
internal inline fun <reified T : Any> rememberInjected(
    vararg keys: Any?,
    noinline parameters: ParametersDefinition? = null,
): T {
    val koin = getKoin()
    return remember(*keys) { koin.get(parameters = parameters) }
}
