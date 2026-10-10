# TrainPilot Wear OS 1.3.1 /2727 — Phone-started measurement (candidate)

Install with phone **1.3.1 /2727**.

The phone's explicit workout start can now remotely open the watch Activity. The watch briefly wakes its screen, waits for the matching active workout ID and revision, then starts the existing Health Services foreground recording if measurement is enabled and sensor permissions are granted. Once recording starts, the display can sleep normally. The initial screen hold is limited to 15 seconds.

Old cache entries and invalid handoffs cannot start a different workout. Disabled measurement, missing permissions, finished/error recordings and another application's exercise retain the existing protections. Mini Journal and measurement history keep their existing cached data and source labels.

Physical Watch7 acceptance remains required for the confirmed #157 case: phone start, watch untouched and display initially off, new recording notification, live measured values, continued recording with display off, final measurement attached to the correct phone Journal entry. Remote-open completion alone does not establish successful measurement. Keep the paired PR draft until user acceptance; do not automatically merge or publish.
