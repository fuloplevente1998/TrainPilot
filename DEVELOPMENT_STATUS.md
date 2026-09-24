# TrainPilot — Development Status

Last updated: 2026-09-24. The authoritative completed phase history is [docs/PHASE_5_6_7_ROADMAP.md](docs/PHASE_5_6_7_ROADMAP.md). The permanent future performance gate is [docs/PERFORMANCE_REGRESSION_POLICY.md](docs/PERFORMANCE_REGRESSION_POLICY.md).

## Stable baseline and current work

- **Latest phone-approved stable app: TrainPilot 1.7.5 / Android versionCode 2664**, merged from PR [#54](https://github.com/fuloplevente1998/TrainPilot/pull/54) to validated `main` commit `8464b3eec72803482eb5aa8037a97a7b69b09100`. Post-merge Quick Validation #680 and Phase 7 Performance #80: PASS. Signed candidate Release Gate #237, Quick #679 and Phase 7 Performance #79: PASS.
- **#49 and #50 completed and merged:** Journal exercise selector is branded on first open after lazy hydration, with a wider multi-line dropdown; Health Pulse opens the existing decorated 7-day chart in place, without a transient legacy panel or full Health Today rewrite. Both issues are closed. Keep the targeted first-open/repeated-open mobile regression tests.
- **Published GitHub Release remains 1.7.4 / 2663**, the phone-approved Phase 7 performance build. 1.7.5 is accepted on main but has **not yet been published as a GitHub Release**. Do not change or overwrite the historical v1.7.4 tag.
- **Phase 7 speedup remains mandatory:** lazy Journal and Program detail hydration, operation-scoped fresh history snapshots (no persistent cache), batched Coach Progress statistics and delayed full Progress hydration after first paint. Keep the permanent [performance regression policy](docs/PERFORMANCE_REGRESSION_POLICY.md), budget CI and [#18 legacy/Drive safety](https://github.com/fuloplevente1998/TrainPilot/issues/18).
- Measured Phase 7 reference 240-workout Chromium dataset: Journal ~3166→104 ms median, 242→1 history calls, ~99k→3.5k DOM nodes; Home ~149→31 ms, Programs ~146→31 ms, Coach first visible feedback ~350–540→32–64 ms. Regression gates also passed for the accepted 1.7.5 main.

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

1. Branch from the latest **physically phone-approved and post-merge validated** `main` app commit. For the upcoming UI fixes (#49/#50), use the accepted 1.7.4 / 2663 main baseline including any subsequent validated docs/test-only commits; never branch from an unapproved development APK.
2. Confirm phase issue acceptance criteria; implement its scope with targeted tests and protected earlier behavior.
3. Run targeted tests, **full Node/regression**, **full Chromium/UI**, and build a **signed, upgrade-safe Android APK** with APK/package/source/version checks.
4. Deliver that phase's APK for a physical Android phone test. If rejected, fix the **same phase branch**, repeat tests and deliver another APK.
5. **No merge until explicit user phone approval**. After approval merge to `main`, verify the post-merge workflow, record accepted commit/PR/issue status.
6. Only then begin the next UX work from the newly approved `main`. Every UX release requires its own testable, signed APK. Documentation/test-only maintenance cannot silently alter the app artifact.

Never trade data correctness, historical Journal recovery, Drive merge/tombstone safety, app signing continuity, Coach logic, or accepted UX for UI simplification or latency improvements. A completed CI run, a draft PR and a mockup are not phone acceptance.

## Recovery instruction

At the beginning of another TrainPilot conversation, read this file, the completed Phase 5–7 roadmap and the permanent performance policy. Verify latest main (accepted app baseline **1.7.5 / 2664**, PR #54 merged at `8464b3eec72803482eb5aa8037a97a7b69b09100`), and check current issues before creating future branches. The signed-APK → physical-phone-approval → merge-to-main → post-merge-validation gate remains mandatory for UI changes.
