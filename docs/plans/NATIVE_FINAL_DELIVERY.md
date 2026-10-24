# Natív véglegesítés – elfogadott feladat és átadási szerződés

Felhasználó által jóváhagyva: 2026-10-24. Ez az aktuális végrehajtási terv;
a részletes batch és a dizájn hat működési feltétele vele együtt kötelező.

## Keretek

- Csak `feat/native-android-migration`, `/workspace/TrainPilot-native-migration`.
- Main referencia `7473b30` / `5d27aa9`, nem merge-elhető/nem módosítható.
- Verzió változatlan: `1.3.71-native-test` /2736, `com.repforge.app` mindkét eszközön.
- Meglévő `com.repforge.app` Kotlin/motor/adatút újrahasználata; a szimulált
  `com.repforge.HealthConnectManager` nem termékforrás.
- A meglévő Coach/Calendar, ötfüles navigáció és globális Settings bekötését
  befejezni/ellenőrizni kell, nem új párhuzamos implementációt írni helyettük.
- Az importált fájl/motorkód megléte nem teljes funkció vagy bevizsgálási bizonyíték.

## Teljes funkcionális befejezés – öt batch-terület

1. Telefonos edzésút: programnapi és quick/outdoor start, aktivitásonként megjegyzett
   explicit kezdeti GPS-választás; natív/importált draft; alsó stopper/kétoldali/GPS/
   kézi idő-táv OK; undo; pihenő skip/±15mp; gyakorlatváltás; közös finalizer,
   mentés/retry/discard. Globális és edzés-intervallumpreset, nem-autoAdvance mód,
   két pozitív oldal feltétele; vegyes Watch→Phone útvonalszakaszok megjelenítése.
2. Naptár/Coach: valódi Samsung/Health Connect adapter és Room-adat, main rf233/
   rf235 célprogramnap és readiness-policy, önálló működő útvonalak; teljes
   katalógus és HU/EN/DE/RO lokalizáció minden termékoldalon/dialóguson.
3. Napló/startup: bounded Room-projekció, lapozás, teljes adathalmazra működő
   keresés/szűrés; 0/200/1500 edzés mérése. Wear mini-napló csak szükséges sorokat kér.
4. Backup/restore/migráció: meglévő BackupRestoreManager/ArchiveIO/LegacyWebViewMigrator,
   JSON/ZIP/fotó/Google Drive app-data adatút, teljes roundtrip, sérült/részleges
   forrás/retry és restore/start/remote-start konkurenciavédelem, adatmegőrzés.
5. Phone↔Wear/Health: ownership/handoff, offline folytatás, reconnect, tartós nyugták,
   törlés utáni visszatérés tiltása, valós engedély/háttérállapotok. Naptári
   Google-szinkron `takeLast(100)` megszüntetése adatvesztés nélkül.

## Aktuális vizuális átadó

A felhasználó 2026-10-24-én külön kérte a négy mockup újratervezését.
Az aktuális képek és konkrét képernyőleírások:
[Native design handoff](../design/NATIVE_DESIGN_HANDOFF.md). A további végső
átadás továbbra valódi emulátoros képeket igényel; a mockup nem tesztbizonyíték.

## Funkciómegőrzés és további részletnézetek

A 24 látványterv minimum áttekintés, nem felső korlát. Teljes egészségnapló,
korábbi napok/mérések, testsúlyrögzítés és grafikonok, pulzusrészletek és a main
minden további funkciója megőrzendő. Szükség szerint további aloldalak/dialógusok
készülnek; hiányzó mockup nem törlési engedély. A main→natív lefedettségi mátrix
külön ellenőrzési feltétel.

## Teljes Compose felület – 24 nézet

A négy chatben átadott képtábla az elfogadott vizuális referencia; átadáskor
külön csatolandó. A motor feltételei: `../design/NATIVE_MOCKUP_REVIEW.md`.
Öt fül: Kezdőlap / Edzés / Napló / Fejlődés / Egészség; globális jobb felső
Settings fogaskerék, aktív mérés szüneteltetése nélkül.

- 1/4: Kezdőlap dinamikus folytatással, mai programmal, gyors edzéssel, órastátusszal,
  2×2 health/regeneráció csempével; Edzés választó A/B napokkal és Programok/Naptár/
  Coach belépéssel; Programok; havi/napi Naptár; kategóriás Gyakorlatkönyvtár; részletek.
