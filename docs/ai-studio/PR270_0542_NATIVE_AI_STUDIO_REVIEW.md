# AI Studio 05:42 native integration audit — 2026-10-28

**Read-only source review; NOT a built or verified Android integration.** This document compares the new separate public repo archive with the older 21:02 ZIP, approved 18:49 Studio reference and current PR #270 branch. No ZIP app code has been copied into the native application.

## Exact sources and provenance

- New ZIP in [Native-AI-Studio](https://github.com/fuloplevente1998/Native-AI-Studio/blob/2db71628c604fb361e7197a7c41b2a0ed9299cea/trainpilot-oct-28-2026-5-42-24-am.zip): **8,861,301 bytes**, SHA-256 **`8f91e79d604de2415ee57bd6094c453f1512a0f2b1c93caf354da31f75b6a308`**. Pinned source repository commit `2db71628c604fb361e7197a7c41b2a0ed9299cea`.
- Previous PR270 checked-in 21:02 ZIP: 8,846,684 bytes, SHA-256 `db5ce026e2559366a7d3366332ab64affef7448839a7727f0dbfce447d7d65d9`.
- **Approved visual reference remains 18:49** `trainpilot-oct-27-2026-6-49-09-pm.zip`; SHA-256 `b699d72bc2b673cefbd3dabbdb8164f681c35762e47d19adf42c12e6b22eabf1`. **Brand exception:** use the *original gold dumbbell launcher asset*, not the Studio mountain.
- Independent GitHub Actions [read-only comparison #40634881569](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40634881569), exact-worktree SHA `36d5b348eaa3553a9523e4132da3b831524d55c5`, **SUCCESS**. Artifact `studio-0542-native-integration-review` includes the full report and public source diffs for the three changed Kotlin files. The ZIP CRC test passed for all three compared archives.

## Scope of actual progress, not inferred from ZIP size

Both 05:42 and 21:02 ZIPs contain **479 total ZIP entries / 379 files**. Normalized archive path comparison found **0 added**, **0 removed** and **4 modified** files:

1. `app/src/main/java/com/repforge/app/data/migration/LegacyLocalStorageReader.kt`;
2. `app/src/main/java/com/repforge/app/MainActivity.kt`;
3. `app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt`;
4. `AISTUDIO_REFINEMENT_REPORT.md`.

Studio Kotlin: 16 files, 15 unchanged from **21:02**; compared with approved 18:49, all 16 are present and 12 byte-identical. Native Kotlin/Java: 166 files in both ZIPs; 164 unchanged, **2 changed**. Compared with the *moving PR270 current worktree*, 150/166 native source files were byte-identical and 16 differed; these 16 include PR270's independent ongoing UI and data updates and are **not** 16 newly changed files in the 05:42 ZIP.

The repository **outside** the ZIP contains standalone Studio DEMO source; the embedded ZIP is a distinct composite project and must be evaluated separately.

## Positive findings: actual changes

### P0 migration safety regression from 21:02 reversed

- `LegacyLocalStorageReader` removes the earlier `catch(Throwable) → emptyMap()` fallback on WebView creation/read, stops treating missing `app_webview` as an automatic empty result, and restores `withTimeout(60_000)` (rather than 8,000 ms).
- The native `MainActivity` restores `else if (migrationReady) { TrainPilotAppRoot(...) }`, visible loading before readiness, and a 4-language `MigrationFailurePanel` with explicit retry. Native cancellation handling is preserved. **This fixes the identified silent false-success *code path*, not proof of a successful migration/upgrade test.**
- `MainActivity` now calls `viewModel.bindNativeEnvironment(applicationContext, settingsStore, database, lifecycleScope)` before rendering Studio.
- The manifest still launches `com.repforge.app.MainActivity`, so the Studio components appear inside the native activity.

### P1 actual data/store integration started

- `TrainPilotViewModel.bindNativeEnvironment` reads the Room body-weight summary, and reads saved workouts through `getAllWorkoutsWithDetails()`, mapping date, set counts, volume and PR flags into the Studio history list. This is **genuine read access**, though it loads the entire workout history, synthesizes fixed `30:00` work and `15:00` rest labels, and combines persisted rows with initial DEMO entries.
- `selectLanguage` calls `TrainPilotSettingsStore.setLanguage`; `selectPalette` calls `setTheme`, reducing purely volatile settings behavior. Timer/sound selection calls `setTimerDefaults`.
- `saveNewBodyWeight` now attempts a Room `BodyWeightEntryEntity` upsert, but optimistically shows success and silently logs failures; needs correct validation, native write-guard and read-back.
- `finishAndSaveWorkout` attempts Room persistence through `LegacyWebViewMigrator.saveWorkoutJsonToRoom` and a generated UUID. **This is not canonical active-workout finalization.**

## Still unsafe as a native app replacement

| Severity | Verified 05:42 behavior | Required replacement/binding |
| --- | --- | --- |
| **P0 / functional correctness** | `finishAndSaveWorkout` constructs a fabricated 38-minute / 1,280 kg / personal-record entry; Room payload hard-codes `started = now - 2280 seconds`, `programId = home_ab_basic`, `dayId = day_a`, and `exercises = JSONArray()`. The UI reports successful save and switches to Log **before** its asynchronous write; write exceptions are only logged. | Use canonical native workout session finalization, real sets and intervals, ownership guards and authoritative completed-row read-back. Never claim success on failure. |
| **P1 / data integrity** | `saveNewBodyWeight` changes state first, launches an asynchronous Room upsert, then swallows exceptions. No validated values, reload or generation/restore guard evident in this bridge. | Delegate to native weight repository with validation/restore fencing and verified success/error state. |
| **P1 / provider truthfulness** | `triggerHealthSyncNow` **still increases dailySteps by 120**, shows a successful sync message, then merely schedules `HealthBackgroundJob` and publishes a Watch snapshot. | Use native Health Connect/Samsung sync result, permissions and source freshness, no fake readings, show pending/error correctly. |
| **P1 / Wear session ownership** | `toggleWatchControlMode` still flips local `phoneControlsWorkout` and says workout handed to Galaxy Watch7, then publishes a Watch home snapshot; **no actual native ownership handoff/ack** here. | Use existing Wear ownership, ack, finalization and disconnected guards. |
| **P1 / settings data** | Theme/language/rest writes start in background after UI already claims success, exceptions swallowed; `setTimerDefaults` also uses hard-coded work=30 and rounds=3. | Persist complete native settings without resetting unrelated timer defaults, confirm commits and retain saved keys. |
| **P1 / display data quality** | Saved workouts mapped into Studio list with fake work/rest split and loaded together with `DemoDataProvider` initial items; Coach/demo modes still synthesize readiness or data. | Native-only projected state, no DEMO records, correct empty/error/freshness states. |
| **P1 / install identity** | ZIP `app/build.gradle.kts` still sets `applicationId = "com.aistudio.trainpilot.tpux01"`, despite its native manifest/activity. | Preserve PR270 released applicationId, signing, Room migrations and signed upgrade. |
| **P1 / test evidence** | Only 4 changed files in export; no new native regression suite; ZIP not independently built/emulated or signed-upgrade tested. | Native exact-source JVM, phone Compose UI, Wear, signed upgrade, real screenshots at known SHA. |

**No proof of actual user data loss.** The important conclusion is that the read/migration guards were restored and some Room/DataStore bridges added, but important **fake-data and premature-success logic remains**.

## PR270 continuation decision

1. **DO NOT copy the full 05:42 ZIP** into PR270. Keep PR270's canonical native activity/migration reader, permanent applicationId, signing, workout finalization, Health and Wear engines.
2. Reuse the **non-destructive integration ideas**: native environment injection, Room-to-Studio presentation projections, durable settings actions, and migration-ready gating, but implement them in PR270's real native repositories and `NativeDataReplacement` guards rather than trusting the prototype bridge.
3. Display the accepted **18:49 original Studio Compose UI** and original **gold dumbbell launcher symbol**, not the newer compiled mockup as a design benchmark.
4. End-to-end next milestone: one real Studio screen reads native Room data and its write action round-trips through native storage, guarded by migration recovery and measured with an exact-SHA emulator screenshot. For each missing feature, replace demo action with a native, authoritative callback.
5. Keep PR #270 DRAFT and main unchanged. CI audit PASS establishes **source review only**, not APK or UI behavior.
