package com.droidknights.a2ui.scenarios

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.BuildConfig
import com.droidknights.a2ui.common.A2uiComparisonDisplayMode
import com.droidknights.a2ui.common.A2uiGeminiApiClient
import com.droidknights.a2ui.common.BankingAppState
import com.droidknights.a2ui.common.ExchangeVoucherItem
import com.droidknights.a2ui.common.ExchangeVoucherQrDialog
import com.droidknights.a2ui.common.ExchangeVoucherQrTicketCard
import com.droidknights.a2ui.common.GeminiApiSettingsDialog
import com.droidknights.a2ui.common.LocalBankingAppState
import com.droidknights.a2ui.common.NativeAccountLedgerDialog
import com.droidknights.a2ui.common.NativeCardStatementDialog
import com.droidknights.a2ui.common.NativeQuickTransferDialog
import java.text.NumberFormat
import java.util.Locale

private enum class BankingHomeSubScreen {
    HOME,
    ACCOUNT_LEDGER_DETAIL,
    EXCHANGE_HISTORY_DETAIL,
}

/**
 * Real-app style Korean mobile banking home screen (Toss / Shinhan SOL style)
 * backed by reactive [LocalBankingAppState] and real native banking dialogs,
 * plus a bottom-right Floating Action Button (`🤖 AI 챗봇`).
 */
