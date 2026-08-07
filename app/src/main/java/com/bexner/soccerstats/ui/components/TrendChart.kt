package com.bexner.soccerstats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/** One bar: a short x-axis label and a value, or null where there is no value. */
data class TrendPoint(
    val label: String,
    val value: Double?
)

/**
 * Bars per game with a cumulative-average line laid over them.
 *
 * The average is cumulative rather than a rolling window: with six or eight
 * games a rolling window would mostly show noise, whereas a running mean shows
 * where the season is actually settling.
 *
 * Points with a null value are drawn as gaps, not zeroes — "didn't play in goal"
 * and "conceded nothing" are different facts and shouldn't look the same.
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    averageColor: Color = MaterialTheme.colorScheme.tertiary,
    valueFormatter: (Double) -> String = { formatCompact(it) }
) {
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.isEmpty()) return@Canvas

            val labelHeight = 18.dp.toPx()
            val valueHeight = 14.dp.toPx()
            val leftPad = 4.dp.toPx()
            val chartTop = valueHeight
            val chartBottom = size.height - labelHeight
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
            val chartWidth = (size.width - leftPad * 2).coerceAtLeast(1f)

            val values = points.mapNotNull { it.value }
            if (values.isEmpty()) return@Canvas
            // Always include zero so bar heights stay proportional and honest.
            val maxValue = max(values.max(), 0.0)
            val scaleMax = if (maxValue <= 0.0) 1.0 else niceCeiling(maxValue)

            fun yFor(v: Double): Float =
                chartBottom - (v / scaleMax * chartHeight).toFloat()

            // Baseline
            drawLine(
                color = axisColor,
                start = Offset(leftPad, chartBottom),
                end = Offset(leftPad + chartWidth, chartBottom),
                strokeWidth = 1.dp.toPx()
            )

            val slot = chartWidth / points.size
            val barWidth = (slot * 0.6f).coerceAtMost(38.dp.toPx())

            points.forEachIndexed { index, point ->
                val centerX = leftPad + slot * index + slot / 2f
                val value = point.value

                if (value != null) {
                    val top = yFor(value)
                    val height = (chartBottom - top).coerceAtLeast(if (value > 0.0) 2f else 0f)
                    if (height > 0f) {
                        drawRect(
                            color = barColor,
                            topLeft = Offset(centerX - barWidth / 2f, chartBottom - height),
                            size = Size(barWidth, height)
                        )
                    }
                    // Value above the bar, but only when there's room for it.
                    if (slot > 26.dp.toPx()) {
                        drawLabel(
                            textMeasurer,
                            valueFormatter(value),
                            centerX,
                            (chartBottom - height) - valueHeight,
                            labelColor,
                            9.sp.value
                        )
                    }
                }

                drawLabel(
                    textMeasurer,
                    point.label,
                    centerX,
                    chartBottom + 3.dp.toPx(),
                    labelColor,
                    9.sp.value
                )
            }

            // Cumulative average across games that have a value.
            val path = Path()
            var runningSum = 0.0
            var runningCount = 0
            var started = false
            points.forEachIndexed { index, point ->
                val value = point.value ?: return@forEachIndexed
                runningSum += value
                runningCount++
                val avg = runningSum / runningCount
                val x = leftPad + slot * index + slot / 2f
                val y = yFor(avg)
                if (!started) {
                    path.moveTo(x, y); started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            if (started && runningCount > 1) {
                drawPath(
                    path = path,
                    color = averageColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.cornerPathEffect(4.dp.toPx())
                    )
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    centerX: Float,
    top: Float,
    color: Color,
    fontSizeSp: Float
) {
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = fontSizeSp.sp))
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(centerX - layout.size.width / 2f, top)
    )
}

/** Rounds an axis maximum up to something readable: 1, 2, 5, 10, 20, 50... */
internal fun niceCeiling(value: Double): Double {
    if (value <= 0.0) return 1.0
    var magnitude = 1.0
    while (value / magnitude >= 10.0) magnitude *= 10.0
    while (value / magnitude < 1.0) magnitude /= 10.0
    val normalized = value / magnitude
    val step = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return step * magnitude
}

/** Whole numbers plain, fractions to one decimal. */
internal fun formatCompact(value: Double): String =
    if (abs(value - value.toLong()) < 0.05) value.toLong().toString()
    else String.format("%.1f", value)

/** Kept for callers that want the same rounding used on the axis. */
internal fun axisSteps(maxValue: Double): Int = ceil(maxValue).toInt().coerceAtLeast(1)
