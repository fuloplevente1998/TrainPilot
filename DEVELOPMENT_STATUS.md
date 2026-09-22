# TrainPilot – Development Status

Last updated: 2026-09-22

This file is the current handoff/source-of-truth for continuing TrainPilot development across chats.

## Stable release promotion

- Approved version: **1.6.3**
- Android versionCode: **2652**
- Promotion source branch: `fix/1.6.2-minor-ui`
- Base branch: `main`
- PR: **#6**
- User phone validation: **approved**
- #2 and #11 are accepted for promotion to `main`.

### Included accepted fixes

- **#2** – Coach: deleted Quick Workout no longer leaves a stale “today workout” count.
- **#11** – Theme selector uses a floating, internally scrollable dropdown aligned with the Language selector behavior; opening it does not push lower Settings cards down.

The 1.6.3 promotion must pass the full signed Release Gate before merging.

## Remaining work – strict order

After 1.6.3 is on `main`, work on these **one at a time**, in this order:

1. **#12** – Side plank: right stopwatch overwrites lower set field; separate Left/Right time fields.
2. **#13** – Home Coach card: Coach-page design, larger/full-card tap target, no redundant Details/Statistics actions.
3. **#14** – Health → Weight log: unified panel/opening behavior; replace large Back button with top-right red X.
4. **#15** – Demo/video UI: compact unified panel, top-right red X, one-tap opening with video + description.
5. **#16** – Active workout controls: sticky Previous/Next/Finish bar and more compact mobile workout layout. Previous stays on the left; Next stays on the right.

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

- Keep the accepted 1.6.3 UI and Health fixes unchanged unless a later issue explicitly requires touching them.
- Preserve current workout data/finish logic while changing UI.
- Preserve the signed-app upgrade path and existing Android signing identity.
- Keep `main` as the validated/stable line.
- Quick checks are used during individual issue iterations; full signed APK/release-gate builds are used at validation/promotion points.

## Resume instruction

When starting a new ChatGPT conversation, ask to continue TrainPilot development and read this file plus the currently open GitHub issues before making changes.

Current sequence after the 1.6.3 promotion:

**#12 → #13 → #14 → #15 → #16.**
