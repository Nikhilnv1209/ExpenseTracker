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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    val c = AppTheme.colors
    var name by remember { mutableStateOf(currentAlias ?: "") }
    val hasAlias = currentAlias != null
    val suggestions = remember(merchant) { aliasSuggestions(merchant) }
    val empty = name.isBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(c.cardRaised)
                .border(
                    1.dp,
                    c.gold.copy(alpha = 0.18f),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .heightIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(c.textPrimary.copy(alpha = 0.2f)),
            )

            // Title + merchant
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Merchant Alias",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = c.gold,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = merchant,
                            style = MaterialTheme.typography.labelMedium,
                            color = c.gold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(c.surface)
                        .border(1.dp, c.hairline, CircleShape)
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

            // Impact card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.gold.copy(alpha = 0.04f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.gold.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Hub, contentDescription = null, tint = c.gold, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (transactionCount <= 1) {
                        "This transaction will be renamed"
                    } else {
                        "$transactionCount past & future transactions will update"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = c.gold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(c.gold.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }

            // Alias field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DISPLAY ALIAS / CLEAN NAME",
                        style = AppType.groupLabel.copy(letterSpacing = 0.8.sp),
                        color = c.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${name.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.gold,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface)
                        .border(1.dp, c.gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Badge, contentDescription = null, tint = c.gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) {
                            Text(
                                text = "Enter clean merchant alias",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textFaint,
                            )
                        }
                        BasicTextField(
                            value = name,
                            onValueChange = { if (it.length <= 40) name = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = c.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            cursorBrush = SolidColor(c.gold),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (name.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { name = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = c.textMuted,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            // Suggestions
            if (suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUICK SUGGESTIONS",
                        style = AppType.groupLabel.copy(letterSpacing = 0.8.sp),
                        color = c.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { suggestion ->
                            val active = name.equals(suggestion, ignoreCase = true)
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) c.gold else c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clip(CircleShape)
                                    .background(if (active) c.gold.copy(alpha = 0.08f) else c.surface)
                                    .border(
                                        1.dp,
                                        if (active) c.gold.copy(alpha = 0.4f) else c.hairline,
                                        CircleShape,
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { name = suggestion }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            // Original preserved
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surface)
                    .border(1.dp, c.hairline, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = c.income, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Original: $merchant",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = c.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Preserved",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = c.income,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.surface)
                        .border(1.dp, c.gold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }

            Spacer(Modifier.height(2.dp))

            // Primary action
            val removing = empty && hasAlias
            val label = when {
                removing -> "REMOVE ALIAS"
                transactionCount > 1 -> "APPLY ALIAS ($transactionCount TRANSACTIONS)"
                else -> "APPLY ALIAS"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (removing) c.textPrimary else Color(0xFF14120E),
                )
            }

            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge,
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
