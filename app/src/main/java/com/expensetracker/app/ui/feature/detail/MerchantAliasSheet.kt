package com.expensetracker.app.ui.feature.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.components.rememberSheetProgress
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType

/**
 * "Merchant Alias" sheet. Renaming the merchant updates all past & future
 * transactions. Per-transaction renames live in the detail hero instead.
 */
@Composable
fun MerchantAliasSheet(
    merchant: String,
    currentAlias: String?,
    transactionCount: Int,
    onDismiss: () -> Unit,
    onApply: (String?) -> Unit,
    visible: Boolean = true,
) {
    val c = AppTheme.colors
    var name by remember { mutableStateOf(currentAlias ?: "") }
    var focused by remember { mutableStateOf(false) }
    val hasAlias = currentAlias != null
    val suggestions = remember(merchant) { aliasSuggestions(merchant) }
    val empty = name.isBlank()
    val progress = rememberSheetProgress(visible)

    LaunchedEffect(visible) {
        if (visible) name = currentAlias ?: ""
    }

    if (progress <= 0f) return

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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .graphicsLayer { translationY = (1f - progress) * 420.dp.toPx() }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(c.cardRaised)
                .border(
                    1.dp,
                    c.hairline,
                    RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                )
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 16.dp)
                .heightIn(max = 660.dp),
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
                        text = "Merchant Alias",
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

            // What this touches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.gold.copy(alpha = 0.04f))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Hub,
                    contentDescription = null,
                    tint = c.gold,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (transactionCount <= 1) {
                        "Renames this transaction"
                    } else {
                        "Renames all $transactionCount transactions"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp),
                    color = c.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "past & future",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = c.gold.copy(alpha = 0.8f),
                )
            }

            // Field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ALIAS",
                        style = AppType.groupLabel.copy(letterSpacing = 0.9.sp, fontSize = 10.5.sp),
                        color = c.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${name.length}/40",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = if (focused) c.gold else c.textFaint,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surface)
                        .border(
                            1.dp,
                            if (focused) c.gold.copy(alpha = 0.55f) else c.hairline,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Badge,
                        contentDescription = null,
                        tint = c.gold.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) {
                            Text(
                                text = "e.g. Blue Tokai",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                                color = c.textFaint,
                            )
                        }
                        BasicTextField(
                            value = name,
                            onValueChange = { if (it.length <= 40) name = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                color = c.textPrimary,
                                fontWeight = FontWeight.Medium,
                            ),
                            cursorBrush = SolidColor(c.gold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focused = it.isFocused },
                        )
                    }
                    if (name.isNotEmpty()) {
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { name = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = c.textSecondary,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            // Suggestions
            if (suggestions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    suggestions.forEach { suggestion ->
                        val active = name.equals(suggestion, ignoreCase = true)
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                            color = if (active) c.gold else c.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(CircleShape)
                                .background(if (active) c.gold.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.03f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { name = suggestion }
                                .padding(horizontal = 13.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            // Original string, kept visible but quiet
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = c.income.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Original: $merchant",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = c.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(4.dp))

            // Primary action
            val removing = empty && hasAlias
            val label = when {
                removing -> "Remove alias"
                transactionCount > 1 -> "Apply to $transactionCount transactions"
                else -> "Apply alias"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (removing) {
                            Brush.horizontalGradient(listOf(c.expenseDeep, c.expense))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFE6C786), Color(0xFFC49F58)))
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onApply(if (empty) null else name.trim()) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = if (removing) c.textPrimary else Color(0xFF14120E),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (removing) c.textPrimary else Color(0xFF14120E),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasAlias) {
                    Text(
                        text = "Remove alias",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp),
                        color = c.expense.copy(alpha = 0.85f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onApply(null) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = c.textMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Plausible clean names derived from the raw bank string. */
private fun aliasSuggestions(merchant: String): List<String> {
    val words = merchant.split(' ', ',', '-', '/', '@')
        .map { it.trim() }
        .filter { it.length > 1 && it.none { ch -> ch.isDigit() } }
    if (words.isEmpty()) return emptyList()

    val out = LinkedHashSet<String>()
    if (words.size >= 2) out += "${words[0]} ${words[1]}"
    if (words.size >= 3) out += "${words[0]} ${words[1]} ${words[2]}"
    if (words.size >= 2) {
        val initials = words.dropLast(1).joinToString("") { it.first().uppercaseChar().toString() }
        out += "$initials ${words.last()}"
    }
    if (out.isEmpty()) out += merchant.take(24)
    return out.toList().take(3)
}
