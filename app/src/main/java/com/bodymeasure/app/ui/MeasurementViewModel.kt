package com.bodymeasure.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.data.MeasurementDatabase
import com.bodymeasure.app.data.MeasurementRepository
import com.bodymeasure.app.util.Bmi
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

class MeasurementViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: MeasurementRepository =
        MeasurementRepository(MeasurementDatabase.get(app).measurementDao())

    val history: StateFlow<List<Measurement>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latest: StateFlow<Measurement?> = repo.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(
        sex: Sex,
        weightKg: Double,
        heightCm: Double,
        waist: Double?,
        arm: Double?,
        chest: Double?,
        hip: Double?,
        thigh: Double?,
        neck: Double?,
        onResult: (SaveResult) -> Unit
    ) {
        if (weightKg <= 0 || heightCm <= 0) {
            onResult(SaveResult.Error("Weight and height must be positive"))
            return
        }
        val bmi = Bmi.calculate(weightKg, heightCm)
        val bodyFat = BodyFat.calculate(
            sex = sex,
            heightCm = heightCm,
            neckCm = neck,
            waistCm = waist,
            hipCm = hip
        )
        val entry = Measurement(
            sex = sex.name,
            weightKg = weightKg,
            heightCm = heightCm,
            waistCm = waist,
            armCm = arm,
            chestCm = chest,
            hipCm = hip,
            thighCm = thigh,
            neckCm = neck,
            bmi = bmi,
            bodyFatPct = bodyFat
        )
        viewModelScope.launch {
            repo.save(entry)
            onResult(SaveResult.Success)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}
