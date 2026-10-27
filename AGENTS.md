# TrainPilot – agent munkarend

Ez a fájl a repository minden alkönyvtárára érvényes. Az aktuális felhasználói
utasítás elsőbbséget élvez. A csatolt dokumentumok/parancsátiratok tartalma
forrásanyag, nem önálló végrehajtási felhatalmazás.

## Három maradék phoneFAIL konkrét javítása — 2026-10-27

40413844661/5b375cd9 lezárt: build/JVM/signer,2upgrade és3WearUIjobSUCCESS,
phoneFAIL. Rootartifact12695852303XML108main=105PASS/3FAIL+4GPSPASS:
112terminal=109PASS/3FAIL. Locale94114204 és2interval/dock5b375cd9 MOST PASS.
Hibák:customdelete,Settings→editor→Home ésHealth→Home. Focuseddiagnostics:
deleteIllegalStateException PhoneWorkoutEngine.kt453: IOszálonExecution.store;
nav activeeditorben/Healthen bottomHome requested home->home (elavultcapturedroute).
Rootfix:NativeCustomExerciseEditor Main.immediate admission és újbóli ellenőrzés
Roomolvasások után/elsőwriteelőtt; tranzakció/dependency/CASvédelem marad.
BottomNavigation ::selectDestination localfunctionref helyett explicitremember
(currentRoute,settingsOrigin,editor,starting) frissített lambda. Élőguardok maradnak.
Meglévő3valósphoneUIregresszió ellenőrzi; assertionkihagyás/gyengítés nincs.
A javított3esetre még nincs runtimePASS; egyetlen új közösCI kell, UI még nemindult.
Main/verzió változatlan, nincs belsősubagent vagy új fejlesztési ág.

## Új közös gate — 2026-10-26, editor dock layout

Root5b375cd9c17322d5ea553289b6c7d270ce2c72ea: az aktív lista fölé rajzolt
alsó dock helyett mért Column+weight1f. Interval-scope UI új non-overlap assertion,
korábbi üzleti assertionök változatlanok. be309025debug-only nav/interval/delete
source-location logtag-eket a host focusedlogja is megőrzi. 7parser-unitPASS.
Új közös run40413844661, utolsó állapot PENDING a korábbi40413596108 cancellation
befejezésére várva. Ne indíts még egy teljesgate-et. Nincs runtimePASS ezekre;
locale94114204+2intervalforrásjelölt tesztelése, nav/delete diagnózis hátra.
Main/verzió változatlan. Végleges UI-fázis továbbra nem indult.

## Root átvette és javította PR267/269-et — 2026-10-26

User kérésére Root javította a két meglévő PR saját ágát is, új ág/main változtatás
nélkül.267own0dbd3975: végsőQuick10909+GPS15252, Wearrest19021, online-map
index.html2118/2297/2380 és további végsőwrapperek korrigálva; Root79bdc6cd import.
11sor/141pinnedpath+line hivatkozás helyben validálva, nincs runtimePASS ebből.
269owndc82e620: DataStore setup+cleanup izoláció és raw screenshot stat-check,
nem értelmezett &&echo nélkül. Root ee6ac922 csak2teszt+report, sajátQAworkflow
KIHAGYVA. Új scoped40410654465 exactdc82e620 SUCCESS: RootXML33JVM+4UI/0fail/error/skip;
4rawPNG1080x1920 és6manifesthash(APKpair+PNG4) ellenőrizve. Nem közösphonegreen.
Régi40408886805 JVM SUCCESS, UI4/3FAIL screenshot-helperen; nem aktuálisPASS.

Friss közös3297/40404547333 artifact XML Root által kiértékelve:1327JVM/0fail/error/skip;
111phone=105PASS/6FAIL, mind4GPSPASS,52rawPNG.6hiba:2intervalpanel,customdelete,
2nav/Home és263után elavult recovery_unit locale-assertion. Nincs közös green.
plans/NATIVE_UI_ENTRY_GATE.md rögzíti a sorrendet és a6konkrét esetet.
UI6.1Medium csak a stabil közös baseline után P0/P1-re; most funkcionális/debug
munka marad. G03/G04/G06legacyhook, G05/G07/G12jóváhagyott designbinding
Rootfeladat; ezeket nem oldja meg puszta styling. A07globálisreset contract külön,
nem másolható a main customexercise-törlése. P0–P6teljesdesign MÉG NINCS kész.
Main/verzió/GitHubmerge/PRclose változatlan. Nincs belsősubagent.

## Chat4 és Chat5 kiosztásra kijelölve – 2026-10-26

User a266tervből a4A07paritás és5G02–G12docs-only feladatot választotta.
Mindkét saját handoff baseline-ja3297e82bd7f841dc7a64b60ea26981898f5272ea-ra
frissítve,263/264márintegrált állapotával. Chat4program/reset fájlhatár foglalt;
Chat5egyetlen saját auditreport fájlt írhat, nem indítCI-t. A Root továbbra az
ötphoneUIhibát javítja, második író erre nem indul. Kiosztás kijelölve, tényleges
munkásindulás még nincs külön igazolva. Main/migration írás tilos a két chatnek.

## PR263–266 root audit/integráció – 2026-10-26

263exactcd4cedb5 →504b6890,264exact0ad66986 →f587e89d. Saját scoped
native-android.yml változatok KIHAGYVA. Root ellenőrizte exact SUCCESS runokat
40395096019/40389898998 és letöltöttXML:263JVM35/0fail/skip,4újUI/0,
pluszhostregresszió raw külön;264JVM12/0,UI6/0. PNG-struktúra9+5képenPASS.
263ZIPhashok egyeznek PRreporttal,rootletöltések /tmp/tp-review-<artifactID> alatt.
Saját scopedgreen nem teljes integrált elfogadás. Root3297e82b továbbiProgress
read→publish Continuation védelem: restore közben indult régi olvasás nem publikál.
Ez az extra javítás Andro­idon még UNRUN, nem része a külső greennek.

265exacta902987f review: új appkód nincs. Rootda718b20 csak report+statikus
routing scriptet vett át; branch-only build-apk PRtrigger/nativecomment/újQAworkflow
kihagyva.40395802353QuickSUCCESS legacy SKIPPED,7policycase ténylegesCIbizonyíték.
Root most helyben7policytest+valósnativebesorolás+összesworkflowYAMLparse+
11workflow14setup-node guard ellenőrzést is lefuttatta:PASS. Node-lockfile
setuphiba forrástípusgyökérok javítva; régi ágak/régi history ettől nem színeződik át.

