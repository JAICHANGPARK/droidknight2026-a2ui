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
                shortChipLabel = "5만엔 환전",
                userPromptText = "인천공항 1터미널 수령으로 5만 엔 환전해 줘.",
                agentIntroText = "50,000 JPY(90% 우대) 환전 정보를 확인해 주세요.",
                schemaBadge = "exchange_surface · /exchange/jpyAmount",
            ),
            AgentPromptPreset(
                id = 1,
                shortChipLabel = "간편 송금",
                userPromptText = "박우리님께 모임회비 24만 원 보내 줘.",
                agentIntroText = "박우리님께 240,000원을 보낼게요.",
                schemaBadge = "exchange_surface · /transfer/amount",
            ),
            AgentPromptPreset(
                id = 2,
                shortChipLabel = "카드 즉시결제",
                userPromptText = "11월 카드 명세서 즉시결제할래.",
                agentIntroText = "11월 명세서 2,161,314원 즉시결제입니다.",
                schemaBadge = "exchange_surface · /cardBill/payAmount",
            ),
            AgentPromptPreset(
                id = 3,
                shortChipLabel = "도쿄 출국 3종",
                userPromptText = "도쿄 환전·해외결제·여행자보험 한 번에 켜 줘.",
                agentIntroText = "5만 엔 환전, 해외원화결제 차단, 여행자보험을 한 번에 준비했어요.",
                schemaBadge = "exchange_surface · /travelBundle",
            ),
            AgentPromptPreset(
                id = 4,
                shortChipLabel = "달러 소액권",
                userPromptText = "1,200달러 소액권 위주로 환전할래.",
                agentIntroText = "1,200 USD 소액권(40%) 환전입니다.",
                schemaBadge = "exchange_surface · /exchange/usdAmount",
            ),
            AgentPromptPreset(
                id = 5,
                shortChipLabel = "목표환율 예약",
                userPromptText = "엔화 900원 이하일 때 10만 엔 자동 환전 걸어 줘.",
                agentIntroText = "100엔당 900원 자동 환전 예약입니다.",
                schemaBadge = "exchange_surface · /exchange/targetRate",
            ),
            AgentPromptPreset(
                id = 6,
                shortChipLabel = "엔화 재환전",
                userPromptText = "남은 18,000엔 내 계좌로 재환전할래.",
                agentIntroText = "18,000 JPY 원화 재환전(80% 우대)입니다.",
                schemaBadge = "exchange_surface · /exchange/sellJpyAmount",
            ),
            AgentPromptPreset(
                id = 7,
                shortChipLabel = "입출금·통장사본",
                userPromptText = "주거래 통장 입출금 내역이랑 통장사본 보여 줘.",
                agentIntroText = "DK 주거래 통장 최근 내역입니다.",
                schemaBadge = "exchange_surface · /ledger/filterMode",
            ),
        )

    fun resolvePresetIndexFromUserText(query: String, fallbackIndex: Int): Int {
        val q = query.lowercase()
        return when {
            q.contains("입출금") || q.contains("통장") || q.contains("잔액") || q.contains("내역") || q.contains("급여") || q.contains("출금") || q.contains("입금") || q.contains("사본") -> 7
            q.contains("이체") || q.contains("송금") || q.contains("박우리") || q.contains("김신한") || q.contains("이아름") || q.contains("김연경") || q.contains("회비") || q.contains("보내") -> 1
            q.contains("카드") || q.contains("명세서") || q.contains("즉시결제") || q.contains("분할") || q.contains("할부") || q.contains("리볼빙") -> 2
            q.contains("보험") || q.contains("출국") || q.contains("3종") || (q.contains("도쿄") && q.contains("카드")) -> 3
            q.contains("달러") || q.contains("usd") || q.contains("소액권") || q.contains("뉴욕") -> 4
            q.contains("목표") || q.contains("자동") || q.contains("예약") || q.contains("900원") -> 5
            q.contains("재환전") || q.contains("남은") || q.contains("현찰") -> 6
            q.contains("엔") || q.contains("jpy") || q.contains("환전") || q.contains("인천공항") -> 0
            else -> fallbackIndex
        }
    }

    fun buildJsonl(caseIndex: Int): List<String> {
        val createSurface =
            """{"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

        return when (caseIndex) {
            0 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":50000,"krwAmount":452060,"branch":["ICN_T1"],"coupon90":true}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","exchange_converter","branch_picker","coupon_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"💴 50,000 JPY 환전 (90% 우대)"},
                      {"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액","exchangeRate":904.12,"unitScale":100,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"}},
                      {"id":"branch_picker","component":"ChoicePicker","label":"수령처","variant":"mutuallyExclusive","options":[{"label":"인천공항 1터미널","value":"ICN_T1"},{"label":"인천공항 2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},
                      {"id":"coupon_chk","component":"CheckBox","label":"주거래 통장 출금 · 90% 우대","value":{"path":"/exchange/coupon90"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"jpy_airport_oneshot","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"}}}}},
                      {"id":"btn_txt","component":"Text","text":"환전 신청하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            1 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/transfer","value":{"recipient":"박우리 (국민 123401-01-******)","amount":"240000","fromAccount":["salary_acc"],"memo":"모임회비","feeWaived":true}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["transfer_card"]},
                      {"id":"transfer_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","acc_picker","amt_tf","memo_tf","btn_submit"]},
                      {"id":"title","component":"Text","text":"💸 박우리님께 송금"},
                      {"id":"acc_picker","component":"ChoicePicker","label":"출금 계좌","variant":"mutuallyExclusive","options":[{"label":"DK 주거래 통장 (31,520,000원)","value":"salary_acc"},{"label":"생활비통장 (623,540원)","value":"living_acc"}],"value":{"path":"/transfer/fromAccount"}},
                      {"id":"amt_tf","component":"TextField","label":"금액 (원)","value":{"path":"/transfer/amount"}},
                      {"id":"memo_tf","component":"TextField","label":"받는 분 표시","value":{"path":"/transfer/memo"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_banking_inquiry","context":{"inquiryType":"transfer","recipient":{"path":"/transfer/recipient"},"amount":{"path":"/transfer/amount"},"fromAccount":{"path":"/transfer/fromAccount"},"memo":{"path":"/transfer/memo"}}}}},
                      {"id":"btn_txt","component":"Text","text":"240,000원 보내기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            2 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/cardBill","value":{"payMode":["full_instant"],"payAmount":"2161314","usePoints":true}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["bill_card"]},
                      {"id":"bill_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","mode_picker","pay_tf","point_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"💳 11월 명세서 결제"},
                      {"id":"mode_picker","component":"ChoicePicker","label":"결제 방식","variant":"mutuallyExclusive","options":[{"label":"전액 결제 (2,161,314원)","value":"full_instant"},{"label":"일부 결제","value":"partial_pay"},{"label":"3개월 분할납부","value":"split_3m"}],"value":{"path":"/cardBill/payMode"}},
                      {"id":"pay_tf","component":"TextField","label":"결제 금액 (원)","value":{"path":"/cardBill/payAmount"}},
                      {"id":"point_chk","component":"CheckBox","label":"포인트 10,000P 사용","value":{"path":"/cardBill/usePoints"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_banking_inquiry","context":{"inquiryType":"card_bill","payMode":{"path":"/cardBill/payMode"},"payAmount":{"path":"/cardBill/payAmount"},"usePoints":{"path":"/cardBill/usePoints"}}}}},
                      {"id":"btn_txt","component":"Text","text":"즉시결제하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            3 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/travelBundle","value":{"jpyAmount":50000,"krwAmount":452060,"dccBlock":true,"insPlan":["standard_8400"]}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["bundle_card"]},
                      {"id":"bundle_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","jpy_conv","dcc_chk","ins_picker","btn_submit"]},
                      {"id":"title","component":"Text","text":"✈️ 도쿄 출국 3종"},
                      {"id":"jpy_conv","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"엔화 환전 (90% 우대)","exchangeRate":904.12,"unitScale":100,"foreignAmount":{"path":"/travelBundle/jpyAmount"},"krwAmount":{"path":"/travelBundle/krwAmount"}},
                      {"id":"dcc_chk","component":"CheckBox","label":"해외 원화결제(DCC) 차단","value":{"path":"/travelBundle/dccBlock"}},
                      {"id":"ins_picker","component":"ChoicePicker","label":"여행자보험","variant":"mutuallyExclusive","options":[{"label":"든든형 (8,400원)","value":"standard_8400"},{"label":"실속형 (4,900원)","value":"light_4900"}],"value":{"path":"/travelBundle/insPlan"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_banking_inquiry","context":{"inquiryType":"travel_bundle","jpyAmount":{"path":"/travelBundle/jpyAmount"},"krwAmount":{"path":"/travelBundle/krwAmount"},"dccBlock":{"path":"/travelBundle/dccBlock"},"insPlan":{"path":"/travelBundle/insPlan"}}}}},
                      {"id":"btn_txt","component":"Text","text":"한 번에 신청하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            4 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"usdAmount":1200,"usdKrwAmount":1661400,"smallBillRatio":40,"tipPackIncluded":true}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","usd_converter","small_slider","tip_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"💵 1,200 USD 소액권 환전"},
                      {"id":"usd_converter","component":"BidirectionalExchangeInput","currencyCode":"USD","currencyLabel":"환전 금액","exchangeRate":1384.50,"unitScale":1,"foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"}},
                      {"id":"small_slider","component":"Slider","label":"소액권 비중 (%)","value":{"path":"/exchange/smallBillRatio"},"min":10,"max":70},
                      {"id":"tip_chk","component":"CheckBox","label":"1달러 신권 30장 포함","value":{"path":"/exchange/tipPackIncluded"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"usd_small_bills","currency":"USD","foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"},"smallBillRatio":{"path":"/exchange/smallBillRatio"},"tipPackIncluded":{"path":"/exchange/tipPackIncluded"}}}}},
                      {"id":"btn_txt","component":"Text","text":"환전 신청하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            5 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"targetRate":900,"autoJpyAmount":100000,"autoKrwAmount":900000,"pushAlert":true}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","target_slider","jpy_converter","push_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"📉 엔화 목표환율 예약"},
                      {"id":"target_slider","component":"Slider","label":"목표 환율 (100엔당 원)","value":{"path":"/exchange/targetRate"},"min":860,"max":920},
                      {"id":"jpy_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"예약 금액","exchangeRate":900.0,"unitScale":100,"foreignAmount":{"path":"/exchange/autoJpyAmount"},"krwAmount":{"path":"/exchange/autoKrwAmount"}},
                      {"id":"push_chk","component":"CheckBox","label":"체결 시 알림 받기","value":{"path":"/exchange/pushAlert"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"auto_target_rate","currency":"JPY","targetRate":{"path":"/exchange/targetRate"},"foreignAmount":{"path":"/exchange/autoJpyAmount"},"krwAmount":{"path":"/exchange/autoKrwAmount"}}}}},
                      {"id":"btn_txt","component":"Text","text":"예약하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            7 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/ledger","value":{"filterMode":["all_tx"],"searchKeyword":"최근 1개월","issuePdfCopy":true}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["ledger_card"]},
                      {"id":"ledger_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","tx_summary","filter_picker","pdf_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"🏦 DK 주거래 통장 (31,520,000원)"},
                      {"id":"tx_summary","component":"Text","text":"• 10/25 급여 +4,850,000원\n• 10/26 박우리 -240,000원\n• 10/27 김신한 -120,000원"},
                      {"id":"filter_picker","component":"ChoicePicker","label":"구분","variant":"mutuallyExclusive","options":[{"label":"전체","value":"all_tx"},{"label":"입금 (+485만)","value":"deposit_only"},{"label":"출금 (-112만)","value":"withdraw_only"}],"value":{"path":"/ledger/filterMode"}},
                      {"id":"pdf_chk","component":"CheckBox","label":"통장사본 함께 발급","value":{"path":"/ledger/issuePdfCopy"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_banking_inquiry","context":{"inquiryType":"account_ledger","filterMode":{"path":"/ledger/filterMode"},"searchKeyword":{"path":"/ledger/searchKeyword"},"issuePdfCopy":{"path":"/ledger/issuePdfCopy"}}}}},
                      {"id":"btn_txt","component":"Text","text":"내역 확인 · 사본 발급"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            else -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"sellJpyAmount":18000,"sellKrwAmount":162180,"billsOnlyConfirmed":true,"depositAccount":"110-482-910284"}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","sell_converter","Deposit_tf","bills_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"🔄 18,000 JPY 원화 재환전"},
                      {"id":"sell_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"재환전 금액","exchangeRate":901.0,"unitScale":100,"foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"}},
                      {"id":"Deposit_tf","component":"TextField","label":"입금 계좌","value":{"path":"/exchange/depositAccount"}},
                      {"id":"bills_chk","component":"CheckBox","label":"80% 환율 우대 적용","value":{"path":"/exchange/billsOnlyConfirmed"}},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"sell_remaining_jpy","currency":"JPY","foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"},"depositAccount":{"path":"/exchange/depositAccount"}}}}},
                      {"id":"btn_txt","component":"Text","text":"재환전 신청하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }
        }
    }

    /**
     * Dynamically builds a tailored A2UI v0.9.1 surface for ANY free-form text inquiry typed by the user
     * in the AI chatbot (even without a live Gemini API key).
     */
    fun buildDynamicBankingJsonlForQuery(userQuery: String, fallbackCaseIndex: Int): Pair<String, List<String>> {
        val trimmed = userQuery.trim()
        val q = trimmed.lowercase()
        val createSurface =
            """{"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

        // 1. 입출금 / 통장 / 잔액 / 거래내역 / 급여 / 사본 문의
        if (q.contains("입출금") || q.contains("통장") || q.contains("잔액") || q.contains("내역") || q.contains("급여") || q.contains("출금") || q.contains("입금") || q.contains("사본")) {
            val safeKeyword = trimmed.replace("\"", "").take(24)
            val reply = "DK 주거래 통장 내역입니다."
            val updateData =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/ledger","value":{"filterMode":["all_tx"],"searchKeyword":"$safeKeyword","issuePdfCopy":true}}}"""
            val updateComponents = buildJsonl(7)[2]
            return reply to listOf(createSurface, updateData, updateComponents)
        }

        // 2. 송금 / 이체 문의 (자연어에서 받는 분 이름 및 금액 추출)
        if (q.contains("이체") || q.contains("송금") || q.contains("보내") || q.contains("박우리") || q.contains("김신한") || q.contains("이아름") || q.contains("김연경") || q.contains("회비")) {
            val recipientName =
                when {
                    q.contains("김신한") -> "김신한"
                    q.contains("이아름") -> "이아름"
                    q.contains("김연경") -> "김연경"
                    else -> "박우리"
                }
            val manMatch = Regex("""(\d+)\s*만\s*원?""").find(q)
            val rawWonMatch = Regex("""(\d{4,})\s*원?""").find(q)
            val parsedAmount =
                when {
                    manMatch != null -> (manMatch.groupValues[1].toLongOrNull() ?: 24L) * 10000L
                    rawWonMatch != null -> rawWonMatch.groupValues[1].toLongOrNull() ?: 240000L
                    q.contains("김신한") -> 120000L
                    q.contains("이아름") -> 80000L
                    q.contains("김연경") -> 50000L
                    else -> 240000L
                }
            val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
            val fmtAmt = fmt.format(parsedAmount)
            val safeMemo = trimmed.replace("\"", "").take(16)
            val reply = "${recipientName}님께 ${fmtAmt}원을 보낼게요."
            val updateData =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/transfer","value":{"recipient":"$recipientName","amount":"$parsedAmount","fromAccount":["salary_acc"],"memo":"$safeMemo","feeWaived":true}}}"""
            val updateComponents =
                """
                {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                  {"id":"root","component":"Column","children":["transfer_card"]},
                  {"id":"transfer_card","component":"Card","child":"card_col"},
                  {"id":"card_col","component":"Column","children":["title","acc_picker","amt_tf","memo_tf","btn_submit"]},
                  {"id":"title","component":"Text","text":"💸 ${recipientName}님께 송금"},
                  {"id":"acc_picker","component":"ChoicePicker","label":"출금 계좌","variant":"mutuallyExclusive","options":[{"label":"DK 주거래 통장 (31,520,000원)","value":"salary_acc"},{"label":"생활비통장 (623,540원)","value":"living_acc"}],"value":{"path":"/transfer/fromAccount"}},
                  {"id":"amt_tf","component":"TextField","label":"금액 (원)","value":{"path":"/transfer/amount"}},
                  {"id":"memo_tf","component":"TextField","label":"받는 분 표시","value":{"path":"/transfer/memo"}},
                  {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_banking_inquiry","context":{"inquiryType":"transfer","recipient":{"path":"/transfer/recipient"},"amount":{"path":"/transfer/amount"},"fromAccount":{"path":"/transfer/fromAccount"},"memo":{"path":"/transfer/memo"}}}}},
                  {"id":"btn_txt","component":"Text","text":"${fmtAmt}원 보내기"}
                ]}}
                """.trimIndent().replace("\n", "")
            return reply to listOf(createSurface, updateData, updateComponents)
        }

        // 3. 카드 명세서 / 결제 / 할부 / 리볼빙 문의
        if (q.contains("카드") || q.contains("명세서") || q.contains("즉시결제") || q.contains("분할") || q.contains("할부") || q.contains("리볼빙")) {
            val preset = promptPresets[2]
            return preset.agentIntroText to buildJsonl(2)
        }

        // 4. 해외출국 / 여행자보험 / 3종 패키지 문의
        if (q.contains("보험") || q.contains("출국") || q.contains("3종") || (q.contains("도쿄") && q.contains("카드"))) {
            val preset = promptPresets[3]
            return preset.agentIntroText to buildJsonl(3)
        }

        // 5. 외화 환전 관련 문의 (엔화, 달러, 목표환율, 재환전)
        if (q.contains("달러") || q.contains("usd") || q.contains("소액권") || q.contains("뉴욕")) {
            return promptPresets[4].agentIntroText to buildJsonl(4)
        }
        if (q.contains("목표") || q.contains("자동") || q.contains("예약") || q.contains("900원")) {
            return promptPresets[5].agentIntroText to buildJsonl(5)
        }
        if (q.contains("재환전") || q.contains("남은") || q.contains("현찰")) {
            return promptPresets[6].agentIntroText to buildJsonl(6)
        }
        if (q.contains("엔") || q.contains("jpy") || q.contains("환전") || q.contains("인천공항")) {
            return promptPresets[0].agentIntroText to buildJsonl(0)
        }

        // 6. 그 외 모든 자유 텍스트 금융 문의 -> 간결한 3-위젯 맞춤 A2UI 제어판 동적 생성!
        val safeQuery = trimmed.replace("\"", "").take(24)
        val reply = "'$safeQuery' 요청을 확인해 주세요."
        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/customInquiry","value":{"actionMode":["instant_apply"],"requestDetail":"$safeQuery","vvipBenefit":true}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["custom_card"]},
              {"id":"custom_card","component":"Card","child":"card_col"},
              {"id":"card_col","component":"Column","children":["title","mode_picker","detail_tf","vvip_chk","btn_submit"]},
              {"id":"title","component":"Text","text":"🤖 $safeQuery"},
              {"id":"mode_picker","component":"ChoicePicker","label":"처리 방식","variant":"mutuallyExclusive","options":[{"label":"즉시 실행","value":"instant_apply"},{"label":"확인증 함께 발급","value":"with_certificate"}],"value":{"path":"/customInquiry/actionMode"}},
              {"id":"detail_tf","component":"TextField","label":"요청 내용","value":{"path":"/customInquiry/requestDetail"}},
              {"id":"vvip_chk","component":"CheckBox","label":"수수료 면제·우대 혜택 적용","value":{"path":"/customInquiry/vvipBenefit"}},
              {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_banking_inquiry","context":{"inquiryType":"custom_inquiry","requestDetail":{"path":"/customInquiry/requestDetail"},"actionMode":{"path":"/customInquiry/actionMode"},"vvipBenefit":{"path":"/customInquiry/vvipBenefit"}}}}},
              {"id":"btn_txt","component":"Text","text":"실행하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return reply to listOf(createSurface, updateData, updateComponents)
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
                "ICN_T2" -> "인천공항 2터미널"
                else -> "인천공항 1터미널"
            }
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        val summaryValue = "${fmt.format(foreignAmt)} $currency (${fmt.format(krwAmt)}원)"
        val agentReply = "환전이 신청되었어요. 수령 방법을 선택해 주세요."

        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchangeNext","value":{"summary":"$summaryValue","branchLabel":"$branchLabel","pickupMethod":["qr_kiosk"],"arrivalPush":true,"recipientPhone":"010-2026-8492"}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["exchange_next_card"]},
              {"id":"exchange_next_card","component":"Card","child":"exchange_next_col"},
              {"id":"exchange_next_col","component":"Column","children":["next_title","next_branch_txt","picker_method","tf_phone","btn_issue_qr","btn_back_edit"]},
              {"id":"next_title","component":"Text","text":"✅ $summaryValue"},
              {"id":"next_branch_txt","component":"Text","text":"수령처: $branchLabel"},
              {"id":"picker_method","component":"ChoicePicker","label":"수령 방법","variant":"mutuallyExclusive","options":[{"label":"모바일 QR (무인환전기)","value":"qr_kiosk"},{"label":"공항 창구 수령","value":"branch_counter"},{"label":"트래블카드 충전","value":"travel_card"}],"value":{"path":"/exchangeNext/pickupMethod"}},
              {"id":"tf_phone","component":"TextField","label":"연락처","value":{"path":"/exchangeNext/recipientPhone"}},
              {"id":"btn_issue_qr","component":"Button","variant":"primary","child":"btn_issue_qr_txt","action":{"event":{"name":"issue_exchange_voucher","context":{"summary":{"path":"/exchangeNext/summary"},"branchLabel":{"path":"/exchangeNext/branchLabel"},"pickupMethod":{"path":"/exchangeNext/pickupMethod"},"arrivalPush":{"path":"/exchangeNext/arrivalPush"},"recipientPhone":{"path":"/exchangeNext/recipientPhone"}}}}},
              {"id":"btn_issue_qr_txt","component":"Text","text":"바우처 발급하기"},
              {"id":"btn_back_edit","component":"Button","variant":"borderless","child":"btn_back_edit_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"edit_again"}}}},
              {"id":"btn_back_edit_txt","component":"Text","text":"금액 다시 입력"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateData, updateComponents)
    }

    /**
     * Stage 3: Triggered when `issue_exchange_voucher` is received from the client.
     */
    fun buildFinalVoucherIssuedJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val summary = ctx["summary"]?.toString() ?: "50,000 JPY (452,060원)"
        val branchLabel = ctx["branchLabel"]?.toString() ?: "인천공항 1터미널"
        val methodRaw = (ctx["pickupMethod"] as? List<*>)?.firstOrNull()?.toString() ?: "qr_kiosk"
        val methodLabel =
            when (methodRaw) {
                "branch_counter" -> "공항 창구 수령"
                "travel_card" -> "트래블카드 충전"
                else -> "모바일 QR 바우처"
            }
        val phone = ctx["recipientPhone"]?.toString() ?: "010-2026-8492"
        val agentReply = "QR 바우처가 발급되었습니다."

        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["exchange_done_card"]},
              {"id":"exchange_done_card","component":"Card","child":"exchange_done_col"},
              {"id":"exchange_done_col","component":"Column","children":["done_title","done_info1","done_info2","done_info3","btn_new_exchange"]},
              {"id":"done_title","component":"Text","text":"🎫 환전 바우처 발급 완료"},
              {"id":"done_info1","component":"Text","text":"• 금액: $summary"},
              {"id":"done_info2","component":"Text","text":"• 수령: $branchLabel ($methodLabel)"},
              {"id":"done_info3","component":"Text","text":"• 연락처: $phone"},
              {"id":"btn_new_exchange","component":"Button","variant":"primary","child":"btn_new_exchange_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"new_exchange"}}}},
              {"id":"btn_new_exchange_txt","component":"Text","text":"확인"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateComponents)
    }

    /**
     * Handles follow-up completion for `submit_banking_inquiry` (`transfer`, `card_bill`, `travel_bundle`).
     */
    fun buildBankingInquiryDoneJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val inquiryType = ctx["inquiryType"]?.toString() ?: "transfer"
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

        return when (inquiryType) {
            "card_bill" -> {
                val rawAmt = ctx["payAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull() ?: 2161314L
                val usePoints = ctx["usePoints"]?.toString() == "true"
                val finalAmt = if (usePoints && rawAmt > 10000L) rawAmt - 10000L else rawAmt
                val amtStr = fmt.format(finalAmt)
                val reply = "${amtStr}원 즉시결제가 완료되었습니다."
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["done_card"]},
                      {"id":"done_card","component":"Card","child":"done_col"},
                      {"id":"done_col","component":"Column","children":["done_title","info1","info2","btn_reset"]},
                      {"id":"done_title","component":"Text","text":"✅ 명세서 결제 완료"},
                      {"id":"info1","component":"Text","text":"• 결제 금액: ${amtStr}원"},
                      {"id":"info2","component":"Text","text":"• 출금 계좌: DK 주거래 통장"},
                      {"id":"btn_reset","component":"Button","variant":"primary","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"확인"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                reply to listOf(updateComponents)
            }

            "travel_bundle" -> {
                val jpyAmt = (ctx["jpyAmount"] as? Number)?.toLong() ?: 50000L
                val planRaw = (ctx["insPlan"] as? List<*>)?.firstOrNull()?.toString() ?: "standard_8400"
                val planLabel = if (planRaw == "light_4900") "실속형" else "든든형"
                val reply = "도쿄 출국 3종 신청이 완료되었습니다."
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["done_card"]},
                      {"id":"done_card","component":"Card","child":"done_col"},
                      {"id":"done_col","component":"Column","children":["done_title","info1","info2","info3","btn_reset"]},
                      {"id":"done_title","component":"Text","text":"✈️ 도쿄 출국 3종 신청 완료"},
                      {"id":"info1","component":"Text","text":"• 환전: ${fmt.format(jpyAmt)} JPY"},
                      {"id":"info2","component":"Text","text":"• 해외결제: 원화결제(DCC) 차단"},
                      {"id":"info3","component":"Text","text":"• 여행자보험: $planLabel 가입 완료"},
                      {"id":"btn_reset","component":"Button","variant":"primary","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"확인"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                reply to listOf(updateComponents)
            }

            "account_ledger" -> {
                val filterRaw = (ctx["filterMode"] as? List<*>)?.firstOrNull()?.toString() ?: "all_tx"
                val filterLabel =
                    when (filterRaw) {
                        "deposit_only" -> "입금 내역 (+4,850,000원)"
                        "withdraw_only" -> "출금 내역 (-1,120,000원)"
                        else -> "전체 내역 (최근 1개월)"
                    }
                val issuePdf = ctx["issuePdfCopy"]?.toString() == "true"
                val reply = if (issuePdf) "통장사본 발급이 완료되었습니다." else "내역 조회가 완료되었습니다."
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["done_card"]},
                      {"id":"done_card","component":"Card","child":"done_col"},
                      {"id":"done_col","component":"Column","children":["done_title","info1","info2","btn_reset"]},
                      {"id":"done_title","component":"Text","text":"🏦 ${if (issuePdf) "통장사본 발급 완료" else "내역 조회 완료"}"},
                      {"id":"info1","component":"Text","text":"• 구분: $filterLabel"},
                      {"id":"info2","component":"Text","text":"• 잔액: 31,520,000원"},
                      {"id":"btn_reset","component":"Button","variant":"primary","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"확인"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                reply to listOf(updateComponents)
            }

            "custom_inquiry" -> {
                val detail = ctx["requestDetail"]?.toString()?.takeIf { it.isNotBlank() } ?: "요청"
                val reply = "'$detail' 처리가 완료되었습니다."
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["done_card"]},
                      {"id":"done_card","component":"Card","child":"done_col"},
                      {"id":"done_col","component":"Column","children":["done_title","info1","btn_reset"]},
                      {"id":"done_title","component":"Text","text":"✅ 처리 완료"},
                      {"id":"info1","component":"Text","text":"• 내용: $detail"},
                      {"id":"btn_reset","component":"Button","variant":"primary","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"확인"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                reply to listOf(updateComponents)
            }

            else -> {
                val recipient = ctx["recipient"]?.toString()?.takeIf { it.isNotBlank() } ?: "박우리"
                val rawAmt = ctx["amount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull() ?: 240000L
                val amtStr = fmt.format(rawAmt)
                val reply = "${recipient}님께 ${amtStr}원을 보냈어요."
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["done_card"]},
                      {"id":"done_card","component":"Card","child":"done_col"},
                      {"id":"done_col","component":"Column","children":["done_title","info1","info2","btn_reset"]},
                      {"id":"done_title","component":"Text","text":"✅ 송금 완료"},
                      {"id":"info1","component":"Text","text":"• 받는 분: $recipient"},
                      {"id":"info2","component":"Text","text":"• 금액: ${amtStr}원 (수수료 면제)"},
                      {"id":"btn_reset","component":"Button","variant":"primary","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"확인"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                reply to listOf(updateComponents)
            }
        }
    }
}
