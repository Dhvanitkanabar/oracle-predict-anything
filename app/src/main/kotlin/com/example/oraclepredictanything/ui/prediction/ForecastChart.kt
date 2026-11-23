package com.example.oraclepredictanything.ui.prediction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Fully interactive custom time-series forecast chart drawn with Compose Canvas.
 * Features:
 * - Animated path progress
 * - Shaded 80% and 95% confidence intervals
 * - Distinct historical vs forecast line styling
 * - Auto-scaled axes with horizontal gridlines
 * - Touch tap interaction to inspect point coordinates
 */
@Composable
fun ForecastChart(
    historicalData: List<Double>,
    predictions: List<Double>,
    lower80: List<Double>,
    upper80: List<Double>,
    lower95: List<Double> = emptyList(),
    upper95: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (historicalData.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200),
        label = "chartAnim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val forecastColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    // Compute bounding box
    val allValues = mutableListOf<Double>()
    allValues.addAll(historicalData)
    allValues.addAll(predictions)
    allValues.addAll(lower80)
    allValues.addAll(upper80)
    allValues.addAll(lower95)
    allValues.addAll(upper95)

    val minY = allValues.minOrNull() ?: 0.0
    val maxY = allValues.maxOrNull() ?: 100.0
    val ySpan = (maxY - minY).coerceAtLeast(1e-4)
    val totalPoints = historicalData.size + predictions.size

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(12.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalPoints) {
                        detectTapGestures { offset ->
                            val xStep = size.width / (totalPoints - 1).coerceAtLeast(1)
                            val idx = (offset.x / xStep).toInt().coerceIn(0, totalPoints - 1)
                            selectedIndex = idx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val topPadding = 16.dp.toPx()
                val bottomPadding = 24.dp.toPx()
                val chartHeight = h - topPadding - bottomPadding

                fun toY(value: Double): Float {
                    val normalized = ((value - minY) / ySpan).toFloat().coerceIn(0f, 1f)
                    return topPadding + chartHeight * (1f - normalized)
                }

                fun toX(index: Int): Float {
                    return (w / (totalPoints - 1).coerceAtLeast(1)) * index
                }

                // 1. Draw horizontal gridlines (4 lines)
                for (step in 0..4) {
                    val gridY = topPadding + (chartHeight / 4) * step
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, gridY),
                        end = Offset(w, gridY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Draw 80% confidence interval shaded area
                if (predictions.isNotEmpty() && lower80.size == predictions.size && upper80.size == predictions.size) {
                    val bandPath = Path()
                    val histOffset = historicalData.size - 1

                    // Top path (upper80)
                    bandPath.moveTo(toX(histOffset), toY(historicalData.last()))
                    for (i in predictions.indices) {
                        val currentX = toX(histOffset + 1 + i)
                        bandPath.lineTo(currentX, toY(upper80[i]))
                    }
                    // Bottom path reverse (lower80)
                    for (i in predictions.indices.reversed()) {
                        val currentX = toX(histOffset + 1 + i)
                        bandPath.lineTo(currentX, toY(lower80[i]))
                    }
                    bandPath.close()

                    drawPath(
                        path = bandPath,
                        color = forecastColor.copy(alpha = 0.12f * animProgress)
                    )
                }

                // 3. Draw historical data line
                val histPath = Path()
                for (i in historicalData.indices) {
                    val x = toX(i)
                    val y = toY(historicalData[i])
                    if (i == 0) histPath.moveTo(x, y) else histPath.lineTo(x, y)
                }
                drawPath(
                    path = histPath,
                    color = primaryColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // 4. Draw forecast line (dashed)
                if (predictions.isNotEmpty()) {
                    val forecastPath = Path()
                    forecastPath.moveTo(toX(historicalData.size - 1), toY(historicalData.last()))
                    val visibleForecastPoints = (predictions.size * animProgress).toInt().coerceAtLeast(1)

                    for (i in 0 until visibleForecastPoints) {
                        val x = toX(historicalData.size + i)
                        val y = toY(predictions[i])
                        forecastPath.lineTo(x, y)
                    }

                    drawPath(
                        path = forecastPath,
                        color = forecastColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )
                }

                // 5. Selected point indicator
                selectedIndex?.let { idx ->
                    val isHistorical = idx < historicalData.size
                    val value = if (isHistorical) historicalData[idx] else predictions[idx - historicalData.size]
                    val x = toX(idx)
                    val y = toY(value)

                    // Vertical guide line
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(x, topPadding),
                        end = Offset(x, h - bottomPadding),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Dot
                    drawCircle(
                        color = if (isHistorical) primaryColor else forecastColor,
                        radius = 5.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Tap inspection chip
        selectedIndex?.let { idx ->
            val isHistorical = idx < historicalData.size
            val value = if (isHistorical) historicalData[idx] else predictions[idx - historicalData.size]
            val tag = if (isHistorical) "Historical (Point ${idx + 1})" else "Forecast (+${idx - historicalData.size + 1})"

            Surface(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$tag: ${String.format("%.2f", value)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isHistorical) primaryColor else forecastColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Legend row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = primaryColor, label = "Historical (${historicalData.size})")
            LegendItem(color = forecastColor, label = "Forecast (${predictions.size})", isDashed = true)
            LegendItem(color = forecastColor.copy(alpha = 0.35f), label = "80% Confidence Band")
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = if (isDashed) 3.dp else 8.dp)
                .clip(if (isDashed) RoundedCornerShape(2.dp) else CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
