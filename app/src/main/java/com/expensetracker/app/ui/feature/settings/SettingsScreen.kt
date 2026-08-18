package com.expensetracker.app.ui.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.ExpenseTrackerApplication
import com.expensetracker.app.ui.feature.home.Currency
import com.expensetracker.app.ui.feature.home.HomeViewModel
import com.expensetracker.app.ui.feature.home.currencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    selectedCurrency: Currency,
    onSelectCurrency: (Currency) -> Unit,
    onViewExcluded: () -> Unit,
    onManageAliases: () -> Unit,
    onIgnoredSenders: () -> Unit,
    onCategoryRules: () -> Unit,
    onReminders: () -> Unit,
    onOpenAgent: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showCurrencyPicker by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var smsActionIsExport by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true &&
            permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            (context.applicationContext as ExpenseTrackerApplication)
                .registerSmsReceiverIfNeeded()
            if (smsActionIsExport) {
                viewModel.exportBankSmsToFile { exportMessage = it }
            } else {
                viewModel.importFromSms()
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingRestoreUri = uri
    }

    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearBackupMessage()
        }
    }
    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { result ->
            Toast.makeText(
                context,
                "Scanned ${result.scanned} · imported ${result.imported} · skipped ${result.skipped}",
                Toast.LENGTH_SHORT,
            ).show()
            viewModel.clearImportResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TopAppBar(
            title = { Text(if (showCurrencyPicker) "Currency" else "Settings") },
            navigationIcon = {
                IconButton(onClick = {
                    if (showCurrencyPicker) showCurrencyPicker = false else onBack()
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        if (showCurrencyPicker) {
            CurrencyPickerContent(
                current = selectedCurrency,
                onSelect = {
                    onSelectCurrency(it)
                    showCurrencyPicker = false
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Currency",
                    subtitle = "${selectedCurrency.name} (${selectedCurrency.code})",
                    onClick = { showCurrencyPicker = true },
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
                    icon = Icons.Rounded.Notifications,
                    title = "Payment Reminders",
                    subtitle = "Manage upcoming payment alerts",
                    onClick = onReminders,
                )

                SettingsRow(
                    icon = Icons.Rounded.Block,
                    title = "Ignored Senders",
                    subtitle = "Skip transactions from specific senders",
                    onClick = onIgnoredSenders,
                )

                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "AI Agent",
                    subtitle = "Open the finance assistant",
                    onClick = onOpenAgent,
                )

                Button(
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) -> {
                                viewModel.importFromSms()
                                (context.applicationContext as ExpenseTrackerApplication)
                                    .registerSmsReceiverIfNeeded()
                            }
                            else -> {
                                smsActionIsExport = false
                                smsPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Import from SMS")
                }

                Button(
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) -> {
                                viewModel.exportBankSmsToFile { exportMessage = it }
                            }
                            else -> {
                                smsActionIsExport = true
                                smsPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Export Raw Bank SMS")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Backup & Restore",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Save or restore all your data as a file",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                Button(
                    onClick = {
                        exportBackupLauncher.launch("expense-tracker-backup-${java.time.LocalDate.now()}.zip")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Export Backup")
                }

                Button(
                    onClick = {
                        openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Restore Backup")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
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

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreUri = null
                        viewModel.importBackup(uri)
                    },
                ) {
                    Text("Restore", color = Color(0xFFE91E63))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Restore backup?") },
            text = {
                Text(
                    "Your current local data will be replaced with the contents of this backup. " +
                        "The app will restart to apply it.",
                )
            },
        )
    }

    if (uiState.restoreReady) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = { relaunchApp(context) }) {
                    Text("Restart")
                }
            },
            title = { Text("Restore complete") },
            text = { Text("Your backup was applied. Restart the app to load the restored data.") },
        )
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
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
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
private fun CurrencyPickerContent(current: Currency, onSelect: (Currency) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        currencies.forEach { currency ->
            val isSelected = currency.code == current.code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(currency) },
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

private fun relaunchApp(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}