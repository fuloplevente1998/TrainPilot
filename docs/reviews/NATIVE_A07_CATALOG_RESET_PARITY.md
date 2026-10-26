# NATIVE A07 — catalog / legacy-plan reset parity audit

## Scope and refs

- Repository: `fuloplevente1998/TrainPilot`
- Own branch: `fix/native-a07-catalog-reset-parity`
- Handoff: `docs/plans/NATIVE_A07_CATALOG_RESET_PARITY_CHAT_HANDOFF.md` as read at `5703883c`
- Pinned implementation baseline required by the handoff: `3297e82bd7f841dc7a64b60ea26981898f5272ea`
- Migration branch HEAD observed before starting: `5703883cc8aebfbf6bf2c6092f3b9e90600502b2`
- Exact main reference: `7473b30f387eda54186c1e31830304ca12bd104f`
- Main and `feat/native-android-migration` were treated read-only.
- No Home / Progress / Workout / Navigation / Settings / DAO / schema / backup / migrator / Wear production file was modified.

## Decision

**Do not add a global "reset everything/defaults" CTA to the native Programs screen.**

The audit finds two different main operations:

1. A live Settings-level global defaults reset that rewrites the exercise catalog and the `home-basic` program.
2. A live legacy per-day CTA whose final storage write still targets the obsolete `plan` key and therefore does not persist into the modern `programs` model.

The native A/B factory-copy day restore is therefore **not evidence of full global-reset parity**, but it is also **not required to reproduce the broken legacy day write**. The missing global operation is a **missing explicit public contract / Root-owned parity gap**, not a safe Programs-screen patch inside this handoff.

No production implementation is added because the only proven missing reachable operation belongs to Settings and its exact main semantics can delete user custom exercise rows without a preview, while leaving custom programs/favorites in place. Reproducing that blindly would create a data-safety regression.

## Main exact live call chains

### A. Settings "Restore defaults" — live and destructive

Final visible Settings CTA remains reachable:

- `www/app.js:11174-11178` builds the final Settings profile card with `onclick="resetPlan()"`.
- `www/app.js:19966-19970` wraps that Settings screen only to inject the training-mode panel; it keeps the profile/reset HTML.

Final handler chain:

1. `www/app.js:9394` — localized `resetPlan` opens the final danger confirmation.
2. `www/app.js:9304-9313` — `TP149_DIALOG_BASES.resetPlan` captured the previous reset wrapper.
3. `www/app.js:6235-6237` — the previous wrapper opens a confirmation and invokes its captured base through `tp2629ConfirmBypass`.
4. `www/app.js:6169` — bypass only suppresses the older synchronous `window.confirm`.
5. `www/app.js:1022` — the actual write:
   - `db.set('exercises', mergeExerciseLibrary(DEFAULT_EXERCISES))`
   - replaces only the existing `home-basic` element in `programs`
   - `db.set('programs', ps)`
   - render + saved feedback.

Relevant storage semantics:

- `www/app.js:899` — `mergeExerciseLibrary(saved)` starts from built-in default + extra exercises and overlays only `saved`.
- Passing `DEFAULT_EXERCISES` at line 1022 does **not** merge the currently stored custom rows; current custom exercises are omitted from the new catalog.
- `www/app.js:893-898` — programs / active program / planner settings are separate storage.
- The reset does not write history, scheduled items, activeProgramId, plannerSettings, general settings or favorites.
- It preserves custom program objects because only `home-basic` is replaced, but any preserved custom program that referenced a removed custom exercise can become dangling.
- Favorites are not part of this reset, so a favorite that referenced a removed custom exercise can also become dangling.
- The two writes are not one CAS/transactional commit and there is no preview snapshot or continuation token.

**Classification:** reachable main behavior, but native contract is missing and the main behavior is unsafe to clone without an explicit product/data rule.

### B. `rf153ResetDay` — live CTA, legacy storage write

