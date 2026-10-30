package com.droidknights.a2ui.scenarios.legacy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.droidknights.a2ui.common.ExchangeVoucherQrTicketCard
import com.droidknights.a2ui.common.LocalBankingAppState
import java.text.NumberFormat
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
sealed interface ExchangeLegacyNavKey : NavKey {
    val stepNumber: Int
    val breadcrumb: String

    @Serializable
    data object Step1BankMenu : ExchangeLegacyNavKey {
        override val stepNumber = 1
        override val breadcrumb = "1/7단계 · 외환 메뉴"
    }

    @Serializable
    data object Step2SelectCurrency : ExchangeLegacyNavKey {
        override val stepNumber = 2
        override val breadcrumb = "2/7단계 · 통화 선택"
    }

    @Serializable
    data object Step3EnterAmount : ExchangeLegacyNavKey {
        override val stepNumber = 3
        override val breadcrumb = "3/7단계 · 금액 입력"
    }

    @Serializable
    data object Step4PickupBranch : ExchangeLegacyNavKey {
        override val stepNumber = 4
        override val breadcrumb = "4/7단계 · 수령 지점"
    }

    @Serializable
    data object Step5AccountAndDenom : ExchangeLegacyNavKey {
        override val stepNumber = 5
        override val breadcrumb = "5/7단계 · 출금계좌·권종"
    }

    @Serializable
    data object Step6TermsAgreement : ExchangeLegacyNavKey {
        override val stepNumber = 6
        override val breadcrumb = "6/7단계 · 약관 동의"
    }

    @Serializable
    data object Step7AccountPin : ExchangeLegacyNavKey {
        override val stepNumber = 7
        override val breadcrumb = "7/7단계 · 비밀번호 입력"
    }

    @Serializable
    data object Completed : ExchangeLegacyNavKey {
        override val stepNumber = 7
        override val breadcrumb = "환전 신청 완료"
    }
}

