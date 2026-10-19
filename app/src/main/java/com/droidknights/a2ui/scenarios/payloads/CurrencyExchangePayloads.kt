package com.droidknights.a2ui.scenarios.payloads

import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.AgentPromptPreset
import java.text.NumberFormat
import java.util.Locale

object CurrencyExchangePayloads {

    val promptPresets =
        listOf(
            AgentPromptPreset(
                id = 0,
                shortChipLabel = "인천공항 5만엔",
                userPromptText = "내일 인천공항 1터미널 수령으로 5만 엔 환전해 줘.",
                agentIntroText = "인천공항 제1터미널 수령 기준 50,000 JPY(90% 우대) 신청서를 준비했습니다. 외화 또는 원화를 수정하고 '환전 신청 및 수령방법 선택'을 눌러보세요.",
                schemaBadge = "exchange_surface · /exchange/jpyAmount",
            ),
            AgentPromptPreset(
                id = 1,
                shortChipLabel = "달러 소액권",
                userPromptText = "뉴욕 출장용 1,200달러 환전할래. 소액권 비중 높여줘.",
                agentIntroText = "1,200 USD 환전 및 소액권(40%) 배분 카드를 준비했습니다. USD 또는 원화(KRW)를 입력해 금액을 조정할 수 있습니다.",
                schemaBadge = "exchange_surface · /exchange/usdAmount",
            ),
            AgentPromptPreset(
                id = 2,
                shortChipLabel = "유로+프랑 동시",
                userPromptText = "유로 800유로랑 스위스 300프랑 한 번에 환전해 줘.",
                agentIntroText = "EUR 800 + CHF 300 동시 환전 시트입니다. 각 통화별 외화·원화 양방향 입력 후 1회 인증으로 처리됩니다.",
                schemaBadge = "exchange_surface · /exchange/eurAmount",
            ),
            AgentPromptPreset(
                id = 3,
                shortChipLabel = "목표환율 예약",
                userPromptText = "엔화 900원 이하로 내려가면 10만 엔 자동 환전 걸어줘.",
                agentIntroText = "100엔당 900원 목표환율 자동 매수 예약 카드입니다.",
                schemaBadge = "exchange_surface · /exchange/targetRate",
            ),
            AgentPromptPreset(
                id = 4,
                shortChipLabel = "엔화 재환전",
                userPromptText = "남은 18,000엔 현찰 내 계좌로 재환전 입금할래.",
                agentIntroText = "보유 현찰 18,000 JPY 원화 재환전(80% 우대) 입금 시트입니다.",
                schemaBadge = "exchange_surface · /exchange/sellJpyAmount",
            ),
        )

