# Chat A – Hiteles kezdőlapi regeneráció és energiaadat bekötése (G01)

Saját ág: feat/native-home-recovery-provenance.
Report: docs/reviews/NATIVE_HOME_RECOVERY_PROVENANCE_HANDOFF.md.

## Konkrét cél

HomeViewModel.fetchTodayMetrics jelenleg a napi energyScore nyers számából
recoveryText-et képez, forrás/idő/fallback nélkül. Kösd a Kezdőlap regenerációját a
meglévő natív HealthJournal + NativeCoachSignals/NativeCoachReadiness szabályokhoz.
Ne fejlessz új pontszámképletet vagy új egészségszolgáltatót.

Megőrzendő hierarchia: hiteles, érvényes MAI Samsung Energy Score esetén azt
mutassa az energia/regeneráció csempe, forrás és időjelöléssel. Ennek hiányában
csak a meglévő számítás által valóban rendelkezésre álló TrainPilot-becslés lehet
fallback, egyértelműen más címkével. Ha adat nincs:—/nincs adat, nem0és nem70.
A Coach javaslati pontszáma terhelési/fájdalom korlátozás miatt eltérhet a Samsung
nyers energiapontjától. Ezt ne mosd össze, ne átlagold őket, és ne írd át a Coachot.
A36órás alvás/HRV-frissesség és a Samsung mai napi hitelesség más szabály:
a meglévő validátorokat olvasd és használd, ne írj saját egyszerűsített dátumszűrőt.
0pont lehet érvényes skálaérték; null/NaN/rossz tartomány nem ugyanaz, mint0.

## Írható fájlhatár

- ui/home/HomeViewModel.kt és ui/home/HomeScreen.kt.
- Új tiszta `ui/home/NativeHomeRecoveryPresentation.kt` és saját Home-adapter helper,
  ha kell; meglévő natív readiness számítást használva.
- Saját res/values{,-en,-de,-ro}/native_home_recovery.xml.
- Saját NativeHomeRecovery*Test.kt JVM és nativePhoneUiTest alatt.
- Meglévő NativeHomeDailyCountTest/NativeHomeRepositoryTest csak kiegészítés,
  az eredeti assertjeik megtartásával.
- Saját report és saját scoped QAworkflow-példány.
Minden más READONLY: Coach/mag, HealthJournalStore/Samsung/HC adapter, DAO/schema,
HomeRepository/main-start/ownership motor, NavHost, aktív edzés, közös fixture és
NativePhoneNavigationTest, NativeProgressQuery/ProgressScreen.

## Teljes végrehajtandó csomag

1. Mutasd ki a meglévő napi/recovery record forrását, érvényességét és időadatát.
   Olvasd a CoachViewModel aktuális adapterét. Legyen leválasztott, tesztelhető
   megjelenítési modell source/timestamp/value/partial/stale állapottal.
2. Room/Health naplóolvasás IO-n, bounded (14nap/utolsó3effort) mint a meglévő mag.
   Nincs all-history hydrate,250msHealth-polling vagy SDK-hívás főszálon.
3. HomeUI valós label/source/time, becslés elkülönítése, részleges/frissítési hiba
   becsületes jelzése. A korábbi adat ne kapjon automatikusan mai/friss jelölést.
4. Gyors egymás utáni load/language/day/zone/restore változásnál régi eredmény
   nem írhat felül újabbat. DataReplacement Continuation/régi admission maradjon;
   kancellációt ne nyeld el. Nincs adatírás a számolás kedvéért.
5. Tesztek:mai hiteles Samsung; tegnapi/36h+ forrás; jövőbeli/hiányzó timestamp;
   0/100/hibás/NaN; alvás/HRV/effort fallback; fájdalmas nagy terhelésnél
   Samsungérték és Coachscore külön; nincs adat; nap/zoneváltás; stale callback.
   Valódi HomeUIonboardolt fixture-rel mutassa a helyes értéket és HU/EN/DE/RO címkét.
   Nem tesztelhető SDK-t szintetikus HealthJournal-fixture helyettesítheti,
   de jelöld, hogy ez nem valódi Samsung készülékes hitelességmérés.
6. Saját célzott GitHub-kapu zöldre javítva, raw screenshot legalábbSamsung/fallback/
   nincsadat állapotokról, valós futóHome-ból. Main/nyolcfülesUI nem állítandó vissza.

## Közös végrehajtási szerződés

Repo fuloplevente1998/TrainPilot, exact indulás `30e087646298fb0191f5ffa0e7e157b14519d669`.
Olvasd AGENTS.md/NATIVE_RESUME aktuális tetejét, majd
`docs/design/final-ui-ux/04-implementation-and-acceptance.md` érintett G-pontját.
Új saját ág a megadott exact commitból, draft PR cél feat/native-android-migration.
Main ÉS migrációs főág csak olvasható: nincs oda push/merge/rebase/force push,
verzió/csomag/séma/dependency csere. Nincs új Codex vagy belső subagent.
Kizárólag az alábbi saját fájlokban dolgozz. Root a közös navigáció, aktív edzés,
GPS/interval UI és #261 folytatás tulajdonosa; másik chat külön területet kap.
Az eredmény tényleges, használható bekötés és teszt legyen, ne audit vagy mockup.
A végleges grafit/arany újratervezés külön szakasz: most funkció/adatmegjelenítési
hookok és a meglévő stílushoz illeszkedő minimális UI készül.32téma,HU/EN/DE/RO,
48dp/kis kijelző/nagy font/accessibility szerződés marad. Nincs kitalált demo-adat.

### Saját ági célzott CI ENGEDÉLYEZVE, végig kell vinni

Saját refen a már regisztrált native-android.yml dispatch futtatható. A saját ág
`.github/workflows/native-android.yml` példánya scoped QA-ra átírható: debug+
UItestAPK fordítás, releváns JVM tesztek és az érintett régi+új UI tesztosztályok
API35 emulátoron. Ez QA-only delta, Roothoz NEM integrálandó; main/migration
workflow-ját nem írhatod. Manuális indítás saját refre, köztes commit [skip ci].
Kizárólag szintetikus, eldobható emulátor-fixture. Helyben teljes Gradle/emulátor
nem fut; gyors Python/XML/diff/lexical indokoltan mehet, külön státusszal.
Ne futtasd a teljes közös phone suite-ot ismételten: az aktív Root-munka. A saját
célzott osztályokat és releváns regressziót ténylegesen futtasd, a hibát javítsd,
majd ismételd a szükséges kaput a saját exact forráson, amíg zöld.
Nem elegendő UNRUN tesztforrást késznek átadni. Valós környezeti blokkolót konkrét
hibával jelezz; az nem PASS. Ne kérj újra engedélyt minden rutinszerű javításhoz.
Nincs teszttörlés/@Ignore/assertgyengítés, no-op gomb, hamis siker vagy csak
sleep/timeoutemelés mint javítás. Root/másik munkás futásait ne indítsd újra/ne töröld.
Kész: egy exact app/test SHA-n debug+releváns JVM+UI zöld, 0hiány/skip/crash,
rawlog/XML/hash és valós, módosítatlan adb screenshot az új részről. A csomag zöld
nem teljes app-elfogadás; az összevont CI és fizikai SDK-készülékpróba Rootnál marad.
Saját docs/reviews/ átadó + PR leírás exactbase/head, tesztnevek és futás/artifact
linkekkel, kimaradt/korlátozott rész külön. Ne merge-elj/ne zárj PR-t/issue-t.
Fájlhatáron túli valódi okhoz pontos patch-javaslatot adj, ne módosíts más területet.
