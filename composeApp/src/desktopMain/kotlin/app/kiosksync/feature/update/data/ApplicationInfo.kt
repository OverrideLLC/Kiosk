package app.kiosksync.feature.update.data

import app.kiosksync.core.APP_PAYLOAD_DIR_NAME
import java.io.File

/**
 * Finds the packaged `app/` folder that contains this class. Development runs
 * from `build/classes/...`, so they deliberately report no update package.
 */
object ApplicationInfo {

    fun currentAppDir(): File? = runCatching {
        val location = ApplicationInfo::class.java.protectionDomain
            ?.codeSource
            ?.location
            ?: return null
        val source = File(location.toURI())
        val directory = if (source.isDirectory) source else source.parentFile ?: return null
        if (!directory.name.equals(APP_PAYLOAD_DIR_NAME, ignoreCase = true)) return null
        if (!directory.isDirectory) return null
        if (directory.listFiles()?.any { it.isFile && it.extension.equals("cfg", true) } != true) {
            return null
        }
        directory.canonicalFile
    }.getOrNull()
}
