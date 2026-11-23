package com.expensetracker.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType

/**
 * "Category & Rules" sheet from the design: compact 2-column category grid,
 * smart merchant rule toggle with alias editor, and Apply & Update Rule CTA.
 * The sheet is capped so the action buttons are always visible.
 */
@Composable
fun CategoryRulesSheet(
    merchant: String,
    currentCategory: Category,
    currentRule: Category?,
    currentAlias: String?,
    onDismiss: () -> Unit,
    onApply: (category: Category, createRule: Boolean, alias: String?) -> Unit,
) {
    val c = AppTheme.colors
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(currentCategory) }
    var ruleOn by remember { mutableStateOf(currentRule != null) }
    var alias by remember { mutableStateOf(currentAlias ?: "") }
    var editingAlias by remember { mutableStateOf(false) }

    val categories = Category.entries.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            val maxSheetHeight = maxHeight * 0.78f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .imePadding()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(c.card)
                    .border(1.dp, c.hairline, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(c.textFaint.copy(alpha = 0.4f)),
                )

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category & Rules",
                            style = MaterialTheme.typography.headlineMedium,
                            color = c.textPrimary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = c.gold,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = merchant,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c.cardRaised)
                            .border(1.dp, c.hairline, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = c.textMuted, modifier = Modifier.size(17.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search categories...", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(17.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = c.textPrimary,
                        unfocusedTextColor = c.textPrimary,
                        focusedBorderColor = c.gold,
                        unfocusedBorderColor = c.hairline,
                        cursorColor = c.gold,
                        focusedContainerColor = c.surface,
                        unfocusedContainerColor = c.surface,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SUGGESTED CATEGORIES",
                        style = AppType.groupLabel,
                        color = c.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "TAP TO ASSIGN", style = AppType.groupLabel, color = c.gold)
                }

                Spacer(Modifier.height(8.dp))

                // Compact 2-column grid — shrinks when the rule/alias section grows.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { category ->
                        val isSelected = category == selected
                        val catColor = categoryColor(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) catColor.copy(alpha = 0.10f) else c.surface)
                                .border(
                                    1.dp,
                                    if (isSelected) catColor.copy(alpha = 0.45f) else c.hairline,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { selected = category }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
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
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) c.textPrimary else c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Smart Merchant Rule
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.surface)
                        .border(1.dp, c.hairline, RoundedCornerShape(18.dp))
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = c.gold,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Smart Merchant Rule",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = ruleOn, onCheckedChange = { ruleOn = it })
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Always categorize \"${merchant.take(18)}\" as ${selected.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(c.hairline.copy(alpha = 0.6f)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Applies automatically to future bank SMS & card alerts",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = c.textFaint,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.cardRaised.copy(alpha = 0.6f))
                            .border(1.dp, c.hairline, RoundedCornerShape(10.dp))
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Alias:", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = alias.ifBlank { "None" },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (alias.isBlank()) c.textFaint else c.goldSoft,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(c.gold.copy(alpha = 0.1f))
                                .border(1.dp, c.gold.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { editingAlias = !editingAlias },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit alias",
                                tint = c.gold,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    if (editingAlias) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Display name for this merchant", color = c.textFaint) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = c.textPrimary,
                                unfocusedTextColor = c.textPrimary,
                                focusedBorderColor = c.gold,
                                unfocusedBorderColor = c.hairline,
                                cursorColor = c.gold,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.gold)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onApply(selected, ruleOn, alias.trim().ifBlank { null }) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "APPLY & UPDATE RULE",
                        style = AppType.groupLabel.copy(letterSpacing = 1.0.sp),
                        color = Color(0xFF14120E),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, c.hairline, RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(CircleShape)
            .background(if (checked) c.gold else c.cardRaised)
            .border(1.dp, if (checked) c.gold else c.hairline, CircleShape)
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
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) c.canvas else c.textFaint),
        )
    }
}
