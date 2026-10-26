# Chat 2 – Adatmegőrzési, migrációs és backup/restore kapu lezárása

Saját ág: `fix/native-data-integrity-final-gate`.
Saját report: `docs/reviews/NATIVE_DATA_INTEGRITY_FINAL_GATE_HANDOFF.md`.

## Cél

A meglévő natív adatúton készíts valós, ismételhető migráció/JSON/ZIP-visszaállítási
ellenőrzést, és javítsd az általa reprodukált hibákat. Ez egy teljes javítási ÉS
futtatási csomag; ne állj meg egy auditdokumentumnál vagy forrásbeli ígéretnél.
Nincs bizonyított új adatvesztés: előbb reprodukálj, ne írd át feleslegesen a működő
motort. Az e12CI két signed-upgrade baseline-ja már zöld; ezt kell megtartani és
az új roundtrip/failure útvonalakkal kiegészíteni, nem új migrátort készíteni.

## Szigorú felelősségi határ

Írható:
- android/app/src/main/java/com/repforge/app/BackupRestoreManager.kt
- android/app/src/main/java/com/repforge/app/ArchiveIO.kt
- android/app/src/main/java/com/repforge/app/data/migration/**
- android/app/src/nativeUpgrade/java/com/repforge/app/NativeMigrationProbe.kt
- android/migration-seed/src/main/java/com/repforge/migrationseed/LegacySeedInstrumentation.java
- scripts/run-native-signed-upgrade.sh
- Saját NativeDataIntegrity* tesztfájlok JVM/nativePhoneUiTest/nativeUpgrade alatt;
  meglévő NativeBackupImportTest/NativeZipRoundTripTest/LegacySnapshotParserTest/
  ArchiveIOTest/LegacyMigrationAndIntegrityTest esetekhez indokolt kiegészítés.
- Saját report és saját ági .github/workflows/native-android.yml QA-példány.
Nem írható: UI/Compose/SettingsScreen/ViewModel, meglévő phoneUItesztek/fixture,
phone-ui-gate.py és evidence-summary.py (másik chaté), NativeDataReplacement/core
engine, Room-schema/DAO/entitások, HealthJournalStore/GoogleSyncRepository,
Gradle/deps/Wear, verzió/csomag. E határon kívüli talált okot pontos patchként add át.

## Ellenőrizendő valódi adatút

1. Először meglévő teszt/probe/run leltár; kizárólag meglévő BackupRestoreManager,
   ArchiveIO, LegacyWebViewMigrator és natív tartós adatbázis útvonal. Teszt nem
   írhat előre olyan natív outputot, amelynek a migrációs eredményét kéne igazolnia.
2. Mindkét régi eredeti aláírt kiadás fölé `adb install -r`: v1.3.71 és
   v1.3.71-perf-20261022. Meglévő hash/signervédelmek maradjanak. Useradat helyett
   seed-fixture, a telefon appját soha ne uninstalláld. Második indítás sem
   duplázhat/módosíthatja az importot. Megszakadt/sérült import nem kap kész nyugtát.
3. 0/200/1500 naplózott edzés reprezentatív fixture: stabilID/forrás/számosság,
   eltérő egyezőidőpontok és Unicode-megjegyzések, sorozatok/kétoldali idő/RIR,
   időnapló, GPS route/vegyes szakaszok, Wear/HC metrikák. Programnapok/módok,
   testsúly+health records/alvás, naptár/tombstone, témák/4nyelv/globális preset,
   folytatható draft és privát fotók. Csak az alkalmazás tényleges formátumában;
   ismert szándékos privacy-kizárást ne alakíts véletlen adathiánnyá vagy szivárgássá.
4. JSON és fotókat is tartalmazó ZIP: export → izolált reset → preview/validation →
   valódi restore → újraolvasás → újraexport. Normalizált üzleti értékek/ID-k,
   GPS és fotó bytes/hash egyezzenek, a szándékosan új runtime timestamp ne legyen
   hamis regresszió. Fotók privát útvonalon maradjanak, ne kerüljön külső abszolút
   path a backupba. Forrásarchívum megmaradjon sikertelen migráció után.
5. Kontrollált hibák: hibás JSON, csonka ZIP/path traversal, hiányzó vagy sérült
   fotó, SQL/importhiba és megszakadt tranzakció. Induló adat nem sérül, false success
   nincs; újraindítás/retry a meglévő recovery/journal/probe útján következetes.
   Aktív/szüneteltetett edzés, ownership/restore fence mellett művelet tiltott;
   elutasítás sem írhat részadatot. Ne kerüld meg a meglévő admission guardot.
6. Tartós törlés/tombstone és restore utáni idempotencia: lokális naplótörlés
   után egy sima restart/ismételt régi migráció nem támaszthatja fel az edzést.
   Explicit, felhasználó által jóváhagyott régebbi backup restore más szerződés;
   ennek jelentését ne cseréld össze háttérbeli visszaszinkronnal.
7. Teljes új Health/Google integrációt nem építesz. Offline portable archívum és
   migráció a cél, valódi cloud adat írás/törlés/revoke/HC import/export tilos.
   Drive/SDK valódi account ellenőrzés külön pending státuszban marad.

## Saját GitHub-kapu és befejezés

A saját native-android.yml QA-példányát szűrheted adatkapura: debug+release/signing,
meglévő és új releváns JVM, új data instrumentáció és KÉT signed-upgrade job.
A telefonos általános UI és WearUI jobot a saját QA workflow-ból elhagyhatod,
MERT másik munkacsomag; ezt explicit `scoped data QA` névvel/leírással jelöld,
ne állíts teljes app CI SUCCESS-t. A biztonsági ellenőrzéseket/aláírás/2baseline/
restore guardokat nem hagyhatod el. A kanonikus workflow másolata Rootnál változatlan.
Az új NativeDataIntegrity* instrumentáció szűrten, pontos class/method névvel fusson
eldobható emulátoron, és raw log/XML legyen hozzá, hiány/crash/skip nem PASS.
A hiba miatt álló saját kaput javítsd és ismételd; ugyanarra az exact SHA-ra legyen
zöld a saját releváns JVM + roundtrip/fault-injection + 2signedupgrade kapu.
0/200/1500minta futását/darabszámát és a sértetlenségi hash-eket artifact igazolja.
Ha túl nagy, a mintafuttatást shardingold szét, de egyik mintát se hagyd ki.

Kész: pontos branchHEAD, igazolt saját kapuk, adat-/bytesassert eredmények,
letölthető kizárólag fixture-t tartalmazó jelentések, javítások és maradék fizikai
készülék/cloud korlátok. Nem állíthatod, hogy a felhasználó összes valós adata
biztosan sértetlenül migrálható csupán e fixture-kapuk alapján.

## Közös szerződés – ezt is végre kell hajtani

Repo https://github.com/fuloplevente1998/TrainPilot
Exact indulás: `de1fe14062cfe6675ec2b3c12d13945b327683b6` a feat/native-android-migration ágról.
Ez tartalmazza a258/259teljes review utáni integrációját, GPS runner izolációját,
és a legutóbb GitHubon kimutatott Calendar ViewModel-lookup fordítási hiba javítását.
Ne kezdd régebbi task-ágon. Hozz létre az alább előírt új saját ágat erről a SHA-ról.
Először AGENTS.md, docs/NATIVE_RESUME.md aktuális teteje és ez a teljes feladat;
a régi területfoglalás/CI-tiltás helyett EHHEZ a két csomaghoz az itteni explicit
saját ági QA-felhatalmazás érvényes. Más területen az eredeti szabály változatlan.

A user kérése: önállóan javíts és TÉNYLEG TESZTELJ a saját csomagod végéig.
Ne állj meg a tesztforrásnál vagy az első piros runnál. Olvasd a teljes releváns
logot, javítsd a valós okot, futtasd újra a szükséges kaput. Egy futást várj meg,
ne indíts párhuzamosan ismétlődő teljes kapukat. Környezeti/hozzáférési blokknál
konkrét hiba és a már elkészített javítás kell; ez nem PASS és nem lezárt feladat.
Main ÉS feat/native-android-migration csak olvasható. Saját branch/draft PR target
feat/native-android-migration. Nem merge-elsz, nem force pusholsz, nincs másik
ágra push/rebase vagy issue/PR-zárás. Nincs belső subagent, másik Codex vagy Gemini.
Csomag/verzió mindvégig com.repforge.app /1.3.71-native-test /2736.
Nincs éles felhasználói adat, fizikai telefon uninstall/install vagy Google/HC
éles adatok írása. Emulátor csak izolált, szintetikus fixture-rel.
Helyben nincs teljes Gradle/emulátor: fordítás és runtime GitHub Actions.
Szűk Python/lexical/diff/XML ellenőrzés helyben megengedett, külön jelölve.

### SAJÁT ÁGI CI most kifejezetten megengedett

Az ismert, már regisztrált `.github/workflows/native-android.yml` workflow_dispatch
futtatható SAJÁT refen. Pontos példa:
`gh workflow run native-android.yml --repo fuloplevente1998/TrainPilot --ref SAJAT_AG`
A saját ág kanonikus workflow-példányát az alábbi csomagszabály szerint átírhatod
QA-ra; main/migration workflow fájlját nem. Ez külön QA-delta, nem integrálandó
appváltozás: PR-leírásban és diffben különítsd el. Root kihagyja a QA-workflow
változtatását az integrációból. Új workflow default-branch regisztrációja nem kell.
Manuális indítás saját refre; köztes commitok [skip ci], tesztelendő commitot
manuálisan indítsd. Saját kísérleti runod hibáját javítsd, Root futásait ne állítsd le.
Nincs assertion gyengítés, @Ignore/teszttörlés, hibás állapot PASS-nak átcímkézése,
engedélyteszt kihagyása, native scope eltüntetése vagy product viselkedés csonkítása.
Ne köss be a régi hardcoded com.repforge.HealthConnectManager prototípusból semmit.
A main7473b30szemantikája és a már integrált natív engine/adatút az alap.

### Folyamatos, de rövid átadó és végső eredmény

Saját docs/reviews/ report és draft PR checklist: pontos kezdet/vég SHA, fájlhatár,
reprodukált ok/javítás, tényleges JVM/UI/upgrade darabszám XML/rawból, run/artifact
link és hash. A jelzett végső SHA legyen azonos a sikeresen tesztelt SHA-val;
a jelentést utána külön docs-only committal frissítheted, ezt jelöld egyértelműen.
A másik csomag fájljához ne nyúlj; azon túli szükséges javítást pontos patchként
írd az átadóba. Root fog review-zni és a közös kaput futtatni; addig draft marad.
Hibás/hiányos run és korábbi más SHA eredménye nem elfogadás. Mindig külön:
saját csomag zöld / más terület hibás / valódi készülék még nem ellenőrzött.
