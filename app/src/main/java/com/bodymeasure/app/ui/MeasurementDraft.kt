package com.bodymeasure.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.util.ActivityLevel
import com.bodymeasure.app.util.Sex
import kotlin.math.roundToLong

/**
 * How the body-water field is read. Devices disagree: many scales show a
 * percentage of body weight, while Samsung Health (Galaxy Watch) shows
 * kilograms. Both are plausible numbers for the other unit — 48.8 kg of water
 * typed into a % field reads as a perfectly believable 48.8% — so the user
 * picks the unit explicitly rather than the app guessing.
 */
enum class WaterUnit { Kg, Percent }

/**
 * Mutable in-progress state of the Record form.
 *
 * Owned by the ViewModel rather than the composable, so a partially filled form
 * survives leaving the Record tab. Composable-local `rememberSaveable` is not
 * enough here: switching tabs removes the screen from the composition and its
 * remembered state goes with it.
 */
class MeasurementDraft {
    /** When the measurement was taken. Editable, so past sessions can be logged. */
    var timestamp by mutableStateOf(System.currentTimeMillis())
    var sex by mutableStateOf(Sex.Male)
    var activity by mutableStateOf<ActivityLevel?>(null)
    var age by mutableStateOf("")
    var weight by mutableStateOf("")
    var height by mutableStateOf("")
    var neck by mutableStateOf("")
    var waist by mutableStateOf("")
    var hip by mutableStateOf("")
    var chest by mutableStateOf("")
    var arm by mutableStateOf("")
    var thigh by mutableStateOf("")
    var calf by mutableStateOf("")
    var wrist by mutableStateOf("")
    var shoulder by mutableStateOf("")
    var bodyWater by mutableStateOf("")

    /**
     * Sticky across entries like sex and activity: people log from the same
     * device each time. Switching re-reads the typed number in the new unit
     * rather than converting it, because switching is how a wrong unit gets
     * corrected.
     */
    var bodyWaterUnit by mutableStateOf(WaterUnit.Kg)

    /** File name of the attached progress photo, or null when none is set. */
    var photoFileName by mutableStateOf<String?>(null)

    /**
     * Clears the measurement fields, keeping sex and activity for the next entry.
     * The date resets to today: carrying a back-dated value forward silently
     * would file the next entry under the wrong day.
     */
    fun clearMeasurements() {
        timestamp = System.currentTimeMillis()
        age = ""
        weight = ""
        height = ""
        neck = ""
        waist = ""
        hip = ""
        chest = ""
        arm = ""
        thigh = ""
        calf = ""
        wrist = ""
        shoulder = ""
        bodyWater = ""
        photoFileName = null
    }

    /** Replaces all fields with the values of a stored entry, for editing. */
    fun loadFrom(m: Measurement) {
        timestamp = m.timestamp
        sex = runCatching { Sex.valueOf(m.sex) }.getOrDefault(Sex.Male)
        activity = ActivityLevel.fromFactor(m.activityFactor)
        age = m.ageYears?.toString() ?: ""
        weight = m.weightKg.toField()
        height = m.heightCm.toField()
        neck = m.neckCm.toField()
        waist = m.waistCm.toField()
        hip = m.hipCm.toField()
        chest = m.chestCm.toField()
        arm = m.armCm.toField()
        thigh = m.thighCm.toField()
        calf = m.calfCm.toField()
        wrist = m.wristCm.toField()
        shoulder = m.shoulderCm.toField()
        // Stored as a percentage; shown in whichever unit the user works in.
        bodyWater = when (bodyWaterUnit) {
            WaterUnit.Percent -> m.bodyWaterPct.toField()
            WaterUnit.Kg -> m.bodyWaterPct
                ?.let { (it * m.weightKg / 100.0 * 10).roundToLong() / 10.0 }
                .toField()
        }
        photoFileName = m.photoFileName
    }

    /** Builds the input for persistence, or null if weight/height aren't valid. */
    fun toInputOrNull(): MeasurementInput? {
        val w = weight.toDoubleOrNull() ?: return null
        val h = height.toDoubleOrNull() ?: return null
        if (w <= 0 || h <= 0) return null
        return MeasurementInput(
            timestamp = timestamp,
            sex = sex,
            ageYears = age.toIntOrNull(),
            activity = activity,
            weightKg = w,
            heightCm = h,
            waistCm = waist.toDoubleOrNull(),
            armCm = arm.toDoubleOrNull(),
            chestCm = chest.toDoubleOrNull(),
            hipCm = hip.toDoubleOrNull(),
            thighCm = thigh.toDoubleOrNull(),
            calfCm = calf.toDoubleOrNull(),
            wristCm = wrist.toDoubleOrNull(),
            shoulderCm = shoulder.toDoubleOrNull(),
            bodyWaterPct = bodyWaterPercentOf(w),
            neckCm = neck.toDoubleOrNull(),
            photoFileName = photoFileName
        )
    }
}

/** Formats a stored value back into an editable field string (drops trailing ".0"). */
/**
 * The body-water field as a percentage of [weightKg], whatever unit it was
 * typed in. Storage is always a percentage, so entries stay comparable however
 * the user's devices report it.
 */
internal fun MeasurementDraft.bodyWaterPercentOf(weightKg: Double): Double? {
    val v = bodyWater.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
    return when (bodyWaterUnit) {
        WaterUnit.Percent -> v
        WaterUnit.Kg -> if (weightKg > 0) v / weightKg * 100.0 else null
    }
}

internal fun Double?.toField(): String = when {
    this == null -> ""
    this % 1.0 == 0.0 -> toLong().toString()
    else -> toString()
}
