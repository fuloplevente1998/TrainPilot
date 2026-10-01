# TrainPilot — Development Status

Last updated: **2026-10-02 (Europe/Budapest)**. Earlier Phase 5–7 history remains below; it is historical. See [docs/PHASE_5_6_7_ROADMAP.md](docs/PHASE_5_6_7_ROADMAP.md) and the permanent [performance regression policy](docs/PERFORMANCE_REGRESSION_POLICY.md).

## Current release

- **TrainPilot 1.0.5 / versionCode 2687**, based on published v1.0.4 / `1d84c54dbcb46f3c6aaf7a94435dafd85586a91a`. Application ID and APK signer are preserved.
- **Scope:** complete Drive dataset merge; opt-in Health export; verified photo ZIP backup and transactional restore; separate erasure/revocation controls; Calendar recreation; bounded request retry and snapshot retention; localized privacy/network messages; patched `brace-expansion` build dependency.
- **Latest fix:** ZIP export reserves the committed local dataset without waiting for Drive/Calendar networking. Drive defers local application until the picker/export closes and rechecks local edits; local photo deletion follows a successful tombstone commit. Network failures release sync state and schedule automatic retries at least 60 seconds apart. Manual sync remains available.
- **Design:** the Health export checkbox shares the app's theme-aware 26px checkbox, selected accent color, checkmark, keyboard focus and minimum 44px label target.
- **Publication authorization:** on 2026-10-02 the user explicitly authorized this final version to merge into main and requested a GitHub Release after validation. This replaces the draft-only restriction for PR #91. It is not a claim that the new build has already passed a physical-phone test.
- **Phone evidence:** the preceding 2686 APK still blocked ZIP with automatic sync enabled; disabling automatic sync allowed export. The final 2687 fix must still be observed on a real device. Keep future physical upgrade, photo round-trip and account authorization checks separate from CI.
- **Performance:** retain lazy Journal/Programs details, operation-scoped history snapshots, batched Coach statistics and the permanent performance regression policy.
- **Google Play/Cloud:** production signing, public policy/contact details, OAuth clients and external-account validation remain configuration work. A GitHub release does not complete these steps.

Release details: [v1.0.5 notes](docs/releases/v1.0.5.md), [implementation and Console work](docs/PUBLICATION_1_0_5.md).

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

1. Branch from the latest **physically phone-approved and post-merge validated** `main` app commit. For the 1.0.0 preparation, start from the published 1.7.7 / 2670 baseline and include later documentation-only commits; never branch from an unapproved development APK.
2. Confirm phase issue acceptance criteria; implement its scope with targeted tests and protected earlier behavior.
3. Run targeted tests, **full Node/regression**, **full Chromium/UI**, and build a **signed, upgrade-safe Android APK** with APK/package/source/version checks.
4. Deliver that phase's APK for a physical Android phone test. If rejected, fix the **same phase branch**, repeat tests and deliver another APK.
5. **No merge until explicit user approval**. Default to phone approval; an explicit instruction to publish the current validated build can override that gate and must be recorded separately from physical-phone evidence. After approval merge to `main`, verify the post-merge workflow, record accepted commit/PR/issue status.
6. Only then begin the next UX work from the newly approved `main`. Every UX release requires its own testable, signed APK. Documentation/test-only maintenance cannot silently alter the app artifact.

Never trade data correctness, historical Journal recovery, Drive merge/tombstone safety, app signing continuity, Coach logic, or accepted UX for UI simplification or latency improvements. A completed CI run, a draft PR and a mockup are not phone acceptance.

## Recovery instruction

At the beginning of a future TrainPilot conversation, read this file, the completed Phase 5–7 roadmap and the permanent performance policy. Verify the current `main` source and its published APK independently. Preserve application ID, signing certificate and data compatibility. The 1.0.5 / 2687 publication was explicitly authorized before its final physical-phone test; do not mistake that exception for a completed device test. Future application changes default to signed APK → physical-phone approval → main merge → post-merge validation.
