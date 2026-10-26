# TrainPilot – aktuális átadás

Frissítve: **2026-10-26**. A main alkalmazásbaseline-ja `7473b30`, dokumentációs HEAD-je `8a36502`; **#235/#237 mainen, készüléken elfogadva, változatlan 1.3.71 /2735**. Latest: `v1.3.71-perf-20261022`, Wear párja `wear-v1.3.71-perf-20261022`; az október 21-i eredeti kiadás megmaradt. A natív migráció külön ágon folyamatban; #214 karbantartási issue marad. [Takarítási szabályzat](REPOSITORY_CLEANUP_POLICY.md).
Először [AGENTS.md](../AGENTS.md), majd ez a fájl.
GitHub az elsődleges; a lentiek pontos forráshoz kötött eredmények.

## PR252–254 integrálva; UI-debug és új közös kapu – 2026-10-26

Legfrissebb app/teszt **f72c9cb0c8e336ab6cc4073d3005e7d30b3ed7fd**.
#252936e5df→f208617c Calendar11JVM+1UI; #2533d45d3ae→acd6927c onboarding7JVM+2UI;
#254c6366826→624aaf01 program6JVM+3UI. Külső exact riportok/delták integrálva,
main/külső PR-merge és verzióemelés nincs. Mindhárom korábbi foglalás felszabadult;
új külső delta csak az átadott HEAD óta review. Részletes root review/korrekció:
reviews/NATIVE_PR252_254_INTEGRATION.md. A07 globális main catalog-reset továbbra
nem igazolt teljes paritás; E15 Google revoke/Drive/calendar maintenance API-k
meglevők, Settingsből még nincsenek hívva (reviews/NATIVE_PR250_INTEGRATION.md).

**Korábbi b28672bf runtime:** 40291546367 telefon1096/0/0 és Wear167/0/0 valódi XML;
R8/signing/package/version PASS, signed upgrade mindkét régi kiadásból2/2PASS;
Wear round68+56+59=183PASS. Telefon UI69-ből csak18terminális:12PASS/6FAIL, majd
INITIALIZED→DESTROYED nav process crash,0screenshot. Journal feedback UI PASS.
Nem teljes telefonos kapu! Raw artifact12636867224.

Rootf72c9cb0: launch/nav explicit Main (log worker-thread navot mutatott), canonical
nyitó Main assert; lazy theme/record/interval scroll, substring progress assert,
valódi pihenő Skip a második stopwatch előtt. Új onboarding miatt meglévő app UI
fixture explicit complete, first-install welcome teszt megőrizve/izolálva; program
JVM runTest/Main pump +store cleanup. Calendar canonical/projection/ownership
schedule védelem Mainen, egyszer/preview.37Kotlin lexical/4locale XML staticPASS.

**Aktuális egyetlen canonical run40315675080**, exact appf72c9cb0:
https://github.com/fuloplevente1998/TrainPilot/actions/runs/40315675080
Indításkor IN_PROGRESS; ezt ellenőrizd, ne indíts másolatot. Új3csomag/korrekció
JVM/UI/R8/upgrade elfogadás még nincs. Felhasználó Extra High-t kért a CI-debugra;
Mediumra csak későbbi vizuális finomítás előtt jelezz. Élő adat fölé install STOP.

## #250 javított audit, új UI-bekötések és közös kapu – 2026-10-26

#250 exact **fb64307** két riportja byteazonosan beemelve **85f09350**.107 ID/sorrend
megmaradt;77 IMPLEMENTED_UNVERIFIED /9 PARTIAL /21 PLATFORM_DEVICE_PENDING,
0 runtime VERIFIED. A korábbi nyers UNRESOLVED problémák javítva, a bizonytalan
A07/A12/C10/E15/E19 caller-igazolások továbbra külön kezelendők. #251 későbbi
Health-integrációja felülírja az audit régi OWNED_BY_OTHER állapotát.
Review: reviews/NATIVE_PR250_INTEGRATION.md.

**7043fc7d**: B08/E17 globális időzítő Settings→VM→Store hatmezős editor, négy
nyelv, validáció/24h limit, explicit Save/Cancel. CAS+replacement epoch megőrzi
az újabb beállítást; aktív timer/frame érintetlen.4 új JVM **ténylegesen PASS**,
2 UI csak lefordult, még nem futott.
**bc18d019**: C03 utólagos hatfokozatú napló-feedback a meglévő editoron; explicit
mentés, eredeti ID/epoch/tombstone/detail CAS, applied-program feedback zárolva.
Unknown feedback/telemetry/photo mezők megmaradnak; régi suggestion törlődik,
nincs automatikus progression/finalizer replay.3 új JVM+1 UI forrás, UNRUN.

Közös40289777728 app7043fc7d **FAIL**: telefon/Wear debug és telefonos UI test-APK
fordult; valós XML telefon1093/4fail, Wear167/0fail. #25118+11 és Samsung grant3
új JVM PASS. A run javított Coach-harnesssel már végigért ~2perces unit lépésben,
nem akadt el. R8/signer/UI/upgrade jobok még nem futottak.

**b28672bf** javítja a négy hibát: Coach metadata REPLACE lenullázta a programnapokat
(FK cascade), most meglévő saveProgramWithDays atomikus republish; Home @Relation
trace két root catalog SELECT-et mutatott, most két flat program/day read ugyanazon
snapshot-tranzakcióban (a one-root/0-200-1500 assert változatlan); planner safety
fixture106elemes kizárása a100as validációs limit előtt bukott, most minden tényleg
eligible, valós,≤100ID kizárásával a safety guardot teszteli; témaassert a két
család vizuális fill/emphasis különbségét és mindkettő≥3 outline kontrasztját kéri,
nem szükségtelenül eltérő, már olvasható határvonalat.

Új canonical **40291546367**, checkout/appb28672bffe574506ed6c808c88d6b7a42c4ecd70:
https://github.com/fuloplevente1998/TrainPilot/actions/runs/40291546367
Indításkor IN_PROGRESS. Ezt ellenőrizd, ne indíts párhuzamos másolatot.

## Három új külső fejlesztési átadó – kiosztás előkészítve

Felhasználó további párhuzamos munkát kért; az átadók GitHubon **9dfb155a**,
kiinduló app **7043fc7d5fac91383f583332fd8fc0e9397f4a1c**, mind saját új ággal,
draft PR célmigration, központi CI/integráció. Indulásukat még nem igazolta.
- Codex: plans/NATIVE_CALENDAR_MULTIWEEK_HANDOFF.md – E02, csak Calendar/új planner.
- Plus1: plans/NATIVE_ONBOARDING_HANDOFF.md – A01, Home/nav +onboarding store-only.
- Plus2: plans/NATIVE_PROGRAM_ACTIONS_HANDOFF.md – A07, forrásigazolt copy/reset.
Pontos fájlhatár/új saját locale XML/teszt és no-build szerződés az átadókban.
Ezeket ne írjuk párhuzamosan; Root Journal/Health/szinkron/CI marad. Nincs belső
subagent, main/verzió/csomag/élő adatos install/PR-close változtatás.

## Samsung SDK fordítási fix és JVM-diagnosztika – 2026-10-26

40284818902 FAIL compileDebugKotlin: #251 grantScope Kotlin listájában nem létező
SDK DataTypes-hivatkozás és következményes típusinferenciás hibák. **cfa113be**
eltávolítja a második elevenelemű SDK-listát; SamsungSdkReader.Reader.grantedKeys
ugyanazt a már Java-adapterben létező types() mappinget használja, a Kotlin scope
csak ezt hívja. Három új valós SDK-permission object JVM-case megírva (all/energy/
empty), nem futott. A wrapper fizikai provider-sikert nem szimulál.

A meglévő canonical workflow unit lépése -PnativeTestDiagnostics kapcsolót kap:
app teszt start/end class+method+status (nincs useradat/token). Csak ebben a CI-
módban12perc Test-task felső korlát, a korábbi31perces csendes elakadás helyének
megtalálására. Nincs tesztkihagyás, új dependency vagy workflow. Normál build
viselkedése változatlan, a hibatűrés/assertszerződés nem lazult.

Új run **40285918450**, checkout/appcfa113be0383c5bb84f8ce7c6324e8dbbafdb1ef:
https://github.com/fuloplevente1998/TrainPilot/actions/runs/40285918450
Indításkor IN_PROGRESS. Kotlin/Java/diff static check nem helyettesít compile/JVM/
UI/R8/upgrade sikert. Ugyanezt a run-t ellenőrizd legközelebb, ne indíts másolatot.
#250 audit továbbra a külön chatben; Root egyik auditfájlt sem módosította.

## #251 utáni közös kapu – 2026-10-26

Új canonical run **40284818902**, checkout1da7a3fde75ae19662b6e8552366ff8f963bf241,
app4b1711b1 (utána docs): https://github.com/fuloplevente1998/TrainPilot/actions/runs/40284818902
Indításkor IN_PROGRESS. Új JVM/UI/signer/upgrade siker még nincs. Nincs duplikált
kapu indítandó; a következő kör e futás compiler/job/XML kimenetét olvassa.

A korábbi40276384595 logja: app:testDebugUnitTest05:53:34 után06:25:30 cancellation,
nincs telefonos XML vagy per-case progress. Az artifact12631797090 csak a Wear
28osztály XML-jét tartalmazta, összesen167/0fail. Ez nem telefonos tesztsiker.
Hosszú JVM-lépés lehetséges elakadását külön diagnosztikával kell ellenőrizni,
nem timeout vak növelésével/tesztkipipálással. Komolydebug előtt ExtraHigh jelzés.

## #251 Health sync review és integráció – 2026-10-26

A felhasználó #250-et visszaadta a külön audit chatnek, a két riport foglalt.
#251 exact91fb333 (app01bf93da) review után **4b1711b1**-ben integrálva;
14fájl byteazonos a külső forrással.30nap/full/HC change/deletions/dirty és
Samsung30/2, privát checkpoint/generation+revision és stale daily védelem.
10Kotlin lexical/4locale11key/diff static PASS;29JVM+1UI tesztkód runtime UNRUN.
Review/ismertkorlát: reviews/NATIVE_PR251_INTEGRATION.md; külső átadó exactcopy
reviews/NATIVE_HEALTH_SYNC_COMPLETION.md.500page/530dirty budget és partial-
grant utáni konzervatív checkpoint viselkedés külön ellenőrzendő.
Új közös CI indítandó ezen az appforráson, korábbi40276384595 CANCELLED nem zöld.
Main/version/package/élő install és külső PR-zárás nem történt.

## #249 integrálva; #250 folytatás vissza a Plus chatnek – 2026-10-26

#249224999b → **654c016c**: kontraszt Oklab/alpha alapú token deriváció,
fókuszos/selected picker outline és4JVM-kód beemelve; runtime UNRUN.
#250a360a5a két auditfájl byteazonos importja **be5e2642**.107sor/80UNRESOLVED,
main JS connector méretkorlát miatti BLOCKED; ez nem107igazolt hibajegy.
A felhasználó a #250 auditot ugyanabban a chatben folytatja; Root ne javítsa
párhuzamosan a két külső auditfájlt. Pontos új prompt:
plans/NATIVE_PR250_AUDIT_CONTINUATION.md. Natív auditedrefbe5e2642/app654c016c.

Hiányzó main-input pótlása0fa3f8ea: docs/audit-input/main-7473b30/INDEX.md és
manifest.json +93max24000byte-os teljes sorhatáros rész;181szekció indexszel.
Numerikus összefűzés BYTEAZONOS main7473b30:www/app.js, Gitblob ésSHA256 ellenőrizve.
Exact eredeti sorszámok, nem kivonat/új implementáció. Source blank EOF megőrzött.
#250 feladata végső JS override/caller és Kotlin symbol/UI-chain feloldása,
valódi PARTIAL vs runtime hiány szétválasztása; a termékfix Rootnál marad.
Közös40276384595 CANCELLED a JVM lépésen; nincs újruntimezöld/hibadiagnózis.
Main/version/új CI/élő install ebben az audit/input körben nem változott.

## Új munkamegosztás: 1 Codex +2 Plus chat – 2026-10-26

A felhasználó felülírta a Gemini/AI Studio kiosztást: Gemini most nem kap munkát.
A fő munkás integrátor/reviewer és a közös CI gazdája marad. Három teljes standalone
prompt pinned e9278a9905629d30d012083e8dd3a4c76051a464 bázison (app3d45a324):
- plans/NATIVE_HEALTH_SYNC_COMPLETION_CODEX_HANDOFF.md: külön Codex nagyobb
  Health30nap/full/incremental implementáció, saját feat/native-health-sync-completion.
  Health repos/JournalStore sync API/Refresh/HealthDashboard/gate fájlok foglaltak.
- plans/NATIVE_FINAL_FEATURE_INVENTORY_PLUS_HANDOFF.md: PlusA teljes main→natív
  funkció/UI/test/bizonyíték matrix, csak két saját riport írható, audit/native-final-feature-inventory.
- plans/NATIVE_THEME_CONTRAST_PLUS_HANDOFF.md: PlusB teljes katalógus/numerikus
  kontraszt és minimális token/picker javítás, fix/native-theme-contrast-parity;
  theme2fájl+NativeThemePicker és új saját teszt/resource/report, store/mapper readonly.

Mindhárom saját draft PR célmigration; main/version/schema/workflow/élő install
változatlan, nincs új belső subagent. Indulás még nem visszaigazolt, átadott írási
területeket Root ne írja párhuzamosan, kivéve igazolt szükséges közös CI-fix külön
koordinációval. Más meglévő fájlra csak külön patchjavaslat. Minden promptban
exact raw linkek, kötelező fájllista és DONE/BLOCKED/bizonyíték szerződés.
Közös40276384595 CI továbbra integrátornál; kiosztás miatt új kapu nem indult.

## CI-import javítás és tényleges hátralévő kapuk – 2026-10-26

