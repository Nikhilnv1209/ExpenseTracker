package com.expensetracker.app.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.AliasDao
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.data.local.toDomain
import com.expensetracker.app.data.local.toEntity
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.sms.SmsImportResult
import com.expensetracker.app.sms.SmsImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val transactionDao: TransactionDao,
    private val aliasDao: AliasDao,
    private val smsImportUseCase: SmsImportUseCase,
    private val smsExportUseCase: com.expensetracker.app.sms.SmsExportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transactionDao.observeAll().collect { entities ->
                val transactions = entities.map { it.toDomain() }
                refreshState(transactions)
            }
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
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val dailyExpenses: List<com.expensetracker.app.ui.components.DailyExpense> = emptyList(),
    val importResult: SmsImportResult? = null,
)
