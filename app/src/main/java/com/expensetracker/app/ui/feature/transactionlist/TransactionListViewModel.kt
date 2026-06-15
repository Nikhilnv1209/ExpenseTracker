package com.expensetracker.app.ui.feature.transactionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.AliasDao
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.data.local.toDomain
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.feature.home.FilterPeriod
import com.expensetracker.app.ui.feature.home.TransactionFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class TransactionListUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val filter: TransactionFilter = TransactionFilter(period = FilterPeriod.ALL_TIME),
    val bankSuggestions: List<String> = emptyList(),
)

private val TransactionListDefaultFilter = TransactionFilter(period = FilterPeriod.ALL_TIME)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val aliasDao: AliasDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadFiltered()
    }

    fun loadFiltered(filter: TransactionFilter = _uiState.value.filter) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val (transactions, incomeTotal, expenseTotal) = fetchFiltered(filter)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        transactions = transactions,
                        totalIncome = incomeTotal,
                        totalExpense = expenseTotal,
                    )
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setFilter(filter: TransactionFilter) {
        loadJob?.cancel()
        _uiState.update { it.copy(filter = filter) }
        loadFiltered(filter)
    }

    fun resetFilter() {
        loadJob?.cancel()
        _uiState.update { it.copy(filter = TransactionListDefaultFilter) }
        loadFiltered(TransactionListDefaultFilter)
    }

    fun loadBankSuggestions() {
        viewModelScope.launch {
            val banks = transactionDao.getUniqueBanks().filterNotNull().filter { it.isNotBlank() }
            _uiState.update { it.copy(bankSuggestions = banks) }
        }
    }

    private suspend fun fetchFiltered(filter: TransactionFilter): Triple<List<Transaction>, Double, Double> {
        val today = LocalDate.now()
        val (start, end) = when (filter.period) {
            FilterPeriod.THIS_WEEK -> today.minusDays(today.dayOfWeek.value.toLong() - 1) to today
            FilterPeriod.THIS_MONTH -> today.with(TemporalAdjusters.firstDayOfMonth()) to today.with(TemporalAdjusters.lastDayOfMonth())
            FilterPeriod.LAST_MONTH -> today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()) to today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
            FilterPeriod.ALL_TIME -> LocalDate.of(2020, 1, 1) to today.plusYears(10)
        }
        val startDate = filter.customStart ?: start.toEpochDay()
        val endDate = filter.customEnd ?: end.toEpochDay()
        val search = filter.search.ifBlank { null }
        val bank = filter.bank
        val type = filter.type

        val transactions = transactionDao.getFiltered(startDate, endDate, type, search, bank).map { it.toDomain() }
        val incomeTotal = transactionDao.getIncomeTotal(startDate, endDate)
        val expenseTotal = transactionDao.getExpenseTotal(startDate, endDate)
        return Triple(transactions, incomeTotal, expenseTotal)
    }

    fun toggleExcluded(transactionId: Long, excluded: Boolean) {
        viewModelScope.launch {
            transactionDao.setExcluded(transactionId, excluded)
            loadFiltered()
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
            loadFiltered()
        }
    }

    fun setNote(transactionId: Long, note: String?) {
        viewModelScope.launch {
            transactionDao.setNote(transactionId, note?.trim()?.ifBlank { null })
            loadFiltered()
        }
    }
}
