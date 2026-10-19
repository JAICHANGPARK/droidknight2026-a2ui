package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.domains.FieldDiagnosticsCatalog
import com.droidknights.a2ui.common.A2uiAgentChatScaffold
import com.droidknights.a2ui.scenarios.legacy.LegacyFieldDiagnosticsScreen
import com.droidknights.a2ui.scenarios.payloads.FieldDiagnosticsPayloads
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldDiagnosticsWizardDemo() {
    var wizardStep by rememberSaveable { mutableIntStateOf(1) }
    var simulateDelayedData by rememberSaveable { mutableStateOf(false) }
    var resetGeneration by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember(context) { FieldDiagnosticsCatalog.create(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val agentActionReplies = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(wizardStep, simulateDelayedData, resetGeneration) {
            A2uiMessageProcessor(catalogs = listOf(catalog))
        }

    LaunchedEffect(processor) {
        outboundLog.clear()
        agentActionReplies.clear()
        launch { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect { msg ->
                if (msg is A2uiClientEventMessage) {
                    when (msg.type) {
                        "advance_wizard_step" -> {
                            val completedStep = (msg.context["completedStep"] as? Number)?.toInt() ?: wizardStep
                            val testResult =
                                (msg.context["testResult"] as? List<*>)?.firstOrNull()?.toString()
                            if (completedStep == 2 && testResult == "resolved") {
                                // Self-resolved at Step 2 -> show maintenance reminder follow-up surface
                                outboundLog.add(msg)
                                val (reply, nextLines) =
                                    FieldDiagnosticsPayloads.buildFollowUpDiagnosticsStepJsonl(msg)
                                agentActionReplies.add(reply)
                                jsonlLines.addAll(nextLines)
                                nextLines.forEach { line -> processor.processInput(parser, line) }
                            } else if (wizardStep < 3) {
                                wizardStep += 1
                            }
                        }
                        "confirm_repair_dispatch",
                        "run_unbalance_recovery",
                        "run_door_latch_self_test" -> {
                            outboundLog.add(msg)
                            val (reply, nextLines) =
                                FieldDiagnosticsPayloads.buildFollowUpDiagnosticsStepJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(nextLines)
                            nextLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "finalize_diagnostics_followup" -> {
                            outboundLog.add(msg)
                            val (reply, doneLines) =
                                FieldDiagnosticsPayloads.buildFinalDiagnosticsDoneJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(doneLines)
                            doneLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "reset_diag_surface" -> {
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

        val (initialLines, delayedLines) =
            FieldDiagnosticsPayloads.buildJsonl(wizardStep, simulateDelayedData)
        jsonlLines.clear()
        jsonlLines.addAll(initialLines)
        initialLines.forEach { line -> processor.processInput(parser, line) }

        if (delayedLines.isNotEmpty()) {
            delay(1800)
            delayedLines.forEach { delayedLine ->
                jsonlLines.add(delayedLine)
                processor.processInput(parser, delayedLine)
            }
        }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    A2uiAgentChatScaffold(
        scenarioTitle = "가전 자가진단",
        scenarioSubtitle = "진단 결과 DataModel 전송에 따라 다음 점검 단계, 기사 배정 안내, 후속 알림 설정까지 이어집니다.",
        catalogBadge = "FieldDiagnosticsCatalog",
        prompts = FieldDiagnosticsPayloads.promptPresets,
        selectedPromptIndex = (wizardStep - 1).coerceIn(0, 4),
        onSelectPrompt = { wizardStep = it + 1 },
        outboundLog = outboundLog,
        jsonlStream = jsonlLines,
        agentActionReplies = agentActionReplies,
        onClearOutbound = {
            resetGeneration += 1
        },
        extraHeaderControls = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = {
                        scope.launch {
                            simulateDelayedData = !simulateDelayedData
                            wizardStep = 1
                        }
                    },
                    label = {
                        Text(if (simulateDelayedData) "즉시 로딩" else "지연 스트리밍 재현")
                    },
                )
                AssistChip(
                    onClick = {
                        scope.launch {
                            val nextIdx = jsonlLines.count { it.contains("/diag/logs/-") } + 1
                            val appendMsg = FieldDiagnosticsPayloads.buildAppendLogJsonl(nextIdx)
                            jsonlLines.add(appendMsg)
                            processor.processInput(parser, appendMsg)
                        }
                    },
                    label = { Text("로그 추가") },
                )
            }
        },
        legacyAsIsContent = {
            LegacyFieldDiagnosticsScreen()
        },
    ) {
        activeSurfaces.forEach { surface ->
            A2uiSurface(
                surfaceModel = surface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal fun buildDiagnosticsWizardJsonl(
    step: Int,
    delayHotspotData: Boolean,
): Pair<List<String>, List<String>> =
    FieldDiagnosticsPayloads.buildJsonl(step, delayHotspotData)
