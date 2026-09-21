# Build és ellenőrzés

A kanonikus webalkalmazás-forrás a `www/app.js`. Az Android csomag azonosítója: `com.repforge.app`.

## Gyors ellenőrzés

```bash
npm ci
npm test
```

A normál commit/push CI csak szintaxisellenőrzést és a gyors Node regressziós teszteket futtatja.

## Teljes Release Gate

A teljes kiadásellenőrzés kizárólag manuálisan indítható GitHub Actions workflow. A Release Gate futtatja:

1. a Node regressziós teszteket;
2. a teljes Chromium UI suite-ot;
3. a Capacitor Android syncet;
4. a Gradle release buildet;
5. az APK package/verzió/aláírás ellenőrzését;
6. az APK-ba csomagolt `www/app.js` bájtszintű összehasonlítását.

## Release signing

Signing kulcs vagy jelszó nem része a repositorynak. Release buildhez ezek szükségesek:

- `ANDROID_KEYSTORE_PATH`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

GitHub Actions alatt a keystore a `ANDROID_KEYSTORE_BASE64` Secretből ideiglenesen készül el a runner temp könyvtárában, és a workflow a futás végén törli.

Lokális release build példa:

```bash
export ANDROID_KEYSTORE_PATH=/safe/path/trainpilot-release.jks
export ANDROID_STORE_PASSWORD='...'
export ANDROID_KEY_ALIAS='...'
export ANDROID_KEY_PASSWORD='...'
npm run sync
cd android
./gradlew --no-daemon clean assembleRelease
```

A keystore és a secret értékek soha ne kerüljenek Gitbe.
