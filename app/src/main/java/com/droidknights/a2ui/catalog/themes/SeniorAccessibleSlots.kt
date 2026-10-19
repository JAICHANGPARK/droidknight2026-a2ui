package com.droidknights.a2ui.catalog.themes

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Senior High-Contrast / Voice-Guided Accessibility Slot Overrides (`A2uiBasicCatalogV1.Card`, `Button`, `Text`).
 */
object SeniorHighContrastCardComponent : A2uiBasicCatalogV1.Card {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val childState = observeA2uiComponentState(childId)
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF9C3),
            border = BorderStroke(3.dp, Color(0xFF111827)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Surface(
                    color = Color(0xFF111827),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "시니어 큰글씨 · 고대비 음성 안내 모드",
                            color = Color(0xFFFACC15),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                        )
                    }
                }
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator()
                    is A2uiComponentState.Error ->
                        Text(childState.exception.message.orEmpty(), fontWeight = FontWeight.Bold)
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
        }
    }
}

object SeniorVoiceGuidedButtonComponent : A2uiBasicCatalogV1.Button {
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

        Column(modifier = modifier.fillMaxWidth()) {
            Button(
                onClick = { dispatchAction(currentAction) },
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color(0xFFFACC15),
                    ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFFFACC15),
                )
                Spacer(Modifier.width(8.dp))
                when (childState) {
                    is A2uiComponentState.Loading -> CircularProgressIndicator(Modifier.size(20.dp))
                    is A2uiComponentState.Error -> Text("버튼 오류")
                    is A2uiComponentState.Success -> A2uiComponent(component = childState.component)
                }
            }
            if (failedCheck != null) {
                Text(
                    text = "⚠️ ${failedCheck.message}",
                    color = Color(0xFFB91C1C),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

object SeniorLargeTextComponent : A2uiBasicCatalogV1.Text {
    @Composable
    override fun A2uiComponentScope.TypedContent(
        text: String,
        variant: A2uiBasicCatalogV1.Text.Variant,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val fontSize =
            when (variant) {
                A2uiBasicCatalogV1.Text.Variant.H1,
                A2uiBasicCatalogV1.Text.Variant.H2,
                A2uiBasicCatalogV1.Text.Variant.H3 -> 22.sp
                A2uiBasicCatalogV1.Text.Variant.H4,
                A2uiBasicCatalogV1.Text.Variant.H5 -> 19.sp
                A2uiBasicCatalogV1.Text.Variant.Caption -> 15.sp
                A2uiBasicCatalogV1.Text.Variant.Body -> 17.sp
            }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            lineHeight = (fontSize.value + 6).sp,
            modifier = modifier,
        )
    }
}
