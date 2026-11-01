package app.kiosksync.core.common.update

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
 * Same version does nothing. An older screen on the same major gets an optional
 * update. A newer screen on the same major stays as it is. A different major,
 * in either direction, forces an update to the controller version because the
 * controller only has its own package.
 */
object AppVersionPolicy {
    fun plan(controllerVersion: String, screenVersion: String?): VersionUpdatePlan {
        val target = controllerVersion.trim()
        val controller = parseAppVersion(target) ?: return VersionUpdatePlan(UpdateUrgency.NONE, target)
        val screen = screenVersion?.let(::parseAppVersion) ?: return VersionUpdatePlan(UpdateUrgency.NONE, target)
        val urgency = when {
            screen == controller -> UpdateUrgency.NONE
            screen.major != controller.major -> UpdateUrgency.FORCED
            screen < controller -> UpdateUrgency.OPTIONAL
            else -> UpdateUrgency.NONE
        }
        return VersionUpdatePlan(urgency, target)
    }

    fun isCompatible(controllerVersion: String, screenVersion: String?): Boolean {
        val controller = parseAppVersion(controllerVersion) ?: return false
        val screen = screenVersion?.let(::parseAppVersion) ?: return false
        return controller.major == screen.major
    }
}
