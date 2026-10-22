package app.kiosksync.core

/**
 * Numeric semantic version used to decide whether a screen needs an update.
 * The app only ships stable versions in the MVP, so pre-release suffixes are
 * deliberately rejected instead of being compared with ambiguous rules.
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int = when {
        major != other.major -> major.compareTo(other.major)
        minor != other.minor -> minor.compareTo(other.minor)
        else -> patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"
}

fun parseAppVersion(value: String): SemanticVersion? {
    val normalized = value.trim().removePrefix("v").removePrefix("V")
    val parts = normalized.split('.')
    if (parts.size != 3) return null
    val numbers = parts.map { part ->
        if (part.isEmpty() || part.length > 4 || part.any { !it.isDigit() }) return null
        part.toIntOrNull() ?: return null
    }
    return SemanticVersion(numbers[0], numbers[1], numbers[2])
}

fun isValidAppVersion(value: String): Boolean = parseAppVersion(value) != null

enum class UpdateUrgency {
    NONE,
    OPTIONAL,
    FORCED
}

data class VersionUpdatePlan(
    val urgency: UpdateUrgency,
    val targetVersion: String
)

/**
 * Decides what the controller should offer to a screen.
 *
 * - Same version: nothing to do.
 * - Screen older, same major: optional update.
 * - Screen newer, same major: the screen is compatible, do not downgrade it.
 * - Different major in either direction: incompatible, force an update to the
 *   controller version. This also covers a controller older than the screen,
 *   because the controller only has its own package.
 */
object AppVersionPolicy {

    fun plan(controllerVersion: String, screenVersion: String?): VersionUpdatePlan {
        val target = controllerVersion.trim()
        if (parseAppVersion(target) == null) {
            return VersionUpdatePlan(UpdateUrgency.NONE, target)
        }
        val screen = screenVersion?.let { parseAppVersion(it) }
            ?: return VersionUpdatePlan(UpdateUrgency.NONE, target)
        val controller = parseAppVersion(target) ?: return VersionUpdatePlan(UpdateUrgency.NONE, target)

        return when {
            screen == controller -> VersionUpdatePlan(UpdateUrgency.NONE, target)
            screen.major != controller.major ->
                VersionUpdatePlan(UpdateUrgency.FORCED, target)
            screen < controller ->
                VersionUpdatePlan(UpdateUrgency.OPTIONAL, target)
            else -> VersionUpdatePlan(UpdateUrgency.NONE, target)
        }
    }

    fun isCompatible(controllerVersion: String, screenVersion: String?): Boolean {
        val controller = parseAppVersion(controllerVersion) ?: return false
        val screen = screenVersion?.let { parseAppVersion(it) } ?: return false
        return controller.major == screen.major
    }
}
