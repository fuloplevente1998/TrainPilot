# TRAINPILOT – TELJES NATÍV ÚJRAÉPÍTÉSI MESTERSPECIFIKÁCIÓ
## Google AI Studio számára – v1, 2026-10-30

### 0. Feladat és eredmény
Építs új, ténylegesen **natív Android alkalmazást Kotlin + Jetpack Compose** alapon, TrainPilot néven. A meglévő Android/Wear edzéslogika és adatstruktúrák lehető legnagyobb arányú újrafelhasználása szükséges. A régi, éles **WebView 1.3.71/2735** telefonapp maradjon működő és visszaállítható. Ne csomagolj HTML/JS oldalt WebView-ba natív appnak nevezve. Első cél Android-telefon, de Wear OS kompatibilitás és az egyetlen edzés/timer-tulajdonos kötelező.

**Fontos:** a jelenlegi main a WebView-változat; a PR279 nincs merge-elve. A források a referenciafolderben csak olvasható másolatok. Nem módosíthatod automatikusan a main runtime-ot. Nincs automatikus merge, aláíráscsere, adatfelülírás vagy Play-közzététel.

### 1. Forrásprioritás és állapot
- **A – Stabil, publikált főprogram:** repository root main, docs/FEATURES.hu.md, README, android/app, android/wear, android/shared, www. Verzió 1.3.71 / versionCode 2735, csomag: com.repforge.app. A kiadásba bekerült képességeket funkcionális minimumként kezeld.
- **B – PR279 újdonság:** ai-studio/sources/pr279-webview/www és browser-tesztek; a főmenü megőrzött 2×4. Forrás-commit 5ff8cbd0da6533d04e0696229cb013936ae39619. Kód + automatizált CI igazolása nem azonos teljes készülékes elfogadással.
- **C – PR277 arculat:** ai-studio/design/pr277/IMPLEMENTATION_PLAN.md és references/01–08 PNG. Ez a kívánt kiinduló dizájn; a felhasználó korábban 7 funkcionális képernyőrészletet adott, ebből nyolc referenciakép készült.
- **D – Natív munkaág:** ai-studio/sources/native-migration/android és kapcsolódó docs/migration/tests/Gradle. Sok kód és emulátoros teszt kész, de a branch nem azonos a publikált mainnel; PR238 archivált, a natív végső UI és valós adatú migráció nincs véglegesen elfogadva.
- **E – Kiegészítő audit:** ai-studio/audits/NATIVE_FINAL_FEATURE_INVENTORY.md/.json, összesen 107 auditpont (81 funkcionális + 26 egészségmetrika), pontos korábbi source/runtimestátusszal. A korábbi auditot új kód mellett újra ellenőrizni kell.
- **F – Archivált kontextus:** #238, #240–#269, #270–#276, #278 és külön PR271, PR262; csak célzottan olvasd, ne tekints minden archivált vagy lezárt PR-t mainre merge-eltnek.

**Állapotcímkék:** STABLE_MAIN, PR279_AUTOMATED, NATIVE_IMPLEMENTED_UNVERIFIED, PARTIAL, DESIGN_ONLY, DEVICE_PENDING, NOT_IMPLEMENTED. Részfeladatonként őrizd meg a címkéket. Pusztán UI-screenshot alapján nem lehet STABLE_MAIN vagy VERIFIED_DEVICE.

### 2. UI/UX – a PR277 nyolc referenciaoldala
**Globális elv:** eredeti TrainPilot-ikon/súlyzó, meglévő főoldali 2×4 közvetlen navigáció (Programok, Naptár, Coach stb. hozzáférése megmarad), felhasználó által választott téma, kompakt kártyák, jól látható állapotok. A PR271 alsó dockja és a külön Studio natív 5 felső fülje nem válthatja fel automatikusan a PR277 főmenüjét. HU/EN/DE/RO, legalább 320/360/393 px és nagy betű; a natív Compose kezelőszervek minimum 48dp körüli érintési felülettel. Animáció visszafogott 150–200 ms csak tartalomra, reduced motion mellett elmarad. Piros X, videóikon és sticky belső fülek megmaradnak. Témaszín vezérelje az akcentust, ne a mockup illusztrációjának fix színei.

