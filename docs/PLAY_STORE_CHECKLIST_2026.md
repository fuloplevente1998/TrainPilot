# TrainPilot – Google Play publikálási ellenőrzőlista (2026-09)

## Technikai

- [x] package: `com.repforge.app` – kompatibilitási okból megtartva
- [x] targetSdk/compileSdk: API 36
- [x] signing kulcs nincs a publikus repositoryban
- [x] release workflow GitHub Secretsből kapja a signing adatokat
- [x] Repository Secrets beállítva: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`
- [x] meglévő telepítések frissítéséhez szükséges kompatibilis signer helyreállítva és Secretként konfigurálva
- [ ] végleges Google Play signing / Play App Signing stratégia rögzítése
- [ ] production release/upload kulcs külön titkosított, lehetőleg offline mentésének és APK-signer egyezésének végső ellenőrzése

> Megjegyzés: a jelenlegi kompatibilitási signer a korábbi telepítések frissíthetőségének megőrzésére szolgál. A Play Store publikálás előtt a Play App Signing beállítását és az esetleges kulcsmigrációt külön kell megtervezni.

## Build és release

- [x] gyors regressziós CI
- [x] teljes Chromium UI Release Gate
- [x] APK + source ZIP + SHA256 artifact-előkészítés a workflow-ban
- [x] `v*` taghez GitHub Release publikálási workflow
- [x] verzió/tag konzisztencia-ellenőrzés
- [x] 1.7.7 / 2670 publikált, aláírt APK és source ZIP sikeres Release Gate után; elfogadott `main` alap
- [x] `v1.7.7` tag és GitHub Release publikálva
- [ ] helyreállított `.jks` aláíró tanúsítvány SHA-256 egyezésének összevetése a kiadott 1.7.7 APK-val
- [ ] 1.0.0 / legalább 2671-es `versionCode` build és teljes tesztkapu
- [ ] 1.0.0 telefonos, korábbi 1.7.7-re telepített, adatmegőrző frissítési próba
- [ ] 1.0.0 végleges telefonos jóváhagyás után merge és kiadás

## Fotók

- [x] rendszer képválasztó / kamera útvonal broad médiatár-jogosultság nélkül
- [x] privát app-tárhely
- [x] újramentett/optimalizált JPEG nem viszi tovább az eredeti EXIF/GPS metaadatot
- [x] opcionális Drive `appDataFolder` fotómentés
- [ ] fizikai telefonos kamera/képválasztó teszt
- [ ] fizikai telefonos Drive fotó fel-/letöltés/törlés teszt

## Google OAuth / Drive / Naptár

- [x] Google Drive és Google Calendar integráció forrása jelen van
- [x] minimális Drive `appDataFolder` scope használata
- [x] alkalmazás által létrehozott naptár/esemény scope-ok használata
- [ ] Google Auth Platform Branding véglegesítése
- [ ] publikus alkalmazás-homepage
- [ ] publikus privacy policy
- [ ] Production audience / Publishing status
- [ ] tényleges production scope-ok jóváhagyása/deklarálása
- [ ] release signing SHA-1/SHA-256 regisztrálása
- [ ] fizikai készülékes Google-fiók / Drive / Naptár teszt

## Health Connect / Play policy

- [x] natív Health Connect integráció jelen van
- [x] Health Connect privacy/activity belépési pont jelen van
- [ ] Health apps declaration
- [ ] csak a tényleges kiadási funkciókhoz szükséges Health Connect jogosultságok végső auditja
- [ ] Data Safety űrlap
- [ ] jogosultságok és funkciók publikus dokumentálása
- [ ] fizikai készülékes Health Connect jogosultság/read/write teszt

## Store listing

- [ ] végleges appnév és rövid/hosszú leírás
- [ ] ikon és feature graphic
- [ ] képernyőképek
- [ ] támogatási email
- [ ] privacy URL
- [ ] tartalombesorolás
- [ ] célközönség deklaráció
- [ ] ads deklaráció

## Publikus repository

- [x] README a tényleges funkciókhoz igazítva
- [x] clean history határ dokumentálva
- [x] signing/secrets szabály dokumentálva
- [ ] repository rövid GitHub Description beállítása
- [ ] Topics beállítása
- [ ] licencelési döntés és szükség esetén `LICENSE` fájl
