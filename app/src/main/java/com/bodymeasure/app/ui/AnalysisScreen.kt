package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.R
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.util.BodyAnalysis
import com.bodymeasure.app.util.FfmiBand
import com.bodymeasure.app.util.RiskLevel
import com.bodymeasure.app.util.Sex
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

private val Good = Color(0xFF43A047)
private val Warn = Color(0xFFFB8C00)
private val Bad = Color(0xFFE53935)
private val Info = Color(0xFF42A5F5)
private val Strong = Color(0xFF26A69A)

@Composable
fun AnalysisScreen(items: List<Measurement>) {
    val latest = items.maxByOrNull { it.timestamp }
    if (latest == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.analysis_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val sex = runCatching { Sex.valueOf(latest.sex) }.getOrDefault(Sex.Male)
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    val leanMass = BodyAnalysis.leanMassKg(latest.weightKg, latest.bodyFatPct)
    val fatMass = BodyAnalysis.fatMassKg(latest.weightKg, latest.bodyFatPct)
    val leanPct = BodyAnalysis.leanPercent(latest.bodyFatPct)
    val ffmi = BodyAnalysis.normalizedFfmi(latest.weightKg, latest.heightCm, latest.bodyFatPct)
    val whtr = BodyAnalysis.waistToHeight(latest.waistCm, latest.heightCm)
    val whr = BodyAnalysis.waistToHip(latest.waistCm, latest.hipCm)
    val healthy = BodyAnalysis.healthyWeightRangeKg(latest.heightCm)
    val delta = BodyAnalysis.weightDeltaToHealthyKg(latest.weightKg, latest.heightCm)

    // Body fat is the gate for lean mass and FFMI, so say what unlocks it.
    val bodyFatNeeds = if (sex == Sex.Female) "neck, waist and hip" else "neck and waist"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.analysis_based_on, df.format(Date(latest.timestamp))),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---- Body composition ----
        AnalysisCard(
            title = stringResource(R.string.analysis_composition),
            help = stringResource(R.string.analysis_lean_mass_help),
            missing = if (leanMass == null) bodyFatNeeds else null
        ) {
            if (leanMass != null && fatMass != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricPair(
                        label = stringResource(R.string.analysis_lean_mass),
                        value = "${BodyAnalysis.format1(leanMass)} kg",
                        sub = leanPct?.let { "${BodyAnalysis.format1(it)}% of body weight" },
                        color = Strong,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(modifier = Modifier.height(60.dp))
                    MetricPair(
                        label = stringResource(R.string.analysis_fat_mass),
                        value = "${BodyAnalysis.format1(fatMass)} kg",
                        sub = latest.bodyFatPct?.let { "${BodyAnalysis.format1(it)}% of body weight" },
                        color = Warn,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- FFMI ----
        val ffmiBand = ffmi?.let { BodyAnalysis.ffmiBand(sex, it) }
        val (ffmiLabel, ffmiColor) = when (ffmiBand) {
            FfmiBand.BelowAverage -> stringResource(R.string.ffmi_below_average) to Info
            FfmiBand.Average -> stringResource(R.string.ffmi_average) to Info
            FfmiBand.AboveAverage -> stringResource(R.string.ffmi_above_average) to Good
            FfmiBand.Athletic -> stringResource(R.string.ffmi_athletic) to Strong
            FfmiBand.Exceptional -> stringResource(R.string.ffmi_exceptional) to Strong
            null -> "" to Color.Unspecified
        }
        AnalysisCard(
            title = stringResource(R.string.analysis_ffmi_full),
            help = stringResource(R.string.analysis_ffmi_help),
            missing = if (ffmi == null) bodyFatNeeds else null
        ) {
            if (ffmi != null) {
                BigValue(
                    value = BodyAnalysis.format1(ffmi),
                    caption = ffmiLabel,
                    color = ffmiColor
                )
            }
        }

        // ---- Waist to height ----
        val whtrRisk = whtr?.let { BodyAnalysis.waistToHeightRisk(it) }
        AnalysisCard(
            title = stringResource(R.string.analysis_whtr),
            help = stringResource(R.string.analysis_whtr_help),
            missing = if (whtr == null) "waist" else null
        ) {
            if (whtr != null && whtrRisk != null) {
                BigValue(
                    value = BodyAnalysis.format2(whtr),
                    caption = riskLabel(whtrRisk),
                    color = riskColor(whtrRisk)
                )
            }
        }

        // ---- Waist to hip ----
        val whrRisk = whr?.let { BodyAnalysis.waistToHipRisk(sex, it) }
        AnalysisCard(
            title = stringResource(R.string.analysis_whr),
            help = stringResource(R.string.analysis_whr_help),
            missing = if (whr == null) "waist and hip" else null
        ) {
            if (whr != null && whrRisk != null) {
                BigValue(
                    value = BodyAnalysis.format2(whr),
                    caption = riskLabel(whrRisk),
                    color = riskColor(whrRisk)
                )
            }
        }

        // ---- Healthy weight range ----
        AnalysisCard(
            title = stringResource(R.string.analysis_healthy_weight),
            help = stringResource(R.string.analysis_healthy_weight_help),
            missing = if (healthy == null) "height" else null
        ) {
            if (healthy != null) {
                val (lo, hi) = healthy
                BigValue(
                    value = "${BodyAnalysis.format1(lo)}–${BodyAnalysis.format1(hi)} kg",
                    caption = when {
                        delta == null -> stringResource(R.string.analysis_within_range)
                        delta > 0 -> stringResource(
                            R.string.analysis_above_range, BodyAnalysis.format1(delta)
                        )
                        else -> stringResource(
                            R.string.analysis_below_range, BodyAnalysis.format1(abs(delta))
                        )
                    },
                    color = if (delta == null) Good else Warn
                )
            }
        }

        Text(
            text = stringResource(R.string.analysis_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun AnalysisCard(
    title: String,
    help: String,
    missing: String?,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (missing != null) {
                Text(
                    text = stringResource(R.string.analysis_needs, missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Box(modifier = Modifier.padding(top = 8.dp)) { content() }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Text(
                help,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BigValue(value: String, caption: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        if (caption.isNotBlank()) {
            Text(caption, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun MetricPair(
    label: String,
    value: String,
    sub: String?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        if (sub != null) {
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun riskLabel(level: RiskLevel): String = stringResource(
    when (level) {
        RiskLevel.Low -> R.string.risk_low
        RiskLevel.Moderate -> R.string.risk_moderate
        RiskLevel.High -> R.string.risk_high
    }
)

private fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.Low -> Good
    RiskLevel.Moderate -> Warn
    RiskLevel.High -> Bad
}
