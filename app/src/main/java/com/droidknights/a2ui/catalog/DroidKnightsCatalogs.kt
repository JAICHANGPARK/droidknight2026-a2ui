package com.droidknights.a2ui.catalog
import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.ProvideActionInterceptor
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.catalog.A2uiAudioPlayerRenderer
import androidx.compose.material3.a2ui.catalog.A2uiImageRenderer
import androidx.compose.material3.a2ui.catalog.A2uiVideoRenderer
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

/**
 * DroidKnights 2026 — Official AndroidX A2UI (`1.0.0-alpha01`) Catalogs & Custom Primitives.
 *
 * References:
 * - https://developer.android.com/develop/ui/compose/agentic/manage-catalogs
 * - https://developer.android.com/develop/ui/compose/agentic/implement-components
 * - https://developer.android.com/develop/ui/compose/agentic/api-naming
 */
object DroidKnightsCatalogs {

    val BASIC_CATALOG_ID: String = A2uiBasicCatalogV1.CatalogId

    /**
     * New in `1.0.0-SNAPSHOT` (`A2uiCoreSurfaceActor` commit `343aaa87431`):
     * Agents sending the `v0_9_1` basic catalog URI automatically resolve to `A2uiBasicCatalogV1.CatalogId` (`v0_9`).
     */
    const val BASIC_CATALOG_ID_V0_9_1_ALIAS: String =
        "https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json"

    const val CATALOG_ID_V1: String =
        "https://droidknights.com/a2ui/catalog/enterprise/v1"

    const val CATALOG_ID_V2: String =
        "https://droidknights.com/a2ui/catalog/enterprise/v2"

