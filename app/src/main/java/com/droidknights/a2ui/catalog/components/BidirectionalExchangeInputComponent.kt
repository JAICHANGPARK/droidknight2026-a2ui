package com.droidknights.a2ui.catalog.components

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.droidknights.a2ui.catalog.schema.DomainCatalogSchemas
import com.droidknights.a2ui.common.LocalBankingAppState
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object BidirectionalExchangeInputComponent : DomainCatalogSchemas.BidirectionalExchangeInput {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        currencyCode: String,
        currencyLabel: String,
        exchangeRate: Double,
        unitScale: Double,
        foreignAmount: Long,
        krwAmount: Long,
        onForeignAmountChange: ((Number) -> Unit)?,
        onKrwAmountChange: ((Number) -> Unit)?,
        modifier: Modifier,
    ) {
        val bankingState = LocalBankingAppState.current
        val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
        var foreignText by remember { mutableStateOf(foreignAmount.toString()) }
        var krwText by remember { mutableStateOf(krwAmount.toString()) }
        var editingSide by remember { mutableStateOf<String?>(null) }

        val currentKrw = krwText.toLongOrNull() ?: krwAmount
        val isSellBackMode = currencyLabel.contains("재환전")
        val isOverBalance = !isSellBackMode && currentKrw > bankingState.mainAccountBalance

        LaunchedEffect(foreignAmount) {
            if (editingSide != "FOREIGN") {
                foreignText = foreignAmount.toString()
            }
        }

        LaunchedEffect(krwAmount) {
            if (editingSide != "KRW") {
                krwText = krwAmount.toString()
            }
        }

        fun updateFromForeign(newForeign: Long) {
            val calcKrw = ((newForeign.toDouble() / unitScale) * exchangeRate).roundToLong()
            foreignText = newForeign.toString()
            krwText = calcKrw.toString()
            onForeignAmountChange?.invoke(newForeign)
            onKrwAmountChange?.invoke(calcKrw)
        }

        val scaleText =
            if (unitScale == 100.0) "100 $currencyCode = $exchangeRate 원"
            else "1 $currencyCode = $exchangeRate 원"

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currencyLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = scaleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }

                OutlinedTextField(
                    value = foreignText,
                    onValueChange = { raw ->
                        editingSide = "FOREIGN"
                        val digits = raw.filter { it.isDigit() }.take(10)
                        foreignText = digits
                        val parsed = digits.toLongOrNull() ?: 0L
                        val calcKrw = ((parsed.toDouble() / unitScale) * exchangeRate).roundToLong()
                        krwText = calcKrw.toString()
                        onForeignAmountChange?.invoke(parsed)
                        onKrwAmountChange?.invoke(calcKrw)
                    },
                    isError = isOverBalance,
                    label = { Text("받으실 외화 ($currencyCode)") },
                    suffix = {
                        Text(
                            text = "${numberFormat.format(foreignText.toLongOrNull() ?: 0L)} $currencyCode",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = krwText,
                    onValueChange = { raw ->
                        editingSide = "KRW"
                        val digits = raw.filter { it.isDigit() }.take(10)
                        krwText = digits
                        val parsedKrw = digits.toLongOrNull() ?: 0L
                        val calcForeign = ((parsedKrw.toDouble() / exchangeRate) * unitScale).roundToLong()
                        foreignText = calcForeign.toString()
                        onKrwAmountChange?.invoke(parsedKrw)
                        onForeignAmountChange?.invoke(calcForeign)
                    },
                    isError = isOverBalance,
                    label = { Text("출금될 원화 (KRW)") },
                    suffix = {
                        Text(
                            text = "약 ${numberFormat.format(krwText.toLongOrNull() ?: 0L)} 원",
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                if (isOverBalance) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isOverBalance) {
                    val shortfall = currentKrw - bankingState.mainAccountBalance
                    Text(
                        text =
                            "🚫 출금 가능 잔액(${numberFormat.format(bankingState.mainAccountBalance)}원)을 초과했습니다 · 부족 금액: ${numberFormat.format(shortfall)}원",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (!isSellBackMode) {
                    Text(
                        text =
                            "💳 출금 가능 잔액: ${numberFormat.format(bankingState.mainAccountBalance)}원 (환전 후 예상 잔액: ${numberFormat.format((bankingState.mainAccountBalance - currentKrw).coerceAtLeast(0L))}원)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val presets =
                    if (unitScale == 100.0) listOf(30000L, 50000L, 100000L)
                    else listOf(500L, 1000L, 2000L)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        SuggestionChip(
                            onClick = {
                                editingSide = null
                                updateFromForeign(preset)
                            },
                            label = {
                                Text(
                                    text = "${numberFormat.format(preset)} $currencyCode",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