    fun buildJsonl(caseIndex: Int): List<String> {
        val createSurface =
            """{"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

        return when (caseIndex) {
            0 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":50000,"krwAmount":452060,"branch":["ICN_T1"],"coupon90":true,"account":"110-482-******","accountPin":"1234"}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","rate_chart","exchange_converter","branch_picker","coupon_chk","pin_input","btn_submit"]},
                      {"id":"title","component":"Text","text":"💴 JPY 인천공항 원샷 환전 (1/2단계)"},
                      {"id":"rate_chart","component":"MetricSparkline","metricName":"JPY/KRW (90% 우대)","currentValue":"904.12 원","severity":"normal","points":[0.75,0.68,0.55,0.42,0.31,0.22]},
                      {"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액 (외화 ↔ 원화 자동 계산)","exchangeRate":904.12,"unitScale":100,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"}},
                      {"id":"branch_picker","component":"ChoicePicker","label":"수령 지점","variant":"mutuallyExclusive","options":[{"label":"인천공항 제1터미널","value":"ICN_T1"},{"label":"인천공항 제2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},
                      {"id":"coupon_chk","component":"CheckBox","label":"주거래 급여통장 출금 · 90% 환율 우대","value":{"path":"/exchange/coupon90"}},
                      {"id":"pin_input","component":"TextField","label":"계좌 비밀번호 4자리","text":{"path":"/exchange/accountPin"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"jpy_airport_oneshot","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"}}}}},
                      {"id":"btn_txt","component":"Text","text":"환전 신청 및 수령 바우처 설정 →"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            1 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"usdAmount":1200,"usdKrwAmount":1661400,"smallBillRatio":40,"tipPackIncluded":true,"accountPin":"1234"}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","usd_converter","small_slider","tip_chk","pin_input","btn_submit"]},
                      {"id":"title","component":"Text","text":"💵 USD 소액권 맞춤 환전 (1/2단계)"},
                      {"id":"usd_converter","component":"BidirectionalExchangeInput","currencyCode":"USD","currencyLabel":"달러 환전 금액 (USD ↔ KRW)","exchangeRate":1384.50,"unitScale":1,"foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"}},
                      {"id":"small_slider","component":"Slider","label":"소액권($10·$1) 비중 (%)","value":{"path":"/exchange/smallBillRatio"},"min":10,"max":70},
                      {"id":"tip_chk","component":"CheckBox","label":"팁용 1달러 신권 30장 포함","value":{"path":"/exchange/tipPackIncluded"}},
                      {"id":"pin_input","component":"TextField","label":"계좌 비밀번호 4자리","text":{"path":"/exchange/accountPin"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"usd_small_bills","currency":"USD","foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"},"smallBillRatio":{"path":"/exchange/smallBillRatio"},"tipPackIncluded":{"path":"/exchange/tipPackIncluded"}}}}},
                      {"id":"btn_txt","component":"Text","text":"USD 환전 신청 및 수령 설정 →"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            2 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"eurAmount":800,"eurKrwAmount":1193680,"chfAmount":300,"chfKrwAmount":470400,"sameDayPickup":true,"accountPin":"1234"}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","eur_converter","chf_converter","pickup_chk","pin_input","btn_submit"]},
                      {"id":"title","component":"Text","text":"🇪🇺🇨🇭 EUR + CHF 다중통화 동시 환전 (1/2단계)"},
                      {"id":"eur_converter","component":"BidirectionalExchangeInput","currencyCode":"EUR","currencyLabel":"유로화 환전 (EUR ↔ KRW)","exchangeRate":1492.10,"unitScale":1,"foreignAmount":{"path":"/exchange/eurAmount"},"krwAmount":{"path":"/exchange/eurKrwAmount"}},
                      {"id":"chf_converter","component":"BidirectionalExchangeInput","currencyCode":"CHF","currencyLabel":"스위스 프랑 환전 (CHF ↔ KRW)","exchangeRate":1568.00,"unitScale":1,"foreignAmount":{"path":"/exchange/chfAmount"},"krwAmount":{"path":"/exchange/chfKrwAmount"}},
                      {"id":"pickup_chk","component":"CheckBox","label":"인천공항 단일 봉투 통합 수령","value":{"path":"/exchange/sameDayPickup"}},
                      {"id":"pin_input","component":"TextField","label":"계좌 비밀번호 4자리","text":{"path":"/exchange/accountPin"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"multi_currency_eur_chf","currency":"EUR+CHF","foreignAmount":{"path":"/exchange/eurAmount"},"chfAmount":{"path":"/exchange/chfAmount"},"krwAmount":{"path":"/exchange/eurKrwAmount"},"sameDayPickup":{"path":"/exchange/sameDayPickup"}}}}},
                      {"id":"btn_txt","component":"Text","text":"일괄 환전 신청 및 바우처 발급 →"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            3 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"targetRate":900,"autoJpyAmount":100000,"autoKrwAmount":900000,"pushAlert":true,"accountPin":"1234"}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","spark","target_slider","jpy_converter","push_chk","pin_input","btn_submit"]},
                      {"id":"title","component":"Text","text":"📉 목표환율 자동예약 (1/2단계)"},
                      {"id":"spark","component":"MetricSparkline","metricName":"최근 7일 JPY/KRW","currentValue":"912.40 원","severity":"warning","points":[0.82,0.74,0.61,0.52,0.44,0.35]},
                      {"id":"target_slider","component":"Slider","label":"목표 환율 (100엔당 원)","value":{"path":"/exchange/targetRate"},"min":860,"max":920},
                      {"id":"jpy_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"자동 매수 금액 (JPY ↔ KRW)","exchangeRate":900.0,"unitScale":100,"foreignAmount":{"path":"/exchange/autoJpyAmount"},"krwAmount":{"path":"/exchange/autoKrwAmount"}},
                      {"id":"push_chk","component":"CheckBox","label":"체결 시 푸시 알림","value":{"path":"/exchange/pushAlert"}},
                      {"id":"pin_input","component":"TextField","label":"계좌 비밀번호 4자리","text":{"path":"/exchange/accountPin"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"auto_target_rate","currency":"JPY","targetRate":{"path":"/exchange/targetRate"},"foreignAmount":{"path":"/exchange/autoJpyAmount"},"krwAmount":{"path":"/exchange/autoKrwAmount"}}}}},
                      {"id":"btn_txt","component":"Text","text":"자동환전 예약 및 알림 설정 →"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            else -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"sellJpyAmount":18000,"sellKrwAmount":162180,"billsOnlyConfirmed":true,"depositAccount":"110-482-******"}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","sell_converter","Deposit_tf","bills_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"🔄 엔화 현찰 원화 재환전 (1/2단계)"},
                      {"id":"sell_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"재환전 현찰 금액 (JPY ↔ 입금 원화)","exchangeRate":901.0,"unitScale":100,"foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"}},
                      {"id":"Deposit_tf","component":"TextField","label":"입금 계좌","text":{"path":"/exchange/depositAccount"}},
                      {"id":"bills_chk","component":"CheckBox","label":"지폐 권종 확인 및 80% 우대 적용","value":{"path":"/exchange/billsOnlyConfirmed"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"sell_remaining_jpy","currency":"JPY","foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"},"depositAccount":{"path":"/exchange/depositAccount"}}}}},
                      {"id":"btn_txt","component":"Text","text":"재환전 접수 및 입금 확인 →"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }
        }
    }

    /**
     * Stage 2: Triggered when `submit_exchange` is received from the client.
     * Uses the live DataModel values transmitted in `event.context` to update `exchange_surface`
     * with a follow-up pickup/voucher configuration card.
     */
    fun buildFollowUpVoucherStepJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val currency = ctx["currency"]?.toString() ?: "JPY"
        val foreignAmt = (ctx["foreignAmount"] as? Number)?.toLong() ?: 50000L
        val krwAmt = (ctx["krwAmount"] as? Number)?.toLong() ?: 452060L
        val branchRaw = (ctx["branch"] as? List<*>)?.firstOrNull()?.toString() ?: "ICN_T1"
        val branchLabel =
            when (branchRaw) {
                "ICN_T2" -> "인천공항 제2터미널"
                else -> "인천공항 제1터미널"
            }
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        val summaryValue = "${fmt.format(foreignAmt)} $currency (${fmt.format(krwAmt)} 원)"
        val agentReply =
            "전송된 데이터 모델($summaryValue · $branchLabel)로 환전 신청이 접수되었습니다. 수령 바우처 발급 방식을 선택해 다음 액션을 완료해 주세요."

        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchangeNext","value":{"summary":"$summaryValue","branchLabel":"$branchLabel","pickupMethod":["qr_kiosk"],"arrivalPush":true,"recipientPhone":"010-2026-8492"}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["exchange_next_card"]},
              {"id":"exchange_next_card","component":"Card","child":"exchange_next_col"},
              {"id":"exchange_next_col","component":"Column","children":["next_title","next_spark","next_branch_txt","picker_method","chk_arrival","tf_phone","btn_issue_qr","btn_back_edit"]},
              {"id":"next_title","component":"Text","text":"✅ [2/2단계] 환전 접수 완료 · 수령 바우처 설정"},
              {"id":"next_spark","component":"MetricSparkline","metricName":"확정 환전 금액 (출금 예정)","currentValue":"$summaryValue","severity":"normal","points":[0.35,0.42,0.50,0.62,0.78,0.92]},
              {"id":"next_branch_txt","component":"Text","text":"📍 지정 수령처: $branchLabel (접수번호 EX-2026-8492)"},
              {"id":"picker_method","component":"ChoicePicker","label":"외화 수령 방식 선택","variant":"mutuallyExclusive","options":[{"label":"모바일 QR 바우처 (공항 무인환전기 즉시 수령)","value":"qr_kiosk"},{"label":"공항 은행 창구 대면 수령","value":"branch_counter"},{"label":"트래블 외화 선불카드 즉시 충전","value":"travel_card"}],"value":{"path":"/exchangeNext/pickupMethod"}},
              {"id":"chk_arrival","component":"CheckBox","label":"출국 당일 공항 도착 시 수령 창구 위치 알림 받기","value":{"path":"/exchangeNext/arrivalPush"}},
              {"id":"tf_phone","component":"TextField","label":"바우처 수신 연락처","text":{"path":"/exchangeNext/recipientPhone"}},
              {"id":"btn_issue_qr","component":"Button","variant":"primary","child":"btn_issue_qr_txt","action":{"event":{"name":"issue_exchange_voucher","context":{"summary":{"path":"/exchangeNext/summary"},"branchLabel":{"path":"/exchangeNext/branchLabel"},"pickupMethod":{"path":"/exchangeNext/pickupMethod"},"arrivalPush":{"path":"/exchangeNext/arrivalPush"},"recipientPhone":{"path":"/exchangeNext/recipientPhone"}}}}},
              {"id":"btn_issue_qr_txt","component":"Text","text":"모바일 수령 QR 바우처 발급 확정"},
              {"id":"btn_back_edit","component":"Button","variant":"borderless","child":"btn_back_edit_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"edit_again"}}}},
              {"id":"btn_back_edit_txt","component":"Text","text":"↩ 환전 금액·조건 다시 변경하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateData, updateComponents)
    }

    /**
     * Stage 3: Triggered when `issue_exchange_voucher` is received from the client.
     */
    fun buildFinalVoucherIssuedJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val summary = ctx["summary"]?.toString() ?: "50,000 JPY (452,060 원)"
        val branchLabel = ctx["branchLabel"]?.toString() ?: "인천공항 제1터미널"
        val methodRaw = (ctx["pickupMethod"] as? List<*>)?.firstOrNull()?.toString() ?: "qr_kiosk"
        val methodLabel =
            when (methodRaw) {
                "branch_counter" -> "공항 은행 창구 대면 수령"
                "travel_card" -> "트래블 외화 선불카드 즉시 충전"
                else -> "모바일 QR 바우처 (공항 무인환전기)"
            }
        val phone = ctx["recipientPhone"]?.toString() ?: "010-2026-8492"
        val agentReply =
            "모바일 수령 바우처($methodLabel)가 $phone 번호로 발급되었습니다. 공항 수령 시 QR 코드를 제시해 주세요."

        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["exchange_done_card"]},
              {"id":"exchange_done_card","component":"Card","child":"exchange_done_col"},
              {"id":"exchange_done_col","component":"Column","children":["done_title","done_spark","done_info1","done_info2","done_info3","btn_new_exchange"]},
              {"id":"done_title","component":"Text","text":"🎫 외화 환전 및 모바일 QR 바우처 발급 완료"},
              {"id":"done_spark","component":"MetricSparkline","metricName":"바우처 상태 · QR-EX-2026-8492","currentValue":"발급 완료 (수령 대기)","severity":"normal","points":[0.5,0.6,0.7,0.8,0.9,1.0]},
              {"id":"done_info1","component":"Text","text":"• 확정 금액: $summary"},
              {"id":"done_info2","component":"Text","text":"• 수령 지점 및 방식: $branchLabel · $methodLabel"},
              {"id":"done_info3","component":"Text","text":"• 알림 발송 연락처: $phone"},
              {"id":"btn_new_exchange","component":"Button","variant":"primary","child":"btn_new_exchange_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"new_exchange"}}}},
              {"id":"btn_new_exchange_txt","component":"Text","text":"새로운 환전 신청하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateComponents)
    }
}
