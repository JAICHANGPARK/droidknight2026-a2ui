package com.droidknights.a2ui.catalog.schema

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Schema Contract for `HotspotDiagramCard` (Used in Domain D: Appliance Diagnostics Wizard & Domain A/B).
 *
 * Responsibilities:
 * - Declares `A2uiProperty` schema (`title`, `hotspotLabel`, `zoneCode`, `status`, `action`)
 * - Overrides `isReady(properties)` for Progressive Rendering: keeps component in `A2uiComponentState.Loading`
 *   (rendering `ShimmerIndicator`) until both `title` and `hotspotLabel` are non-blank in the DataModel.
 */
interface HotspotDiagramCardSchema : A2uiComponent {
    override val name: String
        get() = "HotspotDiagramCard"

    override val description: String
        get() =
            "Interactive hardware/architecture schematic card with a highlighted inspection pin and progressive readiness gate."

    override val properties: List<A2uiProperty<*>>
        get() =
            listOf(
                titleProperty,
                hotspotLabelProperty,
                zoneCodeProperty,
                statusProperty,
                actionProperty,
            )

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

        TypedContent(
            title = title,
            hotspotLabel = hotspotLabel,
            zoneCode = zoneCode,
            status = status,
            action = action,
            modifier = modifier,
        )
    }

    @Composable
    fun A2uiComponentScope.TypedContent(
        title: String,
        hotspotLabel: String,
        zoneCode: String,
        status: String,
        action: Map<String, Any?>?,
        modifier: Modifier,
    )

    companion object {
        val titleProperty = A2uiProperty.dynamicString("title", required = true)
        val hotspotLabelProperty = A2uiProperty.dynamicString("hotspotLabel", required = true)
        val zoneCodeProperty = A2uiProperty.dynamicString("zoneCode", required = false)
        val statusProperty = A2uiProperty.dynamicString("status", required = false)
        val actionProperty = A2uiProperty.action("action", required = false)
    }
}