    /**
     * Media & platform hooks required by [materialA2uiBasicCatalogV1].
     */
    private val defaultImageRenderer = A2uiImageRenderer { url, contentDescription, _, modifier, onError ->
        if (url.contains("broken") || url.contains("404")) {
            SideEffect {
                onError(IllegalArgumentException("Simulated 404 image error for $url"))
            }
        }
        Surface(
            modifier = modifier.fillMaxWidth().height(110.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0EA5E9).copy(alpha = 0.2f),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contentDescription ?: "A2uiImageRenderer Hook",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = url,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    private val defaultVideoRenderer = A2uiVideoRenderer { url, modifier, onError ->
        var isPlaying by remember(url) { mutableStateOf(false) }
        var progress by remember(url) { mutableFloatStateOf(0.35f) }
        if (url.contains("broken")) {
            SideEffect { onError(IllegalStateException("Video stream unreachable: $url")) }
        }
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartDisplay,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "A2uiVideoRenderer (Media3 Hook)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = url,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            isPlaying = !isPlaying
                            if (isPlaying && progress < 0.9f) progress += 0.2f
                        },
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying) "일시정지" else "가이드 재생", fontSize = 12.sp)
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF334155),
                )
            }
        }
    }

    private val defaultAudioRenderer =
        A2uiAudioPlayerRenderer { url, contentDescription, modifier, onError ->
            var isPlaying by remember(url) { mutableStateOf(false) }
            if (url.contains("broken")) {
                SideEffect { onError(IllegalStateException("Audio decode failed: $url")) }
            }
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1B4B),
                border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contentDescription ?: "A2uiAudioPlayerRenderer",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = if (isPlaying) "재생 중 · $url" else "대기 중 · $url",
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = { isPlaying = !isPlaying },
                        border = BorderStroke(1.dp, Color(0xFFA5B4FC)),
                    ) {
                        Text(
                            text = if (isPlaying) "정지" else "트랙 재생",
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

    private fun createBasicFunctions(onOpenUrl: (String) -> Unit): A2uiBasicCatalogV1.Functions =
        object : A2uiBasicCatalogV1.Functions {
            override fun openUrl(url: String, executionContext: A2uiExecutionContext) {
                onOpenUrl(url)
            }
        }

    /**
     * 1. Standard Material 3 Catalog + 4 Reusable Custom Domain Primitives.
     * Used across the 4 Non-Travel Domain Scenarios and Feature #1 (Catalog Mode 1).
     */
    fun createStandardCatalog(
        catalogId: String = BASIC_CATALOG_ID,
        onOpenUrl: (String) -> Unit = {},
    ): A2uiCatalog {
        val baseM3 =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(defaultImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(defaultVideoRenderer),
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer(defaultAudioRenderer),
                functions = createBasicFunctions(onOpenUrl),
            )

        return A2uiCatalog(
            catalogId = catalogId,
            components =
                baseM3.components +
                    listOf(
                        CircularClimateDialComponent,
                        PowerBudgetBarComponent,
                        HotspotDiagramCardComponent,
                        ActionGuardContainerComponent,
                        MetricSparklineComponent,
                    ),
            functions = baseM3.functions,
            themeSchema = baseM3.themeSchema,
        )
    }

    /**
     * 2. Brand Design System Catalog (Slot Override: Card / Button / Text).
     * Demonstrates how 100% identical A2UI JSON renders in a custom Brand Design System (Toss/Fintech style)
     * by overriding `A2uiBasicCatalogV1.Card`, `A2uiBasicCatalogV1.Button`, and `A2uiBasicCatalogV1.Text`.
     */
    fun createBrandDesignSystemCatalog(
        catalogId: String = BASIC_CATALOG_ID,
        onOpenUrl: (String) -> Unit = {},
    ): A2uiCatalog {
        val brandBase =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(defaultImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(defaultVideoRenderer),
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer(defaultAudioRenderer),
                functions = createBasicFunctions(onOpenUrl),
                card = BrandSquircleCardComponent,
                button = BrandGradientButtonComponent,
                text = BrandTypographyTextComponent,
            )

        return A2uiCatalog(
            catalogId = catalogId,
            components =
                brandBase.components +
                    listOf(
                        CircularClimateDialComponent,
                        PowerBudgetBarComponent,
                        HotspotDiagramCardComponent,
                        ActionGuardContainerComponent,
                        MetricSparklineComponent,
                    ),
            functions = brandBase.functions,
            themeSchema = brandBase.themeSchema,
        )
    }

    /**
     * 3. Senior High-Contrast / Voice-Guided Catalog (Accessibility Slot Override).
     * Demonstrates how the exact same A2UI JSON adapts to an eldercare/accessibility mode
     * with oversized touch targets, high-contrast borders, and voice guidance badges.
     */
    fun createSeniorAccessibleCatalog(
        catalogId: String = BASIC_CATALOG_ID,
        onOpenUrl: (String) -> Unit = {},
    ): A2uiCatalog {
        val seniorBase =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(defaultImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(defaultVideoRenderer),
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer(defaultAudioRenderer),
                functions = createBasicFunctions(onOpenUrl),
                card = SeniorHighContrastCardComponent,
                button = SeniorVoiceGuidedButtonComponent,
                text = SeniorLargeTextComponent,
            )

        return A2uiCatalog(
            catalogId = catalogId,
            components =
                seniorBase.components +
                    listOf(
                        CircularClimateDialComponent,
                        PowerBudgetBarComponent,
                        HotspotDiagramCardComponent,
                        ActionGuardContainerComponent,
                        MetricSparklineComponent,
                    ),
            functions = seniorBase.functions,
            themeSchema = seniorBase.themeSchema,
        )
    }

    /**
     * V1 vs V2 Catalog pair for Multi-Catalog Negotiation (`manage-catalogs` + `api-naming`).
     * V1 supports basic components; V2 adds `MetricSparkline` + `CircularClimateDial` with `isInline = true`.
     */
    fun createVersionedCatalogPair(): Pair<A2uiCatalog, A2uiCatalog> {
        val baseM3 =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(defaultImageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(defaultVideoRenderer),
                audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer(defaultAudioRenderer),
                functions = createBasicFunctions {},
            )
        val v1 =
            A2uiCatalog(
                catalogId = CATALOG_ID_V1,
                components = baseM3.components,
                functions = baseM3.functions,
                themeSchema = baseM3.themeSchema,
                isInline = false,
            )
        val v2 =
            A2uiCatalog(
                catalogId = CATALOG_ID_V2,
                components =
                    baseM3.components +
                        listOf(
                            MetricSparklineComponent,
                            CircularClimateDialComponent,
                            PowerBudgetBarComponent,
                        ),
                functions = baseM3.functions,
                themeSchema = baseM3.themeSchema,
                isInline = true,
            )
        return v1 to v2
    }
}

// ============================================================================
// Slot Overrides for Brand Design System & Senior High-Contrast Catalogs
// ============================================================================

private object BrandSquircleCardComponent : A2uiBasicCatalogV1.Card {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val childState = observeA2uiComponentState(childId)
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF0F7FF),
            border = BorderStroke(2.dp, Color(0xFF2563EB)),
            shadowElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "BRAND DESIGN SYSTEM · SQUIRCLE SURFACE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D4ED8),
                        fontFamily = FontFamily.Monospace,
                    )
                }
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator(Modifier.size(24.dp))
                    is A2uiComponentState.Error ->
                        Text(childState.exception.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
        }
    }
}

