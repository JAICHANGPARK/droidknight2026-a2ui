package com.droidknights.a2ui.catalog.schema

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.DynamicA2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.commontypes.A2uiCheckRuleSchema
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
 * Also supports declarative A2UI schema constraints:
 * - `minAmount`: Minimum exchangeable foreign currency amount (e.g. `1000` for JPY, `10` for USD/EUR/CHF)
 * - `banknoteUnit`: Smallest paper banknote denomination unit (e.g. `1000` for JPY because 500 JPY is a coin, `5` for EUR, `10` for CHF, `1` for USD)
 * - `checks`: Standard A2UI `CheckRule` array (e.g. `banknoteDenomination` or `numeric` function checks)
 */
interface BidirectionalExchangeInputSchema : A2uiComponent {
    override val name: String
        get() = "BidirectionalExchangeInput"

    override val description: String
        get() =
            "Dual two-way currency converter text fields where typing either foreign currency or KRW automatically updates the other, with banknote denomination and minimum amount constraints."

    override val properties: List<A2uiProperty<*>>
        get() =
            listOf(
                currencyCodeProperty,
                currencyLabelProperty,
                exchangeRateProperty,
                unitScaleProperty,
                minAmountProperty,
                banknoteUnitProperty,
                foreignAmountProperty,
                krwAmountProperty,
                checksProperty,
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
        val minAmount =
            properties[minAmountProperty]?.toLong() ?: defaultMinAmountForCurrency(currencyCode)
        val banknoteUnit =
            properties[banknoteUnitProperty]?.toLong() ?: defaultBanknoteUnitForCurrency(currencyCode)
        val foreignAmount = properties.bind(foreignAmountProperty)?.toLong() ?: return
        val krwAmount = properties.bind(krwAmountProperty)?.toLong()
        val checks = properties.bind(checksProperty) ?: emptyList()
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

        if (minAmount <= 0L || banknoteUnit <= 0L) {
            SideEffect(minAmount, banknoteUnit) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "BidirectionalExchangeInput minAmount ($minAmount) and banknoteUnit ($banknoteUnit) must be positive",
                        mapOf("path" to minAmountProperty.key),
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
            minAmount = minAmount,
            banknoteUnit = banknoteUnit,
            foreignAmount = foreignAmount,
            krwAmount = effectiveKrw,
            checks = checks,
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
        minAmount: Long,
        banknoteUnit: Long,
        foreignAmount: Long,
        krwAmount: Long,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        onForeignAmountChange: ((Number) -> Unit)?,
        onKrwAmountChange: ((Number) -> Unit)?,
        modifier: Modifier,
    )

    companion object {
        val currencyCodeProperty =
            A2uiProperty.string("currencyCode", required = true, description = "ISO currency code e.g. JPY, USD, EUR, CHF")
        val currencyLabelProperty =
            A2uiProperty.string("currencyLabel", required = false, description = "Display label for foreign currency field")
        val exchangeRateProperty =
            A2uiProperty.number("exchangeRate", defaultValue = 847.88, description = "KRW exchange rate per unitScale")
        val unitScaleProperty =
            A2uiProperty.number("unitScale", defaultValue = 100.0, description = "100 for JPY, 1 for USD/EUR/CHF")
        val minAmountProperty =
            A2uiProperty.number(
                key = "minAmount",
                required = false,
                description = "Minimum exchangeable foreign currency amount (e.g. 1000 for JPY, 10 for USD/EUR/CHF).",
            )
        val banknoteUnitProperty =
            A2uiProperty.number(
                key = "banknoteUnit",
                required = false,
                description = "Smallest paper banknote denomination unit (e.g. 1000 for JPY since 500 JPY is a coin, 5 for EUR, 10 for CHF, 1 for USD).",
            )
        val foreignAmountProperty =
            A2uiProperty.dynamicNumber("foreignAmount", required = true, description = "Foreign currency amount")
        val krwAmountProperty =
            A2uiProperty.dynamicNumber("krwAmount", required = false, description = "Calculated KRW amount")

        val checksProperty: DynamicA2uiProperty<List<A2uiBasicCatalogV1.CheckRule>> =
            A2uiProperty.dynamicCustom(
                key = "checks",
                schema =
                    A2uiArraySchema(
                        items = A2uiCheckRuleSchema.DEFAULT_INSTANCE,
                        description =
                            "A list of checks to perform on the exchange amount (e.g. banknoteDenomination or numeric).",
                    ),
                safeCast = { value ->
                    when (value) {
                        is List<*> ->
                            buildList(value.size) {
                                for (i in value.indices) {
                                    when (val item = value[i]) {
                                        is A2uiBasicCatalogV1.CheckRule -> add(item)
                                        is Map<*, *> -> {
                                            val condition =
                                                item["condition"] as? Boolean ?: return@dynamicCustom null
                                            val message =
                                                item["message"]?.toString() ?: return@dynamicCustom null
                                            add(
                                                A2uiBasicCatalogV1.CheckRule(
                                                    condition = condition,
                                                    message = message,
                                                )
                                            )
                                        }
                                        else -> return@dynamicCustom null
                                    }
                                }
                            }
                        else -> null
                    }
                },
            )

        fun defaultBanknoteUnitForCurrency(currencyCode: String): Long =
            when (currencyCode.uppercase()) {
                "JPY" -> 1_000L
                "EUR" -> 5L
                "CHF" -> 10L
                else -> 1L
            }

        fun defaultMinAmountForCurrency(currencyCode: String): Long =
            when (currencyCode.uppercase()) {
                "JPY" -> 1_000L
                "USD" -> 10L
                "EUR" -> 10L
                "CHF" -> 10L
                else -> 1_000L
            }
    }
}
