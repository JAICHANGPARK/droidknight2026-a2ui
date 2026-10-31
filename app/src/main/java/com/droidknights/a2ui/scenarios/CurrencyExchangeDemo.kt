package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import kotlin.math.roundToLong
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
import com.droidknights.a2ui.common.ExchangeVoucherQrDialog
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

    val isLmStudioActive = A2uiGeminiApiClient.isLmStudioModel(geminiModel)
    val isGeminiActive =
        isGeminiEnabled && (isLmStudioActive || geminiApiKey.isNotBlank())

    val catalog = remember(context) { CurrencyExchangeCatalog.create(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val agentActionReplies = remember { mutableStateListOf<String>() }
    val interceptorAuditLog = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }
    var agentIntroOverride by remember { mutableStateOf<String?>(null) }
    var isStreaming by remember { mutableStateOf(false) }
    var isActionSubmitting by remember { mutableStateOf(false) }
    var streamingStatusText by remember { mutableStateOf<String?>(null) }

    // Biometric / PIN Approval Modal state for A2uiActionInterceptor
    var pendingBiometricRequest by remember { mutableStateOf<PendingBiometricRequest?>(null) }
    // Track last submitted exchange parameters for Stage 3 voucher issuance & balance deduction
    var pendingExchangeCurrency by rememberSaveable { mutableStateOf("JPY") }
    var pendingExchangeForeignAmt by rememberSaveable { mutableLongStateOf(50_000L) }
    var pendingExchangeKrwAmt by rememberSaveable { mutableLongStateOf(423_940L) }
    var pendingExchangeIsSellBack by rememberSaveable { mutableStateOf(false) }
    var hasDeductedInCurrentTurn by rememberSaveable { mutableStateOf(false) }
    var issuedVoucherInTurn by remember { mutableStateOf<ExchangeVoucherItem?>(null) }
    var selectedQrVoucher by remember { mutableStateOf<ExchangeVoucherItem?>(null) }

    selectedQrVoucher?.let { voucher ->
        ExchangeVoucherQrDialog(
            voucher = voucher,
            onDismiss = { selectedQrVoucher = null },
            onCancelVoucher = { target ->
                val cancelled = bankingState.cancelExchangeVoucher(target.voucherCode)
                if (cancelled != null && issuedVoucherInTurn?.voucherCode == cancelled.voucherCode) {
                    issuedVoucherInTurn = null
                }
                selectedQrVoucher = null
            },
        )
    }

    val processor =
        remember(selectedPrompt, resetGeneration, isGeminiActive, geminiModel) {
            val exchangeSecurityInterceptor =
                object : A2uiActionInterceptor {
                    override suspend fun onInterceptAction(action: A2uiUserAction): A2uiUserAction? {
                        if (action !is A2uiEventAction) return action
                        // Block duplicate clicks while streaming, while biometric dialog is open,
                        // or while a previous action is already being submitted.
                        if (isActionSubmitting || isStreaming || pendingBiometricRequest != null) {
                            return null
                        }
                        if (action.eventName == "submit_exchange" && hasDeductedInCurrentTurn) {
                            return null
                        }
                        if (
                            action.eventName in
                                listOf(
                                    "submit_exchange",
                                    "submit_banking_inquiry",
                                    "issue_exchange_voucher",
                                    "cancel_exchange_voucher",
                                )
                        ) {
                            isActionSubmitting = true
                        }
                        val enrichedContext =
                            action.context.toMutableMap().apply {
                                put("securityGate", "PIN_AND_BIOMETRIC_VERIFIED")
                            }
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
        hasDeductedInCurrentTurn = false
        isActionSubmitting = false
        isStreaming = true

        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                try {
                    if (msg is A2uiClientEventMessage) {
                        when (msg.type) {
                            "submit_exchange" -> {
                                if (hasDeductedInCurrentTurn) return@collect
                                val ctx = msg.context
                                val caseName = extractStringField(ctx, "case").orEmpty()
                                val currency = extractStringField(ctx, "currency") ?: "JPY"
                                val foreignAmt =
                                    extractLongAmount(
                                        ctx = ctx,
                                        keys = arrayOf("foreignAmount", "jpyAmount", "usdAmount", "autoJpyAmount", "sellJpyAmount", "amount"),
                                        default = if (currency == "USD") 1_200L else 50_000L,
                                    )
                                val defaultKrw = computeDefaultKrwForCurrency(currency, foreignAmt)
                                val krwAmt =
                                    extractLongAmount(
                                        ctx = ctx,
                                        keys = arrayOf("krwAmount", "usdKrwAmount", "autoKrwAmount", "sellKrwAmount"),
                                        default = defaultKrw,
                                    )
                                val branchRaw = extractFirstSelectionOrString(ctx, "branch", "branchLabel") ?: "ICN_T1"
                                val branchLabel =
                                    when (branchRaw) {
                                        "ICN_T2", "인천공항 2터미널", "인천국제공항 제2터미널" -> "인천공항 2터미널"
                                        "ICN_T1", "인천공항 1터미널", "인천국제공항 제1터미널" -> "인천공항 1터미널"
                                        else -> branchRaw.ifBlank { "인천공항 1터미널" }
                                    }
                                val isSellBack = caseName == "sell_remaining_jpy"

                                // Show Biometric / PIN Approval Dialog with the REAL dereferenced amounts from msg.context!
                                val approvalDeferred = CompletableDeferred<Boolean>()
                                val (title, subtitle, amountLabel, deductionKrw) =
                                    buildBiometricPromptMetadata("submit_exchange", ctx, fmt)
                                val availableBalance = bankingState.mainAccountBalance
                                val isInsufficient = deductionKrw > 0L && deductionKrw > availableBalance

                                pendingBiometricRequest =
                                    PendingBiometricRequest(
                                        title = title,
                                        subtitle = subtitle,
                                        amountLabel = amountLabel,
                                        deductionKrw = deductionKrw,
                                        actionEventName = "submit_exchange",
                                        onApprove = {
                                            pendingBiometricRequest = null
                                            approvalDeferred.complete(!isInsufficient)
                                        },
                                        onCancel = {
                                            pendingBiometricRequest = null
                                            approvalDeferred.complete(false)
                                        },
                                    )

                                val approved = approvalDeferred.await()
                                if (!approved || isInsufficient) {
                                    if (isInsufficient) {
                                        val shortfall = deductionKrw - availableBalance
                                        interceptorAuditLog.add(
                                            "🚫 ActionInterceptor 거절 (잔액 부족): 요청 금액(${fmt.format(deductionKrw)}원)이 보유 잔액(${fmt.format(availableBalance)}원)을 초과하여 거래가 차단되었습니다 (부족: ${fmt.format(shortfall)}원).",
                                        )
                                        streamingStatusText =
                                            "🚫 [거래 거절] 보유 잔액(${fmt.format(availableBalance)}원) 초과로 환전·출금이 거절되었습니다."
                                    } else {
                                        interceptorAuditLog.add(
                                            "🚫 ActionInterceptor 차단: 사용자가 'submit_exchange' 생체인증을 취소했습니다.",
                                        )
                                    }
                                    return@collect
                                }

                                // Immediately lock UI & replace submit button in A2UI surface so it cannot be clicked again
                                hasDeductedInCurrentTurn = true
                                isStreaming = true
                                streamingStatusText =
                                    "📡 [환전 완료 · 계좌 출금 반영] 모바일 QR 바우처 발급 완료 서피스 업데이트 중..."
                                val lockBtnLine =
                                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[{"id":"btn_submit","component":"Card","child":"btn_txt"},{"id":"btn_txt","component":"Text","text":"⏳ 환전 신청 처리 및 QR 바우처 발급 중..."}]}}"""
                                processor.processInput(parser, lockBtnLine)

                                interceptorAuditLog.add(
                                    "🛡️ ActionInterceptor 승인: 'submit_exchange' (${fmt.format(foreignAmt)} $currency · ${fmt.format(krwAmt)}원 · 로컬 생체인증 서명 주입 완료)",
                                )
                                outboundLog.add(msg)

                                pendingExchangeCurrency = currency
                                pendingExchangeForeignAmt = foreignAmt
                                pendingExchangeKrwAmt = krwAmt
                                pendingExchangeIsSellBack = isSellBack

                                // Deduct real banking account balance immediately upon exchange submission approval!
                                val voucher =
                                    bankingState.recordCurrencyExchange(
                                        currency = currency,
                                        foreignAmount = foreignAmt,
                                        krwAmount = krwAmt,
                                        branchLabel = branchLabel,
                                        pickupMethodLabel = "모바일 QR 바우처",
                                        recipientPhone = "010-2026-8492",
                                        isSellBack = isSellBack,
                                    )
                                issuedVoucherInTurn = voucher

                                val (reply, nextLines) =
                                    CurrencyExchangePayloads.buildOneShotExchangeCompletedJsonl(
                                        event = msg,
                                        voucherCode = voucher.voucherCode,
                                    )

                                val balanceNotice =
                                    if (isSellBack) {
                                        "입금 완료 · 주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원"
                                    } else {
                                        "${fmt.format(krwAmt)}원 출금 완료 · 주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원"
                                    }
                                agentActionReplies.add("$reply ($balanceNotice)")
                                nextLines.forEachIndexed { idx, line ->
                                    delay(if (idx == 0) 120L else 180L)
                                    jsonlLines.add(line)
                                    processor.processInput(parser, line)
                                }
                                isStreaming = false
                                streamingStatusText =
                                    "✅ [환전 및 QR 바우처 발급 완료] 주거래 통장 잔액 ${fmt.format(bankingState.mainAccountBalance)}원 반영 · 바우처(${voucher.voucherCode}) 발급 (총 ${bankingState.exchangeVouchers.size}건 보유)"
                            }
                            "issue_exchange_voucher" -> {
                                outboundLog.add(msg)
                                isStreaming = true
                                streamingStatusText = "📡 [바우처 확정] 모바일 QR 바우처 수령 방식 업데이트 중..."

                                val ctx = msg.context
                                val branchLabel = extractStringField(ctx, "branchLabel") ?: "인천공항 1터미널"
                                val methodRaw = extractFirstSelectionOrString(ctx, "pickupMethod") ?: "qr_kiosk"
                                val methodLabel =
                                    when (methodRaw) {
                                        "branch_counter" -> "공항 창구 수령"
                                        "travel_card" -> "트래블카드 충전"
                                        else -> "모바일 QR 바우처"
                                    }
                                val phone = extractStringField(ctx, "recipientPhone") ?: "010-2026-8492"

                                // If already deducted in submit_exchange, update the voucher's pickup details without double-deducting!
                                val voucher =
                                    if (hasDeductedInCurrentTurn) {
                                        bankingState.updateLatestVoucherPickup(
                                            branchLabel = branchLabel,
                                            pickupMethodLabel = methodLabel,
                                            recipientPhone = phone,
                                        ) ?: bankingState.recordCurrencyExchange(
                                            currency = pendingExchangeCurrency,
                                            foreignAmount = pendingExchangeForeignAmt,
                                            krwAmount = pendingExchangeKrwAmt,
                                            branchLabel = branchLabel,
                                            pickupMethodLabel = methodLabel,
                                            recipientPhone = phone,
                                            isSellBack = pendingExchangeIsSellBack,
                                        )
                                    } else {
                                        hasDeductedInCurrentTurn = true
                                        bankingState.recordCurrencyExchange(
                                            currency = pendingExchangeCurrency,
                                            foreignAmount = pendingExchangeForeignAmt,
                                            krwAmount = pendingExchangeKrwAmt,
                                            branchLabel = branchLabel,
                                            pickupMethodLabel = methodLabel,
                                            recipientPhone = phone,
                                            isSellBack = pendingExchangeIsSellBack,
                                        )
                                    }
                                issuedVoucherInTurn = voucher

                                val (reply, doneLines) =
                                    CurrencyExchangePayloads.buildFinalVoucherIssuedJsonl(msg)
                                agentActionReplies.add(
                                    "$reply (주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원)"
                                )
                                doneLines.forEach { line ->
                                    delay(160L)
                                    jsonlLines.add(line)
                                    processor.processInput(parser, line)
                                }
                                isStreaming = false
                                streamingStatusText =
                                    "✅ [완료] 모바일 수령 QR 바우처(${voucher.voucherCode}) 확정 완료"
                            }
                            "submit_banking_inquiry" -> {
                                val ctx = msg.context
                                val inquiryType = extractStringField(ctx, "inquiryType") ?: "transfer"
                                if (inquiryType in listOf("transfer", "card_bill", "travel_bundle")) {
                                    val approvalDeferred = CompletableDeferred<Boolean>()
                                    val (title, subtitle, amountLabel, deductionKrw) =
                                        buildBiometricPromptMetadata("submit_banking_inquiry", ctx, fmt)
                                    val fromAcc = extractFirstSelectionOrString(ctx, "fromAccount") ?: "salary_acc"
                                    val availableBalance =
                                        if (inquiryType == "transfer" && fromAcc == "living_acc") {
                                            bankingState.livingAccountBalance
                                        } else {
                                            bankingState.mainAccountBalance
                                        }
                                    val isInsufficient = deductionKrw > 0L && deductionKrw > availableBalance

                                    pendingBiometricRequest =
                                        PendingBiometricRequest(
                                            title = title,
                                            subtitle = subtitle,
                                            amountLabel = amountLabel,
                                            deductionKrw = deductionKrw,
                                            actionEventName = "submit_banking_inquiry",
                                            onApprove = {
                                                pendingBiometricRequest = null
                                                approvalDeferred.complete(!isInsufficient)
                                            },
                                            onCancel = {
                                                pendingBiometricRequest = null
                                                approvalDeferred.complete(false)
                                            },
                                        )

                                    val approved = approvalDeferred.await()
                                    if (!approved || isInsufficient) {
                                        if (isInsufficient) {
                                            val shortfall = deductionKrw - availableBalance
                                            interceptorAuditLog.add(
                                                "🚫 ActionInterceptor 거절 (잔액 부족): 요청 금액(${fmt.format(deductionKrw)}원)이 보유 잔액(${fmt.format(availableBalance)}원)을 초과하여 거래가 차단되었습니다 (부족: ${fmt.format(shortfall)}원).",
                                            )
                                            streamingStatusText =
                                                "🚫 [거래 거절] 보유 잔액(${fmt.format(availableBalance)}원) 초과로 거래가 거절되었습니다."
                                        } else {
                                            interceptorAuditLog.add(
                                                "🚫 ActionInterceptor 차단: 사용자가 'submit_banking_inquiry' 생체인증을 취소했습니다.",
                                            )
                                        }
                                        return@collect
                                    }
                                    interceptorAuditLog.add(
                                        "🛡️ ActionInterceptor 승인: 'submit_banking_inquiry' ($amountLabel · 로컬 생체인증 서명 주입 완료)",
                                    )
                                }
                                outboundLog.add(msg)
                                isStreaming = true
                                streamingStatusText = "📡 [금융 거래 실행] 계좌 반영 및 완료 서피스 스트리밍 중..."
                                when (inquiryType) {
                                    "transfer" -> {
                                        val recipient = extractStringField(ctx, "recipient")?.ifBlank { "이지훈" } ?: "이지훈"
                                        val rawAmt =
                                            extractLongAmount(
                                                ctx = ctx,
                                                keys = arrayOf("amount"),
                                                default = 240_000L,
                                            )
                                        val memo = extractStringField(ctx, "memo")?.ifBlank { "모임회비" } ?: "모임회비"
                                        val fromAcc = extractFirstSelectionOrString(ctx, "fromAccount") ?: "salary_acc"
                                        bankingState.recordTransfer(
                                            recipient = recipient,
                                            amount = rawAmt,
                                            memo = memo,
                                            fromSalaryAccount = fromAcc != "living_acc",
                                        )
                                    }
                                    "card_bill" -> {
                                        val rawAmt =
                                            extractLongAmount(
                                                ctx = ctx,
                                                keys = arrayOf("payAmount", "amount"),
                                                default = 2_161_314L,
                                            )
                                        val usePoints = extractStringField(ctx, "usePoints") == "true"
                                        bankingState.recordCardBillPayment(
                                            payAmount = rawAmt,
                                            usePoints = usePoints,
                                        )
                                    }
                                    "travel_bundle" -> {
                                        val jpyAmt =
                                            extractLongAmount(
                                                ctx = ctx,
                                                keys = arrayOf("jpyAmount", "foreignAmount"),
                                                default = 50_000L,
                                            )
                                        val krwAmt =
                                            extractLongAmount(
                                                ctx = ctx,
                                                keys = arrayOf("krwAmount"),
                                                default = (jpyAmt * 9.0412).roundToLong(),
                                            )
                                        val dccBlock = extractStringField(ctx, "dccBlock") != "false"
                                        val planRaw = extractFirstSelectionOrString(ctx, "insPlan") ?: "standard_8400"
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
                                    delay(160L)
                                    jsonlLines.add(line)
                                    processor.processInput(parser, line)
                                }
                                isStreaming = false
                                streamingStatusText =
                                    "✅ [완료] 실시간 계좌 잔액 반영 및 거래 완료 서피스 업데이트 완료"
                            }
                            "cancel_exchange_voucher" -> {
                                outboundLog.add(msg)
                                val ctx = msg.context
                                val targetCode = extractFirstSelectionOrString(ctx, "voucherCode")
                                val cancelled = bankingState.cancelExchangeVoucher(targetCode)
                                if (cancelled != null) {
                                    if (issuedVoucherInTurn?.voucherCode == cancelled.voucherCode) {
                                        issuedVoucherInTurn = null
                                    }
                                    interceptorAuditLog.add(
                                        "↩️ 환전 신청 취소 완료: ${cancelled.voucherCode} (${fmt.format(cancelled.foreignAmount)} ${cancelled.currency}) · +${fmt.format(cancelled.krwAmount)}원 주거래 통장 즉시 환불",
                                    )
                                    isStreaming = true
                                    val (reply, doneLines) =
                                        CurrencyExchangePayloads.buildCancelExchangeDoneJsonl(
                                            cancelled = cancelled,
                                            balanceAfter = bankingState.mainAccountBalance,
                                            remainingCount = bankingState.exchangeVouchers.size,
                                        )
                                    agentActionReplies.add(reply)
                                    doneLines.forEach { line ->
                                        delay(160L)
                                        jsonlLines.add(line)
                                        processor.processInput(parser, line)
                                    }
                                    isStreaming = false
                                    streamingStatusText =
                                        "✅ [환전 취소 완료] ${fmt.format(cancelled.krwAmount)}원 환불 반영 · 잔액 ${fmt.format(bankingState.mainAccountBalance)}원"
                                }
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
                } finally {
                    isActionSubmitting = false
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

        val isCancelVoucherQuery =
            activeCustomQuery != null &&
                (activeCustomQuery.contains("취소") || activeCustomQuery.contains("환불")) &&
                bankingState.exchangeVouchers.isNotEmpty()

        if (isCancelVoucherQuery) {
            streamingStatusText = "🤖 [1/3] 환전 신청 취소 서피스 생성 중..."
            val (cancelReply, cancelLines) =
                CurrencyExchangePayloads.buildCancelExchangeVoucherJsonl(
                    vouchers = bankingState.exchangeVouchers.toList(),
                )
            agentIntroOverride = cancelReply
            streamJsonlLinesProgressively(
                rawLines = cancelLines,
                sourceLabel = "환전 취소 A2UI",
            )
            return@LaunchedEffect
        }

        if (isGeminiActive) {
            val modePrefix = if (isLmStudioActive) "🖥️ LM Studio ($geminiModel)" else "✨ Gemini ($geminiModel)"
            streamingStatusText = "📡 [$modePrefix] 실시간 SSE 스트리밍 연결 중..."
            val isNonExchangeBankingTask =
                (activeCustomQuery == null && selectedPrompt >= 2) ||
                    (activeCustomQuery != null &&
                        listOf("송금", "이체", "보내", "회비", "카드", "명세서", "결제", "출국", "보험", "재환전", "자동환전", "통장사본")
                            .any { activeCustomQuery.contains(it) })
            val useReplyOnly = isLmStudioActive && (pureTextReply != null || isNonExchangeBankingTask)
            val geminiResult =
                A2uiGeminiApiClient.streamCurrencyExchangeSurface(
                    context = context,
                    catalog = catalog,
                    userPrompt = promptToUse,
                    replyOnly = useReplyOnly,
                    onReplyDelta = { partialReply ->
                        agentIntroOverride = partialReply
                        if (jsonlLines.isEmpty()) {
                            streamingStatusText = "💬 [$modePrefix] 답변 실시간 스트리밍 중..."
                        }
                    },
                    onJsonlLine = { line ->
                        jsonlLines.add(line)
                        processor.processInput(parser, line)
                        val stepLabel =
                            when {
                                line.contains("\"createSurface\"") -> "createSurface 수신 → 서피스 초기화"
                                line.contains("\"updateComponents\"") -> "updateComponents 수신 → UI 스켈레톤(Shimmer) 렌더링"
                                line.contains("\"updateDataModel\"") -> "updateDataModel 수신 → 데이터 바인딩 완료"
                                else -> "JSONL 수신"
                            }
                        streamingStatusText =
                            "📡 [$modePrefix · ${jsonlLines.size}/3] $stepLabel"
                    },
                )
            val response = geminiResult.getOrNull()
            if (response != null) {
                agentIntroOverride = response.agentReplyText
                if (jsonlLines.isNotEmpty()) {
                    interceptorAuditLog.add(
                        "📡 AI 실시간 SSE 스트리밍 완료 (JSONL ${jsonlLines.size}줄): ${response.modelUsed}",
                    )
                    isStreaming = false
                    streamingStatusText =
                        "✅ [${response.modelUsed} · SSE 스트리밍 완료] 실시간 양방향 환전 서피스 활성화"
                } else if (pureTextReply != null) {
                    interceptorAuditLog.add(
                        "📡 AI 실시간 텍스트 SSE 스트리밍 완료: ${response.modelUsed}",
                    )
                    isStreaming = false
                    streamingStatusText = null
                } else if (activeCustomQuery != null) {
                    interceptorAuditLog.add(
                        "🖥️ AI 답변 스트리밍 완료 (${response.modelUsed}) · JSONL 보완 적용",
                    )
                    val (_, dynLines) =
                        CurrencyExchangePayloads.buildDynamicBankingJsonlForQuery(
                            userQuery = activeCustomQuery,
                            fallbackCaseIndex = selectedPrompt.coerceAtLeast(0),
                        )
                    streamJsonlLinesProgressively(
                        rawLines = dynLines,
                        sourceLabel = "${response.modelUsed} + A2UI",
                    )
                } else {
                    interceptorAuditLog.add(
                        "🖥️ AI 답변 스트리밍 완료 (${response.modelUsed}) · JSONL 보완 적용",
                    )
                    val fallbackLines = CurrencyExchangePayloads.buildJsonl(selectedPrompt.coerceAtLeast(0))
                    streamJsonlLinesProgressively(
                        rawLines = fallbackLines,
                        sourceLabel = "${response.modelUsed} + A2UI",
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
            resetGeneration += 1
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
                        text =
                            when {
                                !isGeminiActive -> "⚙️ AI 설정"
                                isLmStudioActive -> "⚙️ On-Device"
                                else -> "⚙️ Gemini API"
                            },
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
                    ExchangeVoucherQrTicketCard(
                        voucher = voucher,
                        modifier = Modifier.clickable { selectedQrVoucher = voucher },
                    )
                    if (onNavigateHome != null) {
                        Button(
                            onClick = onNavigateHome,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text =
                                    "🏦 은행 홈 화면으로 이동 (차감 후 잔액: ${fmt.format(bankingState.mainAccountBalance)}원 · 누적 바우처 ${bankingState.exchangeVouchers.size}건)",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        legacyAsIsContent = {
            LegacyCurrencyExchangeScreen(onNavigateHome = onNavigateHome)
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

private fun lookupContextValue(ctx: Map<String, Any?>, key: String): Any? {
    ctx[key]?.let { return it }
    for (nestedKey in listOf("payload", "exchange", "data", "transfer", "cardBill", "travelBundle")) {
        val nested = ctx[nestedKey] as? Map<*, *>
        if (nested != null && nested.containsKey(key)) {
            return nested[key]
        }
    }
    return null
}

private fun extractStringField(ctx: Map<String, Any?>, vararg keys: String): String? {
    for (key in keys) {
        val v = lookupContextValue(ctx, key) ?: continue
        when (v) {
            is String -> if (v.isNotBlank()) return v
            is List<*> -> v.firstOrNull()?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            else -> v.toString().takeIf { it.isNotBlank() && it != "null" }?.let { return it }
        }
    }
    return null
}

private fun extractFirstSelectionOrString(ctx: Map<String, Any?>, vararg keys: String): String? =
    extractStringField(ctx, *keys)

private fun extractLongAmount(
    ctx: Map<String, Any?>,
    keys: Array<String>,
    default: Long,
): Long {
    for (key in keys) {
        val v = lookupContextValue(ctx, key) ?: continue
        when (v) {
            is Number -> {
                val n = v.toDouble().roundToLong()
                if (n > 0L) return n
            }
            is String -> {
                val trimmed = v.trim()
                val asDouble = trimmed.replace(",", "").toDoubleOrNull()
                if (asDouble != null && asDouble > 0.0) {
                    return asDouble.roundToLong()
                }
                val digitsOnly = trimmed.substringBefore('.').filter { it.isDigit() }.toLongOrNull()
                if (digitsOnly != null && digitsOnly > 0L) {
                    return digitsOnly
                }
            }
        }
    }
    return default
}

private fun computeDefaultKrwForCurrency(currency: String, foreignAmount: Long): Long =
    when (currency.uppercase()) {
        "JPY" -> ((foreignAmount.toDouble() / 100.0) * 847.88).roundToLong()
        "USD" -> (foreignAmount.toDouble() * 1335.80).roundToLong()
        "EUR" -> (foreignAmount.toDouble() * 1490.75).roundToLong()
        "CHF" -> (foreignAmount.toDouble() * 1619.05).roundToLong()
        else -> ((foreignAmount.toDouble() / 100.0) * 847.88).roundToLong()
    }

private fun buildBiometricPromptMetadata(
    eventName: String,
    ctx: Map<String, Any?>,
    fmt: NumberFormat,
): BiometricPromptMetadata {
    if (eventName == "submit_exchange") {
        val currency = extractStringField(ctx, "currency") ?: "JPY"
        val foreignAmt =
            extractLongAmount(
                ctx = ctx,
                keys = arrayOf("foreignAmount", "jpyAmount", "usdAmount", "autoJpyAmount", "sellJpyAmount", "amount"),
                default = if (currency == "USD") 1_200L else 50_000L,
            )
        val krwAmt =
            extractLongAmount(
                ctx = ctx,
                keys = arrayOf("krwAmount", "usdKrwAmount", "autoKrwAmount", "sellKrwAmount"),
                default = computeDefaultKrwForCurrency(currency, foreignAmt),
            )
        val isSell = extractStringField(ctx, "case") == "sell_remaining_jpy"
        return BiometricPromptMetadata(
            title =
                if (isSell) "$currency ${fmt.format(foreignAmt)} 원화 재환전"
                else "$currency ${fmt.format(foreignAmt)} 외화 환전 신청",
            subtitle = "DK 주거래 통장 (90% 환율 우대 적용)",
            amountLabel = "${fmt.format(foreignAmt)} $currency (${fmt.format(krwAmt)}원)",
            deductionKrw = if (isSell) 0L else krwAmt,
        )
    }

    return when (extractStringField(ctx, "inquiryType")) {
        "card_bill" -> {
            val rawAmt =
                extractLongAmount(
                    ctx = ctx,
                    keys = arrayOf("payAmount", "amount"),
                    default = 2_161_314L,
                )
            val usePoints = extractStringField(ctx, "usePoints") == "true"
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
                extractLongAmount(
                    ctx = ctx,
                    keys = arrayOf("jpyAmount", "foreignAmount"),
                    default = 50_000L,
                )
            val krwAmt =
                extractLongAmount(
                    ctx = ctx,
                    keys = arrayOf("krwAmount"),
                    default = (jpyAmt * 9.0412).roundToLong(),
                )
            val planRaw = extractFirstSelectionOrString(ctx, "insPlan") ?: "standard_8400"
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
            val recipient = extractStringField(ctx, "recipient")?.substringBefore(" (")?.ifBlank { "이지훈" } ?: "이지훈"
            val rawAmt =
                extractLongAmount(
                    ctx = ctx,
                    keys = arrayOf("amount"),
                    default = 240_000L,
                )
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
