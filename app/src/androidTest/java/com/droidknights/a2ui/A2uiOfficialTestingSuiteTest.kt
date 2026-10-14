package com.droidknights.a2ui

import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.testing.A2uiComponentPayload
import androidx.a2ui.compose.ui.testing.A2uiComponentStub
import androidx.a2ui.compose.ui.testing.A2uiTestController
import androidx.a2ui.compose.ui.testing.A2uiTestSurface
import androidx.a2ui.compose.ui.testing.getData
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.droidknights.a2ui.catalog.DroidKnightsCatalogs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DroidKnights 2026 — Official AndroidX A2UI (`1.0.0-alpha01`) Deterministic UI Testing Suite.
 *
 * Implements the 5 official testing scenarios from:
 * https://developer.android.com/develop/ui/compose/agentic/testing
 */
@OptIn(ExperimentalTestApi::class)
class A2uiOfficialTestingSuiteTest {

    /**
     * Scenario 1: Isolate components with `A2uiComponentStub.withId` and `A2uiComponentStub.withType`.
     */
    @Test
    fun scenario1_isolateComponentsWithStubs() = runComposeUiTest {
        val urlProp = A2uiProperty.string("url")
        val imageTypeStub =
            A2uiComponentStub.withType("Image") { props, modifier ->
                val url = props[urlProp] ?: "no-url"
                BasicText("StubbedImage:$url", modifier = modifier.testTag("stubbed_image"))
            }

        val controller =
            A2uiTestController(
                catalog = DroidKnightsCatalogs.createStandardCatalog(),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Image",
                            properties = mapOf("url" to "https://droidknights.com/banner.png"),
                        )
                    ),
                componentStubs = listOf(imageTypeStub),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface)
            }
        }

        onNodeWithTag("stubbed_image").assertIsDisplayed()
        onNodeWithText("StubbedImage:https://droidknights.com/banner.png").assertIsDisplayed()
    }

    /**
     * Scenario 2: Verify reactive one-way and two-way DataModel bindings (`updateData` & `getData`).
     */
    @Test
    fun scenario2_reactiveTwoWayDataBindings() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = DroidKnightsCatalogs.createStandardCatalog(),
                initialData = mapOf("nursery" to mapOf("targetTemp" to 22.0)),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "CircularClimateDial",
                            properties =
                                mapOf(
                                    "label" to "아이방 목표 온도",
                                    "unit" to "°C",
                                    "min" to 18.0,
                                    "max" to 28.0,
                                    "step" to 0.5,
                                    "value" to mapOf("path" to "/nursery/targetTemp"),
                                ),
                        )
                    ),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface)
            }
        }

        onNodeWithText("22.0°C").assertIsDisplayed()

        // 1. User clicks "+ 0.5°C" -> writes back to DataModel via bindUpdater
        onNodeWithText("+ 0.5°C").performClick()
        waitForIdle()
        controller.waitForIdle()

        val updatedByUi: Double? = controller.getData("/nursery/targetTemp")
        assertEquals(22.5, updatedByUi ?: 0.0, 0.01)

        // 2. Agent pushes new data via updateData -> UI recomposes automatically
        controller.updateData("/nursery/targetTemp", 25.0)
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("25.0°C").assertIsDisplayed()
    }

    /**
     * Scenario 3: Verify user action dispatching and outbound event recording (`dispatchedActions` & `outboundEvents`).
     */
    @Test
    fun scenario3_verifyUserActionsAndOutboundEvents() = runComposeUiTest {
        val triggerStub =
            A2uiComponentStub.withId("root") { _, modifier ->
                BasicText(
                    text = "Execute Failover",
                    modifier =
                        modifier.testTag("failover_btn").clickable {
                            dispatchAction(
                                mapOf(
                                    "event" to
                                        mapOf(
                                            "name" to "failover_pg_traffic",
                                            "context" to mapOf("incidentId" to "INC-8821"),
                                        )
                                )
                            )
                        },
                )
            }

        val controller =
            A2uiTestController(
                catalog = DroidKnightsCatalogs.createStandardCatalog(),
                initialComponents = listOf(A2uiComponentPayload("root")),
                componentStubs = listOf(triggerStub),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiTestSurface(surface = surface)
            }
        }

        onNodeWithTag("failover_btn").performClick()
        waitForIdle()
        controller.waitForIdle()

        assertEquals(1, controller.dispatchedActions.size)
        assertEquals(1, controller.outboundEvents.size)
        assertEquals("failover_pg_traffic", controller.outboundEvents.first().type)
        assertEquals("INC-8821", controller.outboundEvents.first().context["incidentId"])
    }

    /**
     * Scenario 4: Verify progressive rendering (`isReady`) and delayed data/component arrival.
     */
    @Test
    fun scenario4_progressiveRenderingAndIsReadyGate() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = DroidKnightsCatalogs.createStandardCatalog(),
                initialData = mapOf("diag" to emptyMap<String, Any?>()),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "HotspotDiagramCard",
                            properties =
                                mapOf(
                                    "title" to mapOf("path" to "/diag/partTitle"),
                                    "hotspotLabel" to mapOf("path" to "/diag/hotspotLabel"),
                                ),
                        )
                    ),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiSurface(surfaceModel = surface)
            }
        }

        // Now stream the missing data paths so HotspotDiagramCard.isReady() transitions from false -> true
        controller.updateData("/diag/partTitle", "드럼세탁기 전면 배수 필터")
        controller.updateData("/diag/hotspotLabel", "잔수 제거 호스 캡")
        controller.waitForIdle()
        waitForIdle()

        onNodeWithText("드럼세탁기 전면 배수 필터").assertIsDisplayed()
    }

    /**
     * Scenario 5: Simulate agent hallucination (`failComponent`) and verify error boundary & `outboundErrors`.
     */
    @Test
    fun scenario5_simulateHallucinationAndAssertErrorBoundary() = runComposeUiTest {
        val controller =
            A2uiTestController(
                catalog = DroidKnightsCatalogs.createStandardCatalog(),
                initialComponents =
                    listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "Text",
                            properties = mapOf("text" to "Healthy Initial State"),
                        )
                    ),
            )

        val surface = controller.start()
        setContent {
            MaterialTheme {
                A2uiSurface(surfaceModel = surface)
            }
        }

        onNodeWithText("Healthy Initial State").assertIsDisplayed()

        controller.failComponent(
            id = "root",
            exception =
                A2uiException.A2uiValidationException(
                    message = "Component type 'HallucinatedWidget' not found in catalog",
                    path = "/components/0/component",
                ),
        )
        controller.waitForIdle()
        waitForIdle()

        assertTrue(controller.outboundErrors.isNotEmpty())
        assertEquals("VALIDATION_FAILED", controller.outboundErrors.first().code)
    }
}
