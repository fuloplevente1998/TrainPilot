# TrainPilot

TrainPilot egy Android edzéstervező és edzésnapló alkalmazás Health Connect, Google Drive/Naptár és opcionális edzésfotó támogatással.

## Clean source history

A publikálásra szánt tiszta repository története a **TrainPilot 1.6.0** állapottól indul. A korábbi fejlesztési/legacy RepForge history nincs átemelve ebbe a repositoryba.

Aktuális clean base:

- verzió: **1.6.0**
- Android `versionCode`: **2649**
- package: `com.repforge.app`
- kanonikus appforrás: `www/app.js`

## Fejlesztési modell

- `main`: stabil clean base / kiadásra jelölt állapot.
- feature/test branchek: célzott módosítások.
- normál commit/push: szintaxis + gyors `npm test`.
- teljes Chromium + Android APK build: csak manuális **Release Gate**.

## Build

```bash
npm ci
npm test
npm run test:ui
npm run sync
```

A részletes build- és signing-leírás: `docs/BUILD.md`.

## Signing és secrets

Release signing kulcs nincs a repositoryban. GitHub Actions kizárólag az alábbi Secret neveket használja:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

A repositoryban signing kulcsot, jelszót, API credentialt vagy `.env` fájlt commitolni tilos.
