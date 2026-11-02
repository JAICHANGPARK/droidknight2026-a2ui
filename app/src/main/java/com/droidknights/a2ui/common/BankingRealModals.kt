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

data class PendingTermsAgreementRequest(
    val agreeForexChecked: Boolean,
    val agreePickupChecked: Boolean,
    val onAgreeAllAndProceed: () -> Unit,
    val onDismissToManualCheck: () -> Unit,
)

data class PendingDenominationRuleRequest(
    val currency: String,
    val requestedAmount: Long,
    val adjustedBanknoteAmount: Long,
    val adjustedKrwAmount: Long,
    val ruleExplanation: String,
    val onAutoAdjustAndProceed: () -> Unit,
    val onDismissToEdit: () -> Unit,
)

/**
 * Banknote Denomination & Minimum Exchange Rule dialog shown when the user enters a coin unit
 * (e.g., 500 JPY, 1,500 JPY, 3 EUR, 5 CHF) or below-minimum amount and taps `환전 신청하기`.
 */
@Composable
fun ExchangeDenominationRuleDialog(
    request: PendingDenominationRuleRequest,
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    AlertDialog(
        onDismissRequest = request.onDismissToEdit,
        containerColor = Color.White,
        titleContentColor = Color(0xFF191F28),
        textContentColor = Color(0xFF191F28),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier.size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9381E).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "💴", fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = "외화 지폐 권종 단위 안내 (동전 환전 불가)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191F28),
                    )
                    Text(
                        text = "시중은행 및 공항 수령은 지폐 최소 권종 단위로만 가능합니다",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4E5968),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = Color(0xFFFFF3F0),
                    contentColor = Color(0xFF191F28),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text =
                                "🚫 입력하신 금액: ${fmt.format(request.requestedAmount)} ${request.currency}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD9381E),
                        )
                        Text(
                            text = request.ruleExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF191F28),
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF2F4F6),
                    contentColor = Color(0xFF191F28),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "🏦 통화별 실제 지폐 권종 및 최소 신청 기준",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191F28),
                        )
                        Text(
                            text =
                                "• 🇯🇵 일본 엔 (JPY): 최소 1,000엔 · 1,000엔 지폐 단위\n" +
                                    "  (500엔·100엔·50엔·10엔·5엔·1엔은 동전이므로 환전·수령 불가)\n" +
                                    "• 🇺🇸 미국 달러 (USD): 최소 10달러 · 1달러 지폐 단위\n" +
                                    "  (센트 동전 환전 불가 · 공항 ATM은 10·100달러권 수령)\n" +
                                    "• 🇪🇺 유럽 유로 (EUR): 최소 10유로 · 5유로 지폐 단위\n" +
                                    "  (1유로·2유로는 동전이므로 지폐 환전 불가)\n" +
                                    "• 🇨🇭 스위스 프랑 (CHF): 최소 10프랑 · 10프랑 지폐 단위\n" +
                                    "  (5프랑·2프랑·1프랑 이하는 동전이므로 지폐 환전 불가)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E5968),
                        )
                    }
                }

                Surface(
                    color = Color(0xFFE8F3FF),
                    contentColor = Color(0xFF191F28),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "✅ 지폐 단위 자동 보정 금액",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B64DA),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text =
                                "${fmt.format(request.adjustedBanknoteAmount)} ${request.currency} (약 ${fmt.format(request.adjustedKrwAmount)}원)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B64DA),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = request.onAutoAdjustAndProceed,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B64DA),
                        contentColor = Color.White,
                    ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "${fmt.format(request.adjustedBanknoteAmount)} ${request.currency}로 보정 후 계속",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = request.onDismissToEdit) {
                Text(
                    text = "직접 금액 수정하기",
                    color = Color(0xFF4E5968),
                )
            }
        },
    )
}

/**
 * Mandatory Foreign Exchange Terms & Airport Pickup Agreement dialog shown when the user taps
 * `환전 신청하기` without checking both mandatory A2UI CheckBoxes (`terms_forex_chk`, `terms_pickup_chk`).
 */
