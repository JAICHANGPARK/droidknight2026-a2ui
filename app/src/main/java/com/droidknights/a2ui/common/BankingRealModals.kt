package com.droidknights.a2ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

data class PendingBiometricRequest(
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val deductionKrw: Long,
    val actionEventName: String,
    val onApprove: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * Realistic Korean mobile banking Biometric / PIN security verification dialog
 * triggered by `A2uiActionInterceptor` or native banking actions.
 */
@Composable
fun BiometricApprovalDialog(
    request: PendingBiometricRequest,
    currentBalance: Long,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val isInsufficientBalance = request.deductionKrw > 0L && request.deductionKrw > currentBalance
    val shortfall = (request.deductionKrw - currentBalance).coerceAtLeast(0L)
    val afterBalance = (currentBalance - request.deductionKrw).coerceAtLeast(0L)
    var usePinMode by rememberSaveable { mutableStateOf(false) }
    var pinDigits by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = request.onCancel,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier.size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isInsufficientBalance) MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                                else Color(0xFF1B64DA).copy(alpha = 0.12f)
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = if (isInsufficientBalance) "🚫" else "🔐", fontSize = 18.sp)
                }
                Column {
                    Text(
                        text =
                            if (isInsufficientBalance) "잔액 부족 · 거래 거절 (ActionInterceptor)"
                            else "금융 보안 인증 (ActionInterceptor)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (isInsufficientBalance) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            if (isInsufficientBalance) "보유 계좌 잔액을 초과하여 출금·환전 요청이 거절되었습니다"
                            else "클라이언트 생체인증 서명 후 에이전트로 전송됩니다",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = request.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = request.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "거래 금액",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = request.amountLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color =
                                    if (isInsufficientBalance) MaterialTheme.colorScheme.error
                                    else Color(0xFF1B64DA),
                            )
                        }
                        if (request.deductionKrw > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "현재 보유 잔액",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${fmt.format(currentBalance)}원",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = if (isInsufficientBalance) "부족 금액" else "출금 후 예상 잔액",
                                    style = MaterialTheme.typography.labelSmall,
                                    color =
                                        if (isInsufficientBalance) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text =
                                        if (isInsufficientBalance) "-${fmt.format(shortfall)}원 (잔액 부족)"
                                        else "${fmt.format(afterBalance)}원",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (isInsufficientBalance) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                if (isInsufficientBalance) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "🚫 보유 잔액을 초과하여 신청할 수 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text =
                                    "출금 필요액(${fmt.format(request.deductionKrw)}원)이 현재 출금 가능 잔액(${fmt.format(currentBalance)}원)보다 ${fmt.format(shortfall)}원 많습니다. 금액을 조정한 뒤 다시 신청해 주세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                } else if (!usePinMode) {
                    Surface(
                        color = Color(0xFFE8F3FF),
                        shape = RoundedCornerShape(16.dp),
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { request.onApprove() },
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1B64DA)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "🫆", fontSize = 28.sp, color = Color.White)
                            }
                            Text(
                                text = "지문 / Face ID 터치하여 즉시 승인",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B64DA),
                            )
                            Text(
                                text = "securityGate: PIN_AND_BIOMETRIC_VERIFIED 서명 주입",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1B64DA).copy(alpha = 0.8f),
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "계좌 비밀번호 4자리 입력",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            repeat(4) { idx ->
                                val filled = idx < pinDigits.length
                                Box(
                                    modifier =
                                        Modifier.size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (filled) Color(0xFF1B64DA)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("1", "2", "3", "4").forEach { digit ->
                                OutlinedButton(
                                    onClick = {
                                        if (pinDigits.length < 4) {
                                            pinDigits += digit
                                            if (pinDigits.length == 4) {
                                                request.onApprove()
                                            }
                                        }
                                    },
                                ) {
                                    Text(digit, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!isInsufficientBalance) {
                    TextButton(
                        onClick = {
                            usePinMode = !usePinMode
                            pinDigits = ""
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = if (usePinMode) "생체인증(지문)으로 인증하기" else "비밀번호(PIN)로 인증하기",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isInsufficientBalance) {
                Button(
                    onClick = request.onCancel,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("확인 (거래 거절)", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = request.onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B64DA)),
                ) {
                    Text("생체인증 승인", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isInsufficientBalance) {
                TextButton(onClick = request.onCancel) {
                    Text("취소")
                }
            }
        },
    )
}

/**
 * Visual QR Code Ticket Card for Airport Kiosk Foreign Currency Pickup.
 */
@Composable
fun ExchangeVoucherQrTicketCard(
    voucher: ExchangeVoucherItem,
    modifier: Modifier = Modifier,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFF191F28),
                contentColor = Color.White,
            ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "🎫 모바일 외화 수령 바우처",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFFCC00),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${fmt.format(voucher.foreignAmount)} ${voucher.currency}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "발급 완료 · 잔액 차감됨",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Crisp Deterministic QR Code Canvas
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(96.dp),
                ) {
                    Canvas(modifier = Modifier.padding(8.dp)) {
                        val gridSize = 13
                        val cell = size.width / gridSize
                        val seed = voucher.voucherCode.hashCode()

                        fun isFinderPattern(r: Int, c: Int): Boolean {
                            val topLeft = r in 0..3 && c in 0..3
                            val topRight = r in 0..3 && c in (gridSize - 4) until gridSize
                            val bottomLeft = r in (gridSize - 4) until gridSize && c in 0..3
                            return topLeft || topRight || bottomLeft
                        }

                        for (r in 0 until gridSize) {
                            for (c in 0 until gridSize) {
                                val drawBlack =
                                    if (isFinderPattern(r, c)) {
                                        val lr = if (r >= gridSize - 4) r - (gridSize - 4) else r
                                        val lc = if (c >= gridSize - 4) c - (gridSize - 4) else c
                                        lr == 0 || lr == 3 || lc == 0 || lc == 3 || (lr in 1..2 && lc in 1..2)
                                    } else {
                                        ((r * 31 + c * 17 + seed) and 1) == 0
                                    }
                                if (drawBlack) {
                                    drawRect(
                                        color = Color(0xFF191F28),
                                        topLeft = Offset(c * cell, r * cell),
                                        size = Size(cell * 0.92f, cell * 0.92f),
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = voucher.voucherCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFFFCC00),
                    )
                    Text(
                        text = "📍 ${voucher.branchLabel}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Text(
                        text = "📦 ${voucher.pickupMethodLabel}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "💳 출금액: ${fmt.format(voucher.krwAmount)}원",
                        fontSize = 12.sp,
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun ExchangeVoucherQrDialog(
    voucher: ExchangeVoucherItem,
    onDismiss: () -> Unit,
    onCancelVoucher: ((ExchangeVoucherItem) -> Unit)? = null,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🎫 공항 무인환전기 QR 바우처",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExchangeVoucherQrTicketCard(voucher = voucher)
                Text(
                    text = "인천공항 환전 키오스크 바코드 리더기에 위 QR 코드를 스캔하고 본인 여권을 인식해 주세요. 수령 전이라면 언제든 환전 신청을 취소하고 출금액(${fmt.format(voucher.krwAmount)}원)을 환불받을 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("닫기")
            }
        },
        dismissButton = {
            if (onCancelVoucher != null) {
                OutlinedButton(
                    onClick = { onCancelVoucher(voucher) },
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(
                        text = "환전 취소 (${fmt.format(voucher.krwAmount)}원 환불)",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    )
}

/**
 * Real Native Account Ledger & Bankbook Copy Dialog when clicking 'DK 주거래 통장' or '입출금' on Home Screen.
 */
@Composable
fun NativeAccountLedgerDialog(
    bankingState: BankingAppState,
    onDismiss: () -> Unit,
    onAskAiAssistant: () -> Unit,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    var filterMode by rememberSaveable { mutableStateOf("ALL") }
    var showBankbookCopy by rememberSaveable { mutableStateOf(false) }

    val filteredTx =
        bankingState.transactions.filter { tx ->
            when (filterMode) {
                "IN" -> tx.amountDelta > 0
                "OUT" -> tx.amountDelta < 0
                else -> true
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "🏦 DK 주거래 통장",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "110-482-910284 · 금리 연 2.0%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showBankbookCopy = !showBankbookCopy }) {
                    Text(if (showBankbookCopy) "내역 보기" else "📄 통장사본")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Live Balance Banner
                Surface(
                    color = Color(0xFFF2F4F6),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "현재 출금가능 잔액",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7684),
                        )
                        Text(
                            text = "${fmt.format(bankingState.mainAccountBalance)}원",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF191F28),
                        )
                    }
                }

                AnimatedVisibility(visible = showBankbookCopy) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F3FF),
                            ),
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
                                fontSize = 14.sp,
                                color = Color(0xFF1B64DA),
                            )
                            Text("• 예금주: 박제창 (Dreamwalker)", fontSize = 12.sp)
                            Text("• 계좌종류: DK 주거래 입출금통장", fontSize = 12.sp)
                            Text("• 계좌번호: 110-482-910284", fontSize = 12.sp)
                            Text("• 발급일시: 2026.10.29 (직인생략)", fontSize = 12.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "전체 (${bankingState.transactions.size})", "IN" to "입금", "OUT" to "출금")
                        .forEach { (key, label) ->
                            FilterChip(
                                selected = filterMode == key,
                                onClick = { filterMode = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    filteredTx.forEach { tx ->
                        val isDeposit = tx.amountDelta > 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = "${tx.dateLabel} · ${tx.subtitle}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text =
                                        if (isDeposit) "+${fmt.format(tx.amountDelta)}원"
                                        else "${fmt.format(tx.amountDelta)}원",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isDeposit) Color(0xFF1B64DA) else Color(0xFF191F28),
                                )
                                Text(
                                    text = "잔액 ${fmt.format(tx.balanceAfter)}원",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }

                Surface(
                    color = Color(0xFF1B64DA).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onAskAiAssistant() },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "🤖 AI 금융비서에게 내역 분석·사본 요청하기",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B64DA),
                        )
                        Text("›", fontWeight = FontWeight.Bold, color = Color(0xFF1B64DA))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("닫기")
            }
        },
    )
}

/**
 * Real Native Quick Transfer Dialog when clicking '송금' or a recent recipient pill on Home Screen.
 */
@Composable
fun NativeQuickTransferDialog(
    initialRecipient: String,
    initialAmount: Long,
    bankingState: BankingAppState,
    onDismiss: () -> Unit,
    onOpenAiTransfer: () -> Unit,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    var recipient by rememberSaveable(initialRecipient) { mutableStateOf(initialRecipient) }
    var amountInput by rememberSaveable(initialAmount) { mutableStateOf(initialAmount.toString()) }
    var memoInput by rememberSaveable { mutableStateOf("모임회비") }
    var showBiometricStep by rememberSaveable { mutableStateOf(false) }
    var transferCompleted by rememberSaveable { mutableStateOf(false) }

    val parsedAmount = amountInput.filter { it.isDigit() }.toLongOrNull() ?: 0L

    if (showBiometricStep) {
        BiometricApprovalDialog(
            request =
                PendingBiometricRequest(
                    title = "${recipient}님께 간편 송금",
                    subtitle = "출금계좌: DK 주거래 통장 (수수료 면제)",
                    amountLabel = "${fmt.format(parsedAmount)}원",
                    deductionKrw = parsedAmount,
                    actionEventName = "native_transfer",
                    onApprove = {
                        bankingState.recordTransfer(
                            recipient = recipient,
                            amount = parsedAmount,
                            memo = memoInput,
                        )
                        showBiometricStep = false
                        transferCompleted = true
                    },
                    onCancel = { showBiometricStep = false },
                ),
            currentBalance = bankingState.mainAccountBalance,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transferCompleted) "✅ 송금 완료" else "💸 빠른 계좌 송금",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            if (transferCompleted) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${recipient}님께 ${fmt.format(parsedAmount)}원을 보냈습니다.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "거래 후 잔액: ${fmt.format(bankingState.mainAccountBalance)}원",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1B64DA),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("이지훈" to 240_000L, "김민수" to 120_000L, "최유진" to 80_000L).forEach { (name, presetAmt) ->
                            FilterChip(
                                selected = recipient == name,
                                onClick = {
                                    recipient = name
                                    amountInput = presetAmt.toString()
                                },
                                label = { Text("$name (${presetAmt / 10000}만)") },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("받는 분 성함 / 계좌") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val isOverBalance = parsedAmount > bankingState.mainAccountBalance
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { raw -> amountInput = raw.filter { it.isDigit() }.take(10) },
                        isError = isOverBalance,
                        label = { Text("보낼 금액 (원) · 잔액 ${fmt.format(bankingState.mainAccountBalance)}원") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isOverBalance) {
                        Text(
                            text =
                                "🚫 보유 잔액(${fmt.format(bankingState.mainAccountBalance)}원)을 초과하여 송금할 수 없습니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10_000L to "+1만", 50_000L to "+5만", 100_000L to "+10만").forEach { (delta, label) ->
                            OutlinedButton(
                                onClick = { amountInput = (parsedAmount + delta).toString() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = memoInput,
                        onValueChange = { memoInput = it },
                        label = { Text("받는 분 통장 표시") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Surface(
                        color = Color(0xFF1B64DA).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenAiTransfer() },
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "🤖 AI 챗봇으로 대화형 송금하기 (A2UI)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B64DA),
                            )
                            Text("›", fontWeight = FontWeight.Bold, color = Color(0xFF1B64DA))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (transferCompleted) {
                Button(onClick = onDismiss) {
                    Text("확인")
                }
            } else {
                Button(
                    onClick = { showBiometricStep = true },
                    enabled =
                        parsedAmount > 0L &&
                            parsedAmount <= bankingState.mainAccountBalance &&
                            recipient.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B64DA)),
                ) {
                    Text(
                        text =
                            if (parsedAmount > bankingState.mainAccountBalance) "잔액 부족"
                            else "${fmt.format(parsedAmount)}원 보내기",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            if (!transferCompleted) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        },
    )
}

/**
 * Real Native Card Statement & Instant Payment Dialog when clicking '11월 카드 명세서 · 즉시결제' on Home Screen.
 */
@Composable
fun NativeCardStatementDialog(
    bankingState: BankingAppState,
    onDismiss: () -> Unit,
    onOpenAiCardAssistant: () -> Unit,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val currentBill = bankingState.cardStatementBalance
    var usePoints by rememberSaveable { mutableStateOf(bankingState.rewardPoints >= 10_000L) }
    var showBiometricStep by rememberSaveable { mutableStateOf(false) }
    var paymentCompleted by rememberSaveable { mutableStateOf(false) }

    val pointDiscount = if (usePoints && bankingState.rewardPoints >= 10_000L && currentBill > 10_000L) 10_000L else 0L
    val finalPayAmount = (currentBill - pointDiscount).coerceAtLeast(0L)

    if (showBiometricStep) {
        BiometricApprovalDialog(
            request =
                PendingBiometricRequest(
                    title = "11월 카드 명세서 즉시결제",
                    subtitle =
                        if (pointDiscount > 0L) "포인트 10,000P 차감 적용 · 주거래 통장 출금"
                        else "DK 주거래 통장 전액 출금",
                    amountLabel = "${fmt.format(finalPayAmount)}원",
                    deductionKrw = finalPayAmount,
                    actionEventName = "native_card_bill",
                    onApprove = {
                        bankingState.recordCardBillPayment(
                            payAmount = currentBill,
                            usePoints = usePoints,
                        )
                        showBiometricStep = false
                        paymentCompleted = true
                    },
                    onCancel = { showBiometricStep = false },
                ),
            currentBalance = bankingState.mainAccountBalance,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (paymentCompleted || currentBill == 0L) "✅ 11월 명세서 결제 완료" else "💳 11월 카드 명세서 상세",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (paymentCompleted || currentBill == 0L) {
                    Text(
                        text = "11월 카드 결제 예정 금액이 모두 납부되어 남은 결제 금액이 0원입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "현재 주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B64DA),
                    )
                } else {
                    Surface(
                        color = Color(0xFFF2F4F6),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("11월 15일 결제 예정 원금", fontSize = 12.sp, color = Color(0xFF6B7684))
                            Text(
                                text = "${fmt.format(currentBill)}원",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            HorizontalDivider()
                            Text("• 대한항공 도쿄 왕복 항공권: 684,000원", fontSize = 12.sp)
                            Text("• 더현대 서울 백화점 (일시불): 892,314원", fontSize = 12.sp)
                            Text("• 쿠팡·일상 생활비 (42건): 585,000원", fontSize = 12.sp)
                        }
                    }

                    if (bankingState.rewardPoints >= 10_000L) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { usePoints = !usePoints },
                        ) {
                            Checkbox(
                                checked = usePoints,
                                onCheckedChange = { usePoints = it },
                            )
                            Text(
                                text = "보유 포인트 10,000P 차감 결제 (실출금 ${fmt.format(finalPayAmount)}원)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF1B64DA).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenAiCardAssistant() },
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "🤖 AI 챗봇으로 분할납부·결제 시뮬레이션 (A2UI)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B64DA),
                            )
                            Text("›", fontWeight = FontWeight.Bold, color = Color(0xFF1B64DA))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (paymentCompleted || currentBill == 0L) {
                Button(onClick = onDismiss) {
                    Text("확인")
                }
            } else {
                Button(
                    onClick = { showBiometricStep = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B64DA)),
                ) {
                    Text("${fmt.format(finalPayAmount)}원 즉시결제", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!paymentCompleted && currentBill > 0L) {
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        },
    )
}