The CTA is still live:

- `www/app.js:1937-1950` — `editPlan` renders `rf153ResetDay(w)`.
- Modern compatibility `plan()` at `www/app.js:975-977` reads the currently active program from `programs`.

Final handler chain:

1. `www/app.js:9419-9422` — final localized confirmation calls `TP149_DIALOG_BASES.resetDay`.
2. `www/app.js:9304-9313` — that base captured the earlier wrapper.
3. `www/app.js:6271-6273` — earlier wrapper calls the original `rf153ResetDay`.
4. `www/app.js:1926-1928` — original handler replaces `p[w]` then writes **`db.set('plan', p)`**.
5. `www/app.js:968` — the `db.set` wrapper does not translate `plan` writes into `programs`.
6. Modern program edits instead use `rf155SaveDay` and write `programs` at `www/app.js:2016-2029`.

Therefore the live day-reset CTA re-renders from the modern active program while its write went to the obsolete legacy key. It is not a valid canonical persistence contract for native parity.

**Classification:** main legacy bug / obsolete side effect. Native must not imitate this storage behavior.

## Native audited behavior at `3297e82b`

### Targeted day restore

`android/app/src/main/java/com/repforge/app/NativeProgramActions.kt`:

- lines 29-38: obtains a data-replacement continuation, requires A/B, reloads the target and allows only a non-builtin program whose `sourceTemplateId == "home-basic"`; factory must be the builtin `home-basic`.
- lines 39-64: computes exact before/after entries, editable recipe IDs and warnings for other days sharing those recipe IDs; preview is read-only.
- lines 68-78: confirm runs through `NativeDataReplacement.mutate`, rechecks the continuation and delegates a narrowly scoped restore to `NativeProgramEditor.save`.

`android/app/src/main/java/com/repforge/app/NativeProgramEditor.kt` (read-only for this chat):

- lines 124-136: restore parameters are tightly constrained and active-workout admission is rechecked.
- lines 181-205: transaction revalidates both target and factory; factory must still be builtin `home-basic`; exact recipe delta must still match the preview.
- lines 223-269: builtins are never overwritten; selected editable recipe fields are reset while unknown raw extensions are preserved.
- lines 241-250: new exercise references must still exist in the library.
- lines 295-337: day JSON is rebuilt while unknown day/program raw data is retained.
- lines 365-370: active-workout/ownership is rechecked before the Room replacement.

`android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt`:

- lines 205-218: preview has explicit loading/error state.
- lines 220-233: confirm is single-flight, invokes the guarded restore, reloads programs and keeps the preview visible on failure.
- lines 237-240: cancel clears preview/error without writing.

`android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt`:

- lines 260-307: explicit before/after/recipe preview dialog, cancel, confirm and error state.
- lines 443-451: restore CTA appears only for non-builtin `home-basic` descendants and only A/B days.

### Built-in catalog

`android/app/src/test/java/com/repforge/app/NativeBuiltinCatalogTest.kt` already asserts the current canonical bundle is **106 distinct exercises / 13 programs**, all four localized names/notes present, and every program day references a known exercise.

`TrainPilotDatabase.ensureBuiltinCatalogSeeded` is an expansion/repair primitive, not a reset primitive: existing customized rows and existing program rows survive seeding. It cannot be substituted for main `resetPlan`.

## State matrix

