package com.expensetracker.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import java.time.format.DateTimeFormatter

private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    currency: com.expensetracker.app.ui.feature.home.Currency,
    categoryRule: Category? = null,
    onToggleExcluded: (Boolean) -> Unit = {},
    onSetAlias: (String?) -> Unit = {},
    onSetNote: (String?) -> Unit = {},
    onSetCategory: (Category, Boolean) -> Unit = { _, _ -> },
    onSetCategoryExempt: (Boolean) -> Unit = {},
) {
    val catColor = categoryColor(transaction.category)
    val displayTitle = transaction.alias ?: transaction.title
    var isEditingAlias by remember { mutableStateOf(false) }
    var aliasText by remember { mutableStateOf(transaction.alias ?: "") }
    var showCategoryPicker by remember { mutableStateOf(false) }

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
            targetState = showCategoryPicker,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {
                if (targetState) {
                    slideInVertically(animationSpec = tween(320)) { it } togetherWith
                        slideOutVertically(animationSpec = tween(320)) { -it }
                } else {
                    slideInVertically(animationSpec = tween(320)) { -it } togetherWith
                        slideOutVertically(animationSpec = tween(320)) { it }
                }
            },
            contentAlignment = Alignment.TopStart,
            label = "categoryPicker",
        ) { isPicker ->
            if (isPicker) {
                CategoryPanel(
                    title = transaction.title,
                    currentCategory = transaction.category,
                    onBack = { showCategoryPicker = false },
                    onConfirm = { category, applyToAll ->
                        onSetCategory(category, applyToAll)
                        showCategoryPicker = false
                    },
                )
            } else {
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
                    .clickable { showCategoryPicker = true }
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
