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

## First exact-source runtime and correction

40649976268 / 52e31054: raw JVM artifact 12799173855, 168 XML files,
1344 tests / 0 failure / 0 error / 0 skip. Wear raw 12799747282/12800381325/
12799613734: 68+59+56=183 PASS, no missing/crash. Both signed upgrade jobs SUCCESS.
Phone raw 12799797327: 119 main, 113 PASS / 6 FAIL +4 isolated GPS PASS,
117/123 overall, zero missing/runner crash. This is NOT a green common gate.

Corrections from actual failures: give scrollable Text itself a measured minimum
width (not just its parent); read child unit/program tags from the unmerged tree
inside genuine clickable Studio cards; scroll the lazy month selector into view;
assert the full localized set count (Compose assertTextContains defaults to exact
string matching). The actual PNG also showed the muscle figure stacked on normal
320dp: preserve side-by-side layout at normal fontScale, stack only at very narrow
width or large fontScale, and use the Studio muscle-card title in all four locales.
A new exact-source common gate is required for these corrections.

## User phone preview profile

User supplied Xiaomi Redmi Note 15, 1080x2392 portrait pixels (2392:1080),
90% system font size and otherwise default display sizing. Add optional display
size/density/font-scale inputs to the existing canonical workflow, not a second
build pipeline. The runner configures the disposable emulator before app launch,
reads back wm size/density/font_scale, records them in the manifest, and verifies
every raw PNG IHDR matches requested pixel dimensions. No image resizing/cropping.

Requested preview: 1080x2392, fontScale=0.90. Density=440dpi is an explicitly
provisional phone-sized profile (~393dp wide), not an observed Redmi system value.
Physical panel PPI must not be substituted for Android logical display density.
If an actual device DPI/smallest-width reading becomes available, change the
workflow parameter and regenerate; do not claim pixel/layout equivalence yet.

Local host regression suite: 11/11 PASS, including pixel/density pairing,
read-back mismatch rejection, and raw PNG geometry checks. Device-profile runtime
still pending. Native app/test source remains identical to c894f653.

## Corrective gate and confirmed owner dimensions

40653023009 / c894f653: build, all Wear and both signed upgrades SUCCESS;
phone raw12801406371:119main114PASS/5FAIL+4GPSPASS,118/123 overall,
0missing/crash. Full-unit artifact12800278854 downloaded for raw verification.
Remaining exact failures: no-wrap Visible text paragraph overflow in both tab
variants; steps unit tag on a parent in unmerged semantics; day label inside a
clickable card; test expecting system-English set units while app is Hungarian.
Corrections use bounded soft-wrapped single-line text, actual Text unit tags,
unmerged day lookup, and explicit app-language resources without weakening assertions.

User then supplied developer-settings screenshot showing **smallest width384dp**.
At1080px this implies **450 logical DPI** (1080×160/384), replacing the earlier
provisional440. Confirmed profile:1080x2392,450dpi,fontScale0.90. The next common
full gate must use these inputs and verify unchanged raw PNG pixel dimensions.

## Owner-profile preflight correction

40655883561 /12c99fd4: native build/JVM, all Wear and both signed upgrade jobs
SUCCESS. Phone job122710908905 failed BEFORE instrumentation because the old
320x640 base AVD did not apply the requested1080x2392 geometry; dimension readback
correctly rejected it. No phone runtime PASS or PNG is claimed for this run.
Next profile run uses a Pixel7 hardware base before applying1080x2392/450/0.90.
Preflight failures now preserve a failed manifest and actual readback diagnostic.
Local host suite12PASS. No APK/main-source behavior changed in this correction.
Two remaining Home child-text lookup groups in the later localization/history
assertions are also updated to the actual unmerged Text nodes; assertions stay.
