package com.expensetracker.app.ui.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.IosShare
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.ExpenseTrackerApplication
import com.expensetracker.app.ui.components.WarmBackdrop
import com.expensetracker.app.ui.components.rememberSheetProgress
import com.expensetracker.app.ui.feature.home.Currency
import com.expensetracker.app.ui.feature.home.HomeViewModel
import com.expensetracker.app.ui.feature.home.currencies
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType

/** Which picker sheet is open over the settings list. */
private enum class SettingsSheet { NONE, CURRENCY, THEME, BACKUP, PRIVACY }

/**
 * Settings, matching the design: grouped appearance / data / automation / about
 * cards with 36dp icon tiles, trailing values, and an on-device footer.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    selectedCurrency: Currency,
    onSelectCurrency: (Currency) -> Unit,
    onViewExcluded: () -> Unit,
    onManageAliases: () -> Unit,
    onManageAccounts: () -> Unit,
    onIgnoredSenders: () -> Unit,
    onCategoryRules: () -> Unit,
    onReminders: () -> Unit,
    onOpenAgent: () -> Unit,
    onSheetOpenChange: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var sheet by remember { mutableStateOf(SettingsSheet.NONE) }
    var lastSheet by remember { mutableStateOf(SettingsSheet.CURRENCY) }
    if (sheet != SettingsSheet.NONE) lastSheet = sheet
    val sheetVisible = sheet != SettingsSheet.NONE

    // The tab bar steps aside while a sheet is open, so the sheet can own the
    // bottom of the screen instead of fighting a bar drawn on top of it.
    LaunchedEffect(sheetVisible) { onSheetOpenChange(sheetVisible) }
    androidx.activity.compose.BackHandler(enabled = sheetVisible) { sheet = SettingsSheet.NONE }

    var exportMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var smsActionIsExport by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true &&
            permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            (context.applicationContext as ExpenseTrackerApplication).registerSmsReceiverIfNeeded()
            if (smsActionIsExport) {
                viewModel.exportBankSmsToFile { exportMessage = it }
            } else {
                viewModel.importFromSms()
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingRestoreUri = uri }

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

    fun runSmsAction(export: Boolean) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) -> {
                if (export) viewModel.exportBankSmsToFile { exportMessage = it }
                else {
                    viewModel.importFromSms()
                    (context.applicationContext as ExpenseTrackerApplication).registerSmsReceiverIfNeeded()
                }
            }
            else -> {
                smsActionIsExport = export
                smsPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                )
            }
        }
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
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    color = c.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF241F1C), Color(0xFF342D26))))
                        .then(
                            Modifier.padding(0.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Transparent),
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
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Title + status pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.card.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8FA882)),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "LOCAL VAULT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp,
                            ),
                            color = c.textSecondary,
                        )
                    }
                }

                // Appearance
                SettingsSection("Appearance") {
                    SettingsRow(
                        icon = Icons.Rounded.CurrencyRupee,
                        title = "Currency",
                        value = "${selectedCurrency.code} (${selectedCurrency.symbol})",
                        onClick = { sheet = SettingsSheet.CURRENCY },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Theme",
                        value = "Smoked Olive & Champagne",
                        valueGold = true,
                        onClick = { sheet = SettingsSheet.THEME },
                    )
                }

                // Data
                SettingsSection("Data") {
                    SettingsRow(
                        icon = Icons.Rounded.Sms,
                        title = "Import from SMS",
                        onClick = { runSmsAction(export = false) },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.IosShare,
                        title = "Export Raw Bank SMS",
                        onClick = { runSmsAction(export = true) },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.VisibilityOff,
                        title = "Excluded Transactions",
                        onClick = onViewExcluded,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Backup,
                        title = "Backup & Restore",
                        onClick = { sheet = SettingsSheet.BACKUP },
                    )
                }

                // Automation
                SettingsSection("Automation") {
                    SettingsRow(
                        icon = Icons.Rounded.AccountBalance,
                        title = "Banks & Cards",
                        onClick = onManageAccounts,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Edit,
                        title = "Manage Aliases",
                        onClick = onManageAliases,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Category,
                        title = "Category Rules",
                        onClick = onCategoryRules,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Payment Reminders",
                        onClick = onReminders,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Block,
                        title = "Ignored Senders",
                        onClick = onIgnoredSenders,
                    )
                }

                // About
                SettingsSection("About") {
                    SettingsRow(
                        icon = Icons.Rounded.VerifiedUser,
                        title = "Privacy Policy",
                        onClick = { sheet = SettingsSheet.PRIVACY },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Version",
                        value = versionName,
                        valueMono = true,
                        mutedIcon = true,
                    )
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = c.goldSoft.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "All data stays on this device",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = c.textMuted,
                    )
                }
            }
        }

        // Sheets
        when (lastSheet) {
            SettingsSheet.CURRENCY -> CurrencySheet(
                current = selectedCurrency,
                visible = sheetVisible && sheet == SettingsSheet.CURRENCY,
                onDismiss = { sheet = SettingsSheet.NONE },
                onSelect = {
                    onSelectCurrency(it)
                    sheet = SettingsSheet.NONE
                },
            )
            SettingsSheet.THEME -> ThemeSheet(
                visible = sheetVisible && sheet == SettingsSheet.THEME,
                onDismiss = { sheet = SettingsSheet.NONE },
            )
            SettingsSheet.BACKUP -> BackupSheet(
                visible = sheetVisible && sheet == SettingsSheet.BACKUP,
                onDismiss = { sheet = SettingsSheet.NONE },
                onExport = {
                    sheet = SettingsSheet.NONE
                    exportBackupLauncher.launch("expense-tracker-backup-${java.time.LocalDate.now()}.zip")
                },
                onRestore = {
                    sheet = SettingsSheet.NONE
                    openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                },
            )
            SettingsSheet.PRIVACY -> PrivacySheet(
                visible = sheetVisible && sheet == SettingsSheet.PRIVACY,
                onDismiss = { sheet = SettingsSheet.NONE },
            )
            SettingsSheet.NONE -> Unit
        }
    }

    exportMessage?.let { message ->
        AppDialog(
            title = "Export SMS",
            message = message,
            confirm = "OK",
            onConfirm = { exportMessage = null },
            onDismiss = { exportMessage = null },
        )
    }

    pendingRestoreUri?.let { uri ->
        AppDialog(
            title = "Restore backup?",
            message = "Your current local data will be replaced with the contents of this backup. " +
                "The app will restart to apply it.",
            confirm = "Restore",
            destructive = true,
            onConfirm = {
                pendingRestoreUri = null
                viewModel.importBackup(uri)
            },
            onDismiss = { pendingRestoreUri = null },
        )
    }

    if (uiState.restoreReady) {
        AppDialog(
            title = "Restore complete",
            message = "Your backup was applied. Restart the app to load the restored data.",
            confirm = "Restart",
            onConfirm = { relaunchApp(context) },
            onDismiss = {},
            dismissible = false,
        )
    }
}

/* ------------------------------- list pieces ------------------------------ */

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val c = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
            ),
            color = c.textMuted,
            modifier = Modifier.padding(start = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.card.copy(alpha = 0.9f)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    valueGold: Boolean = false,
    valueMono: Boolean = false,
    mutedIcon: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    val tint = when {
        mutedIcon -> c.textMuted
        else -> c.gold
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (mutedIcon) Color(0xFF201D1A) else c.gold.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(13.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = c.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = if (valueMono) FontFamily.Monospace else FontFamily.Default,
                ),
                color = if (valueGold || valueMono) c.goldSoft else c.textMuted,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
        }
        if (onClick != null) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = c.textFaint.copy(alpha = 0.8f),
                modifier = Modifier.size(17.dp),
            )
        }
    }
}



