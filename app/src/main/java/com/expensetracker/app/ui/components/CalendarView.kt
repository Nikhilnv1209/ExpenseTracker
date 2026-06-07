package com.expensetracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.max

private val accentColor = Color(0xFF7C3AED)

data class DailyExpense(val day: Int, val total: Double)

@Composable
fun CalendarView(dailyExpenses: List<DailyExpense>, modifier: Modifier = Modifier) {
    var viewMode by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(-1) }
    var currentMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonthHeader(currentYear, currentMonth,
                    onPrev = {
                        if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
                        selectedDay = -1
                    },
                    onNext = {
                        if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
                        selectedDay = -1
                    },
                )
                ViewToggle(isChart = viewMode, onToggle = { viewMode = it })
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewMode) {
                LineChartView(dailyExpenses, currentMonth, currentYear, selectedDay) { selectedDay = it }
            } else {
                CalendarGridView(dailyExpenses, currentMonth, currentYear, selectedDay) { selectedDay = it }
            }

            if (selectedDay > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedDaySummary(selectedDay, currentMonth, currentYear,
                    dailyExpenses.find { it.day == selectedDay }?.total ?: 0.0)
            }
        }
    }
}

@Composable
private fun ViewToggle(isChart: Boolean, onToggle: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = !isChart,
            onClick = { onToggle(false) },
            label = { Text("Cal", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accentColor,
                selectedLabelColor = Color.White,
            ),
        )
        FilterChip(
            selected = isChart,
            onClick = { onToggle(true) },
            label = { Text("Chart", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accentColor,
                selectedLabelColor = Color.White,
            ),
        )
    }
}

@Composable
private fun MonthHeader(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("‹", fontSize = 22.sp, modifier = Modifier
            .clickable(onClick = onPrev)
            .padding(horizontal = 8.dp))
        Text(
            "${monthNames[month]} $year",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text("›", fontSize = 22.sp, modifier = Modifier
            .clickable(onClick = onNext)
            .padding(horizontal = 8.dp))
    }
}

// ---------- Calendar Grid ----------

