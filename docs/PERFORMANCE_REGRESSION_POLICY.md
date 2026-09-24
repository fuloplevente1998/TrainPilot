# TrainPilot — permanent performance regression policy

Introduced after phone acceptance and main merge of **TrainPilot 1.7.4 / 2663**, Phase 7 issue [#19](../issues/19), PR [#48](../pull/48), main merge `4a85b33e3545197a9053461840e60a178b45a341`. Do not revert the measured optimizations while repairing unrelated UX.

## Reproducible reference

The checked-in Chromium benchmark `tests/browser/phase7-performance-19.cjs` seeds **240 logged workouts × 8 exercises × 4 sets**. Reference is a representative final Phase 7 CI run, not a physical-phone timing guarantee:

| Path | Accepted 1.7.4 reference | Measured Phase 6 baseline |
|---|---:|---:|
| Collapsed Journal route | ~104 ms median, **1** history() call, ~3,536 DOM nodes | ~3,166 ms, **242** calls, ~99,056 nodes |
| Home route | ~31 ms median | ~149 ms |
| Programs route | ~31 ms median, ~261 DOM nodes | ~146 ms, ~4,635 nodes |
| Coach **first visible feedback** | ~32–64 ms | ~350–540 ms |
| Repeated route roundtrip | ~120–151 ms | ~3.1–3.3 s |

Do not compare cold and warmed paths, different phone models or datasets as if equivalent. Coach's expensive **full Progress/Statistics** section hydrates **after first paint**; its complete async duration must not be misreported as first-visible-feedback latency.

## Required checks for every subsequent change

1. Begin from the latest phone-approved and post-merge-validated `main`, not an older feature branch. Run the checked-in benchmark **before and after** any Journal, Health, Coach, Programs, history, storage, navigation or dropdown/disclosure change, against the same reference dataset. Include first-open and repeated-open behavior. Keep the benchmark and its stored log in the PR evidence.
2. The `Phase 7 Performance Baseline` workflow runs for **all PRs to main and pushes to main**. Its `phase7-performance-budget.cjs` guard fails on excessive repeated history reads or hidden DOM growth and on **generous CI tripwires** (Journal >650 ms median, Home/Programs >250 ms median, Coach visible feedback >250 ms, slowest roundtrip >1000 ms). Structural limits: collapsed Journal >6,000 DOM nodes or >3 median history() calls; Programs >1,000 nodes. Timing limits are regression alarms, **not** hard Android service guarantees; examine runner noise and repeated runs before altering budgets.
3. For each UX fix, add a **targeted behavioral regression** for the actual first render/interaction and repeat it after navigation/refresh/theme change at **320/360/393/412 px**. Protect first-feedback time, render count, MutationObserver churn, DOM nodes, history() calls and full-page redraws. Avoid fixing WebView styling by eagerly constructing every collapsed Journal editor, adding duplicate event handlers, or rescanning full history per card.
4. After each change, run targeted tests, full Node regressions and full Chromium/UI tests; build a signed, upgrade-safe Android APK and verify package/version/source, then obtain physical-phone approval for UX-affecting changes **before merging**.
5. Preserve legacy data recovery, distinct same-date records, deterministic IDs, Drive three-way merge and tombstones (#18); maintain accepted Phase 3–6 journal editing, Health semantics, photo camera/gallery/cancel behavior and Coach all-exercise statistics. No persistent history cache or blind global rerender as a speed workaround.
6. Profile first when a budget fails. Keep only changes that improve measured outcomes without breaking the above behavior; revert slower experimental refactors. Update this reference **only with a reproducible baseline and explanation**.

## Known post-1.7.4 UI follow-ups

- [#49](../issues/49): first-open Journal exercise picker shows native grey WebView chooser; subsequent branded dropdown is too narrow for long exercise names. Repair the **first-open** initialization and responsive dropdown **without undoing lazy Journal hydration**.
- [#50](../issues/50): Health pulse disclosure differs visually from the accepted weight disclosure and appears to flash stale UI on a real phone. Diagnose the exact path before assuming a cause; protect correct seven-day pulse semantics and avoid global rerenders.
- [#51](../issues/51): long-term benchmark expansion and performance safeguards.

These user-observed issues were deferred by explicit approval so the **already measured and phone-felt speedup** could land on `main` first. Their existence does not imply that Phase 7 caused them; earlier origin is unverified.
