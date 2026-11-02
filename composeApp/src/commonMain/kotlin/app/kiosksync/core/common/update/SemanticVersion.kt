package app.kiosksync.core.common.update

/**
 * Numeric semantic version used to decide whether a screen needs an update.
 * The app only ships stable versions in the MVP, so pre-release suffixes are
 * rejected instead of being compared with ambiguous rules.
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int = compareValuesBy(
        this,
        other,
        SemanticVersion::major,
        SemanticVersion::minor,
        SemanticVersion::patch
    )

    override fun toString(): String = "$major.$minor.$patch"
}

fun parseAppVersion(value: String): SemanticVersion? {
    val parts = value.trim().removePrefix("v").removePrefix("V").split('.')
    if (parts.size != 3) return null
    val numbers = parts.map { part ->
        if (part.isEmpty() || part.length > 4 || part.any { !it.isDigit() }) return null
        part.toInt()
    }
    return SemanticVersion(numbers[0], numbers[1], numbers[2])
}

fun isValidAppVersion(value: String): Boolean = parseAppVersion(value) != null
