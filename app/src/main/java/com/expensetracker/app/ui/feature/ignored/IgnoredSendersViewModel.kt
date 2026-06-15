package com.expensetracker.app.ui.feature.ignored

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.IgnoredSenderDao
import com.expensetracker.app.data.local.IgnoredSenderEntity
import com.expensetracker.app.data.local.TransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IgnoredSendersUiState(
    val senders: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

@HiltViewModel
class IgnoredSendersViewModel @Inject constructor(
    private val ignoredSenderDao: IgnoredSenderDao,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IgnoredSendersUiState())
    val uiState: StateFlow<IgnoredSendersUiState> = _uiState.asStateFlow()

    private var allTitles: List<String> = emptyList()

    init {
        loadSenders()
        loadTitles()
    }

    private fun loadTitles() {
        viewModelScope.launch {
            allTitles = transactionDao.getUniqueTitles()
        }
    }

    fun loadSenders() {
        viewModelScope.launch {
            val senders = ignoredSenderDao.getAll().map { it.sender }
            _uiState.update { it.copy(senders = senders) }
        }
    }

    fun addSender(sender: String) {
        val trimmed = sender.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            ignoredSenderDao.upsert(IgnoredSenderEntity(trimmed))
            transactionDao.setExcludedByTitle(trimmed, true)
            loadSenders()
        }
        _uiState.update { it.copy(suggestions = emptyList()) }
    }

    fun removeSender(sender: String) {
        viewModelScope.launch {
            ignoredSenderDao.delete(sender)
            transactionDao.setExcludedByTitle(sender, false)
            loadSenders()
        }
    }

    fun onQueryChanged(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }
        val lowerQuery = query.lowercase()
        val ignored = _uiState.value.senders.map { it.lowercase() }.toSet()
        val filtered = allTitles
            .filter { lowerQuery in it.lowercase() && it.lowercase() !in ignored }
            .take(5)
        _uiState.update { it.copy(suggestions = filtered) }
    }

    fun clearSuggestions() {
        _uiState.update { it.copy(suggestions = emptyList()) }
    }
}
