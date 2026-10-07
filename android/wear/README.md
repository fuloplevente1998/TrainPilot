# TrainPilot Wear OS module

Native Wear OS companion target for TrainPilot.

The phone application is intentionally not migrated to Kotlin. The existing Capacitor/WebView application remains in `android/app`; this module is an additional Wear-specific frontend.

Initial stack:

- Kotlin;
- Compose for Wear OS;
- Wear Material 3;
- Wear OS 3+ (`minSdk 30`).

Build from `android/`:

```bash
./gradlew :wear:assembleDebug
```

The initial screen is static on purpose. Phone/watch Data Layer synchronization is the next implementation step.

See `docs/WEAR_OS.md` for the architecture and roadmap.
