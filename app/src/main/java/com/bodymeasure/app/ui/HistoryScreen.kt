package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.R
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.BmiCategory
import com.bodymeasure.app.util.BodyFat
import com.bodymeasure.app.util.BodyFatCategory
import com.bodymeasure.app.util.Sex
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    items: List<Measurement>,
    onDelete: (Long) -> Unit
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
            MeasurementRow(m, onDelete = { onDelete(m.id) })
        }
    }
}

@Composable
private fun MeasurementRow(m: Measurement, onDelete: () -> Unit) {
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

    val df = rememberDateFormat()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(df.format(Date(m.timestamp)), style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "BMI ${Bmi.format(m.bmi)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = catColor
                        )
                        Text(
                            "  $catLabel",
                            style = MaterialTheme.typography.labelLarge,
                            color = catColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (bf != null) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "BF ${BodyFat.format(bf)}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = bfColor
                            )
                            Text(
                                "  $bfLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = bfColor,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Stat("Weight", "${fmt(m.weightKg)} kg", modifier = Modifier.weight(1f))
                Stat("Height", "${fmt(m.heightCm)} cm", modifier = Modifier.weight(1f))
            }
            FlowStats(m)
        }
    }
}

@Composable
private fun FlowStats(m: Measurement) {
    val pairs = listOfNotNull(
        m.neckCm?.let { "Neck" to "${fmt(it)} cm" },
        m.waistCm?.let { "Waist" to "${fmt(it)} cm" },
        m.hipCm?.let { "Hip" to "${fmt(it)} cm" },
        m.chestCm?.let { "Chest" to "${fmt(it)} cm" },
        m.armCm?.let { "Arm" to "${fmt(it)} cm" },
        m.thighCm?.let { "Thigh" to "${fmt(it)} cm" }
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
