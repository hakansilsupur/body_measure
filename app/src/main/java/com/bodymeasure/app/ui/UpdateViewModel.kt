package com.bodymeasure.app.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bodymeasure.app.BuildConfig
import com.bodymeasure.app.R
import com.bodymeasure.app.update.AppVersion
import com.bodymeasure.app.update.ReleaseInfo
import com.bodymeasure.app.update.UpdateInstaller
import com.bodymeasure.app.update.UpdatePrefs
import com.bodymeasure.app.update.UpdateSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    /** Nothing to show. */
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class Available(val release: ReleaseInfo) : UpdateState

    /** [progress] is 0..1, or -1 when the server did not give a size. */
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState

    /** Downloaded, but the app is not yet allowed to install packages. */
    data class NeedsPermission(val release: ReleaseInfo) : UpdateState

    data class Failed(val message: String) : UpdateState
}

/**
 * Checks GitHub releases for a newer build and downloads it on request.
 *
 * Two entry points with deliberately different manners. The launch check is
 * silent: it says nothing when the app is current and nothing when the network
 * is down, because an app that greets you with "couldn't check for updates"
 * every time you open it on a train is worse than one that stays quiet. The
 * menu check reports every outcome, because the user just asked it to look.
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    /** A one-shot line for the snackbar, from a check the user asked for. */
    var message by mutableStateOf<String?>(null)
        private set

    /**
     * Set once a download has finished and installing is permitted. The UI
     * consumes it, launches the system installer and clears it — which keeps
     * every Activity and Context-for-startActivity out of this class.
     */
    var installRequest by mutableStateOf<File?>(null)
        private set

    private val prefs = UpdatePrefs(app)
    private var job: Job? = null

    val currentVersion: AppVersion? = AppVersion.parse(BuildConfig.VERSION_NAME)

    fun checkOnLaunch() {
        if (state != UpdateState.Idle) return
        if (!prefs.shouldAutoCheck()) return
        check(silent = true)
    }

    fun checkNow() = check(silent = false)

    private fun check(silent: Boolean) {
        if (job?.isActive == true) return
        if (!silent) state = UpdateState.Checking

        job = viewModelScope.launch {
            val release = try {
                UpdateSource.latestRelease()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Being offline at launch is the normal case, not a problem to
                // report, so only a check the user asked for says anything.
                state = UpdateState.Idle
                if (!silent) message = string(R.string.update_check_failed)
                return@launch
            }

            prefs.markChecked()
            val current = currentVersion

            if (release == null || current == null || release.version <= current) {
                state = UpdateState.Idle
                // Nothing newer exists, so any half-finished download from an
                // earlier session is dead weight in the cache.
                UpdateInstaller.clearDownloads(getApplication())
                if (!silent) message = string(R.string.update_up_to_date, BuildConfig.VERSION_NAME)
                return@launch
            }

            // A skipped version stays skipped at launch, but an explicit
            // "check for updates" overrides it — the user is asking.
            state = if (silent && prefs.isSkipped(release.version)) {
                UpdateState.Idle
            } else {
                UpdateState.Available(release)
            }
        }
    }

    fun download(release: ReleaseInfo) {
        if (job?.isActive == true) return
        state = UpdateState.Downloading(release, -1f)

        job = viewModelScope.launch {
            val context = getApplication<Application>()
            val target = UpdateInstaller.fileFor(context, release)

            try {
                UpdateSource.downloadApk(release, target) { progress ->
                    state = UpdateState.Downloading(release, progress)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = UpdateState.Failed(string(R.string.update_download_failed))
                return@launch
            }

            // Installing needs a permission Android grants only from Settings —
            // there is no runtime prompt — so send the user there rather than
            // firing an intent that would silently do nothing.
            if (UpdateInstaller.canInstall(context)) {
                installRequest = target
                state = UpdateState.Idle
            } else {
                state = UpdateState.NeedsPermission(release)
            }
        }
    }

    /** Called when the user returns from the "install unknown apps" screen. */
    fun retryInstall(release: ReleaseInfo) {
        val context = getApplication<Application>()
        val file = UpdateInstaller.fileFor(context, release)
        when {
            !file.exists() -> state = UpdateState.Available(release)
            UpdateInstaller.canInstall(context) -> {
                installRequest = file
                state = UpdateState.Idle
            }
        }
    }

    fun skip(release: ReleaseInfo) {
        prefs.skip(release.version)
        dismiss()
    }

    fun dismiss() {
        job?.cancel()
        job = null
        state = UpdateState.Idle
    }

    fun consumeInstallRequest() {
        installRequest = null
    }

    fun consumeMessage() {
        message = null
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)
}
