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
import androidx.compose.runtime.mutableLongStateOf
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
import com.droidknights.a2ui.common.BiometricApprovalDialog
import com.droidknights.a2ui.common.ExchangeVoucherItem
import com.droidknights.a2ui.common.ExchangeVoucherQrTicketCard
import com.droidknights.a2ui.common.GeminiApiSettingsDialog
import com.droidknights.a2ui.common.LocalBankingAppState
import com.droidknights.a2ui.common.PendingBiometricRequest
import com.droidknights.a2ui.scenarios.legacy.LegacyCurrencyExchangeScreen
import com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CurrencyExchangeDemo(
    initialPresetIndex: Int = 0,
    onNavigateHome: (() -> Unit)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bankingState = LocalBankingAppState.current
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }

    var selectedPrompt by rememberSaveable(initialPresetIndex) { mutableIntStateOf(initialPresetIndex) }
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

    // Biometric / PIN Approval Modal state for A2uiActionInterceptor
    var pendingBiometricRequest by remember { mutableStateOf<PendingBiometricRequest?>(null) }
    // Track last submitted exchange parameters for Stage 3 voucher issuance & balance deduction
    var pendingExchangeCurrency by rememberSaveable { mutableStateOf("JPY") }
    var pendingExchangeForeignAmt by rememberSaveable { mutableLongStateOf(50_000L) }
    var pendingExchangeKrwAmt by rememberSaveable { mutableLongStateOf(452_060L) }
    var pendingExchangeIsSellBack by rememberSaveable { mutableStateOf(false) }
    var issuedVoucherInTurn by remember { mutableStateOf<ExchangeVoucherItem?>(null) }

    val processor =
        remember(selectedPrompt, resetGeneration, isGeminiActive, geminiModel) {
            val exchangeSecurityInterceptor =
                object : A2uiActionInterceptor {
                    override suspend fun onInterceptAction(action: A2uiUserAction): A2uiUserAction? {
                        if (action !is A2uiEventAction) return action

                        val ctx = action.context
                        val eventName = action.eventName
                        val inquiryType = ctx["inquiryType"]?.toString().orEmpty()

                        // Require native biometric / PIN approval for real money movement actions
                        val requiresBiometricApproval =
                            eventName == "submit_exchange" ||
                                (eventName == "submit_banking_inquiry" &&
                                    inquiryType in listOf("transfer", "card_bill", "travel_bundle"))

                        if (requiresBiometricApproval) {
                            val approvalDeferred = CompletableDeferred<Boolean>()
                            val (title, subtitle, amountLabel, deductionKrw) =
                                buildBiometricPromptMetadata(eventName, ctx, fmt)

                            pendingBiometricRequest =
                                PendingBiometricRequest(
                                    title = title,
                                    subtitle = subtitle,
                                    amountLabel = amountLabel,
                                    deductionKrw = deductionKrw,
                                    actionEventName = eventName,
                                    onApprove = {
                                        pendingBiometricRequest = null
                                        approvalDeferred.complete(true)
                                    },
                                    onCancel = {
                                        pendingBiometricRequest = null
                                        approvalDeferred.complete(false)
                                    },
                                )

                            val approved = approvalDeferred.await()
                            if (!approved) {
                                interceptorAuditLog.add(
                                    "🚫 ActionInterceptor 차단: 사용자가 '${action.eventName}' 생체인증을 취소했습니다.",
                                )
                                return null
                            }
                        }

                        val enrichedContext =
                            action.context.toMutableMap().apply {
                                put("securityGate", "PIN_AND_BIOMETRIC_VERIFIED")
                            }
                        interceptorAuditLog.add(
                            "🛡️ ActionInterceptor 승인: '${action.eventName}' (로컬 계좌 PIN·생체인증 서명 주입 완료)",
                        )
                        return A2uiEventAction(
                            surfaceId = action.surfaceId,
                            componentId = action.componentId,
                            timestamp = action.timestamp,
                            eventName = action.eventName,
                            context = enrichedContext,
                        )
                    }
                }
            A2uiMessageProcessor(
                catalogs = listOf(catalog),
                interceptors = listOf(exchangeSecurityInterceptor),
            )
        }

    pendingBiometricRequest?.let { req ->
        BiometricApprovalDialog(
            request = req,
            currentBalance = bankingState.mainAccountBalance,
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
        issuedVoucherInTurn = null
        isStreaming = true

        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                if (msg is A2uiClientEventMessage) {
                    when (msg.type) {
                        "submit_exchange" -> {
                            outboundLog.add(msg)
                            val ctx = msg.context
                            val caseName = ctx["case"]?.toString().orEmpty()
                            pendingExchangeCurrency = ctx["currency"]?.toString() ?: "JPY"
                            pendingExchangeForeignAmt =
                                ctx["foreignAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                                    ?: (ctx["foreignAmount"] as? Number)?.toLong()
                                    ?: 50_000L
                            pendingExchangeKrwAmt =
                                ctx["krwAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                                    ?: (ctx["krwAmount"] as? Number)?.toLong()
                                    ?: 452_060L
                            pendingExchangeIsSellBack = caseName == "sell_remaining_jpy"

                            isStreaming = true
                            streamingStatusText =
                                "📡 [2/2단계 전환] 수령 바우처 설정 서피스 스트리밍 중..."

                            val (reply, nextLines) =
                                if (isGeminiActive) {
                                    val geminiFollowUp =
                                        A2uiGeminiApiClient.generateFollowUpExchangeSurface(
                                            context = context,
                                            catalog = catalog,
                                            event = msg,
                                        ).getOrNull()
                                    if (geminiFollowUp != null) {
                                        geminiFollowUp.agentReplyText to geminiFollowUp.jsonlLines
                                    } else {
                                        CurrencyExchangePayloads.buildFollowUpVoucherStepJsonl(msg)
                                    }
                                } else {
                                    CurrencyExchangePayloads.buildFollowUpVoucherStepJsonl(msg)
                                }

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
                            streamingStatusText = "📡 [바우처 발급] 모바일 QR 바우처 서피스 스트리밍 및 계좌 출금 처리 중..."

                            val ctx = msg.context
                            val branchLabel = ctx["branchLabel"]?.toString() ?: "인천공항 1터미널"
                            val methodRaw = (ctx["pickupMethod"] as? List<*>)?.firstOrNull()?.toString() ?: "qr_kiosk"
                            val methodLabel =
                                when (methodRaw) {
                                    "branch_counter" -> "공항 창구 수령"
                                    "travel_card" -> "트래블카드 충전"
                                    else -> "모바일 QR 바우처"
                                }
                            val phone = ctx["recipientPhone"]?.toString() ?: "010-2026-8492"

                            // Update real banking account balance, ledger history, and issue QR voucher!
                            val voucher =
                                bankingState.recordCurrencyExchange(
                                    currency = pendingExchangeCurrency,
                                    foreignAmount = pendingExchangeForeignAmt,
                                    krwAmount = pendingExchangeKrwAmt,
                                    branchLabel = branchLabel,
                                    pickupMethodLabel = methodLabel,
                                    recipientPhone = phone,
                                    isSellBack = pendingExchangeIsSellBack,
                                )
                            issuedVoucherInTurn = voucher

                            val (reply, doneLines) =
                                CurrencyExchangePayloads.buildFinalVoucherIssuedJsonl(msg)
                            agentActionReplies.add(
                                "$reply (주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원)"
                            )
                            doneLines.forEach { line ->
                                delay(220L)
                                jsonlLines.add(line)
                                processor.processInput(parser, line)
                            }
                            isStreaming = false
                            streamingStatusText =
                                "✅ [완료] 모바일 수령 QR 바우처(${voucher.voucherCode}) 발급 및 잔액 차감 완료"
                        }
                        "submit_banking_inquiry" -> {
                            outboundLog.add(msg)
                            isStreaming = true
                            streamingStatusText = "📡 [금융 거래 실행] 계좌 반영 및 완료 서피스 스트리밍 중..."

                            val ctx = msg.context
                            val inquiryType = ctx["inquiryType"]?.toString() ?: "transfer"
                            when (inquiryType) {
                                "transfer" -> {
                                    val recipient = ctx["recipient"]?.toString()?.ifBlank { "박우리" } ?: "박우리"
                                    val rawAmt =
                                        ctx["amount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                                            ?: 240_000L
                                    val memo = ctx["memo"]?.toString()?.ifBlank { "모임회비" } ?: "모임회비"
                                    val fromAcc =
                                        (ctx["fromAccount"] as? List<*>)?.firstOrNull()?.toString() ?: "salary_acc"
                                    bankingState.recordTransfer(
                                        recipient = recipient,
                                        amount = rawAmt,
                                        memo = memo,
                                        fromSalaryAccount = fromAcc != "living_acc",
                                    )
                                }
                                "card_bill" -> {
                                    val rawAmt =
                                        ctx["payAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                                            ?: 2_161_314L
                                    val usePoints = ctx["usePoints"]?.toString() == "true"
                                    bankingState.recordCardBillPayment(
                                        payAmount = rawAmt,
                                        usePoints = usePoints,
                                    )
                                }
                                "travel_bundle" -> {
                                    val jpyAmt =
                                        ctx["jpyAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                                            ?: (ctx["jpyAmount"] as? Number)?.toLong()
                                            ?: 50_000L
                                    val krwAmt =
                                        ctx["krwAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                                            ?: (ctx["krwAmount"] as? Number)?.toLong()
                                            ?: 452_060L
                                    val dccBlock = ctx["dccBlock"]?.toString() != "false"
                                    val planRaw =
                                        (ctx["insPlan"] as? List<*>)?.firstOrNull()?.toString() ?: "standard_8400"
                                    val planLabel = if (planRaw == "light_4900") "실속형" else "든든형"
                                    val insFee = if (planRaw == "light_4900") 4_900L else 8_400L
                                    bankingState.recordTravelBundle(
                                        jpyAmount = jpyAmt,
                                        krwAmount = krwAmt,
                                        dccBlocked = dccBlock,
                                        insPlanLabel = planLabel,
                                        insFee = insFee,
                                    )
                                    issuedVoucherInTurn = bankingState.exchangeVouchers.firstOrNull()
                                }
                            }

                            val (reply, doneLines) =
                                CurrencyExchangePayloads.buildBankingInquiryDoneJsonl(msg)
                            agentActionReplies.add(
                                "$reply (현재 주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원)"
                            )
                            doneLines.forEach { line ->
                                delay(200L)
                                jsonlLines.add(line)
                                processor.processInput(parser, line)
                            }
                            isStreaming = false
                            streamingStatusText =
                                "✅ [완료] 실시간 계좌 잔액 반영 및 거래 완료 서피스 업데이트 완료"
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

        val activeCustomQuery = customPromptQuery?.takeIf { it.isNotBlank() }

        // Initial Welcome Text Chat Mode (when opened via 🤖 AI 챗봇 FAB before any user message)
        if (selectedPrompt < 0 && activeCustomQuery == null) {
            isStreaming = false
            streamingStatusText = null
            return@LaunchedEffect
        }

        val activePreset =
            CurrencyExchangePayloads.promptPresets.getOrElse(selectedPrompt.coerceAtLeast(0)) {
                CurrencyExchangePayloads.promptPresets.first()
            }
        val promptToUse = activeCustomQuery ?: activePreset.userPromptText
        val pureTextReply =
            activeCustomQuery?.let {
                CurrencyExchangePayloads.buildConversationalTextReplyIfPureQuestion(it, bankingState)
            }

        if (isGeminiActive) {
            streamingStatusText =
                "✨ Gemini API ($geminiModel) 호출 중..."
            val geminiResult =
                A2uiGeminiApiClient.generateCurrencyExchangeSurface(
                    context = context,
                    catalog = catalog,
                    userPrompt = promptToUse,
                )
            val response = geminiResult.getOrNull()
            if (response != null) {
                agentIntroOverride = response.agentReplyText
                if (response.jsonlLines.isEmpty()) {
                    isStreaming = false
                    streamingStatusText = null
                } else {
                    streamJsonlLinesProgressively(
                        rawLines = response.jsonlLines,
                        sourceLabel = "Gemini API (${response.modelUsed})",
                    )
                }
            } else if (pureTextReply != null) {
                delay(220L)
                agentIntroOverride = pureTextReply
                isStreaming = false
                streamingStatusText = null
            } else if (activeCustomQuery != null) {
                val (dynReply, dynLines) =
                    CurrencyExchangePayloads.buildDynamicBankingJsonlForQuery(
                        userQuery = activeCustomQuery,
                        fallbackCaseIndex = selectedPrompt.coerceAtLeast(0),
                    )
                agentIntroOverride = dynReply
                streamJsonlLinesProgressively(
                    rawLines = dynLines,
                    sourceLabel = "동적 A2UI 생성",
                )
            } else {
                val fallbackLines = CurrencyExchangePayloads.buildJsonl(selectedPrompt.coerceAtLeast(0))
                streamJsonlLinesProgressively(
                    rawLines = fallbackLines,
                    sourceLabel = "폴백 프리셋",
                )
            }
        } else {
            if (pureTextReply != null) {
                streamingStatusText = "💬 AI 금융비서가 답변을 작성 중입니다..."
                delay(220L)
                agentIntroOverride = pureTextReply
                isStreaming = false
                streamingStatusText = null
            } else if (activeCustomQuery != null) {
                streamingStatusText = "🤖 [1/3] createSurface 수신 중 · 서피스 초기화..."
                val (dynReply, dynLines) =
                    CurrencyExchangePayloads.buildDynamicBankingJsonlForQuery(
                        userQuery = activeCustomQuery,
                        fallbackCaseIndex = selectedPrompt.coerceAtLeast(0),
                    )
                agentIntroOverride = dynReply
                streamJsonlLinesProgressively(
                    rawLines = dynLines,
                    sourceLabel = "동적 A2UI 생성",
                )
            } else {
                streamingStatusText = "🤖 [1/3] createSurface 수신 중 · 서피스 초기화..."
                val rawLines = CurrencyExchangePayloads.buildJsonl(selectedPrompt.coerceAtLeast(0))
                streamJsonlLinesProgressively(
                    rawLines = rawLines,
                    sourceLabel = "프리셋 스트리밍",
                )
            }
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
        scenarioTitle = "금융 슈퍼앱",
        scenarioSubtitle = "외화 환전·간편 송금·카드 명세서·출국 3종 세트를 A2UI 양방향 서피스로 즉시 생성합니다.",
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
        conversationalQuestions = CurrencyExchangePayloads.conversationalSampleQuestions,
        onUserSendPrompt = { userText ->
            customPromptQuery = userText
            val isPureQuestion =
                CurrencyExchangePayloads.buildConversationalTextReplyIfPureQuestion(
                    userQuery = userText,
                    bankingState = bankingState,
                ) != null
            if (!isPureQuestion) {
                selectedPrompt =
                    CurrencyExchangePayloads.resolvePresetIndexFromUserText(
                        query = userText,
                        fallbackIndex = selectedPrompt.coerceAtLeast(0),
                    )
            }
            resetGeneration += 1
        },
        onNavigateBack = onNavigateHome,
        onClearOutbound = {
            resetGeneration += 1
        },
        extraHeaderControls = {
            FilterChip(
                selected = isGeminiActive,
                onClick = { showGeminiSettingsDialog = true },
                label = {
                    Text(
                        text = if (isGeminiActive) "✨ API" else "API",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                },
            )
        },
        extraTimelineBanners = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                issuedVoucherInTurn?.let { voucher ->
                    ExchangeVoucherQrTicketCard(voucher = voucher)
                }
            }
        },
        legacyAsIsContent = {
            LegacyCurrencyExchangeScreen()
        },
    ) {
        activeSurfaces.forEach { surface ->
            androidx.compose.runtime.key(surface.id) {
                A2uiSurface(
                    surfaceModel = surface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private data class BiometricPromptMetadata(
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val deductionKrw: Long,
)

private fun buildBiometricPromptMetadata(
    eventName: String,
    ctx: Map<String, Any?>,
    fmt: NumberFormat,
): BiometricPromptMetadata {
    if (eventName == "submit_exchange") {
        val currency = ctx["currency"]?.toString() ?: "JPY"
        val foreignAmt =
            ctx["foreignAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                ?: (ctx["foreignAmount"] as? Number)?.toLong()
                ?: 50_000L
        val krwAmt =
            ctx["krwAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                ?: (ctx["krwAmount"] as? Number)?.toLong()
                ?: 452_060L
        val isSell = ctx["case"]?.toString() == "sell_remaining_jpy"
        return BiometricPromptMetadata(
            title =
                if (isSell) "$currency ${fmt.format(foreignAmt)} 원화 재환전"
                else "$currency ${fmt.format(foreignAmt)} 외화 환전 신청",
            subtitle = "DK 주거래 통장 (90% 환율 우대 적용)",
            amountLabel = "${fmt.format(foreignAmt)} $currency (${fmt.format(krwAmt)}원)",
            deductionKrw = if (isSell) 0L else krwAmt,
        )
    }

    return when (ctx["inquiryType"]?.toString()) {
        "card_bill" -> {
            val rawAmt =
                ctx["payAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                    ?: 2_161_314L
            val usePoints = ctx["usePoints"]?.toString() == "true"
            val finalAmt = if (usePoints && rawAmt > 10_000L) rawAmt - 10_000L else rawAmt
            BiometricPromptMetadata(
                title = "11월 카드 명세서 즉시결제",
                subtitle = if (usePoints) "포인트 10,000P 차감 적용 · 주거래 통장 출금" else "DK 주거래 통장 출금",
                amountLabel = "${fmt.format(finalAmt)}원",
                deductionKrw = finalAmt,
            )
        }
        "travel_bundle" -> {
            val jpyAmt =
                ctx["jpyAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                    ?: (ctx["jpyAmount"] as? Number)?.toLong()
                    ?: 50_000L
            val krwAmt =
                ctx["krwAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                    ?: (ctx["krwAmount"] as? Number)?.toLong()
                    ?: 452_060L
            val planRaw = (ctx["insPlan"] as? List<*>)?.firstOrNull()?.toString() ?: "standard_8400"
            val insFee = if (planRaw == "light_4900") 4_900L else 8_400L
            val totalKrw = krwAmt + insFee
            BiometricPromptMetadata(
                title = "도쿄 출국 3종 한 번에 신청",
                subtitle = "JPY ${fmt.format(jpyAmt)} 환전 + 해외원화결제 차단 + 여행자보험",
                amountLabel = "${fmt.format(totalKrw)}원",
                deductionKrw = totalKrw,
            )
        }
        else -> {
            val recipient = ctx["recipient"]?.toString()?.substringBefore(" (")?.ifBlank { "박우리" } ?: "박우리"
            val rawAmt =
                ctx["amount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                    ?: 240_000L
            BiometricPromptMetadata(
                title = "${recipient}님께 간편 송금",
                subtitle = "출금계좌: DK 주거래 통장 (수수료 면제)",
                amountLabel = "${fmt.format(rawAmt)}원",
                deductionKrw = rawAmt,
            )
        }
    }
}

internal fun buildCurrencyExchangeJsonl(caseIndex: Int): List<String> =
    CurrencyExchangePayloads.buildJsonl(caseIndex)
