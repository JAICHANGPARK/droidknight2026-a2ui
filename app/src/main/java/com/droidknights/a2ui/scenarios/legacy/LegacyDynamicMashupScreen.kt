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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private sealed interface MashupNavNode {
    val breadcrumb: String

    data object HomeMenu : MashupNavNode {
        override val breadcrumb = "전체 메뉴"
    }

    data object ExchangeStep1Currency : MashupNavNode {
        override val breadcrumb = "외환 > 통화 선택"
    }

    data object ExchangeStep2Amount : MashupNavNode {
        override val breadcrumb = "외환 > 금액 입력"
    }

    data object CardStep1Select : MashupNavNode {
        override val breadcrumb = "카드 > 대상 카드 선택"
    }

    data object CardStep2OverseasLock : MashupNavNode {
        override val breadcrumb = "카드 > 해외결제 차단 해제"
    }

    data object InsStep1Plan : MashupNavNode {
        override val breadcrumb = "보험 > 플랜 선택"
    }

    data object InsStep2Agree : MashupNavNode {
        override val breadcrumb = "보험 > 약관 동의"
    }

    data object ClaimStep1Baggage : MashupNavNode {
        override val breadcrumb = "클레임 접수 (1/4단계)"
    }

    data object ClaimStep2FlightDelay : MashupNavNode {
        override val breadcrumb = "클레임 접수 (2/4단계)"
    }

    data object ClaimStep3Medical : MashupNavNode {
        override val breadcrumb = "클레임 접수 (3/4단계)"
    }

    data object ClaimStep4Submit : MashupNavNode {
        override val breadcrumb = "클레임 접수 완료"
    }

    data object DutchPayEqualOnly : MashupNavNode {
        override val breadcrumb = "정산 > 1/N 더치페이"
    }
}

