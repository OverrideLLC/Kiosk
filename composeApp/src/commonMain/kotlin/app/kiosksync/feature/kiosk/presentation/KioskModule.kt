package app.kiosksync.feature.kiosk.presentation

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val kioskPresentationModule = module {
    viewModel { KioskViewModel(get()) }
}
