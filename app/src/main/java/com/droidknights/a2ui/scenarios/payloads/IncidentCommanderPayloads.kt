package com.droidknights.a2ui.scenarios.payloads

import androidx.a2ui.model.protocol.A2uiClientEventMessage
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.common.AgentPromptPreset

object IncidentCommanderPayloads {

    val promptPresets: List<AgentPromptPreset> =
        listOf(
            AgentPromptPreset(
                id = 0,
                shortChipLabel = "PG 타임아웃 우회",
                userPromptText = "결제 승인 API(INC-8821) 지연이 심해. 백업 PG로 우회하는 제어판 띄워줘.",
                agentIntroText = "INC-8821 결제 지연 대응용 백업 PG 우회 제어판입니다. 가중치를 조정하고 '트래픽 우회 실행' 후 로컬 승인을 진행해 보세요.",
                schemaBadge = "oncall_surface · /ops/backupWeight",
            ),
            AgentPromptPreset(
                id = 1,
                shortChipLabel = "주문 워커 증설",
                userPromptText = "주문 워커 파드(INC-8849)가 OOM으로 죽네. 레플리카 증설 콘솔 줘.",
                agentIntroText = "INC-8849 클러스터 메모리 상태와 워커 스케일아웃 제어판입니다.",
                schemaBadge = "oncall_surface · /ops/targetReplicas",
            ),
            AgentPromptPreset(
                id = 2,
                shortChipLabel = "카나리 즉시 롤백",
                userPromptText = "checkout-service v4.18.2 카나리에서 502 에러가 급증해. 롤백할게.",
                agentIntroText = "canary-pod-03 에러 감지(42%) 및 v4.18.1 롤백 카드입니다.",
                schemaBadge = "oncall_surface · /ops/rollbackTicket",
            ),
            AgentPromptPreset(
                id = 3,
                shortChipLabel = "DB CPU 포화 제어",
                userPromptText = "DB 읽기 레플리카 CPU가 95%야. 커넥션 풀 조절 패널 열어줘.",
                agentIntroText = "INC-8930 RDS 부하 게이지 및 슬로우 쿼리 종료 제어판입니다.",
                schemaBadge = "oncall_surface · /ops/maxPoolSize",
            ),
            AgentPromptPreset(
                id = 4,
                shortChipLabel = "웹훅 큐 재처리",
                userPromptText = "파트너 웹훅 큐가 5만 건 밀려있어. 속도 조절하고 DLQ 재처리할래.",
                agentIntroText = "INC-8977 웹훅 큐(52,400건) RPS 조절 및 DLQ 재처리 시트입니다.",
                schemaBadge = "oncall_surface · /ops/rateLimitRps",
            ),
        )

    val incidentTitles: List<String> = promptPresets.map { it.shortChipLabel }

