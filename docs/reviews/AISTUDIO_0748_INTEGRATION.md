# AI Studio 07:48 integration — 2026-10-28

## Exact source

Native-AI-Studio@f1d964223338010db27d883192117d56e0d076b1:
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/f1d964223338010db27d883192117d56e0d076b1/trainpilot-oct-28-2026-7-48-16-am.zip

ZIP 8,896,078 bytes; SHA256 `a5054821008205806835140f1e371c158a2be69f537cdec26feeaef6c3380191`; 480 entries, CRC PASS.
Compared with 06:25: six modified Kotlin files and one added reference audit document.
Reviewed PR270 source baseline: `3624a3197a23fc182f6ce7c258cf3ce72f37c79b`.

## Accepted and integrated

The new AnatomicalMuscleHeatmapCard presentation now runs in the native Progress
screen through NativeStudioMuscleCard: 20dp card, 14dp inset, period pill,
original native anatomy, six actual muscle ratios, legend, and selected muscle's
real set count. All captions use HU/EN/DE/RO resources. Empty/error/loading are
real states; no DemoDataProvider or fake exercise deep-link is imported.

The period pill cycles three calendar months / 30 days / 7 days / today. It uses
the existing aggregate SQL (no full-history graph hydration), native six-group
calculation, local calendar/DST boundaries, tombstones and completed sets.
ProgressViewModel owns selection/read jobs, cancellation, stale-read revision,
restore continuation and date/zone invalidation. Strength/cardio/volume filters
remain independent. Original anatomy testTags and zoom behavior are retained.
Source dimensions adapt to narrow screens and large text without changing fontScale.

Additional existing PR270 regressions addressed before the new common gate:
- Scroll actual top tabs into view before UI tests tap them. The 18:49 shell
  deliberately scrolls at narrow widths; semantic existence is not visibility.
- Measure translated tab text with actual typography/fontScale and reserve
  its full width in both real Studio shell and fallback tabs.
- Restore visible steps/workout unit testTags after the HomeVitalCard port.
- Profile persistence test follows the current real Settings entry; all original
  persistence/cancel assertions remain. Original single-CTA planner layout stays.
- Anatomy screenshot test scrolls to the lazy card before awaiting its bitmap.

## Reviewed but rejected export behaviors

The new ViewModel does call TrainPilotWorkoutFinalization, but can create a new
session from Studio-local values when an existing active session isn't phone-owned.
It still derives a new UUID, guessed duration and a single exercise from local
state. This is not a safe replacement for the native editor/finalization path.

Wear handoff waits 600 ms then claims success without an ownership ack. Phone
resume wraps actual protocol calls in runCatching then reports success regardless.
These are not imported. Existing canonical Wear and finalization paths remain.

Exercise/program reads still fall back to DemoDataProvider on empty/error, and
program days may substitute arbitrary six exercises. The muscle export also uses
nonexistent model member names (`key`/`ratio` vs native `muscleKey`/`loadPercentage`).
Native adapter uses the actual compiled contracts instead.

Backup screen adds file launchers with optional callbacks, but its unchanged
MainActivity does not pass export/import callbacks. No unverified backup success
messages or altered signing/applicationId/build configuration are imported.

## Validation

Local source safety tripwire and git diff check PASS. No local Android builds.
New Room regression checks real sets over all four periods across Budapest DST,
excluding tombstones, incomplete sets, outside-window and future workouts.
New real-Activity Compose test changes muscle period, checks stored counts,
independent volume selection and unchanged Room rows; requests unmodified PNGs.
New 320dp/fontScale2 German test covers the actual Studio shell as well as the
retained fallback tab test. Runtime results pending the exact-source common gate.

Previous common gate 40634885209 built/JVM, all three Wear and both signed upgrades
successfully, but phone artifact 12793976161 reports 117 main tests, 19 failures.
This is not a green baseline for current changes. Current gate must independently
verify raw instrumentation, JVM XML and actual screenshots before claiming PASS.
