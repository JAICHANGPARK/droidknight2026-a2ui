package com.droidknights.a2ui.catalog.domains

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import com.droidknights.a2ui.catalog.hooks.DefaultA2uiMediaRenderers

/**
 * Domain C Catalog Schema & Builder — Cross-Feature Mashup, Dynamic Triage Intake & Differential Dutch-Pay.
 *
 * Key Architectural Proof:
 * Registers **ZERO custom domain components (`emptyList()`)**!
 * Uses 100% pure `materialA2uiBasicCatalogV1` (18 standard components + 15 built-in functions such as
 * `formatString`, `formatCurrency`, `required`, `length`) to dynamically synthesize:
 * 1. Japan Trip 3-in-1 Cross-Feature Mashup (`/japanTrip`)
 * 2. Delivery Claim Triage A / B / C with variable schemas (`/claim`)
 * 3. Multi-round Differential Dutch-Pay Calculator (`/settlement`)
 */
object DynamicMashupCatalog {

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
            components = baseM3.components, // 0 custom widgets! Pure Basic Catalog V1.
            functions = baseM3.functions,
            themeSchema = baseM3.themeSchema,
        )
    }
}
