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
import kotlinx.coroutines.Dispatchers
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
    private const val PREFS_NAME = "droidknights_a2ui_gemini_prefs"
    private const val KEY_API_KEY = "gemini_api_key_override"
    private const val KEY_MODEL = "gemini_model_override"
    private const val KEY_ENABLED = "gemini_api_enabled"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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
            BuildConfig.GEMINI_MODEL.trim().ifEmpty { "gemini-2.5-flash" }
        }
    }

    fun saveModelName(context: Context, modelName: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL, modelName.trim().ifEmpty { "gemini-2.5-flash" })
            .apply()
    }

    fun isGeminiApiEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setGeminiApiEnabled(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun hasConfiguredApiKey(context: Context): Boolean = getApiKey(context).isNotBlank()

    /**
     * Calls the standard Gemini API (`models/{model}:generateContent`) with the user's natural
     * language prompt and the live `CurrencyExchangeCatalog` JSON Schema from `catalog.toJsonSchemaString()`.
     */
    suspend fun generateCurrencyExchangeSurface(
        context: Context,
        catalog: A2uiCatalog,
        userPrompt: String,
    ): Result<GeminiA2uiResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val apiKey = getApiKey(context)
                require(apiKey.isNotBlank()) { "Gemini API 키가 설정되지 않았습니다." }
                val model = getModelName(context)
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

    /**
     * Calls Gemini API when the user triggers a button action (`submit_exchange`, etc.)
     * to generate the follow-up surface update based on the dispatched `A2uiClientEventMessage`.
     */
    suspend fun generateFollowUpExchangeSurface(
        context: Context,
        catalog: A2uiCatalog,
        event: A2uiClientEventMessage,
    ): Result<GeminiA2uiResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val apiKey = getApiKey(context)
                require(apiKey.isNotBlank()) { "Gemini API 키가 설정되지 않았습니다." }
                val model = getModelName(context)
                val catalogSchema = catalog.toJsonSchemaString()

                val systemPrompt = buildFollowUpSystemPrompt(catalogSchema)
                val eventSummaryPrompt =
                    "클라이언트 UI 액션 수신: eventName=${event.type}, surfaceId=${event.surfaceId}, context=${event.context}. " +
                        "이에 맞는 다음 단계(수령 바우처 설정 또는 모바일 QR 바우처 발급 완료) A2UI JSONL을 생성해 주세요."

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

        require(extractedJsonl.isNotEmpty()) {
            "Gemini 응답에서 유효한 A2UI v0.9.1 JSONL 메시지를 찾지 못했습니다."
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
        사용자의 자연어 금융 문의(외화 환전, 간편 송금·이체, 카드 명세서 조회 및 즉시결제, 해외출국 환전·카드·보험 3종 패키지 등)를 분석하여:
        1) 첫 줄에 `REPLY: <한국어 안내 메시지 1~2문장>`을 출력하고,
        2) 이어서 반드시 아래 A2UI v0.9.1 JSONL 규격의 단일 라인 JSON 객체 3줄(`createSurface`, `updateDataModel`, `updateComponents`)만 출력하세요. 마크다운 코드블록(```)은 사용하지 마세요.

        [필수 규칙]
        - surfaceId는 반드시 `"exchange_surface"`로 고정하세요.
        - catalogId는 반드시 `"${DroidKnightsCatalogs.BASIC_CATALOG_ID}"`로 고정하세요.
        - root 컴포넌트의 id는 반드시 `"root"`여야 합니다.
        - 외화 환전 요청인 경우:
          - `BidirectionalExchangeInput` 커스텀 컴포넌트를 포함하여 외화(`foreignAmount`)와 원화(`krwAmount`)가 `/exchange/...` 경로에 양방향 바인딩되도록 하세요 (JPY: unitScale 100 / exchangeRate 904.12, USD: unitScale 1 / exchangeRate 1384.50, EUR: unitScale 1 / exchangeRate 1492.10, CHF: unitScale 1 / exchangeRate 1568.00).
          - 제출 `Button`의 `action`은 `"name": "submit_exchange"` 이벤트를 발생시키고, `context`에 `currency`, `foreignAmount`, `krwAmount`, `branch`를 포함하세요.
        - 송금·이체, 카드 명세서·즉시결제, 해외여행 패키지 등 일반 금융 문의인 경우:
          - `MetricSparkline`, `ChoicePicker`, `TextField`, `CheckBox`, `Slider`, `BidirectionalExchangeInput` 중 적절한 위젯을 조합해 인터랙티브 금융 제어판 카드를 구성하세요.
          - 제출 `Button`의 `action`은 `"name": "submit_banking_inquiry"` 이벤트를 발생시키고, `context`에 `"inquiryType": "transfer" | "card_bill" | "travel_bundle"` 및 바인딩 경로들을 포함하세요.

        [출력 예시 (정확히 이 4줄 형식을 따르세요)]
        REPLY: 요청하신 인천공항 수령 엔화 환전 신청서를 준비했습니다. 외화 또는 원화를 수정하고 신청 버튼을 눌러주세요.
        {"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}
        {"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":50000,"krwAmount":452060,"branch":["ICN_T1"],"coupon90":true,"accountPin":"1234"}}}
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_card"]},{"id":"exchange_card","component":"Card","child":"card_col"},{"id":"card_col","component":"Column","children":["title","rate_chart","exchange_converter","branch_picker","coupon_chk","pin_input","btn_submit"]},{"id":"title","component":"Text","text":"💴 JPY 인천공항 원샷 환전 (1/2단계)"},{"id":"rate_chart","component":"MetricSparkline","metricName":"JPY/KRW (90% 우대)","currentValue":"904.12 원","severity":"normal","points":[0.75,0.68,0.55,0.42,0.31,0.22]},{"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액 (외화 ↔ 원화 자동 계산)","exchangeRate":904.12,"unitScale":100,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"}},{"id":"branch_picker","component":"ChoicePicker","label":"수령 지점","variant":"mutuallyExclusive","options":[{"label":"인천공항 제1터미널","value":"ICN_T1"},{"label":"인천공항 제2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},{"id":"coupon_chk","component":"CheckBox","label":"주거래 급여통장 출금 · 90% 환율 우대","value":{"path":"/exchange/coupon90"}},{"id":"pin_input","component":"TextField","label":"계좌 비밀번호 4자리","value":{"path":"/exchange/accountPin"}},{"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"gemini_custom","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"}}}}},{"id":"btn_txt","component":"Text","text":"환전 신청 및 수령 바우처 설정 →"}]}}

        [클라이언트 등록 카탈로그 JSON 스키마 (catalog.toJsonSchemaString())]
        $catalogSchema
        """.trimIndent()

    private fun buildFollowUpSystemPrompt(catalogSchema: String): String =
        """
        당신은 DroidKnights 2026 외화 환전 AI 비서입니다.
        클라이언트로부터 `submit_exchange` 이벤트를 수신하면 2/2단계 수령 바우처 설정 서피스(`updateDataModel` + `updateComponents`)를 출력하고,
        `issue_exchange_voucher` 이벤트를 수신하면 모바일 QR 바우처 발급 완료 서피스(`updateComponents`)를 출력하세요.
        첫 줄에는 `REPLY: <한국어 안내 문장>`을 출력하고 이어서 한 줄에 하나씩 A2UI v0.9.1 JSONL만 출력하세요.

        [2/2단계 예시 (submit_exchange 수신 시)]
        REPLY: 전송된 환전 신청 데이터로 접수가 완료되었습니다. 수령 바우처 방식을 선택해 주세요.
        {"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchangeNext","value":{"summary":"50,000 JPY (452,060 원)","branchLabel":"인천공항 제1터미널","pickupMethod":["qr_kiosk"],"arrivalPush":true,"recipientPhone":"010-2026-8492"}}}
        {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"root","component":"Column","children":["exchange_next_card"]},{"id":"exchange_next_card","component":"Card","child":"exchange_next_col"},{"id":"exchange_next_col","component":"Column","children":["next_title","next_spark","next_branch_txt","picker_method","chk_arrival","tf_phone","btn_issue_qr","btn_back_edit"]},{"id":"next_title","component":"Text","text":"✅ [2/2단계] 환전 접수 완료 · 수령 바우처 설정"},{"id":"next_spark","component":"MetricSparkline","metricName":"확정 환전 금액 (출금 예정)","currentValue":"50,000 JPY (452,060 원)","severity":"normal","points":[0.35,0.42,0.50,0.62,0.78,0.92]},{"id":"next_branch_txt","component":"Text","text":"📍 지정 수령처: 인천공항 제1터미널"},{"id":"picker_method","component":"ChoicePicker","label":"외화 수령 방식 선택","variant":"mutuallyExclusive","options":[{"label":"모바일 QR 바우처 (공항 무인환전기 즉시 수령)","value":"qr_kiosk"},{"label":"공항 은행 창구 대면 수령","value":"branch_counter"},{"label":"트래블 외화 선불카드 즉시 충전","value":"travel_card"}],"value":{"path":"/exchangeNext/pickupMethod"}},{"id":"chk_arrival","component":"CheckBox","label":"출국 당일 공항 도착 시 수령 창구 위치 알림 받기","value":{"path":"/exchangeNext/arrivalPush"}},{"id":"tf_phone","component":"TextField","label":"바우처 수신 연락처","value":{"path":"/exchangeNext/recipientPhone"}},{"id":"btn_issue_qr","component":"Button","variant":"primary","child":"btn_issue_qr_txt","action":{"event":{"name":"issue_exchange_voucher","context":{"summary":{"path":"/exchangeNext/summary"},"branchLabel":{"path":"/exchangeNext/branchLabel"},"pickupMethod":{"path":"/exchangeNext/pickupMethod"},"arrivalPush":{"path":"/exchangeNext/arrivalPush"},"recipientPhone":{"path":"/exchangeNext/recipientPhone"}}}}},{"id":"btn_issue_qr_txt","component":"Text","text":"모바일 수령 QR 바우처 발급 확정"},{"id":"btn_back_edit","component":"Button","variant":"borderless","child":"btn_back_edit_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"edit_again"}}}},{"id":"btn_back_edit_txt","component":"Text","text":"↩ 환전 금액·조건 다시 변경하기"}]}}

        [클라이언트 카탈로그 스키마]
        $catalogSchema
        """.trimIndent()
}
