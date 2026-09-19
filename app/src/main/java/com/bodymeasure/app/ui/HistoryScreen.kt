package com.bodymeasure.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.R
import coil.compose.AsyncImage
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.data.PhotoStore
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.BmiCategory
import com.bodymeasure.app.util.Bmr
import com.bodymeasure.app.util.BodyFat
import com.bodymeasure.app.util.BodyAnalysis
import com.bodymeasure.app.util.BodyFatCategory
import com.bodymeasure.app.util.FfmiBand
import com.bodymeasure.app.util.Sex
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    items: List<Measurement>,
    onDelete: (Long) -> Unit,
    onEdit: (Measurement) -> Unit = {}
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.empty_history),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { m ->
            MeasurementRow(m, onDelete = { onDelete(m.id) }, onEdit = { onEdit(m) })
        }
    }
}

/**
 * One history entry. Collapsed it shows just the date and the headline figures;
 * tapping expands it to the full breakdown. Editing and deleting live inside the
 * expanded card so the collapsed list stays scannable.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementRow(m: Measurement, onDelete: () -> Unit, onEdit: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var photoViewerOpen by remember { mutableStateOf(false) }

    val bmiCategory = Bmi.categorize(m.bmi)
    val (catLabel, catColor) = when (bmiCategory) {
        BmiCategory.Underweight -> stringResource(R.string.bmi_underweight) to Color(0xFF42A5F5)
        BmiCategory.Normal -> stringResource(R.string.bmi_normal) to Color(0xFF43A047)
        BmiCategory.Overweight -> stringResource(R.string.bmi_overweight) to Color(0xFFFB8C00)
        BmiCategory.Obese -> stringResource(R.string.bmi_obese) to Color(0xFFE53935)
    }
    val sex = runCatching { Sex.valueOf(m.sex) }.getOrDefault(Sex.Male)
    val bf = m.bodyFatPct
    val (bfLabel, bfColor) = bf?.let { BodyFat.categorize(sex, it) }?.let {
        when (it) {
            BodyFatCategory.Essential -> stringResource(R.string.bf_essential) to Color(0xFF42A5F5)
            BodyFatCategory.Athletes -> stringResource(R.string.bf_athletes) to Color(0xFF26A69A)
            BodyFatCategory.Fitness -> stringResource(R.string.bf_fitness) to Color(0xFF43A047)
            BodyFatCategory.Average -> stringResource(R.string.bf_average) to Color(0xFFFB8C00)
            BodyFatCategory.Obese -> stringResource(R.string.bf_obese) to Color(0xFFE53935)
        }
    } ?: ("" to Color.Unspecified)

    // Derived here rather than stored: it is a pure function of weight, height
    // and body fat, so older entries get it retroactively.
    val ffmi = BodyAnalysis.normalizedFfmi(m.weightKg, m.heightCm, m.bodyFatPct)
    val (ffmiLabel, ffmiColor) = ffmi?.let { BodyAnalysis.ffmiBand(sex, it) }?.let {
        when (it) {
            FfmiBand.BelowAverage -> stringResource(R.string.ffmi_below_average) to Color(0xFF42A5F5)
            FfmiBand.Average -> stringResource(R.string.ffmi_average) to Color(0xFF42A5F5)
            FfmiBand.AboveAverage -> stringResource(R.string.ffmi_above_average) to Color(0xFF43A047)
            FfmiBand.Athletic -> stringResource(R.string.ffmi_athletic) to Color(0xFF26A69A)
            FfmiBand.Exceptional -> stringResource(R.string.ffmi_exceptional) to Color(0xFF26A69A)
        }
    } ?: ("" to Color.Unspecified)

    val df = rememberDateFormat()

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

            // ---- always visible: date + headline figures ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        df.format(Date(m.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Metric("BMI", Bmi.format(m.bmi), catColor)
                        bf?.let { Metric("BF", "${BodyFat.format(it)}%", bfColor) }
                        ffmi?.let { Metric("FFMI", BodyAnalysis.format1(it), ffmiColor) }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.collapse else R.string.expand
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- revealed on tap ----
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "BMI ${Bmi.format(m.bmi)} \u2014 $catLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = catColor
                        )
                        if (bf != null) {
                            Text(
                                "Body fat ${BodyFat.format(bf)}% \u2014 $bfLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bfColor
                            )
                        }
                        if (ffmi != null) {
                            Text(
                                "FFMI ${BodyAnalysis.format1(ffmi)} \u2014 $ffmiLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ffmiColor
                            )
                        }
                        m.bmrKcal?.let { bmr ->
                            val tdee = Bmr.tdee(bmr, m.activityFactor)
                            Text(
                                if (tdee != null)
                                    "TDEE ${Bmr.format(tdee)} ${stringResource(R.string.bmr_unit)}" +
                                        "  (BMR ${Bmr.format(bmr)})"
                                else
                                    "BMR ${Bmr.format(bmr)} ${stringResource(R.string.bmr_unit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (tdee != null) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Stat("Weight", "${fmt(m.weightKg)} kg", modifier = Modifier.weight(1f))
                        Stat("Height", "${fmt(m.heightCm)} cm", modifier = Modifier.weight(1f))
                    }
                    FlowStats(m)

                    val context = LocalContext.current
                    val photo = m.photoFileName
                    if (photo != null && PhotoStore.exists(context, photo)) {
                        AsyncImage(
                            model = PhotoStore.file(context, photo),
                            contentDescription = stringResource(R.string.photo_section),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { photoViewerOpen = true }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Text(
                                stringResource(R.string.edit),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text(
                                stringResource(R.string.delete),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val ctx = LocalContext.current
    val photoName = m.photoFileName
    if (photoViewerOpen && photoName != null) {
        PhotoViewerDialog(
            file = PhotoStore.file(ctx, photoName),
            onDismiss = { photoViewerOpen = false }
        )
    }
}

/** Compact "LABEL value" pair for the collapsed card. */
@Composable
private fun Metric(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 3.dp, bottom = 2.dp)
        )
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun FlowStats(m: Measurement) {
    val pairs = listOfNotNull(
        m.ageYears?.let { "Age" to "$it y" },
        m.neckCm?.let { "Neck" to "${fmt(it)} cm" },
        m.waistCm?.let { "Waist" to "${fmt(it)} cm" },
        m.hipCm?.let { "Hip" to "${fmt(it)} cm" },
        m.chestCm?.let { "Chest" to "${fmt(it)} cm" },
        m.armCm?.let { "Arm" to "${fmt(it)} cm" },
        m.thighCm?.let { "Thigh" to "${fmt(it)} cm" },
        m.calfCm?.let { "Calf" to "${fmt(it)} cm" }
    )
    if (pairs.isEmpty()) return
    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        pairs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    Stat(label, value, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)

@Composable
private fun rememberDateFormat(): DateFormat = androidx.compose.runtime.remember {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
}
