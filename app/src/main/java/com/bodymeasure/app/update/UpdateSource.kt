package com.bodymeasure.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext

/** A published release that carries an installable APK. */
data class ReleaseInfo(
    val version: AppVersion,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val sizeBytes: Long
)

/**
 * Reads the project's GitHub releases and downloads the APK attached to one.
 *
 * Deliberately built on [HttpURLConnection] and `org.json`, both in the
 * platform, so checking for updates adds no dependency to an app that otherwise
 * does no networking of its own.
 *
 * The CI artifact from a normal build is *not* usable here: artifact downloads
 * require an authenticated GitHub token, so the app would have to ship a
 * credential to fetch one. Release assets on a public repository are plain
 * anonymous HTTPS, which is why updates go out as releases.
 */
object UpdateSource {

    const val OWNER = "hakansilsupur"
    const val REPO = "body_measure"

    const val RELEASES_PAGE = "https://github.com/$OWNER/$REPO/releases"
    private const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private const val TIMEOUT_MS = 15_000
    private const val MAX_REDIRECTS = 5
    private const val USER_AGENT = "BodyMeasure-Updater"

    /**
     * The newest published release, or null when there is nothing installable.
     *
     * Null covers three cases that all mean the same thing to the caller: the
     * project has no releases, the newest one carries no .apk asset, or its tag
     * is not a version we can parse. In each the honest answer is "nothing to
     * offer", not an error.
     */
    suspend fun latestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val body = readText(LATEST_URL, accept = "application/vnd.github+json")
            ?: return@withContext null
        parseRelease(body)
    }

    /** Split out from the network call so the parsing rules are readable. */
    private fun parseRelease(body: String): ReleaseInfo? {
        val json = JSONObject(body)
        val tag = json.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
        val version = AppVersion.parse(tag) ?: return null
        val assets = json.optJSONArray("assets") ?: return null

        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            val url = asset.optString("browser_download_url").takeIf { it.isNotBlank() } ?: continue
            return ReleaseInfo(
                version = version,
                tag = tag,
                notes = json.optString("body").trim(),
                apkUrl = url,
                apkName = name,
                sizeBytes = asset.optLong("size", -1L)
            )
        }
        return null
    }

    /** Body of a GET, or null on 404. Other failures throw. */
    private fun readText(url: String, accept: String): String? {
        val conn = openFollowingRedirects(url, accept)
        try {
            if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return null
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Streams the APK to [target]. [onProgress] reports 0..1, or -1 when the
     * server does not say how large the file is.
     *
     * Writes to a sibling `.part` file and renames on success, so a download cut
     * off midway can never be handed to the package installer as if it were
     * whole.
     */
    suspend fun downloadApk(
        release: ReleaseInfo,
        target: File,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val job: CoroutineContext = currentCoroutineContext()

        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")
        partial.delete()

        val conn = openFollowingRedirects(release.apkUrl, "application/octet-stream")
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            var read = 0L
            var lastPercent = -1

            conn.inputStream.use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        // Cancelling the dialog should stop the transfer, not
                        // leave it pulling bytes in the background.
                        job.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            // Only on a whole-percent change, or this recomposes
                            // the progress bar thousands of times.
                            val percent = (read * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        } else if (lastPercent < 0) {
                            lastPercent = 0
                            onProgress(-1f)
                        }
                    }
                }
            }

            if (total > 0 && read != total) {
                throw IOException("download truncated at $read of $total bytes")
            }

            target.delete()
            if (!partial.renameTo(target)) throw IOException("could not finalise the download")
            target
        } catch (t: Throwable) {
            partial.delete()
            throw t
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Opens [startUrl], following redirects by hand.
     *
     * Done manually so every hop can be required to stay on HTTPS. GitHub
     * bounces release assets to a CDN whose hostname it has changed more than
     * once, so pinning hostnames would break on their schedule; requiring TLS
     * the whole way is the check that actually matters, and it does not rot.
     */
    private fun openFollowingRedirects(startUrl: String, accept: String): HttpURLConnection {
        var url = URL(startUrl)
        repeat(MAX_REDIRECTS) {
            if (!url.protocol.equals("https", ignoreCase = true)) {
                throw IOException("refusing to fetch over ${url.protocol}")
            }
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", USER_AGENT)
            }
            when (val code = conn.responseCode) {
                301, 302, 303, 307, 308 -> {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) throw IOException("redirect without a location")
                    url = URL(url, location)
                }
                else -> {
                    if (code >= 400 && code != HttpURLConnection.HTTP_NOT_FOUND) {
                        conn.disconnect()
                        throw IOException("HTTP $code")
                    }
                    return conn
                }
            }
        }
        throw IOException("too many redirects")
    }
}
