package com.droidknights.a2ui.scenarios.payloads

import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.AgentPromptPreset

object FieldDiagnosticsPayloads {

    val promptPresets: List<AgentPromptPreset> =
        listOf(
            AgentPromptPreset(
                id = 1,
                shortChipLabel = "OE 1단계 (필터점검)",
                userPromptText = "세탁기에 'OE' 에러가 뜨고 물이 안 빠져요. 어디를 열어야 하나요?",
                agentIntroText = "OE(배수 불량) 1단계입니다. 우측 하단 배수 필터 위치와 가이드 영상을 확인하고 '다음 단계'를 눌러보세요.",
                schemaBadge = "diag_surface · Step 1/3",
            ),
            AgentPromptPreset(
                id = 2,
                shortChipLabel = "OE 2단계 (탈수테스트)",
                userPromptText = "배수 필터 청소했어요. 이제 어떻게 테스트하나요?",
                agentIntroText = "2단계(단독 탈수 테스트) 카드입니다. 목표 회전수를 설정하고 결과를 선택하세요.",
                schemaBadge = "diag_surface · Step 2/3",
            ),
            AgentPromptPreset(
                id = 3,
                shortChipLabel = "OE 3단계 (출장예약)",
                userPromptText = "탈수를 돌려도 계속 OE가 뜨고 모터 소리가 나요. 방문 예약해 주세요.",
                agentIntroText = "배수 펌프 부품 지참 방문 예약 카드입니다. 희망 일시를 선택하세요.",
                schemaBadge = "diag_surface · Step 3/3",
            ),
            AgentPromptPreset(
                id = 4,
                shortChipLabel = "UE 불균형 진단",
                userPromptText = "탈수 중에 'UE' 에러가 뜨면서 통이 멈췄어요.",
                agentIntroText = "UE(세탁물 쏠림) 진단 도면과 저속 탈수 복구 제어판입니다.",
                schemaBadge = "diag_surface · UE",
            ),
            AgentPromptPreset(
                id = 5,
                shortChipLabel = "dE 도어 에러 진단",
                userPromptText = "문을 닫았는데도 'dE' 에러가 뜨면서 시작이 안 돼요.",
                agentIntroText = "dE(도어 미잠김) 래치 스위치 위치와 원격 자가진단 카드입니다.",
                schemaBadge = "diag_surface · dE",
            ),
        )

    val wizardSteps: List<Pair<Int, String>> =
        promptPresets.map { it.id to it.shortChipLabel }

    fun buildAppendLogJsonl(nextIdx: Int): String =
        """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/diag/logs/-","value":{"msg":"[센서 #$nextIdx] 배수 수위 센서 응답 확인 (정상)"}}}"""

    fun buildJsonl(
        step: Int,
        delayHotspotData: Boolean,
    ): Pair<List<String>, List<String>> {
        val createMsg =
            """{"version":"v0.9.1","createSurface":{"surfaceId":"diag_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID_V0_9_1_ALIAS}","sendDataModel":true}}"""

        return when (step) {
            1 -> {
                val fullCompMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                    {"id":"root","component":"Card","child":"col_step1"},
                    {"id":"col_step1","component":"Column","children":["txt_s1_title","hotspot_filter","vid_guide","chk_hose","list_logs","btn_next1"]},
                    {"id":"txt_s1_title","component":"Text","text":"🔧 [1/3단계] OE 배수 필터 위치 확인"},
                    {"id":"hotspot_filter","component":"HotspotDiagramCard","title":{"path":"/diag/partTitle"},"hotspotLabel":{"path":"/diag/hotspotLabel"},"zoneCode":"PUMP-B2","status":"이물질 점검"},
                    {"id":"vid_guide","component":"Video","url":"https://support.appliance.local/oe-drain-filter-60s.mp4"},
                    {"id":"chk_hose","component":"CheckBox","label":"잔수 제거 호스로 물을 먼저 뺐습니다","value":{"path":"/diag/hoseDrained"}},
                    {"id":"list_logs","component":"Column","children":{"path":"/diag/logs","componentId":"tpl_log_item"}},
                    {"id":"tpl_log_item","component":"Text","variant":"caption","text":{"path":"msg"}},
                    {"id":"btn_next1","component":"Button","variant":"primary","child":"btn_next1_txt","action":{"event":{"name":"advance_wizard_step","context":{"completedStep":1,"hoseDrained":{"path":"/diag/hoseDrained"}}}}},
                    {"id":"btn_next1_txt","component":"Text","text":"다음 단계 (단독 탈수 테스트) →"}
                ]}}""".replace("\n", "")

                val fullDataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"partTitle":"전면 우측 하단 서비스 커버","hotspotLabel":"잔수 호스 및 배수 필터","hoseDrained":true,"logs":[{"msg":"[진단] 에러코드 OE 감지 · 배수 필터 점검"}]}}}}"""

