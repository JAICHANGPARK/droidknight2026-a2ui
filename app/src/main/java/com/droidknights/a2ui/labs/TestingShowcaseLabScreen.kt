package com.droidknights.a2ui.labs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Official Docs Feature #5 (`testing`):
 * Interactive guide and code inspector for the 5 deterministic UI test patterns implemented in
 * `src/androidTest/java/com/droidknights/a2ui/A2uiOfficialTestingSuiteTest.kt` using
 * `androidx.a2ui.compose:compose-ui-testing:1.0.0-alpha01`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestingShowcaseLabScreen() {
    var selectedTest by remember { mutableIntStateOf(0) }

    val testCases =
        listOf(
            TestPatternInfo(
                title = "① 컴포넌트 격리 & A2uiComponentStub (withId / withType)",
                docSection = "Isolate components with stubs",
                summary =
                    "복잡한 자식 트리나 네트워크 이미지를 실제 렌더링하지 않고 A2uiComponentStub.withId(\"child\") 또는 " +
                        "A2uiComponentStub.withType(\"Image\")로 대체하여 부모 컨테이너만 결정론적으로 검증합니다.",
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
                title = "② 단방향·양방향 데이터 바인딩 검증 (updateData & getData)",
                docSection = "Test reactive data bindings",
                summary =
                    "LLM 서버 없이 controller.updateData(\"/nursery/targetTemp\", 25.0)로 에이전트 데이터 스트리밍을 재현하고, " +
                        "사용자 UI 조작이 controller.getData(\"/nursery/targetTemp\")에 양방향 반영되는지 즉시 단언합니다.",
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
                    val updated: Double? = controller.getData("/nursery/targetTemp")
                    assertThat(updated).isEqualTo(22.5)
                    """.trimIndent(),
            ),
            TestPatternInfo(
                title = "③ 아웃바운드 액션 & 인터셉터 체인 검증 (dispatchedActions / outboundEvents)",
                docSection = "Verify user actions and outbound events",
                summary =
                    "버튼 클릭 시 dispatchAction()이 호출되어 controller.dispatchedActions와 controller.outboundEvents에 " +
                        "정확한 이벤트 이름과 컨텍스트가 기록되는지 검증합니다.",
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
                title = "④ 프로그레시브 로딩(isReady) & 지연 자식 도착 시뮬레이션",
                docSection = "Test progressive rendering and loading states",
                summary =
                    "초기에 자식 컴포넌트나 필수 데이터 경로를 비워두어 Loading 상태(쉬머 스켈레톤)를 검증한 뒤, " +
                        "controller.updateData() / controller.updateComponent() 호출 후 Success 상태로 전환되는지 테스트합니다.",
                codeSnippet =
                    """
                    // 1. 초기에는 /diag/partTitle 데이터가 없으므로 HotspotDiagramCard.isReady() == false
                    val surface = controller.start()
                    setContent { A2uiSurface(surfaceModel = surface) }

                    // 2. 에이전트가 뒤늦게 데이터를 푸시하면 isReady() == true가 되어 즉시 본문 노출
                    controller.updateData("/diag/partTitle", "드럼세탁기 하단 배수 필터")
                    controller.updateData("/diag/hotspotLabel", "잔수 제거 호스")
                    controller.waitForIdle()
                    onNodeWithText("드럼세탁기 하단 배수 필터").assertIsDisplayed()
                    """.trimIndent(),
            ),
            TestPatternInfo(
                title = "⑤ 에러 주입(failComponent) & 스키마 위반 아웃바운드 에러 단언",
                docSection = "Simulate errors and validate error reporting",
                summary =
                    "controller.failComponent(\"root\", A2uiValidationException(...)) 또는 잘못된 프로퍼티(min > max)를 주입해 " +
                        "UI 에러 폴백과 controller.outboundErrors에 기록된 에러 코드를 단언합니다.",
                codeSnippet =
                    """
                    controller.failComponent(
                        id = "root",
                        exception = A2uiException.A2uiValidationException("Simulated schema error", "/components/0")
                    )
                    controller.waitForIdle()
                    assertThat(controller.outboundErrors).isNotEmpty()
                    """.trimIndent(),
            ),
        )

    val current = testCases[selectedTest]

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "🧪 공식 기능 Lab 5 · LLM 없이 돌리는 결정론적 UI 테스트 (compose-ui-testing)",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
                Text(
                    text =
                        "androidx.a2ui.compose:compose-ui-testing:1.0.0-alpha01 아티팩트의 A2uiTestController, " +
                            "A2uiComponentStub, A2uiTestSurface를 사용한 5가지 공식 테스트 시나리오가 " +
                            "app/src/androidTest/.../A2uiOfficialTestingSuiteTest.kt에 실제 코드로 구현되어 있습니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    testCases.forEachIndexed { idx, info ->
                        FilterChip(
                            selected = selectedTest == idx,
                            onClick = { selectedTest = idx },
                            label = { Text(info.title, fontSize = 12.sp) },
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = current.title,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "공식 문서 섹션: developer.android.com/develop/ui/compose/agentic/testing (${current.docSection})",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = current.summary,
                    color = Color.White,
                    fontSize = 13.sp,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF020617),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = current.codeSnippet,
                        color = Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
    }
}

private data class TestPatternInfo(
    val title: String,
    val docSection: String,
    val summary: String,
    val codeSnippet: String,
)
