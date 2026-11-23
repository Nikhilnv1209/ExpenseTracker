package com.expensetracker.app.ui.feature.transactionlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.components.WarmBackdrop
import com.expensetracker.app.ui.feature.home.Currency
import com.expensetracker.app.ui.feature.home.FilterPeriod
import com.expensetracker.app.ui.feature.home.TransactionFilter
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import com.expensetracker.app.ui.feature.home.currencies
import com.expensetracker.app.ui.feature.home.formatAmount
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

private val dayMonth = DateTimeFormatter.ofPattern("d MMM")
private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy")

/** One quick filter pill on the Activity screen. */
private sealed interface ActivityPill {
    data object All : ActivityPill
    data object Expenses : ActivityPill
    data object Income : ActivityPill
    data class Bank(val name: String) : ActivityPill
    data object Uncategorized : ActivityPill
}

/**
 * Activity ledger, matching the design: search pill, quick filter chips,
 * sticky month summary and soft gradient transaction rows.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    onOpenTransaction: (Long) -> Unit = {},
    currency: Currency = currencies[1],
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val c = AppTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var pill by remember { mutableStateOf<ActivityPill>(ActivityPill.All) }

    LaunchedEffect(Unit) { viewModel.loadBankSuggestions() }

    LaunchedEffect(query, pill) {
        delay(160)
        viewModel.setFilter(
            TransactionFilter(
                search = query.trim(),
                type = when (pill) {
                    ActivityPill.Expenses -> "EXPENSE"
                    ActivityPill.Income -> "INCOME"
                    else -> null
                },
                bank = (pill as? ActivityPill.Bank)?.name,
                category = if (pill is ActivityPill.Uncategorized) Category.OTHER.displayName else null,
                period = FilterPeriod.ALL_TIME,
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WarmBackdrop()

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8FA882)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    color = c.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF241F1C), Color(0xFF342D26)))
                        )
                        .border(1.dp, c.gold.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = c.gold,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            val grouped = uiState.transactions.groupBy { it.date.withDayOfMonth(1) }
            val listState = rememberLazyListState()
            val pills = buildList {
                add(ActivityPill.All)
                add(ActivityPill.Expenses)
                add(ActivityPill.Income)
                uiState.bankSuggestions
                    .distinct()
                    .take(4)
                    .forEach { add(ActivityPill.Bank(it)) }
                add(ActivityPill.Uncategorized)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 110.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    // Title row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            color = c.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "LIVE LEDGER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.3.sp,
                            ),
                            color = c.goldSoft,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(c.gold.copy(alpha = 0.10f))
                                .border(1.dp, c.gold.copy(alpha = 0.20f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                item {
                    SearchPill(
                        query = query,
                        onQueryChange = { query = it },
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        items(pills) { item ->
                            FilterPill(
                                label = when (item) {
                                    ActivityPill.All -> "All"
                                    ActivityPill.Expenses -> "Expenses"
                                    ActivityPill.Income -> "Income"
                                    is ActivityPill.Bank -> item.name.substringBefore(' ')
                                    ActivityPill.Uncategorized -> "Uncategorized"
                                },
                                selected = pill == item,
                                onClick = { pill = if (pill == item) ActivityPill.All else item },
                            )
                        }
                    }
                }

                if (uiState.isLoading && uiState.transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Loading…", color = c.textMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (!uiState.isLoading && uiState.transactions.isEmpty()) {
                    item { EmptyState(onReset = { query = ""; pill = ActivityPill.All }) }
                }

                grouped.entries.sortedByDescending { it.key }.forEach { (month, transactions) ->
                    val monthNet = transactions.sumOf { if (it.isIncome) it.amount else -it.amount }
                    stickyHeader(key = month.toString()) {
                        MonthHeader(
                            month = month.format(monthTitle),
                            total = monthNet,
                            currency = currency,
                        )
                    }

                    items(transactions, key = { it.id }) { transaction ->
                        ActivityRow(
                            transaction = transaction,
                            currency = currency,
                            onClick = { onOpenTransaction(transaction.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(c.card.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = c.gold.copy(alpha = 0.7f),
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = "Search transactions, merchants, or SMS…",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = c.textMuted.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = c.textPrimary),
                cursorBrush = SolidColor(c.gold),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(c.cardRaised)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = c.textMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        color = if (selected) c.goldSoft else c.textMuted,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFF262D24) else c.card.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun MonthHeader(month: String, total: Double, currency: Currency) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.card.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = month,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = c.textPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(c.gold),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${if (total < 0) "−" else "+"}${formatAmount(kotlin.math.abs(total), currency)}",
            style = AppType.amount(14, FontWeight.SemiBold),
            color = c.goldSoft,
        )
    }
}

@Composable
private fun ActivityRow(
    transaction: Transaction,
    currency: Currency,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    // Income wins over category: a salary tagged "Other" is still green.
    val uncategorized = transaction.category == Category.OTHER && !transaction.isIncome
    val catColor = when {
        transaction.isIncome -> c.income
        uncategorized -> Color(0xFFE8A598)
        else -> categoryColor(transaction.category)
    }

    val meta = buildString {
        append(transaction.date.format(dayMonth))
        val bank = transaction.bankName?.substringBefore(' ')?.takeIf { it.isNotBlank() }
        val last4 = transaction.accountLast4
        if (!bank.isNullOrBlank()) append(" · ").append(bank)
        if (!last4.isNullOrBlank()) append(" ··").append(last4)
        append(" · ").append(transaction.category.displayName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF181512), Color(0xFF13100E)))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (transaction.isIncome) Color(0xFF262D24)
                    else if (uncategorized) Color(0xFF2A1D1A)
                    else c.gold.copy(alpha = 0.10f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (uncategorized) Icons.Rounded.HelpOutline else categoryIcon(transaction.category),
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.displayTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (uncategorized) Color(0xFFE8A598).copy(alpha = 0.8f) else c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = "${if (transaction.isIncome) "+" else "−"}${formatAmount(transaction.amount, currency)}",
            style = AppType.amount(14, FontWeight.SemiBold),
            color = when {
                transaction.isIncome -> c.income
                uncategorized -> Color(0xFFF4A89E)
                else -> c.expense
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyState(onReset: () -> Unit) {
    val c = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.card.copy(alpha = 0.7f))
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(c.cardRaised)
                .border(1.dp, c.gold.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = c.gold,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "No matching transactions",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = c.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Try another merchant name, bank, or clear your active filters.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = c.textMuted,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Reset filters",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = c.gold,
            modifier = Modifier
                .clip(CircleShape)
                .background(c.gold.copy(alpha = 0.15f))
                .border(1.dp, c.gold.copy(alpha = 0.30f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onReset,
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}
