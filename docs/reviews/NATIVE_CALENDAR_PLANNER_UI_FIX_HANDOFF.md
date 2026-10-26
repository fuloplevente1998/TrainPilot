# Native Calendar Planner UI – isolated repair handoff

## Exact scope and source
- Base commit: `98ef71b2e4f2fa4f08b415c3d0a94d113b62303d` from `feat/native-android-migration`.
- Work branch: `fix/native-calendar-planner-ui-readiness`; draft PR targets `feat/native-android-migration`.
- Initial implementation/test code commit: `9d3c428ce42ba4e97a657028ea1c2e229f56658a`; subsequent valid-day-index follow-up: `5f5ede2be3b55f01c27dbdf66892c48260bb3256`. Final head is the current PR head (the handoff itself adds a commit).
- Modified paths: `CalendarScreen.kt`, `CalendarViewModel.kt`, `NativeCalendarPlannerUiTest.kt`; added `NativeCalendarPlannerReadinessGuardTest.kt`, plus this handoff. All paths are inside the task allowlist. No migration/main writes, workflow/release/Gradle/fixture changes, or branch merge.

## Observed failure and root-cause analysis
- The historical phone artifact `12649155506`, Actions run `40317771457`, built from `e28ab41887a976ee380f63f465dbdcb51f494329`, contains the exact failed stack: `NativeCalendarPlannerUiTest.previewCancelThenSaveAndReopenStoredWeeks` at line 33; `performScrollTo()` cannot find `calendar_planner_weeks`.
- The original test only waited for presence of `calendar_planner_open` and clicked it without checking enabled state. The production button is gated on `busy`, `plannerLoaded` and available programs, so a non-opened dialog explains the missing weeks tag. This is a source-supported failure mechanism, **not runtime-proven root cause** on the new branch.
- The original test also cancelled a four-week preview and then implicitly expected four weeks without selecting that duration again. Cancel must never persist such an unsaved edit.
- The original activity test did not reset Room/DataStore, explicitly bypass onboarding, or check startup program readiness; it could inherit state from earlier tests or launch the planner too early.

## Fix
- Planner open waits for settings and at least one program containing days. The form defaults to a usable program if the stored choice is unavailable, and clamps/resets an obsolete saved first-day index. It still cannot preview/apply while busy.
- Cancel and successful apply now drop their retained continuation/settings-identity references along with the preview. The existing atomic preview/apply CAS, active-workout/ownership/restore continuation checks, SettingsStore identity comparison and VM busy guard remain intact; no parallel planner engine or direct test DB writes for plan creation were introduced.
- The test resets only a disposable debuggable emulator via the existing unchanged fixture (which seeds real builtin programs and marks onboarding complete); mounts exactly one replacement CalendarScreen composition in the real MainActivity with one actual ViewModel; and uses **read-only** Room queries plus the repository's persisted settings reader for assertions. It never uses Room writes to simulate the user's Save action.
- Main UI test explicitly proves no persisted change after Preview, then Cancel; reopens and checks the original weeks; explicitly chooses 4 weeks and uses the UI Save; verifies exact planned slot identities/count in Room and persisted weeks again after full Activity/ViewModel recreation.
- Guard tests prove that a cancelled preview cannot be applied late and that two same-frame VM apply requests cannot create duplicate Room rows. These exercise production VM/repository code, not a test-only planner implementation.

## Exact tests to run
- Existing JVM class (unchanged, 11 cases): `com.repforge.app.NativeCalendarPlannerTest`.
- UI: `com.repforge.app.NativeCalendarPlannerUiTest#previewCancelThenSaveAndReopenStoredWeeks`.
- UI: `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#cancelInvalidatesPreviewBeforeAnyLateApply`.
- UI: `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#duplicateApplyCannotCreateDuplicateRoomRows`.
- Run only these scoped tests on an explicitly permitted own-branch runner, or defer to Root's integrated gate. No standalone full `native-android.yml` job was triggered here.

## Verification – do not conflate source checks with runtime
| Check | Status | Evidence |
| --- | --- | --- |
| Historical failure read | OBSERVED FAIL, **old e28 source only** | Artifact 12649155506 / `instrumentation.txt` lines 13–31 |
| Allowed-file delta against pinned base | PASS | GitHub compare of pinned base to own branch; four code/test paths and this review document |
| Static Kotlin delimiter/comment/string lexical scan | PASS, limited | All four changed Kotlin files balanced; not a parser or compiler |
| Kotlin compilation | **UNRUN** | No permitted local Gradle, no dedicated branch runner |
| Prior 11 planner JVM cases on this branch | **UNRUN** | Existing test class unchanged; old e28 gate is not evidence for this branch |
| Three targeted Android UI cases | **UNRUN** | No emulation or branch-specific test run performed |
| Active-workout/ownership/restore regression at runtime | **UNRUN** | Guard implementation untouched; requires integrated regression gate |

No runtime PASS is asserted. Root should review the isolated diff, compile the targeted test sources and run the listed classes on the integrated source before merging. No production migration or release acceptance is implied.
