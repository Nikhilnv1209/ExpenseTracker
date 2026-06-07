package com.expensetracker.app.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.expensetracker.app.ui.components.CalendarView
import com.expensetracker.app.ui.components.DailyExpense

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

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean,
    val date: String,
)

private val sampleTransactions = listOf(
    Transaction("1", "Freelance Project", 2400.00, "Work", true, "Jun 8"),
    Transaction("2", "Groceries", 85.50, "Food", false, "Jun 7"),
    Transaction("3", "Netflix Subscription", 15.99, "Entertainment", false, "Jun 6"),
    Transaction("4", "Gas Station", 45.00, "Transport", false, "Jun 5"),
    Transaction("5", "Salary Deposit", 5000.00, "Salary", true, "Jun 3"),
)

private val sampleDailyExpenses = listOf(
    DailyExpense(1, 95.00),
    DailyExpense(2, 35.00),
    DailyExpense(3, 0.00),
    DailyExpense(4, 210.00),
    DailyExpense(5, 120.50),
    DailyExpense(6, 45.00),
    DailyExpense(7, 15.99),
    DailyExpense(8, 0.00),
    DailyExpense(9, 67.00),
    DailyExpense(10, 180.00),
    DailyExpense(11, 55.00),
    DailyExpense(12, 89.99),
    DailyExpense(13, 30.00),
    DailyExpense(14, 0.00),
    DailyExpense(15, 200.00),
    DailyExpense(16, 75.50),
    DailyExpense(17, 42.00),
    DailyExpense(18, 155.00),
    DailyExpense(19, 0.00),
    DailyExpense(20, 90.00),
    DailyExpense(21, 145.00),
    DailyExpense(22, 12.50),
    DailyExpense(23, 65.00),
    DailyExpense(24, 110.00),
    DailyExpense(25, 75.00),
    DailyExpense(26, 0.00),
    DailyExpense(27, 195.00),
    DailyExpense(28, 150.00),
    DailyExpense(29, 85.00),
    DailyExpense(30, 40.00),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CurrencyPickerSheet(
                current = selectedCurrency,
                onSelect = { selectedCurrency = it; showSettings = false },
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { GreetingHeader(onSettingsClick = { showSettings = true }) }
        item { BalanceCard(selectedCurrency) }
        item { CalendarView(dailyExpenses = sampleDailyExpenses) }
        item { RecentTransactionsHeader() }
        items(sampleTransactions) { transaction ->
            TransactionItem(transaction, selectedCurrency)
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun GreetingHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = "Good Morning \uD83C\uDF24\uFE0F",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Welcome Back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "\u22EE",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .clickable(onClick = onSettingsClick)
                .padding(4.dp),
        )
    }
}

@Composable
private fun BalanceCard(currency: Currency) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF7C3AED),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Total Balance",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAmount(12450.00, currency),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun RecentTransactionsHeader() {
    Text(
        text = "Recent Transactions",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun TransactionItem(transaction: Transaction, currency: Currency) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (transaction.isIncome)
                            Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (transaction.isIncome) "↓" else "↑",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${transaction.category} · ${transaction.date}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            Text(
                text = if (transaction.isIncome) "+${formatAmount(transaction.amount, currency)}"
                else "-${formatAmount(transaction.amount, currency)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.isIncome) Color(0xFF4CAF50)
                else Color(0xFFE91E63),
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
                    .clickable { onSelect(currency) }
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
