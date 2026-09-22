package com.bodymeasure.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a downloaded APK to the system package installer.
 *
 * There is no silent self-update available to an ordinary sideloaded app.
 * Installing is a privileged operation, and Android reserves it for the system
 * installer UI unless the app is a device owner or is itself a privileged
 * system app. So "update automatically" here means one tap to download and one
 * confirmation in the system dialog — the confirmation cannot be skipped, and
 * an app that claimed otherwise would be lying.
 *
 * Because every build is signed with the same checked-in debug key, the new APK
 * installs over the old one and the database survives. An APK signed with any
 * other key is refused by the platform, which is also the only thing standing
 * between the user and a substituted download.
 */
object UpdateInstaller {

    /** Where downloads land: app cache, so the OS can reclaim it. */
    fun downloadDir(context: Context): File = File(context.cacheDir, "updates")

    fun fileFor(context: Context, release: ReleaseInfo): File =
        File(downloadDir(context), sanitize(release.apkName))

    /**
     * Whether the user has already allowed this app to install packages. Android
     * grants it per-app from Settings and there is no runtime prompt for it, so
     * when this is false the only route is [unknownSourcesIntent].
     */
    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Settings screen where the user turns on "install unknown apps" for us. */
    fun unknownSourcesIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Clears earlier downloads; the installed build no longer needs them. */
    fun clearDownloads(context: Context) {
        downloadDir(context).listFiles()?.forEach { it.delete() }
    }

    /**
     * The asset name comes from a remote server, so it is never allowed to
     * steer the write anywhere but the download directory.
     */
    private fun sanitize(name: String): String {
        val cleaned = name.substringAfterLast('/').substringAfterLast('\\')
            .filter { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
        return cleaned.takeIf { it.endsWith(".apk", ignoreCase = true) } ?: "update.apk"
    }
}
