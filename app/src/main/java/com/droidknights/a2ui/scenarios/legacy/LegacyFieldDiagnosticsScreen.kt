package com.droidknights.a2ui.scenarios.legacy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class SupportDepthNode(val breadcrumb: String) {
    DEPTH1_PRODUCT_CATEGORY("1/5단계 · 제품군 선택"),
    DEPTH2_ERROR_CODE_LIST("2/5단계 · 에러코드 선택"),
    DEPTH3_MANUAL_PAGE_1("3/5단계 · 매뉴얼 1쪽"),
    DEPTH4_MANUAL_PAGE_2("4/5단계 · 매뉴얼 2쪽"),
    DEPTH5_MANUAL_BOOKING_FORM("5/5단계 · 출장 예약 신청"),
    DEPTH6_BOOKING_DONE("예약 완료"),
}

@Composable
fun LegacyFieldDiagnosticsScreen() {
    val navStack = remember { mutableStateListOf(SupportDepthNode.DEPTH1_PRODUCT_CATEGORY) }
    val currentDepth = navStack.last()
    var touchCount by remember { mutableIntStateOf(0) }

    var isFullScreenLoading by remember { mutableStateOf(false) }

    var manualModelName by remember { mutableStateOf("") }
    var manualErrorCode by remember { mutableStateOf("") }
    var manualSymptomNote by remember { mutableStateOf("") }
    var manualVisitTime by remember { mutableStateOf("") }

    LaunchedEffect(currentDepth) {
        if (currentDepth == SupportDepthNode.DEPTH3_MANUAL_PAGE_1) {
            isFullScreenLoading = true
            delay(800)
            isFullScreenLoading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegacyNavigationHeader(
            missionTitle = "고객지원 센터 (5단계 화면 이동)",
            painPointDesc = "",
            breadcrumb = currentDepth.breadcrumb,
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
                navStack.clear()
                navStack.add(SupportDepthNode.DEPTH1_PRODUCT_CATEGORY)
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
                when (currentDepth) {
                    SupportDepthNode.DEPTH1_PRODUCT_CATEGORY -> {
                        Text(
                            text = "제품 카테고리 선택",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        listOf(
                            "🧺 드럼세탁기" to "세탁·탈수·배수 증상",
                            "👕 의류건조기" to "건조·물통 비움 증상",
                            "🍽️ 식기세척기" to "급수·배수 증상",
                        ).forEach { (name, desc) ->
                            SupportRowCard(
                                title = name,
                                subtitle = desc,
                                onClick = {
                                    touchCount++
                                    navStack.add(SupportDepthNode.DEPTH2_ERROR_CODE_LIST)
                                },
                            )
                        }
                    }

                    SupportDepthNode.DEPTH2_ERROR_CODE_LIST -> {
                        Text(
                            text = "에러코드 선택",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        listOf(
                            "OE (배수 불량)" to "물이 빠지지 않고 탈수가 멈춤",
                            "UE (탈수 불균형)" to "세탁물 쏠림으로 탈수 멈춤",
                            "dE (도어 미잠김)" to "문이 닫히지 않아 시작 불가",
                        ).forEach { (code, desc) ->
                            SupportRowCard(
                                title = code,
                                subtitle = desc,
                                onClick = {
                                    touchCount++
                                    navStack.add(SupportDepthNode.DEPTH3_MANUAL_PAGE_1)
                                },
                            )
                        }
                    }

                    SupportDepthNode.DEPTH3_MANUAL_PAGE_1 -> {
                        if (isFullScreenLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Text(
                                text = "매뉴얼 1쪽: 배수 필터 청소",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "1. 우측 하단 서비스 커버를 여세요.\n2. 잔수 호스 마개를 뽑아 물을 빼세요.\n3. 배수 필터를 분리해 이물질을 청소하세요.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = {
                                    touchCount++
                                    navStack.add(SupportDepthNode.DEPTH4_MANUAL_PAGE_2)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("다음: 매뉴얼 2쪽 (4/5단계)")
                            }
                        }
                    }

                    SupportDepthNode.DEPTH4_MANUAL_PAGE_2 -> {
                        Text(
                            text = "매뉴얼 2쪽: 배수호스 및 펌프 확인",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "필터 청소 후에도 소음이 나고 물이 빠지지 않으면 배수펌프 점검이 필요합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(SupportDepthNode.DEPTH5_MANUAL_BOOKING_FORM)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("출장 수리 예약 화면으로 이동 (5/5단계)")
                        }
                    }

                    SupportDepthNode.DEPTH5_MANUAL_BOOKING_FORM -> {
                        Text(
                            text = "출장 수리 예약 (수기 재입력)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = manualModelName,
                            onValueChange = { manualModelName = it },
                            label = { Text("제품 모델명 (예: WM24KN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = manualErrorCode,
                            onValueChange = { manualErrorCode = it },
                            label = { Text("에러코드 재입력 (예: OE)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = manualSymptomNote,
                            onValueChange = { manualSymptomNote = it },
                            label = { Text("증상 메모") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = manualVisitTime,
                            onValueChange = { manualVisitTime = it },
                            label = { Text("희망 방문 일시") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                touchCount++
                                navStack.add(SupportDepthNode.DEPTH6_BOOKING_DONE)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("예약 신청 완료")
                        }
                    }

                    SupportDepthNode.DEPTH6_BOOKING_DONE -> {
                        Text(
                            text = "출장 예약 접수 완료 (터치 ${touchCount}회)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedButton(
                            onClick = {
                                touchCount = 0
                                navStack.clear()
                                navStack.add(SupportDepthNode.DEPTH1_PRODUCT_CATEGORY)
                            },
                        ) {
                            Text("처음으로")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportRowCard(
    title: String,
    subtitle: String,
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