@Composable
fun ExchangeTermsAgreementDialog(
    request: PendingTermsAgreementRequest,
) {
    AlertDialog(
        onDismissRequest = request.onDismissToManualCheck,
        containerColor = Color.White,
        titleContentColor = Color(0xFF191F28),
        textContentColor = Color(0xFF191F28),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier.size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B64DA).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "📋", fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = "외화 환전 필수 약관 동의",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191F28),
                    )
                    Text(
                        text = "A2UI 카드 하단의 필수 동의 약관 2건을 확인해 주세요",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4E5968),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = Color(0xFFFFF8E1),
                    contentColor = Color(0xFF191F28),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text =
                            "⚠️ 필수 약관 체크박스가 선택되지 않았습니다. 아래 약관 내용을 읽으신 뒤 직접 체크하시거나 '전체 동의 후 계속'을 눌러주세요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp),
                    )
                }

                Surface(
                    color = Color(0xFFF2F4F6),
                    contentColor = Color(0xFF191F28),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "1. [필수] 외국환거래법 및 환전 유의사항",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191F28),
                            )
                            Text(
                                text = if (request.agreeForexChecked) "동의함 ✅" else "미동의 ⬜",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (request.agreeForexChecked) Color(0xFF008A5E)
                                    else Color(0xFFD9381E),
                            )
                        }
                        Text(
                            text =
                                "• 비대면 인터넷 환전은 건당 미화 1만 불 상당액 이하 범위 내에서 신청 가능하며, 신청 완료 시점의 고시 환율(최대 90% 우대)이 즉시 적용됩니다.\n" +
                                    "• 동일자 합산 미화 1만 불 초과 환전 시 외국환거래규정에 따라 국세청 및 관세청에 자동 통보됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E5968),
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF2F4F6),
                    contentColor = Color(0xFF191F28),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "2. [필수] 공항 여권 지참 및 환불 규정",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191F28),
                            )
                            Text(
                                text = if (request.agreePickupChecked) "동의함 ✅" else "미동의 ⬜",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (request.agreePickupChecked) Color(0xFF008A5E)
                                    else Color(0xFFD9381E),
                            )
                        }
                        Text(
                            text =
                                "• 인천국제공항(제1·2여객터미널) 무인환전기 및 창구 수령 시 본인 실물 여권과 모바일 QR 바우처를 반드시 지참하셔야 합니다 (타인 대리 수령 불가).\n" +
                                    "• 외화 실물 수령 전까지는 앱 내 바우처 화면에서 언제든 신청 취소 및 원화 출금액 100% 즉시 환불이 가능합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E5968),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = request.onAgreeAllAndProceed,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B64DA),
                        contentColor = Color.White,
                    ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "약관 확인 · 전체 동의 후 계속",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = request.onDismissToManualCheck) {
                Text(
                    text = "닫고 직접 체크하기",
                    color = Color(0xFF4E5968),
                )
            }
        },
    )
}

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
        containerColor = Color.White,
        titleContentColor = Color(0xFF191F28),
        textContentColor = Color(0xFF191F28),
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
 * Genuine ISO/IEC 18004 Error-Correction-Level-M QR Code bit matrices for real camera scanning.
 */
enum class VoucherQrLinkTarget(
    val label: String,
    val url: String,
    val matrixRows: List<String>,
) {
    GITHUB_REPO(
        label = "🐙 GitHub 레포지토리",
        url = "https://github.com/JAICHANGPARK/droidknight2026-a2ui",
        matrixRows =
            listOf(
                "111111100111101110101101001111111",
                "100000100010100110101100001000001",
                "101110101000101100100100001011101",
                "101110101110000100100000001011101",
                "101110101000001111100111101011101",
                "100000101101110000101000101000001",
                "111111101010101010101010101111111",
                "000000001010000010000101100000000",
                "101111100101001000010000001111100",
                "110110011010110111101101001101111",
                "000110111000011101000000111010110",
                "111011010010010101110100101011100",
                "100000101011101110010001010111010",
                "000100001101110100111011101000111",
                "011110101101111111101010111010010",
                "100001000110011100001100011111100",
                "011000110011100110000010110110001",
                "000010001000001110101101001101101",
                "010110100011011110101010101110110",
                "110110000101101010011100010111111",
                "110010101011110100111000010111011",
                "110010010011100111011101001001001",
                "101000110010101000100000110101010",
                "100110001001111010110110111001100",
                "101100111011111000000011111110010",
                "000000001110011111110001100010101",
                "111111100101001101001111101010100",
                "100000101010101011101111100011111",
                "101110101111011010011010111111001",
                "101110101001110100110001110010011",
                "101110101101101110101010001101100",
                "100000100111100100000110010011100",
                "111111101100001100000011111100010",
            ),
    ),
    LINKEDIN_PROFILE(
        label = "💼 LinkedIn 프로필",
        url = "https://www.linkedin.com/in/park-jaichang/",
        matrixRows =
            listOf(
                "11111110001111100010001111111",
                "10000010111011001100101000001",
                "10111010111100110001001011101",
                "10111010101011011100101011101",
                "10111010001101001111101011101",
                "10000010001000001000001000001",
                "11111110101010101010101111111",
                "00000000111110000001100000000",
                "10000010101111110111011001110",
                "10110000111100110111100110110",
                "01111011010100010100001000000",
                "00101101011100110001100101000",
                "11010111000010001000101100001",
                "01110001110111011111101010011",
                "01100111000101111010111101100",
                "01100100111110100000111110101",
                "11101110000001100101000101100",
                "10111101110110010111101110111",
                "11001110110000010001100001001",
                "10000001000000110010101110000",
                "10011111110101000110111110111",
                "00000000111001110110100011000",
                "11111110011011110101101011100",
                "10000010010110110001100010001",
                "10111010010001110001111111010",
                "10111010010100011001010101101",
                "10111010001111011011011111110",
                "10000010011001001010000101101",
                "11111110101101110101001111100",
            ),
    ),
}

