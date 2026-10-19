package com.droidknights.a2ui.catalog.components

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.schema.DomainCatalogSchemas
import kotlin.math.roundToInt

object PowerBudgetBarComponent : DomainCatalogSchemas.PowerBudgetBar {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        title: String,
        usedKw: Float,
        maxKw: Float,
        detail: String?,
        isOverload: Boolean,
        modifier: Modifier,
    ) {
        val ratio = (usedKw / maxKw).coerceIn(0f, 1f)
        val barColor =
            if (isOverload || ratio > 0.85f) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text =
                            "${((usedKw * 10).roundToInt() / 10.0)} / $maxKw kW (${(ratio * 100).roundToInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = barColor,
                    )
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = barColor,
                )
                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
