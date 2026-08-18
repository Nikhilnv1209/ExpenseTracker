package com.expensetracker.app.ai

import android.content.Context
import android.util.Log
import com.expensetracker.app.ExpenseTrackerApplication
import com.expensetracker.app.data.local.toDomain
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.notification.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class FinanceAgent(context: Context) {

    private val appContext = context.applicationContext
    private val settingsStore = AiSettingsStore(appContext)
    private val client = AiClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val transactionDao by lazy { (appContext as ExpenseTrackerApplication).transactionDao }
    private val aliasDao by lazy { (appContext as ExpenseTrackerApplication).aliasDao }
    private val ignoredSenderDao by lazy { (appContext as ExpenseTrackerApplication).ignoredSenderDao }
    private val reminderDao by lazy { (appContext as ExpenseTrackerApplication).reminderDao }

    suspend fun chat(
        userMessage: String,
        history: List<Pair<String, String>> = emptyList(),
        onContent: (String) -> Unit = {},
        onReasoning: (String) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        if (!settingsStore.isConfigured()) {
            return@withContext "AI is not enabled. Add your API key in Settings → AI Agent."
        }

        val chatStart = System.currentTimeMillis()
        Log.d("AiAgent", "chat() start — user message: \"${userMessage.take(80)}\" | history: ${history.size} msgs")

        val messages = mutableListOf(
            ChatMessageData(role = "system", content = buildSystemPrompt()),
        )

        history.forEach { (role, text) ->
            if (text.isNotBlank()) {
                messages.add(ChatMessageData(role = role, content = text))
            }
        }

        messages.add(ChatMessageData(role = "user", content = userMessage))

        val tools = buildToolDefinitions()
        val contentAccumulator = StringBuilder()

        repeat(5) { iteration ->
            val iterStart = System.currentTimeMillis()
            Log.d("AiAgent", "── Iteration ${iteration + 1}/5 — sending request to API…")

            val completion = client.complete(
                settings = settingsStore,
                messages = messages,
                tools = tools,
                onContentDelta = { delta ->
                    contentAccumulator.append(delta)
                    onContent(contentAccumulator.toString())
                },
                onReasoningDelta = { delta ->
                    onReasoning(delta)
                },
            )

            Log.d("AiAgent", "── Iteration ${iteration + 1} API call took ${System.currentTimeMillis() - iterStart}ms")

            if (completion.error != null) {
                Log.d("AiAgent", "chat() error: ${completion.error}")
                return@withContext "AI error: ${completion.error}"
            }

            val toolCalls = completion.toolCalls
            if (toolCalls.isNullOrEmpty()) {
                val content = completion.content
                val finalContent = if (content.isNullOrBlank()) {
                    "I thought about this but couldn't generate a proper response. Could you try rephrasing your question?"
                } else {
                    content
                }
                if (contentAccumulator.isEmpty()) onContent(finalContent)
                Log.d("AiAgent", "chat() done — no tool calls, returning final answer (${finalContent.length} chars) in ${System.currentTimeMillis() - chatStart}ms total")
                return@withContext finalContent
            }

            Log.d("AiAgent", "── Iteration ${iteration + 1} — AI requested ${toolCalls.size} tool call(s): ${toolCalls.joinToString { it.function.name }}")

            messages.add(ChatMessageData(
                role = "assistant",
                content = completion.content,
                toolCalls = toolCalls.map { tc ->
                    ToolCallData(
                        id = tc.id,
                        function = ToolCallFunctionData(
                            name = tc.function.name,
                            arguments = tc.function.arguments,
                        ),
                    )
                },
            ))

            for (tc in toolCalls) {
                val toolStart = System.currentTimeMillis()
                Log.d("AiAgent", "──── Executing tool: ${tc.function.name}(${tc.function.arguments.take(100)})")
                val result = executeTool(tc.function.name, tc.function.arguments)
                Log.d("AiAgent", "──── Tool ${tc.function.name} took ${System.currentTimeMillis() - toolStart}ms, result: ${result.take(120)}")
                messages.add(ChatMessageData(
                    role = "tool",
                    content = result,
                    toolCallId = tc.id,
                ))
            }

            contentAccumulator.clear()
        }

        Log.d("AiAgent", "chat() exhausted 5 iterations in ${System.currentTimeMillis() - chatStart}ms")
        "The AI took too many steps. Try simplifying your request."
    }

    private fun buildSystemPrompt(): String {
        return """
            You are a helpful finance assistant inside an Android expense tracker app.
            Today is ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}.

            Use the available tools to answer questions about the user's spending, modify transactions, or manage settings.
            Always call tools to get real data before answering — never guess or make up numbers.
            Be concise and friendly. Use markdown formatting for readability.
        """.trimIndent()
    }

    private fun buildToolDefinitions(): List<ToolFunctionDef> {
        val categoryNames = Category.entries.map { it.displayName }

        return listOf(
            ToolFunctionDef(
                name = "get_transactions",
                description = "Get a list of transactions. Optionally filter by period, type, category, or search text.",
                parameters = ToolParams(
                    properties = mapOf(
                        "period" to ToolProperty(
                            type = "string",
                            description = "Time period to filter by. Default: this_month",
                            enum = listOf("today", "this_week", "this_month", "last_month", "last_30_days", "all_time"),
                        ),
                        "type" to ToolProperty(
                            type = "string",
                            description = "Filter by income or expense. Omit for all.",
                            enum = listOf("INCOME", "EXPENSE"),
                        ),
                        "category" to ToolProperty(
                            type = "string",
                            description = "Filter by category name.",
                            enum = categoryNames,
                        ),
                        "search" to ToolProperty(
                            type = "string",
                            description = "Search text for title, alias, or bank name.",
                        ),
                        "limit" to ToolProperty(
                            type = "integer",
                            description = "Max number of transactions to return. Default: 20",
                        ),
                    ),
                ),
            ),
            ToolFunctionDef(
                name = "get_spending_summary",
                description = "Get income and expense totals for a given period.",
                parameters = ToolParams(
                    properties = mapOf(
                        "period" to ToolProperty(
                            type = "string",
                            description = "Time period. Default: this_month",
                            enum = listOf("today", "this_week", "this_month", "last_month", "all_time"),
                        ),
                    ),
                ),
            ),
            ToolFunctionDef(
                name = "get_category_breakdown",
                description = "Get expense breakdown by category for a given period.",
                parameters = ToolParams(
                    properties = mapOf(
                        "period" to ToolProperty(
                            type = "string",
                            description = "Time period. Default: this_month",
                            enum = listOf("this_month", "last_month", "all_time"),
                        ),
                    ),
                ),
            ),
            ToolFunctionDef(
                name = "set_category",
                description = "Change the category of a transaction.",
                parameters = ToolParams(
                    properties = mapOf(
                        "transactionId" to ToolProperty(type = "integer", description = "The ID of the transaction to update."),
                        "category" to ToolProperty(type = "string", description = "The new category.", enum = categoryNames),
                    ),
                    required = listOf("transactionId", "category"),
                ),
            ),
            ToolFunctionDef(
                name = "set_alias",
                description = "Rename a merchant/UPI title. Applies to all matching transactions.",
                parameters = ToolParams(
                    properties = mapOf(
                        "originalTitle" to ToolProperty(type = "string", description = "The original merchant/UPI name."),
                        "alias" to ToolProperty(type = "string", description = "The new display name."),
                    ),
                    required = listOf("originalTitle", "alias"),
                ),
            ),
            ToolFunctionDef(
                name = "set_note",
                description = "Add or update a personal note on a transaction.",
                parameters = ToolParams(
                    properties = mapOf(
                        "transactionId" to ToolProperty(type = "integer", description = "The ID of the transaction."),
                        "note" to ToolProperty(type = "string", description = "The note text."),
                    ),
                    required = listOf("transactionId", "note"),
                ),
            ),
            ToolFunctionDef(
                name = "exclude_transaction",
                description = "Exclude a transaction from balance and totals.",
                parameters = ToolParams(
                    properties = mapOf(
                        "transactionId" to ToolProperty(type = "integer", description = "The ID of the transaction to exclude."),
                    ),
                    required = listOf("transactionId"),
                ),
            ),
            ToolFunctionDef(
                name = "delete_transaction",
                description = "Permanently delete a transaction, including wrongly parsed ones. Works even for excluded transactions.",
                parameters = ToolParams(
                    properties = mapOf(
                        "transactionId" to ToolProperty(type = "integer", description = "The ID of the transaction to delete."),
                    ),
                    required = listOf("transactionId"),
                ),
            ),
            ToolFunctionDef(
                name = "add_ignored_sender",
                description = "Ignore future SMS imports from a specific sender and exclude their existing transactions.",
                parameters = ToolParams(
                    properties = mapOf(
                        "sender" to ToolProperty(type = "string", description = "The sender name to ignore."),
                    ),
                    required = listOf("sender"),
                ),
            ),
        )
    }

    private suspend fun executeTool(name: String, argumentsJson: String): String {
        val args = try {
            json.parseToJsonElement(argumentsJson).jsonObject
        } catch (e: Exception) {
            return """{"error": "Invalid arguments: ${e.message}"}"""
        }

        return when (name) {
            "get_transactions" -> executeGetTransactions(args)
            "get_spending_summary" -> executeGetSummary(args)
            "get_category_breakdown" -> executeGetCategoryBreakdown(args)
            "set_category" -> executeSetCategory(args)
            "set_alias" -> executeSetAlias(args)
            "set_note" -> executeSetNote(args)
            "exclude_transaction" -> executeExcludeTransaction(args)
            "delete_transaction" -> executeDeleteTransaction(args)
            "add_ignored_sender" -> executeAddIgnoredSender(args)
            else -> """{"error": "Unknown tool: $name"}"""
        }
    }

    private suspend fun executeGetTransactions(args: JsonObject): String {
        val period = args["period"]?.let { json.parseToJsonElement(it.toString()).toString().trim('"') } ?: "this_month"
        val (start, end) = resolvePeriod(period)
        val type = args["type"]?.toString()?.trim('"')
        val category = args["category"]?.toString()?.trim('"')
        val search = args["search"]?.toString()?.trim('"')?.ifBlank { null }
        val limit = args["limit"]?.toString()?.toIntOrNull() ?: 20

        val transactions = transactionDao.getFiltered(
            startDate = start.toEpochDay(),
            endDate = end.toEpochDay(),
            type = type,
            search = search,
            bank = null,
            category = category,
        ).map { it.toDomain() }.take(limit)

        if (transactions.isEmpty()) return """{"transactions": [], "message": "No transactions found."}"""

        val txList = transactions.joinToString(",") { tx ->
            val note = tx.note?.let { "\"${escape(it)}\"" } ?: "null"
            """{"id":${tx.id},"title":"${escape(tx.alias ?: tx.title)}","amount":${tx.amount},"type":"${if (tx.isIncome) "INCOME" else "EXPENSE"}","category":"${tx.category.displayName}","date":"${tx.date}","note":$note}"""
        }
        return """{"transactions":[$txList],"count":${transactions.size}}"""
    }

    private suspend fun executeGetSummary(args: JsonObject): String {
        val period = args["period"]?.toString()?.trim('"') ?: "this_month"
        val (start, end) = resolvePeriod(period)
        val income = transactionDao.getIncomeTotal(start.toEpochDay(), end.toEpochDay())
        val expense = transactionDao.getExpenseTotal(start.toEpochDay(), end.toEpochDay())
        return """{"period":"$period","income":$income,"expense":$expense,"net":${income - expense}}"""
    }

    private suspend fun executeGetCategoryBreakdown(args: JsonObject): String {
        val period = args["period"]?.toString()?.trim('"') ?: "this_month"
        val (start, end) = resolvePeriod(period)
        val totals = transactionDao.getCategoryTotals(start.toEpochDay(), end.toEpochDay())
        if (totals.isEmpty()) return """{"categories":[]}"""
        val cats = totals.joinToString(",") { """{"category":"${it.category}","total":${it.total}}""" }
        return """{"categories":[$cats]}"""
    }

    private suspend fun executeSetCategory(args: JsonObject): String {
        val id = args["transactionId"]?.toString()?.toLongOrNull()
        val categoryName = args["category"]?.toString()?.trim('"')
        val category = Category.entries.find {
            it.displayName.equals(categoryName, ignoreCase = true) || it.name.equals(categoryName, ignoreCase = true)
        }
        return if (id != null && category != null) {
            transactionDao.updateCategory(id, category.displayName)
            """{"success":true,"message":"Transaction $id set to ${category.displayName}"}"""
        } else {
            """{"error":"Invalid transactionId or category"}"""
        }
    }

    private suspend fun executeSetAlias(args: JsonObject): String {
        val original = args["originalTitle"]?.toString()?.trim('"')
        val alias = args["alias"]?.toString()?.trim('"')
        return if (!original.isNullOrBlank() && !alias.isNullOrBlank()) {
            aliasDao.upsert(com.expensetracker.app.data.local.AliasEntity(original, alias))
            transactionDao.applyAliasToAll(original, alias)
            """{"success":true,"message":"Renamed '$original' to '$alias'"}"""
        } else {
            """{"error":"Both originalTitle and alias are required"}"""
        }
    }

    private suspend fun executeSetNote(args: JsonObject): String {
        val id = args["transactionId"]?.toString()?.toLongOrNull()
        val note = args["note"]?.toString()?.trim('"')
        return if (id != null) {
            transactionDao.setNote(id, note?.ifBlank { null })
            """{"success":true,"message":"Note updated for transaction $id"}"""
        } else {
            """{"error":"Invalid transactionId"}"""
        }
    }

    private suspend fun executeExcludeTransaction(args: JsonObject): String {
        val id = args["transactionId"]?.toString()?.toLongOrNull()
        return if (id != null) {
            transactionDao.setExcluded(id, true)
            """{"success":true,"message":"Transaction $id excluded"}"""
        } else {
            """{"error":"Invalid transactionId"}"""
        }
    }

    private suspend fun executeDeleteTransaction(args: JsonObject): String {
        val id = args["transactionId"]?.toString()?.toLongOrNull()
        return if (id != null) {
            val existing = reminderDao.findByTransactionId(id)
            if (existing != null) {
                ReminderScheduler.cancel(appContext, existing.id)
                reminderDao.deleteByTransactionId(id)
            }
            transactionDao.deleteById(id)
            """{"success":true,"message":"Transaction $id deleted"}"""
        } else {
            """{"error":"Invalid transactionId"}"""
        }
    }

    private suspend fun executeAddIgnoredSender(args: JsonObject): String {
        val sender = args["sender"]?.toString()?.trim('"')
        return if (!sender.isNullOrBlank()) {
            ignoredSenderDao.upsert(com.expensetracker.app.data.local.IgnoredSenderEntity(sender))
            transactionDao.setExcludedByTitle(sender, true)
            """{"success":true,"message":"'$sender' is now ignored"}"""
        } else {
            """{"error":"Sender name is required"}"""
        }
    }

    private fun resolvePeriod(period: String): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (period) {
            "today" -> today to today
            "this_week" -> today.minusDays(today.dayOfWeek.value.toLong() - 1) to today
            "this_month" -> today.with(TemporalAdjusters.firstDayOfMonth()) to today.with(TemporalAdjusters.lastDayOfMonth())
            "last_month" -> today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()) to today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
            "last_30_days" -> today.minusDays(30) to today
            "all_time" -> LocalDate.of(2020, 1, 1) to today.plusYears(10)
            else -> today.with(TemporalAdjusters.firstDayOfMonth()) to today.with(TemporalAdjusters.lastDayOfMonth())
        }
    }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
}