private object BrandGradientButtonComponent : A2uiBasicCatalogV1.Button {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        variant: A2uiBasicCatalogV1.Button.Variant,
        action: Map<String, Any?>,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {
        val currentAction by rememberUpdatedState(action)
        val childState = observeA2uiComponentState(childId)
        val failedCheck = checks.firstOrNull { !it.condition }
        val enabled = childState is A2uiComponentState.Success && failedCheck == null

        Column(modifier = modifier) {
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .background(
                            if (enabled) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                                )
                            }
                        )
                        .clickable(enabled = enabled) { dispatchAction(currentAction) }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (childState) {
                    is A2uiComponentState.Loading ->
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                    is A2uiComponentState.Error -> Text("오류", color = Color.White)
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
            if (failedCheck != null) {
                Text(
                    text = failedCheck.message,
                    color = Color(0xFFDC2626),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private object BrandTypographyTextComponent : A2uiBasicCatalogV1.Text {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val style =
            when (variant) {
                A2uiBasicCatalogV1.Text.Variant.H1,
                A2uiBasicCatalogV1.Text.Variant.H2,
                A2uiBasicCatalogV1.Text.Variant.H3 ->
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E3A8A),
                        letterSpacing = (-0.5).sp,
                    )
                A2uiBasicCatalogV1.Text.Variant.H4,
                A2uiBasicCatalogV1.Text.Variant.H5 ->
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                    )
                A2uiBasicCatalogV1.Text.Variant.Caption ->
                    MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF475569),
                    )
                A2uiBasicCatalogV1.Text.Variant.Body ->
                    MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Medium,
                    )
            }
        Text(text = text, style = style, modifier = modifier)
    }
}

private object SeniorHighContrastCardComponent : A2uiBasicCatalogV1.Card {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val childState = observeA2uiComponentState(childId)
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF9C3),
            border = BorderStroke(3.dp, Color(0xFF111827)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Surface(
                    color = Color(0xFF111827),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "시니어 큰글씨 · 고대비 음성 안내 모드",
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                        )
                    }
                }
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator()
                    is A2uiComponentState.Error ->
                        Text(childState.exception.message.orEmpty(), fontWeight = FontWeight.Bold)
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
        }
    }
}

