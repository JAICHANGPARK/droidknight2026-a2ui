# CHANGELOG · DroidKnights 2026 Official AndroidX A2UI

이 문서는 **DroidKnights 2026** 발표용 `droidknight2026-a2ui` 저장소의 주요 설계, 빌드 환경 전환, 그리고 AOSP `androidx-main` 최신 스냅샷(`1.0.0-SNAPSHOT`) 반영 내역을 기록합니다.

---

## [2026-10-14] AOSP `androidx-main` (`1.0.0-SNAPSHOT`) 최신 스펙 및 기능 반영 (`0a48f0e`)

Google Maven에 배포된 `1.0.0-alpha01` (2026-09-16 브랜치 컷) 이후 약 3주간 AOSP `androidx-main` (`github.com/androidx/androidx`)에 머지된 후속 패치들을 샘플 코드와 테스트에 반영했습니다.

### 1. 카탈로그 ID 공식 상수 참조 & `v0.9.1` 별칭(Alias) 호환 시연
- **배경 (AOSP 커밋 `b288df8ea1d`, `343aaa87431`)**:
  - `A2uiBasicCatalogV1.CatalogId`에서 `const` 한정자가 제거되어 바이너리 상수 인라이닝을 방지함.
  - `A2uiCoreSurfaceActor`에 `CATALOG_ID_ALIASES` 매핑이 추가되어, 에이전트가 `v0_9_1` 기본 카탈로그 URI(`https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json`)로 `createSurface`를 요청해도 클라이언트에 등록된 `v0_9` 기본 카탈로그(`A2uiBasicCatalogV1.CatalogId`)로 자동 매핑됨.
- **변경 파일**:
  - `app/src/main/java/com/droidknights/a2ui/catalog/DroidKnightsCatalogs.kt`:
    - `BASIC_CATALOG_ID`가 하드코딩 문자열 대신 공식 `A2uiBasicCatalogV1.CatalogId`를 직접 참조하도록 변경.
    - `BASIC_CATALOG_ID_V0_9_1_ALIAS` 상수 추가.
  - `app/src/main/java/com/droidknights/a2ui/scenarios/FieldDiagnosticsWizardDemo.kt`:
    - 시나리오 D(`buildDiagnosticsWizardJsonl`)의 `createSurface` 메시지가 `BASIC_CATALOG_ID_V0_9_1_ALIAS`를 전송하여 `1.0.0-SNAPSHOT`의 카탈로그 별칭 자동 해석을 실증하도록 업데이트.

### 2. `MaterialA2uiDefaults.LoadingIndicatorGroup` 및 방사형 `ShimmerIndicator` 시연
- **배경 (AOSP 커밋 `5fe37ebcf42`)**:
  - `compose/material3/material3-a2ui`에 `ShimmerIndicator.kt`와 `MaterialA2uiDefaults.LoadingIndicatorGroup`이 신규 추가됨.
  - `Column`, `Row`, `List` 내부의 자식 컴포넌트가 `A2uiComponentState.Loading` 상태(아직 `updateComponents`가 도착하지 않았거나 `isReady() == false`인 상태)일 때, 컨테이너 좌상단을 기점으로 퍼져나가는 동기화된 방사형 쉬머(Radial Shimmer Sweep)와 인덱스별 호흡 애니메이션(Staggered Breathing Pulse)이 렌더링됨.
- **변경 파일**:
  - `app/src/main/java/com/droidknights/a2ui/scenarios/FieldDiagnosticsWizardDemo.kt`:
    - 1.8초 지연 스트리밍 재현 버튼 클릭 시 `HotspotDiagramCard`(`isReady() == false`)뿐만 아니라 하위 `vid_guide`, `chk_hose` 컴포넌트 도착도 함께 지연시켜 `Column` 내 3개 슬롯에서 동기화된 다중 `ShimmerIndicatorGroup` 효과를 한눈에 확인할 수 있도록 개선.
  - `app/src/androidTest/java/com/droidknights/a2ui/A2uiOfficialTestingSuiteTest.kt`:
    - `scenario4_progressiveRenderingAndIsReadyGate` 계측 테스트에서 `HotspotDiagramCard`를 `Column` 하위에 배치하고, 데이터 도착 전 `onNodeWithTag("LoadingIndicator").assertIsDisplayed()`로 신규 쉬머 인디케이터 노출을 검증하도록 업데이트.

### 3. `A2uiProperty` 리스트 프로퍼티 `minItems` / `maxItems` 유효성 검증 반영
- **배경 (AOSP 커밋 `8b873c31483`)**:
  - `A2uiProperty.numberList`, `stringList`, `nestedList` 등 리스트 프로퍼티 팩토리에서 `minItems < 0`, `maxItems < 0`, 또는 `minItems > maxItems`일 경우 즉시 `IllegalArgumentException`을 던져 잘못된 JSON Schema가 에이전트로 전송되는 것을 차단함.
