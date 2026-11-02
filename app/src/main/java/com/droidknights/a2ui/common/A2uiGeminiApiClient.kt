package com.droidknights.a2ui.common

import android.content.Context
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.BuildConfig
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
                                val compactLine =
                                    repairAndCompleteA2uiJsonlLine(
                                        rawLine = trimmed,
                                        isFollowUpStage3 = false,
                                    )
                                if (compactLine != null && compactLine !in emittedJsonl) {
                                    parser.parse(compactLine)
                                    emittedJsonl.add(compactLine)
                                    withContext(Dispatchers.Main) { onJsonlLine(compactLine) }
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
                                    else buildLmStudioCompactSystemPrompt(userPrompt),
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
                            systemPrompt = buildLmStudioCompactSystemPrompt(userPrompt),
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
     * Real-time SSE (`stream: true` / `alt=sse`) streaming for Stage 2 (`submit_exchange` → 수령 바우처 설정)
     * and Stage 3 (`issue_exchange_voucher` → 모바일 QR 바우처 발급 완료).
     * Both LM Studio (On-Device) and Cloud Gemini API generate the follow-up `REPLY:` and A2UI v0.9.1 JSONL lines live!
     */
    suspend fun streamFollowUpExchangeSurface(
        context: Context,
        catalog: A2uiCatalog,
        event: A2uiClientEventMessage,
        voucherCode: String = "QR-EX-2026-8492",
        onReplyDelta: suspend (String) -> Unit,
        onJsonlLine: suspend (String) -> Unit,
    ): Result<GeminiA2uiResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val model = getModelName(context)
                val emittedJsonl = mutableListOf<String>()
                val parser = A2uiMessageParser()
                var lastReplyEmitted = ""
                val isStage2 = event.type == "submit_exchange"
                val isBankingInquiry = event.type == "submit_banking_inquiry"
                val expectedLines = if (isStage2) 2 else 1

                val systemPrompt =
                    when {
                        isStage2 -> buildStage2FollowUpSystemPrompt()
                        isBankingInquiry -> buildBankingInquiryDoneSystemPrompt(event)
                        else -> buildStage3FollowUpSystemPrompt(voucherCode)
                    }
                val eventSummaryPrompt =
                    when {
                        isStage2 ->
                            "클라이언트 1단계 환전 신청 이벤트 수신: eventName=${event.type}, context=${event.context}. " +
                                "전달된 외화 금액·원화 금액·수령 지점을 반영하여 2단계(수령 바우처 설정) REPLY와 A2UI JSONL 2줄(updateComponents, updateDataModel)을 출력하세요."
                        isBankingInquiry ->
                            "클라이언트 금융 거래 실행 이벤트 수신: eventName=${event.type}, context=${event.context}. " +
                                "전달된 거래 정보를 반영하여 거래 완료 REPLY와 A2UI JSONL 1줄(updateComponents)을 출력하세요."
                        else ->
                            "클라이언트 2단계 수령 방식 확정 이벤트 수신: eventName=${event.type}, voucherCode=$voucherCode, context=${event.context}. " +
                                "전달된 수령 방식·연락처·금액을 반영하여 3단계(모바일 QR 바우처 발급 완료) REPLY와 A2UI JSONL 1줄(updateComponents)을 출력하세요."
                    }

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
                        } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                            runCatching {
                                val compactLine =
                                    repairAndCompleteA2uiJsonlLine(
                                        rawLine = trimmed,
                                        isFollowUpStage3 = !isStage2,
                                    )
                                // Never emit createSurface on follow-up Stage 2 / Stage 3 turns so existing DataModel is preserved
                                if (
                                    compactLine != null &&
                                        !compactLine.contains("\"createSurface\"") &&
                                        compactLine !in emittedJsonl
                                ) {
                                    parser.parse(compactLine)
                                    emittedJsonl.add(compactLine)
                                    withContext(Dispatchers.Main) { onJsonlLine(compactLine) }
                                }
                            }
                        } else if (lastReplyEmitted.isEmpty() && !trimmed.startsWith("{")) {
                            lastReplyEmitted = trimmed
                            withContext(Dispatchers.Main) { onReplyDelta(trimmed) }
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
                        }
                    }
                    return emittedJsonl.size >= expectedLines
                }

                val rawText =
                    if (isLmStudioModel(model)) {
                        lmStudioMutex.withLock {
                            executeLmStudioStreamingRequest(
                                model = model,
                                systemPrompt = systemPrompt,
                                userPrompt = eventSummaryPrompt,
                                maxTokens = 850,
                                onBufferUpdated = { buf, isFinal ->
                                    processIncrementalBuffer(buf, isFinal)
                                },
                            )
                        }
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
                val finalParsed =
                    parseAndValidateA2uiResponse(
                        rawText = rawText,
                        model = modelLabel,
                        isFollowUpStage3 = !isStage2,
                    )

                if (finalParsed.agentReplyText != lastReplyEmitted) {
                    withContext(Dispatchers.Main) { onReplyDelta(finalParsed.agentReplyText) }
                }
                for (line in finalParsed.jsonlLines) {
                    if (!line.contains("\"createSurface\"") && line !in emittedJsonl) {
                        emittedJsonl.add(line)
                        withContext(Dispatchers.Main) { onJsonlLine(line) }
                    }
                }
                finalParsed
            }.onFailure { e ->
                if (e is CancellationException) throw e
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
        val candidateEndpoints =
            listOf(
                "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey",
                // USB Bridge fallback (`adb reverse tcp:1235 tcp:1235`) when the Android device has no Wi-Fi/LTE
                "http://127.0.0.1:1235/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey",
                "http://10.0.2.2:1235/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey",
            )

        var lastError: Throwable? = null
        for (endpoint in candidateEndpoints) {
            var connection: HttpURLConnection? = null
            var receivedAnyData = false
            try {
                val activeConn =
                    (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = if (endpoint.startsWith("https://")) 5_000 else 3_500
                        readTimeout = 20_000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("Accept", "text/event-stream")
                    }
                connection = activeConn
                val cancelHandle =
                    coroutineContext[Job]?.invokeOnCompletion { cause ->
                        if (cause != null) {
                            runCatching { activeConn.disconnect() }
                        }
                    }

                try {
                    OutputStreamWriter(activeConn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(requestBody)
                        writer.flush()
                    }

                    val status = activeConn.responseCode
                    if (status !in 200..299) {
                        val errText =
                            activeConn.errorStream?.let {
                                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
                            }.orEmpty()
                        throw IllegalStateException("Gemini SSE HTTP $status: ${errText.take(200)}")
                    }

                    val accumulated = StringBuilder()
                    BufferedReader(InputStreamReader(activeConn.inputStream, Charsets.UTF_8)).use { reader ->
                        var sseLine: String? = reader.readLine()
                        while (sseLine != null) {
                            coroutineContext.ensureActive()
                            receivedAnyData = true
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
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                lastError = e
                // If we already received an HTTP response or SSE data from the server (e.g. HTTP 400/403 API key error),
                // do not retry on fallback endpoints. Only try USB Bridge fallback when network connection failed!
                if (receivedAnyData || (e is IllegalStateException && e.message?.startsWith("Gemini SSE HTTP") == true)) {
                    break
                }
            } finally {
                runCatching { connection?.disconnect() }
            }
        }
        throw (lastError ?: IllegalStateException("Gemini API 네트워크 연결 실패"))
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
                                        ?.filterNot { it.contains("embed", ignoreCase = true) || it.contains(":") }
                                        ?.distinct()
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

    private fun buildLmStudioCompactSystemPrompt(userPrompt: String = ""): String {
        val fallbackIdx = CurrencyExchangePayloads.resolvePresetIndexFromUserText(userPrompt, 0)
        val (refReply, refLines) =
            CurrencyExchangePayloads.buildDynamicBankingJsonlForQuery(
                userQuery = userPrompt.ifBlank { "인천공항 1터미널 수령으로 5만 엔 환전해 줘" },
                fallbackCaseIndex = fallbackIdx,
            )
        val createLine = refLines.firstOrNull { it.contains("\"createSurface\"") } ?: refLines[0]
        val compLine = refLines.firstOrNull { it.contains("\"updateComponents\"") } ?: refLines.getOrElse(2) { refLines.last() }
        val dataLine = refLines.firstOrNull { it.contains("\"updateDataModel\"") } ?: refLines.getOrElse(1) { refLines.last() }

        return """
        당신은 DK 뱅크 모바일 금융 슈퍼앱의 AI 금융비서입니다.
        고객 이름: 박제창님 (DK 주거래 통장 보유, 환전·송금 수수료 전액 면제).
        기준환율(VVIP 우대): 일본 엔(JPY 100) 847.88원(90% 우대), 미국 달러(USD 1) 1,335.80원(90% 우대), 유럽 유로(EUR 1) 1,490.75원(80% 우대), 스위스 프랑(CHF 1) 1,619.05원(80% 우대).

        사용자의 입력 의도에 따라 아래 두 가지 중 하나로 응답하세요:
        - [A. 단순 정보 질문인 경우] (예: 인사, 오늘 환율 질문, 수수료 질문, 공항 수령 장소 질문):
          JSONL 없이 `REPLY: <친절한 한국어 답변 1~3문장>`만 출력하세요.
        - [B. 환전·송금·결제·내역조회 등 실행 요청인 경우]:
          1) 첫 줄에 `REPLY: <한국어 안내 문장>`을 출력하고,
          2) 이어서 마크다운 코드블록(```) 없이 아래 A2UI v0.9.1 JSONL 규격의 단일 라인 JSON 객체 3줄(`createSurface` → `updateComponents` → `updateDataModel` 순서)을 직접 생성하여 출력하세요. 사용자가 요청한 금액·수취인·통화에 맞춰 값과 제목을 반영하세요.

        [A2UI v0.9.1 JSONL 출력 예시 (정확히 4줄 형식: createSurface → updateComponents → updateDataModel 순서)]
        REPLY: $refReply
        $createLine
        $compLine
        $dataLine
        """.trimIndent()
    }

    private fun executeGenerateContentRequest(
        apiKey: String,
        model: String,
        requestBody: String,
    ): String {
        val candidateEndpoints =
            listOf(
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey",
                // USB Bridge fallback (`adb reverse tcp:1235 tcp:1235`) when the Android device has no Wi-Fi/LTE
                "http://127.0.0.1:1235/v1beta/models/$model:generateContent?key=$apiKey",
                "http://10.0.2.2:1235/v1beta/models/$model:generateContent?key=$apiKey",
            )

        var lastError: Throwable? = null
        for (endpoint in candidateEndpoints) {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = if (endpoint.startsWith("https://")) 5_000 else 3_500
                readTimeout = 18_000
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
                return parts.joinToString("\n") { part ->
                    part.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
                }
            } catch (e: Throwable) {
                lastError = e
                if (e is IllegalStateException && e.message?.startsWith("Gemini API HTTP") == true) {
                    break
                }
            } finally {
                runCatching { connection.disconnect() }
            }
        }
        throw (lastError ?: IllegalStateException("Gemini API 네트워크 연결 실패"))
    }

    internal fun repairAndCompleteA2uiJsonlLine(
        rawLine: String,
        isFollowUpStage3: Boolean = false,
    ): String? {
        val preRepaired =
            rawLine
                .trim()
                .replace(Regex(""""path"\s*:\s*"([^"]+)"\s*\]"""), """"path":"$1"}""")
                .replace("}}]}}", "}]}}")
                .replace("}]}]}", "}]}}")

        val parsedElement = runCatching { json.parseToJsonElement(preRepaired).jsonObject }.getOrNull() ?: return null
        if (
            !parsedElement.containsKey("createSurface") &&
                !parsedElement.containsKey("updateDataModel") &&
                !parsedElement.containsKey("updateComponents")
        ) {
            return null
        }

        if (parsedElement.containsKey("updateDataModel")) {
            val dmObj = parsedElement["updateDataModel"]?.jsonObject ?: return parsedElement.toString()
            val pathStr = dmObj["path"]?.jsonPrimitive?.content.orEmpty()
            val valueObj = dmObj["value"] as? JsonObject
            if (pathStr == "/exchange" && valueObj != null) {
                val rawJpy = (valueObj["jpyAmount"] as? JsonPrimitive)?.content?.toLongOrNull()
                val fixedJpy =
                    if (rawJpy != null) {
                        CurrencyExchangePayloads.normalizeToBanknoteAmount("JPY", rawJpy).first
                    } else null
                val enrichedValue =
                    buildJsonObject {
                        for ((k, v) in valueObj) {
                            when {
                                k == "jpyAmount" && fixedJpy != null ->
                                    put("jpyAmount", JsonPrimitive(fixedJpy))
                                k == "krwAmount" && fixedJpy != null && fixedJpy != rawJpy ->
                                    put("krwAmount", JsonPrimitive(kotlin.math.round((fixedJpy / 100.0) * 847.88).toLong()))
                                else -> put(k, v)
                            }
                        }
                        if (!valueObj.containsKey("agreeForexTerms")) {
                            put("agreeForexTerms", JsonPrimitive(false))
                        }
                        if (!valueObj.containsKey("agreePickupTerms")) {
                            put("agreePickupTerms", JsonPrimitive(false))
                        }
                    }
                return buildJsonObject {
                    put("version", parsedElement["version"] ?: JsonPrimitive("v0.9.1"))
                    put(
                        "updateDataModel",
                        buildJsonObject {
                            for ((k, v) in dmObj) {
                                if (k == "value") put("value", enrichedValue) else put(k, v)
                            }
                        },
                    )
                }.toString()
            }
            return parsedElement.toString()
        }

        if (!parsedElement.containsKey("updateComponents")) {
            return parsedElement.toString()
        }

        val updateComponentsObj = parsedElement["updateComponents"]?.jsonObject ?: return parsedElement.toString()
        val surfaceId = updateComponentsObj["surfaceId"]?.jsonPrimitive?.content ?: "exchange_surface"
        val origComponents = updateComponentsObj["components"]?.jsonArray ?: return parsedElement.toString()

        val definedIds = mutableSetOf<String>()
        val normalizedList = mutableListOf<JsonObject>()
        val pendingChildTextNodes = mutableListOf<JsonObject>()
        var hasSubmitExchangeButton = false

        for (elem in origComponents) {
            val comp = elem as? JsonObject ?: continue
            val id = comp["id"]?.jsonPrimitive?.content ?: continue
            val type = comp["component"]?.jsonPrimitive?.content ?: continue
            definedIds.add(id)

            when (type) {
                "Button" -> {
                    val directText =
                        (comp["text"] as? JsonPrimitive)?.content
                            ?: (comp["label"] as? JsonPrimitive)?.content
                    val childId =
                        (comp["child"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                            ?: "${id}_txt"
                    val defaultText =
                        directText
                            ?: when {
                                id.contains("issue") -> "모바일 수령 QR 바우처 발급 확정"
                                id.contains("back") -> "금액 다시 입력"
                                id.contains("submit") -> "환전 신청하기"
                                else -> "➕ 추가 환전 신청하기"
                            }
                    val defaultEventName =
                        when {
                            id.contains("issue") -> "issue_exchange_voucher"
                            id.contains("submit") -> "submit_exchange"
                            else -> "reset_exchange_surface"
                        }
                    val rawActionObj = (comp["action"] as? JsonObject)?.takeIf { it.containsKey("event") }
                    val rawEventObj = rawActionObj?.get("event") as? JsonObject
                    val resolvedEventName =
                        rawEventObj?.get("name")?.jsonPrimitive?.content ?: defaultEventName
                    if (resolvedEventName == "submit_exchange") {
                        hasSubmitExchangeButton = true
                    }
                    val validAction =
                        if (rawActionObj != null && rawEventObj != null) {
                            if (resolvedEventName == "submit_exchange") {
                                val rawCtx = rawEventObj["context"] as? JsonObject
                                val enrichedCtx =
                                    buildJsonObject {
                                        if (rawCtx != null) {
                                            for ((k, v) in rawCtx) put(k, v)
                                        }
                                        if (rawCtx == null || !rawCtx.containsKey("agreeForexTerms")) {
                                            put(
                                                "agreeForexTerms",
                                                buildJsonObject { put("path", "/exchange/agreeForexTerms") },
                                            )
                                        }
                                        if (rawCtx == null || !rawCtx.containsKey("agreePickupTerms")) {
                                            put(
                                                "agreePickupTerms",
                                                buildJsonObject { put("path", "/exchange/agreePickupTerms") },
                                            )
                                        }
                                    }
                                buildJsonObject {
                                    put(
                                        "event",
                                        buildJsonObject {
                                            put("name", resolvedEventName)
                                            put("context", enrichedCtx)
                                        },
                                    )
                                }
                            } else {
                                rawActionObj
                            }
                        } else {
                            buildJsonObject {
                                put(
                                    "event",
                                    buildJsonObject {
                                        put("name", defaultEventName)
                                        put(
                                            "context",
                                            buildJsonObject {
                                                put("reason", "new_exchange")
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    val variantStr = (comp["variant"] as? JsonPrimitive)?.content ?: "primary"
                    val rebuiltButton =
                        buildJsonObject {
                            put("id", id)
                            put("component", "Button")
                            put("variant", variantStr)
                            put("child", childId)
                            put("action", validAction)
                        }
                    normalizedList.add(rebuiltButton)
                    pendingChildTextNodes.add(
                        buildJsonObject {
                            put("id", childId)
                            put("component", "Text")
                            put("text", defaultText)
                        }
                    )
                }

                "Text" -> {
                    val textEl = comp["text"]
                    val resolvedTextPrimitive =
                        when {
                            textEl is JsonPrimitive -> textEl
                            isFollowUpStage3 && textEl is JsonObject -> {
                                val pathHint = textEl["path"]?.jsonPrimitive?.content.orEmpty()
                                JsonPrimitive(pathHint.substringAfterLast('/').ifBlank { "완료" })
                            }
                            textEl != null -> null
                            else -> JsonPrimitive("")
                        }
                    if (resolvedTextPrimitive != null) {
                        val rebuiltText =
                            buildJsonObject {
                                for ((k, v) in comp) {
                                    if (k == "text") put("text", resolvedTextPrimitive)
                                    else put(k, v)
                                }
                                if (!comp.containsKey("text")) {
                                    put("text", resolvedTextPrimitive)
                                }
                            }
                        normalizedList.add(rebuiltText)
                    } else {
                        normalizedList.add(comp)
                    }
                }

                else -> {
                    normalizedList.add(comp)
                }
            }
        }

        // Ensure every Button's child Text node exists in definedIds
        for (txtNode in pendingChildTextNodes) {
            val txtId = txtNode["id"]?.jsonPrimitive?.content ?: continue
            if (txtId !in definedIds) {
                definedIds.add(txtId)
                normalizedList.add(txtNode)
            }
        }

        // If this is a Stage 1 exchange form (contains submit_exchange button), guarantee mandatory terms CheckBoxes exist
        if (hasSubmitExchangeButton) {
            if ("terms_forex_chk" !in definedIds) {
                definedIds.add("terms_forex_chk")
                normalizedList.add(
                    buildJsonObject {
                        put("id", "terms_forex_chk")
                        put("component", "CheckBox")
                        put("label", "[필수] 외국환거래법 및 인터넷 환전 유의사항 동의")
                        put("value", buildJsonObject { put("path", "/exchange/agreeForexTerms") })
                    }
                )
            }
            if ("terms_pickup_chk" !in definedIds) {
                definedIds.add("terms_pickup_chk")
                normalizedList.add(
                    buildJsonObject {
                        put("id", "terms_pickup_chk")
                        put("component", "CheckBox")
                        put("label", "[필수] 공항 여권 지참 및 미수령 환불 규정 동의")
                        put("value", buildJsonObject { put("path", "/exchange/agreePickupTerms") })
                    }
                )
            }
        }

        // Check Column / Row children & Card child so no child ID is ever missing from definedIds (prevents infinite Shimmer)
        val extraSynthesizedNodes = mutableListOf<JsonObject>()
        val finalComponents =
            normalizedList.map { comp ->
                val type = comp["component"]?.jsonPrimitive?.content
                if (type == "Column" || type == "Row") {
                    val childrenArr = comp["children"] as? JsonArray
                    if (childrenArr != null) {
                        val validChildren = mutableListOf<String>()
                        for (cEl in childrenArr) {
                            val cid = (cEl as? JsonPrimitive)?.content ?: continue
                            if (
                                hasSubmitExchangeButton &&
                                    cid.contains("submit") &&
                                    type == "Column"
                            ) {
                                if ("terms_forex_chk" !in validChildren && childrenArr.none { (it as? JsonPrimitive)?.content == "terms_forex_chk" }) {
                                    validChildren.add("terms_forex_chk")
                                }
                                if ("terms_pickup_chk" !in validChildren && childrenArr.none { (it as? JsonPrimitive)?.content == "terms_pickup_chk" }) {
                                    validChildren.add("terms_pickup_chk")
                                }
                            }
                            if (cid in definedIds) {
                                validChildren.add(cid)
                            } else if (cid.contains("btn")) {
                                val txtId = "${cid}_txt"
                                val label =
                                    when {
                                        cid.contains("issue") -> "모바일 수령 QR 바우처 발급 확정"
                                        cid.contains("back") -> "금액 다시 입력"
                                        cid.contains("submit") -> "환전 신청하기"
                                        else -> "➕ 추가 환전 신청하기"
                                    }
                                val eventName =
                                    when {
                                        cid.contains("issue") -> "issue_exchange_voucher"
                                        cid.contains("submit") -> "submit_exchange"
                                        else -> "reset_exchange_surface"
                                    }
                                definedIds.add(cid)
                                extraSynthesizedNodes.add(
                                    buildJsonObject {
                                        put("id", cid)
                                        put("component", "Button")
                                        put("variant", if (cid.contains("back")) "borderless" else "primary")
                                        put("child", txtId)
                                        put(
                                            "action",
                                            buildJsonObject {
                                                put(
                                                    "event",
                                                    buildJsonObject {
                                                        put("name", eventName)
                                                        put(
                                                            "context",
                                                            buildJsonObject {
                                                                put("reason", "new_exchange")
                                                            },
                                                        )
                                                    },
                                                )
                                            },
                                        )
                                    }
                                )
                                if (txtId !in definedIds) {
                                    definedIds.add(txtId)
                                    extraSynthesizedNodes.add(
                                        buildJsonObject {
                                            put("id", txtId)
                                            put("component", "Text")
                                            put("text", label)
                                        }
                                    )
                                }
                                validChildren.add(cid)
                            }
                        }
                        buildJsonObject {
                            for ((k, v) in comp) {
                                if (k == "children") {
                                    put(
                                        "children",
                                        buildJsonArray {
                                            validChildren.forEach { add(JsonPrimitive(it)) }
                                        },
                                    )
                                } else {
                                    put(k, v)
                                }
                            }
                        }
                    } else {
                        comp
                    }
                } else {
                    comp
                }
            } + extraSynthesizedNodes

        return buildJsonObject {
            put("version", parsedElement["version"] ?: JsonPrimitive("v0.9.1"))
            put(
                "updateComponents",
                buildJsonObject {
                    put("surfaceId", surfaceId)
                    put(
                        "components",
                        buildJsonArray {
                            finalComponents.forEach { add(it) }
                        },
                    )
                },
            )
        }.toString()
    }

    internal fun parseAndValidateA2uiResponse(
        rawText: String,
        model: String,
        isFollowUpStage3: Boolean = false,
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
                runCatching {
                    val compactLine =
                        repairAndCompleteA2uiJsonlLine(
                            rawLine = line,
                            isFollowUpStage3 = isFollowUpStage3,
                        )
                    if (compactLine != null) {
                        parser.parse(compactLine)
                        extractedJsonl.add(compactLine)
                    }
                }
            } else if (agentReply.isEmpty() && !line.startsWith("{")) {
                agentReply = line
            }
        }

        // If Gemini formatted JSON across multiple lines, fall back to balanced brace extraction
        if (extractedJsonl.isEmpty()) {
            extractBalancedJsonObjects(rawText).forEach { candidateJson ->
                runCatching {
                    val compactLine =
                        repairAndCompleteA2uiJsonlLine(
                            rawLine = candidateJson,
                            isFollowUpStage3 = isFollowUpStage3,
                        )
                    if (compactLine != null) {
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

        [출력 예시 (정확히 이 4줄 형식을 따르세요: createSurface → updateComponents → updateDataModel 순서)]
        REPLY: 요청하신 인천공항 수령 엔화 환전 신청서를 준비했습니다. 외화 또는 원화를 수정하고 필수 약관 동의 후 신청 버튼을 눌러주세요.
        {"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_card"]},{"id":"exchange_card","component":"Card","child":"card_col"},{"id":"card_col","component":"Column","children":["title","exchange_converter","branch_picker","coupon_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},{"id":"title","component":"Text","text":"💴 JPY 인천공항 원샷 환전 (1/2단계)"},{"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액 (외화 ↔ 원화 자동 계산)","exchangeRate":847.88,"unitScale":100,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"}},{"id":"branch_picker","component":"ChoicePicker","label":"수령 지점","variant":"mutuallyExclusive","options":[{"label":"인천공항 제1터미널","value":"ICN_T1"},{"label":"인천공항 제2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},{"id":"coupon_chk","component":"CheckBox","label":"주거래 급여통장 출금 · 90% 환율 우대","value":{"path":"/exchange/coupon90"}},{"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"}},{"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 공항 여권 지참 및 미수령 환불 규정 동의","value":{"path":"/exchange/agreePickupTerms"}},{"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"gemini_custom","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},{"id":"btn_txt","component":"Text","text":"환전 신청하기"}]}}
        {"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":50000,"krwAmount":423940,"branch":["ICN_T1"],"coupon90":true,"agreeForexTerms":false,"agreePickupTerms":false}}}

        [클라이언트 등록 카탈로그 JSON 스키마 (catalog.toJsonSchemaString())]
        $catalogSchema
        """.trimIndent()

    private fun buildStage2FollowUpSystemPrompt(): String =
        """
        당신은 DK 뱅크 모바일 금융 슈퍼앱의 외화 환전 AI 비서입니다.
        클라이언트로부터 1단계 환전 신청 이벤트(`submit_exchange`)와 context(외화 금액, 원화 금액, 수령 지점)를 수신했습니다.
        절대로 차트(`MetricSparkline`)나 1단계 `submit_exchange` 폼을 다시 출력하지 마세요.
        첫 줄에 `REPLY: <한국어 안내 문장>`을 출력하고, 이어서 마크다운 코드블록(```) 없이 **2단계(수령 바우처 방식 설정)** A2UI v0.9.1 JSONL 2줄(`updateComponents` → `updateDataModel` 순서)만 출력하세요.

        [2단계 출력 예시 (정확히 3줄 형식: REPLY → updateComponents → updateDataModel)]
        REPLY: 전송된 데이터 모델(50,000 JPY (423,940원) · 인천공항 제1터미널)로 환전 신청이 접수되었습니다. 수령 바우처 발급 방식을 선택해 다음 액션을 완료해 주세요.
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_next_card"]},{"id":"exchange_next_card","component":"Card","child":"exchange_next_col"},{"id":"exchange_next_col","component":"Column","children":["next_title","next_summary_txt","next_branch_txt","picker_method","chk_push","tf_phone","btn_issue_qr","btn_back_edit"]},{"id":"next_title","component":"Text","text":"✅ [2/2단계] 환전 접수 완료 · 수령 바우처 설정"},{"id":"next_summary_txt","component":"Text","text":"💰 확정 환전 금액: 50,000 JPY (423,940원) (90% 환율 우대 · 출금 완료)"},{"id":"next_branch_txt","component":"Text","text":"📍 지정 수령처: 인천공항 제1터미널 (접수번호 EX-2026-8492)"},{"id":"picker_method","component":"ChoicePicker","label":"외화 수령 방식 선택","variant":"mutuallyExclusive","options":[{"label":"모바일 QR 바우처 (공항 무인환전기 즉시 수령)","value":"qr_kiosk"},{"label":"공항 창구 수령","value":"branch_counter"},{"label":"트래블카드 충전","value":"travel_card"}],"value":{"path":"/exchangeNext/pickupMethod"}},{"id":"chk_push","component":"CheckBox","label":"출국 당일 공항 도착 시 수령 창구 위치 알림 받기","value":{"path":"/exchangeNext/arrivalPush"}},{"id":"tf_phone","component":"TextField","label":"바우처 수신 연락처","value":{"path":"/exchangeNext/recipientPhone"}},{"id":"btn_issue_qr","component":"Button","variant":"primary","child":"btn_issue_qr_txt","action":{"event":{"name":"issue_exchange_voucher","context":{"summary":{"path":"/exchangeNext/summary"},"branchLabel":{"path":"/exchangeNext/branchLabel"},"pickupMethod":{"path":"/exchangeNext/pickupMethod"},"arrivalPush":{"path":"/exchangeNext/arrivalPush"},"recipientPhone":{"path":"/exchangeNext/recipientPhone"}}}}},{"id":"btn_issue_qr_txt","component":"Text","text":"모바일 수령 QR 바우처 발급 확정"},{"id":"btn_back_edit","component":"Button","variant":"borderless","child":"btn_back_edit_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"edit_again"}}}},{"id":"btn_back_edit_txt","component":"Text","text":"금액 다시 입력"}]}}
        {"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchangeNext","value":{"summary":"50,000 JPY (423,940원)","branchLabel":"인천공항 제1터미널","pickupMethod":["qr_kiosk"],"arrivalPush":true,"recipientPhone":"010-2026-8492"}}}
        """.trimIndent()

    private fun buildStage3FollowUpSystemPrompt(voucherCode: String): String =
        """
        당신은 DK 뱅크 모바일 금융 슈퍼앱의 외화 환전 AI 비서입니다.
        클라이언트로부터 2단계 수령 방식 확정 이벤트(`issue_exchange_voucher`)와 context(확정 금액, 수령처, 수령 방식, 연락처)를 수신했습니다.
        절대로 차트(`MetricSparkline`)나 이전 단계 버튼(`submit_exchange`, `issue_exchange_voucher`)을 다시 출력하지 마세요.
        첫 줄에 `REPLY: <한국어 바우처 발급 완료 안내 문장>`을 출력하고, 이어서 마크다운 코드블록(```) 없이 **3단계(모바일 QR 바우처 발급 완료)** A2UI v0.9.1 JSONL 1줄(`updateComponents`)만 출력하세요.

        [3단계 출력 예시 (정확히 2줄 형식: REPLY → updateComponents)]
        REPLY: 모바일 수령 바우처(모바일 QR 바우처 (공항 무인환전기))가 010-2026-8492 번호로 발급되었습니다. 공항 수령 시 QR 코드를 제시해 주세요.
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_done_card"]},{"id":"exchange_done_card","component":"Card","child":"exchange_done_col"},{"id":"exchange_done_col","component":"Column","children":["done_title","done_status_txt","done_info1","done_info2","done_info3","btn_new_exchange"]},{"id":"done_title","component":"Text","text":"🎫 외화 환전 및 모바일 QR 바우처 발급 완료"},{"id":"done_status_txt","component":"Text","text":"• 바우처 번호: $voucherCode (발급 완료 · 수령 대기)"},{"id":"done_info1","component":"Text","text":"• 확정 금액: 50,000 JPY (423,940원)"},{"id":"done_info2","component":"Text","text":"• 수령 지점 및 방식: 인천공항 제1터미널 · 모바일 QR 바우처 (공항 무인환전기)"},{"id":"done_info3","component":"Text","text":"• 알림 발송 연락처: 010-2026-8492"},{"id":"btn_new_exchange","component":"Button","variant":"primary","child":"btn_new_exchange_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"new_exchange"}}}},{"id":"btn_new_exchange_txt","component":"Text","text":"새로운 환전 신청하기"}]}}
        """.trimIndent()

    private fun buildBankingInquiryDoneSystemPrompt(event: A2uiClientEventMessage): String {
        val (refReply, refLines) = CurrencyExchangePayloads.buildBankingInquiryDoneJsonl(event)
        val compLine = refLines.firstOrNull() ?: ""
        return """
        당신은 DK 뱅크 모바일 금융 슈퍼앱의 AI 금융비서입니다.
        클라이언트로부터 금융 실행 완료 이벤트(`submit_banking_inquiry`)와 context를 수신했습니다.
        첫 줄에 `REPLY: <한국어 완료 안내 문장>`을 출력하고, 이어서 마크다운 코드블록(```) 없이 완료 카드 A2UI v0.9.1 JSONL 1줄(`updateComponents`)만 출력하세요.

        [출력 예시 (정확히 2줄 형식: REPLY → updateComponents)]
        REPLY: $refReply
        $compLine
        """.trimIndent()
    }

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