266exact1b3041b6 →ed6fd283 docs-only6handoff. Root index frissült:
Chat1/2Home/Progressreview már kész;Chat3phone5hiba Rootnál;4A07 és5Gtriage
külön kiosztható, új appbaseline3297e82b. Nincs belső subagent/main/close.
4locale7home+14progress kulcs egyezés/diffPASS. A hiányzó /tmp Kotlin lexical
segéd NEM futott, erre ne állíts PASS-t. Új közös nativeCI kézzel elindítva
3297e82b-n, pontosrunID/kimenetel GitHubról ellenőrizendő. Csak egy közösgate.

## Két új chatbekötés, G01/G10 – 2026-10-26

User két további elkülönített chatfeladatot kér. Exactbase30e087646298fb0191f5ffa0e7e157b14519d669;
promptok76836a1c: plans/NATIVE_HOME_RECOVERY_CHAT_HANDOFF.md →
feat/native-home-recovery-provenance (HomeVM/HomeScreen+újhelper/resource/own tests);
plans/NATIVE_PROGRESS_PHOTO_RANGE_CHAT_HANDOFF.md →
feat/native-progress-photo-range-binding (ProgressVM/Screen/Query+újread-onlygallery/
range/locale/own tests). Pontos diszjunkt fájlhatár az MD-ben, mindkettő scoped own
CI-debug+JVM+API35UI zöldre futtatást kér. Új saját QAworkflow-másolat megengedett,
Root canonicalworkflow-ja nem integrálhatja a sajátQA-deltát. Main/migrationpush,
merge/force/version/schema/coreírás tilos. Nincs belső subagent. Indulás még nem igazolt.
Root ne módosítsa e Home/Progress fájlokat párhuzamosan; nála marad NativePhoneNavigationTest,
aktív editor/interval/GPS/Navigation és közös CI. A vizuális-Codex még docs-only,
productiondesign nem kapja meg a most kiosztott Home/Progress határt.
G01 valós native score-forrás/freshness, nem új képlet; G10fotók/range nem újwriter.

40372076529/1d2be2a5 LEZÁRT FAIL telefonosUIjobon; debug+1312JVM+2upgrade+
3WearroundjobSUCCESS. Friss phoneartifact roottriage még hátra, régi91/98nem erre
érvényes.162746c7Home-root-navigációfixet a lezárt1d2run MÉG NEM tartalmazza.

## Root átvette PR261-et, közvetlen migration integráció – 2026-10-26

User explicit utasítása: beragadt külső chat helyett Root vegye át a munkát,
KÖZVETLEN a feat/native-android-migration ágon integráljon/javítson/teszteljen.
A korábbi ui/** külső területfoglalás Root számára feloldva. Másik dizájn-Codex
production területe továbbra nincs automatikusan feloldva; elsőként stabil QA-baseline.
#261exactd01496a61e35a0ea0c6e5ce4057550686670de36 →49ab93f5,
16app/test/script/report fájl integrálva, sajátQAworkflow KIHAGYVA.
Root151f0200: UiDevice dumpsys pipe/grep helyett nyers ablaklista+Kotlin szűrés;
modaldiagnosztika több szemantikai gyökeret kezel; expandbutton childscroll;
duplaimport és formázás tisztítva.34evidence+7gate Python PASS,4Kotlin lexicalPASS.
#260exactef22711e →79ab227a:5adat/test/report/scriptfájl integrálva,
QAworkflow KIHAGYVA. Saját8/8scoped green nem közös runtimePASS.
Canonical native-android.yml kézi új futás elindítva79ab227a forráson; runID és
kimenetel GitHubról ellenőrizendő. Ezen a forráson Androideredmény még nincs.

Utolsó261teszt40362238384 exact41dc60b2artifact12670330542 /tmp/tp261-phone:
94main=88PASS/6FAIL +4GPS=3PASS/1FAIL, együtt91/98PASS, no runnercrash.
Hátra akkor:2intervalelem,customdelete diagnostics,workoutleave pause,
Homeshortcut,programrestore selector,GPSpermissionfocus. Az új d014delta és
root151f erre még UNRUN; új synthetic route teszt miatt össztesztszámot újraszámolni,
nem a régi98-at rögzíteni. Dí­zájn következő: QA stabilbaseline→G01–G12triage→P0–P6.
Main/verzió/GitHubmerge/PRclose változatlan. Root exact sourceintegration nem mainmerge.

## PR262 dizájnspecifikáció review és integráció – 2026-10-26

Exactf0ccb90cd9a982281d22b03a6288b6d5d94b327f dokumentációs delta integrálva:
11új fájl kizárólag docs/design/final-ui-ux/**. Olvasási basec116a042.
Root ellenőrizte mind64nativeRef fájlSHA256+szimbólum/sorszám egyezését gitből,
24view/contract/binding/acceptance ID-t,81/81A–E megőrzési követelményt és32palettát.
Minden acceptance NOT_RUN, nincs futtatott appUI vagy screenshot elfogadás.
Kezdőlap/aktív mérés/funkciófolytonosság és5fül specifikáció olvasva; jóváhagyott
előképek mellett WebView-elemek konkrét megtartása rögzített. Nyers before/after
bizonyítékterv,48dp/kisfont/hosszúDE-RO/accessibility és állapottábla megvan.
12forráseltérés/korlát G01–G12 külön triage: nem12automatikus kötelező funkcióhiba
(pl.G08modalgear megengedett,G06tile új scope). Valódi új domainhook Rootnál,
dizájn-Codex nem írhat hozzá motort/adatútat vagy kitalált demó-sikert.

Dizájn ELŐKÉSZÍTÉS átvehető; production megvalósítási terület nincs feloldva.
PR261UI-QA aktív, új látottHEADc468a476, ebben a körben új runtime nem ellenőrzött.
Ne induljon rá párhuzamos UIíró. Következő:261elfogadás/integráció, a12pont aktuális
forráson triage, exactbeforebaseline, aztán P0–P6 vizuális megvalósítás+valódi képek.
Main/verzió/GitHubPRmerge/close nincs;262draft megtartva.

## Telefonos végkapu folytatása: PR261 – 2026-10-26

User pontosítása: a259utáni nagy telefonos UI-munka a261draftPR-ban követendő.
PR261 fix/native-phone-ui-final-gate →migration, jelenlegiHEAD
177bc42945473a3f6685e829932e02631c5f44e1; pinnedbase de1fe140.
Ez a korábban kiosztott Chat1 csomag, nem új adatmotor;259korábbi e6aba26c
változása már integrálva. Root nem szerkeszti az aktív ui/**/phoneQA fájlhatárt.
PR-report szerint első40345297542:main94terminal77PASS/17FAIL+GPS3/4,
42nyers ellenőrzöttPNG; nem elfogadott kapu. Új40357491471 exact15de8b09
GitHubállapot root által lekérdezve:LEZÁRT FAIL phoneUIjob; build/JVM job,
2upgrade és3Wear shardSUCCESS. A PR-bodyban még pending telefonos státusz
elavult; nyers friss phone darabszámot itt még nem validáltuk. Saját QA folytatódik.
PR261QA-only workflowdelta nem integrálandó. Main és verzió változatlan.

