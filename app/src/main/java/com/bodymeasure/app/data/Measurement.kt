package com.bodymeasure.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double? = null,
    val armCm: Double? = null,
    val chestCm: Double? = null,
    val hipCm: Double? = null,
    val thighCm: Double? = null,
    val neckCm: Double? = null,
    val bmi: Double
)
