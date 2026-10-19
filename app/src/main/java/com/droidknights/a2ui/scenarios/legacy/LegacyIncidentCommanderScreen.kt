package com.droidknights.a2ui.scenarios.legacy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class LegacyServiceItem(
    val id: Int,
    val serviceName: String,
    val alertBadge: String,
    val metricTitle: String,
    val currentMetricValue: String,
    val sparkline: List<Float>,
    val sliderLabel: String,
    val sliderRange: ClosedFloatingPointRange<Float>,
    val defaultSliderVal: Float,
    val checkboxLabel: String,
    val isMobileControlSupported: Boolean,
)

private val legacyServices =
    listOf(
        LegacyServiceItem(
            id = 0,
            serviceName = "💳 Payment-Gateway-Core",
            alertBadge = "SEV-1 · p99 4,820ms",
            metricTitle = "Main PG Latency (p99)",
            currentMetricValue = "4,820 ms",
            sparkline = listOf(120f, 180f, 950f, 2400f, 3900f, 4820f),
            sliderLabel = "Backup PG 우회 비율 (%)",
            sliderRange = 0f..100f,
            defaultSliderVal = 80f,
            checkboxLabel = "Primary PG 서킷브레이커 OPEN",
            isMobileControlSupported = true,
        ),
        LegacyServiceItem(
            id = 1,
            serviceName = "🗄️ Order-DB-Replica",
            alertBadge = "SEV-1 · Pool 99.4%",
            metricTitle = "HikariCP Active Connections",
            currentMetricValue = "99.4 %",
            sparkline = listOf(45f, 62f, 78f, 91f, 97f, 99.4f),
            sliderLabel = "Max Worker Threads 상한",
            sliderRange = 8f..128f,
            defaultSliderVal = 24f,
            checkboxLabel = "조회 쿼리 Read-Replica 전환",
            isMobileControlSupported = true,
        ),
        LegacyServiceItem(
            id = 2,
            serviceName = "🚀 Webhook-Canary-v2.4.1",
            alertBadge = "SEV-2 · 5xx 14.8%",
            metricTitle = "Canary HTTP 5xx Error Rate",
            currentMetricValue = "14.8 %",
            sparkline = listOf(0.2f, 0.3f, 2.1f, 6.8f, 11.4f, 14.8f),
            sliderLabel = "Canary 트래픽 가중치 (%)",
            sliderRange = 0f..50f,
            defaultSliderVal = 0f,
            checkboxLabel = "이전 안정 버전(v2.4.0) 복원",
            isMobileControlSupported = true,
        ),
        LegacyServiceItem(
            id = 3,
            serviceName = "⚡ Cart-Redis-Cluster",
            alertBadge = "SEV-1 · Hit 38.2%",
            metricTitle = "Redis Cache Hit Ratio",
            currentMetricValue = "38.2 %",
            sparkline = listOf(98f, 96f, 71f, 52f, 41f, 38.2f),
            sliderLabel = "SWR TTL (초)",
            sliderRange = 30f..600f,
            defaultSliderVal = 300f,
            checkboxLabel = "로컬 인메모리 폴백 캐시 활성화",
            isMobileControlSupported = false,
        ),
    )

private sealed interface IncidentNavStep {
    data object Depth1ServiceList : IncidentNavStep
    data class Depth2MetricDetail(val service: LegacyServiceItem) : IncidentNavStep
    data class Depth3ControlForm(val service: LegacyServiceItem) : IncidentNavStep
    data class Depth4ConfirmScreen(
        val service: LegacyServiceItem,
        val sliderValue: Int,
        val checkboxChecked: Boolean,
    ) : IncidentNavStep
    data class Depth5Completed(val summary: String) : IncidentNavStep
}

