package com.droidknights.a2ui.common

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun A2uiTelemetryPanel(
    outboundLog: List<A2uiClientToServerMessage>,
    jsonlStream: List<String>,
    onClearOutbound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWire by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (outboundLog.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "outboundEvents (${outboundLog.size})",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        TextButton(onClick = onClearOutbound) {
                            Text("지우기", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    outboundLog.takeLast(3).reversed().forEach { msg ->
                        when (msg) {
                            is A2uiClientEventMessage -> {
                                WireMonoLine("${msg.type}  ${msg.context}")
                            }
                            is A2uiClientErrorMessage -> {
                                Text(
                                    text = "${msg.code}  ${msg.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (jsonlStream.isNotEmpty()) {
            AssistChip(
                onClick = { showWire = !showWire },
                label = {
                    Text(
                        if (showWire) "JSONL 숨기기"
                        else "JSONL 보기 (${jsonlStream.size})"
                    )
                },
            )
            if (showWire) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        jsonlStream.forEach { line ->
                            WireMonoLine(line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WireMonoLine(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    )
}
