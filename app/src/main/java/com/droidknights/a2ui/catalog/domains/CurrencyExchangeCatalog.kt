package com.droidknights.a2ui.catalog.domains

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import com.droidknights.a2ui.catalog.components.ActionGuardContainerComponent
import com.droidknights.a2ui.catalog.components.BidirectionalExchangeInputComponent
import com.droidknights.a2ui.catalog.components.MetricSparklineComponent
import com.droidknights.a2ui.catalog.hooks.DefaultA2uiMediaRenderers

/**
 * Dedicated A2UI Catalog for **Domain Scenario: Foreign Currency Exchange (모바일 뱅킹 외화 환전 도메인)**.
 *
 * Combines `materialA2uiBasicCatalogV1` (`TextField` for 4-digit account PIN,
 * `ChoicePicker` for pickup branch/denominations, `CheckBox`) with:
 * - [BidirectionalExchangeInputComponent] for real-time Foreign Currency ↔ KRW dual text box conversion
 * - [MetricSparklineComponent] for real-time exchange rate trends (`JPY/KRW`)
 * - [ActionGuardContainerComponent] for biometric/PIN exchange execution guardrail
 */
object CurrencyExchangeCatalog {

    val domainComponents: List<A2uiComponent> =
        listOf(
            BidirectionalExchangeInputComponent,
            MetricSparklineComponent,
            ActionGuardContainerComponent,
        )

    fun create(
        catalogId: String = A2uiBasicCatalogV1.CatalogId,
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
            functions = baseM3.functions,
            themeSchema = baseM3.themeSchema,
        )
    }
}