1. **01-settings – Beállítások:** kompakt, nyitható funkciócsoportok: profil, személyre szabás/nyelv/téma, edzés/timer/progresszió, óra és szinkron, mentés és helyreállítás, egészség/engedély/adatvédelem/verzió. Mindegyik a valódi vezérlőkre kapcsoljon. Veszélyes reset csak megerősítéssel.
2. **02-workout-start – Edzésindítás:** program/nap/ciklus, valódi gyakorlatelőnézet (nem minta), gyakorlat- és munkasorozatszám, sorrend és videó. Percérték csak hiteles előzményként („Legutóbbi idő”), nem kitalált becslés. A telefonos indítás működő útvonal; „Órára (Wear OS)” akkor lehet kattintható, ha ténylegesen létezik visszaigazolt távoli programnapindító protokoll.
3. **03-health-sources – Egészség:** tényleges Health Connect/Samsung állapot, külön permission, sync, adatforrás és utolsó igazolt frissítés; forrás/csatorna megjelölése; hiányzó adatoknál hiányállapot, nem nulla vagy hamis „kapcsolódva”.
4. **04-active-workout – Aktív edzés:** kompakt összegzés, aktuális/ következő gyakorlat, közös sorfejléc, előző teljesített eredmény, munkasor/bemelegítés, súly, ismétlés, opcionális tényleges RIR, OK/Undo, meglévő pihenő stopper és haptika. Az alsó saját stopwatch működését ne cseréld le. Nincs második timer-owner.
5. **05-outdoor-measurement – Kültéri mérés:** GPS opt-in/engedély, valódi útvonalszegmensek, idő/táv/tempó/pulzus csak mért adatokból, kis térkép és explicit mérésvége. Beltéri mérés GPS nélkül is működik.
6. **06-fullscreen-map – Nagy térkép:** a meglévő aktív rögzítéstől független megjelenítési felület. A bezárás nem állítja le GPS-t, edzést, időzítőt; semmilyen szintetikus GPS-koordináta.
7. **07-journal-pr – Napló:** kompakt zárt kártyák valós adatokkal, pici PR-jelzés külön lenyitó nyíllal, Összes/PR/GPS filter a lapozás előtt; megmaradó Napló/Fejlődés/Statisztikák sticky fülek, szerkesztés, fotó, dátumszűrő, törlés.
8. **08-programs – Programok:** kompakt programkártyák, típushoz illő ikonok (testsúlyos, erő, futás stb.), aktiválás jobb oldalt, közvetlen balján külön lenyitó nyíl; saját/közösségi, létrehozás, másolás, szerkesztés, sablon és napok. Minden saját programnap ugyanolyan tömör elrendezést kapjon.

**Alternatív mockup:** az AI Studio generálhat saját, azonos funkciójú és azonos arculatú 8-képernyős Compose alternatívát. Külön mutasd meg vizuális összehasonlításra, de ne cseréld le automatikusan az elfogadott PR277 szerkezetet.

