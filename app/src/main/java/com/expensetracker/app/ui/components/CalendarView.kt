package com.expensetracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.components.GlassCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import kotlin.math.roundToInt

private val accentColor = Color(0xFF7C3AED)

data class DailyExpense(val day: Int, val epochDay: Long, val total: Double, val income: Double = 0.0)

@Composable
fun CalendarView(
    dailyExpenses: List<DailyExpense>,
    modifier: Modifier = Modifier,
) {
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
    var tooltipOffset by remember { mutableStateOf<Offset?>(null) }
    var tooltipAmt by remember { mutableStateOf(0.0) }
    var tooltipIncome by remember { mutableStateOf(0.0) }
    val density = LocalDensity.current

    val expenseMap = expenses.associateBy { it.epochDay }

    var canvasWidthPx by remember { mutableStateOf(0f) }

    data class DayInfo(val day: Int, val epochDay: Long, val amt: Double, val expense: Double, val income: Double)

    fun buildWindow(base: Calendar, offsetWeeks: Int, count: Int): List<DayInfo> {
        val cal = Calendar.getInstance().apply { time = base.time; add(Calendar.DAY_OF_MONTH, offsetWeeks * 7) }
        return (0 until count).map { i ->
            val c = Calendar.getInstance().apply { time = cal.time; add(Calendar.DAY_OF_MONTH, i) }
            val ldEpoch = java.time.LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)).toEpochDay()
            val exp = expenseMap[ldEpoch]
            val expense = exp?.total ?: 0.0
            val income = exp?.income ?: 0.0
            DayInfo(c.get(Calendar.DAY_OF_MONTH), ldEpoch, expense + income, expense, income)
        }
    }

    val weekEnd = Calendar.getInstance().apply { time = weekStart.time; add(Calendar.DAY_OF_MONTH, 6) }

    Column {
        Text(
            "${shortMonths[weekStart.get(Calendar.MONTH)]} ${weekStart.get(Calendar.DAY_OF_MONTH)} - ${shortMonths[weekEnd.get(Calendar.MONTH)]} ${weekEnd.get(Calendar.DAY_OF_MONTH)}",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        Spacer(Modifier.height(8.dp))

        Box {
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
                                    tooltipOffset = null
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                                tooltipOffset = null
                            },
                        )
                    },
            ) {
                val w = size.width
                canvasWidthPx = w
                val h = size.height
                val margin = 12f
                val dayW = w / 7f
                val visibleOffset = animatable.value - dragAccumulator / w

                val prevWindow = buildWindow(weekStart, -1, 7)
                val window = buildWindow(weekStart, 0, 7)
                val nextWindow = buildWindow(weekStart, 1, 7)

                val allPoints = prevWindow + window + nextWindow
                val globalMax = allPoints.maxOf { it.amt }.coerceAtLeast(1.0)

                val coords = allPoints.mapIndexed { i, info ->
                    val x = i * dayW + dayW / 2f - (1f + visibleOffset) * w
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

                coords.forEachIndexed { i, c ->
                    if (i in 7..13) {
                        val isTooltipTarget = tooltipOffset != null && i >= 7 && i < 14 && allPoints[i].day == selectedDay && c.x == tooltipOffset?.x
                        if (isTooltipTarget) {
                            drawCircle(Color.White, 6f, c)
                            drawCircle(accentColor, center = c, radius = 8f)
                        } else {
                            drawCircle(Color.White, 4f, c)
                            drawCircle(accentColor, center = c, radius = 5f)
                        }
                    }
                }
            }

            tooltipOffset?.let { tp ->
                val net = tooltipIncome - tooltipAmt
                val label = buildString {
                    if (tooltipIncome > 0) append("+\u20B9${String.format("%.0f", tooltipIncome)} ")
                    if (tooltipAmt > 0) append("-\u20B9${String.format("%.0f", tooltipAmt)} ")
                    append("Net ${if (net >= 0) "+" else ""}\u20B9${String.format("%.0f", net)}")
                }
                val tooltipW = with(density) { 180.dp.toPx() }
                val tooltipH = with(density) { 30.dp.toPx() }
                val canvasW = if (canvasWidthPx > 0f) canvasWidthPx else with(density) { 360.dp.toPx() }

                val aboveSpace = tp.y
                val showAbove = aboveSpace >= tooltipH + with(density) { 12.dp.toPx() }

                val adjustedX: Float
                val adjustedY: Float

                if (showAbove) {
                    adjustedX = (tp.x - tooltipW / 2f).coerceIn(0f, canvasW - tooltipW)
                    adjustedY = tp.y - tooltipH - with(density) { 8.dp.toPx() }
                } else {
                    val showRight = tp.x < canvasW / 2f
                    if (showRight) {
                        adjustedX = tp.x + with(density) { 12.dp.toPx() }
                        adjustedY = (tp.y - tooltipH / 2f).coerceIn(0f, with(density) { 140.dp.toPx() } - tooltipH)
                    } else {
                        adjustedX = tp.x - tooltipW - with(density) { 12.dp.toPx() }
                        adjustedY = (tp.y - tooltipH / 2f).coerceIn(0f, with(density) { 140.dp.toPx() } - tooltipH)
                    }
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(adjustedX.roundToInt(), adjustedY.roundToInt()) }
                        .background(
                            color = Color(0xFF2B2930),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (net >= 0) Color(0xFF4CAF50) else Color(0xFFE91E63),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            val window = buildWindow(weekStart, 0, 7)
            val prevWindow = buildWindow(weekStart, -1, 7)
            val nextWindow = buildWindow(weekStart, 1, 7)
            val allPointsWindow = prevWindow + window + nextWindow
            val globalMax = allPointsWindow.maxOf { it.amt }.coerceAtLeast(1.0)
            val canvasH = with(density) { 140.dp.toPx() }
            val margin = 12f
            val dayLabels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

            window.forEachIndexed { i, info ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (info.amt == 0.0 && info.income == 0.0) {
                                tooltipOffset = null
                                onSelect(info.day)
                                return@clickable
                            }
                            val dotY = canvasH - ((info.amt / globalMax).toFloat() * (canvasH - margin * 2) + margin)
                            val dayW = if (canvasWidthPx > 0f) canvasWidthPx / 7f else with(density) { 360.dp.toPx() } / 7f
                            tooltipAmt = info.expense
                            tooltipIncome = info.income
                            tooltipOffset = Offset(
                                (i * dayW + dayW / 2f),
                                dotY,
                            )
                            onSelect(info.day)
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Text(dayLabels[i], fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("${info.day}", fontSize = 11.sp,
                        fontWeight = if (info.day == selectedDay) FontWeight.Bold else FontWeight.Normal,
                        color = if (info.day == selectedDay) accentColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
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

private fun Calendar.toEpochDay(): Long {
    val instant = toInstant()
    val localDate = java.time.LocalDate.of(
        get(Calendar.YEAR),
        get(Calendar.MONTH) + 1,
        get(Calendar.DAY_OF_MONTH),
    )
    return localDate.toEpochDay()
}
