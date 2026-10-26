# Native data integrity – scoped gate handoff

- Start (read-only base): `de1fe14062cfe6675ec2b3c12d13945b327683b6`
- Branch: `fix/native-data-integrity-final-gate`
- Target (draft only): `feat/native-android-migration`
- Scope: **scoped data QA**, not full application or Wear/Compose CI; never merge the own-branch workflow into the canonical branch.
- Head at first QA submission: set from GitHub after all test-code commits.
- Android package/version (unchanged): `com.repforge.app`, `1.3.71-native-test`, `2736`.

## Investigated implementation and reproduced risk

The original `ArchiveIO.extract` used `ZipInputStream`, which may accept complete local entry records without the ZIP central directory or its end record. This could stage an incomplete archive with apparently valid entries. The fix copies the bounded compressed stream into an internal temporary file **outside** `incoming`, requires `ZipFile` to parse the central directory before extracting, and retains the entry count, size, allow-list and SHA-256 manifest constraints. Added explicit truncated-directory and corrupt-offset tests to `ArchiveIOTest`.

## Fixture-only validation plan

- JVM: all pre-existing native/phone/Wear/shared unit tests and new `ArchiveIOTest` regressions.
- Android instrumented: exact `NativeDataIntegrityArchiveTest` methods, each on a separate disposable emulator, with raw instrumentation output, independently parsed strict JUnit XML and logcat evidence.
- JSON: **0 / 200 / 1500** generated workouts with stable IDs, duplicated start times, Unicode notes, timed bilateral sets/RIR/route keys and sample portable program, weight, calendar, tombstone, draft, health/Wear summary. Actual import → export → isolated erase → import → re-export; semantic SHA-256 and counts compared.
- ZIP: private photo fixture, source and re-export bytes/hash compared, and no external photo absolute path in `backup.json`.
- Fault path: invalid JSON and truncated ZIP must fail without replacing existing history or committing a journal.
- Signed upgrade: original v1.3.71 and v1.3.71-perf-20261022, original release SHA/signing continuity, `adb install -r`, seed fixture, verify, restart, verify again. Never uninstall old test app.
- No live Health Connect, Drive, cloud account or physical phone traffic. No claim of general data safety for real user datasets. Deliberate GPS coordinate privacy exclusion is respected, not counted as a lost route.

## Evidence and gate outcomes

**Not yet verified at commit time.** The checks above are expected assertions, not proof until a GitHub Actions run has completed and raw/XML/artifact hashes have been examined. A red, skipped, missing or crashed test remains FAIL/UNRUN, never PASS.

## Integration exclusion

Do not merge this branch's QA-only `.github/workflows/native-android.yml` into migration: the parent branch keeps canonical full application UI/Wear jobs. Root should select only in-scope application/test changes after review. No other source files, version, Gradle, schema, core engine or UI were modified.

## External constraints

An emulator can verify seeded data flow only. Physical handset continuity, account-bound cloud/SDK behavior, and every unseen customer dataset remain unverified and require separate validation.
