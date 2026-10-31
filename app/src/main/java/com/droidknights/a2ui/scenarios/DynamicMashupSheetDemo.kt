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
import com.droidknights.a2ui.catalog.domains.DynamicMashupCatalog
import com.droidknights.a2ui.common.A2uiAgentChatScaffold
import com.droidknights.a2ui.scenarios.legacy.LegacyDynamicMashupScreen
import com.droidknights.a2ui.scenarios.payloads.DynamicMashupPayloads
import kotlinx.coroutines.launch

@Composable
fun DynamicMashupSheetDemo() {
    var selectedCase by rememberSaveable { mutableIntStateOf(0) }
    var resetGeneration by rememberSaveable { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember(context) { DynamicMashupCatalog.create(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val agentActionReplies = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedCase, resetGeneration) {
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
                        "execute_cross_feature_mashup",
                        "submit_dynamic_payload" -> {
                            outboundLog.add(msg)
                            val (reply, nextLines) =
                                DynamicMashupPayloads.buildFollowUpMashupStepJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(nextLines)
                            nextLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "confirm_mashup_followup" -> {
                            outboundLog.add(msg)
                            val (reply, doneLines) =
                                DynamicMashupPayloads.buildFinalMashupDoneJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(doneLines)
                            doneLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "reset_mashup_surface" -> {
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
        val lines = DynamicMashupPayloads.buildJsonl(selectedCase)
        jsonlLines.clear()
        jsonlLines.addAll(lines)
        lines.forEach { line -> processor.processInput(parser, line) }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    A2uiAgentChatScaffold(
        scenarioTitle = "동적 시트 & 문진",
        scenarioSubtitle = "복합 시트 제출 시 DataModel이 전송되고 부가 혜택·정산 링크 발송 등 다음 액션으로 이어집니다.",
        catalogBadge = "DynamicMashupCatalog",
        prompts = DynamicMashupPayloads.promptPresets,
        selectedPromptIndex = selectedCase,
        onSelectPrompt = { selectedCase = it },
        outboundLog = outboundLog,
        jsonlStream = jsonlLines,
        agentActionReplies = agentActionReplies,
        onClearOutbound = {
            resetGeneration += 1
        },
        legacyAsIsContent = {
            LegacyDynamicMashupScreen()
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

internal fun buildMashupJsonl(caseIndex: Int): List<String> =
    DynamicMashupPayloads.buildJsonl(caseIndex)
