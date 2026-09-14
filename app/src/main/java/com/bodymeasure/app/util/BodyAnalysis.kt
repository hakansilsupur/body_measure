package com.bodymeasure.app.util

import kotlin.math.roundToInt

enum class RiskLevel { Low, Moderate, High }

enum class FfmiBand { BelowAverage, Average, AboveAverage, Athletic, Exceptional }

/**
 * Metrics derived from what the user already records. Everything here is a pure
 * function of stored fields, so nothing extra is persisted and old entries gain
 * these numbers retroactively.
 *
 * Deliberately excluded: anything claiming to isolate *muscle* mass. Separating
 * muscle from bone, organs and water needs skinfold calipers, DEXA or BIA — a
 * tape measure cannot do it. Lean mass and FFMI below are the honest ceiling of
 * what these inputs support.
 */
object BodyAnalysis {

    /** Everything that isn't fat: muscle, bone, organs, water. */
    fun leanMassKg(weightKg: Double, bodyFatPct: Double?): Double? {
        if (bodyFatPct == null || weightKg <= 0) return null
        if (bodyFatPct <= 0 || bodyFatPct >= 100) return null
        return weightKg * (1.0 - bodyFatPct / 100.0)
    }

    fun fatMassKg(weightKg: Double, bodyFatPct: Double?): Double? {
        val lean = leanMassKg(weightKg, bodyFatPct) ?: return null
        return weightKg - lean
    }

    fun leanPercent(bodyFatPct: Double?): Double? {
        if (bodyFatPct == null || bodyFatPct <= 0 || bodyFatPct >= 100) return null
        return 100.0 - bodyFatPct
    }

    /**
     * Fat-free mass index, normalised to a 1.8 m frame (Kouri et al. 1995) so it
     * compares across heights. This is the closest legitimate stand-in for
     * "how muscular am I" from tape measurements.
     */
    fun normalizedFfmi(weightKg: Double, heightCm: Double, bodyFatPct: Double?): Double? {
        val lean = leanMassKg(weightKg, bodyFatPct) ?: return null
        if (heightCm <= 0) return null
        val hM = heightCm / 100.0
        val ffmi = lean / (hM * hM)
        val normalised = ffmi + 6.1 * (1.8 - hM)
        return if (normalised.isFinite() && normalised > 0) normalised else null
    }

    fun ffmiBand(sex: Sex, ffmi: Double): FfmiBand = when (sex) {
        Sex.Male -> when {
            ffmi < 18.0 -> FfmiBand.BelowAverage
            ffmi < 20.0 -> FfmiBand.Average
            ffmi < 22.0 -> FfmiBand.AboveAverage
            ffmi < 25.0 -> FfmiBand.Athletic
            else -> FfmiBand.Exceptional
        }
        Sex.Female -> when {
            ffmi < 14.0 -> FfmiBand.BelowAverage
            ffmi < 15.5 -> FfmiBand.Average
            ffmi < 17.0 -> FfmiBand.AboveAverage
            ffmi < 19.0 -> FfmiBand.Athletic
            else -> FfmiBand.Exceptional
        }
    }

    /** Waist ÷ height. Tracks central fat better than BMI and needs no sex input. */
    fun waistToHeight(waistCm: Double?, heightCm: Double): Double? {
        if (waistCm == null || waistCm <= 0 || heightCm <= 0) return null
        return waistCm / heightCm
    }

    /** Ashwell boundaries: keep your waist under half your height. */
    fun waistToHeightRisk(ratio: Double): RiskLevel = when {
        ratio < 0.5 -> RiskLevel.Low
        ratio < 0.6 -> RiskLevel.Moderate
        else -> RiskLevel.High
    }

    fun waistToHip(waistCm: Double?, hipCm: Double?): Double? {
        if (waistCm == null || hipCm == null) return null
        if (waistCm <= 0 || hipCm <= 0) return null
        return waistCm / hipCm
    }

    /** WHO cut-points for metabolic risk from fat distribution. */
    fun waistToHipRisk(sex: Sex, ratio: Double): RiskLevel = when (sex) {
        Sex.Male -> when {
            ratio < 0.90 -> RiskLevel.Low
            ratio < 1.00 -> RiskLevel.Moderate
            else -> RiskLevel.High
        }
        Sex.Female -> when {
            ratio < 0.80 -> RiskLevel.Low
            ratio < 0.85 -> RiskLevel.Moderate
            else -> RiskLevel.High
        }
    }

    /** Weight span corresponding to BMI 18.5–24.9 at this height. */
    fun healthyWeightRangeKg(heightCm: Double): Pair<Double, Double>? {
        if (heightCm <= 0) return null
        val hM = heightCm / 100.0
        return (18.5 * hM * hM) to (24.9 * hM * hM)
    }

    /** How far weight sits outside the healthy band; null when inside it. */
    fun weightDeltaToHealthyKg(weightKg: Double, heightCm: Double): Double? {
        val (lo, hi) = healthyWeightRangeKg(heightCm) ?: return null
        return when {
            weightKg < lo -> weightKg - lo
            weightKg > hi -> weightKg - hi
            else -> null
        }
    }

    fun format1(v: Double): String = "%.1f".format((v * 10).roundToInt() / 10.0)

    fun format2(v: Double): String = "%.2f".format(v)
}
