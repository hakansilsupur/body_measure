package com.bodymeasure.app.util

import kotlin.math.roundToInt

enum class RiskLevel { Low, Moderate, High }

/**
 * Torso taper ratios worth showing, each with the value McCallum's classical
 * figures imply for it.
 *
 * Deliberately short. Arm-to-waist and thigh-to-waist were dropped: they have
 * no established reference to compare against, and because they share the waist
 * denominator with chest-to-waist they simply restate it — three rows moving in
 * lockstep read as a pattern when they are one number.
 */
enum class TaperRatio(val label: String, val classic: Double) {
    /**
     * The "Adonis index": shoulder circumference over waist, against the golden
     * ratio. Like McCallum's figures it is an aesthetic convention popularised
     * in fitness writing, not a health measure — and McCallum's own set has no
     * shoulder site, which is why this one carries its own reference.
     */
    ShoulderToWaist("Shoulder / waist", 1.618),

    /** The V-taper. Classical waist is 70% of chest, so the ratio is 1/0.70. */
    ChestToWaist("Chest / waist", 1.0 / 0.70),

    /** Classical hip is 85% of chest. */
    ChestToHip("Chest / hip", 1.0 / 0.85)
}

enum class FfmiBand { BelowAverage, Average, AboveAverage, Athletic, Exceptional }

/**
 * Metrics derived from what the user already records. Everything here is a pure
 * function of stored fields, so nothing extra is persisted and old entries gain
 * these numbers retroactively.
 *
 * Muscle is the one figure here that does not come from *your* tape
 * measurements. Separating muscle from bone, organs and water needs DEXA, MRI
 * or BIA; the tape can only split fat from everything else (lean mass, FFMI).
 * [skeletalMuscleKg] is instead a population equation: what MRI found for
 * people of your sex, age, height and weight. Useful as a ballpark, and
 * labelled as one everywhere it appears.
 */
object BodyAnalysis {

    /** Typical error of [skeletalMuscleKg] against MRI (Lee et al. 2000). */
    const val MUSCLE_SEE_KG = 2.8

    /**
     * Average skeletal muscle as a share of body weight, measured by whole-body
     * MRI in 468 adults aged 18-88 (Janssen et al., J Appl Physiol 2000).
     * A reference point, not a target: it falls with age in both sexes.
     */
    fun averageMusclePercent(sex: Sex): Double = if (sex == Sex.Male) 38.4 else 30.6

    /**
     * Approximate whole-body skeletal muscle, kg — the Lee et al. (2000)
     * anthropometric equation, validated against MRI (R^2 0.86, SEE 2.8 kg):
     *
     *   SM = 0.244 x weight + 7.80 x height(m) + 6.6 x sex - 0.098 x age + race - 3.3
     *
     * with sex 1 for men, 0 for women. The race term is -1.2 for Asian and +1.4
     * for African American adults, 0 otherwise; the app does not ask, so it
     * uses 0, which adds roughly that much to the error for those groups.
     *
     * Its blind spot matters more than its error bar: it cannot see body
     * composition. Two people of the same sex, age, height and weight get the
     * same answer whatever they are made of, and gaining a kilo of fat raises
     * the estimate exactly as much as a kilo of muscle would. So it is a
     * one-off ballpark, never a trend — which is why it is not charted and has
     * no "change since last entry". FFMI, which does use your measurements, is
     * the figure to track.
     *
     * Null without an age, since age is a term in the equation.
     */
    fun skeletalMuscleKg(sex: Sex, ageYears: Int?, weightKg: Double, heightCm: Double): Double? {
        if (ageYears == null || ageYears <= 0 || weightKg <= 0 || heightCm <= 0) return null
        val sexTerm = if (sex == Sex.Male) 1.0 else 0.0
        val sm = 0.244 * weightKg + 7.80 * (heightCm / 100.0) + 6.6 * sexTerm -
            0.098 * ageYears - 3.3
        // Outside the equation's sensible range (extreme inputs) say nothing
        // rather than print a negative or more-than-bodyweight muscle mass.
        return sm.takeIf { it.isFinite() && it > 0 && it < weightKg }
    }

    fun musclePercent(muscleKg: Double, weightKg: Double): Double? =
        if (weightKg > 0) muscleKg / weightKg * 100.0 else null

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

    /** Generic circumference ratio, null unless both values are present and positive. */
    fun ratio(numerator: Double?, denominator: Double?): Double? {
        if (numerator == null || denominator == null) return null
        if (numerator <= 0 || denominator <= 0) return null
        val r = numerator / denominator
        return if (r.isFinite()) r else null
    }

    /**
     * Copenhagen City Heart Study (Heitmann & Frederiksen, BMJ 2009) found thigh
     * circumference below roughly 60 cm associated with higher cardiovascular
     * risk, independent of fat mass. Below 50 cm the association was strongest.
     */
    fun thighRisk(thighCm: Double?): RiskLevel? = when {
        thighCm == null || thighCm <= 0 -> null
        thighCm >= 60.0 -> RiskLevel.Low
        thighCm >= 50.0 -> RiskLevel.Moderate
        else -> RiskLevel.High
    }

    /** The taper ratio for [kind], or null when a needed measurement is absent. */
    fun taper(
        kind: TaperRatio,
        chestCm: Double?,
        waistCm: Double?,
        hipCm: Double?,
        shoulderCm: Double?
    ): Double? =
        when (kind) {
            TaperRatio.ShoulderToWaist -> ratio(shoulderCm, waistCm)
            TaperRatio.ChestToWaist -> ratio(chestCm, waistCm)
            TaperRatio.ChestToHip -> ratio(chestCm, hipCm)
        }

    /** Signed two-decimal change, e.g. "+0.04" / "-0.03" / "0.00". */
    fun signed2(v: Double): String {
        val rounded = (v * 100).roundToInt() / 100.0
        return if (rounded > 0) "+%.2f".format(rounded) else "%.2f".format(rounded)
    }

    fun format1(v: Double): String = "%.1f".format((v * 10).roundToInt() / 10.0)

    fun format2(v: Double): String = "%.2f".format(v)
}
