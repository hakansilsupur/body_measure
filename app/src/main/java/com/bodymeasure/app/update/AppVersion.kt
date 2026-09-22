package com.bodymeasure.app.update

/**
 * A three-part version, used to decide whether a published release is newer
 * than the running build.
 *
 * The app has exactly one version string — `versionName` in build.gradle.kts —
 * and `versionCode` is derived from it there. Release tags are that same string
 * with a `v` in front. Keeping one source of truth matters more here than it
 * looks: if the tag and the built version can drift apart, an update can
 * install and still report itself as out of date, and the app nags forever.
 * The release workflow refuses to publish when the two disagree.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * Reads "1.2.3", "v1.2.3", "1.2" and "1.2.3-beta.1".
         *
         * Returns null on anything it cannot read in full rather than guessing.
         * A malformed tag then leaves the user on their current build, which is
         * the safe failure: the alternative is prompting someone to install
         * something off a version string we did not understand.
         *
         * Pre-release suffixes are dropped, so 1.2.0-beta compares equal to
         * 1.2.0. That is fine because the check reads GitHub's "latest release"
         * endpoint, which excludes pre-releases and drafts to begin with.
         */
        fun parse(raw: String?): AppVersion? {
            if (raw.isNullOrBlank()) return null
            val core = raw.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .substringBefore('+')
            val parts = core.split('.')
            if (parts.isEmpty() || parts.size > 3) return null
            val numbers = parts.map { part ->
                val n = part.trim().toIntOrNull() ?: return null
                if (n < 0) return null
                n
            }
            return AppVersion(
                major = numbers.getOrElse(0) { 0 },
                minor = numbers.getOrElse(1) { 0 },
                patch = numbers.getOrElse(2) { 0 }
            )
        }
    }
}
