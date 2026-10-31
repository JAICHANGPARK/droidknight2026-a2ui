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
 * Schema Contract for `BidirectionalExchangeInput` (Used in Foreign Currency Exchange Domain).
 *
 * Provides two synchronized text input boxes:
 * - Foreign currency amount (`foreignAmount`)
 * - KRW amount (`krwAmount`)
 *
 * Typing into the foreign currency box automatically calculates and updates KRW,
 * and typing into the KRW box automatically calculates and updates the foreign currency amount.
 */
interface BidirectionalExchangeInputSchema : A2uiComponent {
    override val name: String
        get() = "BidirectionalExchangeInput"

    override val description: String
        get() =
            "Dual two-way currency converter text fields where typing either foreign currency or KRW automatically updates the other."

    override val properties: List<A2uiProperty<*>>
        get() =
            listOf(
                currencyCodeProperty,
                currencyLabelProperty,
                exchangeRateProperty,
                unitScaleProperty,
                foreignAmountProperty,
                krwAmountProperty,
            )

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties[currencyCodeProperty] != null &&
            properties.bind(foreignAmountProperty) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val currencyCode = properties[currencyCodeProperty] ?: return
        val currencyLabel = properties[currencyLabelProperty] ?: "환전할 외화"
        val exchangeRate = (properties[exchangeRateProperty] ?: 847.88).toDouble()
        val unitScale = (properties[unitScaleProperty] ?: 100.0).toDouble()
        val foreignAmount = properties.bind(foreignAmountProperty)?.toLong() ?: return
        val krwAmount = properties.bind(krwAmountProperty)?.toLong()
        val foreignUpdater = properties.bindUpdater(foreignAmountProperty)
        val krwUpdater = properties.bindUpdater(krwAmountProperty)

        if (exchangeRate <= 0.0 || unitScale <= 0.0) {
            SideEffect(exchangeRate, unitScale) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "BidirectionalExchangeInput exchangeRate ($exchangeRate) and unitScale ($unitScale) must be positive",
                        mapOf("path" to exchangeRateProperty.key),
                    )
                )
            }
            return
        }

        val effectiveKrw =
            krwAmount ?: ((foreignAmount.toDouble() / unitScale) * exchangeRate).toLong()

        TypedContent(
            currencyCode = currencyCode,
            currencyLabel = currencyLabel,
            exchangeRate = exchangeRate,
            unitScale = unitScale,
            foreignAmount = foreignAmount,
            krwAmount = effectiveKrw,
            onForeignAmountChange = foreignUpdater,
            onKrwAmountChange = krwUpdater,
            modifier = modifier,
        )
    }

    @Composable
    fun A2uiComponentScope.TypedContent(
        currencyCode: String,
        currencyLabel: String,
        exchangeRate: Double,
        unitScale: Double,
        foreignAmount: Long,
        krwAmount: Long,
        onForeignAmountChange: ((Number) -> Unit)?,
        onKrwAmountChange: ((Number) -> Unit)?,
        modifier: Modifier,
    )

    companion object {
        val currencyCodeProperty =
            A2uiProperty.string("currencyCode", required = true, description = "ISO currency code e.g. JPY, USD, EUR")
        val currencyLabelProperty =
            A2uiProperty.string("currencyLabel", required = false, description = "Display label for foreign currency field")
        val exchangeRateProperty =
            A2uiProperty.number("exchangeRate", defaultValue = 847.88, description = "KRW exchange rate per unitScale")
        val unitScaleProperty =
            A2uiProperty.number("unitScale", defaultValue = 100.0, description = "100 for JPY, 1 for USD/EUR/CHF")
        val foreignAmountProperty =
            A2uiProperty.dynamicNumber("foreignAmount", required = true, description = "Foreign currency amount")
        val krwAmountProperty =
            A2uiProperty.dynamicNumber("krwAmount", required = false, description = "Calculated KRW amount")
    }
}
