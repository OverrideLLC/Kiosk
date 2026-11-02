package app.kiosksync.feature.pairing.di

import app.kiosksync.feature.pairing.data.JmDnsDiscovery
import app.kiosksync.feature.pairing.domain.DeviceDiscovery
import app.kiosksync.feature.pairing.domain.PairingManager
import org.koin.dsl.module

internal val pairingModule = module {
    single { PairingManager(repository = get(), logger = get()) }
    single<DeviceDiscovery> { JmDnsDiscovery(get()) }
}