A40275657585 futás FAIL: compileDebugAndroidTestKotlin, NativeTrainingModesUiTest
4helyen unresolved getOrNull. Phone/Wear appfordítás továbbjutott, JVM/UI/upgrade
futási elfogadás nem történt. **3d45a324** pótolja az androidx.compose.ui.semantics.getOrNull
importot; nem üzleti kód vagy assertgyengítés. Új canonical CI **40276384595**
(https://github.com/fuloplevente1998/TrainPilot/actions/runs/40276384595), checkout/
app3d45a324c5d31c058353a0e4316e02c9a9133520, indításkor IN_PROGRESS.
Ne indíts másolatot, ugyanennek a job/XML eredménye a következő lépés.

Az öt párhuzamos csomag #244–248 beépítve. A #242 audit mind7 konkrét hiányához
van most kódos bekötés, ez nem teljes main-paritás vagy runtime-elfogadás.
Hátra: közös JVM/Compose/Wear/R8/signer/emulátoros upgrade; teljes A–E/main/24nézet
funkcióleltár és vizuális finomítás/valódi képek;30napos/incremental health sync;
valós OAuth/provider/permission/offline és processdeath/cross-device/cloud-photo
bizonytalan befejezések; adatmegőrző upgrade és fizikai Phone/Watch7 elfogadás.
Élőadat-fölé telepítés még nem igazolt. Párhuzamosítás időnyereségét nem mértük;
csak elkülönített csomagszám/integráció és konkrét teszteredmény tényadat.

## Közös natív CI elindítva – 2026-10-26

Actions **40275657585**: https://github.com/fuloplevente1998/TrainPilot/actions/runs/40275657585
Workflow native-android.yml, manual dispatch, checkout71f16cde1d7182f2d1964a219f50e7c44052527c;
alkalmazásforrás e344fed5, a checkout óta csak docs. Állapot indításkor IN_PROGRESS,
siker/tesztszám/APK/upgrade még nem igazolt. Ne indíts párhuzamos másolatot vagy
runtime watch-loopot; ugyanennek a futásnak job/XML eredményét vizsgáld legközelebb.
Komoly hibakeresés előtt jelezni Extra High-ra váltást, a felhasználó állítja.

## #245 / #247 / #248 integrálva – 2026-10-26

A felhasználó új külső eredményeket jelzett; két új PR valójában #247 és #248.
- #245305b560 → **7d773190**: mind az öt review-javítás ellenőrizve,28 appfájl
  byteazonos a külső HEAD-del. Public/internal, IO→Main save/undo és canonical
  commit, global scope és pure-RIR main-paritás rendezve;35 JVM+6 UI kód UNRUN.
- #24720b7ddb → **6a5b6588**: három test-only fixture/scheduler/querytrace fix;
  Home rootSELECT=1 megmarad, trace eredete csak az új futással dönthető el.
- #248aa01f3b → **e344fed5**: valódi main validateProfile elutasítja a nem létező
  squat/air-squat exclusionID-t. Native guard+3 regresszió; fixture nem gyengült.
  Hash-ellenőrző Node referencia valóban futott Rootnál PASS; validator only,
  nem teljes main generator/JVM. Git diff/Kotlin lexical/XML static PASS.

**Aktuális appforrás e344fed5.** #2444eae08c8 és #246cf1db70b is bent.
A régi „#245 új távoli SHA nincs” és „#247/#248 még nem indult” státusz történeti.
Külső PR-ok nyitott draftok, nincs GitHubmerge/close/main/verzió/élő install.
Következő a canonical native-android.yml közös integrált futása; a korábbi
hat hiba egyikét sem jelöljük zöldnek az új XML nélkül. Teljes A–E/design/
provider/backfill és fizikai elfogadás külön nyitott, ez integrációs kapu.

## #244 beépítve és két új külön chat átadó – 2026-10-26

**Aktuális appforrás4eae08c8:** #244b062e3d javított23fájlos delta integrálva;
#246cf1db70b megmarad. Dupla baseline javítások egyszer, planner fixture neve
cases-main-7473; külön új health workflow nincs átemelve.23fájl byteazonos a
külső review HEAD-del;19Kotlin lexical+4XML/diff PASS, runtime/új CI nincs.
reviews/NATIVE_PR244_INTEGRATION.md. #245 új távoli javítására továbbra várunk.

A felhasználó új chateknek kér munkát, két elkülönített prompt készült pinned
4eae08c8 bázison: plans/NATIVE_BASELINE_HARNESS_HANDOFF.md (három test fixture/
scheduler/querytrace), plans/NATIVE_PLANNER_PARITY_FIX_HANDOFF.md (planner
main-plan-7 oracle/port). Indulás még nincs visszaigazolva, de kijelölt fájljaik
az átadás idejére foglaltak; root ne módosítsa őket párhuzamosan. Saját branch/
draftmigration, nincs belső subagent vagy önálló teljes CI. A korábbi6 közös
hibából Coach/Consent javítás bekerült #244-gyel, a maradék4 vizsgálata e két
csomagé; mindegyik végső runtime-elfogadása az integrált gate-en marad.

## Ellenőrzött aktuális állapot – 2026-10-26

| Rész | Távoli állapot | Migrációs integráció / elfogadás |
|---|---|---|
| #246 kardió | e482cd7 | Beépítve cf1db70b; teljes új közös gate hátra. |
| #244 Health auto-refresh | b062e3d (app/teszt7cb2c11) | A P1 dispatcher és P2 ZIP cleanup javítás source-deltája ellenőrizve; integráció még hátra. |
| #245 edzésmódok | 2ff2985, változatlan | Öt review-javítás nincs a távoli ágon; nem integrált, függőben. |

#244 Actions40243229800 SUCCESS. Root most a valódi XML-ekből is ellenőrizte:
**30/30 JVM +4/4 emulátoros UI PASS**,0hiba/skip (artifact12617517971/12617558453).
App/teszt7cb2c11→b062e3d csak3docsdelta. A főszálas execution probe a metadata
lease előtt fut, az IO prepare/callback már nem hívja a main-only store-t;
ZIP transactionDir a try/finally alatt, hibainjektálás és új production callback
regressziók hozzáadva. Fizikai HC/Samsung és30nap/full/incremental parity továbbra
nyitott. A célzott zöld nem közös release gate.

A felhasználó mindkét rész elkészültét jelezte. #245-öt REST, cache nélküli REST,
git fetch és ls-remote alapján is ellenőriztük: továbbra2ff2985, PR updated_at
2026-10-25T21:28:35Z; más új nyitott mode PR nincs. A kért állapotban ezért nem
jelölhető javítottnak. Következő: #244 átfedő baseline-fixek összevezetése a már
integrált #246-tal; #245 tényleges új távoli delta review, majd integrált gate.
Ebben a körben csak státusz/docs frissült: appforrás cf1db70b változatlan,
main/verzió/PR-lezárás/élőadat-telepítés és új CI nem történt.

## PR246 beépítve, PR245 javított forrása még nem távoli – 2026-10-25

A felhasználó engedélyezte a review-t és migrációs összeépítést.
**Appforrás cf1db70b:** PR246 exacte482cd7 teljes25fájlos appdelta squash-integrálva.
Kardió Room/progress és review6 lifecycle/date/timezone fix +5 JVM-tesztkód.
Korábbi11binding+15model XML PASS e9eb8cb-re; az új5 JVM és3 UI/integrált változat
még nem futott. Diff/XML/20Kotlin lexical PASS, nincs új CI/Gradle/APK vagy main merge.
Részletek: reviews/NATIVE_PR246_INTEGRATION.md. A korábbi „Room+UI nincs” történeti
állapotot ez felülírja. Új hozzáadott tesztkód16 JVM+3 UI; mathmag15 változatlan.
PR245 REST+git ls-remote még2ff2985: öt review-javítás nem érkezett a távoli ágra,
ezért nem emeltük be. PR244 két review-javítása külön várható. A közös hat baseline
hiba és az integrált natív gate hátra; main/verzió/élő adat install STOP marad.

## #244 készre jelentve, review két javítást talált – 2026-10-25

Exact külső HEAD8c00b46, app/teszt5d89db4 (csak docsdelta). Review:
[ NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md ](reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md).
Root valós XML-ből ellenőrizte40239961158:29 JVM+1 Compose UI PASS,0fail/skip;
merge-ref17eedd7 app/Wear/shared/workflow forrásazonos. A production útban P1:
VM IO-prepare és SDK-callback safe()→main-only PhoneWorkoutExecution.store→
catchfalse, így AUTO és MANUAL providerfrissítés is blokkolódik. A zöld tesztek
csak pure eligible +kapcsoló/persistence útját fedik. P2: ZIP-import begin után
transactionDir mkdir még try/finally előtt; IO-hibánál gate számláló beragad.
Külső munkásnak e két fix és valódi IO-entry/staging failure regresszió adható.
Nincs root appjavítás/integráció vagy új CI. Közös40239961291 még futott a review
végén.30napos/incremental providerparitás külön nyitott. Új workflow/baseline
átfedések integrációja külön egyeztetendő, nem vak merge. Main/version STOP.

## #244 részátadás és további munkamegosztás – 2026-10-25

A felhasználó jelezte a #244 részbeni elkészültét, új Codex-munkát nem akar
indítani a tokenköltség miatt. Exact HEAD5d89db49f57eb0867a526f522e201b4d3d72c8f9,
nyitott draft célmigration. Root a40239961158 aktuális állapotát ellenőrizte:
Health JVM test contract SUCCESS, Real Health screen/preference UI IN_PROGRESS.
Ez nem teljes forrásreview, nem integráció és nem közös zöld kapu. A PR-leírás
még régebbi natív runra hivatkozik, a képernyőkép/új run és HEAD a friss adat.

Tokenkímélő javaslat: a három meglévő chat saját javítás/célzott próba körét
zárja: #245 review1–5; #246 review6 és26 kardiós JVM; #244 folyamatban levő UI
és részletes átadó. Hat közös baseline-hiba integrátornál marad; ne javítsa mind
három chat párhuzamosan. Új teljes CI csak összeépített forráson, nem három
másolaton; Actions önállóan futhat, nincs szükség folyamatos agent watch-ra.
#244 által jelzett rf250Sync30nap/incremental backfill-paritás továbbra nyitott,
a foreground legújabb napi frissítés nem az egész egészségszinkron befejezése.

## #245/#246 forrásreview kész – 2026-10-25

Review: [NATIVE_PARALLEL_PR245_PR246_REVIEW.md](reviews/NATIVE_PARALLEL_PR245_PR246_REVIEW.md).
Exact #2452ff2985-ben öt javítandó pont: public VM/internal Preview láthatóság,
IO→main-only mode-save/undo, IO→canonical weight commit, global picker helytelen
programoverride és mainhez képest megengedőbb pure-RIR policy. #246e9eb8cb-nél
nap/hét/timezone-resume invalidation hiányzik. Kardiós11+15 JVM PASS **valódi XML-ből
ellenőrizve**. A40238833748 merge-ref62cc08f app/Wear/shared/workflow fái azonosak
a külső HEAD-del; eltérés csak docs. Phone984/6 fail, Wear167/0; UI/upgrade skipped.
A hat hiba okait/nyitott vizsgálatait a review külön kezeli; discard fixture már
prepare revision1 vs record revision0 miatt elbukik a tényleges discard előtt.
Root kizárólag review/dokumentáció: nincs appjavítás, integráció/merge vagy új CI.
Következő: izolált hibajavítások, exact új delta, #244 külön review, integrált gate.

## Két külső eredmény visszaérkezett – 2026-10-25

- Draft #245, `feat/native-training-modes`, exact
  `2ff2985c4376e2a7ee946c73e6523c65a983a980`: implementáció és33 JVM+3 UI
  teszt megírva a PR szerint; fordítás/futtatás még nincs.27 android/app fájl,
  CoachScreen-bővítés külön felhasználói engedélyét a külső átadás jelzi.
- Draft #246, `feat/native-cardio-progress-ui`, exact
  `e9eb8cbd52d83ab86327f60dbf6193d41e681396`: kardió Room/UI késznek jelentve,
  debug fordítás és11 binding+15 model JVM zöld a külső átadás szerint.
  Root ténylegesen ellenőrizte a legutóbbi Actions40238833748 futás naplóját:
  **984 teszt /6 fail**, nem a képen szereplő korábbi7. Hibás: CoachVariation,
  GoogleConsentBridge, HomeRepository, MetadataGate, PlannerTemplateParity,
  WorkoutDiscard. A Wear UI, Phone Compose UI és signed upgrade job skipped.
  Közös zöld nincs. A külső PR a kijelölt feature-fájlokon kívüli fordítási/
  fixture-javításokat is tartalmaz: ezeket integrálás előtt külön review-zni kell.
- Harmadik draft #244 Health foregroundauto, exact
  `023d411409c808d2512b0120d1c18b338bc54927`; a felhasználó még nem jelentette késznek.
- Ez **PR/meta/CI-log ellenőrzés**, nem teljes forrásreview vagy integráció.
  Mindhárom PR nyitott draft, célmigration. Root appforrás továbbra880026f;
  a külső ág futása nem a migrációs ág tesztbizonyítéka. Nincs merge/lezárás/
  új CI/APK vagy élő telepítés. A felhasználói szünet marad; folytatáskor exact
  diff és a plusz baseline-javítások review-ja következik, nem vak merge.

## Három új párhuzamos átadó – 2026-10-25

Felhasználó Sol6/5.6 külön chateknek kér promptot, teljes GitHub-hozzáféréssel.
Három standalone MD készült pinned **0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20**
bázisra. A felhasználó2026-10-25-én megerősítette: mindhárom külön munka
elindult; új PR/eredmény még nem érkezett. Az5órás limit miatt Root szünetel,
később a visszaérkező PR-ok exact review-ja/integrációja következik. [A kardió bekötés](plans/NATIVE_CARDIO_BINDING_HANDOFF.md),
[B módok/progresszió](plans/NATIVE_TRAINING_MODES_HANDOFF.md),
[C foreground autoHealth](plans/NATIVE_HEALTH_AUTO_REFRESH_HANDOFF.md).
Saját három ág, draft célmigration, köztes skipci, nincs main/version/schema/
workflow változtatás vagy külső teljeskapu. Pontos fájlhatárok az MD-kben;
Root integrátor és a külön csomagokon kívüli Journal/mainleltár marad.
Korábbi #243/Gemini/Flash eredmény mind már beemelve/review-zva. Nem belső
subagent-felhatalmazás. A globális issue197 mainen zárt, a mostani cél natív
portparitás; nem újra lezárandó vagy szenzorfejlesztés.

## IO programmentés guard – 2026-10-25

19ddf80/880026f: a NativeProgramEditor guard immár Main.immediate-en olvassa
az execution store-t, miközben a UI mentés/törlés IO-n marad. Új1 tényleges
IO-entry regressziós JVM-kód, még nem futott. Aktuális kód880026f, batch580 JVM+
52 Phone UI UNRUN. A lentiek részletes korábbi blokkok, nem új futási eredmények.

## Home/HC bekötés, #243 és Gemini/Flash eredmények – 2026-10-25

Aktuális alkalmazás-/tesztforrás **880026f**. Home e915bd9 a meglévő Coach-
kiválasztást és teljes program/day/schedule starttuple-t használja, valós Room/
settings invalidationnel, helyi resume/date/time/zone frissítéssel, IO/cancel és
üres/hiba állapotokkal. Scalar helyinapi count marad; nincs A/4/defaultprogram
kitalálása.5 új JVM+1 UI; nem bizonyított gyorsulás, nem provider auto-sync.

0a69035: Journal Source külön Wear és HC mérőpanelek/zero/missing/stale/partial,
explicit valós readTrainingWindowSuspend. A providerolvasás előtt rövid prepare,
IPC lockon kívül, utána epoch/window/HC CAS és canonical latest-row mentés,
aktív/tombstone guard. Közben történt set edit megmarad; Wear-window/energia
külön, nincs total+active összeadás.22 új string4nyelven,7 JVM+1 UI új kód.

Külső #243 exact1bf745e engedélyezett8fájlos delta →1864a8c; rootc611cc7
picker-lifecycle/locale/48dp és UI-fixture/async-firstload javítás.7 JVM+1 UI új.
Gemini teljes két .kt →5aacc8a, main-paritás korrekció0f8bb70: hibás patch nem
alkalmazva. A pure cardio mag streaming/all-time/8hét/pace/records, **Room+UI
még nincs**.9 külső+6 saját JVM-kód. Exact main JS6fixture valóban futott Node-ban;
ez referencia, nem új Kotlin működési bizonyíték. Két nyelvi audit forrással
ellenőrizve,23 szöveg2ff85ad-ben javítva; DST-offset és runtime-állítások nem
átvéve vakon. Eredeti auditok és integrációs döntések a reviews könyvtárban.

**Batch580 JVM+52 Phone UI NEM FUTOTT**. Lexical/diff/XML/key/format/R-ref és
hívási/lock/CAS/SQL szerződés ellenőrzés volt. Nincs új Kotlin/Room/Compose/
SDK-futás, CI/APK/kép; dd4cc27/40072751592 az utolsó teljes zöld. Az audit7
hiányából4 kódban pótolva; cardio bekötés, modes és foregroundauto nyitott,
plusz a teljes A–E/design/Google/cross-device/platform elfogadás. Következő fő
kód a cardio bounded DAO/projekció +Fejlődés UI, a review korlátaival.

Minden külső részfeladat beérkezett; korábbi fájlfoglalások véget értek. #243/#240/
#241/#242 draftok nem merge-elve/lezárva; új külső commitot külön exact diffből.
Main/verzió/élő install STOP marad; High kód/bekötés, vizuális polish/CI-debug
fázisváltást továbbra jelezni kell. #238 leírás az aktuális HEAD-re frissítendő.

## Naplószerkesztés és két elkülönített átadó – 2026-10-25

**1a9b6d3 /6856abe**: a meglévő Journal részletekből izolált szerkesztő nyílik:
idő, gyakorlat/szett add/remove, súly/ismétlés/idő/kétoldali/táv/done/RIR.
Explicit Save → meglévő bounded PR és canonical Room mapper egy tx-ben;
exact-ID parent+relations CAS, dataset ticket, tombstone és azonos active/
finalization védelem. Main-looper snapshotcheck; más aktív edzést nem replay-el.
Új katalógusgyakorlat unitváltozása stale hibát ad. ID/sync/key/schedule és
megmaradó setUUID/route/fotó/Health megmarad; single-row exporter a backupban is
megőrzi az unknown exercise/set/photo/feedback mezőket. Időváltozáskor eredeti
Health-window jelölés; nincs átcímkézett mérés. Változatlan időknél duration és
szubmásodperc marad. Hibás legacy dátum megnyitás előtt error.20 új resource
négy nyelven, kézi dátummező explicit offsettel; még nem végső vizuális polish.

12 új JVM +1 valódi appos UI-case kód. **Batch545 JVM+49 Phone UI NEM FUTOTT.**
Lexical/XML/kulcs/R-ref/adatút/tx-sorrend és exact-ID SQLite index check volt,
nem Kotlin/Room/Compose futás. Nincs új CI/APK/screenshot, zöld dd4cc27 marad.
A két audit-P1 kódban pótolva, öt P2 nyitott, összes elfogadás továbbra hátra.

Felhasználónak másolható prompt/raw link átadva: [külön ChatGPT – date range](plans/NATIVE_HEALTH_DATE_RANGE_HANDOFF.md)
és [Gemini – kardió számítás](plans/NATIVE_CARDIO_GEMINI_HANDOFF.md). Mindkettő
pinned9a18ca6, saját branch/tiszta fájlhatár; indulás/eredmény még nem igazolt,
belső subagent nincs. Fő munkás következő konkrét része Home target/start/
invalidation, utána workout HC-panel/refresh és modes/foregroundauto. Követő #238
frissítendő az új branch HEAD-re. Main/verzió/élő adatos telepítés STOP marad.

## PR242 audit átvéve, P1 programcélértékek bekötve – 2026-10-25

Külső audit draft #242 **9c3c264**, csak egy dokumentum, pinned main7473b30 /
nativea5f562d, Home e4920f4 delta figyelembe véve. Az eredeti riport byte/hash-
pontosan átemelve reviews/NATIVE_FEATURE_PARITY_AUDIT.md-be; az aktuális rövid
[triage](reviews/NATIVE_FEATURE_PARITY_TRIAGE.md) legyen a belépés, ne a hosszú
permalinkes riport teljes újraolvasása. Hét hiány forrásszinten megerősítve:
P1 journal edit és program Rx, P2 cardio, workout HC, modes/progression,
Health foreground auto és arbitrary date range. Nem futtatott reprodukció.

**b161126**: a program szerkesztőben per-exercise Célértékek popup, sets1..10,
reps/idő1..40karakter, weight0..10000, rest30..300, tizedesvessző és explicit
üres próbasúly a main loadType-jainál. 17 új resource négy nyelven. Program-
szintű hatás minden occurrence-re egyértelmű; child Apply csak izolált drafthoz,
parent Save az egyetlen commit. Inline override explicit mezői és global
prescriptions együtt frissülnek, egyéb variation/RIR/unknown és string ID-k
megmaradnak. Beépített terv klón, program/day CAS/active-workout guard marad.
Katalógus loadType/repUnit/measurementType változáskor stale Save elutasítás a
Room tx-ben. Valódi settings-rest fallback a VM-ből, nem tetszőleges UI-szám.
A NativeWorkoutSession ugyanazt a global→inline adatot olvassa; tp228 prefill
policy és aktív/mentett sorozatok nem változtak. **97f18fd** egyszeri entry-
csoportosítás/parsed-Rx reuse, nincs teljes programparse minden szerkesztett sorhoz.

11 új JVM-kód (5 validáció/precedence/opaque,5 Room atomic/cloning/roundtrip/
unit-CAS és1 actual program start), meglévő valódi app UI-case invalid/child
Cancel/parent Save/reopen/outer Cancel/no-write bővítve. **Batch533 JVM+48 Phone UI
NEM FUTOTT**, nincs új UI-esetszám. Csak lexical/diff/import/XML/key/format/R-ref,
main tp107/tp152 szabály/adatút/commit-sorrend ellenőrzés volt; nem Kotlin/Room/
Compose/Google. Nincs új CI/APK/kép, teljes zöld továbbra dd4cc27/40072751592.

Az auditból Rx kódban pótolva, futtatás/elfogadás hátra; hat további hiány nyitott,
plusz a teljes A–E/24nézet/Health/Google/device kapu. Külső ágra nem írtunk,
#242 draft nyitva, nem GitHub-merge. Következő P1 journal edit és kapcsolt HC-
megőrzés/refresh; Home target/start/invalidation és többi nyitott tételek maradnak.
Main/verzió/élő adatos telepítés STOP változatlan, továbbra High kód/bekötés.

## Kis Home-számláló javítás és külső audit átadva – 2026-10-25

**e4920f4**, [skip ci]. A Home „Mai edzés” csempéje most helyi napkezdettől a
következő nap kezdetéig számol, half-open epoch-tartományú, indexed scalar Room
COUNT-ból, history tombstone-kizárással. Nem teljes naplócount, nem fix24óra,
nincs reláció-/fotó-/setbetöltés. Health-read hiba nem írja felül nullával a már
ismert edzésszámot; cancellation nem nyelődik el. Célnap/azonosítók és automatikus
adat-invalidation ebben a kis lépésben nem változott.

Három új JVM-próbakód: lokális éjfél/UTC eltérés, DST23/25h és ismételt02:30,
tombstone ID/sync/key. **Batch522 JVM+48 Phone UI NEM FUTOTT**. Actual DAO SQL
SQLite boundary/DST/tombstone/index ellenőrzés és lexical/diff PASS; nem Kotlin/
Room/Compose, nincs új CI/APK. Main/verzió változatlan, továbbra High-kódolás.

A felhasználó a funkcióauditot **átadta a külön chatnek**. Ág/PR/riport még nem
érkezett vissza, eredményt nem állítunk. [Feladat](plans/NATIVE_FEATURE_PARITY_AUDIT_HANDOFF.md):
csak docs-audit, pinned a5f562d/main7473b30, javasolt test/native-feature-parity-audit.
Ott a count-hiba még látszik a bázison; átadáskor az e4920f4 deltakód alapján
javítottnak jelölhető. A fő munkásnál marad a Home/Coach/Naptár/cloud fejlesztés.
Következő: meglévő NativeCoachTargets bekötése, programId/dayId/scheduleId start
és tényleges invalidation. Nincs új külső vagy belső agent indítva általunk.

## Felhős fotótörlés helyi admission és draftképek megőrzése – 2026-10-25

**7d62ae3**, [skip ci]. Korábbi photo-delete epoch-check csak a HTTP indításának
pillanatát védte; a már induló törlés alatt új ref vagy restore kerülhetett be.
NativeCloudPhotoDeletion a friss Room scalar live-ID/Drive-alias, nested imported
draft és public/native/ownership snapshot ellenőrzés után, a meglévő metadata
lease és ownership→ActiveStore→canonical store monitorsorrend alatt regisztrál
per-photo reservationt. Az összes production canonical write ugyanazt a
PhoneWorkoutExecution singletonstore-t használja (PhoneWorkoutEngine.kt).

A HTTP ezután e lockokon kívül fut. Amíg a fizikai szinkron hívás fut, explicit
restore/recovery/erase gyorsan elutasítva, és ugyanazon ref nem köthető vissza
sem Room import/save, sem canonical/public frame mentésben. Ordinary settings és
más/új UUID-s workout elérhető; nincs restore/workout-start fence a hálózat alatt.
Native frame guard csak root photos listát néz, nem set/GPS fát minden ticknél.
Regisztrációt is körülvevő finally engedi el a reservationt: dispatcher-return
cancellation sem szivárogtathatja. Transport error után látható hiba és újrapróba.

Külön adatmegőrzési javítás: a stage/archive photoRows közös collectorába a
history mellé a draft és draft.session fotói is bekerülnek. Aktív draft-ref
ugyanazon ID history-tombstone-ja mellett élő: helyi kép nem kerül törlési tervbe,
és verified ZIP incoming/manifest/hash/redo megőrzi. Nem gyűjtjük a könyvtári
asseteket progress-fotónak. Importált deletedAt közös photoDeleted helperből:
a numerikus0.0/false/null/blank nem hamis törlés a Room sorban sem.

**14 új JVM-próbakód, NEM FUTOTT; batch519 JVM+48 Phone UI.** Élő Room/alias,
zero marker, draft/native/mirror, in-flight restore tiltás+metadata elérhetőség,
import/frame rebind rollback+új UUID start, fizikailag futó callback cancellation,
network error/retry, malformed draft, stale ticket, aktív draft stage és valós
ZIP redo bytes esetei. Régi stage-deletion próbák az új suspend callbackhoz
igazítva, ugyanazok az elvárások. Helyben csak actual DAO SQLite17 eset PASS,
lexical delimiter/import/diff, singleton/callsite és forrássorrend ellenőrzés;
nem Kotlin/Room/Google/emulátor. Utolsó teljes zöld továbbra dd4cc27.

Határ: processz-szintű foglalás, nem tartós szerverzár. Timeout/processdeath után
szerveren bizonytalanul befejeződő kérés, cross-device/older-peer verseny és
orphan upload/read-back további protokoll-/valódi Google-igazolás. Nem állítjuk,
hogy minden elosztott törlési verseny lezárt. Nincs új CI/APK/screenshot.

Felhasználó rákérdezett a módra: továbbra High kód/bekötés; Medium vizuális
finomítás előtt, Extra High komoly végső CI-debug előtt jelzendő. Következő kód:
Home count és ajánlott programnap a meglévő NativeCoachTargets-ből, teljes
programId/dayId/scheduleId start, adat-invalidation, majd A–E/UI/Health leltár.
Csak migrációs ág, változatlan verzió/package, main/élő install STOP.

## Lokalizációs integráció és Google consent tényleges bekötése – 2026-10-25

**3b548ac**: draft #240 exact82883ab squash integrálva a migrációba, a külső ágra
nem írtunk. 20 scoped fájl, négy új JVM-kód. Read-key készlet/ordered JSON-write
és feedback adatkulcsok, testTagek megmaradtak, négy XML kulcs/formátum/R-ref rendben.
Első túl szigorú helyi source-audit a teljes régi bázist/duplikált locale-olvasások
számát is mérte; szűk exact integration-parent és read-key set/write-order újra-
ellenőrzés sikeres, nem app-regresszió és nem futtatott Kotlin-teszt.

**6cf13be**: #241 exactab25d84 négy új fájl/hat Compose-eset squash integrálva,
majd közvetlen DataStore-nyelvírás helyett valódi picker-kattintásra javítva.
Settings LazyColumn testTag+scrollToNode szükséges a nagy témapaletta alatti
profilhoz; meglévő fő profil/nyelv UI-próbák is ezt használják. Home programnév
és napnév a tárolt HU/EN/DE/RO mezőből fallbackkel, nincs eldobott egységmező:
látható steps/workouts/recovery caption és stabil metric/name/day tagek. A hat
próba ezek tényleges UI-ját is nézi, többértelmű fejléc-presence nem single-match.

**547c2f8**: a valódi Bejelentkezés gomb connectGoogle-ot hív; authorizeAndRun
végig egy Google-gate alatt tartja auth→consent→profil→közös régi sync-mag útját.
NativeGoogleConsentBridge VM-élettartam, cancellable deferred + exact attempt ID,
nincs token-lemezírás. Compose per-ID launcherkey, ActivityResult; forgatáskor
nincs újralaunch, stale/duplicate/cancel/recreated-process eredmény nem folytathat.
Korábbi request9001/9002/GoogleAuthOutcome út eltávolítva; meglévő connect/Drive/
calendar mag kiemelve (azonos érdemi kód), nem párhuzamos rendszer. Kilenc új
consent/kapu/cancel JVM-próba; négy nyelven kilenc új Google-felirat. Még nem futott.

**87b593b**: dataset-ticket a consent előtt; restore/erase közben nem persistálhat
régi consent profilt/calendar cache-t. Userinfo/HTTP a NativeDataReplacement lease-en
kívül, prefs commit csak rövid mutate+ticket check alatt; calendar következő op/
siker előtt is ellenőriz. Két további nem futott JVM-próba. Már elindult Google/
calendar/photo HTTP nem visszavonható e checkkel, nem elosztott tranzakció.

**Batch505 új JVM+48 Phone UI tesztkód megírva, NEM FUTOTT**: korábbi490+4
külső resource+9 consent+2 ticket; korábbi42+6 külső UI. Helyben delimiter/diff/
XML/kulcsütközés/paraméter/import/R-ref/forrássorrend és kiemelési diff, nem
Kotlin/Room/Compose/Google. Migrációs utolsó teljes zöld továbbra dd4cc27/
40072751592; új CI/APK/screenshot nincs. Mind [skip ci], main/verzió változatlan.
Két külső draft nyitva marad, nem GitHub-merge; integrált exact SHA-k után új
külső változást külön kell vizsgálni. [Integrációs review](reviews/NATIVE_LOCALIZATION_INTEGRATION.md).

Következő: remote delete vs. új referencia/explicit restore, bizonytalan upload és
cross-device próba; teljes A–E/main-UI/Health leltár (Home count/ajánlott nap és
maradék VM-feliratok is), valódi Google-fiókos/grant/cancel/rotation/processdeath
próba, majd közös natív kapu, valódi képek és készülékes elfogadás. Élő adatos
frissítés továbbra tiltott, nincs main merge vagy verzióemelés.

## Google műveleti kapu, publikálási sorrend és valódi nyelvválasztó – 2026-10-25

**1839dd0**: GoogleSyncRepository minden auth/connect/revoke/sync/törlés/
disconnect belépése egy közös processz-szintű atomikus gate-et használ minden
repo-példányon át. Elutasít, nem queue-zik késői fiókváltás/törlés. Busy finally
felszabadul, cancellation nem lesz Result.failure/siker. Prefs profile/clear
commit hibája látható; nem kapcsolt/hiányos fiók még userinfo előtt tiltott.
A korábbi stabil-sub eltérésellenőrzés megmarad, email átnevezés nem tulajdonosváltás.
+17 megírt, nem futott gate/account/repository-admission JVM-eset.

**7e8f1e5**: a Drive snapshot feltöltése a sikeres canonical guard és existing
BackupRestoreManager ZIP-redo acceptance UTÁN következik, nem előtte. Hálózat
lease-en kívül. cloudBase csak publisher/read-back siker után; offline/cancel
esetén helyben elfogadott adatok + régi base megmaradnak a következő merge-hez.
Immutable payload-másolat. NativeDataReplacement epoch continuation ticket
megakadályozza, hogy időközbeni restore/erase után régi base/status íródjon vagy
új remote-delete induljon. Ordinary köztes edit megmarad, base a publikált
pillanatkép, nem a későbbi helyi állapot. +7 valódi Room/fájl/importer-hibafázis
és2 ticket JVM-próbakód. Nem elosztott tranzakció: már futó HTTP-delete nem
vonható vissza a ticket miatt; orphan upload/read-back és új ref/restore verseny
még nyitott. Token/fiókhoz tartozó Google pending consent lifecycle sincs kész.

**a63a1f1**: a user #241-visszajelzése alapján a hiányzó látható HU/EN/DE/RO
választó bekerült a Settings tetejére. NativeLanguagePicker rádiószemantika,
48dp minimum/FlowRow, téma-tokenek/autonymok, settings_language_<code> tagek,
négy külön native_language.xml. Meglévő VM.setLanguage/DataStore út; replacement
alatt tiltás. A meglévő settingsThemeAndLanguageChangesReturnToTheirOrigin UI-
próba most a valódi választóra kattint, nem direkt tárolt nyelvet állít. Nem futott.

**Batch490 új unit+42 Phone UI megírva, NEM FUTOTT** (464+17+9; UI-esetszám
változatlan, egy meglévő javítva). Most csak lexical delimiter/sorrend/diff és
négy XML/kulcsütközés ellenőrzés, nem Kotlin/Room/Compose/Google. [skip ci],
új CI/APK/screenshot nincs. Migrációs ág utolsó teljes zöld dd4cc27/40072751592
read-only ellenőrizve. Main/verzió/élő adatok fölé telepítés STOP változatlan.

Külső #240 friss **82883ab** statikus re-review: 20 engedélyezett fájl, négy XML
651 stringgel /377 native-kulccsal és format-paraméterek egyeznek. BLE maradék javítva, Home
programcím nameHu továbbra nyitott; egységmetaadatok lokalizáltak, de a grid nem
rajzolja ki őket. Négy JVM-kód nem futott, nem integrált. [Review](reviews/NATIVE_UI_LOCALIZATION_PR240_REVIEW.md).
Külső #241 **ab25d84**, cél #240 ága: négy új tesztfájl/hat eset, nem futott,
csak előfeltétel-triage történt, teljes review nem. DataStore-közvetlen nyelvváltás
helyett integrációkor valódi pickerre váltás kell; nincs még kész teljes UI-rész.
A jelenlegi phone-ui-gate.py package-szűrője az új osztályokat is kiválasztja,
külön class-filter bekötés nem kell. A külön4+6 nincs a490+42-ben. Ágaikat nem írtuk.

Következő: külső lokalizáció/UI-próbák maradékai és integráció; Google tényleges
bejelentkezés/pending intent/result adatút (jelenleg disconnected gomb sync-et
hív, callback9001/9002 nincs kezelve), remote-delete/restore verseny, további A–E
funkcióleltár és a közös kapu. Rövid belépési pont: [NATIVE_RESUME](NATIVE_RESUME.md).

## Tartós testsúlytörlés és késői cloud-replay védelem – 2026-10-25

**567117a**, [skip ci]. Új opcionális portable `weightTombstones18` markerarray
(version1/opaque mérés-ID/LegacySyncIdentity18 származtatott legacy-aliasok/deletedAt).
Meglévő kv_documents sorokban tárolva, nincs új súlytár vagy DB-séma. Exact-row CAS
után marker+delete egy Room tranzakció; marker- vagy delete-IO hibánál mindkettő
rollback. Egy napon más ID-jú mérés megmarad, valóban új UUID azonos dátummal és
értékkel is engedett. Legacy/id nélküli késői másolatot a normalizált eredeti
18-as identitás fogja meg, nem nap-szintű törlés. Azonos ID újabb szerkesztése
sem támasztja fel a törölt mérést. Markerek nem csonkolódnak (50k explicit cap).

JSON/ZIP/Drive export/importban marker megmarad; malformed marker még első Room
írás előtt tilt. Ordinary import union/purge, 256-os könnyű ID/date/kg keyset,
meglévő teljes súlylista nélkül. Google merge local/current/base/archive marker-
uniót készít konfliktuspreferenciától függetlenül, majd a régi sorok előtt szűr.
Explicit felhasználói replaceAll régi, marker nélküli backupból szándékosan
visszaállíthatja a mérést/üríti a markert; ordinary legacy import nem. Ez új natív
replay-védelem, a mainben nem volt tartós weight tombstone; régi szoftverű peer
nem értelmezi az új mezőt. Nem ígérünk régi peerre visszamenőleges működést.

18 új JVM-metódus +1 közös cloud ZIP/receipt replay-próbakód. **Batch464 új unit
+42 Phone UI megírva, még nem futott**; #240 külön3 teszt nem integrált.
Actual DAO SQLite1500 keyset/törlés közbeni fedés és marker-prefix izoláció,
delimiter/import elővalidációs sorrend/diff ellenőrizve; nem Kotlin/Room/Drive.
Új CI/APK/screenshot nincs, utolsó teljes zöld dd4cc27. Main/verzió változatlan.

Következő: cloud snapshot-publikálás/local install közötti stale-publish és remote
photo-delete/late restore verseny, operation/account lifecycle; teljes batch
kód/funkció/Health/UI leltár, közös kapu és készülékes próba. #240 javítását a user
visszajelezte, a friss HEAD külön review még hátra; az ottani munkát nem írjuk át.
Nyelvváltási UI-tesztes következő külső prompt a plans/NATIVE_LOCALIZATION_UI_TEST_HANDOFF.md.

## Cloud-fotók privát stagingje és közös helyreállítás – 2026-10-25

**f81a1a4**, [skip ci]. Korábbi hiba: GoogleSyncRepository a végső canonical
CAS/adatcsere-lease előtt törölhetett vagy telepíthetett helyi fotót, és elnyelte
az upload/download hibát. NativeCloudPhotoStage a hálózati fázisban csak privát
noBackup staginget ír; meglévő helyi fájlt immutable másolatból ellenőriz/feltölt,
hiányzó fotót csak név/mime/méret/Google MD5 ellenőrzéssel tölt le. Hiányzó kép
nem válik hamis teljes sikernek. Közös deletedAt jelentés (0/false/null nem törlés),
aktív azonos-ID referencia védi a képet; eltérő Drive-identitás fail-closed.

A canonical metadata guard UTÁN, replacement lease alatt minden érintett helyi
fájl eredeti SHA-ja/hiánya és a staged bájtok újraellenőrzöttek. Az existing
BackupRestoreManager ZIP-manifest/SHA/receipt/originals/redo útját használja
(cloudMerge + pontos cloudPhotoDeletes terv), nem új recovery rendszer. Törlés
és új kép is a touched-journal része, IO-hibánál rollback, processzhiba után
receiptből explicit offline replay. Másolt archívumbájtok a stage hash-ével is
egyeznek. Sikeresen lezárt local install előtt remote delete nem engedett.

Remote törlés csak local commit után, hálózat továbbra a replacement lease-en
kívül. DeletedAt + driveFileId a tartós metadata/snapshot része marad, így hiba
után következő sync újrapróbálja, a hibát nem nyeljük el. cloudStatus csak e lépés
sikere után íródik, cancellation továbbterjed, a privát stage végül takarítódik.
A Google Drive út is az összes history tombstone-t olvassa, nem csak2000-et.

**20 új stage JVM +7 Room/fájl/redo hibafázis-próbakód: batch445 új unit+42 Phone
UI még nem futott.** Csak delimiter/diff és forrássorrend/hash/redo-bekötési check
volt, nem Kotlin-fordítás/Room/Google/valós fotópróba. Bináris fixture nem képi
renderelés. Nincs új CI/APK/screenshot; utolsó zöld dd4cc27.

Nyitott: távoli snapshot-publikálás és local commit nem elosztott atomikus tranzakció;
CAS-hiba után árva upload/stale remote snapshot, remote delete vs. későbbi explicit
restore/új referencia verseny, teljes operation/account lifecycle és Google hálózati
igazolás további munka. Ne jelentsd a teljes cloud/konkurencia területet lezártnak.
Következő: testsúlytörlés tartós replay-védelme; jelen mergeWeightRows18 visszahozhat
remote sort, a main sem tartalmaz külön tartós súlytombstone-t. Meglévő kv_documents
és LegacySyncIdentity18 felhasználásával kell bővíteni, exact-ID védelemmel,
portable JSON/ZIP/Drive és import elővalidálással. UI/resource fájlok lefoglalva.

#240: **59da34d** statikus review, 20 engedélyezett fájl, négy645/371 kulcsú XML
rendben; két P2 maradék (Home/BLE suffix/fallback és Home nameHu választás),
[review](reviews/NATIVE_UI_LOCALIZATION_PR240_REVIEW.md). A felhasználó a review/
javítást már kiadta a másik beszélgetésnek; ott dolgozzon, mi nem írunk arra az ágra.
Új3 JVM teszt ott nem futott és nem integrált, nem számít a445+42-be. Következő
külön UI-tesztes feladat csak előkészítve: plans/NATIVE_LOCALIZATION_UI_TEST_HANDOFF.md.
Main/verzió/élő telepítés STOP változatlan, PR #238 követi.

## Párhuzamos munkamegosztás – 2026-10-25

Felhasználó jóváhagyta: külön ChatGPT-beszélgetés dolgozhat a telefonos UI
megmaradt HU/EN/DE/RO feliratain a **feat/native-ui-localization** ágon,
**958907c** alkalmazásbázisból. Az ág ellenőrzéskor már létezett ezen a refen;
a dokumentáció publikálása közben a másik munkás már új lokalizációs commitokat
pusholt (ellenőrzött pillanatkép bea5192). Emiatt erre az ágra mi nem írtunk;
az átadó a migrációs ágon elérhető. Pontos megengedett fájlok,
átfedés- és fordítási szabályok, riport/draft PR/patch átadás:
[delegálási feladat](plans/NATIVE_UI_LOCALIZATION_HANDOFF.md). AGENTS szűk
ágkivétellel bővült; nem belső subagent-indítás. A fő migrációs agentnél marad
cloud-fotó/backup/szinkron és súlytörlés-replay. A lokalizáció lefoglalt képernyő/
text-helper/négy strings.xml fájljait lehetőleg ne módosítsuk párhuzamosan.

Ez kizárólag munkakiosztás/dokumentáció, nincs új natív funkció vagy futtatott
próba. Az implementáció/teszt eredményét később külön review-val integráljuk;
a lokalizációs draft PR célja migráció, main merge és verzióemelés továbbra tiltott.
Legutóbbi alkalmazásforrás320ea83, batch418+42 nem futott tesztkód változatlan.

## Edzéslezárási rekordvizsgálat teljesnapló-betöltés nélkül – 2026-10-25

**320ea83**, [skip ci]. A TrainPilotWorkoutFinalization meglévő mentés/naptár
Room-tranzakcióján belül a PersonalRecordCalculator226 már csak a mostani edzés
érintett gyakorlataira kér kész sorozatokat. Lightweight PersonalRecordSet226,
256-os rowId-keyset lapok, 128-as ID-chunk a SQLite bind-limithez; nincs rawJson,
fotó/útvonal vagy teljes Room-reláció betöltése. Minden releváns történeti lapot
bejár, nem csak a frisseket; memóriában gyakorlatonként öt maximum marad. Önálló
hívás is read tx-t kap, finalizerből a meglévő tranzakciót használja újra.

A listás kompatibilis adatbemenet és a streaming út közös akkumulátort/evaluatort
használ; a mostani edzés rekorddöntő/formázó kódrésze byte-for-byte változatlan.
Súly/e1RM (+.2 küszöb)/ismétlés/idő/kétoldali maximum/táv, done és saját ID/azonos
started kizárás megmarad. Új explicit korrekció: a tombstone által már töröltnek
jelölt, fizikailag még megmaradt sor nem nyomhatja el az új rekordot (a régi teljes
relációs betöltés ezt nem szűrte). Nincs count-cache. A nyugta, natív event-reconcile,
lezárás, draft-retirement, naptár és hiba utáni retry sorrendje nem változik;
a finalizerben csak a rekordadat-bemenet két sora cserélődött.

16 új célzott JVM-metódus +1 nagy naplós finalizer-integrációs metódus és +1 Phone
UI-próbakód (1500 előzmény, órás finish/replay, egyetlen mentés és natív PR badge).
Korábbi naptárhiba/rollback/retry teszt PR-előzménnyel és badge-asserttal bővült;
a meglévő tesztek nem lettek eltávolítva. **Batch418 új unit+42 Phone UI kód még
nem futott.** Az actual DAO SQL SQLite0/200/1500 próbája ellenőrizte a scoped
fedést, <=256 lapot, oldest maximumot és a saját ID/start/tombstone/done kizárást.
A rekord-evaluator változatlansága, célzott Kotlin delimiter és diff ellenőrzés
megtörtént; ez nem Kotlin-fordítás/Room/Compose futtatás vagy valós sebességmérés.

GitHub read-only ellenőrzéskor a legutóbbi natív workflow továbbra **40072751592 /
dd4cc27 SUCCESS** volt; ez nem a batch igazolása. Nincs új CI/APK/képernyőkép.
Következő: Drive-fotó staging/hibafázisok/konkurencia és súlytörlés késői felhő-
replay védelme; maradó A–E/Health/UI/lokalizáció, majd közös teljes kapu és valódi
Phone/Watch7/migrációs próba. Export/restore teljesadat-olvasása külön marad, nem
lett tiltva vagy megcsonkítva. PR #238 frissítve; csak migrációs ág, main/verzió/
élő adatok fölé telepítés STOP változatlan.

## Lapozott testsúlynapló és teljes adathalmazból számolt áttekintés – 2026-10-25

**29ed6a4**, [skip ci]. A WeightJournalViewModel már nem observeAllWeights teljes
listát figyel: a meglévő repository Room Paging adatútját használja, 32-es kezdő
lappal, 8-as prefetch és 160-as cél-cache beállítással (Paging best effort).
Room kezeli a visszafelé lapozást és az írás/adatcsere miatti generációcserét;
lista külön cold flow, Fejlődés áttekintése nem indít teljes méréslistát.
Az aktív Groovy build a már használt Room-verzióhoz illő room-paging függőséget
kapta; Room-séma/verzió, package/appverzió és Wear változatlan.

A teljes napló kezdő/aktuális/minimum/maximum súlya és darabszáma skalár SQL-ből
jön, nem a látható lapból. Egy read tranzakció tölti az összesítést és a kiválasztott
időszak legfeljebb180 grafikonpontját. Ezek **feliratozott időablak-átlagok**, nem
kitalált egyedi mérések; a mintaszám összege a teljes szűrt adathalmazé, hosszú
időtávnál sincs legutolsó180-ra vágás. Minden egyedi mérés lapozva szerkeszthető;
export/rawJson és exact-ID/stale írásvédelem megmarad. Ismeretlen importdátum
nem lesz hamis grafikonpont, de a mérés a listában/globális statisztikában marad.

Csak body_weights invalidáció indít új összesítést: azonos darabszám melletti
középső súlymódosítás/adatcsere sem marad cache-ben. Gyors időszakváltásnál kérés-
azonosság kizárja a korábbi válasz publikálását. Összesítés- és lapozási hibának
külön explicit retry, HU/EN/DE/RO; az időszak továbbra a grafikont szűri, nem a
szerkeszthető teljes naplót. A korábbi inclusive cutoff (pl.7→8 naptári nap) megmarad.

13 új JVM-metódus +2 Phone UI-próbakód; a régi teljeslistás state-teszt valódi Room
aggregátum/lapozás útvonalra alakítva, az eredeti megőrzési elvárással.
**Batch401 új unit+41 Phone UI megírva, még nem futott.** Tényleges DAO SQL célzott
SQLite-próbája: 0/200/1500 minta, <=180 pont, teljes mintafedés, azonos dátumú1500
külön ID pontos lapozása, skalár szélsőértékek, hibás dátum megőrzése. Négy XML/
resource/formátumargumentum és diff ellenőrizve. Ez nem Kotlin/Room/Compose-build
vagy telefonos sebességmérés; új CI/APK/screenshot nincs, utolsó teljes zöld dd4cc27.

Következő konkrét teljeslista-rés: TrainPilotWorkoutFinalization.finishWorkout a
PR-számításhoz még getAllWorkoutsWithDetails-t hív (összes sorozat/fotó hidratálás).
Ezt ugyanazon PR-szerződés mellett könnyű, bounded projekcióra kell cserélni;
a lezárási tranzakció/nyugta/draft/naptár út nem kerülhető meg. Export/restore
szándékos teljesadat-olvasása külön feladat; cloud-fotó staging/konkurencia,
súlytörlés felhőreplay, maradó A–E/UI/lokalizáció és közös végső kapu továbbra nyitott.
PR #238 követi; nincs main merge, verzióemelés vagy élő adatok fölé telepítés.

## Bizonyított teljes nap és kezdő practiced-kivétel – 2026-10-25

**89a0cba + 4c1d06a**, [skip ci]. NativeProfileDayHistory128 a kanonikus Room
aktív program/azonos nap 90 napos, tombstone-szűrt előzményeiből, 32-es keyset
oldalakon max640 rekordot olvas; nem hidratál fotó/route/sorozat-relációkat.
Teljes, kompatibilis nap csak tényleges és becsült időkeret-egyezéssel maradhat
meg a statikus gyakorlatkapacitás felett. Frissebb részleges/fájdalmas/hiányzó/
túl lassú nap nem enged régebbi kényelmes sikerre visszaesni. Portable
history128.days/activeProgramId és main-alakú reductions (nap, ok, gyakorlat- és
sorozatszám előtte/utána), HU/EN/DE/RO magyarázat. Csak új programot készít.

A main tp128PracticedIds alapján a kezdőnek nem alapból ajánlott gyakorlat
korábbi teljesítéssel megengedhető: jelenlegi Room-katalógus, 90 napos legfrissebb
előfordulás, teljes pozitív sorozat/érvényes súly, alapvariáció/loadType/metric
azonosság, fájdalom nélkül. Egyetlen bounded rekord/érintett gyakorlat; bármely
program bizonyíthat gyakorlottságot, ez nem engedélyez idegen programmunkasúlyt.
Felszerelés/kizárás/kerülendő terület továbbra tilt; Coach-variáció külön guardja
nem lett fellazítva. history128.practiced bizonyíték és felületi magyarázat;
importált/hamis proof nem helyettesíti a friss Room-ellenőrzést.

Elfogadáskor ugyanaz a kanonikus előnézetépítő fut a Room-írás előtt: az asOf a
preview azonossága marad, de a lekérdezés felső határa az aktuális idő. Így a
megnyitás UTÁN érkező edzés/aktívprogram-váltás/törlés sem marad ki. A kezdő
practiced-jogosultság a min3/kapacitás vizsgálat ELŐTT épül. Megszakítás/DB-hiba
nem nyelődik el jogosulatlanul üres előnézetként.

18+14 új JVM tesztmetódus (utóbbiban12 actual-main/Room fixture-helyzet), 2 új
Phone UI-próbakód. **Összesen388 új unit+39 Phone UI megírva, még nem futott.**
A pinned main tényleges practiced-függvénye 12 szintetikus helyzeten futott;
fixture script reprodukálható. A valódi DAO SQL 99 azonos idejű rekordot pontosan
lapozott, idegen programot/tombstone-t kizárt. Négy XML/kulcs/argumentum/resource,
Node-szintaxis és diff ellenőrizve. Ez NEM Kotlin/Room/Compose futtatás; nincs új
natív CI/APK/valós screenshot. Utolsó teljes zöld továbbra dd4cc27.

Szándékos biztonsági eltérés a mainhez képest: legfrissebb rossz előzmény blokkol
régebbi practiced-sikert, min2 sorozattal sem beférő terv tiltott. Aktiválási út és
teljes generátor/A–E paritás még ellenőrzendő; a teljes app nem kész. Következő
konkrét teljesítményrés: WeightJournalViewModel még observeAllWeights teljes
listát figyel; bounded/lapozott lista és globális időtartomány-összesítés szükséges.
Cloud-fotó/konkurencia, maradó funkciók/UI/lokalizáció, végső GitHub-kapu és valódi
Phone/Watch7 próba is nyitott. Wear #239 port csak elfogadás után, külön feladat.
PR #238 frissítve; main/verzió/élő telepítés változatlan.

## Végső A/B sablonok, helyettesítés és számolt időkeret – 2026-10-25

**b69eb56**, [skip ci]. A main végső tp128Template v2 A/B slotjai, NEEDS132,
mozgásszerep/kerülendő terület térképei, rf14Score metaadatai és kezdőpreferenciái
külön data-only `native-planner-main-7473.json` csomagban, ugyanabból a pontos
7473b30 app/index blob-párból. NativePlannerPolicy az aktuális Room-katalógus
mezőit használja, a bundle a kanonikus fallback/adatszabály. Nincs JS-motor az appban.
A korábban tárolt builtin/saját programnapokat nem írjuk felül: az új személyes
előnézet épül a végső slotokra. Basic B crunch, Level2 A front squat/barbell row/
hammer curl és végső B decline/crunch; a régi extra csere/sorrend wrapper eltávolítva.

Elérhetetlen/kizárt/kezdőnek nem megfelelő mozgás szerepkörön belül pontozott,
eltérő elérhető gyakorlatra cserélhető. Ismételt ID nem keletkezik; szerep nélkül
nincs kitalált csere. Kizárások és kerülendő mozgásminták érvényesek; a kezdő
practiced-kivétel továbbra nincs engedélyezve. Háromnál kevesebb alkalmas gyakorlat
esetén az egész előnézet tiltott. Kapacitás a main perc-küszöbei szerint, max7.

Időbecslés: main 4 perc + sorozat*.65 + sorozatok közti pihenő +1 perc/gyakorlat,
napok maximuma felfelé kerekítve. Explicit és örökölt 3-as sorozat időkeret miatt
2-re csökkenthető hátulról; a megkeresett súly/ismétlés nem csökken. Friss workSets,
valódi estimatedMinutes és időkeret látszik, becslés a program JSON-ba is kerül.
Még minimumsorozattal sem beférő terv nem menthető. HU/EN/DE/RO változáslista valódi
névvel, csere nyíllal és indokkal, nem nyers reason/ID; korábbi12 soros limit nincs.

8 parametrizált főági generátorreferencia +3 új unit (becslés, súlyvesztés nélküli
időadaptáció, min3) +1 UI-kód. **Batch356 új unit+37 Phone UI még nem futott.**
A főági JS-generátor 8 szintetikus profilon ténylegesen futott, az adat-fixture
nem Kotlin/Android teszt. A default106/13 katalógusextrakció változatlan outputja
cmp-vel ellenőrizve; négy XML/kulcs/hivatkozás, Node-szintaxis, resource/fixture
shape és diff rendben. Nincs új natív APK/emulátor/képernyőkép vagy teljes CI.

Nyitott: valós 90 napos teljesítésre támaszkodó kezdő practiced-kivétel és
korábban teljesített, időben beférő teljes nap megőrzése a statikus kapacitás helyett;
a főági `history128.reductions` teljes portable nap/sorozat indoklása és további
profilvezérelt programparitás. A jelenlegi időadaptáció min2 után szigorúan tilt,
míg a main ilyen extrém rövid, nem validált profilnál túllépést is visszaadhat.
Teljes UX/lokalizáció és közös CI/upgrade/Phone+Wear készülékpróba hátra. PR #238
frissítve, main/verzió/élő telepítés STOP változatlan.

## Explicit Level2 receptek és közös main-referenciájú mag – 2026-10-25

**5bf0654** [skip ci]. Pontos main `7473b30:www/app.js` tp128Rx és a
`generatePersonalProgram` két Level2-receptfelülírása alapján közös
NativeTrainingPrescription került a személyes előnézet és Coach-variáció elé.
Compound/erő/Level2 ismétléstartomány, gyakorlat-specifikus cél, profil/perc/munka
alapú 2–3 sorozat, 150/120/75 mp pihenő, trial0 és RIR/p. szint; Level2-ben csak
intermediate profilnál paused pushup és explicit kézisúlyzó mellett weighted
side-plank/crunch. A súlyozott core recept `single_dumbbell`, ezért history128
most az effektív recept-terheléstípusból örököl súlyt, nem a katalógus bodyweight
besorolásából. A variációcímke megjelenik az előnézetben; testsúlyos trial0 nem
kér kg-próbasúlyt, a tényleges súlyzós változat igen. Autoajánlott sablon pontosan
intermediate→Level2, egyébként basic; kézi választás megmarad.

**Forráspontosítás:** a main `level2` javaslatszövegének nincs külön automatikus
programszint-emelési hívási útja. Nem találunk ki többgyakorlatos küszöböt. A Level2
kézi előnézet/elfogadás a tényleges út. További valódi eltérés: a data-only
katalógus jelenlegi Level2 napjai a korábbi builtin szerkezetet őrzik, míg a main
későbbi tp128Template v2 más szerepsorrendet/gyakorlatokat épít. Ennek és a
szerepalapú helyettesítésnek, min3 gyakorlatnak, valós időkeret/örökölt terhelés
adaptációnak a teljes portja még nyitott. Ez a receptblokk nem teljes generátorparitás.

`scripts/native/prescription-reference.cjs` a rögzített app blob ellenőrzése után
a tényleges JS-függvényeket futtatva 14 reprodukálható adatreferenciát generál.
Ez helyi célzott JS-referencia, nem Kotlin teszteredmény. Új 14 parametrizált JVM
paritási eset +3 előnézet +1 súlyozott core öröklési eset, +1 Phone UI-próbakód;
**batch345 új unit+36 UI kód megírva, még nem futott**. A korábbi trial-elvárás
igazolt main-szerződés alapján frissítve: a flag önmagában nem jelent kg-kérést.
Négy XML/kulcs/hivatkozás, fixture-shape és diff rendben; nincs új CI/APK/valódi
kép. Utolsó zöld továbbra dd4cc27, main és élő telepítés STOP. PR #238 frissítve.
Következő: tp128Template v2 napok és szerepalapú helyettesítés, majd időkeret-
adaptáció; utána fennmaradó teljes UI/paritás és közös CI.

## Coach variáció: előnézet és külön programmásolat – 2026-10-25

**432727e** alkalmazás-/tesztforrás, [skip ci]. Main `7473b30:www/app.js`
`tp128VariationPreview/tp128Rx` (16837/16598) és a meglévő natív Coach-ajánló
újrahasználva. Három kompatibilis stabil felső teljesítés, valós aktuális
readiness/fájdalom és profil/felszerelés/kizárás/kezdő-feltételek után nyitható
csak olvasható variáció-előnézet. Nem elérhető nehezebb fekvőtámasznál az eredeti
szünetes változat; kerülendő testrész nem kerülhető meg; külső súlyhoz explicit
súlyzó/rúd kell. Új recept trial/0 kg, új variáció/progressionLevel; nincs
mérési készültség vagy tényleges RIR átmásolva.

Mentéskor metadata admission + Room tranzakció alatt újraszámoljuk az ajánlót,
readiness-t, profilt, katalógust, konkrét előzményt. SHA/draft/variáció eltérésnél
nincs írás. A meglévő NativeProgramEditor.save új explicit clone ága biztosítja,
hogy saját és builtin forrás is új azonosítóra menjen; szülő/coach metaadat és
recept ugyanabban a tranzakcióban kerül be. A módosított gyakorlat history128
referenciája törlődik; régi napló/sablon/aktív választás megmarad. Sztring és
objektumos napbejegyzés is kezelve, variáció miatti duplikált gyakorlat tiltott.
HU/EN/DE/RO dialog, hibánál megtartott előnézet, meglévő Wear publikáció.

**UI-döntés/eltérés:** a gomb egyértelműen „Mentés új programként”; aktiválás a
meglévő Programok művelettel külön történik. A webes main az elfogadással aktivál.
Ezt a natív UI nyíltan jelzi; nem állítunk teljes aktiválási paritást vagy teljes
Level2 upgrade/paritást. Globális, több gyakorlatra épülő Level2 előnézet és
readiness/fájdalom/idő programátalakítás maradt; teljes funkció/UI/CI leltár is.

10 új unit+1 Phone UI-kód: read-only/cancel, új saját programmásolat, 2 vs3 alkalom,
fájdalom, profil/programváltozás, felszereléshiány, kizárás, restore és próbarecept.
**Batch327 új unit +35 Phone UI, még nem futott.** XML/kulcs/resource/diff és
újraellenőrzés→klónozás forrássorrend ellenőrzése rendben, nem Kotlin/Room/Compose
futtatás. Nincs új natív APK/kép/zöld CI; utolsó zöld dd4cc27. PR #238 frissítve.
A külön main-alapú Wear #239 engedélyjavítás nincs ebbe a migrációs ágba átvéve;
készülékes elfogadása és későbbi natív átvezetése külön követendő.

## Gyors folytatási review és személyes előnézet újraellenőrzése – 2026-10-25

**7143cef** app-/tesztforrás, [skip ci]. A da65f66 távoli állapot tiszta munkafába,
fast-forwarddal átvéve; nem régebbi mainből indultunk. Célzott review: A/B tervező,
history128 adatút/elfogadás, Progress lekérdezés, új háttér-policy és UI-típusok.
Nem teljes audit és nem Android-futtatási igazolás.

Javítások: a decline-pushup csere ugyanazt a kezdő/kerülendő testrész/felszerelés/
kizárás szűrést kapja, mint az eredeti gyakorlat. Személyes terv elfogadásakor a
Room-tranzakción BELÜL a kanonikus katalógusból/profilból újrageneráljuk az egész
tervet és az esetleges history128-öröklést; a draft, receptek és bizonyíték eltérése
az első programírás előtt megszakítja a mentést. Az eddigi, UI-JSON-ból épített
lineage-újraszámítás önmagában nem védte a katalógusváltozást vagy módosított receptet.
TrainingPrograms UI/VM/state internal; a publikus Health-state BackgroundBlock
enumja publikus, így a belső típusok API-kitettsége rendezve.

4 új unit-próbakód: cserevariáns kezdő/testrész megkerülése, katalógusváltozás,
bizonyíték nélküli recept-módosítás és írás nélküli elutasítás. **Batch317 unit+34
Phone UI kód megírva, még nem futott.** Négy nyelvi XML/egyedi kulcs/hivatkozás,
revalidation-before-write forrássorrend és diff ellenőrzés rendben. Kotlin/Room/
Compose build és készülékes eredmény nincs új. Következő: Coach három stabil alkalom
utáni külön variáció/upgrade előnézet és jóváhagyás a main pontos szabályai alapján;
majd további leltár/dizájn és közös CI. PR #238 frissítve; main/viselhető hotfixág,
verzió és élő adatok változatlanok.

## 90 napos A/B edzéselőzmény és Coach-öröklés adatút

**cd696e7c** `[skip ci]` utolsó alkalmazás-/tesztforrás,
kizárólag `feat/native-android-migration`. Az előző A/B
előnézetből hiányzó `history128` most a kanonikus
Room `workout_history` adatból készül; nem párhuzamos
napló, nincs új migráció/séma.

- `NativeProfileHistory128`: max 90 napos, 32-es
  keyset Room-oldalakon olvas, legfeljebb 20 oldal/
  gyakorlat; adott gyakorlatból legfeljebb **4**
  kompatibilis, konkrét `{programId,key}` referencia.
  Csak ugyanilyen A/B napra és legalább hét átfedő
  mozgásra igazolt forrásprogram fogadható el; Coach
  `compatible` loadType/repUnit/variáció egyezés.
- Befejezetlen/hibás/fájdalmas, nem numerikus vagy
  más mértékegységű sorozatból **nem öröklődik
  munkasúly**. Az újabb ilyen alkalom a régi súly
  fallbacket is blokkolja; jövőbeli, 90 napon túli,
  tombstone-olt edzések nem jutnak át a lekérdezésen.
  Validált teljes sorozatnál a legfrissebb súly
  és 2–3 munkasorozat megőrizhető, a tényleges
  felső ismétléstartomány tágítható; kg/kar nem
  konvertálódik kg összesenné.
- `history128.entries[id]` a Coach `Selection`
  által ismert `loadType/variation/metric/sources`
  sémát kapja; bizonyítékhash a nyers edzésrekordokból.
  `NativeProgramEditor.save` mentés előtt ugyanazon
  történeti időpontra újraszámol és eltérésnél
  **megszakítja az egész Room-tranzakciót**.
  Elfogadáskor `prescriptions`, `history128`
  és profil ugyanabba a kanonikus új programpéldányba
  kerül. Coach külön, **csak javaslat**, nem módosítja
  a napló aktuális munkasúlyát.
- A Planner a két sablonhoz aszinkron előtölti a
  kanonikus történetet; külön betöltési állapot;
  a meglévő UI-tesztek csak a befejezett előnézeten
  ellenőriznek. Előnézet elvetése nem ír adatot.
  JSON/ZIP/Drive importnál az örökölt hivatkozások
  maximuma és egyedisége már a Room adatművelet
  előtt validált, a régi `history128` nélküli
  mentések továbbra elfogadhatók.
- **12 új JVM unit-tesztkód** (valódi Room-import,
  eltérő load type, variáció, hiányos/fájdalom/
  késő/jövőbeli/törölt forrás, régi A/B azonosító,
  4 lineage, Coach 3 stabil alkalom,
  5→5,5 kg csupán javaslat, stale history
  visszagörgetés, backup/duplicate import) és a
  meglévő 3 Compose preview teszt
  aszinkronra igazítva. Összesen **313 új unit
  +34 Phone UI-tesztkód**, **még nem futtatva**.
  21 statikus integrációs ellenőrzés PASS; nem
  jelenti Kotlin/Room/Android/emulátor CI sikerét.

**Nyitott:** három stabil, új programhoz tartozó alkalom
utáni variációváltás külön előnézete és elfogadása,
readiness/fájdalom alapján teljes programfrissítési
döntések, újabb főágbeli #128 pontos idő- és
fájdalomkapuk; teljes 24 nézet/téma és közös
Phone/Wear/upgrade gate. A 20 oldal/gyakorlat
biztonsági lekérési maximum miatt rendkívül nagy
naplónál régebbi előzmények kimaradhatnak;
a hiányból nem lesz automatikus emelés.
Main, verzió, Wear release és APK STOP változatlan.

## Személyes A/B tervező – valódi előnézet és jóváhagyás (részparitás)

**559fd43e** `[skip ci]` utolsó alkalmazás-/tesztforrás,
csak `feat/native-android-migration`. A Programok oldali
`Személyes Edzéstervező` banner korábban csak
`isPersonalPlannerOpen=true` flaget állított, UI nélkül.
Most a meglévő Room `programs/program_days` és
`NativeProgramEditor.save` szolgálja ki a tényleges, külön
elfogadást igénylő A/B előnézetet.

- `NativeProfileProgramPreview`: a főágban elfogadott
  `home-basic` / `home-level2` sablon és aktuális
  `settings.profile` alapján **csak olvasható** előnézet.
  Nincs működés explicit `minutes` és `gear` nélkül;
  ismeretlen kerülendő testrésznél nem találgat. Kezdőknél
  nem kezdőbiztos gyakorlat szűrve; elérhetetlen felszerelés,
  `excluded` és `avoidAreas` figyelembe véve; időhiány
  miatt kihagyott mozgások külön okkóddal.
- B-napi alap sorrend: összetett mozgások és harmadik
  fekvőtámasz, 2. szinten `decline-pushup` csak kifejezetten
  megadott pad mellett. Konzervatív **2** munkasorozat,
  profil/perc/tapasztalat alapján alkalmas összetett
  mozgásoknál **3**; 120/75 mp pihenő, 2 RIR.
  Kitalált kezdősúly nincs: súlyzós recept `weight:0`,
  `trial:true` + látható próbasorozat figyelmeztetés.
- Az előnézet **nem módosít** Roomot, naplót vagy aktív
  programot. Felhasználói elfogadáskor külön, stabil
  `custom-program-UUID` másolat, kanonikus
  `profileSnapshot`, `prescriptions`,
  `templateVersion:2`, `sourceTemplateId` és
  `sourceType:profile-template` mezőkkel. Az írás
  restore-fence + Room-tranzakció és elavult
  profil/sablon CAS mellett történik; csak sikeres új
  példány után vált az aktív program és publikál Wearnek.
  A DataStore aktívprogram-beállítás és a Room insert
  nem egyetlen kereszt-tárolós tranzakció; aktiválási
  hiba esetén a létrejött, de nem aktivált másolat
  megmaradhat (követendő).
- Beállítások → Edzésprofil alatt most a korábbi
  `gear/excluded/avoidAreas` tömbök is explicit
  szerkeszthetők, JSON- és Drive-mentési azonossággal.
  Új `PersonalProgramPreviewDialog` HU/EN/DE/RO
  szöveg, 2 sablon, gyakorlat-/sorozat-összegzés,
  kihagyási ok, Mentés/Elvetés.
- **14 új JVM unit +1 korábbi profil unit +3 Phone UI**
  regressziós próbakód: hiányzó profilfeltétel,
  felszerelés/kizárás/kezdő/testrész, 20 perces rövidítés,
  B-sorrend, próbasúly, read-only előnézet, template
  érintetlenség, stale elutasítás és elfogadás.
  Összesen **301 új unit +34 Phone UI-tesztkód
  megírva, egyik sem futott**. **20/20 célzott statikus
  forrás- és útvonalellenőrzés PASS**; Kotlin/Gradle,
  emulátor, valódi UI-kép és APK továbbra nem igazolt.

**Nem teljes #128 algoritmikus paritás:** 90 napos
kompatibilis edzéstörténet alapján kezdősúlyöröklés,
`history128` 4 konkrét forrásreferenciája, fájdalom /
readiness alapján javaslat és 3 stabil alkalom utáni,
külön jóváhagyott Coach progresszió maradt.
Továbbá profilkötött helyettesítések és testreszabott
osztott programok finomítása. Ezek után összesített
natív/Phone/Wear/upgrade CI, majd készülékes elfogadás.
Main, verzió, Wear release változatlan;
élő adatos telepítés STOP.

## Edzésprofil megőrzés és célzott Progress lekérdezések

**cfdc6d05** `[skip ci]` utolsó alkalmazás-/tesztforrás, kizárólag
`feat/native-android-migration`. A main #128 edzésprofiljának
`settings.profile` objektuma korábban elveszett a natív
DataStore-import/export során. A `NativeTrainingProfile`
most érvényesíti a megadott mezőket és teljes, ismeretlen
kulcsokat/felszerelést/kizárásokat megőrző JSON-nal tárolja,
profiladatot nem talál ki.

- Natív Beállítások → **Edzésprofil** szerkesztő:
  életkor, testmagasság, testsúly, tervezett percek, cél,
  tapasztalat, aktivitás, helyszín, ritmus, felosztás, fókusz;
  HU/EN/DE/RO, mentéskori adatcsere-fence és stale JSON-CAS,
  hibás adat esetén régi profil marad. A nem szerkesztett
  `gear/excluded/avoidAreas` tömbök és jövőbeli metadata
  változatlanul tovább élnek. Ez **adatmegőrzés + alap
  profil-UI**, NEM a main profilvezérelt A/B generátor
  teljes algoritmikus paritása.
- `TrainPilotSettingsStore` a korábbi `settings.profile`
  mezőt DataStore-ba viszi; `LegacyWebViewMigrator`
  minden JSON/ZIP/Drive snapshotban exportálja és importálja.
  Malformed profilt még a Room adatcsere kezdete **előtt**
  elutasítja; nincs részleges programtörlés.
- A Progress oldalon korábban `getAllWorkoutsWithDetails()`
  felolvasta az egész naplót sorozatokkal/fotókkal, és a
  gyakorlatchip-lista csak az első **8** elemet mutatta.
  Most a 30 napos izomterhelés SQLite aggregációval,
  a kiválasztott 1RM-adatsor **256 soros keyset-lapokkal**,
  csak a kiválasztott gyakorlatból töltődik.
  A tombstone-ok és befejezetlen sorozatok kiszűrve,
  minden 106+ gyakorlat elérhető a chip-sorban.
  Izomcsoport felirat HU/EN/DE/RO és valós
  adatlekérési hiba/újrapróbálás.
- **15 új JVM unit +3 Phone Compose UI-tesztkód**,
  köztük profil portable roundtrip, invalid-before-restore,
  tombstone, 0/600/1500 adatsoros progress lapozás,
  teljes katalógus-választás. Összesen **286 új unit
  +31 natív Phone UI próbát írtunk**, **nem futtattuk**.
  17 statikus forrás-/útvonalellenőrzés PASS, de Android
  fordítás, JVM/Room teszt, emulátor, APK és
  1500-as tényleges teljesítménymérés **még nem igazolt**.

Következő: profilvezérelt generátor + Coach/progresszió
főágbeli szabályparitása és a maradék 24 natív
nézet/témakatalógus ellenőrzése, majd a közös
Phone/Wear/upgrade gate. Main, verzió, Wear
változatlan; élő adatos telepítés STOP.

## Natív program- és programnapszerkesztés

**87f9ee10** `[skip ci]` legutóbbi alkalmazás-/tesztforrás, kizárólag
`feat/native-android-migration`. A meglévő `programs` +
`program_days` Room és `LegacyWebViewMigrator` JSON adatútjára épül,
új séma és párhuzamos tároló nélkül.

- `NativeProgramEditor`: saját program létrehozása/átnevezése/
  leírása, beépített programból **saját másolat** (a 13 eredeti
  programot soha nem írja át); stabil program/day ID,
  edzésnap hozzáadás/név/sorrend/eltávolítás.
  Gyakorlatok felvétele a valódi Room-katalógusból,
  eltávolítás és sorrendcsere, az eredeti JSON prescription
  objektumok / extra mezők megőrzésével. Új, ismeretlen
  gyakorlat-azonosító nem írható, már importált változatlan
  legacy-hivatkozás nem veszik el.
- Program/nap módosítása egy `NativeDataReplacement.mutate`
  + Room tranzakcióban. Elavult szerkesztői snapshot nem
  menthet/törölhet. Aktív edzés alatt módosítás STOP.
  Hivatkozott nap (naptár / napló) és aktív vagy hivatkozott
  program törlését blokkolja; beépített nem törölhető.
  **Üres nap edzése nem indítható**.
- `TrainingProgramsScreen` → új `ProgramEditorDialog`
  HU/EN/DE/RO szövegekkel, nap-/gyakorlatrendezéssel,
  katalógus-keresővel, eltávolítási megerősítéssel.
  `TrainingProgramsViewModel` valódi Room-olvasás/mentés,
  mentési hiba esetén szerkesztő nyitva marad.
- A JSON/ZIP/Drive export most a programnap `en/de/ro`
  és ismeretlen legacy JSON mezőit megőrzi. Importkor az
  explicit teljes naplistával érkező program snapshot
  eltávolított napjai nem élednek újra.
- **12 új JVM unit +3 Phone Compose UI-tesztkód** készült
  create/copy/edit, day/order/assignment, CAS, reference
  fences, restore, import/export és valós navigáció ellen.
  Összes megírt új teszt: **271 unit +28 Phone UI**.
  **Még nem futottak**; 15 statikus forrás/hívási
  út/teszteset ellenőrzés PASS. Kotlin/Room/Compose fordítás,
  Android-emulátor, signed upgrade és új APK nincs igazolva.

Következő kódolási blokk: profil/saját beállítások
és további progressziós adatút paritásvizsgálata, majd
közös natív CI/testkapu a teljes forráson.
Main/verzió/Wear release változatlan; saját adatok
fölé telepítés STOP.

## Saját gyakorlatok natív létrehozása/szerkesztése/törlése

**a46a8bcd** `[skip ci]` utolsó alkalmazáskód; csak a
`feat/native-android-migration` ágon. `NativeCustomExerciseEditor`
a már meglévő `exercise_library` Room-táblát és eredeti
`rawJson` import/export formátumot használja; nincs új DB-séma,
új adatszinkron-fork, és a 106 builtin érintetlen.

- Új saját gyakorlat: különleges, ütközésmentes
  `custom-UUID` azonosító, név/leírás, izomcsoport,
  mérési/terhelési típus, egység, sorozat, ismétlés/idő,
  súly, pihenő és opcionális konkrét YouTube watch URL.
  HU/EN/DE/RO formcímkék. A kanonikus JSON minden
  mentett typed mezővel együtt megy a meglévő backup/Drive útvonalra.
- Szerkesztésnél a stabil ID, más nyelvi mezők,
  ismeretlen legacy JSON kulcsok és importált másodlagos
  izomcsoportok megmaradnak. Az eredetileg megnyitott
  `rawJson` ellenőrzése nélkül a stale felület nem írhat
  felül közben módosított sort.
- Törlés csak `custom=true` rekordra, külön megerősítéssel,
  `NativeDataReplacement.mutate` és Room tranzakcióban.
  Aktív edzés, naplóban használt exerciseId, programnapi
  referencia vagy időközben megváltozott rekord esetén
  **nincs törlés**. Siker után ugyanabban a tranzakcióban
  csak a törölt gyakorlat kedvencjelölése tűnik el.
- 11 új JVM unit +2 Compose Phone UI-próba a CRUD,
  hivatkozás-/stale/restore-védelem, videóvalidáció,
  roundtrip és a felület ellenőrzésére. Összes megírt
  új próbakód **259 unit +25 telefonos UI**; egyik sem futott.
  16 statikus adatút/összekötési ellenőrzés PASS,
  de Kotlin/Room/Compose fordítás, emulátor, felhasználói
  APK továbbra **nem igazolt**.

Következő kódolási rész: programnapok és programok
teljes szerkesztése, majd profil/progresszió maradék.
Main, release, verzió és Wear érintetlen; saját adatok
fölé telepítés STOP. PR #238 naprakész állapottal követendő.

## Teljes natív gyakorlatkönyvtár és eredeti videó-hivatkozások

**d5222a32** `[skip ci]` utolsó alkalmazás-/tesztforrás; kizárólag
`feat/native-android-migration`. A végleges main `7473b30`
(`www/app.js` blob `b5567576...`) adatalapjának tartalmát
ellenőriztük: **106 beépített gyakorlat, ebből 100 konkrét YouTube
videoId és 6 eredetileg is nem ellenőrzött keresési URL**.
A meglévő `native-catalog-main-7473.json` 106 sorát/metadataját és a
Room `exercise_library` táblát használjuk, nincs új hardcode gyakorlat,
fiktív videó vagy JS/WebView-függőség.

- Új natív `exercise_library` másodlagos útvonal, Edzésből külön
  Gyakorlatkönyvtár gombbal; kereső, izomcsoportszűrő (HU/EN/DE/RO),
  106+importált/saját sor, kedvencek, részletpanel, leírás,
  alap sorozat/ismétlés és videóforrás feltüntetése.
- `NativeExerciseDemo` kizárólag 11 karakteres YouTube ID-ból
  kanonikus watch-hivatkozást enged; importált, hiányzó demo-metadatajú
  **nem custom** beépített gyakorlat az eredeti azonos ID alapján
  megkapja a hivatkozást, de egyedi override soha nem örököl
  videót automatikusan. A 6 eredeti outdoor keresés külön
  **nem ellenőrzött keresés** feliratot és keresési gombot kap.
  Rossz/idegen URL nem nyitható.
- A gyorsedzés gyakorlat-kiválasztó panelén is megjelenik a
  bemutatóvideó gomb. Videó/YouTube keresés csak a felhasználó
  kattintására, Android `ACTION_VIEW`-val **külső lejátszóban vagy
  böngészőben**, nem beépített natív videólejátszóban indul;
  hibás intent esetén látható hiba. A gomb nem indít edzést.
- 7 új JVM unit +3 új Phone Compose UI-próba: 106/100/6 eredet,
  validáció, importált/külön custom link, könyvtár/részlet és
  keresési provenance, gyorsedzés panel; a főmenü route-tesztjei
  kiegészítve. **Összesen 248 új unit +23 Phone UI tesztkód,
  még nem futtatva**. Katalógus- és statikus útvonal/forrás-
  ellenőrzés sikeres, Gradle/Android/YouTube playback **nincs igazolva**.

Következő: egyedi gyakorlatok CRUD és programnap-szerkesztő,
profil/funkcióparitás; ezek után közös natív CI/kapu és készülékes
visszaellenőrzés. Main és kiadás változatlan. Élő adatos APK
telepítés **STOP**.

## Natív gyakorlatkedvencek v144 – kódolási batch

**4159a76** `[skip ci]` (utolsó appforrás), kizárólag a
`feat/native-android-migration` ágon. A main v144 `exerciseFavorites`
tömbjét ellenőrzött ID-kkel a **meglévő Room kv_documents** tárolja,
sémaemelés nélkül. A korábbi `legacy:exerciseFavorites` alias
olvasható, de az új kanonikus mentés elsőbbséget élvez. Import/restore
előtt szigorú típus-/ID-ellenőrzés, replaceAll régi alias törlés,
migrációs `KNOWN_DB_KEYS` felvétel és a JSON/ZIP/Drive snapshotba
azonos kulcsú roundtrip került. Visszaállítás és háttéradatcsere idején
a kedvencek írása a meglévő `NativeDataReplacement` + Room tranzakció
alatt történik; csak létező gyakorlat kedvenc státusza váltható.
A Drive `mergeSync18` most konfliktuskezeléssel viszi tovább a
kedvenceket, és felhőművelet előtt validálja a kiválasztott tömböt.

A natív gyorsedzés/aktivitásválasztóban kedvenc csillag,
kedvencek szűrő, elsőbbségi rendezés és perzisztens állapot,
HU/EN/DE/RO akadálymentes szövegek. Csillagozás önmagában
**nem indít edzést**. Ez a funkció a gyors választóban már
bekötött; az önálló, teljes gyakorlatkönyvtár részletlapjai és a
program-/gyakorlatszerkesztő **még hátravan**.

**10 új JVM unit +1 natív Phone Compose UI-próbakód megírva;
batch összesen 241 új unit +20 Phone UI, egyiket sem futtattuk.**
Kódkapcsolatok és 13 statikus feltétel rendben; rövid izolált
Kotlin szintaxisellenőrzés sikeres. Nincs új Android/Gradle/Compose
futtatási vagy APK-igazolás. Utolsó teljes zöld `dd4cc27`.
**Élő adatok fölé telepítés STOP.** Következő: teljes natív
gyakorlatkönyvtár és saját gyakorlat szerkesztése, programszerkesztő,
profil és további A–E eltérések.

## Health Connect/BLE háttérengedély és tényleges működési állapot

**f90e7ef** `[skip ci]` alkalmazás-/tesztforrás, kizárólag
`feat/native-android-migration` (több kis visszakövethető részcommit).

A korábbi Health-kapcsolók akkor is beállíthatták a `hcBackground` /
`bleBackground` opt-int, ha a rendszerengedély, támogatott háttér-HC,
mentett RDFit óra vagy Bluetooth/értesítés hiányzott. Hiba esetén a
`HealthBackgroundJob.schedule` kivételét elnyeltük; a kapcsoló a valós
ütemezést/futást nem különítette el. Most a meglévő adapterekhez:
- tiszta `NativeHealthBackgroundPolicy`: HC olvasás + külön U extension
  háttérolvasás, BLE mentett RDFit + Connect + notification + Bluetooth,
  valamint pendingRestore/replacement védelme;
- a UI feltétel- és ütemezett/futó státuszt külön mutat. Hiányzó runtime
  engedélyt kizárólag kifejezett felhasználói kapcsolás után kér.
  Négy nyelven 14 új státusz-/hiba-string, BLE tesztelhető lazy lista;
- háttér-preferencia írás `NativeDataReplacement.mutate` alatt, egyszerre
  csak egy kapcsolás. Sikertelen HC jobütemezés / BLE FGS start után
  visszaállítás és látható hiba, nincs hamis siker-nyugta.
  A BLE `resume` most igazolja a startkérés eredményét, nem az órás
  kapcsolat létrejöttét.

**10 új unit +2 natív Phone Compose UI-tesztkód készült; összesen
231 új unit és 19 Phone UI, még nem futtatva.** Statikus bekötés,
fájl- és 4×14 resource-ellenőrzés rendben, de Kotlin/Gradle-fordítás,
valódi engedélykérés, Android-emulátor, Samsung/Watch7 és CI **nem**
igazolt. Továbbra sincs új telepíthető APK; utolsó teljes zöld
`dd4cc27`, élő adat fölé telepítés STOP. Következő: maradó
profil/program/egyedi gyakorlat/kedvenc és A–E lefedés, majd a közös kapu.

## Egyedi Health Connect-eredet választása – natív kódbatch

**031f0a60** `[skip ci]`, csak a `feat/native-android-migration` ágon.
A Napi aktivitás Auto/Priority mellett a natív UI már a Health Connect naplóban
**ténylegesen megtalált forrásazonosítókat** kínálja fel (max 100, deduplikálva,
validálva; saját app/invalid/belső eredetek kizárva). A korábban kiválasztott,
de az aktuális naplóban már nem található eredet kijelzése megmarad, de nem
választható újra ellenőrzés nélkül. Nincs kitalált appadat, új SDK-kérés,
implicit háttérfrissítés vagy új tároló. A meglévő HC-policy/persist/dirty
jelzés érvényes: korábbi összesítés a tudatos Frissítésig részleges.

2 új JVM unit +1 natív Phone Compose UI-tesztkód (originlista, mentés,
stale aggregátum). **Batch: 221 új unit +17 telefonos UI-kód, egyik sem
futott még**. Forráscsere/diff ellenőrizve; Android fordítás, UI-kapu és
APK továbbra nincs. Utolsó teljes zöld forrás `dd4cc27` — **élő
adatok fölé telepítés STOP**. Következő: HC/BLE háttér-/engedélyállapotok,
maradó profil/program/kedvenc és teljes A–E lefedés, majd közös CI.

## Napi aktivitás forrásmód – kis lezárt kódolási lépés

**b55df92**, [skip ci]: Egészség HC forráskártyán Automatikus / Health Connect
prioritás választó, a meglévő hcDailySource és valódi provider-policy használatával.
Metadata admission, mentési állapot és kezelt hiba; nincs implicit SDK/engedélykérés.
Módváltás után a tároló által dirty/partial állapotúnak jelölt korábbi napi értékek
figyelmeztetést kapnak; explicit Frissítésig nem állítjuk, hogy az új móddal készültek.
A már mentett egyedi origin érték olvashatóan megmarad, új egyedi origin-választó
még nincs bekötve. HU/EN/DE/RO. 2 unit +1 UI új próbakód, még nem futott;
**batch219 új unit+16 UI kód**, közös CI később. Helyben XML/resource/diff ellenőrzés.
PR #238 és rövid folytatási pont frissítendő/ehhez igazítva; következő a többi
Health preferencia/engedély, majd a teljes funkcióleltár nyitott pontjai.

## Követhető összállapot és legfrissebb egészségrészletek

Draft [PR #238](https://github.com/fuloplevente1998/TrainPilot/pull/238) a teljes
natív ág pipálható állapotlapja. A kód elkészülte, a futtatással igazolt állapot és
az elfogadás külön lista; részcommitok után frissítendő. Nem merge-felhatalmazás.

Alkalmazásforrás **606b8c0**, [skip ci]. Egészség → Egyedi egészségmérések / Alvásrészletek:
a kiválasztott nap/csatorna meglévő HealthJournalStore rekordjai külön, csak olvasható
panelben. 25 rekord/oldal, SQL típus- és eredetszűrés a LIMIT előtt, kétkulcsos
idő+row_id kurzor, előző/következő. Forrásválasztók a kért nap/csatorna/típus adataiból
(max200 eredet), nincs teljes egészségtörténet-betöltés vagy napi alvásösszesítés
minden rekordlaphoz. A régi 7-paraméteres Kotlin/Java page API defaultjai megmaradnak.

Minden rekord saját channel/source/start/end/forrásbeli módosítási időt mutat.
Napi összesítésből nem készül kitalált egyedi mérés; egymást átfedő források nem
adódnak össze. Skalár/vérnyomás és időbélyeges pulzus/sebességminták; szakasz/minta
részletek 20-as oldalakban. Health Connect numerikus+milliszekundumos és Samsung
nevesített+ISO alvásfázisok kezelve. Az SDK által tárolt alvásidő/fázis alapú idő
külön feliratot kap, nem számolunk új összeadott éjszakát. Ismeretlen/hibás fázis
ismeretlen marad. Panelbezárás a napot, mérési adatot és edzést nem módosítja.

Metadata admission és Health generation/pendingRestore ellenőrzés: adatcsere után
régi kurzor nem használható; hiba régi sorok nélkül jelenik meg, explicit újratöltés
indít új böngészést. Azonos kérés újrapróbálása külön reload kulccsal is működő útba
kötve. HU/EN/DE/RO 33 új szöveg; meglévő napi alvásszámláló felirata pontosítva.

16 új unit +3 Phone UI-próbakód: 63 azonos idejű rekord lapozása, típus/eredet/nap/
csatorna, generation/restore, aggregátumból nem gyártott sor, HC/Samsung/hibás fázis,
mintaoldalak, forrásos UI, mindkét alvásforma és adatcsere utáni reload. **Összesen
217 új unit +15 instrumentációs tesztkód megírva, még nem futott.** Helyben négy
XML/hivatkozás/diff és célzott SQLite scope/kurzor-próba történt, nem Kotlin/Android
futtatás. Nincs új APK/screenshot/CI; utolsó teljes zöld dd4cc27, élő telepítés STOP.
További Health forráspreferencia/háttérengedély/lokalizáció és teljes A–E leltár,
program/profil/egyedi gyakorlat/kedvencek/UI/performance/backup részletek még nyitottak;
PR #238-ban külön láthatók. Ez a blokk nem teljes app- vagy Health-paritás elfogadása.

## Legfrissebb alkalmazásforrás – közös kézi testsúlynapló

**e3e79d3**, [skip ci]. Egészség és Fejlődés → ugyanaz a `weight_journal` útvonal,
a meglévő Room body_weights táblával; nem új egészségadat-tár. Dátumos hozzáadás,
súly szerkesztése változatlan mérési dátummal/azonosítóval, megerősített pontos-ID
törlés (más aznapi mérés megmarad), grafikon/kezdő/aktuális/változás/min-max,
7/30/90/365/mind tartomány a meglévő DataStore weightRange beállítással.
HU/EN/DE/RO, globális fejléc/Settings és visszatérés meglévő navigációvédelmen át.

Metadata admission előbb, utána Room tranzakció és teljes eredetisor-egyezés:
nyitott szerkesztő nem írhatja felül a közben módosított vagy törölt mérést.
Ismeretlen rawJson mezők megmaradnak; hiba esetén nincs mentett nyugta és nem
záródik be a beviteli panel, explicit retry lehetséges. Testsúlyírás és
megfigyelés külön VM/adatút: nem tölti újra az összes edzés relációját. A Fejlődés
általános edzésbetöltése és a teljes súlylista megfigyelése még nem bounded;
felhős súlytörlés késői replay-védelme nincs ebben a blokkban igazolva.

13 új unit +2 Phone UI-próbakód: stale edit/delete, azonos napi törlés, validáció,
restore fence, hibás JSON rollback, óra-visszaállás, tényleges backup-mapper
roundtrip, dátumos CRUD és sikertelen mentés utáni retry. **Batch201 új unit+12
instrumentációs tesztkód, még nem futott.** Helyben négy XML/21 kulcs/hivatkozás,
pontos-ID SQLite-próba és diff ellenőrzés történt, nem Kotlin/Android futtatás.
Nincs új CI/APK/képernyőkép; utolsó zöld továbbra dd4cc27, élő telepítés STOP.
Következő: történeti Health nyers mérések/eredet/időbélyeg/alvásrészletek,
teljes leltár további funkciói, majd a közös CI-kapu.

## Legfrissebb alkalmazásforrás – történeti egészségnapló

**edc8f52**, [skip ci]. Egészség: hónaplapozás, napválasztás, napi összesítés/HC/
Samsung/legacy/BLE csatorna. Meglévő HealthJournalStore havi(max31 nap) adatút,
hiányzó kiválasztott napnál nincs régi érték automatikus átemelése. Új kérés a régit
cancelleli, betöltés alatt régi érték nem marad új dátummal. A provider-hozzáférési
ellenőrzések IO-n futnak, adatbetöltés a meglévő replacement admission alatt.
Kifejezett SDK-frissítés a kiválasztott napra kér adatot; a Coach readToday útja
megmarad. Natív metrikalista most a provider mind26 kulcsát lefedi(hiányzó hat:
pulzusmintaszám, zsír-/izom-/zsírmentes tömeg, testvíz, alváspontszám); négy nyelvű
címkék, véges szám ellenőrzés, nulla/nincs-adat helyes megkülönböztetése. Az alvás
összefoglaló naplózott szakaszszám, nem az első szakaszból kitalált teljes alvásidő.

7 új unit és1 történeti testsúly-kiválasztási UI-próbakód. **Batch188 új unit+10
Phone instrumentációs tesztkód, még nem futott.** Csak XML/kulcs/hivatkozás/diff és
26 provider/display kulcshalmaz-egyezés ellenőrzése történt. Nincs új CI/APK/kép.
Utolsó zöld dd4cc27; kódolás/High fázis. Következő: nyers méréslapozás/részletek,
forrás-időbélyeg és eredetszűrés, alvásrészletek, testsúly edit/delete és a teljes
main-funkcióleltár további nyitott tételei. A teljes egészségparitás még nem kész.

## Teljes funkcióleltár az új dizájnátadóban

A felhasználó kéri, hogy a main összes funkciója megmaradjon, más elhelyezéssel és
kevesebb önálló oldallal is. NATIVE_DESIGN_HANDOFF.md részletes A01–A15, B01–B18,
C01–C14, D01–D14 és E01–E20 funkció→új elérési hely leltárral bővült, pontos
main-forrásreferenciával. Ez követelménylista, nem kész állapot. A kedvencek valóban
main-funkciók, így a mockupból kimaradt vezérlőjük pótlandó. A metrikák kulcsonkénti
és minden útvonal/teszt végső megfeleltetése továbbra hátra van; az ismert20/26
natív egészségeltérés nincs elfedve. Öt fő fül, közös egészségnapló/testsúlyadat és
összevont szerkesztő/mentési panelek az irány.

## Felhasználói pontosítás – teljes funkciókör, nem csak24 mockup

Az egészségnapló, testsúly, pulzus és a main összes további funkciója megőrzendő.
A 24 kép csak főnézet; hiányzó történeti/részletes felületek nem eshetnek ki.
Rögzítve a dizájnátadóban és a teljes tervben. Gyors forrásaudit: natív Health VM
20 metrikát definiál és ma-adatot tölt, a UI26-os felirata jelenleg megtévesztő.
Történeti HealthJournalStore adatút és Progress testsúlyrögzítés/történet létezik,
azonban teljes UI-paritás/futtatás még nincs igazolva. Következő coding prioritás:
main-funkció/metrika→natív útvonal/adatforrás/teszt megfeleltetés és a hiányzó
historikus/részletes egészségfelületek bekötése.

## Legfrissebb forrás és frissített dizájnátadó

Alkalmazásforrás **033386e**: main106 gyakorlat/13 program és négynyelvű
nevek/leírások natív adatcsomagból; adat-only resource, nem JS-motor. Hiányzó sorok
pótlása metadata admission+Room tranzakció+IGNORE mellett, meglévő egyedi értékek,
régi natív ID-k és programnapok megőrzése. Legacy importban builtin fordítás/mérési
klasszifikáció pótlása; explicit custom adatok megmaradnak. 7 új unit+1 UI tesztkód.
**Batch181 új unit+9 instrumentációs próbakód, még nem futott.** A main pontos
app/index blobjaiból106/13 kinyerés és reprodukálás, Node-szintaxis/diff/adatstruktúra
ellenőrzés történt, nem Android-teszt. Utolsó zöld továbbra dd4cc27.

A felhasználó külön kérte az eredeti négy mockup alapján mind a24 képernyő
újratervezését. Az új képek és hosszú konkrét specifikáció egy helyen:
[Native design handoff](design/NATIVE_DESIGN_HANDOFF.md), négyPNG a mockups/
könyvtárban, hash-manifesttel. Új review-változatok, nem tényleges emulátorképek;
a meglévő motorfeltételek elsőbbségét és a képfelirati eltéréseket az átadó rögzíti.
A vizuális fázishoz Medium jelzés elhangzott. A teljes Compose vizuális kidolgozás,
programszerkesztő, további adatút/lokalizáció/UI próbák, végső közös CI még hátra van.

## Legfrissebb alkalmazásforrás – gyakorlatonkénti Coach

**`ccc5c9a`**, `[skip ci]`. A korábban hiányzó gyakorlatonkénti tanács a meglévő
Coach képernyőhöz kapcsolódik, nem külön edzésmotor. A main végső tp140/tp128 és
rf233 szabályai: csak kompatibilis, legfeljebb90 napos, max4 előzmény; a program
által jóváhagyott history128 forrásokat külön ellenőrzi. Részleges edzés, fájdalom,
RIR-cél, rossz/romló teljesítés, stabil felső cél, testsúlyos változat és Coach
készenléti korlátozás elkülönül. UI csak javasol, nem módosít aktív/importált
edzést vagy programot. A súlyértelmezés (kézenként/összesen) megmarad.

A Room-szülők 32-es keyset lapokban, a megfigyelt súlylépcső 128-as distinct
lapokban jön, nincs teljes napló-relációtöltés/cache. A meglévő DataStore/backup
megtartja a progressionSteps felülírásokat. Coach-adatbetöltés ugyanazt a metadata
admissiont használja, hiba esetén korábbi tanács/cél nem marad frissnek látszó adat.
HU/EN/DE/RO új erőforrások. Terhelés/programváltozat tényleges alkalmazásának és
programszerkesztőnek teljes paritása továbbra külön ellenőrizendő.

Referencia kifejezetten `7473b30:www/app.js`, Git blob
`b5567576bed9a8f93608a6bf1ed8db1dee4723f4`. A másik `/workspace/TrainPilot` munkafa
`local-release-136` ágát nem módosítottuk/nem használtuk main-referenciaként.
`scripts/native/coach-reference.cjs` a pontos blobot ellenőrzi, a tényleges végső
JS-függvényekből 32 szintetikus referenciaesetet generált, ismétlésre azonos fixture-rel.
Ez csak JS-referencia előállítás, nem Kotlin-paritás eredmény. Erre épül32 új
paraméterezett Kotlin unit-eset, továbbá6 Room és1 portable-step unit, +1 Compose
read-only/advice UI-próba; ezek még nem futottak.

**Batch: 174 új unit + 8 Phone instrumentációs tesztkód megírva, még nem futtatva.**
Helyben Node-szintaxis, fixture-reprodukálás, négy resource-XML kulcs/hivatkozás,
diff és két SQLite-query szintaxis ellenőrzés. Nincs új Gradle/CI/APK/screenshot.
Utolsó teljes zöld forrás `dd4cc27`. Következő: teljes katalógus/nevek/lokalizáció,
további programszerkesztő/UI/adatút-paritás, majd közös GitHub-kapu. Élő telepítés STOP.

## Legfrissebb forrás – Coach-adatút és terminális mérési állapotok

**`29ac3a2`** / előzménye **`036a2cd`**, `[skip ci]`. A Coach és az Egészség
ugyanazt a `NativeHealthRefresh` foreground életciklus-védett, tényleges Samsung/HC
adapterhívását használja. Coachban közvetlen frissítő gomb és kezelt hibajelzés;
nincs kitalált mérés vagy automatikus engedélykérés. A korábbi célválasztási blokk
`NativeCoachTargets`-ba került: ISO-offsetek tényleges időrendje SQL julianday-jel,
lokális ma/későbbi osztályozás, 32-es oldalak, cancelled/hiányzó programnap skip.
A cloudDeviceId létrehozása is meglévő metadata admission alatt fut.

A nem-auto interval természetes lejárata nyugdíjazza a saját tervét, megtartott
időértékkel, így kézi OK és újraindítás lehetséges; régi tick kizárt. Elvetett
inaktív frame unimported eseménye/receiptje továbbra is megmarad, de más workout
visszaadása emiatt nem áll meg; ugyanaz a lezárt workout nem támasztható fel.
Új 6 célválasztási + 4 Health lifecycle + 3 terminal/handoff unit-próbakód és
1 Coach failure UI-próba. **Batch: 135 új unit + 7 instrumentációs tesztkód megírva,
még nem futott.** Helyben csak diff/resource XML és két SQLite dátumfüggvényes
feltétel ellenőrzése; ez nem Android/Room teszt, nincs új build/CI/APK/screenshot.
Utolsó teljes zöld továbbra `dd4cc27`. Élő adatos telepítés STOP.

**Copilot-ágellenőrzés (2026-10-24):** main `8a36502` változatlan; a
`feat/native-android-finalization` `6a21fc1` a saját `036a2cd` commitunkból indult,
+2 commit/7 környezet- és CI/dokumentáció fájl, appkódváltoztatás nélkül. A saját
`feat/native-android-migration` refet nem módosította. Mi továbbra is kizárólag a
migration ágra írunk; a külön ág setup/build scriptjeit nem futtattuk/nem emeltük át.

Következő: teljes per-gyakorlat Coach/progresszió a main végső felülírásai alapján
(nem az első rf152/rf233 definíció alapján!), katalógus/nevek/HU-EN-DE-RO, további
nyitott adatút/UI tesztek és teljes mockup. A provider-frissítés kódja sem SDK-val,
sem készüléken nincs most bevizsgálva; ezek külön kapuk maradnak.

## Friss munkablokk – intervallummód és hordozható preset

A meglévő PhoneWorkoutPlans/IntervalTimerService út támogatja az explicit
`autoAdvance=false` módot: egy aktuális sorozat, nincs automatikus done/léptetés;
a tényleges pozitív munkaidő stopnál vagy a körök végén megmarad, utána ellenőrizhető
és a meglévő kézi OK/undo használható. Auto módban a preset minden sorba állított
időalapú gyakorlatra azonos, a kézi sorozatok saját pihenőjét ez nem írja felül.

A meglévő DataStore timer-defaultok globális móddal bővültek. Program/quick start
azt veszi át, importált draft a saját interval110 beállítását tartja meg. Main
intervalPreset110 migráció és export/roundtrip bekötve; régi natív módhiány a korábbi
automatikus viselkedés marad, legacy preset explicit true hiányában nem automatikus.
A négy nyelvű opcionális panelen külön globális checkbox van, futó tervet a következő
indításra mentett konfig nem cserél le. Típus-/24h-validáció, 9 új unit + 1 új
Compose UI-próbakód (lokális/globalis hatókör), még futtatás nélkül.

**122 új unit + 6 instrumentációs tesztkód megírva, még nem futott.**
Diff és a négy XML kulcskészlete/resource-hivatkozások ellenőrizve; nincs új build,
CI/APK/kép. Utolsó zöld továbbra `dd4cc27`. Következő: Coach provider/per-gyakorlat,
további bekötések, teljes lokalizáció/UI, végül közös GitHub-kapu; élő telepítés STOP.

## Friss munkablokk – Phone elvetés és UI-írások

A telefonos elvetés a meglévő `TrainPilotWorkoutFinalization` közös mutexét és
metadata admissiont használja. Tartós, megkülönböztetett discard-fence mellett
megállítja a méréseket; tombstone és az azonos hash-ű legacy draft archiválása egy
Room-tranzakció. A natív discard sikerét ellenőrzi, és csak ezután zár/publikál,
illetve lép vissza a UI. Hiba/processzújraindítás után explicit elvetési retry;
függő mentést nem lehet elvetni, függő elvetést nem lehet menteni.

A testsúlyírás is admissiont kapott, a testsúlykülönbség zárójelhibája javítva.
Settings/programválasztás kapuelutasításból kezelt, négy nyelvű UI-hiba lett.
8 új unit-próbakód (valós Room/natív SQLite hibák, újranyitás, draft rollback,
stale/tulajdonos/fence) és 1 Compose UI hiba+retry próbakód készült.
**113 új unit + 5 instrumentációs tesztkód megírva, még nem futott.**
Helyben diff és a négy erőforrás-XML szerkezeti ellenőrzése történt. Nincs új CI,
APK vagy screenshot. Következő: intervallum/preset/nem-autoAdvance paritás, Coach,
lokalizáció és teljes UI; utolsó zöld forrás `dd4cc27`. Élő adatos telepítés STOP.

## Legfrissebb forrás – háttérírások és ZIP-replay

**`0f51320`**, `[skip ci]`: a meglévő replacement mutex írási admissiont kapott,
nem külön adatmotort. A korábban belépett metaadatíró végigfuthat; replacement
közben új, független író elutasított. Context-lease engedi az import/finalizer
saját egymásba ágyazott callbackjeit; érvénytelenített lease és replacement-epoch
védi a régi adatállapothoz tartozó várakozó munkát. Zársorrend: admission előbb,
utána finalizer/Room; a belső callback örökölt érvényes admissiont használjon.

Bekötve: teljes Wear háttérparancs, közös finish, migrátor import/export/egyedimentés,
DataStore set/reset/mark műveletek, naptári VM-módosítás és naplótörlés. Nem minden
szabadon hívható DAO vagy külön Health SQLite-írás globálisan zárolt. A bodyweight
UI-írás és explicit discard teljes útjának auditja/bekötése még szükséges; UI-ban
kezelni kell az elutasított régi callbackek hibaállapotát is.

ZIP redo megőrzi a teljes touched-journalt, és már telepített új fotót nem másol
át tévesen „eredetinek”, ha a művelet előtt az adott fájl nem is létezett.
5 új gate/cancel/reentrancia tesztkód, 2 valódi ZIP fájl/checksum/Room/draft/settings
roundtrip és megszakítás/replay próbakód. A fotófixture bináris adat, nem képrenderelési
bizonyíték. **105 új unit + 4 Phone instrumentációs próba megírva/nem futtatva.**
Csak diff/forrásellenőrzés; nincs új build/APK/kép, utolsó teljes zöld `dd4cc27`.

Következő: a még különálló írók és UI hibakezelés; interval/preset és Coach
per-gyakorlat funkciók a teljes tervből, majd teljes kritikus UI suite és a batch
végén összevont CI. Main/verzió változatlan, saját adatos telepítés STOP.

## Aktuális folytatás – Drive import és kontextus

Rövid, új chatben/tömörítés után olvasandó állapot: [NATIVE_RESUME.md](NATIVE_RESUME.md).
**369a4d0** `[skip ci]`: Drive merge hálózati szakasza után adatcsere-zár alatt
újraellenőrzi a tényleges canonical helyi tartalmat, majd a meglévő staged redo
útvonalat használja. Aktív edzés vagy közben módosult adat esetén nincs stale helyi
import. A hálózat idejére nem tartja a zárat. A cloud merge a privát koordinátákat
strukturálisan kiszűri feltöltés előtt. Ebben a körben külső Google-művelet nem futott.

Új3 canonical-guard unit-próbakód; összesen98 új unit + 4 Phone instrumentációs
próba megírva/nem futtatva. Utolsó zöld forrás továbbra dd4cc27, nincs új APK/kép.
A canonical check és az import közötti már belépett background Room-írások versenye,
cloud fotó-staging, teljes ZIP/fotó/Health roundtrip és a terv többi része még nyitott.

## Legfrissebb forrás – tranzakciós backup import és hordozható állapot

**`851391c`**, `[skip ci]`: a migrátor Room-oldali importja egy tranzakció;
a hordozható DataStore-beállítások csak sikeres Room-commit után, egyetlen editben
frissülnek. SQL-hiba nem törölhet részlegesen naplót és nem írja át előbb a nyelvet.
Ez két külön tár egymás utáni commitja, nem elosztott atomikus tranzakció; a helyi
redo-receipt továbbra szükséges DataStore/Health/fotó fázishibánál.

Backup roundtrip: időzítő/haptika/HC preferencia, súlytartomány, onboarding és
aktivitásonkénti GPS-választás; a helyi cloudDevice/migration/runtime identitás nem
importálódik másik eszközről. Draft/plannerSettings/plan/closed-workout dokumentumok
exportálódnak; replaceAll esetén hiányzó draft/plan nem élhet tovább. Privát nyers
WebView/GPS-trip archívum nem exportálódik. Új strukturális sanitizer koordináta
mezőket kiszűr, mérési értéket és route-keyt megőriz; ismeretlen kódolt/egzotikus
koordinátareprezentációk teljes privacy-auditja még külön szükséges.

A backup az összes törlésjelölőt exportálja, nem csak az utolsó2000-et; bulk import
előre beolvassa a teljes markerlistát, így a régi törlés sem támadhat fel, és nincs
edzésenként újra teljes marker-lekérdezés. A Health-store félbeszakadt pending
restore tokenjét redo előtt visszaállítja/lezárja, majd a kiválasztott payloadot
újra alkalmazza. Manager-szintű teszt-only checkpointok a receipt/photo/data/
Health/commit fázisoknál; nincs termékbeállításos hibainjektálás.

5 új backup unit-próba (valódi SQLite triggeres rollback, portable settings/device
identity, draft, 2101 tombstone, koordinátaszűrés) + 2 új recovery fault-próba
(több receipt és Health fázis). **95 új unit + 4 instrumentációs próba megírva,
nem futtatva.** Diff/forrásellenőrzés történt, nincs új CI/APK/screenshot;
utolsó teljes zöld forrás továbbra `dd4cc27`.

Következő: Google Drive merge/import ugyanebbe az adatcsere-folyamatba, már elindult
background Room-írások tranzakciós versenyvédelme; teljes ZIP/fotó/draft/Health
roundtrip és futtatott regresszió. Ezután interval/preset, Coach per-gyakorlat és
a teljes elfogadott terv többi bekötése/UI. Saját adatos telepítés STOP.

## Legfrissebb kód – megszakadt helyi adatcsere explicit folytatása

**`7296f0b`**, `[skip ci]`: a helyi JSON/ZIP import és erase tartós privát
redo-receiptet ír (`noBackupFilesDir/native-data-replacement.json`). A választott
JSON vagy ZIP-staging ellenőrzőösszeghez kötött; ZIP replay ismét ellenőrzi a
manifestet és a fotóhash-eket. Receipt a módosítások előtt, committed jelölés a
befejezés után; a committed replay már csak takarít. A félkész receipt megtartja
mindkét adatcsere-fence-et hiba/cancel/processzhalál után is.

Új startup recovery gate és Settings-helyreállító panel: tudatos „Művelet
folytatása”, vagy másik ellenőrzött JSON/ZIP kiválasztása. Nincs automatikus
unlock/törlés vagy új edzésindítás. Négy nyelvű erőforrások. A natív migration
kész jelölése megmarad explicit restore után, hogy újraindítás ne hozza vissza a
régi WebView tartalmat. Settings-mutációk/szinkrongombok helyi busy alatt tiltottak.

**Fontos szemantika:** ez a korábban kiválasztott célállapot idempotens, explicit
**előrefelé folytatása**, nem teljes előző állapotra visszagörgetés. Módosítás
utáni hiba esetén az app a recovery kapu mögött marad a folytatásig vagy új mentésig.
A megőrzött fotóeredetik replay során nem íródnak felül új fotóval.

7 új unit-próbakód: receipt/commit/path-validáció, zár megtartása, tényleges
meglévő importerrel JSON replay, sérült payload, cleanup-only commit és hiányzó
receipt fail-closed. Új Phone UI cold-start recovery próbakód raw screenshot
checkpointokkal. **Batch: 88 új unit + 4 Phone instrumentációs próba megírva,
még nem futtatva.** Csak diff/XML-struktúra ellenőrzés; nincs új CI/APK/kép.
Utolsó teljes zöld forrás továbbra `dd4cc27`.

Következő backend munka: teljes JSON/ZIP/Health/DataStore/fotó roundtrip és minden
crash-fázis tesztelhető hibainjektálása; Room tranzakciós import/Settings-kompenzáció
vizsgálata; Google Drive import és már belépett background Room-írások
konkurenciavédelme. Ezután a további interval/preset/Coach/paritás és teljes UI
lefedés. A működés készüléken még nincs igazolva, élő adatokra telepítés STOP.

## Legfrissebb kód – helyi restore/erase konkurenciavédelem

**`9c6f4a8`**, `[skip ci]`: a JSON/ZIP helyi visszaállítás és explicit erase
közös kizáró zárat kapott. Main szálon ownership-journal → snapshot sorrendben
ellenőrzi az aktív/paused/watch/native-only sessiont és a függő ownership-állapotot.
Tartós publikációs jelző + meglévő natív restoreBlocked flag tiltja a natív
indítást/recoveryt, háttérből snapshottelepítést és ownership receive-et.
Párhuzamos második adatcsere elutasítva; normál hiba/cancel mellett NonCancellable
felszabadítás. A sérült vagy processzhalálból megmaradt fence **nem törlődik némán**.
ZIP rollback hibánál az eredeti fotók és journal megmaradnak a helyreállításhoz.

8 új `NativeDataReplacementTest` megírva: phone/watch/paused/native-only védelem,
IO-s publikáció és indítás tiltása, hiba/cancel cleanup, konkurens lease, tartós
félbeszakadt fence és ownership-üzenet. **81 új unit + 3 instrumentációs tesztkód**
a batchben; még nincs fordítás/futtatás/CI/APK. Utolsó zöld forrás `dd4cc27`.

Fontos nyitott pontok: félbeszakadt tartós fence **explicit helyreállító UI/eljárása
még hiányzik** (a jelenlegi védelem fail-closed); a globális Room/DataStore/Health/
fotó restore teljes atomikussága és crash-fázisai, teljes mentés-visszaállítási
roundtrip még igazolandó/javítandó. Google Drive merge/import még nincs a közös
helyi lease alá bekötve; már belépett aszinkron parancsok összes Room-írásának
versenyvizsgálata is hátra van. Nem állítunk teljes restore-adatbiztonságot.
A következő backend lépés ezeket rendezni, majd az öt terület további bekötései.

Mainen csak az AGENTS.md thinking-szabály frissült felhasználói kérésre
(`8a36502d`); main alkalmazáskód/kiadás/verzió változatlan. Natív munka továbbra
külön ágon; élő adatokra telepítés és main merge nincs engedélyezve.

## Legfrissebb forrás – telefonos Compose UI-kapu alapja

**`1d38edf`**, `[skip ci]`: a meglévő `native-android.yml` új `phone-ui` jobja,
izolált `-PnativePhoneUiTests` AndroidTest source set és manifest. A megőrzött
hybrid AndroidTest forrás változatlan. Új Compose/UIAutomator teszt-APK, három
integrált smoke-próba: öt fül + tervezési belépések; négy nyelv/téma + visszatérés;
valódi alsó stopper → Settings → ugyanazon futó session, tényleges elhagyáskor pause.
A Coach-hoz ready/loading/error szemantikai jelölő került a stabil várakozáshoz.

`scripts/native/phone-ui-gate.py`: tesztcheckpoint kérés/nyugta, valódi előtéri
app-fókusz ellenőrzése és **változatlan `adb exec-out screencap -p` PNG**.
Manifestben forráscommit, két APK-hash, eszköz/API, téma/nyelv/fixture, képhash;
nyers instrumentation/logcat + jelölten származtatott XML, hibás/kihagyott próbánál
piros kapu. Ez megírt infrastruktúra, **nem tényleges screenshot vagy zöld UI-próba**.

Még nem futtatva/fordítva: felhasználói sorrend szerint a teljes kódbatch után
jön a közös CI. 73 új unit-próbakód + 3 új instrumentációs smoke-próba; a teljes
kritikus Phone UI lefedés (GPS, kétoldali, mentés/discard/retry, backup/restore,
0/200/1500 és további képernyők) még megírandó/bővítendő. A végleges dizájn/
paritás és az öt terület hátralévő munkája változatlanul nyitott. Utolsó tényleges
zöld forrás `dd4cc27`; nincs új APK/screenshot átadás vagy main merge.

## Kommunikáció/thinking megállapodás

Kód/bekötés: felhasználó High; dizájnfázis előtt jelezni a Mediumhoz;
CI-hibánál/komoly debug előtt jelezni Extra Highhoz. Részletek az AGENTS.md-ben.
Jelenleg kódolás és telefonos UI-teszt/CI-infrastruktúra írása zajlik, nem dizájnfinomítás.

## Aktuális befejezési batch – Napló, naptárszinkron, programnapi GPS

Legújabb alkalmazásforrás **`0a3865c`**, előzményei `7d5ac83`, `b018e6c`,
`1daf855`; minden köztes commit `[skip ci]`. A felhasználó rövid, lényegi
kommunikációt kér; a teljes elfogadott terv alapján önállóan kell folytatni.

- **Napló (`7d5ac83`)**: Jetpack Paging 3.3.6, 32-es kulcsalapú lapok, stabil
  epoch/id sorrend. Üres keresésnél csak összegző oszlopok, nincs raw JSON vagy
  sorozat/fotó/GPS reláció. Globális Unicode/ékezetfüggetlen keresés bounded
  128-as könnyű szövegprojekciókon; dátum/hónap SQL-szűrés helyi éjféli határokkal.
  Room tartalmi invalidáció, nem elemszám-cache; törölt/módosított sor frissít.
- Telefonos JournalViewModel/Screen már az új adatúton: részlet csak egy edzés
  megnyitására, reaktív törlés/módosítás, külön Gyakorlatok/Statisztikák/Forrás
  fülek; privát fotó-előnézet és néző, útvonal elérés. Tombstone + naplótörlés
  tranzakcióban, route-cleanup utána. Hiba/retry/loading/missing állapotok.
  HU/EN/DE/RO erőforrások. A történeti gyakorlatnevek egyelőre HU/EN-ben vannak,
  DE/RO esetén EN fallback; teljes katalógushoz kötött névparitás még szükséges.
- Több Phone/Wear route-key külön szegmensként olvasható, nincs köztes vonal;
  hiányzó rész jelölve. Fullscreen privát offline térkép piros X-szel, amely csak
  a térképet zárja. Az összetett ledger és külön phone-track átfedéseinek
  teljes deduplikáció/időrendi paritása továbbra külön integrációs ellenőrzés.
  A lista mini-route thumbnailje és a forrás-fül részletes Health mutatói még hiányoznak.
- **Naptárszinkron (`b018e6c`)**: `takeLast(100)` eltávolítva. Minden naptársor,
  régi skipped/lemondás is műveletet kap. Stabil Google/determinisztikus ID;
  meglévő DELETE404/410 és POST409→PUT viselkedés megmarad. Ez kód, nem végrehajtott
  Google-fiókos szinkronpróba; ebben a körben külső adatírás nem történt.
- **Programnapi GPS (`1daf855`)**: közös `startProgramWorkout` ad memóriabeli
  Launch célt, a korábbi `startWorkout` API megmarad. A kezdő kültéri aktivitásnál
  megjelenik a megjegyzett explicit GPS-választás; új sikeres saját sessionre
  egyszeri intent, recovery/repeated/watch-won startnál nincs GPS vagy preferenciacsere.
  Programváltozás stale választása publikálás előtt elutasítva; eredeti naptári
  tranzakció megmarad. Későbbi gyakorlatra nem indít néma GPS-t; ott explicit mérés kell.
  `0a3865c`: közös explicit nyelvű Resources helper, Activity context cseréje nélkül.
- **20 új tesztkód**: 8 paging/search/Room-invalidáció/DST; 3 route merge;
  3 naptári sync-terv (251 sor, régi lemondás, stabil ID); 6 program GPS/recovery.
  A batch összesen **73 megírt, még nem futtatott próba**. Helyben csak diff és XML
  struktúra/kulcsegyezés ellenőrzés. Nincs új Gradle/CI/emulátor/APK/screenshot;
  utolsó teljes zöld alkalmazásforrás továbbra **`dd4cc27`**.

Következő: teljes telefonos Compose UI-teszt/infrastruktúra megírása (a batch végén
futtatva); teljes interval/preset és Coach per-gyakorlat bekötés; backend restore
konkurenciavédelem + roundtrip, további UI/katalógus/lokalizáció/paritás és
Health/Phone↔Wear ellenőrzés a teljes terv szerint. Valós napló/frame teljesítmény
mérés emulátoron még nincs. Main/verzió változatlan, élő adatfrissítés továbbra STOP.

## Aktuális teljes-app munka – tématokenek után napló (2026-10-24)

A felhasználó ismét megerősítette: **a teljes hátralévő natív app a feladat**,
nem csak a témarendszer. A `NATIVE_FINAL_DELIVERY.md` minden funkcióterülete,
meglévő blokk/adatút bekötése, 24 nézet és teszt/CI/screenshot kötelező marad.

- **`a5538db`**: központi kontrasztos gombfelirat/akcentusszöveg tokenek,
  Basic matte vs. Vivid kijelölés; csoportos tördelődő 32-palettás picker,
  négy nyelv, radio semantics/testTag. Telefonos képernyők szétszórt fix
  gombfeliratszíneinek cseréje; natív motor/Wear UI változatlan.
- **`f105003`**: WearSyncRepository a Room `getWearMiniJournal` korlátozott
  lekérdezését használja (8 szülő, kronologikus epoch + stabil ID rendezés),
  nem az összes részletet tölti be a take(8) előtt. Új telefonos
  `JournalWorkoutSummary` részlet/rawJson/GPS/fotó nélküli projekció és laplekérés.
  **A telefonos JournalViewModel még a régi teljes részlet-flow-t használja**;
  következő konkrét munka az új projekció/UI, lapozás, teljes adathalmazos
  keresés/hónapszűrés és explicit részletbetöltés bekötése, helyes invalidációval.
- 4 új kontraszt/paletta és 2 Room-regressziós tesztkód (0/200/1500, stabil
  lapok, elemszámot nem változtató módosítás). Batch összesen **53 megírt,
  még nem futtatott próba**. Csak diff/forrásellenőrzés; nincs új Gradle,
  emulátor/CI/screenshot/APK. Utolsó teljes zöld forrás változatlan `dd4cc27`.
- Teljes tématoken vizuális paritás, kontraszt/UI bizonyítás, korábbi natív
  extra témák webes roundtripje és az öt terület maradéka még nyitott.
  Verzió/main nem változott, nincs élő adatokra telepítési ajánlás.

## Új alkalmazásforrás – témakatalógus és migráció első javítása

**`787bc8d`**, `[skip ci]`: main `7473b30` végső palettái alapján 10 Basic +
16 Vivid témakulcs/szín átvéve, a korábbi natív hat extra kulcs is megmarad
(összesen 32 választható paletta). HU/EN/DE/RO témanevek. A migráció most már
beolvassa a main gyökérszintű `themeAccent` témakulcsát is, `settings` objektum
nélkül; natív `settings.themeKey` elsőbbség, hiányzó/ismeretlen nem írja át a
választást. Exportban `themeAccent` kulcs, nem hex; natív kulcs külön is megmarad.
Négy új `NativeThemeParityTest` megírva, befagyasztott main-paletta fixture-rel;
a batch összesen **47 megírt, még nem futtatott próba**. Csak forrás/diff ellenőrzés,
nincs új Gradle/UI/CI/APK; utolsó teljes zöld forrás `dd4cc27`.

Még hátra: teljes tématoken/kontraszt és Basic/Vivid megjelenés, csoportos picker,
összes termékoldal fix akcentusainak cseréje, valódi DataStore/restore/upgrade
roundtrip és UI-tesztek több témával. A hat csak natív paletta webes visszaimport
kompatibilitását is külön rendezni kell; nem állítunk teljes cross-runtime paritást.
Ezután a teljes terv további moduljai. Main/Wear/verzió változatlan.

## Elfogadott teljes befejezési feladat (2026-10-24)

A felhasználó jóváhagyta a [teljes feladatot és pontosításokat](plans/NATIVE_FINAL_DELIVERY.md).
Ez alapján dolgozunk: öt funkcióterület + 24 nézet + main teljes témakatalógus/
migráció + HU/EN/DE/RO, utána közös CI és valódi emulátoros képernyőképek.
Első konkrét munka: hiányzó main-paletták és témakulcs migrációja. Nincs main merge,
verzióemelés vagy saját adatokra telepítési ajánlás. Az új Phone UI/screenshot
kaput a meglévő natív workflow-ba kell megírni; fizikai Health/Wear próba külön.

## Új Compose-elfogadási feltételek – csak rögzítve (2026-10-24)

A felhasználó hat pontosítást adott a mockup motorhoz kötéséhez:
[hat kötelező bekötési feltétel](design/NATIVE_MOCKUP_REVIEW.md#kötelező-compose-bekötési-feltételek--felhasználói-pontosítás-2026-10-24).
Dinamikus draft/aktív folytatás; pihenő Skip/±15 mp; gyakorlatváltás megmaradó
pihenővel; „Mentés vázlatként” = pause/kilépés és külön discard; Samsung/TrainPilot/
korlátozott Coach score külön jelentése; friss valós pulzus vagy „— bpm”.
**Ebben a körben csak dokumentáció**; alkalmazásforrás `4f60bc0`, utolsó teljes
zöld forrás `dd4cc27`, 43 új batch-próba továbbra megírt/nem futtatott.
A hat feltételt a további implementációban és az összevont UI-próbákban kell
ellenőrizni; a történeti motorkapu nem igazolja az új kijelzést/adatbekötést.

## Legfrissebb alkalmazásforrás – öt fül és globális Beállítások (2026-10-24)

- **`4f60bc0`** a migrációs ágon, `[skip ci]`. Öt alsó fül:
  Kezdőlap / Edzés / Napló / Fejlődés / Egészség. Kompakt közös toolbar,
  jobb felső 48dp Beállítások fogaskerék és másodlagos oldalakra visszanyíl.
  HU/EN/DE/RO navigációs feliratok, UI-próbához szemantika/testTag.
- A Beállítások az aktuális oldal fölé kerül, ugyanaz az aktív editor backstack
  és ViewModel marad; nincs `leaveWorkout`, sessionindítás vagy implicit pause.
  A tényleges fülváltás/edzéselhagyás megtartja a telefonos pause-t.
  Befejezés alatti navigáció védett; sikertelen késői lezárásnál a meglévő
  editorhoz visszatérés elérhető marad az újrapróbáláshoz. Órán közben lezárt
  edzésből vissza nem vezetünk a régi editorba. Ez **kódban bekötött, még nem
  futtatással igazolt viselkedés**.
- Programok/Naptár/Coach mind a Kezdőlapon, mind az Edzés oldalon tördelődő
  gyorsgombokkal elérhető. Az aktív jelzősáv a bottom scaffold része, helyet foglal,
  nem lebeg rá a lista utolsó gombjára. A toolbar csak `isFinishing` változását
  figyeli, nem iratkozik fel az editor összes 250ms stopper-frissítésére.
- Beállítások UI/SAF callback védelme tiltja az aktív telefonos/órás/paused vagy
  csak natív tárban élő edzés alatt a restore-t/törlést. **Ez UI-szintű előfeltétel,
  nem a teljes restore konkurenciavédelmének igazolása**; a backend tranzakciós
  restore/start/remote-start versenyteszt és védelem a 4–5. területben még hátra van.
- `NativeMainMenuTest` 4→10, új `NativeSettingsProtectionTest` 5: a batchben
  összesen **43 megírt, még nem futtatott próba**. Most forrás/hívási út és
  `git diff --check`; nincs új Kotlin/Compose/Room fordítás, emulátor vagy APK.
  Az utolsó teljes zöld alkalmazásforrás továbbra **`dd4cc27`**.
- A választott dizájn navigációs váza készült el; a 24 mockupoldal teljes
  vizuális megvalósítása, integrált telefonos UI-tesztkód és az öt terület
  hátralévő részei folytatandók. Main/verzió/Wear termékforrás változatlan;
  saját adatokra telepítés továbbra STOP.

## Legfrissebb felhasználói UI-döntés (2026-10-24)

A mockup áttekintése után **az öt alsó füles változat elfogadva**:
Kezdőlap, Edzés, Napló, Fejlődés, Egészség. A korábbi nyolcgombos döntés
felülírva. Beállítások: jobb felső fogaskerék minden főoldalon/aktív edzésen;
vissza ugyanoda, a megnyitás önmagában nem pause/félbehagyás vagy új session.
Mentés/lezárás és védett műveletek zárolása megmarad.
[Részletes vizuális döntés](design/NATIVE_MOCKUP_REVIEW.md).
A döntést `b16d092` rögzítette; az ötfüles navigáció és a globális Beállítások
első bekötése azóta **`4f60bc0`** (részletek fent). A végső vizuális és
működési ellenőrzés még hátra van. Nincs új build/teszt/APK;
utolsó teljes zöld forrás továbbra `dd4cc27`.

## Előző kód és UI-döntés – az új döntés felülírja (2026-10-24, még nincs új tesztkapu)

- Új alkalmazásforrás **`8fb3ff6`**, előzménye `9b09af7`: Coach bekötve valós
  HealthJournalStore napokhoz és bounded 14 nap/3 edzéshez; szakaszalapú alvás,
  forrásazonos, legalább 3 korábbi napos HRV-bázis, valós naptári/ciklikus célpont.
  Coach screen/route, Health és Fejlődés elérés. Per-gyakorlat progressziós
  tanácsok és teljes readiness modal vizuális egyezése még hiányzik.
- Felhasználó kérésére nyolcgombos main-menü első natív szerkezete bekötve,
  az ötfüles alsó navigáció levéve. Edzés és Programok külön oldal; aktív Edzés
  ugyanabba a sessionbe tér vissza, menüből explicit elhagyás a meglévő VM pause
  útján megy. A teljes main UI vizuális egyezése **nincs igazolva**; több oldal
  importált Compose-layoutja még eltér, referencia szerinti rendezés szükséges.
- Új 6 `NativeCoachSignalsTest` és 4 `NativeMainMenuTest`: a batchben összesen
  **32 új megírt, még nem futtatott próba**. Nincs új fordítás/APK/CI-igazolás;
  utolsó teljes zöld forrás továbbra `dd4cc27`. `[skip ci]` munkamód változatlan.
- Ezután a felhasználó az ötfüles alternatíváról mockupot kért áttekintésre:
  4 kép/24 nézet elkészült a chatben. **Ez nem redesign-jóváhagyás**.
  [UI-döntés és képtáblák](design/NATIVE_MOCKUP_REVIEW.md). Átadáskor a képeket
  külön kell mellékelni, ha a következő modellnek látványtervként szükségesek.
- Következő kódolás: main vizuális egyezés, Coach hiányzó per-gyakorlat részei,
  majd az ötpontos terv további pontjai/telefonos UI-tesztkód. Coach célpont SQL
  startIso szöveges rendezésének timezone/offset-paritását külön ellenőrizni kell.
  Saját adatokra telepítés továbbra STOP; nincs main merge vagy verzióemelés.

## Új kódolási batch – még nem bevizsgált forrás (2026-10-24)

- Legfrissebb alkalmazásforrás **`d134785`**, előzményei `e05ff57`, `3921c10`.
  Mind a migrációs ágon, `[skip ci]` jelöléssel: a felhasználó előbb az öt terület
  kódját kéri, utána összevont tesztelést. Új APK/fordítási/teszteredmény nincs;
  az utolsó teljes zöld kapu továbbra **`dd4cc27`** lent.
- Naptár: havi bounded Room-nézet, naplózott edzések, CRUD/áthelyezés/skip/restore,
  szinkronizálható lemondás, CAS és aktív/teljesített védelem. A naptári indítás
  scheduleId-val kapcsolódik a közös natív sessionhöz/lezáráshoz. UI/nav bekötve,
  HU/EN/DE/RO feliratok; 11 megírt, még nem futtatott új teszt.
- Coach: csak a tiszta readiness számítási mag portolva a main végső policyjéből,
  külön Samsung/TrainPilot és frissesség/terhelési korlátozásokkal. 11 további
  megírt, még nem futtatott teszt. **Coach adapter/adatbetöltés és UI/nav még nincs
  bekötve**, a Coach gomb egyelőre trainingre visz; ez a következő konkrét lépés.
- Részletek és további sorrend: [batch terv](plans/NATIVE_COMPLETION_BATCH.md).
  Az öt terület nincs késznek jelölve. Verzió/Wear termékforrás változatlan,
  nincs main merge/kiadás, saját adatokra telepítés továbbra STOP.

## Aktuális irány – előbb kód, utána összevont kapu (2026-10-24)

Felhasználói kérés szerint az öt hátralévő terület kódját és tesztkódját előbb
összeállítjuk a meglévő Kotlin-blokkok felhasználásával, utána teszteljük együtt.
[Rögzített sorrend és forrásellenőrzés](plans/NATIVE_COMPLETION_BATCH.md).
70 fájl történetileg importálva; nem minden blokk kész/bekötött funkció.
Most nincs alkalmazáskód-változás vagy új teszteredmény. Következő: a meglevő
források funkciónkénti bekötése/javítása és a telefonos Compose UI-kapu kódja;
a teljes futtatást a batch végére időzítjük. Köztes commit `[skip ci]`.
Utolsó zöld forrás továbbra `dd4cc27` lent; main/verzió változatlan.

## Legfrissebb zöld kapu – importált draft natív átvétele (2026-10-24)

- **Bevizsgált CI-/APK-forrás `dd4cc273899fef9f631896e9fa0e223f2b6eb7fb`**; [natív CI 40072751592](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40072751592) **teljes SUCCESS, 6/6 job**. Letöltött XML: telefon **393/393**, Wear **167/167**; mind az előző **540** unit-eset megmaradt, +**20** új próba (12 preflight, 6 átvétel, 2 cleanup/finalization). Kerek Wear **183/183**, azonos esetazonosítókkal; eredeti és gyorsított 1.3.71-ről emulátoros signed upgrade **2/2**, nulla hiba/kihagyás. Dokumentációs HEAD eltérhet a tesztelt APK forrásától. A korábbi köztes futások leváltott/cancelled futások, nem zöld bizonylatok.
- `resumeImportedDraft`: közös start-lock/recovery; Room-tranzakciós library/history ID+syncId+started/tombstone/closed-watch ellenőrzés, hiányzó megjelenítési/mérési meta és timer-config pótlása. Mért érték, done/RIR, útvonalhivatkozás és meglévő ID megmarad; hiányzó set-ID determinisztikus. SHA256-kötésű eredeti archívum, tartós natív frame, majd ActiveStore projekció. A draft átvételkor nem törlődik; az edzés szünetben áll helyre, nincs automatikus GPS/stopper. Ismételt indítás a már meglévő sessiont adja.
- Home a Room draft-flow alapján mutatja a **Félbehagyott edzés folytatása** gombot; paused editorban külön tudatos folytatás kell. Mentéskor tartós napló után, még retryképes finalization-pending frame mellett csak a hash szerint egyező eredeti draft archiválható/törölhető; újabb eltérő draft védett. Elvetéskor tombstone után ugyanaz a `retire` tranzakció. SQL-hibák/retry és újabb draft védelme az új unit-próbákban zöld. A teljes telefonos UI-út ettől még külön ellenőrzendő.
- [Gépi bizonylat és APK-hash](builds/2026-10-24-native-legacy-draft.json). Mindkét letöltött APK **com.repforge.app /1.3.71-native-test /2736**, eredeti signer/csomag/verzió/hash/ZIP-épség és új draft-protokoll a telefonos R8 DEX-ben ellenőrizve. [Phone signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40072751592/artifacts/12551814173), SHA256 `5d5c82faa70b27e68ecd2fd80c6e36684e9d26c494d74d49b0e23005906d150d`; [Wear signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40072751592/artifacts/12552251982), SHA256 `57df1f4418bd1199b7236b1a3c58ac4e8e839b6b2f1de316619bb2afa98ac24d`. Wear/upgrade XML jelölten származtatott nyers runneradatból, nem eredeti JUnit. Wear/shared termékforrás, main `7473b30` és verzió változatlan; nincs merge/kiadás.
- **Teljes natív app és saját adatokra telepítés továbbra STOP. Következő fő lépés: integrált telefonos Compose Folytatás → mérés → mentés/elvetés UI-kapu**, post-publication crash és history-alias/discard végponttól végpontig regresszió. Utána programnapi kezdeti GPS-választás, további intervallummód/globális preset, Naptár/Coach/katalógus/nyelvek/nagy napló és közös watch/phone útvonalszakasz UI. Teljes backup/restore, valós Phone/Watch7 és kétkészülékes offline szinkron külön hiányzó kapu. A `NativeLegacyDraft` félrevezető régi fejlécét a későbbi kódolási batch már pontosította.

## Korábbi zöld kapu – natív gyors/kültéri indítás (2026-10-23)

- **Bevizsgált CI-/APK-forrás `9da639d09653ce46cd1eed295b24664ca53bb3df`**, core `3eb8eb4`; [natív CI 40017855126](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40017855126) **teljes SUCCESS, 6/6 job**. Letöltött XML: telefon **373/373**, Wear **167/167**, mind az előző **525** unit-eset megmaradt, +**15** `NativeQuickWorkoutTest`. Kerek Wear **183/183**, azonos esetazonosítókkal; eredeti és október 22-i gyorsított 1.3.71-ről signed upgrade **2/2**, nulla hiba/kihagyás. Dokumentációs HEAD eltérhet a tesztelt APK forrásától.
- A Home gyors/outdoor gombja valódi könyvtárválasztóba vezet, nem nemlétező programnaphoz. Programfüggetlen, stabil ID-s quick session; közös #228 előtöltési szabály program/quick/+gyakorlat esetén, mért érték üres. Gyorsedzésben a már hozzáadott gyakorlat újbóli választása nem készít másolatot. Új indítás nem ír felül aktív edzést, órás mirrort vagy importált draftot; SQLite-hiba után nincs publikált session/mentett új GPS-választás, retry védett.
- Kültéri allowlist és indításkor látható, aktivitásonként megjegyzett GPS-választás, HU/EN/DE/RO választó. Csak új, kifejezett indítás ad memóriabeli GPS-szándékot; recovery, ismételt tap, órás mirror és háttér nem játssza újra. A meglévő GPS-parancs permission/rendszerhely/owner/set/generation ellenőrzése megmarad. Első engedélykérés a resumed editorban, controller readiness után; sikertelenség nem törli az edzést. Útvonalpontot/távot nem gyártunk. Ez core-unit és forrás/build igazolás; az új telefonos Compose folyamat tényleges UI-próbája még külön hiányzik.
- [Gépi bizonylat és APK-hash](builds/2026-10-23-native-quick-outdoor.json). Mindkét letöltött APK **com.repforge.app /1.3.71-native-test /2736**, eredeti signer, hash/ZIP-épség és új telefonos GPS-választás R8 DEX-ben ellenőrizve. [Phone signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40017855126/artifacts/12529971616), SHA256 `a3872eb3d18329bb25b19efdd2c56d846da584140836f5f5ba60bfa211909616`; [Wear signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40017855126/artifacts/12529681897), SHA256 `e6cbec996d8eb237ef184fcd825144e503468e62d3d18bbfe3e0df2264a04a6f`. Wear/upgrade XML jelölten származtatott a nyers runnerlogból, nem eredeti JUnit-riport. Eredeti Wear/shared termékforrás változatlan.
- **Teljes natív app és saját adatokra telepítés továbbra STOP.** Main `7473b30`, verzió változatlan, nincs merge/kiadás. **Következő fő lépés: importált draft tényleges natív folytatása**. Régi forrás: `persistDraft` `{session,workout,current,restEndAt,savedAt}`; sorozatok/mért értékek/ID-k megőrzése, nem teljesítésnek vett szünet, tulajdonosi/finalizálási/törlési kerítések; csak tartós natív átadás után, azonosított régi drafton lehet takarítani. Általános programnapi induló GPS-választás, további intervallummód/globális preset, Naptár/Coach/katalógus/nyelvek/nagy napló és közös watch/phone útvonalszakasz UI még nyitott. Integrált telefonos UI, teljes backup/restore és valós Phone/Watch7/offline szinkron külön kapu.

## Korábbi zöld kapu – kézi mért rögzítés és közös időnapló (2026-10-23)

- **Bevizsgált CI-/APK-forrás `40384e76feefbefa9d12b57d314e8cb63b08b627`**, core `aca4ac3`, UI `a5f4cd8`; [natív CI 40011226981](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40011226981) **teljes SUCCESS**. Letöltött XML: telefon **358/358**, Wear **167/167**; mind az előző **505** unit-eset megmaradt, +**20** új próba (19 `NativeMeasuredEditingTest`, 1 GPS-időnapló). Kerek Wear **183/183**, azonos esetazonosítókkal; eredeti/gyorsított release-ről signed upgrade **2/2**, nulla hiba/kihagyás. Dokumentációs HEAD eltérhet a tesztelt APK forrásától.
- `completeMeasuredSet`: a kézi idő/táv OK a közös natív record/outbox/receipt/undo tranzakcióba kerül. Nincs külön UI-pipa/fiktív pihenőindítás. Kétoldalihoz két pozitív idő, távolsághoz pozitív idő+táv kell; target/generation/revision/érték-egyezés, futó mérés és tulajdonos védett. SQL-hiba után nem kész, újrapróbálható; replay egy rögzítést ad. Kézi másodperc nem gyárt megfigyelt munkaidőt. Legacy `completeSet` és korábbi fingerprint-formátum változatlan.
- `NativeMeasuredTime`: az alsó stopper és GPS megfigyelt ideje közös `timeLogs162`/`automaticTime162` naplóba jut. Folytatás/undo/reset nem duplázza és nem törli a tényleges fizikai időt; külön mérési szakaszkulcsok. Stopper implicit szünete ugyanabban a folyamatban mérhető; másik folyamat/tulajdonos csak tartós checkpointot ismer, kiesett időt nem találunk ki. Új mérés/gyakorlat lezárja az előző szünetét. Élő UI-összesítés memóriában, nincs extra folyamatos SQLite-írás a szünetben álló stopper miatt. GPS idő a Room nyers naplójába és `timeLogs162Json` mezőjébe is eljut (meglévő két lezárási próba új állításokkal).
- Opcionális, összecsukott, **edzésre érvényes** intervallum/pihenő-beállítás. Egyoldali időalapú sorozathoz külön tudatos **automatikus** indítás; ilyenkor csak az intervallum órája látszik. Kétoldali/GPS saját vezérlés marad. Sorozatidőbe csak tényleges munka kerül, pihenő külön. Részleges explicit leállítás megőrzi az időt, nem teljesít; visszavont kulccsal késői callback elutasítva. Futó tervet a jövőre mentett beállítás nem cserél le. Általános nem-autoAdvance mód/globális preset további paritási feladat, nincs késznek jelölve.
- [Gépi bizonylat, tesztek, APK-hash](builds/2026-10-23-native-measured-intervals.json). Mindkét letöltött APK **com.repforge.app /1.3.71-native-test /2736**, eredeti signer/csomag/verzió/hash igazolva; új measured protokoll és időnaplómezők a telefonos R8 DEX-ben is jelen vannak. [Phone signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40011226981/artifacts/12526692755), SHA256 `1389409ea3b3fab298d251a52ef58698ce53ac88d3aabbfbf8e3bc2c83df963a`; [Wear signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40011226981/artifacts/12526877324), SHA256 `0e559ff65e09a97f7546c427139566ba601a14282f9fcdaf3c639f9cc899dbd8`. Wear/upgrade XML jelölten származtatott a nyers runnerlogból, nem eredeti JUnit-riport. Első `a5f4cd8` fordítás hiányzó `SystemClock` importnál állt meg; `40384e7` javítja, a tesztelvárások megmaradtak.
- **Teljes natív app és saját adatokra telepítés továbbra STOP.** Main `7473b30`, verzió/eredeti Wear termékkód változatlan; nincs merge/kiadás. Következő fő lépés: gyors/outdoor és megjegyzett GPS-választás, majd importált draft. Utána Naptár/Coach/katalógus/nyelvek/nagy napló; további intervallummód/globális preset külön nyitott paritási pont. Integrált telefonos Compose UI, teljes backup/restore és valós Phone/Watch7/offline szinkron külön kapu. Watch→Phone közös útvonalszakasz UI is még vizsgálandó.

## Korábbi zöld kapu – GPS sorozatkötés és valódi pihenővezérlés (2026-10-23)

- **Bevizsgált CI-/APK-forrás `922b6b75d5ac87c8a9bee77e896549dacd5b939b`**, termékkód `e164ef3`; [natív CI 40000812477](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40000812477) **teljes SUCCESS**. Letöltött XML: telefon **338/338**, Wear **167/167**, a korábbi **478** unit-eset mind megmaradt, +**27** új próba (17 GPS, 10 rest). Nyers kerek UI **183/183**, azonos esetazonosítókkal; aláírt eredeti/gyorsított upgrade **2/2**, nulla hiba/kihagyás. Dokumentációs HEAD később eltérhet az APK forrásától.
- `NativeGpsMeasurement`: stabil workout/generation/exercise/set és egyedi measurement-ID, privát route-key. A szolgáltatás tartós explicit stop-nyugtája csak pozitív idő+táv és hibamentes befejezés után jut a natív `completeGpsSet` record/outbox/receipt/undo tranzakcióba, külön OK nélkül. Implicit stop/pause/navigáció/workout finish nem teljesít, de a részadatokat és térképhivatkozást megőrzi. SQL-hiba utáni retry egy rögzítést hoz létre. Régi mérés callbackje/befejező gombja nem állíthat le új mérést; függő service-start visszavonható. A közös workout-pause út is leállítja a kötött GPS-t, régi generation/revision ezt nem teheti meg. Koordináta nincs a JSON-frame-ben.
- `NativeTimerTarget`: a renderelt key/workout/generation vezérli a valódi skip/±15 mp órát. A rest-only terv módosítása tartós és SQL-hiba esetén visszaáll; kihagyott/hozzáadott virtuális másodperc nem tényleges pihenő. Sima editor-edit nem dobhatja el a service tervét. Gyakorlatváltás megtartja a pihenőt; a lejárat nem írja vissza az explicit kiválasztást, az alsó stopper ilyenkor csak szünetel.
- Telefonos UI: aktív gyakorlat és Előző/Következő, GPS panel pontos helyengedély-kéréssel és renderelt measurement-ID-val, összecsukott kézi táv/idő, közös aktív/napló privát útvonalolvasó. A **piros X csak térképet zár**, nem állít GPS-t és nem teljesít. Android-visszalépés tartós workout-pause; háttérbe kerülés önmagában nem.
- [Gépi bizonylat, tesztek és APK-hash](builds/2026-10-23-native-gps-rest.json). Mindkét letöltött APK **com.repforge.app /1.3.71-native-test /2736**, eredeti signer igazolva. [Phone signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40000812477/artifacts/12522116570), SHA256 `382f1fedf95c697eefc279da4c5449b61bd2637634d8e36a23e3caf6d1436158`; [Wear signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40000812477/artifacts/12521673004), SHA256 `7f5e69b8373fb2aef30f1879333d51360ece5e14a2dd209fd52e39999487dabd`. A measured protokoll a telefonos R8 DEX-ben is jelen van. A Wear/upgrade XML jelölten származtatott a nyers runnerlogból, nem eredeti JUnit-riport.
- Előzmény: `076b1e8` debug/unit/signing zöld, teljes futását a közös pause-javítás váltotta. `e164ef3` telefon 338/1 hiba: egy új teszt a hiányzó, alapértelmezetten hamis `workoutPaused` mezőt `getBoolean`-nal olvasta. `db542d6` kizárólag ezt a teszthibát javította `optBoolean`-ra, a termékkód és minden régi elvárás változatlan. `db542d6` első Wear-futásában egy 160dp korona-fókuszvárakozás timeoutolt; a logban setupwizard/SystemUI megjelenés. Ugyanazzal az APK-val megismételve is ugyanott hibázott (182/183). `1657023`: csak CI-előkészítés stabil home-fókuszra várással és hibaablak-diagnosztikával; az első readiness-olvasás `dumpsys window windows` részkimenetéből hiányzott a fókusz, ezért a tesztek nem indultak. `922b6b7` a teljes `dumpsys window` kimenetet olvassa; a megőrzött emulátordumpon helyben ellenőrizve. Termék Wear-forrás és tesztelvárás változatlan, a végső friss kapu ezt igazolja.
- **A teljes natív app továbbra sem kész; saját adatokra telepítés STOP.** Main `7473b30`, verzió és eredeti Wear implementáció változatlan; nincs merge/kiadás. Következő: kézi measured OK/undo, opcionális összecsukott intervallum-beállítás és teljes mérési időösszesítés; majd gyors/outdoor, megjegyzett GPS-választás és importált draft. Naptár/Coach/további funkcióegyezés, nagy napló, integrált telefonos UI, teljes backup/restore és valódi Phone/Watch7 külön kapu. Watch→Phone folytatásnál a két forrás útvonalszakaszainak közös UI-ja még vizsgálandó.

## Korábbi zöld kapu – stabil UI-célzás és alsó stopper (2026-10-23)

- Bevizsgált **alkalmazás-/APK-forrás `b6e815824ae0e4c0936959863afd81ff32ef80d8`**, [natív CI 39965579401](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39965579401) **teljes SUCCESS**. Letöltött XML: telefon **311/311**, Wear **167/167**, a korábbi **459** unit-eset mind megmaradt, +**19** új próba. Nyers kerek UI **183/183**, azonos esetazonosítókkal; aláírt eredeti/gyorsított upgrade **2/2**, nulla hiba/kihagyás. Dokumentációs HEAD később eltérhet; a lentebbi `9492755` az előző kapu.
- A telefonos set callback most `NativeWorkoutTarget(workoutId, generation, exerciseId, setId)` célpontot visz; sorszám/átrendezés nem irányítja más sorozatra. Ismételt gyakorlat külön set-ID listakulccsal, új sorozat friss UUID-vel. Eltűnt/dupla/új edzéshez tartozó célpont elutasítva.
- `NativeSetStopwatch`: egy eredeti alsó stopper, a kanonikus natív frame-ben; explicit pozitív stop az új `completeTimedSet` natív parancson át, ugyanabban a tranzakcióban frame+esemény+receipt+undo. Kétoldali első pozitív oldal tartós, még nem kész; mindkettő után automatikus kész. Nulla/reset/pause/controller-leállás nem teljesít. A régi `completeSet` továbbra sem fogad el időmérést, és a régi receipt-fingerprint byteformátuma változatlan.
- Meglévő foreground controller másodpercenként checkpointol; folyamatmegszakadáskor csak a tartós checkpoint áll helyre szünetben (normál checkpointolás mellett legfeljebb az utolsó másodperc vész el). Azonosított régi controller nem szüneteltethet új workout/generation mérést. Sima edit nem írja felül a natív stopperállapotot. A natív editor kézi rögzítése utáni pihenő nem indítja el automatikusan a következő időalapú sorozatot/felső stoppert; legacy automatikus terv viselkedése megmarad.
- **19 új regressziós teszt** (14 stopper, 5 célzás) a letöltött XML-ben mind zöld. Helyben csak diff/szerkesztés, Gradle/emulátor/APK GitHubon. Valós telefonos Compose UI-próba még külön hiányzik; unit nem helyettesíti. A Compose callback a rendereléskori teljes célpontot rögzíti, nem a későbbi State-delegált workout ID-ját olvassa. Az undo ismételt gyakorlatazonosítónál is a set-ID-hez tér vissza. Nyugvó stopper nem írja másodpercenként az SQLite-t; explicit újraindítás újraélesíti a checkpointot. Tulajdonosi átadás előtt a stoppernek tartósan szünetelnie kell; a timeline nélküli ARM controller visszavonása is azonnal pause, soha nem completion.
- [Gépi bizonylat, tesztek, forrás, APK-hash](builds/2026-10-23-native-set-stopwatch.json). Csomag/verzió mindkettőn **com.repforge.app /1.3.71-native-test /2736**, eredeti signer a letöltött APK-kon is ellenőrizve; a telefonos R8 DEX tartalmazza az új stopperparancsot. [Phone artifact 12506433788](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39965579401/artifacts/12506433788), SHA256 `be2c4becd3fc18bd150cae026d18ed78ae5b327455b45a0b4de53fcc479b9e1c`; [Wear 12506593127](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39965579401/artifacts/12506593127), SHA256 `cd933c9b29ede7808bf80927d0abe8c5c9b0c1a5b8d36c686e7b641cc75c6d88`. CI-bizonylatok, **nem saját adatokra telepítési ajánlás**. A Wear/upgrade XML jelölten származtatott a nyers runnerlogból, nem eredeti JUnit-riport.
- Következő: **GPS set/route kötés és pozitív explicit stop automatikus teljesítés**, valós skip/± pihenőparancs és gyakorlatváltás; kézi idő-OK natív undo és intervallum-beállítás, mérési időösszesítés teljes paritása. Utána gyors/outdoor, importált draft, Naptár/Coach/további paritás. Valós integrált telefonos Compose UI-próba és teljes backup/restore még hiányzik. A teljes natív app és saját adatokra telepítés **STOP**, main `7473b30`, verzió változatlan.

## Korábbi zöld kapu – natív editor és motor (2026-10-23)

- Korábban bevizsgált **termék-/APK-forrás `9492755797171fe728423050f0295f91a48244bb`**, [CI 39957648783](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39957648783) **teljes SUCCESS**. XML: telefon **292/292**, Wear **167/167**; a korábbi 451 unit-eset mind megmaradt, +8 új editorpróba, nincs hiba/kihagyás. Nyers kerek UI **183/183** egyedi eset, az előző kapuval azonos esetazonosítók; aláírt upgrade **2/2** az eredeti és a gyorsított 1.3.71-ről. **Élő telepítés továbbra is STOP; a teljes natív app nem kész.**
- `bf219eb` új `NativeWorkoutEditing`: teljes natív frame az editor tartós forrása; `PhoneWorkoutExecutionStore.projection()` egy SQLite-tranzakcióban ad frame+eseménykurzort. Snapshot commit előtt nincs nyugta, sikertelen UI-publikálás után a natív szerkesztés visszaállítható; stale UI nem írja felül a natív rögzítést. Complete/undo a natív parancsmotoron halad, foreground controller csak a látható/resumed editorból armolódik; háttérbe kerülés nem pause. `ownerEpoch193` átadva. Lezárt snapshot nem éleszthető újra.
- Első `bf219eb` CI **39956933123**: fordítás zöld, telefon unit **291 összes /8 hiba**. Hét korábbi API-teszt jogosan függő eseményt várt a régi háttérprotokollnál; az új publisher túl korán ackolt. Egy új teszt nem armolta a rögzítéshez előírt controllert. **9492755 javítja:** csak az új, teljes `nativeEditorProjection=1` frame optál be a tartós natív fogyasztóba, a régi protokoll korábbi szemantikája változatlan. Új tesztek valódi Robolectric service ARM-mal, nincs gyengített régi elvárás. Mind a **8 új editorpróba zöld**: valódi szerviz ARM/record/undo, tulajdonosi epoch, sikertelen SharedPreferences commit mellett nincs ack/adatvesztés, stale edit, régi kurzor, lezárt/órás/frozen snapshot védelem. A régi protokolltesztek elvárásai változatlanok.
- [Gépi bizonylat, forrás, esetek, APK-hash és korábbi hiba](builds/2026-10-23-native-editor-integration.json). **1.3.71-native-test /2736**, `com.repforge.app`, eredeti signer mindkét letöltött APK-n ellenőrizve. [Phone artifact 12503517077](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39957648783/artifacts/12503517077), SHA256 `1e11c8dbe7f1dc89994d7d61b09455022d751bec0fa4016e3ed4e507a57e2101`; [Wear 12504275863](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39957648783/artifacts/12504275863), SHA256 `9bee471ed43ae3f3ae7c08c91ea853a41404af376b646eaaeb7fb4ef57412c35`. CI-bizonylatok, nem saját adatokra telepítési ajánlás. A Wear/upgrade XML származtatott, a nyers runnerlogok az artifactokban.
- Érintett források: `NativeWorkoutEditing.kt`, natív store `projection()`, `PhoneWorkoutExecution.publish`, `NativeWorkoutSession.installPrepared`, `ActiveWorkoutViewModel` és lifecycle-bekötés; teszt `NativeWorkoutEditingTest.kt`. A régi `com.repforge` prototípus nem lett launcher vagy második kanonikus motor.
- Az akkori következő lépés (a fenti frissebb kapu részben teljesítette): stabil UI-set-ID callbackok (most még indexet adnak), ismételt gyakorlatkártya kulcsa és új sorozat UUID-je (törlés+új hozzáadás nem használhatja újra a régi szám-alapú ID-t); majd alsó stopper/GPS explicit measured completion, valós skip/± pihenővezérlés (a mostani skip üres timerkey-t ad, az adjust csak snapshotot módosít), gyakorlatváltás/undo, gyors/outdoor és importált draft. Native-editor jelző nélküli régi session/draft folytatása még külön egyeztetendő; ne dobjuk el, ne „javítsuk” törléssel. Main/kiadás változatlan, nincs verzióemelés vagy saját telefonos tesztfeladat.

## Natív edzésindítás és lezárás – legutóbbi fejlesztés (2026-10-23)

- **Termékkód `cab522a1b9fabf7a31fec294c57eab98644b5c3d`; legutóbbi CI-forrás `1a0cc941e515f9ad68c10c8c5e50a41330f67198` (azonos `android` fa: `add906cc76607965c9c0f0106f801d3d7dc6f0d7`)**, kizárólag `feat/native-android-migration`; main továbbra is `7473b30`. Nincs verzióemelés, merge, release vagy élő adatokra telepítési kérés.
- Commitok: `92172eb` tartós telefonos session és #228 előtöltés; `a0511e2` közös telefon/óra lezárás; `c063d32` tulajdonos-/restore-/törlési védelem; `cab522a` atomi snapshot-mentési kerítés. Dokumentációs HEAD később eltérhet az APK forrásától.
- A Home programnapi és a Programok indítógombja `NativeWorkoutSession.startWorkout` után navigál. Stabil workout/set UUID, natív execution előbb tartós, aztán aktív snapshot. Ismételt indítás a meglévő telefonos vagy órás edzést nyitja; publikálási megszakításból a meglévő natív frame áll helyre. Nem írja felül az importált `draft` dokumentumot: annak natív folytatása még külön feladat. **Home „quick” és „outdoor-gps” művelete még külön bekötendő**, nem létező programnapként nem indítható.
- `NativeWorkoutStartValues`: elfogadott main `tp228*` port. Naplópozíciónként legfrissebb érvényes súly/ismétlés, másik nap/program/gyorsedzés is forrás lehet; nincs készre jelölés/RIR/GPS/idő/korábbi set-ID öröklés. 32-es adatbázislapok, megfelelő edzésekre szűkítve; érvénytelen/törölt/jövőbeli/dupla gyakorlatazonosítós forrás elutasítva.
- **Egyetlen `TrainPilotWorkoutFinalization.finishWorkout`:** UI és Wear finish ugyanazt használja. Tartós pending draft + natív parancsok felfüggesztése, timer/GPS leállítás, natív események és kurzor együttes tartós mentése a nyugta előtt; Room napló+naptár tranzakció; valódi `closeExecution`, majd drafttakarítás. Nincs mentve nyugta lezárási/takarítási hiba mellett. Stabil azonosító és első finish idő megmarad újrapróbáláskor; régi órás lezárás nem törölheti az új telefonos sessiont. UI csak siker után navigál, mentés alatt visszalépés/ismételt indítás blokkolva.
- `ActiveWorkoutStore.saveOwned` atomi szinten elutasítja a lezárás alatti módosító snapshotokat, akkor is, ha azok lemásolták a pending jelzőt. Csak a külön belső `saveFinalizing` írhatja tovább ezt a draftot. Handoff/restore alatt nincs lezárás; régi, 2000 tombstone ablakon túli törlési alias sem éleszthető újra. Függő lezárásnál retry kell, nem discard. Telefonos mutációk az órás tükröt védik.
- [CI 39944821598](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39944821598): debug/release, csomag/verzió/signer ellenőrzés sikeres; letöltött XML **telefon 284/284, Wear 167/167**, 0 failure/error/skipped, benne **19 új teszt**. Eredeti és október 22-i gyorsított release → signed native `adb install -r` **2/2 sikeres**, nyers seed/verify státusz és telepített verzió ellenőrizve. Az első Wear UI próbán **182/183**, a 160dp/font1 korona-teszt 5s fókusz-timeoutot kapott; logcat szerint az emulator setupwizard/sysui/charging activity előtérbe jött. A változatlan forrású második próbán ugyanaz az 1 fókuszhiba volt. `3d92afc` / `39953204714`: termékbuild/unit/upgrade zöld, az emulátorscript védett komponens tiltásán SecurityExceptiont kapott, ezért nem futottak az UI-próbák. `1a0cc94` / [39953886817](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39953886817): a tiltó komponensparancsok eltávolítva; `dumpsys battery unplug` és ébren tartás modellezi a töltőről leválasztott órát. **39953886817 teljes SUCCESS**, új Wear UI **183/183**, 183 egyedi eset, 0 failure/error/skipped (68+59+56). Letöltött friss XML telefon **284/284**, Wear **167/167**; friss raw upgrade **2/2** ellenőrizve. A tesztek és a termék Wear fókuszfeltételei változatlanok; a töltőről leválasztott emulátor megoldotta a környezeti fókuszhibát.
- Legfrissebb bevizsgált APK-/CI-forrás **`1a0cc941e515f9ad68c10c8c5e50a41330f67198`** (termékkód Android-fája azonos a `cab522a` fájával). Eredeti aláírás, **1.3.71-native-test /2736**. [Phone artifact 12500999530](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39953886817/artifacts/12500999530), SHA256 `908e3f33df6115483d4549d6e52dd8ff6eddf5b132e383243bcb9f9310efc7da`; [Wear 12502025977](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39953886817/artifacts/12502025977), SHA256 `3a9e6ac9f14fe143b4dea0fad9252f8cd866cfd7800db9e07640f23ad8e0323b`. Csomag/verzió/cert/hash függetlenül letöltve ellenőrizve. **CI-bizonylat, nem élő telepítési ajánlás.**
- [Gépi bizonylat, 19 regressziós eset és kapuk](builds/2026-10-23-native-workout-start-finalization.json). A gépi bizonylat a friss `1a0cc94` kaput és a korábbi emulátorhibákat is rögzíti. A Wear/upgrade XML származtatott és így megjelölt; eredeti nyers runnerlog az artifactokban.
- Következő: folyamatos Compose editor–natív motor egyeztetés (edit/complete/undo még részben közvetlen snapshot-mutáció; controller foreground arm/pihenő nincs bekötve), gyors/outdoor indítás és draft folytatás, alsó sorozatstopper/GPS explicit stop automatikus teljesítése és natív undo, majd további képernyők/paritás. Naptár/Coach, teljes katalógus, nyelvek, nagy napló teljesítménye és tényleges natív UI/backup-visszaállítás még nincs lezárva. Saját telefon/Watch7 és valódi kéteszközös szinkron külön későbbi kapu.

## Korábbi teljes technikai kapu – 134adcd (2026-10-23)

- **Élő appra telepítési STOP:** a felhasználó adatvédelmi aggálya jogos. A `134adcd` APK-pár technikai CI-bizonylat, nem az adatokat tartalmazó napi alkalmazás frissítésére átadott build. A teljes telefonos funkcióegyezés hiányzik. Előbb elkülönített emulátoron/teszteszközön működés- és migrációvizsgálat, mentés-visszaállítás ellenőrzése, majd csak a releváns kapuk után készülékes frissítési próba. Azonos csomagnévvel a tesztbuild az élő app közös adatterét használja; a régi tár megőrzése és a szintetikus upgrade-siker nem teljes adatbiztonsági garancia. A régi APK visszatelepítése önmagában nem visszaállítás. Ne kérj eltávolítást, adattörlést vagy downgrade-et. Külön alkalmazásazonosítójú párhuzamos tesztkiadás még nem készült; ilyet ne állíts elérhetőnek, és a végleges azonosítót ne változtasd meg.
- **Kizárólag `feat/native-android-migration`; main merge/kiadás nem engedélyezett.** Munkafa: `/workspace/TrainPilot-native-migration`. Más/Codespaces munkafákat ne írj felül. Ellenőrizd a GitHub valódi HEAD-et; a dokumentációs commit nem új APK.
- **Bevizsgált APK-/alkalmazásforrás `134adcdb76480a9c26a22cc17df6ecc9a8561f0c`.** [CI 39937602813](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39937602813) **teljes SUCCESS**. Telefon/Wear debug és eredeti kulcsos R8 release, csomagnév/verzió/tanúsítvány ellenőrzése sikeres. Letöltött XML: telefon **265/265**, Wear **167/167**, 0 failure/error/skipped.
- **Kerek Wear UI 183/183**, 183 egyedi eset, 0 failure/error/skipped, API30 x86 emulátoron (68+59+56). Az eredeti fókuszvédelem helyes volt: a Wear setup wizard `EmulatorStatusActivity`-ja a teszt közben előtérbe ugrott. A tesztemulátorban letiltva, a forgatási teszt megvárja a tényleges fókuszt. Termék Wear fókuszműködés nincs megkerülve. Képek és logok az artifactokban.
- **Valódi aláírt frissítés 2/2 sikeres:** az eredeti `v1.3.71` és a **gyorsított `v1.3.71-perf-20261022`** tényleges release APK-ról `adb install -r`, eltávolítás/adattörlés nélkül. A közös szintetikus fixture Unicode/nagy napló, törlés, program/nap, testsúly, naptár, nyelv/haptika, draft/ismeretlen kulcs, változatlan eredeti localStorage/mentéshash, GPS, fotó, régi health DB és idempotencia ellenőrzése mind sikeres. **Nem minden valós felhasználói adatváltozat garanciája.**
- Mindkét APK: `com.repforge.app`, **`1.3.71-native-test /2736`**, eredeti signer `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`. [Telefon signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39937602813/artifacts/12494139412), [Wear signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39937602813/artifacts/12494159370). Telefon SHA256 `bdcaedac58a56aa20bd46cfc0870146d7d3f04cf97dc0ca45bae6bd63fa3bd82`; Wear `8ae386e71995ab92daba5a66dea0c8d4403d078b3c57e84375244cbfdbdd7e72`.
- [Bizonylat/artifactazonosítók és korábbi hibák](builds/2026-10-23-native-migration-checkpoint.json). A naplókból **származtatott**, így megjelölt [Wear XML](builds/native-134adcd/wear-round-derived.xml) és [upgrade XML](builds/native-134adcd/signed-upgrade-derived.xml) nem a futtató natív JUnit-riportja; az eredeti nyers eredmények is artifactban maradtak. Unit XML-ek közvetlen Gradle-riportok.
- **A teljes natív kiváltás nincs kész; az élő app cseréje még nem elfogadott.** [Konkrét funkcióhiányok](NATIVE_PARITY_AUDIT.md): telefonos edzésindítás callback csak navigál, lezárás hiányos és túl korán navigál; alsó stopper/GPS teljesítés, Naptár/Coach, nyelvek és nagy napló teljesítménye hátra van. A zöld technikai kapu ezeket nem zárja le. Fizikai telefon/Watch7 és kétirányú szinkron szintén külön kapu.
- **Következő implementáció:** a kanonikus telefonos edzésindítás, natív eseményegyeztetés és egyetlen biztonságos lezárás; a már elfogadott stopper/GPS készre jelölés megőrzésével. Utána Naptár/Coach és további paritás a 2026-10-22-i gyorsított mainhez. Csak megfelelő mentés és releváns sikeres próbák után élő készülékes teszt; ne kérj eltávolítást vagy adattörlést.
- Migráció: aktuális `https://localhost` WebView localStorage chunkolt olvasása, eredeti tár változatlan, helyi archívum `noBackupFilesDir/native-migration-v1371/legacy-localstorage.json`, Room-tranzakció, sérült forrásnál nincs kész nyugta. Destruktív Room downgrade nincs. A natív UI indulása a migráció sikeréig vár.
- A korábbi AndroidJUnit release-ellenőrzés a közös R8/Kotlin ABI miatt még a teszt előtt omlott össze. Helyette `:migration-seed` **CI-segéd** (nem termékapp) használ platform Instrumentationt. A natív APK-ban **csak `-PnativeUpgradeTests` esetén** szerepel a vele együtt optimalizált `src/nativeUpgrade/.../NativeMigrationProbe.kt`, egyetlen megtartott belépési ponttal. Minden korábbi adatellenőrző feltétel megmaradt. A régi hybrid AndroidJUnit források változatlanok; a natív upgrade lane nem használja őket. A fixture a segéd APK assetje, nem a telefonos termék UI-ja.
- Aktív **Groovy** Gradle: JDK21/JVM17, AGP8.13.0/Gradle8.13, Kotlin/Compose/serialization **2.2.21**, Room2.7 KAPT, compile/target SDK36, Wear Fragment1.8.6. Az inaktív `.gradle.kts` fájlok később rendezendők. [Toolchain](../NATIVE_TOOLCHAIN_POLICY.md). Gradle heap 3GB, két worker, Kotlin daemon1GB; a korábbi D8 1,5GB-os OOM megszűnt. Ez buildmemória, nem telefonos igény. Forgó cache megőrzi a tényleges build-cache frissítéseit.
- Minden Gradle/emulátor/APK a meglévő `native-android.yml` Actionsben. A natív ágon a használhatatlan Capacitor Quick/Phone/Wear auto kapuk kizárva; natív kapuk helyettesítik. Nincs main merge/force push/új termékág vagy a jóváhagyott native-test verzión túli emelés. Main ellenőrzött HEAD továbbra is `7473b30f387eda54186c1e31830304ca12bd104f`.
- Ha a CLI joblog üres/403, `github_fetch_workflow_job_logs` ad szöveget, az artifact connector letöltése működik. Ne írd ki a teljes aláírt URL-t/környezetet. A felületi távoli hibajelzés után a kapcsolat/környezet működött és minden commit/APK megmaradt. Figyeléshez a `gh run watch` mellől elhagyható az `--exit-status`: a workflow piros volta ettől még az API-ban ellenőrzendő.

## Aktuális kiadási alap és megőrzendő gyorsítások – 2026-10-22

- A gyorsított telefon/Wear kiadás tag-/APK-forrása **`5d27aa9c5ebf68d1c206c88a74dea89e660a4937`**, alkalmazáskódja az elfogadott `715dc5d` builddel egyezik. Változatlan `1.3.71 /2735`, `com.repforge.app`, eredeti aláírás. Main ellenőrzött HEAD: `7473b30f387eda54186c1e31830304ca12bd104f` (kiadási dokumentáció).
- Telefon SHA256 `850b5e1e597e574dbf5cf55cf82721a3f77879447db9852fa056d2b889d50570`; Wear SHA256 `c145afd729f4a9bf91d7cef48f2e9c82ddb265b73dc2b5bb8a15b12a9839e2ea`. A natív frissítési teszt most mindkét telefonos 1.3.71 APK-ról indul; a verziószám önmagában nem különbözteti meg őket.
- A mainhez képesti áttekintésből megőrzendő: egyetlen stabil azonosítós, újrapróbálható lezárási folyamat; nincs mentve nyugta lezárási/takarítási hiba esetén; naptári teljesítés és stopperleállítás; eredeti alsó stopper és explicit pozitív stop utáni automatikus teljesítés; mindkét oldal szükséges oldalsó planknél. A natív importban ezen viselkedések egyezése **még nincs igazolva**.
- Teljesítmény: a gyorsított WebView már csak 32 naplókártyát rajzol először, startup olvasásokat közösít, elrendezésméréseket csökkent, R8-at használ, a Health/Wear munkát az inputszálról leveszi. A Compose megfelelőit külön kell vizsgálni: Lazy-lista/adatbázislekérdezés mérete, háttér-IO, Room/Flow frissítés, képernyőmegjelenés és navkijelölés egyezése. A natív kódban az onResume Health munka már IO-n fut; a felesleges health-preferenciavisszaírást `eb1d21f` kerüli. WebView startup baseline-profilt nem kell vakon átmásolni Compose-ra.
- A main ág működő UI-ját nem keverjük automatikus merge-gel a teljesen natív ág eltérő Gradle/Activity struktúrájába. A forrásellenőrzés nem teljes funkcionális elfogadás. [Gyorsított kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.71-perf-20261022).

## #214 – GitHub-bemutató frissítve (2026-10-21)

- Kifejezett felhasználói kérésre a GitHub About rövid angol leírása és 11 Topics címkéje ténylegesen elmentve, böngészőben visszaellenőrizve.
- Angol/magyar README: csoportosított aktuális funkciók, 8 telefonos + 2 natív Wear-kép. Új [magyar funkcióáttekintés](FEATURES.hu.md); [képeredet](screenshots/README.md) és SHA-256 jegyzék.
- Képek változtatás nélkül a kiadott `60fe647a` forrás main CI-jéből: Phone **39519510921**, Wear round **39519510953**. Szintetikus fixture-k, nem személyes adatok és nem Play-készülékfotók.
- Kizárólag dokumentáció/képek és repository metaadatok változtak. App-/natív kód, 1.3.71 /2735 verzió és kiadott APK-k változatlanok. Új APK-/emulátorkaput nem indítunk ugyanarra a forrásra.
- #214 nyitva marad a korábban megőrzött Play/partneri/licencelési feladatokkal.

## Lezárva – #234 / 1.3.71 kiadva (2026-10-21)

- #234 merged mainre: `60fe647a98ccdcc7f102d647a6a1cc8db40bbbfa`; elfogadott PR-head `be76bf3753e821cab0d97390fa84fcb5c53b1108`. HEAD, PR teszt-merge és main fája azonos: `4943c34a3aa135e78ed823d54b4206bde25c8c22`.
- Végleges verzió **1.3.71 /2735** mindkét platformon. A lentebbi 1.3.7 teszt-APK a készülékes elfogadás előzménye.
- Készülékes visszajelzés a `cbff3509` APK-ról: a rejtett rekordrészletező megnyílik, de az Új PR jelvénnyel nem zárható.
- A click handler most a korábbi nyitott állapotot váltja; második koppintás teljesen elrejti a panelt és nyitva hagyja az edzéskártyát. A részletek címével is összecsukható.
- A jelvény `aria-expanded` állapota és nyila követi a panelt; bezáráskor a fókusz visszatér a látható jelvényre.
- A meglévő Chromium PR-teszt bővítve nyitás–zárás–újranyitásra, külön cím általi zárásra és HU/EN/DE/RO kis kijelzős, nagy betűs változatokra.
- További készülékes kérés: az Edzésstílus/progresszió zárt panel legyen laposabb; a Mégse láthatóan dobja el a szerkesztést; a sorozat-haptika pipája egyezzen az appéval.
- A belső módpanel 46px-es kompakt fejléccel, dupla függőleges padding nélkül jelenik meg. A Mégse eldobja a függőben lévő választást, visszaállítja a mentett mezőket és becsukja a belső panelt, a külső Edzés beállításai nyitva marad.
- A haptika ugyanazt a `tp155-sync-check` témázott 22px-es négyzet/pipa vezérlőt használja, mint a többi szinkronbeállítás. A mentett kapcsoló működése változatlan.
- A módválasztó/mentés tesztek és a haptika integrált UI-tesztje kibővítve; a Mégse modellteszt helyben sikeres. A PR-részletező célzott tesztje bekerült a kötelező Chromium suite-ba.
- Végső teszt/APK-forrás: `924664ef7882aa59cbedfbcebe6c95605511d9b1`. Quick **159/159**, Performance **SUCCESS**, telefonos Release Gate **39515146159 SUCCESS**, Chromium **6/6, 107/107**, natív XML **251/251, 0 failure/error/skipped**.
- Aláírt, kicsomagolt telefonos APK átadva; verzió **1.3.7 /2734**. SHA-256 `2d2ba088f61cf1c34a9da0252a66d0e8caee41449bf817860aacfc2aaedaaa3a`. A csomagolt app.js/CSS blobazonos a forrással, a source stamp a fenti commit. Aláírás és signer-folytonosság kapu sikeres.
- [Korábbi készülékes teszt-APK bizonylata](builds/2026-10-21-234-pr-toggle-training-ui.json). A felhasználó az alábbi üzenettel elfogadta és lezárta ezt a kört.
- Felhasználói elfogadás: „Zárható, mehet mainre 1.3.71, ez lesz a legfrissebb verzió, release gate stb. leírásban is frissítsük.”
- Jóváhagyott kiadásjelölt: **1.3.71 /2735** telefon és Wear; metaadatok, cache, verziótesztek és kiadási jegyzetek frissítve. Helyi verzió/kiadási tesztek **11/11**, app.js szintaxis sikeres.
- Az 1.3.71 merge előtti kapui: Quick **39518365505: 159/159**, Performance **39518365457 SUCCESS**, metadata **39518365472 SUCCESS**; telefon **39518361442**, Chromium **6/6, 107/107**, natív XML **251/251**; Wear **39518361372**, XML **167/167**; kerek UI **39518361440**, XML **183/183** három shardban (68+59+56). Minden XML: **0 failure/error/skipped**. Aláírás/signer-folytonosság sikeres.
- Main Quick **39519510907**, Performance **39519510934**, telefon **39519510921**, Wear **39519510922**: SUCCESS. Telefon XML **251/251**, Wear XML **167/167**, 0 failure/error/skipped.
- Publikus APK-pár, forrás mindkettőnél a fenti main merge, aláírás/signer-folytonosság sikeres, letöltött artifact és release digest egyezik:
  - [Telefon 1.3.71](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.71/TrainPilot-1.3.71.apk), SHA-256 `f3fc054d9e2142fc7919b0e7beee8c2db35527de2fcb85406c23ab23141e5e68`.
  - [Wear 1.3.71](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.71/TrainPilot-1.3.71-wear.apk), SHA-256 `ea963e7393bb29748fdd3325d3e83e6a715261e5a93bdd8ed38ab5876773e97a`.
- `v1.3.71` explicit Latest, `wear-v1.3.71` latest=false; mindkét tag a main merge-re mutat. Telefon app.js/CSS blobazonos; mindkét APK forrásjelölése ellenőrizve. [Kiadási bizonylat](builds/2026-10-21-release-1371.json).
- Main kerek UI **39519510953 SUCCESS**, mindhárom shard és teljes **183/183** lefedettség sikeres. A mentett snapshot hiánya miatti cold boot figyelmeztetés nem teszthiba.
- Következő: nincs további #234 fejlesztés vagy készülékes jóváhagyásra váró pont. #225 lezárva (történeti audit, #230/#234 által felváltva); #231 külön marad; Play-előkészítés #214 változatlan.

## Elfogadva – #230 / 1.3.7 közzétéve

- **Felhasználói jóváhagyás (2026-10-20):** „Rendben, zárható :) mehet mainre 1.3.7 verzioszamon, a release gatenel, teszteknel ezt vegyü figyelembe.”
- #226 mentett PR-jelvény, #227 kézi sorozat haptika, #228 Napló-alapú gyakorlatonkénti súly- és ismétlés-előtöltés és #229 tényleges sorozat-RIR: felhasználó által elfogadott #230 csomag.
- #228 korábbi készülékes hibajelzés: a tervezett edzés rendben volt, a gyors edzésnél a célismétlés felülírta a Napló alapján előtöltött tényleges ismétléseket. Javítva mindkét gyorsedzés-indítási úton; a hat Chromium shard integrált tesztet futtat.
- **1.3.7 /2734** összehangolt telefon/Wear verzió: `package.json`, `package-lock.json`, két SOURCE_VERSION, két Gradle, `www/app.js`. A verziópolitikai tesztek az 1.3.7 címkéket és a 2734 kódot várják.
- **Merge előtti kötelező kapuk:** forrásazonos Quick/Performance, 6/6 Chromium, telefonos és Wear aláírt release gate, óra kerek kijelző teszt. Az előző `49784cb` 1.3.6 /2733 sikeres kapui a funkciót igazolták, de nem bizonyítják az 1.3.7-es buildet.
- **Main merge és kiadás kizárólag sikeres 1.3.7 kapuk után.** A mainre kerülő azonosított forrásból az önálló telefon/Wear workflow-k külön `v1.3.7` / `wear-v1.3.7` kiadást állítanak elő. Verifikáció és #226–#229 lezárás a tényleges merge/kiadás után.

## Aktuális – #223 mainen, #214 külső előkészítése nyitott

- Felhasználói merge-jóváhagyás 2026-10-20 18:21 (Europe/Budapest): „Minden mehet mainre”.
- **#223 merged mainre: `c9a395e56259849a2182c8b7c8b4e0d9c5d1ab7c`**; merge fa `6b978991c22ae20380bb6cab322678082c171686`, byteazonos az elfogadott `7ea45f3305366f5253fefef7df9f42ec77dfc1a0` PR-head fájával.
- Ág: `maintenance/214-repository-hygiene`, alap main `a3a4ddce2250c32ef5b55e9ed8733b6a3aa85bda`.
- Angol + magyar README, 37 release dátum/platform szerinti jegyzéke, friss családos változáslista és Play-checklist, proprietary csomagjelölés.
- Új verzióegyezési ellenőrzés és célzott tesztek; csak leírásfejlécet módosító release-karbantartás, védett eredeti szöveg/tag/melléklet-metaadatokkal.
- Helyi célzott teszt: 8/8; verzió 1.3.6 /2733, eredeti kiadási feljegyzések és alkalmazás-/natív forrás változatlan.
- [#223 – beolvasztott PR](https://github.com/fuloplevente1998/TrainPilot/pull/223), ellenőrzött kód/metaadat HEAD: `3275b97cc2a6d1342ed866e523d24c807f73da60`; fa `c72d127e14c3d15ddfc0b9d5654da6f079e05f13`. A PR-merge tesztfája byteazonos.
- Quick [39454775410](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454775410): **154/154 + 8/8 karbantartási teszt**, Performance [39454775235](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454775235): **SUCCESS**, metadata PR-validálás [39454774998](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454774998): **SUCCESS**.
- A tényleges release-leírás rendezés [39454564328](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454564328) sikeres a `58bd1b1` forráson. **37/37 eredeti szöveg hash-azonos; 37 tag és 154 melléklet metaadata/digestje változatlan**, független GitHub/ls-remote utóellenőrzéssel. [Riport](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454564328/artifacts/12281085093).
- Végső PR-head (`7ea45f3`) Quick **39455207276**, Performance **39455207248**, metadata-validálás **39455207238**: mind SUCCESS. A tesztelt PR-merge fa egyezik a HEAD-del és a tényleges main merge fájával.
- Automatikus main kapuk a merge `c9a395e` forrásán: Quick **39456946831**, Performance **39456946849**, Phone Gate **39456946879**, Wear Gate **39456946991**, Wear round UI **39456946769**; az átadás rögzítésekor még folyamatban. Ez a kizárólag átadást rögzítő dokumentációs commit nem indít duplikált kapukat; a karbantartási és alkalmazáskód változatlan.
- Verzió **1.3.6 /2733**; a merge nem módosított taget vagy közzétett APK-mellékletet és nem publikált Playre. Következő: a #214 megőrzött külső Play/Samsung feladatai; külön #224 audit Draft PR is nyitott, nem része a #223 csomagnak.
- #214 nyitva marad a [rögzített külső Play/Samsung/aláírás/licenc feladatokkal](MAINTENANCE_214.md).
- A felhasználó #184/#197 main engedélyét 2026-10-20-án újra megerősítette; GitHub szerint mindkettő már completed, #222 merged `f413a3a`.

## Elfogadott alkalmazás – #222 mainen, funkcionális kör lezárva

- Felhasználói elfogadás/lezárás 2026-10-20 17:12 (Europe/Budapest):
  „Zarhato ez is, igy akkor kb minden mar nem ?”
  Ez a legutolsó Samsung/Regeneráció-javítás elfogadása és a korábban feltételesen
  jóváhagyott RIR utáni #222 integráció lezárási pontja; nem kitalált részletes mérési napló.
- **#222 merged mainre: `f413a3a475cf31a4e596429fe1c8dcf3e1e135df`. #184 és #197 completed.**
  Main merge fa `791db4a8608a27bdcbfc31490a8cbf427b3fd307`, byteazonos a PR-head fájával.
  A korábbi dokumentációs eltérésen túl a bevizsgált alkalmazásforrás változatlan:
  `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`.
- Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A végső dokumentációs HEAD Quick
  **39441673066** és Performance **39441673033** is sikeres.
  A merge után automatikusan induló main futások külön futások; eredményüket nem találjuk ki.
- Verzió **1.3.6 /2733**, Wear változatlan. Nem történt verzióemelés vagy új release.
  A már telepített, elfogadott [telefonos APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379)
  alkalmazásforrása azonos a mainre került kóddal.
  [CI/APK bizonylat](builds/2026-10-20-222-recovery-tiles.json);
  [lezárási bizonylat](builds/2026-10-20-222-accepted.json).
- **Egyetlen nyitott issue: #214**, karbantartás és Google Play-előkészítés.
  A Samsung nyilvános/production partner-hozzáférés ellenőrzése is ott marad;
  saját készülékes siker nem partnerregisztrációs igazolás. Nyitott PR nincs.
- A jelenlegi funkcionális fejlesztési sor lezárult. A dizájnmódosítások lehetnek
  a következő külön feladat; még nem kezdődtek el. Rögzített ötletek:
  [UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md), külön későbbi tesztágra.

## Történeti #222 – Samsung-pontszám olvasható, Regeneráció-csempe egységesítése

- Felhasználó 16:24-es képe **sikeres készülékes Samsung-olvasást igazol** az e97fe1e APK-n.
  Ez nem teljes PR/main elfogadás és nem külön egyezésigazolás a Samsung Health kijelzésével.
- Kérés: a Regeneráció csempe is ugyanazt a mai külső pontszámot mutassa.
  A csempe eddig friss saját alvás/HRV rekordhoz kötötten, Coach-korláttal számolt.
  Most a hiteles mai Samsung-pontszámot mutatja Home/Egészség oldalon akkor is,
  ha külön alvás/HRV-rekord nincs. Külső adat hiányában a korábbi saját becslés marad.
- Közös egész számú kijelzés: a képen látott SDK-tizedes érték 55/100-ként jelenik meg.
  A nyers SDK-pontszám és a Coach fájdalom/terhelés védelmei változatlanok.
  Kártya/csempe/Coach forrássor azonos formázást használ. Natív és Wear kód érintetlen.
- Az új teszt elkapta a nyitott panel mögött elmaradó csempefrissítést; javítva csak
  a read-only pontszám közvetlen DOM-frissítésével, panel/draft/scroll megőrzése mellett.
- **Végső alkalmazásforrás:** `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`;
  PR-merge fája is `8332fd6ffa21d0281af7bc5f702f928ecdcef6e7`.
  Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**. Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A Samsung-teszt egész számú kártya/Health/Home
  csempe-egyezést igazol frissítéskor, nyitott panel és külön alvásrekord hiánya mellett.
- **Telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379),
  1.3.6 /2733, SHA-256 `3798a8e679f9a5a16b013be774f918e62eca24e0070b36084a7d2e1349cf5e28`.
  app.js/index.html byteazonos, forrásjelölés/SHA256SUMS/aláírás és signer-folytonosság helyes.
  [Bizonylat](builds/2026-10-20-222-recovery-tiles.json). Wear APK változatlan.
- **Következő:** új APK-n csempe/kártya egyezésének készülékes visszajelzése.
  #222 Draft, main/verzió változatlan, nincs merge/kiadás/issue-lezárás.

## Korábbi #222 – 1001-es napi lekérdezés javítása

- Felhasználó 16:07-kor az új APK képernyőképét küldte: **SAMSUNG_HEALTH_1001**.
  Samsung dokumentáció: ERR_INVALID_INPUT (érvénytelen bemenet); nem igazolt partnerhiba.
- Konkrét kódhiba: a napi startDate és endDate azonos dátum volt, mindkettő inclusive.
  A valódi Samsung SDK ezt azonos éjféli kezdő/végű LocalTimeFilterré alakítja.
  Javítás: pozitív [mai éjfél, következő éjfél) helyi dátumintervallum; a holnapi pont
  továbbra is kizárt. Nincs másnapi vagy saját becslés Samsung-pontként felcímkézve.
- Natív teszt a tényleges buildelt SDK-kérés LocalTimeFilterét vizsgálja; 1001-et okozó
  nulla hosszúságot minden energiaolvasó teszt elutasít. Új regresszió 23/24/25 órás
  budapesti napokra és a holnapi éjféli pont kizárására. Korábban a mock ezt nem nézte.
- **Alkalmazásforrás:** `e97fe1e31a824640ea78c1cf39b45c1c572456db`;
  Quick/Performance PR-merge fa is `d5f7d46679560ee44fcf7615d66c393b9ad25a0c`.
  Quick **39436835835: 154/154**, Performance **39436835798: SUCCESS**,
  Phone Gate **39436826354: SUCCESS**. Chromium **6/6, 105/105**, letöltött JSON-ok
  forrása/duplikációmentessége ellenőrizve. Natív XML **249/249, 0 failure/error/skipped**;
  valódi SDK-szűrő/DST regresszió és a régi nulla hosszúságot igazoló teszt sikeres.
- **Új telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39436826354/artifacts/12271587790),
  1.3.6 /2733, SHA-256 `52eecad1878da78d20d773cb35193ed27482c6464cb14215de75e8c499b98fc8`.
  app.js/index.html és a forrásarchívum natív kódja byteazonos, forrásjelölés és SHA256SUMS
  helyes; apksigner és eredeti signer-folytonosság sikeres.
  [Bizonylat](builds/2026-10-20-222-samsung1001.json).
- Forrásváltozás csak telefonos Samsung-olvasó és natív teszt; felület/Wear/verzió azonos.
  **Következő:** új APK telepítése, Regeneráció → Samsung-pontszám frissítése;
  a mai Samsung-érték egyezésének készülékes visszajelzése. Készülékes siker még nem
  igazolt; #222 Draft, main `1393982d`, nincs merge/verzióemelés/issue-lezárás.

## Korábbi #222 – Regeneráció alá rendezés

- Felhasználó az új APK-ról képet küldött: Samsung Healthen telefonon/órán van mai
  pontszám, TrainPilot az általános „olvasás nem sikerült” állapotot mutatja.
  **Ez sikertelen készülékes teszt**, a korábbi zöld CI nem igazolja a Samsung-hozzáférést.
- Kérés szerint a Samsung-kártya a Regeneráció megnyitható részébe került;
  külön felső szintű kártya nincs. Frissítés után a panel nyitva marad.
- Az önálló olvasás állapotellenőrzése is kizárólag ENERGY_SCORE engedélyre kérdez;
  más adattípus nem akadályozhatja. Natív SDK-hibakód megőrizve aszinkron és belső
  platform-kivételen át; részleges olvasásnál is strukturált mezőhiba jut a felületre.
- A felület külön jelzi a 2000 engedélyhiányt, 2003 app-hozzáférési korlátot,
  3001 frissítési igényt és 3000/3002/3003 Samsung-beállítási akadályt. Nyers
  kivétel/adat nem kerül ki; diagnózishoz hibakód látszik. Hiánynál marad a TP-becslés.
- Samsung hivatalos API-feltétel: csomagnév + release aláírás + engedélyezett
  adattípusok partnerregisztrációja kell normál felhasználói hozzáféréshez.
  https://developer.samsung.com/health/data/process.html
  https://developer.samsung.com/health/data/api-reference/-shd/com.samsung.android.sdk.health.data.error/-error-code/index.html
  A konkrét készülékhibát kód nélkül **nem** minősítjük biztos partnerhibának.
- **Bevizsgált alkalmazásforrás:** `ae82d481b31d8d01394ce41d08ef003c8847d0c1`.
  PR-merge fája is `4056ab3898c1386ea61969c330c06287b612380f`, azonos alkalmazásforrás.
  Quick **39423620810: 154/154**, Performance **39423620964: SUCCESS**.
  Phone Gate **39423616111: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **248/248, 0 failure/error/skipped**. Új engedélyhatár- és kivételláncteszt sikeres;
  Samsung Chromium: Regeneráción belüli kártya, nyitva maradó panel és hibakód rendben.
- **Új telefonos teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39423616111/artifacts/12264688934),
  1.3.6 /2733, SHA-256 `49426605ba22046bdba3051004528e70814c0b62de9714c372c488ef67ff752d`.
  APK app.js/index.html byteazonos, forrásjelölés és SHA256SUMS helyes, signer
  az eredeti v1.0.0-val egyezik. Wear változatlan. [Bizonylat](builds/2026-10-20-222-recovery.json).
