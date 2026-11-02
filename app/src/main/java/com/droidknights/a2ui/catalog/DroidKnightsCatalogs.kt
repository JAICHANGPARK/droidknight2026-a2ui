package com.droidknights.a2ui.catalog

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import com.droidknights.a2ui.catalog.components.ActionGuardContainerComponent
import com.droidknights.a2ui.catalog.components.BidirectionalExchangeInputComponent
import com.droidknights.a2ui.catalog.components.CircularClimateDialComponent
import com.droidknights.a2ui.catalog.components.HotspotDiagramCardComponent
import com.droidknights.a2ui.catalog.components.MetricSparklineComponent
import com.droidknights.a2ui.catalog.components.PowerBudgetBarComponent
import com.droidknights.a2ui.catalog.functions.A2uiBanknoteDenominationFunction
import com.droidknights.a2ui.catalog.hooks.DefaultA2uiMediaRenderers
import com.droidknights.a2ui.catalog.themes.BrandGradientButtonComponent
import com.droidknights.a2ui.catalog.themes.BrandSquircleCardComponent
import com.droidknights.a2ui.catalog.themes.BrandTypographyTextComponent
import com.droidknights.a2ui.catalog.themes.SeniorHighContrastCardComponent
import com.droidknights.a2ui.catalog.themes.SeniorLargeTextComponent
import com.droidknights.a2ui.catalog.themes.SeniorVoiceGuidedButtonComponent

/**
 * DroidKnights 2026 — Official AndroidX A2UI (`1.0.0-alpha01` / `1.0.0-SNAPSHOT`) Catalog Registry.
 *
 * Modular Architecture:
 * - `catalog/schema/DomainCatalogSchemas.kt` — Protocol schema contracts (`A2uiProperty`, `isReady`, `TypedContent`)
 * - `catalog/components/`                    — Pure Compose Widget UI implementations (reusable domain primitives)
 * - `catalog/themes/`                        — Brand Design System & Senior Accessibility `A2uiBasicCatalogV1` overrides
 * - `catalog/hooks/DefaultA2uiMediaRenderers.kt` — Media3 Video / Audio / Image platform hooks
 */
object DroidKnightsCatalogs {

    val BASIC_CATALOG_ID: String = A2uiBasicCatalogV1.CatalogId

    /**
     * New in `1.0.0-SNAPSHOT` (`A2uiCoreSurfaceActor` commit `343aaa87431`):
     * Agents sending the `v0_9_1` basic catalog URI automatically resolve to `A2uiBasicCatalogV1.CatalogId` (`v0_9`).
     */
    const val BASIC_CATALOG_ID_V0_9_1_ALIAS: String =
        "https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json"

    /**
     * 앱 자체 커스텀 카탈로그 ID 예시 (Lab 1 V1/V2 역량 협상 시연용 사내 도메인 URI 식별자).
     * 실제 웹 서버 주소가 아니라 XML 네임스페이스/JSON Schema $id처럼 고유 식별자로 동작합니다.
     */
    const val CATALOG_ID_V1: String =
        "https://droidknights.com/a2ui/catalog/enterprise/v1"

    const val CATALOG_ID_V2: String =
        "https://droidknights.com/a2ui/catalog/enterprise/v2"

    /**
     * The reusable custom domain primitives registered on top of `materialA2uiBasicCatalogV1`.
     */
    val domainComponents: List<A2uiComponent> =
        listOf(
            BidirectionalExchangeInputComponent,
            CircularClimateDialComponent,
            PowerBudgetBarComponent,
            HotspotDiagramCardComponent,
            ActionGuardContainerComponent,
            MetricSparklineComponent,
        )

    /**
     * Custom domain `A2uiFunction`s registered alongside `A2uiBasicCatalogV1` functions (`numeric`,
     * `required`, `regex`, `and`, `or`, `not`, `formatCurrency`, `openUrl`, etc.).
     */
    val domainFunctions: List<A2uiFunction> =
        listOf(
            A2uiBanknoteDenominationFunction.INSTANCE,
        )

