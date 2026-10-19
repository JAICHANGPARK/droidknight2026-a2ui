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

        TypedContent(
            metricName = metricName,
            currentValue = currentValue,
            severity = severity,
            points = points,
            modifier = modifier,
        )
    }

    @Composable
    fun A2uiComponentScope.TypedContent(
        metricName: String,
        currentValue: String,
        severity: String,
        points: List<Float>,
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
    }
}
