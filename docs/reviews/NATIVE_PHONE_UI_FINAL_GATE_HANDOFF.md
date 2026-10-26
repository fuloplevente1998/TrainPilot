# Native phone UI final gate – own-branch QA handoff (IN PROGRESS)

Updated: 2026-10-26. **Not an acceptance claim; the phone gate is NOT green.**

## Exact scope and provenance

- Repository: `fuloplevente1998/TrainPilot`.
- Exact pinned source: `de1fe14062cfe6675ec2b3c12d13945b327683b6` (from `feat/native-android-migration`).
- Own branch: `fix/native-phone-ui-final-gate`; PR target: `feat/native-android-migration`, draft only.
- First tested SHA: `c7acb659a7f3edd91642a1fb07b0dee620999937`.
- Second tested SHA: `15de8b096f89222bbedac61804bef13734c5c96a`; CI [40357491471](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40357491471) was still executing its emulator jobs when this report was written.
- Version and package **unchanged**: `com.repforge.app` / `1.3.71-native-test` / `2736`.
- No main/migration write, merge/rebase/force push, PR/issue closure, physical-device or real HC/Google data operation.

## Work done so far

1. Own-branch-only `.github/workflows/native-android.yml` push trigger explicitly enables QA. **Do not integrate the QA-only workflow delta into the migration branch.** It runs the existing native build, JVM, two signed upgrades, three Wear UI shards, main phone instrumentation and four isolated GPS cases.
2. UI production changes (all under permitted `android/app/src/main/java/com/repforge/app/ui/**`):
   - Retain the expanded program-day card state across LazyColumn recycling using `rememberSaveable`.
   - Retain expanded interval controls across workout item recomposition.
   - Give the nested Health source selector a stable LazyRow semantics tag for real UI interaction.
   - Retain prior Coach readiness on failed refresh/reload and keep the failure visible.
3. Phone instrumentation changes (only `android/app/src/nativePhoneUiTest/**`):
   - Poll the asynchronous emulator location-provider change before asserting, without disabling GPS permission tests.
   - Use the real Health nested scroll container for a selected HC origin; preserve origin persistence assertions.
   - Wait for actual workout pause navigation; scroll the Home shortcuts rather than assuming visibility.
   - Check selected Health date by substring inside its labeled text, not exact string.
   - Attach unmerged Compose semantics tree details to program comparison timeout errors while preserving all original assertions.
4. `scripts/native/phone-ui-gate.py`: save isolated `NativeModeApproval` diagnostics from logcat after failed main suite; no assertions weakened, no test omitted or timeout increased.

## Runtime evidence (separate exact SHAs)

### First QA source `c7acb659…`

- [Run 40345297542](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40345297542): native-debug and unit/build job PASS; both signed upgrades PASS; all three Wear shards PASS.
- From 163 JVM XML files: `app 1143 PASS / 0 fail / 0 error / 0 skip`, `wear 167 PASS / 0 fail / 0 error / 0 skip`; combined **1310 PASS**.
- From raw phone instrumentations: **94 terminal in the main suite = 77 PASS / 17 FAIL / 0 skip / 0 missing**, plus **4 isolated GPS = 3 PASS / 1 FAIL**, no duplicate reported. Combined 80 PASS / 18 FAIL. Phone gate FAILED, **not accepted**.
- Captured 42 real `adb exec-out screencap -p` PNGs; this report's offline verification checked all 42 PNG magic headers, per-image SHA-256, byte length and foreground `com.repforge.app/` focus fields against the manifest. The manifest correctly says `result: failed`. These PNGs are incomplete acceptance evidence, not a passed screenshot gate.
- Historical job log [40342319155](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40342319155) failed Android-test Kotlin compilation on the split-line Calendar ViewModelProvider lookup. Pinned base `de1fe140` already includes the explicit `.get(...)` fix.

### Second QA source `15de8b096…`

- [Run 40357491471](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40357491471): native-debug build/job PASS, including JVM compilation and signed update/signature verification. From downloaded 163 JVM XML files: **1310 PASS / 0 fail / 0 error / 0 skip** (1143 app + 167 wear).
- Both signed upgrade jobs completed PASS. At last inspection the phone Compose emulator and some Wear UI shards were still running. **No phone terminal result, screenshot count or overall PASS is claimed yet.**
- The corresponding branch build-apk/validate-branch jobs have separate failures; they are not reclassified as native phone evidence or overall project success.

## Known phone blockers, follow-up gate

The first run left 8 training-mode preview/approval cases, two interval-element cases, native program-day restore selector, Coach failed-refresh, custom-exercise delete, workout pause, Health daily-origin selector, Health selected-date assertion, Home shortcut and the isolated GPS location reset. This branch contains targeted fixes/diagnostics for many of these, but ONLY a fresh same-SHA phone run may demonstrate which resolved. Never disable the original `late cancel / restore ABA / active-paused / duplicate approve` guards.

Required before own-package closure: `compileDebugAndroidTestKotlin` PASS; all **98 source-declared** phone methods terminal PASS (94 main + all 4 isolated GPS), **zero failed/error/skipped/missing/crash**, original untouched screenshots and manifest complete, and offline `evidence-summary.py` success matching the exact app/test APK hashes and commit. Record both same-SHA native JVM and phone evidence. Physical Watch/HC/Google/OEM remain unverified on emulator.

The test result SHA, once green, must be distinguished from any later docs-only handoff commit. Root is responsible for common cross-package review/gate and explicit eventual integration; draft PR alone does not authorize merge or release.
