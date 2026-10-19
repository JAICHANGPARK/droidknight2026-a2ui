package com.droidknights.a2ui.common

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.labs.buildCatalogComparisonJsonl
import com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
import com.droidknights.a2ui.scenarios.payloads.IncidentCommanderPayloads
import com.droidknights.a2ui.theme.DroidKnightsA2uiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Mirrors the upcoming `androidx.a2ui.compose:compose-ui-tooling-preview` API
 * (`A2uiPreviewParameterProvider` and `rememberA2uiPreviewSurface`, AOSP CL `4324646`).
 *
 * Converts raw A2UI JSON protocol messages into ready-to-render [A2uiSurfaceModel] instances
 * for Android Studio Compose `@Preview` functions.
 */
open class A2uiPreviewParameterProvider
private constructor(
    private val payloads: List<Pair<String?, List<String>>>,
    private val catalog: A2uiCatalog,
) : PreviewParameterProvider<A2uiSurfaceModel> {

    constructor(
        catalog: A2uiCatalog,
        namedJsonMessages: Map<String, List<String>>,
    ) : this(
        payloads = namedJsonMessages.toList(),
        catalog = catalog,
    )

    constructor(
        catalog: A2uiCatalog,
        vararg jsonMessages: List<String>,
    ) : this(
        payloads = jsonMessages.map { null to it },
        catalog = catalog,
    )

    override val values: Sequence<A2uiSurfaceModel>
        get() {
            val scope = CoroutineScope(SupervisorJob())
            return payloads
                .indices
                .asSequence()
                .map { index ->
                    createA2uiPreviewSurfaceModel(
                        catalog = catalog,
                        jsonMessages = payloads[index].second,
                        scope = scope,
                    )
                }
        }

    override fun getDisplayName(index: Int): String? = payloads.getOrNull(index)?.first
}

/**
 * Creates and remembers an [A2uiSurfaceModel] from raw A2UI JSON messages for Compose `@Preview`s.
 */
@Composable
fun rememberA2uiPreviewSurface(
    catalog: A2uiCatalog,
    jsonMessages: List<String>,
): A2uiSurfaceModel {
    val scope = remember(catalog, jsonMessages) { CoroutineScope(SupervisorJob()) }
    val surfaceModel =
        remember(scope) {
            createA2uiPreviewSurfaceModel(
                catalog = catalog,
                jsonMessages = jsonMessages,
                scope = scope,
            )
        }
    DisposableEffect(scope) { onDispose { scope.cancel() } }
    return surfaceModel
}

internal fun createA2uiPreviewSurfaceModel(
    catalog: A2uiCatalog,
    jsonMessages: List<String>,
    scope: CoroutineScope,
): A2uiSurfaceModel {
    val parser = A2uiMessageParser()
    val processor = A2uiMessageProcessor(catalogs = listOf(catalog))
    scope.launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
        processor.collectMessages()
    }

    for (i in jsonMessages.indices) {
        processor.processMessage(parser.parse(jsonMessages[i]))
    }

    val surfaces = processor.activeSurfaces.value
    require(surfaces.isNotEmpty()) {
        "No active surface was created. Ensure jsonMessages includes a createSurface message."
    }
    return surfaces[0]
}

class DroidKnightsA2uiSurfacePreviewProvider :
    A2uiPreviewParameterProvider(
        catalog = DroidKnightsCatalogs.createStandardCatalog(),
        namedJsonMessages =
            mapOf(
                "1. Currency Exchange" to CurrencyExchangePayloads.buildJsonl(0),
                "2. Incident Commander" to IncidentCommanderPayloads.buildJsonl(0, false),
                "3. Catalog V2 Transfer" to
                    buildCatalogComparisonJsonl(DroidKnightsCatalogs.CATALOG_ID_V2, true),
            ),
    )

@Preview(showBackground = true, widthDp = 412, name = "A2UI Surface Preview Parameter")
@Composable
internal fun DroidKnightsA2uiSurfacePreview(
    @PreviewParameter(DroidKnightsA2uiSurfacePreviewProvider::class)
    surfaceModel: A2uiSurfaceModel,
) {
    DroidKnightsA2uiTheme {
        A2uiSurface(
            surfaceModel = surfaceModel,
            modifier = Modifier.fillMaxWidth(),
        )
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
        A2uiSurface(
            surfaceModel = surfaceModel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
