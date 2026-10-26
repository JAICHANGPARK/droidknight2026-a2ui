package com.droidknights.a2ui.catalog.schema

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Schema Contract for `MetricSparkline` (Used in Domain A: On-Call SRE Telemetry & Lab 1/2: V2 Catalog Evolution).
 *
 * Responsibilities:
 * - Declares `A2uiProperty` schema (`metricName`, `currentValue`, `severity`, `points`)
 * - Uses `A2uiProperty.numberList(minItems = 2, maxItems = 24)` (`1.0.0-SNAPSHOT` bounds validation)
 */
interface MetricSparklineSchema : A2uiComponent {
    override val name: String
        get() = "MetricSparkline"

    override val description: String
        get() =
            "Renders a compact time-series metric card with a live sparkline curve (V2 catalog capability)."

    override val properties: List<A2uiProperty<*>>
        get() =
            listOf(
                metricNameProperty,
                currentValueProperty,
                severityProperty,
                pointsProperty,
                thresholdProperty,
                annotationsProperty,
            )

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(metricNameProperty) != null &&
            properties.bind(currentValueProperty) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val metricName = properties.bind(metricNameProperty) ?: return
        val currentValue = properties.bind(currentValueProperty) ?: return
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
        val thresholdObj = properties[thresholdProperty]
        val thresholdSummary =
            thresholdObj?.let { obj ->
                val warn = obj[thresholdWarnProperty]
                val crit = obj[thresholdCritProperty]
                if (warn != null || crit != null) "SLO warn=$warn / crit=$crit" else null
            }
        val annotations =
            properties[annotationsProperty].orEmpty().mapNotNull { itemProps ->
                val tag = itemProps.bind(annotationTagProperty) ?: return@mapNotNull null
                val note = itemProps.bind(annotationNoteProperty)
                if (note.isNullOrBlank()) "#$tag" else "#$tag · $note"
            }

        TypedContent(
            metricName = metricName,
            currentValue = currentValue,
            severity = severity,
            points = points,
            thresholdSummary = thresholdSummary,
            annotations = annotations,
            modifier = modifier,
        )
    }

    @Composable
    fun A2uiComponentScope.TypedContent(
        metricName: String,
        currentValue: String,
        severity: String,
        points: List<Float>,
        thresholdSummary: String?,
        annotations: List<String>,
        modifier: Modifier,
    )

    companion object {
        val metricNameProperty = A2uiProperty.dynamicString("metricName", required = true)
        val currentValueProperty = A2uiProperty.dynamicString("currentValue", required = true)
        val severityProperty = A2uiProperty.dynamicString("severity", required = false)
        val pointsProperty =
            A2uiProperty.numberList(
                key = "points",
                required = false,
                minItems = 2,
                maxItems = 24,
                description =
                    "Normalized sparkline points (0.0..1.0) with minItems/maxItems schema bounds",
            )
        val thresholdWarnProperty =
            A2uiProperty.number("warn", required = false, description = "Warning threshold")
        val thresholdCritProperty =
            A2uiProperty.number("critical", required = false, description = "Critical threshold")
        val thresholdProperty =
            A2uiProperty.nested(
                key = "threshold",
                properties = listOf(thresholdWarnProperty, thresholdCritProperty),
                required = false,
                description = "Nested threshold bounds (AOSP CL 4390685 A2uiProperty.nested)",
            )
        val annotationTagProperty =
            A2uiProperty.dynamicString("tag", required = true, description = "Annotation tag")
        val annotationNoteProperty =
            A2uiProperty.dynamicString("note", required = false, description = "Annotation note")
        val annotationsProperty =
            A2uiProperty.nestedList(
                key = "annotations",
                properties = listOf(annotationTagProperty, annotationNoteProperty),
                required = false,
                maxItems = 8,
                description = "Inline annotation items (AOSP CL 4390685 A2uiProperty.nestedList)",
            )
    }
}
