package com.expensetracker.app.ui.feature.detail

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.data.local.ReminderEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.components.ReminderPanel
import com.expensetracker.app.ui.components.WarmBackdrop
import com.expensetracker.app.ui.feature.home.HomeViewModel
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import com.expensetracker.app.ui.feature.home.currencies
import com.expensetracker.app.ui.feature.home.formatAmount
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class Overlay { NONE, CATEGORY, ALIAS, REMINDER }

/**
 * Transaction Detail, matching the "alias / note / rename" design:
 * compact hero with name, note and time pills, two-line detail rows with
 * badges, separate SMS provenance card, and Close / Save pills.
 */
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    currency: com.expensetracker.app.ui.feature.home.Currency = currencies[1],
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val c = AppTheme.colors
    val transaction by viewModel.observeTransaction(transactionId).collectAsState(initial = null)
    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var lastSheet by remember { mutableStateOf(Overlay.CATEGORY) }
    if (overlay != Overlay.NONE) lastSheet = overlay
    val sheetVisible = overlay != Overlay.NONE

    BackHandler { if (overlay != Overlay.NONE) overlay = Overlay.NONE else onBack() }

    val txn = transaction
    if (txn == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Transaction not found", color = c.textMuted, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val categoryRule by produceState(initialValue = null as Category?, key1 = txn.title) {
        value = viewModel.getCategoryRule(txn.title)
    }
    val reminder by produceState(
        initialValue = null as ReminderEntity?,
        key1 = txn.id,
        key2 = viewModel.hashCode(),
    ) { value = viewModel.getReminder(txn.id) }
    val aliasCount by produceState(initialValue = 1, key1 = txn.title) {
        value = viewModel.getAliasImpactCount(txn.title)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WarmBackdrop()

        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed header
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, c.hairline, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (overlay != Overlay.NONE) overlay = Overlay.NONE else onBack() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = c.textSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Transaction Detail",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(c.gold.copy(alpha = 0.2f), c.secondary.copy(alpha = 0.2f)),
                            )
                        )
                        .border(1.dp, c.hairline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = c.gold, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Main modal card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(c.card.copy(alpha = 0.82f))
                        .border(1.dp, c.hairline, RoundedCornerShape(32.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 44.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(c.textFaint.copy(alpha = 0.4f)),
                    )

                    Hero(
                        txn = txn,
                        currency = currency,
                        onSaveTitle = { viewModel.setCustomTitle(txn.id, it) },
                        onSaveNote = { note -> viewModel.setNote(txn.id, note) },
                    )

                    DetailRows(
                        txn = txn,
                        categoryRule = categoryRule,
                        reminder = reminder,
                        aliasCount = aliasCount,
                        onCategory = { overlay = Overlay.CATEGORY },
                        onAlias = { overlay = Overlay.ALIAS },
                        onReminder = { overlay = Overlay.REMINDER },
                        onToggleExcluded = { viewModel.toggleExcluded(txn.id, it) },
                        onToggleCategoryExempt = { viewModel.setCategoryExempt(txn.id, txn.title, it) },
                        onDelete = {
                            viewModel.deleteTransaction(txn.id)
                            onBack()
                        },
                    )

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
                                    onClick = onBack,
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
                                .background(Brush.horizontalGradient(listOf(Color(0xFFE6C786), Color(0xFFC49F58))))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onBack,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF14120E),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // SMS provenance card
                val rawSms = txn.rawSms
                if (!rawSms.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(c.card.copy(alpha = 0.82f))
                            .border(1.dp, c.hairline, RoundedCornerShape(26.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Sms,
                                contentDescription = null,
                                tint = c.gold,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = (txn.bankName ?: "SMS").uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp,
                                ),
                                color = c.textPrimary,
                            )
                            Spacer(Modifier.weight(1f))
                            if (txn.smsDate != null) {
                                Text(
                                    text = formatShort(txn.smsDate!!),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textSecondary,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = "Parsed on-device",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = c.income,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(c.income.copy(alpha = 0.1f))
                                    .border(1.dp, c.income.copy(alpha = 0.2f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = rawSms,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 19.sp,
                            ),
                            color = c.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(1.dp, c.hairline, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                        )
                    }
                }
            }
        }

        // Sheets stay mounted while closing so they can slide away.
        when (lastSheet) {
            Overlay.CATEGORY -> CategoryRulesSheet(
                merchant = txn.title,
                currentCategory = txn.category,
                currentRule = categoryRule,
                currentAlias = txn.alias,
                visible = sheetVisible,
                onDismiss = { overlay = Overlay.NONE },
                onApply = { category, createRule, alias ->
                    viewModel.setCategory(txn.id, txn.title, category, createRule)
                    if (alias != null) viewModel.setAlias(txn.id, txn.title, alias)
                    overlay = Overlay.NONE
                },
            )

            Overlay.ALIAS -> MerchantAliasSheet(
                merchant = txn.title,
                currentAlias = txn.alias,
                transactionCount = aliasCount,
                visible = sheetVisible,
                onDismiss = { overlay = Overlay.NONE },
                onApply = { value ->
                    viewModel.setAlias(txn.id, txn.title, value)
                    overlay = Overlay.NONE
                },
            )

            else -> Unit
        }

        if (overlay == Overlay.REMINDER) {
            ReminderPanel(
                transaction = txn,
                existingReminder = reminder,
                onBack = { overlay = Overlay.NONE },
                onConfirm = { daysBefore, customDate, hour, minute ->
                    viewModel.setReminder(txn, daysBefore, customDate, hour, minute)
                    overlay = Overlay.NONE
                },
                onRemove = {
                    viewModel.removeReminder(txn.id)
                    overlay = Overlay.NONE
                },
            )
        }
    }
}