- **Következő készülékes próba:** Egészség → Regeneráció → Samsung energiapontszám,
  egyszeri frissítés/engedélyezés; ha hiba marad, a konkrét hibakód szükséges.
  Ez a build az adatolvasás és hibaazonosítás javítása, nem igazolt készülékes megoldás.
  #222 Draft, main/verzió változatlan, issue-k nyitva.

## Korábbi #222 – teljes CI sikeres, teszt-APK kész

- Integráció: `feat/184-197-coach-integration`, **#222 nyitott / Draft**.
  **Bevizsgált alkalmazásforrás:** `a8bec13630136d89128f259bd065f6c2f57b2cea`.
  Quick/Performance PR-merge fája byteazonos: `889b18da87d94f3dc38cbb37b9629ecab2927e71`.
  A későbbi dokumentációs commit nem új alkalmazásbuild.
- **Quick 39391181566: 154/154; Performance 39391181636: SUCCESS.**
  **Phone Release Gate 39391176400: SUCCESS; Chromium 6/6, 105/105**,
  mind a 6 letöltött shard-JSON pontos forrásra és duplikáció nélkül ellenőrizve.
  Natív XML: **246/246, 0 failure/error/skipped**, benne a Samsung SDK olvasó 8 esete.
- **Telefon teszt-APK: 1.3.6 /2733**, [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39391176400/artifacts/12249603397).
  SHA-256: `0352cbfc68b6f46dc402f82a267878bc1f0aaec2035e3514e782606c2bf7fb80`.
  A letöltött APK app.js/index.html byteazonos a bevizsgált forrással; source stamp helyes,
  apksigner és eredeti v1.0.0 signer-folytonosság rendben. Wear forrás/APK változatlan.
  [Ellenőrzési bizonylat](builds/2026-10-20-222-rir-samsung.json).
