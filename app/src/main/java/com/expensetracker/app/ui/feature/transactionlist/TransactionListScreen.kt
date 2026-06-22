package com.expensetracker.app.ui.feature.transactionlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.components.GlassCard
import com.expensetracker.app.ui.components.LiquidGlassLayout
import com.expensetracker.app.ui.components.TransactionDetailSheet
import com.expensetracker.app.ui.feature.home.FilterPeriod
import com.expensetracker.app.ui.feature.home.FilterSheet
import com.expensetracker.app.ui.feature.home.TransactionFilter
import com.expensetracker.app.ui.feature.home.categoryColor
import com.expensetracker.app.ui.feature.home.categoryIcon
import com.expensetracker.app.ui.feature.home.currencies
import com.expensetracker.app.ui.feature.home.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBack: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currency = currencies[1]
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showFilter by remember { mutableStateOf(false) }
    val defaultFilter = remember { TransactionFilter(period = FilterPeriod.ALL_TIME) }
    val isFilterActive = uiState.filter != defaultFilter

    val isSheetOpen = showFilter || selectedTransaction != null
    BackHandler(enabled = isSheetOpen) {
        showFilter = false
        selectedTransaction = null
    }

    LiquidGlassLayout(
        isSheetOpen = isSheetOpen,
        onDismiss = {
            showFilter = false
            selectedTransaction = null
        },
        mainContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                TopAppBar(
                    title = { Text("All Transactions") },
                    navigationIcon = {
                        Text(
                            text = "←",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onBack,
                                ),
                        )
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (isFilterActive) Color(0xFFFF9800).copy(alpha = 0.15f) else Color(0xFF7C3AED).copy(alpha = 0.1f),
                                    shape = CircleShape,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showFilter = true; viewModel.loadBankSuggestions() },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Filter",
                                modifier = Modifier.size(20.dp),
                                tint = if (isFilterActive) Color(0xFFFF9800) else Color(0xFF7C3AED).copy(alpha = 0.7f),
                            )
                        }
                        Spacer(Modifier.width(20.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color(0xFF7C3AED))
                    }
                } else {
                    val grouped = uiState.transactions.groupBy { it.date.withDayOfMonth(1) }

                    if (isFilterActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Showing ${uiState.transactions.size} filtered results",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            TextButton(
                                onClick = { viewModel.resetFilter() },
                            ) {
                                Text(
                                    text = "Clear filter",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF9800),
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        grouped.entries.sortedByDescending { it.key }.forEach { (month, transactions) ->
                            item {
                                val monthIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
                                val monthExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
                                Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                                    Text(
                                        text = month.format(monthFormatter),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Text(
                                            text = "Income: ${formatAmount(monthIncome, currency)}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF4CAF50),
                                        )
                                        Text(
                                            text = "Expense: ${formatAmount(monthExpense, currency)}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFE91E63),
                                        )
                                    }
                                }
                            }

                            var lastDate: LocalDate? = null
                            items(transactions) { transaction ->
                                if (lastDate != transaction.date) {
                                    Text(
                                        text = transaction.date.format(dayFormatter),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                                    )
                                }
                                lastDate = transaction.date
                                TransactionListItem(transaction, currency, onClick = { selectedTransaction = transaction })
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        },
    ) {
        if (showFilter) {
            FilterSheet(
                filter = uiState.filter,
                bankSuggestions = uiState.bankSuggestions,
                defaultPeriod = FilterPeriod.ALL_TIME,
                onApply = { filter ->
                    showFilter = false
                    viewModel.setFilter(filter)
                },
                onReset = {
                    showFilter = false
                    viewModel.resetFilter()
                },
            )
        }

        selectedTransaction?.let { txn ->
            val categoryRule by androidx.compose.runtime.produceState(
                initialValue = null as Category?,
                key1 = txn.title,
            ) {
                value = viewModel.getCategoryRule(txn.title)
            }
            TransactionDetailSheet(
                transaction = txn,
                currency = currency,
                categoryRule = categoryRule,
                onToggleExcluded = { excluded ->
                    viewModel.toggleExcluded(txn.id, excluded)
                    selectedTransaction = null
                },
                onSetAlias = { alias ->
                    viewModel.setAlias(txn.id, txn.title, alias)
                    selectedTransaction = null
                },
                onSetNote = { note ->
                    viewModel.setNote(txn.id, note)
                    selectedTransaction = null
                },
                onSetCategory = { category, applyToAll ->
                    viewModel.setCategory(txn.id, txn.title, category, applyToAll)
                    selectedTransaction = null
                },
                onSetCategoryExempt = { exempt ->
                    viewModel.setCategoryExempt(txn.id, txn.title, exempt)
                    selectedTransaction = null
                },
            )
        }
    }
}

@Composable
private fun TransactionListItem(transaction: Transaction, currency: com.expensetracker.app.ui.feature.home.Currency, onClick: () -> Unit = {}) {
    val catColor = categoryColor(transaction.category)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        tint = catColor,
        tintAlpha = 0.08f,
        borderAlpha = 0.1f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = catColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(transaction.category),
                    contentDescription = transaction.category.displayName,
                    tint = catColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.alias ?: transaction.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category.displayName,
                        fontSize = 12.sp,
                        color = catColor,
                    )
                    transaction.bankName?.let { bank ->
                        Text(
                            text = " · $bank",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (transaction.isIncome) "+${formatAmount(transaction.amount, currency)}"
                    else "-${formatAmount(transaction.amount, currency)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFE91E63),
                )
                if (transaction.noteIsManual && transaction.note != null) {
                    Text(
                        text = transaction.note!!,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = if (transaction.isIncome) "Income" else "Expense",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    )
                }
            }

            Spacer(Modifier.width(16.dp))
        }
    }
}
