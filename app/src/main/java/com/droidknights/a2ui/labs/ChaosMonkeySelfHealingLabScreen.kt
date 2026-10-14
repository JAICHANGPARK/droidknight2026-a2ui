package com.droidknights.a2ui.labs

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Official Docs Feature #2 (`render-surfaces`):
 * - Hallucination Chaos Monkey (Unknown Component Type, Schema/Runtime Violation, Sparse Array Index `[999999]`)
 * - Component-level Error Boundary Isolation (`A2uiSurface` errorContent & child error fallback)
 * - Outbound `A2uiClientErrorMessage` (`VALIDATION_FAILED` / `RUNTIME_ERROR`) -> Automatic Agent Self-Correction Loop!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChaosMonkeySelfHealingLabScreen() {
    var autoSelfHeal by remember { mutableStateOf(true) }
    var statusBanner by remember {
        mutableStateOf("정상 렌더링 중 · 아래 Chaos Monkey 버튼으로 LLM 환각(Hallucination)을 주입해보세요.")
    }
    val scope = rememberCoroutineScope()

    val catalog = remember { DroidKnightsCatalogs.createStandardCatalog() }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor = remember { A2uiMessageProcessor(catalogs = listOf(catalog)) }

    LaunchedEffect(processor) {
        launch { processor.collectMessages() }
        val initLines = buildInitialHealthySurfaceJsonl()
        jsonlLines.clear()
        jsonlLines.addAll(initLines)
        initLines.forEach { processor.processInput(parser, it) }
    }

    LaunchedEffect(processor, autoSelfHeal) {
        processor.outboundEvents.collect { msg ->
            outboundLog.add(msg)
            if (msg is A2uiClientErrorMessage && autoSelfHeal) {
                statusBanner =
                    "🚨 에러 감지 (${msg.code}): ${msg.message} -> 1.4초 후 에이전트가 outboundEvents를 읽고 자동 교정 패치를 전송합니다..."
                delay(1400)
                val patch =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
                        {"id":"slot_widget","component":"MetricSparkline","metricName":"Auto-Healed MetricSparkline (교정 완료)","currentValue":"99.98% Healthy","severity":"normal"},
                        {"id":"slot_slider","component":"Slider","label":"정상 범위로 복구된 슬라이더 (min=0, max=100)","min":0,"max":100,"value":{"path":"/chaos/sliderVal"}}
                    ]}}""".replace("\n", "")
                jsonlLines.add(patch)
                processor.processInput(parser, patch)
                statusBanner = "✅ 자가치유(Self-Healing) 완료! 에이전트가 에러 리포트를 받고 유효한 컴포넌트로 핫패치했습니다."
            } else if (msg is A2uiClientErrorMessage) {
                statusBanner =
                    "⚠️ 에러 격리됨 (${msg.code}): 자동 자가치유가 꺼져 있어 해당 슬롯만 에러 경계(Error Boundary)로 표시됩니다."
            }
        }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "🐒 공식 기능 Lab 2 · 환각 카오스 몽키 & 에이전트 자가치유(Self-Healing) 루프",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "LLM이 카탈로그에 없는 위젯 이름이나 잘못된 스키마를 환각하더라도 앱 전체가 크래시하지 않고 해당 슬롯만 격리되며, " +
                            "processor.outboundEvents로 A2uiClientErrorMessage가 방출되어 에이전트가 스스로 수정 패치를 보냅니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (autoSelfHeal) Color(0xFF064E3B) else Color(0xFF3F3F46),
                    border = BorderStroke(1.dp, if (autoSelfHeal) Color(0xFF10B981) else Color(0xFF71717A)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🤖 에이전트 자동 자가치유 루프 (Auto Self-Healing)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = "활성화 시 outboundEvents의 A2uiClientErrorMessage를 감지해 1.4초 뒤 교정 패치를 주입합니다.",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                            )
                        }
                        Switch(checked = autoSelfHeal, onCheckedChange = { autoSelfHeal = it })
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val badComp =
                                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
                                        {"id":"slot_widget","component":"Hallucinated3DChart","title":"존재하지 않는 위젯"}
                                    ]}}""".replace("\n", "")
                                jsonlLines.add(badComp)
                                processor.processInput(parser, badComp)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    ) {
                        Text("💥 1. 미등록 컴포넌트 환각 주입", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val badSlider =
                                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
                                        {"id":"slot_slider","component":"Slider","label":"환각 역전 슬라이더 (min=100 > max=10)","min":100,"max":10,"value":{"path":"/chaos/sliderVal"}}
                                    ]}}""".replace("\n", "")
                                jsonlLines.add(badSlider)
                                processor.processInput(parser, badSlider)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    ) {
                        Text("⚠️ 2. Slider min > max 런타임 에러 주입", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val sparseMsg =
                                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"chaos_surface","path":"/sparseItems/999999","value":"인덱스 999,999 희소 배열 안전 저장 완료 (OOM 없음!)"}}"""
                                jsonlLines.add(sparseMsg)
                                processor.processInput(parser, sparseMsg)
                                statusBanner =
                                    "🛡️ SnapshotStateSparseList 검증 완료: /sparseItems/999999 경로 업데이트 시 999,999개의 빈 슬롯을 할당하지 않아 OOM 없이 즉시 반영되었습니다."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    ) {
                        Text("🛡️ 3. 희소 배열 [999999] OOM 방어 테스트", fontSize = 12.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = statusBanner,
                        color = Color(0xFFFDE68A),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }

        activeSurfaces.forEach { surface ->
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier.fillMaxWidth(),
                errorContent = { exception ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🚨 커스텀 A2uiSurface Error Fallback",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = exception.message.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                            )
                        }
                    }
                },
            )
        }

        A2uiTelemetryPanel(
            outboundLog = outboundLog,
            jsonlStream = jsonlLines,
            onClearOutbound = { outboundLog.clear() },
        )
    }
}

internal fun buildInitialHealthySurfaceJsonl(): List<String> {
    val createMsg =
        """{"version":"v0.9.1","createSurface":{"surfaceId":"chaos_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""
    val dataMsg =
        """{"version":"v0.9.1","updateDataModel":{"surfaceId":"chaos_surface","path":"/","value":{"chaos":{"sliderVal":40},"sparseItems":{"999999":"대기 중 (3번 버튼을 눌러보세요)"}}}}"""
    val compMsg =
        """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
            {"id":"root","component":"Card","child":"col_chaos"},
            {"id":"col_chaos","component":"Column","children":["txt_chaos_title","slot_widget","slot_slider","txt_sparse"]},
            {"id":"txt_chaos_title","component":"Text","variant":"h3","text":"결함 격리 & 자가치유(Self-Healing) 테스트 베드"},
            {"id":"slot_widget","component":"MetricSparkline","metricName":"정상 슬롯 (1번 버튼으로 환각 주입 가능)","currentValue":"100% Valid","severity":"normal"},
            {"id":"slot_slider","component":"Slider","label":"정상 슬라이더 (2번 버튼으로 min>max 에러 주입 가능)","min":0,"max":100,"value":{"path":"/chaos/sliderVal"}},
            {"id":"txt_sparse","component":"Text","variant":"caption","text":{"call":"formatString","args":{"value":"희소 배열 /sparseItems/999999 값: ${'$'}{/sparseItems/999999}"}}}
        ]}}""".replace("\n", "")
    return listOf(createMsg, dataMsg, compMsg)
}
