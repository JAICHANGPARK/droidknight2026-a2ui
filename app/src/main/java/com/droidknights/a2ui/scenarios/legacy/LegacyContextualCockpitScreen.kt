package com.droidknights.a2ui.scenarios.legacy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class CockpitScreenNode(val title: String) {
    HOME_GRID("전체 기기 목록"),
    HVAC_DETAIL("실내 공조기"),
    BATTERY_DETAIL("배터리 방전 하한선"),
    V2L_DETAIL("V2L 220V 콘센트"),
    CAMERA_LOCK_DETAIL("카메라 & 도어락"),
    AUDIO_LIGHT_DETAIL("오디오 & 조명"),
}

@Composable
fun LegacyContextualCockpitScreen() {
    val navStack = remember { mutableStateListOf(CockpitScreenNode.HOME_GRID) }
    val currentScreen = navStack.last()
    var touchCount by remember { mutableIntStateOf(0) }

    var hvacTemp by remember { mutableFloatStateOf(18f) }
    var hvacConfigured by remember { mutableStateOf(false) }

    var batteryReserve by remember { mutableFloatStateOf(10f) }
    var batteryConfigured by remember { mutableStateOf(false) }

    var v2lEnabled by remember { mutableStateOf(false) }
    var parcelUnlocked by remember { mutableStateOf(false) }
    var rainSoundPlaying by remember { mutableStateOf(false) }

    val completedMissionCount =
        listOf(hvacConfigured, batteryConfigured, v2lEnabled, rainSoundPlaying).count { it }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LegacyNavigationHeader(
            missionTitle = "차박 설정 완료 현황: $completedMissionCount/4",
            painPointDesc = "",
            breadcrumb = currentScreen.title,
            touchCount = touchCount,
            canGoBack = navStack.size > 1,
            onBack = {
                if (navStack.size > 1) {
                    touchCount++
                    navStack.removeAt(navStack.lastIndex)
                }
            },
            onReset = {
                touchCount = 0
                hvacConfigured = false
                batteryConfigured = false
                v2lEnabled = false
                rainSoundPlaying = false
                navStack.clear()
                navStack.add(CockpitScreenNode.HOME_GRID)
            },
        )

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (currentScreen) {
                    CockpitScreenNode.HOME_GRID -> {
                        Text(
                            text = "스마트홈 · 차량 기기 목록",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LegacyDeviceMenuRow(
                            title = "🌡️ 실내 공조기",
                            status = if (hvacConfigured) "${hvacTemp.toInt()}°C 설정됨" else "미설정",
                            isDone = hvacConfigured,
                            onClick = {
                                touchCount++
                                navStack.add(CockpitScreenNode.HVAC_DETAIL)
                            },
                        )
                        LegacyDeviceMenuRow(
                            title = "🔋 배터리 방전 하한선",
                            status = if (batteryConfigured) "${batteryReserve.toInt()}% 설정됨" else "미설정",
                            isDone = batteryConfigured,
                            onClick = {
                                touchCount++
                                navStack.add(CockpitScreenNode.BATTERY_DETAIL)
                            },
                        )
                        LegacyDeviceMenuRow(
                            title = "🔌 V2L 220V 콘센트",
                            status = if (v2lEnabled) "ON" else "OFF",
                            isDone = v2lEnabled,
                            onClick = {
                                touchCount++
                                navStack.add(CockpitScreenNode.V2L_DETAIL)
                            },
                        )
                        LegacyDeviceMenuRow(
                            title = "📹 카메라 & 도어락",
                            status = if (parcelUnlocked) "잠금 해제됨" else "잠김",
                            isDone = parcelUnlocked,
                            onClick = {
                                touchCount++
                                navStack.add(CockpitScreenNode.CAMERA_LOCK_DETAIL)
                            },
                        )
                        LegacyDeviceMenuRow(
                            title = "🎵 사운드 & 무드 조명",
                            status = if (rainSoundPlaying) "빗소리 재생 중" else "정지",
                            isDone = rainSoundPlaying,
                            onClick = {
                                touchCount++
                                navStack.add(CockpitScreenNode.AUDIO_LIGHT_DETAIL)
                            },
                        )
                    }

                    CockpitScreenNode.HVAC_DETAIL -> {
                        Text(
                            text = "실내 공조기 설정",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "목표 온도: ${hvacTemp.toInt()}°C",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = hvacTemp,
                            onValueChange = {
                                touchCount++
                                hvacTemp = it
                                hvacConfigured = true
                            },
                            valueRange = 16f..30f,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                hvacConfigured = true
                                navStack.removeAt(navStack.lastIndex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("저장 후 목록으로")
                        }
                    }

                    CockpitScreenNode.BATTERY_DETAIL -> {
                        Text(
                            text = "배터리 방전 하한선",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "하한선: ${batteryReserve.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = batteryReserve,
                            onValueChange = {
                                touchCount++
                                batteryReserve = it
                                batteryConfigured = true
                            },
                            valueRange = 10f..50f,
                        )
                        Button(
                            onClick = {
                                touchCount++
                                batteryConfigured = true
                                navStack.removeAt(navStack.lastIndex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("저장 후 목록으로")
                        }
                    }

                    CockpitScreenNode.V2L_DETAIL -> {
                        Text(
                            text = "V2L 220V 콘센트 제어",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = v2lEnabled,
                                onCheckedChange = {
                                    touchCount++
                                    v2lEnabled = it
                                },
                            )
                            Text("220V V2L 콘센트 전원 공급", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = {
                                touchCount++
                                navStack.removeAt(navStack.lastIndex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("저장 후 목록으로")
                        }
                    }

                    CockpitScreenNode.CAMERA_LOCK_DETAIL -> {
                        Text(
                            text = "카메라 & 도어락 제어",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = parcelUnlocked,
                                onCheckedChange = {
                                    touchCount++
                                    parcelUnlocked = it
                                },
                            )
                            Text("현관 도어락 일회성 잠금 해제", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = {
                                touchCount++
                                navStack.removeAt(navStack.lastIndex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("저장 후 목록으로")
                        }
                    }

                    CockpitScreenNode.AUDIO_LIGHT_DETAIL -> {
                        Text(
                            text = "사운드 & 조명 제어",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rainSoundPlaying,
                                onCheckedChange = {
                                    touchCount++
                                    rainSoundPlaying = it
                                },
                            )
                            Text("빗소리 사운드 재생", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = {
                                touchCount++
                                navStack.removeAt(navStack.lastIndex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("저장 후 목록으로")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyDeviceMenuRow(
    title: String,
    status: String,
    isDone: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isDone) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
