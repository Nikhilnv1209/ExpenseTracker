package com.expensetracker.app.ui.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.components.AddTransactionSheet
import com.expensetracker.app.ui.components.Dot
import com.expensetracker.app.ui.components.ExpandableFab
import com.expensetracker.app.ui.components.FabAction
import com.expensetracker.app.ui.components.GlassSurface
import com.expensetracker.app.ui.components.IconOrb
import com.expensetracker.app.ui.components.LiquidGlassLayout
import com.expensetracker.app.ui.components.SegmentedTextTabs
import com.expensetracker.app.ui.components.TransactionDetailSheet
import com.expensetracker.app.ui.components.TrendChart
import com.expensetracker.app.ui.components.WarmBackdrop
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType
import com.expensetracker.app.ui.theme.CategoryBronze
import com.expensetracker.app.ui.theme.CategoryClay
import com.expensetracker.app.ui.theme.CategoryGold
import com.expensetracker.app.ui.theme.CategoryMoss
import com.expensetracker.app.ui.theme.CategorySage
import com.expensetracker.app.ui.theme.CategorySand
import com.expensetracker.app.ui.theme.CategoryTerracotta
import com.expensetracker.app.ui.theme.CategoryWheat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class Currency(
    val symbol: String,
    val code: String,
    val name: String,
)

val currencies = listOf(
    Currency("$", "USD", "US Dollar"),
    Currency("\u20B9", "INR", "Indian Rupee"),
    Currency("\u00A3", "GBP", "British Pound"),
    Currency("\u20AC", "EUR", "Euro"),
    Currency("\u00A5", "JPY", "Japanese Yen"),
    Currency("A$", "AUD", "Australian Dollar"),
    Currency("C$", "CAD", "Canadian Dollar"),
)

fun formatAmount(amount: Double, currency: Currency): String {
    val sym = currency.symbol
    if (currency.code == "JPY") return "$sym${amount.toInt()}"
    return "$sym${String.format("%.2f", amount)}"
}

