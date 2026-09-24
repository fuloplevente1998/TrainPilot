# TrainPilot — revised Phase 5–7 roadmap

Completed 2026-09-24. Phase 5, Phase 6 and Phase 7 have each shipped to phone-approved `main`. The ongoing release contract and future benchmark budget are in [PERFORMANCE_REGRESSION_POLICY.md](PERFORMANCE_REGRESSION_POLICY.md).

## Stable baseline and release contract

- **Latest stable approved main app:** TrainPilot **1.7.4 / Android versionCode 2663**. Phase 7 PR #48 merged at `4a85b33e3545197a9053461840e60a178b45a341`; post-merge Quick Validation #652 passed. Phase 7 issue #19 is completed.
- This approval was for the **measured and phone-felt speed optimization**. Existing Journal picker UI (#49) and Health pulse disclosure visual behavior (#50) are separately tracked follow-ups. Their suspected earlier origin is not proven.
- For every subsequent **UX** change: branch from latest validated `main`, add targeted first-open and repeated-open regression, run permanent benchmark budgets, full Node and Chromium/UI tests, build signed upgrade-safe APK, obtain explicit physical-phone acceptance, **then merge**, and verify post-merge main.
- The future performance guard and required baseline evidence are documented in [PERFORMANCE_REGRESSION_POLICY.md](PERFORMANCE_REGRESSION_POLICY.md); tracking issue #51.

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

**Result:** completed and phone-approved. PR #47 merged to `main`; #42/#43 closed. Accepted TrainPilot 1.7.3 / 2662 baseline commit: `f064298ddbe1e7e4cf1ecdfca18b3d93899dc74a`.

## Phase 7 — App-wide latency, jank and render architecture

**Issue:** [#19](https://github.com/fuloplevente1998/TrainPilot/issues/19); **result:** accepted on physical Android phone and merged via [PR #48](https://github.com/fuloplevente1998/TrainPilot/pull/48), **TrainPilot 1.7.4 / 2663**. Validated Phase 6 parent was `f064298ddbe1e7e4cf1ecdfca18b3d93899dc74a`; Phase 7 merge commit `4a85b33e3545197a9053461840e60a178b45a341`.

- Reproducible 240-workout benchmark demonstrated Journal ~3166→104 ms (median), 242→1 history reads, ~99k→3.5k DOM nodes; Home ~149→31 ms, Programs ~146→31 ms and Coach first visible feedback ~350–540→32–64 ms.
- Merged architecture: lazy Journal/Program detail, operation-scoped fresh history snapshots, batched Coach Progress and deferred full Progress hydration. Earlier phase behavior and Drive legacy data safeguards remain protected by regression coverage.
- Full Node + Chromium/UI regression, signed 1.7.4 Android release gate #228, Performance #70 and PR Quick #651 passed. The user tested the APK and explicitly requested merging the speed improvements first. Post-merge Quick #652 passed.
- Separately tracked follow-ups: Journal first-open native picker and insufficient dropdown width [#49](https://github.com/fuloplevente1998/TrainPilot/issues/49); Health pulse disclosure UI flash [#50](https://github.com/fuloplevente1998/TrainPilot/issues/50); permanent future slowdown guard [#51](https://github.com/fuloplevente1998/TrainPilot/issues/51). Do not presume these bugs originated in Phase 7.
- All future changes must follow [the permanent performance policy](PERFORMANCE_REGRESSION_POLICY.md) and its benchmark CI gate without sacrificing #18 / Phase 3–6 behavior.

## Dependency and issue map

| Phase | Issue(s) | Work | State |
|---|---|---|---|
| 5 | #27, #41, #44 | Health + global visual consistency + shared today grid | ✅ Phone-approved and merged (PR #40 + #45), TrainPilot 1.7.2 |
| 6 | #42, #43 | Native camera/photo dialog + minor Journal inline Health loading-flash fix | ✅ Phone-approved and merged (PR #47), TrainPilot 1.7.3 |
| 7 | #19 | App-wide diagnostics and measured optimizations | ✅ Phone-approved and merged (PR #48), TrainPilot 1.7.4 / 2663 |

**Next:** use approved 1.7.4 main for independently tested follow-up issues **#49 / #50**, and preserve the permanent regression policy **#51**. No UI-code merge before its own signed APK and phone approval.