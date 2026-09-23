# TrainPilot – Development Status

Last updated: 2026-09-23

This file is the current handoff/source-of-truth for continuing TrainPilot development across chats.

## Current stable line

- Stable branch: `main`
- Current version: **1.6.9 minor 2**
- Android versionCode: **2658**
- PR **#33 – TrainPilot 1.6.9 minor 2 – compact Health cleanup**: **merged**
- Merge commit: `921c2a26a84c647eeb33e36586c08a38032b1c9d`
- Full regression / Chromium UI validation: **passed**
- Signed Android release APK build and APK/package-source verification: **passed**
- Post-merge `main` Quick TrainPilot Validation: **passed**
- Phone validation: **passed**
- This accepted 1.6.9 minor 2 `main` is the baseline for the next development phases.

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
