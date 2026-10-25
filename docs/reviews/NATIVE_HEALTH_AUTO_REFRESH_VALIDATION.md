# Native Health foreground auto-refresh – branch validation

Updated: 2026-10-25. Development branch `feat/native-health-auto-refresh`, pinned integration base `0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20`. Draft PR #244 targets `feat/native-android-migration`. Do **not** merge without native test/device acceptance.

## Actual native call path

`HealthDashboardScreen` lifecycle → `HealthDashboardViewModel` → persisted `HealthJournalStore.preferences().autoOnOpen` → 10 min / in-flight `NativeHealthAutoRefreshGate` → fail-closed workout/ownership, backup and restore admission → `NativeHealthRefresh.readDay` → existing `HealthConnectRepository` / `SamsungHealthRepository` SDK calls → short protected journal publication → re-read selected day without moving its history filter.

Manual refresh bypasses the auto interval; no automatic permission prompt. Active/paused workout, finalization, ownership transfer, backup staging or dataset replacement disallow auto. Existing archive operations are marked by `NativeHealthBackupGate` for the full SAF read/write/verification window. The durable restore epoch continues to belong to `NativeDataReplacement`. The SDK IPC never holds the restore mutex.

## Data safety notes

* Source day interval is timezone-aware and clipped to `Instant.now()` for today; future days are rejected. DST 23/25 hour days retain the correct local-midnight bounds.
* A partial Health Connect grant cannot be treated as a complete daily measurement; the first partial day now saves real fields as `partial` and preserves unavailable fields. Later partial reads preserve older values where missing; no fabricated zero.
* Samsung Health permission changes are checked again before a protected journal write. Failure/denial keeps existing daily fields via `failedFields` merge.
* A live foreground cancellation, changed provider, changed permissions, changed dataset generation, changed timezone or date invalidates the publication ticket. Restore may not resurrect stale SDK values.
* No new provider client, background worker, database schema, backup format, or version bump was introduced.

## Verification matrix

* `NativeHealthAutoRefreshTest`: 8 pure gate cases (opt-in, throttle, manual, no/partial grant, concurrency, stale identity).
* `NativeHealthAutoRefreshAdmissionTest`: 5 session/ownership/restore policy cases.
* `NativeHealthAutoRefreshPublicationTest`: 5 publication/lifecycle/prefs cases.
* `NativeHealthAutoRefreshBackupTest`: 2 backup/first partial-day cases.
* `NativeHealthRefreshWindowTest`: 4 DST/future-window cases.
* `NativeHealthAutoRefreshUiTest`: 1 real Compose navigation-and-persistence scenario.

New cases total: **24 JVM test methods + 1 UI test method**, pending actual Gradle/emulator execution. Script/static checks do not constitute JVM or provider SDK validation. A physical Health Connect/Samsung permission, revoke, foreground navigation and data comparison is still required for complete device acceptance.

## Main parity boundary

This feature provides **foreground/on-open latest-day real provider refresh**, not a reimplementation of the old WebView `rf250Sync` incremental change-log/30-day full backfill. The latter remains a separate parity task, not claimed by this branch. Older WebView entry points, canonical workout refresh and Wear ownership were not rewritten.

## CI diagnosis and integration additions

The initial existing PR validation at `70c4f584` ran (GitHub Actions `40233270552`, `40233270556`).
* Native APK workflow failed in pre-existing `WearSyncRepository.kt:106` during `:app:kaptGenerateStubsDebugKotlin`, Kotlin FIR internal analysis exception; it did **not** get as far as JVM tests. A narrow explicit `mutate<Unit>` type annotation is added on this branch to validate the workaround.
* Quick validation stopped at `actions/setup-node` because the native migration branch lacks `package.json` and npm lockfile; the native migration does not own the main web/npm tree. No unrelated web toolchain was copied into this feature branch.

The separate `NativeHealthBackupGate` now covers JSON/ZIP SAF import/export **and** native Drive backup/sync staging. `HealthJournalStore.healthDay` persists a first partial HC daily row without inventing absent metric values. `NativeHealthRefresh.dayWindow` clips today's future end and is tested across Budapest DST. A transient permission-dialog pause does not cancel explicit Samsung permission consent; STOP/dispose still invalidates stale publication.
