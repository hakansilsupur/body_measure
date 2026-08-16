package com.bodymeasure.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.bodymeasure.app.R

/**
 * How-to-measure tutorial entries. Each guide loads its image from a URL at
 * runtime — replace the URLs below with photos you have permission to use
 * (e.g. CC-licensed Wikimedia Commons images, your own photos hosted on a
 * static URL, etc.). An empty URL renders a "no image set" placeholder so
 * the text instructions still display.
 *
 * Why URLs instead of bundled images? Bundling stock photos would require a
 * license per image. Loading from URLs keeps the licensing decision (and
 * attribution) under your control.
 */
enum class MeasurementGuide(
    @StringRes val titleRes: Int,
    val imageUrl: String,
    val attribution: String? = null,
    @StringRes val textRes: Int
) {
    Neck(
        titleRes = R.string.guide_neck_title,
        imageUrl = "",
        attribution = null,
        textRes = R.string.guide_neck_text
    ),
    Waist(
        titleRes = R.string.guide_waist_title,
        imageUrl = "",
        attribution = null,
        textRes = R.string.guide_waist_text
    ),
    Hip(
        titleRes = R.string.guide_hip_title,
        imageUrl = "",
        attribution = null,
        textRes = R.string.guide_hip_text
    ),
    Chest(
        titleRes = R.string.guide_chest_title,
        imageUrl = "",
        attribution = null,
        textRes = R.string.guide_chest_text
    ),
    Arm(
        titleRes = R.string.guide_arm_title,
        imageUrl = "",
        attribution = null,
        textRes = R.string.guide_arm_text
    ),
    Thigh(
        titleRes = R.string.guide_thigh_title,
        imageUrl = "",
        attribution = null,
        textRes = R.string.guide_thigh_text
    )
}

@Composable
fun MeasurementGuideDialog(guide: MeasurementGuide, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(guide.titleRes)) },
        text = {
            Column {
                GuideImage(url = guide.imageUrl, contentDescription = stringResource(guide.titleRes))
                guide.attribution?.takeIf { it.isNotBlank() }?.let { credit ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        credit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(guide.textRes),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.got_it))
            }
        }
    )
}

@Composable
private fun GuideImage(url: String, contentDescription: String) {
    val box = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (url.isBlank()) {
        Box(modifier = box, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.guide_no_image),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = box.padding(8.dp)
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is AsyncImagePainter.State.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.guide_image_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            else -> SubcomposeAsyncImageContent()
        }
    }
}