/* ---------------------------------- hero ---------------------------------- */

@Composable
private fun Hero(
    txn: Transaction,
    currency: com.expensetracker.app.ui.feature.home.Currency,
    onSaveTitle: (String?) -> Unit,
    onSaveNote: (String?) -> Unit,
) {
    val c = AppTheme.colors
    val catColor = categoryColor(txn.category)
    val manualNote = txn.note?.takeIf { txn.noteIsManual }
    var editingTitle by remember(txn.id) { mutableStateOf(false) }
    var titleText by remember(txn.id, txn.customTitle) { mutableStateOf(txn.customTitle ?: "") }
    var editingNote by remember(txn.id) { mutableStateOf(false) }
    var noteText by remember(txn.id, manualNote) { mutableStateOf(manualNote ?: "") }
    val titleFocus = remember { FocusRequester() }
    val noteFocus = remember { FocusRequester() }

    LaunchedEffect(editingTitle) { if (editingTitle) titleFocus.requestFocus() }
    LaunchedEffect(editingNote) { if (editingNote) noteFocus.requestFocus() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(64.dp)) {
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF25221F), Color(0xFF181513))))
                    .border(1.dp, c.gold.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(txn.category),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            text = "${if (txn.isIncome) "+" else "−"}${formatAmount(txn.amount, currency)}",
            style = AppType.amount(30),
            color = if (txn.isIncome) c.incomeBright else c.expense,
        )

        // Title — per-transaction name, edits inline
        if (editingTitle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.025f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = c.gold.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "This transaction only",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = c.textFaint,
                        modifier = Modifier.weight(1f),
                    )
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (titleText.isEmpty()) {
                        Text(
                            text = txn.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = c.textFaint,
                        )
                    }
                    BasicTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, color = c.textPrimary),
                        cursorBrush = SolidColor(c.gold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocus),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (txn.customTitle != null) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = c.textMuted,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    titleText = ""
                                    editingTitle = false
                                    onSaveTitle(null)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                        color = c.textMuted,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                titleText = txn.customTitle ?: ""
                                editingTitle = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                        color = c.gold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                editingTitle = false
                                onSaveTitle(titleText.trim().ifBlank { null })
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { editingTitle = true }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = txn.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Rename transaction",
                    tint = c.gold.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(13.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { editingTitle = true },
                )
            }
        }

        // Note — edits inline right under the title
        if (editingNote) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.025f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.StickyNote2,
                        contentDescription = null,
                        tint = c.gold.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Private to you · this transaction only",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = c.textFaint,
                        modifier = Modifier.weight(1f),
                    )
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (noteText.isEmpty()) {
                        Text(
                            text = "Why this spend? e.g. split with Rahul",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textFaint,
                        )
                    }
                    BasicTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.textPrimary),
                        cursorBrush = SolidColor(c.gold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 38.dp, max = 76.dp)
                            .focusRequester(noteFocus),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (manualNote != null) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = c.expense,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    noteText = ""
                                    editingNote = false
                                    onSaveNote(null)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = c.textMuted,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                noteText = manualNote ?: ""
                                editingNote = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                        color = c.gold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                editingNote = false
                                onSaveNote(noteText.trim().ifBlank { null })
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { editingNote = true }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.StickyNote2,
                    contentDescription = null,
                    tint = c.gold,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = manualNote ?: "Add a note",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                    color = if (manualNote == null) c.textFaint else c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Edit note",
                    tint = c.gold.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(13.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { editingNote = true },
                )
            }
        }

        // Time pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = c.gold.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = heroTimestamp(txn),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = c.textSecondary,
            )
        }
    }
}

/* --------------------------------- rows ---------------------------------- */

