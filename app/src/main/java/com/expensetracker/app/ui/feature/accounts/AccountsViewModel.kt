package com.expensetracker.app.ui.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.AccountDao
import com.expensetracker.app.data.local.AccountEntity
import com.expensetracker.app.data.local.AccountKind
import com.expensetracker.app.data.local.DetectedAccount
import com.expensetracker.app.data.local.TransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountRow(
    val account: AccountEntity,
    val transactionCount: Int,
)

data class AccountsUiState(
    val accounts: List<AccountRow> = emptyList(),
    val detected: List<DetectedAccount> = emptyList(),
    val banks: List<String> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { loadInternal() }
    }

    private suspend fun loadInternal() {
        val accounts = accountDao.getAll().map { account ->
            AccountRow(
                account = account,
                transactionCount = transactionDao.countTransactionsForAccount(account.id),
            )
        }
        val detected = transactionDao.getDetectedAccounts()
            .filter { it.bankName.isNotBlank() && it.last4.isNotBlank() && it.bankName != "Bank" }
        val banks = (transactionDao.getUniqueBanks().filterNotNull() + accounts.map { it.account.bankName })
            .filter { it.isNotBlank() && it != "Bank" }
            .distinct()
            .sorted()

        _uiState.update {
            it.copy(
                accounts = accounts,
                detected = detected,
                banks = banks,
            )
        }
    }

    fun saveAccount(
        id: Long?,
        name: String,
        bankName: String,
        kind: AccountKind,
        last4: String,
    ) {
        val cleanName = name.trim()
        val cleanBank = bankName.trim()
        val cleanLast4 = last4.filter(Char::isDigit).take(4)
        if (cleanName.isBlank() || cleanBank.isBlank() || cleanLast4.length !in 3..4) return

        viewModelScope.launch {
            val duplicate = accountDao.findByBankAndLast4(cleanBank, cleanLast4)
            if (duplicate != null && duplicate.id != id) {
                _uiState.update {
                    it.copy(message = "$cleanBank ··$cleanLast4 is already added as \"${duplicate.name}\".")
                }
                return@launch
            }

            if (id == null) {
                val newId = accountDao.insert(
                    AccountEntity(
                        name = cleanName,
                        bankName = cleanBank,
                        kind = kind.name,
                        last4 = cleanLast4,
                    ),
                )
                val linked = transactionDao.linkTransactionsToAccount(
                    accountId = newId,
                    accountName = cleanName,
                    bankName = cleanBank,
                    last4 = cleanLast4,
                )
                _uiState.update {
                    it.copy(
                        message = if (linked > 0) {
                            "Account added and linked to $linked transaction${if (linked == 1) "" else "s"}."
                        } else {
                            "Account added."
                        },
                    )
                }
            } else {
                val existing = accountDao.findById(id) ?: return@launch
                accountDao.update(
                    existing.copy(
                        name = cleanName,
                        bankName = cleanBank,
                        kind = kind.name,
                        last4 = cleanLast4,
                    ),
                )
                transactionDao.renameAccountInTransactions(id, cleanName)
                transactionDao.linkTransactionsToAccount(
                    accountId = id,
                    accountName = cleanName,
                    bankName = cleanBank,
                    last4 = cleanLast4,
                )
                _uiState.update { it.copy(message = "Account updated.") }
            }
            loadInternal()
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            transactionDao.unlinkAccountFromTransactions(account.id)
            accountDao.deleteById(account.id)
            _uiState.update { it.copy(message = "\"${account.name}\" removed. Its transactions are unlinked.") }
            loadInternal()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
