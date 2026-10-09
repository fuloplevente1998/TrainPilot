# TrainPilot — Development Status

Last updated: **2026-10-09**.

## Phone test candidate — 1.2.13 /2720, Health first-frame flash (#153)

Based on released main `dfa3f049ed3f5eb507dec6697f045a3186fffa68` (1.2.12). The reported video shows legacy pulse/chevrons briefly returning during Health navigation. A closed-over deferred base decorator rebuilt the legacy view after final synchronous decoration. All Health entry points now resolve the complete pulse/chevron decorator at call time; competing deferred decoration is removed. Existing pulse subtree preservation, seven-day/source semantics and weight/recovery actions remain.

The new behavioral regression audits timer/mutation boundaries and actual animation frames, first/repeated navigation, synchronous/async refresh and weight range across 32 phone/language/theme cases. It fails on the previous source and passes with the fix. 125 Node regressions + four runner tests passed locally; the canonical UI gate now has 83 scripts. Existing Health/pulse/training follow-ups and unchanged performance budgets are checked. [Candidate notes](docs/releases/v1.2.13.md).

The new Health fix is prepared as a signed phone test APK and draft PR, pending physical-phone acceptance under the permanent performance policy. No physical acceptance or main merge for this new fix is claimed. The earlier calendar/feedback scope is already merged as PR #152 and publicly released as 1.2.12.

## Released phone — 1.2.12 /2719, calendar planning and shared action feedback

Based on main after the approved Wear #151 merge. The multiweek Calendar planner replaces all unfinished plans in the selected start-date/week window with the active program, including older programs. Completed/logged/active/draft entries, previous tombstones and dates outside the window remain protected. Retained/moved rows keep their IDs; surplus plans become sync tombstones. Repeated planning is idempotent and a failed settings write rolls calendar storage back. Personal program names are visible, and calendar-started sessions use their actual exercise list and prescriptions.

The separate Calendar → Replan dialog retains its selected program/first day/date/source across redraws, previews the chosen program and replaces the original program's remaining plan. Saves and confirmed deletions across the app use the existing bottom-centre toast; errors/dirty states and confirmation dialogs remain explicit. One host/timer gives screen-reader feedback without extra full-page renders, including a once-only receipt after local-data erase/reload.

125 Node regressions + four runner tests passed locally. The canonical UI suite now contains 82 scripts, with new action-feedback and personalized multiweek replacement regressions covering 32 phone-size/language/theme cases each. Full Chromium, native tests, signed APK and source/signer checks run in the existing phone gate. Performance budgets remain unchanged. [Release notes and detailed scope](docs/releases/v1.2.12.md).

The user explicitly authorized main merge after completing these requested changes (“Utána az ha megvan mehet mainre :)”), and selected replacement of all remaining plans in the chosen weeks, including previous programs. PR #152 merged as `dfa3f049ed3f5eb507dec6697f045a3186fffa68`; both branch and main release gates passed and the public v1.2.12 APK/source assets were independently verified. No unperformed physical-phone check is claimed. Wear 1.2.15 remains separately released.

## Phone-approved stable release — 1.2.6 /2709

The user approved main merge and public APK/source release from PR #114 on 2026-10-04. Accepted application source: `b2260cb00da91b7d80073d0f1e4181018c227fce`; APK SHA-256: `c85a3c9fdf9433c1430ddef37435bc5a67515fc400c3b3f796609871813c0f82`.

