# D2 — Code Quality

## Native Android App

### Static Analysis — SonarCloud

The Android project is configured for **SonarCloud** static analysis via the `mobile/sonar-project.properties` file. The scanner is set to analyze all Kotlin source files located under `app/src/main/java`.

**SonarCloud Configuration:**

| Property | Value |
|---|---|
| Project Key | `kkaovsp_2025-ITCS383-Aaaaaaa-MobileApps` |
| Organization | `kkaovsp` |
| Source Path | `app/src/main/java` |
| Language | Kotlin |

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

---

### Unit Testing

Basic logic unit tests have been added using **JUnit 4**. A utility class (`AppUtils.kt`) was extracted from `MainActivity` to isolate pure, testable functions from the UI layer.

#### Test Execution Results

```
Test Suite:  com.kkaovsp.boothorganizer.AppUtilsTest
Timestamp:   2026-04-30T17:00:38
Duration:    0.018s
Result:      ✅ BUILD SUCCESSFUL
```

| Metric | Value |
|---|---|
| Total Tests | 46 |
| Passed ✅ | 46 |
| Failed ❌ | 0 |
| Errors ⚠️ | 0 |
| Skipped ⏭️ | 0 |
| Pass Rate | **100%** |

#### Test Coverage by Category

| Category | Function(s) Tested | Tests |
|---|---|---|
| Reservation Status Mapping | `reservationColor()` | 6 |
| Payment Status Mapping | `paymentColor()` | 4 |
| Approval Status Mapping | `approvalColor()` | 4 |
| Color Math | `lighten()` | 3 |
| Filename Sanitization | `safeFileName()` | 7 |
| JSON Safe Access | `jsonClean()` | 5 |
| JSON Boolean Parsing | `jsonOptBooleanLike()` | 9 |
| Error Response Parsing | `parseErrorBody()` | 6 |
| Exception Class | `ApiException` | 2 |
| **Total** | | **46** |

#### Detailed Test Results

All 46 test cases passed successfully:

```
✅ reservationColor_pendingPayment_returnsWarning          0.000s
✅ reservationColor_waitingForApproval_returnsSecondary     0.000s
✅ reservationColor_confirmed_returnsSuccess                0.000s
✅ reservationColor_cancelled_returnsDanger                 0.000s
✅ reservationColor_unknownStatus_returnsMuted              0.000s
✅ reservationColor_emptyString_returnsMuted                0.000s
✅ paymentColor_approved_returnsSuccess                     0.001s
✅ paymentColor_pending_returnsWarning                      0.000s
✅ paymentColor_rejected_returnsDanger                      0.000s
✅ paymentColor_unknownStatus_returnsMuted                  0.000s
✅ approvalColor_approved_returnsSuccess                    0.000s
✅ approvalColor_rejected_returnsDanger                     0.000s
✅ approvalColor_pending_returnsWarning                     0.000s
✅ approvalColor_unknownStatus_returnsMuted                 0.000s
✅ lighten_black_returnsLightGray                           0.000s
✅ lighten_white_staysWhite                                 0.000s
✅ lighten_resultIsLighterThanInput                         0.000s
✅ safeFileName_normalText_returnsLowercase                 0.008s
✅ safeFileName_specialCharacters_replacedWithUnderscores   0.000s
✅ safeFileName_preservesThaiCharacters                     0.000s
✅ safeFileName_emptyString_returnsEvent                    0.000s
✅ safeFileName_onlySpecialChars_returnsEvent               0.000s
✅ safeFileName_leadingAndTrailingSpecialChars_trimmed       0.001s
✅ safeFileName_mixedInput_producesCleanName                0.000s
✅ jsonClean_existingKey_returnsValue                       0.000s
✅ jsonClean_missingKey_returnsEmptyString                  0.000s
✅ jsonClean_nullValue_returnsEmptyString                   0.000s
✅ jsonClean_emptyStringValue_returnsEmptyString            0.000s
✅ jsonClean_numericValue_returnsStringRepresentation       0.000s
✅ jsonOptBooleanLike_booleanTrue_returnsTrue               0.000s
✅ jsonOptBooleanLike_booleanFalse_returnsFalse             0.000s
✅ jsonOptBooleanLike_numberOne_returnsTrue                 0.000s
✅ jsonOptBooleanLike_numberZero_returnsFalse               0.000s
✅ jsonOptBooleanLike_stringTrue_returnsTrue                0.000s
✅ jsonOptBooleanLike_stringOne_returnsTrue                 0.001s
✅ jsonOptBooleanLike_stringFalse_returnsFalse              0.000s
✅ jsonOptBooleanLike_missingKey_returnsFalse               0.000s
✅ jsonOptBooleanLike_nullValue_returnsFalse                0.000s
✅ parseErrorBody_blankBody_returnsFallback                 0.000s
✅ parseErrorBody_jsonWithError_returnsErrorField           0.000s
✅ parseErrorBody_jsonWithDetail_returnsDetailField         0.003s
✅ parseErrorBody_jsonWithMessage_returnsMessageField       0.000s
✅ parseErrorBody_invalidJson_returnsRawBody                0.000s
✅ parseErrorBody_jsonWithMultipleFields_prefersError       0.000s
✅ apiException_holdsStatusCodeAndMessage                   0.001s
✅ apiException_isAnException                               0.000s
```

#### How to Run Tests

```bash
cd mobile
./gradlew testDebugUnitTest
```

Test results (JUnit XML) are generated at:
```
mobile/app/build/test-results/testDebugUnitTest/
```

> **Note:** All tests are strictly logic-only. No Compose UI screens or Activities are tested, in line with the SonarCloud coverage exclusion strategy.
