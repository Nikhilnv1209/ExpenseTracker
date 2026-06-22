package com.expensetracker.app.ui.feature.home

import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.components.AddTransactionSheet
import com.expensetracker.app.ui.components.CalendarView
import com.expensetracker.app.ui.components.DailyExpense
import com.expensetracker.app.ui.components.ExpandableFab
import com.expensetracker.app.ui.components.FabAction
import com.expensetracker.app.ui.components.GlassCard
import com.expensetracker.app.ui.components.LiquidGlassLayout
import com.expensetracker.app.ui.components.TransactionDetailSheet
import com.expensetracker.app.ui.theme.Violet400
import com.expensetracker.app.ui.theme.Violet700
import java.time.LocalDate
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeeAll: () -> Unit = {},
    onViewExcluded: () -> Unit = {},
    onManageAliases: () -> Unit = {},
    onIgnoredSenders: () -> Unit = {},
    onCategoryRules: () -> Unit = {},
    onOpenAgent: () -> Unit = {},
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

    var selectedCurrency by remember { mutableStateOf(currencies[1]) }
    var showSettings by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAddTransaction by remember { mutableStateOf(false) }
    var addTransactionIsIncome by remember { mutableStateOf(false) }
    var addTransactionContent by remember { mutableStateOf(false) }

    LaunchedEffect(showAddTransaction) {
        if (!showAddTransaction && addTransactionContent) {
            kotlinx.coroutines.delay(350)
            addTransactionContent = false
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true &&
            permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            viewModel.importFromSms()
            (context.applicationContext as com.expensetracker.app.ExpenseTrackerApplication)
                .registerSmsReceiverIfNeeded()
        }
    }

    var exportMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.transactions) {
        val activity = context as? android.app.Activity
        val txnId = activity?.intent?.getLongExtra(
            com.expensetracker.app.notification.TransactionNotificationHelper.EXTRA_TRANSACTION_ID,
            -1L,
        ) ?: -1L
        if (txnId != -1L) {
            uiState.transactions.find { it.id == txnId }?.let { txn ->
                selectedTransaction = txn
                activity?.intent?.removeExtra(
                    com.expensetracker.app.notification.TransactionNotificationHelper.EXTRA_TRANSACTION_ID,
                )
            }
        }
    }

    val isSheetOpen = showSettings || selectedTransaction != null || showFilter || showAddTransaction
    BackHandler(enabled = isSheetOpen) {
        showSettings = false
        showFilter = false
        selectedTransaction = null
        showAddTransaction = false
    }

    LiquidGlassLayout(
        isSheetOpen = isSheetOpen,
        onDismiss = {
            showSettings = false
            showFilter = false
            selectedTransaction = null
            showAddTransaction = false
        },
        mainContent = {
            Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { GreetingHeader(
                    onSettingsClick = { showSettings = true },
                    onFilterClick = { showFilter = true; viewModel.loadBankSuggestions() },
                    hasActiveFilter = uiState.filter != TransactionFilter(),
                ) }
                item { BalanceCard(selectedCurrency, uiState) { viewModel.cycleBalanceMode() } }
                item { GraphPager(uiState) }
                item { RecentTransactionsHeader(onSeeAll = onSeeAll) }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7C3AED))
                        }
                    }
                }

                items(uiState.transactions) { transaction ->
                    TransactionItem(transaction, selectedCurrency, onClick = { selectedTransaction = transaction })
                }

                if (uiState.importResult != null) {
                    item {
                        ImportResultCard(
                            result = uiState.importResult!!,
                            onDismiss = viewModel::clearImportResult,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            }

            if (!isSheetOpen) {
                ExpandableFab(
                    actions = listOf(
                        FabAction(
                            label = "Income",
                            icon = Icons.Rounded.ArrowUpward,
                            color = Color(0xFF4CAF50),
                        ) {
                            addTransactionIsIncome = true
                            addTransactionContent = true
                            showAddTransaction = true
                        },
                        FabAction(
                            label = "Expense",
                            icon = Icons.Rounded.ArrowDownward,
                            color = Color(0xFFFF5722),
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

        if (showSettings) {
            SettingsSheet(
                current = selectedCurrency,
                onSelectCurrency = { selectedCurrency = it; showSettings = false },
                onViewExcluded = {
                    showSettings = false
                    onViewExcluded()
                },
                onManageAliases = {
                    showSettings = false
                    onManageAliases()
                },
                onIgnoredSenders = {
                    showSettings = false
                    onIgnoredSenders()
                },
                onCategoryRules = {
                    showSettings = false
                    onCategoryRules()
                },
                onOpenAgent = {
                    showSettings = false
                    onOpenAgent()
                },
                onImportSms = {
                    showSettings = false
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) -> {
                            viewModel.importFromSms()
                        }
                        else -> {
                            smsPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                            )
                        }
                    }
                },
                onExportSms = {
                    showSettings = false
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) -> {
                            viewModel.exportBankSmsToFile { exportMessage = it }
                        }
                        else -> {
                            smsPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                            )
                        }
                    }
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
                onSetReminder = { daysBefore, customDate, hour, minute ->
                    viewModel.setReminder(txn, daysBefore, customDate, hour, minute)
                    selectedTransaction = null
                },
                onRemoveReminder = {
                    viewModel.removeReminder(txn.id)
                    selectedTransaction = null
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

    exportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            confirmButton = {
                TextButton(onClick = { exportMessage = null }) {
                    Text("OK")
                }
            },
            title = { Text("Export SMS") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun GreetingHeader(
    onSettingsClick: () -> Unit,
    onFilterClick: () -> Unit,
    hasActiveFilter: Boolean = false,
) {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val (greeting, icon) = when (hour) {
        in 5..11 -> "Good Morning" to Icons.Rounded.WbSunny
        in 12..16 -> "Good Afternoon" to Icons.Rounded.LightMode
        in 17..20 -> "Good Evening" to Icons.Rounded.Brightness3
        else -> "Good Night" to Icons.Rounded.Nightlight
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = greeting,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Welcome Back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (hasActiveFilter) Color(0xFFFF9800).copy(alpha = 0.15f) else Color(0xFF7C3AED).copy(alpha = 0.1f),
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFilterClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "Filter",
                    modifier = Modifier.size(20.dp),
                    tint = if (hasActiveFilter) Color(0xFFFF9800) else Color(0xFF7C3AED).copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFF7C3AED).copy(alpha = 0.1f),
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSettingsClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF7C3AED),
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(currency: Currency, uiState: HomeUiState, onCycleMode: () -> Unit) {
    val mode = uiState.balanceMode

    val gradientColors = when (mode) {
        0 -> listOf(Color(0xFF1E1B2E), Color(0xFF2D2640))
        1 -> listOf(Color(0xFF1A2332), Color(0xFF1B3A2F))
        else -> listOf(Color(0xFF2A1F1A), Color(0xFF3D2A1F))
    }

    val animColor1 by animateColorAsState(gradientColors[0], tween(400))
    val animColor2 by animateColorAsState(gradientColors[1], tween(400))

    val label = when (mode) {
        0 -> "Total Balance"
        1 -> "Total Income"
        else -> "Total Expense"
    }
    val amount = when (mode) {
        0 -> uiState.totalBalance
        1 -> uiState.totalIncome
        else -> uiState.totalExpense
    }
    val amountColor = when (mode) {
        0 -> Color.White
        1 -> Color(0xFF81C784)
        else -> Color(0xFFFFAB91)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(animColor1, animColor2),
                    start = Offset.Zero,
                    end = Offset(800f, 1200f),
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCycleMode() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatAmount(amount, currency),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    BalanceSubItem("Income", uiState.totalIncome, Color(0xFF81C784), currency)
                    BalanceSubItem("Expense", uiState.totalExpense, Color(0xFFFFAB91), currency)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == mode) 7.dp else 5.dp)
                                .background(
                                    color = if (i == mode) Color.White.copy(alpha = 0.9f)
                                    else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape,
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceSubItem(label: String, amount: Double, color: Color, currency: Currency) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.45f),
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = formatAmount(amount, currency),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun RecentTransactionsHeader(onSeeAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = "Recent Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
        Text(
            text = "See All",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF7C3AED),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSeeAll,
            ),
        )
    }
}

@Composable
private fun ImportResultCard(
    result: com.expensetracker.app.sms.SmsImportResult,
    onDismiss: () -> Unit,
) {
    GlassCard(
        tint = Color(0xFF7C3AED),
        tintAlpha = 0.1f,
        borderAlpha = 0.15f,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SMS Import",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = Color(0xFF7C3AED))
                }
            }
            Text(
                text = "Scanned ${result.scanned} messages, found ${result.parsed} transactions.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                text = "Imported: ${result.imported} · Skipped: ${result.skipped}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF7C3AED),
            )
        }
    }
}

