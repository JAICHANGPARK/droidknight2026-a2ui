package com.droidknights.a2ui.scenarios

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.droidknights.a2ui.catalog.domains.ContextualCockpitCatalog
import com.droidknights.a2ui.common.A2uiAgentChatScaffold
import com.droidknights.a2ui.scenarios.legacy.LegacyContextualCockpitScreen
import com.droidknights.a2ui.scenarios.payloads.ContextualCockpitPayloads
import kotlinx.coroutines.launch

@Composable
fun ContextualCockpitDemo() {
    var selectedRoutine by rememberSaveable { mutableIntStateOf(0) }
    var resetGeneration by rememberSaveable { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember(context) { ContextualCockpitCatalog.create(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val agentActionReplies = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedRoutine, resetGeneration) {
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
                        "apply_nursery_scene",
                        "apply_ev_camping_mode",
                        "unlock_front_door",
                        "apply_cinema_mode",
                        "dispense_pet_care" -> {
                            outboundLog.add(msg)
                            val (reply, nextLines) =
                                ContextualCockpitPayloads.buildFollowUpAutomationJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(nextLines)
                            nextLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "confirm_cockpit_automation" -> {
                            outboundLog.add(msg)
                            val (reply, doneLines) =
                                ContextualCockpitPayloads.buildFinalCockpitDoneJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(doneLines)
                            doneLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "reset_cockpit_surface" -> {
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
        kotlinx.coroutines.delay(50)
        val lines = ContextualCockpitPayloads.buildJsonl(selectedRoutine)
        jsonlLines.clear()
        jsonlLines.addAll(lines)
        lines.forEach { line -> processor.processInput(parser, line) }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    A2uiAgentChatScaffold(
        scenarioTitle = "스마트홈 · SDV 콕핏",
        scenarioSubtitle = "기기 제어값 전송 후 유지 시간 타이머 및 맞춤 루틴 등록 액션까지 이어집니다.",
        catalogBadge = "ContextualCockpitCatalog",
        prompts = ContextualCockpitPayloads.promptPresets,
        selectedPromptIndex = selectedRoutine,
        onSelectPrompt = { selectedRoutine = it },
        outboundLog = outboundLog,
        jsonlStream = jsonlLines,
        agentActionReplies = agentActionReplies,
        onClearOutbound = {
            resetGeneration += 1
        },
        legacyAsIsContent = {
            LegacyContextualCockpitScreen()
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

internal fun buildCockpitJsonl(routineIndex: Int): List<String> =
    ContextualCockpitPayloads.buildJsonl(routineIndex)
