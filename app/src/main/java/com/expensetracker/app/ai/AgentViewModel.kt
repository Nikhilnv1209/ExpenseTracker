package com.expensetracker.app.ai

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.ChatMessageDao
import com.expensetracker.app.data.local.ChatMessageEntity
import com.expensetracker.app.data.local.ChatSessionDao
import com.expensetracker.app.data.local.ChatSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    application: Application,
    private val chatSessionDao: ChatSessionDao,
    private val chatMessageDao: ChatMessageDao,
) : AndroidViewModel(application) {

    private val financeAgent = FinanceAgent(application)

    private val _messages = MutableStateFlow<List<AgentMessage>>(emptyList())
    val messages: StateFlow<List<AgentMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
    val sessions: StateFlow<List<ChatSessionEntity>> = _sessions.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _sessions.value = chatSessionDao.getAll()
        }
    }

    fun searchSessions(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _sessions.value = chatSessionDao.getAll()
            } else {
                chatSessionDao.search(query).collect { _sessions.value = it }
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = chatSessionDao.findById(sessionId) ?: return@launch
            val dbMessages = chatMessageDao.getBySession(sessionId)
            _currentSessionId.value = sessionId
            _messages.value = dbMessages.map { msg ->
                AgentMessage(
                    role = if (msg.role == "user") AgentMessageRole.USER else AgentMessageRole.MODEL,
                    text = msg.content,
                    reasoning = msg.reasoning,
                )
            }
        }
    }

    fun startNewSession() {
        _currentSessionId.value = null
        _messages.value = emptyList()
    }

    private suspend fun ensureSessionId(firstMessage: String): Long {
        _currentSessionId.value?.let { return it }
        val now = System.currentTimeMillis()
        val title = firstMessage.take(50).ifBlank { "New chat" }
        val id = chatSessionDao.insert(
            ChatSessionEntity(
                title = title,
                createdAt = now,
                updatedAt = now,
                preview = firstMessage.take(100),
                messageCount = 1,
            )
        )
        _currentSessionId.value = id
        loadSessions()
        return id
    }

    private suspend fun saveMessage(sessionId: Long, role: String, content: String, reasoning: String?) {
        chatMessageDao.insert(
            ChatMessageEntity(
                sessionId = sessionId,
                role = role,
                content = content,
                reasoning = reasoning,
                timestamp = System.currentTimeMillis(),
            )
        )
        chatSessionDao.updatePreview(
            sessionId = sessionId,
            preview = content.take(100),
            updatedAt = System.currentTimeMillis(),
        )
        loadSessions()
    }

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
            val sessionId = ensureSessionId(text)
            saveMessage(sessionId, "user", text, null)

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

            val finalReasoning = if (reasoningBuilder.isNotEmpty()) reasoningBuilder.toString() else null
            saveMessage(sessionId, "assistant", response, finalReasoning)
            _isLoading.value = false
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            chatMessageDao.deleteBySession(id)
            chatSessionDao.delete(id)
            if (_currentSessionId.value == id) {
                startNewSession()
            }
            loadSessions()
        }
    }

    fun clear() {
        startNewSession()
    }
}

data class AgentMessage(
    val role: AgentMessageRole,
    val text: String,
    val reasoning: String? = null,
)

enum class AgentMessageRole { USER, MODEL }
