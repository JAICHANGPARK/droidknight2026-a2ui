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
                    text = "🧾 시나리오 C · 차등 정산 & 가변 필드(Variable Schema) 역전송",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "Q1. \"미리 화면을 다 짜두면 되지 않나?\" → 1·2·3차 참석자, 무알콜 할인, 국물 누수 vs 메뉴 누락처럼 상황마다 입력 조합이 무한합니다.\n" +
                            "Q2. \"필드가 매번 달라지면 앱에서 어떻게 처리하나?\" → 앱은 고정 DTO 없이 A2uiDataModel 트리(\"/settlement\" 또는 \"/claim\")에 바인딩만 하고, 제출 시 루트 포인터만 서버로 돌려보냅니다!",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val cases =
                        listOf(
                            "① 1·2·3차 회식 차등 정산 (커스텀 위젯 0개 + 내장 함수)",
                            "② 배달 클레임 A: 국물 누수·보냉백 오염",
                            "③ 배달 클레임 B: 메뉴 누락·재배달 요청",
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
                                0 -> "현재 DataModel 스키마: /settlement/{round1Total, round2Total, soberDiscountPct, excludeLatecomer, memo}"
                                1 -> "현재 DataModel 스키마: /claim/{claimType='SPILL', spillRatioPct, bagSoiled, refundAccount}"
                                else -> "현재 DataModel 스키마: /claim/{claimType='MISSING', missingItems[], wantRedelivery, riderDoorCode}"
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
            // Zero custom widgets! Uses only Basic Catalog V1 + formatCurrency, formatString, required, length
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
        1 -> {
            // Claim Case A: Broth spill -> fields: spillRatioPct, bagSoiled, refundAccount
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"claimType":"BROTH_SPILL","orderNo":"ORD-2026-991","spillRatioPct":60,"bagSoiled":true,"refundAccount":"카카오뱅크 3333-01-998877"}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                {"id":"root","component":"Card","child":"col_spill"},
                {"id":"col_spill","component":"Column","children":["txt_spill_title","img_spill","slider_spill","chk_bag","tf_refund","btn_spill_submit"]},
                {"id":"txt_spill_title","component":"Text","variant":"h3","text":"🍲 [클레임 유형 A] 국물 용기 파손 · 누수 접수"},
                {"id":"img_spill","component":"Image","url":"https://claims.delivery.local/ord-2026-991-photo.jpg","description":"고객 업로드 파손 사진 · 비전 분석 완료","variant":"header"},
                {"id":"slider_spill","component":"Slider","label":"국물 유실/파손 추정 비율 (%)","min":10,"max":100,"value":{"path":"/claim/spillRatioPct"}},
                {"id":"chk_bag","component":"CheckBox","label":"다회용 보냉백 내부 오염 발생 (회수 세척 필요)","value":{"path":"/claim/bagSoiled"}},
                {"id":"tf_refund","component":"TextField","label":"부분 환불 입금 계좌","value":{"path":"/claim/refundAccount"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/claim/refundAccount"}}},"message":"환불 계좌를 입력해주세요."}]},
                {"id":"btn_spill_submit","component":"Button","variant":"primary","child":"btn_spill_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_BROTH_SPILL","payload":{"path":"/claim"}}}}},
                {"id":"btn_spill_txt","component":"Text","text":"파손 클레임 전송 (/claim 트리 통째로 반환)"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        else -> {
            // Claim Case B: Missing items -> completely different fields: missingItems[], wantRedelivery, riderDoorCode
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"mashup_surface","path":"/","value":{"claim":{"claimType":"MISSING_ITEMS","orderNo":"ORD-2026-991","missingItems":["cheese_ball","zero_cola"],"wantRedelivery":true,"riderDoorCode":"#1234*"}}}}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"mashup_surface","components":[
                {"id":"root","component":"Card","child":"col_missing"},
                {"id":"col_missing","component":"Column","children":["txt_miss_title","picker_missing","chk_redelivery","tf_door","btn_miss_submit"]},
                {"id":"txt_miss_title","component":"Text","variant":"h3","text":"🥡 [클레임 유형 B] 주문 메뉴 누락 · 재배달 접수"},
                {"id":"picker_missing","component":"ChoicePicker","label":"영수증 대조 결과 누락된 품목 선택 (복수 선택)","variant":"multipleSelection","options":[{"label":"모짜렐라 치즈볼 (5,000원)","value":"cheese_ball"},{"label":"제로 콜라 500ml (2,500원)","value":"zero_cola"},{"label":"갈릭 디핑소스 (1,000원)","value":"garlic_dip"}],"value":{"path":"/claim/missingItems"}},
                {"id":"chk_redelivery","component":"CheckBox","label":"부분 환불 대신 누락 메뉴 즉시 재배달 요청","value":{"path":"/claim/wantRedelivery"}},
                {"id":"tf_door","component":"TextField","label":"재배달 라이더 공동현관 비밀번호","value":{"path":"/claim/riderDoorCode"},"checks":[{"condition":{"call":"required","args":{"value":{"path":"/claim/riderDoorCode"}}},"message":"공동현관 출입 방법을 입력해주세요."}]},
                {"id":"btn_miss_submit","component":"Button","variant":"primary","child":"btn_miss_txt","action":{"event":{"name":"submit_dynamic_payload","context":{"schemaType":"CLAIM_MISSING_ITEMS","payload":{"path":"/claim"}}}}},
                {"id":"btn_miss_txt","component":"Text","text":"누락 클레임 전송 (/claim 트리 통째로 반환)"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
    }
}