                if (delayHotspotData) {
                    val initialSkeletonCompMsg =
                        """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                        {"id":"root","component":"Card","child":"col_step1"},
                        {"id":"col_step1","component":"Column","children":["txt_s1_title","hotspot_filter","vid_guide","chk_hose","list_logs","btn_next1"]},
                        {"id":"txt_s1_title","component":"Text","text":"🔧 [1/3단계] OE 배수 필터 위치 확인"},
                        {"id":"hotspot_filter","component":"HotspotDiagramCard","title":{"path":"/diag/partTitle"},"hotspotLabel":{"path":"/diag/hotspotLabel"},"zoneCode":"PUMP-B2","status":"이물질 점검"},
                        {"id":"list_logs","component":"Column","children":{"path":"/diag/logs","componentId":"tpl_log_item"}},
                        {"id":"tpl_log_item","component":"Text","variant":"caption","text":{"path":"msg"}},
                        {"id":"btn_next1","component":"Button","variant":"primary","child":"btn_next1_txt","action":{"event":{"name":"advance_wizard_step","context":{"completedStep":1,"hoseDrained":{"path":"/diag/hoseDrained"}}}}},
                        {"id":"btn_next1_txt","component":"Text","text":"다음 단계 (단독 탈수 테스트) →"}
                    ]}}""".replace("\n", "")
                    val delayedRemainingCompMsg =
                        """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                        {"id":"vid_guide","component":"Video","url":"https://support.appliance.local/oe-drain-filter-60s.mp4"},
                        {"id":"chk_hose","component":"CheckBox","label":"잔수 제거 호스로 물을 먼저 뺐습니다","value":{"path":"/diag/hoseDrained"}}
                    ]}}""".replace("\n", "")
                    val emptyDataMsg =
                        """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"hoseDrained":true,"logs":[{"msg":"[진단] 데이터 스트리밍 대기 중..."}]}}}}"""
                    listOf(createMsg, emptyDataMsg, initialSkeletonCompMsg) to
                        listOf(fullDataMsg, delayedRemainingCompMsg)
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
                    {"id":"txt_s2_title","component":"Text","text":"🔧 [2/3단계] 단독 탈수 테스트"},
                    {"id":"slider_rpm","component":"Slider","label":"시험 탈수 회전수 (RPM)","min":400,"max":1200,"value":{"path":"/diag/spinRpm"}},
                    {"id":"chk_impeller","component":"CheckBox","label":"내부 임펠러 회전 확인","value":{"path":"/diag/impellerClean"}},
                    {"id":"picker_result","component":"ChoicePicker","label":"단독 탈수 후 상태","variant":"mutuallyExclusive","options":[{"label":"배수 정상 완료 (자가 해결)","value":"resolved"},{"label":"여전히 OE 에러 발생 (출장 예약)","value":"still_oe"}],"value":{"path":"/diag/testResult"}},
                    {"id":"btn_next2","component":"Button","variant":"primary","child":"btn_next2_txt","action":{"event":{"name":"advance_wizard_step","context":{"completedStep":2,"spinRpm":{"path":"/diag/spinRpm"},"testResult":{"path":"/diag/testResult"}}}}},
                    {"id":"btn_next2_txt","component":"Text","text":"결과 전송 및 다음 단계 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg) to emptyList()
            }
            3 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"visitDate":"2026-10-16T14:00:00Z","symptomNote":"필터 청소 후에도 OE 지속 발생"}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                    {"id":"root","component":"Card","child":"col_step3"},
                    {"id":"col_step3","component":"Column","children":["txt_s3_title","dt_visit","tf_symptom","btn_book"]},
                    {"id":"txt_s3_title","component":"Text","text":"🔧 [3/3단계] 배수 펌프 교체 출장 예약"},
                    {"id":"dt_visit","component":"DateTimeInput","label":"방문 희망 일시","enableDate":true,"enableTime":true,"value":{"path":"/diag/visitDate"}},
                    {"id":"tf_symptom","component":"TextField","label":"증상 메모","value":{"path":"/diag/symptomNote"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/diag/symptomNote"}}},"message":"메모를 입력해주세요."}]},
                    {"id":"btn_book","component":"Button","variant":"primary","child":"btn_book_txt","action":{"event":{"name":"confirm_repair_dispatch","context":{"partCode":"AHA75673404","visitDate":{"path":"/diag/visitDate"},"symptomNote":{"path":"/diag/symptomNote"}}}}},
                    {"id":"btn_book_txt","component":"Text","text":"출장 AS 예약 접수 및 기사 배정 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg) to emptyList()
            }
            4 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"partTitle":"4축 서스펜션 댐퍼 & 수평 센서","hotspotLabel":"우측 후면 댐퍼 진동 감지","targetSpinRpm":600,"autoRedistribute":true}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                    {"id":"root","component":"Card","child":"col_ue"},
                    {"id":"col_ue","component":"Column","children":["txt_ue_title","hotspot_ue","slider_ue_rpm","chk_ue_redist","btn_ue_run"]},
                    {"id":"txt_ue_title","component":"Text","text":"⚖️ UE 탈수 불균형 복구 (1/2단계)"},
                    {"id":"hotspot_ue","component":"HotspotDiagramCard","title":{"path":"/diag/partTitle"},"hotspotLabel":{"path":"/diag/hotspotLabel"},"zoneCode":"DAMPER-R2","status":"수평 편차 감지"},
                    {"id":"slider_ue_rpm","component":"Slider","label":"안전 탈수 회전수 (RPM)","min":400,"max":1000,"value":{"path":"/diag/targetSpinRpm"}},
                    {"id":"chk_ue_redist","component":"CheckBox","label":"포 풀림 헹굼 후 탈수 재시도","value":{"path":"/diag/autoRedistribute"}},
                    {"id":"btn_ue_run","component":"Button","variant":"primary","child":"btn_ue_txt","action":{"event":{"name":"run_unbalance_recovery","context":{"payload":{"path":"/diag"}}}}},
                    {"id":"btn_ue_txt","component":"Text","text":"저속 탈수 재시작 및 진동 측정 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg) to emptyList()
            }
            else -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/","value":{"diag":{"partTitle":"전면 도어 래치 & 가스켓","hotspotLabel":"우측 도어락 스위치 유격","gasketChecked":true,"childLockOff":true}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
                    {"id":"root","component":"Card","child":"col_de"},
                    {"id":"col_de","component":"Column","children":["txt_de_title","hotspot_de","chk_de_gasket","chk_de_lock","btn_de_test"]},
                    {"id":"txt_de_title","component":"Text","text":"🚪 dE 도어 래치 자가진단 (1/2단계)"},
                    {"id":"hotspot_de","component":"HotspotDiagramCard","title":{"path":"/diag/partTitle"},"hotspotLabel":{"path":"/diag/hotspotLabel"},"zoneCode":"LATCH-01","status":"접점 미인식"},
                    {"id":"chk_de_gasket","component":"CheckBox","label":"고무 패킹 끼임 없음 확인","value":{"path":"/diag/gasketChecked"}},
                    {"id":"chk_de_lock","component":"CheckBox","label":"어린이 보호 잠금 해제 후 재검증","value":{"path":"/diag/childLockOff"}},
                    {"id":"btn_de_test","component":"Button","variant":"primary","child":"btn_de_txt","action":{"event":{"name":"run_door_latch_self_test","context":{"payload":{"path":"/diag"}}}}},
                    {"id":"btn_de_txt","component":"Text","text":"도어 자가진단 실행 및 결과 확인 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg) to emptyList()
            }
        }
    }

    /**
     * Follow-up surface when Step 2 resolves the issue (`testResult == ["resolved"]`),
     * or when Step 3 (`confirm_repair_dispatch`), Step 4 (`run_unbalance_recovery`),
     * or Step 5 (`run_door_latch_self_test`) is submitted.
     */
    fun buildFollowUpDiagnosticsStepJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val (title, detailText, optionLabel, opt1, opt2) =
            when (event.type) {
                "advance_wizard_step" -> {
                    val rpm = ctx["spinRpm"] ?: 800
                    DiagFollowUpSpec(
                        title = "🎉 배수 정상 완료 · 자가진단 해결 (출장비 절약)",
                        detailText = "${rpm} RPM 단독 탈수에서 배수가 정상 완료되었습니다.",
                        optionLabel = "향후 배수 필터 청소 알림 주기",
                        opt1 = "30일마다 세척 푸시 알림 받기 (권장)" to "cycle_30d",
                        opt2 = "60일마다 세척 푸시 알림 받기" to "cycle_60d",
                    )
                }
                "confirm_repair_dispatch" -> {
                    val visitDate = ctx["visitDate"]?.toString() ?: "2026-10-16T14:00:00Z"
                    val partCode = ctx["partCode"]?.toString() ?: "AHA75673404"
                    DiagFollowUpSpec(
                        title = "🧑‍🔧 출장 엔지니어 배정 완료 · 방문 전 사전 안내",
                        detailText = "희망 일시($visitDate) · 지참 부품: 배수 펌프($partCode)",
                        optionLabel = "엔지니어 방문 시 주차 및 출입 안내",
                        opt1 = "아파트 방문차량 사전등록 완료" to "parking_registered",
                        opt2 = "도착 시 인터폰 호출 요청" to "intercom_call",
                    )
                }
                "run_unbalance_recovery" -> {
                    val p = ctx["payload"] as? Map<*, *>
                    val rpm = p?.get("targetSpinRpm") ?: 600
                    DiagFollowUpSpec(
                        title = "✅ UE 포 풀림 헹굼 및 ${rpm} RPM 저속 탈수 완료",
                        detailText = "진동 센서 편차가 정상 범위(0.2G 이하)로 복구되었습니다.",
                        optionLabel = "후속 세탁 코스 동작 선택",
                        opt1 = "건조 코스로 자동 이어서 진행" to "auto_dry_next",
                        opt2 = "종료 후 세탁물 구김 방지 회전 유지" to "wrinkle_care",
                    )
                }
                else -> {
                    DiagFollowUpSpec(
                        title = "✅ dE 도어 래치 접점 복구 완료 · 세탁 재개 설정",
                        detailText = "도어락 마그네틱 스위치 잠금이 정상 인식되었습니다.",
                        optionLabel = "중단되었던 세탁 코스 재개 방식",
                        opt1 = "중단 시점(헹굼 2회)부터 즉시 이어하기" to "resume_wash",
                        opt2 = "표준 세탁 코스로 처음부터 다시 시작" to "restart_wash",
                    )
                }
            }

        val agentReply = "$detailText 후속 설정을 선택하고 확정해 주세요."

        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"diag_surface","path":"/diagNext","value":{"detailText":"$detailText","selectedOption":["${opt1.second}"],"callBeforeVisit":true,"extraMemo":"방문 30분 전 연락 부탁드립니다"}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
              {"id":"root","component":"Card","child":"dg_next_col"},
              {"id":"dg_next_col","component":"Column","children":["dg_next_title","dg_next_detail","dg_picker_opt","dg_chk_call","dg_tf_memo","btn_dg_finalize","btn_dg_back"]},
              {"id":"dg_next_title","component":"Text","text":"$title"},
              {"id":"dg_next_detail","component":"Text","text":"• 진단/접수 요약: $detailText"},
              {"id":"dg_picker_opt","component":"ChoicePicker","label":"$optionLabel","variant":"mutuallyExclusive","options":[{"label":"${opt1.first}","value":"${opt1.second}"},{"label":"${opt2.first}","value":"${opt2.second}"}],"value":{"path":"/diagNext/selectedOption"}},
              {"id":"dg_chk_call","component":"CheckBox","label":"진행 상태 변경 시 카카오톡 알림톡 수신","value":{"path":"/diagNext/callBeforeVisit"}},
              {"id":"dg_tf_memo","component":"TextField","label":"추가 요청 메모","value":{"path":"/diagNext/extraMemo"}},
              {"id":"btn_dg_finalize","component":"Button","variant":"primary","child":"btn_dg_finalize_txt","action":{"event":{"name":"finalize_diagnostics_followup","context":{"detailText":{"path":"/diagNext/detailText"},"selectedOption":{"path":"/diagNext/selectedOption"},"extraMemo":{"path":"/diagNext/extraMemo"}}}}},
              {"id":"btn_dg_finalize_txt","component":"Text","text":"후속 설정 최종 확정"},
              {"id":"btn_dg_back","component":"Button","variant":"borderless","child":"btn_dg_back_txt","action":{"event":{"name":"reset_diag_surface","context":{"reason":"back"}}}},
              {"id":"btn_dg_back_txt","component":"Text","text":"↩ 이전 진단 카드로 돌아가기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateData, updateComponents)
    }

    /**
     * Stage 3: Final confirmation screen for Field Diagnostics.
     */
    fun buildFinalDiagnosticsDoneJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val detail = ctx["detailText"]?.toString() ?: "진단 및 예약 완료"
        val option = (ctx["selectedOption"] as? List<*>)?.firstOrNull()?.toString() ?: "기본 설정"
        val memo = ctx["extraMemo"]?.toString() ?: ""
        val agentReply = "가전 자가진단 및 후속 조치($option)가 모두 완료되었습니다."

        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"diag_surface","components":[
              {"id":"root","component":"Card","child":"dg_done_col"},
              {"id":"dg_done_col","component":"Column","children":["dg_done_title","dg_done_txt1","dg_done_txt2","dg_done_txt3","btn_dg_reset"]},
              {"id":"dg_done_title","component":"Text","text":"✅ 가전 자가진단 & 후속 조치 최종 완료"},
              {"id":"dg_done_txt1","component":"Text","text":"• 처리 결과: $detail"},
              {"id":"dg_done_txt2","component":"Text","text":"• 후속 옵션: $option"},
              {"id":"dg_done_txt3","component":"Text","text":"• 요청 메모: $memo"},
              {"id":"btn_dg_reset","component":"Button","variant":"primary","child":"btn_dg_reset_txt","action":{"event":{"name":"reset_diag_surface","context":{"reason":"done"}}}},
              {"id":"btn_dg_reset_txt","component":"Text","text":"진단 초기 화면으로 돌아가기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateComponents)
    }

    private data class DiagFollowUpSpec(
        val title: String,
        val detailText: String,
        val optionLabel: String,
        val opt1: Pair<String, String>,
        val opt2: Pair<String, String>,
    )
}
