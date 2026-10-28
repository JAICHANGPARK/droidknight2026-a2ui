package com.droidknights.a2ui.navigation

import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import kotlinx.serialization.Serializable

/**
 * =========================================================================================
 * Jetpack Navigation 3 (`androidx.navigation3`) Top-Level Routes & Multi-Backstack State
 * =========================================================================================
 *
 * Implements the official `navigation-3` & `adaptive` skills:
 * 1. `@Serializable` `NavKey` routes for type-safe navigation & rotation/foldable state restoration
 * 2. `TopLevelBackStack` with `rememberSaveable` Saver so screen rotation & foldable fold/unfold
 *    preserve the active tab and backstack history
 * 3. `TwoPaneScene` & `TwoPaneSceneStrategy` + Foldable Hinge & Posture (`isTabletop`, `hingeList`)
 *    and Landscape Compact-Height (`< 480dp`) detection
 */
@Serializable
sealed interface A2uiTopLevelRoute : NavKey {
    val routeId: String
    val tabTitle: String
    val shortRailLabel: String
    val emojiIcon: String

    @Serializable
    data object BankingHome : A2uiTopLevelRoute {
        override val routeId = "home"
        override val tabTitle = "🏦 뱅킹 홈"
        override val shortRailLabel = "뱅킹홈"
        override val emojiIcon = "🏦"
    }

    @Serializable
    data object CurrencyExchange : A2uiTopLevelRoute {
        override val routeId = "exchange"
        override val tabTitle = "💴 외화 환전"
        override val shortRailLabel = "환전"
        override val emojiIcon = "💴"
    }

    @Serializable
    data object IncidentCommander : A2uiTopLevelRoute {
        override val routeId = "incident"
        override val tabTitle = "🚨 온콜 제어"
        override val shortRailLabel = "온콜"
        override val emojiIcon = "🚨"
    }

    @Serializable
    data object ContextualCockpit : A2uiTopLevelRoute {
        override val routeId = "cockpit"
        override val tabTitle = "🚗 스마트 콕핏"
        override val shortRailLabel = "콕핏"
        override val emojiIcon = "🚗"
    }

    @Serializable
    data object DynamicMashup : A2uiTopLevelRoute {
        override val routeId = "mashup"
        override val tabTitle = "🧾 동적 시트"
        override val shortRailLabel = "시트"
        override val emojiIcon = "🧾"
    }

    @Serializable
    data object FieldDiagnostics : A2uiTopLevelRoute {
        override val routeId = "diagnostics"
        override val tabTitle = "🔧 가전 진단"
        override val shortRailLabel = "진단"
        override val emojiIcon = "🔧"
    }

    @Serializable
    data object Spec10BridgeLab : A2uiTopLevelRoute {
        override val routeId = "lab0"
        override val tabTitle = "🚀 Spec 1.0"
        override val shortRailLabel = "v1.0"
        override val emojiIcon = "🚀"
    }

    @Serializable
    data object CatalogEvolutionLab : A2uiTopLevelRoute {
        override val routeId = "lab1"
        override val tabTitle = "🎨 카탈로그"
        override val shortRailLabel = "카탈로그"
        override val emojiIcon = "🎨"
    }

    @Serializable
    data object ChaosMonkeyLab : A2uiTopLevelRoute {
        override val routeId = "lab2"
        override val tabTitle = "🐒 자가 치유"
        override val shortRailLabel = "자가치유"
        override val emojiIcon = "🐒"
    }

    @Serializable
    data object TestingShowcaseLab : A2uiTopLevelRoute {
        override val routeId = "lab5"
        override val tabTitle = "🧪 UI 테스트"
        override val shortRailLabel = "테스트"
        override val emojiIcon = "🧪"
    }