@Composable
private fun CalendarGridView(
    expenses: List<DailyExpense>, month: Int, year: Int, selected: Int, onSelect: (Int) -> Unit,
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDay = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
    val maxAmt = expenses.maxOfOrNull { it.total }?.coerceAtLeast(1.0) ?: 1.0
    val today = Calendar.getInstance()
    val rows = (daysInMonth + firstDay + 6) / 7

    DayOfWeekLabels()
    Spacer(modifier = Modifier.height(6.dp))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                for (c in 0 until 7) {
                    val d = r * 7 + c - firstDay + 1
                    if (d in 1..daysInMonth) {
                        val exp = expenses.find { it.day == d }
                        val isToday = today.get(Calendar.YEAR) == year &&
                                today.get(Calendar.MONTH) == month &&
                                today.get(Calendar.DAY_OF_MONTH) == d
                        BarDayCell(d, exp, maxAmt, selected == d, isToday) { onSelect(d) }
                    } else {
                        Box(Modifier.size(44.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOfWeekLabels() {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
            Text(it, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BarDayCell(
    day: Int, expense: DailyExpense?, maxAmt: Double,
    isSelected: Boolean, isToday: Boolean, onClick: () -> Unit,
) {
    val bg = when {
        isSelected -> accentColor.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    val barH = if (expense != null) (expense.total / maxAmt * 22).dp.coerceIn(2.dp, 22.dp) else 0.dp

    Column(
        Modifier.width(44.dp).height(56.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            "$day", fontSize = 12.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when { isSelected -> accentColor; isToday -> accentColor; else -> MaterialTheme.colorScheme.onSurface },
        )
        Spacer(Modifier.height(4.dp))
        if (expense != null) {
            Box(Modifier.width(20.dp).height(barH)
                .background(accentColor, RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(2.dp))
    }
}

// ---------- Line Chart ----------

@Composable
private fun LineChartView(
    expenses: List<DailyExpense>, month: Int, year: Int,
    selectedDay: Int, onSelect: (Int) -> Unit,
) {
    var weekOffset by remember { mutableIntStateOf(0) }

    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year); set(Calendar.MONTH, month)
        firstDayOfWeek = Calendar.MONDAY
    }

    // Calculate Monday of the current week
    val today = Calendar.getInstance()
    val currentMonday = Calendar.getInstance().apply {
        time = today.time
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    cal.time = currentMonday.time
    cal.add(Calendar.DAY_OF_MONTH, weekOffset * 7)

    val weekStart = Calendar.getInstance().apply { time = cal.time }
    val weekEnd = Calendar.getInstance().apply {
        time = cal.time; add(Calendar.DAY_OF_MONTH, 6)
    }

    val weekStartDay = weekStart.get(Calendar.DAY_OF_MONTH)
    val daysInWeek = (1..7).map { offset ->
        val d = Calendar.getInstance().apply { time = weekStart.time; add(Calendar.DAY_OF_MONTH, offset - 1) }
        d.get(Calendar.DAY_OF_MONTH)
    }

    val points = daysInWeek.map { day ->
        day to (expenses.find { it.day == day }?.total ?: 0.0)
    }
    val maxAmt = points.maxOf { it.second }.coerceAtLeast(1.0)

    val monthNamesShort = arrayOf("Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec")

    Column {
        // Week navigation
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${monthNamesShort[month]} ${weekStartDay} - ${monthNamesShort[weekEnd.get(Calendar.MONTH)]} ${weekEnd.get(Calendar.DAY_OF_MONTH)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Row {
                Text("‹", fontSize = 18.sp,
                    modifier = Modifier.clickable { weekOffset-- }.padding(horizontal = 8.dp))
                Text("›", fontSize = 18.sp,
                    modifier = Modifier.clickable { weekOffset++ }.padding(horizontal = 8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val w = size.width
            val h = size.height
            val margin = 12f
            val step = w / 6f // 6 gaps for 7 days

            val coords = points.map { (day, amt) ->
                val x = daysInWeek.indexOf(day) * step
                val y = (h - ((amt / maxAmt) * (h - margin * 2) + margin)).toFloat()
                Offset(x, y)
            }

            if (coords.isEmpty() || coords.all { it.y == h }) return@Canvas

            // Smooth path using cubic bezier
            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val p0 = if (i > 0) coords[i - 1] else coords[i]
                    val p1 = coords[i]
                    val p2 = coords[i + 1]
                    val p3 = if (i + 2 < coords.size) coords[i + 2] else coords[i + 1]

                    val cp1 = Offset(
                        x = p1.x + (p2.x - p0.x) / 6f,
                        y = p1.y + (p2.y - p0.y) / 6f,
                    )
                    val cp2 = Offset(
                        x = p2.x - (p3.x - p1.x) / 6f,
                        y = p2.y - (p3.y - p1.y) / 6f,
                    )
                    cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                }
            }

            // Gradient fill under curve
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, h)
                lineTo(coords.first().x, h)
                close()
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    0f to accentColor.copy(alpha = 0.35f),
                    1f to accentColor.copy(alpha = 0.0f),
                ),
            )

            // Line
            drawPath(linePath, color = accentColor, style = Stroke(width = 3f))

            // Dots
            coords.forEach { c ->
                drawCircle(Color.White, 4f, c)
                drawCircle(accentColor, center = c, radius = 5f)
            }
        }

        Spacer(Modifier.height(6.dp))

        // Day labels
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEachIndexed { i, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("${daysInWeek[i]}", fontSize = 11.sp,
                        fontWeight = if (daysInWeek[i] == selectedDay) FontWeight.Bold else FontWeight.Normal,
                        color = if (daysInWeek[i] == selectedDay) accentColor
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ---------- Summary ----------

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
