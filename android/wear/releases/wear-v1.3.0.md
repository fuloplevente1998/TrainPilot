# TrainPilot Wear OS 1.3.0 /2726 — Measurement history (candidate)

Compatible with phone **1.3.0 /2726** on draft PR #156.

- **Órás mérések** shows the three most recent synchronized saved workouts with a TrainPilot Wear measurement: date, workout name, duration, exercise names, measured total kcal and average pulse when available. Zero remains zero, missing values stay absent, and partial recordings are labeled.
- Tapping an entry opens its existing Mini Journal detail with completed sets and separately labeled Wear / Health Connect / Samsung Health values. Both the on-screen back button and system back return to the originating measurements view, including the workout's measurement page.
- **Mini napló megnyitása** opens the existing list of up to eight saved phone workouts; **Előzmények frissítése** requests the existing read-only phone projection. Opening measurements also refreshes history. Cached entries remain readable offline.
- No new workout store or invented health values. The direct Wear session summary remains distinct from the selected health provider's interval report.

User reports that the previous paired build transferred measurements correctly for **20 squats**. Broader health-data and Journal acceptance remains in progress; this single result does not close all acceptance items. The earlier **Wear 4 kcal vs phone 1 active / 2 total kcal** report must not be forgotten: exact external session/window/source coverage still needs paired-device verification. Phone-only training uses the selected health provider; absent watch readings cannot be reconstructed.

Validation: Wear JVM coverage for newest-first source filtering, saved-Journal preservation, duplicate entries, zero/missing values and partial readings; signed Wear release gate and signer continuity. Physical Watch7 acceptance still required for scrolling, enlarged fonts, entry/back navigation, reconnection and saved health values. Keep PR draft.
