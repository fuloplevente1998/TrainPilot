# TrainPilot – Google Play publikálási ellenőrzőlista (2026-09)

## Technikai

- [x] package: `com.repforge.app`
- [x] targetSdk/compileSdk: API 36
- [x] repositoryból eltávolított régi debug signing kulcs és hardcoded signing credential
- [x] release build környezeti változókból / GitHub Secretsből kapja a signing adatokat
- [ ] saját release keystore létrehozása és biztonságos offline mentése
- [ ] GitHub Secrets beállítása: `ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`
- [ ] Play App Signing beállítása

## Fotók

- [x] rendszer picker / kamera intent, broad médiatár-jogosultság nélkül
- [x] privát app-tárhely
- [x] EXIF/GPS eltávolítás az optimalizált másolatból
- [x] opcionális Drive appDataFolder mentés
- [ ] fizikai telefonos kamera/picker/Drive teszt

## Google OAuth

- [ ] Google Auth Platform Branding
- [ ] publikus alkalmazás-homepage
- [ ] publikus privacy policy
- [ ] Production audience / Publishing status
- [ ] tényleges scope-ok deklarálása
- [ ] release signing SHA-1/SHA-256 regisztrálása

## Health Connect / Play policy

- [ ] Health apps declaration
- [ ] csak ténylegesen használt Health Connect jogosultságok
- [ ] Data Safety űrlap
- [ ] jogosultságok és funkciók dokumentálása

## Store listing és tesztelés

- [ ] appnév, rövid és hosszú leírás
- [ ] ikon, feature graphic, képernyőképek
- [ ] támogatási email és privacy URL
- [ ] tartalombesorolás
- [ ] célközönség / ads deklaráció
