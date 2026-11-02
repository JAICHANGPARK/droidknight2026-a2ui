package com.droidknights.a2ui.catalog.functions

import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicNumberSchema
import kotlin.math.roundToLong

/**
 * Custom A2UI Domain Validation Function (`call: "banknoteDenomination"`).
 *
 * Extends the A2UI Basic Catalog V1 functions (`numeric`, `required`, `regex`, `and`, `or`, `not`)
 * to validate real-world commercial bank foreign currency cash exchange rules:
 * 1. Foreign currency amount (`value`) must be at least the minimum exchange amount (`min`).
 * 2. Foreign currency amount (`value`) must be an exact multiple of the smallest paper banknote
 *    denomination (`unit`), rejecting coin units (e.g., 500 JPY coin when `unit = 1000`,
 *    1/2 EUR coins when `unit = 5`, or 5 CHF coin when `unit = 10`).
 */
class A2uiBanknoteDenominationFunction private constructor() : A2uiFunction {

    override val definition: A2uiFunctionDefinition
        get() = DEFINITION

    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any {
        val rawValue = (args[ARG_VALUE_KEY] as? Number)?.toDouble() ?: return false
        val min = (args[ARG_MIN_KEY] as? Number)?.toLong() ?: 1L
        val unit = (args[ARG_UNIT_KEY] as? Number)?.toLong() ?: 1L
        return typedExecute(
            value = rawValue.roundToLong(),
            min = min,
            unit = unit,
        )
    }

    fun typedExecute(
        value: Long,
        min: Long,
        unit: Long,
    ): Boolean {
        if (unit <= 0L || min <= 0L) return false
        if (value < min) return false
        return value % unit == 0L
    }

    companion object {
        private const val ARG_VALUE_KEY = "value"
        private const val ARG_MIN_KEY = "min"
        private const val ARG_UNIT_KEY = "unit"

        val DEFINITION: A2uiFunctionDefinition =
            object : A2uiFunctionDefinition {
                override val name: String = "banknoteDenomination"

                override val description: String =
                    "Checks that a foreign currency amount is at least 'min' and an exact multiple of the smallest paper banknote denomination 'unit' (excluding coins)."

                override val returnType: A2uiFunctionReturnType = A2uiFunctionReturnType.BOOLEAN

                override val argumentSchema: A2uiSchema =
                    A2uiObjectSchema(
                        properties =
                            mapOf(
                                ARG_VALUE_KEY to
                                    A2uiDynamicNumberSchema(
                                        description = "The foreign currency amount to validate."
                                    ),
                                ARG_MIN_KEY to
                                    A2uiNumberSchema(
                                        description = "Minimum allowed foreign currency exchange amount (e.g. 1000 for JPY, 10 for USD/EUR/CHF)."
                                    ),
                                ARG_UNIT_KEY to
                                    A2uiNumberSchema(
                                        description = "Smallest paper banknote denomination unit (e.g. 1000 for JPY, 5 for EUR, 10 for CHF, 1 for USD)."
                                    ),
                            ),
                        required = setOf(ARG_VALUE_KEY, ARG_MIN_KEY, ARG_UNIT_KEY),
                        isAdditionalPropertiesAllowed = false,
                    )
            }

        @JvmField
        val INSTANCE: A2uiBanknoteDenominationFunction = A2uiBanknoteDenominationFunction()
    }
}