@Composable
private fun GraphPager(uiState: HomeUiState) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(310.dp),
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> CalendarView(
                            dailyExpenses = uiState.dailyExpenses,
                            graphMode = uiState.balanceMode,
                        )
                        1 -> CategoryBreakdown(
                            categoryTotals = uiState.categoryTotals,
                            totalExpense = uiState.totalExpense,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Trend",
                    fontSize = 12.sp,
                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (pagerState.currentPage == 0) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                            .background(
                                color = if (pagerState.currentPage == index) Color(0xFF7C3AED) else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape,
                            ),
                )
                if (index < 1) Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Category",
                fontSize = 12.sp,
                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal,
                color = if (pagerState.currentPage == 1) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        }
    }
}

@Composable
private fun CategoryBreakdown(
    categoryTotals: List<com.expensetracker.app.data.local.TransactionDao.CategoryTotal>,
    totalExpense: Double,
) {
    if (categoryTotals.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No expenses this month",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    } else {
        var selectedIndex by remember { mutableStateOf(-1) }

        val total = categoryTotals.sumOf { it.total }.coerceAtLeast(0.001)
        val arcs = categoryTotals.mapIndexed { i, item ->
            val sweep = (item.total / total).toFloat() * 360f
            val startAngle = if (i == 0) -90f else categoryTotals.take(i).sumOf { it.total } / total * 360f - 90f
            Triple(i, startAngle.toFloat(), sweep.toFloat())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Spending by Category",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(170.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val dx = tapOffset.x - cx
                                    val dy = tapOffset.y - cy
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                    val strokePx = 24.dp.toPx()
                                    val diameter = minOf(size.width, size.height) - strokePx
                                    val innerR = diameter / 2f
                                    val outerR = innerR + strokePx

                                    if (dist in innerR..outerR || dist <= outerR) {
                                        var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < -90f) angle += 360f
                                        angle += 90f
                                        if (angle >= 360f) angle -= 360f
                                        if (angle < 0f) angle += 360f

                                        val matched = arcs.indexOfFirst { (_, start, sweep) ->
                                            val end = start + sweep
                                            angle >= start && angle < end
                                        }
                                        selectedIndex = if (matched == selectedIndex) -1 else matched
                                    }
                                }
                            },
                    ) {
                        val strokePx = 24.dp.toPx()
                        val diameter = minOf(size.width, size.height) - strokePx
                        val topLeft = androidx.compose.ui.geometry.Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f,
                        )
                        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                        categoryTotals.forEachIndexed { i, item ->
                            val cat = Category.fromDisplayName(item.category)
                            val color = categoryColor(cat)
                            val (_, startAngle, sweep) = arcs[i]
                            val isSelected = i == selectedIndex
                            val isDimmed = selectedIndex != -1 && !isSelected

                            drawArc(
                                color = if (isDimmed) color.copy(alpha = 0.18f) else color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (isSelected) strokePx + 6.dp.toPx() else strokePx,
                                    cap = StrokeCap.Round,
                                ),
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selectedIndex >= 0 && selectedIndex < categoryTotals.size) {
                            val item = categoryTotals[selectedIndex]
                            val cat = Category.fromDisplayName(item.category)
                            val catColor = categoryColor(cat)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(catColor.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = categoryIcon(cat),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "₹${String.format("%.0f", item.total)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val pct = if (totalExpense > 0) ((item.total / totalExpense) * 100).toInt() else 0
                            Text(
                                text = "$pct%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        } else {
                            Text(
                                text = "₹${String.format("%.0f", totalExpense)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Total",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    categoryTotals.forEachIndexed { i, item ->
                        val cat = Category.fromDisplayName(item.category)
                        val catColor = categoryColor(cat)
                        val isSelected = i == selectedIndex
                        val percent = if (totalExpense > 0) ((item.total / totalExpense) * 100).toInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedIndex = if (isSelected) -1 else i }
                                .padding(vertical = 3.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (isSelected) catColor else catColor.copy(alpha = 0.5f),
                                        CircleShape,
                                    ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = cat.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "$percent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            if (selectedIndex >= 0 && selectedIndex < categoryTotals.size) {
                Spacer(Modifier.height(10.dp))
                val item = categoryTotals[selectedIndex]
                val cat = Category.fromDisplayName(item.category)
                Text(
                    text = "${cat.displayName} · ₹${String.format("%.0f", item.total)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = categoryColor(cat),
                )
            }
        }
    }
}

internal fun categoryColor(category: Category): Color = when (category) {
    Category.SALARY -> Color(0xFF4CAF50)
    Category.FOOD -> Color(0xFFFF9800)
    Category.ENTERTAINMENT -> Color(0xFFE91E63)
    Category.TRANSPORT -> Color(0xFF2196F3)
    Category.SHOPPING -> Color(0xFF9C27B0)
    Category.HEALTH -> Color(0xFF00BCD4)
    Category.BILLS -> Color(0xFFFF5722)
    Category.GROCERY -> Color(0xFF8BC34A)
    Category.CASH -> Color(0xFF795548)
    Category.TRANSFER -> Color(0xFF607D8B)
    Category.OTHER -> Color(0xFF9E9E9E)
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

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
private fun TransactionItem(transaction: Transaction, currency: Currency, onClick: () -> Unit = {}) {
    val catColor = categoryColor(transaction.category)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
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
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category.displayName,
                        fontSize = 12.sp,
                        color = catColor,
                    )
                    Text(
                        text = " · ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                    Text(
                        text = transaction.date.format(dateFormatter),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (transaction.isIncome) "+${formatAmount(transaction.amount, currency)}"
                    else "-${formatAmount(transaction.amount, currency)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isIncome) Color(0xFF4CAF50)
                    else Color(0xFFE91E63),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    current: Currency,
    onSelectCurrency: (Currency) -> Unit,
    onViewExcluded: () -> Unit,
    onManageAliases: () -> Unit,
    onIgnoredSenders: () -> Unit,
    onCategoryRules: () -> Unit,
    onOpenAgent: () -> Unit,
    onImportSms: () -> Unit,
    onExportSms: () -> Unit,
) {
    var showCurrencyPicker by remember { mutableStateOf(false) }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            current = current,
            onSelect = {
                onSelectCurrency(it)
                showCurrencyPicker = false
            },
        )
    } else {
        SettingsContent(
            current = current,
            onShowCurrencyPicker = { showCurrencyPicker = true },
            onViewExcluded = onViewExcluded,
            onManageAliases = onManageAliases,
            onIgnoredSenders = onIgnoredSenders,
            onCategoryRules = onCategoryRules,
            onOpenAgent = onOpenAgent,
            onImportSms = onImportSms,
            onExportSms = onExportSms,
        )
    }
}