- Clean Health Connect journal: a persistent transactional native store, one summary per day, two aggregation modes and collapsed paginated measurements. Preserve imported IDs/origins/deletions, consented manual backups, restore/erase guards and permitted foreground/background refresh.
- Floating Calendar day details with existing actions, fixed month/planner sizing, preserved pending values and nested-picker/confirmation Back priority.
- Replanning includes today's elapsed unfinished session, preserves reviewed program/start date on redraw, protects completed/logged/active sessions and retains identities/order/time/duration/collision rules. Header/apply remain reachable on long previews.
- Consistent close buttons. Coach readiness explanations open on the score tap and show actual fresh/missing inputs and point effects with the existing formula; exercise advice and deferred statistics remain.
- **119 Node regressions + 4 runner tests, 62 Chromium scripts, Android release tests and unchanged performance budgets passed** on the accepted source. [Signed gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37230428304), [Phase 7](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37230432178). Complete ZIP/Git tree, all 25 packaged web assets, checksum/version and original signer were independently verified.
- Release preparation updates current documentation and removes obsolete product descriptions as requested. The application implementation/dependencies/version remain the accepted source; native/web privacy text receives the same description cleanup and stable-version wording. These changes are checked again by the main gate.
- Main merge, release commit/run/tag, four public assets and final verification checkpoints are tracked in [#119](https://github.com/fuloplevente1998/TrainPilot/issues/119). Follow-up [#113](https://github.com/fuloplevente1998/TrainPilot/issues/113) is limited to further HRV-source access.

The earlier development history below is historical. Current usage: [release notes](docs/releases/v1.2.6.md), [Health Connect journal](docs/HEALTH_JOURNAL_ROADMAP.md). The permanent [performance policy](docs/PERFORMANCE_REGRESSION_POLICY.md) remains applicable.

## Candidate — TrainPilot 1.1.0 / 2698 (#103)

- Starts from verified public main `81032d9b3ec697680e7fdb1210e89f94e584b299`. [#103](https://github.com/fuloplevente1998/TrainPilot/issues/103) was recorded before implementation. Stable main/release remains 1.0.9 / 2697 pending physical-phone acceptance.
- Weekly saved-workout and monthly cardio-kilometre goals reuse Home's Today header and the shared Goals panel, with settings/backup/Drive persistence. No extra Home card. Zero disables a goal; local Monday/month boundaries and distinct legacy records are preserved.
- Calendar replanning previews missed and following sessions for the selected program, preserves IDs/day order/duration/local time, and respects alternate/weekly/custom rhythms. Completed/logged/active-draft sessions stay protected; occupied days are avoided. A changed snapshot requires review again before applying.
- Indoor work/rest intervals reuse the existing timer and stable owner set. Presets are backed up; pause/resume and saved drafts survive restart. The final rest is omitted, elapsed time clamps at the planned end, and set completion remains manual. Foreground audio/native vibration cues require the app open; background elapsed recovery does not promise background alarms.
- Coach explains the existing sleep/HRV/load/pain weights, dated observations, missing baseline and stale data in HU/EN/DE/RO. The scoring algorithm is unchanged. Empty Journal/filter/search/cardio states offer a next action; successful saves use existing non-blocking feedback while write failures retain explicit errors.
- Android's system text zoom is honored. Enlarged text enables vertical scrolling for Home, Health and Calendar and wraps controls without horizontal overflow; default overview sizing remains fixed.
- Added one core regression and one end-to-end UI script: full suites contain **118 Node regressions and 57 Chromium UI scripts**. New UI cases cover four widths/four languages/two themes, failed saves, stale replanning, real timer phases/cues, pause/reload recovery and enlarged-text reachability. Performance budgets and the Phase 7 reference remain unchanged.
- VersionName **1.1.0**, versionCode **2698**, unchanged application ID/storage/signer. See [implementation and phone checklist](docs/FEATURES_1_1_0.md) and [candidate notes](docs/releases/v1.1.0.md). Physical-phone approval for this candidate is pending.

## TrainPilot 1.0.9 / 2697 — minimal page design polish (#101)

- Phone screenshots and feedback are recorded in [#101](https://github.com/fuloplevente1998/TrainPilot/issues/101). The requested scope is consistent main-page width, the existing Coach-reference faint 1px frame, stable Journal tab position and shared Progress detail arrows.
- One scoped stylesheet gives root pages the existing Coach/Calendar/Settings outer width, content gutters and themed neutral border. Existing floating panels retain their layout; fixed workout controls follow the same content lane. Journal Log/Progress/Statistics share the same top padding; the root entrance animation fades without translating the frame.
- Progress arrows use the shared theme-aware chevron. Lazily inserted metric detail rows are decorated locally when opened; disclosure arrows rotate right/down. Compact metric/volume controls keep a smaller footprint. Narrow, short Home/Health pages recover a few pixels from inter-card gaps so translated summaries remain fully visible.
- Regression coverage compares all main frames/gutters with the Coach reference, checks all three Journal tab positions immediately and after animation, and checks shared chevrons opening/closing across four phone sizes, four languages and matte/vivid themes. The full UI suite now has 56 scripts. Existing fixed-view, editor/cardio, global styling and #99 scrolling regressions remain required.
- VersionName **1.0.9**, versionCode **2697**, existing app ID/data/signer. The performance baseline and budgets are unchanged. [Release notes](docs/releases/v1.0.9.md). No physical-phone acceptance is claimed for this new polish.

## Previous release — TrainPilot 1.0.8 / 2696, theme dropdown scroll fix (#99)

- The user reported that the published 1.0.7 theme dropdown opens but cannot scroll to the remaining colours. Recorded before implementation as [#99](https://github.com/fuloplevente1998/TrainPilot/issues/99).
- Reproduced in Chromium: the outer menu has overflowing content, but the legacy inner palette list creates a non-overflowing scroll container with `overscroll-behavior: contain`, blocking the gesture from reaching its parent. The inline two-column palette now allows overflow; the existing outer menu owns scrolling. The separate legacy modal keeps its own scrolling behaviour.
- A behavioural regression fails on 2695 and checks real wheel and CDP touch swipes over both columns, selection of each column's last colour, persistence after reload, and Calendar → Settings navigation across four phone sizes, four languages and matte/vivid themes. The full UI suite now contains 55 scripts.
- Local validation: **117 Node regressions passed**; the new #99 regression passed all 32 phone-size/language/theme combinations with actual wheel and touch gestures.
- The public 1.0.7 / 2695 tag and assets are retained. This correction uses versionName **1.0.8** and versionCode **2696**, preserving `com.repforge.app`, stored data and the release signer. No new physical-phone acceptance is claimed for this fix.
- [Release notes](docs/releases/v1.0.8.md). Main publication requires Node/UI, Android/JVM, package/source and signer-continuity gates; the performance budgets remain unchanged.

## Previous phone-approved release — TrainPilot 1.0.7 / 2695

- On **2026-10-03**, after receiving signed build **2695**, the user accepted the phone result: “Ez nagyon jol sikerult, mehet mainre, aktualizalhatjuk a doksikat is. Release apk+zip ,stb :)”. This authorizes PR #94 merge and public GitHub APK/source publication.
- Phone-approved app source: `d31d0f9d4140e2fb2505d56c058f3d06d974df0c`; app SHA-256 `e1e9184b7aaa8431abc3c78fcd26b32033f5ba2451bdb11b6d7fa35d78e389d3`. Acceptance documentation changes do not alter application code, native code, dependencies or version metadata.
- **117 Node regressions, all 54 Chromium UI scripts, unchanged performance budgets, Android/JVM release tests and APK/source/signer continuity passed.** Evidence: [Quick](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37108589260), [Performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37108589267), [signed Android gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37108587187). Main's repeated release gate passed and published [v1.0.7](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.0.7) from `5b83b1c6f1c6b1d90d011c910093b78a99e2135e`. [Main Quick](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37114486342), [Main Performance](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37114486406), [Main signed gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/37114486318) succeeded. All four public assets were independently downloaded and verified against main; the signer matches accepted 2695 and public 2690.
- **PR #94:** backup/GPS consolidation, explicit program Save, fixed overviews, cardio statistics and indoor timing. Phone follow-ups **#95–98** cover Coach styling, compact Progress, Journal Statistics and Theme/Language selector parity.
- Version code **2695**, app ID `com.repforge.app`; signer matches public 2690 and the 2694 test APK. Existing data and all accepted Phase 3–7 behaviors remain protected.
- Release files: signed APK, source ZIP with stamped `SOURCE_VERSION.json`, `SHA256SUMS.txt`, `apk-badging.txt`. [v1.0.7 release](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.0.7), [release notes](docs/releases/v1.0.7.md).

## Previous release — 1.0.6

- **TrainPilot 1.0.6 / versionCode 2690**, merged from user-approved PR #92 to public `main` as `efb72b5d69102818dfbfdd80073b63298dc346e7`.
- **Distance activities:** six built-in distance exercises (running, jogging, inline skating, cycling, walking, hiking), available in Quick Workout and own programs.
- **Measurement model:** repetitions, time, and time+distance. Kilometres remain separate from strength repetitions and lifted volume.
- **Custom builders:** own exercise/program create/edit flows use the framed shared overlay with a header-contained red X and themed controls.
- **GPS:** user-started Android location foreground service; aggregate time/distance only, no saved route coordinates. Manual distance entry remains available without GPS/location permission.
- **Indoor recording:** distance activities may be completed with duration only when GPS distance is unknown.
- **Workout steps:** Health Connect `READ_STEPS` is aggregated over exact active workout windows, excluding pauses and merging overlaps. A missing step permission is explicitly detected and can be requested from the workout card. Imported steps are not written back and are not converted into GPS distance.
- **Approval:** the signed 2690 candidate passed Node, Chromium UI, Android/JVM, performance, APK/source and signer-continuity checks. After receiving the test APK, the user explicitly authorized merge to `main`.
- **Post-merge:** Quick Validation and Phase 7 Performance passed on `efb72b5d...`; the post-merge Release Gate publishes the stable GitHub APK/source release.
- **Google Play/Cloud:** foreground-location declaration, privacy/Data safety and external OAuth/Cloud settings remain separate publication work.

Release details: [v1.0.6 notes](docs/releases/v1.0.6.md), [implementation and phone checks](docs/CUSTOM_EXERCISE_DISTANCE_1_0_6.md).


## 1.0.7 implementation and phone feedback history

User-authorized first cleanup step on `fix/backup-gps-consolidation`, based on post-merge-validated main `ef09c5ee44b61902a7bb265f8ef2b3d2ea8de88e` (public 1.0.6 app source verified against its APK).

- Validate imported identifiers and nested rows before restore mutations, preventing imported program IDs from executing through inline actions.
- Bind GPS trips to stable set IDs, migrate running 1.0.6 trips, stop tracking before removing its owning set, and protect asynchronous deletion.
- Consolidate 68 historical backup-builder definitions into one. Preserve all exported fields, legacy recovery and call-time Health consent.
- The user explicitly expanded this same unmerged candidate to include fixed Home/Calendar/collapsed Health layouts, a shared explicit-Save program editor, cardio statistics and a GPS-free indoor timer. This scope expansion is authorized; it is not phone acceptance of the earlier 2691 APK.
- Phone feedback on the 2692 APK: restore the full-size personal planner with its full-width button whenever no workout draft exists. Compact planner styling applies only to an unfinished workout; removing its draft, navigation, refresh and theme changes restore the normal card. On narrow screens, surrounding summaries recover space while all content remains visible.
- Phone feedback on 2693: keep Calendar's rounded top border below navigation and fit its outer frame to its content. Clear Calendar-only layout when the shared panel switches to Coach or Settings. Keep Health column/card widths and compact summary styling stable when any disclosure expands; expanded content remains normally scrollable. A populated-calendar/two-theme/five-viewport browser regression checks actual wheel scrolling and all four Health disclosure families.
- Home fits with and without a saved workout draft; expanded Health remains scrollable. Calendar keeps the whole month and the full two-column planning settings visible without scrolling; its selectors and temporal pickers still open above the fixed page. Long selected-day details retain their own scrolling region. Pending workouts use Resume as the primary Home action; starting another workout remains available on Workout.
- Workout and Programs share a lazy prescription editor. Save validates all fields and updates the summary immediately; editing a non-active program does not activate it. Saved prescriptions feed the next workout.
- Cardio: local Monday-based weekly kilometres, previous week, eight-week overview, distance-weighted pace and separate same-activity/same-distance fastest records with second-best comparison. Missing distance is not invented.
- Indoor timer: start/pause/resume/reset without GPS or a permission request, stable set ownership, manual kilometres and paused time corrections, draft reload/background recovery. Internal timer state is removed before history recording.
- Local Node: 117 passed; the new behavioral Chromium checks cover four languages and 320/360/393/412 px sizes, actual Save/start flows, timer reload/pause/delete/finish and cardio records. The complete final 2695 UI, performance and signed Android gates passed.
- **Accepted:** signed 2695 was approved on the phone on 2026-10-03, with explicit main merge, documentation and GitHub release authorization.

- Phone feedback on 2694 is recorded before implementation as [#95](https://github.com/fuloplevente1998/TrainPilot/issues/95), [#96](https://github.com/fuloplevente1998/TrainPilot/issues/96), [#97](https://github.com/fuloplevente1998/TrainPilot/issues/97), [#98](https://github.com/fuloplevente1998/TrainPilot/issues/98). Candidate 2695 builds directly on 2694: matching Home Coach frame, upper-right compact Progress comparison, a Journal Statistics destination with Personal records/Cardio sections, and shared Language/Theme dropdown animation. The subsequent 2695 phone approval is recorded above.

Implementation and phone checklists: [backup/GPS consolidation](docs/BACKUP_GPS_CONSOLIDATION_1_0_7.md), [program editing and cardio](docs/TRAINING_CARDIO_1_0_7.md). Version code 2695 upgrades public 2690 and earlier 2691/2692/2693/2694 test candidates.

## Phase 5–7 acceptance boundaries

**Phase 5 — Health + shared design system (#27, #41, #44).** Correct 7-calendar-day pulse source/semantics and weight edit; audited common theme-aware chevrons (navigation only; actual play controls remain play), Health chart safe zone and basic-matte/vivid-glow styling; unified 2×3 Today status in **existing** Home/Health positions with `Alvás / HRV / Pulzus / Lépések / Mai edzés / Regeneráció`. Home grid visibly more compact than Health grid; both share one daily data model.

**Phase 6 — Journal/media reliability (#42, #43).** Fix approved Android camera photos being reported as cancelled while preserving gallery and genuine cancellation; make the photo dialog compact with top-right red X and theme-aware styles. For #43, keep the Journal inline Health refresh visually stable and specifically suppress the brief `Health Connect adatok lekérése` loading window/text; preserve expanded state and scroll position.

**Phase 7 — latency and smoothness (#19).** Profile start/navigation, exercise editor, Home/Health/Coach/Journal, history expansion/Health sync, photo flow and scroll with representative datasets. Measure baseline before refactoring; inspect layered legacy wrappers, scheduled DOM rewrites, duplicate reads/writes, event handlers and WebView work without presuming causality. Quantify before/after on the same phone; protect legacy data and Drive synchronization.

## Previous phone-approved behavior to preserve

### Accepted #18 behavior

- Old ID-less workout and body-weight records must not disappear because of legacy key collisions.
- Truly distinct records that share an old date/time-style key must remain separate.
- True duplicate copies originating from repeated Drive snapshots must collapse back to one logical record.
- Legacy rows receive stable deterministic sync IDs.
- `cloudBase` and bounded older snapshots may rescue missing legacy rows without resurrecting stable-ID rows that may have been explicitly deleted.
- Drive merge remains three-way/conflict-aware for the existing scalar/settings data.
- Home Coach startup and post-navigation rendering must remain visually identical; the accepted **Mai javaslat** card must not be replaced by the older compact Coach card after launch.

### Accepted Phase 4 — Coach, TrainPilot 1.7.1

- All exercises with valid Journal history participate in Coach Statistics, including weighted, repetition, timed and bilateral `mp/oldal` types; avoid the former 12-row cap.
- Next-workout Coach summary displays program name, training day and date/time; responsive at narrow mobile widths.
- Home Coach startup remains consistent; Phase 4 Release Gate #124 and post-merge `main` Quick #477 passed before phone-approved merge.

### Accepted Phase 3 / TrainPilot 1.7.0 behavior

- The Journal list stays compact and shows only workout-level metadata while collapsed.
- Expanding a workout shows the accepted one-row **1×4** summary metrics with icons: exercises, sets, total reps and total volume.
- **Mit edzettél?** contains independently collapsible exercise rows; opening an exercise injects its compact editor locally without re-rendering the whole Journal card.
- Per-exercise editing supports modifying sets, adding/removing sets, Save, Cancel and deleting that exercise.
- **+ Gyakorlat hozzáadása** adds a new exercise into the logged workout; an exercise already present is extended with additional sets instead of duplicated.
- Health data stays inside the expanded workout; its chevron is right-aligned and uses the same visual pattern as Workout photos.
- Deleted workouts retain local tombstones so stale Drive snapshots cannot resurrect them.
- Journal/Statistics/Home detail arrows use the accepted unified chevron language.
- The personal training planner from #37 opens as a compact overlay panel and preserves the underlying route.
- Final phone feedback fixes: exercise editor opening no longer triggers a whole-Journal render, and the Health chevron matches the Workout photos disclosure.
- TrainPilot 1.7.0 release gate #122 passed all regression, Chromium/UI, signing and APK/source verification checks.

### Phase 3 UI consistency requirement – unified right chevrons

During Phase 3 Journal/UI polish, all right-facing navigation/detail chevrons that serve the same interaction role must be visually unified to the accepted Home **Mai javaslat** reference arrow.

Reference behavior/style:
- use the same **right-facing chevron form** as the Home card;
- match its visual **shape, stroke/weight, size, accent/gold color and vertical alignment**;
- use consistent right-side spacing/padding;
- do not mix visually different triangle/play icons, thin glyphs or mismatched chevrons for equivalent “open details / navigate right” actions;
- preserve accessibility, tappable hit area and existing navigation behavior;
- this is a UI consistency change only and must not alter the underlying Journal/Statistics navigation logic.

This requirement is part of **Phase 3 (#29 + #30)** unless explicitly moved later.

## Permanent TrainPilot release process — mandatory for **each subsequent app release**

1. Branch from the latest **physically phone-approved and post-merge validated** `main` app commit. For the 1.0.0 preparation, start from the published 1.7.7 / 2670 baseline and include later documentation-only commits; never branch from an unapproved development APK.
2. Confirm phase issue acceptance criteria; implement its scope with targeted tests and protected earlier behavior.
3. Run targeted tests, **full Node/regression**, **full Chromium/UI**, and build a **signed, upgrade-safe Android APK** with APK/package/source/version checks.
4. Deliver that phase's APK for a physical Android phone test. If rejected, fix the **same phase branch**, repeat tests and deliver another APK.
5. **No merge until explicit user approval**. Default to phone approval; an explicit instruction to publish the current validated build can override that gate and must be recorded separately from physical-phone evidence. After approval merge to `main`, verify the post-merge workflow, record accepted commit/PR/issue status.
6. Only then begin the next UX work from the newly approved `main`. Every UX release requires its own testable, signed APK. Documentation/test-only maintenance cannot silently alter the app artifact.

Never trade data correctness, historical Journal recovery, Drive merge/tombstone safety, app signing continuity, Coach logic, or accepted UX for UI simplification or latency improvements. A completed CI run, a draft PR and a mockup are not phone acceptance.

## Recovery instruction

At the beginning of a future TrainPilot conversation, read this file, the completed Phase 5–7 roadmap and the permanent performance policy. Verify the current `main` source and its published APK independently. Preserve application ID, signing certificate and data compatibility. The 1.0.5 / 2687 publication was explicitly authorized before its final physical-phone test; do not mistake that exception for a completed device test. Future application changes default to signed APK → physical-phone approval → main merge → post-merge validation.
