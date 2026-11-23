package com.expensetracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 7-day trend line: dashed vertical guides, gradient stroke (champagne → sage),
 * soft area fill, a highlighted peak, and press-and-drag tooltips per day.
 */
@Composable
fun TrendChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 144.dp,
    valueTexts: List<String> = emptyList(),
    titleTexts: List<String> = emptyList(),
    onSwipeWeek: (Int) -> Unit = {},
) {
    val c = AppTheme.colors
    val maxRaw = values.maxOrNull() ?: 0.0
    val minRaw = values.minOrNull() ?: 0.0
    val top = maxOf(maxRaw, 0.0) * 1.18
    val bottom = minOf(minRaw, 0.0) * 1.18
    val span = (top - bottom).takeIf { it > 0.0 } ?: 1.0
    val peakIndex = values.indices.maxByOrNull { values[it] } ?: 0

    var selected by remember(values) { mutableStateOf<Int?>(null) }
    var releaseJob by remember { mutableStateOf<Job?>(null) }
    var chartWidth by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val topPad = 16.dp
    val bottomPad = 10.dp

    fun indexAt(x: Float): Int {
        if (values.size <= 1) return 0
        val insetPx = with(density) { 8.dp.toPx() }
        val step = (chartWidth - insetPx * 2).toFloat() / (values.size - 1)
        if (step <= 0f) return 0
        return ((x - insetPx) / step).roundToInt().coerceIn(0, values.lastIndex)
    }

    fun pick(index: Int?) {
        releaseJob?.cancel()
        selected = index
    }

    fun scheduleDismiss() {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(1800)
            selected = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .onSizeChanged { chartWidth = it.width }
                .pointerInput(values) {
                    val weekThreshold = 56.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dx = 0f
                        var dy = 0f
                        // null = undecided, true = horizontal scrub, false = let the page scroll
                        var horizontal: Boolean? = null

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val delta = change.positionChange()
                            dx += delta.x
                            dy += delta.y

                            if (horizontal == null &&
                                (abs(dx) > viewConfiguration.touchSlop || abs(dy) > viewConfiguration.touchSlop)
                            ) {
                                horizontal = abs(dx) > abs(dy) * 1.4f
                            }

                            if (horizontal == true) {
                                pick(indexAt(change.position.x))
                                change.consume()
                            } else if (horizontal == false) {
                                break
                            }
                        }

                        when (horizontal) {
                            true -> {
                                if (abs(dx) >= weekThreshold) {
                                    selected = null
                                    releaseJob?.cancel()
                                    onSwipeWeek(if (dx > 0f) 1 else -1)
                                } else {
                                    scheduleDismiss()
                                }
                            }
                            null -> {
                                pick(indexAt(down.position.x))
                                scheduleDismiss()
                            }
                            else -> Unit
                        }
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                val w = size.width
                val h = size.height
                val n = values.size.coerceAtLeast(2)
                val insetPx = 8.dp.toPx()
                val stepX = (w - insetPx * 2) / (n - 1)
                val topPadPx = topPad.toPx()
                val bottomPadPx = bottomPad.toPx()
                val usable = h - topPadPx - bottomPadPx

                fun yAt(v: Double) = topPadPx + usable * (1f - ((v - bottom) / span).toFloat())
                val points = values.mapIndexed { i, v -> Offset(insetPx + stepX * i, yAt(v)) }

                // Zero baseline when the series dips negative (net balance mode).
                if (bottom < 0.0) {
                    val zeroY = yAt(0.0)
                    drawLine(
                        color = c.textFaint.copy(alpha = 0.45f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // Vertical guides, one per day; peak and selected are highlighted.
                points.forEachIndexed { i, p ->
                    val isSelected = i == selected
                    val highlighted = i == peakIndex
                    drawLine(
                        color = when {
                            isSelected -> c.gold.copy(alpha = 0.9f)
                            highlighted -> c.secondary.copy(alpha = 0.8f)
                            else -> c.textFaint.copy(alpha = 0.35f)
                        },
                        start = Offset(p.x, 0f),
                        end = Offset(p.x, h),
                        strokeWidth = if (isSelected || highlighted) 1.2.dp.toPx() else 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(2.dp.toPx(), if (isSelected || highlighted) 2.dp.toPx() else 3.dp.toPx()),
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
                    val baseY = if (bottom < 0.0) yAt(0.0) else h
                    lineTo(points.last().x, baseY)
                    lineTo(points.first().x, baseY)
                    close()
                }

                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(c.secondary.copy(alpha = 0.22f), c.secondary.copy(alpha = 0.0f)),
                        startY = topPadPx,
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

                selected?.let { index ->
                    val p = points[index.coerceIn(0, points.lastIndex)]
                    drawCircle(color = c.gold.copy(alpha = 0.22f), radius = 10.dp.toPx(), center = p)
                    drawCircle(color = c.gold, radius = 4.dp.toPx(), center = p)
                }

                val peak = points[peakIndex.coerceIn(0, points.lastIndex)]
                if (selected != peakIndex) {
                    drawCircle(color = c.secondary.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = peak)
                    drawCircle(color = c.secondary, radius = 5.dp.toPx(), center = peak)
                }
            }

            // Tooltip bubble for the touched point
            selected?.let { index ->
                val safeIndex = index.coerceIn(0, values.lastIndex)
                val n = values.size.coerceAtLeast(2)
                val insetPx = with(density) { 8.dp.toPx() }
                val stepPx = (chartWidth - insetPx * 2).toFloat() / (n - 1)
                val pointX = insetPx + stepPx * safeIndex
                val usablePx = with(density) { (height - topPad - bottomPad).toPx() }
                val pointY = with(density) { topPad.toPx() } + usablePx *
                    (1f - ((values[safeIndex] - bottom) / span).toFloat())

                val bubbleWidth = 104.dp
                val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
                val halfBubble = bubbleWidthPx / 2f
                val x = (pointX - halfBubble).coerceIn(0f, (chartWidth - bubbleWidthPx).coerceAtLeast(0f))
                val placeAbove = pointY > with(density) { 44.dp.toPx() }
                val y = if (placeAbove) pointY - with(density) { 48.dp.toPx() } else pointY + with(density) { 14.dp.toPx() }

                Column(
                    modifier = Modifier
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        .width(bubbleWidth)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.cardRaised)
                        .border(1.dp, c.gold.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = valueTexts.getOrNull(safeIndex) ?: values[safeIndex].let { "%.0f".format(it) },
                        style = AppType.amount(12, FontWeight.SemiBold),
                        color = c.textPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = titleTexts.getOrNull(safeIndex) ?: labels.getOrNull(safeIndex)?.substringAfter("|").orEmpty(),
                        style = AppType.groupLabel.copy(letterSpacing = 0.2.sp, fontSize = 9.sp),
                        color = c.textMuted,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, label ->
                val active = index == (selected ?: peakIndex)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (index == selected) c.gold.copy(alpha = 0.12f)
                            else if (active) c.secondary.copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        text = label.substringBefore("|"),
                        style = AppType.groupLabel.copy(letterSpacing = 0.2.sp),
                        color = when {
                            index == selected -> c.gold
                            active -> c.secondary
                            else -> c.textFaint
                        },
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label.substringAfter("|"),
                        style = AppType.amount(12, if (active) FontWeight.SemiBold else FontWeight.Medium),
                        color = when {
                            index == selected -> c.gold
                            active -> c.secondary
                            else -> c.textMuted
                        },
                    )
                }
            }
        }
    }
}
