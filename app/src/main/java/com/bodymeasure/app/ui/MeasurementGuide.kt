package com.bodymeasure.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.R

enum class MeasurementGuide(
    @StringRes val titleRes: Int,
    @DrawableRes val imageRes: Int,
    @StringRes val textRes: Int
) {
    Weight(R.string.guide_weight_title, R.drawable.measure_weight, R.string.guide_weight_text),
    Height(R.string.guide_height_title, R.drawable.measure_height, R.string.guide_height_text),
    Neck(R.string.guide_neck_title, R.drawable.measure_neck, R.string.guide_neck_text),
    Waist(R.string.guide_waist_title, R.drawable.measure_waist, R.string.guide_waist_text),
    Hip(R.string.guide_hip_title, R.drawable.measure_hip, R.string.guide_hip_text),
    Chest(R.string.guide_chest_title, R.drawable.measure_chest, R.string.guide_chest_text),
    Arm(R.string.guide_arm_title, R.drawable.measure_arm, R.string.guide_arm_text),
    Thigh(R.string.guide_thigh_title, R.drawable.measure_thigh, R.string.guide_thigh_text)
}

@Composable
fun MeasurementGuideDialog(guide: MeasurementGuide, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(guide.titleRes)) },
        text = {
            Column {
                Image(
                    painter = painterResource(guide.imageRes),
                    contentDescription = stringResource(guide.titleRes),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                )
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
