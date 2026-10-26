# Program-level training mode approval UI – scoped handoff

Date: 2026-10-26  
Task: `docs/plans/NATIVE_MODE_UI_REPAIR_PLUS_HANDOFF.md` (linked revision `e8985f10`)  
Base: `98ef71b2e4f2fa4f08b415c3d0a94d113b62303d` from `feat/native-android-migration`  
Own branch: `fix/native-training-mode-approval-ui`  
Code/test head before this handoff: `6f32fbe1ac61ca7e36d85c5207902b5fd6803687`  
Final branch head: see the own draft PR (handoff commit changes SHA).

## Reproduction and diagnosis

The **previous source**, GitHub Actions run [40317771457](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40317771457), checkout `e28ab418`, artifact `12649155506`, raw `instrumentation.txt`, reports:
`NativeTrainingModesUiTest.programPickerApprovalOverridesGlobalWithoutChangingProgramRecipes` → `ComposeTimeoutException`, at `NativeTrainingModesUiTest.kt:159`, waiting 30 s for `training_modes_program_approve` to become enabled. The same earlier run completed 75 tests: 35 PASS / 40 FAIL; three other `NativeTrainingModesUiTest` cases passed. This is **not a run of this branch**.

Source-level failure paths on the pinned base:
- The program picker was inside a virtualized `LazyColumn.item` in `TrainingProgramsScreen`; the modal proposal and `LaunchedEffect` comparison owner were also created by that lazy item. Item disposal could cancel/recreate the comparison and leave the test waiting for enabled approval. The old raw artifact contains no application-level comparison exception or dialog-state trace, so this ownership mechanism is a source-derived risk, **not a runtime-proven sole cause**.
- `NativeTrainingModeComparison.read` could return an empty snapshot for absent program/days or silently omit unmatched before/after exercises. A nonempty partial comparison could previously authorize approval. The actual exceptional condition in the earlier timed-out run cannot be recovered from that log alone.
- The former UI test waited for approval without distinguishing comparison failures from ongoing loading.

## Scoped implementation

1. `TrainingProgramsScreen.kt`: holds the program picker proposal state and approval dialog outside `LazyColumn`; the settings card stays at its original list position. State is keyed to settings raw and active program ID.
2. `NativeTrainingModePicker.kt`: shared optional state holder and separated program card/dialog rendering; one dialog-side comparison effect with cancellation tied to the exact proposal; approval checks raw/active/program identity, nonempty ready rows and matched proposal, and consumes pending state before dispatch. Global card/dialog still use the prior combined call site. Failure is surfaced in UI and logcat via `NativeModeApproval`; no unconditional enabling.
3. `NativeTrainingModeComparison.kt`: strict before/after exercise identity, nonempty actual rows, no silent missing-program/missing-day acceptance; rereads settings and program raw/day recipes after comparison. Provides a read-only `isCurrent` recheck.
4. `TrainingProgramsViewModel.kt`: program approval path validates the displayed comparison again on IO, then calls the **existing** `NativeTrainingModeChange.approve`, which owns active-workout/restore admission and exact settings/active-program CAS. No new writer; no program recipe writes.
5. New `NativeTrainingModeApprovalUiTest.kt`: real disposable-emulator integration tests for HIT/RIR rows with no write, program preview/cancel preserving settings and recipes, and a competing settings update rejecting stale approval. This is test code, not executed evidence.

No modifications to `NativeTrainingModeChange`, `TrainPilotSettingsStore`, shared fixture, Settings screen/VM, navigation, workflow, dependencies, schema, versions, Calendar, GPS or Wear. No translations added.

## Regression classes to run (runtime UNRUN)

**Native phone UI (instrumentation):**
- `com.repforge.app.NativeTrainingModesUiTest` — four original methods unchanged.
- `com.repforge.app.NativeTrainingModeApprovalUiTest` — three new methods.
- `com.repforge.app.NativeTrainingModeCommitUiTest` — existing main-thread admission / active-workout / stale ticket guards.

**Related JVM:**
- `com.repforge.app.NativeTrainingModeChangeTest`
- `com.repforge.app.NativeTrainingModesTest`
- `com.repforge.app.NativeTrainingModesStoreTest`
- `com.repforge.app.NativeTrainingModeProgressionTest`
- `com.repforge.app.NativeTrainingModeProposalTest`

Global preview/cancel/approve/undo and inheritance remain in `NativeTrainingModesUiTest`, and the global-picker-with-program-override case remains present. Original JVM assertion bodies were not edited.

## Checks performed on this branch

| Check | Status / evidence |
| --- | --- |
| Earlier `e28ab418` failing artifact | **READ**: raw ZIP `12649155506`, `instrumentation.txt`, test 30 s timeout at line 159 |
| Branch source | **PASS**: new branch forked at exact base `98ef71b2...`, no protected-branch writes |
| Path-scope check | **PASS**: GitHub compare base → code head lists only four permitted Kotlin production files and one permitted new UI-test file; this report is the sixth allowed file |
| Kotlin delimiter/lexical static check | **PASS**: balanced delimiters on the four changed production files, original four-test file, and new UI-test file (simple source scanner, not compilation) |
| Original test retention | **PASS**: four `@Test` methods in `NativeTrainingModesUiTest` verified present; no existing test/fixture deletion |
| Kotlin/Android compilation | **UNRUN** (no local Gradle; no scoped runner available here) |
| JVM test execution | **UNRUN** |
| Compose/instrumentation execution | **UNRUN** |
| Emulator screenshot / APK | **UNRUN** |
| Full `native-android.yml` gate | **NOT TRIGGERED**, reserved for Root |

## Remaining acceptance / limits

Root should run only the listed relevant classes on the common integration source, inspect any `NativeModeApproval` logcat errors, and verify program HIT/RIR approval + re-inheritance, rawJson equality, stale/active-workout/restore guards and duplicate approval in runtime. In particular, the old timeout is **not claimed fixed by a green emulator run**, because no new runtime ran here. The proposed lazy ownership explanation needs runtime confirmation. No merge or installation approval is implied.
