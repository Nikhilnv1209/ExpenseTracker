package com.expensetracker.app.ui.feature.accounts

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.app.data.local.AccountEntity
import com.expensetracker.app.data.local.AccountKind
import com.expensetracker.app.data.local.DetectedAccount
import com.expensetracker.app.ui.components.GlassCard

private val Accent = Color(0xFF7C3AED)

private data class AccountDraft(
    val id: Long? = null,
    val name: String = "",
    val bankName: String = "",
    val kind: AccountKind = AccountKind.BANK,
    val last4: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<AccountDraft?>(null) }
    var deleting by remember { mutableStateOf<AccountEntity?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    editing?.let { draft ->
        AccountEditorDialog(
            draft = draft,
            banks = uiState.banks,
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.saveAccount(
                    id = updated.id,
                    name = updated.name,
                    bankName = updated.bankName,
                    kind = updated.kind,
                    last4 = updated.last4,
                )
                editing = null
            },
        )
    }

    deleting?.let { account ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(account)
                    deleting = null
                }) { Text("Delete", color = Color(0xFFE91E63)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            },
            title = { Text("Remove account?") },
            text = {
                Text(
                    "\"${account.name}\" will be removed. Its transactions stay, but they won't be linked to a named account.",
                )
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TopAppBar(
            title = { Text("Banks & Cards") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            actions = {
                IconButton(onClick = { editing = AccountDraft() }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add bank or card", tint = Accent)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Name the bank accounts and cards that appear in your SMS. Matching transactions are linked automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            if (uiState.accounts.isEmpty()) {
                item {
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        tint = Accent,
                        tintAlpha = 0.05f,
                        borderAlpha = 0.08f,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = Accent.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No banks or cards yet", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                text = "Tap + to add one.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            } else {
                items(uiState.accounts, key = { it.account.id }) { row ->
                    AccountCard(
                        row = row,
                        onEdit = {
                            editing = AccountDraft(
                                id = row.account.id,
                                name = row.account.name,
                                bankName = row.account.bankName,
                                kind = runCatching { AccountKind.valueOf(row.account.kind) }
                                    .getOrDefault(AccountKind.BANK),
                                last4 = row.account.last4,
                            )
                        },
                        onDelete = { deleting = row.account },
                    )
                }
            }

            if (uiState.detected.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Detected from transactions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "These appear in imported SMS but aren't named yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                items(uiState.detected, key = { "detected-${it.bankName}-${it.last4}" }) { detected ->
                    DetectedAccountCard(
                        detected = detected,
                        onAdd = {
                            editing = AccountDraft(
                                name = "${detected.bankName} ··${detected.last4}",
                                bankName = detected.bankName,
                                last4 = detected.last4,
                                kind = AccountKind.BANK,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    row: AccountRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val account = row.account
    val isCard = account.kind == AccountKind.CARD.name
    GlassCard(
        shape = RoundedCornerShape(16.dp),
        tint = Accent,
        tintAlpha = 0.06f,
        borderAlpha = 0.08f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isCard) Icons.Rounded.CreditCard else Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(account.bankName).append(" ··").append(account.last4)
                        append(" · ")
                        append(if (row.transactionCount == 1) "1 transaction" else "${row.transactionCount} transactions")
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remove ${account.name}",
                    tint = Color(0xFFE91E63).copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DetectedAccountCard(
    detected: DetectedAccount,
    onAdd: () -> Unit,
) {
    GlassCard(
        shape = RoundedCornerShape(14.dp),
        tint = Color(0xFFF59E0B),
        tintAlpha = 0.05f,
        borderAlpha = 0.07f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${detected.bankName} ··${detected.last4}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (detected.transactionCount == 1) {
                        "1 transaction found"
                    } else {
                        "${detected.transactionCount} transactions found"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            TextButton(onClick = onAdd) {
                Text("Add", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AccountEditorDialog(
    draft: AccountDraft,
    banks: List<String>,
    onDismiss: () -> Unit,
    onSave: (AccountDraft) -> Unit,
) {
    var form by remember(draft) { mutableStateOf(draft) }
    var bankMenu by remember { mutableStateOf(false) }
    val canSave = form.name.isNotBlank() && form.bankName.isNotBlank() && form.last4.length in 3..4

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(form) },
                enabled = canSave,
            ) { Text("Save", color = if (canSave) Accent else Accent.copy(alpha = 0.4f)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(if (form.id == null) "Add bank or card" else "Edit bank or card") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { form = form.copy(name = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    placeholder = { Text("e.g. ICICI Salary, HDFC Credit Card") },
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp),
                )

                Box {
                    OutlinedTextField(
                        value = form.bankName,
                        onValueChange = { form = form.copy(bankName = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bank") },
                        placeholder = { Text("e.g. ICICI Bank") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { bankMenu = true }) {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Detected banks")
                            }
                        },
                        colors = fieldColors(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    DropdownMenu(
                        expanded = bankMenu,
                        onDismissRequest = { bankMenu = false },
                    ) {
                        if (banks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No detected banks yet") },
                                onClick = { bankMenu = false },
                                enabled = false,
                            )
                        } else {
                            banks.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text(bank) },
                                    onClick = {
                                        form = form.copy(bankName = bank)
                                        bankMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = form.last4,
                    onValueChange = {
                        form = form.copy(last4 = it.filter(Char::isDigit).take(4))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Last 3-4 digits") },
                    placeholder = { Text("5381 or 387") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(10.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = form.kind == AccountKind.BANK,
                        onClick = { form = form.copy(kind = AccountKind.BANK) },
                        label = { Text("Bank account") },
                    )
                    FilterChip(
                        selected = form.kind == AccountKind.CARD,
                        onClick = { form = form.copy(kind = AccountKind.CARD) },
                        label = { Text("Card") },
                    )
                }
            }
        },
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    cursorColor = Accent,
)
