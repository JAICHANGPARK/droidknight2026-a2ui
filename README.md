# DroidKnights 2026 · Official AndroidX A2UI (`1.0.0-SNAPSHOT` / `1.0.0-alpha02`)

Google 공식 AndroidX CI 스냅샷(`1.0.0-SNAPSHOT`) 및 Google Maven에 정식 출시된 **`androidx.a2ui` (`1.0.0-alpha02`)** · **`androidx.compose.material3:material3-a2ui` (`1.0.0-alpha02`)** 아티팩트만을 100% 사용하여 구현한 **DroidKnights 2026** 세션 샘플 프로젝트입니다.

---

## 📂 디렉터리 구조

```text
droidknight2026-a2ui/
├── app/
│   ├── src/main/java/com/droidknights/a2ui/
│   │   ├── MainActivity.kt                                         # 9개 탭 통합 쇼케이스 허브
│   │   ├── catalog/
│   │   │   ├── DroidKnightsCatalogs.kt                             # 통합 카탈로그 레지스트리 & V1/V2 협상 팩토리
│   │   │   ├── domains/                                            # [도메인/예제별 전용 카탈로그 정의]
│   │   │   │   ├── CurrencyExchangeCatalog.kt                      # 외화 환전 도메인 전용 카탈로그 (Basic V1 + BidirectionalExchangeInput 등 3종)
│   │   │   │   ├── IncidentCommanderCatalog.kt                     # 도메인 A 전용 카탈로그 (Basic V1 + SRE/Guardrail 5종)
│   │   │   │   ├── ContextualCockpitCatalog.kt                     # 도메인 B 전용 카탈로그 (Basic V1 + Media3 + IoT/SDV 4종)
│   │   │   │   ├── DynamicMashupCatalog.kt                         # 도메인 C 전용 카탈로그 (커스텀 위젯 0개 · 순수 Basic V1)
│   │   │   │   └── FieldDiagnosticsCatalog.kt                      # 도메인 D 전용 카탈로그 (Basic V1 + HotspotDiagramCard)
│   │   │   ├── schema/                                             # [개별 컴포넌트 스키마 계약 (A2uiProperty / isReady / TypedContent)]
│   │   │   │   ├── BidirectionalExchangeInputSchema.kt             # 양방향 외화↔원화 환율 입력 스키마 계약
│   │   │   │   ├── CircularClimateDialSchema.kt                    # 원형 다이얼 스키마 & RW/RO 바인딩 계약
│   │   │   │   ├── PowerBudgetBarSchema.kt                         # 전력/메모리 예산 바 스키마 & 과부하 검증 계약
│   │   │   │   ├── HotspotDiagramCardSchema.kt                     # 핫스팟 카드 스키마 & isReady() 로딩 게이트 계약
│   │   │   │   ├── ActionGuardContainerSchema.kt                   # 위험 액션 로컬 2FA 인터셉터 스키마 계약
│   │   │   │   ├── MetricSparklineSchema.kt                        # 스파크라인 스키마 & numberList(minItems/maxItems) 계약
│   │   │   │   └── DomainCatalogSchemas.kt                         # 스키마 통합 Facade
│   │   │   ├── components/                                         # [순수 Compose 위젯 UI 구현체]
│   │   │   │   ├── BidirectionalExchangeInputComponent.kt          # 양방향 외화↔원화 실시간 환율 계산 입력기 UI
│   │   │   │   ├── CircularClimateDialComponent.kt                 # 원형 온도/비율 다이얼 UI
│   │   │   │   ├── PowerBudgetBarComponent.kt                      # 실시간 전력/메모리 예산 바 UI
│   │   │   │   ├── HotspotDiagramCardComponent.kt                  # 아키텍처/부품 핫스팟 카드 UI
│   │   │   │   ├── ActionGuardContainerComponent.kt                # 위험 액션 로컬 2FA 인터셉터 모달 UI
│   │   │   │   └── MetricSparklineComponent.kt                     # V2 실시간 메트릭 스파크라인 UI
│   │   │   ├── themes/
│   │   │   │   ├── BrandDesignSystemSlots.kt                       # 사내 브랜드 디자인 시스템 슬롯 오버라이드 (Card/Button/Text)
│   │   │   │   └── SeniorAccessibleSlots.kt                        # 시니어 큰글씨·고대비·음성안내 슬롯 오버라이드
│   │   │   └── hooks/
│   │   │       └── DefaultA2uiMediaRenderers.kt                    # Media3 Video / Audio / Image 렌더러 훅 & Functions(context) + URL 스킴 보안 검증
│   │   ├── common/
│   │   │   ├── A2uiTelemetryPanel.kt                               # outboundEvents(clientDataModel/Error) & JSONL 실시간 인스펙터
│   │   │   ├── A2uiPreviewSupport.kt                               # A2uiPreviewParameterProvider & rememberA2uiPreviewSurface 프리뷰 헬퍼
│   │   │   └── A2uiSpec10Bridge.kt                                 # A2UI Spec 1.0 (v1.0/@path/@call) 차세대 브릿지
│   │   ├── scenarios/                                              # [5대 실무 도메인 시나리오 UI & 페이로드 분리]
│   │   │   ├── CurrencyExchangeDemo.kt                             # 대표 도메인 (외화 환전) UI: 외화 환전 2-Pane & 3단계 멀티턴 바우처 진화
│   │   │   ├── LegacyCurrencyExchangeScreen.kt                     # 대표 도메인 (외화 환전) As-Is: 기존 7단계 명령형 네비게이션 환전 화면
│   │   │   ├── IncidentCommanderDemo.kt                            # 도메인 A UI: 새벽 2시 온콜 커맨더
│   │   │   ├── ContextualCockpitDemo.kt                            # 도메인 B UI: 스마트홈/SDV 마이크로 콕핏
│   │   │   ├── DynamicMashupSheetDemo.kt                           # 도메인 C UI: 즉석 합체 & 동적 클레임 & 차등 정산
│   │   │   ├── FieldDiagnosticsWizardDemo.kt                       # 도메인 D UI: 세탁기 OE 에러코드 자가진단 위저드
│   │   │   └── payloads/
│   │   │       ├── CurrencyExchangePayloads.kt                     # 대표 도메인 (외화 환전) 에이전트 JSONL 5대 프리셋 & 3단계 멀티턴 진화
│   │   │       ├── IncidentCommanderPayloads.kt                    # 도메인 A 에이전트 JSONL 스트림 & 메타데이터
│   │   │       ├── ContextualCockpitPayloads.kt                    # 도메인 B 에이전트 JSONL 스트림 & 루틴 정의
│   │   │       ├── DynamicMashupPayloads.kt                        # 도메인 C 에이전트 JSONL 스트림 & 가변 스키마 정의
│   │   │       └── FieldDiagnosticsPayloads.kt                     # 도메인 D 에이전트 JSONL 스트림 & 단계별 진화 정의
│   │   └── labs/
│   │       ├── Spec10BridgeLabScreen.kt                            # 공식 기능 Lab 0: Spec 1.0 차세대 브릿지 & 와이어 비교
│   │       ├── CatalogEvolutionLabScreen.kt                        # 공식 기능 Lab 1: 1 JSON × 3 디자인 시스템 & toJsonSchemaString()
│   │       ├── ChaosMonkeySelfHealingLabScreen.kt                  # 공식 기능 Lab 2: 환각 카오스 몽키, URL 스킴 가드레일, 자가치유 루프
│   │       └── TestingShowcaseLabScreen.kt                         # 공식 기능 Lab 5: A2uiTestController 결정론적 UI 테스트 인스펙터
│   ├── src/test/java/com/droidknights/a2ui/
│   │   └── A2uiPayloadsVerificationTest.kt                         # 전체 시나리오/Lab JSONL 파서 & 스키마 & openUrl 보안 단위 테스트
│   └── src/androidTest/java/com/droidknights/a2ui/
│       └── A2uiOfficialTestingSuiteTest.kt                         # compose-ui-testing:1.0.0-alpha01 기반 5대 공식 UI 테스트
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📦 사용된 AndroidX A2UI 공식 아티팩트 (`gradle/libs.versions.toml`)

자체 스펙 파서나 AOSP 로컬 소스 복제본(`:androidx-a2ui`) 없이 **Google 공식 AndroidX CI 스냅샷 저장소 (`https://androidx.dev/snapshots/latest/artifacts/repository`)** 및 **Google Maven (`https://dl.google.com/dl/android/maven2/`)** 아티팩트만 참조합니다:

