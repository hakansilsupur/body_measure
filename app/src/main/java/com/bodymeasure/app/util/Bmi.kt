package com.bodymeasure.app.util

import kotlin.math.roundToInt

enum class BmiCategory { Underweight, Normal, Overweight, Obese }

object Bmi {

    fun calculate(weightKg: Double, heightCm: Double): Double {
        require(weightKg > 0) { "Weight must be positive" }
        require(heightCm > 0) { "Height must be positive" }
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun categorize(bmi: Double): BmiCategory = when {
        bmi < 18.5 -> BmiCategory.Underweight
        bmi < 25.0 -> BmiCategory.Normal
        bmi < 30.0 -> BmiCategory.Overweight
        else -> BmiCategory.Obese
    }

    fun format(bmi: Double): String {
        val rounded = (bmi * 10).roundToInt() / 10.0
        return "%.1f".format(rounded)
    }
}
