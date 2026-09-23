package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.R
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.util.AnchorRange
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.BodyAnalysis
import com.bodymeasure.app.util.BodyFat
import com.bodymeasure.app.util.ClassicalProportions
import com.bodymeasure.app.util.ProportionRow
import com.bodymeasure.app.util.RangeScale
import com.bodymeasure.app.util.ReferenceRanges
import com.bodymeasure.app.util.FfmiBand
import com.bodymeasure.app.util.RiskLevel
import com.bodymeasure.app.util.Sex
import com.bodymeasure.app.util.TaperRatio
import com.bodymeasure.app.util.WaistReference
import com.bodymeasure.app.util.WaistRisk
import com.bodymeasure.app.util.ZoneTone
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

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

    // The entry before the latest one, used to show direction rather than a
    // bare number. Null on the very first entry.
    val previous = remember(items) {
        items.sortedBy { it.timestamp }.let { it.getOrNull(it.size - 2) }
    }

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
        // BMI needs only weight and height, which every entry has, so the card
        // always has something to show. Body fat and FFMI join it once the
        // tape measurements that unlock body fat are recorded.
        val ffmiBand = ffmi?.let { BodyAnalysis.ffmiBand(sex, it) }
        val ffmiLabel = when (ffmiBand) {
            FfmiBand.BelowAverage -> stringResource(R.string.ffmi_below_average)
            FfmiBand.Average -> stringResource(R.string.ffmi_average)
            FfmiBand.AboveAverage -> stringResource(R.string.ffmi_above_average)
            FfmiBand.Athletic -> stringResource(R.string.ffmi_athletic)
            FfmiBand.Exceptional -> stringResource(R.string.ffmi_exceptional)
            null -> ""
        }
        AnalysisCard(
            title = stringResource(R.string.analysis_composition),
            help = stringResource(R.string.analysis_composition_help),
            missing = null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
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

                RangeBar(
                    label = stringResource(R.string.bmi),
                    valueText = Bmi.format(latest.bmi),
                    caption = null,
                    value = latest.bmi,
                    scale = ReferenceRanges.bmi()
                )

                val bf = latest.bodyFatPct
                if (bf != null) {
                    RangeBar(
                        label = stringResource(R.string.body_fat),
                        valueText = "${BodyFat.format(bf)}%",
                        caption = null,
                        value = bf,
                        scale = ReferenceRanges.bodyFat(sex)
                    )
                }

                if (ffmi != null) {
                    RangeBar(
                        label = stringResource(R.string.analysis_ffmi),
                        valueText = BodyAnalysis.format1(ffmi),
                        // The bar lumps above-average, athletic and exceptional
                        // into one zone; the caption keeps the finer band.
                        caption = ffmiLabel,
                        value = ffmi,
                        scale = ReferenceRanges.ffmi(sex)
                    )
                }

                if (bf == null) {
                    Text(
                        stringResource(R.string.analysis_composition_needs, bodyFatNeeds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
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

        // ---- Waist against health thresholds and the population ----
        val waist = latest.waistCm?.takeIf { it > 0 }
        val waistRisk = waist?.let { WaistReference.risk(sex, it) }
        AnalysisCard(
            title = stringResource(R.string.analysis_waist_context),
            help = stringResource(R.string.analysis_waist_context_help),
            missing = if (waist == null) "waist" else null
        ) {
            if (waist != null && waistRisk != null) {
                val riskColour = when (waistRisk) {
                    WaistRisk.Low -> Good
                    WaistRisk.Increased -> Warn
                    WaistRisk.High -> Bad
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigValue(
                        value = "${BodyAnalysis.format1(waist)} cm",
                        caption = when (waistRisk) {
                            WaistRisk.Low -> stringResource(R.string.risk_low)
                            WaistRisk.Increased -> stringResource(R.string.risk_moderate)
                            WaistRisk.High -> stringResource(R.string.risk_high)
                        },
                        color = riskColour
                    )
                    WaistScale(sex = sex, waistCm = waist)
                    Text(
                        stringResource(
                            R.string.analysis_waist_who,
                            BodyAnalysis.format1(WaistReference.increasedAt(sex)),
                            BodyAnalysis.format1(WaistReference.highAt(sex))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val (lo, hi) = WaistReference.typicalRange(sex)
                    Text(
                        stringResource(
                            R.string.analysis_waist_typical,
                            BodyAnalysis.format1(lo), BodyAnalysis.format1(hi)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (WaistReference.typicalIsAboveThreshold(sex)) {
                        Text(
                            stringResource(R.string.analysis_waist_average_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

        // ---- Thigh circumference ----
        val thighRisk = BodyAnalysis.thighRisk(latest.thighCm)
        AnalysisCard(
            title = stringResource(R.string.analysis_thigh_size),
            help = stringResource(R.string.analysis_thigh_help),
            missing = if (thighRisk == null) "thigh" else null
        ) {
            if (latest.thighCm != null && thighRisk != null) {
                BigValue(
                    value = "${BodyAnalysis.format1(latest.thighCm)} cm",
                    caption = riskLabel(thighRisk),
                    color = riskColor(thighRisk)
                )
            }
        }

        // ---- Taper (chest against waist and hip) ----
        // Each row carries its own reference and its own direction, so a bare
        // ratio never has to be interpreted in a vacuum.
        val taperRows = TaperRatio.entries.mapNotNull { kind ->
            val now = BodyAnalysis.taper(kind, latest.chestCm, latest.waistCm, latest.hipCm)
                ?: return@mapNotNull null
            val before = previous?.let {
                BodyAnalysis.taper(kind, it.chestCm, it.waistCm, it.hipCm)
            }
            Triple(kind, now, before)
        }

        AnalysisCard(
            title = stringResource(R.string.analysis_proportions),
            help = stringResource(R.string.analysis_proportions_help),
            missing = if (taperRows.isEmpty())
                stringResource(R.string.analysis_proportions_needs) else null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                taperRows.forEach { (kind, now, before) ->
                    TaperRow(
                        kind = kind,
                        value = now,
                        previous = before,
                        previousDate = previous?.let { df.format(Date(it.timestamp)) }
                    )
                }
                Text(
                    stringResource(R.string.analysis_classic_marker),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // ---- Classical proportions ----
        val classicAnchor = ClassicalProportions.anchor(latest.wristCm, latest.chestCm)
        val classicRows = classicAnchor?.let {
            ClassicalProportions.rows(
                anchor = it,
                chestCm = latest.chestCm,
                neckCm = latest.neckCm,
                armCm = latest.armCm,
                waistCm = latest.waistCm,
                hipCm = latest.hipCm,
                thighCm = latest.thighCm,
                calfCm = latest.calfCm
            )
        } ?: emptyList()
        AnalysisCard(
            title = stringResource(R.string.analysis_classic),
            help = stringResource(R.string.analysis_classic_help),
            missing = if (classicRows.isEmpty())
                stringResource(R.string.analysis_classic_needs) else null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (val a = classicAnchor) {
                    is ClassicalProportions.Anchor.FromWrist -> {
                        Text(
                            stringResource(
                                R.string.analysis_classic_anchor_wrist,
                                BodyAnalysis.format1(a.wristCm),
                                BodyAnalysis.format1(a.chestCm)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // A wrist outside McCallum's span moves every row the same
                        // way at once, so say so before the rows rather than let
                        // seven agreeing bars read as a verdict.
                        val warning = when (ClassicalProportions.wristRange(a.wristCm)) {
                            AnchorRange.Below -> R.string.analysis_classic_wrist_low
                            AnchorRange.Above -> R.string.analysis_classic_wrist_high
                            AnchorRange.Calibrated -> null
                        }
                        if (warning != null) {
                            Text(
                                stringResource(warning, BodyAnalysis.format1(a.wristCm)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Warn
                            )
                        }
                    }
                    is ClassicalProportions.Anchor.FromChest -> Text(
                        stringResource(
                            R.string.analysis_classic_anchor_chest,
                            BodyAnalysis.format1(a.chestCm)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    null -> Unit
                }
                classicRows.forEach { ClassicRow(it) }

                Text(
                    stringResource(R.string.analysis_classic_marker),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                ClassicalProportions.symmetrySpreadCm(
                    latest.neckCm, latest.armCm, latest.calfCm
                )?.let { spread ->
                    Text(
                        stringResource(
                            R.string.analysis_classic_symmetry, BodyAnalysis.format1(spread)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Change over time ----
        ProgressCard(items = items)

        Text(
            text = stringResource(R.string.analysis_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

/**
 * One taper ratio: the value, the classical figure beside it, the same bar the
 * classical card uses, and the change since the previous entry.
 *
 * The change is coloured by whether it moved *toward* the classical figure, not
 * by its sign. Someone already past the classical taper who goes further past it
 * is moving away from the reference, and colouring that green would be wrong.
 */
@Composable
private fun TaperRow(
    kind: TaperRatio,
    value: Double,
    previous: Double?,
    previousDate: String?
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(kind.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(
                    R.string.analysis_taper_value,
                    BodyAnalysis.format2(value),
                    BodyAnalysis.format2(kind.classic)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        ProportionBar(value / kind.classic)
        Spacer(Modifier.height(4.dp))
        if (previous != null && previousDate != null) {
            val change = value - previous
            val closer = abs(value - kind.classic) < abs(previous - kind.classic)
            Text(
                stringResource(
                    R.string.analysis_taper_change,
                    BodyAnalysis.signed2(change),
                    previousDate
                ),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    abs(change) < 0.005 -> MaterialTheme.colorScheme.outline
                    closer -> Good
                    else -> Warn
                }
            )
        } else {
            Text(
                stringResource(R.string.analysis_taper_single),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Per-measurement deltas against the previous entry and the very first one. */
@Composable
private fun ProgressCard(items: List<Measurement>) {
    val ordered = remember(items) { items.sortedBy { it.timestamp } }
    if (ordered.size < 2) {
        AnalysisCard(
            title = stringResource(R.string.analysis_progress),
            help = stringResource(R.string.analysis_progress_help),
            missing = null
        ) {
            Text(
                stringResource(R.string.analysis_progress_needs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    val current = ordered.last()
    val previous = ordered[ordered.size - 2]
    val first = ordered.first()

    val rows = listOf(
        Triple("Weight", "kg", Triple(current.weightKg, previous.weightKg, first.weightKg)),
        Triple("Waist", "cm", Triple(current.waistCm, previous.waistCm, first.waistCm)),
        Triple("Chest", "cm", Triple(current.chestCm, previous.chestCm, first.chestCm)),
        Triple("Arm", "cm", Triple(current.armCm, previous.armCm, first.armCm)),
        Triple("Thigh", "cm", Triple(current.thighCm, previous.thighCm, first.thighCm)),
        Triple("Calf", "cm", Triple(current.calfCm, previous.calfCm, first.calfCm)),
        Triple("Wrist", "cm", Triple(current.wristCm, previous.wristCm, first.wristCm)),
        Triple("Hip", "cm", Triple(current.hipCm, previous.hipCm, first.hipCm)),
        Triple("Neck", "cm", Triple(current.neckCm, previous.neckCm, first.neckCm)),
        Triple("Body fat", "%", Triple(current.bodyFatPct, previous.bodyFatPct, first.bodyFatPct))
    ).filter { it.third.first != null }

    AnalysisCard(
        title = stringResource(R.string.analysis_progress),
        help = stringResource(R.string.analysis_progress_help),
        missing = if (rows.isEmpty()) "any measurement" else null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1.4f))
                Label(stringResource(R.string.analysis_vs_previous), Modifier.weight(1f))
                Label(stringResource(R.string.analysis_vs_first), Modifier.weight(1f))
            }
            rows.forEach { (name, unit, values) ->
                val (cur, prev, firstVal) = values
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1.4f)) {
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${BodyAnalysis.format1(cur!!)} $unit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DeltaText(cur, prev, unit, Modifier.weight(1f))
                    DeltaText(cur, firstVal, unit, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Shows the signed change. Deliberately not colour-coded by direction: whether a
 * measurement should rise or fall depends entirely on the user's goal.
 */
@Composable
private fun DeltaText(current: Double?, baseline: Double?, unit: String, modifier: Modifier = Modifier) {
    val text = when {
        current == null || baseline == null -> "—"
        else -> {
            val d = current - baseline
            val rounded = (d * 10).roundToInt() / 10.0
            when {
                rounded > 0 -> "+${BodyAnalysis.format1(rounded)} $unit"
                rounded < 0 -> "${BodyAnalysis.format1(rounded)} $unit"
                else -> "±0 $unit"
            }
        }
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}


/**
 * One site against its classical figure. The bar is deliberately a single
 * neutral colour with no good/bad banding — these proportions are an aesthetic
 * convention, and colouring them would dress an opinion up as a verdict.
 */
@Composable
private fun ClassicRow(row: ProportionRow) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(row.site.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${BodyAnalysis.format1(row.actualCm)} cm  ·  classic " +
                    "${BodyAnalysis.format1(row.classicCm)} cm",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        ProportionBar(row.ratio)
    }
}

@Composable
private fun ProportionBar(ratio: Double) {
    // The scale runs to 1.5x the classical figure, so the tick sits two thirds
    // along and there is room to show overshoot without the bar pinning.
    val maxRatio = 1.5
    val fill = (ratio / maxRatio).coerceIn(0.02, 1.0).toFloat()
    val markerAt = (1.0 / maxRatio).toFloat()
    val track = MaterialTheme.colorScheme.surface
    val bar = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(12.dp)) {
        val full = maxWidth
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(track)
        )
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fill)
                .clip(RoundedCornerShape(6.dp)).background(bar)
        )
        Box(
            Modifier.fillMaxHeight().width(2.dp).offset(x = full * markerAt).background(tick)
        )
    }
}


/**
 * Waist on a fixed 60-130 cm scale: WHO risk zones as the track, the typical
 * population band bracketed above it, and the user's own value marked. Drawing
 * both on one axis is the point — it shows where "average" actually falls
 * relative to the health thresholds.
 */
@Composable
private fun WaistScale(sex: Sex, waistCm: Double) {
    val increased = WaistReference.increasedAt(sex)
    val high = WaistReference.highAt(sex)
    val (typLo, typHi) = WaistReference.typicalRange(sex)

    val pIncreased = WaistReference.scalePosition(increased)
    val pHigh = WaistReference.scalePosition(high)
    val pYou = WaistReference.scalePosition(waistCm)
    val pTypLo = WaistReference.scalePosition(typLo)
    val pTypHi = WaistReference.scalePosition(typHi)

    val onSurf = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val full = maxWidth
        Column {
            // Typical-population bracket, sitting above the risk track.
            Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                Box(
                    Modifier
                        .offset(x = full * pTypLo)
                        .width(full * (pTypHi - pTypLo))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(muted.copy(alpha = 0.55f))
                )
            }
            // WHO risk zones.
            Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                Row(Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp))) {
                    Box(Modifier.fillMaxHeight().weight(pIncreased).background(Good))
                    Box(Modifier.fillMaxHeight().weight(pHigh - pIncreased).background(Warn))
                    Box(Modifier.fillMaxHeight().weight(1f - pHigh).background(Bad))
                }
                // The user's own waist.
                Box(
                    Modifier
                        .offset(x = full * pYou - 1.5.dp)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(onSurf)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.analysis_waist_you),
                style = MaterialTheme.typography.labelSmall,
                color = onSurf,
                modifier = Modifier.offset(x = (full * pYou - 10.dp).coerceAtLeast(0.dp))
            )
        }
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

private fun toneColor(tone: ZoneTone, muted: Color): Color = when (tone) {
    ZoneTone.Low -> Info
    ZoneTone.Mid -> Good
    ZoneTone.High -> Warn
    ZoneTone.Alert -> Bad
    ZoneTone.Strong -> Strong
    ZoneTone.Muted -> muted
}

/**
 * A value placed on a banded reference scale: the zones as a segmented track,
 * a marker where the value sits, and each zone's name and bounds underneath.
 *
 * The zone the value falls in is drawn at full strength and its label bolded;
 * the rest are faded. That keeps the whole scale visible for context while making
 * it obvious at a glance which band applies — without a verdict word doing the
 * work the picture already does.
 */
@Composable
private fun RangeBar(
    label: String,
    valueText: String,
    caption: String?,
    value: Double,
    scale: RangeScale
) {
    val current = scale.zoneFor(value)
    val muted = MaterialTheme.colorScheme.outline
    val marker = MaterialTheme.colorScheme.onSurface
    val currentColor = toneColor(current.tone, muted)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valueText, style = MaterialTheme.typography.titleMedium, color = currentColor)
                Text(
                    "  ${caption?.takeIf { it.isNotBlank() } ?: current.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = currentColor
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val full = maxWidth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                scale.zones.forEach { zone ->
                    val w = scale.width(zone)
                    if (w > 0f) {
                        val base = toneColor(zone.tone, muted)
                        Box(
                            Modifier
                                .weight(w)
                                .fillMaxHeight()
                                .background(if (zone == current) base else base.copy(alpha = 0.3f))
                        )
                    }
                }
            }
            // Marker: a bar taller than the track, so it reads on any zone colour.
            Box(
                Modifier
                    .offset(x = (full * scale.position(value) - 2.dp).coerceIn(0.dp, full - 4.dp))
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(marker)
            )
        }
        Spacer(Modifier.height(2.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            scale.zones.forEach { zone ->
                val w = scale.width(zone)
                if (w > 0f) {
                    val here = zone == current
                    Column(
                        modifier = Modifier.weight(w),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            zone.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (here) FontWeight.Bold else FontWeight.Normal,
                            color = if (here) toneColor(zone.tone, muted)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            ReferenceRanges.rangeText(zone, scale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
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
