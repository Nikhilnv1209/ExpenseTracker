package com.expensetracker.app.ui.feature.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.ui.components.rememberSheetProgress
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType

/**
 * "Category & Rules" sheet: pick a category, optionally turn it into a smart
 * merchant rule, and optionally set the merchant alias in one pass.
 */
@Composable
fun CategoryRulesSheet(
    merchant: String,
    currentCategory: Category,
    currentRule: Category?,
    currentAlias: String?,
    onDismiss: () -> Unit,
    onApply: (category: Category, createRule: Boolean, alias: String?) -> Unit,
    visible: Boolean = true,
) {
    val c = AppTheme.colors
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(currentCategory) }
    var ruleOn by remember { mutableStateOf(currentRule != null) }
    var alias by remember { mutableStateOf(currentAlias ?: "") }
    var editingAlias by remember { mutableStateOf(false) }
    val progress = rememberSheetProgress(visible)
    val gridState = rememberLazyGridState()

    LaunchedEffect(visible) {
        if (visible) {
            query = ""
            selected = currentCategory
            ruleOn = currentRule != null
            alias = currentAlias ?: ""
            editingAlias = false
            val index = Category.entries.indexOf(currentCategory)
            if (index > 4) gridState.scrollToItem(index)
        }
    }

    if (progress <= 0f) return

    val categories = Category.entries.filter {
        query.isBlank() || it.displayName.contains(query, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = visible,
                    onClick = onDismiss,
                ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - progress) * 480.dp.toPx() },
        ) {
            val sheetMax = maxHeight * 0.88f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMax)
                    .imePadding()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(c.cardRaised)
                    .padding(horizontal = 22.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(c.textPrimary.copy(alpha = 0.18f)),
                )

                Spacer(Modifier.height(4.dp))

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category & Rules",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp),
                            color = c.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Storefront,
                                contentDescription = null,
                                tint = c.gold.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = merchant,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
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
                            .background(Color.White.copy(alpha = 0.04f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = c.textMuted, modifier = Modifier.size(16.dp))
                    }
                }

                // Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surface)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = c.textFaint,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search categories",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                                color = c.textFaint,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, color = c.textPrimary),
                            cursorBrush = SolidColor(c.gold),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Category grid
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SUGGESTED",
                        style = AppType.groupLabel.copy(letterSpacing = 0.9.sp, fontSize = 10.5.sp),
                        color = c.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "tap to assign",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = c.gold.copy(alpha = 0.8f),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = true,
                ) {
                    items(categories) { category ->
                        val isSelected = category == selected
                        val catColor = categoryColor(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) catColor.copy(alpha = 0.10f)
                                    else Color.White.copy(alpha = 0.03f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { selected = category }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = categoryIcon(category),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
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

                // Smart merchant rule
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = c.gold,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "Smart Merchant Rule",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = c.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        WarmToggle(checked = ruleOn, onCheckedChange = { ruleOn = it })
                    }
                    Text(
                        text = "Always categorize \"${merchant.take(20)}\" as ${selected.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = c.textSecondary,
                    )
                    Text(
                        text = "Applies to future bank SMS & card alerts",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = c.textFaint,
                    )

                    // Alias row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.22f))
                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Alias",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = c.textFaint,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = alias.ifBlank { "Not set" },
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                            color = if (alias.isBlank()) c.textFaint else c.goldSoft,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(c.gold.copy(alpha = 0.10f))
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.surface)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (alias.isEmpty()) {
                                    Text(
                                        text = "Clean merchant name",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = c.textFaint,
                                    )
                                }
                                BasicTextField(
                                    value = alias,
                                    onValueChange = { alias = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, color = c.textPrimary),
                                    cursorBrush = SolidColor(c.gold),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (alias.isNotEmpty()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear alias",
                                    tint = c.textMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { alias = "" },
                                )
                            }
                        }
                    }
                }
                }

                // Primary action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
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
                        style = AppType.groupLabel.copy(
                            letterSpacing = 1.0.sp,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color(0xFF14120E),
                    )
                }

                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = c.textMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WarmToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(CircleShape)
            .background(if (checked) c.gold else c.surface)
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
