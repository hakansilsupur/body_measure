package com.bodymeasure.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bodymeasure.app.R
import com.bodymeasure.app.data.Measurement
import com.bodymeasure.app.util.Bmi
import com.bodymeasure.app.util.Bmr
import com.bodymeasure.app.util.BodyFat

@Composable
fun TrendsScreen(items: List<Measurement>) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.empty_history),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val bmiPoints = items.map { ChartPoint(it.timestamp, it.bmi) }
    val bfPoints = items.mapNotNull { m ->
        m.bodyFatPct?.let { ChartPoint(m.timestamp, it) }
    }
    val bmrPoints = items.mapNotNull { m ->
        m.bmrKcal?.let { ChartPoint(m.timestamp, it) }
    }

    val needTwo = stringResource(R.string.chart_need_two)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LineChart(
            title = stringResource(R.string.trends_bmi),
            points = bmiPoints,
            color = MaterialTheme.colorScheme.primary,
            formatValue = Bmi::format,
            needTwoMessage = needTwo
        )
        LineChart(
            title = stringResource(R.string.trends_body_fat),
            points = bfPoints,
            color = MaterialTheme.colorScheme.secondary,
            formatValue = { "${BodyFat.format(it)}%" },
            needTwoMessage = needTwo
        )
        LineChart(
            title = stringResource(R.string.trends_bmr),
            points = bmrPoints,
            color = MaterialTheme.colorScheme.tertiary,
            formatValue = Bmr::format,
            needTwoMessage = needTwo
        )
    }
}
