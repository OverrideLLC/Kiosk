package app.kiosksync.feature.kiosk.presentation

import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

internal class KioskViewModelHost(
    val scope: CoroutineScope,
)

internal val kioskPresentationModule = module {
    factory { (host: KioskViewModelHost) ->
        KioskViewModel(get(), host.scope)
    }
}
