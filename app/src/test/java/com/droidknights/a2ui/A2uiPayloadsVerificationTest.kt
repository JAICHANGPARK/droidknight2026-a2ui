package com.droidknights.a2ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.processor.A2uiJsonMessageParser
import androidx.a2ui.model.processor.A2uiJsonReader
import androidx.a2ui.model.processor.A2uiJsonToken
import androidx.a2ui.model.processor.A2uiMessageParser
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientCapabilities
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import com.droidknights.a2ui.labs.buildCatalogComparisonJsonl
import com.droidknights.a2ui.labs.buildInitialHealthySurfaceJsonl
import com.droidknights.a2ui.scenarios.buildCockpitJsonl
import com.droidknights.a2ui.scenarios.buildDiagnosticsWizardJsonl
import com.droidknights.a2ui.scenarios.buildIncidentJsonl
import com.droidknights.a2ui.scenarios.buildMashupJsonl
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM end-to-end verification test that parses and feeds EVERY JSONL payload across all 4
 * Domain Scenarios and all Official Feature Labs through the real `androidx.a2ui` (`1.0.0-alpha01`)
 * `A2uiJsonMessageParser` and `A2uiMessageProcessor` to guarantee zero schema or syntax errors.
 */
class A2uiPayloadsVerificationTest {

    private fun createJvmParser(): A2uiMessageParser<String> =
        A2uiJsonMessageParser { jsonString ->
            val reader = JsonReader(StringReader(jsonString))
            object : A2uiJsonReader {
                override fun peek(): A2uiJsonToken =
                    when (reader.peek()) {
                        JsonToken.BEGIN_ARRAY -> A2uiJsonToken.BEGIN_ARRAY
                        JsonToken.END_ARRAY -> A2uiJsonToken.END_ARRAY
                        JsonToken.BEGIN_OBJECT -> A2uiJsonToken.BEGIN_OBJECT
                        JsonToken.END_OBJECT -> A2uiJsonToken.END_OBJECT
                        JsonToken.NAME -> A2uiJsonToken.NAME
                        JsonToken.STRING -> A2uiJsonToken.STRING
                        JsonToken.NUMBER -> A2uiJsonToken.NUMBER
                        JsonToken.BOOLEAN -> A2uiJsonToken.BOOLEAN
                        JsonToken.NULL -> A2uiJsonToken.NULL
                        JsonToken.END_DOCUMENT -> A2uiJsonToken.END_DOCUMENT
                    }

                override fun beginObject() = reader.beginObject()
                override fun endObject() = reader.endObject()
                override fun beginArray() = reader.beginArray()
                override fun endArray() = reader.endArray()
                override fun hasNext(): Boolean = reader.hasNext()
                override fun nextName(): String = reader.nextName()
                override fun nextString(): String = reader.nextString()
                override fun nextBoolean(): Boolean = reader.nextBoolean()
                override fun nextDouble(): Double = reader.nextDouble()
                override fun nextInt(): Int = reader.nextInt()
                override fun nextLong(): Long = reader.nextLong()
                override fun nextNull() = reader.nextNull()
                override fun skipValue() = reader.skipValue()
                override fun close() = reader.close()
            }
        }

    private fun verifyStreamHasZeroErrors(
        streamName: String,
        catalogs: List<A2uiCatalog>,
        lines: List<String>,
    ) = runBlocking {
        val parser = createJvmParser()
        val processor = A2uiMessageProcessor(catalogs = catalogs)
        val errors = mutableListOf<A2uiClientErrorMessage>()

        val job = launch {
            launch { processor.collectMessages() }
            launch {
                processor.outboundEvents.collect { msg ->
                    if (msg is A2uiClientErrorMessage) {
                        errors.add(msg)
                    }
                }
            }
        }

        delay(30)
        lines.forEachIndexed { idx, line ->
            // 1. Direct parse check
            try {
                parser.parse(line)
            } catch (e: Throwable) {
                throw AssertionError("Failed to parse line #$idx in $streamName: $line", e)
            }
            // 2. Full processor pipeline check (schema validation + data model update)
            processor.processInput(parser, line)
        }
        delay(80)

        assertTrue(
            "Expected 0 errors in $streamName, but got: $errors",
            errors.isEmpty(),
        )
        assertEquals(
            "Expected 1 active surface in $streamName",
            1,
            processor.activeSurfaces.value.size,
        )
        job.cancel()
    }

    @Test
    fun verifyDomainA_IncidentCommander_allCasesAndModes() {
        val catalog = DroidKnightsCatalogs.createStandardCatalog()
        for (incidentIdx in 0..2) {
            for (readOnly in listOf(false, true)) {
                verifyStreamHasZeroErrors(
                    streamName = "IncidentCommander(incident=$incidentIdx, readOnly=$readOnly)",
                    catalogs = listOf(catalog),
                    lines = buildIncidentJsonl(incidentIdx, readOnly),
                )
            }
        }
    }

