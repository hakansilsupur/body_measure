package com.bodymeasure.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sex: String = "Male",
    val ageYears: Int? = null,
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double? = null,
    val armCm: Double? = null,
    val chestCm: Double? = null,
    val hipCm: Double? = null,
    val thighCm: Double? = null,
    val neckCm: Double? = null,
    val bmi: Double,
    val bodyFatPct: Double? = null,
    val bmrKcal: Double? = null,
    val activityFactor: Double? = null
)