### 3. Stabil main – részletes funkcionális minimum
**Program/katalógus:** profil + cél/korlát/felszerelés + előnézetes A/B terv, gyakorlatkönyvtár, saját gyakorlatok és videók, sablon/saját program, napok és gyakorlatok szerkesztése, aktiválás és gyors edzés. Naptár: nap/heti/beosztás, edzés- és távolságcél, elmulasztott edzés újratervezése.
**Erőedzés:** súly/ismétlés/munkasor/bemelegítés, kettőoldalas/timed gyakorlatok, 0–10 tényleges (nem célszám) RIR, előző érvényes teljesített sorozat alapján automatikus értékelés és következő súly/ismétlés előtöltés. Az RIR/OK/draft/egészség mérési adat nem másolható át új completed tényként. Normál és HIT-inspirált stílus, dupla/lineáris/RIR-Coach progresszió; ajánlás külön jóváhagyással.
**Futás/pihenő:** kanonikus munka/pihenő timer, opcionális hang és rezgés, pihenő +/- és kihagyás, automata fázis/előrelépés, kétoldalas és beltéri időzítés; explicit pozitív stopperstop készrejelölés, implicit pause/0 mp nem. Sorozat OK/Undo idempotens.
**Napló:** mentett edzésidők, sorozatok, fotók, személyes rekordok és korábbi→új érték, utólagos visszajelzés, tételszerkesztés/törlés, naptárkapcsolat. Készrejelölés visszavonása oldja a naptári kötést, a napló adatainak külön törlése nélkül.
**Fejlődés:** összvolumen, edzésszám/idő, becsült 1RM, izomcsoport relatív terhelése, kardió mérőszámok, testtömeg és privát fotógaléria; adatok időszak-szűrése. Ne nevezd e1RM-et mért maximumvizsgálatnak.
**Health:** helyi napló, pulzus/alvás/lépés/HRV/testtömeg/más engedélyezett metrikák; Health Connect és támogatott Samsung Health olvasás, forrás és időbélyeg; igazolt mai Samsung Energy Score elsőbbsége a regenerációs kijelzésben, egyébként TrainPilot becslés vagy hiány. A Coach fájdalom/regeneráció/hiányzó adatok védelmét megtartani.
**GPS:** telefon vagy óra helyadatok, szakaszok, privát natív koordinátatár, valódi térkép; megjegyzett opt-in, külön OS hely+app pontosság. A sikeres explicit „Mérés befejezése” a távolsági sorozatot külön OK nélkül teljesíti, de implicit stop/térképbezárás/hiba nem.
**Wear OS:** önálló offline tervezett/gyors edzés, telefon↔óra tulajdonosátadás nyugtával, egyetlen workoutId/setId és időzítő-owner, csak olvasható tükör, retry/idempotencia. Pulzus/kalória/táv szenzortól és engedélytől függ; naptár, Mini napló, Tile, complication, korona/lünetta, 3 mp touch lock és Undo. A készülék szinkronja nem azonos az órás *távoli kiválasztott programnap indítással*.
**Backup/felhő:** helyi JSON/ZIP export/import, ZIP-ben fotók ellenőrzőösszeggel, Drive appDataFolder opcionális szinkron, Google Calendar csak kezelt TrainPilot-események, erasure/tombstone, megerősítés, hibajelentés, többnyelvű feliratok. Nincsen automatikus Android rendszer-cloud backup az app-adatokra.
**Személyre szabás:** fő téma/kontraszt és színek, HU/EN/DE/RO, akadálymentes kezelés, engedélyek, beállítások tartóssága; a natív korábbi 32 paletta forrásai elérhetők.

### 4. PR279 – delta és jelenlegi korlátok
**Kódban és automatizáltan tesztelt:** PR277 nyolc képernyő-átalakítás, kompakt edzésnapok, gyorsedzés-segédszöveg csökkentése, Health felső rész kompaktabb, valós Napló PR/GPS szűrés, valódi route-előnézet, modal dátumszűrő Ettől–Eddig/Törlés/Ma/X/Back, naptári Visszavonás egyetlen vezérlővel, programikonok és jobb oldali Aktiválás, egységes összecsukható Progresszió / Coach, RIR bevitel az OK előtt, csukott/nyitott edzésnap +N előnézet. A konkrét legutolsó HEAD 5ff8cbd0 115/115 Chromium és aláírt telefonos tesztbuild eredménye szerepel a PR279 leírásában; ez **nem release**.
**Felhasználó által egyelőre elfogadott részfeladat:** naptári Visszavonás felületi működése; Progresszió/Coach összevonás. A többi új pont fizikai telefonos elfogadása részben még hiányzik.
**Wear indítás: NYITOTT:** a régi startWatchMeasurement csak RemoteActivity-t nyit a már publikált edzés kontextusában; nem bizonyít új, kiválasztott programnapot indító óra-owner protokollt. Új, idempotens és ACK-alapú telefon→óra programnapindító külön szükséges (másik aktív edzés, offline, több óra, stale command, biztos tulajdonos). Nem kész funkció.
**Nagyobb font, összes képernyő térköz, valós Wear/GPS/Health és új migrációs forrású APK elfogadása** további teszt.
**Status:** PR279 OPEN/DRAFT, nincs main-merge, verzióemelés vagy release. A PR277/278/271 archivált.