- **#197:** opcionális valódi 0–10 RIR, közvetlen számmezőn; azonnali draftmentés
  újrarajzolás nélkül; hibás szerkesztés nem hagy korábbi mérést. Reload, edzéslezárás,
  a tényleges lazy Napló és a következő konzervatív Coach-ajánlás E2E sikeres.
  A cél-RIR nem mérés, hiányzó cél nem implicit 0; nincs automatikus súlyátírás.
- **#184 felhasználói hiba:** Samsung Healthben van pontszám, TrainPilotban nem látszik.
  Javítás: külön napi ENERGY_SCORE olvasás, külön engedély, a lépés-/kalóriaforrástól
  független automatikus frissítés; mai hiteles Samsung-pont elsődleges, hiány/hiba/
  engedélymegvonás esetén saját becslés. Nincs kettős pontozás, fájdalomkorlát megmarad.
  Az Egészség oldalon adat nélkül is látszik a pontszámkártya és az ok/frissítés.
  Az önálló olvasás nem írja felül a teljes Samsung-napi rekordot. Éjfél kizáró végpont javítva.
- **Felhasználói felhatalmazás:** „ha a RIR javítás bekerül, mehet mainre” feltételes
  jóváhagyás, majd kifejezett #222 feltöltési engedély. Ez nem sikeres készülékes
  Samsung-tesztbeszámoló. A közben jelentett pontszámhiba új APK-n még visszaellenőrzendő.
