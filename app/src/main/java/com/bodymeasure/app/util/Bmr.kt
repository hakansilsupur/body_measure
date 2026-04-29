package com.bodymeasure.app.util

import kotlin.math.roundToInt

object Bmr {

    /**
     * Mifflin-St Jeor BMR (kcal/day). Returns null when any input is missing
     * or out of a sane range (age in 1..130 years).
     *
     *   Male:   10·kg + 6.25·cm − 5·years + 5
     *   Female: 10·kg + 6.25·cm − 5·years − 161
     */
    fun calculate(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int?): Double? {
        if (weightKg <= 0 || heightCm <= 0) return null
        if (ageYears == null || ageYears < 1 || ageYears > 130) return null
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        val bmr = when (sex) {
            Sex.Male -> base + 5.0
            Sex.Female -> base - 161.0
        }
        return if (bmr > 0 && bmr.isFinite()) bmr else null
    }

    fun format(kcal: Double): String = kcal.roundToInt().toString()
}
