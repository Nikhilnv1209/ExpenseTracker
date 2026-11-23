package com.expensetracker.app.ui.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.data.local.ReminderEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.components.CategoryPanel
import com.expensetracker.app.ui.components.ReminderPanel
import com.expensetracker.app.ui.components.WarmBackdrop
import com.expensetracker.app.ui.feature.home.HomeViewModel
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import com.expensetracker.app.ui.feature.home.currencies
import com.expensetracker.app.ui.feature.home.formatAmount
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class View { DETAIL, CATEGORY, REMINDER }

/**
 * Full page for a transaction, matching the "Transaction Detail" design:
 * fixed header, floating card hero, grouped detail rows, Close + Save pills.
 */
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    currency: com.expensetracker.app.ui.feature.home.Currency = currencies[1],
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val transaction by viewModel.observeTransaction(transactionId).collectAsState(initial = null)
    var view by remember { mutableStateOf(View.DETAIL) }

    BackHandler { if (view != View.DETAIL) view = View.DETAIL else onBack() }

    if (transaction == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Transaction not found", color = AppTheme.colors.textMuted, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    val txn = transaction!!

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    val categoryRule by produceState(initialValue = null as Category?, key1 = txn.title) {
        value = viewModel.getCategoryRule(txn.title)
    }
    val reminder by produceState(
        initialValue = null as ReminderEntity?,
        key1 = txn.id,
        key2 = viewModel.hashCode(),
    ) { value = viewModel.getReminder(txn.id) }

    Box(modifier = Modifier.fillMaxSize()) {
        WarmBackdrop()

        // Fixed header
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, AppTheme.colors.hairline, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (view != View.DETAIL) view = View.DETAIL else onBack() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = AppTheme.colors.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Transaction Detail",
                style = MaterialTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AppTheme.colors.gold.copy(alpha = 0.18f), AppTheme.colors.secondary.copy(alpha = 0.12f)),
                        )
                    )
                    .border(1.dp, AppTheme.colors.hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = AppTheme.colors.gold, modifier = Modifier.size(18.dp))
            }
        }

        AnimatedContent(
            targetState = view,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 64.dp),
            transitionSpec = {
                (slideInVertically(tween(260)) { it } + fadeIn(tween(260))) togetherWith
                    (slideOutVertically(tween(260)) { -it } + fadeOut(tween(260)))
            },
            label = "detailView",
        ) { target ->
            when (target) {
                View.CATEGORY -> CategoryPanel(
                    title = txn.title,
                    currentCategory = txn.category,
                    onBack = { view = View.DETAIL },
                    onConfirm = { category, applyToAll ->
                        viewModel.setCategory(txn.id, txn.title, category, applyToAll)
                        view = View.DETAIL
                    },
                )
                View.REMINDER -> ReminderPanel(
                    transaction = txn,
                    existingReminder = reminder,
                    onBack = { view = View.DETAIL },
                    onConfirm = { daysBefore, customDate, hour, minute ->
                        viewModel.setReminder(txn, daysBefore, customDate, hour, minute)
                        view = View.DETAIL
                    },
                    onRemove = {
                        viewModel.removeReminder(txn.id)
                        view = View.DETAIL
                    },
                )
                View.DETAIL -> DetailBody(
                    txn = txn,
                    currency = currency,
                    categoryRule = categoryRule,
                    reminder = reminder,
                    onCategory = { view = View.CATEGORY },
                    onEditAlias = { editing = EditTarget.ALIAS },
                    onEditTitle = { editing = EditTarget.TITLE },
                    onEditNote = { editing = EditTarget.NOTE },
                    onReminder = { view = View.REMINDER },
                    onToggleExcluded = { viewModel.toggleExcluded(txn.id, it) },
                    onToggleCategoryExempt = { viewModel.setCategoryExempt(txn.id, txn.title, it) },
                    onDelete = { showDeleteConfirm = true },
                    onClose = onBack,
                    onSave = onBack,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete transaction?", color = AppTheme.colors.textPrimary) },
            text = {
                Text(
                    "This will permanently remove \"${txn.displayTitle}\".",
                    color = AppTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(txn.id)
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete", color = AppTheme.colors.expense) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = AppTheme.colors.textMuted) }
            },
            containerColor = AppTheme.colors.cardRaised,
        )
    }

    editing?.let { target ->
        val current = when (target) {
            EditTarget.TITLE -> txn.customTitle ?: ""
            EditTarget.ALIAS -> txn.alias ?: ""
            EditTarget.NOTE -> txn.note?.takeIf { txn.noteIsManual } ?: ""
        }
        var text by remember(target) { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = {
                Text(
                    when (target) {
                        EditTarget.TITLE -> "Rename transaction"
                        EditTarget.ALIAS -> "Set alias"
                        EditTarget.NOTE -> "Note"
                    },
                    color = AppTheme.colors.textPrimary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = when (target) {
                            EditTarget.TITLE -> "Only this transaction. Use Set Alias to rename every transaction with the parsed title."
                            EditTarget.ALIAS -> "Applies to all past and future transactions with title \"${txn.title}\"."
                            EditTarget.NOTE -> "A personal reason or note for this transaction."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textMuted,
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = target != EditTarget.NOTE,
                        minLines = if (target == EditTarget.NOTE) 3 else 1,
                        maxLines = if (target == EditTarget.NOTE) 4 else 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.textPrimary,
                            unfocusedTextColor = AppTheme.colors.textPrimary,
                            focusedBorderColor = AppTheme.colors.gold,
                            unfocusedBorderColor = AppTheme.colors.hairline,
                            cursorColor = AppTheme.colors.gold,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = text.trim().ifBlank { null }
                    when (target) {
                        EditTarget.TITLE -> viewModel.setCustomTitle(txn.id, value)
                        EditTarget.ALIAS -> viewModel.setAlias(txn.id, txn.title, value)
                        EditTarget.NOTE -> viewModel.setNote(txn.id, value)
                    }
                    editing = null
                }) { Text("Save", color = AppTheme.colors.gold) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Cancel", color = AppTheme.colors.textMuted) }
            },
            containerColor = AppTheme.colors.cardRaised,
        )
    }
}

private enum class EditTarget { TITLE, ALIAS, NOTE }

@Composable
private fun DetailBody(
    txn: Transaction,
    currency: com.expensetracker.app.ui.feature.home.Currency,
    categoryRule: Category?,
    reminder: ReminderEntity?,
    onCategory: () -> Unit,
    onEditAlias: () -> Unit,
    onEditTitle: () -> Unit,
    onEditNote: () -> Unit,
    onReminder: () -> Unit,
    onToggleExcluded: (Boolean) -> Unit,
    onToggleCategoryExempt: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    val c = AppTheme.colors
    val catColor = categoryColor(txn.category)
    var excluded by remember(txn.isExcluded) { mutableStateOf(txn.isExcluded) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Floating modal card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(c.card.copy(alpha = 0.72f))
                .border(1.dp, c.hairline, RoundedCornerShape(32.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(c.textFaint.copy(alpha = 0.4f)),
            )

            // Hero
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(84.dp)) {
                        val r = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(c.secondary.copy(alpha = 0.28f), Color.Transparent),
                                center = center,
                                radius = r,
                            ),
                            radius = r,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF25221F), Color(0xFF181513)),
                                )
                            )
                            .border(1.dp, c.gold.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIcon(txn.category),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                Text(
                    text = "${if (txn.isIncome) "+" else "−"}${formatAmount(txn.amount, currency)}",
                    style = AppType.amount(38),
                    color = if (txn.isIncome) c.incomeBright else c.expense,
                )

                Text(
                    text = txn.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = c.textPrimary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEditTitle,
                    ),
                )

                val dateTime = remember(txn.smsDate, txn.date) {
                    if (txn.smsDate != null) {
                        java.time.Instant.ofEpochMilli(txn.smsDate!!)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
                    } else {
                        txn.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, c.hairline, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = dateTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted,
                    )
                }
            }

            // Detail rows container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(1.dp, c.hairline, RoundedCornerShape(26.dp)),
            ) {
                DetailRow(
                    icon = Icons.Rounded.LocalDining,
                    iconTint = catColor,
                    label = "Category",
                    value = txn.category.displayName,
                    onClick = onCategory,
                )
                if (categoryRule != null) {
                    DetailDivider()
                    DetailRow(
                        icon = Icons.Rounded.Category,
                        iconTint = if (txn.categoryExempt) c.gold else c.secondary,
                        label = "Category rule",
                        value = if (txn.categoryExempt) "Keeping own" else "Following rule",
                        valueMuted = false,
                        onClick = { onToggleCategoryExempt(!txn.categoryExempt) },
                    )
                }
                DetailDivider()
                DetailRow(
                    icon = Icons.Rounded.Storefront,
                    iconTint = c.gold,
                    label = "Alias",
                    value = txn.alias ?: "None",
                    valueMuted = txn.alias == null,
                    onClick = onEditAlias,
                )
                DetailDivider()
                val manualNote = txn.note?.takeIf { txn.noteIsManual }
                DetailRow(
                    icon = Icons.Rounded.EditNote,
                    iconTint = c.goldSoft,
                    label = "Note",
                    value = manualNote ?: "Add a note",
                    valueMuted = manualNote == null,
                    onClick = onEditNote,
                )
                DetailDivider()
                val reminderLabel = when {
                    reminder == null -> "None"
                    reminder!!.customDate != null -> "On ${java.time.LocalDate.ofEpochDay(reminder!!.customDate).format(DateTimeFormatter.ofPattern("MMM d"))}"
                    reminder!!.daysBefore == 0 -> "On payment day"
                    else -> "${reminder!!.daysBefore}d before"
                }
                DetailRow(
                    icon = Icons.Rounded.NotificationsNone,
                    iconTint = c.textMuted,
                    label = "Reminder",
                    value = if (reminder != null) "$reminderLabel · %02d:%02d".format(reminder!!.hour, reminder!!.minute) else "None",
                    valueMuted = reminder == null,
                    onClick = onReminder,
                )
                DetailDivider()
                DetailRow(
                    icon = Icons.Rounded.CreditCard,
                    iconTint = c.gold,
                    label = "Bank / Card",
                    value = txn.displayAccount ?: "—",
                    onClick = {},
                )
                DetailDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c.textMuted.copy(alpha = 0.12f))
                            .border(1.dp, c.textMuted.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = c.textMuted,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "Exclude from totals",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = c.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Toggle(
                        checked = excluded,
                        onCheckedChange = {
                            excluded = it
                            onToggleExcluded(it)
                        },
                    )
                }
                DetailDivider()
                DetailRow(
                    icon = Icons.Rounded.DeleteOutline,
                    iconTint = c.expense,
                    label = "Delete transaction",
                    value = "",
                    valueColor = c.expense,
                    onClick = onDelete,
                )
            }

            // Close / Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, c.hairline, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClose,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Close", style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(c.goldSoft, c.gold, c.goldDeep),
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSave,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF14120E),
                    )
                }
            }

            if (!txn.rawSms.isNullOrBlank()) {
                Text(
                    text = "Original SMS",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.card.copy(alpha = 0.55f))
                        .border(1.dp, c.hairline, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        text = txn.rawSms,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueMuted: Boolean = false,
    valueColor: Color = AppTheme.colors.textPrimary,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f))
                .border(1.dp, iconTint.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = if (valueMuted) c.textFaint else valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = c.textFaint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DetailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppTheme.colors.hairline),
    )
}

@Composable
private fun Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 26.dp)
            .clip(CircleShape)
            .background(if (checked) c.secondary else Color(0xFF25221F))
            .border(1.dp, if (checked) c.secondary else c.hairline, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) c.canvas else c.textFaint),
        )
    }
}
