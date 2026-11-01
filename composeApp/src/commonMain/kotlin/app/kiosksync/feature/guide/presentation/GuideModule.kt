package app.kiosksync.feature.guide.presentation

import org.koin.dsl.module

internal val guidePresentationModule = module {
    factory { GuideViewModel(get()) }
}
