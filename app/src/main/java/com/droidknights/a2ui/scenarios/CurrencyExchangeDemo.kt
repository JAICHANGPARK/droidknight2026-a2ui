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
import com.droidknights.a2ui.catalog.domains.CurrencyExchangeCatalog
import com.droidknights.a2ui.common.A2uiAgentChatScaffold
import com.droidknights.a2ui.scenarios.legacy.LegacyCurrencyExchangeScreen
import com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
import kotlinx.coroutines.launch

@Composable
fun CurrencyExchangeDemo() {
    var selectedPrompt by rememberSaveable { mutableIntStateOf(0) }
    var resetGeneration by rememberSaveable { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val catalog = remember(context) { CurrencyExchangeCatalog.create(context = context) }
    val parser = remember { A2uiMessageParser() }
    val outboundLog = remember { mutableStateListOf<A2uiClientToServerMessage>() }
    val agentActionReplies = remember { mutableStateListOf<String>() }
    val jsonlLines = remember { mutableStateListOf<String>() }

    val processor =
        remember(selectedPrompt, resetGeneration) {
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
                        "submit_exchange" -> {
                            outboundLog.add(msg)
                            val (reply, nextLines) =
                                CurrencyExchangePayloads.buildFollowUpVoucherStepJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(nextLines)
                            nextLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "issue_exchange_voucher" -> {
                            outboundLog.add(msg)
                            val (reply, doneLines) =
                                CurrencyExchangePayloads.buildFinalVoucherIssuedJsonl(msg)
                            agentActionReplies.add(reply)
                            jsonlLines.addAll(doneLines)
                            doneLines.forEach { line -> processor.processInput(parser, line) }
                        }
                        "reset_exchange_surface" -> {
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
        val lines = CurrencyExchangePayloads.buildJsonl(selectedPrompt)
        jsonlLines.clear()
        jsonlLines.addAll(lines)
        lines.forEach { line -> processor.processInput(parser, line) }
    }

    val activeSurfaces by processor.activeSurfaces.collectAsState()

    A2uiAgentChatScaffold(
        scenarioTitle = "외화 환전",
        scenarioSubtitle = "기존 7단계 환전 화면 이동과 A2UI 양방향 DataModel + 후속 바우처 발급 루프를 비교합니다.",
        catalogBadge = "CurrencyExchangeCatalog",
        prompts = CurrencyExchangePayloads.promptPresets,
        selectedPromptIndex = selectedPrompt,
        onSelectPrompt = { selectedPrompt = it },
        outboundLog = outboundLog,
        jsonlStream = jsonlLines,
        agentActionReplies = agentActionReplies,
        onClearOutbound = {
            resetGeneration += 1
        },
        legacyAsIsContent = {
            LegacyCurrencyExchangeScreen()
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

internal fun buildCurrencyExchangeJsonl(caseIndex: Int): List<String> =
    CurrencyExchangePayloads.buildJsonl(caseIndex)
