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
 * Schema Contract for `PowerBudgetBar` (Used in Domain B: EV V2L Power Budget & Domain A: K8s Cluster Memory).
 *
 * Responsibilities:
 * - Declares `A2uiProperty` schema (`title`, `usedKw`, `maxKw`, `detail`)
 * - Evaluates reactive DataModel bindings
 * - Reports runtime error (`A2uiRuntimeException`) when `usedKw > maxKw`
 */
interface PowerBudgetBarSchema : A2uiComponent {
    override val name: String
        get() = "PowerBudgetBar"

    override val description: String
        get() = "Displays real-time power or resource consumption against a maximum budget."

    override val properties: List<A2uiProperty<*>>
        get() = listOf(titleProperty, usedProperty, maxProperty, detailProperty)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(titleProperty) != null &&
            properties.bind(usedProperty) != null &&
            properties.bind(maxProperty) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val title = properties.bind(titleProperty) ?: return
        val used = properties.bind(usedProperty)?.toFloat() ?: return
        val max = (properties.bind(maxProperty)?.toFloat() ?: return).coerceAtLeast(0.1f)
        val detail = properties.bind(detailProperty)
        val isOverload = used > max

        if (isOverload) {
            SideEffect(used, max) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "PowerBudgetBar overload: usedKw ($used) exceeds maxKw ($max)",
                        mapOf("path" to usedProperty.key),
                    )
                )
            }
        }

        TypedContent(
            title = title,
            usedKw = used,
            maxKw = max,
            detail = detail,
            isOverload = isOverload,
            modifier = modifier,
        )
    }

    @Composable
    fun A2uiComponentScope.TypedContent(
        title: String,
        usedKw: Float,
        maxKw: Float,
        detail: String?,
        isOverload: Boolean,
        modifier: Modifier,
    )

    companion object {
        val titleProperty = A2uiProperty.dynamicString("title", required = true)
        val usedProperty = A2uiProperty.dynamicNumber("usedKw", required = true)
        val maxProperty = A2uiProperty.dynamicNumber("maxKw", required = true)
        val detailProperty = A2uiProperty.dynamicString("detail", required = false)
    }
}