    @Test
    fun verifyDomainB_ContextualCockpit_allRoutines() {
        val catalog = DroidKnightsCatalogs.createStandardCatalog()
        for (routineIdx in 0..2) {
            verifyStreamHasZeroErrors(
                streamName = "ContextualCockpit(routine=$routineIdx)",
                catalogs = listOf(catalog),
                lines = buildCockpitJsonl(routineIdx),
            )
        }
    }

    @Test
    fun verifyDomainC_DynamicMashupSheet_allCases() {
        val catalog = DroidKnightsCatalogs.createStandardCatalog()
        for (caseIdx in 0..2) {
            verifyStreamHasZeroErrors(
                streamName = "DynamicMashupSheet(case=$caseIdx)",
                catalogs = listOf(catalog),
                lines = buildMashupJsonl(caseIdx),
            )
        }
    }

    @Test
    fun verifyDomainD_FieldDiagnosticsWizard_allSteps() {
        val catalog = DroidKnightsCatalogs.createStandardCatalog()
        for (step in 1..3) {
            val (initialLines, delayedLine) = buildDiagnosticsWizardJsonl(step, delayHotspotData = false)
            val allLines = if (delayedLine != null) initialLines + delayedLine else initialLines
            verifyStreamHasZeroErrors(
                streamName = "FieldDiagnosticsWizard(step=$step)",
                catalogs = listOf(catalog),
                lines = allLines,
            )
        }
        // Also test delayed progressive data streaming on Step 1
        val (delayedInit, delayedFollowup) = buildDiagnosticsWizardJsonl(1, delayHotspotData = true)
        verifyStreamHasZeroErrors(
            streamName = "FieldDiagnosticsWizard(step=1, delayed=true)",
            catalogs = listOf(catalog),
            lines = delayedInit + listOfNotNull(delayedFollowup),
        )
    }

    @Test
    fun verifyLab1_CatalogEvolution_allSkinsAndVersions() {
        val (v1, v2) = DroidKnightsCatalogs.createVersionedCatalogPair()
        val skins =
            listOf(
                DroidKnightsCatalogs.createStandardCatalog(DroidKnightsCatalogs.CATALOG_ID_V2),
                DroidKnightsCatalogs.createBrandDesignSystemCatalog(DroidKnightsCatalogs.CATALOG_ID_V2),
                DroidKnightsCatalogs.createSeniorAccessibleCatalog(DroidKnightsCatalogs.CATALOG_ID_V2),
            )
        skins.forEachIndexed { skinIdx, skinCatalog ->
            verifyStreamHasZeroErrors(
                streamName = "CatalogEvolutionLab(skin=$skinIdx, V2=true)",
                catalogs = listOf(v1, skinCatalog),
                lines = buildCatalogComparisonJsonl(DroidKnightsCatalogs.CATALOG_ID_V2, includeV2Widgets = true),
            )
        }
        verifyStreamHasZeroErrors(
            streamName = "CatalogEvolutionLab(V1=true)",
            catalogs = listOf(v1, v2),
            lines = buildCatalogComparisonJsonl(DroidKnightsCatalogs.CATALOG_ID_V1, includeV2Widgets = false),
        )

        // Verify schema serialization & capabilities
        val schemaStr = v2.toJsonSchemaString()
        assertTrue(schemaStr.contains("MetricSparkline"))
        val caps = A2uiClientCapabilities(listOf(v1.id, v2.id)).toPayloadMap()
        assertTrue(caps.containsKey("a2uiClientCapabilities"))
    }

    @Test
    fun verifyLab2_ChaosMonkey_healthyAndHallucinationErrorReporting() = runBlocking {
        val catalog = DroidKnightsCatalogs.createStandardCatalog()
        // 1. Healthy initial state has 0 errors
        verifyStreamHasZeroErrors(
            streamName = "ChaosMonkeyInitialHealthy",
            catalogs = listOf(catalog),
            lines = buildInitialHealthySurfaceJsonl(),
        )

        // 2. Hallucinated component triggers VALIDATION_FAILED outbound error
        val parser = createJvmParser()
        val processor = A2uiMessageProcessor(catalogs = listOf(catalog))
        val errors = mutableListOf<A2uiClientErrorMessage>()
        val job = launch {
            launch { processor.collectMessages() }
            launch {
                processor.outboundEvents.collect { msg ->
                    if (msg is A2uiClientErrorMessage) errors.add(msg)
                }
            }
        }
        delay(30)
        buildInitialHealthySurfaceJsonl().forEach { processor.processInput(parser, it) }
        val hallucination =
            """{"version":"v0.9.1","updateComponents":{"surfaceId":"chaos_surface","components":[{"id":"slot_widget","component":"Hallucinated3DChart","title":"존재하지 않는 위젯"}]}}"""
        processor.processInput(parser, hallucination)
        delay(80)

        assertEquals(1, errors.size)
        assertEquals("VALIDATION_FAILED", errors.first().code)
        job.cancel()
    }
}
