package com.expensetracker.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import com.expensetracker.app.data.local.ReminderEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

private enum class DetailView { DETAIL, CATEGORY, REMINDER }

@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    currency: com.expensetracker.app.ui.feature.home.Currency,
    categoryRule: Category? = null,
    reminder: ReminderEntity? = null,
    onToggleExcluded: (Boolean) -> Unit = {},
    onSetAlias: (String?) -> Unit = {},
    onSetNote: (String?) -> Unit = {},
    onSetCategory: (Category, Boolean) -> Unit = { _, _ -> },
    onSetCategoryExempt: (Boolean) -> Unit = {},
    onSetReminder: (Int, Long?, Int, Int) -> Unit = { _, _, _, _ -> },
    onRemoveReminder: () -> Unit = {},
) {
    val catColor = categoryColor(transaction.category)
    val displayTitle = transaction.alias ?: transaction.title
    var isEditingAlias by remember { mutableStateOf(false) }
    var aliasText by remember { mutableStateOf(transaction.alias ?: "") }
    var activeView by remember { mutableStateOf(DetailView.DETAIL) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(catColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.category),
                    contentDescription = transaction.category.displayName,
                    tint = catColor,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (transaction.alias != null) {
                    Text(
                        text = "Originally: ${transaction.title}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                Text(
                    text = transaction.category.displayName,
                    fontSize = 14.sp,
                    color = catColor,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        val detailScrollState = rememberScrollState()
        AnimatedContent(
            targetState = activeView,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                if (targetState != DetailView.DETAIL) {
                    slideInVertically(animationSpec = tween(320)) { it } togetherWith
                        slideOutVertically(animationSpec = tween(320)) { -it }
                } else {
                    slideInVertically(animationSpec = tween(320)) { -it } togetherWith
                        slideOutVertically(animationSpec = tween(320)) { it }
                }
            },
            contentAlignment = Alignment.TopStart,
            label = "detailView",
        ) { view ->
            when (view) {
                DetailView.CATEGORY -> CategoryPanel(
                    title = transaction.title,
                    currentCategory = transaction.category,
                    onBack = { activeView = DetailView.DETAIL },
                    onConfirm = { category, applyToAll ->
                        onSetCategory(category, applyToAll)
                        activeView = DetailView.DETAIL
                    },
                )
                DetailView.REMINDER -> ReminderPanel(
                    transaction = transaction,
                    existingReminder = reminder,
                    onBack = { activeView = DetailView.DETAIL },
                    onConfirm = { daysBefore, customDate, hour, minute ->
                        onSetReminder(daysBefore, customDate, hour, minute)
                        activeView = DetailView.DETAIL
                    },
                    onRemove = {
                        onRemoveReminder()
                        activeView = DetailView.DETAIL
                    },
                )
                DetailView.DETAIL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(detailScrollState),
                    ) {
            GlassCard(
                shape = RoundedCornerShape(12.dp),
                tint = Color(0xFF7C3AED),
                tintAlpha = 0.06f,
                borderAlpha = 0.08f,
            ) {
            if (isEditingAlias) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Set Alias",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This alias will be applied to all past and future transactions with title \"${transaction.title}\"",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aliasText,
                        onValueChange = { aliasText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter alias...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF7C3AED),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { isEditingAlias = false; aliasText = transaction.alias ?: "" }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier
                                .clickable {
                                    val trimmed = aliasText.trim()
                                    onSetAlias(trimmed.ifBlank { null })
                                    isEditingAlias = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditingAlias = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit alias",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                        text = if (transaction.alias != null) "Alias: ${transaction.alias}" else "Set Alias",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7C3AED),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (transaction.alias != null) "Tap to edit or remove" else "Applies to all matching transactions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(
            shape = RoundedCornerShape(12.dp),
            tint = catColor,
            tintAlpha = 0.06f,
            borderAlpha = 0.08f,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeView = DetailView.CATEGORY }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(catColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon(transaction.category),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Category",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (categoryRule != null && categoryRule == transaction.category)
                            "${transaction.category.displayName} · Rule for \"${transaction.title}\""
                        else transaction.category.displayName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit category",
                    tint = catColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (categoryRule != null) {
            Spacer(Modifier.height(16.dp))
            GlassCard(
                shape = RoundedCornerShape(12.dp),
                tint = if (transaction.categoryExempt) Color(0xFFFF9800) else catColor,
                tintAlpha = 0.06f,
                borderAlpha = 0.08f,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetCategoryExempt(!transaction.categoryExempt) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = if (transaction.categoryExempt) Color(0xFFFF9800) else catColor,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!transaction.categoryExempt) {
                            Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (transaction.categoryExempt)
                                "Keeping own category"
                            else
                                "Following \"${transaction.title}\" category rule",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (transaction.categoryExempt) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (transaction.categoryExempt)
                                "Tap to follow the category rule"
                            else
                                "Tap to keep this transaction separate",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val note = transaction.note
        var isEditingNote by remember { mutableStateOf(false) }
        var noteText by remember(transaction.note) { mutableStateOf(note ?: "") }

        GlassCard(
            shape = RoundedCornerShape(12.dp),
            tint = Color(0xFF7C3AED),
            tintAlpha = 0.06f,
            borderAlpha = 0.08f,
        ) {
            if (isEditingNote) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Note",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a reason or note...") },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF7C3AED),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { isEditingNote = false; noteText = note ?: "" }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier
                                .clickable {
                                    val trimmed = noteText.trim()
                                    onSetNote(trimmed.ifBlank { null })
                                    isEditingNote = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditingNote = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit note",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Note",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7C3AED),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            text = note ?: "Add a reason or note",
                            fontSize = 12.sp,
                            color = if (note != null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(
            shape = RoundedCornerShape(16.dp),
            tint = catColor,
            tintAlpha = 0.06f,
            borderAlpha = 0.08f,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Type", if (transaction.isIncome) "Income" else "Expense")
                DetailRow("Amount", com.expensetracker.app.ui.feature.home.formatAmount(transaction.amount, currency),
                    valueColor = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFE91E63))
                DetailRow("Date", transaction.date.format(fullDateFormatter))
                transaction.bankName?.let { DetailRow("Bank", it) }
                transaction.accountLast4?.let { DetailRow("Account", "****$it") }
                transaction.note?.let { DetailRow("Note", it) }
            }
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(
            shape = RoundedCornerShape(12.dp),
            tint = if (reminder != null) Color(0xFF2196F3) else Color(0xFF7C3AED),
            tintAlpha = 0.06f,
            borderAlpha = 0.08f,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeView = DetailView.REMINDER }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Reminder",
                    tint = if (reminder != null) Color(0xFF2196F3) else Color(0xFF7C3AED),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (reminder != null) {
                            if (reminder.customDate != null) "Custom reminder set"
                            else if (reminder.daysBefore == 0) "Reminder on payment day"
                            else "Reminder ${reminder.daysBefore} day${if (reminder.daysBefore > 1) "s" else ""} before"
                        } else "Set Reminder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (reminder != null) Color(0xFF2196F3) else Color(0xFF7C3AED),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (reminder != null) {
                            val timeStr = "%02d:%02d".format(reminder.hour, reminder.minute)
                            if (reminder.customDate != null) {
                                val date = java.time.LocalDate.ofEpochDay(reminder.customDate)
                                "On ${date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))} at $timeStr"
                            } else {
                                "At $timeStr · tap to edit"
                            }
                        } else "Get notified before next payment",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(
            shape = RoundedCornerShape(16.dp),
            tint = if (transaction.isExcluded) Color(0xFFFF9800) else Color.White,
            tintAlpha = if (transaction.isExcluded) 0.1f else 0.04f,
            borderAlpha = if (transaction.isExcluded) 0.15f else 0.06f,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExcluded(!transaction.isExcluded) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (transaction.isExcluded) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (transaction.isExcluded) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (transaction.isExcluded) "Excluded from totals" else "Exclude from totals",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (transaction.isExcluded) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (transaction.isExcluded) "Tap to include this transaction" else "Excluded transactions won't affect balance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (transaction.rawSms != null) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Original SMS",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(8.dp))

            GlassCard(
                shape = RoundedCornerShape(12.dp),
                tint = Color.White,
                tintAlpha = 0.04f,
                borderAlpha = 0.06f,
            ) {
                Text(
                    text = transaction.rawSms,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ReminderPanel(
    transaction: Transaction,
    existingReminder: ReminderEntity?,
    onBack: () -> Unit,
    onConfirm: (daysBefore: Int, customDate: Long?, hour: Int, minute: Int) -> Unit,
    onRemove: () -> Unit,
) {
    var selectedDaysBefore by remember { mutableStateOf(existingReminder?.daysBefore ?: 1) }
    var useCustomDate by remember { mutableStateOf(existingReminder?.customDate != null) }
    var customDate by remember {
        mutableStateOf(existingReminder?.customDate ?: java.time.LocalDate.now().toEpochDay())
    }
    var hour by remember { mutableStateOf(existingReminder?.hour ?: 9) }
    var minute by remember { mutableStateOf(existingReminder?.minute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "Set Reminder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(12.dp))

            GlassCard(
                shape = RoundedCornerShape(14.dp),
                tint = Color(0xFF7C3AED),
                tintAlpha = 0.05f,
                borderAlpha = 0.07f,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (!useCustomDate) {
                        listOf(0 to "On payment day", 1 to "1 day before", 3 to "3 days before", 7 to "1 week before").forEach { (days, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDaysBefore = days }
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedDaysBefore == days, onClick = { selectedDaysBefore = days })
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (days == 0) "$label (day ${transaction.date.dayOfMonth})" else label,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { useCustomDate = !useCustomDate }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = useCustomDate, onClick = { useCustomDate = !useCustomDate })
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Custom date (one-time)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (useCustomDate) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                                .background(Color(0xFF2196F3).copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = java.time.LocalDate.ofEpochDay(customDate)
                                    .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2196F3),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            GlassCard(
                shape = RoundedCornerShape(14.dp),
                tint = Color(0xFF2196F3),
                tintAlpha = 0.07f,
                borderAlpha = 0.1f,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "%02d:%02d %s".format(
                                if (hour % 12 == 0) 12 else hour % 12,
                                minute,
                                if (hour < 12) "AM" else "PM",
                            ),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                        )
                    }
                    Text(
                        text = "Change",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2196F3).copy(alpha = 0.7f),
                    )
                }
            }

            if (existingReminder != null) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRemove() }
                        .background(Color(0xFFE91E63).copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Remove reminder",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE91E63),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (useCustomDate) {
                    onConfirm(0, customDate, hour, minute)
                } else {
                    onConfirm(selectedDaysBefore, null, hour, minute)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Save")
        }
    }

    if (showTimePicker) {
        var tempHour by remember { mutableStateOf(if (hour % 12 == 0) 12 else hour % 12) }
        var tempMinute by remember { mutableStateOf(minute) }
        var isAM by remember { mutableStateOf(hour < 12) }

        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showTimePicker = false },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1A1825))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.04f))
                            ),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Pick time",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WheelTimeColumn(
                                value = tempHour,
                                onValueChange = { tempHour = it },
                                range = 1..12,
                            )

                            Text(
                                text = ":",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )

                            WheelTimeColumn(
                                value = tempMinute,
                                onValueChange = { tempMinute = it },
                                range = 0..59,
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2A2540)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                        .background(if (isAM) Color(0xFF7C3AED) else Color.Transparent)
                                        .clickable { isAM = true }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = "AM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAM) Color.White else Color.White.copy(alpha = 0.3f),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                                        .background(if (!isAM) Color(0xFF7C3AED) else Color.Transparent)
                                        .clickable { isAM = false }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = "PM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isAM) Color.White else Color.White.copy(alpha = 0.3f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clickable { showTimePicker = false }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "OK",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier
                                    .clickable {
                                        val h = if (isAM) {
                                            if (tempHour == 12) 0 else tempHour
                                        } else {
                                            if (tempHour == 12) 12 else tempHour + 12
                                        }
                                        hour = h
                                        minute = tempMinute
                                        showTimePicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        CustomDatePickerDialog(
            selectedDate = customDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                customDate = it
                showDatePicker = false
            },
        )
    }
}

