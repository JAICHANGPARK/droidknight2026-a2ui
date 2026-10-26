package com.droidknights.a2ui.common

import androidx.a2ui.compose.ui.tooling.preview.A2uiPreviewParameterProvider
import androidx.a2ui.compose.ui.tooling.preview.A2uiSurfaceModelFactory
import androidx.a2ui.compose.ui.tooling.preview.rememberA2uiPreviewSurface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.labs.buildCatalogComparisonJsonl
import com.droidknights.a2ui.scenarios.payloads.ContextualCockpitPayloads
import com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
import com.droidknights.a2ui.scenarios.payloads.DynamicMashupPayloads
import com.droidknights.a2ui.scenarios.payloads.FieldDiagnosticsPayloads
import com.droidknights.a2ui.scenarios.payloads.IncidentCommanderPayloads
import com.droidknights.a2ui.theme.DroidKnightsA2uiTheme

/**
 * Official `androidx.a2ui.compose:compose-ui-tooling-preview` (`98b3d9f6fc0` / CL `4324646`)
 * [A2uiPreviewParameterProvider] implementation for DroidKnights 2026 domain scenarios.
 *
 * Uses the `@Composable () -> A2uiCatalog` (`catalogProvider`) constructor so that
 * `LocalContext.current` and `remember` are resolved inside composition when
 * [A2uiSurfaceModelFactory.create] is invoked in `@Preview`.
 */
class DroidKnightsA2uiSurfacePreviewProvider :
    A2uiPreviewParameterProvider(
        catalogProvider = {
            val context = LocalContext.current
            remember(context) { DroidKnightsCatalogs.createStandardCatalog(context = context) }
        },
        namedJsonMessages =
            mapOf(
                "1. Currency Exchange" to CurrencyExchangePayloads.buildJsonl(0),
                "2. Incident Commander" to IncidentCommanderPayloads.buildJsonl(0, false),
                "3. Contextual Cockpit" to ContextualCockpitPayloads.buildJsonl(0),
                "4. Dynamic Mashup" to DynamicMashupPayloads.buildJsonl(0),
                "5. Field Diagnostics" to FieldDiagnosticsPayloads.buildJsonl(1, false).first,
                "6. Catalog V2 Transfer" to
                    buildCatalogComparisonJsonl(DroidKnightsCatalogs.CATALOG_ID_V2, true),
            ),
    )

@Preview(showBackground = true, widthDp = 412, name = "A2UI Surface Preview Parameter")
@Composable
internal fun DroidKnightsA2uiSurfacePreview(
    @PreviewParameter(DroidKnightsA2uiSurfacePreviewProvider::class)
    surfaceFactory: A2uiSurfaceModelFactory,
) {
    DroidKnightsA2uiTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                A2uiSurface(
                    surfaceModel = surfaceFactory.create(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "A2UI Remember Preview Surface")
@Composable
internal fun DroidKnightsRememberA2uiPreviewSurfaceSample() {
    val context = LocalContext.current
    val catalog = remember(context) { DroidKnightsCatalogs.createStandardCatalog(context = context) }
    val surfaceModel =
        rememberA2uiPreviewSurface(
            catalog = catalog,
            jsonMessages = CurrencyExchangePayloads.buildJsonl(0),
        )
    DroidKnightsA2uiTheme {
        Surface {
            Box(modifier = Modifier.padding(16.dp)) {
                A2uiSurface(
                    surfaceModel = surfaceModel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
