package com.droidknights.a2ui.scenarios.payloads

import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.AgentPromptPreset
import com.droidknights.a2ui.common.BankingAppState
import com.droidknights.a2ui.common.ExchangeVoucherItem
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
                userPromptText = "이지훈님께 모임회비 24만 원 보내 줘.",
                agentIntroText = "이지훈님께 240,000원을 보낼게요.",
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

    /**
     * Quick text-only question chips that demonstrate standard text Q&A before/alongside A2UI surfaces.
     */
    val conversationalSampleQuestions: List<Pair<String, String>> =
        listOf(
            "💬 오늘 환율 얼마야?" to "오늘 엔화랑 달러 환율 얼마야?",
            "💬 내 통장 잔액은?" to "지금 내 주거래 통장 잔액이랑 최근 거래 알려줘.",
            "💬 공항 수령 어디서 해?" to "인천공항에서 환전한 돈 어디서 수령해?",
        )

    /**
     * Returns a natural Korean text reply if the user's message is a conversational or informational
     * question (greeting, exchange rate inquiry, balance inquiry, airport pickup guide, etc.)
     * that does NOT yet request an interactive A2UI execution form.
     * Returns `null` when the user requests an actionable banking task (exchange, transfer, bill pay, etc.).
     */
    fun buildConversationalTextReplyIfPureQuestion(
        userQuery: String,
        bankingState: BankingAppState,
    ): String? {
        val trimmed = userQuery.trim()
        val q = trimmed.lowercase()
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

        if (q.contains("취소") || q.contains("환불")) {
            return if (bankingState.exchangeVouchers.isEmpty()) {
                "현재 취소 가능한 외화 환전 신청(미수령 바우처) 내역이 없습니다. 환전을 먼저 신청하신 뒤 수령 전이라면 언제든 취소하고 출금액 전액을 환불받으실 수 있습니다."
            } else {
                null
            }
        }

        val hasExplicitActionIntent =
            q.contains("환전해") ||
                q.contains("환전할") ||
                q.contains("환전 신청") ||
                q.contains("바꿔") ||
                q.contains("보내") ||
                q.contains("송금해") ||
                q.contains("송금할") ||
                q.contains("이체해") ||
                q.contains("즉시결제") ||
                q.contains("결제할") ||
                q.contains("결제해") ||
                q.contains("켜 줘") ||
                q.contains("켜줘") ||
                q.contains("걸어 줘") ||
                q.contains("걸어줘") ||
                q.contains("예약해") ||
                q.contains("재환전") ||
                q.contains("사본") ||
                q.contains("소액권") ||
                q.contains("3종") ||
                Regex("""\d+\s*(만\s*엔|엔|달러|usd|jpy|유로|만\s*원|원)""").containsMatchIn(q)

        if (hasExplicitActionIntent) {
            return null
        }

        // 1. 인사 / 자기소개 / 도움말
        if (
            q.contains("안녕") ||
                q.contains("반가") ||
                q.contains("누구") ||
                q.contains("도움") ||
                q.contains("뭐 할 수") ||
                q.contains("무엇을") ||
                q.contains("기능") ||
                q == "하이" ||
                q == "hi" ||
                q == "hello"
        ) {
            return "안녕하세요 박제창님! DK 금융 슈퍼앱 AI 금융비서입니다 😊\n\n" +
                "일반 텍스트 질문과 실시간 A2UI 실행 화면 생성을 모두 도와드릴 수 있어요.\n" +
                "• 💬 텍스트 질문: \"오늘 엔화 환율 얼마야?\", \"내 통장 잔액 얼마야?\", \"인천공항 수령은 어디서 해?\"\n" +
                "• ⚡ A2UI 실행 요청: \"인천공항 1터미널 수령으로 5만 엔 환전해 줘\", \"이지훈님께 24만 원 보내 줘\", \"11월 카드 명세서 즉시결제할래\""
        }

        // 2. 환율 / 시세 / 우대율 / 수수료 질문
        if (
            q.contains("환율") ||
                q.contains("시세") ||
                q.contains("우대") ||
                q.contains("수수료") ||
                q.contains("엔화 얼마") ||
                q.contains("달러 얼마") ||
                q.contains("유로 얼마")
        ) {
            return "📈 현재 DK 뱅크 실시간 환율 안내 (VVIP 우대 적용 기준)\n\n" +
                "• 🇯🇵 일본 엔 (JPY 100): 847.88원 (90% 환율 우대)\n" +
                "• 🇺🇸 미국 달러 (USD 1): 1,335.80원 (90% 환율 우대)\n" +
                "• 🇪🇺 유럽 유로 (EUR 1): 1,490.75원 (80% 환율 우대)\n" +
                "• 🇨🇭 스위스 프랑 (CHF 1): 1,619.05원 (80% 환율 우대)\n\n" +
                "박제창님은 VVIP 등급으로 환전·송금 수수료가 전액 면제됩니다. 바로 환전하시려면 \"인천공항 1터미널 5만 엔 환전해 줘\"처럼 말씀해 주세요!"
        }

        // 3. 계좌 잔액 / 최근 거래 / 카드값 / 보유 바우처 질문 (단순 질문)
        if (
            q.contains("잔액") ||
                q.contains("얼마 남았") ||
                q.contains("내 돈") ||
                q.contains("바우처") ||
                q.contains("카드값") ||
                q.contains("최근 거래") ||
                q.contains("내역 알려")
        ) {
            val latestTx = bankingState.transactions.firstOrNull()
            val vouchers = bankingState.exchangeVouchers
            val cardBillText =
                if (bankingState.cardStatementBalance == 0L) "0원 (납부 완료 ✅)"
                else "${fmt.format(bankingState.cardStatementBalance)}원"

            return buildString {
                append("🏦 박제창님의 실시간 금융 자산 브리핑입니다.\n\n")
                append("• DK 주거래 통장 잔액: ${fmt.format(bankingState.mainAccountBalance)}원\n")
                append("• 11월 카드 명세서 예정액: $cardBillText\n")
                append("• 보유 포인트: ${fmt.format(bankingState.rewardPoints)}P\n")
                if (latestTx != null) {
                    val sign = if (latestTx.amountDelta > 0) "+" else ""
                    append("• 최근 거래: ${latestTx.title} ($sign${fmt.format(latestTx.amountDelta)}원 · ${latestTx.dateLabel})\n")
                }
                if (vouchers.isNotEmpty()) {
                    append("• 🎫 보유 환전 바우처 (${vouchers.size}건):\n")
                    vouchers.forEachIndexed { idx, v ->
                        append(
                            "   ${idx + 1}) ${v.voucherCode} (${fmt.format(v.foreignAmount)} ${v.currency} · ${v.branchLabel})\n"
                        )
                    }
                }
                append("\n통장사본 발급이나 상세 필터 조회가 필요하시면 \"입출금 내역이랑 통장사본 보여 줘\"라고 입력해 주세요!")
            }
        }

        // 4. 공항 수령 장소 / 방법 / 준비물 질문
        if (
            q.contains("어디서") ||
                q.contains("수령") ||
                q.contains("위치") ||
                q.contains("준비물") ||
                q.contains("여권") ||
                q.contains("키오스크") ||
                q.contains("몇 시") ||
                q.contains("영업시간")
        ) {
            return "🛫 인천국제공항 외화 수령 안내\n\n" +
                "• 제1여객터미널: 3층 출국장 D·J 체크인 카운터 옆 24시간 무인환전기(키오스크) 및 은행 창구\n" +
                "• 제2여객터미널: 3층 출국장 E 체크인 카운터 옆 24시간 무인환전기\n" +
                "• 필수 준비물: 본인 실물 여권 + 앱에서 발급된 모바일 QR 바우처\n\n" +
                "지금 바로 신청하시려면 \"인천공항 1터미널 수령으로 5만 엔 환전해 줘\"라고 말씀해 주세요!"
        }

        // 5. 그 외 일반 질문 형태 ("~야?", "~인가요?", "~알려줘", "~뭐야", "~어때")
        if (
            q.endsWith("?") ||
                q.contains("얼마") ||
                q.contains("어떻게") ||
                q.contains("뭐야") ||
                q.contains("뭔가요") ||
                q.contains("알려") ||
                q.contains("어때") ||
                q.contains("가능해") ||
                q.contains("되나요")
        ) {
            return "문의하신 \"$trimmed\"에 대한 안내입니다.\n\n" +
                "현재 박제창님의 DK 주거래 통장 잔액은 ${fmt.format(bankingState.mainAccountBalance)}원이며, " +
                "외화 환전(JPY·USD·EUR·CHF 90% 우대), 수수료 면제 간편 송금, 11월 카드 명세서 즉시결제, 해외출국 3종 패키지 신청을 즉시 실행하실 수 있습니다.\n" +
                "실행 화면(A2UI)을 띄우시려면 원하시는 거래와 금액을 말씀해 주세요!"
        }

        return null
    }

    fun resolvePresetIndexFromUserText(query: String, fallbackIndex: Int): Int {
        val q = query.lowercase()
        return when {
            q.contains("입출금") || q.contains("통장") || q.contains("잔액") || q.contains("내역") || q.contains("급여") || q.contains("출금") || q.contains("입금") || q.contains("사본") -> 7
            q.contains("이체") || q.contains("송금") || q.contains("이지훈") || q.contains("김민수") || q.contains("최유진") || q.contains("김연경") || q.contains("회비") || q.contains("보내") -> 1
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
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":50000,"krwAmount":423940,"branch":["ICN_T1"],"coupon90":true,"agreeForexTerms":false,"agreePickupTerms":false}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","exchange_converter","branch_picker","coupon_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"💴 50,000 JPY 환전 (90% 우대)"},
                      {"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액","exchangeRate":847.88,"unitScale":100,"minAmount":1000,"banknoteUnit":1000,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"checks":[{"condition":{"call":"banknoteDenomination","args":{"value":{"path":"/exchange/jpyAmount"},"min":1000,"unit":1000}},"message":"엔화는 최소 1,000 JPY 이상, 1,000엔 지폐 단위로만 신청 가능합니다 (500엔 동전 불가)."}]},
                      {"id":"branch_picker","component":"ChoicePicker","label":"수령처","variant":"mutuallyExclusive","options":[{"label":"인천공항 1터미널","value":"ICN_T1"},{"label":"인천공항 2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},
                      {"id":"coupon_chk","component":"CheckBox","label":"주거래 통장 출금 · 90% 우대","value":{"path":"/exchange/coupon90"}},
                      {"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"},"checks":[{"condition":{"path":"/exchange/agreeForexTerms"},"message":"외국환거래법 및 인터넷 환전 유의사항 필수 동의가 필요합니다."}]},
                      {"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 공항 여권 지참 및 미수령 환불 규정 동의","value":{"path":"/exchange/agreePickupTerms"},"checks":[{"condition":{"path":"/exchange/agreePickupTerms"},"message":"공항 수령 및 환불 규정 필수 동의가 필요합니다."}]},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"jpy_airport_oneshot","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},
                      {"id":"btn_txt","component":"Text","text":"환전 신청하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            1 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/transfer","value":{"recipient":"이지훈 (123401-01-******)","amount":"240000","fromAccount":["salary_acc"],"memo":"모임회비","feeWaived":true}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["transfer_card"]},
                      {"id":"transfer_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","acc_picker","amt_tf","memo_tf","btn_submit"]},
                      {"id":"title","component":"Text","text":"💸 이지훈님께 송금"},
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
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/travelBundle","value":{"jpyAmount":50000,"krwAmount":423940,"dccBlock":true,"insPlan":["standard_8400"]}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["bundle_card"]},
                      {"id":"bundle_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","jpy_conv","dcc_chk","ins_picker","btn_submit"]},
                      {"id":"title","component":"Text","text":"✈️ 도쿄 출국 3종"},
                      {"id":"jpy_conv","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"엔화 환전 (90% 우대)","exchangeRate":847.88,"unitScale":100,"foreignAmount":{"path":"/travelBundle/jpyAmount"},"krwAmount":{"path":"/travelBundle/krwAmount"}},
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
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"usdAmount":1200,"usdKrwAmount":1602960,"smallBillRatio":40,"tipPackIncluded":true,"agreeForexTerms":false,"agreePickupTerms":false}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","usd_converter","small_slider","tip_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"💵 1,200 USD 소액권 환전"},
                      {"id":"usd_converter","component":"BidirectionalExchangeInput","currencyCode":"USD","currencyLabel":"환전 금액","exchangeRate":1335.80,"unitScale":1,"minAmount":10,"banknoteUnit":1,"foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"},"checks":[{"condition":{"call":"banknoteDenomination","args":{"value":{"path":"/exchange/usdAmount"},"min":10,"unit":1}},"message":"미국 달러는 최소 10 USD 이상, 1달러 지폐 단위로만 신청 가능합니다."}]},
                      {"id":"small_slider","component":"Slider","label":"소액권 비중 (%)","value":{"path":"/exchange/smallBillRatio"},"min":10,"max":70},
                      {"id":"tip_chk","component":"CheckBox","label":"1달러 신권 30장 포함","value":{"path":"/exchange/tipPackIncluded"}},
                      {"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"},"checks":[{"condition":{"path":"/exchange/agreeForexTerms"},"message":"외국환거래법 및 인터넷 환전 유의사항 필수 동의가 필요합니다."}]},
                      {"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 공항 여권 지참 및 미수령 환불 규정 동의","value":{"path":"/exchange/agreePickupTerms"},"checks":[{"condition":{"path":"/exchange/agreePickupTerms"},"message":"공항 수령 및 환불 규정 필수 동의가 필요합니다."}]},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"usd_small_bills","currency":"USD","foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"},"smallBillRatio":{"path":"/exchange/smallBillRatio"},"tipPackIncluded":{"path":"/exchange/tipPackIncluded"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},
                      {"id":"btn_txt","component":"Text","text":"환전 신청하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                listOf(createSurface, updateData, updateComponents)
            }

            5 -> {
                val updateData =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"targetRate":900,"autoJpyAmount":100000,"autoKrwAmount":900000,"pushAlert":true,"agreeForexTerms":false,"agreePickupTerms":false}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","target_slider","jpy_converter","push_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"📉 엔화 목표환율 예약"},
                      {"id":"target_slider","component":"Slider","label":"목표 환율 (100엔당 원)","value":{"path":"/exchange/targetRate"},"min":860,"max":920},
                      {"id":"jpy_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"예약 금액","exchangeRate":900.0,"unitScale":100,"minAmount":1000,"banknoteUnit":1000,"foreignAmount":{"path":"/exchange/autoJpyAmount"},"krwAmount":{"path":"/exchange/autoKrwAmount"},"checks":[{"condition":{"call":"banknoteDenomination","args":{"value":{"path":"/exchange/autoJpyAmount"},"min":1000,"unit":1000}},"message":"엔화는 최소 1,000 JPY 이상, 1,000엔 지폐 단위로만 예약 가능합니다."}]},
                      {"id":"push_chk","component":"CheckBox","label":"체결 시 알림 받기","value":{"path":"/exchange/pushAlert"}},
                      {"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"},"checks":[{"condition":{"path":"/exchange/agreeForexTerms"},"message":"외국환거래법 및 인터넷 환전 유의사항 필수 동의가 필요합니다."}]},
                      {"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 공항 여권 지참 및 미수령 환불 규정 동의","value":{"path":"/exchange/agreePickupTerms"},"checks":[{"condition":{"path":"/exchange/agreePickupTerms"},"message":"공항 수령 및 환불 규정 필수 동의가 필요합니다."}]},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"auto_target_rate","currency":"JPY","targetRate":{"path":"/exchange/targetRate"},"foreignAmount":{"path":"/exchange/autoJpyAmount"},"krwAmount":{"path":"/exchange/autoKrwAmount"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},
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
                      {"id":"tx_summary","component":"Text","text":"• 10/25 급여 +4,850,000원\n• 10/26 이지훈 -240,000원\n• 10/27 김민수 -120,000원"},
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
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"sellJpyAmount":18000,"sellKrwAmount":162180,"billsOnlyConfirmed":true,"depositAccount":"110-482-910284","agreeForexTerms":false,"agreePickupTerms":false}}}"""
                val updateComponents =
                    """
                    {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                      {"id":"root","component":"Column","children":["exchange_card"]},
                      {"id":"exchange_card","component":"Card","child":"card_col"},
                      {"id":"card_col","component":"Column","children":["title","sell_converter","Deposit_tf","bills_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},
                      {"id":"title","component":"Text","text":"🔄 18,000 JPY 원화 재환전"},
                      {"id":"sell_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"재환전 금액","exchangeRate":901.0,"unitScale":100,"minAmount":1000,"banknoteUnit":1000,"foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"},"checks":[{"condition":{"call":"banknoteDenomination","args":{"value":{"path":"/exchange/sellJpyAmount"},"min":1000,"unit":1000}},"message":"엔화 동전(500엔 이하)은 재환전이 불가하며 1,000엔 지폐 단위로만 가능합니다."}]},
                      {"id":"Deposit_tf","component":"TextField","label":"입금 계좌","value":{"path":"/exchange/depositAccount"}},
                      {"id":"bills_chk","component":"CheckBox","label":"80% 환율 우대 적용","value":{"path":"/exchange/billsOnlyConfirmed"}},
                      {"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"},"checks":[{"condition":{"path":"/exchange/agreeForexTerms"},"message":"외국환거래법 및 인터넷 환전 유의사항 필수 동의가 필요합니다."}]},
                      {"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 외화 현찰 재매도 및 계좌 입금 약관 동의","value":{"path":"/exchange/agreePickupTerms"},"checks":[{"condition":{"path":"/exchange/agreePickupTerms"},"message":"외화 현찰 재매도 약관 필수 동의가 필요합니다."}]},
                      {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"sell_remaining_jpy","currency":"JPY","foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"},"depositAccount":{"path":"/exchange/depositAccount"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},
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
        if (q.contains("이체") || q.contains("송금") || q.contains("보내") || q.contains("이지훈") || q.contains("김민수") || q.contains("최유진") || q.contains("김연경") || q.contains("회비")) {
            val recipientName =
                when {
                    q.contains("김민수") -> "김민수"
                    q.contains("최유진") -> "최유진"
                    q.contains("김연경") -> "김연경"
                    else -> "이지훈"
                }
            val manMatch = Regex("""(\d+)\s*만\s*원?""").find(q)
            val rawWonMatch = Regex("""(\d{4,})\s*원?""").find(q)
            val parsedAmount =
                when {
                    manMatch != null -> (manMatch.groupValues[1].toLongOrNull() ?: 24L) * 10000L
                    rawWonMatch != null -> rawWonMatch.groupValues[1].toLongOrNull() ?: 240000L
                    q.contains("김민수") -> 120000L
                    q.contains("최유진") -> 80000L
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

        // 5. 외화 환전 관련 문의 (엔화, 달러, 유로, 프랑, 목표환율, 재환전 — 자연어에서 금액·통화·터미널 동적 추출!)
        val parsedExchangeAmount = parseForeignAmountFromQuery(q)
        if (q.contains("목표") || q.contains("자동") || q.contains("예약") || q.contains("900원")) {
            val rawJpy = parsedExchangeAmount ?: 100_000L
            val (autoJpy, note) = normalizeToBanknoteAmount("JPY", rawJpy)
            val autoKrw = kotlin.math.round(autoJpy * 9.0).toLong()
            val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
            val reply =
                if (note != null) "$note 100엔당 900원 이하일 때 ${fmt.format(autoJpy)} JPY (${fmt.format(autoKrw)}원) 자동 환전 예약입니다."
                else "100엔당 900원 이하일 때 ${fmt.format(autoJpy)} JPY (${fmt.format(autoKrw)}원) 자동 환전 예약입니다."
            val updateData =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"targetRate":900,"autoJpyAmount":$autoJpy,"autoKrwAmount":$autoKrw,"pushAlert":true,"agreeForexTerms":false,"agreePickupTerms":false}}}"""
            val updateComponents = buildJsonl(5)[2]
            return reply to listOf(createSurface, updateData, updateComponents)
        }
        if (q.contains("재환전") || q.contains("남은") || q.contains("현찰")) {
            val rawSellJpy = parsedExchangeAmount ?: 18_000L
            val (sellJpy, note) = normalizeToBanknoteAmount("JPY", rawSellJpy)
            val sellKrw = kotlin.math.round((sellJpy / 100.0) * 901.0).toLong()
            val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
            val reply =
                if (note != null) "$note ${fmt.format(sellJpy)} JPY 원화 재환전(80% 우대 · ${fmt.format(sellKrw)}원 입금 예정)입니다."
                else "${fmt.format(sellJpy)} JPY 원화 재환전(80% 우대 · ${fmt.format(sellKrw)}원 입금 예정)입니다."
            val updateData =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"sellJpyAmount":$sellJpy,"sellKrwAmount":$sellKrw,"billsOnlyConfirmed":true,"depositAccount":"110-482-910284","agreeForexTerms":false,"agreePickupTerms":false}}}"""
            val updateComponents =
                """
                {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                  {"id":"root","component":"Column","children":["exchange_card"]},
                  {"id":"exchange_card","component":"Card","child":"card_col"},
                  {"id":"card_col","component":"Column","children":["title","sell_converter","Deposit_tf","bills_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},
                  {"id":"title","component":"Text","text":"🔄 ${fmt.format(sellJpy)} JPY 원화 재환전"},
                  {"id":"sell_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"재환전 금액","exchangeRate":901.0,"unitScale":100,"minAmount":1000,"banknoteUnit":1000,"foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"},"checks":[{"condition":{"call":"banknoteDenomination","args":{"value":{"path":"/exchange/sellJpyAmount"},"min":1000,"unit":1000}},"message":"엔화 동전(500엔 이하)은 재환전이 불가하며 1,000엔 지폐 단위로만 가능합니다."}]},
                  {"id":"Deposit_tf","component":"TextField","label":"입금 계좌","value":{"path":"/exchange/depositAccount"}},
                  {"id":"bills_chk","component":"CheckBox","label":"80% 환율 우대 적용","value":{"path":"/exchange/billsOnlyConfirmed"}},
                  {"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"},"checks":[{"condition":{"path":"/exchange/agreeForexTerms"},"message":"외국환거래법 및 인터넷 환전 유의사항 필수 동의가 필요합니다."}]},
                  {"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 외화 현찰 재매도 및 계좌 입금 약관 동의","value":{"path":"/exchange/agreePickupTerms"},"checks":[{"condition":{"path":"/exchange/agreePickupTerms"},"message":"외화 현찰 재매도 약관 필수 동의가 필요합니다."}]},
                  {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"sell_remaining_jpy","currency":"JPY","foreignAmount":{"path":"/exchange/sellJpyAmount"},"krwAmount":{"path":"/exchange/sellKrwAmount"},"depositAccount":{"path":"/exchange/depositAccount"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},
                  {"id":"btn_txt","component":"Text","text":"재환전 신청하기"}
                ]}}
                """.trimIndent().replace("\n", "")
            return reply to listOf(createSurface, updateData, updateComponents)
        }
        if (q.contains("달러") || q.contains("usd") || q.contains("소액권") || q.contains("뉴욕") || q.contains("유로") || q.contains("eur") || q.contains("프랑") || q.contains("chf")) {
            val isEur = q.contains("유로") || q.contains("eur")
            val isChf = q.contains("프랑") || q.contains("chf")
            val currencyCode =
                when {
                    isEur -> "EUR"
                    isChf -> "CHF"
                    else -> "USD"
                }
            val rate =
                when (currencyCode) {
                    "EUR" -> 1490.75
                    "CHF" -> 1619.05
                    else -> 1335.80
                }
            val emoji =
                when (currencyCode) {
                    "EUR" -> "💶"
                    "CHF" -> "🇨🇭"
                    else -> "💵"
                }
            val rawForeignAmt = parsedExchangeAmount ?: 1_200L
            val (foreignAmt, note) = normalizeToBanknoteAmount(currencyCode, rawForeignAmt)
            val krwAmt = kotlin.math.round(foreignAmt * rate).toLong()
            val minAmt = 10L
            val unitAmt =
                when (currencyCode) {
                    "EUR" -> 5L
                    "CHF" -> 10L
                    else -> 1L
                }
            val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
            val fmtForeign = fmt.format(foreignAmt)
            val fmtKrw = fmt.format(krwAmt)
            val reply =
                if (note != null) "$note $fmtForeign $currencyCode (${fmtKrw}원) 환전 정보를 확인해 주세요."
                else "$fmtForeign $currencyCode (${fmtKrw}원) 환전 정보를 확인해 주세요."
            val updateData =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"usdAmount":$foreignAmt,"usdKrwAmount":$krwAmt,"smallBillRatio":40,"tipPackIncluded":true,"agreeForexTerms":false,"agreePickupTerms":false}}}"""
            val updateComponents =
                """
                {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                  {"id":"root","component":"Column","children":["exchange_card"]},
                  {"id":"exchange_card","component":"Card","child":"card_col"},
                  {"id":"card_col","component":"Column","children":["title","usd_converter","small_slider","tip_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},
                  {"id":"title","component":"Text","text":"$emoji $fmtForeign $currencyCode 소액권 환전"},
                  {"id":"usd_converter","component":"BidirectionalExchangeInput","currencyCode":"$currencyCode","currencyLabel":"환전 금액","exchangeRate":$rate,"unitScale":1,"minAmount":$minAmt,"banknoteUnit":$unitAmt,"foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"},"checks":[{"condition":{"call":"banknoteDenomination","args":{"value":{"path":"/exchange/usdAmount"},"min":$minAmt,"unit":$unitAmt}},"message":"최소 $minAmt $currencyCode 이상, $unitAmt $currencyCode 지폐 단위로만 신청 가능합니다."}]},
                  {"id":"small_slider","component":"Slider","label":"소액권 비중 (%)","value":{"path":"/exchange/smallBillRatio"},"min":10,"max":70},
                  {"id":"tip_chk","component":"CheckBox","label":"소액 신권 우대 포함","value":{"path":"/exchange/tipPackIncluded"}},
                  {"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"},"checks":[{"condition":{"path":"/exchange/agreeForexTerms"},"message":"외국환거래법 및 인터넷 환전 유의사항 필수 동의가 필요합니다."}]},
                  {"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 공항 여권 지참 및 미수령 환불 규정 동의","value":{"path":"/exchange/agreePickupTerms"},"checks":[{"condition":{"path":"/exchange/agreePickupTerms"},"message":"공항 수령 및 환불 규정 필수 동의가 필요합니다."}]},
                  {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"usd_small_bills","currency":"$currencyCode","foreignAmount":{"path":"/exchange/usdAmount"},"krwAmount":{"path":"/exchange/usdKrwAmount"},"smallBillRatio":{"path":"/exchange/smallBillRatio"},"tipPackIncluded":{"path":"/exchange/tipPackIncluded"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},
                  {"id":"btn_txt","component":"Text","text":"환전 신청하기"}
                ]}}
                """.trimIndent().replace("\n", "")
            return reply to listOf(createSurface, updateData, updateComponents)
        }
        if (q.contains("엔") || q.contains("jpy") || q.contains("환전") || q.contains("인천공항")) {
            val rawJpyAmt = parsedExchangeAmount ?: 50_000L
            val (jpyAmt, note) = normalizeToBanknoteAmount("JPY", rawJpyAmt)
            val krwAmt = kotlin.math.round((jpyAmt / 100.0) * 847.88).toLong()
            val branchCode =
                if (q.contains("2터미널") || q.contains("제2터미널") || q.contains("t2")) "ICN_T2"
                else "ICN_T1"
            val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
            val fmtJpy = fmt.format(jpyAmt)
            val fmtKrw = fmt.format(krwAmt)
            val reply =
                if (note != null) "$note $fmtJpy JPY(90% 우대 · ${fmtKrw}원) 환전 정보를 확인해 주세요."
                else "$fmtJpy JPY(90% 우대 · ${fmtKrw}원) 환전 정보를 확인해 주세요."
            val updateData =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchange","value":{"currency":"JPY","jpyAmount":$jpyAmt,"krwAmount":$krwAmt,"branch":["$branchCode"],"coupon90":true,"agreeForexTerms":false,"agreePickupTerms":false}}}"""
            val updateComponents =
                """
                {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
                  {"id":"root","component":"Column","children":["exchange_card"]},
                  {"id":"exchange_card","component":"Card","child":"card_col"},
                  {"id":"card_col","component":"Column","children":["title","exchange_converter","branch_picker","coupon_chk","terms_forex_chk","terms_pickup_chk","btn_submit"]},
                  {"id":"title","component":"Text","text":"💴 $fmtJpy JPY 환전 (90% 우대)"},
                  {"id":"exchange_converter","component":"BidirectionalExchangeInput","currencyCode":"JPY","currencyLabel":"환전 금액","exchangeRate":847.88,"unitScale":100,"minAmount":1000,"banknoteUnit":1000,"foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"checks":[{"condition":{"call":"banknoteDenomination","args":{"value":{"path":"/exchange/jpyAmount"},"min":1000,"unit":1000}},"message":"엔화는 최소 1,000 JPY 이상, 1,000엔 지폐 단위로만 신청 가능합니다 (500엔 동전 불가)."}]},
                  {"id":"branch_picker","component":"ChoicePicker","label":"수령처","variant":"mutuallyExclusive","options":[{"label":"인천공항 1터미널","value":"ICN_T1"},{"label":"인천공항 2터미널","value":"ICN_T2"}],"value":{"path":"/exchange/branch"}},
                  {"id":"coupon_chk","component":"CheckBox","label":"주거래 통장 출금 · 90% 우대","value":{"path":"/exchange/coupon90"}},
                  {"id":"terms_forex_chk","component":"CheckBox","label":"[필수] 외국환거래법 및 인터넷 환전 유의사항 동의","value":{"path":"/exchange/agreeForexTerms"},"checks":[{"condition":{"path":"/exchange/agreeForexTerms"},"message":"외국환거래법 및 인터넷 환전 유의사항 필수 동의가 필요합니다."}]},
                  {"id":"terms_pickup_chk","component":"CheckBox","label":"[필수] 공항 여권 지참 및 미수령 환불 규정 동의","value":{"path":"/exchange/agreePickupTerms"},"checks":[{"condition":{"path":"/exchange/agreePickupTerms"},"message":"공항 수령 및 환불 규정 필수 동의가 필요합니다."}]},
                  {"id":"btn_submit","component":"Button","child":"btn_txt","variant":"primary","action":{"event":{"name":"submit_exchange","context":{"case":"jpy_airport_oneshot","currency":"JPY","foreignAmount":{"path":"/exchange/jpyAmount"},"krwAmount":{"path":"/exchange/krwAmount"},"branch":{"path":"/exchange/branch"},"coupon90":{"path":"/exchange/coupon90"},"agreeForexTerms":{"path":"/exchange/agreeForexTerms"},"agreePickupTerms":{"path":"/exchange/agreePickupTerms"}}}}},
                  {"id":"btn_txt","component":"Text","text":"환전 신청하기"}
                ]}}
                """.trimIndent().replace("\n", "")
            return reply to listOf(createSurface, updateData, updateComponents)
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
     * One-shot Exchange Completion: Triggered when `submit_exchange` is received from the client.
     */
    fun buildOneShotExchangeCompletedJsonl(
        event: A2uiClientEventMessage,
        voucherCode: String = "QR-EX-2026-8492",
    ): Pair<String, List<String>> {
        val ctx = event.context
        val caseName = ctx["case"]?.toString().orEmpty()
        val isSellBack = caseName == "sell_remaining_jpy"
        val currency = ctx["currency"]?.toString() ?: "JPY"
        val foreignAmt = (ctx["foreignAmount"] as? Number)?.toLong() ?: 50000L
        val krwAmt = (ctx["krwAmount"] as? Number)?.toLong() ?: 423940L
        val branchRaw = (ctx["branch"] as? List<*>)?.firstOrNull()?.toString() ?: "ICN_T1"
        val branchLabel =
            when (branchRaw) {
                "ICN_T2", "인천공항 2터미널", "인천국제공항 제2터미널" -> "인천공항 2터미널"
                "ICN_T1", "인천공항 1터미널", "인천국제공항 제1터미널" -> "인천공항 1터미널"
                else -> branchRaw.ifBlank { "인천공항 1터미널" }
            }
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        val summaryValue = "${fmt.format(foreignAmt)} $currency (${fmt.format(krwAmt)}원)"
        val agentReply =
            if (isSellBack) "재환전 입금이 완료되었습니다."
            else "환전 및 모바일 QR 바우처($voucherCode) 발급이 한 번에 완료되었습니다."
        val doneTitle =
            if (isSellBack) "✅ 외화 재환전 입금 완료"
            else "🎫 환전 및 QR 바우처 발급 완료 ($voucherCode)"
        val doneSub =
            if (isSellBack) "• 입금 계좌: DK 주거래 통장 (80% 우대)"
            else "• 수령처: $branchLabel (모바일 QR 바우처 즉시 발급)"

        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["exchange_done_card"]},
              {"id":"exchange_done_card","component":"Card","child":"exchange_done_col"},
              {"id":"exchange_done_col","component":"Column","children":["done_title","done_info1","done_info2","btn_new_exchange"]},
              {"id":"done_title","component":"Text","text":"$doneTitle"},
              {"id":"done_info1","component":"Text","text":"• 환전 금액: $summaryValue"},
              {"id":"done_info2","component":"Text","text":"$doneSub"},
              {"id":"btn_new_exchange","component":"Button","variant":"borderless","child":"btn_new_exchange_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"new_exchange"}}}},
              {"id":"btn_new_exchange_txt","component":"Text","text":"➕ 추가 환전 신청하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateComponents)
    }

    /**
     * Stage 2: Triggered when `submit_exchange` is received from the client.
     * Uses the live DataModel values transmitted in `event.context` to update `exchange_surface`
     * with a follow-up pickup/voucher configuration card (matches Slide 73 Stage 2).
     */
    fun buildFollowUpVoucherStepJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val currency = ctx["currency"]?.toString() ?: "JPY"
        val foreignAmt =
            (ctx["foreignAmount"] as? Number)?.toLong()
                ?: ctx["foreignAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                ?: 50000L
        val krwAmt =
            (ctx["krwAmount"] as? Number)?.toLong()
                ?: ctx["krwAmount"]?.toString()?.filter { it.isDigit() }?.toLongOrNull()
                ?: 423940L
        val branchRaw =
            when (val b = ctx["branch"]) {
                is List<*> -> b.firstOrNull()?.toString()
                is String -> b
                else -> null
            } ?: "ICN_T1"
        val branchLabel =
            when (branchRaw) {
                "ICN_T2", "인천공항 2터미널", "인천국제공항 제2터미널" -> "인천공항 제2터미널"
                else -> "인천공항 제1터미널"
            }
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        val summaryValue = "${fmt.format(foreignAmt)} $currency (${fmt.format(krwAmt)}원)"
        val agentReply =
            "전송된 데이터 모델($summaryValue · $branchLabel)로 환전 신청이 접수되었습니다. 수령 바우처 발급 방식을 선택해 다음 액션을 완료해 주세요."

        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/exchangeNext","value":{"summary":"$summaryValue","branchLabel":"$branchLabel","pickupMethod":["qr_kiosk"],"arrivalPush":true,"recipientPhone":"010-2026-8492"}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["exchange_next_card"]},
              {"id":"exchange_next_card","component":"Card","child":"exchange_next_col"},
              {"id":"exchange_next_col","component":"Column","children":["next_title","next_summary_txt","next_branch_txt","picker_method","chk_push","tf_phone","btn_issue_qr","btn_back_edit"]},
              {"id":"next_title","component":"Text","text":"✅ [2/2단계] 환전 접수 완료 · 수령 바우처 설정"},
              {"id":"next_summary_txt","component":"Text","text":"💰 확정 환전 금액: $summaryValue (90% 환율 우대 · 출금 완료)"},
              {"id":"next_branch_txt","component":"Text","text":"📍 지정 수령처: $branchLabel (접수번호 EX-2026-8492)"},
              {"id":"picker_method","component":"ChoicePicker","label":"외화 수령 방식 선택","variant":"mutuallyExclusive","options":[{"label":"모바일 QR 바우처 (공항 무인환전기 즉시 수령)","value":"qr_kiosk"},{"label":"공항 창구 수령","value":"branch_counter"},{"label":"트래블카드 충전","value":"travel_card"}],"value":{"path":"/exchangeNext/pickupMethod"}},
              {"id":"chk_push","component":"CheckBox","label":"출국 당일 공항 도착 시 수령 창구 위치 알림 받기","value":{"path":"/exchangeNext/arrivalPush"}},
              {"id":"tf_phone","component":"TextField","label":"바우처 수신 연락처","value":{"path":"/exchangeNext/recipientPhone"}},
              {"id":"btn_issue_qr","component":"Button","variant":"primary","child":"btn_issue_qr_txt","action":{"event":{"name":"issue_exchange_voucher","context":{"summary":{"path":"/exchangeNext/summary"},"branchLabel":{"path":"/exchangeNext/branchLabel"},"pickupMethod":{"path":"/exchangeNext/pickupMethod"},"arrivalPush":{"path":"/exchangeNext/arrivalPush"},"recipientPhone":{"path":"/exchangeNext/recipientPhone"}}}}},
              {"id":"btn_issue_qr_txt","component":"Text","text":"모바일 수령 QR 바우처 발급 확정"},
              {"id":"btn_back_edit","component":"Button","variant":"borderless","child":"btn_back_edit_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"edit_again"}}}},
              {"id":"btn_back_edit_txt","component":"Text","text":"금액 다시 입력"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateData, updateComponents)
    }

    /**
     * Stage 3: Triggered when `issue_exchange_voucher` is received from the client (matches Slide 73 Stage 3).
     */
    fun buildFinalVoucherIssuedJsonl(
        event: A2uiClientEventMessage,
        voucherCode: String = "QR-EX-2026-8492",
    ): Pair<String, List<String>> {
        val ctx = event.context
        val summary = ctx["summary"]?.toString() ?: "50,000 JPY (423,940원)"
        val branchLabel = ctx["branchLabel"]?.toString() ?: "인천공항 제1터미널"
        val methodRaw =
            when (val m = ctx["pickupMethod"]) {
                is List<*> -> m.firstOrNull()?.toString()
                is String -> m
                else -> null
            } ?: "qr_kiosk"
        val methodLabel =
            when (methodRaw) {
                "branch_counter" -> "공항 창구 수령"
                "travel_card" -> "트래블카드 충전"
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
              {"id":"exchange_done_col","component":"Column","children":["done_title","done_status_txt","done_info1","done_info2","done_info3","btn_new_exchange"]},
              {"id":"done_title","component":"Text","text":"🎫 외화 환전 및 모바일 QR 바우처 발급 완료"},
              {"id":"done_status_txt","component":"Text","text":"• 바우처 번호: $voucherCode (발급 완료 · 수령 대기)"},
              {"id":"done_info1","component":"Text","text":"• 확정 금액: $summary"},
              {"id":"done_info2","component":"Text","text":"• 수령 지점 및 방식: $branchLabel · $methodLabel"},
              {"id":"done_info3","component":"Text","text":"• 알림 발송 연락처: $phone"},
              {"id":"btn_new_exchange","component":"Button","variant":"primary","child":"btn_new_exchange_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"new_exchange"}}}},
              {"id":"btn_new_exchange_txt","component":"Text","text":"새로운 환전 신청하기"}
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
                      {"id":"btn_reset","component":"Button","variant":"borderless","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"➕ 다시 입력하기"}
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
                      {"id":"btn_reset","component":"Button","variant":"borderless","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"➕ 다시 입력하기"}
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
                      {"id":"btn_reset","component":"Button","variant":"borderless","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"➕ 다시 입력하기"}
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
                      {"id":"btn_reset","component":"Button","variant":"borderless","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"➕ 다시 입력하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                reply to listOf(updateComponents)
            }

            else -> {
                val recipient = ctx["recipient"]?.toString()?.takeIf { it.isNotBlank() } ?: "이지훈"
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
                      {"id":"btn_reset","component":"Button","variant":"borderless","child":"btn_reset_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"done"}}}},
                      {"id":"btn_reset_txt","component":"Text","text":"➕ 다시 입력하기"}
                    ]}}
                    """.trimIndent().replace("\n", "")
                reply to listOf(updateComponents)
            }
        }
    }

    fun buildCancelExchangeVoucherJsonl(
        vouchers: List<ExchangeVoucherItem>,
    ): Pair<String, List<String>> {
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        val firstVoucher = vouchers.first()
        val optionsJson =
            vouchers.joinToString(",") { v ->
                val label =
                    "${v.voucherCode} · ${fmt.format(v.foreignAmount)} ${v.currency} (${fmt.format(v.krwAmount)}원 환불 · ${v.branchLabel})"
                        .replace("\"", "'")
                """{"label":"$label","value":"${v.voucherCode}"}"""
            }
        val reply =
            "보유 중인 미수령 환전 바우처(${vouchers.size}건) 중 취소할 내역을 선택해 주세요. 취소 즉시 주거래 통장으로 출금액 전액이 환불됩니다."
        val createSurface =
            """{"version":"v0.9.1","createSurface":{"surfaceId":"exchange_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""
        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"exchange_surface","path":"/cancelExchange","value":{"voucherCode":["${firstVoucher.voucherCode}"],"refundAccount":"DK 주거래 통장 (즉시 입금)"}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["cancel_card"]},
              {"id":"cancel_card","component":"Card","child":"cancel_col"},
              {"id":"cancel_col","component":"Column","children":["cancel_title","voucher_picker","refund_acc","btn_cancel"]},
              {"id":"cancel_title","component":"Text","text":"↩️ 외화 환전 신청 취소 (수령 전 전액 환불)"},
              {"id":"voucher_picker","component":"ChoicePicker","label":"취소할 환전 바우처 선택 (${vouchers.size}건 보유)","variant":"mutuallyExclusive","options":[$optionsJson],"value":{"path":"/cancelExchange/voucherCode"}},
              {"id":"refund_acc","component":"TextField","label":"환불 입금 계좌","value":{"path":"/cancelExchange/refundAccount"}},
              {"id":"btn_cancel","component":"Button","variant":"primary","child":"btn_cancel_txt","action":{"event":{"name":"cancel_exchange_voucher","context":{"voucherCode":{"path":"/cancelExchange/voucherCode"}}}}},
              {"id":"btn_cancel_txt","component":"Text","text":"선택한 환전 신청 취소 및 원화 환불받기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return reply to listOf(createSurface, updateData, updateComponents)
    }

    fun buildCancelExchangeDoneJsonl(
        cancelled: ExchangeVoucherItem,
        balanceAfter: Long,
        remainingCount: Int,
    ): Pair<String, List<String>> {
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        val reply =
            "${cancelled.voucherCode} (${fmt.format(cancelled.foreignAmount)} ${cancelled.currency}) 환전 신청이 취소되어 ${fmt.format(cancelled.krwAmount)}원이 환불되었습니다."
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"exchange_surface","components":[
              {"id":"root","component":"Column","children":["cancel_done_card"]},
              {"id":"cancel_done_card","component":"Card","child":"cancel_done_col"},
              {"id":"cancel_done_col","component":"Column","children":["done_title","done_info1","done_info2","done_info3","btn_new_exchange"]},
              {"id":"done_title","component":"Text","text":"✅ 환전 신청 취소 및 원화 환불 완료"},
              {"id":"done_info1","component":"Text","text":"• 취소 바우처: ${cancelled.voucherCode} (${fmt.format(cancelled.foreignAmount)} ${cancelled.currency})"},
              {"id":"done_info2","component":"Text","text":"• 환불 금액: +${fmt.format(cancelled.krwAmount)}원 (주거래 통장 즉시 입금)"},
              {"id":"done_info3","component":"Text","text":"• 환불 후 잔액: ${fmt.format(balanceAfter)}원 (남은 바우처 ${remainingCount}건)"},
              {"id":"btn_new_exchange","component":"Button","variant":"borderless","child":"btn_new_exchange_txt","action":{"event":{"name":"reset_exchange_surface","context":{"reason":"new_exchange"}}}},
              {"id":"btn_new_exchange_txt","component":"Text","text":"➕ 새로 환전 신청하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return reply to listOf(updateComponents)
    }

    /**
     * Extracts foreign currency amount from natural Korean/English expressions such as:
     * - `"6000엔"`, `"6,000엔"`, `"6천엔"`, `"5만엔"`, `"3만5천엔"`, `"1200달러"`, `"300유로"`
     * while ignoring terminal numbers (`1터미널`, `2터미널`), discount percentages (`90%`), or target rate (`900원`).
     */
    internal fun parseForeignAmountFromQuery(rawQuery: String): Long? {
        val cleaned =
            rawQuery
                .lowercase()
                .replace(",", "")
                .replace(Regex("""[12]\s*터미널"""), " ")
                .replace(Regex("""제\s*[12]\s*터미널"""), " ")
                .replace(Regex("""t[12]"""), " ")
                .replace(Regex("""\d+\s*%"""), " ")
                .replace(Regex("""\d+\s*종"""), " ")
                .replace(Regex("""100\s*엔당"""), " ")
                .replace(Regex("""\d+\s*원\s*이하"""), " ")

        // 1. 복합 "N만 M천" (예: "3만5천엔", "1만 2천 엔")
        Regex("""(\d+)\s*만\s*(\d+)\s*천""").find(cleaned)?.let { m ->
            val man = m.groupValues[1].toLongOrNull() ?: 0L
            val cheon = m.groupValues[2].toLongOrNull() ?: 0L
            val total = man * 10_000L + cheon * 1_000L
            if (total > 0L) return total
        }

        // 2. "N만" (예: "5만엔", "10만 엔", "2만달러")
        Regex("""(\d+(?:\.\d+)?)\s*만""").find(cleaned)?.let { m ->
            val num = m.groupValues[1].toDoubleOrNull() ?: 0.0
            val total = kotlin.math.round(num * 10_000.0).toLong()
            if (total > 0L) return total
        }

        // 3. "N천" (예: "6천엔", "5천 엔", "1천달러")
        Regex("""(\d+(?:\.\d+)?)\s*천""").find(cleaned)?.let { m ->
            val num = m.groupValues[1].toDoubleOrNull() ?: 0.0
            val total = kotlin.math.round(num * 1_000.0).toLong()
            if (total > 0L) return total
        }

        // 4. "N백" (예: "5백달러", "3백유로")
        Regex("""(\d+(?:\.\d+)?)\s*백""").find(cleaned)?.let { m ->
            val num = m.groupValues[1].toDoubleOrNull() ?: 0.0
            val total = kotlin.math.round(num * 100.0).toLong()
            if (total > 0L) return total
        }

        // 5. 통화 단위 앞의 직접 숫자 (예: "6000엔", "15000 jpy", "1200달러", "500 usd", "300유로")
        Regex("""(\d+)\s*(?:엔|jpy|달러|불|usd|유로|eur|프랑|chf)""").find(cleaned)?.let { m ->
            val num = m.groupValues[1].toLongOrNull() ?: 0L
            if (num > 0L) return num
        }

        // 6. 문장 내 일반 숫자 (예: "엔화 6000 환전")
        Regex("""(\d{2,})""").find(cleaned)?.let { m ->
            val num = m.groupValues[1].toLongOrNull() ?: 0L
            if (num > 0L) return num
        }

        return null
    }

    /**
     * Enforces real commercial bank banknote denomination & minimum exchange rules:
     * - **JPY**: Minimum 1,000 JPY, multiples of 1,000 JPY (500/100/50/10/5/1 JPY are coins and cannot be exchanged)
     * - **USD**: Minimum 10 USD, multiples of 1 USD (cents are coins)
     * - **EUR**: Minimum 10 EUR, multiples of 5 EUR (1/2 EUR are coins; smallest banknote is 5 EUR)
     * - **CHF**: Minimum 10 CHF, multiples of 10 CHF (5/2/1 CHF are coins; smallest banknote is 10 CHF)
     */
    internal fun normalizeToBanknoteAmount(
        currency: String,
        requestedAmount: Long,
    ): Pair<Long, String?> {
        val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
        return when (currency.uppercase()) {
            "JPY" -> {
                if (requestedAmount < 1_000L) {
                    1_000L to "💡 일본 엔화는 500엔 이하가 동전(주화)이므로 지폐 최소 권종인 1,000엔 단위로만 환전이 가능하여 최소 신청 금액인 1,000 JPY로 자동 보정했습니다."
                } else if (requestedAmount % 1_000L != 0L) {
                    val rounded = ((requestedAmount + 500L) / 1_000L) * 1_000L
                    rounded to "💡 엔화 ${fmt.format(requestedAmount % 1_000L)}엔 단위는 동전(주화)이므로 지폐 최소 권종(1,000엔권) 단위에 맞춰 ${fmt.format(rounded)} JPY로 자동 보정했습니다."
                } else {
                    requestedAmount to null
                }
            }
            "EUR" -> {
                if (requestedAmount < 10L) {
                    10L to "💡 유로화는 1·2유로가 동전(주화)이고 최소 지폐가 5유로권이므로 최소 신청 금액인 10 EUR로 자동 보정했습니다."
                } else if (requestedAmount % 5L != 0L) {
                    val rounded = ((requestedAmount + 2L) / 5L) * 5L
                    rounded to "💡 유로화는 5유로권 지폐 단위로만 환전이 가능하여 ${fmt.format(rounded)} EUR로 자동 보정했습니다."
                } else {
                    requestedAmount to null
                }
            }
            "CHF" -> {
                if (requestedAmount < 10L) {
                    10L to "💡 스위스 프랑은 5프랑 이하가 동전(주화)이고 최소 지폐가 10프랑권이므로 최소 신청 금액인 10 CHF로 자동 보정했습니다."
                } else if (requestedAmount % 10L != 0L) {
                    val rounded = ((requestedAmount + 5L) / 10L) * 10L
                    rounded to "💡 스위스 프랑은 10프랑권 지폐 단위로만 환전이 가능하여 ${fmt.format(rounded)} CHF로 자동 보정했습니다."
                } else {
                    requestedAmount to null
                }
            }
            else -> {
                if (requestedAmount < 10L) {
                    10L to "💡 미국 달러 모바일 환전 최소 신청 금액은 10 USD 이상(1달러 지폐 단위)이므로 10 USD로 자동 보정했습니다."
                } else {
                    requestedAmount to null
                }
            }
        }
    }
}