@Composable
fun LegacyDynamicMashupScreen() {
    val navStack = remember { mutableStateListOf<MashupNavNode>(MashupNavNode.HomeMenu) }
    val currentNode = navStack.last()
    var touchCount by remember { mutableIntStateOf(0) }

    var exchangeDone by remember { mutableStateOf(false) }
    var cardDone by remember { mutableStateOf(false) }
    var insDone by remember { mutableStateOf(false) }

    var jpyAmount by remember { mutableFloatStateOf(50000f) }
    var unlockOverseas by remember { mutableStateOf(true) }
    var agreeTerms by remember { mutableStateOf(false) }

    var baggageTag by remember { mutableStateOf("NH-884192") }
    var delayHours by remember { mutableFloatStateOf(0f) }
    var hospitalName by remember { mutableStateOf("") }

    var headCount by remember { mutableFloatStateOf(5f) }

    val travelDoneCount = listOf(exchangeDone, cardDone, insDone).count { it }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegacyNavigationHeader(
            missionTitle = "출국 준비 ($travelDoneCount/3 완료)",
            painPointDesc = "",
            breadcrumb = currentNode.breadcrumb,
            touchCount = touchCount,
            canGoBack = navStack.size > 1,
            onBack = {
                if (navStack.size > 1) {
                    touchCount++
                    navStack.removeAt(navStack.lastIndex)
                }
            },
            onReset = {
                touchCount = 0
                exchangeDone = false
                cardDone = false
                insDone = false
                navStack.clear()
                navStack.add(MashupNavNode.HomeMenu)
            },
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (currentNode) {
                    MashupNavNode.HomeMenu -> {
                        Text(
                            text = "서비스 메뉴",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LegacyMenuCard(
                            title = "💴 외화 환전 신청",
                            subtitle = if (exchangeDone) "50,000 JPY 신청 완료" else "통화 선택 → 금액 입력",
                            isDone = exchangeDone,
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.ExchangeStep1Currency)
                            },
                        )
                        LegacyMenuCard(
                            title = "💳 해외결제 차단 해제",
                            subtitle = if (cardDone) "잠금 해제 완료" else "카드 선택 → 해외결제 설정",
                            isDone = cardDone,
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.CardStep1Select)
                            },
                        )
                        LegacyMenuCard(
                            title = "🛡️ 해외여행자 보험 가입",
                            subtitle = if (insDone) "가입 완료" else "플랜 선택 → 약관 동의",
                            isDone = insDone,
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.InsStep1Plan)
                            },
                        )
                        LegacyMenuCard(
                            title = "📋 고객 클레임 접수 (4단계 고정 폼)",
                            subtitle = "모든 단계를 순서대로 거쳐야 제출 가능",
                            isDone = false,
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.ClaimStep1Baggage)
                            },
                        )
                        LegacyMenuCard(
                            title = "🍺 모임 1/N 정산하기",
                            subtitle = "균등 1/N 나누기 전용",
                            isDone = false,
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.DutchPayEqualOnly)
                            },
                        )
                    }

                    MashupNavNode.ExchangeStep1Currency -> {
                        Text(
                            text = "환전 통화 선택 (1/2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.ExchangeStep2Amount)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("일본 엔화 (JPY) 선택")
                        }
                    }

                    MashupNavNode.ExchangeStep2Amount -> {
                        Text(
                            text = "환전 금액: ${jpyAmount.toInt()} JPY (2/2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Slider(
                            value = jpyAmount,
                            onValueChange = {
                                touchCount++
                                jpyAmount = it
                            },
                            valueRange = 10000f..200000f,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                exchangeDone = true
                                navStack.clear()
                                navStack.add(MashupNavNode.HomeMenu)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("신청 후 전체 메뉴로")
                        }
                    }

                    MashupNavNode.CardStep1Select -> {
                        Text(
                            text = "대상 카드 선택 (1/2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.CardStep2OverseasLock)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("신용카드 (4219-****) 선택")
                        }
                    }

                    MashupNavNode.CardStep2OverseasLock -> {
                        Text(
                            text = "해외결제 차단 해제 (2/2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = unlockOverseas,
                                onCheckedChange = {
                                    touchCount++
                                    unlockOverseas = it
                                },
                            )
                            Text("해외 결제 잠금 해제 및 DCC 차단", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = {
                                touchCount++
                                cardDone = true
                                navStack.clear()
                                navStack.add(MashupNavNode.HomeMenu)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("저장 후 전체 메뉴로")
                        }
                    }

                    MashupNavNode.InsStep1Plan -> {
                        Text(
                            text = "여행자보험 플랜 선택 (1/2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.InsStep2Agree)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("실속형 플랜 (4,200원) 선택")
                        }
                    }

                    MashupNavNode.InsStep2Agree -> {
                        Text(
                            text = "약관 동의 및 가입 (2/2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = agreeTerms,
                                onCheckedChange = {
                                    touchCount++
                                    agreeTerms = it
                                },
                            )
                            Text("여행자보험 필수 약관 동의", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = {
                                touchCount++
                                insDone = true
                                navStack.clear()
                                navStack.add(MashupNavNode.HomeMenu)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("가입 완료 후 전체 메뉴로")
                        }
                    }

                    MashupNavNode.ClaimStep1Baggage -> {
                        Text(
                            text = "1/4단계 · 물품/이물질 번호",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = baggageTag,
                            onValueChange = { baggageTag = it },
                            label = { Text("접수 번호 (해당 없으면 건너뛰기)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.ClaimStep2FlightDelay)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("다음 (2/4단계)")
                        }
                    }

                    MashupNavNode.ClaimStep2FlightDelay -> {
                        Text(
                            text = "2/4단계 · 지연 시간 입력",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "지연 시간: ${delayHours.toInt()}시간",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = delayHours,
                            onValueChange = {
                                touchCount++
                                delayHours = it
                            },
                            valueRange = 0f..24f,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.ClaimStep3Medical)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("다음 (3/4단계)")
                        }
                    }

                    MashupNavNode.ClaimStep3Medical -> {
                        Text(
                            text = "3/4단계 · 추가 증빙 입력",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = hospitalName,
                            onValueChange = { hospitalName = it },
                            label = { Text("상세 메모 (해당 없으면 비워두기)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(MashupNavNode.ClaimStep4Submit)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("다음 (4/4단계)")
                        }
                    }

                    MashupNavNode.ClaimStep4Submit -> {
                        Text(
                            text = "접수 완료 (터치 ${touchCount}회)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedButton(
                            onClick = {
                                touchCount++
                                navStack.clear()
                                navStack.add(MashupNavNode.HomeMenu)
                            },
                        ) {
                            Text("전체 메뉴로 돌아가기")
                        }
                    }

                    MashupNavNode.DutchPayEqualOnly -> {
                        val perPerson = (180000 / headCount.toInt().coerceAtLeast(1))
                        Text(
                            text = "1/N 균등 정산",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "총 180,000원 / ${headCount.toInt()}명 = 1인당 ${perPerson}원",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = headCount,
                            onValueChange = {
                                touchCount++
                                headCount = it
                            },
                            valueRange = 2f..10f,
                        )
                        Text(
                            text = "비음주자 차등 감면 옵션은 지원되지 않습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedButton(
                            onClick = {
                                touchCount++
                                navStack.clear()
                                navStack.add(MashupNavNode.HomeMenu)
                            },
                        ) {
                            Text("전체 메뉴로 돌아가기")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyMenuCard(
    title: String,
    subtitle: String,
    isDone: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick),
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
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isDone) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
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
