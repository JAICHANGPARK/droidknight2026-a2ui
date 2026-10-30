package com.droidknights.a2ui.scenarios

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    // Native Banking Modal States
    var showAccountLedgerDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickTransferDialog by rememberSaveable { mutableStateOf(false) }
    var transferRecipientPreset by rememberSaveable { mutableStateOf("박우리") }
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
        )
    }

    val bgLight = Color(0xFFF4F6FA)
    val cardWhite = Color(0xFFFFFFFF)
    val primaryBlue = Color(0xFF1B64DA)
    val accentYellow = Color(0xFFFFCC00)
    val darkNavy = Color(0xFF191F28)
    val mutedGray = Color(0xFF6B7684)

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
                        color = Color(0xFFE5E8EB),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp).clickable { showGeminiSettingsDialog = true },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "⚙️", fontSize = 16.sp)
                        }
                    }
                }
            }

            // 1-B. Issued Foreign Currency Exchange QR Voucher Banner (Appears dynamically after Exchange!)
            AnimatedVisibility(visible = bankingState.exchangeVouchers.isNotEmpty()) {
                val latestVoucher = bankingState.exchangeVouchers.firstOrNull()
                if (latestVoucher != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExchangeVoucherQrTicketCard(
                            voucher = latestVoucher,
                            modifier = Modifier.clickable { selectedQrVoucher = latestVoucher },
                        )
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
                    // Account Title Row -> Opens Native Account Ledger & Bankbook Copy Dialog
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { showAccountLedgerDialog = true },
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
                                transferRecipientPreset = "박우리"
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
                                Triple("박우리 24만", "박우리" to 240_000L, Color(0xFF1B64DA)),
                                Triple("김신한 12만", "김신한" to 120_000L, Color(0xFFF59E0B)),
                                Triple("이아름 8만", "이아름" to 80_000L, Color(0xFF10B981)),
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
                        modifier = Modifier.fillMaxWidth().clickable { showAccountLedgerDialog = true },
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
                                text = "전체보기 (${bankingState.transactions.size}건) ›",
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
                    val quickMenus: List<Triple<String, String, () -> Unit>> =
                        listOf(
                            Triple("💴", "환전 (7단계)") {
                                onOpenAiAssistant(0, A2uiComparisonDisplayMode.AS_IS_LEGACY_APP)
                            },
                            Triple("🧾", "입출금내역") {
                                showAccountLedgerDialog = true
                            },
                            Triple("✈️", "출국 3종(AI)") {
                                onOpenAiAssistant(3, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                            },
                            Triple("💸", "간편송금") {
                                transferRecipientPreset = "박우리"
                                transferAmountPreset = 240_000L
                                showQuickTransferDialog = true
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
