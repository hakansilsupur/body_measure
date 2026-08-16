package com.bodymeasure.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.data.MeasurementDatabase
import com.bodymeasure.app.data.MeasurementRepository
import com.bodymeasure.app.util.ActivityLevel
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.Bmr
import com.bodymeasure.app.util.BodyFat
import com.bodymeasure.app.util.Sex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SaveResult {
    data object Idle : SaveResult
    data object Success : SaveResult
    data class Error(val message: String) : SaveResult
}

/** All user-entered values from the Record form, before derived metrics. */
data class MeasurementInput(
    val sex: Sex,
    val ageYears: Int?,
    val activity: ActivityLevel?,
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double?,
    val armCm: Double?,
    val chestCm: Double?,
    val hipCm: Double?,
    val thighCm: Double?,
    val neckCm: Double?
)

class MeasurementViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: MeasurementRepository =
        MeasurementRepository(MeasurementDatabase.get(app).measurementDao())

    val history: StateFlow<List<Measurement>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latest: StateFlow<Measurement?> = repo.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Persist [input]. When [editingId] is null a new row is inserted; otherwise
     * the existing row is updated in place, preserving its original timestamp so
     * edits don't reorder history.
     */
    fun save(
        input: MeasurementInput,
        editingId: Long? = null,
        onResult: (SaveResult) -> Unit
    ) {
        if (input.weightKg <= 0 || input.heightCm <= 0) {
            onResult(SaveResult.Error("Weight and height must be positive"))
            return
        }
        viewModelScope.launch {
            val existing = editingId?.let { repo.getById(it) }
            if (editingId != null && existing == null) {
                onResult(SaveResult.Error("That entry no longer exists"))
                return@launch
            }
            val entry = input.toMeasurement(
                id = existing?.id ?: 0L,
                timestamp = existing?.timestamp ?: System.currentTimeMillis()
            )
            if (existing == null) repo.save(entry) else repo.update(entry)
            onResult(SaveResult.Success)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

private fun MeasurementInput.toMeasurement(id: Long, timestamp: Long): Measurement {
    val bmi = Bmi.calculate(weightKg, heightCm)
    val bodyFat = BodyFat.calculate(
        sex = sex,
        heightCm = heightCm,
        neckCm = neckCm,
        waistCm = waistCm,
        hipCm = hipCm
    )
    val bmr = Bmr.calculate(sex, weightKg, heightCm, ageYears)
    return Measurement(
        id = id,
        timestamp = timestamp,
        sex = sex.name,
        ageYears = ageYears,
        weightKg = weightKg,
        heightCm = heightCm,
        waistCm = waistCm,
        armCm = armCm,
        chestCm = chestCm,
        hipCm = hipCm,
        thighCm = thighCm,
        neckCm = neckCm,
        bmi = bmi,
        bodyFatPct = bodyFat,
        bmrKcal = bmr,
        activityFactor = activity?.factor
    )
}