- `androidx.a2ui:a2ui-model:1.0.0-SNAPSHOT` (또는 `1.0.0-alpha02`)
- `androidx.a2ui:a2ui-engine:1.0.0-SNAPSHOT` (또는 `1.0.0-alpha02`)
- `androidx.a2ui.compose:compose-runtime:1.0.0-SNAPSHOT` (또는 `1.0.0-alpha02`)
- `androidx.a2ui.compose:compose-ui:1.0.0-SNAPSHOT` (또는 `1.0.0-alpha02`)
- `androidx.a2ui.compose:compose-ui-tooling-preview:1.0.0-SNAPSHOT` (`98b3d9f6fc0` 신규 공식 프리뷰 모듈)
- `androidx.compose.material3:material3-a2ui:1.0.0-SNAPSHOT` (또는 `1.0.0-alpha02`)
- `androidx.a2ui.compose:compose-ui-testing:1.0.0-SNAPSHOT` (또는 `1.0.0-alpha02`)

### 🔄 최신 `androidx-main` 자동 추적 vs 특정 빌드 고정 (`gradle.properties`)
- **기본값 (`androidx.a2ui.snapshotBuildId=latest`)**: AOSP `androidx-main` 최신 성공 빌드를 자동 추적합니다 (`./gradlew :app:assembleDebug --refresh-dependencies`로 즉시 최신화 가능).
- **특정 빌드 고정 (`androidx.a2ui.snapshotBuildId=16770660`)**: 발표 리허설/본 발표 직전 특정 스냅샷 빌드 ID로 고정하여 업스트림 변경으로부터 빌드를 보호할 수 있습니다.

---

## 🚀 빌드 및 검증 명령어

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
