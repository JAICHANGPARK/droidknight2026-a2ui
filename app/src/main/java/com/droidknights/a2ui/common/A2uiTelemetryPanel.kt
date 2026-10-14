package com.droidknights.a2ui.common

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Shared inspector panel displaying `processor.outboundEvents` (`A2uiClientEventMessage` with
 * variable `clientDataModel` snapshots and `A2uiClientErrorMessage` with JSON Pointer `path`s)
 * alongside the raw JSONL messages processed by the engine.
 */
@Composable
fun A2uiTelemetryPanel(
    outboundLog: List<A2uiClientToServerMessage>,
    jsonlStream: List<String>,
    onClearOutbound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showJsonl by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF334155)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📡 processor.outboundEvents (Client → Agent 역방향 스트림)",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "sendDataModel=true 시 에이전트가 동적으로 정의한 가변 DataModel 필드가 함께 직렬화됩니다.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { showJsonl = !showJsonl }) {
                        Text(
                            text = if (showJsonl) "JSONL 숨기기" else "JSONL 원문(${jsonlStream.size})",
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                    }
                    if (outboundLog.isNotEmpty()) {
                        OutlinedButton(onClick = onClearOutbound) {
                            Text("비우기", color = Color(0xFFF87171), fontSize = 11.sp)
                        }
                    }
                }
            }

            if (outboundLog.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "아직 발생한 이벤트/에러가 없습니다. 위 A2uiSurface의 컴포넌트를 조작하거나 버튼을 눌러보세요.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                outboundLog.takeLast(5).reversed().forEach { msg ->
                    when (msg) {
                        is A2uiClientEventMessage -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF064E3B).copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text =
                                            "✅ A2uiClientEventMessage · type=\"${msg.type}\" · component=\"${msg.componentId}\"",
                                        color = Color(0xFF6EE7B7),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Text(
                                        text = "context = ${msg.context}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    if (msg.clientDataModel != null) {
                                        Text(
                                            text = "clientDataModel = ${msg.clientDataModel?.surfaces}",
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        }
                        is A2uiClientErrorMessage -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF7F1D1D).copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, Color(0xFFF87171)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text =
                                            "🚨 A2uiClientErrorMessage · code=\"${msg.code}\" · surface=\"${msg.surfaceId}\"",
                                        color = Color(0xFFFCA5A5),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Text(
                                        text = "message = ${msg.message}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                    Text(
                                        text = "context = ${msg.context}",
                                        color = Color(0xFFFECACA),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showJsonl) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF020617),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Inbound JSONL Messages (Agent → A2uiMessageProcessor.processInput)",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        jsonlStream.forEachIndexed { idx, line ->
                            Text(
                                text = "[#${idx + 1}] $line",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}
