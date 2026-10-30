# Chat B – Fejlődés: privát fotók és súlyzós trend időszakválasztás (G10)

Saját ág: feat/native-progress-photo-range-binding.
Report: docs/reviews/NATIVE_PROGRESS_PHOTO_RANGE_HANDOFF.md.

## Konkrét cél és meglévő alap

ProgressUiState.progressPhotos listát a VM már feltölti, a ProgressScreen azonban
nem jeleníti meg. A strength1RM trendhez nincs külön időszakválasztó, miközben a
kardió és testsúly külön saját szűréssel bír. Kösd be a hiányzó megjelenítést és
valódi súlyzós időszakot; ne címkézd át a kardió/testsúly szűrőt strength-szűrőnek.

## Írható fájlhatár

- ui/progress/ProgressViewModel.kt, ProgressScreen.kt, NativeProgressQuery.kt.
- Új saját `ui/progress/NativeProgressPhotoGallery.kt`, `NativeStrengthRange.kt`
  és indokolt privát megjelenítési helper.
- Saját res/values{,-en,-de,-ro}/native_progress_photo_range.xml.
- Saját NativeProgressPhotoRange*Test.kt JVM/nativePhoneUiTest és a meglévő
  NativeProgressQueryTest csak kiegészítés, eredeti assertjei megmaradnak.
- Saját report, saját scoped QAworkflow-példány.
Minden más READONLY: NativeCardioModel/Adapter/CalendarRefresh, WeightJournal,
Room/schema/DAO, WorkoutPhotoRepository/photo writer, backup/archive/Drive,
Home/Coach/NavHost, aktív edzés és a közös fixture/NativePhoneNavigationTest.
Új DAO-megoldás helyett először használd a meglévő keyset256os lekérdezést,
range lowerboundnál korai megállással. Ha ez ténylegesen elégtelen, a DAO-igényt
külön pontos patchként add át Rootnak; a közösDAO-fájlt ne írd.

## Követelmények

1. Strength időszak:30nap/90nap/1év/összes vagy a main tényleges végső szűrőivel
   bizonyított megfelelő választék. Megjelenített választék és tényleges adatablak
   egyezzen. Egyértelmű helyi naphatárok/DST/jövőbeli hibás dátum kezelése.
   1RMképlet/rögzített loadType/setdone szabályok nem változhatnak.
2. Egy adott gyakorlat keyset lapozása maradjon bounded; nincs1500edzésreláció/
   GPS/fotóhydrate a görbéhez. Editsamecount/delete/restore invalideventre frissül.
   Gyors gyakorlat- vagy rangecsere régi queryje nem publikálhat az új választásba.
3. Privát fotók: meglévő photo ID/metadata/privateFile adatút. Ne listázz vakon
   minden .jpg fájlt legitim fotóként: orphan/törölt/quarantine/más app-ból behozott
   adat nem jelenhet meg. UI read-only galéria/thumbnail/nagyítás és vissza,
   állapotmegőrzéssel. Nincs új fotóíró/törlő/nyilvános export/saját Drive-szinkron.
   Méretkorlátos decode IO-n, ne egyidejű full-res bitmap minden képből.
   Sérült/hiányzó fotó őszinte helyettesítő+hibaállapot; nincs crash vagy mockfotó.
4. Program/edzés/forrás összerendelésnél használd a meglévő kanonikus metaadatot,
   ne a lastModified dátumot jelöld a mérés hiteles dátumának. Ha a régi File-lista
   ehhez kevés, a megengedett readadapterben fogd össze a meglévő metadatával;
   dokumentáld a korlátokat, ne találj ki mérési időt/Előtte-Utána minősítést.
5. A jelenlegi kardió, izomterhelés, testsúly gyorslink,PR/1RM, theme/locale/scroll
   regressziói maradjanak. Üres galéria, hiányzó mérés és üres időszak külön állapot.
6. Tesztek:0/200/1500history; határnap/DST; gyakorlat/rangecsere; samecount edit;
   törölt/missing/corrupt/orphan fotó; boundeddecode; explicitgalleryclose;
   nyelv/témaváltás után állapot megmarad. Seedeltprivát kép fixture legyen jelölve.
7. Valódi ProgressUI emulátoron, meglévő3NativeCardioProgressUiTest is zöld ugyanazon
   saját forráson. Saját új strength/photo tesztek +releváns JVM ténylegesen futnak,
   screenshot gallery/range/üresállapotról és XML/rawlog/hash legyen. Ne kezdj teljes
   új grafit/arany redesignba; ez a későbbi dizájn valós bekötéseit készíti elő.

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