private val shortMonthsArray = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

@Composable
private fun WheelTimeColumn(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
) {
    val itemHeightDp = 40.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.toPx() }
    val total = range.last - range.first + 1
    val visibleCount = 3
    val halfVisible = visibleCount / 2

    val scrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val centerFloat = value - range.first - scrollOffset.value / itemHeightPx
    val roundedCenter = centerFloat.roundToInt()
    val frac = centerFloat - roundedCenter.toFloat()

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(itemHeightDp * visibleCount)
            .pointerInput(value, range) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val currentPx = scrollOffset.value
                            val snappedItems = (currentPx / itemHeightPx).roundToInt()
                            val snappedPx = snappedItems * itemHeightPx
                            scrollOffset.animateTo(snappedPx)
                            val newIndex = ((value - range.first - snappedItems) % total + total) % total
                            onValueChange(range.first + newIndex)
                            scrollOffset.snapTo(0f)
                        }
                    },
                ) { _, dragAmount ->
                    scope.launch {
                        scrollOffset.snapTo(scrollOffset.value + dragAmount)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        for (i in -halfVisible..halfVisible) {
            val valueIndex = ((roundedCenter + i) % total + total) % total
            val num = range.first + valueIndex

            var dist: Float = valueIndex - centerFloat
            if (dist > total / 2f) dist -= total
            if (dist < -total / 2f) dist += total
            val absDist = abs(dist)

            val alpha = (1f - absDist / (halfVisible + 1f)).coerceIn(0f, 1f)
            val scale = (1f - absDist * 0.1f).coerceIn(0.7f, 1f)
            val yPx = (i - frac) * itemHeightPx

            Text(
                text = "%02d".format(num),
                fontSize = 22.sp,
                fontWeight = if (absDist < 0.5f) FontWeight.Bold else FontWeight.Medium,
                color = Color.White.copy(alpha = alpha),
                modifier = Modifier
                    .offset { IntOffset(0, yPx.roundToInt()) }
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                    },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1825), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF1A1825)),
                    ),
                ),
        )
    }
}
private val fullMonthsArray = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")
private val dowLabels = listOf("M","T","W","T","F","S","S")

