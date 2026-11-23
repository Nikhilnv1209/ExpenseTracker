package com.expensetracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType

/**
 * 7-day trend line, matched to the reference:
 * vertical dashed guides, gradient stroke (champagne → sage), soft sage area fill,
 * a highlighted peak with a halo dot, and single-letter day labels.
 */
@Composable
fun TrendChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 144.dp,
) {
    val c = AppTheme.colors
    val maxValue = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0) * 1.18
    val peakIndex = values.indices.maxByOrNull { values[it] } ?: 0

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val w = size.width
            val h = size.height
            val n = values.size.coerceAtLeast(2)
            val stepX = w / (n - 1)
            val topPad = 14.dp.toPx()
            val bottomPad = 10.dp.toPx()
            val usable = h - topPad - bottomPad

            fun yAt(v: Double) = topPad + usable * (1f - (v / maxValue).toFloat())
            val points = values.mapIndexed { i, v -> Offset(stepX * i, yAt(v)) }

            // Vertical guides, one per day; the peak guide is highlighted.
            points.forEachIndexed { i, p ->
                val highlighted = i == peakIndex
                drawLine(
                    color = if (highlighted) c.secondary.copy(alpha = 0.8f) else c.textFaint.copy(alpha = 0.35f),
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, h),
                    strokeWidth = if (highlighted) 1.2.dp.toPx() else 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(2.dp.toPx(), if (highlighted) 2.dp.toPx() else 3.dp.toPx()),
                    ),
                )
            }

            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }

            val fill = Path().apply {
                addPath(line)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }

            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    colors = listOf(c.secondary.copy(alpha = 0.22f), c.secondary.copy(alpha = 0.0f)),
                    startY = topPad,
                    endY = h,
                ),
            )

            drawPath(
                path = line,
                brush = Brush.horizontalGradient(
                    colors = listOf(c.gold.copy(alpha = 0.85f), c.secondary, c.secondary),
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            val peak = points[peakIndex.coerceIn(0, points.lastIndex)]
            drawCircle(color = c.secondary.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = peak)
            drawCircle(color = c.secondary, radius = 5.dp.toPx(), center = peak)
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, label ->
                val active = index == peakIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) c.secondary.copy(alpha = 0.14f) else Color.Transparent)
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        text = label.substringBefore("|"),
                        style = AppType.groupLabel.copy(letterSpacing = 0.2.sp),
                        color = if (active) c.secondary else c.textFaint,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label.substringAfter("|"),
                        style = AppType.amount(12, if (active) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (active) c.secondary else c.textMuted,
                    )
                }
            }
        }
    }
}