    companion object {
        val ALL_ROUTES: List<A2uiTopLevelRoute> =
            listOf(
                BankingHome,
                CurrencyExchange,
                IncidentCommander,
                ContextualCockpit,
                DynamicMashup,
                FieldDiagnostics,
                Spec10BridgeLab,
                CatalogEvolutionLab,
                ChaosMonkeyLab,
                TestingShowcaseLab,
            )

        fun fromRouteId(id: String): A2uiTopLevelRoute =
            ALL_ROUTES.firstOrNull { it.routeId == id } ?: BankingHome
    }
}

/**
 * Manages per-tab back stacks and exposes a flattened `backStack` for `NavDisplay`.
 * Survives screen rotation and foldable fold/unfold configuration changes via [rememberTopLevelBackStack].
 */
class TopLevelBackStack<T : Any>(startKey: T) {
    private val topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> =
        linkedMapOf(startKey to mutableStateListOf(startKey))

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack: SnapshotStateList<T> = mutableStateListOf(startKey)

    private fun updateBackStack() {
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }
    }

    fun selectTopLevel(key: T) {
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            topLevelStacks.remove(key)?.let { existingStack ->
                topLevelStacks[key] = existingStack
            }
        }
        topLevelKey = key
        updateBackStack()
    }

    fun push(key: T) {
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        if (topLevelStacks[topLevelKey]?.isEmpty() == true) {
            topLevelStacks.remove(removedKey)
        }
        topLevelKey = topLevelStacks.keys.last()
        updateBackStack()
        return true
    }

    fun snapshotTopLevelKeys(): List<T> = topLevelStacks.keys.toList()
}

/**
 * Remembers [TopLevelBackStack] across screen rotations (`Portrait ↔ Landscape`) and
 * foldable device fold/unfold configuration changes using `rememberSaveable`.
 */
@Composable
fun rememberTopLevelBackStack(
    startRoute: A2uiTopLevelRoute = A2uiTopLevelRoute.CurrencyExchange,
): TopLevelBackStack<A2uiTopLevelRoute> {
    return rememberSaveable(
        saver =
            Saver(
                save = { stack ->
                    stack.snapshotTopLevelKeys().map { it.routeId }
                },
                restore = { savedIds ->
                    val restoredRoutes = savedIds.map { A2uiTopLevelRoute.fromRouteId(it) }
                    val first = restoredRoutes.firstOrNull() ?: startRoute
                    TopLevelBackStack(first).also { stack ->
                        restoredRoutes.drop(1).forEach { route ->
                            stack.selectTopLevel(route)
                        }
                    }
                },
            )
    ) {
        TopLevelBackStack(startRoute)
    }
}

/**
 * Custom Navigation 3 [Scene] that displays two [NavEntry]s side-by-side in a 50/50 split
 * when the window width is at least Medium (`600dp`, including unfolded foldables).
 */
data class TwoPaneScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val firstEntry: NavEntry<T>,
    val secondEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(firstEntry, secondEntry)

    override val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.5f)) {
                firstEntry.Content()
            }
            Column(modifier = Modifier.weight(0.5f)) {
                secondEntry.Content()
            }
        }
    }

    companion object {
        fun twoPane() = metadata {
            put(TwoPaneKey, true)
        }
    }

    object TwoPaneKey : NavMetadataKey<Boolean>
}

class TwoPaneSceneStrategy<T : Any>(
    private val windowSizeClass: WindowSizeClass,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            return null
        }
        val lastTwoEntries = entries.takeLast(2)
        return if (
            lastTwoEntries.size == 2 &&
                lastTwoEntries.all { it.metadata.contains(TwoPaneScene.TwoPaneKey) }
        ) {
            val firstEntry = lastTwoEntries.first()
            val secondEntry = lastTwoEntries.last()
            TwoPaneScene(
                key = Pair(firstEntry.contentKey, secondEntry.contentKey),
                previousEntries = entries.dropLast(1),
                firstEntry = firstEntry,
                secondEntry = secondEntry,
            )
        } else {
            null
        }
    }
}

