package com.expensetracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.components.GlassCard
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

private val accentColor = Color(0xFF7C3AED)

data class DailyExpense(val day: Int, val total: Double)

@Composable
fun CalendarView(dailyExpenses: List<DailyExpense>, modifier: Modifier = Modifier) {
    var selectedDay by remember { mutableIntStateOf(-1) }
    var baseMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var baseYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var weekOffset by remember { mutableIntStateOf(0) }

    val weekStart = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.YEAR, baseYear)
        set(Calendar.MONTH, baseMonth)
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        add(Calendar.DAY_OF_MONTH, weekOffset * 7)
    }

    val displayMonth = run {
        val c = Calendar.getInstance().apply { time = weekStart.time }
        val counts = mutableMapOf<Int, Int>()
        repeat(7) { counts[c.get(Calendar.MONTH)] = (counts[c.get(Calendar.MONTH)] ?: 0) + 1; c.add(Calendar.DAY_OF_MONTH, 1) }
        counts.maxByOrNull { it.value }?.key ?: weekStart.get(Calendar.MONTH)
    }
    val displayYear = run {
        val c = Calendar.getInstance().apply { time = weekStart.time }
        val counts = mutableMapOf<Int, Int>()
        repeat(7) { counts[c.get(Calendar.YEAR)] = (counts[c.get(Calendar.YEAR)] ?: 0) + 1; c.add(Calendar.DAY_OF_MONTH, 1) }
        counts.maxByOrNull { it.value }?.key ?: weekStart.get(Calendar.YEAR)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthHeader(displayYear, displayMonth,
                onPrev = {
                    if (baseMonth == 0) { baseMonth = 11; baseYear-- } else baseMonth--
                    weekOffset = 0; selectedDay = -1
                },
                onNext = {
                    if (baseMonth == 11) { baseMonth = 0; baseYear++ } else baseMonth++
                    weekOffset = 0; selectedDay = -1
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            LineChartView(
                expenses = dailyExpenses,
                weekStart = weekStart,
                onSettle = { delta -> weekOffset += delta },
                selectedDay = selectedDay,
                onSelect = { selectedDay = it },
            )
            if (selectedDay > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedDaySummary(selectedDay, displayMonth, displayYear,
                    dailyExpenses.find { it.day == selectedDay }?.total ?: 0.0)
            }
        }
    }
}

