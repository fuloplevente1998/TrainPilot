# Chat 1 – Telefonos Compose teljes UI-kapu és valódi képernyőképek lezárása

Saját ág: `fix/native-phone-ui-final-gate`.
Saját report: `docs/reviews/NATIVE_PHONE_UI_FINAL_GATE_HANDOFF.md`.

## Feladat, nem csak audit

Vidd végig az ÖSSZES jelenlegi telefonos Compose/instrumentációs teszt fordítását
és futtatását, a valódi képernyőképek host-fogadását, és javítsd a felbukkanó UI /
fixture / Compose lifecycle hibákat, amíg a saját forrásodon a telefonos kapu zöld.
Nem kell új dizájnt tervezni vagy hiányzó adatmotort kitalálni.

Induló bizonyíték:
-40317771457/e28:75terminal,35PASS/40FAIL,21screenshot-ack hibával.
-40334773058/e12:fordítás/JVM/R8/aláírás/2upgrade/Wear zöld; phone82declared,
  7terminal(6PASS/1FAIL),75missing,crash,0PNG. Cardio3/3 és GoogleUI3/3 ténylegPASS.
  A GPS approximate teszt a saját UID engedély-visszavonásával megölte a runnert.
-14513da5:GPS4eredeti eset külön folyamatba téve, előtte host készíti az engedélyt;
  phone rawlog és isolated GPS rawlog külön, manifest additionalInstrumentation.
-40342319155/14513:compileDebugAndroidTestKotlin megállt a Calendar UI harness
  split-line ViewModelProvider indexelésén; de1fe140 explicit .get(...) javítja.
Mindez nem friss runtime PASS; olvasd a tényleges legutóbbi artifactokat/logot.

## Szigorú felelősségi határ

Írható:
- android/app/src/main/java/com/repforge/app/ui/** Kotlin Compose képernyők/VM-ek,
  kivéve BackupRestoreManager/ArchiveIO/migrátor adatút (ezek eleve máshol vannak).
  UI-javításhoz szükséges VM hiba javítható, motor/persistence átírás nem.
- android/app/src/nativePhoneUiTest/** (a MÁSIK chat új NativeDataIntegrity* tesztjei kivétel).
- scripts/native/phone-ui-gate.py és scripts/native/evidence-summary.py;
  kapcsolódó tests/native/test_phone_ui_gate_report.py /test_evidence_summary.py.
- Új, saját névterű locale resource négy nyelven; meglévő string csak bizonyított
  felirat/fordítási hiba esetén, teljes HU/EN/DE/RO paritással.
- Saját report és saját ági .github/workflows/native-android.yml QA-módosítás.
Nem írható: BackupRestoreManager, ArchiveIO, data/migration, Room/DAO/schema,
NativeDataReplacement és többi core engine, nativeUpgrade, migration-seed,
GoogleSyncRepository/HealthJournalStore, Gradle/deps, Wear. Másik chaté az adatkapu.

## Elvárt munka és elfogadás

1. Debug és UI-testAPK fordítás után minden meglévő phoneUIteszt fusson.
   A pontos összlétszámot a forrásból/futásból számold, nem a régi75/82-t várjuk.
   GPS4isolated eset ÉS main suite együtt legyen hiánytalan és duplamentes;
   report/CI nem zöldülhet, ha a fő suite zöld, de külön GPS-log hiányzik/piros.
2. Kiemelten ellenőrizd az integrált #2587Calendar és #25910modes/race új esetet,
   globális mode regressionöket, #255Google confirm és #256GPS visszatérést.
   Latecancel/restoreABA/activepaused/duplaapproval nem gyengíthető.
3. Maradék korábbi hibák: interval_rest/interval_auto_advance eltűnő node,
   Coach lazy-cél, Settings theme/language, program copy/reset, Health részletek,
   draft/undo/GPS/timer mentés. Diagnózis alapján javíts, ne növeld a timeoutot.
   Ha apphiba, product-javítás kell; ha hibás fixture/selector, azt javítsd.
4. Screenshot adatút: valódi `adb exec-out screencap -p`, módosítatlan PNG,
   manifest SHA/bytes/source commit és foreground saját app. Nincs generált kép,
   overlay/crop vagy régi screenshot bemásolása. Host request/ack hibát ne nyeld el.
5. Képek a tényleges app Kezdőlap/Edzés/aktív súlyzós-időalapú-GPS/Napló/Útvonal/
   Fejlődés/Egészség/Coach/Naptár/Beállítások nézeteiről, több témával és HU/EN/DE/RO
   bemutatással. Szintetikus GPS/pulzusadatot fixture-ként jelöld, nem valós mérésnek.
   Új screenshot checkpointot a meglévő valós UI tesztútban adj, nem demo activityben.
6. Teljes native-android.yml használható saját ágon. Ha a futási idő/izoláció miatt
   QA jobokat osztasz, minden phone test és mind4GPS eset maradjon a lefedésben,
   ugyanazon exact app/testAPKhash és checkout. JVM/debug fordítás és releváns
   unit tesztek is maradjanak zöldek; R8/Wear/upgrade jobot ne nevezd saját telefonos
   bizonyítéknak, ha nem futtattad. Összprojekt zöld csak tényleges összprojekt zöld.
7. Kész: fresh exact SHA compilePASS + mindenphoneUIterminalPASS/0skip/crash,
   érvényes képek/artifactok és az offline evidence ellenőrzése. Enélkül folytasd
   a saját hibák javítását. Valós Watch/SDK/OEM adatot ne ígérj emulátor alapján.

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
