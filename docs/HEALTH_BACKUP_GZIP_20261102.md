# 2026-11-02 — Nagy Health-napló mentése és JSON/ZIP választás

## Hiba

A `HealthJournalStore.exportSnapshot()` a 18 MiB feletti teljes napló-exportot elutasította. Ez akkor is meghiúsította a JSON- és ZIP-mentést, ha a többi alkalmazásadat belefért a 20 MiB-os mentésfájl-korlátba. A korábbi JSON-funkció a kezelőfelületen „Mentés fájlba” néven szerepelt, ami a ZIP megjelenése óta nem egyértelmű.

## Változtatás

- Az új Android `HealthJournal.exportCompressedSnapshot` az **összes** natív Health-rekordot és napi adatot az eredeti `schemaVersion:1` JSON sémában gyűjti össze, és veszteségmentes GZIP + Base64 formában adja át. Nincs csendes dátum- vagy rekordlevágás.
- A mentés új opcionális mezője `healthJournalGzip`; a régi `healthJournal` mezőt a visszaállító továbbra is elfogadja. A Health-adatok explicit engedély nélkül egyik formátumban sem szerepelnek.
- A ZIP továbbra is a teljes mentés JSON-ját, valamint a fotókat és SHA-256 ellenőrzésüket tartalmazza. A JSON mentés önmagában nem tartalmazza a fotófájlokat.
- Az Android `prepareRestore` natív oldalon kibontja és előzetesen ellenőrzi a tömörített naplót, majd a meglévő SQLite-tranzakciós telepítés/visszagörgetés útját használja.
- A Beállítások képernyőn külön JSON-/ZIP-mentés és -visszatöltés gombok jelennek meg.

## Biztonsági korlátok

- A teljes kibontott natív Health-snapshot maximum **64 MiB** (UTF-8 bájtban). A beérkező Base64 adatmennyiség és a teljes JSON/ZIP-hez átadott JSON payload továbbra is maximum **20 MiB**. A fotókra és a ZIP-re érvényes korábbi limitek változatlanok.
- Ha a tömörített vagy a teljes mentés nem fér el, **hibával leáll**, nem hagy el rekordokat, és nem ad sikeres mentési visszajelzést. Az ennél nagyobb vagy rosszul tömöríthető adatkészletekhez külön streamelt/fájlalapú formátum szükséges; ezt ez a PR nem valósítja meg.
- A Health-visszaállítás felülírás előtt ellenőrzi az adatokat, és megőrzi a korábbi naplót a rollbackhez. Sérült GZIP, hibás Base64, túlméretes vagy érvénytelen JSON nem telepíthető.
- Az **új** `healthJournalGzip` mezőt a korábbi APK-k nem feltétlenül tudják visszaállítani. Régi kiadásokkal való helyreállításhoz a korábbi mentések használhatók; új mentést ne tölts vissza régi APK-val.

## Elfogadási lista

- [ ] A 18–64 MiB közötti, jól tömöríthető natív napló JSON-mentésből és ZIP-mentésből visszaállítható az összes rekorddal, összegzéssel és törlési jelöléssel.
- [ ] A korábbi `healthJournal` és Health nélküli JSON/ZIP mentések importálhatók.
- [ ] A mentési engedély kikapcsolása nem módosítja vagy törli a helyi Health-adatokat.
- [ ] A képernyőn egyértelműen külön JSON és ZIP mentés/visszatöltés látható.
- [ ] A sérült/túlméretes mentés nem változtatja meg az edzésnaplót vagy az egészségnaplót; sikertelen telepítés után rollback.
- [ ] Fizikai telefonon ellenőrzött APK: export, megszakítás, import, újraindítás, fotók, SHA-256, többféle adatforrás.

## Tesztelés

Java/Robolectric: `HealthJournalRestoreSizeTest` (>18 MiB, visszagörgetés, commit, hibás tömörítés). JS: `tests/publication-105.cjs` és `tests/browser/health-journal-120.cjs`, beleértve a felületi gombokat, az adatszűrést és a tömörített útvonalat. A készülékes ellenőrzés külön szükséges, mielőtt mainre kerül.
