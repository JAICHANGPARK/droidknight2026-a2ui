package com.droidknights.a2ui.labs

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientCapabilities
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.launch

/**
 * Official Docs Feature #1:
 * - 3-Way Catalog Switching (`materialA2uiBasicCatalogV1` Slot Overrides: M3 Default vs Brand Design System vs Senior Voice/High-Contrast)
 * - Multi-Catalog V1/V2 Negotiation (`A2uiMessageProcessor(catalogs = listOf(v1, v2))`)
 * - Live `A2uiCatalog.toJsonSchemaString()` & `A2uiClientCapabilities.toPayloadMap()` Inspector
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogEvolutionLabScreen() {
    var selectedSkin by remember { mutableIntStateOf(0) }
    var negotiateV2 by remember { mutableStateOf(true) }
    var showSchemaInspector by remember { mutableStateOf(false) }

    val (v1Base, v2Base) = remember { DroidKnightsCatalogs.createVersionedCatalogPair() }
    val targetCatalogId = if (negotiateV2) DroidKnightsCatalogs.CATALOG_ID_V2 else DroidKnightsCatalogs.CATALOG_ID_V1

    val activeCatalogForSkin =
        remember(selectedSkin, targetCatalogId) {
            when (selectedSkin) {
                0 -> DroidKnightsCatalogs.createStandardCatalog(catalogId = targetCatalogId)
                1 -> DroidKnightsCatalogs.createBrandDesignSystemCatalog(catalogId = targetCatalogId)
                else -> DroidKnightsCatalogs.createSeniorAccessibleCatalog(catalogId = targetCatalogId)
            }
        }

    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(activeCatalogForSkin, negotiateV2) {
            A2uiMessageProcessor(catalogs = listOf(v1Base, activeCatalogForSkin))
        }

    LaunchedEffect(processor) {
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                outboundLog.add(msg)
            }
        }
        val lines = buildCatalogComparisonJsonl(targetCatalogId, negotiateV2)
        jsonlLines.clear()
        jsonlLines.addAll(lines)
        lines.forEach { line -> processor.processInput(parser, line) }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    val capabilitiesSummary = remember(v1Base, v2Base) {
        val caps =
            A2uiClientCapabilities(
                supportedCatalogIds = listOf(v1Base.id, v2Base.id),
            )
        caps.toPayloadMap().toString()
    }

    val schemaSnippet = remember(v2Base) {
        v2Base.toJsonSchemaString().take(900) + "\n... (총 ${v2Base.toJsonSchemaString().length} chars 자동 직렬화됨)"
    }

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
                    text = "🎨 공식 기능 Lab 1 · 1개의 JSON 스트림 × 3개의 카탈로그 & V1/V2 버전 협상",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "에이전트가 보내는 JSONL 페이로드는 단 1바이트도 바꾸지 않고, 안드로이드 클라이언트의 materialA2uiBasicCatalogV1 슬롯 오버라이드만으로 " +
                            "① M3 기본, ② 브랜드 디자인 시스템(Toss/Kurly풍), ③ 시니어 고대비/음성 안내 UI로 즉시 전환합니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val skins =
                        listOf(
                            "① Material 3 기본 카탈로그",
                            "② 브랜드 디자인 시스템 (Card/Button/Text 오버라이드)",
                            "③ 시니어 큰글씨·고대비 음성 카탈로그",
                        )
                    skins.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedSkin == idx,
                            onClick = { selectedSkin = idx },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text =
                                    if (negotiateV2) {
                                        "협상된 카탈로그: V2 (${DroidKnightsCatalogs.CATALOG_ID_V2})"
                                    } else {
                                        "협상된 카탈로그: V1 (${DroidKnightsCatalogs.CATALOG_ID_V1})"
                                    },
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text =
                                    if (negotiateV2) {
                                        "V2 전용 MetricSparkline + PowerBudgetBar 컴포넌트가 활성화됩니다."
                                    } else {
                                        "레거시 V1 카탈로그로 다운그레이드되어 기본 M3 컴포넌트로만 렌더링합니다."
                                    },
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                            )
                        }
                        Switch(
                            checked = negotiateV2,
                            onCheckedChange = { negotiateV2 = it },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showSchemaInspector = !showSchemaInspector }) {
                        Text(
                            text =
                                if (showSchemaInspector) {
                                    "toJsonSchemaString() & Capabilities 숨기기"
                                } else {
                                    "🔍 A2uiCatalog.toJsonSchemaString() & Capabilities 보기"
                                },
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                    }
                }

                AnimatedVisibility(visible = showSchemaInspector) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF020617),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "1) A2uiClientCapabilities.toPayloadMap()",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = capabilitiesSummary,
                                color = Color(0xFFA7F3D0),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = "2) catalog.toJsonSchemaString() (시스템 프롬프트 자동 주입용 JSON Schema)",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = schemaSnippet,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
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
            jsonlStream = jsonlLines,
            onClearOutbound = { outboundLog.clear() },
        )
    }
}

internal fun buildCatalogComparisonJsonl(catalogId: String, includeV2Widgets: Boolean): List<String> {
    val createMsg =
        """{"version":"v0.9.1","createSurface":{"surfaceId":"catalog_lab_surface","catalogId":"$catalogId","sendDataModel":true}}"""
    val dataMsg =
        """{"version":"v0.9.1","updateDataModel":{"surfaceId":"catalog_lab_surface","path":"/","value":{"transfer":{"recipient":"김드로이드 총무","amount":45000,"memo":"DroidKnights 뒤풀이 회비","usedLimit":1.8,"maxLimit":5.0}}}}"""

    val childrenList =
        if (includeV2Widgets) {
            """["txt_head","txt_amount","spark_v2","budget_v2","tf_memo","btn_confirm"]"""
        } else {
            """["txt_head","txt_amount","tf_memo","btn_confirm"]"""
        }

    val v2Components =
        if (includeV2Widgets) {
            """{"id":"spark_v2","component":"MetricSparkline","metricName":"FDS 이상거래 탐지 스코어","currentValue":"0.02% (안전)","severity":"normal","points":[0.72,0.58,0.66,0.35,0.88,0.24,0.94]},
               {"id":"budget_v2","component":"PowerBudgetBar","title":"일일 간편송금 한도 사용량 (백만 원)","usedKw":{"path":"/transfer/usedLimit"},"maxKw":{"path":"/transfer/maxLimit"},"detail":"V2 카탈로그 전용 커스텀 컴포넌트"},"""
        } else {
            ""
        }

    val compMsg =
        """{"version":"v0.9.1","updateComponents":{"surfaceId":"catalog_lab_surface","components":[
            {"id":"root","component":"Card","child":"col_body"},
            {"id":"col_body","component":"Column","children":$childrenList},
            {"id":"txt_head","component":"Text","variant":"h3","text":"동일한 JSON 스트림 -> 카탈로그별 디자인 시스템 렌더링"},
            {"id":"txt_amount","component":"Text","variant":"body","text":{"call":"formatString","args":{"value":"받는 분: ${'$'}{/transfer/recipient} · 송금액: ${'$'}{formatCurrency(value:${'$'}{/transfer/amount}, currency:'KRW', decimals:0)}"}}},
            $v2Components
            {"id":"tf_memo","component":"TextField","label":"받는 분 통장 표시 메모","value":{"path":"/transfer/memo"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/transfer/memo"}}},"message":"통장 메모를 입력해주세요."}]},
            {"id":"btn_confirm","component":"Button","variant":"primary","child":"btn_confirm_txt","checks":[{"condition":{"call":"required","args":{"value":{"path":"/transfer/memo"}}},"message":"메모가 비어 있습니다."}],"action":{"event":{"name":"confirm_transfer","context":{"recipient":{"path":"/transfer/recipient"},"amount":{"path":"/transfer/amount"},"memo":{"path":"/transfer/memo"}}}}},
            {"id":"btn_confirm_txt","component":"Text","text":"카탈로그 스타일로 즉시 송금 실행"}
        ]}}""".replace("\n", "")

    return listOf(createMsg, dataMsg, compMsg)
}
