package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.BuildConfig
import com.droidknights.a2ui.catalog.domains.CurrencyExchangeCatalog
import com.droidknights.a2ui.common.A2uiAgentChatScaffold
import com.droidknights.a2ui.common.A2uiGeminiApiClient
import com.droidknights.a2ui.common.GeminiApiSettingsDialog
import com.droidknights.a2ui.scenarios.legacy.LegacyCurrencyExchangeScreen
import com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CurrencyExchangeDemo() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedPrompt by rememberSaveable { mutableIntStateOf(0) }
    var resetGeneration by rememberSaveable { mutableIntStateOf(0) }
    var customPromptQuery by rememberSaveable { mutableStateOf<String?>(null) }

    var showGeminiSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var geminiApiKey by remember { mutableStateOf(A2uiGeminiApiClient.getApiKey(context)) }
    var geminiModel by remember { mutableStateOf(A2uiGeminiApiClient.getModelName(context)) }
    var isGeminiEnabled by remember {
        mutableStateOf(A2uiGeminiApiClient.isGeminiApiEnabled(context))
    }

    val isGeminiActive = isGeminiEnabled && geminiApiKey.isNotBlank()

    val catalog = remember(context) { CurrencyExchangeCatalog.create(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val agentActionReplies = remember { mutableStateListOf<String>() }
    val interceptorAuditLog = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }
    var agentIntroOverride by remember { mutableStateOf<String?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var streamingStatusText by remember { mutableStateOf<String?>(null) }

    val processor =
        remember(selectedPrompt, resetGeneration, isGeminiActive, geminiModel) {
            val exchangeSecurityInterceptor =
                object : A2uiActionInterceptor {
                    override suspend fun onInterceptAction(action: A2uiUserAction): A2uiUserAction? {
                        return if (action is A2uiEventAction) {
                            val enrichedContext =
                                action.context.toMutableMap().apply {
                                    put("securityGate", "PIN_AND_BIOMETRIC_VERIFIED")
                                }
                            interceptorAuditLog.add(
                                "🛡️ ActionInterceptor 승인: '${action.eventName}' (로컬 계좌 PIN·생체인증 서명 주입 완료)",
                            )
                            A2uiEventAction(
                                surfaceId = action.surfaceId,
                                componentId = action.componentId,
                                timestamp = action.timestamp,
                                eventName = action.eventName,
                                context = enrichedContext,
                            )
                        } else {
                            action
                        }
                    }
                }
            A2uiMessageProcessor(
                catalogs = listOf(catalog),
                interceptors = listOf(exchangeSecurityInterceptor),
            )
        }

    suspend fun streamJsonlLinesProgressively(
        rawLines: List<String>,
        sourceLabel: String,
    ) {
        val createLine = rawLines.firstOrNull { it.contains("\"createSurface\"") }
        val updateDataLine = rawLines.firstOrNull { it.contains("\"updateDataModel\"") }
        val updateComponentsLine = rawLines.firstOrNull { it.contains("\"updateComponents\"") }

        if (createLine != null && updateComponentsLine != null && updateDataLine != null) {
            delay(120L)
            jsonlLines.add(createLine)
            processor.processInput(parser, createLine)

            delay(160L)
            streamingStatusText =
                "📡 [$sourceLabel · 2/3] updateComponents 수신 → UI 스켈레톤(Shimmer) 렌더링 및 DataModel 대기 중..."
            jsonlLines.add(updateComponentsLine)
            processor.processInput(parser, updateComponentsLine)

            delay(500L)
            jsonlLines.add(updateDataLine)
            processor.processInput(parser, updateDataLine)
            isStreaming = false
            streamingStatusText =
                "✅ [$sourceLabel · 3/3] updateDataModel 바인딩 완료 · 실시간 양방향 환전 서피스 활성화"
        } else {
            rawLines.forEach { line ->
                delay(180L)
                jsonlLines.add(line)
                processor.processInput(parser, line)
            }
            isStreaming = false
            streamingStatusText = "✅ [$sourceLabel] A2UI 서피스 스트리밍 완료 (${jsonlLines.size}줄)"
        }
    }

    LaunchedEffect(processor) {
        outboundLog.clear()
        agentActionReplies.clear()
        interceptorAuditLog.clear()
        jsonlLines.clear()
        agentIntroOverride = null
        isStreaming = true

        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                if (msg is A2uiClientEventMessage) {
                    when (msg.type) {
                        "submit_exchange" -> {
                            outboundLog.add(msg)
                            isStreaming = true
                            streamingStatusText =
                                "📡 [2/2단계 전환] 수령 바우처 설정 서피스 스트리밍 중..."
                            val (reply, nextLines) =
                                CurrencyExchangePayloads.buildFollowUpVoucherStepJsonl(msg)
                            agentActionReplies.add(reply)
                            nextLines.forEachIndexed { idx, line ->
                                delay(if (idx == 0) 180L else 260L)
                                jsonlLines.add(line)
                                processor.processInput(parser, line)
                            }
                            isStreaming = false
                            streamingStatusText =
                                "✅ [2/2단계] 수령 바우처 설정 서피스 업데이트 완료 (총 ${jsonlLines.size}줄)"
                        }
                        "issue_exchange_voucher" -> {
                            outboundLog.add(msg)
                            isStreaming = true
                            streamingStatusText = "📡 [바우처 발급] 모바일 QR 바우처 서피스 스트리밍 중..."
                            val (reply, doneLines) =
                                CurrencyExchangePayloads.buildFinalVoucherIssuedJsonl(msg)
                            agentActionReplies.add(reply)
                            doneLines.forEach { line ->
                                delay(220L)
                                jsonlLines.add(line)
                                processor.processInput(parser, line)
                            }
                            isStreaming = false
                            streamingStatusText =
                                "✅ [완료] 모바일 수령 QR 바우처 발급 완료 (총 ${jsonlLines.size}줄)"
                        }
                        "reset_exchange_surface" -> {
                            resetGeneration += 1
                        }
                        else -> {
                            outboundLog.add(msg)
                        }
                    }
                } else {
                    outboundLog.add(msg)
                }
            }
        }

        val activePreset =
            CurrencyExchangePayloads.promptPresets.getOrElse(selectedPrompt) {
                CurrencyExchangePayloads.promptPresets.first()
            }
        val promptToUse = customPromptQuery?.takeIf { it.isNotBlank() } ?: activePreset.userPromptText

        if (isGeminiActive) {
            streamingStatusText =
                "✨ Gemini API ($geminiModel) 호출 중 · catalog.toJsonSchemaString() 스키마 전달 및 A2UI 생성 대기..."
            val geminiResult =
                A2uiGeminiApiClient.generateCurrencyExchangeSurface(
                    context = context,
                    catalog = catalog,
                    userPrompt = promptToUse,
                )
            val response = geminiResult.getOrNull()
            if (response != null) {
                agentIntroOverride = response.agentReplyText
                streamJsonlLinesProgressively(
                    rawLines = response.jsonlLines,
                    sourceLabel = "Gemini API (${response.modelUsed})",
                )
            } else {
                val errorReason =
                    geminiResult.exceptionOrNull()?.message?.take(80) ?: "네트워크/응답 오류"
                interceptorAuditLog.add(
                    "⚠️ Gemini API 폴백 ($errorReason) → 오프라인 안전 프리셋 스트리밍으로 자동 전환됨",
                )
                val fallbackLines = CurrencyExchangePayloads.buildJsonl(selectedPrompt)
                streamJsonlLinesProgressively(
                    rawLines = fallbackLines,
                    sourceLabel = "폴백 프리셋",
                )
            }
        } else {
            streamingStatusText = "🤖 [1/3] createSurface 수신 중 · 서피스 초기화..."
            val rawLines = CurrencyExchangePayloads.buildJsonl(selectedPrompt)
            streamJsonlLinesProgressively(
                rawLines = rawLines,
                sourceLabel = "프리셋 스트리밍",
            )
        }
    }

    if (showGeminiSettingsDialog) {
        GeminiApiSettingsDialog(
            initialApiKey = geminiApiKey,
            initialModel = geminiModel,
            initialEnabled = isGeminiEnabled,
            hasBuildConfigKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            onDismiss = { showGeminiSettingsDialog = false },
            onSave = { newKey, newModel, newEnabled ->
                A2uiGeminiApiClient.saveApiKey(context, newKey)
                A2uiGeminiApiClient.saveModelName(context, newModel)
                A2uiGeminiApiClient.setGeminiApiEnabled(context, newEnabled)
                geminiApiKey = A2uiGeminiApiClient.getApiKey(context)
                geminiModel = A2uiGeminiApiClient.getModelName(context)
                isGeminiEnabled = A2uiGeminiApiClient.isGeminiApiEnabled(context)
                showGeminiSettingsDialog = false
                resetGeneration += 1
            },
        )
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    A2uiAgentChatScaffold(
        scenarioTitle = "외화 환전",
        scenarioSubtitle = "기존 7단계 환전 화면 이동과 A2UI 양방향 DataModel + ActionInterceptor 보안 통제를 비교합니다.",
        catalogBadge = "CurrencyExchangeCatalog",
        prompts = CurrencyExchangePayloads.promptPresets,
        selectedPromptIndex = selectedPrompt,
        onSelectPrompt = {
            customPromptQuery = null
            selectedPrompt = it
        },
        outboundLog = outboundLog,
        jsonlStream = jsonlLines,
        agentActionReplies = agentActionReplies,
        agentIntroOverride = agentIntroOverride,
        streamingStatusText = streamingStatusText,
        isStreaming = isStreaming,
        expectedJsonlCount = 3,
        onUserSendPrompt = { userText ->
            customPromptQuery = userText
            resetGeneration += 1
        },
        onClearOutbound = {
            resetGeneration += 1
        },
        extraHeaderControls = {
            FilterChip(
                selected = isGeminiActive,
                onClick = { showGeminiSettingsDialog = true },
                label = {
                    Text(
                        text =
                            if (isGeminiActive) "✨ Gemini API ($geminiModel) 연결됨"
                            else "🔑 Gemini API 설정 (프리셋 모드)",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        },
        extraTimelineBanners = {
            if (interceptorAuditLog.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        interceptorAuditLog.forEach { logEntry ->
                            Text(
                                text = logEntry,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        },
        legacyAsIsContent = {
            LegacyCurrencyExchangeScreen()
        },
    ) {
        activeSurfaces.forEach { surface ->
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal fun buildCurrencyExchangeJsonl(caseIndex: Int): List<String> =
    CurrencyExchangePayloads.buildJsonl(caseIndex)
