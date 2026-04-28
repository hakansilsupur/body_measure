package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.BmiCategory
import com.bodymeasure.app.util.BodyFat
import com.bodymeasure.app.util.BodyFatCategory
import com.bodymeasure.app.util.Sex

@Composable
fun AddMeasurementScreen(
    onSave: (
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
    ) -> Unit,
    showMessage: (String) -> Unit
) {
    var sex by rememberSaveable { mutableStateOf(Sex.Male) }
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var waist by rememberSaveable { mutableStateOf("") }
    var arm by rememberSaveable { mutableStateOf("") }
    var chest by rememberSaveable { mutableStateOf("") }
    var hip by rememberSaveable { mutableStateOf("") }
    var thigh by rememberSaveable { mutableStateOf("") }
    var neck by rememberSaveable { mutableStateOf("") }

    val weightD = weight.toDoubleOrNull()
    val heightD = height.toDoubleOrNull()
    val waistD = waist.toDoubleOrNull()
    val hipD = hip.toDoubleOrNull()
    val neckD = neck.toDoubleOrNull()

    val livePreviewBmi = if (weightD != null && heightD != null && weightD > 0 && heightD > 0)
        Bmi.calculate(weightD, heightD) else null
    val livePreviewBodyFat = if (heightD != null && heightD > 0)
        BodyFat.calculate(sex, heightD, neckD, waistD, hipD) else null

    val savedLabel = stringResource(R.string.saved)
    val errInvalid = stringResource(R.string.error_invalid_number)
    val errRequired = stringResource(R.string.error_required_weight_height)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PreviewCard(bmi = livePreviewBmi, sex = sex, bodyFat = livePreviewBodyFat)

        SexSelector(sex = sex, onChange = { sex = it })

        NumberField(value = weight, onChange = { weight = it }, label = stringResource(R.string.weight_kg))
        NumberField(value = height, onChange = { height = it }, label = stringResource(R.string.height_cm))
        NumberField(value = neck, onChange = { neck = it }, label = stringResource(R.string.neck_cm))
        NumberField(value = waist, onChange = { waist = it }, label = stringResource(R.string.waist_cm))
        NumberField(value = hip, onChange = { hip = it }, label = stringResource(R.string.hip_cm))
        NumberField(value = chest, onChange = { chest = it }, label = stringResource(R.string.chest_cm))
        NumberField(value = arm, onChange = { arm = it }, label = stringResource(R.string.arm_cm))
        NumberField(value = thigh, onChange = { thigh = it }, label = stringResource(R.string.thigh_cm))

        Text(
            text = stringResource(
                if (sex == Sex.Female) R.string.bf_hint_female else R.string.bf_hint_male
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    weight = ""; height = ""; waist = ""; arm = ""
                    chest = ""; hip = ""; thigh = ""; neck = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.clear)) }

            Button(
                onClick = {
                    val w = weight.toDoubleOrNull()
                    val h = height.toDoubleOrNull()
                    if (w == null || h == null) {
                        showMessage(if (weight.isBlank() || height.isBlank()) errRequired else errInvalid)
                        return@Button
                    }
                    onSave(
                        sex,
                        w, h,
                        waist.toDoubleOrNull(),
                        arm.toDoubleOrNull(),
                        chest.toDoubleOrNull(),
                        hip.toDoubleOrNull(),
                        thigh.toDoubleOrNull(),
                        neck.toDoubleOrNull()
                    ) { result ->
                        when (result) {
                            is SaveResult.Success -> {
                                showMessage(savedLabel)
                                weight = ""; height = ""; waist = ""; arm = ""
                                chest = ""; hip = ""; thigh = ""; neck = ""
                            }
                            is SaveResult.Error -> showMessage(result.message)
                            SaveResult.Idle -> Unit
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.save)) }
        }

        Spacer(Modifier.height(24.dp))
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
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
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
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PreviewCard(bmi: Double?, sex: Sex, bodyFat: Double?) {
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricColumn(
                title = stringResource(R.string.bmi),
                valueText = bmi?.let(Bmi::format) ?: "—",
                category = bmiLabel,
                color = bmiColor,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier
                    .height(72.dp)
                    .padding(horizontal = 8.dp)
            )
            MetricColumn(
                title = stringResource(R.string.body_fat),
                valueText = bodyFat?.let { "${BodyFat.format(it)}%" } ?: "—",
                category = bfLabel,
                color = bfColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    valueText: String,
    category: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(valueText, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(category, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
