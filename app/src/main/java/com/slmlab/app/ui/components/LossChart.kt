package com.slmlab.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slmlab.app.data.MetricPoint
import java.util.Locale

/**
 * Line chart of metric series. X is the step, Y is the value, both auto-ranged with 5% padding.
 * Series over 600 points are downsampled.
 */
@Composable
fun LossChart(series: Map<String, List<MetricPoint>>, modifier: Modifier = Modifier) {
    val all = series.values.flatten()
    if (all.isEmpty()) return

    val scheme = MaterialTheme.colorScheme
    val palette = listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    val gridColor = scheme.outline
    val labelArgb = scheme.onSurfaceVariant.toArgb()
    val styles: List<Triple<String, List<MetricPoint>, Color>> = series.entries.mapIndexed { index, entry ->
        Triple(entry.key, entry.value, palette[index % palette.size])
    }

    val stepMin = all.minOf { it.step }
    val stepMax = all.maxOf { it.step }
    val valMin = all.minOf { it.value }
    val valMax = all.maxOf { it.value }

    val xSpan = (stepMax - stepMin).coerceAtLeast(1).toFloat()
    val xLo = stepMin - xSpan * 0.05f
    val xHi = stepMax + xSpan * 0.05f
    val ySpan = if (valMax - valMin > 1e-9f) valMax - valMin else 1f
    val yLo = valMin - ySpan * 0.05f
    val yHi = valMax + ySpan * 0.05f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            styles.forEach { (name, points, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color, CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$name ${String.format(Locale.US, "%.3f", points.last().value)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
        ) {
            val padLeft = 44.dp.toPx()
            val padRight = 8.dp.toPx()
            val padTop = 8.dp.toPx()
            val padBottom = 22.dp.toPx()
            val plotWidth = size.width - padLeft - padRight
            val plotHeight = size.height - padTop - padBottom

            fun xPx(step: Float): Float = padLeft + (step - xLo) / (xHi - xLo) * plotWidth
            fun yPx(v: Float): Float = padTop + plotHeight * (1f - (v - yLo) / (yHi - yLo))

            val labelPx = 11.sp.toPx()
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = labelPx
                color = labelArgb
                typeface = Typeface.MONOSPACE
            }
            val nativeCanvas = drawContext.canvas.nativeCanvas

            // Four horizontal grid lines with Y labels.
            for (i in 0..3) {
                val frac = i / 3f
                val y = padTop + plotHeight * frac
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, y),
                    end = Offset(padLeft + plotWidth, y),
                    strokeWidth = 1.dp.toPx(),
                )
                val labelValue = yHi - (yHi - yLo) * frac
                labelPaint.textAlign = Paint.Align.RIGHT
                nativeCanvas.drawText(
                    String.format(Locale.US, "%.2f", labelValue),
                    padLeft - 6.dp.toPx(),
                    y + labelPx / 3f,
                    labelPaint,
                )
            }

            // X labels at the first and last step.
            val labelY = padTop + plotHeight + 16.dp.toPx()
            labelPaint.textAlign = Paint.Align.LEFT
            nativeCanvas.drawText(stepMin.toString(), xPx(stepMin.toFloat()), labelY, labelPaint)
            if (stepMax != stepMin) {
                labelPaint.textAlign = Paint.Align.RIGHT
                nativeCanvas.drawText(stepMax.toString(), xPx(stepMax.toFloat()), labelY, labelPaint)
            }

            // One line per series.
            for ((_, points, color) in styles) {
                val sampled = downsample(points)
                if (sampled.size == 1) {
                    drawCircle(
                        color = color,
                        radius = 3.dp.toPx(),
                        center = Offset(xPx(sampled[0].step.toFloat()), yPx(sampled[0].value)),
                    )
                } else {
                    val path = Path()
                    sampled.forEachIndexed { index, point ->
                        val x = xPx(point.step.toFloat())
                        val y = yPx(point.value)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

/** Every k-th point when there are more than [maxPoints]; the last point is always kept. */
private fun downsample(points: List<MetricPoint>, maxPoints: Int = 600): List<MetricPoint> {
    if (points.size <= maxPoints) return points
    val k = (points.size + maxPoints - 1) / maxPoints
    val sampled = points.filterIndexed { index, _ -> index % k == 0 }.toMutableList()
    if (sampled.last() != points.last()) sampled += points.last()
    return sampled
}