private object SeniorVoiceGuidedButtonComponent : A2uiBasicCatalogV1.Button {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        variant: A2uiBasicCatalogV1.Button.Variant,
        action: Map<String, Any?>,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {
        val currentAction by rememberUpdatedState(action)
        val childState = observeA2uiComponentState(childId)
        val failedCheck = checks.firstOrNull { !it.condition }
        val enabled = childState is A2uiComponentState.Success && failedCheck == null

        Column(modifier = modifier.fillMaxWidth()) {
            Button(
                onClick = { dispatchAction(currentAction) },
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color(0xFFFACC15),
                    ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFFFACC15),
                )
                Spacer(Modifier.width(8.dp))
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator(Modifier.size(20.dp))
                    is A2uiComponentState.Error -> Text("버튼 오류")
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
            if (failedCheck != null) {
                Text(
                    text = "⚠️ ${failedCheck.message}",
                    color = Color(0xFFB91C1C),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private object SeniorLargeTextComponent : A2uiBasicCatalogV1.Text {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val fontSize =
            when (variant) {
                A2uiBasicCatalogV1.Text.Variant.H1,
                A2uiBasicCatalogV1.Text.Variant.H2,
                A2uiBasicCatalogV1.Text.Variant.H3 -> 22.sp
                A2uiBasicCatalogV1.Text.Variant.H4,
                A2uiBasicCatalogV1.Text.Variant.H5 -> 19.sp
                A2uiBasicCatalogV1.Text.Variant.Caption -> 15.sp
                A2uiBasicCatalogV1.Text.Variant.Body -> 17.sp
            }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            lineHeight = (fontSize.value + 6).sp,
            modifier = modifier,
        )
    }
}

// ============================================================================
// 4 Reusable Custom Domain Components (`implement-components`)
// ============================================================================

/**
 * Custom Component 1: `CircularClimateDial`
 * Demonstrates:
 * - Two-way reactive data binding via `properties.bind(valueProperty)` + `properties.bindUpdater(valueProperty)`
 * - Read-only degradation when `bindUpdater == null` (e.g., when agent provides a literal instead of `{"path": "..."}`)
 * - Runtime error reporting via `reportError(A2uiRuntimeException(...))` if `min >= max`
 */
object CircularClimateDialComponent : A2uiComponent {
    override val name: String = "CircularClimateDial"
    override val description: String =
        "A circular dial for controlling temperature, fan speed, or target threshold with two-way DataModel binding."

    val labelProperty = A2uiProperty.dynamicString("label", required = true, description = "Dial title")
    val unitProperty = A2uiProperty.string("unit", required = false, description = "Unit suffix such as °C or %")
    val valueProperty = A2uiProperty.dynamicNumber("value", required = true, description = "Current numeric value")
    val minProperty = A2uiProperty.number("min", defaultValue = 16.0, description = "Minimum allowed value")
    val maxProperty = A2uiProperty.number("max", defaultValue = 30.0, description = "Maximum allowed value")
    val stepProperty = A2uiProperty.number("step", defaultValue = 0.5, description = "Step increment")

    override val properties: List<A2uiProperty<*>> =
        listOf(labelProperty, unitProperty, valueProperty, minProperty, maxProperty, stepProperty)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val label = properties.bind(labelProperty) ?: "Dial"
        val unit = properties[unitProperty] ?: "°C"
        val rawValue = properties.bind(valueProperty)?.toFloat() ?: 22f
        val min = (properties[minProperty] ?: 16.0).toFloat()
        val max = (properties[maxProperty] ?: 30.0).toFloat()
        val step = (properties[stepProperty] ?: 0.5).toFloat()
        val updater = properties.bindUpdater(valueProperty)
        val isWritable = updater != null

        if (min >= max) {
            SideEffect(min, max) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "CircularClimateDial min ($min) must be strictly less than max ($max)"
                    )
                )
            }
            return
        }

        val clamped = rawValue.coerceIn(min, max)
        val sweepFraction = ((clamped - min) / (max - min)).coerceIn(0f, 1f)

        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, if (isWritable) Color(0xFF38BDF8) else Color(0xFF64748B)),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(76.dp)) {
                        val strokeWidth = 8.dp.toPx()
                        drawArc(
                            color = Color(0xFF334155),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = Size(size.width, size.height),
                        )
                        drawArc(
                            color = if (isWritable) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            startAngle = 135f,
                            sweepAngle = 270f * sweepFraction,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = Size(size.width, size.height),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${((clamped * 10).roundToInt() / 10.0)}$unit",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = if (isWritable) "RW Path" else "ReadOnly",
                            color = if (isWritable) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text =
                            if (isWritable) {
                                "bindUpdater != null · 양방향 JSON Pointer 바인딩 활성"
                            } else {
                                "bindUpdater == null · 리터럴/관찰자 읽기 전용 잠금"
                            },
                        color = if (isWritable) Color(0xFF7DD3FC) else Color(0xFFFBBF24),
                        fontSize = 11.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                updater?.invoke(((clamped - step).coerceAtLeast(min) * 10).roundToInt() / 10.0)
                            },
                            enabled = isWritable && clamped > min,
                        ) {
                            Text("- $step$unit", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                updater?.invoke(((clamped + step).coerceAtMost(max) * 10).roundToInt() / 10.0)
                            },
                            enabled = isWritable && clamped < max,
                        ) {
                            Text("+ $step$unit", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Component 2: `PowerBudgetBar`
 * Demonstrates:
 * - Multi-property reactive DataModel binding (`usedKw`, `maxKw`, `breakdownLabel`)
 * - Automatic runtime safety check (`reportError` when `usedKw > maxKw`)
 */
object PowerBudgetBarComponent : A2uiComponent {
    override val name: String = "PowerBudgetBar"
    override val description: String =
        "Displays real-time power or resource consumption against a maximum budget."

    val titleProperty = A2uiProperty.dynamicString("title", required = true)
    val usedProperty = A2uiProperty.dynamicNumber("usedKw", required = true)
    val maxProperty = A2uiProperty.dynamicNumber("maxKw", required = true)
    val detailProperty = A2uiProperty.dynamicString("detail", required = false)

    override val properties: List<A2uiProperty<*>> =
        listOf(titleProperty, usedProperty, maxProperty, detailProperty)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val title = properties.bind(titleProperty) ?: "Power Budget"
        val used = properties.bind(usedProperty)?.toFloat() ?: 0f
        val max = (properties.bind(maxProperty)?.toFloat() ?: 10f).coerceAtLeast(0.1f)
        val detail = properties.bind(detailProperty)
        val ratio = (used / max).coerceIn(0f, 1f)
        val isOverload = used > max

        if (isOverload) {
            SideEffect(used, max) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "PowerBudgetBar overload: usedKw ($used) exceeds maxKw ($max)"
                    )
                )
            }
        }

        val barColor =
            when {
                isOverload -> Color(0xFFEF4444)
                ratio > 0.8f -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, barColor.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    Text(
                        text = "${((used * 10).roundToInt() / 10.0)} / $max kW (${(ratio * 100).roundToInt()}%)",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = barColor,
                    )
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = barColor,
                    trackColor = Color(0xFFE2E8F0),
                )
                if (!detail.isNullOrBlank()) {
                    Text(detail, fontSize = 11.sp, color = Color(0xFF475569))
                }
            }
        }
    }
}

