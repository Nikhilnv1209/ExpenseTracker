package com.expensetracker.app.ai

import android.app.Application
import android.util.Log
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

        val history = _messages.value
            .filter { it.text.isNotBlank() }
            .map { msg ->
                (if (msg.role == AgentMessageRole.USER) "user" else "assistant") to msg.text
            }

        _messages.value += AgentMessage(role = AgentMessageRole.USER, text = text)
        _isLoading.value = true

        viewModelScope.launch {
            var msgAdded = false
            val reasoningBuilder = StringBuilder()

            val response = financeAgent.chat(
                userMessage = text,
                history = history,
                onReasoning = { delta ->
                    reasoningBuilder.append(delta)
                    if (!msgAdded) {
                        msgAdded = true
                        _isLoading.value = false
                        _messages.value += AgentMessage(
                            role = AgentMessageRole.MODEL,
                            text = "",
                            reasoning = reasoningBuilder.toString(),
                        )
                    } else {
                        _messages.value = _messages.value.toMutableList().also {
                            it[it.lastIndex] = it[it.lastIndex].copy(reasoning = reasoningBuilder.toString())
                        }
                    }
                },
                onContent = { partial ->
                    if (!msgAdded) {
                        msgAdded = true
                        _isLoading.value = false
                        _messages.value += AgentMessage(role = AgentMessageRole.MODEL, text = partial)
                    } else {
                        _messages.value = _messages.value.toMutableList().also {
                            it[it.lastIndex] = it[it.lastIndex].copy(text = partial)
                        }
                    }
                },
            )

            if (!msgAdded) {
                _isLoading.value = false
                _messages.value += AgentMessage(role = AgentMessageRole.MODEL, text = response)
            } else {
                _messages.value = _messages.value.toMutableList().also {
                    it[it.lastIndex] = it[it.lastIndex].copy(text = response)
                }
            }
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
    val reasoning: String? = null,
)

enum class AgentMessageRole { USER, MODEL }