@Composable
fun BankingHomeScreen(
    onOpenAiAssistant: (presetIndex: Int, mode: A2uiComparisonDisplayMode) -> Unit,
    onOpenAllLabsShowcase: () -> Unit,
) {
    val context = LocalContext.current
    val bankingState = LocalBankingAppState.current
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }

    var hideBalance by rememberSaveable { mutableStateOf(false) }
    var activeSubScreenName by rememberSaveable { mutableStateOf(BankingHomeSubScreen.HOME.name) }
    val activeSubScreen =
        BankingHomeSubScreen.entries.firstOrNull { it.name == activeSubScreenName }
            ?: BankingHomeSubScreen.HOME

    // Native Banking Modal States
    var showAccountLedgerDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickTransferDialog by rememberSaveable { mutableStateOf(false) }
    var transferRecipientPreset by rememberSaveable { mutableStateOf("이지훈") }
    var transferAmountPreset by rememberSaveable { mutableLongStateOf(240_000L) }
    var showCardStatementDialog by rememberSaveable { mutableStateOf(false) }
    var selectedQrVoucher by remember { mutableStateOf<ExchangeVoucherItem?>(null) }

    var showGeminiSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var geminiApiKey by remember { mutableStateOf(A2uiGeminiApiClient.getApiKey(context)) }
    var geminiModel by remember { mutableStateOf(A2uiGeminiApiClient.getModelName(context)) }
    var isGeminiEnabled by remember {
        mutableStateOf(A2uiGeminiApiClient.isGeminiApiEnabled(context))
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
            },
        )
    }

    if (showAccountLedgerDialog) {
        NativeAccountLedgerDialog(
            bankingState = bankingState,
            onDismiss = { showAccountLedgerDialog = false },
            onAskAiAssistant = {
                showAccountLedgerDialog = false
                onOpenAiAssistant(7, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
            },
        )
    }

    if (showQuickTransferDialog) {
        NativeQuickTransferDialog(
            initialRecipient = transferRecipientPreset,
            initialAmount = transferAmountPreset,
            bankingState = bankingState,
            onDismiss = { showQuickTransferDialog = false },
            onOpenAiTransfer = {
                showQuickTransferDialog = false
                onOpenAiAssistant(1, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
            },
        )
    }

    if (showCardStatementDialog) {
        NativeCardStatementDialog(
            bankingState = bankingState,
            onDismiss = { showCardStatementDialog = false },
            onOpenAiCardAssistant = {
                showCardStatementDialog = false
                onOpenAiAssistant(2, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
            },
        )
    }

    selectedQrVoucher?.let { voucher ->
        ExchangeVoucherQrDialog(
            voucher = voucher,
            onDismiss = { selectedQrVoucher = null },
            onCancelVoucher = { target ->
                bankingState.cancelExchangeVoucher(target.voucherCode)
                selectedQrVoucher = null
            },
        )
    }

    val bgLight = Color(0xFFF4F6FA)
    val cardWhite = Color(0xFFFFFFFF)
    val primaryBlue = Color(0xFF1B64DA)
    val accentYellow = Color(0xFFFFCC00)
    val darkNavy = Color(0xFF191F28)
    val mutedGray = Color(0xFF6B7684)

    if (activeSubScreen != BankingHomeSubScreen.HOME) {
        BackHandler {
            activeSubScreenName = BankingHomeSubScreen.HOME.name
        }
        when (activeSubScreen) {
            BankingHomeSubScreen.ACCOUNT_LEDGER_DETAIL -> {
                AccountLedgerDetailScreen(
                    bankingState = bankingState,
                    onNavigateBack = { activeSubScreenName = BankingHomeSubScreen.HOME.name },
                    onOpenTransfer = {
                        transferRecipientPreset = "이지훈"
                        transferAmountPreset = 240_000L
                        showQuickTransferDialog = true
                    },
                    onAskAiAssistant = {
                        onOpenAiAssistant(7, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                    },
                )
            }
            BankingHomeSubScreen.EXCHANGE_HISTORY_DETAIL -> {
                ExchangeHistoryDetailScreen(
                    bankingState = bankingState,
                    onNavigateBack = { activeSubScreenName = BankingHomeSubScreen.HOME.name },
                    onSelectQrVoucher = { selectedQrVoucher = it },
                    onOpenLegacyExchange = {
                        onOpenAiAssistant(0, A2uiComparisonDisplayMode.AS_IS_LEGACY_APP)
                    },
                    onOpenAiExchange = {
                        onOpenAiAssistant(0, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                    },
                )
            }
            BankingHomeSubScreen.HOME -> Unit
        }
        return
    }

    Scaffold(
        containerColor = bgLight,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onOpenAiAssistant(-1, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                },
                containerColor = primaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(text = "🤖", fontSize = 20.sp)
                    Text(
                        text = "AI 챗봇",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Minimal Top Bar: Name | Reset / Labs / Settings Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "박제창님",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = darkNavy,
                    )
                    Surface(
                        color = primaryBlue.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "VVIP",
                            color = primaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val isModifiedState =
                        bankingState.mainAccountBalance != 31_520_000L ||
                            bankingState.exchangeVouchers.isNotEmpty() ||
                            bankingState.cardStatementBalance == 0L
                    if (isModifiedState) {
                        Surface(
                            color = Color(0xFFE8F3FF),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable { bankingState.resetToInitial() },
                        ) {
                            Text(
                                text = "↺ 초기잔액 복원",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE5E8EB),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp).clickable { onOpenAllLabsShowcase() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🧪", fontSize = 15.sp)
                        }
                    }

                    Surface(
                        color = if (isGeminiEnabled) primaryBlue.copy(alpha = 0.12f) else Color(0xFFE5E8EB),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.clickable { showGeminiSettingsDialog = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = "⚙️", fontSize = 14.sp)
                            Text(
                                text =
                                    when {
                                        !isGeminiEnabled -> "AI 설정"
                                        A2uiGeminiApiClient.isLmStudioModel(geminiModel) -> "On-Device"
                                        else -> "Gemini API"
                                    },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGeminiEnabled) primaryBlue else darkNavy,
                            )
                        }
                    }
                }
            }

            // 1-B. Issued Foreign Currency Exchange QR Voucher HorizontalPager (Swipeable Wallet Pass Carousel)
            AnimatedVisibility(visible = bankingState.exchangeVouchers.isNotEmpty()) {
                val vouchers = bankingState.exchangeVouchers
                val pagerState = rememberPagerState(pageCount = { vouchers.size })
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    activeSubScreenName = BankingHomeSubScreen.EXCHANGE_HISTORY_DETAIL.name
                                },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "🎫 보유 외화 수령 바우처",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = darkNavy,
                            )
                            Surface(
                                color = primaryBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text(
                                    text = "${ (pagerState.currentPage + 1).coerceAtMost(vouchers.size.coerceAtLeast(1)) } / ${vouchers.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = "환전 내역 · 전체보기 ›",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue,
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        contentPadding =
                            PaddingValues(
                                end = if (vouchers.size > 1) 24.dp else 0.dp,
                            ),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        val voucher = vouchers.getOrNull(page)
                        if (voucher != null) {
                            ExchangeVoucherQrTicketCard(
                                voucher = voucher,
                                modifier =
                                    Modifier.clickable {
                                        selectedQrVoucher = voucher
                                    },
                            )
                        }
                    }

                    if (vouchers.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(vouchers.size) { idx ->
                                val isSelected = pagerState.currentPage == idx
                                Box(
                                    modifier =
                                        Modifier.padding(horizontal = 3.dp)
                                            .height(6.dp)
                                            .width(if (isSelected) 18.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) primaryBlue
                                                else Color(0xFFD1D6DB)
                                            ),
                                )
                            }
                        }
                    }
                }
            }

            // 1-C. Active Travel Bundle Banner (Appears after Tokyo 3-in-1 Bundle application)
            AnimatedVisibility(visible = bankingState.activeTravelBundle != null) {
                val bundle = bankingState.activeTravelBundle
                if (bundle != null) {
                    Surface(
                        color = Color(0xFFE8F3FF),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "✈️ ${bundle.destination} 출국 3종 보호중",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryBlue,
                                )
                                Text(
                                    text = "해외원화결제(DCC) 차단 ON · 여행자보험(${bundle.insurancePlanLabel}) 가입완료",
                                    fontSize = 11.sp,
                                    color = darkNavy,
                                )
                            }
                            Text(text = "✅", fontSize = 18.sp)
                        }
                    }
                }
            }

            // 2. Main Account Card (입출금 통장 + 송금 / 환전 + 최근 이체 및 실시간 거래내역)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Account Title Row -> Navigates to Dedicated Account Ledger Detail Screen
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    activeSubScreenName = BankingHomeSubScreen.ACCOUNT_LEDGER_DETAIL.name
                                },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(36.dp)
                                        .clip(CircleShape)
                                        .background(primaryBlue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "DK",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                )
                            }
                            Column {
                                Text(
                                    text = "DK 주거래 통장",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkNavy,
                                )
                                Text(
                                    text = "110-482-910284 · 터치하여 입출금내역 보기",
                                    fontSize = 12.sp,
                                    color = mutedGray,
                                )
                            }
                        }
                        Text(
                            text = "›",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = mutedGray,
                        )
                    }

                    // Live Reactive Balance Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AnimatedContent(
                            targetState = bankingState.mainAccountBalance,
                            label = "LiveBalanceText",
                        ) { currentBalance ->
                            Text(
                                text = if (hideBalance) "잔액 숨김" else "${fmt.format(currentBalance)}원",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = darkNavy,
                            )
                        }
                        Surface(
                            color = Color(0xFFF2F4F6),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable { hideBalance = !hideBalance },
                        ) {
                            Text(
                                text = if (hideBalance) "보기" else "숨김",
                                fontSize = 12.sp,
                                color = mutedGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // 2 Primary Buttons: 송금 (Native Transfer Dialog) | 환전 (7-Step Legacy Flow)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                transferRecipientPreset = "이지훈"
                                transferAmountPreset = 240_000L
                                showQuickTransferDialog = true
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = accentYellow,
                                    contentColor = darkNavy,
                                ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text(
                                text = "송금",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }

                        Button(
                            onClick = {
                                onOpenAiAssistant(0, A2uiComparisonDisplayMode.AS_IS_LEGACY_APP)
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8F3FF),
                                    contentColor = primaryBlue,
                                ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text(
                                text = "환전",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                    }

                    // Compact 1-line Recent Transfer Pills
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val recentTransfers =
                            listOf(
                                Triple("이지훈 24만", "이지훈" to 240_000L, Color(0xFF1B64DA)),
                                Triple("김민수 12만", "김민수" to 120_000L, Color(0xFFF59E0B)),
                                Triple("최유진 8만", "최유진" to 80_000L, Color(0xFF10B981)),
                            )
                        recentTransfers.forEach { (label, preset, dotColor) ->
                            Surface(
                                color = Color(0xFFF2F4F6),
                                shape = RoundedCornerShape(20.dp),
                                modifier =
                                    Modifier.clickable {
                                        transferRecipientPreset = preset.first
                                        transferAmountPreset = preset.second
                                        showQuickTransferDialog = true
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor),
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = darkNavy,
                                    )
                                }
                            }
                        }
                    }

                    // Latest 2 Transactions Preview (Live updated!)
                    HorizontalDivider(color = Color(0xFFF2F4F6))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier.fillMaxWidth().clickable {
                                activeSubScreenName = BankingHomeSubScreen.ACCOUNT_LEDGER_DETAIL.name
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "최근 입출금 내역",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = mutedGray,
                            )
                            Text(
                                text = "상세 화면 보기 (${bankingState.transactions.size}건) ›",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryBlue,
                            )
                        }
                        bankingState.transactions.take(2).forEach { tx ->
                            val isDeposit = tx.amountDelta > 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text =
                                            when (tx.category) {
                                                "환전" -> "💴"
                                                "카드결제" -> "💳"
                                                "해외여행" -> "✈️"
                                                "급여" -> "💰"
                                                else -> "💸"
                                            },
                                        fontSize = 14.sp,
                                    )
                                    Column {
                                        Text(
                                            text = tx.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = darkNavy,
                                        )
                                        Text(
                                            text = "${tx.dateLabel} · ${tx.subtitle}",
                                            fontSize = 11.sp,
                                            color = mutedGray,
                                        )
                                    }
                                }
                                Text(
                                    text =
                                        if (isDeposit) "+${fmt.format(tx.amountDelta)}원"
                                        else "${fmt.format(tx.amountDelta)}원",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeposit) primaryBlue else darkNavy,
                                )
                            }
                        }
                    }
                }
            }

            // 3. Compact Card Statement Card (11월 명세서 · 즉시결제)
            val cardBill = bankingState.cardStatementBalance
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable { showCardStatementDialog = true },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "11월 카드 명세서",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mutedGray,
                        )
                        Text(
                            text = if (cardBill == 0L) "0원 (납부완료 ✅)" else "${fmt.format(cardBill)}원",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (cardBill == 0L) Color(0xFF10B981) else darkNavy,
                        )
                    }
                    Button(
                        onClick = { showCardStatementDialog = true },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = if (cardBill == 0L) Color(0xFFE8F3FF) else Color(0xFFF2F4F6),
                                contentColor = if (cardBill == 0L) primaryBlue else darkNavy,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = if (cardBill == 0L) "내역확인" else "즉시결제",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // 4. Clean 4-Item Menu Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val voucherCount = bankingState.exchangeVouchers.size
                    val exchangeHistoryLabel =
                        if (voucherCount > 0) "환전내역($voucherCount)" else "환전내역"
                    val quickMenus: List<Triple<String, String, () -> Unit>> =
                        listOf(
                            Triple("💴", "환전신청") {
                                onOpenAiAssistant(0, A2uiComparisonDisplayMode.AS_IS_LEGACY_APP)
                            },
                            Triple("🎫", exchangeHistoryLabel) {
                                activeSubScreenName = BankingHomeSubScreen.EXCHANGE_HISTORY_DETAIL.name
                            },
                            Triple("🧾", "입출금내역") {
                                activeSubScreenName = BankingHomeSubScreen.ACCOUNT_LEDGER_DETAIL.name
                            },
                            Triple("✈️", "출국 3종(AI)") {
                                onOpenAiAssistant(3, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                            },
                        )
                    quickMenus.forEach { (emoji, label, onClickAction) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier =
                                Modifier.clip(RoundedCornerShape(14.dp))
                                    .clickable { onClickAction() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF2F4F6)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = darkNavy,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

/**
 * Full-screen dedicated detail screen for DK Main Account Transactions (입출금 내역 상세 화면).
 */
@Composable
private fun AccountLedgerDetailScreen(
    bankingState: BankingAppState,
    onNavigateBack: () -> Unit,
    onOpenTransfer: () -> Unit,
    onAskAiAssistant: () -> Unit,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    var filterMode by rememberSaveable { mutableStateOf("ALL") }
    var showBankbookCopy by rememberSaveable { mutableStateOf(false) }

    val bgLight = Color(0xFFF4F6FA)
    val cardWhite = Color(0xFFFFFFFF)
    val primaryBlue = Color(0xFF1B64DA)
    val darkNavy = Color(0xFF191F28)
    val mutedGray = Color(0xFF6B7684)

    val filteredTx =
        bankingState.transactions.filter { tx ->
            when (filterMode) {
                "IN" -> tx.amountDelta > 0
                "OUT" -> tx.amountDelta < 0
                "EXCHANGE" -> tx.category == "환전" || tx.category == "환전취소"
                else -> true
            }
        }

    Scaffold(
        containerColor = bgLight,
        topBar = {
            Surface(
                color = cardWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = Color(0xFFF2F4F6),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp).clickable { onNavigateBack() },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "←",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkNavy,
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "입출금 내역 상세",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = darkNavy,
                            )
                            Text(
                                text = "DK 주거래 통장 (110-482-910284)",
                                fontSize = 11.sp,
                                color = mutedGray,
                            )
                        }
                    }

                    TextButton(onClick = onAskAiAssistant) {
                        Text(
                            text = "🤖 AI 분석·사본",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Account Summary Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "현재 출금가능 잔액",
                                fontSize = 12.sp,
                                color = mutedGray,
                            )
                            Text(
                                text = "${fmt.format(bankingState.mainAccountBalance)}원",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = darkNavy,
                            )
                        }
                        Surface(
                            color = Color(0xFFE8F3FF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { showBankbookCopy = !showBankbookCopy },
                        ) {
                            Text(
                                text = if (showBankbookCopy) "통장사본 닫기" else "📄 통장사본 보기",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }

                    AnimatedVisibility(visible = showBankbookCopy) {
                        Surface(
                            color = Color(0xFFF2F6FC),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "📜 전자 통장사본 (계좌개설확인서)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = primaryBlue,
                                )
                                Text("• 예금주: 박제창 (VVIP)", fontSize = 12.sp, color = darkNavy)
                                Text("• 계좌종류: DK 주거래 입출금통장 (연 2.0%)", fontSize = 12.sp, color = darkNavy)
                                Text("• 계좌번호: 110-482-910284", fontSize = 12.sp, color = darkNavy)
                                Text("• 발급일시: 2026.10.31 (직인생략)", fontSize = 12.sp, color = mutedGray)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onOpenTransfer,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = primaryBlue,
                                    contentColor = Color.White,
                                ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("💸 간편 송금하기", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onAskAiAssistant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("🤖 AI 내역 조회", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "ALL" to "전체 (${bankingState.transactions.size})",
                    "IN" to "입금·환불 (${bankingState.transactions.count { it.amountDelta > 0 }})",
                    "OUT" to "출금 (${bankingState.transactions.count { it.amountDelta < 0 }})",
                    "EXCHANGE" to "환전·환전취소 (${bankingState.transactions.count { it.category == "환전" || it.category == "환전취소" }})",
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filterMode == key,
                        onClick = { filterMode = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    )
                }
            }

            // 3. Full Transaction List Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (filteredTx.isEmpty()) {
                        Text(
                            text = "해당 조건의 거래 내역이 없습니다.",
                            fontSize = 13.sp,
                            color = mutedGray,
                            modifier = Modifier.padding(vertical = 20.dp),
                        )
                    } else {
                        filteredTx.forEachIndexed { idx, tx ->
                            val isDeposit = tx.amountDelta > 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDeposit) Color(0xFFE8F3FF)
                                                    else Color(0xFFF2F4F6)
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text =
                                                when (tx.category) {
                                                    "환전" -> "💴"
                                                    "환전취소" -> "↩️"
                                                    "카드결제" -> "💳"
                                                    "해외여행" -> "✈️"
                                                    "급여" -> "💰"
                                                    else -> "💸"
                                                },
                                            fontSize = 17.sp,
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = tx.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = darkNavy,
                                        )
                                        Text(
                                            text = "${tx.dateLabel} · ${tx.subtitle}",
                                            fontSize = 11.sp,
                                            color = mutedGray,
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text =
                                            if (isDeposit) "+${fmt.format(tx.amountDelta)}원"
                                            else "${fmt.format(tx.amountDelta)}원",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDeposit) primaryBlue else darkNavy,
                                    )
                                    Text(
                                        text = "잔액 ${fmt.format(tx.balanceAfter)}원",
                                        fontSize = 11.sp,
                                        color = mutedGray,
                                    )
                                }
                            }
                            if (idx < filteredTx.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF2F4F6))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen dedicated detail screen for Foreign Currency Exchange Vouchers & Exchange History (환전 내역 상세 화면).
 */
@Composable
private fun ExchangeHistoryDetailScreen(
    bankingState: BankingAppState,
    onNavigateBack: () -> Unit,
    onSelectQrVoucher: (ExchangeVoucherItem) -> Unit,
    onOpenLegacyExchange: () -> Unit,
    onOpenAiExchange: () -> Unit,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val bgLight = Color(0xFFF4F6FA)
    val cardWhite = Color(0xFFFFFFFF)
    val primaryBlue = Color(0xFF1B64DA)
    val darkNavy = Color(0xFF191F28)
    val mutedGray = Color(0xFF6B7684)

    val vouchers = bankingState.exchangeVouchers
    val exchangeTransactions =
        bankingState.transactions.filter {
            it.category == "환전" || it.category == "환전취소"
        }

    Scaffold(
        containerColor = bgLight,
        topBar = {
            Surface(
                color = cardWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = Color(0xFFF2F4F6),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp).clickable { onNavigateBack() },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "←",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkNavy,
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "외화 환전 · 바우처 내역",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = darkNavy,
                            )
                            Text(
                                text = "미수령 바우처 ${vouchers.size}건 · 환전 이력 ${exchangeTransactions.size}건",
                                fontSize = 11.sp,
                                color = mutedGray,
                            )
                        }
                    }

                    TextButton(onClick = onOpenAiExchange) {
                        Text(
                            text = "🤖 AI 환전",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryBlue,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Active Exchange Vouchers Section
            Text(
                text = "🎫 보유 외화 수령 바우처 (${vouchers.size}건)",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = darkNavy,
            )

            if (vouchers.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardWhite),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "현재 수령 대기 중인 외화 환전 바우처가 없습니다.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkNavy,
                        )
                        Text(
                            text = "기존 7단계 환전 또는 AI 챗봇 1단계 환전으로 신청하시면 즉시 인천공항 수령 모바일 QR 바우처가 발급됩니다.",
                            fontSize = 12.sp,
                            color = mutedGray,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onOpenLegacyExchange,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("환전 (기존 7단계)", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenAiExchange,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = primaryBlue,
                                        contentColor = Color.White,
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("🤖 AI 원샷 환전", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                vouchers.forEach { voucher ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExchangeVoucherQrTicketCard(
                            voucher = voucher,
                            modifier = Modifier.clickable { onSelectQrVoucher(voucher) },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onSelectQrVoucher(voucher) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "🔍 QR 바우처 크게 보기",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            OutlinedButton(
                                onClick = { bankingState.cancelExchangeVoucher(voucher.voucherCode) },
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "↩️ 신청 취소 (+${fmt.format(voucher.krwAmount)}원 환불)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            // 2. Exchange & Cancellation Transaction History Section
            Text(
                text = "📋 환전 · 재환전 · 취소 처리 내역 (${exchangeTransactions.size}건)",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = darkNavy,
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (exchangeTransactions.isEmpty()) {
                        Text(
                            text = "아직 진행한 외화 환전 내역이 없습니다.",
                            fontSize = 13.sp,
                            color = mutedGray,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        exchangeTransactions.forEachIndexed { idx, tx ->
                            val isRefundOrSell = tx.amountDelta > 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isRefundOrSell) Color(0xFFE8F3FF)
                                                    else Color(0xFFF2F4F6)
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (tx.category == "환전취소") "↩️" else "💴",
                                            fontSize = 17.sp,
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = tx.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = darkNavy,
                                        )
                                        Text(
                                            text = "${tx.dateLabel} · ${tx.subtitle}",
                                            fontSize = 11.sp,
                                            color = mutedGray,
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text =
                                            if (isRefundOrSell) "+${fmt.format(tx.amountDelta)}원"
                                            else "${fmt.format(tx.amountDelta)}원",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isRefundOrSell) primaryBlue else darkNavy,
                                    )
                                    Text(
                                        text = "잔액 ${fmt.format(tx.balanceAfter)}원",
                                        fontSize = 11.sp,
                                        color = mutedGray,
                                    )
                                }
                            }
                            if (idx < exchangeTransactions.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF2F4F6))
                            }
                        }
                    }
                }
            }
        }
    }
}