/* --------------------------------- sheets --------------------------------- */

@Composable
private fun SheetShell(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val c = AppTheme.colors
    val progress = rememberSheetProgress(visible)
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxPanel = (maxHeight - 16.dp).coerceAtLeast(200.dp)

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - progress) * 420.dp.toPx() }
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(c.cardRaised)
                    .heightIn(max = maxPanel)
                    .padding(horizontal = 22.dp)
                    .padding(top = 12.dp, bottom = 14.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(c.textPrimary.copy(alpha = 0.18f)),
                )
                content()
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp),
            color = c.textPrimary,
            modifier = Modifier.weight(1f),
        )
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
}

@Composable
private fun CurrencySheet(
    current: Currency,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Currency) -> Unit,
) {
    val c = AppTheme.colors
    SheetShell(visible = visible, onDismiss = onDismiss) {
        SheetHeader("Currency", onDismiss)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            currencies.forEach { currency ->
                val isSelected = currency.code == current.code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) c.gold.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(currency) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = if (isSelected) c.gold else c.textSecondary,
                        modifier = Modifier.width(34.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currency.code,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = c.textPrimary,
                        )
                        Text(
                            text = currency.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = c.textMuted,
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = c.gold, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSheet(visible: Boolean, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    SheetShell(visible = visible, onDismiss = onDismiss) {
        SheetHeader("Theme", onDismiss)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.gold.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(c.gold, Color(0xFF8FA882)))
                    ),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Smoked Olive & Champagne",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold),
                    color = c.textPrimary,
                )
                Text(
                    text = "Dark, gold-lit, calm",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = c.textMuted,
                )
            }
            Icon(Icons.Rounded.Check, contentDescription = null, tint = c.gold, modifier = Modifier.size(17.dp))
        }
        Text(
            text = "More themes are on the way.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = c.textFaint,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun BackupSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
) {
    val c = AppTheme.colors
    SheetShell(visible = visible, onDismiss = onDismiss) {
        SheetHeader("Backup & Restore", onDismiss)
        Text(
            text = "Save everything as a file, or bring it back on a new phone.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = c.textSecondary,
        )
        ActionRow(
            icon = Icons.Rounded.IosShare,
            title = "Export backup",
            subtitle = "Write a .zip with all transactions and settings",
            onClick = onExport,
        )
        ActionRow(
            icon = Icons.Rounded.SystemUpdateAlt,
            title = "Restore backup",
            subtitle = "Replace local data from a backup file",
            onClick = onRestore,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.gold.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.gold, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                color = c.textPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = c.textMuted,
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun PrivacySheet(visible: Boolean, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    SheetShell(visible = visible, onDismiss = onDismiss) {
        SheetHeader("Privacy Policy", onDismiss)
        Text(
            text = "Your ledger never leaves this phone.\n\n" +
                "• Bank SMS are parsed on-device and never uploaded.\n" +
                "• Transactions, aliases and rules live in a local database.\n" +
                "• The AI agent only sends the question you type, and only when you use it.\n" +
                "• Backups are written wherever you choose; you control the file.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            color = c.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/* --------------------------------- dialog --------------------------------- */

@Composable
private fun AppDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissible: Boolean = true,
) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = dismissible,
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.cardRaised)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                color = c.textPrimary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = c.textSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissible) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp),
                        color = c.textMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = confirm,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (destructive) c.expense else c.gold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onConfirm,
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun relaunchApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}
