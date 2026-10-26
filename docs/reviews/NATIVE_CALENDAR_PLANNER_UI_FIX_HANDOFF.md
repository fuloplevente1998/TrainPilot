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


## PR #258 second-review follow-up (plan at `60eeab4a`)

- Reviewed exact old PR HEAD: `b5cefd4e776f830c2b15745462f82a422f5aacf4`; this section describes changes made **after** that SHA on the same `fix/native-calendar-planner-ui-readiness` branch. No cherry-pick/rebase/force-push, no migration/main write.
- `CalendarViewModel` now assigns each preview attempt a monotonically increasing `AtomicLong` epoch before dispatch. Cancel increments the epoch and drops the preview plus continuation/settings identity. New requests invalidate earlier ones; `onCleared` increments again and clears references. A computed repository result is allowed to publish its preview/continuation only when its epoch still matches; all checks and publication happen together on Main. The existing UI busy/disabled gate is **unchanged** and an Apply still checks original continuation, settings identity and repository CAS.
- Deterministic instrumental regression uses the **real** production repository/Room preview, with an optional default-no-op VM constructor suspend gate placed *after* the repository read and before Main publication. Sequence: dialog Generate -> real read reaches gate -> programmatically cancel while busy -> release late result -> assert no preview, no subsequent Apply callback and no persisted rows/settings. A normal disabled Cancel UI cannot itself trigger that sequence; this is an explicit internal concurrency regression, not a claim of prior emulator reproduction.
- Two independent default-no-op VM constructor gates delay initial planner settings read or program Flow subscription. Tests assert the existing button is disabled and no dialog appears with either missing dependency; after releasing the gate a deliberate click opens the real form. An internal pure `calendarPlannerInitialSelection` function used by the real dialog checks removed program -> usable fallback/day 0, and too-large saved first-day index -> last valid index; the Generate button cannot be clicked with no program days.
- The main cancel/save/Activity recreate test, original completed-preview cancel guard and double-apply protection remain, including persisted settings and one-to-one preview Slot/Room row matching. UI error handling still closes the planner only after `repository.applyPreview` successfully finishes; failed Apply leaves the dialog open and surfaces VM error instead of calling `saved()`. The existing 45-day Room observation window still covers the 3–4 week targeted test plans; it has not been enlarged without extending the read boundary.
- No changes to the shared fixture, native engine, NavHost, SettingsStore, CI workflow or build configuration. Root's `e12ed8a5` shared fixture fix for missing Compose `performScrollTo`/`assertIsDisplayed` imports is **not** copied into this isolated branch; Root must ensure those two imports are present in the integrated baseline for compilation if needed.

### Scoped runtime selectors after second review

Existing unmodified JVM class (11 cases): `com.repforge.app.NativeCalendarPlannerTest`.

UI methods (seven source methods, **not** seven passing executions):

1. `com.repforge.app.NativeCalendarPlannerUiTest#previewCancelThenSaveAndReopenStoredWeeks`
2. `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#cancelInvalidatesPreviewBeforeAnyLateApply`
3. `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#duplicateApplyCannotCreateDuplicateRoomRows`
4. `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#plannerOpenDisabledUntilSettingsLoadedThenClickOpens`
5. `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#plannerOpenDisabledUntilProgramsLoadedThenClickOpens`
6. `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#cancelDuringRepositoryPreviewReadDropsLateResultAndContinuation`
7. `com.repforge.app.NativeCalendarPlannerReadinessGuardTest#missingProgramAndShortenedProgramGetValidFirstDayFallback`

### Evidence and non-executed checks

| Check | Second-review result |
| --- | --- |
| Allowed file boundaries / new diff against exact reviewed HEAD | PASS (GitHub compare, after final handoff commit) |
| Kotlin source lexical structural scan | PASS (delimiters, comments and strings only; **not** parsing or compilation) |
| Kotlin/Android source compilation | **UNRUN** (no allowed local Gradle or branch-specific compilation runner) |
| Eleven existing planner JVM cases | **UNRUN** on this branch; the old e28 result does not apply |
| Seven targeted phone UI methods | **UNRUN** on this branch; source code added, no new emulator run |
| Whole phone Android/runtime & restore/ownership guards | **UNRUN**; Root owns the integrated gate |

The late-IO, readiness and fallback tests have **not been run**. No Android PASS, release-readiness or merge authorization is implied. Root should use the exact final PR HEAD, review this scoped delta, compile with the shared fixture import fix available, then run only the listed selectors in the coordinated integrated environment.
