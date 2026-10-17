package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Domain Scenario D: Appliance Error Code (`OE` / `UE`) Interactive Field Diagnostics Wizard
 * + Official Feature #4: `A2uiComponent.isReady()` Progressive Rendering & Incremental `updateComponents` Evolution.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldDiagnosticsWizardDemo() {
    var wizardStep by remember { mutableIntStateOf(1) }
    var simulateDelayedData by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val catalog = remember { DroidKnightsCatalogs.createStandardCatalog() }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(wizardStep, simulateDelayedData) {
            A2uiMessageProcessor(catalogs = listOf(catalog))
        }

    LaunchedEffect(processor) {
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                outboundLog.add(msg)
                if (msg is A2uiClientEventMessage && msg.type == "advance_wizard_step") {
                    if (wizardStep < 3) wizardStep += 1
                }
            }
        }

        val (initialLines, delayedLines) = buildDiagnosticsWizardJsonl(wizardStep, simulateDelayedData)
        jsonlLines.clear()
        jsonlLines.addAll(initialLines)
        initialLines.forEach { line -> processor.processInput(parser, line) }

        if (delayedLines.isNotEmpty()) {
            delay(1800)
            delayedLines.forEach { delayedLine ->
                jsonlLines.add(delayedLine)
                processor.processInput(parser, delayedLine)
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
                    text = "🔧 시나리오 D · 세탁기 OE 에러코드 자가진단 위저드 (isReady & ShimmerIndicatorGroup)",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "점검 결과에 따라 같은 Surface ID(\"diag_surface\") 안에서 UI 트리가 단계별로 진화(updateComponents)합니다. " +
                            "1.0.0-SNAPSHOT에 신규 추가된 v0.9.1 카탈로그 ID 별칭(Alias) 호환과 MaterialA2uiDefaults.LoadingIndicatorGroup의 " +
                            "다중 컴포넌트 방사형 쉬머(ShimmerIndicator) 애니메이션을 확인할 수 있습니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        1 to "Step 1. 전면 하단 배수필터 위치 확인",
                        2 to "Step 2. 수압/잔수 점검 및 시험 탈수",
                        3 to "Step 3. 해결 불가 시 방문 AS 일정 예약",
                    ).forEach { (step, title) ->
                        FilterChip(
                            selected = wizardStep == step,
                            onClick = { wizardStep = step },
                            label = { Text(title, fontSize = 12.sp) },
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                simulateDelayedData = true
                                wizardStep = 1
                            }
                        }
                    ) {
                        Text("⏳ 1.8초 지연 스트리밍 재현 (ShimmerIndicatorGroup)", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val nextIdx = jsonlLines.count { it.contains("/diag/logs/-") } + 1
                                val appendMsg =
                                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/diag/logs/-","value":{"msg":"[실시간 센서 #$nextIdx] 배수 펌프 수위 센서 응답 확인 (정상)"}}}"""
                                jsonlLines.add(appendMsg)
                                processor.processInput(parser, appendMsg)
                            }
                        }
                    ) {
                        Text("➕ /diag/logs/- 배열 Append 스트리밍", color = Color.White, fontSize = 12.sp)
                    }
                    if (simulateDelayedData) {
                        OutlinedButton(onClick = { simulateDelayedData = false }) {
                            Text("즉시 로딩 모드로 복귀", color = Color.White, fontSize = 12.sp)
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

internal fun buildDiagnosticsWizardJsonl(
    step: Int,
    delayHotspotData: Boolean,
): Pair<List<String>, List<String>> {
    // Uses v0_9_1 catalogId URI to demonstrate 1.0.0-SNAPSHOT's automatic CATALOG_ID_ALIASES mapping to v0_9
    val createMsg =
        """{"version":"v0.9.1","createSurface":{"surfaceId":"diag_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID_V0_9_1_ALIAS}","sendDataModel":true}}"""

    return when (step) {
        1 -> {
            val fullCompMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                {"id":"root","component":"Card","child":"col_step1"},
                {"id":"col_step1","component":"Column","children":["txt_s1_title","hotspot_filter","vid_guide","chk_hose","list_logs","btn_next1"]},
                {"id":"txt_s1_title","component":"Text","variant":"h3","text":"[1/3단계] 세탁기 OE(배수 불량) · 하단 배수 필터 위치 확인"},
                {"id":"hotspot_filter","component":"HotspotDiagramCard","title":{"path":"/diag/partTitle"},"hotspotLabel":{"path":"/diag/hotspotLabel"},"zoneCode":"PUMP-B2","status":"이물질 점검"},
                {"id":"vid_guide","component":"Video","url":"https://support.appliance.local/oe-drain-filter-60s.mp4"},
                {"id":"chk_hose","component":"CheckBox","label":"비상 배수 호스 마개를 열어 잔수를 먼저 제거했습니다","value":{"path":"/diag/hoseDrained"}},
                {"id":"list_logs","component":"List","children":{"path":"/diag/logs","componentId":"tpl_log_item"}},
                {"id":"tpl_log_item","component":"Text","variant":"caption","text":{"path":"msg"}},
                {"id":"btn_next1","component":"Button","variant":"primary","child":"btn_next1_txt","action":{"event":{"name":"advance_wizard_step","context":{"completedStep":1,"hoseDrained":{"path":"/diag/hoseDrained"}}}}} ,
                {"id":"btn_next1_txt","component":"Text","text":"1단계 완료 -> 2단계(펌프 임펠러 점검)로 진화"}
            ]}}""".replace("\n", "")

            val fullDataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"partTitle":"드럼세탁기 전면 우측 하단 서비스 커버","hotspotLabel":"잔수 제거 호스 및 배수 펌프 거름망","hoseDrained":true,"logs":[{"msg":"[초기 진단] 에러코드 OE 감지 · 배수 펌프 거름망 점검 시작"}]}}}}"""

            if (delayHotspotData) {
                // Initially omit `vid_guide` & `chk_hose` from updateComponents AND omit `/diag/partTitle` from DataModel
                // so 3 consecutive items in `col_step1` render MaterialA2uiDefaults.LoadingIndicatorGroup's synchronized radial ShimmerIndicator!
                val initialSkeletonCompMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                    {"id":"root","component":"Card","child":"col_step1"},
                    {"id":"col_step1","component":"Column","children":["txt_s1_title","hotspot_filter","vid_guide","chk_hose","list_logs","btn_next1"]},
                    {"id":"txt_s1_title","component":"Text","variant":"h3","text":"[1/3단계] 세탁기 OE(배수 불량) · 하단 배수 필터 위치 확인"},
                    {"id":"hotspot_filter","component":"HotspotDiagramCard","title":{"path":"/diag/partTitle"},"hotspotLabel":{"path":"/diag/hotspotLabel"},"zoneCode":"PUMP-B2","status":"이물질 점검"},
                    {"id":"list_logs","component":"List","children":{"path":"/diag/logs","componentId":"tpl_log_item"}},
                    {"id":"tpl_log_item","component":"Text","variant":"caption","text":{"path":"msg"}},
                    {"id":"btn_next1","component":"Button","variant":"primary","child":"btn_next1_txt","action":{"event":{"name":"advance_wizard_step","context":{"completedStep":1,"hoseDrained":{"path":"/diag/hoseDrained"}}}}} ,
                    {"id":"btn_next1_txt","component":"Text","text":"1단계 완료 -> 2단계(펌프 임펠러 점검)로 진화"}
                ]}}""".replace("\n", "")
                val delayedRemainingCompMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                    {"id":"vid_guide","component":"Video","url":"https://support.appliance.local/oe-drain-filter-60s.mp4"},
                    {"id":"chk_hose","component":"CheckBox","label":"비상 배수 호스 마개를 열어 잔수를 먼저 제거했습니다","value":{"path":"/diag/hoseDrained"}}
                ]}}""".replace("\n", "")
                val emptyDataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"hoseDrained":true,"logs":[{"msg":"[초기 진단] 지연 스트리밍 대기 중..."}]}}}}"""
                listOf(createMsg, emptyDataMsg, initialSkeletonCompMsg) to listOf(fullDataMsg, delayedRemainingCompMsg)
            } else {
                listOf(createMsg, fullDataMsg, fullCompMsg) to emptyList()
            }
        }
        2 -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"spinRpm":800,"impellerClean":true,"testResult":["still_oe"]}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                {"id":"root","component":"Card","child":"col_step2"},
                {"id":"col_step2","component":"Column","children":["txt_s2_title","slider_rpm","chk_impeller","picker_result","btn_next2"]},
                {"id":"txt_s2_title","component":"Text","variant":"h3","text":"[2/3단계] 거름망 세척 후 단독 탈수 테스트"},
                {"id":"slider_rpm","component":"Slider","label":"시험 탈수 목표 회전수 (RPM)","min":400,"max":1200,"value":{"path":"/diag/spinRpm"}},
                {"id":"chk_impeller","component":"CheckBox","label":"내부 임펠러(날개)가 동전/머리핀 걸림 없이 회전함","value":{"path":"/diag/impellerClean"}},
                {"id":"picker_result","component":"ChoicePicker","label":"단독 탈수 3분 실행 후 상태","variant":"mutuallyExclusive","options":[{"label":"배수 정상 완료 (해결됨)","value":"resolved"},{"label":"여전히 OE 에러 발생 (배수모터 불량 의심)","value":"still_oe"}],"value":{"path":"/diag/testResult"}},
                {"id":"btn_next2","component":"Button","variant":"primary","child":"btn_next2_txt","action":{"event":{"name":"advance_wizard_step","context":{"completedStep":2,"testResult":{"path":"/diag/testResult"}}}}},
                {"id":"btn_next2_txt","component":"Text","text":"여전히 OE 발생 -> 3단계(출장 AS 예약) UI로 진화"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg) to emptyList()
        }
        else -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"visitDate":"2026-10-16T14:00:00Z","symptomNote":"필터 청소 완료했으나 배수모터 소음과 함께 OE 지속 발생"}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                {"id":"root","component":"Card","child":"col_step3"},
                {"id":"col_step3","component":"Column","children":["txt_s3_title","dt_visit","tf_symptom","btn_book"]},
                {"id":"txt_s3_title","component":"Text","variant":"h3","text":"[3/3단계] 배수 펌프 어셈블리 교체 출장 예약"},
                {"id":"dt_visit","component":"DateTimeInput","label":"희망 엔지니어 방문 일시","enableDate":true,"enableTime":true,"value":{"path":"/diag/visitDate"}},
                {"id":"tf_symptom","component":"TextField","label":"엔지니어 사전 전달 메모 (부품 지참용)","value":{"path":"/diag/symptomNote"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/diag/symptomNote"}}},"message":"증상 메모를 입력해주세요."}]},
                {"id":"btn_book","component":"Button","variant":"primary","child":"btn_book_txt","action":{"event":{"name":"confirm_repair_dispatch","context":{"partCode":"AHA75673404 (Drain Pump)","visitDate":{"path":"/diag/visitDate"},"symptomNote":{"path":"/diag/symptomNote"}}}}},
                {"id":"btn_book_txt","component":"Text","text":"배수펌프 부품 지참 출장 AS 확정"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg) to emptyList()
        }
    }
}
