package com.droidknights.a2ui.labs

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChaosMonkeySelfHealingLabScreen() {
    var autoSelfHeal by remember { mutableStateOf(true) }
    var statusBanner by remember {
        mutableStateOf("정상 렌더링 중 · 버튼으로 환각 에러를 주입해 보세요.")
    }
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember(context) { DroidKnightsCatalogs.createStandardCatalog(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor = remember(catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }

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
                statusBanner = "에러 감지 (${msg.code}) → 1.4초 후 에이전트 자동 교정 패치 전송..."
                delay(1400)
                val patch =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
                        {"id":"slot_widget","component":"MetricSparkline","metricName":"Auto-Healed Sparkline","currentValue":"99.98% Healthy","severity":"normal"},
                        {"id":"slot_slider","component":"Slider","label":"복구된 슬라이더 (0~100)","min":0,"max":100,"value":{"path":"/chaos/sliderVal"}}
                    ]}}""".replace("\n", "")
                jsonlLines.add(patch)
                processor.processInput(parser, patch)
                statusBanner = "자가치유 완료: 에이전트가 유효한 컴포넌트로 핫패치했습니다."
            } else if (msg is A2uiClientErrorMessage) {
                statusBanner = "에러 격리됨 (${msg.code}): 해당 슬롯만 에러 경계로 격리되었습니다."
            }
        }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "결함 격리 & 에이전트 자가치유",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "미등록 컴포넌트나 잘못된 스키마 주입 시 슬롯 단위 격리와 outboundEvents 기반 자동 복구를 검증합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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
                        text = "자동 자가치유 루프 (Auto Self-Healing)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoSelfHeal, onCheckedChange = { autoSelfHeal = it })
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
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
            ) {
                Text("1. 미등록 위젯 주입", style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val badSlider =
                            """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
                                {"id":"slot_slider","component":"Slider","label":"역전 슬라이더 (min=100 > max=10)","min":100,"max":10,"value":{"path":"/chaos/sliderVal"}}
                            ]}}""".replace("\n", "")
                        jsonlLines.add(badSlider)
                        processor.processInput(parser, badSlider)
                    }
                },
            ) {
                Text("2. min > max 에러 주입", style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val sparseMsg =
                            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"chaos_surface","path":"/sparseItems/999999","value":"인덱스 999,999 안전 저장 완료"}}"""
                        jsonlLines.add(sparseMsg)
                        processor.processInput(parser, sparseMsg)
                        statusBanner = "희소 배열 /sparseItems/999999 업데이트 완료 (OOM 방어)"
                    }
                },
            ) {
                Text("3. 희소 배열 [999999] 테스트", style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val maliciousUrlBtn =
                            """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
                                {"id":"slot_widget","component":"Button","variant":"borderless","child":"txt_malicious_url","action":{"functionCall":{"call":"openUrl","args":{"url":"javascript:alert('XSS')"}}}},
                                {"id":"txt_malicious_url","component":"Text","text":"⚠️ 비허용 스킴 클릭 (javascript:alert)"}
                            ]}}""".replace("\n", "")
                        jsonlLines.add(maliciousUrlBtn)
                        processor.processInput(parser, maliciousUrlBtn)
                        statusBanner = "openUrl 스킴 검증 버튼 주입됨: 클릭 시 javascript: 스킴이 차단되고 에이전트로 에러가 보고됩니다."
                    }
                },
            ) {
                Text("4. 비허용 URL 스킴 차단", style = MaterialTheme.typography.labelMedium)
            }

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val dupChoicePicker =
                            """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
                                {"id":"slot_widget","component":"ChoicePicker","label":"중복 옵션 ChoicePicker","variant":"mutuallyExclusive","options":[{"label":"서울 리전","value":"SEL"},{"label":"중복 서울 리전","value":"SEL"}],"value":{"path":"/chaos/region"}}
                            ]}}""".replace("\n", "")
                        jsonlLines.add(dupChoicePicker)
                        processor.processInput(parser, dupChoicePicker)
                    }
                },
            ) {
                Text("5. 중복 옵션 에러 주입", style = MaterialTheme.typography.labelMedium)
            }
        }

        activeSurfaces.forEach { surface ->
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier.fillMaxWidth(),
                errorContent = { exception ->
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "A2uiSurface 에러 경계",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = exception.message.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
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
        """{"version":"v0.9.1","updateDataModel":{"surfaceId":"chaos_surface","path":"/","value":{"chaos":{"sliderVal":40,"region":["SEL"]},"sparseItems":{"999999":"대기 중"}}}}"""
    val compMsg =
        """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[
            {"id":"root","component":"Card","child":"col_chaos"},
            {"id":"col_chaos","component":"Column","children":["txt_chaos_title","slot_widget","slot_slider","txt_sparse"]},
            {"id":"txt_chaos_title","component":"Text","text":"🐒 결함 격리 & 자가치유 테스트 베드"},
            {"id":"slot_widget","component":"MetricSparkline","metricName":"정상 슬롯","currentValue":"100% Valid","severity":"normal"},
            {"id":"slot_slider","component":"Slider","label":"정상 슬라이더 (0~100)","min":0,"max":100,"value":{"path":"/chaos/sliderVal"}},
            {"id":"txt_sparse","component":"Text","variant":"caption","text":{"call":"formatString","args":{"value":"희소 배열 [999999]: ${'$'}{/sparseItems/999999}"}}}
        ]}}""".replace("\n", "")
    return listOf(createMsg, dataMsg, compMsg)
}
