package com.droidknights.a2ui.labs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestingShowcaseLabScreen() {
    var selectedTest by remember { mutableIntStateOf(0) }

    val testCases =
        listOf(
            TestPatternInfo(
                shortLabel = "1. 컴포넌트 스텁",
                title = "컴포넌트 격리 & A2uiComponentStub",
                docSection = "Isolate components with stubs",
                summary = "A2uiComponentStub.withType(\"Image\")로 자식 컴포넌트를 대체해 부모 컨테이너만 결정론적으로 검증합니다.",
                codeSnippet =
                    """
                    val imageStub = A2uiComponentStub.withType("Image") { props, modifier ->
                        val url = props[A2uiProperty.string("url")] ?: "no-url"
                        BasicText("Stubbed Image: ${'$'}url", modifier.testTag("image_stub"))
                    }
                    val controller = A2uiTestController(
                        catalog = DroidKnightsCatalogs.createStandardCatalog(),
                        initialComponents = listOf(
                            A2uiComponentPayload("root", "Image", mapOf("url" to "https://dk26.dev/hero.png"))
                        ),
                        componentStubs = listOf(imageStub),
                    )
                    val surface = controller.start()
                    setContent { A2uiTestSurface(surface = surface) }
                    onNodeWithText("Stubbed Image: https://dk26.dev/hero.png").assertIsDisplayed()
                    """.trimIndent(),
            ),
            TestPatternInfo(
                shortLabel = "2. 양방향 바인딩",
                title = "데이터 바인딩 검증 (updateData & getData)",
                docSection = "Test reactive data bindings",
                summary = "LLM 없이 controller.updateData()와 controller.getData()로 양방향 상태 동기화를 검증합니다.",
                codeSnippet =
                    """
                    val controller = A2uiTestController(
                        catalog = DroidKnightsCatalogs.createStandardCatalog(),
                        initialData = mapOf("nursery" to mapOf("targetTemp" to 22.0)),
                        initialComponents = listOf(
                            A2uiComponentPayload(
                                id = "root",
                                type = "CircularClimateDial",
                                properties = mapOf(
                                    "label" to "아이방 온도",
                                    "value" to mapOf("path" to "/nursery/targetTemp")
                                )
                            )
                        ),
                    )
                    val surface = controller.start()
                    setContent { A2uiTestSurface(surface = surface) }
                    onNodeWithText("+ 0.5°C").performClick()
                    controller.waitForIdle()
                    assertThat(controller.getData<Double>("/nursery/targetTemp")).isEqualTo(22.5)
                    """.trimIndent(),
            ),
            TestPatternInfo(
                shortLabel = "3. 아웃바운드 액션",
                title = "아웃바운드 이벤트 검증 (dispatchedActions)",
                docSection = "Verify user actions and outbound events",
                summary = "버튼 클릭 시 controller.dispatchedActions와 outboundEvents에 기록된 이벤트를 검증합니다.",
                codeSnippet =
                    """
                    onNodeWithTag("submit_button").performClick()
                    waitForIdle()
                    controller.waitForIdle()

                    assertThat(controller.dispatchedActions).hasSize(1)
                    assertThat(controller.outboundEvents).hasSize(1)
                    assertThat(controller.outboundEvents.first().type).isEqualTo("failover_pg_traffic")
                    """.trimIndent(),
            ),
            TestPatternInfo(
                shortLabel = "4. 프로그레시브 로딩",
                title = "프로그레시브 로딩 (isReady) 시뮬레이션",
                docSection = "Test progressive rendering and loading states",
                summary = "필수 데이터 도착 전 Loading 스켈레톤과 updateData() 이후 본문 전환을 테스트합니다.",
                codeSnippet =
                    """
                    val surface = controller.start()
                    setContent { A2uiSurface(surfaceModel = surface) }

                    controller.updateData("/diag/partTitle", "하단 배수 필터")
                    controller.updateData("/diag/hotspotLabel", "잔수 제거 호스")
                    controller.waitForIdle()
                    onNodeWithText("하단 배수 필터").assertIsDisplayed()
                    """.trimIndent(),
            ),
            TestPatternInfo(
                shortLabel = "5. 에러 주입",
                title = "에러 주입 (failComponent) & 에러 리포팅",
                docSection = "Simulate errors and validate error reporting",
                summary = "controller.failComponent() 호출 시 에러 경계 UI와 outboundErrors 기록을 단언합니다.",
                codeSnippet =
                    """
                    controller.failComponent(
                        id = "root",
                        exception = A2uiException.A2uiValidationException("Simulated error", "/components/0")
                    )
                    controller.waitForIdle()
                    assertThat(controller.outboundErrors).isNotEmpty()
                    """.trimIndent(),
            ),
        )

    val current = testCases[selectedTest]

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "결정론적 UI 테스트 (compose-ui-testing)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "A2uiTestController와 A2uiComponentStub을 활용한 5가지 테스트 패턴입니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            testCases.forEachIndexed { idx, info ->
                FilterChip(
                    selected = selectedTest == idx,
                    onClick = { selectedTest = idx },
                    label = { Text(info.shortLabel, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = current.docSection,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = current.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = current.codeSnippet,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp),
                    )
                }
            }
        }
    }
}

private data class TestPatternInfo(
    val shortLabel: String,
    val title: String,
    val docSection: String,
    val summary: String,
    val codeSnippet: String,
)