- **Következő:** APK telepítés; Egészség → Samsung energiapontszám, szükség esetén
  egyszeri engedélyezés. Egyezzen a Samsung Health mai értékével; hiánynál fallback.
  Ellenőrizendő a pihenőidők megőrzése APK-frissítés/Drive-visszaállítás után és releváns
  telefon/Wear használat. A készülékes eredményt ne találd ki.
  Main továbbra is `1393982d`, nincs merge, verzióemelés, release vagy Issue-lezárás.
  Dizájnmódosítások a funkcionális lezárás után, külön későbbi feladatként jönnek.

## #216 lezárva, 1.3.6 mainen

Felhasználói engedély: **„Rendben, akkor mehet, zarhatjuk ezt a teljes részt es akkor 1.3.6.lesz”**.
Ez lezárási/main/kiadási jóváhagyás; részletes fizikai tesztbeszámolót nem találunk ki.

- **#218 merged `2059770c0eadf9c9e66929635865861667404d41`; #216 completed.**
  Órás menü: aktív edzés/Gyors edzés elöl; Naptár/Programnapok közös csoportban.
  Új kültéri gyors/tervezett edzés előtt látható, aktivitásonként megjegyzett GPS-választás,
  futó edzésről közvetlen állapot/kezelés. Reconnect/render nem indít rögzítést.
  Kézi gyakorlatváltás megőrzi a pihenő eredeti határidejét és automata fázis/cél/tulajdonosát.
  Telefonos működés és GPS-adatút megmaradt. HU/EN/DE/RO.
