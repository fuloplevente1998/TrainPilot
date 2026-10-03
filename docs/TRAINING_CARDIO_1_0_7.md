# TrainPilot 1.0.7 / 2695 — program editing and cardio

The user expanded PR #94's existing backup/GPS cleanup scope before merge. The base remains the phone-approved, post-merge validated 1.0.6 main `ef09c5ee44b61902a7bb265f8ef2b3d2ea8de88e`. The earlier 2691/2692/2693/2694 APKs are superseded by this upgrade-safe 2695. The user accepted signed 2695 on 2026-10-03 and explicitly authorized main merge, documentation updates and the APK/source release. Automated tests and this actual phone approval are distinct evidence.

## Resulting behavior

- Home fits in the viewport with or without a saved workout draft. When a draft exists, Resume is the primary Home action; the Workout page can still start another workout. Localized lifetime counters remain available on Home.
- Collapsed Health fits in the viewport; opening pulse, weight or another section restores normal scrolling. Compact Today tiles keep labels and values visible.
- Calendar's frame fits below the navigation and shows all days, including six-row months. The full two-column planning settings remain fixed without internal scrolling; the themed selectors and date/time pickers open above the fixed page. Long selected-day details retain their own scrolling region. Calendar itself does not page-scroll.
- The same exercise-row editor works on Workout and Programs, including built-in programs. It hydrates only when opened. Typing marks the row unsaved; Save validates the complete prescription, writes it once, updates the summary immediately and shows Saved. Reset changes restores stored values. Invalid fields cannot partially save. Replacement/removal target the row's own program rather than the active program. Saved set count and rest settings feed the next workout; an in-progress workout retains its original prescription snapshot.
- Journal has Workout log, Progress and Statistics tabs. Statistics has Personal records and Cardio statistics sections, rendered only when selected; redundant standalone Progress buttons are removed. Weekly totals use local Monday-to-Sunday weeks, with the previous week and eight-week history. Average pace uses total seconds / total kilometres for sets that have both values. Time-only and distance-only entries do not distort pace. Unfinished, invalid or future entries are excluded.
- Records compare the same exercise ID and distance rounded to metres. The fastest time wins, with the second-best time and difference shown. Cycling and running are separate; a short run is not extrapolated into a 5 km record. The existing exercise statistics recognize distance activities rather than ranking them as longer timed holds.
- Indoor timing requires no GPS or location permission. Start/Pause/Resume/Reset operate on the current unfinished distance set by stable set ID. Manual distance remains independent. Paused time corrections become the new duration. A persisted timestamp preserves elapsed time through background/reload; paused periods do not count. Set completion, deletion, exercise changes and finish stop the owner timer. GPS and indoor timing cannot run together. Only duration/distance go into history; internal timer state is removed.
- HU/EN/DE/RO copies and existing theme tokens are used. The web cache is bumped and the new stylesheet is precached for offline upgrades.

## Validation

- `npm test`: 117 passing regressions, including local week boundaries, weighted pace, comparable records, missing/invalid data, stable owner IDs, pause/resume/draft restoration and manual time corrections.
- `tests/browser/training-cardio-107.cjs`: actual app DOM at 320×740, 360×800, 393×873 and 412×915, four languages and two themes; normal/draft Home, collapsed/expanded Health, six-row Calendar, visible metric values, active/non-active editor Save/reset/validation, next-workout prescriptions, reload persistence, indoor manual km/pause/resume/delete/finish, real draft reload and cardio/full-statistics integration. Screenshots are archived with existing UI evidence.
- `npm run test:ui`: the full 54-script Chromium suite protects prior Journal/Drive/Health/Coach/GPS behavior.
- Performance uses the permanent unchanged budgets and the same 240-workout dataset. Local before/after logs are retained; CI also compares validated main and the final candidate on the same runner. Lazy Programs hydration and the bounded per-render history snapshot remain.
- The signed Android gate runs Node/Chromium, Android unit tests, package/version/source validation, checksums and signing-certificate continuity. Branch builds upload artifacts without publishing a public release.

## Physical-phone checklist

