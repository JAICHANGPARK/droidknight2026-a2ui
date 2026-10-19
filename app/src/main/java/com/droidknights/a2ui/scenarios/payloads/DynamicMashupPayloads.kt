package com.droidknights.a2ui.scenarios.payloads

import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.AgentPromptPreset

object DynamicMashupPayloads {

    val promptPresets: List<AgentPromptPreset> =
        listOf(
            AgentPromptPreset(
                id = 0,
                shortChipLabel = "일본 출국 3종 합체",
                userPromptText = "내일 일본 가는데 5만 엔 환전하고 해외 카드 차단 풀고 3일 여행자 보험도 켜줘.",
                agentIntroText = "환전·해외카드 차단 해제·여행자 보험 3개 메뉴를 한 장의 카드로 합쳤습니다. '한 번에 실행하기'를 눌러 후속 조치를 확인해 보세요.",
                schemaBadge = "mashup_surface · /japanTrip",
            ),
            AgentPromptPreset(
                id = 1,
                shortChipLabel = "음식 이물질 접수",
                userPromptText = "배달 음식에 이물질이 나왔어요. 수거 예약이랑 환불 접수할게요.",
                agentIntroText = "사진 확인, 수거 일시, 환불 계좌 선택 컨트롤만 조합했습니다.",
                schemaBadge = "mashup_surface · /claim (HAIR_IN_FOOD)",
            ),
            AgentPromptPreset(
                id = 2,
                shortChipLabel = "초인종 미준수 접수",
                userPromptText = "벨 누르지 말라고 했는데 초인종을 누르고 가셨어요.",
                agentIntroText = "비대면 요청 미준수 건에 필요한 체크박스 2개만 생성했습니다.",
                schemaBadge = "mashup_surface · /claim (DOORBELL_RUNG)",
            ),
            AgentPromptPreset(
                id = 3,
                shortChipLabel = "쿠폰 사후 적용",
                userPromptText = "방금 결제했는데 5,000원 할인 쿠폰 적용을 깜빡했어요.",
                agentIntroText = "주문 취소 없이 보유 쿠폰을 적용해 차액만 재결제합니다.",
                schemaBadge = "mashup_surface · /claim (MISSED_COUPON)",
            ),
            AgentPromptPreset(
                id = 4,
                shortChipLabel = "회식 차등 정산",
                userPromptText = "1차 186,000원, 2차 94,000원 나왔어. 비음주자 감면 정산표 만들어줘.",
                agentIntroText = "통화 포맷팅과 계좌 입력 검증이 포함된 차등 정산 시트입니다.",
                schemaBadge = "mashup_surface · /settlement",
            ),
        )

    val caseTitles: List<String> = promptPresets.map { it.shortChipLabel }

    fun schemaSummary(caseIndex: Int): String =
        promptPresets.getOrElse(caseIndex) { promptPresets.first() }.schemaBadge

