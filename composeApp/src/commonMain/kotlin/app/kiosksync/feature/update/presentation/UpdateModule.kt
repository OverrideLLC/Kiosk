package app.kiosksync.feature.update.presentation

import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

internal class ReceiverUpdateHost(
    val scope: CoroutineScope,
    val onExit: () -> Unit,
)

internal val updatePresentationModule = module {
    factory { (host: ReceiverUpdateHost) ->
        ReceiverUpdateViewModel(
            manager = get(),
            scope = host.scope,
            onExit = host.onExit,
        )
    }
}
