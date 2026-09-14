package com.bodymeasure.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.data.MeasurementBackup
import com.bodymeasure.app.data.MeasurementDatabase
import com.bodymeasure.app.data.MeasurementRepository
import com.bodymeasure.app.util.ActivityLevel
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.Bmr
import com.bodymeasure.app.util.BodyFat
import com.bodymeasure.app.util.Sex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** In-progress new entry. Kept here so it survives leaving the Record tab. */
    val newDraft = MeasurementDraft()

    /** Separate draft for editing, so starting an edit doesn't discard a new entry. */
    val editDraft = MeasurementDraft()

    /** Id of the entry currently being edited, or null when creating a new one. */
    var editingId by mutableStateOf<Long?>(null)
        private set

    fun startEdit(measurement: Measurement) {
        editDraft.loadFrom(measurement)
        editingId = measurement.id
    }

    fun stopEdit() {
        editingId = null
    }

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

    /** Writes all recorded history to [uri] as JSON. */
    fun exportTo(uri: Uri, onResult: (TransferResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val entries = repo.getAllOnce()
                    if (entries.isEmpty()) return@runCatching TransferResult.Empty
                    val json = MeasurementBackup.encode(entries)
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri, "wt")
                        ?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: return@runCatching TransferResult.Failed("Couldn't open that file for writing")
                    TransferResult.Exported(entries.size)
                }.getOrElse { TransferResult.Failed(it.message ?: "Export failed") }
            }
            onResult(result)
        }
    }

    /**
     * Reads a backup from [uri] and adds entries that aren't already present.
     * Existing entries are matched by timestamp, so re-importing the same file
     * is a no-op rather than creating duplicates.
     */
    fun importFrom(uri: Uri, onResult: (TransferResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = getApplication<Application>().contentResolver
                    val text = resolver.openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: return@runCatching TransferResult.Failed("Couldn't open that file")

                    val parsed = MeasurementBackup.decode(text)
                    val existing = repo.allTimestamps()
                    val fresh = parsed.filter { it.timestamp !in existing }
                    if (fresh.isNotEmpty()) repo.insertAll(fresh)
                    TransferResult.Imported(
                        added = fresh.size,
                        skipped = parsed.size - fresh.size
                    )
                }.getOrElse {
                    val message = when (it) {
                        is MeasurementBackup.InvalidBackupException ->
                            it.message ?: "That file isn't a valid backup"
                        else -> it.message ?: "Import failed"
                    }
                    TransferResult.Failed(message)
                }
            }
            onResult(result)
        }
    }
}

/** Outcome of an export or import, for surfacing in a snackbar. */
sealed interface TransferResult {
    data class Exported(val count: Int) : TransferResult
    data class Imported(val added: Int, val skipped: Int) : TransferResult
    data object Empty : TransferResult
    data class Failed(val message: String) : TransferResult
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
