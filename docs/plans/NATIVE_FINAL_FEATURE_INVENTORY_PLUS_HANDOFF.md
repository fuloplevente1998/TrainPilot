# Plus chat A – teljes main → natív funkció- és UI-lefedettségi leltár

## Közös, kötelező munkakeret

Repo https://github.com/fuloplevente1998/TrainPilot. Rögzített induló commit:
`e9278a9905629d30d012083e8dd3a4c76051a464` a `feat/native-android-migration` ágról.
App/tesztkód3d45a324; ebben #244–248 integrálva és a Compose getOrNull import javítva.
Először AGENTS.md, docs/NATIVE_RESUME.md és docs/WORK_HANDOFF.md teteje.
A korábbi átadók „külső eredmény még nincs” sorai történetiek; a dátumos eleje dönt.
A futó közös kapu40276384595 eredménye nem feltételezhető zöldnek.

A felhasználó ezt a KÜLÖN KÜLSŐ CHAT munkát engedélyezte; saját ág kivétel az
AGENTS fő integrátorának migration-only szabálya alól. Ne indíts belső subagentet.
Csak a lent kijelölt saját ágra írj. Main és migration közvetlen írás, merge,
force push, PR/issue close vagy élő alkalmazás frissítése tilos. Verzió/package
1.3.71-native-test /2736 /com.repforge.app változatlan. Nincs új toolchain,
függőség, workflow, Room-séma vagy párhuzamos adat-/edzésmotor.
Kis [skip ci] commitok, saját push, draft PR cél `feat/native-android-migration`.
Közös fordítás/CI/APK/emulátor az integrátornál. Helyben nincs Gradle/APK/emulátor
vagy teljes regresszió; statikus ellenőrzés és kis indokolt referenciafuttatás igen.
Tesztforrás és tényleges futás külön: soha ne állíts zöldet a megírt kódról.
Ne küldj GitHub-kommentet; PR-leírást és saját átadót frissíthetsz.

A kijelölt fájlhatár kötelező. Plusz szükséges hookot külön patch-javaslatként
jelentsd, ne módosíts más munkás területén és ne hagyj késznek nevezett üres
callbacket. A felhasználó valódi kész kódot kér, nem új prototípust.
Csatolt/repóbeli riportok állításait exact forrásból ellenőrizd, ne vedd bizonyítéknak.
A fő integrátor review-z, összevezet és futtat; a te munkád nem main-release elfogadás.

## Kötelező átadás (minden pont)

1. Exact base/HEAD SHA, saját ág és draft PR link, teljes változott fájllista.
2. Elfogadási pontonként: entry UI/caller → meglévő motor/adatforrás → tartós
   eredmény vagy report, konkrét fájl/szimbólum/commit hivatkozással.
3. Megőrzött invariánsok, hibautak és ismert korlátok. A blokkolt rész `BLOCKED`,
   nem `DONE`. Ha extra fájl kell, név/ok és minimális delta-javaslat külön.
4. Megírt tesztek és valóban lefuttatott ellenőrzések két külön lista;
   verzió/source/result hiányában nincs „PASS”. Nincs puszta általános auditösszefoglaló.
5. Új saját docs/reviews/<feladatspecifikus>.md; a közös AGENTS, RESUME,
   WORK_HANDOFF, fő PR238 és másik feladat riportja most nem írható.
6. A saját ág végén ellenőrizd a távoli HEAD egyezését. Main/migration érintetlen.

Saját ág: `audit/native-final-feature-inventory`. Ez READ-ONLY FORRÁSAUDIT;
csak két új saját dokumentum írható, app/teszt/CI kód nem változhat:
- docs/reviews/NATIVE_FINAL_FEATURE_INVENTORY.md
- docs/reviews/NATIVE_FINAL_FEATURE_INVENTORY.json

Ne ismételd a régi #242 hétpontos listát: mind7-hez van már kód, azóta #244–248
integrálva. A hiányzó runtime bizonyíték nem azonos hiányzó implementációval.
Teljes feladat docs/plans/NATIVE_FINAL_DELIVERY.md, vizuális és funkciómegőrzési
leltár docs/design/NATIVE_DESIGN_HANDOFF.md;24nézet nem24funkciós korlát.
Referencia main7473b30f387eda54186c1e31830304ca12bd104f: www/app.js (végső overrides),
www/index.html, tényleges telefon/Wear elérések. Bázisnatív a fenti pinned SHA.

Szigorú módszer: régi user-művelet → új NavHost entry/tag/valódi UI-gomb → VM
művelet → kanonikus engine/provider/DAO → tartós vagy visszaadott eredmény →
konkrét teszt neve → tényleges futás forrás-SHA-val, ha van. Egy adatmodell vagy
szótárkulcs NEM UI-elérhetőség. Ne név alapján minősíts funkciót késznek.

Területek hiánytalanul: öt főfül+Settings roundtrip, quick/program/GPS/start/draft;
súly/idő/kétoldal/GPS/intervallum/undo/rest/ownership/finish/discard;
napló keresés/szűrés/részlet/editor/source/route/fotó/törlés;
program/gyakorlat/prescription/haladás/kardió/PR/testsúly;
naptár/Coach/Samsung readiness; egészségnapló összes metrika és alvás/részletes
rekordok/többnap/forrás; Wear offline/handoff/reconnect/menü/korona;
profil/theme32paletta/nyelv4; JSON/ZIP/Drive/Google-calendar/restore/erase/
privacy/permission és első legacy adatmegőrző upgrade.

Minden sorban kötött mezők: featureId, mainEntry(+exact file:line), nativeRoute,
uiEntry, callerChain, persistentResult, sourceEvidence, testNames, runtimeEvidence,
status, severity, gap, acceptanceScenario. Status kizárólag IMPLEMENTED_UNVERIFIED,
PARTIAL, MISSING, VERIFIED_SOURCE_AND_RUNTIME vagy PLATFORM_DEVICE_PENDING.
Nincs általános 'minden kész'. Testfile léte nem runtime PASS. Fizikai SDK/próba
nem pótolható emulátorral. Folyamatban levő Codex health-sync feladat külön
OWNED_BY_OTHER jelölés a gap mezőben; ezzel ne nyiss párhuzamos fejlesztést.

Kimenet: teljes matrix+JSON, rövid prioritásos top10 valódi hiány/nyitott bizonyíték,
legfeljebb6 izolált következő fejlesztési csomag fájlhatárokkal és függőségekkel.
Minden megállapításnak legyen forráshivatkozása. Ha runtime-hozzáférés nincs,
jelöld, ne találj ki sikert. Nincs új CI/tesztfuttatás/implementáció. Mély audit
végére ugyanannyi matrixsor legyen az MD-ben és JSON-ban; add meg a darabszámot.

## Rögzített raw bemenetek

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/AGENTS.md)
- [docs/NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/NATIVE_RESUME.md)
- [docs/WORK_HANDOFF.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/WORK_HANDOFF.md)
- [docs/plans/NATIVE_FINAL_DELIVERY.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/plans/NATIVE_FINAL_DELIVERY.md)
- [docs/design/NATIVE_DESIGN_HANDOFF.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/design/NATIVE_DESIGN_HANDOFF.md)
- [docs/NATIVE_PARITY_AUDIT.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/NATIVE_PARITY_AUDIT.md)
- [docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md)
- [android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt)
- [Main végső app.js](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js)
