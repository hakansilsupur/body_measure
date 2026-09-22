package com.bodymeasure.app.update

import android.content.Context

/**
 * The little that the updater needs to remember between launches: when it last
 * looked, and which version the user told it to stop asking about.
 *
 * Kept out of the Room database on purpose. This is disposable UI state, and
 * putting it in the database would mean a migration and would carry a stale
 * "skipped" flag into an exported backup.
 */
class UpdatePrefs(context: Context) {

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    /** Once every six hours is often enough to notice a release, and rare
     *  enough to stay far under GitHub's anonymous rate limit. */
    private val checkIntervalMs = 6 * 60 * 60 * 1000L

    fun shouldAutoCheck(now: Long = System.currentTimeMillis()): Boolean {
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        // A clock moved backwards would otherwise park the next check in the
        // far future, so treat any future timestamp as due.
        if (last > now) return true
        return now - last >= checkIntervalMs
    }

    fun markChecked(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
    }

    fun skip(version: AppVersion) {
        prefs.edit().putString(KEY_SKIPPED, version.toString()).apply()
    }

    /**
     * Skipping suppresses that one version, not every later one — so a user who
     * skips 1.2.0 is still told about 1.3.0.
     */
    fun isSkipped(version: AppVersion): Boolean =
        AppVersion.parse(prefs.getString(KEY_SKIPPED, null)) == version

    private companion object {
        const val KEY_LAST_CHECK = "last_check_ms"
        const val KEY_SKIPPED = "skipped_version"
    }
}