1. Install 2695 over the current app or 2691/2692/2693/2694 candidates. Check historical workouts, custom/built-in programs, settings, weights, photos and sync data.
2. Check Home with/without a saved draft, collapsed Health and Calendar on the actual device, including a six-row month. Open Health details, planner controls, day details and close/Back; no content or save control should become inaccessible. Check theme/language changes and keyboard dismissal.
3. On Workout, edit sets/reps/load/rest and tap Save. Confirm immediate summary update, then start the next workout and check the new prescription. On Programs, edit a different program without activating it; reopen/relaunch and verify persistence. Check Reset changes, invalid values and exercise replacement/removal.
4. Start indoor timing, background the app, return, pause, correct the seconds manually and resume. Enter kilometres manually. Complete/finish the workout and check duration/distance in Journal. Repeat after a draft reload; reset or remove the timed set and confirm no other set inherits its time. Check switching to/from GPS.
5. Open Journal → Statistics → Cardio statistics. Check weekly km, activity filtering, pace, time-only sessions and two runs on the same distance. Confirm the shorter time is the record and the second-best comparison is correct; cycling and different distances stay separate.
6. Repeat the [backup/GPS checklist](BACKUP_GPS_CONSOLIDATION_1_0_7.md), normal Journal editing and Drive sync. Compare responsiveness with the current phone baseline.

The user accepted the signed 2695 APK and authorized main merge/publication on 2026-10-03. The checklist above remains useful for subsequent regression checks.

## Planner correction after 2692 phone feedback

The Home personal planner keeps the accepted normal heading, copy and full-width button below the copy when no unfinished workout exists. Only a saved workout draft selects its compact two-column layout. Removing the draft restores the full card, including after navigation, refresh or a theme change. Small-screen summary spacing is adjusted to preserve the fixed Home viewport without compressing the normal planner. Browser regressions cover both profile states, four languages and all four phone sizes.

The user also requested a fixed Calendar planning section. Rhythm, first workout day, start/time, duration/weeks and the planning button share a two-column layout. All planning modes, including weekly days and manual planning, must fit without internal scrolling. Existing themed choice/date/time pickers remain usable above the fixed calendar. The regression matrix uses a six-row month and real dropdown selection, wheel input and first-workout/time pickers in all four languages and phone sizes.

## Layout correction after 2693 phone feedback

The shared Calendar/Coach/Settings host now toggles Calendar sizing both on and off, and Calendar CSS also requires the matching active panel type. The Calendar top border sits 2 px below the navigation; the outer frame fits the month and planner content instead of forcing a viewport-high empty tail. Health has a permanent summary/column layout class and a separate closed-only viewport cap: opening pulse, weight, further data or Health Connect preserves card widths and enables normal page scrolling.

The new browser regression covers populated five-/six-row months, both matte and vivid themes, 320/360/393/412 px plus the phone-like 393×823 viewport, all four Health disclosure families, actual wheel scrolling, Calendar → Coach → Settings → Calendar transitions and visible top/bottom frame geometry. The full UI suite now contains 54 scripts. Those earlier reports preceded the final 2695 phone acceptance on 2026-10-03.

## Phone feedback after 2694: issues #95–98

The compact Home Coach keeps its current copy/size and now uses the exact Coach recommendation border/background tokens in matte and vivid themes. Compact Progress places its period comparison at the upper right and lifts period controls beside the chart. Journal Statistics is a third destination, with Personal records and Cardio statistics tabs; only the selected report is built. Record accordions retain full historic exercise coverage and native Back collapse behavior, while cardio filtering and fastest-distance comparisons remain unchanged. The page uses one section heading and normal scrolling, without nested overlay headers or excessive top whitespace.

Theme selection now uses the same `tp-select` trigger, arrow and menu transition as Language; the two palette columns remain. Shared dropdown closing clears its open state on outside click, Escape, navigation/resize and Android Back. Selection persists without closing Settings.

`tests/browser/phone-ux-95-98.cjs` covers four languages, four phone widths and both matte/vivid themes, real navigation/clicks/scrolling, compact comparison coordinates, matching Coach styles with/without drafts, all exercise records, lazy section switching and matching selector arrows/animation. The final 2695 phone result was accepted on 2026-10-03; all four fixes are included in the approved release.

## Approval and published package

Phone-approved source: `d31d0f9d4140e2fb2505d56c058f3d06d974df0c`. The user approved the signed 2695 APK, main merge, documentation updates and release on 2026-10-03. Only acceptance/release documentation was added afterward; the approved app assets and native code are unchanged. Main CI revalidates the merged source before public publication.

[Release notes](releases/v1.0.7.md) · [GitHub v1.0.7](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.0.7). The release contains the signed APK, exact tagged source ZIP, SHA-256 checksums and Android package metadata. `SOURCE_VERSION.json.sourceCommit` in the ZIP identifies the build's actual source commit.
