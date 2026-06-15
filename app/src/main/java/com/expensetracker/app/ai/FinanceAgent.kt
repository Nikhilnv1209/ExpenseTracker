package com.expensetracker.app.ai

import android.content.Context
import androidx.core.content.edit
import com.expensetracker.app.ExpenseTrackerApplication
import com.expensetracker.app.data.local.toDomain
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.ui.feature.home.FilterPeriod
import com.expensetracker.app.ui.feature.home.TransactionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FinanceAgent(context: Context) {

    private val appContext = context.applicationContext
    private val settingsStore = AiSettingsStore(appContext)
    private val client = AiClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val transactionDao by lazy { (appContext as ExpenseTrackerApplication).transactionDao }
    private val aliasDao by lazy { (appContext as ExpenseTrackerApplication).aliasDao }
    private val ignoredSenderDao by lazy { (appContext as ExpenseTrackerApplication).ignoredSenderDao }

    suspend fun chat(userMessage: String): AgentResponse = withContext(Dispatchers.IO) {
        if (!settingsStore.isConfigured()) {
            return@withContext AgentResponse("AI is not enabled. Add your API key in Settings → AI Agent.")
        }

        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(userMessage)

        val raw = try {
            client.complete(settingsStore, systemPrompt, userPrompt)
        } catch (e: Exception) {
            return@withContext AgentResponse("AI error: ${e.localizedMessage ?: e.message}")
        } ?: return@withContext AgentResponse("No response from AI. Check your API key, base URL, and network.")

        parseAndExecute(raw)
    }

    private fun buildSystemPrompt(): String {
        return """
            You are a helpful, privacy-aware finance assistant inside an Android expense tracker app.
            The app runs offline by default. The user has voluntarily enabled you by providing their own cloud AI API key.

            Today is ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}.

            Available tools (optional — only include an action if needed):

            1. setCategory
               params: transactionId (Long), category (String)
               Use when the user wants to change a transaction category. Valid categories: ${Category.entries.joinToString()}.

            2. setAlias
               params: originalTitle (String), alias (String)
               Use when the user wants to rename a merchant/UPI title. This applies to all matching transactions.

            3. setNote
               params: transactionId (Long), note (String)
               Use when the user wants to add a personal note/reason to a transaction.

            4. excludeTransaction
               params: transactionId (Long)
               Use when the user wants to exclude a transaction from balance/totals.

            5. addIgnoredSender
               params: sender (String)
               Use when the user says they never want to import transactions from a certain sender again.

            6. summarize
               params: period (String: today/this_week/this_month/last_month/all_time)
               Use when the user asks for an overview of their spending. The app already provides the data, so just confirm with a friendly summary.

            Rules:
            - Always reply in valid JSON.
            - If no action is needed, set action to null.
            - When returning data to the user (transactions, totals), summarize in plain language.
            - Be concise and friendly.

            JSON format:
            {
              "reply": "Your message to the user",
              "action": { "name": "toolName", "params": { "key": "value" } }
            }
        """.trimIndent()
    }

    private suspend fun buildUserPrompt(userMessage: String): String {
        val today = LocalDate.now()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val monthStart = today.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth())
        val monthEnd = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())

        val todayExpense = transactionDao.getExpenseTotal(today.toEpochDay(), today.toEpochDay())
        val todayIncome = transactionDao.getIncomeTotal(today.toEpochDay(), today.toEpochDay())
        val weekExpense = transactionDao.getExpenseTotal(weekStart.toEpochDay(), today.toEpochDay())
        val weekIncome = transactionDao.getIncomeTotal(weekStart.toEpochDay(), today.toEpochDay())
        val monthExpense = transactionDao.getExpenseTotal(monthStart.toEpochDay(), monthEnd.toEpochDay())
        val monthIncome = transactionDao.getIncomeTotal(monthStart.toEpochDay(), monthEnd.toEpochDay())

        val recent = transactionDao.getFiltered(
            startDate = today.minusDays(30).toEpochDay(),
            endDate = today.toEpochDay(),
            type = null,
            search = null,
            bank = null,
        ).map { it.toDomain() }.take(20)

        val context = """
            Current context:
            - Today: income ₹${fmt(todayIncome)}, expense ₹${fmt(todayExpense)}
            - This week: income ₹${fmt(weekIncome)}, expense ₹${fmt(weekExpense)}
            - This month: income ₹${fmt(monthIncome)}, expense ₹${fmt(monthExpense)}

            Recent 20 transactions:
            ${recent.joinToString("\n") { "- id=${it.id} | ${it.date} | ${it.alias ?: it.title} | ${if (it.isIncome) "INCOME" else "EXPENSE"} ₹${fmt(it.amount)} | ${it.category.displayName}${it.note?.let { n -> " | note: $n" } ?: ""}" }}

            User message: $userMessage
        """.trimIndent()

        return context
    }

    private fun fmt(value: Double): String = String.format("%.2f", value)

    private suspend fun parseAndExecute(raw: String): AgentResponse {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val response = try {
            json.decodeFromString(AgentResponse.serializer(), cleaned)
        } catch (e: Exception) {
            return AgentResponse(raw)
        }

        val action = response.action ?: return response

        return when (action.name) {
            "setCategory" -> {
                val id = action.params["transactionId"]?.toLongOrNull()
                val categoryName = action.params["category"]
                val category = Category.entries.find { it.name.equals(categoryName, ignoreCase = true) || it.displayName.equals(categoryName, ignoreCase = true) }
                if (id != null && category != null) {
                    transactionDao.updateCategory(id, category.displayName)
                    AgentResponse("${response.reply}\n\nDone — transaction $id is now ${category.displayName}.")
                } else {
                    AgentResponse("${response.reply}\n\nI couldn't change the category. Need a valid id and category.")
                }
            }
            "setAlias" -> {
                val original = action.params["originalTitle"]
                val alias = action.params["alias"]
                if (!original.isNullOrBlank() && !alias.isNullOrBlank()) {
                    aliasDao.upsert(com.expensetracker.app.data.local.AliasEntity(original.trim(), alias.trim()))
                    transactionDao.applyAliasToAll(original.trim(), alias.trim())
                    AgentResponse("${response.reply}\n\nDone — renamed '$original' to '$alias' for all matching transactions.")
                } else {
                    AgentResponse("${response.reply}\n\nI couldn't set the alias. Need both original and alias names.")
                }
            }
            "setNote" -> {
                val id = action.params["transactionId"]?.toLongOrNull()
                val note = action.params["note"]
                if (id != null) {
                    transactionDao.setNote(id, note?.trim()?.ifBlank { null })
                    AgentResponse("${response.reply}\n\nDone — note updated for transaction $id.")
                } else {
                    AgentResponse("${response.reply}\n\nI couldn't update the note. Need a valid transaction id.")
                }
            }
            "excludeTransaction" -> {
                val id = action.params["transactionId"]?.toLongOrNull()
                if (id != null) {
                    transactionDao.setExcluded(id, true)
                    AgentResponse("${response.reply}\n\nDone — transaction $id is excluded from totals.")
                } else {
                    AgentResponse("${response.reply}\n\nI couldn't exclude the transaction. Need a valid id.")
                }
            }
            "addIgnoredSender" -> {
                val sender = action.params["sender"]
                if (!sender.isNullOrBlank()) {
                    ignoredSenderDao.upsert(com.expensetracker.app.data.local.IgnoredSenderEntity(sender.trim()))
                    transactionDao.setExcludedByTitle(sender.trim(), true)
                    AgentResponse("${response.reply}\n\nDone — '$sender' is now ignored and existing transactions are excluded.")
                } else {
                    AgentResponse("${response.reply}\n\nI couldn't ignore the sender. Need a valid sender name.")
                }
            }
            else -> response
        }
    }
}

@Serializable
data class AgentResponse(
    val reply: String,
    val action: AgentAction? = null,
)

@Serializable
data class AgentAction(
    val name: String,
    val params: Map<String, String> = emptyMap(),
)
