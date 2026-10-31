package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiUserAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.domains.IncidentCommanderCatalog
import com.droidknights.a2ui.common.A2uiAgentChatScaffold
import com.droidknights.a2ui.scenarios.legacy.LegacyIncidentCommanderScreen
import com.droidknights.a2ui.scenarios.payloads.IncidentCommanderPayloads
import kotlinx.coroutines.launch

@Composable
fun IncidentCommanderDemo() {
    var selectedIncident by rememberSaveable { mutableIntStateOf(0) }
    var isObserverReadOnly by rememberSaveable { mutableStateOf(false) }
    var resetGeneration by rememberSaveable { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember(context) { IncidentCommanderCatalog.create(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val agentActionReplies = remember { mutableStateListOf<String>() }
    val processorAuditLog = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedIncident, isObserverReadOnly, resetGeneration) {
            val globalAuditInterceptor =
                object : A2uiActionInterceptor {
                    override suspend fun onInterceptAction(action: A2uiUserAction): A2uiUserAction? {
                        return if (action is A2uiEventAction) {
                            val enrichedContext =
                                action.context.toMutableMap().apply {
                                    put("biometricAudit", "VERIFIED_BY_PROCESSOR_INTERCEPTOR")
                                }
                            processorAuditLog.add(
                                "🔐 인터셉터 감사 서명 완료: '${action.eventName}' (biometricAudit 주입)",
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
        outboundLog.clear()
        agentActionReplies.clear()
        processorAuditLog.clear()
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                if (msg is A2uiClientEventMessage) {
                    when (msg.type) {
                        "failover_pg_traffic",
                        "restart_worker_pods",
                        "production_rollback",
                        "throttle_db_pool",
                        "drain_webhook_queue" -> {
                            outboundLog.add(msg)
                            val (reply, nextLines) =
                                IncidentCommanderPayloads.buildFollowUpVerificationJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(nextLines)
                            nextLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "resolve_incident_followup" -> {
                            outboundLog.add(msg)
                            val (reply, doneLines) =
                                IncidentCommanderPayloads.buildFinalResolvedJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(doneLines)
                            doneLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "reset_incident_surface" -> {
                            resetGeneration += 1
                        }
                        else -> {
                            outboundLog.add(msg)
                        }
                    }
                } else {
                    outboundLog.add(msg)
                }
            }
        }
        val lines = IncidentCommanderPayloads.buildJsonl(selectedIncident, isObserverReadOnly)
        jsonlLines.clear()
        jsonlLines.addAll(lines)
        lines.forEach { line -> processor.processInput(parser, line) }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    A2uiAgentChatScaffold(
        scenarioTitle = "온콜 인시던트 제어",
        scenarioSubtitle = "장애 제어 파라미터 전송 후 실시간 지표 복구 확인 및 인시던트 종료까지 이어집니다.",
        catalogBadge = "IncidentCommanderCatalog",
        prompts = IncidentCommanderPayloads.promptPresets,
        selectedPromptIndex = selectedIncident,
        onSelectPrompt = { selectedIncident = it },
        outboundLog = outboundLog,
        jsonlStream = jsonlLines,
        agentActionReplies = agentActionReplies,
        onClearOutbound = {
            resetGeneration += 1
        },
        extraHeaderControls = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(
                    checked = isObserverReadOnly,
                    onCheckedChange = { isObserverReadOnly = it },
                )
                Text(
                    text = if (isObserverReadOnly) "관찰자 모드 (읽기 전용)" else "커맨더 모드 (제어 활성)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        extraTimelineBanners = {
            if (processorAuditLog.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        processorAuditLog.forEach { entry ->
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        },
        legacyAsIsContent = {
            LegacyIncidentCommanderScreen()
        },
    ) {
        activeSurfaces.forEach { surface ->
            androidx.compose.runtime.key(surface.id) {
                A2uiSurface(
                    surfaceModel = surface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

internal fun buildIncidentJsonl(incidentIndex: Int, readOnly: Boolean): List<String> =
    IncidentCommanderPayloads.buildJsonl(incidentIndex, readOnly)
