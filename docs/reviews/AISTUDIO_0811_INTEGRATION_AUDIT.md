# AI Studio 08:11 — source audit, 2026-10-28

## Forrás és hatókör

Pinned repo: `fuloplevente1998/Native-AI-Studio@bc8e49fa7949b160aef72e38992334f72435fc8e`.
[Full ZIP](https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/bc8e49fa7949b160aef72e38992334f72435fc8e/trainpilot-oct-28-2026-8-11-58-am.zip).
8,901,767 bytes, 481 entries, CRC PASS; SHA256
`7af0b768ced2b6104651a704c135aa07a6813c5b07632a5026dea8fcf989890c`.
Compared with 07:48: four changed Kotlin files, one new `app/AISTUDIO_REFINEMENT_REPORT.md`.
No native engine, DAO, schema, build config or native launcher changes between these exports.
Static review only. Export's build/unit success statements have no independent raw log/APK attached.
Current PR270 runtime remains frozen at `1a6b7102`, run40658293784; this review changes documentation only.

## Ellenőrzött elemek

- `com/example/MainActivity.kt:465–475` now supplies actual export/import callbacks
  to BackupRestoreScreen. Previously these optional callbacks were not supplied.
  The existing VM calls canonical BackupRestoreManager and waits for Result.
  This is a real correction in the Studio project; native PR270 already has these
  document contracts and guarded operations, so no duplicate adapter is imported.
- Exercise mapping now supplies HU/EN/DE/RO names and real stored default sets,
  weight and rest fields (all exist on ExerciseLibraryEntity), replacing several
  invalid constructor arguments from 07:48. Program/day constructors and empty
  editor fallback use the actual Studio model names.
- Muscle mapping now correctly reads `muscleKey` and `loadPercentage`, addressing
  the invalid `key`/`ratio` fields. PR270's NativeStudioMuscleCard already uses the
  canonical model directly and requires no extra mapping layer.

## Kockázatok / hiányzó bizonyíték

### 1. P1 — camera/gallery selection is still falsely reported as private storage

ZIP `com/example/ui/screens/HealthCoachAndSettingsScreens.kt:2608–2623`:
TakePicturePreview launches a real camera but only stores bitmap dimensions in a
remembered status string; the bitmap is never written to private storage. PickVisualMedia
only stores the URI's lastPathSegment. Neither callback calls WorkoutPhotoRepository,
copies the bytes, records ownership metadata, or verifies durable storage. Both report
success as if the private photo were saved. A camera/selection result is not a save receipt.
Keep native photo repository/session guards and acknowledge only durable private writes.

### 2. P0/P1 — previously rejected session/ownership semantics remain unchanged

ZIP Studio ViewModel still installs a candidate session from local single-exercise
state when it cannot use a phone-owned active session, and still generates a new
workout identity/guessed duration. Calling the canonical finalizer afterwards does
not make that manufactured input equivalent to the canonical active workout.
The Wear path still publishes a home snapshot, delays600ms, then claims successful
handoff without acknowledgement. Phone resume still swallows protocol errors before
claiming success. Do not import these paths into the native app.

### 3. P1 — model-name fixes do not yet ensure data fidelity

ZIP Studio ViewModel `:243–316`: unknown/cardio exercise categories now fall back
to CHEST; program active state is `pIdx == 0`, not the stored activeProgramId.
Empty/error reads still fall back to DemoDataProvider, and program days can still
substitute arbitrary first-six exercises. Rest below30s is coerced to30; isTimed /
isBilateral measurement fields are not mapped. Real data can therefore appear in
the wrong workout mode/category or with a changed prescription.

Existing packed Color.value theme conversion, fixed work=30/rounds=3 writes,
whole-history hydration, and unguarded body-weight DAO writes also remain.
The sandbox applicationId is unchanged. The report's claim that this ensures
PR270 merge compatibility does not establish signed-update compatibility.

## Következő mikro-lépés

Keep the new backup callback wiring as a useful Studio-project correction, retain
native PR270's already-working backup/photo/program/ownership data paths, and continue
the original Studio presentation port on those canonical operations. No new app-source
change is justified by this small delta while owner-profile validation runs. Overall
full-native integration and complete visual parity remain WIP; main stays unchanged.
