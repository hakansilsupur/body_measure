package com.bodymeasure.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.util.ActivityLevel
import com.bodymeasure.app.util.Sex

/**
 * Mutable in-progress state of the Record form.
 *
 * Owned by the ViewModel rather than the composable, so a partially filled form
 * survives leaving the Record tab. Composable-local `rememberSaveable` is not
 * enough here: switching tabs removes the screen from the composition and its
 * remembered state goes with it.
 */
class MeasurementDraft {
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

    /** Clears the measurement fields, keeping sex and activity for the next entry. */
    fun clearMeasurements() {
        age = ""
        weight = ""
        height = ""
        neck = ""
        waist = ""
        hip = ""
        chest = ""
        arm = ""
        thigh = ""
    }

    /** Replaces all fields with the values of a stored entry, for editing. */
    fun loadFrom(m: Measurement) {
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
    }

    /** Builds the input for persistence, or null if weight/height aren't valid. */
    fun toInputOrNull(): MeasurementInput? {
        val w = weight.toDoubleOrNull() ?: return null
        val h = height.toDoubleOrNull() ?: return null
        if (w <= 0 || h <= 0) return null
        return MeasurementInput(
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
            neckCm = neck.toDoubleOrNull()
        )
    }
}

/** Formats a stored value back into an editable field string (drops trailing ".0"). */
internal fun Double?.toField(): String = when {
    this == null -> ""
    this % 1.0 == 0.0 -> toLong().toString()
    else -> toString()
}
