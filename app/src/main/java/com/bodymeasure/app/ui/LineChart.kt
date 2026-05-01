package com.bodymeasure.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChartPoint(val timestamp: Long, val value: Double)

@Composable
fun LineChart(
    title: String,
    points: List<ChartPoint>,
    color: Color,
    modifier: Modifier = Modifier,
    formatValue: (Double) -> String = { "%.1f".format(it) },
    needTwoMessage: String = "Add at least two entries to see a chart."
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))

            if (points.size < 2) {
                Text(
                    needTwoMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val sorted = points.sortedBy { it.timestamp }
            val xMin = sorted.first().timestamp
            val xMax = sorted.last().timestamp
            val xRange = (xMax - xMin).coerceAtLeast(1L)

            val rawYMin = sorted.minOf { it.value }
            val rawYMax = sorted.maxOf { it.value }
            val rawSpan = rawYMax - rawYMin
            val pad = if (rawSpan < 0.001) (rawYMax.coerceAtLeast(1.0) * 0.05) else rawSpan * 0.1
            val yLow = rawYMin - pad
            val yHigh = rawYMax + pad
            val yRange = (yHigh - yLow).coerceAtLeast(0.001)

            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
            val outline = MaterialTheme.colorScheme.outlineVariant

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "min ${formatValue(rawYMin)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
                Text(
                    "max ${formatValue(rawYMax)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val padTop = 8f
                val padBottom = 8f
                val padLeft = 8f
                val padRight = 8f
                val drawW = size.width - padLeft - padRight
                val drawH = size.height - padTop - padBottom

                fun xCoord(t: Long): Float =
                    padLeft + ((t - xMin).toDouble() / xRange.toDouble() * drawW).toFloat()

                fun yCoord(v: Double): Float =
                    padTop + ((yHigh - v) / yRange * drawH).toFloat()

                drawLine(
                    color = outline,
                    start = Offset(padLeft, padTop),
                    end = Offset(padLeft, padTop + drawH),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = outline,
                    start = Offset(padLeft, padTop + drawH),
                    end = Offset(padLeft + drawW, padTop + drawH),
                    strokeWidth = 1.5f
                )

                val path = Path().apply {
                    sorted.forEachIndexed { i, p ->
                        val x = xCoord(p.timestamp)
                        val y = yCoord(p.value)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                sorted.forEach { p ->
                    drawCircle(
                        color = color,
                        radius = 6f,
                        center = Offset(xCoord(p.timestamp), yCoord(p.value))
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            val dateFmt = remember<SimpleDateFormat> { SimpleDateFormat("MMM d", Locale.getDefault()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dateFmt.format(Date(xMin)),
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
                Text(
                    "${sorted.size} entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
                Text(
                    dateFmt.format(Date(xMax)),
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
            }
        }
    }
}

