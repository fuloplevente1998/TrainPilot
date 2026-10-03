# TrainPilot 1.0.7 / 2692 — program editing and cardio candidate

The user expanded PR #94's existing backup/GPS cleanup scope before merge. The base remains the phone-approved, post-merge validated 1.0.6 main `ef09c5ee44b61902a7bb265f8ef2b3d2ea8de88e`. The earlier 2691 APK is superseded by this upgrade-safe 2692 candidate. Public main remains 1.0.6; automated tests are not physical-phone acceptance.

## Resulting behavior

- Home fits in the viewport with or without a saved workout draft. When a draft exists, Resume is the primary Home action; the Workout page can still start another workout. Localized lifetime counters remain available on Home.
- Collapsed Health fits in the viewport; opening pulse, weight or another section restores normal scrolling. Compact Today tiles keep labels and values visible.
- Calendar's frame fits below the navigation and shows all days, including six-row months. Long planner/day content scrolls inside its own region, preserving access to every control. Calendar itself does not page-scroll.
- The same exercise-row editor works on Workout and Programs, including built-in programs. It hydrates only when opened. Typing marks the row unsaved; Save validates the complete prescription, writes it once, updates the summary immediately and shows Saved. Reset changes restores stored values. Invalid fields cannot partially save. Replacement/removal target the row's own program rather than the active program. Saved set count and rest settings feed the next workout; an in-progress workout retains its original prescription snapshot.
- Cardio statistics open from Progress beside the existing full-statistics button. Weekly totals use local Monday-to-Sunday weeks, with the previous week and eight-week history. Average pace uses total seconds / total kilometres for sets that have both values. Time-only and distance-only entries do not distort pace. Unfinished, invalid or future entries are excluded.
- Records compare the same exercise ID and distance rounded to metres. The fastest time wins, with the second-best time and difference shown. Cycling and running are separate; a short run is not extrapolated into a 5 km record. The existing exercise statistics recognize distance activities rather than ranking them as longer timed holds.
- Indoor timing requires no GPS or location permission. Start/Pause/Resume/Reset operate on the current unfinished distance set by stable set ID. Manual distance remains independent. Paused time corrections become the new duration. A persisted timestamp preserves elapsed time through background/reload; paused periods do not count. Set completion, deletion, exercise changes and finish stop the owner timer. GPS and indoor timing cannot run together. Only duration/distance go into history; internal timer state is removed.
- HU/EN/DE/RO copies and existing theme tokens are used. The web cache is bumped and the new stylesheet is precached for offline upgrades.

## Validation

- `npm test`: 117 passing regressions, including local week boundaries, weighted pace, comparable records, missing/invalid data, stable owner IDs, pause/resume/draft restoration and manual time corrections.
- `tests/browser/training-cardio-107.cjs`: actual app DOM at 320×740, 360×800, 393×873 and 412×915, four languages and two themes; normal/draft Home, collapsed/expanded Health, six-row Calendar, visible metric values, active/non-active editor Save/reset/validation, next-workout prescriptions, reload persistence, indoor manual km/pause/resume/delete/finish, real draft reload and cardio/full-statistics integration. Screenshots are archived with existing UI evidence.
- `npm run test:ui`: the full 52-script Chromium suite protects prior Journal/Drive/Health/Coach/GPS behavior.
- Performance uses the permanent unchanged budgets and the same 240-workout dataset. Local before/after logs are retained; CI also compares validated main and the final candidate on the same runner. Lazy Programs hydration and the bounded per-render history snapshot remain.
- The signed Android gate runs Node/Chromium, Android unit tests, package/version/source validation, checksums and signing-certificate continuity. Branch builds upload artifacts without publishing a public release.

## Physical-phone checklist

1. Install 2692 over the current app or 2691 candidate. Check historical workouts, custom/built-in programs, settings, weights, photos and sync data.
2. Check Home with/without a saved draft, collapsed Health and Calendar on the actual device, including a six-row month. Open Health details, planner controls, day details and close/Back; no content or save control should become inaccessible. Check theme/language changes and keyboard dismissal.
3. On Workout, edit sets/reps/load/rest and tap Save. Confirm immediate summary update, then start the next workout and check the new prescription. On Programs, edit a different program without activating it; reopen/relaunch and verify persistence. Check Reset changes, invalid values and exercise replacement/removal.
4. Start indoor timing, background the app, return, pause, correct the seconds manually and resume. Enter kilometres manually. Complete/finish the workout and check duration/distance in Journal. Repeat after a draft reload; reset or remove the timed set and confirm no other set inherits its time. Check switching to/from GPS.
5. Open Progress → Cardio statistics. Check weekly km, activity filtering, pace, time-only sessions and two runs on the same distance. Confirm the shorter time is the record and the second-best comparison is correct; cycling and different distances stay separate.
6. Repeat the [backup/GPS checklist](BACKUP_GPS_CONSOLIDATION_1_0_7.md), normal Journal editing and Drive sync. Compare responsiveness with the current phone baseline.

Explicit user acceptance is required before main merge. No phone result is claimed by this document.
