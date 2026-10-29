# PR #270 — 21:02 AI Studio merged export integration review
**Date:** 2026-10-28. **Status:** P0/P1 open, export is **not** authorized to replace the native source tree. **Design source:** the user-approved 18:49 export only.

## Source / audit evidence

- 21:02 ZIP: `docs/ai-studio/trainpilot-oct-27-2026-9-02-00-pm.zip`, 8,846,684 bytes, SHA-256 `db5ce026e2559366a7d3366332ab64affef7448839a7727f0dbfce447d7d65d9`.
- Approved original 18:49 ZIP: `docs/ai-studio/trainpilot-oct-27-2026-6-49-09-pm.zip`, SHA-256 `b699d72bc2b673cefbd3dabbdb8164f681c35762e47d19adf42c12e6b22eabf1`.
- Independent read-only GitHub Actions [source comparison #40632319565](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40632319565), successful, and [full report/source snapshot #40632429214](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40632429214), successful. Artifact `studio-2102-vs-1849-source-audit` from #40632429214 includes exact launcher / Studio ViewModel / demo provider excerpts. This is **not** proof of a running integrated APK.
- The ZIP contains 479 total archive entries (directories included); 379 file entries counted by the CI script; 16 Studio-source Kotlin files, all retained, 12 byte-identical to 18:49 and 4 changed (`LogAndProgressScreens.kt`, `TrainPilotPalettes.kt`, `Type.kt`, `TrainPilotViewModel.kt`). 165 native Kotlin files are present under `com/repforge/app/` and additional native Java, resources, build files exist.
- Native exported Kotlin / repository parity is relative to a moving HEAD: CI showed 149 of 165 native Kotlin paths identical when compared on its HEAD. Another static review found 160 of 176 native Kotlin/Java after normalizing module paths against earlier SHA `dc6ffc18`. Different denominators and comparison SHAs; do not treat either as a complete feature parity assertion.

## Verified launcher bridge: partial, not production state parity

1. Manifest MAIN + LAUNCHER points to **`com.repforge.app.MainActivity`**, not to the prototype-only `com.example.MainActivity`.
2. ZIP native `MainActivity.kt:67–103`: settingsFlow is collected and `syncNativeSettings` forwards language, themeKey, restSec, sound and vibration to the **Studio TrainPilotViewModel**; it then renders `TrainPilotAppRoot(viewModel = viewModel)` and its Studio theme. Thus the display route does reach the original Studio components.
3. This is **one-way** native → Studio settings hydration; Studio ViewModel methods currently mutate its own StateFlow without committing changes to `TrainPilotSettingsStore`. The app will not reliably persist Studio setting changes across process death without a proper write bridge.
4. ZIP `app/build.gradle.kts` uses sandbox `applicationId = "com.aistudio.trainpilot.tpux01"`. This is **not** proof of update/install continuity with the released phone app. Never copy the sandbox application identity, manifest or signing configuration into PR #270.

## Critical native function/state gaps

| Severity | ZIP location | Verified behavior | Acceptance requirement |
| --- | --- | --- | --- |
| **P0** | `com/repforge/app/data/migration/LegacyLocalStorageReader.kt` lines 23–30,37,66–67 | WebView read/init errors and timeout are converted to `emptyMap()` through broad `catch(Throwable)`, 60 s timeout reduced to 8 s; cancellations can be swallowed | Use **canonical PR270 native** `LegacyLocalStorageReader` (60 s, propagates errors/cancellation). Keep `LegacyWebViewMigrator` completion guard and test interrupted/failed migrations never mark complete |
| **P0** | ZIP native `MainActivity.kt:89–104` | Renders `TrainPilotAppRoot` whenever recoveryNeeded is false, even before `migrationReady`; migrationFailure/readiness retry panel is no longer the visible gate | Preserve canonical PR270 `MainActivity` `if(recoveryNeeded) ... else if(migrationReady) ... else {loading/error+retry}`. **Only** after migrationReady show the approved Studio shell, eventually binding genuine native ViewModels |
| **P1** | Studio `TrainPilotViewModel.kt:519–541` | `finishAndSaveWorkout()` appends a fixed-date, fixed-duration DEMO entry to StateFlow, no Room finalization | Delegate to real native workout completion/history repo; maintain active session, ownership/ack and cancellation invariants |
| **P1** | Studio `TrainPilotViewModel.kt:653–660` | `saveNewBodyWeight()` modifies only volatile StateFlow | Persist through native weight repository and actual journal write validation |
| **P1** | Studio `TrainPilotViewModel.kt:673–679` | `triggerHealthSyncNow()` adds 120 fake steps | Call native Health Connect/Samsung integration with permission and freshness/source guards; preserve missing/partial/error states |
| **P1** | Studio `TrainPilotViewModel.kt:683–691` | `toggleWatchControlMode()` toggles demo boolean / message | Route through native Wear ownership/ack/handoff without taking ownership or editing session unless the native protocol accepts |
| **P1** | Studio theme/language/audio/rest selectors | Changes only Studio StateFlow | Write through `TrainPilotSettingsStore`; ensure round-trip and 4 language/32 Studio palettes without losing stored old themes |
| **P1** | ZIP test/build configuration | 3 generic Example tests and no independent full native exact-source testing included in export; Studio Theme clamps fontScale | Retain native regression suites and test accessible large font, all four locales, actual palettes, paired Wear and signed upgrade |

The P0 issue is a **possible missed migration and false success**, **not a proven data-loss incident**. No destructive native migration source change was applied while conducting this review.

## What to do in PR #270, in order

1. **Freeze the native persistence safety boundary:** keep canonical `LegacyLocalStorageReader`, `LegacyWebViewMigrator`, `MainActivity` recovery/migration UI, `applicationId`, migrations, signer and all native repositories. Add a fail-closed automated source guard.
2. Reuse **18:49 Studio UI component tree** for the real, post-readiness native app, not a separate DEMO runtime. The 21:02 ZIP shows how `TrainPilotAppRoot` can render, but **not how complete native functional state is connected**.
3. Connect one native feature end-to-end at a time: settings write+read, actual workout save, weight save, provider Health sync, Wear ownership. For each: replace demo callback, preserve original native transaction and lifecycle guards, verify DB re-read after relaunch, and screenshot approved Studio UI on the **same commit**.
4. Route real Progress/Napló/Health/Coach data to Studio cards without hardcoded examples. The original main anatomical muscle renderer and real GPS segmented route stay canonical exceptions to Studio's demo graphics.
5. Run exact-source JVM, phone Compose UI, Wear, old-key signed upgrade, and native screenshot evidence. Keep PR DRAFT, main untouched. An exported PASS claim is not a test result.

## Review and CI status

The first native compile run for prior source SHA `dc6ffc18` failed for a missing `border` import in `StudioProgressTabs.kt`; this import was repaired in PR #270 after the run. Quick and phone-only gates on subsequent commits are NOT proof that the native/Wear device gate passed. Do not label the 21:02 ZIP integrated, release-ready, or installed-update safe until exact-source CI and visual parity evidence is available.
