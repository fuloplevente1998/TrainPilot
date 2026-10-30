# TrainPilot Wear OS 1.2.17 /2724 — Mini Journal refresh (candidate)

Paired with phone **1.2.15 /2722** on draft PR #156. Includes previous Quick Workout and Mini Journal work.

- Opening **Mini napló** asks the phone for a fresh snapshot; **Frissítés** retries explicitly through the durable Data Layer command outbox.
- **Telefon megnyitása** opens the paired phone app. The view identifies the sending phone version and distinguishes a legacy payload without Journal support from an empty saved Journal.
- Older phone DataItems cannot overwrite a newer cached home/Journal snapshot. Cached history remains readable offline.
- The saved workout, sets and separate Wear/Health Connect readings remain read-only.

Physical Watch7/phone acceptance is required: open phone after watch update, refresh while viewing Mini napló, reconnect after temporary disconnection, and finish ten pushups. Confirm exactly one phone entry and the same Wear kcal in both views. Separate Health Connect active/total energy stays separately labeled. Keep the PR draft until acceptance.
