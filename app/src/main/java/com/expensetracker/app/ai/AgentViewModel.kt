package com.expensetracker.app.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val financeAgent = FinanceAgent(application)

    private val _messages = MutableStateFlow<List<AgentMessage>>(emptyList())
    val messages: StateFlow<List<AgentMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.value += AgentMessage(role = AgentMessageRole.USER, text = text)
        _isLoading.value = true

        viewModelScope.launch {
            val response = financeAgent.chat(text)
            _messages.value += AgentMessage(role = AgentMessageRole.MODEL, text = response.reply)
            _isLoading.value = false
        }
    }

    fun clear() {
        _messages.value = emptyList()
    }
}

data class AgentMessage(
    val role: AgentMessageRole,
    val text: String,
)

enum class AgentMessageRole { USER, MODEL }