### 5. Natív migration – hasznos meglévő alap, nem kész termék
Olvasd a sources/native-migration/android/app/src/main/java/com/repforge/app/ fákat és a docs/design/final-ui-ux specifikációt. A native branchben léteznek Kotlin/Compose UI, Room és DataStore, natív edzés/stopper/ownership, Wear/GPS/Health és import adapterek; migrációs kód például:
- android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt
- android/app/src/main/java/com/repforge/app/ui/
- android/shared/ és android/wear/
- docs/design/final-ui-ux/ (24 nézet és 81 funkcióazonosító)
- docs/design/mockups/ és docs/design/STUDIO_DRIVE_VISUAL_REFERENCE_2026-10-28.md
- audits/NATIVE_FINAL_FEATURE_INVENTORY.md és JSON: 107 korábbi forrás-ellenőrzési tétel.

A natív migrációs PR238 **CLOSED, NOT MERGED**; külön natív ágon a 2026-10-29-i PR270 több futása zöld volt, de ez nem végleges publikus termék. A natív UI teljes PR277 vizuális egyezése és fizikai készülékes elfogadás nem igazolt. Régi natív P0–P6 tervezésekből ne jelents kész funkciót. Különösen vizsgáld újra: onboarding, naptár többhetes terv, program-/gyakorlat reset, utólagos feedback, globális timer-beállítás, GPS recovery, Health permission/ háttér-sync, Google maintenance, fotógaléria, 24 UI nézet és 32 témaváltozat. A branch HEAD-hez képest korábbi auditok lehetnek elavultak.

### 6. WebView-adatvisszaállítás és upgrade – elfogadási kritérium
A legacy forrás a main, a backup formátumai és a LegacyWebViewMigrator. Tervezd az importot **két külön útvonalon**:
A) explicit mentés: a felhasználó a régi appból **ZIP**-et exportál, fotókkal és ellenőrzőösszegekkel; vagy JSON-t, fotók nélkül. Natív app importál, validál, előnézetet/módosítási összegzést mutat, egyetlen jóváhagyás után atomikusan cserél; hiba/mégse/túl nagy/sérült ZIP esetén visszagörget. Megőriz programot, napokat, naptárt, naplót, súlyokat, RIR/feedback, fotókat, téma/nyelv/profil és szinkron/törlés metaadatokat; OS permissiont és OAuth tokeneket soha nem másol.
B) in-place upgrade csak **ugyanazon com.repforge.app és ténylegesen azonos APK signer** mellett, a versionCode monoton növelésével, telepítés után a meglévő WebView localStorage LevelDB + Capacitor SharedPreferences tartalom egyszeri, megismételhető, ellenőrizhető Room/DataStore migrációjával. Későbbi sikertelen migráció nem törölhet régi adatot; az adatmentés és helyreállítás visszajelzés legyen valódi. Play eltérő aláírója mellett ez nem ígérhető, ott A) útvonal szükséges.
- **WebView-backup visszaállítása:** tesztelt export/import szerződés. Az eredeti WebView bináris visszaállítása csak megőrzött régi APK + aláírás + kompatibilis adat/backup mellett lehetséges; az új Room adatok nem garantáltan olvashatók vissza régi WebView-val. Régi APK újratelepítése csak külön mentéssel és visszaállítási próbával.
- **Tiltások:** automatikus eltávolítás/uninstall, régi adatbázis törlése, új app megnyitásakor automatikus Cloud restore, nem kontrollált migráció, sérült fotó csendes eldobása, duplikált workout vagy resurrected tombstone.
- GPS koordinátákat tilos a Drive/cloud backupba beemelni. A health export alapból tiltott, csak expliciten engedélyezett. A fotó ZIP byte-digest egyezzen. Offline esetben a helyi ZIP maradjon elérhető.

### 7. Natív architektúra
Kotlin/Jetpack Compose + Navigation + ViewModel/StateFlow + Room + DataStore, Android Health Connect / Samsung SDK csak jogszerű valós hozzáféréssel, Foreground Service a meglévő szükséges eseteire, GPS/Maps meglévő privát forrásból, Wear Data Layer biztonságos ACK/outbox-szal; nincs párhuzamos edzés-adatmodell és nincs második timer-owner. Külön domain/data/UI rétegek, DI, import versioning, event/revision/idempotens mutáció. A read-only Watch-owned állapot a telefonon nem lehet interaktív edzésíró. Egészség/route hiányát tisztán kell mutatni. Theme tokenek és Compose previewk valós adat/fixture elválasztással.