@Composable
fun LegacyIncidentCommanderScreen() {
    val backStack = remember { mutableStateListOf<IncidentNavStep>(IncidentNavStep.Depth1ServiceList) }
    val currentStep = backStack.last()
    var touchCount by remember { mutableIntStateOf(0) }

    var sliderVal by remember { mutableFloatStateOf(80f) }
    var checkVal by remember { mutableStateOf(true) }
    var auditMemo by remember { mutableStateOf("긴급 온콜 조치 승인") }

    val breadcrumb =
        when (currentStep) {
            is IncidentNavStep.Depth1ServiceList -> "1/4단계 · 서비스 목록"
            is IncidentNavStep.Depth2MetricDetail -> "2/4단계 · 메트릭 조회"
            is IncidentNavStep.Depth3ControlForm -> "3/4단계 · 파라미터 설정"
            is IncidentNavStep.Depth4ConfirmScreen -> "4/4단계 · 최종 승인"
            is IncidentNavStep.Depth5Completed -> "조치 완료"
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegacyNavigationHeader(
            missionTitle = "온콜 모니터링 (4단계 화면 이동)",
            painPointDesc = "",
            breadcrumb = breadcrumb,
            touchCount = touchCount,
            canGoBack = backStack.size > 1,
            onBack = {
                if (backStack.size > 1) {
                    touchCount++
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            onReset = {
                touchCount = 0
                backStack.clear()
                backStack.add(IncidentNavStep.Depth1ServiceList)
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val step = currentStep) {
                    is IncidentNavStep.Depth1ServiceList -> {
                        Text(
                            text = "장애 발생 서비스 선택",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        legacyServices.forEach { svc ->
                            OutlinedCard(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            touchCount++
                                            sliderVal = svc.defaultSliderVal
                                            checkVal = true
                                            backStack.add(IncidentNavStep.Depth2MetricDetail(svc))
                                        },
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = svc.serviceName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = svc.alertBadge,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontFamily = FontFamily.Monospace,
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

                    is IncidentNavStep.Depth2MetricDetail -> {
                        val svc = step.service
                        Text(
                            text = svc.serviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        PredefinedSparklineCard(
                            metricName = svc.metricTitle,
                            currentValue = svc.currentMetricValue,
                            severity = "critical",
                            dataPoints = svc.sparkline,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                backStack.add(IncidentNavStep.Depth3ControlForm(svc))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("다음: 제어 설정 (3/4단계)")
                        }
                    }

                    is IncidentNavStep.Depth3ControlForm -> {
                        val svc = step.service
                        Text(
                            text = "${svc.serviceName} 설정",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        if (!svc.isMobileControlSupported) {
                            Card(
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "모바일 미지원 제어 항목",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = "전용 제어 화면이 모바일 앱에 배포되지 않았습니다.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            touchCount++
                                            backStack.clear()
                                            backStack.add(IncidentNavStep.Depth1ServiceList)
                                        },
                                    ) {
                                        Text("목록으로 돌아가기")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "${svc.sliderLabel}: ${sliderVal.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Slider(
                                value = sliderVal,
                                onValueChange = {
                                    touchCount++
                                    sliderVal = it
                                },
                                valueRange = svc.sliderRange,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checkVal,
                                    onCheckedChange = {
                                        touchCount++
                                        checkVal = it
                                    },
                                )
                                Text(svc.checkboxLabel, style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(
                                onClick = {
                                    touchCount++
                                    backStack.add(
                                        IncidentNavStep.Depth4ConfirmScreen(
                                            service = svc,
                                            sliderValue = sliderVal.toInt(),
                                            checkboxChecked = checkVal,
                                        ),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("다음: 보안 승인 (4/4단계)")
                            }
                        }
                    }

                    is IncidentNavStep.Depth4ConfirmScreen -> {
                        val svc = step.service
                        Text(
                            text = "최종 승인 및 실행",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${svc.serviceName} · ${svc.sliderLabel}: ${step.sliderValue}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = auditMemo,
                            onValueChange = { auditMemo = it },
                            label = { Text("감사 사유") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                touchCount++
                                backStack.add(
                                    IncidentNavStep.Depth5Completed(
                                        "${svc.serviceName} 조치 완료 (터치 ${touchCount}회)",
                                    ),
                                )
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("승인 및 원격 실행")
                        }
                    }

                    is IncidentNavStep.Depth5Completed -> {
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
                                    text = "복구 명령 전송 완료",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = step.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                OutlinedButton(
                                    onClick = {
                                        touchCount = 0
                                        backStack.clear()
                                        backStack.add(IncidentNavStep.Depth1ServiceList)
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
    }
}

@Composable
internal fun LegacyNavigationHeader(
    missionTitle: String,
    painPointDesc: String,
    breadcrumb: String,
    touchCount: Int,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onReset: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (canGoBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = if (canGoBack) 0.dp else 4.dp)) {
                    Text(
                        text = breadcrumb,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (missionTitle.isNotBlank()) {
                        Text(
                            text = missionTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = "터치 ${touchCount}회",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
                if (canGoBack || touchCount > 0) {
                    TextButton(onClick = onReset) {
                        Text("초기화", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PredefinedSparklineCard(
    metricName: String,
    currentValue: String,
    severity: String,
    dataPoints: List<Float>,
) {
    val accent =
        if (severity == "critical") {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = metricName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currentValue,
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                if (dataPoints.size >= 2) {
                    val min = dataPoints.minOrNull() ?: 0f
                    val max = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(min + 1f)
                    val path = Path()
                    dataPoints.forEachIndexed { idx, v ->
                        val x = (idx.toFloat() / (dataPoints.size - 1)) * size.width
                        val y = size.height - ((v - min) / (max - min)) * size.height
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(accent, radius = 3.dp.toPx(), center = Offset(x, y))
                    }
                    drawPath(path, color = accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}