@Composable
fun LegacyCurrencyExchangeScreen() {
    val bankingState = LocalBankingAppState.current
    val fmt = androidx.compose.runtime.remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val navBackStack = rememberNavBackStack(ExchangeLegacyNavKey.Step1BankMenu)
    val currentStep = (navBackStack.lastOrNull() as? ExchangeLegacyNavKey) ?: ExchangeLegacyNavKey.Step1BankMenu
    var touchCount by rememberSaveable { mutableIntStateOf(0) }

    var selectedCurrency by rememberSaveable { mutableStateOf("JPY (일본 엔)") }
    var exchangeAmount by rememberSaveable { mutableFloatStateOf(50000f) }
    var apply90Coupon by rememberSaveable { mutableStateOf(true) }
    var selectedBranch by rememberSaveable { mutableStateOf("인천국제공항 제1터미널") }
    var selectedAccount by rememberSaveable { mutableStateOf("주거래 급여통장 (110-482-******)") }
    var smallBillRatio by rememberSaveable { mutableFloatStateOf(30f) }
    var agreeTerm1 by rememberSaveable { mutableStateOf(false) }
    var agreeTerm2 by rememberSaveable { mutableStateOf(false) }
    var accountPin by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegacyNavigationHeader(
            missionTitle = "모바일 뱅킹 환전 (Navigation 3 · 7단계 플로우)",
            painPointDesc = "",
            breadcrumb = currentStep.breadcrumb,
            touchCount = touchCount,
            canGoBack = navBackStack.size > 1,
            onBack = {
                if (navBackStack.size > 1) {
                    touchCount++
                    navBackStack.removeLastOrNull()
                }
            },
            onReset = {
                touchCount = 0
                agreeTerm1 = false
                agreeTerm2 = false
                accountPin = ""
                while (navBackStack.size > 1) {
                    navBackStack.removeLastOrNull()
                }
            },
        )

        LinearProgressIndicator(
            progress = { currentStep.stepNumber / 7f },
            modifier = Modifier.fillMaxWidth(),
        )

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NavDisplay(
                    backStack = navBackStack,
                    onBack = {
                        if (navBackStack.size > 1) {
                            touchCount++
                            navBackStack.removeLastOrNull()
                        }
                    },
                    entryProvider =
                        entryProvider {
                            entry<ExchangeLegacyNavKey.Step1BankMenu> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "외환 메뉴 선택",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    listOf(
                                        "💴 외화 환전 신청" to "공항·영업점 수령",
                                        "🌍 해외 송금 보내기" to "수취인 정보 입력",
                                        "💳 외화카드 충전" to "외화 계좌 연결",
                                    ).forEach { (title, sub) ->
                                        OutlinedCard(
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .clickable {
                                                        touchCount++
                                                        navBackStack.add(ExchangeLegacyNavKey.Step2SelectCurrency)
                                                    },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    Text(
                                                        text = sub,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            entry<ExchangeLegacyNavKey.Step2SelectCurrency> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "환전 통화 선택",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    listOf(
                                        "JPY (일본 엔) · 912.40원",
                                        "USD (미국 달러) · 1,384.50원",
                                        "EUR (유럽 유로) · 1,492.10원",
                                        "CHF (스위스 프랑) · 1,568.00원",
                                    ).forEach { currency ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier =
                                                Modifier.fillMaxWidth().clickable {
                                                    touchCount++
                                                    selectedCurrency = currency.substringBefore(" ·")
                                                },
                                        ) {
                                            RadioButton(
                                                selected = currency.startsWith(selectedCurrency),
                                                onClick = {
                                                    touchCount++
                                                    selectedCurrency = currency.substringBefore(" ·")
                                                },
                                            )
                                            Text(currency, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            touchCount++
                                            navBackStack.add(ExchangeLegacyNavKey.Step3EnterAmount)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("다음: 금액 입력 (3/7단계)")
                                    }
                                }
                            }

                            entry<ExchangeLegacyNavKey.Step3EnterAmount> {
                                val ratePerUnit =
                                    when {
                                        selectedCurrency.startsWith("JPY") -> 9.0412f
                                        selectedCurrency.startsWith("USD") -> 1384.5f
                                        selectedCurrency.startsWith("EUR") -> 1492.1f
                                        else -> 1568.0f
                                    }
                                var foreignInput by rememberSaveable(selectedCurrency) {
                                    mutableStateOf(exchangeAmount.toInt().toString())
                                }
                                var krwInput by rememberSaveable(selectedCurrency) {
                                    mutableStateOf((exchangeAmount * ratePerUnit).toInt().toString())
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "환전 금액 입력 (외화 ↔ 원화 자동 계산)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    OutlinedTextField(
                                        value = foreignInput,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }.take(9)
                                            foreignInput = digits
                                            val parsed = digits.toFloatOrNull() ?: 0f
                                            exchangeAmount = parsed
                                            krwInput = (parsed * ratePerUnit).toInt().toString()
                                            touchCount++
                                        },
                                        label = { Text("환전할 외화 ($selectedCurrency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = krwInput,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }.take(10)
                                            krwInput = digits
                                            val parsedKrw = digits.toFloatOrNull() ?: 0f
                                            val calcForeign = (parsedKrw / ratePerUnit)
                                            exchangeAmount = calcForeign
                                            foreignInput = calcForeign.toInt().toString()
                                            touchCount++
                                        },
                                        label = { Text("출금될 원화 (KRW)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = apply90Coupon,
                                            onCheckedChange = {
                                                touchCount++
                                                apply90Coupon = it
                                            },
                                        )
                                        Text("90% 환율 우대 쿠폰 적용", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Button(
                                        onClick = {
                                            touchCount++
                                            navBackStack.add(ExchangeLegacyNavKey.Step4PickupBranch)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("다음: 수령 지점 선택 (4/7단계)")
                                    }
                                }
                            }

                            entry<ExchangeLegacyNavKey.Step4PickupBranch> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "수령 지점 선택",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    listOf(
                                        "인천국제공항 제1터미널",
                                        "인천국제공항 제2터미널",
                                        "김포국제공항 국제선",
                                        "시내 영업점 방문",
                                    ).forEach { branch ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier =
                                                Modifier.fillMaxWidth().clickable {
                                                    touchCount++
                                                    selectedBranch = branch
                                                },
                                        ) {
                                            RadioButton(
                                                selected = selectedBranch == branch,
                                                onClick = {
                                                    touchCount++
                                                    selectedBranch = branch
                                                },
                                            )
                                            Text(branch, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            touchCount++
                                            navBackStack.add(ExchangeLegacyNavKey.Step5AccountAndDenom)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("다음: 출금 계좌·권종 (5/7단계)")
                                    }
                                }
                            }

                            entry<ExchangeLegacyNavKey.Step5AccountAndDenom> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "출금 계좌 및 권종 선택",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    listOf(
                                        "주거래 급여통장 (110-482-******)",
                                        "생활비 통장 (333-910-******)",
                                    ).forEach { acc ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier =
                                                Modifier.fillMaxWidth().clickable {
                                                    touchCount++
                                                    selectedAccount = acc
                                                },
                                        ) {
                                            RadioButton(
                                                selected = selectedAccount == acc,
                                                onClick = {
                                                    touchCount++
                                                    selectedAccount = acc
                                                },
                                            )
                                            Text(acc, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Text(
                                        text = "소액권 포함 비율: ${smallBillRatio.toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Slider(
                                        value = smallBillRatio,
                                        onValueChange = {
                                            touchCount++
                                            smallBillRatio = it
                                        },
                                        valueRange = 0f..50f,
                                    )
                                    Button(
                                        onClick = {
                                            touchCount++
                                            navBackStack.add(ExchangeLegacyNavKey.Step6TermsAgreement)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("다음: 약관 동의 (6/7단계)")
                                    }
                                }
                            }

                            entry<ExchangeLegacyNavKey.Step6TermsAgreement> {
                                val bothAgreed = agreeTerm1 && agreeTerm2
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "약관 및 수령 유의사항 동의",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                            Modifier.fillMaxWidth().clickable {
                                                touchCount++
                                                agreeTerm1 = !agreeTerm1
                                            },
                                    ) {
                                        Checkbox(
                                            checked = agreeTerm1,
                                            onCheckedChange = {
                                                touchCount++
                                                agreeTerm1 = it
                                            },
                                        )
                                        Text("[필수] 외화 환전 거래 기본약관 동의", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                            Modifier.fillMaxWidth().clickable {
                                                touchCount++
                                                agreeTerm2 = !agreeTerm2
                                            },
                                    ) {
                                        Checkbox(
                                            checked = agreeTerm2,
                                            onCheckedChange = {
                                                touchCount++
                                                agreeTerm2 = it
                                            },
                                        )
                                        Text("[필수] 수령 시 본인 여권 지참 확인", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (!bothAgreed) {
                                        Text(
                                            text = "⚠️ 필수 약관 2개 항목에 모두 체크해야 다음 단계로 이동할 수 있습니다.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            touchCount++
                                            navBackStack.add(ExchangeLegacyNavKey.Step7AccountPin)
                                        },
                                        enabled = bothAgreed,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("다음: 계좌 비밀번호 입력 (7/7단계)")
                                    }
                                }
                            }

                            entry<ExchangeLegacyNavKey.Step7AccountPin> {
                                val isPinValid = accountPin.length == 4
                                val ratePerUnit =
                                    when {
                                        selectedCurrency.startsWith("JPY") -> 9.0412f
                                        selectedCurrency.startsWith("USD") -> 1384.5f
                                        selectedCurrency.startsWith("EUR") -> 1492.1f
                                        else -> 1568.0f
                                    }
                                val krwCost = (exchangeAmount * ratePerUnit).toLong()
                                val currencyCode = selectedCurrency.take(3)
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "계좌 비밀번호 입력",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "$selectedCurrency ${fmt.format(exchangeAmount.toLong())} (출금 ${fmt.format(krwCost)}원) · $selectedBranch",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedTextField(
                                        value = accountPin,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }.take(4)
                                            if (digits != accountPin) {
                                                touchCount++
                                                accountPin = digits
                                            }
                                        },
                                        label = { Text("비밀번호 숫자 4자리") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions =
                                            KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (!isPinValid) {
                                        Text(
                                            text = "숫자 4자리를 입력해 주세요 (${accountPin.length}/4)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            touchCount++
                                            bankingState.recordCurrencyExchange(
                                                currency = currencyCode,
                                                foreignAmount = exchangeAmount.toLong(),
                                                krwAmount = krwCost,
                                                branchLabel = selectedBranch,
                                                pickupMethodLabel = "모바일 QR 바우처 (7단계 신청)",
                                            )
                                            navBackStack.add(ExchangeLegacyNavKey.Completed)
                                        },
                                        enabled = isPinValid,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("환전 신청 완료")
                                    }
                                }
                            }

                            entry<ExchangeLegacyNavKey.Completed> {
                                val latestVoucher = bankingState.exchangeVouchers.firstOrNull()
                                Card(
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = "환전 신청 완료 (7단계 · 터치 ${touchCount}회)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Text(
                                            text = "$selectedCurrency ${fmt.format(exchangeAmount.toLong())} · $selectedBranch",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Text(
                                            text = "출금 후 주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        if (latestVoucher != null) {
                                            ExchangeVoucherQrTicketCard(voucher = latestVoucher)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                touchCount = 0
                                                accountPin = ""
                                                while (navBackStack.size > 1) {
                                                    navBackStack.removeLastOrNull()
                                                }
                                            },
                                        ) {
                                            Text("처음으로")
                                        }
                                    }
                                }
                            }
                        },
                )
            }
        }
    }
}