@Composable
private fun CustomDatePickerDialog(
    selectedDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val initial = java.time.LocalDate.ofEpochDay(selectedDate)
    var displayMonth by remember { mutableStateOf(initial.monthValue - 1) }
    var displayYear by remember { mutableStateOf(initial.year) }
    var pickedDay by remember { mutableStateOf(initial.dayOfMonth) }

    val today = java.time.LocalDate.now()
    val daysInMonth = java.time.YearMonth.of(displayYear, displayMonth + 1).lengthOfMonth()
    val firstDayOfWeek = java.time.LocalDate.of(displayYear, displayMonth + 1, 1).dayOfWeek.value

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A1825))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.04f))
                        ),
                        shape = RoundedCornerShape(24.dp),
                    ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Pick date",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    if (displayMonth == 0) { displayMonth = 11; displayYear-- } else displayMonth--
                                    pickedDay = pickedDay.coerceAtMost(
                                        java.time.YearMonth.of(displayYear, displayMonth + 1).lengthOfMonth()
                                    )
                                }
                                .background(Color(0xFF7C3AED).copy(alpha = 0.12f), CircleShape),
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
                                .clickable {
                                    if (displayMonth == 11) { displayMonth = 0; displayYear++ } else displayMonth++
                                    pickedDay = pickedDay.coerceAtMost(
                                        java.time.YearMonth.of(displayYear, displayMonth + 1).lengthOfMonth()
                                    )
                                }
                                .background(Color(0xFF7C3AED).copy(alpha = 0.12f), CircleShape),
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                                val isPicked = isCurrentMonth && dayIndex == pickedDay
                                val isToday = isCurrentMonth &&
                                    displayYear == today.year &&
                                    displayMonth == today.monthValue - 1 &&
                                    dayIndex == today.dayOfMonth

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            if (isCurrentMonth) pickedDay = dayIndex
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
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "OK",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier
                                .clickable {
                                    onConfirm(java.time.LocalDate.of(displayYear, displayMonth + 1, pickedDay).toEpochDay())
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPanel(
    title: String,
    currentCategory: Category,
    onBack: () -> Unit,
    onConfirm: (Category, Boolean) -> Unit,
) {
    var selected by remember { mutableStateOf(currentCategory) }
    var applyToAll by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "Change category",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Category.entries.toList()) { cat ->
                val catColor = categoryColor(cat)
                val isSelected = cat == selected
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = cat },
                    shape = RoundedCornerShape(16.dp),
                    tint = catColor,
                    tintAlpha = if (isSelected) 0.22f else 0.05f,
                    borderAlpha = if (isSelected) 0.55f else 0.08f,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = if (isSelected) catColor else catColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = categoryIcon(cat),
                                contentDescription = cat.displayName,
                                tint = if (isSelected) Color.White else catColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = cat.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Apply to",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { applyToAll = true }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = applyToAll, onClick = { applyToAll = true })
            Spacer(Modifier.width(4.dp))
            Text(
                text = "All \"$title\" transactions (past & future)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { applyToAll = false }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = !applyToAll, onClick = { applyToAll = false })
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Only this transaction",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (applyToAll) {
            Text(
                text = "Creates a rule so future transactions from \"$title\" (including aliases) use this category.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onConfirm(selected, applyToAll) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Save")
        }
    }
}
