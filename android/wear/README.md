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

The first phone → watch Data Layer path is implemented. When the phone has an active TrainPilot session, the Wear app caches and renders the current program, exercise and set. The next step is watch → phone workout commands.

See `docs/WEAR_OS.md` for the architecture and roadmap.
