package com.droidknights.a2ui.common

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.navigation.rememberA2uiWindowLayoutInfo

data class AgentPromptPreset(
    val id: Int,
    val shortChipLabel: String,
    val userPromptText: String,
    val agentIntroText: String,
    val schemaBadge: String,
)

enum class A2uiComparisonDisplayMode(val label: String) {
    TO_BE_AGENT_CHAT("💬 AI 챗봇 (A2UI)"),
    AS_IS_LEGACY_APP("📱 기존 7단계 (As-Is)"),
    ADAPTIVE_TWO_PANE_SPLIT("⚖️ 2-Pane 비교"),
}

val LocalInitialComparisonMode = compositionLocalOf<A2uiComparisonDisplayMode?> { null }

/**
 * Messenger-style chat bubble with sender avatar & role label for stage demo clarity.
 */
@Composable
internal fun ChatBubble(
    text: String,
    fromUser: Boolean,
    senderLabel: String = if (fromUser) "나 (고객)" else "AI 에이전트",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!fromUser) {
                Box(
                    modifier =
                        Modifier.size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🤖",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Text(
                text = senderLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
        Surface(
            color =
                if (fromUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            contentColor =
                if (fromUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            shape =
                RoundedCornerShape(
                    topStart = if (fromUser) 16.dp else 4.dp,
                    topEnd = if (fromUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp,
                ),
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Renders a user action turn (outbound `A2uiClientEventMessage`) as a right-aligned user action card
 * in the chat timeline, followed by the Agent's left-aligned response bubble.
 */
@Composable
internal fun DispatchedDataModelEventCard(
    event: A2uiClientEventMessage,
    stepIndex: Int,
    priorEvents: List<A2uiClientEventMessage> = emptyList(),
    agentReply: String?,
) {
    val entries = flattenContextMap(event.context)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (priorEvents.isNotEmpty()) {
            val historySummary =
                priorEvents.mapIndexed { idx, msg -> "#${idx + 1} ${msg.type}" }.joinToString(" → ")
            Text(
                text = "✓ 이전 단계 완료: $historySummary",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End),
            )
        }

        // Right-aligned User Action Bubble showing transmitted DataModel context
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "나 (UI 액션 전송 #$stepIndex)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Card(
                modifier = Modifier.widthIn(max = 520.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                shape =
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "📤 ${event.type} (DataModel 바인딩 값 전송)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (entries.isNotEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                        )
                        entries.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "$key:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color =
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                            alpha = 0.75f
                                        ),
                                )
                                Text(
                                    text = value,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!agentReply.isNullOrBlank()) {
            ChatBubble(
                text = agentReply,
                fromUser = false,
                senderLabel = "AI 에이전트 · 후속 단계 응답",
            )
        }
    }
}

private fun flattenContextMap(
    map: Map<*, *>?,
    prefix: String = "",
): List<Pair<String, String>> =
    map.orEmpty().flatMap { (k, v) ->
        val keyName = if (prefix.isEmpty()) k.toString() else "$prefix.$k"
        when (v) {
            is Map<*, *> -> flattenContextMap(v, keyName)
            is List<*> -> listOf(keyName to v.joinToString(", "))
            else -> listOf(keyName to (v?.toString() ?: "null"))
        }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun A2uiAgentChatScaffold(
    scenarioTitle: String,
    scenarioSubtitle: String,
    catalogBadge: String,
    prompts: List<AgentPromptPreset>,
    selectedPromptIndex: Int,
    onSelectPrompt: (Int) -> Unit,
    outboundLog: List<A2uiClientToServerMessage>,
    jsonlStream: List<String>,
    onClearOutbound: () -> Unit,
    agentActionReplies: List<String> = emptyList(),
    streamingStatusText: String? = null,
    isStreaming: Boolean = false,
    expectedJsonlCount: Int = 3,
    extraHeaderControls: (@Composable () -> Unit)? = null,
    extraTimelineBanners: (@Composable () -> Unit)? = null,
    legacyAsIsContent: (@Composable () -> Unit)? = null,
    surfaceContent: @Composable () -> Unit,
) {
    val activePreset = prompts.getOrElse(selectedPromptIndex) { prompts.first() }
    val windowLayoutInfo = rememberA2uiWindowLayoutInfo()
    val initialModeOverride = LocalInitialComparisonMode.current

    var userOverriddenModeName by rememberSaveable(initialModeOverride) {
        mutableStateOf(initialModeOverride?.name)
    }
    // Default to TO_BE_AGENT_CHAT so the app opens directly as a realistic AI Chat screen on stage
    val displayMode =
        userOverriddenModeName?.let { name ->
            A2uiComparisonDisplayMode.entries.firstOrNull { it.name == name }
        } ?: A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT

    var showTelemetryDrawer by rememberSaveable { mutableStateOf(false) }

    var customInputText by rememberSaveable(selectedPromptIndex) {
        mutableStateOf("")
    }
    var displayedUserMessage by rememberSaveable(selectedPromptIndex) {
        mutableStateOf(activePreset.userPromptText)
    }
    var chatTurnCounter by rememberSaveable { mutableIntStateOf(0) }

    val sendPromptMessage: (String, Int?) -> Unit = { promptText, targetIdx ->
        val finalPrompt = promptText.trim().ifBlank { activePreset.userPromptText }
        displayedUserMessage = finalPrompt
        customInputText = ""
        chatTurnCounter += 1
        val resolvedIdx = targetIdx ?: matchOfflinePromptIndex(finalPrompt, prompts)
        if (resolvedIdx != null && resolvedIdx != selectedPromptIndex) {
            onSelectPrompt(resolvedIdx)
        } else {
            // Re-trigger streaming on the current prompt
            onClearOutbound()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Top App Bar: Title + Mode Switcher (Chat vs As-Is 7-Step vs 2-Pane) + JSONL Telemetry Toggle
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier =
                                Modifier.size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "🤖", style = MaterialTheme.typography.titleMedium)
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "$scenarioTitle AI 비서",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = catalogBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                text = scenarioSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AssistChip(
                            onClick = { onClearOutbound() },
                            label = { Text("🔄 스트리밍 재실행", style = MaterialTheme.typography.labelSmall) },
                        )
                        FilterChip(
                            selected = showTelemetryDrawer,
                            onClick = { showTelemetryDrawer = !showTelemetryDrawer },
                            label = {
                                val eventCount = outboundLog.size
                                val badgeText =
                                    if (eventCount > 0) {
                                        "🔍 JSONL(${jsonlStream.size}) · 이벤트($eventCount)"
                                    } else {
                                        "🔍 JSONL 로그 (${jsonlStream.size})"
                                    }
                                Text(badgeText, style = MaterialTheme.typography.labelSmall)
                            },
                        )
                    }
                }

                // Mode Switcher Row + Optional Extra Header Controls
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (legacyAsIsContent != null) {
                        A2uiComparisonDisplayMode.entries.forEach { mode ->
                            FilterChip(
                                selected = displayMode == mode,
                                onClick = { userOverriddenModeName = mode.name },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                    extraHeaderControls?.invoke()
                }
            }
        }

        HorizontalDivider()

        // 2. Collapsible Top/Bottom Telemetry Drawer when toggled from Top Bar
        AnimatedVisibility(
            visible = showTelemetryDrawer,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
            ) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    A2uiTelemetryPanel(
                        outboundLog = outboundLog,
                        jsonlStream = jsonlStream,
                        onClearOutbound = onClearOutbound,
                    )
                }
            }
        }

        // 3. Main Content Area (Chat Timeline vs As-Is Legacy vs 2-Pane Split)
        val renderToBeChatPane: @Composable (Modifier) -> Unit = { paneModifier ->
            val chatScrollState = rememberScrollState()
            val eventMessages = outboundLog.filterIsInstance<A2uiClientEventMessage>()

            // Smoothly auto-scroll when user sends a message, JSONL streams in, or action is dispatched
            LaunchedEffect(
                selectedPromptIndex,
                chatTurnCounter,
                jsonlStream.size,
                eventMessages.size,
            ) {
                if (jsonlStream.size > 1 || eventMessages.isNotEmpty()) {
                    chatScrollState.animateScrollTo(chatScrollState.maxValue)
                }
            }

            Column(modifier = paneModifier.fillMaxSize()) {
                // Scrollable Chat Timeline
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(chatScrollState)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // System security notice pill at top of chat
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = "🔒 A2UI 선언형 프로토콜 연결됨 · 등록된 카탈로그 위젯만 렌더링됩니다",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Turn 1: User's natural language chat bubble
                    ChatBubble(
                        text = displayedUserMessage,
                        fromUser = true,
                    )

                    // Turn 2: Agent's response bubble + Progressive Streaming Status Bar
                    ChatBubble(
                        text = activePreset.agentIntroText,
                        fromUser = false,
                        senderLabel = "AI 에이전트 · ${activePreset.schemaBadge}",
                    )

                    // Progressive JSONL Streaming Status Banner
                    val currentStepCount = jsonlStream.size.coerceAtMost(expectedJsonlCount)
                    val statusLabel =
                        streamingStatusText
                            ?: when {
                                isStreaming || jsonlStream.size < expectedJsonlCount ->
                                    "📡 A2UI JSONL 스트리밍 중 ($currentStepCount/$expectedJsonlCount) · createSurface → updateComponents(Shimmer) → updateDataModel"
                                else ->
                                    "✅ A2UI 스트리밍 완료 (${jsonlStream.size}줄 수신) · 인라인 네이티브 Compose 서피스"
                            }

                    Surface(
                        color =
                            if (isStreaming) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color =
                                        if (isStreaming)
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (isStreaming) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    // Multi-turn Dispatched Action Cards & Agent Replies in the Chat Timeline
                    if (eventMessages.isNotEmpty()) {
                        val latestIdx = eventMessages.lastIndex
                        DispatchedDataModelEventCard(
                            event = eventMessages[latestIdx],
                            stepIndex = latestIdx + 1,
                            priorEvents = eventMessages.dropLast(1),
                            agentReply = agentActionReplies.getOrNull(latestIdx),
                        )
                    }

                    outboundLog.filterIsInstance<A2uiClientErrorMessage>().forEach { msg ->
                        Text(
                            text = "⚠️ ${msg.code}: ${msg.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    extraTimelineBanners?.invoke()

                    // Live inline A2uiSurface rendered inside the Agent's chat flow
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
                        surfaceContent()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 4. Bottom-Pinned Chat Input Composer + Quick Prompt Chips
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Quick Prompt Suggestion Chips Row (pinned right above the chat input box)
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "빠른 질문:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                            prompts.forEachIndexed { index, preset ->
                                val isSelected = selectedPromptIndex == index
                                SuggestionChip(
                                    onClick = {
                                        sendPromptMessage(preset.userPromptText, index)
                                    },
                                    label = {
                                        Text(
                                            text =
                                                if (isSelected) "💬 ${preset.shortChipLabel} ✓"
                                                else "💬 ${preset.shortChipLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight =
                                                if (isSelected) FontWeight.Bold
                                                else FontWeight.Normal,
                                        )
                                    },
                                )
                            }
                        }

                        // Messenger Input Row (TextField + Send Button)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = customInputText,
                                onValueChange = { customInputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = activePreset.userPromptText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions =
                                    KeyboardActions(
                                        onSend = {
                                            sendPromptMessage(customInputText, null)
                                        }
                                    ),
                                trailingIcon = {
                                    if (customInputText.isEmpty()) {
                                        TextButton(
                                            onClick = {
                                                customInputText = activePreset.userPromptText
                                            }
                                        ) {
                                            Text("예시 채우기", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                },
                            )
                            Button(
                                onClick = {
                                    sendPromptMessage(customInputText, null)
                                },
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text("전송 ↑", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                displayMode == A2uiComparisonDisplayMode.AS_IS_LEGACY_APP &&
                    legacyAsIsContent != null -> {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                    ) {
                        legacyAsIsContent()
                    }
                }
                displayMode == A2uiComparisonDisplayMode.ADAPTIVE_TWO_PANE_SPLIT &&
                    legacyAsIsContent != null -> {
                    if (windowLayoutInfo.isMediumOrWider) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier =
                                    Modifier.weight(1f)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                            ) {
                                legacyAsIsContent()
                            }
                            VerticalDivider()
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                renderToBeChatPane(Modifier.fillMaxSize())
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier =
                                    Modifier.weight(0.45f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp),
                            ) {
                                legacyAsIsContent()
                            }
                            HorizontalDivider()
                            Box(modifier = Modifier.weight(0.55f).fillMaxWidth()) {
                                renderToBeChatPane(Modifier.fillMaxSize())
                            }
                        }
                    }
                }
                else -> {
                    renderToBeChatPane(Modifier.fillMaxSize())
                }
            }
        }
    }
}

private fun matchOfflinePromptIndex(
    inputText: String,
    prompts: List<AgentPromptPreset>,
): Int? {
    val q = inputText.lowercase()
    prompts.forEachIndexed { index, preset ->
        if (
            q.contains(preset.shortChipLabel.lowercase()) ||
                preset.shortChipLabel.lowercase().split(" ").any { token ->
                    token.length >= 2 && q.contains(token)
                }
        ) {
            return index
        }
    }
    return when {
        q.contains("엔") || q.contains("jpy") || q.contains("인천공항") -> 0
        q.contains("달러") || q.contains("usd") || q.contains("소액권") -> 1
        q.contains("유로") || q.contains("프랑") || q.contains("eur") -> 2
        q.contains("예약") || q.contains("목표") || q.contains("자동") -> 3
        q.contains("재환전") || q.contains("현찰") || q.contains("남은") -> 4
        else -> null
    }
}
