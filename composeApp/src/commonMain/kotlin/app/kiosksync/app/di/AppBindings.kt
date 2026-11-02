package app.kiosksync.app.di

import org.koin.core.qualifier.named

fun interface PasswordDigest {
    fun hash(value: String): String
}

fun interface DeviceIdFactory {
    fun create(): String
}

fun interface ImageFilePicker {
    suspend fun pick(): List<String>
}

object AppQualifiers {
    val io = named("io")
    val serviceDispatcher = named("serviceDispatcher")
    val serviceScope = named("serviceScope")
    val kioskScope = named("kioskScope")
}
