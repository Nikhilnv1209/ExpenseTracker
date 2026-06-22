package com.expensetracker.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class AddView { FORM, CATEGORY, DATE }

@Composable
fun AddTransactionSheet(
    isIncome: Boolean,
    onSave: (title: String, amount: Double, category: Category, date: LocalDate, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(if (isIncome) Category.SALARY else Category.FOOD)
    }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var activeView by remember { mutableStateOf(AddView.FORM) }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    AnimatedContent(
        targetState = activeView,
        transitionSpec = {
            if (targetState == AddView.FORM) {
                slideInVertically { it / 4 } + fadeIn() togetherWith slideOutVertically { -it / 4 } + fadeOut()
            } else {
                slideInVertically { -it / 4 } + fadeIn() togetherWith slideOutVertically { it / 4 } + fadeOut()
            }
        },
        label = "addView",
    ) { view ->
        when (view) {
            AddView.FORM -> {
                val accentColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFFF5722)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onDismiss() },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isIncome) "Add Income" else "Add Expense",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF7C3AED),
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = Color(0xFF7C3AED),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            ),
                        )

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Amount") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF7C3AED),
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = Color(0xFF7C3AED),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            ),
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Category",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        CategoryChip(
                            category = selectedCategory,
                            onClick = { activeView = AddView.CATEGORY },
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Date",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { activeView = AddView.DATE }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = date.format(dateFormatter),
                                fontSize = 15.sp,
                                color = Color.White,
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF7C3AED),
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = Color(0xFF7C3AED),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                            ),
                        )

                        Spacer(Modifier.height(28.dp))

                        val canSave = title.isNotBlank() && amountText.toDoubleOrNull()?.let { it > 0 } == true
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (canSave) Brush.verticalGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
                                    ) else Brush.verticalGradient(
                                        listOf(Color(0xFF7C3AED).copy(alpha = 0.2f), Color(0xFF5B21B6).copy(alpha = 0.2f))
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (canSave) {
                                        onSave(
                                            title.trim(),
                                            amountText.toDouble(),
                                            selectedCategory,
                                            date,
                                            note.ifBlank { null },
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Save",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canSave) Color.White else Color.White.copy(alpha = 0.4f),
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }

            AddView.CATEGORY -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding(),
                ) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { activeView = AddView.FORM },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            text = "Select category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val categories = if (isIncome) {
                        listOf(Category.SALARY, Category.CASH, Category.OTHER)
                    } else {
                        listOf(
                            Category.FOOD, Category.TRANSPORT, Category.SHOPPING,
                            Category.ENTERTAINMENT, Category.HEALTH, Category.BILLS,
                            Category.GROCERY, Category.OTHER,
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) categoryColor(cat).copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.04f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) categoryColor(cat)
                                        else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        selectedCategory = cat
                                        activeView = AddView.FORM
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor(cat).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = cat.displayName,
                                        tint = categoryColor(cat),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = cat.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            AddView.DATE -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding(),
                ) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { activeView = AddView.FORM },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            text = "Pick date",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    SimpleCalendarPicker(
                        selectedDate = date,
                        onDateSelected = {
                            date = it
                            activeView = AddView.FORM
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: Category, onClick: () -> Unit) {
    val catColor = categoryColor(category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = category.displayName,
            fontSize = 15.sp,
            color = Color.White,
        )
    }
}

@Composable
private fun SimpleCalendarPicker(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var displayMonth by remember { mutableStateOf(selectedDate.monthValue - 1) }
    var displayYear by remember { mutableStateOf(selectedDate.year) }
    val today = LocalDate.now()
    val daysInMonth = java.time.YearMonth.of(displayYear, displayMonth + 1).lengthOfMonth()
    val firstDayOfWeek = java.time.LocalDate.of(displayYear, displayMonth + 1, 1).dayOfWeek.value
    val fullMonthsArray = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
    val dowLabels = listOf("M","T","W","T","F","S","S")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C3AED).copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("\u276E", fontSize = 14.sp, color = Color(0xFF7C3AED))
            }
            Text(
                text = "${fullMonthsArray[displayMonth]} $displayYear",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C3AED).copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("\u276F", fontSize = 14.sp, color = Color(0xFF7C3AED))
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            dowLabels.forEach { d ->
                Text(
                    text = d,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val totalCells = ((firstDayOfWeek - 1 + daysInMonth + 6) / 7) * 7
        val rows = totalCells / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - (firstDayOfWeek - 1) + 1
                    val isCurrentMonth = dayIndex in 1..daysInMonth
                    val isPicked = isCurrentMonth && dayIndex == selectedDate.dayOfMonth &&
                        displayMonth == selectedDate.monthValue - 1 && displayYear == selectedDate.year
                    val isToday = isCurrentMonth &&
                        displayYear == today.year &&
                        displayMonth == today.monthValue - 1 &&
                        dayIndex == today.dayOfMonth

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (isCurrentMonth) {
                                    onDateSelected(LocalDate.of(displayYear, displayMonth + 1, dayIndex))
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = when {
                                        isPicked -> Color(0xFF7C3AED)
                                        isToday -> Color(0xFF7C3AED).copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    },
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isCurrentMonth) "$dayIndex" else "",
                                fontSize = 13.sp,
                                fontWeight = if (isPicked || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isPicked -> Color.White
                                    isToday -> Color(0xFF7C3AED)
                                    else -> Color.White.copy(alpha = 0.7f)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
