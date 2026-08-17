package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.R
import com.bodymeasure.app.util.ActivityLevel
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.BmiCategory
import com.bodymeasure.app.util.Bmr
import com.bodymeasure.app.util.BodyFat
import com.bodymeasure.app.util.BodyFatCategory
import com.bodymeasure.app.util.Sex

/**
 * Record form. State lives in [draft] (owned by the ViewModel) so a partially
 * filled form survives switching tabs. When [isEditing] is true, saving updates
 * the existing entry rather than inserting a new one.
 */
@Composable
fun AddMeasurementScreen(
    draft: MeasurementDraft,
    onSave: (input: MeasurementInput, editingId: Long?, onResult: (SaveResult) -> Unit) -> Unit,
    showMessage: (String) -> Unit,
    isEditing: Boolean = false,
    editingId: Long? = null,
    onCancelEdit: () -> Unit = {},
    onEditDone: () -> Unit = {}
) {
    var openGuide by rememberSaveable { mutableStateOf<MeasurementGuide?>(null) }

    openGuide?.let { guide ->
        MeasurementGuideDialog(guide = guide, onDismiss = { openGuide = null })
    }

    val sex = draft.sex
    val ageI = draft.age.toIntOrNull()
    val weightD = draft.weight.toDoubleOrNull()
    val heightD = draft.height.toDoubleOrNull()
    val waistD = draft.waist.toDoubleOrNull()
    val hipD = draft.hip.toDoubleOrNull()
    val neckD = draft.neck.toDoubleOrNull()

    val livePreviewBmi = if (weightD != null && heightD != null && weightD > 0 && heightD > 0)
        Bmi.calculate(weightD, heightD) else null
    val livePreviewBodyFat = if (heightD != null && heightD > 0)
        BodyFat.calculate(sex, heightD, neckD, waistD, hipD) else null
    val livePreviewBmr = if (weightD != null && heightD != null)
        Bmr.calculate(sex, weightD, heightD, ageI) else null
    val livePreviewTdee = Bmr.tdee(livePreviewBmr, draft.activity?.factor)

    val savedLabel = stringResource(R.string.saved)
    val updatedLabel = stringResource(R.string.updated)
    val errInvalid = stringResource(R.string.error_invalid_number)
    val errRequired = stringResource(R.string.error_required_weight_height)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditing) {
            EditingBanner(onCancel = onCancelEdit)
        }

        PreviewCard(
            bmi = livePreviewBmi,
            sex = sex,
            bodyFat = livePreviewBodyFat,
            bmrKcal = livePreviewBmr,
            tdeeKcal = livePreviewTdee
        )

        SexSelector(sex = draft.sex, onChange = { draft.sex = it })
        ActivitySelector(activity = draft.activity, onChange = { draft.activity = it })

        IntegerField(value = draft.age, onChange = { draft.age = it },
            label = stringResource(R.string.age_years))
        NumberField(value = draft.weight, onChange = { draft.weight = it },
            label = stringResource(R.string.weight_kg))
        NumberField(value = draft.height, onChange = { draft.height = it },
            label = stringResource(R.string.height_cm))
        NumberField(value = draft.neck, onChange = { draft.neck = it },
            label = stringResource(R.string.neck_cm),
            onInfoClick = { openGuide = MeasurementGuide.Neck })
        NumberField(value = draft.waist, onChange = { draft.waist = it },
            label = stringResource(R.string.waist_cm),
            onInfoClick = { openGuide = MeasurementGuide.Waist })
        NumberField(value = draft.hip, onChange = { draft.hip = it },
            label = stringResource(R.string.hip_cm),
            onInfoClick = { openGuide = MeasurementGuide.Hip })
        NumberField(value = draft.chest, onChange = { draft.chest = it },
            label = stringResource(R.string.chest_cm),
            onInfoClick = { openGuide = MeasurementGuide.Chest })
        NumberField(value = draft.arm, onChange = { draft.arm = it },
            label = stringResource(R.string.arm_cm),
            onInfoClick = { openGuide = MeasurementGuide.Arm })
        NumberField(value = draft.thigh, onChange = { draft.thigh = it },
            label = stringResource(R.string.thigh_cm),
            onInfoClick = { openGuide = MeasurementGuide.Thigh })

        Text(
            text = stringResource(
                if (sex == Sex.Female) R.string.bf_hint_female else R.string.bf_hint_male
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.bmr_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { if (isEditing) onCancelEdit() else draft.clearMeasurements() },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(if (isEditing) R.string.cancel else R.string.clear))
            }

            Button(
                onClick = {
                    val input = draft.toInputOrNull()
                    if (input == null) {
                        showMessage(
                            if (draft.weight.isBlank() || draft.height.isBlank()) errRequired
                            else errInvalid
                        )
                        return@Button
                    }
                    onSave(input, editingId) { result ->
                        when (result) {
                            is SaveResult.Success -> {
                                if (isEditing) {
                                    showMessage(updatedLabel)
                                    onEditDone()
                                } else {
                                    showMessage(savedLabel)
                                    draft.clearMeasurements()
                                }
                            }
                            is SaveResult.Error -> showMessage(result.message)
                            SaveResult.Idle -> Unit
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(if (isEditing) R.string.update else R.string.save))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EditingBanner(onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.editing_entry),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun SexSelector(sex: Sex, onChange: (Sex) -> Unit) {
    Column {
        Text(
            stringResource(R.string.sex),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = sex == Sex.Male,
                onClick = { onChange(Sex.Male) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.sex_male)) }
            SegmentedButton(
                selected = sex == Sex.Female,
                onClick = { onChange(Sex.Female) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.sex_female)) }
        }
    }
}

