package app.kiosksync.feature.projection.di

import app.kiosksync.feature.projection.domain.ProjectionController
import app.kiosksync.feature.projection.domain.ProjectionService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val projectionModule = module {
    singleOf(::ProjectionController)
    single<ProjectionService> { get<ProjectionController>() }
}
