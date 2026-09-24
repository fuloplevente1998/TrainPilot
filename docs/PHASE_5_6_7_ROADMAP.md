# TrainPilot — revised Phase 5–7 roadmap

Approved 2026-09-24. This plan **supersedes the old six-phase roadmap**. The six currently open issues are assigned exactly once: #27, #41, #44, #42, #43 and #19.

## Stable baseline and release contract

- Latest phone-approved stable app code: `main`, TrainPilot **1.7.2 / Android versionCode 2661**. Phase 5 PR #40 and final phone-chevron follow-up PR #45 are merged; accepted app baseline commit: `377523e0fc94776a494d44ac59ddf31a30189988`; post-merge Quick Validation `36009395458` PASS.
- **Phase 5 is complete and accepted. Phase 6 is next.** Phase 6 must branch from the latest validated `main` after any CI-only maintenance.
- **Never merge an unapproved phase**. Complete targeted checks + full Node regression + full Chromium/UI regression + signed APK + package/source verification, then physical-phone test and explicit approval. Only after merging and post-merge `main` validation start the next phase from the newly approved `main`.
- Planned scopes below are phase boundaries, **not claims of implemented functionality**. If a severe data-loss/security issue emerges, stop and handle it explicitly rather than concealing it in performance or design work.

## Phase 5 — Health + shared visual system

**Issues:** [#27](https://github.com/fuloplevente1998/TrainPilot/issues/27) + [#41](https://github.com/fuloplevente1998/TrainPilot/issues/41) + [#44](https://github.com/fuloplevente1998/TrainPilot/issues/44).

**Result:** completed and phone-approved. PR #40 plus final follow-up PR #45 are merged; #27/#41/#44 are closed.

- **#27 Health correctness:** explicit Health Connect → local daily `averageHeartRate` provenance, true last seven calendar days, today vs trend, missing/0 handling and persistent body-weight editing using a modern compact dialog. Already implemented and automatically tested; retain the phone-validated behavior and retest within the complete Phase 5 APK.
- **#41 global navigation and theme styling:** audit all relevant right-facing **navigation/disclosure** arrows across Home, Workout, Health, Programs, Coach, Journal, Calendar and Settings. Use one shared, theme-token-following chevron; replace remaining white triangle/detail arrows, including dynamically rendered surfaces. **Actual play/media controls stay play icons.** Fix Pulzustrend chart/chevron safe zone and apply **matte/no glow to basic themes**, allowing accent glow only on vivid themes. Previous automated PASS did not catch every visible phone defect, so phone testing must cover the reported Workout and Programs locations.
- **#44 unified “Mai állapot”:** same six daily values in the same 2×3 order at both unchanged page positions:
  `Alvás | HRV | Pulzus` / `Lépések | Mai edzés | Regeneráció`. Share one daily-data model and card system; `Mai edzés` is today's logged workout count, replacing resting HR **in the grid only**. The Home variant must be **visibly shorter/denser**; the Health variant can be larger and closer to square tiles. Do not move the Home section to the page top. Preserve meaningful missing-value states.
- Test theme switches, closed/open disclosures, deferred/dynamic renders, common 320/360/393/412 px viewport sizes, Home/Health data equality, and retained Health/Coach readiness functionality.

**Exit:** completed. TrainPilot 1.7.2 / 2661 is the accepted Phase 5 baseline.

## Phase 6 — Journal/photo camera and Health refresh stability

**Issues:** [#42](https://github.com/fuloplevente1998/TrainPilot/issues/42) + [#43](https://github.com/fuloplevente1998/TrainPilot/issues/43).

**Branch:** create `feat/phase6-journal-media-health-42-43` from the latest validated `main`. Create a separate Phase 6 PR and signed release APK.

- **#42 native camera functional repair:** investigate approved camera shot → Android activity result/URI → imported private image. The current phone behavior reports “Művelet megszakítva” although the camera shot was approved; gallery selection does work. Keep real cancellation distinguishable from a successful capture, preserve permission/security cleanup, and add appropriate Android-level/test hooks. Modernize the photo-source modal: compact, theme-aware, right-aligned **red X**; remove the old left-side “Bezárás” button; keep `Edzés előtt / Edzés után / Egyéb` and camera/gallery selection.
- **#43 minor UI — Journal → Health `Frissítés`:** the refresh itself works, but the **`Health Connect adatok lekérése` loading window/text briefly flashes into view**. For this inline Journal refresh the global loading UI must remain hidden while Health data refreshes in place. Preserve expanded workout/panel state and scroll position; keep shared theme/button/chevron styling.
- Test captured photo acceptance on an **actual phone**, picker regression, true cancel path, multiple photo additions, app resume, refreshed in-place Journal Health rendering, back navigation and 320–412 px layouts.
- The suspected native callback/URI cause is a **hypothesis until instrumented/verified**; do not assume a specific OEM or Android cancellation behavior without evidence.

**Exit:** separate signed Phase 6 APK and explicit phone approval → merge Phase 6 PR to `main` → verify `main`.

## Phase 7 — App-wide latency, jank and render architecture

**Issue:** [#19](https://github.com/fuloplevente1998/TrainPilot/issues/19), expanded from the original post-1.6.5 slowdown into an app-wide performance investigation.

**Branch:** create `feat/phase7-performance-19` **only after approved Phase 6 is on `main`**.

**First measure, then change:** compare Phase 6's phone-approved APK with instrumentation and repeatable navigation datasets. Capture startup and initial paint; Home ↔ Workout ↔ Health ↔ Programs/Journal/Coach navigation; opening/editing workout exercises and weight; history with small/large/legacy records; Journal expansion and Health refresh; photo modal entry and return; scrolling and repeated navigation.

Investigate, without presuming the cause:
- Multiple legacy render wrappers, overlay/decorator layers and stacked CSS or hidden DOM.
- Duplicate `render()` calls, `setTimeout` post-decoration, DOM rewrites/mutation churn and layout thrashing.
- Repeated `history()` normalization/copies, duplicated computed stats/Health parsing, redundant storage/Drive writes.
- Event listener accumulation, unreleased photo/media resources, Android WebView main-thread work, GC and frame-rate drops.

Capture useful before/after evidence (e.g. trace/Performance marks, relevant Android WebView frame timing, DOM node count, long tasks, render duration and UI action latency). Set measurable thresholds **after collecting the baseline**, not by inventing targets. Remove/merge legacy layers **only when profiling supports it** and targeted regression demonstrates parity. Do not weaken Drive merge/tombstone safety, persisted data, Coach logic or phase-approved designs.

**Exit:** full regression + browser UI + signed Phase 7 APK; compare with the Phase 6 baseline on the same physical phone; explicit user approval → merge and post-merge validation.

## Dependency and issue map

| Phase | Issue(s) | Work | State |
|---|---|---|---|
| 5 | #27, #41, #44 | Health + global visual consistency + shared today grid | ✅ Phone-approved and merged (PR #40 + #45), TrainPilot 1.7.2 |
| 6 | #42, #43 | Native camera/photo dialog + minor Journal inline Health loading-flash fix | **Next** |
| 7 | #19 | App-wide performance diagnostics and verified optimization | Planned; wait for Phase 6 merge |

**Sequence from here:** latest validated `main` → Phase 6 branch/APK → phone approval → merge/validate `main` → Phase 7 branch/performance baseline/APK → phone approval → merge/validate.