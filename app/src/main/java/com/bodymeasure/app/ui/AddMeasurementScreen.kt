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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
fun AddMeasurementScreen(
    onSave: (
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
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var arm by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }
    var thigh by remember { mutableStateOf("") }
    var neck by remember { mutableStateOf("") }

    val weightD = weight.toDoubleOrNull()
    val heightD = height.toDoubleOrNull()
    val livePreviewBmi = if (weightD != null && heightD != null && weightD > 0 && heightD > 0)
        Bmi.calculate(weightD, heightD) else null

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
        BmiPreviewCard(bmi = livePreviewBmi)

        NumberField(value = weight, onChange = { weight = it }, label = stringResource(R.string.weight_kg))
        NumberField(value = height, onChange = { height = it }, label = stringResource(R.string.height_cm))
        NumberField(value = waist, onChange = { waist = it }, label = stringResource(R.string.waist_cm))
        NumberField(value = arm, onChange = { arm = it }, label = stringResource(R.string.arm_cm))
        NumberField(value = chest, onChange = { chest = it }, label = stringResource(R.string.chest_cm))
        NumberField(value = hip, onChange = { hip = it }, label = stringResource(R.string.hip_cm))
        NumberField(value = thigh, onChange = { thigh = it }, label = stringResource(R.string.thigh_cm))
        NumberField(value = neck, onChange = { neck = it }, label = stringResource(R.string.neck_cm))

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
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Accept digits, single dot, and comma (replace comma with dot)
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
private fun BmiPreviewCard(bmi: Double?) {
    val category = bmi?.let { Bmi.categorize(it) }
    val (label, color) = when (category) {
        BmiCategory.Underweight -> stringResource(R.string.bmi_underweight) to Color(0xFF42A5F5)
        BmiCategory.Normal -> stringResource(R.string.bmi_normal) to Color(0xFF43A047)
        BmiCategory.Overweight -> stringResource(R.string.bmi_overweight) to Color(0xFFFB8C00)
        BmiCategory.Obese -> stringResource(R.string.bmi_obese) to Color(0xFFE53935)
        null -> "—" to MaterialTheme.colorScheme.outline
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.bmi), style = MaterialTheme.typography.labelLarge)
            Text(
                text = bmi?.let(Bmi::format) ?: "—",
                style = MaterialTheme.typography.displaySmall,
                color = color
            )
            Text(label, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}
