package com.droidknights.a2ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidknights.a2ui.labs.CatalogEvolutionLabScreen
import com.droidknights.a2ui.labs.ChaosMonkeySelfHealingLabScreen
import com.droidknights.a2ui.labs.Spec10BridgeLabScreen
import com.droidknights.a2ui.labs.TestingShowcaseLabScreen
import com.droidknights.a2ui.scenarios.ContextualCockpitDemo
import com.droidknights.a2ui.scenarios.DynamicMashupSheetDemo
import com.droidknights.a2ui.scenarios.FieldDiagnosticsWizardDemo
import com.droidknights.a2ui.scenarios.IncidentCommanderDemo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF2563EB),
                        onPrimary = Color.White,
                        secondary = Color(0xFF0EA5E9),
                        surface = Color(0xFFF8FAFC),
                        background = Color(0xFF0B1120),
                    )
            ) {
                DroidKnightsA2uiApp()
            }
        }
    }
}

@Composable
fun DroidKnightsA2uiApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs =
        listOf(
            "🚨 A. 온콜 커맨더 (Guardrail/RO)",
            "🚗 B. 스마트홈·SDV 콕핏",
            "🧾 C. 차등 정산·가변 클레임",
            "🔧 D. 가전 진단 위저드 (isReady)",
            "🚀 Lab 0. Spec 1.0 차세대 브릿지",
            "🎨 Lab 1. 3-Way 카탈로그·협상",
            "🐒 Lab 2. 카오스 몽키·자가치유",
            "🧪 Lab 5. 결정론적 UI 테스트",
        )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0B1120),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF0B1120)),
        ) {
            // Top Header
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "DroidKnights 2026 · Official AndroidX A2UI",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                        )
                        Surface(
                            color = Color(0xFF1D4ED8).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        ) {
                            Text(
                                text = "Maven 1.0.0-SNAPSHOT",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Text(
                        text =
                            "androidx.a2ui:* + androidx.compose.material3:material3-a2ui 공식 아티팩트 기반 4대 비여행 도메인 & 5대 공식 기능 쇼케이스",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                    )
                }
            }

            // Scrollable Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF38BDF8),
                edgePadding = 12.dp,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight =
                                    if (selectedTab == index) FontWeight.ExtraBold else FontWeight.Medium,
                                color =
                                    if (selectedTab == index) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            )
                        },
                    )
                }
            }

            // Content Area
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
            ) {
                when (selectedTab) {
                    0 -> IncidentCommanderDemo()
                    1 -> ContextualCockpitDemo()
                    2 -> DynamicMashupSheetDemo()
                    3 -> FieldDiagnosticsWizardDemo()
                    4 -> Spec10BridgeLabScreen()
                    5 -> CatalogEvolutionLabScreen()
                    6 -> ChaosMonkeySelfHealingLabScreen()
                    else -> TestingShowcaseLabScreen()
                }
            }
        }
    }
}
