# TrainPilot – Development Status

Last updated: 2026-09-23

This file is the current handoff/source-of-truth for continuing TrainPilot development across chats.

## Current stable line

- Stable branch: `main`
- Current package version: **1.7.0**
- Android versionCode: **2659**
- Completed/accepted roadmap phases so far:
  - **Phase 2 – #15 + #16 Demo / active-workout UI**: completed and phone-approved.
  - **Original Phase 1 – #18 Drive sync and data protection**: completed and phone-approved.
  - **Phase 3 – #29 + #30 Journal full polish + #37 personal planner panel**: completed and user-approved for `main` after the final phone feedback fixes.
- PR **#34 – #15 unified exercise demo panel**: **merged**
- PR **#35 – #16 active workout sticky controls and rest timer**: **merged**
- PR **#36 – #18 Drive sync data-safety and legacy dedupe**: **merged**
- #18 merge commit: `25e3deb7edc0480b86fb5a9380935a9c7adf33f4`
- #18 signed APK release gate: **passed**
- #18 full Node/regression suite: **passed**
- #18 full Chromium/UI regression suite: **passed**
- #18 APK/package/source verification: **passed**
- #18 physical-phone validation: **passed**
- #18 post-merge `main` Quick TrainPilot Validation (run `35856303959`): **passed**
- Issues **#15, #16 and #18**: **closed / completed**
- The current `main` is the only valid baseline for the next development step.
- **Phase 3 release gate #122**: full Node regression, full Chromium/UI regression, signed APK build and APK/package/source verification all passed.
- **Next development step: Phase 4 – #17 + #21 Coach full exercise coverage.**

### Accepted #18 behavior

- Old ID-less workout and body-weight records must not disappear because of legacy key collisions.
- Truly distinct records that share an old date/time-style key must remain separate.
- True duplicate copies originating from repeated Drive snapshots must collapse back to one logical record.
- Legacy rows receive stable deterministic sync IDs.
- `cloudBase` and bounded older snapshots may rescue missing legacy rows without resurrecting stable-ID rows that may have been explicitly deleted.
- Drive merge remains three-way/conflict-aware for the existing scalar/settings data.
- Home Coach startup and post-navigation rendering must remain visually identical; the accepted **Mai javaslat** card must not be replaced by the older compact Coach card after launch.

### Current six-phase roadmap and execution order

The original six-phase roadmap is:

1. **Drive sync and data protection** – issue **#18** ✅
2. **Demo and active workout** – issues **#15 + #16** ✅
3. **Journal full polish** – issues **#29 + #30** ✅
4. **Coach full exercise coverage** – issues **#17 + #21**
5. **Health pulse-trend data source** – issue **#27**
6. **Performance and smoothness** – issue **#19**

Because development began with Phase 2 before the original order was recovered, the actual completed order is:

**Phase 2 (#15 + #16) ✅ → Phase 1 (#18) ✅ → Phase 3 (#29 + #30, plus #37) ✅ → Phase 4 (#17 + #21) → Phase 5 (#27) → Phase 6 (#19).**

The **next phase is Phase 4: #17 + #21 Coach full exercise coverage**, and it must start only from the newly accepted TrainPilot 1.7.0 `main`.

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

## Permanent TrainPilot development / release workflow – MANDATORY

This workflow is the default rule for the current six-phase development cycle and for future TrainPilot development unless the user explicitly changes it.

### Core rule

**Every development phase must produce its own testable signed APK.**

A phase is not considered accepted, finished, or eligible to become the new baseline until the user has installed and tested that phase's APK on a physical phone and explicitly approved it.

### Required phase flow

For **every phase**, always use this sequence:

1. Start from the latest **phone-approved `main`** commit.
2. Create/work on a dedicated branch for that phase.
3. Re-read the relevant GitHub issue(s) and inspect the current implementation.
4. Implement only the planned scope of that phase, with minimal regression risk.
5. Run all targeted tests relevant to the changed functionality.
6. Run the **full automated regression suite**.
7. Run the **full Chromium / UI regression suite**.
8. Verify that all previously accepted functionality from earlier versions/phases still works.
9. Review the diff against the issue acceptance criteria and protected behavior.
10. Build a **signed Android APK** for that phase.
11. Run APK/package/source/version consistency and install/upgrade-path checks required by the release gate.
12. Provide the phase APK to the user for **physical-phone testing**.
13. **Do not merge the phase to `main` before explicit phone approval.**
14. If the phone test finds a problem, fix it on the same phase branch, repeat the required tests, build a new APK, and test again.
15. After explicit phone approval, merge the accepted phase to `main`.
16. Run post-merge `main` Quick TrainPilot Validation and any required merge/release verification.
17. Record the accepted commit / PR / issue status.
18. **Only then start the next phase, branching from this newly accepted `main`.**

### Six-phase chain rule

The current six-phase cycle must therefore progress strictly like this:

`1.6.9 minor 2 main`
→ **Phase 1 branch**
→ full tests
→ signed APK
→ phone test
→ user approval
→ merge to `main`
→ `main` validation
→ **Phase 2 from the new `main`**
→ repeat the same process
→ Phase 3
→ Phase 4
→ Phase 5
→ Phase 6.

There must be **one separately testable APK for every one of the six phases**, so the user can directly see and compare what each phase changed.

### No shortcut rule

The following are not acceptable substitutes for phone approval:

- CI passing by itself;
- Chromium tests passing by themselves;
- successful APK build by itself;
- code review by itself;
- a later phase appearing to include the same fix.

A phase becomes the new baseline only after its own APK has been tested and accepted on the user's phone.

### Regression rule

The full regression process remains active in **all six phases**.

Do not reduce the checks just because a phase looks small or UI-only. Small changes can still affect navigation, overlays, Android Back behavior, Health, Coach, Journal, active workout state, persisted data, or mobile layout.

For every phase preserve:

- targeted functional tests;
- full Node/automated regression suite;
- full Chromium/UI regression suite;
- previous accepted-phase regression coverage;
- signed APK build;
- APK/package/source/version verification;
- physical Android phone test;
- post-merge `main` validation.

### Performance phase

The final performance-optimization phase must follow the same gate as every other phase.

Additionally:

- capture a measurable baseline before optimization;
- compare the optimized build against the previous phone-approved phase;
- preserve legacy Journal/data-recovery protection;
- do not trade correctness or data safety for speed;
- verify Android WebView scrolling/rendering behavior and large-history performance where applicable.

## Protected expectations

- Never build a new phase from an unapproved development branch.
- Never use an untested phase as the base of the next phase.
- Keep the signed-app upgrade path and Android signing identity unchanged.
- Protect user workout, Journal, weight, Health and synchronization data.
- Preserve already phone-approved UI/UX behavior unless the active phase explicitly changes it.
- Avoid large unrelated bundles of changes; keep phase scope reviewable and attributable.
- `main` means the latest phone-approved stable baseline.

## Resume instruction

When starting or recovering a TrainPilot conversation:

1. Read this file first.
2. Check the current `main`, open issues, latest merged PRs and validation state.
3. Treat the latest phone-approved `main` as the only valid development baseline.
4. Continue the current phase without skipping the APK → phone approval → merge gate.

If chat history is lost or deleted, this GitHub document takes precedence for the development/release procedure.
