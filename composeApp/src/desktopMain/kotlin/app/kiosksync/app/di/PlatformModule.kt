package app.kiosksync.app.di

import app.kiosksync.core.common.app.AppArgs
import app.kiosksync.core.common.app.AppLogger
import app.kiosksync.core.security.SecretProtector
import app.kiosksync.core.security.SecretProtectorFactory
import app.kiosksync.core.security.Sha256
import app.kiosksync.feature.playlist.data.DesktopFilePicker
import java.io.File
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.dsl.module

internal fun platformModule(
    args: AppArgs,
    dataDir: File,
    logger: AppLogger,
    closeables: BootstrapCloseables,
) = module {
    single { args }
    single { dataDir }
    single { logger }
    single<CoroutineDispatcher>(AppQualifiers.io) { Dispatchers.IO }
    single<CoroutineDispatcher>(AppQualifiers.serviceDispatcher) {
        val dispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "OverrideKiosk-services").apply { isDaemon = true }
        }.asCoroutineDispatcher()
        closeables.serviceDispatcher = dispatcher
        dispatcher
    }
    single<CoroutineScope>(AppQualifiers.serviceScope) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(AppQualifiers.serviceDispatcher))
    }
    single<CoroutineScope>(AppQualifiers.kioskScope) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    single<PasswordDigest> {
        PasswordDigest { input -> Sha256.hex(input.toByteArray(Charsets.UTF_8)) }
    }
    single<DeviceIdFactory> { DeviceIdFactory { UUID.randomUUID().toString() } }
    single<ImageFilePicker> {
        val log = get<AppLogger>()
        ImageFilePicker { DesktopFilePicker.pickImages(log) }
    }
    single<SecretProtector> { SecretProtectorFactory.create(get()) }
    single {
        val windows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)
        AppSession(
            args = get(),
            dataDirPath = get<File>().absolutePath,
            deviceName = resolveDeviceName(),
            platformLockAvailable = windows && !get<AppArgs>().windowed,
        )
    }
}

private fun resolveDeviceName(): String = try {
    System.getenv("COMPUTERNAME") ?: InetAddress.getLocalHost().hostName
} catch (_: Exception) {
    System.getenv("COMPUTERNAME") ?: "WindowsPC"
}