@Composable
fun ScannableIsoQrCodeCanvas(
    target: VoucherQrLinkTarget,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.padding(10.dp)) {
            val rows = target.matrixRows
            val gridSize = rows.size
            val cell = size.width / gridSize
            // Overlap by 0.6px so adjacent black modules have zero sub-pixel gaps for camera scanners
            val moduleSize = Size(cell + 0.6f, cell + 0.6f)
            for (r in 0 until gridSize) {
                val rowStr = rows[r]
                for (c in 0 until rowStr.length) {
                    if (rowStr[c] == '1') {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cell, r * cell),
                            size = moduleSize,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual QR Code Ticket Card for Airport Kiosk Foreign Currency Pickup.
 */
@Composable
fun ExchangeVoucherQrTicketCard(
    voucher: ExchangeVoucherItem,
    modifier: Modifier = Modifier,
    qrTarget: VoucherQrLinkTarget =
        if ((voucher.voucherCode.lastOrNull()?.digitToIntOrNull() ?: 0) % 2 == 0) {
            VoucherQrLinkTarget.GITHUB_REPO
        } else {
            VoucherQrLinkTarget.LINKEDIN_PROFILE
        },
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
                // Real Camera-Scannable ISO/IEC 18004 QR Code
                ScannableIsoQrCodeCanvas(
                    target = qrTarget,
                    modifier = Modifier.size(104.dp),
                )

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
                    Text(
                        text = "🔗 QR 스캔: ${qrTarget.label}",
                        fontSize = 11.sp,
                        color = Color(0xFF34D399),
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
    val defaultTarget =
        if ((voucher.voucherCode.lastOrNull()?.digitToIntOrNull() ?: 0) % 2 == 0) {
            VoucherQrLinkTarget.GITHUB_REPO
        } else {
            VoucherQrLinkTarget.LINKEDIN_PROFILE
        }
    var selectedTarget by remember { mutableStateOf(defaultTarget) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color(0xFF191F28),
        textContentColor = Color(0xFF191F28),
        title = {
            Text(
                text = "🎫 공항 무인환전기 QR 바우처",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VoucherQrLinkTarget.entries.forEach { target ->
                        FilterChip(
                            selected = selectedTarget == target,
                            onClick = { selectedTarget = target },
                            label = {
                                Text(
                                    text = target.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Large crisp camera-scannable QR code in modal
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191F28)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "${fmt.format(voucher.foreignAmount)} ${voucher.currency} · ${voucher.voucherCode}",
                            color = Color(0xFFFFCC00),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                        ScannableIsoQrCodeCanvas(
                            target = selectedTarget,
                            modifier = Modifier.size(210.dp),
                        )
                        Text(
                            text = selectedTarget.url,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF93C5FD),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "📍 ${voucher.branchLabel} · ${voucher.pickupMethodLabel}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }

                Text(
                    text = "스마트폰 기본 카메라로 위 QR 코드를 스캔하면 실제 링크(${selectedTarget.label})가 즉시 열립니다. 수령 전이라면 언제든 환전 신청을 취소하고 출금액(${fmt.format(voucher.krwAmount)}원)을 환불받을 수 있습니다.",
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
        containerColor = Color.White,
        titleContentColor = Color(0xFF191F28),
        textContentColor = Color(0xFF191F28),
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
                    contentColor = Color(0xFF191F28),
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
                                contentColor = Color(0xFF191F28),
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
                            Text("• 예금주: 박제창 (Dreamwalker)", fontSize = 12.sp, color = Color(0xFF191F28))
                            Text("• 계좌종류: DK 주거래 입출금통장", fontSize = 12.sp, color = Color(0xFF191F28))
                            Text("• 계좌번호: 110-482-910284", fontSize = 12.sp, color = Color(0xFF191F28))
                            Text("• 발급일시: 2026.10.29 (직인생략)", fontSize = 12.sp, color = Color(0xFF191F28))
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
        containerColor = Color.White,
        titleContentColor = Color(0xFF191F28),
        textContentColor = Color(0xFF191F28),
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
        containerColor = Color.White,
        titleContentColor = Color(0xFF191F28),
        textContentColor = Color(0xFF191F28),
        title = {
            Text(
                text = if (paymentCompleted || currentBill == 0L) "✅ 11월 명세서 결제 완료" else "💳 11월 카드 명세서 상세",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191F28),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (paymentCompleted || currentBill == 0L) {
                    Text(
                        text = "11월 카드 결제 예정 금액이 모두 납부되어 남은 결제 금액이 0원입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF191F28),
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
                        contentColor = Color(0xFF191F28),
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
                                color = Color(0xFF191F28),
                            )
                            HorizontalDivider(color = Color(0xFFE5E8EB))
                            Text("• 대한항공 도쿄 왕복 항공권: 684,000원", fontSize = 12.sp, color = Color(0xFF191F28))
                            Text("• 더현대 서울 백화점 (일시불): 892,314원", fontSize = 12.sp, color = Color(0xFF191F28))
                            Text("• 쿠팡·일상 생활비 (42건): 585,000원", fontSize = 12.sp, color = Color(0xFF191F28))
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
                                color = Color(0xFF191F28),
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
