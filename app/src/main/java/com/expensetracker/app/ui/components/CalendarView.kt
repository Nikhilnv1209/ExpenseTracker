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
import androidx.compose.foundation.layout.IntrinsicSize
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
private val incomeColor = Color(0xFF4CAF50)
private val expenseColor = Color(0xFFE91E63)

data class DailyExpense(val day: Int, val epochDay: Long, val total: Double, val income: Double = 0.0)

@Composable
fun CalendarView(
    dailyExpenses: List<DailyExpense>,
    modifier: Modifier = Modifier,
    graphMode: Int = 0,
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
                graphMode = graphMode,
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
    graphMode: Int = 0,
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

                val lineColor: Color
                val fillColor: Color
                val valueExtractor: (DayInfo) -> Double

                when (graphMode) {
                    1 -> {
                        lineColor = incomeColor
                        fillColor = incomeColor
                        valueExtractor = { it.income }
                    }
                    2 -> {
                        lineColor = expenseColor
                        fillColor = expenseColor
                        valueExtractor = { it.expense }
                    }
                    else -> {
                        lineColor = accentColor
                        fillColor = accentColor
                        valueExtractor = { it.amt }
                    }
                }

                if (graphMode == 0) {
                    val globalMaxIncome = allPoints.maxOf { it.income }.coerceAtLeast(1.0)
                    val globalMaxExpense = allPoints.maxOf { it.expense }.coerceAtLeast(1.0)
                    val halfH = (h - margin * 2) / 2f
                    val midY = margin + halfH

                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1f,
                    )

                    val incomeCoords = allPoints.mapIndexed { i, info ->
                        val x = i * dayW + dayW / 2f - (1f + visibleOffset) * w
                        val yUp = if (info.income > 0) midY - (info.income / globalMaxIncome * halfH).toFloat() else midY
                        Offset(x, yUp)
                    }
                    val expenseCoords = allPoints.mapIndexed { i, info ->
                        val x = i * dayW + dayW / 2f - (1f + visibleOffset) * w
                        val yDown = if (info.expense > 0) midY + (info.expense / globalMaxExpense * halfH).toFloat() else midY
                        Offset(x, yDown)
                    }

                    val incomePathCoords = incomeCoords.filter { it.x >= -dayW && it.x <= w + dayW }
                    val expensePathCoords = expenseCoords.filter { it.x >= -dayW && it.x <= w + dayW }

                    if (incomePathCoords.size >= 2) {
                        val incomeLine = catmullRomPath(incomePathCoords)
                        val incomeFill = Path().apply {
                            addPath(incomeLine)
                            lineTo(incomePathCoords.last().x, midY)
                            lineTo(incomePathCoords.first().x, midY)
                            close()
                        }
                        drawPath(incomeFill, brush = Brush.verticalGradient(0f to incomeColor.copy(alpha = 0.3f), 1f to incomeColor.copy(alpha = 0.0f)))
                        drawPath(incomeLine, color = incomeColor, style = Stroke(width = 2.5f))
                    }

                    if (expensePathCoords.size >= 2) {
                        val expenseLine = catmullRomPath(expensePathCoords)
                        val expenseFill = Path().apply {
                            addPath(expenseLine)
                            lineTo(expensePathCoords.last().x, midY)
                            lineTo(expensePathCoords.first().x, midY)
                            close()
                        }
                        drawPath(expenseFill, brush = Brush.verticalGradient(0f to expenseColor.copy(alpha = 0.0f), 1f to expenseColor.copy(alpha = 0.3f)))
                        drawPath(expenseLine, color = expenseColor, style = Stroke(width = 2.5f))
                    }

                    incomeCoords.forEachIndexed { i, c ->
                        if (i in 7..13 && allPoints[i].income > 0) {
                            val isTarget = tooltipOffset != null && i >= 7 && i < 14 && allPoints[i].day == selectedDay
                            if (isTarget) {
                                drawCircle(Color.White, 6f, c)
                                drawCircle(incomeColor, center = c, radius = 8f)
                            } else {
                                drawCircle(Color.White, 3f, c)
                                drawCircle(incomeColor, center = c, radius = 4f)
                            }
                        }
                    }
                    expenseCoords.forEachIndexed { i, c ->
                        if (i in 7..13 && allPoints[i].expense > 0) {
                            val isTarget = tooltipOffset != null && i >= 7 && i < 14 && allPoints[i].day == selectedDay
                            if (isTarget) {
                                drawCircle(Color.White, 6f, c)
                                drawCircle(expenseColor, center = c, radius = 8f)
                            } else {
                                drawCircle(Color.White, 3f, c)
                                drawCircle(expenseColor, center = c, radius = 4f)
                            }
                        }
                    }
                } else {
                    val globalMax = allPoints.maxOf { valueExtractor(it) }.coerceAtLeast(1.0)

                    val coords = allPoints.mapIndexed { i, info ->
                        val x = i * dayW + dayW / 2f - (1f + visibleOffset) * w
                        val y = (h - ((valueExtractor(info) / globalMax) * (h - margin * 2) + margin)).toFloat()
                        Offset(x, y)
                    }

                    if (coords.isEmpty()) return@Canvas

                    val pathCoords = coords.filter { it.x >= -dayW && it.x <= w + dayW }

                    val linePath = catmullRomPath(pathCoords)

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(pathCoords.last().x, h)
                        lineTo(pathCoords.first().x, h)
                        close()
                    }
                    drawPath(
                        fillPath,
                        brush = Brush.verticalGradient(0f to fillColor.copy(alpha = 0.35f), 1f to fillColor.copy(alpha = 0.0f)),
                    )
                    drawPath(linePath, color = lineColor, style = Stroke(width = 3f))

                    coords.forEachIndexed { i, c ->
                        if (i in 7..13) {
                            val isTooltipTarget = tooltipOffset != null && i >= 7 && i < 14 && allPoints[i].day == selectedDay && c.x == tooltipOffset?.x
                            if (isTooltipTarget) {
                                drawCircle(Color.White, 6f, c)
                                drawCircle(lineColor, center = c, radius = 8f)
                            } else {
                                drawCircle(Color.White, 4f, c)
                                drawCircle(lineColor, center = c, radius = 5f)
                            }
                        }
                    }
                }
            }

            tooltipOffset?.let { tp ->
                val net = tooltipIncome - tooltipAmt
                val canvasW = if (canvasWidthPx > 0f) canvasWidthPx else with(density) { 360.dp.toPx() }

                val tooltipContent: @Composable () -> Unit
                val tooltipH: Float

                when (graphMode) {
                    1 -> {
                        tooltipH = with(density) { 28.dp.toPx() }
                        tooltipContent = {
                            Text(
                                text = "+\u20B9${String.format("%.0f", tooltipIncome)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = incomeColor,
                            )
                        }
                    }
                    2 -> {
                        tooltipH = with(density) { 28.dp.toPx() }
                        tooltipContent = {
                            Text(
                                text = "-\u20B9${String.format("%.0f", tooltipAmt)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = expenseColor,
                            )
                        }
                    }
                    else -> {
                        tooltipH = with(density) { 72.dp.toPx() }
                        tooltipContent = {
                            Column(Modifier.width(IntrinsicSize.Max)) {
                                Text(
                                    text = "+\u20B9${String.format("%.0f", tooltipIncome)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = incomeColor,
                                )
                                Text(
                                    text = "-\u20B9${String.format("%.0f", tooltipAmt)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = expenseColor,
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                )
                                Text(
                                    text = "${if (net >= 0) "+" else ""}\u20B9${String.format("%.0f", net)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (net >= 0) incomeColor else expenseColor,
                                )
                            }
                        }
                    }
                }

                val aboveSpace = tp.y
                val showAbove = aboveSpace >= tooltipH + with(density) { 12.dp.toPx() }

                val adjustedX: Float
                val adjustedY: Float

                if (showAbove) {
                    adjustedX = (tp.x - with(density) { 50.dp.toPx() }).coerceIn(0f, canvasW - with(density) { 100.dp.toPx() })
                    adjustedY = tp.y - tooltipH - with(density) { 8.dp.toPx() }
                } else {
                    val showRight = tp.x < canvasW / 2f
                    if (showRight) {
                        adjustedX = tp.x + with(density) { 12.dp.toPx() }
                        adjustedY = (tp.y - tooltipH / 2f).coerceIn(0f, with(density) { 140.dp.toPx() } - tooltipH)
                    } else {
                        adjustedX = tp.x - with(density) { 80.dp.toPx() } - with(density) { 12.dp.toPx() }
                        adjustedY = (tp.y - tooltipH / 2f).coerceIn(0f, with(density) { 140.dp.toPx() } - tooltipH)
                    }
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(adjustedX.roundToInt(), adjustedY.roundToInt()) }
                        .background(
                            color = Color(0xFF1A1825).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = 60f,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    tooltipContent()
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            val window = buildWindow(weekStart, 0, 7)
            val prevWindow = buildWindow(weekStart, -1, 7)
            val nextWindow = buildWindow(weekStart, 1, 7)
            val allPointsWindow = prevWindow + window + nextWindow
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
                            val dayW = if (canvasWidthPx > 0f) canvasWidthPx / 7f else with(density) { 360.dp.toPx() } / 7f
                            val dotY: Float
                            if (graphMode == 0) {
                                val halfH = (canvasH - margin * 2) / 2f
                                val midY = margin + halfH
                                val gMaxInc = allPointsWindow.maxOf { it.income }.coerceAtLeast(1.0)
                                val gMaxExp = allPointsWindow.maxOf { it.expense }.coerceAtLeast(1.0)
                                dotY = if (info.income >= info.expense) {
                                    midY - (info.income / gMaxInc * halfH).toFloat()
                                } else {
                                    midY + (info.expense / gMaxExp * halfH).toFloat()
                                }
                            } else {
                                val value = if (graphMode == 1) info.income else info.expense
                                val gMax = allPointsWindow.maxOf {
                                    if (graphMode == 1) it.income else it.expense
                                }.coerceAtLeast(1.0)
                                dotY = canvasH - ((value / gMax).toFloat() * (canvasH - margin * 2) + margin)
                            }
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

private fun catmullRomPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]
        path.cubicTo(
            p1.x + (p2.x - p0.x) / 6f,
            p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f,
            p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y,
        )
    }
    return path
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
