package com.bodymeasure.app.util

/**
 * Health and population reference points for waist circumference.
 *
 * Two different kinds of number live here and they must not be confused:
 *
 *  - WHO cut-points are *health* thresholds for central adiposity.
 *  - The typical range is what people actually measure, which in most surveyed
 *    populations already sits at or above the WHO "increased risk" line.
 *
 * So being below average says nothing on its own — the card that renders this
 * states plainly that average is not a health target.
 *
 * WHO/IDF cut-points (Europid): men 94 / 102 cm, women 80 / 88 cm.
 * Typical ranges span national surveys (e.g. NHANES puts US adult men near
 * 101 cm, Health Survey for England nearer 97), so they are given as a band
 * rather than false-precision single figures.
 */
enum class WaistRisk { Low, Increased, High }

object WaistReference {

    /** Drawing bounds for the scale, wide enough to hold essentially any adult. */
    const val SCALE_MIN = 60.0
    const val SCALE_MAX = 130.0

    fun increasedAt(sex: Sex): Double = if (sex == Sex.Male) 94.0 else 80.0

    fun highAt(sex: Sex): Double = if (sex == Sex.Male) 102.0 else 88.0

    fun risk(sex: Sex, waistCm: Double): WaistRisk = when {
        waistCm < increasedAt(sex) -> WaistRisk.Low
        waistCm < highAt(sex) -> WaistRisk.Increased
        else -> WaistRisk.High
    }

    /** What adults in surveyed populations actually measure, as a band. */
    fun typicalRange(sex: Sex): Pair<Double, Double> =
        if (sex == Sex.Male) 95.0 to 102.0 else 89.0 to 98.0

    /** True when the typical range reaches the increased-risk line or beyond. */
    fun typicalIsAboveThreshold(sex: Sex): Boolean =
        typicalRange(sex).second >= increasedAt(sex)

    /** Position of [cm] along the drawn scale, clamped to 0..1. */
    fun scalePosition(cm: Double): Float =
        (((cm - SCALE_MIN) / (SCALE_MAX - SCALE_MIN)).coerceIn(0.0, 1.0)).toFloat()
}