@Composable
private fun SettingsContent(
    current: Currency,
    onShowCurrencyPicker: () -> Unit,
    onViewExcluded: () -> Unit,
    onManageAliases: () -> Unit,
    onIgnoredSenders: () -> Unit,
    onCategoryRules: () -> Unit,
    onOpenAgent: () -> Unit,
    onImportSms: () -> Unit,
    onExportSms: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsRow(
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "Currency",
            subtitle = "${current.name} (${current.code})",
            onClick = onShowCurrencyPicker,
        )

        SettingsRow(
            icon = Icons.Rounded.Undo,
            title = "Excluded Transactions",
            subtitle = "View and restore excluded items",
            onClick = onViewExcluded,
        )

        SettingsRow(
            icon = Icons.Rounded.Edit,
            title = "Manage Aliases",
            subtitle = "Rename and organize transaction titles",
            onClick = onManageAliases,
        )

        SettingsRow(
            icon = Icons.Rounded.Category,
            title = "Category Rules",
            subtitle = "Auto-categorize transactions by sender",
            onClick = onCategoryRules,
        )

        SettingsRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "Finance Agent",
            subtitle = "Ask AI about your spending and organize transactions",
            onClick = onOpenAgent,
        )

        SettingsRow(
            icon = Icons.Rounded.Block,
            title = "Ignored Senders",
            subtitle = "Skip transactions from specific senders",
            onClick = onIgnoredSenders,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onImportSms,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Import from SMS")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onExportSms,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Export Raw Bank SMS")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun CurrencyPickerSheet(current: Currency, onSelect: (Currency) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = "Select Currency",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))
        currencies.forEach { currency ->
            val isSelected = currency.code == current.code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(currency) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currency.symbol,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(48.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currency.code,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = currency.name,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                if (isSelected) {
                    Text(
                        text = "\u2713",
                        fontSize = 18.sp,
                        color = Color(0xFF7C3AED),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private val LocalDate.formatted: String
    get() = format(dateFormatter)