    fun buildJsonl(incidentIndex: Int, readOnly: Boolean): List<String> {
        val createMsg =
            """{"version":"v0.9.1","createSurface":{"surfaceId":"oncall_surface","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}"""

        return when (incidentIndex) {
            0 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/","value":{"ops":{"incidentId":"INC-8821","backupWeight":75,"drainQueue":true,"operatorNote":"토스페이먼츠 우회"}}}}"""
                val weightExpr = if (readOnly) "75" else """{"path":"/ops/backupWeight"}"""
                val drainExpr = if (readOnly) "true" else """{"path":"/ops/drainQueue"}"""
                val noteExpr =
                    if (readOnly) {
                        """"토스페이먼츠 우회 (읽기전용)""""
                    } else {
                        """{"path":"/ops/operatorNote"}"""
                    }
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
                    {"id":"root","component":"Card","child":"col_main"},
                    {"id":"col_main","component":"Column","children":["txt_header","spark_p99","dial_weight","chk_drain","tf_note","guard_failover"]},
                    {"id":"txt_header","component":"Text","text":"🚨 [INC-8821] PG 결제 승인 지연 우회 (1/2단계)"},
                    {"id":"spark_p99","component":"MetricSparkline","metricName":"PG p99 Latency","currentValue":"8,420 ms","severity":"critical"},
                    {"id":"dial_weight","component":"CircularClimateDial","label":"백업 PG 우회 가중치","unit":"%","min":0,"max":100,"step":5,"value":$weightExpr},
                    {"id":"chk_drain","component":"CheckBox","label":"타임아웃 대기 큐 자동 재시도","value":$drainExpr},
                    {"id":"tf_note","component":"TextField","label":"조치 사유","value":$noteExpr,"checks":[{"condition":{"call":"required","args":{"value":$noteExpr}},"message":"우회 사유를 입력해 주세요."}]},
                    {"id":"guard_failover","component":"ActionGuardContainer","child":"btn_failover","guardedActionName":"failover_pg_traffic","guardTitle":"프로덕션 PG 라우팅 변경"},
                    {"id":"btn_failover","component":"Button","variant":"primary","child":"btn_failover_txt","checks":[{"condition":{"call":"required","args":{"value":$noteExpr}},"message":"조치 사유가 비어 있습니다."}],"action":{"event":{"name":"failover_pg_traffic","context":{"incidentId":{"path":"/ops/incidentId"},"backupWeight":$weightExpr,"drainQueue":$drainExpr,"note":$noteExpr}}}},
                    {"id":"btn_failover_txt","component":"Text","text":"트래픽 우회 실행 →"}
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
                    {"id":"txt_header","component":"Text","text":"🚨 [INC-8849] 주문 워커 OOM 증설 (1/2단계)"},
                    {"id":"budget_mem","component":"PowerBudgetBar","title":"클러스터 메모리 할당량 (GiB)","usedKw":{"path":"/ops/usedGb"},"maxKw":{"path":"/ops/maxGb"},"detail":"JVM Heap 98.1% 도달"},
                    {"id":"slider_replicas","component":"Slider","label":"워커 레플리카 수 (2 ~ 16)","min":2,"max":16,"value":$replicasExpr},
                    {"id":"chk_heap","component":"CheckBox","label":"재시작 전 S3 힙 덤프 보관","value":$heapExpr},
                    {"id":"guard_restart","component":"ActionGuardContainer","child":"btn_restart","guardedActionName":"restart_worker_pods","guardTitle":"워커 클러스터 롤링 재시작"},
                    {"id":"btn_restart","component":"Button","variant":"primary","child":"btn_restart_txt","action":{"event":{"name":"restart_worker_pods","context":{"incidentId":{"path":"/ops/incidentId"},"targetReplicas":$replicasExpr,"captureHeap":$heapExpr}}}},
                    {"id":"btn_restart_txt","component":"Text","text":"스케일아웃 및 재시작 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            2 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/","value":{"ops":{"incidentId":"INC-8902","canaryTitle":"checkout-service v4.18.2","hotspotLabel":"ap-northeast-2a · canary-pod-03","rollbackTicket":"HOTFIX-912"}}}}"""
                val ticketExpr = if (readOnly) """"HOTFIX-912"""" else """{"path":"/ops/rollbackTicket"}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
                    {"id":"root","component":"Card","child":"col_main"},
                    {"id":"col_main","component":"Column","children":["txt_header","hotspot_canary","tf_ticket","guard_rollback"]},
                    {"id":"txt_header","component":"Text","text":"🚨 [INC-8902] 카나리 v4.18.2 롤백 (1/2단계)"},
                    {"id":"hotspot_canary","component":"HotspotDiagramCard","title":{"path":"/ops/canaryTitle"},"hotspotLabel":{"path":"/ops/hotspotLabel"},"zoneCode":"CANARY-5%","status":"502 Bad Gateway"},
                    {"id":"tf_ticket","component":"TextField","label":"롤백 승인 티켓 (HOTFIX-숫자)","value":$ticketExpr,"checks":[{"condition":{"call":"regex","args":{"value":$ticketExpr,"pattern":"^HOTFIX-[0-9]{3,}$"}},"message":"'HOTFIX-123' 형식으로 입력해 주세요."}]},
                    {"id":"guard_rollback","component":"ActionGuardContainer","child":"btn_rollback","guardedActionName":"production_rollback","guardTitle":"즉시 롤백 (v4.18.2 -> v4.18.1)"},
                    {"id":"btn_rollback","component":"Button","variant":"primary","child":"btn_rollback_txt","checks":[{"condition":{"call":"regex","args":{"value":$ticketExpr,"pattern":"^HOTFIX-[0-9]{3,}$"}},"message":"유효한 티켓 번호가 필요합니다."}],"action":{"event":{"name":"production_rollback","context":{"incidentId":{"path":"/ops/incidentId"},"ticket":$ticketExpr,"targetVersion":"v4.18.1"}}}},
                    {"id":"btn_rollback_txt","component":"Text","text":"v4.18.1 즉시 롤백 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            3 -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/","value":{"ops":{"incidentId":"INC-8930","cpuUsedKw":9.5,"cpuMaxKw":10.0,"maxPoolSize":30,"killSlowQueries":true}}}}"""
                val poolExpr = if (readOnly) "30" else """{"path":"/ops/maxPoolSize"}"""
                val killExpr = if (readOnly) "true" else """{"path":"/ops/killSlowQueries"}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
                    {"id":"root","component":"Card","child":"col_main"},
                    {"id":"col_main","component":"Column","children":["txt_header","budget_cpu","dial_pool","chk_kill_slow","guard_db"]},
                    {"id":"txt_header","component":"Text","text":"🚨 [INC-8930] DB 읽기 레플리카 CPU 포화 (1/2단계)"},
                    {"id":"budget_cpu","component":"PowerBudgetBar","title":"RDS vCPU 부하율","usedKw":{"path":"/ops/cpuUsedKw"},"maxKw":{"path":"/ops/cpuMaxKw"},"detail":"슬로우 쿼리 14건 실행 중"},
                    {"id":"dial_pool","component":"CircularClimateDial","label":"최대 커넥션 풀 상한","unit":"개","min":10,"max":100,"step":5,"value":$poolExpr},
                    {"id":"chk_kill_slow","component":"CheckBox","label":"3초 초과 슬로우 쿼리 강제 종료","value":$killExpr},
                    {"id":"guard_db","component":"ActionGuardContainer","child":"btn_db","guardedActionName":"throttle_db_pool","guardTitle":"DB 풀 조절 및 쿼리 종료 승인"},
                    {"id":"btn_db","component":"Button","variant":"primary","child":"btn_db_txt","action":{"event":{"name":"throttle_db_pool","context":{"incidentId":{"path":"/ops/incidentId"},"maxPoolSize":$poolExpr,"killSlowQueries":$killExpr}}}},
                    {"id":"btn_db_txt","component":"Text","text":"커넥션 풀 조절 실행 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
            else -> {
                val dataMsg =
                    """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/","value":{"ops":{"incidentId":"INC-8977","rateLimitRps":450,"replayDlq":true}}}}"""
                val rpsExpr = if (readOnly) "450" else """{"path":"/ops/rateLimitRps"}"""
                val dlqExpr = if (readOnly) "true" else """{"path":"/ops/replayDlq"}"""
                val compMsg =
                    """{"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
                    {"id":"root","component":"Card","child":"col_main"},
                    {"id":"col_main","component":"Column","children":["txt_header","spark_queue","slider_rps","chk_dlq","guard_webhook"]},
                    {"id":"txt_header","component":"Text","text":"🚨 [INC-8977] 웹훅 큐 52,400건 적체 (1/2단계)"},
                    {"id":"spark_queue","component":"MetricSparkline","metricName":"Webhook Queue","currentValue":"52,400 msg","severity":"critical"},
                    {"id":"slider_rps","component":"Slider","label":"최대 발송 속도 (RPS)","min":100,"max":1000,"value":$rpsExpr},
                    {"id":"chk_dlq","component":"CheckBox","label":"데드레터 큐(DLQ) 1,280건 재처리 포함","value":$dlqExpr},
                    {"id":"guard_webhook","component":"ActionGuardContainer","child":"btn_webhook","guardedActionName":"drain_webhook_queue","guardTitle":"대량 웹훅 재발송 승인"},
                    {"id":"btn_webhook","component":"Button","variant":"primary","child":"btn_webhook_txt","action":{"event":{"name":"drain_webhook_queue","context":{"incidentId":{"path":"/ops/incidentId"},"rateLimitRps":$rpsExpr,"replayDlq":$dlqExpr}}}},
                    {"id":"btn_webhook_txt","component":"Text","text":"처리율 상향 및 재처리 →"}
                ]}}""".replace("\n", "")
                listOf(createMsg, dataMsg, compMsg)
            }
        }
    }

    /**
     * Stage 2: Triggered when a primary remediation action is executed.
     * Shows real-time metric recovery and interactive post-incident follow-up actions.
     */
    fun buildFollowUpVerificationJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val incidentId = ctx["incidentId"]?.toString() ?: "INC-8821"
        val actionDetail =
            when (event.type) {
                "failover_pg_traffic" -> "백업 PG 우회 가중치 ${ctx["backupWeight"] ?: 75}% 적용"
                "restart_worker_pods" -> "워커 파드 레플리카 ${ctx["targetReplicas"] ?: 8}개로 증설 완료"
                "production_rollback" -> "티켓 ${ctx["ticket"] ?: "HOTFIX-912"} 기준 ${ctx["targetVersion"] ?: "v4.18.1"} 롤백 완료"
                "throttle_db_pool" -> "커넥션 풀 상한 ${ctx["maxPoolSize"] ?: 30}개 제한 및 슬로우 쿼리 종료"
                else -> "웹훅 발송 속도 ${ctx["rateLimitRps"] ?: 450} RPS 상향 및 DLQ 재처리"
            }
        val agentReply =
            "[$incidentId] $actionDetail 명령이 프로덕션에 반영되어 지표가 정상화(p99 142ms)되었습니다. 사후 조치 옵션을 선택해 인시던트를 종료해 주세요."

        val updateData =
            """{"version":"v0.9.1","updateDataModel":{"surfaceId":"oncall_surface","path":"/opsNext","value":{"incidentId":"$incidentId","actionDetail":"$actionDetail","postAction":["auto_resolve_15m"],"notifySlack":true,"closingNote":"$actionDetail 후 지표 정상화 확인"}}}"""
        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
              {"id":"root","component":"Card","child":"ops_next_col"},
              {"id":"ops_next_col","component":"Column","children":["ops_next_title","ops_next_spark","ops_next_summary","ops_picker_post","ops_chk_slack","ops_tf_note","btn_ops_resolve","btn_ops_back"]},
              {"id":"ops_next_title","component":"Text","text":"🟢 [$incidentId] 1차 복구 반영 완료 · 사후 검증 (2/2단계)"},
              {"id":"ops_next_spark","component":"MetricSparkline","metricName":"복구 후 실시간 p99 Latency","currentValue":"142 ms (정상화)","severity":"normal","points":[0.95,0.78,0.45,0.22,0.14,0.12]},
              {"id":"ops_next_summary","component":"Text","text":"• 적용된 제어: $actionDetail"},
              {"id":"ops_picker_post","component":"ChoicePicker","label":"후속 조치 선택","variant":"mutuallyExclusive","options":[{"label":"15분간 정상 유지 시 인시던트 자동 종료(Resolved)","value":"auto_resolve_15m"},{"label":"포스트모템(사후 분석) 티켓 자동 생성","value":"create_postmortem"},{"label":"당직자 수동 모니터링 유지","value":"keep_monitoring"}],"value":{"path":"/opsNext/postAction"}},
              {"id":"ops_chk_slack","component":"CheckBox","label":"#incident-war-room 슬랙 채널에 복구 리포트 전송","value":{"path":"/opsNext/notifySlack"}},
              {"id":"ops_tf_note","component":"TextField","label":"종료 코멘트","value":{"path":"/opsNext/closingNote"}},
              {"id":"btn_ops_resolve","component":"Button","variant":"primary","child":"btn_ops_resolve_txt","action":{"event":{"name":"resolve_incident_followup","context":{"incidentId":{"path":"/opsNext/incidentId"},"actionDetail":{"path":"/opsNext/actionDetail"},"postAction":{"path":"/opsNext/postAction"},"notifySlack":{"path":"/opsNext/notifySlack"},"closingNote":{"path":"/opsNext/closingNote"}}}}},
              {"id":"btn_ops_resolve_txt","component":"Text","text":"인시던트 최종 종료(Resolved) 확정"},
              {"id":"btn_ops_back","component":"Button","variant":"borderless","child":"btn_ops_back_txt","action":{"event":{"name":"reset_incident_surface","context":{"reason":"reconfigure"}}}},
              {"id":"btn_ops_back_txt","component":"Text","text":"↩ 제어 파라미터 다시 조정하기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateData, updateComponents)
    }

    /**
     * Stage 3: Triggered when `resolve_incident_followup` is dispatched.
     */
    fun buildFinalResolvedJsonl(event: A2uiClientEventMessage): Pair<String, List<String>> {
        val ctx = event.context
        val incidentId = ctx["incidentId"]?.toString() ?: "INC-8821"
        val actionDetail = ctx["actionDetail"]?.toString() ?: "복구 제어 완료"
        val closingNote = ctx["closingNote"]?.toString() ?: "정상화 확인"
        val agentReply = "[$incidentId] 인시던트가 최종 종료(RESOLVED) 처리되었으며 온콜 로그에 기록되었습니다."

        val updateComponents =
            """
            {"version":"v0.9.1","updateComponents":{"surfaceId":"oncall_surface","components":[
              {"id":"root","component":"Card","child":"ops_done_col"},
              {"id":"ops_done_col","component":"Column","children":["ops_done_title","ops_done_spark","ops_done_txt1","ops_done_txt2","btn_ops_reset"]},
              {"id":"ops_done_title","component":"Text","text":"✅ [$incidentId] 인시던트 최종 종료 (RESOLVED)"},
              {"id":"ops_done_spark","component":"MetricSparkline","metricName":"서비스 가용성 (SLA)","currentValue":"99.99% Healthy","severity":"normal","points":[0.2,0.18,0.15,0.12,0.10,0.10]},
              {"id":"ops_done_txt1","component":"Text","text":"• 조치 내역: $actionDetail"},
              {"id":"ops_done_txt2","component":"Text","text":"• 종료 코멘트: $closingNote"},
              {"id":"btn_ops_reset","component":"Button","variant":"primary","child":"btn_ops_reset_txt","action":{"event":{"name":"reset_incident_surface","context":{"reason":"done"}}}},
              {"id":"btn_ops_reset_txt","component":"Text","text":"초기 제어판으로 돌아가기"}
            ]}}
            """.trimIndent().replace("\n", "")
        return agentReply to listOf(updateComponents)
    }
}
