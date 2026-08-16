package com.bodymeasure.app.util

import kotlin.math.roundToInt

enum class ActivityLevel(val factor: Double, val displayName: String, val description: String) {
    Sedentary(1.2, "Sedentary", "Desk job, no exercise"),
    Light(1.375, "Light", "1–3 workouts / week"),
    Moderate(1.55, "Moderate", "3–5 workouts / week"),
    Active(1.725, "Active", "6–7 workouts / week"),
    Extreme(1.9, "Extreme", "Hard daily training / physical job");

    companion object {
        fun fromFactor(factor: Double?): ActivityLevel? =
            factor?.let { f -> entries.firstOrNull { kotlin.math.abs(it.factor - f) < 0.001 } }
    }
}

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

    /** TDEE = BMR × activity factor. Null if either input is missing. */
    fun tdee(bmrKcal: Double?, activityFactor: Double?): Double? {
        if (bmrKcal == null || activityFactor == null || activityFactor <= 0) return null
        return bmrKcal * activityFactor
    }

    fun format(kcal: Double): String = kcal.roundToInt().toString()
}
