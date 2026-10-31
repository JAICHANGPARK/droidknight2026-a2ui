package com.droidknights.a2ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.droidknights.a2ui.common.A2uiComparisonDisplayMode
import com.droidknights.a2ui.common.LocalBankingAppState
import com.droidknights.a2ui.common.LocalInitialComparisonMode
import com.droidknights.a2ui.common.rememberBankingAppState
import com.droidknights.a2ui.labs.CatalogEvolutionLabScreen
import com.droidknights.a2ui.labs.ChaosMonkeySelfHealingLabScreen
import com.droidknights.a2ui.labs.Spec10BridgeLabScreen
import com.droidknights.a2ui.labs.TestingShowcaseLabScreen
import com.droidknights.a2ui.navigation.A2uiTopLevelRoute
import com.droidknights.a2ui.navigation.FormFactorPreviews
import com.droidknights.a2ui.navigation.TwoPaneScene
import com.droidknights.a2ui.navigation.rememberA2uiWindowLayoutInfo
import com.droidknights.a2ui.navigation.rememberTopLevelBackStack
import com.droidknights.a2ui.navigation.rememberTwoPaneSceneStrategy
import com.droidknights.a2ui.scenarios.BankingHomeScreen
import com.droidknights.a2ui.scenarios.ContextualCockpitDemo
import com.droidknights.a2ui.scenarios.CurrencyExchangeDemo
import com.droidknights.a2ui.scenarios.DynamicMashupSheetDemo
import com.droidknights.a2ui.scenarios.FieldDiagnosticsWizardDemo
import com.droidknights.a2ui.scenarios.IncidentCommanderDemo
import com.droidknights.a2ui.theme.DroidKnightsA2uiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialRouteId = intent?.getStringExtra("route")
        val initialRoute =
            initialRouteId?.let { A2uiTopLevelRoute.fromRouteId(it) }
                ?: A2uiTopLevelRoute.BankingHome
        val initialMode =
            intent?.getStringExtra("mode")?.let { modeName ->
                A2uiComparisonDisplayMode.entries.firstOrNull { it.name == modeName }
            }
        setContent {
            DroidKnightsA2uiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(LocalInitialComparisonMode provides initialMode) {
                        DroidKnightsA2uiApp(startRoute = initialRoute)
                    }
                }
            }
        }
    }
}

