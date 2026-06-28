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
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS })
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun complete(
        settings: AiSettingsStore,
        messages: List<ChatMessageData>,
        tools: List<ToolFunctionDef>? = null,
        onContentDelta: (String) -> Unit = {},
        onReasoningDelta: (String) -> Unit = {},
    ): AiCompletion = withContext(Dispatchers.IO) {
        val baseUrl = settings.baseUrl.trim().removeSuffix("/")
        val apiKey = settings.apiKey.trim()
        if (apiKey.isBlank()) return@withContext AiCompletion(null, null, "API key is blank")

        val request = ChatRequest(
            model = settings.model,
            messages = messages,
            temperature = 0.2,
            tools = tools?.map { ToolWrapper("function", it) },
            stream = true,
        )

        val body = json.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d("AiAgent", "→ API request: POST $baseUrl/chat/completions  model=${settings.model}  stream=true  msgs=${messages.size}")
        try {
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    return@withContext AiCompletion(null, null, "HTTP ${response.code}: ${errorBody.take(300)}")
                }

                val reader = response.body?.byteStream()?.bufferedReader()
                    ?: return@withContext AiCompletion(null, null, "Empty response body")

                val contentBuilder = StringBuilder()
                val toolCallMap = mutableMapOf<Int, Pair<String, StringBuilder>>()
                val toolCallIds = mutableMapOf<Int, String>()
                val toolCallNames = mutableMapOf<Int, String>()
                var chunkCount = 0
                val startTime = System.currentTimeMillis()
                var firstChunkTime = 0L

                reader.use { r ->
                    while (true) {
                        val line = r.readLine() ?: break
                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break

                        chunkCount++
                        if (firstChunkTime == 0L) {
                            firstChunkTime = System.currentTimeMillis()
                            Log.d("AiClient", "First chunk at ${firstChunkTime - startTime}ms")
                        }

                        try {
                            val chunk = json.decodeFromString(StreamChunk.serializer(), data)
                            val delta = chunk.choices.firstOrNull()?.delta ?: continue

                            delta.content?.let {
                                contentBuilder.append(it)
                                onContentDelta(it)
                            }

                            delta.reasoningContent?.let {
                                onReasoningDelta(it)
                            }

                            delta.toolCalls?.forEach { tc ->
                                val idx = tc.index ?: 0
                                tc.id?.let { toolCallIds[idx] = it }
                                tc.function?.name?.let { toolCallNames[idx] = it }
                                tc.function?.arguments?.let { arg ->
                                    val existing = toolCallMap[idx]
                                    if (existing == null) {
                                        toolCallMap[idx] = (toolCallNames[idx] ?: "") to StringBuilder(arg)
                                    } else {
                                        existing.second.append(arg)
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }

                Log.d("AiClient", "Stream done: $chunkCount chunks in ${System.currentTimeMillis() - startTime}ms, content=${contentBuilder.length} chars")

                val toolCalls = if (toolCallMap.isNotEmpty()) {
                    toolCallMap.toSortedMap().map { (idx, pair) ->
                        ToolCallResponse(
                            id = toolCallIds[idx] ?: "call_$idx",
                            function = ToolCallFunction(
                                name = pair.first,
                                arguments = pair.second.toString(),
                            ),
                        )
                    }
                } else null

                AiCompletion(
                    content = contentBuilder.toString().ifBlank { null },
                    toolCalls = toolCalls,
                    error = null,
                )
            }
        } catch (e: IOException) {
            AiCompletion(null, null, e.localizedMessage ?: e.message ?: "Network error")
        }
    }
}

data class AiCompletion(
    val content: String?,
    val toolCalls: List<ToolCallResponse>?,
    val error: String?,
)

@Serializable
data class ChatMessageData(
    val role: String,
    val content: String? = null,
    val toolCalls: List<ToolCallData>? = null,
    val toolCallId: String? = null,
)

@Serializable
data class ToolCallData(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunctionData,
)

@Serializable
data class ToolCallFunctionData(
    val name: String,
    val arguments: String,
)

@Serializable
data class ToolFunctionDef(
    val name: String,
    val description: String,
    val parameters: ToolParams,
)

@Serializable
data class ToolParams(
    val type: String = "object",
    val properties: Map<String, ToolProperty> = emptyMap(),
    val required: List<String> = emptyList(),
)

@Serializable
data class ToolProperty(
    val type: String? = null,
    val description: String? = null,
    val enum: List<String>? = null,
)

@Serializable
data class ToolWrapper(
    val type: String = "function",
    val function: ToolFunctionDef,
)

@Serializable
data class ToolCallResponse(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction,
)

@Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String,
)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageData>,
    val temperature: Double = 0.2,
    val tools: List<ToolWrapper>? = null,
    val stream: Boolean = false,
)

@Serializable
private data class StreamChunk(val choices: List<StreamChoice>) {
    @Serializable
    data class StreamChoice(val delta: StreamDelta, val finishReason: String? = null) {
        @Serializable
        data class StreamDelta(
            val content: String? = null,
            @kotlinx.serialization.SerialName("reasoning_content") val reasoningContent: String? = null,
            @kotlinx.serialization.SerialName("tool_calls") val toolCalls: List<StreamToolCall>? = null,
        ) {
            @Serializable
            data class StreamToolCall(
                val index: Int? = null,
                val id: String? = null,
                val type: String? = null,
                val function: StreamToolFunction? = null,
            ) {
                @Serializable
                data class StreamToolFunction(
                    val name: String? = null,
                    val arguments: String? = null,
                )
            }
        }
    }
}
