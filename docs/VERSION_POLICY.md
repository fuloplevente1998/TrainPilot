# Verzió- és kiadási szabály

Aktuális metaadat: **1.3.71 /2735** mindkét Android-modulban. Verzióemelés külön felhasználói döntés; dokumentációs karbantartás önmagában nem emel verziót.

## Együtt módosítandó források

- `package.json`, `package-lock.json` gyökér és üres csomagbejegyzés.
- `SOURCE_VERSION.json` és `android/wear/SOURCE_VERSION.json`: version, displayVersion, versionCode.
- `android/app/build.gradle`, `android/wear/build.gradle`: versionName, versionCode.
- `www/app.js`: TRAINPILOT_VERSION.

A `scripts/check-version-policy.cjs` és a Quick CI ellenőrzi az egyezést. Új verzióhoz növekvő Android-kód kell; részleges emelés és visszalépés hibát okoz. Azonos metaadatokkal készülhet fejlesztési/tesztbuild, ezért az APK forráscommitja és SHA-256 értéke mindig szükséges azonosító. A tesztbuild nem válik automatikusan új publikus kiadássá.

## Új kiadások

A jövőbeli kiadás alapja `MAJOR.MINOR.PATCH`: nagyobb kompatibilitástörés, új funkció, illetve javítás szerint, külön egyeztetett számmal. A történeti 1.3.51 nem írható át; az azt követő 1.3.6 számsorrendi kivételét a [jegyzék](releases/INDEX.md) mutatja. Következő szám választásakor ezt és a Play/Android kódsorrendet külön ellenőrizni kell.

- Telefon: `vX.Y.Z`; Wear: `wear-vX.Y.Z`; jelölt: `candidate-X.Y.Z-CODE`.
- A 2026-10-22-i kifejezett felhasználói döntés szerinti újrakiadás változatlan **1.3.71 /2735**: külön `v1.3.71-perf-20261022` / `wear-v1.3.71-perf-20261022` kiadási címkével. Ezek buildazonosítók, nem az APK verziónevei. A korábbi címkék és mellékletek megmaradnak.
- Új tag csak az elfogadott pontos forrásra kerülhet. Meglévő taget nem mozgatunk/átnevezünk, régi APK-t nem címkézünk új forrásúnak.
- Quick, Performance, telefonos Chromium/natív kapu és ahol érintett Wear-kapu, majd a megfelelő készülékes elfogadás szükséges.
- Kiadási melléklet: aláírt APK, forrás/metaadat, checksum és signer-folytonosság. A Play-előkészítés ettől külön feladat.
- A main új commitja nem írhatja át automatikusan egy korábbi kiadás leírását. A publikált APK és az elfogadott main külön állapot.

## Leírások karbantartása

A `Release Metadata Maintenance` workflow az ellenőrzött jegyzék meglévő release-ei elé egységes státuszfejlécet ad. Az eredeti szöveg byteazonosan megmarad. Teljes írás előtti ellenőrzés szükséges: release-azonosító/cím/státusz, eredeti szöveg hash, tagcommit és melléklet-metaadatok. Eltérés esetén leáll; a már frissített fejléc ismételhető.

A workflow nem hoz létre/töröl release-t, nem módosít címet, taget, latest/prerelease jelölést, verziót vagy bináris mellékletet. A #214 külön engedélyezett karbantartási ágára történő push végzi az első rendezést; később kézzel indítható. Új release felvétele a jegyzékbe felülvizsgált módosítás legyen. Az eredeti feljegyzésekben levő nyitott feladatok történeti állítások.

Az `UNLICENSED` csomagmetaadat a proprietary állapotot jelzi; nem új felhasználási szerződés vagy nyílt forrású licenc.