    /**
     * 1. Standard Material 3 Catalog + 6 Reusable Custom Domain Primitives + Custom Domain Functions.
     */
    fun createStandardCatalog(
        catalogId: String = BASIC_CATALOG_ID,
        context: android.content.Context? = null,
        onOpenUrl: ((String) -> Unit)? = null,
    ): A2uiCatalog {
        val baseM3 =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(DefaultA2uiMediaRenderers.imageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(DefaultA2uiMediaRenderers.videoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(DefaultA2uiMediaRenderers.audioRenderer),
                functions = DefaultA2uiMediaRenderers.createBasicFunctions(context = context, onOpenUrl = onOpenUrl),
            )

        return A2uiCatalog(
            catalogId = catalogId,
            components = baseM3.components + domainComponents,
            functions = baseM3.functions + domainFunctions,
            themeSchema = baseM3.themeSchema,
        )
    }

    /**
     * 2. Brand Design System Catalog (Slot Override: Card / Button / Text).
     */
    fun createBrandDesignSystemCatalog(
        catalogId: String = BASIC_CATALOG_ID,
        context: android.content.Context? = null,
        onOpenUrl: ((String) -> Unit)? = null,
    ): A2uiCatalog {
        val brandBase =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(DefaultA2uiMediaRenderers.imageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(DefaultA2uiMediaRenderers.videoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(DefaultA2uiMediaRenderers.audioRenderer),
                functions = DefaultA2uiMediaRenderers.createBasicFunctions(context = context, onOpenUrl = onOpenUrl),
                card = BrandSquircleCardComponent,
                button = BrandGradientButtonComponent,
                text = BrandTypographyTextComponent,
            )

        return A2uiCatalog(
            catalogId = catalogId,
            components = brandBase.components + domainComponents,
            functions = brandBase.functions + domainFunctions,
            themeSchema = brandBase.themeSchema,
        )
    }

    /**
     * 3. Senior High-Contrast / Voice-Guided Catalog (Accessibility Slot Override).
     */
    fun createSeniorAccessibleCatalog(
        catalogId: String = BASIC_CATALOG_ID,
        context: android.content.Context? = null,
        onOpenUrl: ((String) -> Unit)? = null,
    ): A2uiCatalog {
        val seniorBase =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(DefaultA2uiMediaRenderers.imageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(DefaultA2uiMediaRenderers.videoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(DefaultA2uiMediaRenderers.audioRenderer),
                functions = DefaultA2uiMediaRenderers.createBasicFunctions(context = context, onOpenUrl = onOpenUrl),
                card = SeniorHighContrastCardComponent,
                button = SeniorVoiceGuidedButtonComponent,
                text = SeniorLargeTextComponent,
            )

        return A2uiCatalog(
            catalogId = catalogId,
            components = seniorBase.components + domainComponents,
            functions = seniorBase.functions + domainFunctions,
            themeSchema = seniorBase.themeSchema,
        )
    }

    /**
     * V1 vs V2 Catalog pair for Multi-Catalog Negotiation (`manage-catalogs` + `api-naming`).
     */
    fun createVersionedCatalogPair(
        context: android.content.Context? = null,
    ): Pair<A2uiCatalog, A2uiCatalog> {
        val baseM3 =
            materialA2uiBasicCatalogV1(
                image = MaterialA2uiBasicCatalogV1Defaults.image(DefaultA2uiMediaRenderers.imageRenderer),
                video = MaterialA2uiBasicCatalogV1Defaults.video(DefaultA2uiMediaRenderers.videoRenderer),
                audioPlayer =
                    MaterialA2uiBasicCatalogV1Defaults.audioPlayer(DefaultA2uiMediaRenderers.audioRenderer),
                functions = DefaultA2uiMediaRenderers.createBasicFunctions(context = context),
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
