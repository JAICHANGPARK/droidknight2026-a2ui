package com.droidknights.a2ui.catalog.schema

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier

/**
 * Schema Contract for `CircularClimateDial` (Used in Domain B: SmartHome/SDV Cockpit & Domain A: Traffic Weight).
 *
 * Responsibilities:
 * - Declares `A2uiProperty` schema (`label`, `unit`, `value`, `min`, `max`, `step`)
 * - Evaluates two-way DataModel binding (`bind` + `bindUpdater`)
 * - Enforces structural Read-Only mode when `bindUpdater == null`
 * - Reports runtime error (`A2uiRuntimeException`) if `min >= max`
 */
interface CircularClimateDialSchema : A2uiComponent {
    override val name: String
        get() = "CircularClimateDial"

    override val description: String
        get() =
            "A circular dial for controlling temperature, fan speed, or target threshold with two-way DataModel binding."

    override val properties: List<A2uiProperty<*>>
        get() =
            listOf(
                labelProperty,
                unitProperty,
                valueProperty,
                minProperty,
                maxProperty,
                stepProperty,
            )

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val label = properties.bind(labelProperty) ?: "Dial"
        val unit = properties[unitProperty] ?: "°C"
        val rawValue = properties.bind(valueProperty)?.toFloat() ?: 22f
        val min = (properties[minProperty] ?: 16.0).toFloat()
        val max = (properties[maxProperty] ?: 30.0).toFloat()
        val step = (properties[stepProperty] ?: 0.5).toFloat()
        val updater = properties.bindUpdater(valueProperty)

        if (min >= max) {
            SideEffect(min, max) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "CircularClimateDial min ($min) must be strictly less than max ($max)"
                    )
                )
            }
            return
        }

        TypedContent(
            label = label,
            unit = unit,
            rawValue = rawValue,
            min = min,
            max = max,
            step = step,
            onValueChange = updater,
            modifier = modifier,
        )
    }

    @Composable
    fun A2uiComponentScope.TypedContent(
        label: String,
        unit: String,
        rawValue: Float,
        min: Float,
        max: Float,
        step: Float,
        onValueChange: ((Number) -> Unit)?,
        modifier: Modifier,
    )

    companion object {
        val labelProperty =
            A2uiProperty.dynamicString("label", required = true, description = "Dial title")
        val unitProperty =
            A2uiProperty.string(
                "unit",
                required = false,
                description = "Unit suffix such as °C or %",
            )
        val valueProperty =
            A2uiProperty.dynamicNumber(
                "value",
                required = true,
                description = "Current numeric value",
            )
        val minProperty =
            A2uiProperty.number(
                "min",
                defaultValue = 16.0,
                description = "Minimum allowed value",
            )
        val maxProperty =
            A2uiProperty.number(
                "max",
                defaultValue = 30.0,
                description = "Maximum allowed value",
            )
        val stepProperty =
            A2uiProperty.number("step", defaultValue = 0.5, description = "Step increment")
    }
}
