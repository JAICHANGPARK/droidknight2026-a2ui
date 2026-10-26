package com.droidknights.a2ui.catalog.components

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.schema.DomainCatalogSchemas

object MetricSparklineComponent : DomainCatalogSchemas.MetricSparkline {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        metricName: String,
        currentValue: String,
        severity: String,
        points: List<Float>,
        thresholdSummary: String?,
        annotations: List<String>,
        modifier: Modifier,
    ) {
        val isCritical = severity.equals("critical", ignoreCase = true)
        val accentColor =
            if (isCritical) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metricName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = currentValue,
                            style = MaterialTheme.typography.titleMedium,
                            color = accentColor,
                        )
                    }
                    Canvas(modifier = Modifier.width(96.dp).height(32.dp)) {
                        val stepX = size.width / (points.size - 1)
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = accentColor,
                                start = Offset(i * stepX, size.height * (1f - points[i])),
                                end = Offset((i + 1) * stepX, size.height * (1f - points[i + 1])),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
                val metaParts = buildList {
                    if (!thresholdSummary.isNullOrBlank()) add(thresholdSummary)
                    addAll(annotations)
                }
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString("  |  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