    fun buildJsonl(caseIndex: Int): List<String> {
        val createMsg =
            """{"version":"v0.9.1","createSurface":{"surfaceId":"mashup_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

        return when (caseIndex) {
            0 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"japanTrip":{"jpyAmount":50000,"unblockOverseasCard":true,"blockDccKrw":true,"join3DayInsurance":true,"pickupAirport":"인천공항 제1터미널"}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                    {"id":"root","component":"Card","child":"col_japan"},
                    {"id":"col_japan","component":"Column","children":["txt_jp_title","slider_jpy","chk_card_unblock","chk_dcc","chk_insurance","btn_jp_exec"]},
                    {"id":"txt_jp_title","component":"Text","text":"✈️ 일본 출국 원스톱 제어판 (1/2단계)"},
                    {"id":"slider_jpy","component":"Slider","label":"엔화(JPY) 환전 금액","min":10000,"max":200000,"value":{"path":"/japanTrip/jpyAmount"}},
                    {"id":"chk_card_unblock","component":"CheckBox","label":"일본 현지 IC 카드 결제 차단 해제","value":{"path":"/japanTrip/unblockOverseasCard"}},
                    {"id":"chk_dcc","component":"CheckBox","label":"해외 원화결제(DCC) 수수료 차단","value":{"path":"/japanTrip/blockDccKrw"}},
                    {"id":"chk_insurance","component":"CheckBox","label":"3일 실속형 여행자 보험(4,200원) 가입","value":{"path":"/japanTrip/join3DayInsurance"}},
                    {"id":"btn_jp_exec","component":"Button","variant":"primary","child":"btn_jp_txt","action":{"event":{"name":"execute_cross_feature_mashup","context":{"mashupType":"JAPAN_TRIP_COMBO","payload":{"path":"/japanTrip"}}}}},
                    {"id":"btn_jp_txt","component":"Text","text":"3종 한 번에 실행하기 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            1 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"issueType":"HAIR_IN_FOOD","orderNo":"ORD-2026-991","pickupDateTime":"2026-10-16T11:00:00Z","refundAccount":["toss_main"]}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                    {"id":"root","component":"Card","child":"col_hair"},
                    {"id":"col_hair","component":"Column","children":["txt_hair_title","img_hair","dt_pickup","picker_account","btn_hair_submit"]},
                    {"id":"txt_hair_title","component":"Text","text":"🔍 이물질 접수 · 수거 및 환불 (1/2단계)"},
                    {"id":"img_hair","component":"Image","url":"https://claims.delivery.local/ord-2026-991-foreign.jpg","description":"첨부 사진 확인됨","variant":"header"},
                    {"id":"dt_pickup","component":"DateTimeInput","label":"수거 희망 일시","enableDate":true,"enableTime":true,"value":{"path":"/claim/pickupDateTime"}},
                    {"id":"picker_account","component":"ChoicePicker","label":"환불 계좌 선택","variant":"mutuallyExclusive","options":[{"label":"토스뱅크 주계좌","value":"toss_main"},{"label":"카카오뱅크 계좌","value":"kakao_sub"},{"label":"결제 카드 취소","value":"card_cancel"}],"value":{"path":"/claim/refundAccount"}},
                    {"id":"btn_hair_submit","component":"Button","variant":"primary","child":"btn_hair_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_HAIR_FOREIGN","payload":{"path":"/claim"}}}}},
                    {"id":"btn_hair_txt","component":"Text","text":"수거 예약 및 환불 접수 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            2 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"issueType":"DOORBELL_RUNG","orderNo":"ORD-2026-991","notifyRiderWarning":true,"pinDefaultSilentNote":true}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                    {"id":"root","component":"Card","child":"col_bell"},
                    {"id":"col_bell","component":"Column","children":["txt_bell_title","chk_rider_warn","chk_pin_default","btn_bell_submit"]},
                    {"id":"txt_bell_title","component":"Text","text":"🔕 비대면 요청 미준수 접수 (1/2단계)"},
                    {"id":"chk_rider_warn","component":"CheckBox","label":"배달 기사님께 벨 주의 알림 전달","value":{"path":"/claim/notifyRiderWarning"}},
                    {"id":"chk_pin_default","component":"CheckBox","label":"다음 주문에도 '벨 누르지 말아주세요' 고정","value":{"path":"/claim/pinDefaultSilentNote"}},
                    {"id":"btn_bell_submit","component":"Button","variant":"primary","child":"btn_bell_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_DOORBELL_RUNG","payload":{"path":"/claim"}}}}},
                    {"id":"btn_bell_txt","component":"Text","text":"요청사항 접수 및 보상 확인 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            3 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"issueType":"MISSED_COUPON","orderNo":"ORD-2026-991","selectedCoupon":["dk_vip_5000"]}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                    {"id":"root","component":"Card","child":"col_coupon"},
                    {"id":"col_coupon","component":"Column","children":["txt_cp_title","picker_coupon","btn_cp_rebill"]},
                    {"id":"txt_cp_title","component":"Text","text":"🎟️ 미적용 쿠폰 사후 적용 (1/2단계)"},
                    {"id":"picker_coupon","component":"ChoicePicker","label":"적용할 보유 쿠폰","variant":"mutuallyExclusive","options":[{"label":"VIP 5,000원 할인 쿠폰","value":"dk_vip_5000"},{"label":"주말 15% 할인 쿠폰 (-4,200원)","value":"weekend_15pct"}],"value":{"path":"/claim/selectedCoupon"}},
                    {"id":"btn_cp_rebill","component":"Button","variant":"primary","child":"btn_cp_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_MISSED_COUPON_REBILL","payload":{"path":"/claim"}}}}},
                    {"id":"btn_cp_txt","component":"Text","text":"쿠폰 적용 및 차액 재결제 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            else -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"settlement":{"groupName":"DroidKnights 뒤풀이 정산 (6명)","round1Total":186000,"round2Total":94000,"soberDiscountPct":50,"includeTaxi":true,"bankMemo":"토스뱅크 1000-1234-5678"}}}}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                    {"id":"root","component":"Card","child":"col_dutch"},
                    {"id":"col_dutch","component":"Column","children":["txt_title","txt_r1","txt_r2","slider_sober","chk_taxi","tf_bank","btn_send_kakao"]},
                    {"id":"txt_title","component":"Text","text":{"path":"/settlement/groupName"}},
                    {"id":"txt_r1","component":"Text","variant":"body","text":{"call":"formatString","args":{"value":"1차 총액 (6명): ${'$'}{formatCurrency(value:${'$'}{/settlement/round1Total}, currency:'KRW', decimals:0)}"}}},
                    {"id":"txt_r2","component":"Text","variant":"body","text":{"call":"formatString","args":{"value":"2차 총액 (4명): ${'$'}{formatCurrency(value:${'$'}{/settlement/round2Total}, currency:'KRW', decimals:0)}"}}},
                    {"id":"slider_sober","component":"Slider","label":"비음주자 감면율 (%)","min":0,"max":100,"value":{"path":"/settlement/soberDiscountPct"}},
                    {"id":"chk_taxi","component":"CheckBox","label":"심야 택시비(28,000원) 합산","value":{"path":"/settlement/includeTaxi"}},
                    {"id":"tf_bank","component":"TextField","label":"입금 계좌 (8자 이상)","value":{"path":"/settlement/bankMemo"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/settlement/bankMemo"}}},"message":"계좌를 입력해주세요."},{"condition":{"call":"length","args":{"value":{"path":"/settlement/bankMemo"},"min":8}},"message":"8자 이상 입력해주세요."}]},
                    {"id":"btn_send_kakao","component":"Button","variant":"primary","child":"btn_send_txt","checks":[{"condition":{"call":"length","args":{"value":{"path":"/settlement/bankMemo"},"min":8}},"message":"계좌 번호가 짧습니다."}],"action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"DUTCH_PAY_MULTI_ROUND","payload":{"path":"/settlement"}}}}},
                    {"id":"btn_send_txt","component":"Text","text":"정산표 확정 및 공유 설정 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
        }
    }

    /**
     * Stage 2: Triggered when `execute_cross_feature_mashup` or `submit_dynamic_payload` is received.
     */
    fun buildFollowUpMashupStepJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val schemaType =
            ctx["mashupType"]?.toString()
                ?: ctx["schemaType"]?.toString()
                ?: "JAPAN_TRIP_COMBO"
        val payload = ctx["payload"] as? Map<*, *>

        val (stepTitle, summaryText, optionLabel, opt1, opt2) =
            when (schemaType) {
                "JAPAN_TRIP_COMBO" -> {
                    val jpy = payload?.get("jpyAmount") ?: 50000
                    FollowUpSpec(
                        stepTitle = "✅ [2/2단계] 일본 출국 3종 접수 완료 · 부가 혜택 선택",
                        summaryText = "엔화 $jpy JPY 환전 · 해외 IC결제 활성화 · 3일 여행자보험 동시 가입됨",
                        optionLabel = "무료 제공 출국 부가 서비스 선택",
                        opt1 = "로밍 eSIM 3일 무료 쿠폰 발급" to "esim_coupon",
                        opt2 = "인천공항 스마트패스/라운지 할인권" to "lounge_pass",
                    )
                }
                "DUTCH_PAY_MULTI_ROUND" -> {
                    val soberPct = payload?.get("soberDiscountPct") ?: 50
                    val bank = payload?.get("bankMemo") ?: "토스뱅크 1000-1234-5678"
                    FollowUpSpec(
                        stepTitle = "✅ [2/2단계] 차등 정산표 산출 완료 · 송금 링크 발송",
                        summaryText = "비음주자 ${soberPct}% 감면 적용 · 입금 계좌: $bank",
                        optionLabel = "참석자 정산 요청 발송 채널",
                        opt1 = "카카오톡 단체방 1/N 송금 버튼 공유" to "kakao_group",
                        opt2 = "개별 문자(SMS) 및 계좌 복사 링크 발송" to "sms_link",
                    )
                }
                else -> {
                    val issue = payload?.get("issueType") ?: schemaType
                    FollowUpSpec(
                        stepTitle = "✅ [2/2단계] 고객지원 접수 완료 (CLM-2026-991) · 후속 안내",
                        summaryText = "접수 유형: $issue · 주문번호 ORD-2026-991 즉시 반영됨",
                        optionLabel = "추가 보상 및 진행 알림 방식",
                        opt1 = "즉시 사용 가능한 사과 쿠폰(3,000원) 함께 받기" to "apology_coupon",
                        opt2 = "처리 완료 시 앱 푸시 및 알림톡 수신" to "kakao_alert",
                    )
                }
            }

        val agentReply =
            "전송된 데이터 모델($summaryText)이 정상 접수되었습니다. 이어서 후속 옵션을 선택해 완료해 주세요."

        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/mashupNext","value":{"summaryText":"$summaryText","selectedOption":["${opt1.second}"],"sendReceipt":true,"memo":"빠른 처리 감사합니다"}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
              {"id":"root","component":"Card","child":"ms_next_col"},
              {"id":"ms_next_col","component":"Column","children":["ms_next_title","ms_next_summary","ms_picker_opt","ms_chk_receipt","ms_tf_memo","btn_ms_confirm","btn_ms_back"]},
              {"id":"ms_next_title","component":"Text","text":"$stepTitle"},
              {"id":"ms_next_summary","component":"Text","text":"• 처리 내역: $summaryText"},
              {"id":"ms_picker_opt","component":"ChoicePicker","label":"$optionLabel","variant":"mutuallyExclusive","options":[{"label":"${opt1.first}","value":"${opt1.second}"},{"label":"${opt2.first}","value":"${opt2.second}"}],"value":{"path":"/mashupNext/selectedOption"}},
              {"id":"ms_chk_receipt","component":"CheckBox","label":"처리 결과 상세 내역서 알림톡 수신","value":{"path":"/mashupNext/sendReceipt"}},
              {"id":"ms_tf_memo","component":"TextField","label":"추가 전달 메모","value":{"path":"/mashupNext/memo"}},
              {"id":"btn_ms_confirm","component":"Button","variant":"primary","child":"btn_ms_confirm_txt","action":{"event":{"name":"confirm_mashup_followup","context":{"summaryText":{"path":"/mashupNext/summaryText"},"selectedOption":{"path":"/mashupNext/selectedOption"},"sendReceipt":{"path":"/mashupNext/sendReceipt"},"memo":{"path":"/mashupNext/memo"}}}}},
              {"id":"btn_ms_confirm_txt","component":"Text","text":"후속 조치 최종 확정"},
              {"id":"btn_ms_back","component":"Button","variant":"borderless","child":"btn_ms_back_txt","action":{"event":{"name":"reset_mashup_surface","context":{"reason":"edit_again"}}}},
              {"id":"btn_ms_back_txt","component":"Text","text":"↩ 이전 시트 다시 수정하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateData, updateComponents)
    }

    /**
     * Stage 3: Triggered when `confirm_mashup_followup` is dispatched.
     */
    fun buildFinalMashupDoneJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val summaryText = ctx["summaryText"]?.toString() ?: "접수 완료"
        val option = (ctx["selectedOption"] as? List<*>)?.firstOrNull()?.toString() ?: "기본 옵션"
        val memo = ctx["memo"]?.toString() ?: ""
        val agentReply = "모든 요청과 후속 조치($option)가 최종 완료되었습니다."

        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
              {"id":"root","component":"Card","child":"ms_done_col"},
              {"id":"ms_done_col","component":"Column","children":["ms_done_title","ms_done_txt1","ms_done_txt2","ms_done_txt3","btn_ms_reset"]},
              {"id":"ms_done_title","component":"Text","text":"🎉 원샷 매시업 & 후속 조치 최종 완료"},
              {"id":"ms_done_txt1","component":"Text","text":"• 실행 완료: $summaryText"},
              {"id":"ms_done_txt2","component":"Text","text":"• 선택된 후속 옵션: $option"},
              {"id":"ms_done_txt3","component":"Text","text":"• 전달 메모: $memo"},
              {"id":"btn_ms_reset","component":"Button","variant":"primary","child":"btn_ms_reset_txt","action":{"event":{"name":"reset_mashup_surface","context":{"reason":"done"}}}},
              {"id":"btn_ms_reset_txt","component":"Text","text":"처음 시트로 돌아가기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateComponents)
    }

    private data class FollowUpSpec(
        val stepTitle: String,
        val summaryText: String,
        val optionLabel: String,
        val opt1: Pair<String, String>,
        val opt2: Pair<String, String>,
    )
}