- **#219 merged `98be66221430202715aafa9a71583bbbd22d4fd3`; 1.3.6 /2733.**
  Mindkét app metaadatai és appfelirat, SW cache, verzióelvárások és kiadási jegyzetek frissítve.
  Main teljes fája byteazonos a bevizsgált `76fa539` PR-head fájával:
  `d0f5487aa3338369c879126b7d2ca15cbbd4e3b3`.
- #216 alkalmazáskód `c04851f`; utána csak tesztjavítások. A home fixture törli az aktív edzést,
  ezért a felfelé gesztus után **Gyors edzés** az első látható menügomb, nem Napló/Folytatás.
  Az eredeti azonnali láthatósági, gesztus- és geometriai feltételek megmaradtak.

## Ellenőrzött kiadási forrás

- Quick **39231417696**, Performance **39231417699**: sikeres (`76fa539`).
- Telefon **39231412495**: Chromium **6/6**, JVM **243/243** (`76fa539`).
- Wear **39231317332**: JVM **159/159** (`7b600e4`).
- Ownership **39231417723**: **81/81**; kerek UI/platform **39231317394: 183/183**.
- Minden vizsgált XML: **0 failure/error/skipped**. Külön #216 kerek UI
  **39231315228: 183/183**; új GPS/pihenő/menü tesztek mind az öt kijelző/betűméreten.
