package com.expensetracker.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun complete(
        settings: AiSettingsStore,
        systemPrompt: String,
        userPrompt: String,
    ): String? = withContext(Dispatchers.IO) {
        val baseUrl = settings.baseUrl.trim().removeSuffix("/")
        val apiKey = settings.apiKey.trim()
        if (apiKey.isBlank()) return@withContext null

        val requestBody = ChatCompletionRequest(
            model = settings.model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt),
            ),
            responseFormat = ResponseFormat(type = "json_object"),
        )

        val body = json.encodeToString(requestBody).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("AI request failed: ${response.code} ${response.message}")
            }
            val responseBody = response.body?.string() ?: return@use null
            val completion = json.decodeFromString(ChatCompletionResponse.serializer(), responseBody)
            completion.choices.firstOrNull()?.message?.content
        }
    }

    @Serializable
    private data class ChatCompletionRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.2,
        val responseFormat: ResponseFormat? = null,
    )

    @Serializable
    private data class ResponseFormat(val type: String)

    @Serializable
    private data class ChatCompletionResponse(val choices: List<Choice>)

    @Serializable
    private data class Choice(val message: ChatMessage)
}

@Serializable
data class ChatMessage(val role: String, val content: String)
