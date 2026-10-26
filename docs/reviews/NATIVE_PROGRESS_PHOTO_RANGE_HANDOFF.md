# G10 — progress photo gallery / strength calendar range handoff

## Scope and exact ancestry

- Repository: `fuloplevente1998/TrainPilot`
- Required starting commit: `30e087646298fb0191f5ffa0e7e157b14519d669`.
- Independent branch: `feat/native-progress-photo-range-binding`.
- Proposed target: `feat/native-android-migration` (draft only; no merge).
- QA-only change: `.github/workflows/native-android.yml` **must not be integrated into the target**.

## Implementation

- Separate 30/90 calendar days, 1 local year, all-history strength choices; lower bound
  uses `atStartOfDay(zone)`, excludes future timestamps even in all-history mode.
- Preserves the original completed-set SQL, 1RM equation and exercise-scoped 256-row
  descending keyset; stops at the first out-of-window page.
- Existing Room invalidation events (now including `workout_photos`) reload results;
  cancellable per-request revision also excludes old exercise/range responses.
- Photo list is derived from `workout_photos` joined to its owning live
  `workout_history`, including tombstone/deleted checks. Private image location is
  resolved **only** from the existing repository's validated ID. No directory scan.
- Read-only, lazy thumbnail previews (256px sampling), one 1600px detail preview,
  explicit close; missing/corrupt photos have visible error states.
- Program, session and device source use persisted workout metadata; no file
  lastModified inferred as a measurement date or invented before/after labels.
- HU/EN/DE/RO string resources and existing palette integration. No new DAO,
  schema, writer, cloud upload, sync/export or other module modifications.

## Test gate — initial status (replace with observed evidence)

**UNRUN / PENDING.** Do not interpret this report or committed test sources as a PASS.
The own-branch PR-triggered QA is limited to:

1. `NativeProgressQueryTest` and `NativeProgressPhotoRangeTest` JVM
   (0/200/600/1500, DST/local boundary, future, deleted/orphan/missing/corrupt,
   bounded decode).
2. The three `NativeCardioProgressUiTest` tests plus three new
   `NativeProgressPhotoRangeUiTest` tests on API 35.
3. The same built debug and androidTest APK pair, raw instrumentation log,
   derived JUnit XML with six terminal results, raw adb screenshots and SHA-256
   manifest. Missing/skip/crash must fail the gate.

## Remaining limitations / integration instructions

- Scoped QA does not certify the combined Root suite or physical SDK devices.
- 1RM point date labels preserve existing `started` ISO strings; inclusion is
  based on the canonical `startedEpochMs`, not that formatted string.
- Private file path fallback is intentionally absent; a missing ID-linked file
  remains a user-visible error rather than an orphan JPEG.
- Root should transplant only app/test/resources/report changes after reviewing
  exact source and green evidence; do **not** copy the QA-only workflow.
