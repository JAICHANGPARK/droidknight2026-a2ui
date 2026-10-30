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
import com.droidknights.a2ui.common.A2uiSpec10Bridge
import com.droidknights.a2ui.labs.buildCatalogComparisonJsonl
import com.droidknights.a2ui.labs.buildInitialHealthySurfaceJsonl
import com.droidknights.a2ui.labs.buildSpec10SampleJsonl
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
        val catalog = com.droidknights.a2ui.catalog.domains.IncidentCommanderCatalog.create()
        for (incidentIdx in 0..4) {
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
        val catalog = com.droidknights.a2ui.catalog.domains.ContextualCockpitCatalog.create()
        for (routineIdx in 0..4) {
            verifyStreamHasZeroErrors(
                streamName = "ContextualCockpit(routine=$routineIdx)",
                catalogs = listOf(catalog),
                lines = buildCockpitJsonl(routineIdx),
            )
        }
    }

    @Test
    fun verifyDomainC_DynamicMashupSheet_allCases() {
        val catalog = com.droidknights.a2ui.catalog.domains.DynamicMashupCatalog.create()
        for (caseIdx in 0..4) {
            verifyStreamHasZeroErrors(
                streamName = "DynamicMashupSheet(case=$caseIdx)",
                catalogs = listOf(catalog),
                lines = buildMashupJsonl(caseIdx),
            )
        }
    }

    @Test
    fun verifyDomainD_FieldDiagnosticsWizard_allSteps() {
        val catalog = com.droidknights.a2ui.catalog.domains.FieldDiagnosticsCatalog.create()
        for (step in 1..5) {
            val (initialLines, delayedLines) = buildDiagnosticsWizardJsonl(step, delayHotspotData = false)
            val allLines = initialLines + delayedLines
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
            lines = delayedInit + delayedFollowup,
        )
    }

    @Test
    fun verifyDomainCurrencyExchange_allCases() {
        val catalog = com.droidknights.a2ui.catalog.domains.CurrencyExchangeCatalog.create()
        for (caseIdx in 0..4) {
            verifyStreamHasZeroErrors(
                streamName = "CurrencyExchange(case=$caseIdx)",
                catalogs = listOf(catalog),
                lines = com.droidknights.a2ui.scenarios.buildCurrencyExchangeJsonl(caseIdx),
            )
        }
    }

    @Test
    fun verifyAllDomains_FollowUpAndFinalActionLoops() {
        val dummyEvent =
            androidx.a2ui.model.protocol.A2uiClientEventMessage(
                surfaceId = "exchange_surface",
                componentId = "btn_submit",
                timestamp = 1729140000000L,
                type = "submit_exchange",
                context =
                    mapOf(
                        "currency" to "JPY",
                        "foreignAmount" to 50000,
                        "krwAmount" to 452060,
                        "branch" to listOf("ICN_T1"),
                        "incidentId" to "INC-8821",
                        "backupWeight" to 80,
                        "targetTemp" to 23.5,
                        "mashupType" to "JAPAN_TRIP_COMBO",
                        "visitDate" to "2026-10-16T14:00:00Z",
                    ),
            )

        // 1. Exchange Stage 1 -> Stage 2 -> Stage 3
        val exchangeCatalog = com.droidknights.a2ui.catalog.domains.CurrencyExchangeCatalog.create()
        val exchangeStage1 = com.droidknights.a2ui.scenarios.buildCurrencyExchangeJsonl(0)
        val (_, exchangeStage2) = com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads.buildFollowUpVoucherStepJsonl(dummyEvent)
        val (_, exchangeStage3) = com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads.buildFinalVoucherIssuedJsonl(dummyEvent)
        verifyStreamHasZeroErrors("Exchange_3StageLoop", listOf(exchangeCatalog), exchangeStage1 + exchangeStage2 + exchangeStage3)

        // 2. Incident Stage 1 -> Stage 2 -> Stage 3
        val incCatalog = com.droidknights.a2ui.catalog.domains.IncidentCommanderCatalog.create()
        val incStage1 = buildIncidentJsonl(0, false)
        val (_, incStage2) = com.droidknights.a2ui.scenarios.payloads.IncidentCommanderPayloads.buildFollowUpVerificationJsonl(dummyEvent)
        val (_, incStage3) = com.droidknights.a2ui.scenarios.payloads.IncidentCommanderPayloads.buildFinalResolvedJsonl(dummyEvent)
        verifyStreamHasZeroErrors("Incident_3StageLoop", listOf(incCatalog), incStage1 + incStage2 + incStage3)

        // 3. Cockpit Stage 1 -> Stage 2 -> Stage 3
        val ckCatalog = com.droidknights.a2ui.catalog.domains.ContextualCockpitCatalog.create()
        val ckStage1 = buildCockpitJsonl(0)
        val (_, ckStage2) = com.droidknights.a2ui.scenarios.payloads.ContextualCockpitPayloads.buildFollowUpAutomationJsonl(dummyEvent)
        val (_, ckStage3) = com.droidknights.a2ui.scenarios.payloads.ContextualCockpitPayloads.buildFinalCockpitDoneJsonl(dummyEvent)
        verifyStreamHasZeroErrors("Cockpit_3StageLoop", listOf(ckCatalog), ckStage1 + ckStage2 + ckStage3)

        // 4. Mashup Stage 1 -> Stage 2 -> Stage 3
        val msCatalog = com.droidknights.a2ui.catalog.domains.DynamicMashupCatalog.create()
        val msStage1 = buildMashupJsonl(0)
        val (_, msStage2) = com.droidknights.a2ui.scenarios.payloads.DynamicMashupPayloads.buildFollowUpMashupStepJsonl(dummyEvent)
        val (_, msStage3) = com.droidknights.a2ui.scenarios.payloads.DynamicMashupPayloads.buildFinalMashupDoneJsonl(dummyEvent)
        verifyStreamHasZeroErrors("Mashup_3StageLoop", listOf(msCatalog), msStage1 + msStage2 + msStage3)

        // 5. Diagnostics Stage 1 -> Stage 2 -> Stage 3
        val dgCatalog = com.droidknights.a2ui.catalog.domains.FieldDiagnosticsCatalog.create()
        val (dgStage1, _) = buildDiagnosticsWizardJsonl(3, false)
        val (_, dgStage2) = com.droidknights.a2ui.scenarios.payloads.FieldDiagnosticsPayloads.buildFollowUpDiagnosticsStepJsonl(dummyEvent)
        val (_, dgStage3) = com.droidknights.a2ui.scenarios.payloads.FieldDiagnosticsPayloads.buildFinalDiagnosticsDoneJsonl(dummyEvent)
        verifyStreamHasZeroErrors("Diagnostics_3StageLoop", listOf(dgCatalog), dgStage1 + dgStage2 + dgStage3)
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

    /**
     * Verifies the 3 post-alpha01 commits in `1.0.0-SNAPSHOT` (`androidx-main`):
     * 1. `v0_9_1` -> `v0_9` Basic Catalog ID Alias resolution in `A2uiCoreSurfaceActor` (`343aaa87431`)
     * 2. `A2uiProperty` list `minItems` / `maxItems` bounds validation (`8b873c31483`)
     * 3. Sorted `required` array in `A2uiObjectSchema` JSON serialization (`39da586fc41`)
     */
    @Test
    fun verifySnapshotNewFeatures_aliasAndListBoundsAndSortedSchema() {
        // 1. Catalog registered with v0_9 ID accepts createSurface with v0_9_1 catalogId URI
        val catalogV09 = DroidKnightsCatalogs.createStandardCatalog(DroidKnightsCatalogs.BASIC_CATALOG_ID)
        val aliasStream =
            listOf(
                """{"version":"v0.9.1","createSurface":{"surfaceId":"alias_test","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID_V0_9_1_ALIAS}","sendDataModel":true}}""",
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"alias_test","components":[{"id":"root","component":"Text","text":"Resolved via v0.9.1 alias"}]}}""",
            )
        verifyStreamHasZeroErrors(
            streamName = "SnapshotFeature_CatalogIdAlias",
            catalogs = listOf(catalogV09),
            lines = aliasStream,
        )

        // 2. A2uiProperty.numberList throws IllegalArgumentException when minItems > maxItems or negative
        var threwOnInvertedBounds = false
        try {
            androidx.a2ui.compose.runtime.A2uiProperty.numberList(
                key = "invalidBounds",
                minItems = 10,
                maxItems = 2,
            )
        } catch (_: IllegalArgumentException) {
            threwOnInvertedBounds = true
        }
        assertTrue(
            "Expected IllegalArgumentException when minItems > maxItems in 1.0.0-SNAPSHOT",
            threwOnInvertedBounds,
        )

        // 3. Sorted required properties in generated JSON schema (even though metricName was declared before currentValue)
        val compactSchemaJson = catalogV09.toJsonSchemaString().replace("\\s+".toRegex(), "")
        val sparklineSection = compactSchemaJson.substringAfter("\"MetricSparkline\"")
        val requiredArray =
            sparklineSection
                .substringAfter("\"threshold\"")
                .substringAfter("\"required\":[")
                .substringBefore("]")
        assertTrue(
            "Expected sorted required properties where currentValue precedes metricName, got: $requiredArray",
            requiredArray.indexOf("\"currentValue\"") in 0 until requiredArray.indexOf("\"metricName\""),
        )
    }

    @Test
    fun verifyLab0_Spec10Bridge_desugaringAndExecution() {
        val catalog = DroidKnightsCatalogs.createStandardCatalog()

        // 1. Verify that pure Spec 1.0 JSONL directly fed to official v0.9.1 engine throws Unsupported protocol version
        val rawSpec10Line = buildSpec10SampleJsonl(0).first()
        var threwUnsupportedVersion = false
        try {
            createJvmParser().parse(rawSpec10Line)
        } catch (e: Exception) {
            threwUnsupportedVersion = e.message?.contains("Unsupported protocol version: v1.0") == true
        }
        assertTrue(
            "Direct v1.0 parse must throw Unsupported protocol version in official v0.9.1 engine",
            threwUnsupportedVersion,
        )

        // 2. Scenario 0: Test inline createSurface decomposition (1 Spec 1.0 message -> 3 AndroidX v0.9.1 messages)
        val transformedBatch0 = A2uiSpec10Bridge.transform(rawSpec10Line)
        assertEquals(
            "Inline createSurface with dataModel and components should decompose into 3 v0.9.1 messages",
            3,
            transformedBatch0.size,
        )
        assertTrue("Message 1 must be createSurface", transformedBatch0[0].contains("\"createSurface\""))
        assertTrue("Message 2 must be updateDataModel", transformedBatch0[1].contains("\"updateDataModel\""))
        assertTrue("Message 3 must be updateComponents", transformedBatch0[2].contains("\"updateComponents\""))

        // Verify version and catalog normalization
        transformedBatch0.forEach { line ->
            assertTrue("Transformed line must have version v0.9.1", line.contains("\"version\":\"v0.9.1\""))
            assertTrue(
                "Transformed line must not contain v1_0 catalog URI",
                !line.contains(A2uiSpec10Bridge.SPEC_1_0_BASIC_CATALOG_ID),
            )
            assertTrue("Transformed line must not contain @path directive", !line.contains("\"@path\""))
            assertTrue("Transformed line must not contain @call directive", !line.contains("\"@call\""))
        }

        // Verify 0 errors when processed by A2uiMessageProcessor
        verifyStreamHasZeroErrors(
            streamName = "Spec10Bridge_Scenario0_InlineDecomposition",
            catalogs = listOf(catalog),
            lines = transformedBatch0,
        )

        // 3. Scenario 1: Two-way binding (@path) & dynamic functions (@call) desugaring
        val rawSpec10Scenario1 = buildSpec10SampleJsonl(1)
        val transformedBatch1 = rawSpec10Scenario1.flatMap { A2uiSpec10Bridge.transform(it) }
        verifyStreamHasZeroErrors(
            streamName = "Spec10Bridge_Scenario1_PathAndCall",
            catalogs = listOf(catalog),
            lines = transformedBatch1,
        )
    }

    /**
     * Verifies the latest `A2uiBasicCatalogV1.Functions` typed contract (`f3ed67ecaa9`, `5098f93b2fc`)
     * and the `openUrl` scheme security allowlist (`http`, `https`, `mailto`, `tel` — CL `4346347`).
     */
    @Test
    fun verifyLatestFunctionsContract_andOpenUrlSchemeSecurity() {
        val openedUrls = mutableListOf<String>()
        val functions =
            com.droidknights.a2ui.catalog.hooks.DefaultA2uiMediaRenderers.createBasicFunctions(
                context = null,
                onOpenUrl = { openedUrls.add(it) },
            )

        val dummyExecutionContext =
            object : androidx.a2ui.model.protocol.A2uiExecutionContext {
                override val dataPath: androidx.a2ui.model.protocol.A2uiDataPath =
                    androidx.a2ui.model.protocol.A2uiDataPath("/")
                override fun evaluatePayload(payload: Any?): Any? = payload
                override fun executeFunction(name: String, args: Map<String, Any>): Any? = null
                override fun resolveValue(path: androidx.a2ui.model.protocol.A2uiDataPath): Any? = null
                override fun <T : Any> getOrCreateFunctionScopedCache(
                    functionDefinition: androidx.a2ui.model.catalog.A2uiFunctionDefinition,
                    factory: () -> T,
                ): T = factory()
            }

        // 1. Allowed schemes (https, http, mailto, tel) succeed
        listOf(
            "https://developer.android.com/develop/ui/compose/agentic",
            "http://example.com/status",
            "mailto:oncall@droidknights.com",
            "tel:+821012345678",
        ).forEach { safeUrl ->
            functions.openUrl(safeUrl, dummyExecutionContext)
        }
        assertEquals(4, openedUrls.size)

        // 2. Disallowed schemes (javascript:, file:, intent:, empty, control chars, no scheme) throw A2uiRuntimeException
        // with distinct diagnostic messages (AOSP CL 4346347)
        val expectedErrorSnippets =
            listOf(
                "" to "empty URL",
                "https://example.com/\nmalicious" to "control characters",
                "example.com/no-scheme" to "no scheme",
                "javascript:alert(1)" to "unsupported URL scheme 'javascript'",
                "file:///etc/passwd" to "unsupported URL scheme 'file'",
                "intent://scan/#Intent;scheme=zxing;end" to "unsupported URL scheme 'intent'",
            )
        expectedErrorSnippets.forEach { (unsafeUrl, expectedSnippet) ->
            var caughtMessage: String? = null
            try {
                functions.openUrl(unsafeUrl, dummyExecutionContext)
            } catch (e: androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException) {
                caughtMessage = e.message
            }
            assertTrue(
                "Expected A2uiRuntimeException containing '$expectedSnippet' for unsafe URL '$unsafeUrl', but got: $caughtMessage",
                caughtMessage?.contains(expectedSnippet) == true,
            )
        }
        assertEquals("Unsafe URLs must not reach onOpenUrl", 4, openedUrls.size)

        // 3. Verify typed default methods on A2uiBasicCatalogV1.Functions (5098f93b2fc / f3ed67ecaa9)
        assertTrue(functions.required("non-empty", dummyExecutionContext))
        assertTrue(!functions.required("", dummyExecutionContext))
        assertTrue(functions.length("DroidKnights", min = 5, max = 20, executionContext = dummyExecutionContext))
        assertTrue(functions.numeric(42.0, min = 0.0, max = 100.0, executionContext = dummyExecutionContext))
        assertTrue(functions.email("dev@droidknights.com", dummyExecutionContext))
        assertTrue(functions.regex("INC-8821", "^INC-[0-9]{4}$", dummyExecutionContext))

        // 4. Verify official androidx.a2ui.compose:compose-ui-tooling-preview (98b3d9f6fc0 / CL 4324646)
        // A2uiPreviewParameterProvider<A2uiSurfaceModelFactory> display names and lazy caching
        val previewProvider = com.droidknights.a2ui.common.DroidKnightsA2uiSurfacePreviewProvider()
        val firstFactories = previewProvider.values.toList()
        val secondFactories = previewProvider.values.toList()
        assertEquals(6, firstFactories.size)
        assertEquals("1. Currency Exchange", previewProvider.getDisplayName(0))
        assertEquals("6. Catalog V2 Transfer", previewProvider.getDisplayName(5))
        assertTrue(
            "A2uiPreviewParameterProvider.values should lazily cache A2uiSurfaceModelFactory instances",
            firstFactories[0] === secondFactories[0],
        )

        // 5. Verify A2uiProperty.nestedObject, A2uiProperty.nestedObjectList, and A2uiProperty.dynamicAny
        // schema & validation (merged CL 4390685 rev 10) and A2uiRuntimeException property path attribution
        val dynamicAnyProp =
            androidx.a2ui.compose.runtime.A2uiProperty.dynamicAny(
                key = "dynamicPayload",
                required = false,
                description = "Dynamic arbitrary JSON value (CL 4390685 A2uiProperty.dynamicAny)",
            )
        assertEquals("dynamicPayload", dynamicAnyProp.key)

        val catalog = DroidKnightsCatalogs.createStandardCatalog()
        val schemaJson = catalog.toJsonSchemaString()
        assertTrue("Expected nestedObject 'threshold' property in MetricSparkline schema", schemaJson.contains("\"threshold\""))
        assertTrue("Expected nestedObjectList 'annotations' property in MetricSparkline schema", schemaJson.contains("\"annotations\""))

        val nestedPayloadStream =
            listOf(
                """{"version":"v0.9.1","createSurface":{"surfaceId":"nested_test","catalogId":"${DroidKnightsCatalogs.BASIC_CATALOG_ID}","sendDataModel":true}}""",
                """{"version":"v0.9.1","updateComponents":{"surfaceId":"nested_test","components":[{"id":"root","component":"MetricSparkline","metricName":"API P99","currentValue":"42ms","threshold":{"warn":50.0,"critical":100.0},"annotations":[{"tag":"deploy-v2","note":"Canary rollout"},{"tag":"autoscale","note":"HPA +2 pods"}]}]}}""",
            )
        verifyStreamHasZeroErrors(
            streamName = "NestedObjectAndNestedObjectListProperties_CL4390685",
            catalogs = listOf(catalog),
            lines = nestedPayloadStream,
        )

        // 6. Verify upstream A2uiOpenUrlFunction native scheme enforcement (merged CL 4346347)
        val nativeOpenUrlFn =
            androidx.a2ui.model.catalog.functions.A2uiOpenUrlFunction { url ->
                openedUrls.add(url)
            }
        var nativeRejectedJavascript = false
        try {
            nativeOpenUrlFn.execute(mapOf("url" to "javascript:alert(1)"), dummyExecutionContext)
        } catch (e: androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException) {
            nativeRejectedJavascript =
                e.message?.contains("unsupported URL scheme 'javascript'") == true
        }
        assertTrue(
            "Expected upstream A2uiOpenUrlFunction to reject javascript: scheme",
            nativeRejectedJavascript,
        )

        val attributedError =
            androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException(
                "CircularClimateDial min (30.0) must be strictly less than max (16.0)",
                mapOf("path" to com.droidknights.a2ui.catalog.schema.CircularClimateDialSchema.minProperty.key),
            )
        assertTrue(attributedError.message?.contains("CircularClimateDial min") == true)
        assertEquals("min", attributedError.context["path"])
    }

    @Test
    fun verifyBankingAppState_liveTransactionsAndBalanceUpdates() {
        val state = com.droidknights.a2ui.common.BankingAppState()
        assertEquals(31_520_000L, state.mainAccountBalance)
        assertEquals(2_161_314L, state.cardStatementBalance)
        assertEquals(4, state.transactions.size)

        // 1. Exchange 50,000 JPY (452,060 KRW) -> deducts balance, issues QR voucher, prepends transaction
        val voucher =
            state.recordCurrencyExchange(
                currency = "JPY",
                foreignAmount = 50_000L,
                krwAmount = 452_060L,
                branchLabel = "인천공항 1터미널",
            )
        assertEquals(31_520_000L - 452_060L, state.mainAccountBalance)
        assertEquals(1, state.exchangeVouchers.size)
        assertEquals("QR-EX-2026-8492", voucher.voucherCode)
        assertEquals(5, state.transactions.size)

        // 2. Transfer 240,000 KRW to 박우리
        state.recordTransfer(recipient = "박우리", amount = 240_000L, memo = "모임회비")
        assertEquals(31_520_000L - 452_060L - 240_000L, state.mainAccountBalance)
        assertEquals(6, state.transactions.size)

        // 3. Pay Card Bill with 10,000P points
        val paidCash = state.recordCardBillPayment(payAmount = 2_161_314L, usePoints = true)
        assertEquals(2_151_314L, paidCash)
        assertEquals(0L, state.cardStatementBalance)
        assertEquals(0L, state.rewardPoints)

        // 4. Reset demo state restores initial balance
        state.resetToInitial()
        assertEquals(31_520_000L, state.mainAccountBalance)
        assertEquals(2_161_314L, state.cardStatementBalance)
        assertTrue(state.exchangeVouchers.isEmpty())

        // 5. Conversational text Q&A vs Actionable A2UI surface classification
        val rateAnswer =
            com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
                .buildConversationalTextReplyIfPureQuestion("오늘 엔화랑 달러 환율 얼마야?", state)
        assertTrue("Rate inquiry should return a pure text answer", rateAnswer != null && rateAnswer.contains("904.12원"))

        val balanceAnswer =
            com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
                .buildConversationalTextReplyIfPureQuestion("지금 내 통장 잔액 얼마 남았어?", state)
        assertTrue("Balance inquiry should return live balance in text", balanceAnswer != null && balanceAnswer.contains("31,520,000원"))

        val exchangeAction =
            com.droidknights.a2ui.scenarios.payloads.CurrencyExchangePayloads
                .buildConversationalTextReplyIfPureQuestion("인천공항 1터미널 수령으로 5만 엔 환전해 줘.", state)
        assertEquals("Actionable exchange request should return null to trigger A2UI surface", null, exchangeAction)
    }
}


