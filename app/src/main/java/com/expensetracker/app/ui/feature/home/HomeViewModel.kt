package com.expensetracker.app.ui.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.AliasDao
import com.expensetracker.app.data.local.CategoryRuleDao
import com.expensetracker.app.data.local.CategoryRuleEntity
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.data.local.toDomain
import com.expensetracker.app.data.local.toEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.sms.SmsImportResult
import com.expensetracker.app.sms.SmsImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val transactionDao: TransactionDao,
    private val aliasDao: AliasDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val smsImportUseCase: SmsImportUseCase,
    private val smsExportUseCase: com.expensetracker.app.sms.SmsExportUseCase,
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState(balanceMode = prefs.getInt("balanceMode", 0)))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var isRefreshing = false

    init {
        viewModelScope.launch {
            transactionDao.observeAll().collect { entities ->
                val transactions = entities.map { it.toDomain() }
                val currentFilter = _uiState.value.filter
                if (currentFilter == TransactionFilter()) {
                    refreshState(transactions)
                } else {
                    applyFilter(currentFilter)
                }
            }
        }
    }

    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val importJob = launch { smsImportUseCase.importTransactions() }
            kotlinx.coroutines.delay(800)
            importJob.join()
            val currentFilter = _uiState.value.filter
            if (currentFilter == TransactionFilter()) {
                val entities = transactionDao.getAllSorted()
                val transactions = entities.map { it.toDomain() }
                refreshState(transactions)
            } else {
                applyFilter(currentFilter)
            }
            _uiState.update { it.copy(isRefreshing = false) }
            isRefreshing = false
        }
    }

    private suspend fun refreshState(transactions: List<Transaction>) {
        val today = LocalDate.now()
        val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
        val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())

        val monthTransactions = transactions.filter {
            !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd)
        }

        val incomeTotal = transactionDao.getIncomeTotal(
            monthStart.toEpochDay(),
            monthEnd.toEpochDay(),
        )
        val expenseTotal = transactionDao.getExpenseTotal(
            monthStart.toEpochDay(),
            monthEnd.toEpochDay(),
        )

        val categoryTotals = transactionDao.getCategoryTotals(
            monthStart.toEpochDay(),
            monthEnd.toEpochDay(),
        )

        val chartStart = today.minusMonths(6).with(TemporalAdjusters.firstDayOfMonth())
        val chartEnd = today.plusMonths(6).with(TemporalAdjusters.lastDayOfMonth())

        val dailyTotals = transactionDao.getDailyExpenseTotals(
            chartStart.toEpochDay(),
            chartEnd.toEpochDay(),
        )

        val dailyIncomeTotals = transactionDao.getDailyIncomeTotals(
            chartStart.toEpochDay(),
            chartEnd.toEpochDay(),
        )

        val dailyExpenses = (dailyTotals.map { it.date } + dailyIncomeTotals.map { it.date })
            .toSet()
            .map { epochDay ->
                val localDate = LocalDate.ofEpochDay(epochDay)
                com.expensetracker.app.ui.components.DailyExpense(
                    day = localDate.dayOfMonth,
                    epochDay = epochDay,
                    total = dailyTotals.find { it.date == epochDay }?.total ?: 0.0,
                    income = dailyIncomeTotals.find { it.date == epochDay }?.total ?: 0.0,
                )
            }

        _uiState.update {
            it.copy(
                isLoading = false,
                transactions = monthTransactions,
                totalIncome = incomeTotal,
                totalExpense = expenseTotal,
                totalBalance = incomeTotal - expenseTotal,
                dailyExpenses = dailyExpenses,
                categoryTotals = categoryTotals,
            )
        }
    }

    fun importFromSms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = smsImportUseCase.importTransactions()
            _uiState.update { it.copy(isLoading = false, importResult = result) }
        }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    fun exportBankSmsToFile(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val uri = smsExportUseCase.exportBankSmsToFile()
            val message = if (uri != null) {
                "Exported bank SMS to:\n$uri"
            } else {
                "No bank SMS found to export"
            }
            onResult(message)
        }
    }

    fun toggleExcluded(transactionId: Long, excluded: Boolean) {
        viewModelScope.launch {
            transactionDao.setExcluded(transactionId, excluded)
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
        }
    }

    fun setNote(transactionId: Long, note: String?) {
        viewModelScope.launch {
            transactionDao.setNote(transactionId, note?.trim()?.ifBlank { null })
        }
    }

    fun setCategory(transactionId: Long, title: String, category: Category, applyToAll: Boolean) {
        viewModelScope.launch {
            if (applyToAll) {
                categoryRuleDao.upsert(CategoryRuleEntity(title, category.displayName))
                transactionDao.applyCategoryToAll(title, category.displayName)
                transactionDao.setCategoryExempt(transactionId, false)
            } else {
                transactionDao.updateCategory(transactionId, category.displayName)
                transactionDao.setCategoryExempt(transactionId, true)
            }
        }
    }

    fun setCategoryExempt(transactionId: Long, title: String, exempt: Boolean) {
        viewModelScope.launch {
            transactionDao.setCategoryExempt(transactionId, exempt)
            if (!exempt) {
                categoryRuleDao.findByTitle(title)?.let { rule ->
                    transactionDao.updateCategory(transactionId, rule.category)
                }
            }
        }
    }

    suspend fun getCategoryRule(title: String): Category? {
        return categoryRuleDao.findByTitle(title)?.category?.let { Category.fromDisplayName(it) }
    }

    fun cycleBalanceMode() {
        val newMode = (_uiState.value.balanceMode + 1) % 3
        prefs.edit().putInt("balanceMode", newMode).apply()
        _uiState.update { it.copy(balanceMode = newMode) }
    }

    fun setFilter(filter: TransactionFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilter(filter)
    }

    private fun applyFilter(filter: TransactionFilter) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val (start, end) = when (filter.period) {
                FilterPeriod.THIS_WEEK -> today.minusDays(today.dayOfWeek.value.toLong() - 1) to today
                FilterPeriod.THIS_MONTH -> today.with(TemporalAdjusters.firstDayOfMonth()) to today.with(TemporalAdjusters.lastDayOfMonth())
                FilterPeriod.LAST_MONTH -> today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()) to today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
                FilterPeriod.ALL_TIME -> LocalDate.of(2020, 1, 1) to today.plusYears(10)
            }
            val startDate = (filter.customStart ?: start.toEpochDay())
            val endDate = (filter.customEnd ?: end.toEpochDay())
            val search = filter.search.ifBlank { null }
            val bank = filter.bank
            val type = filter.type

            val filtered = transactionDao.getFiltered(startDate, endDate, type, search, bank).map { it.toDomain() }

            val incomeTotal = transactionDao.getIncomeTotal(startDate, endDate)
            val expenseTotal = transactionDao.getExpenseTotal(startDate, endDate)

            _uiState.update {
                it.copy(
                    transactions = filtered,
                    totalIncome = incomeTotal,
                    totalExpense = expenseTotal,
                    totalBalance = incomeTotal - expenseTotal,
                )
            }
        }
    }

    fun loadBankSuggestions() {
        viewModelScope.launch {
            val banks = transactionDao.getUniqueBanks().filterNotNull().filter { it.isNotBlank() }
            _uiState.update { it.copy(bankSuggestions = banks) }
        }
    }

    fun resetFilter() {
        val default = TransactionFilter()
        _uiState.update { it.copy(filter = default) }
        refresh()
    }
}

data class TransactionFilter(
    val search: String = "",
    val type: String? = null,
    val bank: String? = null,
    val period: FilterPeriod = FilterPeriod.THIS_MONTH,
    val customStart: Long? = null,
    val customEnd: Long? = null,
)

enum class FilterPeriod {
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    ALL_TIME,
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val dailyExpenses: List<com.expensetracker.app.ui.components.DailyExpense> = emptyList(),
    val categoryTotals: List<com.expensetracker.app.data.local.TransactionDao.CategoryTotal> = emptyList(),
    val importResult: SmsImportResult? = null,
    val balanceMode: Int = 0,
    val filter: TransactionFilter = TransactionFilter(),
    val bankSuggestions: List<String> = emptyList(),
)
