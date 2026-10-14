# DroidKnights 2026 · Official AndroidX A2UI (`1.0.0-alpha01`)

Google Maven에 정식 출시된 **`androidx.a2ui` (`1.0.0-alpha01`)** 및 **`androidx.compose.material3:material3-a2ui` (`1.0.0-alpha01`)** 아티팩트만을 100% 사용하여 구현한 **DroidKnights 2026** 세션 샘플 프로젝트입니다.

---

## 📂 디렉터리 구조

```text
droidknight2026-a2ui/
├── app/
│   ├── src/main/java/com/droidknights/a2ui/
│   │   ├── MainActivity.kt                        # 7개 탭 통합 쇼케이스 허브
│   │   ├── catalog/DroidKnightsCatalogs.kt        # 3-Way 카탈로그, V1/V2 협상 카탈로그, 5종 커스텀 컴포넌트
│   │   ├── common/A2uiTelemetryPanel.kt           # outboundEvents(clientDataModel/Error) & JSONL 실시간 인스펙터
│   │   ├── scenarios/
│   │   │   ├── IncidentCommanderDemo.kt           # 도메인 A: 새벽 2시 온콜 커맨더 + ProvideActionInterceptor + Read-Only
│   │   │   ├── ContextualCockpitDemo.kt           # 도메인 B: 스마트홈/SDV 마이크로 콕핏 (Media3/Image 훅 + Dial/BudgetBar)
│   │   │   ├── DynamicMashupSheetDemo.kt          # 도메인 C: 1·2·3차 차등 정산(커스텀 위젯 0개) & 가변 필드 클레임 역전송
│   │   │   └── FieldDiagnosticsWizardDemo.kt      # 도메인 D: 세탁기 OE 에러코드 자가진단 위저드 (isReady 쉬머 스켈레톤)
│   │   └── labs/
│   │       ├── CatalogEvolutionLabScreen.kt       # 공식 기능 #1: 1 JSON × 3 디자인 시스템 & toJsonSchemaString()
│   │       ├── ChaosMonkeySelfHealingLabScreen.kt # 공식 기능 #2: 환각 카오스 몽키, 에러 경계, 에이전트 자가치유 루프
│   │       └── TestingShowcaseLabScreen.kt        # 공식 기능 #5: A2uiTestController 결정론적 UI 테스트 인스펙터
│   ├── src/test/java/com/droidknights/a2ui/
│   │   └── A2uiPayloadsVerificationTest.kt        # 전체 시나리오/Lab JSONL 파서 & 스키마 단위 테스트
│   └── src/androidTest/java/com/droidknights/a2ui/
│       └── A2uiOfficialTestingSuiteTest.kt        # compose-ui-testing:1.0.0-alpha01 기반 5대 공식 UI 테스트
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📦 사용된 Google Maven 공식 아티팩트 (`gradle/libs.versions.toml`)

자체 스펙 파서나 AOSP 소스 복사본 없이 **Google Maven (`https://dl.google.com/dl/android/maven2/`)** 배포 아티팩트만 참조합니다:

- `androidx.a2ui:a2ui-model:1.0.0-alpha01`
- `androidx.a2ui.compose:compose-runtime:1.0.0-alpha01`
- `androidx.a2ui.compose:compose-ui:1.0.0-alpha01`
- `androidx.compose.material3:material3-a2ui:1.0.0-alpha01`
- `androidx.a2ui.compose:compose-ui-testing:1.0.0-alpha01`

---

## 🚀 빌드 및 검증 명령어

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```
