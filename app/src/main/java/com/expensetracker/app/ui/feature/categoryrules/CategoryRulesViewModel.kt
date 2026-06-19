package com.expensetracker.app.ui.feature.categoryrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.CategoryRuleDao
import com.expensetracker.app.data.local.CategoryRuleEntity
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRulesUiState(
    val rules: List<CategoryRuleEntity> = emptyList(),
)

@HiltViewModel
class CategoryRulesViewModel @Inject constructor(
    private val categoryRuleDao: CategoryRuleDao,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryRulesUiState())
    val uiState: StateFlow<CategoryRulesUiState> = _uiState.asStateFlow()

    init {
        loadRules()
    }

    fun loadRules() {
        viewModelScope.launch {
            _uiState.update { it.copy(rules = categoryRuleDao.getAll()) }
        }
    }

    fun updateCategory(title: String, category: Category) {
        viewModelScope.launch {
            categoryRuleDao.upsert(CategoryRuleEntity(title, category.displayName))
            transactionDao.applyCategoryToAll(title, category.displayName)
            loadRules()
        }
    }

    fun deleteRule(title: String) {
        viewModelScope.launch {
            categoryRuleDao.delete(title)
            loadRules()
        }
    }
}
