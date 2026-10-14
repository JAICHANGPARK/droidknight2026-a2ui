package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
 * Domain Scenario A: 2 AM Mobile On-Call Incident Commander
 * + Official Feature #3:
 *   1. Local Component Tree Interception (`ProvideActionInterceptor` in `ActionGuardContainer`)
 *   2. Global Processor Interception (`A2uiActionInterceptor` in `A2uiMessageProcessor`)
 *   3. Read-Only Observer Mode (`bindUpdater == null` when literal values are sent instead of `{"path": ...}`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncidentCommanderDemo() {
    var selectedIncident by remember { mutableIntStateOf(0) }
    var isObserverReadOnly by remember { mutableStateOf(false) }

    val catalog = remember { DroidKnightsCatalogs.createStandardCatalog() }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val processorAuditLog = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedIncident, isObserverReadOnly) {
            val globalAuditInterceptor =
                object : A2uiActionInterceptor {
                    override suspend fun onInterceptAction(action: A2uiUserAction): A2uiUserAction? {
                        return if (action is A2uiEventAction) {
                            val enrichedContext =
                                action.context.toMutableMap().apply {
                                    put("biometricAudit", "VERIFIED_BY_PROCESSOR_INTERCEPTOR")
                                }
                            processorAuditLog.add(
                                "[A2uiActionInterceptor] '${action.eventName}' 승인됨 -> context에 biometricAudit 서명 주입 완료",
                            )
                            A2uiEventAction(
                                surfaceId = action.surfaceId,
                                componentId = action.componentId,
                                timestamp = action.timestamp,
                                eventName = action.eventName,
                                context = enrichedContext,
                            )
                        } else {
                            action
                        }
                    }
                }
            A2uiMessageProcessor(
                catalogs = listOf(catalog),
                interceptors = listOf(globalAuditInterceptor),
            )
        }

    LaunchedEffect(processor) {
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                outboundLog.add(msg)
            }
        }
        val lines = buildIncidentJsonl(selectedIncident, isObserverReadOnly)
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
                    text = "🚨 시나리오 A · 새벽 2시 모바일 온콜 인시던트 커맨더",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "장애 유형(PG 타임아웃 vs Pod OOM vs 카나리 배포 불량)마다 필요한 진단 지표와 복구 컨트롤이 전혀 다릅니다. " +
                            "에이전트가 상황에 맞는 복구 콘솔을 즉석 조립하며, 위험 액션은 ProvideActionInterceptor가 로컬 생체인증으로 가로챕니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val incidents =
                        listOf(
                            "① 결제 PG 타임아웃 급증",
                            "② 주문 워커 OOM 크래시",
                            "③ 카나리 v4.18.2 배포 불량",
                        )
                    incidents.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedIncident == index,
                            onClick = { selectedIncident = index },
                            label = { Text(title, fontSize = 12.sp) },
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isObserverReadOnly) Color(0xFF451A03) else Color(0xFF064E3B),
                    border =
                        BorderStroke(
                            1.dp,
                            if (isObserverReadOnly) Color(0xFFF59E0B) else Color(0xFF10B981),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text =
                                    if (isObserverReadOnly) {
                                        "👁️ 관찰자(Observer) 모드 · 리터럴 전송 (bindUpdater == null)"
                                    } else {
                                        "🛠️ 온콜 커맨더 모드 · JSON Pointer 바인딩 ({\"path\": \"/ops/...\"})"
                                    },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                            Text(
                                text =
                                    if (isObserverReadOnly) {
                                        "에이전트가 {\"path\": ...} 대신 리터럴 값을 내려보내면 Slider/TextField/Dial이 자동으로 읽기 전용으로 잠깁니다."
                                    } else {
                                        "모든 컨트롤이 DataModel 경로에 양방향 바인딩되어 조작 즉시 clientDataModel에 반영됩니다."
                                    },
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                            )
                        }
                        Switch(
                            checked = isObserverReadOnly,
                            onCheckedChange = { isObserverReadOnly = it },
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

        if (processorAuditLog.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF064E3B),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "🛡️ 글로벌 프로세서 인터셉터 감사 로그 (A2uiActionInterceptor)",
                        color = Color(0xFF6EE7B7),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                    )
                    processorAuditLog.forEach { entry ->
                        Text(
                            text = entry,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        A2uiTelemetryPanel(
            outboundLog = outboundLog,
            jsonlStream = jsonlLines,
            onClearOutbound = {
                outboundLog.clear()
                processorAuditLog.clear()
            },
        )
    }
}

