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

## PR #259 second-review follow-up (2026-10-26)

Review contract: `docs/plans/NATIVE_PR259_REVIEW_FOLLOWUP.md` at `60eeab4a`, review HEAD `9b1a2e2265117825d9b5c7d4396d61639a8855ba`. The continuation stays on the same `fix/native-training-mode-approval-ui` draft branch/PR. The original six paths remain unchanged; one additional named test file below is within the permitted new `NativeTrainingModeApproval*Test.kt` scope. Exact **current** HEAD is the branch SHA associated with the PR; this handoff write itself creates one last commit.

### Re-reviewed root cause and code change

Previous `isCurrent(context, preview, snapshot) && NativeTrainingModeChange.approve(...)` contained a TOCTOU window: a legitimate `NativeProgramEditor.save` could edit program recipes after revalidation, before `saveApprovedTrainingModes197` admitted the settings CAS. The settings/activeProgram CAS does not compare program recipes. Now `TrainingProgramsViewModel.commitComparedTrainingMode` wraps **both** operations in one outer `NativeDataReplacement.mutate(context)`. The nested `NativeTrainingModeChange.approve` calls the existing DataStore admission and CAS unchanged. `NativeDataReplacement` coroutine `Access` explicitly supports reentrancy: no extra lock, writer or bypass.

`NativeTrainingModeComparison.read` captures `NativeDataReplacement.continuationForCurrentData(context)` **before** its program/settings reads, stores the `Continuation` inside the existing `Snapshot`, and checks it in `isCurrent` before and after revalidation. Replaced/restored bytes that are identical still have a different replacement epoch, and the old snapshot fails closed. No new settings schema or persistent token.

Verified read-only sources: `NativeDataReplacement.kt` mutex/Access/replacementEpoch/Continuation; `NativeProgramEditor.kt` save/delete use `mutate` with Room transactions; `NativeProgramActions.kt` restore uses `mutate` then `NativeProgramEditor.save`. The lease covers **these audited program writers** and restore/erase. It is not a claim of process-wide atomicity for uninspected or nonparticipating direct DAO writers or non-recipe Coach history updates. Those would need their own Root audit, not changes outside this PR's file scope.

### New controlled regression source (not runtime results)

`NativeTrainingModeApprovalUiTest` keeps its original 3 methods and adds 5:
- `uiEmptyComparisonNeverEnablesApproval` — actual empty custom program, error state and disabled Approve, no settings write.
- `uiApprovalRejectsProgramRawChangeAfterShownComparison` — preview via Compose, then real `NativeProgramEditor.save` metadata edit, click approval, verify rejection/settings and saved recipe intact.
- `uiApprovalRejectsProgramDayRecipeChangeAfterShownComparison` — Compose preview, real program edit removing a day exercise, click approval, verify rejection and changed recipe intact.
- `uiApprovalRejectsSameBytesAfterReplacementGenerationChanges` — Compose preview, actual `NativeDataReplacement.exclusively` generation boundary with unchanged bytes, click approval, verify rejection.
- `doubleClickCannotApproveProgramTwice` — UI consumes proposal, no second confirm, one revision increment.

New `NativeTrainingModeApprovalRaceTest`:
- `queuedProgramEditorSaveInvalidatesComparisonBeforeApprovalCas` — `CompletableDeferred` gates order a legitimate editor save under the metadata lease ahead of a competing VM approval; stale approval must return false, settings unchanged.
- `activeAndPausedNativeWorkoutBothDenyComparedApproval` — real quick workout then real execution pause via `PhoneWorkoutExecution.setWorkoutPause`; existing admission must deny both; no mode write.

All **four** original `NativeTrainingModesUiTest` tests remain present and unchanged, including global preview/cancel/approve/undo, selected-program global override, and program inheritance with separate confirmation. The existing `NativeTrainingModeCommitUiTest` remains read-only/unchanged and continues to cover main-only execution admission, stale tickets, and active session in targeted runtime.

### Follow-up check matrix (as of this handoff)

| Verification | Status | Evidence / qualification |
| --- | --- | --- |
| Review baseline pin | **PASS** | Branch fork remains `98ef71b2e4f2fa4f08b415c3d0a94d113b62303d`; follow-up diff compared with `9b1a2e2265117825d9b5c7d4396d61639a8855ba` |
| Path and original-test preservation | **PASS – static** | Production only ViewModel/Comparison; test changes only two `NativeTrainingModeApproval*Test.kt`; handoff only this allowed review file |
| Kotlin source delimiter scan | **PASS – lexical only** | `TrainingProgramsViewModel`, `NativeTrainingModeComparison`, picker, two own test classes and untouched original four-test class |
| Real race/admission proof on new source | **UNRUN** | Barrier and active/paused tests written; no Android runtime executed |
| Kotlin compilation | **UNRUN** | No local Gradle by contract. Standalone earlier branch also lacks `NativeLocalizationEmulatorFixture.kt` shared `performScrollTo` / `assertIsDisplayed` imports; Root already fixed both in `e12ed8a5`. No unauthorized fixture copy: integration compile must include Root fix |
| JVM cases | **UNRUN** | See exact list below |
| Compose/phone UI instrumentation | **UNRUN** | No own scoped runner made available |
| Full native CI / signed APK / screenshots | **NOT TRIGGERED** | Root-owned only |

**Exact phone instrumentation classes**: `com.repforge.app.NativeTrainingModesUiTest` (4 unchanged), `com.repforge.app.NativeTrainingModeApprovalUiTest` (8 total), `com.repforge.app.NativeTrainingModeApprovalRaceTest` (2), `com.repforge.app.NativeTrainingModeCommitUiTest` (2 existing). **Exact JVM classes**: `com.repforge.app.NativeTrainingModeChangeTest`, `com.repforge.app.NativeTrainingModesTest`, `com.repforge.app.NativeTrainingModesStoreTest`, `com.repforge.app.NativeTrainingModeProgressionTest`, `com.repforge.app.NativeTrainingModeProposalTest`.

**Acceptance still pending:** actual compile + targeted emulator run on Root-integrated source, including logcat comparison errors. Original e28 timeout was not reproduced on this branch and no new PASS is claimed. No PR merge, main modification or install approval.