@Composable
private fun MonthHeader(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Box(
            Modifier
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .clickable(onClick = onPrev)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { Text("\u276E", fontSize = 16.sp, color = accentColor) }
        Text(
            "${monthNames[month]} $year",
            fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            Modifier
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .clickable(onClick = onNext)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { Text("\u276F", fontSize = 16.sp, color = accentColor) }
    }
}

@Composable
private fun LineChartView(
    expenses: List<DailyExpense>,
    weekStart: Calendar,
    onSettle: (Int) -> Unit,
    selectedDay: Int,
    onSelect: (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val animatable = remember { Animatable(0f) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    data class DayInfo(val day: Int, val amt: Double, val label: String)

    fun buildWindow(base: Calendar, offsetWeeks: Int, count: Int): Triple<List<DayInfo>, Double, List<Int>> {
        val cal = Calendar.getInstance().apply { time = base.time; add(Calendar.DAY_OF_MONTH, offsetWeeks * 7) }
        val days = (0 until count).map { i ->
            val c = Calendar.getInstance().apply { time = cal.time; add(Calendar.DAY_OF_MONTH, i) }
            val d = c.get(Calendar.DAY_OF_MONTH)
            DayInfo(d, expenses.find { it.day == d }?.total ?: 0.0, "${d}")
        }
        val maxAmt = days.maxOf { it.amt }.coerceAtLeast(1.0)
        val daysList = days.map { it.day }
        return Triple(days, maxAmt, daysList)
    }

    val weekEnd = Calendar.getInstance().apply { time = weekStart.time; add(Calendar.DAY_OF_MONTH, 6) }

    Column {
        Text(
            "${shortMonths[weekStart.get(Calendar.MONTH)]} ${weekStart.get(Calendar.DAY_OF_MONTH)} - ${shortMonths[weekEnd.get(Calendar.MONTH)]} ${weekEnd.get(Calendar.DAY_OF_MONTH)}",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        Spacer(Modifier.height(8.dp))

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val w = size.width.toFloat()
                                val total = animatable.value - dragAccumulator / w
                                if (kotlin.math.abs(total) > 0.4f) {
                                    val target = total.roundToInt()
                                    onSettle(target)
                                    animatable.snapTo(0f)
                                } else {
                                    animatable.animateTo(0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                }
                                dragAccumulator = 0f
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                        },
                    )
                },
        ) {
            val w = size.width
            val h = size.height
            val margin = 12f
            val dayW = w / 7f
            val visibleOffset = animatable.value - dragAccumulator / w

            val (prevWindow, _, _) = buildWindow(weekStart, -1, 7)
            val (window, _, _) = buildWindow(weekStart, 0, 7)
            val (nextWindow, _, _) = buildWindow(weekStart, 1, 7)

            val allPoints = prevWindow + window + nextWindow
            val globalMax = allPoints.maxOf { it.amt }.coerceAtLeast(1.0)

            val coords = allPoints.mapIndexed { i, info ->
                val x = i * dayW - (1f + visibleOffset) * w
                val y = (h - ((info.amt / globalMax) * (h - margin * 2) + margin)).toFloat()
                Offset(x, y)
            }

            if (coords.isEmpty()) return@Canvas

            val pathCoords = coords.filter { it.x >= -dayW && it.x <= w + dayW }

            val linePath = Path().apply {
                if (pathCoords.isEmpty()) return@apply
                moveTo(pathCoords.first().x, pathCoords.first().y)
                for (i in 0 until pathCoords.size - 1) {
                    val p0 = if (i > 0) pathCoords[i - 1] else pathCoords[i]
                    val p1 = pathCoords[i]
                    val p2 = pathCoords[i + 1]
                    val p3 = if (i + 2 < pathCoords.size) pathCoords[i + 2] else pathCoords[i + 1]
                    cubicTo(
                        p1.x + (p2.x - p0.x) / 6f,
                        p1.y + (p2.y - p0.y) / 6f,
                        p2.x - (p3.x - p1.x) / 6f,
                        p2.y - (p3.y - p1.y) / 6f,
                        p2.x, p2.y,
                    )
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(pathCoords.last().x, h)
                lineTo(pathCoords.first().x, h)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(0f to accentColor.copy(alpha = 0.35f), 1f to accentColor.copy(alpha = 0.0f)),
            )
            drawPath(linePath, color = accentColor, style = Stroke(width = 3f))

            coords.forEach { c ->
                if (c.x in -dayW..w + dayW) {
                    drawCircle(Color.White, 4f, c)
                    drawCircle(accentColor, center = c, radius = 5f)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            val (_, _, days) = buildWindow(weekStart, 0, 7)
            listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEachIndexed { i, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("${days.getOrElse(i) { "" }}", fontSize = 11.sp,
                        fontWeight = if (days.getOrElse(i) { 0 } == selectedDay) FontWeight.Bold else FontWeight.Normal,
                        color = if (days.getOrElse(i) { 0 } == selectedDay) accentColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun SelectedDaySummary(day: Int, month: Int, year: Int, total: Double) {
    Box(
        Modifier.fillMaxWidth()
            .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("${shortMonths[month]} $day, $year",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text("$${String.format("%.2f", total)}",
                fontWeight = FontWeight.Bold,
                color = if (total > 0) Color(0xFFE91E63) else Color(0xFF4CAF50))
        }
    }
}

private val monthNames = arrayOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December"
)
private val shortMonths = arrayOf(
    "Jan","Feb","Mar","Apr","May","Jun",
    "Jul","Aug","Sep","Oct","Nov","Dec"
)
