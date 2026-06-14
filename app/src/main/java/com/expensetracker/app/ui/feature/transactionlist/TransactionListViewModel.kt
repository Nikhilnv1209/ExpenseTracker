package com.expensetracker.app.ui.feature.transactionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.AliasDao
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.data.local.toDomain
import com.expensetracker.app.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionListUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val aliasDao: AliasDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val transactions = transactionDao.getAllSorted().map { it.toDomain() }
            val incomeTotal = transactionDao.getAllIncomeTotal()
            val expenseTotal = transactionDao.getAllExpenseTotal()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    transactions = transactions,
                    totalIncome = incomeTotal,
                    totalExpense = expenseTotal,
                )
            }
        }
    }

    fun toggleExcluded(transactionId: Long, excluded: Boolean) {
        viewModelScope.launch {
            transactionDao.setExcluded(transactionId, excluded)
            loadAll()
        }
    }

    fun setAlias(transactionId: Long, originalTitle: String, alias: String?) {
        viewModelScope.launch {
            if (alias != null) {
                aliasDao.upsert(com.expensetracker.app.data.local.AliasEntity(originalTitle, alias))
            } else {
                aliasDao.delete(originalTitle)
            }
            transactionDao.applyAliasToAll(originalTitle, alias)
            loadAll()
        }
    }
}
