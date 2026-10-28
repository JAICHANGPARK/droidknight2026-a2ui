package com.droidknights.a2ui.scenarios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.BuildConfig
import com.droidknights.a2ui.common.A2uiComparisonDisplayMode
import com.droidknights.a2ui.common.A2uiGeminiApiClient
import com.droidknights.a2ui.common.GeminiApiSettingsDialog

/**
 * Clean, real-app style Korean mobile banking home screen (Toss / Shinhan SOL style)
 * with a bottom-right Floating Action Button (`🤖 AI 챗봇`).
 */
@Composable
fun BankingHomeScreen(
    onOpenAiAssistant: (presetIndex: Int, mode: A2uiComparisonDisplayMode) -> Unit,
    onOpenAllLabsShowcase: () -> Unit,
) {
    val context = LocalContext.current
    var hideBalance by rememberSaveable { mutableStateOf(false) }

    var showGeminiSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var geminiApiKey by remember { mutableStateOf(A2uiGeminiApiClient.getApiKey(context)) }
    var geminiModel by remember { mutableStateOf(A2uiGeminiApiClient.getModelName(context)) }
    var isGeminiEnabled by remember {
        mutableStateOf(A2uiGeminiApiClient.isGeminiApiEnabled(context))
    }

    if (showGeminiSettingsDialog) {
        GeminiApiSettingsDialog(
            initialApiKey = geminiApiKey,
            initialModel = geminiModel,
            initialEnabled = isGeminiEnabled,
            hasBuildConfigKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
            onDismiss = { showGeminiSettingsDialog = false },
            onSave = { newKey, newModel, newEnabled ->
                A2uiGeminiApiClient.saveApiKey(context, newKey)
                A2uiGeminiApiClient.saveModelName(context, newModel)
                A2uiGeminiApiClient.setGeminiApiEnabled(context, newEnabled)
                geminiApiKey = A2uiGeminiApiClient.getApiKey(context)
                geminiModel = A2uiGeminiApiClient.getModelName(context)
                isGeminiEnabled = A2uiGeminiApiClient.isGeminiApiEnabled(context)
                showGeminiSettingsDialog = false
            },
        )
    }

    val bgLight = Color(0xFFF4F6FA)
    val cardWhite = Color(0xFFFFFFFF)
    val primaryBlue = Color(0xFF1B64DA)
    val accentYellow = Color(0xFFFFCC00)
    val darkNavy = Color(0xFF191F28)
    val mutedGray = Color(0xFF6B7684)

    Scaffold(
        containerColor = bgLight,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onOpenAiAssistant(0, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                },
                containerColor = primaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(text = "🤖", fontSize = 20.sp)
                    Text(
                        text = "AI 챗봇",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Minimal Top Bar: Name | Subtle Settings Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "박제창님",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = darkNavy,
                    )
                    Surface(
                        color = primaryBlue.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "VVIP",
                            color = primaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = Color(0xFFE5E8EB),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp).clickable { showGeminiSettingsDialog = true },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "⚙️", fontSize = 16.sp)
                        }
                    }
                }
            }

            // 2. Main Account Card (입출금 통장 + 송금 / 환전 + 최근 이체)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Account Title Row
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    onOpenAiAssistant(7, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                                },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(36.dp)
                                        .clip(CircleShape)
                                        .background(primaryBlue),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "DK",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                )
                            }
                            Column {
                                Text(
                                    text = "DK 주거래 통장",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkNavy,
                                )
                                Text(
                                    text = "110-482-910284",
                                    fontSize = 12.sp,
                                    color = mutedGray,
                                )
                            }
                        }
                        Text(
                            text = "›",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = mutedGray,
                        )
                    }

                    // Balance Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = if (hideBalance) "잔액 숨김" else "31,520,000원",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = darkNavy,
                        )
                        Surface(
                            color = Color(0xFFF2F4F6),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable { hideBalance = !hideBalance },
                        ) {
                            Text(
                                text = if (hideBalance) "보기" else "숨김",
                                fontSize = 12.sp,
                                color = mutedGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // 2 Primary Buttons: 송금 | 환전
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                onOpenAiAssistant(1, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = accentYellow,
                                    contentColor = darkNavy,
                                ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text(
                                text = "송금",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }

                        Button(
                            onClick = {
                                onOpenAiAssistant(0, A2uiComparisonDisplayMode.AS_IS_LEGACY_APP)
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8F3FF),
                                    contentColor = primaryBlue,
                                ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text(
                                text = "환전",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                    }

                    // Compact 1-line Recent Transfer Pills
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val recentTransfers =
                            listOf(
                                "박우리 24만" to Color(0xFF1B64DA),
                                "김신한 12만" to Color(0xFFF59E0B),
                                "이아름 8만" to Color(0xFF10B981),
                            )
                        recentTransfers.forEach { (label, dotColor) ->
                            Surface(
                                color = Color(0xFFF2F4F6),
                                shape = RoundedCornerShape(20.dp),
                                modifier =
                                    Modifier.clickable {
                                        onOpenAiAssistant(1, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor),
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = darkNavy,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Compact Card Statement Card (11월 명세서 · 즉시결제)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable {
                            onOpenAiAssistant(2, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                        },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "11월 카드 명세서",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mutedGray,
                        )
                        Text(
                            text = "2,161,314원",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = darkNavy,
                        )
                    }
                    Button(
                        onClick = {
                            onOpenAiAssistant(2, A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT)
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF2F4F6),
                                contentColor = darkNavy,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "즉시결제",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // 4. Clean 4-Item Menu Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val quickMenus =
                        listOf(
                            Triple("💴", "환전", 0 to A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT),
                            Triple("🧾", "입출금", 7 to A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT),
                            Triple("✈️", "해외여행", 3 to A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT),
                            Triple("💸", "송금", 1 to A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT),
                        )
                    quickMenus.forEach { (emoji, label, target) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier =
                                Modifier.clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onOpenAiAssistant(target.first, target.second)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF2F4F6)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = darkNavy,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
