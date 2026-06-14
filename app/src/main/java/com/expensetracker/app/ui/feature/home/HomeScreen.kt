package com.expensetracker.app.ui.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.components.CalendarView
import com.expensetracker.app.ui.components.DailyExpense
import com.expensetracker.app.ui.components.GlassCard
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
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedCurrency by remember { mutableStateOf(currencies[1]) }
    var showSettings by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.importFromSms()
        }
    }

    var exportMessage by remember { mutableStateOf<String?>(null) }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SettingsSheet(
                current = selectedCurrency,
                onSelectCurrency = { selectedCurrency = it; showSettings = false },
                onImportSms = {
                    showSettings = false
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) -> {
                            viewModel.importFromSms()
                        }
                        else -> {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
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
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    }
                },
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { GreetingHeader(onSettingsClick = { showSettings = true }) }
        item { BalanceCard(selectedCurrency, uiState) }
        item { CalendarView(dailyExpenses = uiState.dailyExpenses) }
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
            TransactionItem(transaction, selectedCurrency)
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
private fun BalanceCard(currency: Currency, uiState: HomeUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Violet400, Violet700),
                    start = Offset.Zero,
                    end = Offset(1000f, 1000f),
                )
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                )
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
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
                text = formatAmount(uiState.totalBalance, currency),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BalanceSubItem("Income", uiState.totalIncome, Color(0xFF4CAF50), currency)
                BalanceSubItem("Expense", uiState.totalExpense, Color(0xFFE91E63), currency)
            }
        }
    }
}

@Composable
private fun BalanceSubItem(label: String, amount: Double, color: Color, currency: Currency) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatAmount(amount, currency),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun RecentTransactionsHeader(onSeeAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Recent Transactions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "See All",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF7C3AED),
            modifier = Modifier.clickable(onClick = onSeeAll),
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

internal fun categoryColor(category: Category): Color = when (category) {
    Category.SALARY -> Color(0xFF4CAF50)
    Category.FOOD -> Color(0xFFFF9800)
    Category.ENTERTAINMENT -> Color(0xFFE91E63)
    Category.TRANSPORT -> Color(0xFF2196F3)
    Category.SHOPPING -> Color(0xFF9C27B0)
    Category.HEALTH -> Color(0xFF00BCD4)
    Category.BILLS -> Color(0xFFFF5722)
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
    Category.CASH -> Icons.Rounded.Payments
    Category.TRANSFER -> Icons.Rounded.SwapHoriz
    Category.OTHER -> Icons.Rounded.MoreHoriz
}

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
private fun TransactionItem(transaction: Transaction, currency: Currency) {
    val catColor = categoryColor(transaction.category)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
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
                    text = transaction.title,
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
                Text(
                    text = if (transaction.isIncome) "Income" else "Expense",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
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
    onImportSms: () -> Unit,
    onExportSms: () -> Unit,
) {
    var showCurrencyPicker by remember { mutableStateOf(false) }

    if (showCurrencyPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCurrencyPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CurrencyPickerSheet(
                current = current,
                onSelect = {
                    onSelectCurrency(it)
                    showCurrencyPicker = false
                },
            )
        }
    }

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
            onClick = { showCurrencyPicker = true },
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
            .clickable(onClick = onClick)
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
        Text(
            text = ">",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
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

private val LocalDate.formatted: String
    get() = format(dateFormatter)
