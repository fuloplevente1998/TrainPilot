# Native Home Recovery – provenance handoff (G01)

- Scope: Home only. Exact pinned start: `30e087646298fb0191f5ffa0e7e157b14519d669`.
- Branch: `feat/native-home-recovery-provenance`. Target PR base: `feat/native-android-migration`.
- UI/VM: `ui/home/HomeScreen.kt` and `HomeViewModel.kt`, new `NativeHomeRecoveryPresentation.kt`.
- Tests: `NativeHomeRecoveryPresentationTest`, `NativeHomeRecoveryUiTest`. Existing Home/Coach regression classes are not weakened.
- Translations: HU/EN/DE/RO in the scoped native_home_recovery.xml files.
- Workflow change: `.github/workflows/native-android.yml` is **own-branch QA only; DO NOT integrate it** into the target migration branch.

## Boundaries and input tracing

Home reads `HealthJournalStore.page(today - 14 days, today, "journal", ...)` on Dispatchers.IO;
the underlying journal produces a canonical provider-projected `days` snapshot
with `day`, `energyScore`, `energyScoreTime`,
`energyScoreSource`/metricProviders and `partial` fields. The separate Room
`getCoachRecentHistory(now - 14 days, now)` query is also executed on IO.
No all-history hydration, foreground provider SDK read, synthetic energy value,
history write, database schema change, or 250ms polling is introduced.

The pure adapter reuses `NativeCoachSignals.fromDays` and
`NativeCoachReadiness.calculate`. A verified **today** Samsung reading is
displayed as the uncapped `samsungScore`, with its actual source and timestamp.
The Coach's load/pain-capped `score` is never used for this tile. Without
verified Samsung energy, only observed `trainpilotScore` is an estimate.
Without observations, the display is a dash and explicitly **no data**.
Journal `partial` and read errors are separately visible.

The shared `NativeCoachReadiness.validEnergy` currently rejects zero with
`value <= 0`, contrary to the G01 valid 0–100 range. Because Coach is
READONLY for this task, the Home adapter uses the **same canonical validity
logic** with a defensive copy of an *existing exactly zero* record, setting
only the value to 1 for validation, then presents the original zero. It does
not write the copy or change the Coach score. **Upstream patch suggestion**:
in `NativeCoachReadiness.kt`, change `if (value <= 0 || value > 100)`
to `if (value < 0 || value > 100)` and add zero-specific shared validator
tests; afterwards delete the isolated Home compatibility probe.

`HomeLocalRefresh` already listens for date/time/timezone changes and
lifecycle resume; settings flow triggers language refresh, and Room invalidates
after persisted state changes. The ViewModel cancels previous loads and admits
results only for the current atomic request generation. `NativeDataReplacement`
admission remains unchanged; cancellation is always rethrown.

## Targeted QA matrix

JVM: `NativeHomeRecoveryPresentationTest`,
`NativeHomeDailyCountTest`, `NativeHomeRepositoryTest`,
`NativeCoachReadinessTest`, `NativeCoachSignalsTest`.

API 35 instrumented: `NativeHomeRecoveryUiTest`:
verified Samsung source/time; TrainPilot-only fallback; no-data;
HU/EN/DE/RO localizations. Uses the existing disposable-emulator reset fixture,
actual `MainActivity`, real HealthJournal and real Compose tree.
Images are raw `screencap -p` PNGs taken while this Home UI is visible,
not mockups or postprocessed exports.

Actions run [40384275222](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40384275222)
is the first own-branch source validation at
`3c3911e6267e7e8d126b84885c0e60146300acfd`.
**Status at initial handoff: IN PROGRESS, not PASS.** Replace with final exact
SHA, job/test totals, raw evidence hashes, and known limitations after results.

## Limits and follow-up

An API35 synthetic HealthJournal fixture does **not** prove successful source
acquisition or Samsung's physical-device SDK trust: device sign-in, permissions,
and provider timestamp provenance need Root's separate physical Samsung test.
The dedicated branch workflow is QA-only and cannot be cherry-picked.
Global phone suite, app acceptance, release signing, and migration/main
integration remain Root-owned.