### 8. Megvalósítási és tesztelési sorrend
1. Source audit: main, PR279, PR277 images, native migration, 107-soros audit. Válaszd el a biztos tényeket a tervtől; írj új paritásmátrixot.
2. Compose app-skeleton és navigáció PR277 2×4 arculatban; Settings és 8 képernyő statikus komponensei, **demóadat nem production**.
3. Room/DataStore és régi stabil domain/motor: programok/gyorsedzés/könyvtár/naptár.
4. Egységes aktív session, set/undo/RIR/Coach, timer és háttérfolytatás; persistent draft.
5. Napló/PR/progresszió, fotók, adatvédelmi és visszaállítási útvonalak.
6. Health/GPS/Wear integráció: valós adapter, engedély, tulajdonos/ACK, offline; hiányzó távoli Wear programnapindító külön modul és külön elfogadás.
7. JSON/ZIP + upgrade migráció csak tesztadatokkal; előtte/utána semantikus egyezés 0/200/1500 edzésnél, sérült ZIP, restart, storage/permission/network és fotók.
8. 320/360/393px, 384dp/450dpi, 4 nyelv, fontScale 1/1.4/2, 32 téma, TalkBack, reduced-motion, életciklus, indítás sebesség és screenshot-mátrix.
9. Verzió-/signer-/source-hash egyezés, R8 release és telefon/Wear APK CI; két korábbi kiadásból upgrade-ellenőrzés; külön valós készülékes engedély/Health/Samsung/GPS/Wear/profil teszt. FIZIKAI FELHASZNÁLÓI ELFOGADÁS NÉLKÜL nincs main merge/release.

### 9. Az AI Studio-nak adandó első utasítás
> A repository gyökerében a régi, működő WebView TrainPilot van. Olvasd el először az ai-studio/README.md és ai-studio/TRAINPILOT_NATIVE_MASTER_SPEC.md fájlokat, majd a forrásokat a dokumentált prioritással. Készíts egy **teljesen új, tiszta Kotlin/Jetpack Compose Android projektet** külön fejlesztési célhelyre, régi adatok törlése és az eredeti main módosítása nélkül. Az eredeti PR277 nyolc PNG-je legyen az első UI/UX referencia. Először sorold fel a bizonyított működéseket, a részleges és tervezett elemeket, az adatimport tervét és 8 képernyő Compose mockupját. Ezután implementálj mérhető, buildelhető, tesztelt natív részeket, és minden modulnál add meg a source, teszt és eszközös elfogadás állapotát. Ne használj WebView-t főalkalmazásként, ne találj ki egészség/GPS/Wear adatot, ne próbálj főágat vagy felhasználói mentést felülírni.

### 10. Eredeti hiteles hivatkozások
- Stabil main: https://github.com/fuloplevente1998/TrainPilot/tree/main
- PR279: https://github.com/fuloplevente1998/TrainPilot/pull/279
- PR277 dizájn: https://github.com/fuloplevente1998/TrainPilot/pull/277
- PR238 native: https://github.com/fuloplevente1998/TrainPilot/pull/238
- PR250 audit: https://github.com/fuloplevente1998/TrainPilot/pull/250
- PR262 24-nézetes docs: https://github.com/fuloplevente1998/TrainPilot/pull/262
- PR270 natív Studio: https://github.com/fuloplevente1998/TrainPilot/pull/270
- PR271 elvetett nav: https://github.com/fuloplevente1998/TrainPilot/pull/271
- PR280 issue (programnap-kompaktság): https://github.com/fuloplevente1998/TrainPilot/issues/280
- PR239 Wear measurement permission: https://github.com/fuloplevente1998/TrainPilot/pull/239
- Release: https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.71-perf-20261022

**Ez specifikáció és referenciacsomag. Nem állítja, hogy az új natív APK már elkészült vagy készüléken elfogadott.**
