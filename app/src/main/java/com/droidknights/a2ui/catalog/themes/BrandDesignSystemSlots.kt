package com.droidknights.a2ui.catalog.themes

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Brand Design System Slot Overrides (`A2uiBasicCatalogV1.Card`, `Button`, `Text`).
 *
 * Separates the A2UI protocol schema contract (`A2uiBasicCatalogV1`) from the custom company
 * design system (Squircle Surface, Gradient Button, Brand Typography).
 */
object BrandSquircleCardComponent : A2uiBasicCatalogV1.Card {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val childState = observeA2uiComponentState(childId)
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF0F7FF),
            border = BorderStroke(2.dp, Color(0xFF2563EB)),
            shadowElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "BRAND DESIGN SYSTEM · SQUIRCLE SURFACE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D4ED8),
                        fontFamily = FontFamily.Monospace,
                    )
                }
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator(Modifier.size(24.dp))
                    is A2uiComponentState.Error ->
                        Text(childState.exception.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
        }
    }
}

object BrandGradientButtonComponent : A2uiBasicCatalogV1.Button {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        variant: A2uiBasicCatalogV1.Button.Variant,
        action: Map<String, Any?>,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {
        val currentAction by rememberUpdatedState(action)
        val childState = observeA2uiComponentState(childId)
        val failedCheck = checks.firstOrNull { !it.condition }
        val enabled = childState is A2uiComponentState.Success && failedCheck == null

        Column(modifier = modifier) {
            Box(
                modifier =
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .background(
                            if (enabled) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                                )
                            }
                        )
                        .clickable(enabled = enabled) { dispatchAction(currentAction) }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (childState) {
                    is A2uiComponentState.Loading ->
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                    is A2uiComponentState.Error -> Text("오류", color = Color.White)
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
            if (failedCheck != null) {
                Text(
                    text = failedCheck.message,
                    color = Color(0xFFDC2626),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

object BrandTypographyTextComponent : A2uiBasicCatalogV1.Text {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val style =
            when (variant) {
                A2uiBasicCatalogV1.Text.Variant.H1,
                A2uiBasicCatalogV1.Text.Variant.H2,
                A2uiBasicCatalogV1.Text.Variant.H3 ->
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E3A8A),
                        letterSpacing = (-0.5).sp,
                    )
                A2uiBasicCatalogV1.Text.Variant.H4,
                A2uiBasicCatalogV1.Text.Variant.H5 ->
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                    )
                A2uiBasicCatalogV1.Text.Variant.Caption ->
                    MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF475569),
                    )
                A2uiBasicCatalogV1.Text.Variant.Body ->
                    MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Medium,
                    )
            }
        Text(text = text, style = style, modifier = modifier)
    }
}
