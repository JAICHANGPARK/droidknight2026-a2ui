package com.droidknights.a2ui.catalog.domains

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import com.droidknights.a2ui.catalog.components.ActionGuardContainerComponent
import com.droidknights.a2ui.catalog.components.CircularClimateDialComponent
import com.droidknights.a2ui.catalog.components.HotspotDiagramCardComponent
import com.droidknights.a2ui.catalog.components.PowerBudgetBarComponent
import com.droidknights.a2ui.catalog.hooks.DefaultA2uiMediaRenderers

/**
 * Domain B Catalog Schema & Builder — Smart Home / EV (SDV) Contextual Micro-Cockpit.
 *
 * Combines `materialA2uiBasicCatalogV1` Media Hooks (`Video`, `AudioPlayer`, `Image`) with
 * reusable IoT & SDV primitives:
 * - [CircularClimateDialComponent] (Nursery AC & EV Cabin Heat Pump temperature dial)
 * - [PowerBudgetBarComponent] (EV V2L inverter power budget bar)
 * - [HotspotDiagramCardComponent] (Utility room water valve sensor pin)
 * - [ActionGuardContainerComponent] (Remote front door unlock 2FA guardrail)
 */
object ContextualCockpitCatalog {

    val domainComponents: List<A2uiComponent> =
        listOf(
            CircularClimateDialComponent,
            PowerBudgetBarComponent,
            HotspotDiagramCardComponent,
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
