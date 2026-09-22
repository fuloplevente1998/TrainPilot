# TrainPilot – Development Status

Last updated: 2026-09-22

This file is the current handoff/source-of-truth for continuing TrainPilot development across chats.

## Current release work

- Target patch: **1.6.2**
- Working branch: `fix/1.6.2-minor-ui`
- Base branch: `main`
- Open PR: **#6 – TrainPilot 1.6.2 minor UI polish**
- Current app-code head before this status note: `0386e29`

### Implemented, waiting for phone validation

1. **#2 – Coach: deleted Quick Workout causes stale “today workout” count**
   - Fix commit: `45fdd16`
   - Automated validation: passed.
   - Status: **DO NOT merge/close yet. Build APK and validate on phone first.**

2. **#11 – Theme selector as inline dropdown**
   - Fix commit: `0386e29`
   - Automated validation: passed.
   - Status: **DO NOT merge/close yet. Build APK and validate on phone first.**

### Approval gate

Next action is to build a signed **TrainPilot 1.6.2 test APK** from the current feature branch.

Only after Levi confirms #2 and #11 are correct on the phone:
- close #2 and #11 as appropriate;
- merge PR #6 / the validated 1.6.2 changes into `main`;
- continue with the remaining issues below.

If either phone test fails, fix the failed issue on the feature branch, run checks again, create a new APK, and repeat phone validation before merging.

## Remaining work – strict order

Work on these **one at a time**, in this order:

1. **#12** – Side plank: right stopwatch overwrites lower set field; separate Left/Right time fields.
2. **#13** – Home Coach card: Coach-page design, larger/full-card tap target, no redundant Details/Statistics actions.
3. **#14** – Health → Weight log: unified panel/opening behavior; replace large Back button with top-right red X.
4. **#15** – Demo/video UI: compact unified panel, top-right red X, one-tap opening with video + description.
5. **#16** – Active workout controls: sticky Previous/Next/Finish bar and more compact mobile workout layout.

## Required workflow for #12–#16

For each issue separately:

1. Re-read the issue and inspect the current implementation.
2. Implement **only that issue** with minimal regression risk.
3. Run the relevant automated tests and regression checks.
4. Review the diff against the issue acceptance criteria.
5. Commit only when the change is clean and checks pass.
6. Record the commit SHA against the issue.
7. Then move to the next issue.

Do not bundle #12–#16 into one large unreviewed change.

## Protected expectations

- Keep the already accepted 1.6.x UI fixes unchanged unless an issue explicitly requires touching them.
- Preserve current workout data/finish logic while changing UI.
- Preserve the signed-app upgrade path and existing Android signing identity.
- Keep `main` as the validated/stable line; phone-test release candidates before promoting them.
- Full APK/release-gate builds are for validation points; quick checks should be used during individual code iterations.

## Resume instruction

When starting a new ChatGPT conversation, ask to continue TrainPilot development and read this file plus the currently open GitHub issues before making changes. The current intended sequence is:

**#2 + #11 phone validation → merge to main if approved → #12 → #13 → #14 → #15 → #16.**