| State | Native result / audit decision |
|---|---|
| 13 programs / 106 exercises | Canonical native bundle confirmed by source test; new A07 test rechecks it before restore. |
| Factory `home-basic` | No destructive restore CTA. Editing a builtin creates a custom descendant first. Acceptable native UX. |
| Unmodified A/B descendant | Preview allowed; confirm is semantically safe. Old preview cannot be replayed after commit. |
| Modified A/B descendant | Only chosen day + affected editable prescription fields restore from factory. |
| Other day | Preserved byte/row-equivalent; shared recipe impact is previewed. |
| Unrelated custom program | Cannot claim factory reset; preview rejected. |
| Legacy/imported custom exercise | Targeted day restore must not rewrite/delete it; explicit new JVM + API35 cases added. |
| Favorite | Outside day-restore write set; explicit new JVM case added. |
| Cancel | Read-only; existing API35 case + screenshot. |
| Stale target/factory preview | Rejected; existing JVM/API35 cases. |
| Double submit | First commit invalidates old opened snapshot; existing JVM case rejects replay. |
| Data replacement during restore | Continuation / replacement lease rejects it; existing JVM case. |
| Failure/rollback | Room transaction prevents partial target-program publication; explicit new failure case added. |
| Active program selection | Day restore does not write Settings; explicit new JVM assertion. |
| Active workout / transfer | Commit admission rejects mutation. |
| History / scheduled entries | No writer in day-restore call chain; remain outside scope. |
| Locale | Preview uses current UI language; restored day names are taken from factory HU/EN/DE/RO rows, while user program metadata remains intact. |
| Main global default reset | **Missing explicit native public contract. Root-owned. Do not add to Programs from this chat.** |

## Root-owned patch requirement for the missing global contract

If product parity requires the main Settings action, Root should implement it in the Settings/data layer, not by widening `NativeProgramActions`:

1. **UI ownership:** `android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt:345-378` — add a separate defaults-reset CTA; do not overload "erase all local data".
2. **State/admission:** `android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt:297-335` — use a distinct preview/confirm state with the same replacement/active-workout admission principles.
3. **Core/DAO ownership:** create a Root-owned transactional reset helper. Current DAO/schema/core are read-only in this chat.
4. Before implementation, decide the explicit data rule that main never states:
   - whether custom exercise rows are deliberately deleted or preserved;
   - what happens to custom programs and favorites that reference those rows;
   - whether imported builtin rows with user modifications reset or remain;
   - whether activeProgramId stays unchanged when it is not `home-basic`.
5. Required native implementation quality if enabled:
   - preview exact affected program/exercise/custom/favorite references;
   - continuation/CAS against data replacement and stale preview;
   - one Room transaction, no partial catalog/program reset;
   - active workout/transfer recheck at commit;
   - explicit idempotence/replay behavior;
   - history/schedules/settings preserved unless the public contract explicitly says otherwise.

Until that contract exists, reproducing main line 1022 would risk deleting user-created catalog data and leaving dangling references.

## Changes on this branch

Production app code: **none**.

Test/evidence changes:

- new `android/app/src/test/java/com/repforge/app/NativeA07CatalogResetParityTest.kt`
  - full 13/106 catalog + custom/favorite + active-program preservation;
  - actual `LegacyWebViewMigrator` custom-catalog import survives day restore;
  - missing catalog dependency produces an atomic failed restore with no partial target write.
- extended `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeProgramActionsUiTest.kt`
  - retains all original cases;
  - captures raw preview / confirmed / stale-error screenshots;
  - adds migrated custom-catalog confirm case.
- branch-only `.github/workflows/native-a07-reset-qa.yml`
  - **QA-only, do not integrate into migration/main**;
  - runs only the four required JVM classes and only `NativeProgramActionsUiTest` on API 35;
  - uploads raw Gradle log, JUnit XML, APKs, screenshots and SHA256 manifest.

## Required gate status

Target JVM classes:

- `NativeProgramActionsTest`
- `NativeProgramEditorTest`
- `NativeBuiltinCatalogTest`
- `NativeA07CatalogResetParityTest`

API35:

- all cases in `NativeProgramActionsUiTest`, same app/test APK pair.

At the time this report file was first created: **NOT_RUN**. Run/job/artifact/XML/hash evidence is appended only after GitHub Actions completes; no pending run is counted as PASS.

Full restore/migration, signed-upgrade and combined phone UI acceptance remain Root-owned and are **not** implied by this scoped gate.