@Composable
private fun ActivitySelector(activity: ActivityLevel?, onChange: (ActivityLevel?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            stringResource(R.string.activity),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = activity?.let { "${it.displayName}  ×${it.factor}" }
                        ?: stringResource(R.string.activity_none),
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.activity_none)) },
                    onClick = { onChange(null); expanded = false }
                )
                ActivityLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("${level.displayName}  ×${level.factor}")
                                Text(
                                    level.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { onChange(level); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    onInfoClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val sanitized = input.replace(',', '.').filter { it.isDigit() || it == '.' }
            val singleDot = sanitized.indexOf('.').let { first ->
                if (first == -1) sanitized
                else sanitized.substring(0, first + 1) +
                        sanitized.substring(first + 1).replace(".", "")
            }
            onChange(singleDot)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = onInfoClick?.let { handler ->
            {
                IconButton(onClick = handler) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.how_to_measure)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun IntegerField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onChange(input.filter { it.isDigit() }.take(3)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PreviewCard(
    bmi: Double?,
    sex: Sex,
    bodyFat: Double?,
    bmrKcal: Double?,
    tdeeKcal: Double?
) {
    val bmiCat = bmi?.let { Bmi.categorize(it) }
    val (bmiLabel, bmiColor) = when (bmiCat) {
        BmiCategory.Underweight -> stringResource(R.string.bmi_underweight) to Color(0xFF42A5F5)
        BmiCategory.Normal -> stringResource(R.string.bmi_normal) to Color(0xFF43A047)
        BmiCategory.Overweight -> stringResource(R.string.bmi_overweight) to Color(0xFFFB8C00)
        BmiCategory.Obese -> stringResource(R.string.bmi_obese) to Color(0xFFE53935)
        null -> "—" to MaterialTheme.colorScheme.outline
    }
    val bfCat = bodyFat?.let { BodyFat.categorize(sex, it) }
    val (bfLabel, bfColor) = when (bfCat) {
        BodyFatCategory.Essential -> stringResource(R.string.bf_essential) to Color(0xFF42A5F5)
        BodyFatCategory.Athletes -> stringResource(R.string.bf_athletes) to Color(0xFF26A69A)
        BodyFatCategory.Fitness -> stringResource(R.string.bf_fitness) to Color(0xFF43A047)
        BodyFatCategory.Average -> stringResource(R.string.bf_average) to Color(0xFFFB8C00)
        BodyFatCategory.Obese -> stringResource(R.string.bf_obese) to Color(0xFFE53935)
        null -> "—" to MaterialTheme.colorScheme.outline
    }
    val bmrColor = if (bmrKcal != null) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.outline
    val tdeeColor = if (tdeeKcal != null) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetricColumn(
                    title = stringResource(R.string.bmi),
                    valueText = bmi?.let(Bmi::format) ?: "—",
                    subtitle = bmiLabel,
                    color = bmiColor,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(64.dp))
                MetricColumn(
                    title = stringResource(R.string.body_fat),
                    valueText = bodyFat?.let { "${BodyFat.format(it)}%" } ?: "—",
                    subtitle = bfLabel,
                    color = bfColor,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetricColumn(
                    title = stringResource(R.string.bmr),
                    valueText = bmrKcal?.let(Bmr::format) ?: "—",
                    subtitle = stringResource(R.string.bmr_unit),
                    color = bmrColor,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(64.dp))
                MetricColumn(
                    title = stringResource(R.string.tdee),
                    valueText = tdeeKcal?.let(Bmr::format) ?: "—",
                    subtitle = stringResource(R.string.bmr_unit),
                    color = tdeeColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    valueText: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(valueText, style = MaterialTheme.typography.titleLarge, color = color)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
