package com.droidknights.a2ui.common

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    TO_BE_AGENT_CHAT("AI 챗봇 (A2UI)"),
    AS_IS_LEGACY_APP("기존 앱 (As-Is)"),
    ADAPTIVE_TWO_PANE_SPLIT("2-Pane 비교"),
}

val LocalInitialComparisonMode = compositionLocalOf<A2uiComparisonDisplayMode?> { null }

/**
 * Clean Material 3 chat bubble matching `ChatBubble.kt` in `droidkaigi2026-a2ui-private/a2ui-compose-labs`.
 */
@Composable
internal fun ChatBubble(
    text: String,
    fromUser: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color =
                if (fromUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Renders the resolved DataModel payload transmitted when the user triggers an A2UI Button action,
 * followed by the Agent's next-step response.
 */
@Composable
internal fun DispatchedDataModelEventCard(
    event: A2uiClientEventMessage,
    stepIndex: Int,
    priorEvents: List<A2uiClientEventMessage> = emptyList(),
    agentReply: String?,
) {
    val entries = flattenContextMap(event.context)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (priorEvents.isNotEmpty()) {
            val historySummary =
                priorEvents.mapIndexed { idx, msg -> "#${idx + 1} ${msg.type}" }.joinToString(" → ")
            Text(
                text = "✓ 이전 액션 완료: $historySummary",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "📤 DataModel 전송됨 (#$stepIndex · ${event.type})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
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
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
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
        if (!agentReply.isNullOrBlank()) {
            ChatBubble(text = agentReply, fromUser = false)
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
    val displayMode =
        userOverriddenModeName?.let { name ->
            A2uiComparisonDisplayMode.entries.firstOrNull { it.name == name }
        }
            ?: if (windowLayoutInfo.isMediumOrWider && legacyAsIsContent != null) {
                A2uiComparisonDisplayMode.ADAPTIVE_TWO_PANE_SPLIT
            } else {
                A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT
            }

    var customInputText by rememberSaveable(selectedPromptIndex) {
        mutableStateOf("")
    }
    var displayedUserMessage by rememberSaveable(selectedPromptIndex) {
        mutableStateOf(activePreset.userPromptText)
    }

    val showToBePromptChips = displayMode != A2uiComparisonDisplayMode.AS_IS_LEGACY_APP

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = scenarioTitle,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = scenarioSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (legacyAsIsContent != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                A2uiComparisonDisplayMode.entries.forEach { mode ->
                    FilterChip(
                        selected = displayMode == mode,
                        onClick = { userOverriddenModeName = mode.name },
                        label = { Text(mode.label) },
                    )
                }
            }
        }

        if (showToBePromptChips) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                prompts.forEachIndexed { index, preset ->
                    FilterChip(
                        selected = selectedPromptIndex == index,
                        onClick = { onSelectPrompt(index) },
                        label = { Text(preset.shortChipLabel) },
                    )
                }
            }
            extraHeaderControls?.invoke()
        }

        val renderToBeChatPane: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChatBubble(text = displayedUserMessage, fromUser = true)
                ChatBubble(text = activePreset.agentIntroText, fromUser = false)

                val eventMessages = outboundLog.filterIsInstance<A2uiClientEventMessage>()
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
                        text = "${msg.code}: ${msg.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // Live A2uiSurface rendered directly in the Material 3 theme
                // Updates in-place as follow-up updateDataModel & updateComponents arrive!
                surfaceContent()

                extraTimelineBanners?.invoke()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(activePreset.userPromptText, maxLines = 1) },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val prompt = customInputText.trim().ifBlank { activePreset.userPromptText }
                            displayedUserMessage = prompt
                            customInputText = ""
                            val matchedIdx = matchOfflinePromptIndex(prompt, prompts)
                            if (matchedIdx != null && matchedIdx != selectedPromptIndex) {
                                onSelectPrompt(matchedIdx)
                            }
                        },
                    ) {
                        Text("전송")
                    }
                }

                A2uiTelemetryPanel(
                    outboundLog = outboundLog,
                    jsonlStream = jsonlStream,
                    onClearOutbound = onClearOutbound,
                )
            }
        }

        when {
            displayMode == A2uiComparisonDisplayMode.AS_IS_LEGACY_APP && legacyAsIsContent != null -> {
                legacyAsIsContent()
            }
            displayMode == A2uiComparisonDisplayMode.ADAPTIVE_TWO_PANE_SPLIT && legacyAsIsContent != null -> {
                if (windowLayoutInfo.isMediumOrWider) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(windowLayoutInfo.recommendedPaneSpacing),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            legacyAsIsContent()
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            renderToBeChatPane()
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        legacyAsIsContent()
                        renderToBeChatPane()
                    }
                }
            }
            else -> {
                renderToBeChatPane()
            }
        }
    }
}

private fun matchOfflinePromptIndex(
    inputText: String,
    prompts: List<AgentPromptPreset>,
): Int? {
    val normalized = inputText.trim().lowercase()
    if (normalized.isEmpty()) return null

    val exactIdx = prompts.indexOfFirst { it.userPromptText.lowercase() == normalized }
    if (exactIdx >= 0) return exactIdx

    val tokens =
        normalized
            .split(Regex("[\\s,./·?!]+"))
            .filter { it.length >= 2 }
    if (tokens.isEmpty()) return null

    var bestIdx: Int? = null
    var bestScore = 0
    prompts.forEachIndexed { idx, preset ->
        val corpus = "${preset.shortChipLabel} ${preset.userPromptText}".lowercase()
        val score = tokens.count { token -> corpus.contains(token) }
        if (score > bestScore) {
            bestScore = score
            bestIdx = idx
        }
    }
    return bestIdx
}