- **변경 파일**:
  - `app/src/main/java/com/droidknights/a2ui/catalog/DroidKnightsCatalogs.kt`:
    - V2 카탈로그 전용 `MetricSparklineComponent`에 `A2uiProperty.numberList(key = "points", required = false, minItems = 2, maxItems = 24)` 프로퍼티를 추가하고 실제 스파크라인 캔버스 데이터와 바인딩.
  - `app/src/main/java/com/droidknights/a2ui/labs/CatalogEvolutionLabScreen.kt`:
    - V2 카탈로그 비교 스트림(`spark_v2`)에 `"points": [0.72, 0.58, 0.66, 0.35, 0.88, 0.24, 0.94]` 페이로드 추가.

### 4. `1.0.0-SNAPSHOT` 신규 기능 검증 단위 테스트 추가
- **배경 (AOSP 커밋 `39da586fc41` 포함)**:
  - `A2uiObjectSchema`를 JSON Schema로 직렬화할 때 `required` 프로퍼티 배열이 선언 순서와 무관하게 알파벳순으로 결정론적 정렬됨.
- **변경 파일**:
  - `app/src/test/java/com/droidknights/a2ui/A2uiPayloadsVerificationTest.kt`:
    - `verifySnapshotNewFeatures_aliasAndListBoundsAndSortedSchema` 단위 테스트를 추가하여 ① `v0.9.1` 카탈로그 ID alias 해석, ② `minItems > maxItems` 예외 발생, ③ `toJsonSchemaString()`의 `required` 배열 정렬을 JVM 단위 테스트에서 자동 검증.

---

## [2026-10-14] `androidx.dev` 공식 CI 스냅샷(`1.0.0-SNAPSHOT`) 자동 추적 구성 (`1cfb016`)

로컬에 AOSP 소스를 통째로 복제(`:androidx-a2ui`)하지 않고도 GitHub `androidx/androidx` (`androidx-main` 브랜치)의 최신 빌드를 자동으로 가져오도록 Gradle 저장소 구성을 업데이트했습니다.

- **변경 파일**:
  - `settings.gradle.kts`:
    - `https://androidx.dev/snapshots/latest/artifacts/repository` (또는 특정 빌드 고정 URL) 스냅샷 Maven 저장소를 추가하고, `includeGroupByRegex("androidx\\.a2ui.*")` 및 `includeModule("androidx.compose.material3", "material3-a2ui")`로 스코프를 제한.
  - `gradle.properties`:
    - `androidx.a2ui.snapshotBuildId=latest` 프로퍼티 추가 (평소에는 `latest`로 최신 `androidx-main` 빌드를 자동 추적하고, 발표 직전에는 `16602000` 등 검증된 빌드 ID로 고정 가능).
  - `gradle/libs.versions.toml`:
    - `a2ui` 버전을 `"1.0.0-SNAPSHOT"`으로 전환.
  - `README.md`:
    - 스냅샷 자동 추적 및 빌드 ID 고정 방법 문서화.

---

## [2026-10-14] DroidKnights 2026 초기 프로젝트 및 슬라이드 구성 (`41154a3`)

- **비여행(Non-Travel) 4대 실무 도메인 시나리오 구현 (`app/src/main/java/com/droidknights/a2ui/scenarios/`)**:
  1. `IncidentCommanderDemo.kt`: 새벽 2시 모바일 온콜 인시던트 커맨더 (`ProvideActionInterceptor` 1차 로컬 생체인증 모달 + `A2uiActionInterceptor` 2차 감사 서명 주입 + `bindUpdater == null` 관찰자 Read-Only 모드).
  2. `ContextualCockpitDemo.kt`: 스마트홈 / 전기차(SDV) 맥락형 마이크로 콕핏 (`A2uiImageRenderer`, `A2uiVideoRenderer`, `A2uiAudioPlayerRenderer` 미디어 훅 + `CircularClimateDial`, `PowerBudgetBar`).
  3. `DynamicMashupSheetDemo.kt`: 1·2·3차 회식 차등 정산(커스텀 위젯 0개, 내장 `formatString`/`formatCurrency`/`required`/`length` 활용) 및 배달 클레임 가변(Variable) 입력 필드 역전송(`outboundEvents` + `clientDataModel`).
  4. `FieldDiagnosticsWizardDemo.kt`: 세탁기 `OE` 에러코드 현장 자가진단 3단계 위저드 (동일 Surface 진화 및 `isReady()` 프로그레시브 렌더링).
- **공식 문서 5대 핵심 기능 Lab & 테스트 구현 (`labs/`, `test/`, `androidTest/`)**:
  - `CatalogEvolutionLabScreen.kt`: 1개의 JSON 스트림 × 3개 디자인 시스템 카탈로그 슬롯 오버라이드 & V1/V2 멀티 카탈로그 협상 & `toJsonSchemaString()` 인스펙터.
  - `ChaosMonkeySelfHealingLabScreen.kt`: 에이전트 환각 카오스 몽키, 컴포넌트 단위 에러 경계 격리, 1.4초 자동 자가치유 루프, 희소 배열(`/sparseItems/999999`) OOM 방어 검증.
  - `TestingShowcaseLabScreen.kt` & `A2uiOfficialTestingSuiteTest.kt`: `A2uiTestController`, `A2uiComponentStub`, `A2uiTestSurface` 기반 5대 결정론적 UI 테스트.
- **발표 자료 (`slides/`)**:
  - `slides/00-outline-ko.md` 및 `slides/slides.md` 작성.
