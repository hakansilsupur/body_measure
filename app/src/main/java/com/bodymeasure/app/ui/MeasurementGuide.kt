package com.bodymeasure.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.bodymeasure.app.R

/**
 * How-to-measure tutorial entries.
 *
 * Each guide ships a bundled diagram ([imageRes]) so the dialog always shows
 * something, offline and with no licensing question. Photographs of measurement
 * technique are almost all stock-licensed, so they cannot be bundled here.
 *
 * To use a photo instead, set [imageUrl] to a direct image URL you have the
 * right to use; it takes precedence over the bundled diagram. Set [attribution]
 * when the licence requires a credit and it renders under the image.
 */
enum class MeasurementGuide(
    @StringRes val titleRes: Int,
    @DrawableRes val imageRes: Int,
    val imageUrl: String = "",
    val attribution: String? = null,
    @StringRes val textRes: Int
) {
    Neck(
        titleRes = R.string.guide_neck_title,
        imageRes = R.drawable.measure_neck,
        textRes = R.string.guide_neck_text
    ),
    Waist(
        titleRes = R.string.guide_waist_title,
        imageRes = R.drawable.measure_waist,
        textRes = R.string.guide_waist_text
    ),
    Hip(
        titleRes = R.string.guide_hip_title,
        imageRes = R.drawable.measure_hip,
        textRes = R.string.guide_hip_text
    ),
    Chest(
        titleRes = R.string.guide_chest_title,
        imageRes = R.drawable.measure_chest,
        textRes = R.string.guide_chest_text
    ),
    Arm(
        titleRes = R.string.guide_arm_title,
        imageRes = R.drawable.measure_arm,
        textRes = R.string.guide_arm_text
    ),
    Thigh(
        titleRes = R.string.guide_thigh_title,
        imageRes = R.drawable.measure_thigh,
        textRes = R.string.guide_thigh_text
    ),
    Calf(
        titleRes = R.string.guide_calf_title,
        imageRes = R.drawable.measure_calf,
        textRes = R.string.guide_calf_text
    )
}

@Composable
fun MeasurementGuideDialog(guide: MeasurementGuide, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(guide.titleRes)) },
        text = {
            Column {
                GuideImage(
                    url = guide.imageUrl,
                    fallbackRes = guide.imageRes,
                    contentDescription = stringResource(guide.titleRes)
                )
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
private fun GuideImage(
    url: String,
    @DrawableRes fallbackRes: Int,
    contentDescription: String
) {
    val box = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (url.isBlank()) {
        Box(modifier = box.padding(8.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(fallbackRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
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
