package app.kiosksync.feature.guide.presentation

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val guidePresentationModule = module {
    viewModel { GuideViewModel(get()) }
}