@Composable
fun DroidKnightsA2uiApp(
    startRoute: A2uiTopLevelRoute = A2uiTopLevelRoute.BankingHome,
) {
    val bankingAppState = rememberBankingAppState()
    val topLevelBackStack = rememberTopLevelBackStack(startRoute)
    val twoPaneStrategy = rememberTwoPaneSceneStrategy<A2uiTopLevelRoute>()
    val windowLayoutInfo = rememberA2uiWindowLayoutInfo()
    val routes = A2uiTopLevelRoute.ALL_ROUTES
    val selectedRoute = topLevelBackStack.topLevelKey
    val selectedIndex = routes.indexOf(selectedRoute).coerceAtLeast(0)

    var selectedBankingPresetIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedBankingModeName by rememberSaveable { mutableStateOf<String?>(null) }
    var bankingNavigationGeneration by rememberSaveable { mutableIntStateOf(0) }
    val inheritedMode = LocalInitialComparisonMode.current
    val effectiveMode =
        selectedBankingModeName?.let { name ->
            A2uiComparisonDisplayMode.entries.firstOrNull { it.name == name }
        } ?: inheritedMode

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    ),
        ) {
            val isBankingFlow =
                selectedRoute == A2uiTopLevelRoute.BankingHome ||
                    selectedRoute == A2uiTopLevelRoute.CurrencyExchange

            if (!windowLayoutInfo.shouldUseNavigationRail && !isBankingFlow) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    edgePadding = 12.dp,
                ) {
                    routes.forEach { route ->
                        val isSelected = route == selectedRoute
                        Tab(
                            selected = isSelected,
                            onClick = { topLevelBackStack.selectTopLevel(route) },
                            text = {
                                Text(
                                    text = route.tabTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
                HorizontalDivider()
            }

            Row(modifier = Modifier.fillMaxSize()) {
                if (windowLayoutInfo.shouldUseNavigationRail && !isBankingFlow) {
                    NavigationRail(
                        modifier =
                            Modifier.fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                    ) {
                        routes.forEach { route ->
                            val isSelected = route == selectedRoute
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { topLevelBackStack.selectTopLevel(route) },
                                icon = {
                                    Text(
                                        text = route.emojiIcon,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                },
                                label = {
                                    Text(
                                        text = route.shortRailLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                    VerticalDivider()
                }

                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .imePadding(),
                ) {
                    CompositionLocalProvider(
                        LocalInitialComparisonMode provides effectiveMode,
                        LocalBankingAppState provides bankingAppState,
                    ) {
                        NavDisplay(
                            backStack = topLevelBackStack.backStack,
                            onBack = { topLevelBackStack.pop() },
                            sceneStrategies = listOf(twoPaneStrategy),
                            entryProvider =
                                entryProvider {
                                    entry<A2uiTopLevelRoute.BankingHome> {
                                        BankingHomeScreen(
                                            onOpenAiAssistant = { presetIdx, mode ->
                                                selectedBankingPresetIndex = presetIdx
                                                selectedBankingModeName = mode.name
                                                bankingNavigationGeneration += 1
                                                topLevelBackStack.selectTopLevel(A2uiTopLevelRoute.CurrencyExchange)
                                            },
                                            onOpenAllLabsShowcase = {
                                                selectedBankingModeName = A2uiComparisonDisplayMode.TO_BE_AGENT_CHAT.name
                                                topLevelBackStack.selectTopLevel(A2uiTopLevelRoute.IncidentCommander)
                                            },
                                        )
                                    }
                                    entry<A2uiTopLevelRoute.CurrencyExchange>(
                                        metadata = TwoPaneScene.twoPane()
                                    ) {
                                        androidx.compose.runtime.key(bankingNavigationGeneration) {
                                            CurrencyExchangeDemo(
                                                initialPresetIndex = selectedBankingPresetIndex,
                                                onNavigateHome = {
                                                    topLevelBackStack.selectTopLevel(
                                                        A2uiTopLevelRoute.BankingHome
                                                    )
                                                },
                                            )
                                        }
                                    }
                                entry<A2uiTopLevelRoute.IncidentCommander>(
                                    metadata = TwoPaneScene.twoPane()
                                ) {
                                    IncidentCommanderDemo()
                                }
                                entry<A2uiTopLevelRoute.ContextualCockpit>(
                                    metadata = TwoPaneScene.twoPane()
                                ) {
                                    ContextualCockpitDemo()
                                }
                                entry<A2uiTopLevelRoute.DynamicMashup>(
                                    metadata = TwoPaneScene.twoPane()
                                ) {
                                    DynamicMashupSheetDemo()
                                }
                                entry<A2uiTopLevelRoute.FieldDiagnostics>(
                                    metadata = TwoPaneScene.twoPane()
                                ) {
                                    FieldDiagnosticsWizardDemo()
                                }
                                entry<A2uiTopLevelRoute.Spec10BridgeLab> {
                                    Column(
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                    ) {
                                        Spec10BridgeLabScreen()
                                    }
                                }
                                entry<A2uiTopLevelRoute.CatalogEvolutionLab> {
                                    Column(
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                    ) {
                                        CatalogEvolutionLabScreen()
                                    }
                                }
                                entry<A2uiTopLevelRoute.ChaosMonkeyLab> {
                                    Column(
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                    ) {
                                        ChaosMonkeySelfHealingLabScreen()
                                    }
                                }
                                entry<A2uiTopLevelRoute.TestingShowcaseLab> {
                                    Column(
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(16.dp),
                                    ) {
                                        TestingShowcaseLabScreen()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@FormFactorPreviews
@Composable
private fun DroidKnightsA2uiAppAdaptivePreview() {
    DroidKnightsA2uiTheme {
        DroidKnightsA2uiApp()
    }
}
