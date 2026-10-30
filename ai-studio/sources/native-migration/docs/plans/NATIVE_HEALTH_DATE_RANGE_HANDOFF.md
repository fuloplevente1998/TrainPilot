# Külön ChatGPT-chat – többnapos egészségrekord-szűrés

2026-10-25. A felhasználónak átadott prompt és rögzített raw források.
Visszaérkezett: draft #243 exact1bf745e, integrálva1864a8c/c611cc7.
Review: ../reviews/NATIVE_HEALTH_DATE_RANGE_INTEGRATION.md. Nincs futtatott kapu
vagy GitHub-merge. A lenti prompt történeti átadó, nem új kiosztási kérés.

## Másolható feladat

TrainPilot: készítsd el a natív egészségrekord-böngésző többnapos dátumszűrését.

Bázis: `feat/native-android-migration`, **9a18ca60a04a17715298993d432c80c63f587cc3**.
Saját új ág: **test/native-health-date-range**. A main és a migrációs ág írása/
merge tiltott. Ez a konkrét elkülönített feladat kivétel az AGENTS fő munkásának
általános ágkorlátozása alól. Olvasd el az AGENTS.md-t, docs/NATIVE_RESUME.md-t
és az alábbi forrásokat; a dokumentált régi CI nem az új változtatás igazolása.

A HealthRecordQuery jelenleg egyetlen napot tartalmaz, a NativeHealthRecords.load
ugyanazt a napot adja a meglévő HealthJournalStore.page(from,to,...) mindkét
határának. Bővítsd a meglévő adatútját tetszőleges kezdő- és zárónappal.

- Az egynapos hívások és meglévő konstruktorhívások maradjanak kompatibilisek.
- A HealthRecordBrowser felületén legyen valóban használható kezdő- és zárónap,
  lokalizált HU/EN/DE/RO feliratokkal. Hibás dátum/fordított tartomány ne fusson.
- A dátumhatárok a HealthJournalStore.page tényleges szerződését kövessék.
- Maradjon a csatorna-/forrás-/típusszűrés és a25-ös lapméret.
- Tartomány vagy bármely szűrő váltásakor cursor/generation/lapozási előzmény
  reset; későn visszaérő régi válasz nem írhatja felül az új nézetet.
- Restore/generation-védelem, meglévő rekord- és alvásrészletek megmaradnak.
- Továbbra lapozás; nincs összes rekord egyidejű betöltése, új provider/journal/DB.

Csak két meglévő forrásfájl módosulhat:

- `android/app/src/main/java/com/repforge/app/ui/health/NativeHealthRecords.kt`
- `android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt`

Új fájlként engedélyezett külön `native_health_date_range.xml` a négy
`values{,-en,-de,-ro}` könyvtárban, új célzott JVM-teszt az app/src/test alatt,
és szükség esetén új külön Compose UI-teszt az app/src/nativePhoneUiTest alatt,
mindkettő `java/com/repforge/app` csomagban. Ne írd a nagy strings.xml-eket vagy
a fő munkás NativePhoneNavigationTest fájlját.

Más meglévő fájl, HealthJournalStore/provider, HealthScreen/VM, DAO, séma,
szinkron, naplószerkesztés, verzió vagy workflow módosítása tilos.

Tesztkód: egynapos kompatibilitás; többnapos és mindkét határ; hibás/fordított
range; több lap ismétlés/hiány nélkül; dátum+csatorna/forrás/típus együtt;
dátumváltás lapozás után; generation/restore; UI-próbában valódi dátumvezérlők.

Kis `[skip ci]` commitok, helyi Gradle/APK/emulátor vagy teljes CI indítása nélkül.
A közös kaput a fő munkás futtatja. Push a saját ágra, draft PR célja a migrációs
ág. Nincs merge, issue-lezárás, új munka kiosztása vagy fő handoff átírása.
GitHub-hozzáférés hiányában teljes fájlok és alkalmazható unified diff.
Átadás: exact SHA/PR/fájlok, kompatibilitás/korlátok, megírt és ténylegesen
futtatott ellenőrzések külön. Hiányzó forrást kérj, ne találj ki implementációt.

## Raw források

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/AGENTS.md)
- [NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/docs/NATIVE_RESUME.md)
- [NativeHealthRecords.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/android/app/src/main/java/com/repforge/app/ui/health/NativeHealthRecords.kt)
- [HealthRecordBrowser.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt)
- [HealthJournalStore.kt – csak referencia](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt)

Az útvonalak a megadott Git-objektumban ellenőrizve; a raw HTTP-hozzáférés
az adott másik chat képességétől függ. Ha nincs, ugyanezek a fájlok feltölthetők.
