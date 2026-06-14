package com.expensetracker.app.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val smsImportUseCase: SmsImportUseCase,
    private val smsExportUseCase: com.expensetracker.app.sms.SmsExportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = LocalDate.now()
            val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
            val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())

            val transactions = transactionDao.getByDateRange(
                monthStart.toEpochDay(),
                monthEnd.toEpochDay(),
            ).map { it.toDomain() }

            val incomeTotal = transactionDao.getIncomeTotal(
                monthStart.toEpochDay(),
                monthEnd.toEpochDay(),
            )
            val expenseTotal = transactionDao.getExpenseTotal(
                monthStart.toEpochDay(),
                monthEnd.toEpochDay(),
            )

            val dailyTotals = transactionDao.getDailyExpenseTotals(
                monthStart.toEpochDay(),
                monthEnd.toEpochDay(),
            )

            val dailyExpenses = (1..monthEnd.dayOfMonth).map { day ->
                com.expensetracker.app.ui.components.DailyExpense(
                    day = day,
                    total = dailyTotals.find { it.date == monthStart.plusDays((day - 1).toLong()).toEpochDay() }?.total ?: 0.0,
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    transactions = transactions,
                    totalIncome = incomeTotal,
                    totalExpense = expenseTotal,
                    totalBalance = incomeTotal - expenseTotal,
                    dailyExpenses = dailyExpenses,
                )
            }
        }
    }

    fun importFromSms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = smsImportUseCase.importTransactions()
            _uiState.update { it.copy(isLoading = false, importResult = result) }
            loadTransactions()
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