- 2/4: súlyzós Kg/Ism/RIR + undo; egyetlen alsó stopper; pihenőkártya skip/±15mp;
  időalapú és kétoldali mérés külön oldalállapottal; kültéri GPS táv/idő/tempó/
  valós pulzus, mini térkép, explicit stop, összecsukott kézi adatok; intervallumok;
  befejezés összegzéssel, mentésre vár állapottal, hat effort-válasszal, megjegyzéssel,
  pause/kilépés („vázlat”), valódi mentés és külön discard.
- 3/4: Napló hónap/keresés/PR/mini route; részletek Gyakorlatok/Statisztikák/Forrás
  fülekkel és fotó/route eléréssel; privát fullscreen térkép piros X-szel (csak térkép
  bezárása); Fejlődés 1RM/volumen/PR; Egészség valódi forrásokkal; Coach kör,
  külön Samsung és saját score, kulcstényezők és javasolt edzésindítás.
- 4/4: Beállítások profil/megjelenés/nyelv/időzítő/eszköz/backup/adatvédelem;
  Óra/szinkron átadás és offline státusszal; backup/restore előnézet;
  Engedélyek; privát Előtte/Utána fotók; Program szerkesztése.

## Témarendszer megőrzése

A main teljes Basic/Vivid katalógusa, stabil kulcsai és effektív színei az alapok,
a végső felülírások figyelembevételével. Kódban már26 main és6 kompatibilitási
natív paletta szerepel; ezek teljes UI-ellenőrzése még szükséges.
A korábban natívban elmentett kulcsok se vesszenek el. Migráció/backup/restore
őrizze meg a választott témát; arany mockup miatt nincs automatikus átszínezés.
Minden komponens, grafikon, kijelölés és kontraszt tématokenből dolgozzon.
Hex érték a központi paletta/token definícióban megengedett; termékoldalon nincs
szétszórt fix akcentusszín. Szemantikus danger/success token és piros térkép-X marad.

## Kódolás → közös kapu → tényleges képek

- Előbb a teljes kód és unit/integrált telefonos Compose UI tesztkód; kis commitok
  `[skip ci]`. Nincs részletenként teljes CI/Gradle/emulátor helyben.
- A meglévő `native-android.yml` bővítendő valódi telefonos Compose UI-kapuval
  és screenshot-artifacttal; ez jelenleg nem kész kapu. Nincs párhuzamos APK-workflow.
- Kódfagyasztás után skip nélküli commit/meglévő workflow futtatása: debug és eredeti
  kulcsos R8 release Phone/Wear, DEX/csomag/verzió/signer/hash ellenőrzés, összes
  unit, integrált Phone UI, meglévő 183 Wear UI-eset megőrizve, új esetekkel bővíthető;
  mindkét eredeti/gyorsított signed `adb install -r` upgrade (2/2).
- A régi teszteset-azonosítók megőrzendők, nem csak a darabszám. Riportokban nincs
  elrejtett hiba/kihagyás vagy tesztgyengítés. Hiba után javítás és érintett kapu
  újrafuttatás; végső bizonyíték egy rögzített alkalmazásforráshoz tartozzon.
- Valódi, futó natív appot készíts elő az emulátoron; módosítatlan PNG-k
  `adb exec-out screencap -p`-vel, főoldalakról, edzéstípusokról, térképről,
  Naptár/Coach/Settings nézetekről, több témával és nyelvvel. Az újratervezett mockupok külön felhasználói kérésre készültek;
  a végső screenshot-átadás valódi alkalmazásképeket jelent.
  A manifest rögzítse commit/APK-hash/eszköz/API/nyelv/téma/adatfixture azonosítóját.
- Tesztfixture és injektált GPS megengedett, a jelentésben megjelölve; valódi
  képernyőkép nem igazolja a fizikai Galaxy Watch7 szenzorát vagy élő Samsung SDK-t.
  Fizikai Phone/Watch7 és kétkészülékes offline próba külön elfogadási állapot.
- Átadás: azonos forrású zöld CI link, nyers tesztriport, képek, aláírt Phone/Wear
  APK artifactok és ellenőrzési bizonylat. Élő adatos frissítést csak a teljes
  adatmegőrzési és mentés-visszaállítási kapuk igazolása után javasolj; main merge
  és verzióemelés továbbra külön jóváhagyás.

## Követés

Aktuális részállapot és következő konkrét feladat: `../WORK_HANDOFF.md`.
Az összes terület kész státuszához implementáció + teszt + UI-bizonylat kell;
a még szükséges fizikai próbát kifejezetten külön jelöld.
