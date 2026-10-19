package com.droidknights.a2ui.catalog.schema

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Schema Contract for `ActionGuardContainer` (Used in Domain A: On-Call Commander & Domain B: SmartLock 2FA).
 *
 * Responsibilities:
 * - Declares `A2uiProperty` schema (`child`, `guardedActionName`, `guardTitle`)
 * - Delegates to `TypedContent` which wraps `childId` in `ProvideActionInterceptor`
 */
interface ActionGuardContainerSchema : A2uiComponent {
    override val name: String
        get() = "ActionGuardContainer"

    override val description: String
        get() =
            "Wraps a child component and intercepts high-risk descendant actions using ProvideActionInterceptor for local 2FA approval."

    override val properties: List<A2uiProperty<*>>
        get() = listOf(childProperty, guardedActionNameProperty, guardTitleProperty)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val childId = properties[childProperty] ?: return
        val guardedName = properties[guardedActionNameProperty].orEmpty()
        val guardTitle =
            properties.bind(guardTitleProperty) ?: "로컬 보안 가드레일 (ProvideActionInterceptor)"

        TypedContent(
            childId = childId,
            guardedActionName = guardedName,
            guardTitle = guardTitle,
            modifier = modifier,
        )
    }

    @Composable
    fun A2uiComponentScope.TypedContent(
        childId: String,
        guardedActionName: String,
        guardTitle: String,
        modifier: Modifier,
    )

    companion object {
        val childProperty = A2uiProperty.componentId("child", required = true)
        val guardedActionNameProperty = A2uiProperty.string("guardedActionName", required = true)
        val guardTitleProperty = A2uiProperty.dynamicString("guardTitle", required = false)
    }
}
