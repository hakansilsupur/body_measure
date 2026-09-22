package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.BuildConfig
import com.bodymeasure.app.R
import com.bodymeasure.app.update.ReleaseInfo

/**
 * The update prompt, in whichever of its four shapes the state calls for.
 *
 * No dialog appears for [UpdateState.Idle] or a silent check, so the app is
 * only ever interrupted when there is genuinely something new to install.
 */
@Composable
fun UpdateDialog(
    state: UpdateState,
    onDownload: (ReleaseInfo) -> Unit,
    onSkip: (ReleaseInfo) -> Unit,
    onGrantPermission: (ReleaseInfo) -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        UpdateState.Idle -> Unit

        UpdateState.Checking -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_checking)) },
            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        )

        is UpdateState.Available -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_available_title, state.release.version)) },
            text = { ReleaseSummary(state.release) },
            confirmButton = {
                TextButton(onClick = { onDownload(state.release) }) {
                    Text(stringResource(R.string.update_download_install))
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.update_later))
                    }
                    TextButton(onClick = { onSkip(state.release) }) {
                        Text(stringResource(R.string.update_skip))
                    }
                }
            }
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_downloading, state.release.version)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // A server that sends no Content-Length gives us nothing to
                    // fill a bar with, so spin instead of faking a percentage.
                    if (state.progress < 0f) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        )

        is UpdateState.NeedsPermission -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_text)) },
            confirmButton = {
                TextButton(onClick = { onGrantPermission(state.release) }) {
                    Text(stringResource(R.string.update_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_failed_title)) },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}

@Composable
private fun ReleaseSummary(release: ReleaseInfo) {
    Column(
        modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.update_from_to, BuildConfig.VERSION_NAME, release.version.toString()),
            style = MaterialTheme.typography.bodyMedium
        )
        if (release.sizeBytes > 0) {
            Text(
                stringResource(R.string.update_size, formatMb(release.sizeBytes)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (release.notes.isNotBlank()) {
            Text(release.notes, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            stringResource(R.string.update_install_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMb(bytes: Long): String = "%.1f".format(bytes / 1_048_576.0)
