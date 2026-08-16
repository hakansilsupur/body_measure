package com.bodymeasure.app.util

import kotlin.math.log10
import kotlin.math.roundToInt

enum class Sex { Male, Female }

enum class BodyFatCategory { Essential, Athletes, Fitness, Average, Obese }

object BodyFat {

    /**
     * U.S. Navy body-fat estimate. Returns percent (e.g. 18.4) or null when
     * required measurements are missing/non-positive or yield a non-finite result.
     *
     * Required: heightCm, neckCm, waistCm. Female also requires hipCm.
     */
    fun calculate(
        sex: Sex,
        heightCm: Double,
        neckCm: Double?,
        waistCm: Double?,
        hipCm: Double? = null
    ): Double? {
        if (heightCm <= 0) return null
        if (neckCm == null || neckCm <= 0) return null
        if (waistCm == null || waistCm <= 0) return null

        val bf = when (sex) {
            Sex.Male -> {
                val diff = waistCm - neckCm
                if (diff <= 0) return null
                495.0 / (1.0324 - 0.19077 * log10(diff) + 0.15456 * log10(heightCm)) - 450.0
            }
            Sex.Female -> {
                if (hipCm == null || hipCm <= 0) return null
                val diff = waistCm + hipCm - neckCm
                if (diff <= 0) return null
                495.0 / (1.29579 - 0.35004 * log10(diff) + 0.22100 * log10(heightCm)) - 450.0
            }
        }
        return if (bf.isFinite() && bf > 0) bf else null
    }

    /** ACE body-fat ranges. */
    fun categorize(sex: Sex, bodyFatPct: Double): BodyFatCategory = when (sex) {
        Sex.Male -> when {
            bodyFatPct < 6 -> BodyFatCategory.Essential
            bodyFatPct < 14 -> BodyFatCategory.Athletes
            bodyFatPct < 18 -> BodyFatCategory.Fitness
            bodyFatPct < 25 -> BodyFatCategory.Average
            else -> BodyFatCategory.Obese
        }
        Sex.Female -> when {
            bodyFatPct < 14 -> BodyFatCategory.Essential
            bodyFatPct < 21 -> BodyFatCategory.Athletes
            bodyFatPct < 25 -> BodyFatCategory.Fitness
            bodyFatPct < 32 -> BodyFatCategory.Average
            else -> BodyFatCategory.Obese
        }
    }

    fun format(bodyFatPct: Double): String {
        val rounded = (bodyFatPct * 10).roundToInt() / 10.0
        return "%.1f".format(rounded)
    }
}
