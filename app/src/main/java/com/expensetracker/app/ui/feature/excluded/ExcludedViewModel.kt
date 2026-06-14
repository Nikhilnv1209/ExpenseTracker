package com.expensetracker.app.ui.feature.excluded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class ExcludedUiState(
    val transactions: List<Transaction> = emptyList(),
)

@HiltViewModel
class ExcludedViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExcludedUiState())
    val uiState: StateFlow<ExcludedUiState> = _uiState.asStateFlow()

    init {
        loadExcluded()
    }

    fun loadExcluded() {
        viewModelScope.launch {
            val transactions = transactionDao.getExcluded().map { it.toDomain() }
            _uiState.update { it.copy(transactions = transactions) }
        }
    }

    fun includeTransaction(id: Long) {
        viewModelScope.launch {
            transactionDao.setExcluded(id, false)
            loadExcluded()
        }
    }
}
