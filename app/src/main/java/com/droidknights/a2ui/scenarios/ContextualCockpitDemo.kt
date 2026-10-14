package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.launch

/**
 * Domain Scenario B: Smart Home / EV (SDV) Contextual Micro-Cockpit
 * Demonstrates `materialA2uiBasicCatalogV1` Media Hooks (`A2uiImageRenderer`, `A2uiVideoRenderer`,
 * `A2uiAudioPlayerRenderer`) combined with reusable `CircularClimateDial` & `PowerBudgetBar`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContextualCockpitDemo() {
    var selectedRoutine by remember { mutableIntStateOf(0) }

    val catalog = remember { DroidKnightsCatalogs.createStandardCatalog() }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedRoutine) {
            A2uiMessageProcessor(catalogs = listOf(catalog))
        }

    LaunchedEffect(processor) {
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                outboundLog.add(msg)
            }
        }
        val lines = buildCockpitJsonl(selectedRoutine)
        jsonlLines.clear()
        jsonlLines.addAll(lines)
        lines.forEach { line -> processor.processInput(parser, line) }
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
                    text = "🚗 시나리오 B · 스마트홈 / 전기차(SDV) 맥락형 마이크로 콕핏",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "40개 기기 화면을 미리 다 짜두는 대신, 사용자의 한마디 발화 맥락에 필요한 이종 기기 컨트롤만 " +
                            "1장의 마이크로 콕핏 카드로 즉석 합성합니다. materialA2uiBasicCatalogV1의 Video/Audio/Image 렌더러 훅이 함께 동작합니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val routines =
                        listOf(
                            "① \"아기 재울 준비 해줘\"",
                            "② \"차 캠핑 모드로 바꿔줘 (V2L)\"",
                            "③ \"외출 보안 + 세탁실 밸브 점검\"",
                        )
                    routines.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedRoutine == index,
                            onClick = { selectedRoutine = index },
                            label = { Text(label, fontSize = 12.sp) },
                        )
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

internal fun buildCockpitJsonl(routineIndex: Int): List<String> {
    val createMsg =
        """{"version":"v0.9.1","createSurface":{"surfaceId":"cockpit_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

    return when (routineIndex) {
        0 -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"cockpit_surface","path":"/","value":{"nursery":{"targetTemp":23.5,"dimmerPct":15,"muteDoorbell":true}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
                {"id":"root","component":"Card","child":"col_nursery"},
                {"id":"col_nursery","component":"Column","children":["txt_title","vid_cam","aud_lullaby","dial_temp","slider_dimmer","chk_doorbell","btn_apply"]},
                {"id":"txt_title","component":"Text","variant":"h3","text":"🌙 아이방 취침 집중 마이크로 콕핏"},
                {"id":"vid_cam","component":"Video","url":"rtsp://home.local/nursery-cam-hd"},
                {"id":"aud_lullaby","component":"AudioPlayer","url":"https://audio.home.local/white-noise-rain.flac","description":"백색소음 · 빗소리 루프 (42dB)"},
                {"id":"dial_temp","component":"CircularClimateDial","label":"아이방 무풍 에어컨 목표 온도","unit":"°C","min":18,"max":28,"step":0.5,"value":{"path":"/nursery/targetTemp"}},
                {"id":"slider_dimmer","component":"Slider","label":"간접 수유등 밝기 (%)","min":0,"max":100,"value":{"path":"/nursery/dimmerPct"}},
                {"id":"chk_doorbell","component":"CheckBox","label":"현관 초인종 벨소리 무음 전환 (앱 푸시만 수신)","value":{"path":"/nursery/muteDoorbell"}},
                {"id":"btn_apply","component":"Button","variant":"primary","child":"btn_apply_txt","action":{"event":{"name":"apply_nursery_scene","context":{"targetTemp":{"path":"/nursery/targetTemp"},"dimmerPct":{"path":"/nursery/dimmerPct"},"muteDoorbell":{"path":"/nursery/muteDoorbell"}}}}} ,
                {"id":"btn_apply_txt","component":"Text","text":"취침 루틴 기기 일괄 동기화"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        1 -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"cockpit_surface","path":"/","value":{"ev":{"cabinTemp":21.5,"usedKw":2.8,"maxKw":3.6,"keepFridge":true,"floorLimit":["30%"]}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
                {"id":"root","component":"Card","child":"col_ev"},
                {"id":"col_ev","component":"Column","children":["txt_ev_title","budget_v2l","dial_cabin","picker_floor","chk_fridge","btn_ev_apply"]},
                {"id":"txt_ev_title","component":"Text","variant":"h3","text":"⛺ SDV 캠핑 & V2L 전력 버짓 콕핏"},
                {"id":"budget_v2l","component":"PowerBudgetBar","title":"외부 V2L 인버터 실시간 부하","usedKw":{"path":"/ev/usedKw"},"maxKw":{"path":"/ev/maxKw"},"detail":"인덕션(1.8kW) + 히트펌프(0.8kW) + 조명(0.2kW) 가동 중"},
                {"id":"dial_cabin","component":"CircularClimateDial","label":"차박 실내 히트펌프 유지 온도","unit":"°C","min":16,"max":28,"step":0.5,"value":{"path":"/ev/cabinTemp"}},
                {"id":"picker_floor","component":"ChoicePicker","label":"배터리 방전 하한선 (귀가 주행거리 보호)","variant":"mutuallyExclusive","options":[{"label":"20% (최대 캠핑 시간)","value":"20%"},{"label":"30% (권장 · 110km 확보)","value":"30%"},{"label":"40% (안전 우선)","value":"40%"}],"value":{"path":"/ev/floorLimit"}},
                {"id":"chk_fridge","component":"CheckBox","label":"트렁크 12V 캠핑 냉장고 상시 전원 유지","value":{"path":"/ev/keepFridge"}},
                {"id":"btn_ev_apply","component":"Button","variant":"primary","child":"btn_ev_txt","action":{"event":{"name":"apply_ev_camping_mode","context":{"cabinTemp":{"path":"/ev/cabinTemp"},"floorLimit":{"path":"/ev/floorLimit"},"keepFridge":{"path":"/ev/keepFridge"}}}}},
                {"id":"btn_ev_txt","component":"Text","text":"캠핑 유틸리티 모드 확정"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        else -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"cockpit_surface","path":"/","value":{"security":{"valveTitle":"다용도실 메인 급수 배관","hotspot":"세탁기 분기 밸브 #2 (미세 유량 감지)","shutoffValve":true}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
                {"id":"root","component":"Card","child":"col_sec"},
                {"id":"col_sec","component":"Column","children":["txt_sec_title","img_door","hotspot_valve","chk_valve","guard_unlock"]},
                {"id":"txt_sec_title","component":"Text","variant":"h3","text":"🛡️ 외출 방범 & 배관 이상 통합 브리핑"},
                {"id":"img_door","component":"Image","url":"https://cam.home.local/front-door-last-snap.jpg","description":"현관 모션 카메라 최근 스냅샷 (방금 전)","variant":"header"},
                {"id":"hotspot_valve","component":"HotspotDiagramCard","title":{"path":"/security/valveTitle"},"hotspotLabel":{"path":"/security/hotspot"},"zoneCode":"VALVE-02","status":"0.3L/h 감지"},
                {"id":"chk_valve","component":"CheckBox","label":"외출 중 세탁실 전동 급수 밸브 자동 차단","value":{"path":"/security/shutoffValve"}},
                {"id":"guard_unlock","component":"ActionGuardContainer","child":"btn_unlock","guardedActionName":"unlock_front_door","guardTitle":"원격 현관 도어락 잠금 해제 (2FA 인증 필수)"},
                {"id":"btn_unlock","component":"Button","variant":"primary","child":"btn_unlock_txt","action":{"event":{"name":"unlock_front_door","context":{"shutoffValve":{"path":"/security/shutoffValve"},"reason":"방문 점검 기사 임시 출입"}}}},
                {"id":"btn_unlock_txt","component":"Text","text":"현관 도어락 1회 임시 잠금 해제"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
    }
}
