package com.droidknights.a2ui.scenarios.payloads

import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.AgentPromptPreset

object ContextualCockpitPayloads {

    val promptPresets: List<AgentPromptPreset> =
        listOf(
            AgentPromptPreset(
                id = 0,
                shortChipLabel = "아이방 수면 준비",
                userPromptText = "아기 재울 준비 해줘. 홈캠 틀고 자장가 켜고 에어컨 23.5도로 맞춰줘.",
                agentIntroText = "아이방 수면 루틴에 필요한 홈캠·자장가·온도·수유등 4개 제어만 모았습니다. 값을 조정하고 '수면 루틴 적용'을 눌러보세요.",
                schemaBadge = "cockpit_surface · /nursery",
            ),
            AgentPromptPreset(
                id = 1,
                shortChipLabel = "차박 캠핑 모드",
                userPromptText = "차 캠핑 모드로 바꿔줘. V2L 전력량이랑 실내 히터 온도 설정할래.",
                agentIntroText = "V2L 전력 게이지(2.8/3.6kW)와 히트펌프·배터리 하한선 제어판입니다.",
                schemaBadge = "cockpit_surface · /ev",
            ),
            AgentPromptPreset(
                id = 2,
                shortChipLabel = "외출 보안·밸브 점검",
                userPromptText = "외출 보안 켜면서 세탁실 급수 밸브 확인하고 도어락 원격 해제 준비해줘.",
                agentIntroText = "현관 스냅샷, 급수 배관 #2 유량 도면, 원격 도어락 제어를 조합했습니다.",
                schemaBadge = "cockpit_surface · /security",
            ),
            AgentPromptPreset(
                id = 3,
                shortChipLabel = "거실 영화관 모드",
                userPromptText = "거실 영화관 모드 켜줘. 프로젝터랑 사운드바 볼륨, 암막 커튼 조절할래.",
                agentIntroText = "4K 프로젝터, 사운드바 볼륨, 전동 암막 커튼 통합 제어 카드입니다.",
                schemaBadge = "cockpit_surface · /cinema",
            ),
            AgentPromptPreset(
                id = 4,
                shortChipLabel = "반려견 원격 케어",
                userPromptText = "강아지 혼자 있는데 거실 펫캠 보여주고 자동 급식기랑 온도 조절할래.",
                agentIntroText = "거실 펫캠 스냅샷, 실내 온도 다이얼, 급식기 사료 토출 제어판입니다.",
                schemaBadge = "cockpit_surface · /petCare",
            ),
        )

    val routineLabels: List<String> = promptPresets.map { it.shortChipLabel }