- `7b600e4..76fa539` csak AGENTS/átadás/bizonylat; alkalmazás- és tesztkód byteazonos.
- Aláírás folytonos a nyilvános v1.0.0-val; signer SHA-256:
  `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- [Bizonylat, APK-hashek és források](builds/2026-10-19-release-136.json).

## Végleges, publikált 1.3.6 APK-pár

Mindkét kiadás/tag forrása **`98be66221430202715aafa9a71583bbbd22d4fd3`**, 1.3.6 /2733:
- [Telefon APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.6/TrainPilot-1.3.6.apk),
  SHA-256 `4b3b2fd90b38912a7a51157ed18c337f361f5e3f5521df69d10a64697c4cc987`.
- [Wear APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.6/TrainPilot-1.3.6-wear.apk),
  SHA-256 `ac9d16066df7f63290bca20656bf883391e41bbd1399cde397b9292176233958`.

Main telefon **39233098299**: 6/6 UI, JVM XML 243/243; Wear **39233098238**: JVM XML 159/159;
0 failure/error/skipped. Mindkét apksigner és eredeti signer-folytonosság sikeres.
A letöltött main artifact APK-hashek egyeznek a nyilvános release asset digestjeivel,
a telefonos forrásarchívum és Wear SOURCE_VERSION egyaránt `98be6622`-t igazol.
Main Quick **39233098560**, Performance **39233098199** sikeres.
Kerek UI **39233098292** további automatikus main futás; az azonos teljes forrásfa
183/183 kapuja a fenti **39231317394** futáson XML-ben igazolt. Élő státuszát GitHubon ellenőrizd.
A későbbi dokumentációs HEAD nem új alkalmazásbuild; a két APK tényleges forrása a fenti commit.

## Külön maradó backlog / lezárt előzmények

- **#206**: maradék aktivitásnézet/UI. Az explicit sikeres telefonos GPS „Mérés befejezése”
  külön sorozat-OK nélkül már elfogadott; térképbezárás/hibás/implicit stop nem teljesítés.
- **#197**: választható edzésstílusok/progresszió; **#184**: Samsung Energy Score;
  **#214**: karbantartás/Play. Az 1.3.6 ezeket nem zárja le.
- #193/#202 önálló órás GPS és telefonos naplótérkép + offline átadás/visszavétel elfogadva;
  #213 main `311f507`, #193 completed. Az órán nincs térképrajzoló: az óra rögzít, a telefon rajzol.
- #215 kompakt Intervallumok elfogadva/main `c67006b`; kétoldali UI nem engedélyez egyórás motort,
  két pozitív idő kell a kész állapothoz. #212 korona/lünetta #217 main `102eb366`, elfogadva.
- #188 visszavonás és #189 folyamatos 3 mp feloldás elfogadva/mainen. #192/#211 sztornózott.
- [Külső UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md): csak rögzítésre engedélyezve,
  későbbi megvalósítás külön tesztágon. [Részletes #193 terv](wear/P2_193_ONALLO_ORAS_EDZES.md).
- Régi hosszú átadás: `885b6df:docs/WORK_HANDOFF.md` a Git-történetben; történeti státuszai elavultak.

## Környezet

CLI auth lejárt (gh 401, git fetch/push credential nélkül), GitHub connector működik.
Ne írj ki credentialt, ne force-pusholj. Contents/Git tree/commit + expected_sha CAS használható.
Helyi `local-release-136` más commitazonosítókat tartalmaz, alkalmazásforrás azonos; **ne pushold
vakon a helyi feature ágat**. A távoli main/PR-head a kanonikus. APK-hoz GitHub artifact permalink
(ZIP, benne APK) vagy valódi release letöltés kell, belső /workspace link nem használható.