private val dayNumber = DateTimeFormatter.ofPattern("d")
private val weekdayInitial = DateTimeFormatter.ofPattern("EEEEE")
private val rowDate = DateTimeFormatter.ofPattern("d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeeAll: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onViewExcluded: () -> Unit = {},
    onManageAliases: () -> Unit = {},
    onIgnoredSenders: () -> Unit = {},
    onCategoryRules: () -> Unit = {},
    onOpenAgent: () -> Unit = {},
    onReminders: () -> Unit = {},
    selectedCurrency: Currency = currencies[1],
    onSelectCurrency: (Currency) -> Unit = {},
    pendingTransactionId: Long? = null,
    onTransactionHandled: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    remember {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            (context.applicationContext as com.expensetracker.app.ExpenseTrackerApplication)
                .registerSmsReceiverIfNeeded()
        }
        true
    }

    var showFilter by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
    var reminderVersion by remember { mutableStateOf(0) }
    var showAddTransaction by remember { mutableStateOf(false) }
    var addTransactionIsIncome by remember { mutableStateOf(false) }
    var addTransactionContent by remember { mutableStateOf(false) }

    LaunchedEffect(showAddTransaction) {
        if (!showAddTransaction && addTransactionContent) {
            kotlinx.coroutines.delay(350)
            addTransactionContent = false
        }
    }

    LaunchedEffect(pendingTransactionId) {
        val id = pendingTransactionId ?: return@LaunchedEffect
        viewModel.getTransaction(id)?.let { transaction ->
            selectedTransaction = transaction
            selectedTransactionId = transaction.id
        }
        onTransactionHandled()
    }

    LaunchedEffect(selectedTransactionId) {
        val id = selectedTransactionId
        if (id == null) {
            selectedTransaction = null
            return@LaunchedEffect
        }
        viewModel.observeTransaction(id).collect { latest ->
            selectedTransaction = latest
            if (latest == null) selectedTransactionId = null
        }
    }

    val isSheetOpen = selectedTransaction != null || showFilter || showAddTransaction
    BackHandler(enabled = isSheetOpen) {
        showFilter = false
        selectedTransaction = null
        selectedTransactionId = null
        showAddTransaction = false
    }

    LiquidGlassLayout(
        isSheetOpen = isSheetOpen,
        onDismiss = {
            showFilter = false
            selectedTransaction = null
            selectedTransactionId = null
            showAddTransaction = false
        },
        mainContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                WarmBackdrop()

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 128.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        item {
                            HomeTopBar(
                                hasActiveFilter = uiState.filter != TransactionFilter(),
                                onFilterClick = {
                                    showFilter = true
                                    viewModel.loadBankSuggestions()
                                },
                                onSettingsClick = onOpenSettings,
                                onAgentClick = onOpenAgent,
                            )
                        }
                        item { GreetingBlock(uiState = uiState, currency = selectedCurrency) }
                        item {
                            BalanceCard(
                                currency = selectedCurrency,
                                uiState = uiState,
                                onCycle = { viewModel.cycleBalanceMode() },
                            )
                        }
                        item {
                            AnalyticsCard(
                                uiState = uiState,
                                onMonthChanged = viewModel::setSelectedMonth,
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { RecentHeader(onSeeAll = onSeeAll) }

                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = AppTheme.colors.gold)
                                }
                            }
                        }

                        items(uiState.transactions.size) { index ->
                            val transaction = uiState.transactions[index]
                            TransactionCard(
                                transaction = transaction,
                                currency = selectedCurrency,
                                onClick = {
                                    selectedTransaction = transaction
                                    selectedTransactionId = transaction.id
                                },
                            )
                        }

                        if (uiState.importResult != null) {
                            item {
                                ImportResultCard(
                                    result = uiState.importResult!!,
                                    onDismiss = viewModel::clearImportResult,
                                )
                            }
                        }
                    }
                }

                if (!isSheetOpen) {
                    ExpandableFab(
                        actions = listOf(
                            FabAction(
                                label = "Income",
                                icon = Icons.Rounded.ArrowUpward,
                                color = AppTheme.colors.income,
                            ) {
                                addTransactionIsIncome = true
                                addTransactionContent = true
                                showAddTransaction = true
                            },
                            FabAction(
                                label = "Expense",
                                icon = Icons.Rounded.ArrowDownward,
                                color = AppTheme.colors.expense,
                            ) {
                                addTransactionIsIncome = false
                                addTransactionContent = true
                                showAddTransaction = true
                            },
                        ),
                    )
                }
            }
        },
    ) {
        if (showFilter) {
            FilterSheet(
                filter = uiState.filter,
                bankSuggestions = uiState.bankSuggestions,
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
            val reminder by androidx.compose.runtime.produceState(
                initialValue = null as com.expensetracker.app.data.local.ReminderEntity?,
                key1 = txn.id,
                key2 = reminderVersion,
            ) {
                value = viewModel.getReminder(txn.id)
            }
            TransactionDetailSheet(
                transaction = txn,
                currency = selectedCurrency,
                categoryRule = categoryRule,
                reminder = reminder,
                onToggleExcluded = { excluded ->
                    viewModel.toggleExcluded(txn.id, excluded)
                },
                onSetAlias = { alias ->
                    viewModel.setAlias(txn.id, txn.title, alias)
                },
                onSetCustomTitle = { title ->
                    viewModel.setCustomTitle(txn.id, title)
                },
                onSetNote = { note ->
                    viewModel.setNote(txn.id, note)
                },
                onSetCategory = { category, applyToAll ->
                    viewModel.setCategory(txn.id, txn.title, category, applyToAll)
                },
                onSetCategoryExempt = { exempt ->
                    viewModel.setCategoryExempt(txn.id, txn.title, exempt)
                },
                onSetReminder = { daysBefore, customDate, hour, minute ->
                    viewModel.setReminder(txn, daysBefore, customDate, hour, minute)
                    reminderVersion++
                },
                onRemoveReminder = {
                    viewModel.removeReminder(txn.id)
                    reminderVersion++
                },
                onDelete = {
                    viewModel.deleteTransaction(txn.id)
                    selectedTransaction = null
                    selectedTransactionId = null
                },
            )
        }

        if (addTransactionContent) {
            AddTransactionSheet(
                isIncome = addTransactionIsIncome,
                onSave = { title, amount, category, date, note ->
                    viewModel.addTransaction(title, amount, category, addTransactionIsIncome, date, note)
                    showAddTransaction = false
                },
                onDismiss = { showAddTransaction = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(
    hasActiveFilter: Boolean,
    onFilterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAgentClick: () -> Unit,
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color = c.gold, size = 6.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Home",
                style = MaterialTheme.typography.titleLarge,
                color = c.textPrimary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleAction(Icons.Rounded.AutoAwesome, onAgentClick, c.gold)
            CircleAction(
                Icons.Rounded.FilterList,
                onFilterClick,
                if (hasActiveFilter) c.gold else c.textMuted,
            )
            CircleAction(Icons.Rounded.Settings, onSettingsClick, c.textMuted)
        }
    }
}

@Composable
private fun CircleAction(icon: ImageVector, onClick: () -> Unit, tint: Color) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(c.cardRaised.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun GreetingBlock(uiState: HomeUiState, currency: Currency) {
    val c = AppTheme.colors
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val today = remember { LocalDate.now() }
    val todayLabel = remember { today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")) }

    val todayTotal = uiState.dailyExpenses
        .firstOrNull { it.epochDay == today.toEpochDay() }
        ?.total ?: 0.0
    val todayCount = uiState.transactions.count { it.date == today }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(color = c.secondary, size = 6.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = todayLabel.uppercase(),
                    style = AppType.eyebrow,
                    color = c.textFaint,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = c.textPrimary,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(color = c.gold, size = 5.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatAmount(todayTotal, currency),
                    style = AppType.amount(12, FontWeight.SemiBold),
                    color = c.gold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (todayCount) {
                    0 -> "No activity"
                    1 -> "1 transaction"
                    else -> "$todayCount transactions"
                },
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Balance card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(
    currency: Currency,
    uiState: HomeUiState,
    onCycle: () -> Unit,
) {
    val c = AppTheme.colors
    val mode = uiState.balanceMode

    val label = when (mode) {
        0 -> "Total balance"
        1 -> "Total income"
        else -> "Total expense"
    }
    val amount = when (mode) {
        0 -> uiState.totalBalance
        1 -> uiState.totalIncome
        else -> uiState.totalExpense
    }
    val accent = when (mode) {
        0 -> c.gold
        1 -> c.income
        else -> c.expense
    }
    val amountColor = when (mode) {
        0 -> c.textPrimary
        1 -> c.incomeBright
        else -> c.expense
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(c.card.copy(alpha = 0.55f))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        c.gold.copy(alpha = 0.05f),
                        androidx.compose.ui.graphics.Color.Transparent,
                        c.secondary.copy(alpha = 0.03f),
                    ),
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCycle,
            )
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(color = accent, size = 6.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label.uppercase(),
                        style = AppType.eyebrow.copy(letterSpacing = 0.8.sp),
                        color = c.textMuted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tap to cycle",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = c.textFaint,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = formatAmount(amount, currency),
                style = AppType.heroAmount,
                color = amountColor,
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    label = "Income",
                    amount = uiState.totalIncome,
                    color = c.income,
                    icon = Icons.Rounded.ArrowDownward,
                    currency = currency,
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    label = "Expense",
                    amount = uiState.totalExpense,
                    color = c.expense,
                    icon = Icons.Rounded.ArrowUpward,
                    currency = currency,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    currency: Currency,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(c.surface.copy(alpha = 0.8f))
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
                maxLines = 1,
            )
            Text(
                text = formatAmount(amount, currency),
                style = AppType.amount(12, FontWeight.SemiBold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analytics card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsCard(
    uiState: HomeUiState,
    onMonthChanged: (YearMonth) -> Unit,
) {
    val c = AppTheme.colors
    var page by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = c.gold,
                    modifier = Modifier.size(18.dp),
                )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MonthChevron(Icons.Rounded.KeyboardArrowLeft) {
                        onMonthChanged(uiState.selectedMonth.minusMonths(1))
                    }
                    MonthChevron(Icons.Rounded.KeyboardArrowRight) {
                        onMonthChanged(uiState.selectedMonth.plusMonths(1))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            SegmentedTextTabs(
                labels = listOf("Trend", "Category", "Banks"),
                selectedIndex = page,
                onSelect = { page = it },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(8.dp))

            when (page) {
                0 -> TrendPage(uiState)
                1 -> CategoryPage(uiState)
                else -> BankPage(uiState)
            }
    }
}

@Composable
private fun MonthChevron(icon: ImageVector, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(c.cardRaised.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TrendPage(uiState: HomeUiState) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val byEpoch = uiState.dailyExpenses.associateBy { it.epochDay }

    val values = days.map { day ->
        val entry = byEpoch[day.toEpochDay()]
        when (uiState.balanceMode) {
            1 -> entry?.income ?: 0.0
            2 -> entry?.total ?: 0.0
            else -> (entry?.total ?: 0.0) - (entry?.income ?: 0.0)
        }
    }
    val labels = days.map { "${it.format(weekdayInitial)}|${it.format(dayNumber)}" }

    TrendChart(values = values, labels = labels)
}

@Composable
private fun CategoryPage(uiState: HomeUiState) {
    val c = AppTheme.colors
    if (uiState.categoryTotals.isEmpty()) {
        EmptyPanel("No spending this month")
        return
    }
    val max = uiState.categoryTotals.maxOf { it.total }.coerceAtLeast(0.001)
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        uiState.categoryTotals.take(5).forEach { item ->
            val category = runCatching { Category.valueOf(item.category) }.getOrElse { Category.OTHER }
            val color = categoryColor(category)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(color = color, size = 6.dp, glow = false)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "₹${String.format("%.0f", item.total)}",
                        style = AppType.amount(13, FontWeight.Medium),
                        color = c.textPrimary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(c.textFaint.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((item.total / max).toFloat().coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun BankPage(uiState: HomeUiState) {
    val c = AppTheme.colors
    if (uiState.bankTotals.isEmpty()) {
        EmptyPanel("No spending this month")
        return
    }
    val max = uiState.bankTotals.maxOf { it.total }.coerceAtLeast(0.001)
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        uiState.bankTotals.take(5).forEachIndexed { index, item ->
            val color = categoryColor(Category.values()[index % Category.values().size])
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(color = color, size = 6.dp, glow = false)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "₹${String.format("%.0f", item.total)}",
                        style = AppType.amount(13, FontWeight.Medium),
                        color = c.textPrimary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(c.textFaint.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((item.total / max).toFloat().coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textFaint)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentHeader(onSeeAll: () -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Recent",
            style = MaterialTheme.typography.titleLarge,
            color = c.textPrimary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSeeAll,
                )
                .padding(vertical = 4.dp, horizontal = 2.dp),
        ) {
            Text(text = "See all", style = MaterialTheme.typography.labelMedium, color = c.gold)
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = c.gold,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    currency: Currency,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    val catColor = categoryColor(transaction.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = categoryIcon(transaction.category),
            contentDescription = null,
            tint = catColor,
            modifier = Modifier.size(22.dp),
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.displayTitle,
                style = MaterialTheme.typography.labelLarge,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = buildString {
                    append(transaction.date.format(rowDate))
                    append(" · ").append(transaction.category.displayName)
                    transaction.displayAccount?.let { append(" · ").append(it) }
                },
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = if (transaction.isIncome) "+${formatAmount(transaction.amount, currency)}"
            else "−${formatAmount(transaction.amount, currency)}",
            style = AppType.amount(15, FontWeight.SemiBold),
            color = if (transaction.isIncome) c.income else c.expense,
        )
    }
}

@Composable
private fun ImportResultCard(
    result: com.expensetracker.app.sms.SmsImportResult,
    onDismiss: () -> Unit,
) {
    val c = AppTheme.colors
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tint = c.gold,
        tintAlpha = 0.03f,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SMS import",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                )
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.gold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(4.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Scanned ${result.scanned} messages, found ${result.parsed} transactions.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            Text(
                text = "Imported ${result.imported} · Skipped ${result.skipped}",
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category helpers (shared with the detail / add sheets)
// ─────────────────────────────────────────────────────────────────────────────

internal fun categoryColor(category: Category): Color = when (category) {
    Category.SALARY -> CategorySage
    Category.FOOD -> CategoryGold
    Category.ENTERTAINMENT -> CategoryClay
    Category.TRANSPORT -> CategoryBronze
    Category.SHOPPING -> CategoryTerracotta
    Category.HEALTH -> CategoryMoss
    Category.BILLS -> CategoryWheat
    Category.GROCERY -> CategorySage
    Category.CASH -> CategorySand
    Category.TRANSFER -> CategoryBronze
    Category.OTHER -> CategorySand
}

internal fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.SALARY -> Icons.Rounded.AccountBalanceWallet
    Category.FOOD -> Icons.Rounded.LocalDining
    Category.ENTERTAINMENT -> Icons.Rounded.Theaters
    Category.TRANSPORT -> Icons.Rounded.DirectionsCar
    Category.SHOPPING -> Icons.Rounded.ShoppingBag
    Category.HEALTH -> Icons.Rounded.MedicalServices
    Category.BILLS -> Icons.Rounded.Receipt
    Category.GROCERY -> Icons.Rounded.Storefront
    Category.CASH -> Icons.Rounded.Payments
    Category.TRANSFER -> Icons.Rounded.SwapHoriz
    Category.OTHER -> Icons.Rounded.MoreHoriz
}
