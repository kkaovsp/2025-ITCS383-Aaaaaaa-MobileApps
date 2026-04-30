# D2 — Code Quality

## Native Android App

### Static Analysis — SonarCloud

The Android project is configured for **SonarCloud** static analysis via the `mobile/sonar-project.properties` file. The scanner is set to analyze all Kotlin source files located under `app/src/main/java`.

#### Coverage Exclusions

UI components are **explicitly excluded** from code coverage metrics. Since these files are purely view-layer and contain no business logic, measuring their coverage would produce misleading results. The following patterns are excluded:

| Pattern | Reason |
|---|---|
| `**/*Activity.kt` | Android Activity classes (UI lifecycle) |
| `**/*Screen.kt` | Compose screen definitions |
| `**/ui/**` | UI package directory |
| `**/theme/**` | Theme/styling definitions |
| `**/components/**` | Reusable UI components |
| `**/*Compose*.kt` | Any Compose-related files |

This configuration ensures that coverage metrics reflect only the **testable business logic** of the application.

### Unit Testing

Basic logic unit tests have been added using **JUnit 4**. A utility class (`AppUtils.kt`) was extracted from `MainActivity` to isolate pure, testable functions from the UI layer. The test file `AppUtilsTest.kt` contains **37 test cases** covering:

- **Status-to-color mapping** — `reservationColor()`, `paymentColor()`, `approvalColor()`
- **Color math** — `lighten()` blending function
- **String sanitization** — `safeFileName()` for safe file naming
- **JSON helpers** — `jsonClean()`, `jsonOptBooleanLike()` for safe JSON parsing
- **Error parsing** — `parseErrorBody()` for API error response handling
- **Exception class** — `ApiException` status code and message validation

Tests can be executed via:

```bash
cd mobile
./gradlew testDebugUnitTest
```

> **Note:** All tests are strictly logic-only. No Compose UI screens or Activities are tested, in line with the SonarCloud coverage exclusion strategy.
