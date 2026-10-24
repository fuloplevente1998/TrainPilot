# Natív befejezés – meglévő blokkokból, összevont teszteléssel

Egyeztetés: 2026-10-24. Ág: `feat/native-android-migration`.
Felhasználói kérés: előbb mind az öt hátralévő terület kódja legyen kész,
utána összevont funkcionális/UI tesztelés. A korábbi Kotlin-blokkok az elsődleges
megvalósítási alapok; ne készüljön párhuzamos új implementáció létező helyett.

## Munkasorrend

1. Telefonos teljes edzésfolyamat: meglévő Compose/editor/finalizer bekötésének
   rendezése; emulátoros UI-tesztek megírása indításra, draftra, stopper/GPS-re,
   visszavonásra, mentésre/elvetésre, megszakításra.
2. Funkcióegyezés: Naptár/Coach tényleges útvonalak és felületek; induló GPS,
   intervallummód/preset, katalógus/nyelvek és vegyes útvonalszakaszok.
3. Napló/indulás: korlátozott adatlekérés, lapozás, globális szűrés;
   0/200/1500 edzéshez mérési és regressziós tesztkód.
4. Mentés/visszaállítás és migráció: meglévő BackupRestoreManager/ArchiveIO és
   LegacyWebViewMigrator teljes adatútja, referenciaadatok és visszaolvasás.
5. Telefon–óra/Health: meglevő kétirányú protokoll megőrzése és bekötése,
   offline/újracsatlakozás/engedélyek tesztkódja; végső azonos forrású kapu.

Az öt pont kódjának elkészülte után kódfagyasztás és GitHubon teljes build,
unit, telefonos Compose/emulátor, Wear UI, signed upgrade és APK-ellenőrzés.
Talált hibák javítása után az érintett ellenőrzések újrafuttatása. Tesztkód
elkészülte, sikeres fordítás, emulátor és fizikai készülékes elfogadás külön állapot.
A fizikai Watch7-próbát nem helyettesíti az emulátor.

Kódolás közben: célzott forrás/diff/szintaxis-ellenőrzés, kis visszakövethető
commitok. Köztes batch commitokon `[skip ci]`, így a GitHub nem indít minden
részletre teljes kaput; a kódfagyasztó commiton nincs ilyen jelölés, illetve a
meglévő natív workflow kézzel is indítható. Ezzel nem jelölünk késznek kaput,
nem merge-elünk, és nem változtatunk verziót. Saját adatok fölé telepítés STOP.

## Ellenőrzött források és különbségek

- `TrainPilot_Full_Conversation_Export.json`: 115 beszélgetési chunk.
- `NATIVE_IMPORT_MANIFEST.md`: 70 kiválasztott fájl importálva (történeti
  importállapot, nem a mostani módosított fájlok hash-igazolása).
- A félretett `migration-source/review/chunk-004.json` és `chunk-016.json`
  navigációs cél/útvonal deklaráció; nem teljes hiányzó képernyőimplementáció.
- Naptárhoz a Room entity/DAO, import/export, GoogleSync és naptárintent kód
  megvan (export 47/50/65/68). Ezeket kell újrahasználni. Külön teljes natív
  CalendarScreen/CoachScreen implementációt az átnézett manifest/blokkok nem
  azonosítanak; a jelenlegi Coach gomb trainingre vezet.
- `com.repforge.HealthConnectManager` régi prototípus: hardcoded 465 perc alvás,
  58 bpm és szimulált Samsung-pontszám. Nem köthető be valós Health-forrásként.
  A kanonikus `com.repforge.app` valós Health adapterei/projekciói az alapok.
- Új rész megírása előtt konkrét szimbólum/blokk és hívási út ellenőrzése;
  meglévő kód javítása/bekötése elsőbbséget élvez. Elfogadott main viselkedés
  ellenőrzése továbbra szükséges: a generált blokk önmagában nem funkcióegyezés.

## Állapot – kódolási batch, 2026-10-24

- `e05ff57`: Naptár repository a meglévő Room-entitás/DAO és Google-eseményazonosító
  felhasználásával. Létrehozás/áthelyezés, stale-CAS, kihagyás/visszaállítás és
  lemondási rekord; aktív/teljesített/historyhoz kötött alkalom védett. Naptári
  indítás a natív sessionbe tartós scheduleId-t tesz, közvetlenül publikálás előtt
  tranzakcióban újraellenőrzi az alkalmat. 11 új NativeCalendarTest tesztkód.
