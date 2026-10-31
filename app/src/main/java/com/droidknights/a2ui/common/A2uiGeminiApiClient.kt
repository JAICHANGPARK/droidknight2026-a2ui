package com.droidknights.a2ui.common

import android.content.Context
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.BuildConfig
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class GeminiA2uiResponse(
    val agentReplyText: String,
    val jsonlLines: List<String>,
    val modelUsed: String,
)

/**
 * Standard Gemini REST API (`generateContent`) client that injects the official
 * `A2uiCatalog.toJsonSchemaString()` schema into `systemInstruction` and returns
 * validated A2UI v0.9.1 JSONL messages (`createSurface`, `updateDataModel`, `updateComponents`).
 */
object A2uiGeminiApiClient {
    const val DEFAULT_LM_STUDIO_MODEL = "google/gemma-4-e2b"
    private const val PREFS_NAME = "droidknights_a2ui_gemini_prefs"
    private const val KEY_API_KEY = "gemini_api_key_override"
    private const val KEY_MODEL = "gemini_or_lmstudio_model_v2"
    private const val KEY_EXPLICIT_DISABLED = "gemini_api_explicitly_disabled"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val lmStudioMutex = Mutex()

    fun isLmStudioModel(model: String): Boolean {
        val m = model.trim().lowercase()
        return m.contains("/") ||
            m.startsWith("gemma") ||
            m.startsWith("lmstudio") ||
            m.startsWith("local") ||
            m.startsWith("qwen") ||
            m.startsWith("llama")
    }

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val overrideKey = prefs.getString(KEY_API_KEY, null)?.trim().orEmpty()
        return if (overrideKey.isNotEmpty()) {
            overrideKey
        } else {
            BuildConfig.GEMINI_API_KEY.trim()
        }
    }

    fun saveApiKey(context: Context, apiKey: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    fun getModelName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val overrideModel = prefs.getString(KEY_MODEL, null)?.trim().orEmpty()
        return if (overrideModel.isNotEmpty()) {
            overrideModel
        } else {
            DEFAULT_LM_STUDIO_MODEL
        }
    }

    fun saveModelName(context: Context, modelName: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL, modelName.trim().ifEmpty { DEFAULT_LM_STUDIO_MODEL })
            .apply()
    }

    fun isGeminiApiEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val explicitlyDisabled = prefs.getBoolean(KEY_EXPLICIT_DISABLED, false)
        return hasConfiguredApiKey(context) && !explicitlyDisabled
    }

    fun setGeminiApiEnabled(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EXPLICIT_DISABLED, !enabled)
            .apply()
    }

    fun hasConfiguredApiKey(context: Context): Boolean =
        isLmStudioModel(getModelName(context)) || getApiKey(context).isNotBlank()

    /**
     * Real-time SSE (`stream: true` / `alt=sse`) streaming version of [generateCurrencyExchangeSurface].
     * Streams `REPLY:` token-by-token via [onReplyDelta] and emits each validated A2UI v0.9.1 JSONL line
     * (`createSurface`, `updateComponents`, `updateDataModel`) via [onJsonlLine] the instant that line finishes streaming.
     */
    suspend fun streamCurrencyExchangeSurface(
        context: Context,
        catalog: A2uiCatalog,
        userPrompt: String,
        replyOnly: Boolean = false,
        onReplyDelta: suspend (String) -> Unit,
        onJsonlLine: suspend (String) -> Unit,
    ): Result<GeminiA2uiResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val model = getModelName(context)
                val emittedJsonl = mutableListOf<String>()
                val parser = A2uiMessageParser()
                var lastReplyEmitted = ""

                suspend fun processIncrementalBuffer(buffer: String, isFinal: Boolean): Boolean {
                    val sanitized = buffer.replace(Regex("""<think>[\s\S]*?</think>"""), "")
                    if (sanitized.contains("<think>") && !sanitized.contains("</think>")) {
                        return false
                    }
                    val lines = sanitized.split("\n")
                    val completeLines = if (isFinal) lines else lines.dropLast(1)
                    val activeLine = if (isFinal) null else lines.lastOrNull()

                    for (rawLine in completeLines) {
                        val trimmed = rawLine.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("```")) continue
                        if (trimmed.startsWith("REPLY:", ignoreCase = true)) {
                            val replyText = trimmed.removePrefix("REPLY:").removePrefix("reply:").trim()
                            if (replyText.isNotEmpty() && replyText != lastReplyEmitted) {
                                lastReplyEmitted = replyText
                                withContext(Dispatchers.Main) { onReplyDelta(replyText) }
                            }
                            if (replyOnly) {
                                return true
                            }
                        } else if (!replyOnly && trimmed.startsWith("{") && trimmed.endsWith("}")) {
                            runCatching {
                                val parsedElement = json.parseToJsonElement(trimmed).jsonObject
                                if (
                                    parsedElement.containsKey("createSurface") ||
                                        parsedElement.containsKey("updateDataModel") ||
                                        parsedElement.containsKey("updateComponents")
                                ) {
                                    val compactLine = parsedElement.toString()
                                    if (compactLine !in emittedJsonl) {
                                        parser.parse(compactLine)
                                        emittedJsonl.add(compactLine)
                                        withContext(Dispatchers.Main) { onJsonlLine(compactLine) }
                                    }
                                }
                            }
                        } else if (lastReplyEmitted.isEmpty() && !trimmed.startsWith("{")) {
                            lastReplyEmitted = trimmed
                            withContext(Dispatchers.Main) { onReplyDelta(trimmed) }
                            if (replyOnly) {
                                return true
                            }
                        }
                    }

                    if (activeLine != null) {
                        val trimmedActive = activeLine.trim()
                        if (trimmedActive.startsWith("REPLY:", ignoreCase = true)) {
                            val partialReply =
                                trimmedActive.removePrefix("REPLY:").removePrefix("reply:").trim()
                            if (partialReply.isNotEmpty() && partialReply != lastReplyEmitted) {
                                lastReplyEmitted = partialReply
                                withContext(Dispatchers.Main) { onReplyDelta(partialReply) }
                            }
                        } else if (
                            lastReplyEmitted.isEmpty() &&
                                completeLines.all { it.isBlank() || it.trim().startsWith("```") } &&
                                trimmedActive.isNotEmpty() &&
                                !trimmedActive.startsWith("{") &&
                                !trimmedActive.startsWith("```") &&
                                !trimmedActive.startsWith("<")
                        ) {
                            lastReplyEmitted = trimmedActive
                            withContext(Dispatchers.Main) { onReplyDelta(trimmedActive) }
                        } else if (
                            lastReplyEmitted.isNotEmpty() &&
                                emittedJsonl.isEmpty() &&
                                trimmedActive.length > 16 &&
                                !trimmedActive.startsWith("{") &&
                                !trimmedActive.startsWith("```")
                        ) {
                            // Model already completed REPLY: on line 1 and is generating non-JSON prose on line 2;
                            // stop early so the caller doesn't wait for unnecessary tokens.
                            return true
                        }
                    }
                    return !replyOnly && emittedJsonl.size >= 3
                }

                val rawText =
                    if (isLmStudioModel(model)) {
                        lmStudioMutex.withLock {
                            executeLmStudioStreamingRequest(
                                model = model,
                                systemPrompt =
                                    if (replyOnly) buildLmStudioReplyOnlySystemPrompt()
                                    else buildLmStudioCompactSystemPrompt(),
                                userPrompt = userPrompt,
                                maxTokens = if (replyOnly) 140 else 850,
                                onBufferUpdated = { buf, isFinal ->
                                    processIncrementalBuffer(buf, isFinal)
                                },
                            )
                        }
                    } else {
                        val apiKey = getApiKey(context)
                        require(apiKey.isNotBlank()) { "Gemini API 키가 설정되지 않았습니다." }
                        val catalogSchema = catalog.toJsonSchemaString()
                        val systemPrompt = buildCurrencyExchangeSystemPrompt(catalogSchema)
                        val requestBodyJson =
                            buildJsonObject {
                                put(
                                    "systemInstruction",
                                    buildJsonObject {
                                        put(
                                            "parts",
                                            buildJsonArray {
                                                add(buildJsonObject { put("text", systemPrompt) })
                                            },
                                        )
                                    },
                                )
                                put(
                                    "contents",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("role", "user")
                                                put(
                                                    "parts",
                                                    buildJsonArray {
                                                        add(
                                                            buildJsonObject {
                                                                put("text", userPrompt)
                                                            }
                                                        )
                                                    },
                                                )
                                            }
                                        )
                                    },
                                )
                                put(
                                    "generationConfig",
                                    buildJsonObject {
                                        put("temperature", 0.2)
                                    },
                                )
                            }.toString()

                        executeGeminiStreamingRequest(
                            apiKey = apiKey,
                            model = model,
                            requestBody = requestBodyJson,
                            onBufferUpdated = { buf, isFinal ->
                                processIncrementalBuffer(buf, isFinal)
                            },
                        )
                    }

                val modelLabel = if (isLmStudioModel(model)) "LM Studio ($model)" else model
                val finalParsed = parseAndValidateA2uiResponse(rawText = rawText, model = modelLabel)

                if (finalParsed.agentReplyText != lastReplyEmitted) {
                    withContext(Dispatchers.Main) { onReplyDelta(finalParsed.agentReplyText) }
                }
                if (!replyOnly) {
                    for (line in finalParsed.jsonlLines) {
                        if (line !in emittedJsonl) {
                            emittedJsonl.add(line)
                            withContext(Dispatchers.Main) { onJsonlLine(line) }
                        }
                    }
                }
                finalParsed
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }

    /**
     * Calls either the local LM Studio OpenAI-compatible server (`http://127.0.0.1:1234/v1/chat/completions` /
     * `http://10.0.2.2:1234/v1/chat/completions`) or the standard Gemini API (`models/{model}:generateContent`).
     */
    suspend fun generateCurrencyExchangeSurface(
        context: Context,
        catalog: A2uiCatalog,
        userPrompt: String,
    ): Result<GeminiA2uiResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val model = getModelName(context)
                if (isLmStudioModel(model)) {
                    val rawText =
                        executeLmStudioChatCompletionRequest(
                            model = model,
                            systemPrompt = buildLmStudioCompactSystemPrompt(),
                            userPrompt = userPrompt,
                        )
                    parseAndValidateA2uiResponse(rawText = rawText, model = "LM Studio ($model)")
                } else {
                    val apiKey = getApiKey(context)
                    require(apiKey.isNotBlank()) { "Gemini API 키가 설정되지 않았습니다." }
                    val catalogSchema = catalog.toJsonSchemaString()
                    val systemPrompt = buildCurrencyExchangeSystemPrompt(catalogSchema)
                    val requestBodyJson =
                        buildJsonObject {
                            put(
                                "systemInstruction",
                                buildJsonObject {
                                    put(
                                        "parts",
                                        buildJsonArray {
                                            add(buildJsonObject { put("text", systemPrompt) })
                                        },
                                    )
                                },
                            )
                            put(
                                "contents",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("role", "user")
                                            put(
                                                "parts",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("text", userPrompt)
                                                        }
                                                    )
                                                },
                                            )
                                        }
                                    )
                                },
                            )
                            put(
                                "generationConfig",
                                buildJsonObject {
                                    put("temperature", 0.2)
                                },
                            )
                        }.toString()

                    val rawText = executeGenerateContentRequest(apiKey = apiKey, model = model, requestBody = requestBodyJson)
                    parseAndValidateA2uiResponse(rawText = rawText, model = model)
                }
            }
        }

    /**
     * Calls LM Studio or Gemini API when the user triggers a button action (`submit_exchange`, etc.)
     * to generate the follow-up surface update based on the dispatched `A2uiClientEventMessage`.
     */
    suspend fun generateFollowUpExchangeSurface(
        context: Context,
        catalog: A2uiCatalog,
        event: A2uiClientEventMessage,
    ): Result<GeminiA2uiResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val model = getModelName(context)
                val catalogSchema = catalog.toJsonSchemaString()
                val systemPrompt = buildFollowUpSystemPrompt(catalogSchema)
                val eventSummaryPrompt =
                    "클라이언트 UI 액션 수신: eventName=${event.type}, surfaceId=${event.surfaceId}, context=${event.context}. " +
                        "이에 맞는 모바일 QR 바우처 발급 완료 A2UI JSONL을 생성해 주세요."

                if (isLmStudioModel(model)) {
                    val rawText =
                        executeLmStudioChatCompletionRequest(
                            model = model,
                            systemPrompt = systemPrompt,
                            userPrompt = eventSummaryPrompt,
                        )
                    parseAndValidateA2uiResponse(rawText = rawText, model = "LM Studio ($model)")
                } else {
                    val apiKey = getApiKey(context)
                    require(apiKey.isNotBlank()) { "Gemini API 키가 설정되지 않았습니다." }
                    val requestBodyJson =
                        buildJsonObject {
                            put(
                                "systemInstruction",
                                buildJsonObject {
                                    put(
                                        "parts",
                                        buildJsonArray {
                                            add(buildJsonObject { put("text", systemPrompt) })
                                        },
                                    )
                                },
                            )
                            put(
                                "contents",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("role", "user")
                                            put(
                                                "parts",
                                                buildJsonArray {
                                                    add(buildJsonObject { put("text", eventSummaryPrompt) })
                                                },
                                            )
                                        }
                                    )
                                },
                            )
                            put(
                                "generationConfig",
                                buildJsonObject {
                                    put("temperature", 0.2)
                                },
                            )
                        }.toString()

                    val rawText = executeGenerateContentRequest(apiKey = apiKey, model = model, requestBody = requestBodyJson)
                    parseAndValidateA2uiResponse(rawText = rawText, model = model)
                }
            }
        }

    private suspend fun executeLmStudioStreamingRequest(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 850,
        onBufferUpdated: suspend (String, Boolean) -> Boolean,
    ): String {
        val requestBody =
            buildJsonObject {
                put("model", model)
                put("stream", true)
                put("reasoning_effort", "none")
                put("temperature", 0.2)
                put("max_tokens", maxTokens)
                put(
                    "messages",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            }
                        )
                        add(
                            buildJsonObject {
                                put("role", "user")
                                put("content", userPrompt)
                            }
                        )
                    },
                )
            }.toString()

        val candidateEndpoints =
            listOf(
                "http://127.0.0.1:1234/v1/chat/completions",
                "http://10.0.2.2:1234/v1/chat/completions",
            )

        var lastError: Throwable? = null
        for (endpoint in candidateEndpoints) {
            var requestSent = false
            try {
                coroutineContext.ensureActive()
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 2_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "text/event-stream")
                }
                val cancelHandle =
                    coroutineContext[Job]?.invokeOnCompletion { cause ->
                        if (cause != null) {
                            runCatching { connection.disconnect() }
                        }
                    }
                try {
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(requestBody)
                        writer.flush()
                    }
                    requestSent = true
                    val status = connection.responseCode
                    if (status !in 200..299) {
                        val errText =
                            connection.errorStream?.let {
                                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
                            }.orEmpty()
                        throw IllegalStateException("LM Studio HTTP $status: ${errText.take(200)}")
                    }

                    val accumulated = StringBuilder()
                    BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                        var sseLine: String? = reader.readLine()
                        while (sseLine != null) {
                            coroutineContext.ensureActive()
                            val trimmed = sseLine.trim()
                            if (trimmed.startsWith("data:")) {
                                val dataPayload = trimmed.removePrefix("data:").trim()
                                if (dataPayload == "[DONE]") {
                                    break
                                }
                                if (dataPayload.startsWith("{")) {
                                    val deltaText =
                                        runCatching {
                                            val chunkObj = json.parseToJsonElement(dataPayload).jsonObject
                                            val firstChoice =
                                                chunkObj["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                                            val deltaObj = firstChoice?.get("delta")?.jsonObject
                                            deltaObj?.get("content")?.jsonPrimitive?.content.orEmpty()
                                        }.getOrDefault("")
                                    if (deltaText.isNotEmpty()) {
                                        accumulated.append(deltaText)
                                        val shouldStopEarly = onBufferUpdated(accumulated.toString(), false)
                                        if (shouldStopEarly) {
                                            break
                                        }
                                    }
                                }
                            }
                            sseLine = reader.readLine()
                        }
                    }

                    val finalContent =
                        accumulated
                            .toString()
                            .replace(Regex("""<think>[\s\S]*?</think>"""), "")
                            .trim()
                    if (finalContent.isNotEmpty()) {
                        onBufferUpdated(finalContent, true)
                        return finalContent
                    }
                    throw IllegalStateException("LM Studio SSE 스트림 content가 비어 있습니다.")
                } finally {
                    cancelHandle?.dispose()
                    runCatching { connection.disconnect() }
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                lastError = e
                // If the request was already sent to this endpoint and failed during read,
                // do NOT fire a duplicate request to the second endpoint.
                if (requestSent) break
            }
        }
        throw (lastError ?: IllegalStateException("LM Studio 로컬 서버(포트 1234) 연결 실패"))
    }

    private suspend fun executeGeminiStreamingRequest(
        apiKey: String,
        model: String,
        requestBody: String,
        onBufferUpdated: suspend (String, Boolean) -> Boolean,
    ): String {
        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "text/event-stream")
        }
        val cancelHandle =
            coroutineContext[Job]?.invokeOnCompletion { cause ->
                if (cause != null) {
                    runCatching { connection.disconnect() }
                }
            }

        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                val errText =
                    connection.errorStream?.let {
                        BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
                    }.orEmpty()
                throw IllegalStateException("Gemini SSE HTTP $status: ${errText.take(200)}")
            }

            val accumulated = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                var sseLine: String? = reader.readLine()
                while (sseLine != null) {
                    coroutineContext.ensureActive()
                    val trimmed = sseLine.trim()
                    if (trimmed.startsWith("data:")) {
                        val dataPayload = trimmed.removePrefix("data:").trim()
                        if (dataPayload.startsWith("{")) {
                            val deltaText =
                                runCatching {
                                    val rootObj = json.parseToJsonElement(dataPayload).jsonObject
                                    val firstCandidate =
                                        rootObj["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                                    val parts =
                                        firstCandidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
                                    parts?.joinToString("") { part ->
                                        part.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
                                    }.orEmpty()
                                }.getOrDefault("")
                            if (deltaText.isNotEmpty()) {
                                accumulated.append(deltaText)
                                onBufferUpdated(accumulated.toString(), false)
                            }
                        }
                    }
                    sseLine = reader.readLine()
                }
            }

            val finalContent = accumulated.toString().trim()
            if (finalContent.isNotEmpty()) {
                onBufferUpdated(finalContent, true)
                return finalContent
            }
            throw IllegalStateException("Gemini SSE 스트림 응답이 비어 있습니다.")
        } finally {
            cancelHandle?.dispose()
            runCatching { connection.disconnect() }
        }
    }

    private fun executeLmStudioChatCompletionRequest(
        model: String,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        val requestBody =
            buildJsonObject {
                put("model", model)
                put("reasoning_effort", "none")
                put("temperature", 0.2)
                put("max_tokens", 1200)
                put(
                    "messages",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            }
                        )
                        add(
                            buildJsonObject {
                                put("role", "user")
                                put("content", userPrompt)
                            }
                        )
                    },
                )
            }.toString()

        val candidateEndpoints =
            listOf(
                "http://127.0.0.1:1234/v1/chat/completions",
                "http://10.0.2.2:1234/v1/chat/completions",
            )

        var lastError: Throwable? = null
        for (endpoint in candidateEndpoints) {
            try {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 4_000
                    readTimeout = 25_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                try {
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(requestBody)
                        writer.flush()
                    }
                    val status = connection.responseCode
                    val stream =
                        if (status in 200..299) connection.inputStream
                        else connection.errorStream
                    val bodyText =
                        stream?.let {
                            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                                reader.readText()
                            }
                        }.orEmpty()

                    if (status !in 200..299) {
                        throw IllegalStateException("LM Studio HTTP $status: ${bodyText.take(200)}")
                    }

                    val rootObj = json.parseToJsonElement(bodyText).jsonObject
                    val choices = rootObj["choices"]?.jsonArray
                    val firstChoice =
                        choices?.firstOrNull()?.jsonObject
                            ?: throw IllegalStateException("LM Studio 응답에 choices가 없습니다.")
                    val messageObj = firstChoice["message"]?.jsonObject
                    val content =
                        messageObj?.get("content")?.jsonPrimitive?.content.orEmpty()
                            .replace(Regex("""<think>[\s\S]*?</think>"""), "")
                            .trim()
                    if (content.isNotEmpty()) {
                        return content
                    }
                    throw IllegalStateException("LM Studio 응답 content가 비어 있습니다.")
                } finally {
                    connection.disconnect()
                }
            } catch (e: Throwable) {
                lastError = e
            }
        }
        throw (lastError ?: IllegalStateException("LM Studio 로컬 서버(포트 1234) 연결 실패"))
    }

    /**
     * Queries `GET /v1/models` on the local LM Studio server (`127.0.0.1:1234` or `10.0.2.2:1234`)
     * to discover downloaded LLM models in real time for the Settings Dialog.
     */
    suspend fun fetchLmStudioModels(): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoints =
                    listOf(
                        "http://127.0.0.1:1234/v1/models",
                        "http://10.0.2.2:1234/v1/models",
                    )
                var lastErr: Throwable? = null
                for (url in endpoints) {
                    try {
                        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 2_500
                            readTimeout = 4_000
                        }
                        try {
                            if (conn.responseCode in 200..299) {
                                val text =
                                    BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                                        it.readText()
                                    }
                                val dataArr = json.parseToJsonElement(text).jsonObject["data"]?.jsonArray
                                val ids =
                                    dataArr
                                        ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                                        ?.filterNot { it.contains("embed", ignoreCase = true) }
                                        .orEmpty()
                                if (ids.isNotEmpty()) return@runCatching ids
                            }
                        } finally {
                            conn.disconnect()
                        }
                    } catch (e: Throwable) {
                        lastErr = e
                    }
                }
                throw (lastErr ?: IllegalStateException("LM Studio 로컬 서버 미실행"))
            }
        }

    private fun buildLmStudioReplyOnlySystemPrompt(): String =
        """
        당신은 DK 뱅크 모바일 금융 슈퍼앱의 AI 금융비서입니다.
        고객 이름: 박제창님 (DK 주거래 통장 보유, 환전·송금 수수료 전액 면제).
        기준환율(VVIP 우대): 일본 엔(JPY 100) 847.88원(90% 우대), 미국 달러(USD 1) 1,335.80원(90% 우대), 유럽 유로(EUR 1) 1,490.75원(80% 우대), 스위스 프랑(CHF 1) 1,619.05원(80% 우대).
        사용자의 금융 요청(간편 송금·이체, 카드 명세서 즉시결제, 해외출국 패키지, 재환전, 목표환율 자동환전 등)이나 질문에 대해 JSON 코드 없이 반드시 `REPLY: <친절하고 간결한 한국어 안내 1~2문장>` 한 줄만 즉시 출력하세요.
        """.trimIndent()

    private fun buildLmStudioCompactSystemPrompt(): String =
        """
        당신은 DK 뱅크 모바일 금융 슈퍼앱의 AI 금융비서입니다.
        고객 이름: 박제창님 (DK 주거래 통장 보유, 환전·송금 수수료 전액 면제).
        기준환율(VVIP 우대): 일본 엔(JPY 100) 847.88원(90% 우대), 미국 달러(USD 1) 1,335.80원(90% 우대), 유럽 유로(EUR 1) 1,490.75원(80% 우대), 스위스 프랑(CHF 1) 1,619.05원(80% 우대).

        사용자의 입력 의도에 따라 아래 두 가지 중 하나로 응답하세요:
        - [A. 단순 정보 질문인 경우] (예: 인사, 오늘 환율 질문, 수수료 질문, 공항 수령 장소 질문):
          JSONL 없이 `REPLY: <친절한 한국어 답변 1~3문장>`만 출력하세요.
        - [B. 환전·송금·결제 등 실행 요청인 경우]:
          1) 첫 줄에 `REPLY: <한국어 안내 문장>`을 출력하고,
          2) 이어서 마크다운 코드블록(```) 없이 아래 A2UI v0.9.1 JSONL 규격의 단일 라인 JSON 객체 3줄(`createSurface`, `updateDataModel`, `updateComponents`)을 직접 생성하여 출력하세요. 사용자가 요청한 외화 금액(예: 6000엔 → jpyAmount: 6000, krwAmount: 50873 / 50000엔 → jpyAmount: 50000, krwAmount: 423940)에 맞춰 숫자와 제목을 변경하세요.

        [A2UI v0.9.1 JSONL 출력 예시 (정확히 4줄 형식: createSurface → updateComponents → updateDataModel 순서)]
        REPLY: 박제창님, 요청하신 인천공항 수령 엔화 환전 신청서를 준비했습니다. 금액을 확인하고 신청 버튼을 눌러주세요.
        {"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_card"]},{"id":"exchange_card","component":"Card","child":"card_col"},{"id":"card_col","component":"Column","children":["title","exchange_converter","branch_picker","coupon_chk","btn_submit"]},{"id":"title","component":"Text","text":"💴 50,000 JPY 환전 (90% 우대)"},{"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액","exchangeRate":847.88,"unitScale":100,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"}},{"id":"branch_picker","component":"ChoicePicker","label":"수령처","variant":"mutuallyExclusive","options":[{"label":"인천공항 1터미널","value":"ICN_T1"},{"label":"인천공항 2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},{"id":"coupon_chk","component":"CheckBox","label":"주거래 통장 출금 · 90% 우대","value":{"path":"/exchange/coupon90"}},{"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"jpy_airport_oneshot","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"}}}}},{"id":"btn_txt","component":"Text","text":"환전 신청하기"}]}}
        {"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":50000,"krwAmount":423940,"branch":["ICN_T1"],"coupon90":true}}}
        """.trimIndent()

    private fun executeGenerateContentRequest(
        apiKey: String,
        model: String,
        requestBody: String,
    ): String {
        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 18_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val status = connection.responseCode
            val stream =
                if (status in 200..299) connection.inputStream
                else connection.errorStream
            val bodyText =
                stream?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                }.orEmpty()

            if (status !in 200..299) {
                throw IllegalStateException("Gemini API HTTP $status: ${bodyText.take(200)}")
            }

            val rootObj = json.parseToJsonElement(bodyText).jsonObject
            val candidates = rootObj["candidates"]?.jsonArray
            val firstCandidate =
                candidates?.firstOrNull()?.jsonObject
                    ?: throw IllegalStateException("Gemini API 응답에 candidates가 없습니다.")
            val parts =
                firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray
                    ?: throw IllegalStateException("Gemini API 응답에 content.parts가 없습니다.")
            parts.joinToString("\n") { part ->
                part.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseAndValidateA2uiResponse(
        rawText: String,
        model: String,
    ): GeminiA2uiResponse {
        val cleanedLines =
            rawText
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("```") }

        var agentReply = ""
        val extractedJsonl = mutableListOf<String>()
        val parser = A2uiMessageParser()

        for (line in cleanedLines) {
            if (line.startsWith("REPLY:", ignoreCase = true)) {
                agentReply = line.removePrefix("REPLY:").removePrefix("reply:").trim()
            } else if (line.startsWith("{") && line.endsWith("}")) {
                // Validate that it is a valid A2UI v0.9.1 message
                val parsedElement = json.parseToJsonElement(line).jsonObject
                if (
                    parsedElement.containsKey("createSurface") ||
                        parsedElement.containsKey("updateDataModel") ||
                        parsedElement.containsKey("updateComponents")
                ) {
                    val compactLine = parsedElement.toString()
                    parser.parse(compactLine)
                    extractedJsonl.add(compactLine)
                }
            } else if (agentReply.isEmpty() && !line.startsWith("{")) {
                agentReply = line
            }
        }

        // If Gemini formatted JSON across multiple lines, fall back to balanced brace extraction
        if (extractedJsonl.isEmpty()) {
            extractBalancedJsonObjects(rawText).forEach { candidateJson ->
                runCatching {
                    val parsedElement: JsonObject = json.parseToJsonElement(candidateJson).jsonObject
                    if (
                        parsedElement.containsKey("createSurface") ||
                            parsedElement.containsKey("updateDataModel") ||
                            parsedElement.containsKey("updateComponents")
                    ) {
                        val compactLine = parsedElement.toString()
                        parser.parse(compactLine)
                        extractedJsonl.add(compactLine)
                    }
                }
            }
        }

        val nonJsonLines =
            cleanedLines.filterNot { it.startsWith("{") && it.endsWith("}") }
        if (extractedJsonl.isEmpty() && agentReply.isBlank() && nonJsonLines.isNotEmpty()) {
            agentReply =
                nonJsonLines.joinToString("\n") {
                    it.removePrefix("REPLY:").removePrefix("reply:").trim()
                }
        }

        require(extractedJsonl.isNotEmpty() || agentReply.isNotBlank()) {
            "Gemini 응답에서 유효한 텍스트 또는 A2UI v0.9.1 JSONL 메시지를 찾지 못했습니다."
        }

        val finalReply =
            agentReply.ifBlank {
                "Gemini ($model)가 요청하신 외화 환전 조건에 맞춰 실시간 A2UI 서피스를 생성했습니다."
            }

        return GeminiA2uiResponse(
            agentReplyText = finalReply,
            jsonlLines = extractedJsonl,
            modelUsed = model,
        )
    }

    private fun extractBalancedJsonObjects(text: String): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        var startIdx = -1
        var inQuotes = false
        var escape = false

        for (i in text.indices) {
            val c = text[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\' && inQuotes) {
                escape = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) startIdx = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && startIdx >= 0) {
                        results.add(text.substring(startIdx, i + 1))
                        startIdx = -1
                    }
                }
            }
        }
        return results
    }

    private fun buildCurrencyExchangeSystemPrompt(catalogSchema: String): String =
        """
        당신은 DroidKnights 2026 모바일 금융 슈퍼앱 AI 비서입니다.
        사용자의 입력 의도에 따라 아래 두 가지 방식 중 하나로 응답하세요:
        - [A. 일반 대화·단순 정보 질문인 경우] (예: 인사, 오늘 환율 질문, 우대율·수수료 질문, 공항 수령 위치·준비물 질문 등 폼 조작이 아직 필요 없는 질문):
          JSONL 없이 `REPLY: <친절하고 구체적인 한국어 텍스트 답변>`만 출력하세요. (JPY 기준환율 100엔당 847.88원·90% 우대, USD 1,335.80원, EUR 1,490.75원, CHF 1,619.05원)
        - [B. 금융 실행·조작·신청·내역조회 요청인 경우] (예: 외화 환전 신청, 간편 송금·이체, 카드 명세서 즉시결제, 해외출국 3종 패키지, 통장사본 발급 등):
          1) 첫 줄에 `REPLY: <한국어 안내 메시지 1~2문장>`을 출력하고,
          2) 이어서 반드시 아래 A2UI v0.9.1 JSONL 규격의 단일 라인 JSON 객체 3줄(`createSurface`, `updateDataModel`, `updateComponents`)만 출력하세요. 마크다운 코드블록(```)은 사용하지 마세요.

        [필수 규칙]
        - surfaceId는 반드시 `"exchange_surface"`로 고정하세요.
        - catalogId는 반드시 `"${DroidKnightsCatalogs.BASIC_CATALOG_ID}"`로 고정하세요.
        - root 컴포넌트의 id는 반드시 `"root"`여야 합니다.
        - 외화 환전 요청인 경우:
          - `BidirectionalExchangeInput` 커스텀 컴포넌트를 포함하여 외화(`foreignAmount`)와 원화(`krwAmount`)가 `/exchange/...` 경로에 양방향 바인딩되도록 하세요 (JPY: unitScale 100 / exchangeRate 847.88, USD: unitScale 1 / exchangeRate 1335.80, EUR: unitScale 1 / exchangeRate 1490.75, CHF: unitScale 1 / exchangeRate 1619.05).
          - 제출 `Button`의 `action`은 `"name": "submit_exchange"` 이벤트를 발생시키고, `context`에 `currency`, `foreignAmount`, `krwAmount`, `branch`를 포함하세요.
        - 송금·이체, 카드 명세서·즉시결제, 해외여행 패키지 등 일반 금융 문의인 경우:
          - `MetricSparkline`, `ChoicePicker`, `TextField`, `CheckBox`, `Slider`, `BidirectionalExchangeInput` 중 적절한 위젯을 조합해 인터랙티브 금융 제어판 카드를 구성하세요.
          - 제출 `Button`의 `action`은 `"name": "submit_banking_inquiry"` 이벤트를 발생시키고, `context`에 `"inquiryType": "transfer" | "card_bill" | "travel_bundle"` 및 바인딩 경로들을 포함하세요.

        [출력 예시 (정확히 이 4줄 형식을 따르세요)]
        REPLY: 요청하신 인천공항 수령 엔화 환전 신청서를 준비했습니다. 외화 또는 원화를 수정하고 신청 버튼을 눌러주세요.
        {"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}
        {"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":50000,"krwAmount":423940,"branch":["ICN_T1"],"coupon90":true,"accountPin":"1234"}}}
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_card"]},{"id":"exchange_card","component":"Card","child":"card_col"},{"id":"card_col","component":"Column","children":["title","rate_chart","exchange_converter","branch_picker","coupon_chk","pin_input","btn_submit"]},{"id":"title","component":"Text","text":"💴 JPY 인천공항 원샷 환전 (1/2단계)"},{"id":"rate_chart","component":"MetricSparkline","metricName":"JPY/KRW (90% 우대)","currentValue":"847.88 원","severity":"normal","points":[0.75,0.68,0.55,0.42,0.31,0.22]},{"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액 (외화 ↔ 원화 자동 계산)","exchangeRate":847.88,"unitScale":100,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"}},{"id":"branch_picker","component":"ChoicePicker","label":"수령 지점","variant":"mutuallyExclusive","options":[{"label":"인천공항 제1터미널","value":"ICN_T1"},{"label":"인천공항 제2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},{"id":"coupon_chk","component":"CheckBox","label":"주거래 급여통장 출금 · 90% 환율 우대","value":{"path":"/exchange/coupon90"}},{"id":"pin_input","component":"TextField","label":"계좌 비밀번호 4자리","value":{"path":"/exchange/accountPin"}},{"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"gemini_custom","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"}}}}},{"id":"btn_txt","component":"Text","text":"환전 신청 및 수령 바우처 설정 →"}]}}

        [클라이언트 등록 카탈로그 JSON 스키마 (catalog.toJsonSchemaString())]
        $catalogSchema
        """.trimIndent()

    private fun buildFollowUpSystemPrompt(catalogSchema: String): String =
        """
        당신은 DroidKnights 2026 외화 환전 AI 비서입니다.
        클라이언트로부터 `submit_exchange` 또는 `issue_exchange_voucher` 이벤트를 수신하면 모바일 QR 바우처 발급 완료 서피스(`updateComponents`)를 출력하세요.
        절대로 다시 `submit_exchange` 버튼이나 `BidirectionalExchangeInput` 입력 폼을 출력하지 마세요.
        첫 줄에는 `REPLY: <한국어 완료 안내 문장>`을 출력하고 이어서 한 줄에 하나씩 A2UI v0.9.1 JSONL만 출력하세요.

        [환전 및 바우처 발급 완료 예시 (submit_exchange 수신 시)]
        REPLY: 환전 및 모바일 수령 QR 바우처 발급이 한 번에 완료되었습니다.
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_done_card"]},{"id":"exchange_done_card","component":"Card","child":"exchange_done_col"},{"id":"exchange_done_col","component":"Column","children":["done_title","done_info1","done_info2","btn_new_exchange"]},{"id":"done_title","component":"Text","text":"🎫 환전 및 QR 바우처 발급 완료"},{"id":"done_info1","component":"Text","text":"• 환전 금액: 50,000 JPY (423,940원)"},{"id":"done_info2","component":"Text","text":"• 수령처: 인천공항 1터미널 (모바일 QR 바우처 즉시 발급)"},{"id":"btn_new_exchange","component":"Button","variant":"borderless","child":"btn_new_exchange_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"new_exchange"}}}},{"id":"btn_new_exchange_txt","component":"Text","text":"➕ 추가 환전 신청하기"}]}}

        [클라이언트 카탈로그 스키마]
        $catalogSchema
        """.trimIndent()
}
