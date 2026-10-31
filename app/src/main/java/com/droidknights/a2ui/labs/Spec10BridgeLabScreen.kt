package com.droidknights.a2ui.labs

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiSpec10Bridge
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Spec10BridgeLabScreen() {
    var selectedScenario by remember { mutableIntStateOf(0) }
    var showWireComparison by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember(context) { DroidKnightsCatalogs.createStandardCatalog(context = context) }
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "A2UI Spec 1.0 브릿지",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "v1.0 (@path, @call, 인라인 createSurface) 페이로드를 v0.9.1 엔진으로 변환합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = { showWireComparison = !showWireComparison },
                label = {
                    Text(
                        text = if (showWireComparison) "비교 닫기" else "JSONL 비교",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                0 to "인라인 createSurface",
                1 to "@path 바인딩 & @call 함수",
            ).forEach { (idx, title) ->
                FilterChip(
                    selected = selectedScenario == idx,
                    onClick = { selectedScenario = idx },
                    label = { Text(title, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        AnimatedVisibility(visible = showWireComparison) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Spec 1.0 원본 JSONL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        spec10Lines.forEach { line ->
                            Text(
                                text = line,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "변환된 AndroidX v0.9.1 JSONL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        desugaredLines.forEachIndexed { i, line ->
                            Text(
                                text = "[#$i] $line",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        activeSurfaces.forEach { surface ->
            androidx.compose.runtime.key(surface.id) {
                A2uiSurface(
                    surfaceModel = surface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
            val singleMessage =
                """{"version":"v1.0","createSurface":{"surfaceId":"spec10_inline_surf","catalogId":"${A2uiSpec10Bridge.SPEC_1_0_BASIC_CATALOG_ID}","sendDataModel":true,"dataModel":{"order":{"dishName":"한우 안심 스테이크","tableNo":7,"servings":2,"ready":true}},"components":[
                {"id":"root","component":"Card","child":"col_main"},
                {"id":"col_main","component":"Column","children":["txt_header","txt_desc","tf_special_req","btn_order"]},
                {"id":"txt_header","component":"Text","text":"🍽️ 다이닝 주문 확인 (Spec 1.0 인라인)"},
                {"id":"txt_desc","component":"Text","variant":"body","text":{"@call":"formatString","args":{"value":"테이블 ${'$'}{/order/tableNo}번 · ${'$'}{/order/dishName} (${'$'}{/order/servings}인분)"}}},
                {"id":"tf_special_req","component":"TextField","label":"요청사항 (굽기 정도)","value":{"@path":"/order/specialReq"}},
                {"id":"btn_order","component":"Button","variant":"primary","child":"btn_order_txt","action":{"event":{"name":"submit_order","context":{"dish":{"@path":"/order/dishName"},"requests":{"@path":"/order/specialReq"}}}}},
                {"id":"btn_order_txt","component":"Text","text":"주문 확정"}
            ]}}""".replace("\n", "")
            listOf(singleMessage)
        }
        else -> {
            val createMsg =
                """{"version":"v1.0","createSurface":{"surfaceId":"spec10_eval_surf","catalogId":"${A2uiSpec10Bridge.SPEC_1_0_BASIC_CATALOG_ID}","sendDataModel":true,"dataModel":{"smartPlug":{"power":true,"wattage":340,"maxWatt":2000}}}}""".replace("\n", "")
            val compMsg =
                """{"version":"v1.0","updateComponents":{"surfaceId":"spec10_eval_surf","components":[
                {"id":"root","component":"Card","child":"col_plug"},
                {"id":"col_plug","component":"Column","children":["txt_title","txt_status","chk_power","btn_toggle"]},
                {"id":"txt_title","component":"Text","text":"🔌 스마트 플러그 전력 제어"},
                {"id":"txt_status","component":"Text","variant":"body","text":{"@call":"formatString","args":{"value":"소비 전력: ${'$'}{/smartPlug/wattage} W / ${'$'}{/smartPlug/maxWatt} W"}}},
                {"id":"chk_power","component":"CheckBox","label":"전원 활성화 (@path)","value":{"@path":"/smartPlug/power"}},
                {"id":"btn_toggle","component":"Button","variant":"primary","child":"btn_txt","action":{"event":{"name":"toggle_plug","context":{"power":{"@path":"/smartPlug/power"}}}}},
                {"id":"btn_txt","component":"Text","text":"상태 저장"}
            ]}}""".replace("\n", "")
            listOf(createMsg, compMsg)
        }
    }
}