    fun buildJsonl(routineIndex: Int): List<String> {
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
                    {"id":"txt_title","component":"Text","text":"🌙 아이방 수면 집중 제어 (1/2단계)"},
                    {"id":"vid_cam","component":"Video","url":"rtsp://home.local/nursery-cam-hd"},
                    {"id":"aud_lullaby","component":"AudioPlayer","url":"https://audio.home.local/white-noise-rain.flac","description":"백색소음 · 빗소리 (42dB)"},
                    {"id":"dial_temp","component":"CircularClimateDial","label":"무풍 에어컨 목표 온도","unit":"°C","min":18,"max":28,"step":0.5,"value":{"path":"/nursery/targetTemp"}},
                    {"id":"slider_dimmer","component":"Slider","label":"수유등 밝기 (%)","min":0,"max":100,"value":{"path":"/nursery/dimmerPct"}},
                    {"id":"chk_doorbell","component":"CheckBox","label":"현관 초인종 무음 전환","value":{"path":"/nursery/muteDoorbell"}},
                    {"id":"btn_apply","component":"Button","variant":"primary","child":"btn_apply_txt","action":{"event":{"name":"apply_nursery_scene","context":{"targetTemp":{"path":"/nursery/targetTemp"},"dimmerPct":{"path":"/nursery/dimmerPct"},"muteDoorbell":{"path":"/nursery/muteDoorbell"}}}}},
                    {"id":"btn_apply_txt","component":"Text","text":"수면 루틴 적용 및 타이머 설정 →"}
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
                    {"id":"txt_ev_title","component":"Text","text":"⛺ SDV 캠핑 & V2L 전력 제어 (1/2단계)"},
                    {"id":"budget_v2l","component":"PowerBudgetBar","title":"V2L 실시간 부하","usedKw":{"path":"/ev/usedKw"},"maxKw":{"path":"/ev/maxKw"},"detail":"인덕션(1.8kW) + 히트펌프(0.8kW)"},
                    {"id":"dial_cabin","component":"CircularClimateDial","label":"실내 히트펌프 온도","unit":"°C","min":16,"max":28,"step":0.5,"value":{"path":"/ev/cabinTemp"}},
                    {"id":"picker_floor","component":"ChoicePicker","label":"배터리 방전 하한선","variant":"mutuallyExclusive","options":[{"label":"20%","value":"20%"},{"label":"30% (권장)","value":"30%"},{"label":"40%","value":"40%"}],"value":{"path":"/ev/floorLimit"}},
                    {"id":"chk_fridge","component":"CheckBox","label":"트렁크 12V 냉장고 상시 전원","value":{"path":"/ev/keepFridge"}},
                    {"id":"btn_ev_apply","component":"Button","variant":"primary","child":"btn_ev_txt","action":{"event":{"name":"apply_ev_camping_mode","context":{"cabinTemp":{"path":"/ev/cabinTemp"},"floorLimit":{"path":"/ev/floorLimit"},"keepFridge":{"path":"/ev/keepFridge"}}}}},
                    {"id":"btn_ev_txt","component":"Text","text":"캠핑 모드 적용 및 기상 스케줄 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            2 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"cockpit_surface","path":"/","value":{"security":{"valveTitle":"다용도실 급수 배관","hotspot":"세탁기 분기 밸브 #2","shutoffValve":true}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
                    {"id":"root","component":"Card","child":"col_sec"},
                    {"id":"col_sec","component":"Column","children":["txt_sec_title","img_door","hotspot_valve","chk_valve","guard_unlock"]},
                    {"id":"txt_sec_title","component":"Text","text":"🛡️ 외출 보안 & 배관 점검 (1/2단계)"},
                    {"id":"img_door","component":"Image","url":"https://cam.home.local/front-door-last-snap.jpg","description":"현관 모션 스냅샷","variant":"header"},
                    {"id":"hotspot_valve","component":"HotspotDiagramCard","title":{"path":"/security/valveTitle"},"hotspotLabel":{"path":"/security/hotspot"},"zoneCode":"VALVE-02","status":"0.3L/h 감지"},
                    {"id":"chk_valve","component":"CheckBox","label":"외출 중 급수 밸브 자동 차단","value":{"path":"/security/shutoffValve"}},
                    {"id":"guard_unlock","component":"ActionGuardContainer","child":"btn_unlock","guardedActionName":"unlock_front_door","guardTitle":"원격 도어락 잠금 해제"},
                    {"id":"btn_unlock","component":"Button","variant":"primary","child":"btn_unlock_txt","action":{"event":{"name":"unlock_front_door","context":{"shutoffValve":{"path":"/security/shutoffValve"},"reason":"방문 기사 임시 출입"}}}},
                    {"id":"btn_unlock_txt","component":"Text","text":"도어락 1회 잠금 해제 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            3 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"cockpit_surface","path":"/","value":{"cinema":{"curtainClosePct":95,"soundbarVolume":65,"warmPopcornMaker":true}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
                    {"id":"root","component":"Card","child":"col_cinema"},
                    {"id":"col_cinema","component":"Column","children":["txt_cin_title","vid_trailer","aud_atmos","slider_curtain","dial_vol","chk_popcorn","btn_cin_apply"]},
                    {"id":"txt_cin_title","component":"Text","text":"🎬 거실 홈시네마 제어 (1/2단계)"},
                    {"id":"vid_trailer","component":"Video","url":"rtsp://home.local/projector-hdmi1-preview"},
                    {"id":"aud_atmos","component":"AudioPlayer","url":"https://audio.home.local/dolby-atmos-calibration.flac","description":"돌비 애트모스 7.1.4 채널"},
                    {"id":"slider_curtain","component":"Slider","label":"암막 커튼 차폐율 (%)","min":0,"max":100,"value":{"path":"/cinema/curtainClosePct"}},
                    {"id":"dial_vol","component":"CircularClimateDial","label":"사운드바 볼륨","unit":"%","min":0,"max":100,"step":5,"value":{"path":"/cinema/soundbarVolume"}},
                    {"id":"chk_popcorn","component":"CheckBox","label":"주방 워머 스마트 플러그 켜기","value":{"path":"/cinema/warmPopcornMaker"}},
                    {"id":"btn_cin_apply","component":"Button","variant":"primary","child":"btn_cin_txt","action":{"event":{"name":"apply_cinema_mode","context":{"payload":{"path":"/cinema"}}}}},
                    {"id":"btn_cin_txt","component":"Text","text":"홈시네마 적용 및 종료 타이머 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            else -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"cockpit_surface","path":"/","value":{"petCare":{"roomTemp":24.0,"feedGrams":80,"playCalmingMusic":true}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
                    {"id":"root","component":"Card","child":"col_pet"},
                    {"id":"col_pet","component":"Column","children":["txt_pet_title","img_petcam","dial_pet_temp","slider_feed","chk_music","btn_pet_apply"]},
                    {"id":"txt_pet_title","component":"Text","text":"🐶 반려견 원격 돌봄 (1/2단계)"},
                    {"id":"img_petcam","component":"Image","url":"https://cam.home.local/living-room-petcam-live.jpg","description":"거실 펫캠 스냅샷","variant":"header"},
                    {"id":"dial_pet_temp","component":"CircularClimateDial","label":"실내 유지 온도","unit":"°C","min":18,"max":28,"step":0.5,"value":{"path":"/petCare/roomTemp"}},
                    {"id":"slider_feed","component":"Slider","label":"급식기 사료 토출량 (g)","min":20,"max":200,"value":{"path":"/petCare/feedGrams"}},
                    {"id":"chk_music","component":"CheckBox","label":"안정 피아노 사운드 재생","value":{"path":"/petCare/playCalmingMusic"}},
                    {"id":"btn_pet_apply","component":"Button","variant":"primary","child":"btn_pet_txt","action":{"event":{"name":"dispense_pet_care","context":{"payload":{"path":"/petCare"}}}}},
                    {"id":"btn_pet_txt","component":"Text","text":"급여·온도 적용 및 반복 스케줄 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
        }
    }

    /**
     * Stage 2: Triggered when a primary cockpit routine button is clicked.
     */
    fun buildFollowUpAutomationJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val appliedSummary =
            when (event.type) {
                "apply_nursery_scene" ->
                    "목표 온도 ${ctx["targetTemp"] ?: 23.5}°C · 수유등 ${ctx["dimmerPct"] ?: 15}% · 초인종 무음(${ctx["muteDoorbell"] ?: true})"
                "apply_ev_camping_mode" ->
                    "히트펌프 ${ctx["cabinTemp"] ?: 21.5}°C · 배터리 하한 ${(ctx["floorLimit"] as? List<*>)?.firstOrNull() ?: "30%"}"
                "unlock_front_door" ->
                    "도어락 1회 임시 해제 완료 · 급수 밸브 자동 차단(${ctx["shutoffValve"] ?: true})"
                "apply_cinema_mode" -> {
                    val p = ctx["payload"] as? Map<*, *>
                    "암막 커튼 ${p?.get("curtainClosePct") ?: 95}% · 사운드바 볼륨 ${p?.get("soundbarVolume") ?: 65}%"
                }
                else -> {
                    val p = ctx["payload"] as? Map<*, *>
                    "실내 온도 ${p?.get("roomTemp") ?: 24.0}°C · 사료 토출 ${p?.get("feedGrams") ?: 80}g"
                }
            }
        val agentReply =
            "요청하신 기기 상태($appliedSummary)가 즉시 동기화되었습니다. 유지 시간 타이머와 자동 루틴 저장 여부를 선택해 주세요."

        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"cockpit_surface","path":"/cockpitNext","value":{"appliedSummary":"$appliedSummary","timerHours":4,"endBehavior":["eco_restore"],"savePreset":true,"usedKw":1.6,"maxKw":3.6}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
              {"id":"root","component":"Card","child":"ck_next_col"},
              {"id":"ck_next_col","component":"Column","children":["ck_next_title","ck_next_budget","ck_next_summary","ck_slider_timer","ck_picker_end","ck_chk_preset","btn_ck_confirm","btn_ck_back"]},
              {"id":"ck_next_title","component":"Text","text":"✨ [2/2단계] 기기 동기화 완료 · 유지 타이머 설정"},
              {"id":"ck_next_budget","component":"PowerBudgetBar","title":"통합 루틴 실시간 소비 전력","usedKw":{"path":"/cockpitNext/usedKw"},"maxKw":{"path":"/cockpitNext/maxKw"},"detail":"모든 기기가 정상 응답했습니다"},
              {"id":"ck_next_summary","component":"Text","text":"• 동기화 내역: $appliedSummary"},
              {"id":"ck_slider_timer","component":"Slider","label":"루틴 유지 시간 (시간, 1~12h)","min":1,"max":12,"value":{"path":"/cockpitNext/timerHours"}},
              {"id":"ck_picker_end","component":"ChoicePicker","label":"타이머 종료 시 동작","variant":"mutuallyExclusive","options":[{"label":"절전/기본 상태로 자동 복귀","value":"eco_restore"},{"label":"종료 10분 전 연장 알림 발송","value":"ask_extend"}],"value":{"path":"/cockpitNext/endBehavior"}},
              {"id":"ck_chk_preset","component":"CheckBox","label":"현재 설정값을 '내 맞춤 원터치 루틴'으로 저장","value":{"path":"/cockpitNext/savePreset"}},
              {"id":"btn_ck_confirm","component":"Button","variant":"primary","child":"btn_ck_confirm_txt","action":{"event":{"name":"confirm_cockpit_automation","context":{"appliedSummary":{"path":"/cockpitNext/appliedSummary"},"timerHours":{"path":"/cockpitNext/timerHours"},"endBehavior":{"path":"/cockpitNext/endBehavior"},"savePreset":{"path":"/cockpitNext/savePreset"}}}}},
              {"id":"btn_ck_confirm_txt","component":"Text","text":"유지 타이머 및 자동화 확정"},
              {"id":"btn_ck_back","component":"Button","variant":"borderless","child":"btn_ck_back_txt","action":{"event":{"name":"reset_cockpit_surface","context":{"reason":"edit_devices"}}}},
              {"id":"btn_ck_back_txt","component":"Text","text":"↩ 기기 온도·밝기 다시 조절하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateData, updateComponents)
    }

    /**
     * Stage 3: Triggered when `confirm_cockpit_automation` is dispatched.
     */
    fun buildFinalCockpitDoneJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val summary = ctx["appliedSummary"]?.toString() ?: "기기 동기화 완료"
        val timerHours = (ctx["timerHours"] as? Number)?.toInt() ?: 4
        val savePreset = ctx["savePreset"] as? Boolean ?: true
        val presetText = if (savePreset) "즐겨찾기 루틴 등록됨" else "1회성 실행"
        val agentReply = "${timerHours}시간 유지 타이머와 루틴 설정($presetText)이 최종 등록되었습니다."

        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"cockpit_surface","components":[
              {"id":"root","component":"Card","child":"ck_done_col"},
              {"id":"ck_done_col","component":"Column","children":["ck_done_title","ck_done_txt1","ck_done_txt2","btn_ck_reset"]},
              {"id":"ck_done_title","component":"Text","text":"✅ 스마트 콕핏 루틴 & 타이머 가동 중"},
              {"id":"ck_done_txt1","component":"Text","text":"• 적용 설정: $summary"},
              {"id":"ck_done_txt2","component":"Text","text":"• 유지 타이머: ${timerHours}시간 후 자동 전환 ($presetText)"},
              {"id":"btn_ck_reset","component":"Button","variant":"primary","child":"btn_ck_reset_txt","action":{"event":{"name":"reset_cockpit_surface","context":{"reason":"done"}}}},
              {"id":"btn_ck_reset_txt","component":"Text","text":"콕핏 초기 화면으로 돌아가기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateComponents)
    }
}