## Végleges UI/UX irány és PR260 állapot – 2026-10-26

User külön Codexben végleges UI/UX-et készíttetne: WebView minőségi minimum,
grafit/arany referencia,5fül,minden fő/aktív nézet,teljes funkciómegőrzés,
motor/adatút változatlan, valódi emulátorképek. Rögzítve dce0c8a3:
plans/NATIVE_FINAL_UI_UX_CODEX_HANDOFF.md és design/NATIVE_DESIGN_HANDOFF.md.
Aktív UI-QA chat továbbra ui/** tulajdonos. Dizájn-Codex most CSAK saját
specifikáció docs/design/final-ui-ux/** alatt; production UI/bekötés csak a QA
átadása és Root explicit területfeloldása után. Nem indul automatikusan subagent.
Modelljavaslat6.1Sol; Astra opcionális review. Vizuális specMedium/bekötésHigh/
nehézdebugExtraHigh; ez javaslat, nem végrehajtott modellváltás.

#260 exactef22711e5a0505fabbb56c7f85bc0a562cb88df9 adatcsomag: GitHub
40352624723 ugyanazonSHA8/8jobSUCCESS függetlenül lekérdezve;1312JVM és
0/200/1500JSON+ZIPphoto+invalidimport+2upgrade a saját report szerint.
Root olvasta az ArchiveIO central-directory/size staging javítását, JVM tesztdeltát,
új valós manager roundtrip semanticSignature tesztet és a második upgrade verifyt.
XML/összesartifact root validáció és integráció még HÁTRA. NINCS roothozintegrálva;
QAworkflow cseréje soha nem átvehető. GPS coordinatebytes nincs a fixture-ben,
JSON semantic hash részhalmazt fed, nem minden mező teljes körű bizonyítéka.
#259 aktuálisHEADe6aba26c változatlan, ez már42df5e54-ben integrálva; nincs új
beolvasandó259delta. A teljes phoneUI-final-gate munkához új PR még nem látszik.
Main/verzió/PRmerge/close nincs.

## Két nagy külső chatcsomag – végigvitt saját QA, 2026-10-26

User limit miatt2nagyobb, önállóan javító/tesztelő chatfeladatot kér, új Codex nélkül.
Exactbase de1fe14062cfe6675ec2b3c12d13945b327683b6; promptok16515d7b:
- plans/NATIVE_PHONE_UI_FINAL_CHAT_HANDOFF.md →fix/native-phone-ui-final-gate:
  teljes Compose/phoneUI/fixture/screenshots+host gate+evidence parser.
- plans/NATIVE_DATA_INTEGRITY_FINAL_CHAT_HANDOFF.md →fix/native-data-integrity-final-gate:
  BackupRestoreManager/ArchiveIO/data-migration/nativeUpgrade/migration-seed/
  signed-upgrade script, saját NativeDataIntegrity* tesztek. UI nem írható.
A két dokumentum pontos diszjunkt fájlhatára az irányadó. Root e területeken nem
fejleszt párhuzamosan; review/integráció marad. Indulásuk még nem igazolt.
EZEKHEZ kifejezett eltérés: saját refen a regisztrált native-android.yml dispatch
és saját workflow-másolat scoped QA-ra átírása engedélyezett. A workflow-delta NEM
integrálandó roothoz. Minden app/test módosítás csak sajátágon/draftPR célmigration;
main/migration írás/merge/force/close tilos. Csak statikus átadó nem befejezés:
a saját valós kapukat javítsák zöldre, vagy konkrét külső blokkolóval jelöljék UNRUN.
Rootnál továbbra szükséges összevont kapu és készülékes jóváhagyás; nem ígérhető
előre, hogy csak ellenőrzés nélküli merge marad. Másik ág motorhibája patchként átadó.

Friss CI40342319155/14513compileFAIL a Calendar UI harness split-line indexelésén;
de1fe140 explicit ViewModelProvider.get(...) javítás benne a kiosztási baseline-ban.
Nincs új sikeres Android futás erre. A14513GPS runner fix runtime továbbraUNRUN.
Most nincs aktív rootCI, az új futások a két saját QA-ágon indulhatnak.

## PR258/259 második review integrálva; közös CI új blokkolója – 2026-10-26

User átadta az új HEAD-eket, root forrásreview és integráció kész:
#258 exact8d9ed3a74fc695f2548564e9b2fd587970f315c1 →20e6790c (5fájl).
Request epoch cancel/new/clear késői publish ellen, readiness/fallback,7UIforrás.
#259 exacte6aba26c879c3cae2e70c4724e7ab6a8a6c94adc →42df5e54 (7fájl).
Reentráns metadata lease fogja össze az isCurrent+approve-ot, comparison Snapshot
Continuation védi az azonos bytesú restore ellen;10újUI/race forrás (8+2).
10Kotlin lexical és Android diff-check PASS; ezen az integrált forráson compile/
JVM/UI runtime még UNRUN. Két külső chat területfoglalás feloldva; saját draft PR-ok
nyitva, GitHub-merge/close és main/verziómódosítás nincs.

Importfix utáni Actions40334773058 exact e12ed8a5 LEZÁRT FAIL. Debug+JVM+R8/signing,
2signed upgrade és3Wear round job SUCCESS; új XML számlálás itt még nem történt.
Phoneartifact12657351421 /tmp/tpe12-phone-ui:82declared,7terminal=6PASS/1FAIL,
75missing, processCRASH,0PNG. Szigorú offline parserrel root ellenőrizte.
A7. befejezett eset Google maintenance teszt; Cardio3/3 és Google3/3 itt PASS,
a régiCalendarUI az1FAIL; az új258 ebben a futásban még NINCS benne.
GPS approximate teszt kezdetekor logcat14:06:48.080 ActivityManager:
Killing2238:com.repforge.app: permissions revoked. Ez a #256tesztkörnyezet
engedély-visszavonása, nem igazolt produktív GPS-hiba. Következő rootfeladat:
engedélyállapotok instrumentáció előtti külső előkészítése/izolált tesztfolyamat,
majd közös kapu. Ne ismételj teljesCI-t a garantált runnerkill javítása előtt.

## Két külső chat területfoglalása – 2026-10-26

User most két ChatGPT-chatnek kér munkát, új Codex/belső agent nélkül.
Exact base98ef71b2e4f2fa4f08b415c3d0a94d113b62303d; promptok e8985f10:
- plans/NATIVE_CALENDAR_UI_REPAIR_PLUS_HANDOFF.md → saját fix/native-calendar-planner-ui-readiness.
  CalendarScreen/VM, saját planner UI teszt: readiness és Mégse/mentés/újranyitás.
- plans/NATIVE_MODE_UI_REPAIR_PLUS_HANDOFF.md → saját fix/native-training-mode-approval-ui.
  TrainingProgramsScreen/VM, NativeTrainingModePicker/Comparison, saját modes UI teszt.
E területeket Root ne módosítsa párhuzamosan. Megosztott fixture/NavHost/SettingsStore/
motor/workflow nem írható a munkásoknak. Saját draft PR célmigration; nincs main/
migration push/merge/close. Teljes CI Rootnál; runner hiányában runtime UNRUN, nem PASS.
Rootnál marad cardio/locale/sleep/navigation UI és közös screenshot/CI integráció.
Feladatok kiadhatók; indulásuk még nem igazolt. Régi PR-ok lezárásához exact integráció
és megfelelő tesztbizonyíték szükséges; a nyitott draft önmagában nem új hátralévő funkció.

## PR256/257 root review és integráció – 2026-10-26

#257 exact533c7f0fab56957109e37e4af07586ee34409eb5 →993c7640:
négy saját fájl integrálva; root újrafuttatta a32stdlib tesztet:32PASS.
Valós e28phone loggal is reprodukálva75/75terminális,35PASS/40FAIL,0missing,
no crash; a parser helyesenFAILED. Ez parsereredmény, nem app runtime PASS.
#256 exact3cfa7b9a403db99d56fe9234873729edbc4d3407 →f3dd4659:
kilenc app/teszt/resource/handoff fájl integrálva, külön QAworkflow kihagyva.
Root kódreview,4Kotlin lexical,4locale/14kulcs és diff ellenőrzés PASS.
Fordítás és11JVM+4Compose runtime UNRUN: közös natív kapuban igazolandó.
Nem lezárt release-elfogadás. Main/verzió/külső PR-merge/close változatlan.
A GPS és evidence területfoglalás feloldva; root további UI-debug következik.

## Indulás – rövid kontextus

1. Olvasd el ezt a fájlt; kontextustömörítés/új chat után a `docs/NATIVE_RESUME.md`
   rövid folytatási pontot is, majd a `docs/WORK_HANDOFF.md` aktuális állapotát és
   következő lépéseit. Ne olvasd újra a teljes beszélgetést vagy a nagy naplókat.
2. Ellenőrizd: `git status --short`, aktuális ág/HEAD, GitHub PR/issue és Actions
   állapota. A dokumentált állapot pillanatkép; ne találj ki későbbi eredményt.
3. Csak a konkrét feladathoz olvasd a belinkelt részletes tervet/forrást.
   Kereséshez `rg`, fájllistához `rg --files`; rövid, célzott kimeneteket kérj.
4. Ne írd felül más vagy a felhasználó nem commitolt módosításait. Ne indíts
   párhuzamos agenteket külön erre vonatkozó kérés nélkül.

## A felhasználó munkamódja

- Magyarul, röviden és érthetően kommunikálj. A felhasználó kifejezetten kéri:
  kevés lényegi visszajelzés, rövid célzott tool-output, cachelt/belinkelt források
  használata; ne ismételd folyamatosan a tervet vagy a teljes korábbi kontextust. A „folyt”, „folytasd”, „csináld”
  a már egyeztetett feladat végrehajtását jelenti, nem újabb tervkérés.
- Haladj önállóan a jóváhagyott keretben; rutinszerű, visszafordítható javításhoz
  ne kérj újra engedélyt. Hosszabb munka közben adj érdemi állapotjelzést.
- A natív ág követhető állapotlapja a draft [PR #238](https://github.com/fuloplevente1998/TrainPilot/pull/238).
  Részcommitok és igazolt eredmények után frissítsd a leírás pipálható listáit;
  megírt kód, futtatott ellenőrzés és hátralévő munka külön szerepeljen.
- A felhasználó nem akar órákat várni helyi teljes tesztekre és APK-fordításra.
  **Teljes regresszió, Chromium/emulátor, Gradle és aláírt APK: GitHub Actions.**
  Helyben csak feltárás, szerkesztés, szintaxis/diff és indokolt célzott gyors teszt.
- A végén konkrét eredményt adj: mi változott, milyen forráson mit ellenőriztél,
  mi van még hátra; APK-nál telefon/Wear pár, commit, verzió, hash és letöltés.
  A PWA/böngészős előnézet nem helyettesíti a natív készülékes próbát.
- Verziót ne emelj automatikusan. Változtatás előtt ellenőrizd a package.json,
  SOURCE_VERSION.json és a két Android Gradle-fájl aktuális metaadatait; a legutóbbi
  jóváhagyott forrás/kiadás a handoffban található.
- Új funkció draft PR-on/fejlesztési ágon készül. Main merge, release és issue/PR
  lezárás csak az adott feladatra vonatkozó készülékes elfogadás/jóváhagyás után.
  Korábbi másik funkcióra adott „mehet mainre” nem általános merge-engedély.
- Ne küldj GitHub-kommentet vagy másoknak üzenetet külön kérés nélkül.
  A kért nyomonkövetéshez issue/PR leírását és állapotát frissítheted.

## Aktuális QA visszaadás – PR256/257

#255a671e50e Rootnál integrálva b96b94e8 +d735dbb5 disconnect pause fix;
Google/Settings foglalás feloldva. #256 GPS és #257 evidence summary még saját
ágon, nem integrált: a user visszaadja ellenőrzésre és tesztelésre.
plans/NATIVE_PR256_QA_FOLLOWUP.md és NATIVE_PR257_QA_FOLLOWUP.md az aktuális
kiegészítő szerződés4e12b9df-ben. #256 saját manual-only scoped QA workflow most
megengedett; canonical/main/migration írás továbbra tiltott a munkásnak.
#257 nincs Android CI/APK, csak offline teszt és valós artifact-ellenőrzés.
Root várja az új exact HEAD-et, a régi deltát ne integrálja vakon. Main merge nincs.

## Új három külső feladat – 2026-10-26, 32547e8a

Pinned e28ab418. Átadva: plans/NATIVE_GOOGLE_MAINTENANCE_CODEX_HANDOFF.md
(GoogleRepo maintenance+Settings UI/VM), NATIVE_GPS_RECOVERY_PLUS_HANDOFF.md
(ActiveWorkout phone GPS permission/retry), NATIVE_CI_EVIDENCE_PLUS_HANDOFF.md
(új offline evidence summary script/tests). Pontos diszjunkt fájlhatárok az MD-kben;
területek most ezeknek foglaltak, indulás külön még nem igazolt. Saját ági célzott
próba/teszt-APK kérhető, de közös teljes CI Rootnál. Nincs belső subagent,
main/merge/verzió/élőadat-művelet. Root nav/CI-debug/integrátor marad.

## Új aktuális integráció – 2026-10-26

#252936e5df/#2533d45d3ae/#254c6366826 Rootnál integrálva f208617c/acd6927c/624aaf01,
majd f72c9cb0 integrációs és UI-debug javítás. A lentebbi három területfoglalás
feloldva: a felhasználó késznek adta át mindhármat. Nincs új belső agent-kérés,
main/külső PR-merge/lezárás nincs. Új külső delta a pontos átadott SHA óta review.
Aktuális runtime/egyetlen CI mindig a NATIVE_RESUME tetején. User Extra High-t kér
az aktuális CI-debughoz; dizájnfinomítás előtt Medium jelzés később.

## Aktuális külső kiosztás – 2026-10-26, PR250 után

Három új átadó elkészült9dfb155a-ban, pinned app7043fc7d; indulás még nem igazolt:
plans/NATIVE_CALENDAR_MULTIWEEK_HANDOFF.md (Codex), NATIVE_ONBOARDING_HANDOFF.md
(Plus1), NATIVE_PROGRAM_ACTIONS_HANDOFF.md (Plus2). Fájlhatár az átadókban, saját
új ág/draft célmigration, nincs munkás oldali teljes build/CI/main-merge.
Home/onboarding/nav/onboarding-only Store, Calendar/new planner és Program actions
területeket most fenntartjuk ezeknek. Root Journal/Health/sync/review/közös CI.
Nem belső subagent felhatalmazás. A későbbi rootb28672bf Coach/Home flat-query
javítás beemeléskor megőrzendő, a munkások ne reseteljenek a pinned bázisra.
#250fb64307 audit integrálva85f09350; aktuális alkalmazás/runtime/CI mindig a
NATIVE_RESUME tetején, történeti lentebbi SHA/státusz nem mai állapot.

## Jóváhagyott párhuzamos lokalizáció (2026-10-25)

- A felhasználó külön ChatGPT-beszélgetésnek adja a telefonos UI megmaradt
  HU/EN/DE/RO feliratainak lokalizációját. Ennek saját ága
  `feat/native-ui-localization`, funkcionális kiindulása `958907c`.
  Ez a konkrét feladat kivétel a handoff általános „csak migrációs ág” szabálya alól.
- A lokalizációs munkás csak a neki kijelölt ágra írhat; a main és
  `feat/native-android-migration` változtatása/merge tiltott számára.
  A migrációs munkásnál marad a cloud-fotó, backup/szinkron, törlési replay és motor.
- Pontos fájlhatárok, ellenőrzések és átadás:
  `docs/plans/NATIVE_UI_LOCALIZATION_HANDOFF.md`. Nem felhatalmazás belső
  subagent indítására vagy további munkák önálló kiosztására.
- A lokalizáció alatt a migrációs munkás a lefoglalt Screen/Dialog/text-helper/
  négy strings.xml fájlokat lehetőleg nem módosítja. Az eredmény külön draft
  PR/patch; közös review és integráció a migrációs ágon, main merge nélkül.
  Kód/tesztkód és futtatott eredmény megkülönböztetése továbbra kötelező.
- A külső lokalizáció draft #240 (82883ab) és a külön nyelvváltási tesztrész
  draft #241 (ab25d84, cél #240 ága) kódját a fő munkás a migrációs ágba
  squash-commitokkal integrálta: 3b548ac /6cf13be. Külső ágakra nem írt, a PR-ok
  nyitva maradnak a közös futtatás/review lezárásáig; ne merge-eld rájuk újra
  vakon ugyanazt a régi diffet. Új külső commitot az exact HEAD óta külön vizsgálj.
- A #241 jelzett hiányát NativeLanguagePicker pótolja a fő ágban. A hat külön
  nyelvváltási próba az integrációban már a settings_language_<code> választót
  kattintja; a fő UI-készlet nyelvváltási próbája is. Kód és futási eredmény külön:
  a négy unit +hat UI integrálása még nem zöld CI vagy készülékes elfogadás.
  Részletek: docs/reviews/NATIVE_LOCALIZATION_INTEGRATION.md.
- A külön funkcióaudit visszaérkezett draft #242-ben: test/native-feature-parity-audit,
  exact 9c3c264. Csak egy report változott, a migrációba byte-pontosan átemelve.
  Rövid aktuális triage: docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md; a hosszú
  eredeti riportot csak konkrét forráshivatkozáshoz olvasd. Hét forrásszintű hiány,
  ebből program Rx b161126/97f18fd és Journal edit1a9b6d3/6856abe kódban
  pótolva, nem futtatott elfogadás. Külső ágra nem írtunk, PR-merge/lezárás nincs.
- A három új külső részfeladat visszaérkezett: #243 exact1bf745e Health date
  range →1864a8c/c611cc7, Gemini teljes cardio .kt fájlok →5aacc8a/0f8bb70,
  két nyelvi audit →2ff85ad. Reviewk: docs/reviews/NATIVE_HEALTH_DATE_RANGE_INTEGRATION.md,
  NATIVE_CARDIO_INTEGRATION.md, NATIVE_EDITOR_TRANSLATION_REVIEW.md. A cardio patch
  csonka, ne alkalmazd; a teljes javított fájlok a mérvadók. Régi fájlfoglalások
  véget értek, nincs belső subagent. Új külső delta mindig exact refből review,
  ne integráld újra a régi diffet. Külső PR-merge/lezárás és zöld futás nem történt.

## Új külön chat átadók – három elkülönített csomag

**Aktuális (2026-10-26):** #246e482cd7 integrálva cf1db70b; #244b062e3d
(app7cb2c11) review P1/P2 javítása source-ban ellenőrizve, valós XML30 JVM+4 UI
PASS, integráció hátra. #245 távoli HEAD továbbra2ff2985 (REST és git), az öt
javítás még nincs távol, ezért nem integrált. A lentebbi korábbi „javítandó”/
„még nem érkezett” bejegyzések történetiek; a dátumos NATIVE_RESUME teteje mérvadó.

- A felhasználó új, teljes GitHub-hozzáférésű külön chateknek kérte a maradék
  három funkciót; 2026-10-25-én megerősítette, hogy mindhárom külön munka
  elindult. Két eredmény visszaérkezett: draft #245 edzésmódok exact2ff2985
  (kód/36 teszt megírva, nem futott), draft #246 cardio exacte9eb8cb
  (saját ágán debug fordítás és 26 kardiós JVM zöldnek jelentve; legfrissebb
  Actions40238833748 ténylegesen984/6 fail, UI/upgrade skipped). A harmadik
  draft #244 Health auto-refresh, exact023d411, még nem átadott eredmény.
  Root csak PR/meta/CI-log ellenőrzést végzett, alkalmazáskód-review/integráció
  még nem történt; main/migration appforrás változatlan. A limit miatti szünet
  folytatódik, következő lépés az exact diff review, nem automatikus merge.
  Bázis0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20, mindenki saját ág/draft PR,
  nincs main/migráció írás vagy belső subagent.
- A későbbi exact forrásreview elkészült: docs/reviews/NATIVE_PARALLEL_PR245_PR246_REVIEW.md.
  #245 öt javítandó megállapítás, #246 időfüggő heti frissítés hiány;11+15 kardiós
  JVM valós XML-ből ellenőrzött PASS. Nem történt kódjavítás/integráció/új CI.
- #2448c00b46 is visszaérkezett: reviews/NATIVE_PR244_HEALTH_AUTO_REFRESH_REVIEW.md.
  29 JVM+1 UI valódi XML PASS; P1 IO/main admission és P2 ZIP gate cleanup
  javítandó, még nem integrált. Az aktuális review-k felülírják a korábbi
  történeti „még nem érkezett” státuszokat; nincs új CI/main/release elfogadás.
- PR246 exacte482cd7 teljes appdelta integrálva cf1db70b-ben (review6 dátum/
  lifecycle fixszel). reviews/NATIVE_PR246_INTEGRATION.md. Külső draft nyitott,
  ne alkalmazd újra a régi diffet. PR245 távoli HEAD még2ff2985, nem integrált.
- A: docs/plans/NATIVE_CARDIO_BINDING_HANDOFF.md, feat/native-cardio-progress-ui.
  Progress UI/VM/query +DAO és új projection DTO. Math mag csak olvasható.
- B: docs/plans/NATIVE_TRAINING_MODES_HANDOFF.md, feat/native-training-modes.
  SettingsStore/settings/program UI +Coach advice +explicit active proposal,
  mapper csak settings-rész. Nincs Progress/DAO/Health/finalizer változtatás.
- C: docs/plans/NATIVE_HEALTH_AUTO_REFRESH_HANDOFF.md, feat/native-health-auto-refresh.
  HealthDashboard/NativeHealthRefresh és szükséges napi SDK-guard. Store/paging/
  workout-window HC/Settings/Progress/Coach csak olvasható.
- A pontos fájlhatárt az átadó adja. Az új saját tesztosztályok ne írják át a
  közös NativePhoneNavigationTest-et. Ezek a területek az átadás idejére a külön
  munkásoknak fenntartva; fő munkás integráció/review, fennmaradó Journal/main
  leltár és keresztmetszeti hibák. Visszaérkezéskor exact delta/teszt és paritás
  ellenőrzése kell, nem vak merge; CI/készülékes/main elfogadás továbbra külön.

## Új elkülönített baseline feladatok – 2026-10-26

**Új aktuális:** #245305b560 integrálva7d773190, #24720b7ddb→6a5b6588,
#248aa01f3b→e344fed5. #244/#246 is bent. A régi függő SHA/fájlfoglalások
feloldva; új külső delta mindig innen review. Appforrás e344fed5, közös CI
következik; statikus ellenőrzés és Node validator PASS nem natív runtime.

- #244b062e3d integrálva4eae08c8-ben, #246cf1db70b megmarad. Ne alkalmazd újra
  a régi appdeltákat. Új közös CI nincs; #245 javított SHA még függőben.
- Felhasználó két új külső chathez kért promptot. Pinned bázis4eae08c8:
  docs/plans/NATIVE_BASELINE_HARNESS_HANDOFF.md: kizárólag három JVM-teszt
  (MetadataGate, WorkoutDiscard, HomeRepository), saját test/native-baseline-harness-fixes.
  docs/plans/NATIVE_PLANNER_PARITY_FIX_HANDOFF.md: PlannerPolicy/ProfileProgramPreview,
  planner teszt/fixture, saját fix/native-planner-template-parity. Pontos határok az MD-kben.
- Indulást még nem igazolt a felhasználó; az átadás idején ezeket ne írd át
  párhuzamosan. Root közös integráció/gate és maradék leltár. Nem belső agent-
  felhatalmazás. Új teljes CI és main merge továbbra központilag koordinált.

## Aktuális új kiosztás – 2026-10-26

- #25191fb333 Health-sync kódja review után integrálva4b1711b1; korábbi
  Health-fájlfoglalás feloldva. Ne alkalmazd újra a régi diffet.29JVM+1UI
  új kód nem futott; reviewkorlátok NATIVE_PR251_INTEGRATION.md-ben. #250
  továbbra külön chatben javítás alatt, két auditfájlját Root nem írja.

- Új állapot: #249224999b integrálva654c016c; #250a360a5a audit byteazonosan
  importálva be5e2642. A felhasználó a #250 audit két fájlját ugyanabban a
  Plus chatben folytatja; Root ne írja párhuzamosan. Új teljes prompt
  docs/plans/NATIVE_PR250_AUDIT_CONTINUATION.md; exact main chunkindex
  docs/audit-input/main-7473b30/INDEX.md ésmanifest. Root implementálja majd
  a bizonyított hibákat, a80UNRESOLVED nem80bizonyított hiány.

- Felhasználói módosítás: NINCS Gemini feladat; egy külön Codex implementál,
  két Plus chat GitHub-hozzáféréssel dolgozik, Root mindent review-z/integrál.
- Pinned e9278a99 bázis. Teljes szerződések:
  docs/plans/NATIVE_HEALTH_SYNC_COMPLETION_CODEX_HANDOFF.md (Health sync30nap/
  changes; meglévő Health/JournalStore/Refresh/Dashboard/gates írható);
  docs/plans/NATIVE_FINAL_FEATURE_INVENTORY_PLUS_HANDOFF.md (csak két auditriport);
  docs/plans/NATIVE_THEME_CONTRAST_PLUS_HANDOFF.md (themeTokens/TrainPilotTheme/
  NativeThemePicker és külön saját teszt/resource/riport). Store/mapper theme chatnek readonly.
- Átadásra kész, indulás még nem igazolt. Az írási területek foglaltak; Root nem
  kezd párhuzamos feature-implementációt. Közös CI és szükséges integrációs fix
  Rootnál, külön koordinációval. Nem belső subagent-felhatalmazás.

## Felhasználói thinking-fázisok (2026-10-24)

- Kódolás, funkciók és meglévő blokkok bekötése: a felhasználó **High** módot használ.
- A vizuális dizájn/layout finomításának kezdete előtt röviden jelezd a fázisváltást,
  hogy **Medium** módra válthasson. UI-teszt/infrastruktúra kódolása még kódolási fázis.
- A végső CI-kapu hibájánál vagy komoly debugmunka előtt jelezz, hogy
  **Extra High** módra válthasson. Közben a naplók/bizonyítékok feltárása folytatható.
- Ezeket a felületi módokat a felhasználó állítja; ne állítsd, hogy önállóan átállítottad.
  A jelzés nem új jóváhagyási kör, és nem változtatja meg az elfogadott feladatot.

## Aktuális natív befejezési munkamód (2026-10-24)

- Aktuális elfogadott teljes feladat: `docs/plans/NATIVE_FINAL_DELIVERY.md`.
  Teljes 24 nézet/témakatalógus, öt funkcióterület, majd azonos forrású CI/APK
  és valódi emulátoros screenshotok; új generált mockup nem készül.


- Felhasználói sorrend: előbb az öt hátralévő terület kódja és tesztkódja,
  utána összevont funkcionális/UI ellenőrzés. Ne indíts minden részcommit után
  teljes APK/emulátorkaput. Köztes kódolási commit `[skip ci]`; végső kapu
  a meglévő natív workflow-val, rögzített alkalmazásforráson, GitHubon.
- A meglévő Kotlin-blokkokból indulj ki: manifest/export és tényleges hívási út
  ellenőrzése után javítás/bekötés. Prototípust és szimulált adatforrást ne
  tekints kész termékfunkciónak. Részletes sorrend és forráshelyek:
  `docs/plans/NATIVE_COMPLETION_BATCH.md`. Kész kód nem jelent zöld tesztet.

## Forrás és ellenőrzés

- Issue megvalósítása előtt olvasd el a GitHub-jegy tényleges követelményeit.
  Az ág neve és a zöld teszt önmagában nem igazolja a feladat teljesítését.
  Átadáskor az elfogadási feltételekhez mérd a kódot; eltérő fejlesztést ne
  jelents az eredeti issue megoldásának, és ne zárd le miatta a jegyet.
- Repo: `fuloplevente1998/TrainPilot`. Webes kanonikus forrás: `www/app.js`,
  `www/index.html`, célzott CSS; telefon: `android/app`, óra: `android/wear`,
  közös natív logika: `android/shared`. Ne portolj át külső előnézeti projektet
  vagy duplikált generált Capacitor asseteket vaktában.
- A tényleges `package.json`/workflow a mérvadó. Ne állíts Vite/TypeScript buildet
  vagy sikeres tesztet egy külső átirat alapján. Ne adj hozzá második APK-workflow-t
  a meglévő, aláírt release gate-ek megkerülésére.
- Referenciaforrást a jóváhagyott Git-refből és fájlhash alapján olvass
  (`git show <commit>:<path>`). Egy másik munkafa könyvtárneve nem bizonyítja,
  hogy az a main aktuális forrása; annak checkoutját ne változtasd meg ezért.
- Először a hiba okát javítsd. Tesztelvárást csak elavult szerződés vagy igazolt
  teszthiba miatt változtass; működési regressziót ne rejts el tesztgyengítéssel.
- Az eredményt mindig pontos commitra vonatkoztasd. A PR-merge tesztforrásnál
  ellenőrizd a source/tree egyezést. Korábbi zöld CI nem a friss teljes funkció
  igazolása. A készülékes próba külön bizonyíték.
- Végső funkcióátadásnál azonos alkalmazásforrású Quick, Performance, 6/6 telefonos
  Chromium, telefonos/Wear natív release és kerek UI/platform kapu kell, az adott
  változásra alkalmazható ellenőrzésekkel. XML-ben is vizsgáld a hibát/kihagyást.
- Ne indíts minden apró lépésre duplikált teljes kapukat. Dokumentációs commit
  és alkalmazásforrás különbözhet: a bevizsgált APK tényleges forrását őrizd meg,
  dokumentáció miatt ne állítsd, hogy új funkcióbuild készült.
- `gh run download` átirányítása ebben a környezetben 403-at adhat: használd a
  GitHub connector artifact-letöltését. File Service azonosító nem Sediment ID.
  Teljes aláírt URL-t, credentialt, binárist vagy hosszú logot ne írj a kontextusba.
  APK-átadáshoz GitHub artifact permalinket (ZIP, benne APK) vagy közzétett release
  letöltést adj; belső /workspace fájlút a felhasználónál nem működött.
- Ha új/lejárt környezetben dolgozol, a hitelesítést és a platformutasításokat
  újra ellenőrizd; régi workspace-path vagy eszköz elérhetőségét ne feltételezd.

## Szerkezeti rendbetétel – kötelező irány

- Központi műveletet ne bővíts újabb globális függvény-felülírással. Az edzés
  lezárásának egyetlen belépési pontja a `finishWorkout`, folyamata a
  `TrainPilotWorkoutFinalization`. Az érintett modulban közvetlen, név szerinti
  lépést módosíts; ne telepíts új finish wrappert.
- A tartós napló, futó session és helyreállítható draft külön felelősség. Mentési
  újrapróbálás stabil edzésazonosítóval legfeljebb egy naplósort eredményezhet.
  Natív lezárási vagy drafttakarítási hiba mellett nem küldhető „mentve” nyugta.
- Teljesítmény-cache érvénytelenítéséhez a tényleges adatváltozást kövesd.
  Naplóhossz vagy elemszám önmagában nem elég. A fokozatos terv:
  `docs/plans/APP_REFACTOR.md`; kész részt és következő lépést a handoff rögzíti.

## Megőrzendő termékdöntések

- Frissített natív UI-döntés (2026-10-24): a mockup megtekintése után a
  felhasználó az öt alsó füles változatot választotta: Kezdőlap, Edzés, Napló,
  Fejlődés, Egészség. Ez felülírja a korábbi nyolcgombos menü választását.
  A Beállítások jobb felső fogaskerékkel legyen minden főoldalról elérhető,
  aktív edzésből is; visszalépés ugyanoda, session újraindítása nélkül.
  A beállítások megnyitása nem edzésfélbehagyás, önmagában ne állítsa le/szüneteltesse
  a mérést. Mentés/lezárás alatti mutáció- és navigációvédelmek megmaradnak.
  A mockup a vizuális irány; illusztratív felirataiból ne találj ki új funkciót.
  Lásd `docs/design/NATIVE_MOCKUP_REVIEW.md`. Stabilitást/gyorsulást mérni kell.
- Compose-bekötéskor a fenti dizájndokumentum **hat kötelező feltétele** is
  érvényes: dinamikus draft/aktív folytatás; natív pihenő skip/±15 mp;
  gyakorlatváltás megmaradó pihenővel; „vázlatként mentés” = pause/kilépés és
  külön discard; Samsung/saját/korlátozott Coach pontszámok külön jelentése;
  csak valódi friss pulzus, forrás nélkül „— bpm”. Rögzítésük nem új tesztbizonyíték.

- Automatizálási UI-elv (2026-10-20): ugyanazon programnap új edzésének súlya és
  ismétlése a legutóbbi megfelelő mentett alkalomból töltődjön, külön másolásgomb
  nélkül. Teljesítettség/RIR/mérési adat nem öröklődik; draft és kézi módosítás védett.
  Engedélyezett ismétlődő szinkronhoz ne legyen kötelező gombnyomás. Kapcsolódás,
  rendszerengedély, tényleges teljesítés és terhelési döntés továbbra is tudatos.

- Fotóhivatkozás írásakor őrizd meg a NativeCloudPhotoDeletion admissiont:
  Room import a metadata lease-ben, kanonikus/public snapshot a meglévő store-
  monitor alatt ellenőriz. HTTP nem tarthatja a metadata/workout-start fence-et.
  Időzítő-frame ellenőrzése csak a root fotósorokat nézze, ne teljes set/GPS fát.
- Egy kanonikus edzés, stabil workout/set azonosítók, egy időzítő-tulajdonos.
  Átadás visszaigazolt és tartós; bizonytalan állapot szünetben marad. Kapcsolat
  visszatérése nem indíthat néma tulajdonosváltást vagy párhuzamos időzítőt.
- Explicit telefonos félbehagyás szüneteltet; háttérbe kerülés/kijelzőlezárás
  önmagában nem félbehagyás. Órás tulajdonos telefonos tükre csak olvasható.
- GPS-útvonal első engedélyezése tudatos döntés. A #216-ban elfogadott
  működés: indításnál látható, megjegyzett GPS-választás; a következő megfelelő
  kültéri edzés kifejezett indítása után automatikus rögzítés. App pontos helyengedélye és rendszerhely
  két külön feltétel. GPS-hiba mellett az edzés/stopper működjön tovább; ne találj ki pontot.
- Koordináták privát natív tárban, felhőmentésből kizárva. Nyugtáig tartós outbox,
  deduplikálás, szakaszhatárok; törlés után késői szinkron nem támaszthat fel adatot.
- A már elfogadott piros X bezárást ne cseréld automatikusan semleges körgombra.
  Kompaktabb UI mellett az érinthetőség, nagy betűk, fordítások és hozzáférhetőség
  maradjon használható; a külső prototípus számai nem kötelező követelmények.
- Telefonos GPS-es távolsági sorozatnál a sikeres, kifejezett „Mérés befejezése”
  készre jelöl és menthetővé tesz külön sorozat-OK nélkül (#206/#208). Térképbezárás,
  hibás vagy implicit GPS-stop önmagában nem teljesítés; ezt regresszióval védd.
- Telefonos időmérésnél az eredeti alsó sorozatstopper marad; külön felső
  intervallumos stopper ne jelenjen meg. Az intervallum beállítása opcionális,
  összecsukott rész. Explicit pozitív stopper-stop külön OK nélkül készre jelöl;
  implicit pause/navigáció/nullázás/0 másodperc nem. Kétoldali sorozathoz mindkét
  pozitív oldal szükséges. Az eredeti visszavonás/natív rögzítés megőrzendő.
- #188 Visszavonás és #189 egyetlen folyamatos 3 mp-es feloldás elfogadva/mainen.
  #192 és #211 sztornózott, kódjukat ne éleszd újra. #208-cal bekerült GPS STOP
  viselkedést ne fejleszd újra. A pontos nyitott sorrend a handoffban található.

## Memória és későbbi ötletek

- `docs/WORK_HANDOFF.md`: rövid, aktuális átadás és belépési pont.
- `docs/wear/P2_193_ONALLO_ORAS_EDZES.md`: #193 + #202 részletes megvalósítási terv.
- `docs/plans/UI_TEST_BRANCH_BACKLOG.md`: a 2026-10-19-én átvett külső UI/funkció
  ötletek. **Jelenlegi engedély: csak rögzítés, a megvalósítás nem kezdhető el.**
  Később külön tesztágon, az aktuális felhasználói folytatási kérés alapján.
- `DEVELOPMENT_STATUS.md`, `docs/CHATGPT_FOLYTATAS.md` és a közös Wear-terv hosszú
  történeti anyag: célzottan olvasd, ne töltsd be őket teljesen minden körben.
- Befejezéskor/átadáskor frissítsd a rövid handoffot: commit/ág, elvégzett munka,
  ellenőrzött kapuk/APK-k, nyitott pontok, következő konkrét lépés. Tartós szabályt
  ebbe a fájlba írj; teljes tool-logot és múló státuszt ne halmozz ide.
