package com.droidknights.a2ui.catalog.components

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.schema.DomainCatalogSchemas
import kotlin.math.roundToInt

object CircularClimateDialComponent : DomainCatalogSchemas.CircularClimateDial {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String,
        unit: String,
        rawValue: Float,
        min: Float,
        max: Float,
        step: Float,
        onValueChange: ((Number) -> Unit)?,
        modifier: Modifier,
    ) {
        val isWritable = onValueChange != null
        val clamped = rawValue.coerceIn(min, max)
        val sweepFraction = ((clamped - min) / (max - min)).coerceIn(0f, 1f)
        val trackColor = MaterialTheme.colorScheme.outlineVariant
        val activeColor =
            if (isWritable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(64.dp)) {
                        val strokeWidth = 6.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = Size(size.width, size.height),
                        )
                        drawArc(
                            color = activeColor,
                            startAngle = 135f,
                            sweepAngle = 270f * sweepFraction,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = Size(size.width, size.height),
                        )
                    }
                    Text(
                        text = "${((clamped * 10).roundToInt() / 10.0)}$unit",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = if (isWritable) "범위 ${min.toInt()}~${max.toInt()}$unit" else "읽기 전용",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onValueChange?.invoke(
                                    ((clamped - step).coerceAtLeast(min) * 10).roundToInt() / 10.0
                                )
                            },
                            enabled = isWritable && clamped > min,
                        ) {
                            Text("- $step$unit")
                        }
                        Button(
                            onClick = {
                                onValueChange?.invoke(
                                    ((clamped + step).coerceAtMost(max) * 10).roundToInt() / 10.0
                                )
                            },
                            enabled = isWritable && clamped < max,
                        ) {
                            Text("+ $step$unit")
                        }
                    }
                }
            }
        }
    }
}
