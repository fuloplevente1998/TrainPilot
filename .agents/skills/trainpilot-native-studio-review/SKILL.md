---
name: trainpilot-native-studio-review
description: Review ongoing TrainPilot native Kotlin/Jetpack Compose, Wear OS, GPS/Health and migration work as one sequential cross-functional studio. Use to assess branch feat/native-android-migration, PR #238, changed-code risks, test coverage or release readiness without launching subagents.
---

# TrainPilot native studio review (pilot)

This skill is a **review workflow**, not authorization to change app code, run release gates, merge, or spin up independent agents. Keep `AGENTS.md` authoritative; user instructions and current accepted plans take priority. Respond briefly in Hungarian.

## Scope and current source

1. Read the relevant parts of `AGENTS.md`, `docs/NATIVE_RESUME.md` and the **top current status** of `docs/WORK_HANDOFF.md`. Read `docs/plans/NATIVE_FINAL_DELIVERY.md` only as needed for the changed area; never reload long historical logs by default.
2. Confirm repository `fuloplevente1998/TrainPilot`, branch `feat/native-android-migration`, exact HEAD and draft PR #238. Compare the changed files or precise commits with the applicable acceptance criteria. A handoff or PR body may lag HEAD: do not mistake its earlier snapshot for the current code.
3. Do not modify another developer's uncommitted changes, change the branch, create subagents, or modify code merely because a review is requested.

## Six specialist passes, performed **sequentially by one agent**

- **Technical director:** Identify feature scope, existing canonical implementation, call path and affected contracts. Mark out-of-scope passes N/A.
- **Android / Compose:** Check navigation, ViewModel/data flow, state restoration, explicit user actions and app lifecycle; reuse existing Kotlin implementation.
- **Wear / sync:** If affected, check one workout owner, stable IDs, ack/retry, offline handoff and no duplicate finish/record; otherwise N/A.
- **Data / GPS / Health:** Check Room/JSON/ZIP/Drive compatibility, retry/restore fences, private route storage and genuine Health sources where touched. Never accept fake measurements or undocumented data loss.
- **UX / localization:** Check HU/EN/DE/RO strings, accessibility, compact controls, error/empty/loading states and the accepted five-tab Compose flow. For exercise demo links distinguish canonical video metadata from an unverified search; no auto-open or invented provenance.
- **QA / release:** Match each acceptance criterion to unit/Compose/Wear checks; distinguish test code written from test actually executed. Confirm whether GitHub Actions ran **on the exact app source**. Do not trigger CI for an intermediate `[skip ci]` batch or claim APK/device acceptance from code review.

## Findings and stop conditions

Report up to three prioritized actionable findings with `path:line` and exact commit SHA where available. Separate **verified in source**, **potential risk**, **not yet tested**, and **device acceptance pending**. If current HEAD differs from the reviewed SHA, flag the report as stale instead of claiming current readiness.

For implementation work in a *separate explicitly authorized task*, follow the existing branch workflow: small commits, `[skip ci]` while batching, final consolidated CI and signed paired APKs only at the agreed gate; do not merge, bump versions, install over live user data, or close issues without user/device acceptance.

## Output contract

Use four short headings: `Forrás és hatókör`, `Ellenőrzött elemek`, `Kockázatok / hiányzó bizonyíték`, `Következő mikro-lépés`. Prefer fewer than 220 Hungarian words. Never claim a review means the feature is finished.