/**
 * Custom Component 3: `HotspotDiagramCard`
 * Demonstrates:
 * - `isReady(properties)` override for progressive rendering: stays in `A2uiComponentState.Loading`
 *   (showing Material3 radial shimmer skeleton) until `title` and `hotspotLabel` arrive in DataModel!
 * - Interactive hotspot pin that dispatches an A2UI action back to the agent.
 */
object HotspotDiagramCardComponent : A2uiComponent {
    override val name: String = "HotspotDiagramCard"
    override val description: String =
        "Interactive hardware/architecture schematic card with a highlighted inspection pin and progressive readiness gate."

    val titleProperty = A2uiProperty.dynamicString("title", required = true)
    val hotspotLabelProperty = A2uiProperty.dynamicString("hotspotLabel", required = true)
    val zoneCodeProperty = A2uiProperty.dynamicString("zoneCode", required = false)
    val statusProperty = A2uiProperty.dynamicString("status", required = false)
    val actionProperty = A2uiProperty.action("action", required = false)

    override val properties: List<A2uiProperty<*>> =
        listOf(titleProperty, hotspotLabelProperty, zoneCodeProperty, statusProperty, actionProperty)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        val title = properties.bind(titleProperty)
        val hotspot = properties.bind(hotspotLabelProperty)
        return !title.isNullOrBlank() && !hotspot.isNullOrBlank()
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val title = properties.bind(titleProperty).orEmpty()
        val hotspotLabel = properties.bind(hotspotLabelProperty).orEmpty()
        val zoneCode = properties.bind(zoneCodeProperty) ?: "ZONE-A1"
        val status = properties.bind(statusProperty) ?: "점검 필요"
        val action = properties[actionProperty]

        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "$zoneCode · $status",
                            color = Color(0xFFFCD34D),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // Schematic Diagram Canvas with pulsing hotspot
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(86.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(86.dp)) {
                        val gridStep = 24.dp.toPx()
                        var x = gridStep
                        while (x < size.width) {
                            drawLine(
                                color = Color(0xFF334155),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f,
                            )
                            x += gridStep
                        }
                        val pinCenter = Offset(size.width * 0.28f, size.height * 0.5f)
                        drawCircle(
                            color = Color(0xFFF59E0B).copy(alpha = 0.28f),
                            radius = 22.dp.toPx(),
                            center = pinCenter,
                        )
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = 8.dp.toPx(),
                            center = pinCenter,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.width(72.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📍 핫스팟 위치: $hotspotLabel",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = "isReady() 통과 후 렌더링됨 (Progressive Ready)",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                            )
                        }
                        if (action != null) {
                            FilledTonalButton(onClick = { dispatchAction(action) }) {
                                Text("점검 완료", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Component 4: `ActionGuardContainer`
 * Demonstrates `ProvideActionInterceptor` from `androidx.a2ui.compose.runtime`!
 * Any descendant button inside `child` that dispatches an event whose name matches `guardedActionName`
 * is intercepted locally in the Compose tree before reaching the Surface/Server, requiring explicit
 * local 2FA/biometric approval.
 */
object ActionGuardContainerComponent : A2uiComponent {
    override val name: String = "ActionGuardContainer"
    override val description: String =
        "Wraps a child component and intercepts high-risk descendant actions using ProvideActionInterceptor for local 2FA approval."

    val childProperty = A2uiProperty.componentId("child", required = true)
    val guardedActionNameProperty = A2uiProperty.string("guardedActionName", required = true)
    val guardTitleProperty = A2uiProperty.dynamicString("guardTitle", required = false)

    override val properties: List<A2uiProperty<*>> =
        listOf(childProperty, guardedActionNameProperty, guardTitleProperty)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val childId = properties[childProperty] ?: return
        val guardedName = properties[guardedActionNameProperty].orEmpty()
        val guardTitle = properties.bind(guardTitleProperty) ?: "로컬 보안 가드레일 (ProvideActionInterceptor)"
        var pendingPayload by remember { mutableStateOf<Map<String, Any?>?>(null) }

        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProvideActionInterceptor(
                onIntercept = { actionPayload ->
                    val eventMap = actionPayload["event"] as? Map<*, *>
                    val eventName = eventMap?.get("name") as? String
                    if (eventName == guardedName) {
                        pendingPayload = actionPayload
                        true // Consume locally! Do not send to server yet.
                    } else {
                        false // Pass through to surface/server
                    }
                }
            ) {
                val childState = observeA2uiComponentState(childId)
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator(Modifier.size(20.dp))
                    is A2uiComponentState.Error ->
                        Text(childState.exception.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }

            AnimatedContent(targetState = pendingPayload, label = "LocalGuardModal") { intercepted ->
                if (intercepted != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF450A0A),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFFCA5A5),
                                )
                                Text(
                                    text = "🔒 $guardTitle",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                )
                            }
                            Text(
                                text =
                                    "하위 버튼의 '$guardedName' 액션이 서버(outboundEvents)로 나가기 전에 " +
                                        "ProvideActionInterceptor에 의해 로컬에서 가로채졌습니다.",
                                color = Color(0xFFFECACA),
                                fontSize = 12.sp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { pendingPayload = null },
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                ) {
                                    Text("취소 (차단 유지)", color = Color.White, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        val approved = intercepted
                                        pendingPayload = null
                                        // Dispatch from outside ProvideActionInterceptor so it reaches the surface
                                        dispatchAction(approved)
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDC2626),
                                            contentColor = Color.White,
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("생체인증 승인 및 서버 전송", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Component 5: `MetricSparkline` (Available in V2 Catalog & Standard Catalog)
 */
object MetricSparklineComponent : A2uiComponent {
    override val name: String = "MetricSparkline"
    override val description: String =
        "Renders a compact time-series metric card with a live sparkline curve (V2 catalog capability)."

    val metricNameProperty = A2uiProperty.dynamicString("metricName", required = true)
    val currentValueProperty = A2uiProperty.dynamicString("currentValue", required = true)
    val severityProperty = A2uiProperty.dynamicString("severity", required = false)
    /**
     * New in `1.0.0-SNAPSHOT` (`A2uiProperty` commit `8b873c31483`):
     * `numberList` validates `minItems >= 0`, `maxItems >= 0`, and `minItems <= maxItems` at construction time.
     */
    val pointsProperty =
        A2uiProperty.numberList(
            key = "points",
            required = false,
            minItems = 2,
            maxItems = 24,
            description = "Normalized sparkline points (0.0..1.0) with minItems/maxItems schema bounds",
        )

    override val properties: List<A2uiProperty<*>> =
        listOf(metricNameProperty, currentValueProperty, severityProperty, pointsProperty)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val metricName = properties.bind(metricNameProperty) ?: "Latency"
        val currentValue = properties.bind(currentValueProperty) ?: "0ms"
        val severity = properties.bind(severityProperty) ?: "normal"
        val rawPoints = properties[pointsProperty]
        val points =
            remember(rawPoints) {
                val parsed = rawPoints?.map { it.toFloat().coerceIn(0.05f, 0.98f) }
                if (parsed != null && parsed.size >= 2) {
                    parsed
                } else {
                    listOf(0.7f, 0.55f, 0.65f, 0.35f, 0.85f, 0.2f, 0.9f)
                }
            }
        val accentColor =
            if (severity.equals("critical", ignoreCase = true)) {
                Color(0xFFEF4444)
            } else {
                Color(0xFF10B981)
            }

        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "V2 MetricSparkline · $metricName",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = currentValue,
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                }
                Canvas(modifier = Modifier.width(100.dp).height(32.dp)) {
                    val stepX = size.width / (points.size - 1)
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = accentColor,
                            start = Offset(i * stepX, size.height * (1f - points[i])),
                            end = Offset((i + 1) * stepX, size.height * (1f - points[i + 1])),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}
