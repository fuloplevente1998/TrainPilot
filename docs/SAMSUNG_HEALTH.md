# Direct Samsung Health provider (internal test build)

TrainPilot's Java phone app can read either Health Connect or Samsung Health Data
SDK 1.1.0. Choose the provider on the Health screen or in Health journal settings.
Health Connect remains the default; switching to Samsung explicitly opens its
read-permission sheet before persisting the preference.

## Data and units

The first integration reads steps, daily active/total energy, distance, heart rate,
exercise sessions and their calories, sleep stages, body composition and SpO₂.
Samsung exercise calories are matched to TrainPilot's active workout intervals;
daily energy and basal energy are never substituted for exercise calories.
Time-clipped exercise calories are marked as prorated. Missing readings remain
missing, while a real zero is preserved.

Body composition uses the SDK's own units: weight/fat mass/skeletal muscle mass
in kg, body fat in %, body water in liters, basal metabolic rate in kcal/day.
It does not reinterpret the SDK's muscle percentage as muscle mass in kg.

**No HRV/RMSSD/SDNN or inter-beat interval field is exposed by Data SDK 1.1.0.**
No HRV is inferred from pulse averages or Energy Score. Already shared Health
Connect RMSSD readings can remain available as a labelled fallback. Samsung's
Watch Sensor SDK/IBI is a separate future feature, not implemented here.

## Storage, permissions and fallback

Samsung readings live in the existing native SQLite journal under a separate
`samsung_health` channel. A projection selects one value per metric; it never sums
the same data through two providers. Samsung is preferred only when selected.
Authorized Health Connect readings can fill missing fields, with `metricProviders`
provenance. The Samsung path never silently requests Health Connect permissions.
Incomplete SDK reads retain previously read failed fields and are marked partial.

Reads run off the UI thread, require the foreground, have a 30-second per-request
timeout and bounded pagination with repeated-token detection. Restore and provider
changes invalidate in-flight journal writes. Local backup includes the full native
journal only with existing Health consent; the full journal remains excluded from
Drive snapshots. Existing backup preferences are not replaced by imported data.

The first sync reads 30 days; subsequent syncs recheck today/yesterday. This first
version is read-only and foreground-only. Health Connect background jobs remain
Health Connect jobs and retain their independent Android permission. No exercise
is written back to Samsung by this integration.

## SDK/build provenance and device acceptance

SDK AAR supplied by the repository owner: `android/app/libs/samsung-health-data-api-1.1.0.aar`.
SHA-256: `f5d3d83cf00b97d0bb1b1db4da076e861eb1c3e6e704d89a34e68909d2f38654`.
The accompanying open-source component notice is preserved; it is not a license
claim for the entire Samsung SDK. The DataViewer APK and SDK ZIP are not packaged.

The optional provider requires Android 10+ and Samsung Health 6.30.2+. Runtime
guards retain the phone application's existing API 24 minimum; Samsung's AAR
manifest minimum is overridden only for that guarded library. Compiled SDK Kotlin
runtime dependencies do not change the phone app's Java/Capacitor architecture.

Samsung requires partner approval/package-and-signature registration for public
distribution of an SDK-integrating app. Internal development testing is supported
according to Samsung's app-verification documentation. No partner approval has
been obtained or submitted by this implementation. Do not publish this feature as
a publicly validated Samsung integration before completing that process.

Automated tests use the real SDK builders with synthetic responses and test
storage, units, missing HRV, partial reads, paging, permissions and browser UI.
They do not prove that a real Samsung account exposes every listed metric. Samsung
service access must be accepted on a real supported phone, not an emulator.

Device checks: select Samsung, authorize reads, compare the two Samsung exercise
sessions (112 + 280 kcal) with TrainPilot; compare active versus total daily energy;
check a dated body-fat/muscle measurement; restart and verify the provider remains
selected; switch back to Health Connect. The Wear APK does not need an update for
this phone-only integration.

References:

- https://developer.samsung.com/health/data/overview.html
- https://developer.samsung.com/health/data/guide/hello-sdk/app-module.html
- https://developer.samsung.com/health/data/guide/app-verification.html
- https://developer.samsung.com/health/data/process.html
- https://developer.samsung.com/health/data/api-reference/index.html
- https://developer.samsung.com/health/sensor/faq.html