@Composable
fun <T : Any> rememberTwoPaneSceneStrategy(): TwoPaneSceneStrategy<T> {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return remember(windowSizeClass) {
        TwoPaneSceneStrategy(windowSizeClass)
    }
}

/**
 * Comprehensive Adaptive & Foldable/Rotation Window Info (`adaptive` skill):
 * - [isMediumOrWider] (`>= 600dp`): True on Unfolded Foldables (`~673dp-768dp`), Landscape Phones, and Tablets
 * - [isExpandedWidth] (`>= 840dp`): True on Tablets, Landscape Foldables, and Desktops
 * - [isCompactHeight] (`< 480dp`): True when a Phone is rotated to Landscape (compact vertical height)
 * - [isTabletopPosture]: True when a Foldable is half-folded in Tabletop / Laptop posture (`windowPosture.isTabletop`)
 * - [hasSeparatingHinge]: True when a Foldable reports a physical hinge separating two panes
 * - [recommendedPaneSpacing]: Hinge-aware spacing between Left (`As-Is`) and Right (`To-Be`) panes
 */
data class A2uiWindowLayoutInfo(
    val isMediumOrWider: Boolean,
    val isExpandedWidth: Boolean,
    val isCompactHeight: Boolean,
    val isTabletopPosture: Boolean,
    val hasSeparatingHinge: Boolean,
    val recommendedPaneSpacing: Dp,
    val widthSizeLabel: String,
) {
    /**
     * Show left `NavigationRail` instead of top tab row whenever:
     * - Width is Medium or wider (`>= 600dp`: Unfolded Foldable / Tablet / Desktop), OR
     * - Phone is rotated to Landscape (`isCompactHeight`), saving precious vertical screen height!
     */
    val shouldUseNavigationRail: Boolean
        get() = isMediumOrWider || isCompactHeight
}

@Composable
fun rememberA2uiWindowLayoutInfo(): A2uiWindowLayoutInfo {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val posture = adaptiveInfo.windowPosture
    val density = LocalDensity.current

    val isMediumOrWider = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isCompactHeight = !windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val isTabletop = posture.isTabletop

    val separatingHinge =
        posture.hingeList.firstOrNull { it.isSeparating || it.isOccluding }
            ?: posture.hingeList.firstOrNull()
    val hasSeparatingHinge = separatingHinge != null
    val hingeWidthDp =
        if (separatingHinge != null) {
            with(density) { separatingHinge.bounds.width.toDp() }.coerceAtLeast(16.dp)
        } else {
            12.dp
        }

    val label =
        when {
            isTabletop -> "Tabletop"
            hasSeparatingHinge -> "Foldable"
            isCompactHeight && isMediumOrWider -> "Landscape"
            isExpandedWidth -> "Expanded"
            isMediumOrWider -> "Foldable/Medium"
            else -> "Compact"
        }

    return A2uiWindowLayoutInfo(
        isMediumOrWider = isMediumOrWider,
        isExpandedWidth = isExpandedWidth,
        isCompactHeight = isCompactHeight,
        isTabletopPosture = isTabletop,
        hasSeparatingHinge = hasSeparatingHinge,
        recommendedPaneSpacing = hingeWidthDp,
        widthSizeLabel = label,
    )
}

/**
 * Multi-preview annotation from the `adaptive` skill covering Phone Portrait, Phone Landscape (Rotation),
 * Unfolded Foldable (`673x841dp`), and Tablet/Desktop.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "1. Phone - Portrait (세로)", device = Devices.PHONE)
@Preview(
    name = "2. Phone - Landscape (가로 회전)",
    device = "spec:width=891dp,height=411dp,orientation=landscape,dpi=420",
)
@Preview(name = "3. Foldable - Unfolded (폴더블 펼침)", device = Devices.FOLDABLE)
@Preview(name = "4. Tablet (태블릿)", device = Devices.TABLET)
@Preview(
    name = "5. Desktop",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    uiMode = UI_MODE_TYPE_NORMAL,
)
annotation class FormFactorPreviews
