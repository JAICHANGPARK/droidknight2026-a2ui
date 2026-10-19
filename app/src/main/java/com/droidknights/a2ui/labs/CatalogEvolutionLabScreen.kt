package com.droidknights.a2ui.labs

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientCapabilities
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogEvolutionLabScreen() {
    var selectedSkin by remember { mutableIntStateOf(0) }
    var negotiateV2 by remember { mutableStateOf(true) }
    var showSchemaInspector by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val (v1Base, v2Base) = remember(context) { DroidKnightsCatalogs.createVersionedCatalogPair(context = context) }
    val targetCatalogId = if (negotiateV2) DroidKnightsCatalogs.CATALOG_ID_V2 else DroidKnightsCatalogs.CATALOG_ID_V1

    val activeCatalogForSkin =
        remember(context, selectedSkin, targetCatalogId) {
            when (selectedSkin) {
                0 -> DroidKnightsCatalogs.createStandardCatalog(catalogId = targetCatalogId, context = context)
                1 -> DroidKnightsCatalogs.createBrandDesignSystemCatalog(catalogId = targetCatalogId, context = context)
                else -> DroidKnightsCatalogs.createSeniorAccessibleCatalog(catalogId = targetCatalogId, context = context)
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
        v2Base.toJsonSchemaString().take(600) + "\n..."
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "카탈로그 디자인 시스템 & 버전 협상",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "동일한 JSONL 스트림을 슬롯 오버라이드와 V1/V2 협상으로 다르게 렌더링합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = { showSchemaInspector = !showSchemaInspector },
                label = {
                    Text(
                        text = if (showSchemaInspector) "스키마 닫기" else "스키마 보기",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val skins =
                listOf(
                    "Material 3 기본",
                    "브랜드 디자인 시스템",
                    "시니어 큰글씨·고대비",
                )
            skins.forEachIndexed { idx, label ->
                FilterChip(
                    selected = selectedSkin == idx,
                    onClick = { selectedSkin = idx },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (negotiateV2) "카탈로그 V2 (커스텀 위젯 포함)" else "카탈로그 V1 (기본 M3 전용)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = targetCatalogId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Switch(
                    checked = negotiateV2,
                    onCheckedChange = { negotiateV2 = it },
                )
            }
        }

        AnimatedVisibility(visible = showSchemaInspector) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "A2uiClientCapabilities.toPayloadMap()",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = capabilitiesSummary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "catalog.toJsonSchemaString()",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = schemaSnippet,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
        """{"version":"v0.9.1","updateDataModel":{"surfaceId":"catalog_lab_surface","path":"/","value":{"transfer":{"recipient":"김드로이드 총무","amount":45000,"memo":"드로이드나이츠 회비","usedLimit":1.8,"maxLimit":5.0}}}}"""

    val childrenList =
        if (includeV2Widgets) {
            """["txt_head","txt_amount","spark_v2","budget_v2","tf_memo","btn_confirm"]"""
        } else {
            """["txt_head","txt_amount","tf_memo","btn_confirm"]"""
        }

    val v2Components =
        if (includeV2Widgets) {
            """{"id":"spark_v2","component":"MetricSparkline","metricName":"FDS 이상거래 스코어","currentValue":"0.02% (안전)","severity":"normal","points":[0.72,0.58,0.66,0.35,0.88,0.24,0.94]},
               {"id":"budget_v2","component":"PowerBudgetBar","title":"일일 송금 한도 (백만 원)","usedKw":{"path":"/transfer/usedLimit"},"maxKw":{"path":"/transfer/maxLimit"},"detail":"V2 전용 커스텀 컴포넌트"},"""
        } else {
            ""
        }

    val compMsg =
        """{"version":"v0.9.1","updateComponents":{"surfaceId":"catalog_lab_surface","components":[
            {"id":"root","component":"Card","child":"col_body"},
            {"id":"col_body","component":"Column","children":$childrenList},
            {"id":"txt_head","component":"Text","text":"💸 간편 송금 확인"},
            {"id":"txt_amount","component":"Text","variant":"body","text":{"call":"formatString","args":{"value":"받는 분: ${'$'}{/transfer/recipient} · ${'$'}{formatCurrency(value:${'$'}{/transfer/amount}, currency:'KRW', decimals:0)}"}}},
            $v2Components
            {"id":"tf_memo","component":"TextField","label":"받는 분 통장 메모","value":{"path":"/transfer/memo"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/transfer/memo"}}},"message":"메모를 입력해주세요."}]},
            {"id":"btn_confirm","component":"Button","variant":"primary","child":"btn_confirm_txt","checks":[{"condition":{"call":"required","args":{"value":{"path":"/transfer/memo"}}},"message":"메모가 비어 있습니다."}],"action":{"event":{"name":"confirm_transfer","context":{"recipient":{"path":"/transfer/recipient"},"amount":{"path":"/transfer/amount"},"memo":{"path":"/transfer/memo"}}}}},
            {"id":"btn_confirm_txt","component":"Text","text":"송금 실행"}
        ]}}""".replace("\n", "")

    return listOf(createMsg, dataMsg, compMsg)
}
