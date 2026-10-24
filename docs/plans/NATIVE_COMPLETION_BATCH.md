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

## Állapot

A batch munkamód és forrásellenőrzés rögzítve. Az öt pont NINCS késznek jelölve.
Az új telefonos UI tesztkészlet még nincs implementálva. Ebben az egyeztető
körben alkalmazáskód nem változott, új build/teszt nem futott.
Utolsó teljes zöld alkalmazásforrás: `dd4cc27`, bizonylata az átadóban.
