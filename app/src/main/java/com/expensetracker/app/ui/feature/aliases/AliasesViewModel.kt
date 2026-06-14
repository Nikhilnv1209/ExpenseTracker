package com.expensetracker.app.ui.feature.aliases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.AliasDao
import com.expensetracker.app.data.local.AliasEntity
import com.expensetracker.app.data.local.TransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AliasesUiState(
    val aliases: List<AliasEntity> = emptyList(),
)

@HiltViewModel
class AliasesViewModel @Inject constructor(
    private val aliasDao: AliasDao,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AliasesUiState())
    val uiState: StateFlow<AliasesUiState> = _uiState.asStateFlow()

    init {
        loadAliases()
    }

    fun loadAliases() {
        viewModelScope.launch {
            val aliases = aliasDao.getAll()
            _uiState.update { it.copy(aliases = aliases) }
        }
    }

    fun updateAlias(originalTitle: String, newAlias: String?) {
        viewModelScope.launch {
            if (newAlias != null) {
                aliasDao.upsert(AliasEntity(originalTitle, newAlias))
            } else {
                aliasDao.delete(originalTitle)
            }
            transactionDao.applyAliasToAll(originalTitle, newAlias)
            loadAliases()
        }
    }

    fun deleteAlias(originalTitle: String) {
        viewModelScope.launch {
            aliasDao.delete(originalTitle)
            transactionDao.applyAliasToAll(originalTitle, null)
            loadAliases()
        }
    }
}
