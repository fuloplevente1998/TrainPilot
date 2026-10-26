# Új chat B – planner main-plan-7 paritás kivizsgálása és minimális javítása

Repo: https://github.com/fuloplevente1998/TrainPilot
Rögzített induló commit: `4eae08c8b917893ac5da37346201872828783814` a feat/native-android-migration ágról.
A bázis tartalmazza #246 kardió és #244 javított Health kódját. #245 külön munkásnál.

Először AGENTS.md, docs/NATIVE_RESUME.md, docs/WORK_HANDOFF.md teteje, majd
csak a konkrét alábbi források. A későbbi főági státusz nem a bázisod tesztbizonyítéka.
Ez a felhasználó által kért KÜLÖN CHAT-feladat, saját ág kivétel az AGENTS fő
munkásának migration-only szabálya alól; nem belső subagent-felhatalmazás.
Main/migration írás, merge, force push, issue/PR-close, release tiltott.
App com.repforge.app /1.3.71-native-test /2736 változatlan. Nincs schema/toolchain/
függőség/workflow változtatás, új implementáció a meglévő motor helyett vagy élő install.
Kis [skip ci] commitok a saját ágra, draft PR cél feat/native-android-migration.
Helyben nincs Gradle/APK/emulátor/teljes teszt. Forrásvizsgálat, statikus ellenőrzés
és indokolt kis referenciafuttatás megengedett; közös natív CI az integrátornál,
ne indíts külön teljes kaput. Megírt teszt nem futtatott teszt; pontosan külön jelöld.
Ne küldj GitHub-kommentet. PR-leírás/új saját átadód frissíthető. Magyarul röviden.
Javasolt High kódoláshoz; nem látványterv- vagy témarendszer-feladat.

Átadás: exact SHA, fájllista, bizonyított ok és minimális fix, megőrzött invariánsok,
megírt és valóban futtatott ellenőrzés külön, nyitott pontok. Más feladat fájlját
ne módosítsd: szükséges plusz termékfixet külön javaslatként adj át, ne rejtsd tesztbe.

Saját ág: `fix/native-planner-template-parity`.

A korábbi40238833748 CI NativePlannerTemplateParityTest main-plan-7 esetén
IllegalArgumentExceptiont várt, de a natív preview elfogadta. Az oracle vagy a
port hibás lehet; a pinned main valódi végső viselkedése dönt, nem a zöldre festés.

Írható meglévő fájlok az android/app alatt:
- src/main/java/com/repforge/app/NativePlannerPolicy.kt
- src/main/java/com/repforge/app/NativeProfileProgramPreview.kt
- src/test/java/com/repforge/app/NativePlannerTemplateParityTest.kt
- src/test/resources/native-planner-cases-main-7473.json
Új külön planner regressziós teszt/reference harness és saját docs/reviews átadó
megengedett. Nincs Database/DAO/catalog/schema, NativeProgramEditor, Settings,
Coach/modes, Health, Progress, workout-motor vagy közös UI-teszt módosítás.
A production native-planner-main-7473.json erőforrás csak olvasható; test fixture
ne kapja ugyanazt a nevet (korábban ez88 hibát okozott classpath-elfedéssel).

Referenciamain7473b30f387eda54186c1e31830304ca12bd104f, www/app.js blob
b5567576bed9a8f93608a6bf1ed8db1dee4723f4. Olvasd a végső függvényfelülírásokat,
valódi catalog/metadatát és a referencia előállításának dokumentált módját.
Fő bemenet: home-basic;beginner;gear=[];90perc;fitness/mixed/auto/balanced;
excluded=[pushup,modified-pushup,incline-pushup,squat,air-squat], avoidAreas=[];
age30/height175/weight75/locationhome/cadencealternate, üres synthetic history.
A fixture jelenleg rejected=true. Derítsd ki: a végső main ugyanezzel elutasít-e,
vagy választ biztonságos helyettesítést. Indokolt kis Node-referencia futtatható,
ha tényleges mainkódot használsz, és minden stub/forráshash/korlát dokumentált.

Ha porthiba: minimális policy/preview korrekció, a többi main-plan eset és
exclusion/equipment/safety/minimum3gyakorlat/B-nap/estimate guard megőrzésével.
Ha oraclehiba: csak igazolt exact maineredmény alapján regeneráld az érintett
fixture-t, ne kézzel töröld a rejected flaget. Test assertion maradjon érdemi,
biztonságos és nem teljesíthető profilon továbbra valódi elutasítást ellenőrizzen.
További RIR/mode/újtervgeneráló fejlesztést ne indíts.

JVM futás helyben nincs; a helyes tesztforrás és forrásalapú/reference bizonyíték
kell, végső GitHubkaput a fő munkás futtatja. A másik chat három baseline-tesztet
javít, azokat ne módosítsd. Ha plusz termékfájl szükséges, külön jelentsd.
