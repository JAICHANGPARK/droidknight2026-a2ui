package com.droidknights.a2ui.labs

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiSpec10Bridge
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.launch

/**
 * Official Feature Lab 0:
 * A2UI Spec 1.0 (v1.0 Candidate) Native Payloads & Client Bridge Showcase.
 *
 * Demonstrates:
 * 1. Pure Spec 1.0 agent JSONL (`"version": "v1.0"`, `"@path"`, `"@call"`, inline `createSurface`).
 * 2. `A2uiSpec10Bridge` transparent desugaring into AndroidX `v0.9.1` runtime without modifying the official library.
 * 3. 1 Spec 1.0 message -> 3 AndroidX sequential lifecycle events (`createSurface` -> `updateDataModel` -> `updateComponents`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Spec10BridgeLabScreen() {
    var selectedScenario by remember { mutableIntStateOf(0) }
    var showWireComparison by remember { mutableStateOf(false) }

    val catalog = remember { DroidKnightsCatalogs.createStandardCatalog() }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val spec10Lines = remember { mutableStateListOf<String>() }
    val desugaredLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedScenario) {
            A2uiMessageProcessor(catalogs = listOf(catalog))
        }

    LaunchedEffect(processor) {
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                outboundLog.add(msg)
            }
        }

        val rawSpec10 = buildSpec10SampleJsonl(selectedScenario)
        spec10Lines.clear()
        spec10Lines.addAll(rawSpec10)

        desugaredLines.clear()
        rawSpec10.forEach { line ->
            val transformedBatch = A2uiSpec10Bridge.transform(line)
            transformedBatch.forEach { transformedLine ->
                desugaredLines.add(transformedLine)
                processor.processInput(parser, transformedLine)
            }
        }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "🚀 A2UI Spec 1.0 차세대 브릿지 (v1.0 / @path / @call)",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                    Surface(
                        color = Color(0xFF0369A1).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    ) {
                        Text(
                            text = "Spec 1.0 Candidate",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text =
                        "에이전트(LLM)는 완전한 최신 A2UI Spec 1.0 규격(\"version\":\"v1.0\", \"@path\", \"@call\", 인라인 createSurface)으로 통신하고, " +
                            "클라이언트의 A2uiSpec10Bridge가 이를 현재 구글 공식 androidx.a2ui (v0.9.1 엔진)에 투명하게 연결합니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        0 to "① 1.0 단일 인라인 createSurface (컴포넌트+데이터 모델 한 번에 생성)",
                        1 to "② 1.0 @path 양방향 바인딩 & @call 동적 함수 호출",
                    ).forEach { (idx, title) ->
                        FilterChip(
                            selected = selectedScenario == idx,
                            onClick = { selectedScenario = idx },
                            label = { Text(title, fontSize = 12.sp) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showWireComparison = !showWireComparison }) {
                        Text(
                            text = if (showWireComparison) "비교 뷰 접기" else "🔍 Spec 1.0 vs AndroidX v0.9.1 페이로드 비교 보기",
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                    }
                }

                AnimatedVisibility(visible = showWireComparison) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF020617),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "1) 에이전트가 보낸 순수 A2UI Spec 1.0 JSONL (\"version\": \"v1.0\", @path, 인라인):",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                )
                                spec10Lines.forEach { line ->
                                    Text(
                                        text = line,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF020617),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "2) A2uiSpec10Bridge가 디슈가링하여 AndroidX 엔진으로 주입한 v0.9.1 메시지 시퀀스:",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                )
                                desugaredLines.forEachIndexed { i, line ->
                                    Text(
                                        text = "[#$i] $line",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        activeSurfaces.forEach { surface ->
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        A2uiTelemetryPanel(
            outboundLog = outboundLog,
            jsonlStream = desugaredLines,
            onClearOutbound = { outboundLog.clear() },
        )
    }
}

internal fun buildSpec10SampleJsonl(scenario: Int): List<String> {
    return when (scenario) {
        0 -> {
            // Spec 1.0 single-message inline surface creation!
            // Notice: components and dataModel are bundled directly into createSurface!
            val singleMessage =
                """{"version":"v1.0","createSurface":{"surfaceId":"spec10_inline_surf","catalogId":"${A2uiSpec10Bridge.SPEC_1_0_BASIC_CATALOG_ID}","sendDataModel":true,"dataModel":{"order":{"dishName":"한우 안심 수비드 스테이크","tableNo":7,"servings":2,"ready":true}},"components":[
                {"id":"root","component":"Card","child":"col_main"},
                {"id":"col_main","component":"Column","children":["txt_header","txt_desc","tf_special_req","btn_order"]},
                {"id":"txt_header","component":"Text","variant":"h2","text":"🍽️ [A2UI Spec 1.0] 단일 인라인 createSurface 쇼케이스"},
                {"id":"txt_desc","component":"Text","variant":"body","text":{"@call":"formatString","args":{"value":"테이블: ${'$'}{/order/tableNo}번 · 메뉴: ${'$'}{/order/dishName} (${'$'}{/order/servings}인분)"}}},
                {"id":"tf_special_req","component":"TextField","label":"조리 시 특별 요청사항 (굽기 정도)","value":{"@path":"/order/specialReq"}},
                {"id":"btn_order","component":"Button","variant":"primary","child":"btn_order_txt","action":{"event":{"name":"submit_order","context":{"dish":{"@path":"/order/dishName"},"requests":{"@path":"/order/specialReq"}}}}},
                {"id":"btn_order_txt","component":"Text","text":"주문 확정 (Spec 1.0 @path context 전송)"}
            ]}}""".replace("\n", "")
            listOf(singleMessage)
        }
        else -> {
            // Spec 1.0 two-way binding & dynamic functions
            val createMsg =
                """{"version":"v1.0","createSurface":{"surfaceId":"spec10_eval_surf","catalogId":"${A2uiSpec10Bridge.SPEC_1_0_BASIC_CATALOG_ID}","sendDataModel":true,"dataModel":{"smartPlug":{"power":true,"wattage":340,"maxWatt":2000}}}}""".replace("\n", "")
            val compMsg =
                """{"version":"v1.0","updateComponents":{"surfaceId":"spec10_eval_surf","components":[
                {"id":"root","component":"Card","child":"col_plug"},
                {"id":"col_plug","component":"Column","children":["txt_title","txt_status","chk_power","btn_toggle"]},
                {"id":"txt_title","component":"Text","variant":"h3","text":"🔌 스마트 플러그 전력 제어 (@path & @call)"},
                {"id":"txt_status","component":"Text","variant":"body","text":{"@call":"formatString","args":{"value":"소비 전력: ${'$'}{/smartPlug/wattage} W / 최대 허용: ${'$'}{/smartPlug/maxWatt} W"}}},
                {"id":"chk_power","component":"CheckBox","label":"전원 활성화 여부 (@path 바인딩)","value":{"@path":"/smartPlug/power"}},
                {"id":"btn_toggle","component":"Button","variant":"primary","child":"btn_txt","action":{"event":{"name":"toggle_plug","context":{"power":{"@path":"/smartPlug/power"}}}}},
                {"id":"btn_txt","component":"Text","text":"상태 토글 및 에이전트 역전송"}
            ]}}""".replace("\n", "")
            listOf(createMsg, compMsg)
        }
    }
}
