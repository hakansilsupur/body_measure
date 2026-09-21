package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.bodymeasure.app.util.Bmr
import com.bodymeasure.app.util.BodyAnalysis
import com.bodymeasure.app.util.BodyFat

/** Matches the teal the Analysis tab uses for FFMI, so the two read as one metric. */
private val FfmiTeal = Color(0xFF26A69A)

private class ChartSpec(
    val title: String,
    val points: List<ChartPoint>,
    val color: Color,
    val format: (Double) -> String
)

@Composable
fun TrendsScreen(items: List<Measurement>) {
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

    val needTwo = stringResource(R.string.chart_need_two)
    val cm: (Double) -> String = { "${BodyAnalysis.format1(it)} cm" }
    val kg: (Double) -> String = { "${BodyAnalysis.format1(it)} kg" }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    fun pointsOf(select: (Measurement) -> Double?): List<ChartPoint> =
        items.mapNotNull { m -> select(m)?.let { ChartPoint(m.timestamp, it) } }

    // Derived metrics first, then every circumference actually recorded.
    val charts = listOf(
        ChartSpec(stringResource(R.string.trends_bmi), pointsOf { it.bmi }, primary) { Bmi.format(it) },
        ChartSpec(stringResource(R.string.trends_weight), pointsOf { it.weightKg }, primary, kg),
        ChartSpec(stringResource(R.string.trends_body_fat), pointsOf { it.bodyFatPct }, secondary) { "${BodyFat.format(it)}%" },
        // FFMI is derived rather than stored, so it appears for every past entry
        // that has a body fat figure without needing a migration.
        ChartSpec(
            stringResource(R.string.trends_ffmi),
            pointsOf { BodyAnalysis.normalizedFfmi(it.weightKg, it.heightCm, it.bodyFatPct) },
            FfmiTeal,
            BodyAnalysis::format1
        ),
        ChartSpec(stringResource(R.string.trends_bmr), pointsOf { it.bmrKcal }, tertiary) { Bmr.format(it) },
        ChartSpec(stringResource(R.string.trends_waist), pointsOf { it.waistCm }, secondary, cm),
        ChartSpec(stringResource(R.string.trends_chest), pointsOf { it.chestCm }, tertiary, cm),
        ChartSpec(stringResource(R.string.trends_arm), pointsOf { it.armCm }, primary, cm),
        ChartSpec(stringResource(R.string.trends_thigh), pointsOf { it.thighCm }, secondary, cm),
        ChartSpec(stringResource(R.string.trends_calf), pointsOf { it.calfCm }, primary, cm),
        ChartSpec(stringResource(R.string.trends_wrist), pointsOf { it.wristCm }, tertiary, cm),
        ChartSpec(stringResource(R.string.trends_hip), pointsOf { it.hipCm }, tertiary, cm),
        ChartSpec(stringResource(R.string.trends_neck), pointsOf { it.neckCm }, primary, cm)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Skip measurements never recorded, rather than showing an empty card for
        // every field the user doesn't track.
        charts.filter { it.points.isNotEmpty() }.forEach { spec ->
            LineChart(
                title = spec.title,
                points = spec.points,
                color = spec.color,
                formatValue = spec.format,
                needTwoMessage = needTwo
            )
        }
    }
}