@Composable
private fun DetailRows(
    txn: Transaction,
    categoryRule: Category?,
    reminder: ReminderEntity?,
    aliasCount: Int,
    onCategory: () -> Unit,
    onAlias: () -> Unit,
    onReminder: () -> Unit,
    onToggleExcluded: (Boolean) -> Unit,
    onToggleCategoryExempt: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val c = AppTheme.colors
    val catColor = categoryColor(txn.category)
    var excluded by remember(txn.isExcluded) { mutableStateOf(txn.isExcluded) }
    var deleteArmed by remember { mutableStateOf(false) }

    LaunchedEffect(deleteArmed) {
        if (deleteArmed) {
            delay(2500)
            deleteArmed = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.18f)),
    ) {
        DetailRow(
            icon = Icons.Rounded.Restaurant,
            tint = catColor,
            title = "Category",
            subtitle = txn.category.displayName,
            onClick = onCategory,
            trailing = {
                if (categoryRule != null) {
                    QuietTag(text = "Rules", color = c.gold)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(18.dp))
            },
        )

        if (categoryRule != null) {
            DetailRow(
                icon = Icons.Rounded.AutoAwesome,
                tint = c.gold,
                title = "Category rule",
                subtitle = if (txn.categoryExempt) "Keeping its own category" else "Inherits merchant rule",
                onClick = { onToggleCategoryExempt(!txn.categoryExempt) },
                trailing = {
                    Pill(
                        text = if (txn.categoryExempt) "Keeping own" else "Following rule",
                        dotColor = if (txn.categoryExempt) c.gold else c.income,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.SyncAlt, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(18.dp))
                },
            )
        }

        DetailRow(
            icon = Icons.Rounded.Store,
            tint = c.gold,
            title = "Merchant Alias",
            subtitle = if (txn.alias != null) "Renames all imports" else "Applies to all imports",
            onClick = onAlias,
            trailing = {
                Text(
                    text = txn.alias ?: "Not set",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                    color = if (txn.alias == null) c.textFaint else c.goldSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 104.dp),
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(18.dp))
            },
        )

        DetailRow(
            icon = Icons.Rounded.CreditCard,
            tint = c.gold,
            title = "Bank / Card",
            subtitle = bankLine(txn),
            subtitleSingleLine = false,
            onClick = {},
            trailing = {},
        )

        DetailRow(
            icon = Icons.Rounded.NotificationsNone,
            tint = c.textSecondary,
            title = "Reminder",
            subtitle = reminderSubtitle(reminder),
            onClick = onReminder,
            trailing = {
                if (reminder != null) {
                    Text(
                        text = "%02d:%02d".format(reminder.hour, reminder.minute),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                        color = c.goldSoft,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(18.dp))
            },
        )

        DetailRow(
            icon = Icons.Rounded.VisibilityOff,
            tint = if (excluded) c.gold else c.textSecondary,
            title = "Exclude from totals",
            subtitle = "Hide from totals & charts, keep record",
            subtitleSingleLine = false,
            onClick = {
                excluded = !excluded
                onToggleExcluded(excluded)
            },
            trailing = {
                WarmSwitch(
                    checked = excluded,
                    onCheckedChange = {
                        excluded = it
                        onToggleExcluded(it)
                    },
                )
            },
        )

        val rose = Color(0xFFE57373)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (deleteArmed) onDelete() else deleteArmed = true
                }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(rose.copy(alpha = 0.10f))
                    .border(1.dp, rose.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = rose, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (deleteArmed) "Tap again to confirm" else "Delete transaction",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                color = rose,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = rose.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String?,
    badge: String? = null,
    subtitleSingleLine: Boolean = true,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.10f))
                .border(1.dp, tint.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    QuietTag(text = badge, color = c.gold)
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = c.textMuted,
                    maxLines = if (subtitleSingleLine) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

/** Bare tinted label: no outline, no fill weight. */
@Composable
private fun QuietTag(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Medium),
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.09f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun Pill(text: String, dotColor: Color) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = c.goldSoft,
        )
    }
}

@Composable
private fun WarmSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
            .padding(2.dp),
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

/* -------------------------------- helpers -------------------------------- */

private fun heroTimestamp(txn: Transaction): String {
    val millis = txn.smsDate
    return if (millis != null) {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))
    } else {
        txn.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
}

private fun formatShort(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d-MMM-yy HH:mm"))

private fun bankLine(txn: Transaction): String {
    val name = txn.displayAccount ?: txn.bankName ?: "Unknown bank"
    val last4 = txn.accountLast4
    return if (last4 != null && !name.contains(last4)) "$name (··$last4)" else name
}

private fun reminderSubtitle(reminder: ReminderEntity?): String {
    if (reminder == null) return "No reminder set"
    val day = when {
        reminder.customDate != null -> java.time.LocalDate.ofEpochDay(reminder.customDate)
            .format(DateTimeFormatter.ofPattern("MMM d"))
        reminder.daysBefore == 0 -> "payment day"
        else -> "${reminder.daysBefore}d before"
    }
    return if (reminder.customDate != null) "Reminds on $day" else "Reminds $day"
}