internal fun buildIncidentJsonl(incidentIndex: Int, readOnly: Boolean): List<String> {
    val createMsg =
        """{"version":"v0.9.1","createSurface":{"surfaceId":"oncall_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

    return when (incidentIndex) {
        0 -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/","value":{"ops":{"incidentId":"INC-8821","backupWeight":75,"drainQueue":true,"operatorNote":"이니시스 -> 토스페이먼츠 우회"}}}}"""
            val weightExpr = if (readOnly) "75" else """{"path":"/ops/backupWeight"}"""
            val drainExpr = if (readOnly) "true" else """{"path":"/ops/drainQueue"}"""
            val noteExpr = if (readOnly) """"이니시스 -> 토스페이먼츠 우회 (읽기전용)"""" else """{"path":"/ops/operatorNote"}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
                {"id":"root","component":"Card","child":"col_main"},
                {"id":"col_main","component":"Column","children":["txt_header","spark_p99","dial_weight","chk_drain","tf_note","guard_failover"]},
                {"id":"txt_header","component":"Text","variant":"h3","text":"[INC-8821] PG 결제 승인 API p99 8,400ms 돌파"},
                {"id":"spark_p99","component":"MetricSparkline","metricName":"PG Approval p99 Latency","currentValue":"8,420 ms (정상 < 450ms)","severity":"critical"},
                {"id":"dial_weight","component":"CircularClimateDial","label":"백업 PG(토스페이먼츠) 우회 가중치","unit":"%","min":0,"max":100,"step":5,"value":$weightExpr},
                {"id":"chk_drain","component":"CheckBox","label":"기존 타임아웃 대기 큐 자동 재시도 활성화","value":$drainExpr},
                {"id":"tf_note","component":"TextField","label":"온콜 조치 사유 (필수)","value":$noteExpr,"checks":[{"condition":{"call":"required","args":{"value":$noteExpr}},"message":"우회 사유를 입력해야 실행할 수 있습니다."}]},
                {"id":"guard_failover","component":"ActionGuardContainer","child":"btn_failover","guardedActionName":"failover_pg_traffic","guardTitle":"프로덕션 PG 라우팅 변경 (2FA 필수)"},
                {"id":"btn_failover","component":"Button","variant":"primary","child":"btn_failover_txt","checks":[{"condition":{"call":"required","args":{"value":$noteExpr}},"message":"조치 사유가 비어 있습니다."}],"action":{"event":{"name":"failover_pg_traffic","context":{"incidentId":{"path":"/ops/incidentId"},"backupWeight":$weightExpr,"drainQueue":$drainExpr,"note":$noteExpr}}}},
                {"id":"btn_failover_txt","component":"Text","text":"백업 PG로 트래픽 즉시 우회 실행"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        1 -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/","value":{"ops":{"incidentId":"INC-8849","usedGb":31.4,"maxGb":32.0,"targetReplicas":8,"captureHeap":true}}}}"""
            val replicasExpr = if (readOnly) "8" else """{"path":"/ops/targetReplicas"}"""
            val heapExpr = if (readOnly) "true" else """{"path":"/ops/captureHeap"}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
                {"id":"root","component":"Card","child":"col_main"},
                {"id":"col_main","component":"Column","children":["txt_header","budget_mem","slider_replicas","chk_heap","guard_restart"]},
                {"id":"txt_header","component":"Text","variant":"h3","text":"[INC-8849] order-worker-pod OOMKilled 연쇄 발생"},
                {"id":"budget_mem","component":"PowerBudgetBar","title":"K8s 클러스터 메모리 할당량 (GiB)","usedKw":{"path":"/ops/usedGb"},"maxKw":{"path":"/ops/maxGb"},"detail":"JVM Heap 98.1% 도달 · GC Overhead Limit 초과 직전"},
                {"id":"slider_replicas","component":"Slider","label":"주문 워커 레플리카 증설 개수 (2 ~ 16 Pods)","min":2,"max":16,"value":$replicasExpr},
                {"id":"chk_heap","component":"CheckBox","label":"롤링 재시작 전 S3로 힙 덤프(Heap Dump) 자동 업로드","value":$heapExpr},
                {"id":"guard_restart","component":"ActionGuardContainer","child":"btn_restart","guardedActionName":"restart_worker_pods","guardTitle":"프로덕션 워커 클러스터 강제 재시작 보호"},
                {"id":"btn_restart","component":"Button","variant":"primary","child":"btn_restart_txt","action":{"event":{"name":"restart_worker_pods","context":{"incidentId":{"path":"/ops/incidentId"},"targetReplicas":$replicasExpr,"captureHeap":$heapExpr}}}},
                {"id":"btn_restart_txt","component":"Text","text":"레플리카 스케일아웃 및 롤링 재시작"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
        else -> {
            val dataMsg =
                """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/","value":{"ops":{"incidentId":"INC-8902","canaryTitle":"checkout-service v4.18.2 카나리 노드","hotspotLabel":"ap-northeast-2a · canary-pod-03 (5xx 42%)","rollbackTicket":"HOTFIX-912"}}}}"""
            val ticketExpr = if (readOnly) """"HOTFIX-912"""" else """{"path":"/ops/rollbackTicket"}"""
            val compMsg =
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
                {"id":"root","component":"Card","child":"col_main"},
                {"id":"col_main","component":"Column","children":["txt_header","hotspot_canary","tf_ticket","guard_rollback"]},
                {"id":"txt_header","component":"Text","variant":"h3","text":"[INC-8902] 카나리 배포 v4.18.2 직후 HTTP 502 급증"},
                {"id":"hotspot_canary","component":"HotspotDiagramCard","title":{"path":"/ops/canaryTitle"},"hotspotLabel":{"path":"/ops/hotspotLabel"},"zoneCode":"CANARY-5%","status":"502 Bad Gateway"},
                {"id":"tf_ticket","component":"TextField","label":"롤백 승인 티켓 번호 (HOTFIX-숫자)","value":$ticketExpr,"checks":[{"condition":{"call":"regex","args":{"value":$ticketExpr,"pattern":"^HOTFIX-[0-9]{3,}$"}},"message":"'HOTFIX-123' 형식으로 입력해야 합니다."}]},
                {"id":"guard_rollback","component":"ActionGuardContainer","child":"btn_rollback","guardedActionName":"production_rollback","guardTitle":"프로덕션 즉시 롤백 (v4.18.2 -> v4.18.1)"},
                {"id":"btn_rollback","component":"Button","variant":"primary","child":"btn_rollback_txt","checks":[{"condition":{"call":"regex","args":{"value":$ticketExpr,"pattern":"^HOTFIX-[0-9]{3,}$"}},"message":"유효한 HOTFIX 티켓 번호가 필요합니다."}],"action":{"event":{"name":"production_rollback","context":{"incidentId":{"path":"/ops/incidentId"},"ticket":$ticketExpr,"targetVersion":"v4.18.1"}}}},
                {"id":"btn_rollback_txt","component":"Text","text":"v4.18.1 안정 버전으로 즉시 롤백"}
            ]}}""".replace("\n", "")
            listOf(createMsg, dataMsg, compMsg)
        }
    }
}
