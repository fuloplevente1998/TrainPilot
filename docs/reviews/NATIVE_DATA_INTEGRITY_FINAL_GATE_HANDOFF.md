# Native data integrity — final scoped QA handoff

## Provenance and exact scope

- Required immutable start: `de1fe14062cfe6675ec2b3c12d13945b327683b6`.
- Own branch: `fix/native-data-integrity-final-gate`, **draft** [PR #260](https://github.com/fuloplevente1998/TrainPilot/pull/260) targeting `feat/native-android-migration`. No merge, rebasing, force push or modification to the migration/default branches.
- Full scoped QA **SUCCESS**: [Actions run 40351280170](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40351280170) (run #107), exact tested **source head** `b7c2971a4e720116a00088259d72de1a9b3dc727`; eight jobs `success`.
- The report-only commit following that run does not alter tested production, test or workflow code. The CI source head reported in raw emulator artifacts is identical to the tested SHA above. A further run after this report update may be linked separately if triggered.
- **Not full app CI**: own-branch workflow deliberately omits general Compose/Wear UI jobs. **Do not merge** the QA-only `.github/workflows/native-android.yml` replacement into the canonical migration branch; preserve canonical CI. The original app ID, versionCode 2736, versionName `1.3.71-native-test`, Gradle, schema, DAO, engine and UI were not changed in this diff.

## Change and regression rationale

Original `ArchiveIO.extract` used `ZipInputStream`, which may return all local ZIP entries without enforcing the central-directory/end record. The scoped change stages a **compressed-size-bounded ZIP file outside `incoming`**, requires `ZipFile` to parse the directory before payload acceptance, and retains expanded-byte, per-entry, count, allow-list and SHA-256 manifest checks. The staging bound includes a finite ZIP-header overhead allowance. JVM `ArchiveIOTest` reproduces truncated directory and corrupt central offsets. The first JVM rerun exposed an overhead-bound false rejection; corrected after reading the exact JUnit XML.

The new `NativeDataIntegrityArchiveTest` invokes the **actual native BackupRestoreManager** and uses synthetic emulated-only data. Exercises 0/200/1500 JSON import → export → isolated erase → restore → re-export; private photo ZIP bytes and re-export; invalid JSON and truncated ZIP reject without erasing an existing row. Synthetic fixture includes stable IDs, Unicode notes, bilateral timed sets and RIR, GPS **route keys** (not private coordinate samples), program/day, calendar, weight, tombstone, draft and portable settings, Wear/health summaries. No mocked successful cloud/health provider. The native signed upgrade script adds a second force-stop/start/verify without uninstall.

## Verified results — Actions 40351280170

| Exact gate | Result | Independent evidence |
| --- | --- | --- |
| JVM/Gradle debug unit tests | **PASS — 1,312/1,312; zero failures, errors or skips**, across 163 JUnit XMLs | Artifact `12662874759` |
| Release build, package and same-key signing gates | **PASS** | Artifact `12664236021`, native-debug job |
| Original v1.3.71 → native signed in-place upgrade | **PASS**: `seedStatus=ok`, `verifyStatus=ok` and `verify-second-startStatus=ok` (stored file has `verifyStatus=ok`), both `INSTRUMENTATION_CODE: -1`; installed version 2736 | Artifact `12665130392` |
| Original v1.3.71-perf-20261022 → native signed in-place upgrade | **PASS**: seed/verify/second-verify all ok, version 2736 | Artifact `12662784516` |
| JSON roundtrip, 0 rows | **PASS 1/1**, source == re-export semantic SHA `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | Artifact `12664965520`, strict XML zero failure |
| JSON roundtrip, 200 rows | **PASS 1/1**, source == re-export semantic SHA `8d2a6104edff1da632f6b552d6654ea4cba069bdecb5f04e9dc7e9a4509c5fc5` | Artifact `12663514318`, strict XML zero failure |
| JSON roundtrip, 1500 rows | **PASS 1/1**, source == re-export semantic SHA `6f062e3dd881f2c87ae9d80e9c1fd05541c0c8faaba7e3da80665641010ba8f0` | Artifact `12664686028`, strict XML zero failure |
| ZIP photo private bytes and re-export | **PASS 1/1**, both photo SHA-256 `a65a9fce2995cbd9ed6a401d68b48fd22e3d1d4edbee6b98482b396fc62aa032` | Artifact `12664710895`, strict XML zero failure |
| Invalid JSON/truncated ZIP protection | **PASS 1/1**, existing 1 workout retained, replacement receipt/fence cleared | Artifact `12663024932`, strict XML zero failure |

All five exact Android instrumentations report `INSTRUMENTATION_STATUS_CODE: 1` followed by `0`, a single `OK (1 test)`, terminal `INSTRUMENTATION_CODE: -1`, and exactly one data-assertion `status=PASS` logcat receipt. JUnit XMLs were generated in a **separate GitHub Actions Bash step** requiring the same exact test method, absence of duplicates/crashes, a matching source SHA and the logcat receipt. Each raw/XML/logcat/source artifact has a local `artifact-sha256.txt`.

Downloaded artifact ZIP SHA-256 (for exact evidence reuse):
- JVM `12662874759`: `233ff595bf679786a61b4fbaa5aa820f64efdcd478ed01cacf4d3dc91bba8f1e`
- 0 `12664965520`: `eef897ffb72be32e462ce28c96ddf8f750d63023cd7db3560532611c815894e4`
- 200 `12663514318`: `8dd192a3804fb6783c0c1e8a2c83fcaf152eef501fc0733afa167fcc957a8257`
- 1500 `12664686028`: `66ffafdeb7de1d14a7cb48fa85fab27c8a746bf4980b0e26587a183ec2678d79`
- ZIP photo `12664710895`: `6630c10e3184f90ae7b70091e6a437cc0c4b6ac2a821af0a8d63597d3acb0f3b`
- Invalid import `12663024932`: `1230d42ae99033e23582de79e659c4414af8895ada76b7ba76c68df9d6fac31d`
- Original upgrade `12665130392`: `3cb9e26ddf93f09d2609664fe359b5c7894f76b18d42afb6474609938865da55`
- Perf upgrade `12662784516`: `8854cdf1a0b8abc07fc357856a075b7b2bdceb896ce32f12ead6b743cc92bb5f`

## Failures actually observed and corrected

1. First run `40347005837` failed compiling new test JSON fixture: missing Kotlin closing parenthesis. Fixed at `dc463fcf`.
2. Run `40347521905`: 1,145 JVM tests, two ZIP regressions falsely rejected tiny but structurally valid compressed archives because the compressed-size cap excluded ZIP overhead. Corrected at `ee32930e` after JUnit XML inspection.
3. Run `40348514178`: all five raw Android instrumentations reported `OK (1 test)` and both signed upgrades passed, but all five emulator **jobs** failed because the emulator action executes the embedded Python heredoc line-by-line (`/usr/bin/sh: import: not found`). Moved the strict parser into a separate Actions `run` step, fixed its regex literal, and reran to full job success at `b7c2971a`.
4. Separate general `Quick TrainPilot Validation` / `Phone Release Gate` jobs may fail due to the repository's root npm lockfile/caching setup, outside this locked data-owner scope; **do not interpret own scoped QA success as full-app CI success, and do not edit out-of-scope package files**.

## Remaining non-claims and integration boundaries

This is a **fixture-bounded emulated Android data-integrity gate**. It does not establish losslessness for unseen customer archives, proprietary Health Connect/Samsung providers, logged-in Drive account data, real phone photos/GPS coordinate sample retention, or physical-device update behaviors. GPS coordinate privacy exclusions are deliberate; preserving allowed route key references is not a live GPS recording assertion.

Integration: selectively cherry-pick/reapply only approved data-scope fixes and tests if reviewed; **exclude** this branch's QA-only workflow. No PR merge performed, no version/core/schema/UI/physical user data changed.
