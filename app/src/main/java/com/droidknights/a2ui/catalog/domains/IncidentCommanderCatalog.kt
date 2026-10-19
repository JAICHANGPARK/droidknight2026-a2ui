package com.droidknights.a2ui.catalog.domains

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import com.droidknights.a2ui.catalog.components.ActionGuardContainerComponent
import com.droidknights.a2ui.catalog.components.CircularClimateDialComponent
import com.droidknights.a2ui.catalog.components.HotspotDiagramCardComponent
import com.droidknights.a2ui.catalog.components.MetricSparklineComponent
import com.droidknights.a2ui.catalog.components.PowerBudgetBarComponent
import com.droidknights.a2ui.catalog.hooks.DefaultA2uiMediaRenderers

/**
 * Domain A Catalog Schema & Builder — 2 AM Mobile On-Call Incident Commander.
 *
 * Combines `materialA2uiBasicCatalogV1` with SRE & Guardrail primitives:
 * - [ActionGuardContainerComponent] (`ProvideActionInterceptor` local 2FA guardrail)
 * - [MetricSparklineComponent] (`minItems`/`maxItems` p99 latency telemetry)
 * - [CircularClimateDialComponent] (`bindUpdater` RW/Read-Only traffic weight dial)
 * - [PowerBudgetBarComponent] (K8s cluster memory budget gauge)
 * - [HotspotDiagramCardComponent] (Canary node failure topology pin)
 */
object IncidentCommanderCatalog {

    val domainComponents: List<A2uiComponent> =
        listOf(
            ActionGuardContainerComponent,
            MetricSparklineComponent,
            CircularClimateDialComponent,
            PowerBudgetBarComponent,
            HotspotDiagramCardComponent,
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
