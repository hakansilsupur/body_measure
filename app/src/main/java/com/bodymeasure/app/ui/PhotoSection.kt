package com.bodymeasure.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.bodymeasure.app.R
import com.bodymeasure.app.data.PhotoStore
import java.io.File

/**
 * Progress-photo picker for the Record form. Offers camera capture and gallery
 * selection, shows a preview once set, and allows removing it.
 *
 * Gallery selection goes through the system photo picker, so no storage
 * permission is needed. Camera capture writes via FileProvider into the app's
 * own photo directory, so no CAMERA permission is needed either.
 */
@Composable
fun PhotoSection(
    photoFileName: String?,
    onPicked: (Uri) -> Unit,
    onCaptured: (String) -> Unit,
    onRemove: () -> Unit,
    newCameraTarget: () -> Pair<String, File>,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var pendingCaptureName by remember { mutableStateOf<String?>(null) }
    var viewerOpen by remember { mutableStateOf(false) }

    val cameraFailed = stringResource(R.string.photo_camera_failed)

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPicked) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val name = pendingCaptureName
        pendingCaptureName = null
        when {
            !success && name != null -> {
                // Cancelled or failed: drop the empty file the camera left behind.
                PhotoStore.delete(context, name)
            }
            success && name != null -> onCaptured(name)
        }
    }

    Column {
        Text(
            stringResource(R.string.photo_section),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (photoFileName != null && PhotoStore.exists(context, photoFileName)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = PhotoStore.file(context, photoFileName),
                        contentDescription = stringResource(R.string.photo_section),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clickable { viewerOpen = true }
                    )
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.photo_remove)
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.photo_tap_to_enlarge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (photoFileName != null) 8.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val (name, file) = newCameraTarget()
                    val uri = runCatching {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    }.getOrNull()
                    if (uri == null) {
                        showMessage(cameraFailed)
                    } else {
                        pendingCaptureName = name
                        runCatching { cameraLauncher.launch(uri) }.onFailure {
                            pendingCaptureName = null
                            PhotoStore.delete(context, name)
                            showMessage(cameraFailed)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Text(
                    stringResource(
                        if (photoFileName == null) R.string.photo_take else R.string.photo_retake
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Text(
                    stringResource(R.string.photo_choose),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }

    if (viewerOpen && photoFileName != null) {
        PhotoViewerDialog(
            file = PhotoStore.file(context, photoFileName),
            onDismiss = { viewerOpen = false }
        )
    }
}

/** Full-screen view of a stored photo. */
@Composable
fun PhotoViewerDialog(file: File, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                model = file,
                contentDescription = stringResource(R.string.photo_section),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.got_it))
            }
        }
    }
}
