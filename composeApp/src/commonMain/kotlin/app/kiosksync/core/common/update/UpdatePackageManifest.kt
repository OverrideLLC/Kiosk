package app.kiosksync.core.common.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePackageManifest(
    val version: String,
    val format: Int = UPDATE_PACKAGE_FORMAT
)
