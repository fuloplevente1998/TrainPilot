# TrainPilot — Development Status

Last updated: 2026-09-24. The authoritative phase-by-phase scope is [docs/PHASE_5_6_7_ROADMAP.md](docs/PHASE_5_6_7_ROADMAP.md). This file supersedes the former six-phase plan. **Planning does not mean an issue or APK is implemented or approved.**

## Stable baseline and current work

- Latest **phone-approved** stable app baseline: `main`, TrainPilot **1.7.3 / Android versionCode 2662**, validated main commit `f064298ddbe1e7e4cf1ecdfca18b3d93899dc74a`. Phase 6 PR #47 is merged; #42 and #43 are closed/completed.
- **Phase 6 completed and phone-approved:** camera approval now attaches the photo through the private FileProvider flow, genuine cancel remains cancel, gallery selection remains functional, the photo modal is compact with the accepted red X, and Journal Health refresh no longer flashes the global loading UI or loses expanded/scroll state.
- **Active Phase 7 candidate:** issue #19 on `feat/phase7-performance-19`, based exactly on validated `main` `f064298ddbe1e7e4cf1ecdfca18b3d93899dc74a`; candidate version **1.7.4 / 2663**. Draft PR #48 remains unmerged until explicit physical-phone approval.
- Phase 7 measured bottlenecks rather than removing legacy layers blindly. The main fixes are lazy Journal detail hydration, operation-scoped history reuse, lazy Program detail hydration, batched Progress statistics and post-first-paint Coach Progress hydration.
- Representative 240-workout Chromium benchmark: Journal fell from ~3166 ms / 242 `history()` calls / ~99k DOM nodes to ~88–104 ms / 1 call / ~3.5k nodes; Coach first visible feedback fell from roughly 350–540 ms to ~30–58 ms while the complete statistics still hydrate afterward.
- Remaining roadmap issue: Phase 7 (#19) only. No merge until signed 1.7.4 APK passes physical-phone comparison against 1.7.3.


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

## Permanent TrainPilot release process — mandatory for **each remaining phase**

1. Branch from the latest **physically phone-approved and post-merge validated** `main` commit. Phase 6 must start from the accepted TrainPilot 1.7.2 baseline (plus any validated CI-only maintenance merged afterward), never from an unapproved development branch.
2. Confirm phase issue acceptance criteria; implement its scope with targeted tests and protected earlier behavior.
3. Run targeted tests, **full Node/regression**, **full Chromium/UI**, and build a **signed, upgrade-safe Android APK** with APK/package/source/version checks.
4. Deliver that phase's APK for a physical Android phone test. If rejected, fix the **same phase branch**, repeat tests and deliver another APK.
5. **No merge until explicit user phone approval**. After approval merge to `main`, verify the post-merge workflow, record accepted commit/PR/issue status.
6. Only then begin the next phase from the newly approved `main`. Each of Phases 5, 6 and 7 needs its **own testable, signed APK**.

Never trade data correctness, historical Journal recovery, Drive merge/tombstone safety, app signing continuity, Coach logic, or accepted UX for UI simplification or latency improvements. A completed CI run, a draft PR and a mockup are not phone acceptance.

## Recovery instruction

At the beginning of another TrainPilot conversation, read this file, then [the current roadmap](docs/PHASE_5_6_7_ROADMAP.md), check issues #42/#43/#19 and the actual `main` validation, then continue with the active **Phase 7 (#19)** branch. Retain the signed-APK → physical-phone-approval → merge-to-main → post-merge-validation gate.
