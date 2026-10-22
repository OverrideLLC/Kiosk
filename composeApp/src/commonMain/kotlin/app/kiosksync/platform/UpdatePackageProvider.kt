package app.kiosksync.platform

/**
 * Returns the application payload that the controller sends to older screens.
 * The bytes are shared between all screens; the implementation caches the
 * package so several screens do not build or hold separate copies.
 */
data class UpdatePackage(
    val version: String,
    val sha256: String,
    val bytes: ByteArray
)

interface UpdatePackageProvider {
    fun isAvailable(): Boolean
    suspend fun getPackage(): Result<UpdatePackage>
}
