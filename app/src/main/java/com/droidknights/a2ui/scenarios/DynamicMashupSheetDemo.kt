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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.A2uiTelemetryPanel
import kotlinx.coroutines.launch

/**
 * Domain Scenario C: Group Gathering Differential Dutch-Pay & Variable-Field Claim Intake
 * + Official Feature #4: Zero-Custom-Widget Built-in Functions (`formatCurrency`, `formatString`,
 *   `required`, `length`, `regex`) & Variable DataModel Round-Trip (`{"path": "/claim"}`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicMashupSheetDemo() {
    var selectedCase by remember { mutableIntStateOf(0) }

    val catalog = remember { DroidKnightsCatalogs.createStandardCatalog() }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedCase) {
            A2uiMessageProcessor(catalogs = listOf(catalog))
        }

    LaunchedEffect(processor) {
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                outboundLog.add(msg)
            }
        }
        val lines = buildMashupJsonl(selectedCase)
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
                    text = "🧩 시나리오 C · 왜 미리 Compose로 다 짜두면 안 될까? (즉석 합체 & 동적 문진)",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "Q1. \"그냥 미리 Compose로 화면 다 만들어 놓고 부르면 되는 거 아님?\"\n" +
                            "→ ① 여러 메뉴에 흩어진 기능의 즉석 합체(일본 여행 환전+카드차단해제+보험)와 ② 상황마다 질문 칸 자체가 완전히 달라지는 동적 문진(상황 A·B·C)은 미리 화면을 짜둘 수 없습니다!\n" +
                            "Q2. \"필드가 매번 다른데 앱에서 어떻게 처리해?\" → 앱(ViewModel)은 필드를 꺼내 보지 않고 우체부처럼 outboundEvents를 그대로 AI에게 토스합니다!",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val cases =
                        listOf(
                            "① [즉석 합체] 일본 여행 (5만엔 환전+카드차단해제+보험)",
                            "② [동적 문진 A] 음식에 머리카락 (사진+수거일시+환불계좌)",
                            "③ [동적 문진 B] 벨 누르고 감 (주의전달+요청사항 기본값 고정)",
                            "④ [동적 문진 C] 쿠폰 깜빡함 (보유쿠폰 선택+차액 재결제)",
                            "⑤ [로컬 수식] 회식 차등 정산 (formatCurrency)",
                        )
                    cases.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedCase == index,
                            onClick = { selectedCase = index },
                            label = { Text(title, fontSize = 12.sp) },
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text =
                            when (selectedCase) {
                                0 -> "현재 DataModel 스키마: /japanTrip/{jpyAmount, unblockOverseasCard, blockDccKrw, join3DayInsurance} → 3개 메뉴 화면을 1장의 Surface로 즉석 합체!"
                                1 -> "현재 DataModel 스키마: /claim/{issueType='HAIR_FOREIGN', pickupDateTime, refundAccount} → 사진 + DateTimeInput(수거일시) + ChoicePicker(환불계좌)"
                                2 -> "현재 DataModel 스키마: /claim/{issueType='DOORBELL_RUNG', notifyRiderWarning, pinDefaultRequest} → 사진·수거일시·계좌 불필요! 체크박스 2개만 합성"
                                3 -> "현재 DataModel 스키마: /claim/{issueType='MISSED_COUPON', selectedCouponId, approveDiffRebill} → 보유 쿠폰 ChoicePicker + 차액 자동 재결제 승인"
                                else -> "현재 DataModel 스키마: /settlement/{round1Total, round2Total, soberDiscountPct, includeTaxi, bankMemo}"
                            },
                        color = Color(0xFF7DD3FC),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp),
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

internal fun buildMashupJsonl(caseIndex: Int): List<String> {
    val createMsg =
        """{"version":"v0.9.1","createSurface":{"surfaceId":"mashup_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

    return when (caseIndex) {
        0 -> {
            // 체감 예시 ①: 여러 화면에 흩어진 기능의 즉석 합체 (Cross-Feature Mashup)
            // "나 내일 일본 가는데, 5만 엔 환전 신청하면서 일본 현지 카드 결제 차단 풀고, 3일짜리 여행자 보험도 제일 싼 걸로 한 번에 켜줘."
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"japanTrip":{"jpyAmount":50000,"unblockOverseasCard":true,"blockDccKrw":true,"join3DayInsurance":true,"pickupAirport":"인천공항 제1터미널 환전소"}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                {"id":"root","component":"Card","child":"col_japan"},
                {"id":"col_japan","component":"Column","children":["txt_jp_title","txt_jp_sub","slider_jpy","chk_card_unblock","chk_dcc","chk_insurance","btn_jp_exec"]},
                {"id":"txt_jp_title","component":"Text","variant":"h3","text":"✈️ [즉석 합체 Mashup] 내일 일본 출국 원스톱 제어판"},
                {"id":"txt_jp_sub","component":"Text","variant":"caption","text":"기존 앱의 [환전 화면] + [카드 해외결제 관리 화면] + [여행자 보험 화면] 3개 메뉴를 이동 없이 1장의 Surface로 즉석 합성했습니다."},
                {"id":"slider_jpy","component":"Slider","label":"① 엔화(JPY) 환전 신청 금액 (엔)","min":10000,"max":200000,"value":{"path":"/japanTrip/jpyAmount"}},
                {"id":"chk_card_unblock","component":"CheckBox","label":"② 일본 현지 오프라인 IC 카드 결제 차단 해제","value":{"path":"/japanTrip/unblockOverseasCard"}},
                {"id":"chk_dcc","component":"CheckBox","label":"③ 해외 원화결제(DCC) 이중수수료 자동 차단 켜기","value":{"path":"/japanTrip/blockDccKrw"}},
                {"id":"chk_insurance","component":"CheckBox","label":"④ 3일 실속형 여행자 보험(4,200원 · 휴대품 손해 포함) 즉시 가입 동의","value":{"path":"/japanTrip/join3DayInsurance"}},
                {"id":"btn_jp_exec","component":"Button","variant":"primary","child":"btn_jp_txt","action":{"event":{"name":"execute_cross_feature_mashup","context":{"mashupType":"JAPAN_TRIP_COMBO","payload":{"path":"/japanTrip"}}}}},
                {"id":"btn_jp_txt","component":"Text","text":"환전 + 카드 차단 해제 + 여행자 보험 한 번에 실행하기"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        1 -> {
            // 체감 예시 ② - 상황 A: "음식에 머리카락이 나왔어요"
            // 필요한 UI: [사진 첨부/확인 Image] + [음식 수거 희망 날짜/시간 DateTimeInput] + [환불 계좌 선택 ChoicePicker]
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"issueType":"HAIR_IN_FOOD","orderNo":"ORD-2026-991","pickupDateTime":"2026-10-16T11:00:00Z","refundAccount":["toss_main"]}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                {"id":"root","component":"Card","child":"col_hair"},
                {"id":"col_hair","component":"Column","children":["txt_hair_title","img_hair","dt_pickup","picker_account","btn_hair_submit"]},
                {"id":"txt_hair_title","component":"Text","variant":"h3","text":"🔍 [상황 A] 음식 이물질(머리카락) 접수 · 회수 및 환불"},
                {"id":"img_hair","component":"Image","url":"https://claims.delivery.local/ord-2026-991-foreign.jpg","description":"고객 첨부 이물질 사진 확인 완료","variant":"header"},
                {"id":"dt_pickup","component":"DateTimeInput","label":"위생팀 음식 수거 희망 날짜 및 시간","enableDate":true,"enableTime":true,"value":{"path":"/claim/pickupDateTime"}},
                {"id":"picker_account","component":"ChoicePicker","label":"100% 환불 입금 계좌 선택","variant":"mutuallyExclusive","options":[{"label":"토스뱅크 주계좌 (1000-**-5678)","value":"toss_main"},{"label":"카카오뱅크 생활비 계좌 (3333-**-8877)","value":"kakao_sub"},{"label":"결제 수단(신한카드) 즉시 취소","value":"card_cancel"}],"value":{"path":"/claim/refundAccount"}},
                {"id":"btn_hair_submit","component":"Button","variant":"primary","child":"btn_hair_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_HAIR_FOREIGN","payload":{"path":"/claim"}}}}},
                {"id":"btn_hair_txt","component":"Text","text":"수거 예약 및 환불 접수 (/claim 서브트리 전송)"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        2 -> {
            // 체감 예시 ② - 상황 B: "배달 기사님이 벨 누르지 말랬는데 누르고 가셨어요"
            // 필요한 UI: 사진도, 수거 날짜도, 환불 계좌도 필요 없음! [기사님께 주의 전달 CheckBox] + [다음 주문 시 요청사항 기본값으로 고정 스위치]
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"issueType":"DOORBELL_RUNG","orderNo":"ORD-2026-991","notifyRiderWarning":true,"pinDefaultSilentNote":true}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                {"id":"root","component":"Card","child":"col_bell"},
                {"id":"col_bell","component":"Column","children":["txt_bell_title","txt_bell_desc","chk_rider_warn","chk_pin_default","btn_bell_submit"]},
                {"id":"txt_bell_title","component":"Text","variant":"h3","text":"🔕 [상황 B] 비대면 요청사항 미준수 (초인종 누름)"},
                {"id":"txt_bell_desc","component":"Text","variant":"caption","text":"사진 첨부나 음식 수거 일시, 환불 계좌 입력 칸은 전혀 생성되지 않고 지금 상황에 꼭 필요한 컨트롤 2개만 조합됩니다."},
                {"id":"chk_rider_warn","component":"CheckBox","label":"해당 배달 기사님께 '벨 누르지 말아주세요' 주의 알림 전달","value":{"path":"/claim/notifyRiderWarning"}},
                {"id":"chk_pin_default","component":"CheckBox","label":"다음 주문 시에도 '문 앞에 두고 노크/벨 X' 요청사항을 기본값으로 고정","value":{"path":"/claim/pinDefaultSilentNote"}},
                {"id":"btn_bell_submit","component":"Button","variant":"primary","child":"btn_bell_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_DOORBELL_RUNG","payload":{"path":"/claim"}}}}},
                {"id":"btn_bell_txt","component":"Text","text":"기사님 주의 전달 및 기본 요청사항 고정"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        3 -> {
            // 체감 예시 ② - 상황 C: "쿠폰 적용을 깜빡하고 결제했어요"
            // 필요한 UI: [보유 중인 사용 가능 쿠폰 목록 ChoicePicker] + [차액 자동 재결제 승인 Button]
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"issueType":"MISSED_COUPON","orderNo":"ORD-2026-991","selectedCoupon":["dk_vip_5000"]}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                {"id":"root","component":"Card","child":"col_coupon"},
                {"id":"col_coupon","component":"Column","children":["txt_cp_title","picker_coupon","btn_cp_rebill"]},
                {"id":"txt_cp_title","component":"Text","variant":"h3","text":"🎟️ [상황 C] 결제 후 미적용 쿠폰 사후 적용 · 차액 재결제"},
                {"id":"picker_coupon","component":"ChoicePicker","label":"현재 주문(28,000원)에 적용 가능한 보유 쿠폰 선택","variant":"mutuallyExclusive","options":[{"label":"드로이드나이츠 VIP 5,000원 할인 쿠폰 (오늘 만료)","value":"dk_vip_5000"},{"label":"주말 전 메뉴 15% 할인 쿠폰 (-4,200원)","value":"weekend_15pct"}],"value":{"path":"/claim/selectedCoupon"}},
                {"id":"btn_cp_rebill","component":"Button","variant":"primary","child":"btn_cp_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_MISSED_COUPON_REBILL","payload":{"path":"/claim"}}}}},
                {"id":"btn_cp_txt","component":"Text","text":"주문 취소 없이 쿠폰 적용 후 차액 자동 재결제 승인"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        else -> {
            // 보너스: Zero custom widgets! Uses only Basic Catalog V1 + formatCurrency, formatString, required, length
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"settlement":{"groupName":"DroidKnights 스피커 뒤풀이 (6명)","round1Total":186000,"round2Total":94000,"soberDiscountPct":50,"includeTaxi":true,"bankMemo":"토스뱅크 1000-1234-5678"}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                {"id":"root","component":"Card","child":"col_dutch"},
                {"id":"col_dutch","component":"Column","children":["txt_title","txt_r1","txt_r2","slider_sober","chk_taxi","tf_bank","btn_send_kakao"]},
                {"id":"txt_title","component":"Text","variant":"h3","text":{"path":"/settlement/groupName"}},
                {"id":"txt_r1","component":"Text","variant":"body","text":{"call":"formatString","args":{"value":"1차 삼겹살 총액 (6명 전원): ${'$'}{formatCurrency(value:${'$'}{/settlement/round1Total}, currency:'KRW', decimals:0)}"}}},
                {"id":"txt_r2","component":"Text","variant":"body","text":{"call":"formatString","args":{"value":"2차 수제맥주 총액 (4명 참석): ${'$'}{formatCurrency(value:${'$'}{/settlement/round2Total}, currency:'KRW', decimals:0)}"}}},
                {"id":"slider_sober","component":"Slider","label":"비음주자(운전자 2명) 2차 분담금 감면율 (%)","min":0,"max":100,"value":{"path":"/settlement/soberDiscountPct"}},
                {"id":"chk_taxi","component":"CheckBox","label":"심야 막차 이후 택시비 영수증(28,000원) N분의 1 합산","value":{"path":"/settlement/includeTaxi"}},
                {"id":"tf_bank","component":"TextField","label":"입금 계좌 안내 문구 (최소 8자 이상)","value":{"path":"/settlement/bankMemo"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/settlement/bankMemo"}}},"message":"입금 계좌 안내를 입력해주세요."},{"condition":{"call":"length","args":{"value":{"path":"/settlement/bankMemo"},"min":8}},"message":"계좌 번호를 포함해 8자 이상 입력해주세요."}]},
                {"id":"btn_send_kakao","component":"Button","variant":"primary","child":"btn_send_txt","checks":[{"condition":{"call":"length","args":{"value":{"path":"/settlement/bankMemo"},"min":8}},"message":"계좌 번호가 너무 짧습니다."}],"action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"DUTCH_PAY_MULTI_ROUND","payload":{"path":"/settlement"}}}}},
                {"id":"btn_send_txt","component":"Text","text":"차등 정산표 확정 및 에이전트로 전송"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
    }
}