- `3921c10`, `d134785`: Naptár útvonal és felső gyorsgomb, havi/dátum szerinti
  felület, naplózott edzések, szerkesztő és törlési megerősítés; HU/EN/DE/RO
  feliratszótár. Havi bounded Room-projekció, részletes sorozatok/fotók nélkül.
- `d134785`: Coach tiszta számítási mag az elfogadott main végső rf245/tp184
  szabályaiból (külön Samsung/TrainPilot, 36h frissesség, 14 nap/3 edzés,
  easy +3/light +1/good 0/challenging -4/hard -8/pain -20; nincs kitalált alapscore).
  11 új NativeCoachReadinessTest tesztkód. A valós adapter/adatbetöltés és a
  Coach-képernyő/nav **még nincs bekötve**; a prototípus simulatív Health-managere
  továbbra sem használható valós forrásként.
- Összesen 22 új regressziós próba megírva, **még nem futtatva**. Most csak
  forrás/hívási út és diff-ellenőrzés történt; nincs új Kotlin/Room/Compose
  fordítási igazolás. A `[skip ci]` a felhasználó által kért batch munkamód része.
- Utolsó teljes zöld alkalmazásforrás változatlanul `dd4cc27`, bizonylat az átadóban.

Következő: Coach valós provider- és naplóadatút, célprogramnap, felület/navigáció
bekötése a main policy alapján. A célpont kiválasztásánál a main tényleges
rf233/rf235 felülírásait is ellenőrizni kell, nem csak a korábbi definíciót.
Ezután további 2. pontok, bounded napló, backup/restore, Health/szinkron és a
telefonos Compose UI-tesztkészlet/összevont kapu kódja. Az öt pont nincs kész.
Későbbi integrációs vizsgálat: szerkesztés vs. indulás verseny, naptár állapotváltás
mentéskor, felhőből lemondás visszaszinkronizálása, időzónák/DST, kis kijelző/nagy
betű, programnevek lokalizációja és navigációs visszalépés. Naptári Google-szinkron
régi takeLast(100) korlátját az 5. terület részeként külön rendezni kell.

## Újabb batch-kiegészítés és vizuális review

`9b09af7`/`8fb3ff6`: Coach helyi valódi adatút/UI bekötve, 6 signals- és 4
menüteszt megírva. Aktuális nyitott részletek és 32 új, még nem futtatott próba
az átadó tetején. A mockup áttekintése után a felhasználó az öt alsó füles
változatot választotta, jobb felső globális Beállítások fogaskerékkel.
Ez felülírja a korábbi nyolcgombos irányt; az új UI bekötése még hátra van.
A Beállítások megnyitása ne szüneteltesse a mérést; védett lezárási műveletek
zárolása megmarad. Részletek: `docs/design/NATIVE_MOCKUP_REVIEW.md`.
Coach további per-gyakorlat tanács/progresszió és a választott dizájn megvalósítása,
valamint az öt pont többi része továbbra nincs lezárva.


## Ötfüles navigáció és Beállítások – első bekötés

`4f60bc0` `[skip ci]`: a választott öt fül, jobb felső globális fogaskerék,
ugyanazon editor fölé nyíló, mérést nem szüneteltető Beállítások; tényleges
edzéselhagyásnál meglévő pause. Programok/Naptár/Coach belépési pontok a
Kezdőlapon és az Edzés oldalon; mért helyű aktív jelzősáv. Késői save-pending
mellett editor retry elérés megmarad; aktív edzés alatt UI restore/erase tiltás
és SAF callback újraellenőrzés. Ez utóbbi nem helyettesíti a teljes backend
restore-versenyvédelmet, amely a 4–5. pont része marad.
10 menü/policy és 5 új settings-védelmi tesztkód; a batch összesen **43 megírt,
még nem futtatott próba**. Nincs új build/UI/teszteredmény. A 24 mockup teljes
vizuális megvalósítása, integrált Phone UI-kapu és az öt terület maradéka hátra van.


## Hat rögzített Compose-elfogadási feltétel

A `docs/design/NATIVE_MOCKUP_REVIEW.md` „Kötelező Compose-bekötési feltételek”
része a további UI-kód és az 1–2./5. terület próbáinak része: draft/aktív
folytatás, natív pihenő skip/±15 mp, gyakorlatváltás megmaradó pihenővel,
pause/kilépés vs. discard/finish, külön Samsung/saját/Coach score, valódi friss
Watch/BLE pulzus vagy hiányzóadat-állapot. Most csak dokumentálva, nem megvalósítva
vagy újra bevizsgálva; a forrás és a 43 megírt tesztkód állapota változatlan.
