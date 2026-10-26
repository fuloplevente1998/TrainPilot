# A 24 nézet kivitelezési szerződése

Minden **tervezett komponensnév** prezentációs javaslat; nem állítjuk, hogy már létező composable. A hivatkozott kisbetűs kódaliasok exact c116a042 forrásra mutatnak. A binding-dokumentum részletezi az adat/callback utat. A globális G-AC01…08 elfogadás **mind a24nézetre kötelező**, és az alábbi nézetspecifikus próbák hozzáadódnak. Minden screenshot-/runtime-állapot jelenleg NOT_RUN.

A nézetazonosítók V01–06 az1.tábla balról jobbra, fentről lefelé; V07–12 a2., V13–18 a3., V19–24 a4.tábla sorrendjét követik. A megosztott modalt több útvonal is megnyithatja; ez nem24külön route.

## V01. Kezdőlap

**Meglévő hely:** `home`. **Megőrzési ID-k:** A01, A03, A04, A05, C11, E05.

**Meglévő forrás:** [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62) · [homestate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L36) · [homeenergy](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt#L117) · [homequery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/NativeHomeRepository.kt#L18) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**WebView folytonosság és kép:** A main mai naptári elsőbbsége, Folytatás és gyorsindítás marad. A mockup arany Folytatás-kártyája kerül első helyre; a köszöntés nem lehet fix Levente, az óra neve nem fix Watch7.

**Felépítés, felülről lefelé:** Globális fejléc → aktív/paused/importált Folytatás hero (ha van) → különben következő valódi programnap → Gyors edzés → óraállapot → mai metrikák → Programok/Naptár/Coach. A folytatás mellett az ajánlott új indítás ne legyen vele versengő elsődleges CTA.

**Elsődleges / másodlagos műveletek:** Fő: meglévő banner selectDestination("training") aktívnál, onResumeDraft importáltnál, különben onStartWorkout(programId, dayId, scheduleId). Másodlagos: onQuickWorkout(false/true), onPrograms/onCalendar/onCoach; onOnboarding kihagyott profilnál.

**Scroll / billentyűzet / Back:** Egy vertikális scroll; visszatéréskor scroll és célzott nap megmarad. Nincs billentyűzet. Bottom aktív banner mért Scaffold-helyet foglal. Back nem indít új sessiont.

**Üres / töltő / hiba / permission és session:** loading: valódi folyamatjelző; loadFailed: loadHomeData retry, régi adat nem kap mai címkét. Nincs cél: programválasztó, nem inaktív demóedzés. Óraadat hiánya: ismeretlen/—. Samsung-forrás csak igazolt forrással.

**Nézetspecifikus elfogadás:** 320dp-n egy teljes Folytatás CTA és a session állapota elérhető. DE/RO 2.0 fontnál metrikák egy oszlopra váltanak. TalkBack a session állapotát a CTA előtt olvassa. Aktív+importált fixture: az aktív marad a kanonikus session.

**Forrásból bizonyított nyitott kötés:** G01, G02: Home recoveryText nyers energyScore; watchBattery nincs kitöltve. A felső aktív hero adat/callback már a scaffoldban megvan, a prezentációs slotot Root-reviewed hunkkal kell áthelyezni, nem új Home-motorral.

## V02. Edzésválasztó

**Meglévő hely:** `training`. **Megőrzési ID-k:** A03, A04, A06, B01, E01.

**Meglévő forrás:** [training](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt#L23) · [programs](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt#L63) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**WebView folytonosság és kép:** A bevált gyors beltéri/kültéri indítás és az aktuális program naplistája marad. A mockup három gyorscsempéje és kijelölt programnapja adja a ritmust; nem találunk ki becsült edzésidőt.

**Felépítés, felülről lefelé:** Gyors edzés fő kártya → Gyakorlatok/Kültéri két kártya → aktuális program neve → programnapok sorrendben → Programok/Naptár/Coach gyorselérés. Sok napnál vertikális lista, nem levágott A/B fixrács.

**Elsődleges / másodlagos műveletek:** Fő: onStart(programId, dayId), onQuick(false/true). Másodlagos: onLibrary/onPrograms/onCalendar/onCoach. Busy/aktív új indítás a meglévő nav szabályával folytatást kap.

**Scroll / billentyűzet / Back:** Főoldali scroll mentett helyzettel; nincs IME. Programnapok kártyája egy címzett startművelet, a teljes kártya és belső gomb ne legyen két fókuszállomás ugyanarra.

**Üres / töltő / hiba / permission és session:** Nincs aktív program: onPrograms elérés. Betöltés alatt nem villan üres programként az állapot; ahol nincs loading state, ne találjunk ki külön sikerállapotot.

**Nézetspecifikus elfogadás:** Kis szélesség/nagy betű: két gyorscsempe egymás alá, minimum48dp start. Hosszú napnév teljesen tördelődik. TalkBack név + gyakorlatdarab + Indítás; nem színből derül ki a kiválasztás.

**Forrásból bizonyított nyitott kötés:** —

## V03. Programok

**Meglévő hely:** `programs`. **Megőrzési ID-k:** A06, A07, A08, A09, A10, C14.

**Meglévő forrás:** [programs](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt#L63) · [programvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt#L138) · [programeditor](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt#L72) · [rx](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramPrescriptionDialog.kt#L32)

**WebView folytonosság és kép:** A main programaktiválás, másolás, saját szerkesztés és napi visszaállítás marad. A mockup kiemelt Aktuális program kártyája és listaritmusa megmarad; a Közösségi mintafül kimarad.

**Felépítés, felülről lefelé:** Aktív program + állapotcímke → további beépített/saját listák → Saját terv/új program → kártyán belüli naplista és műveletmenü. A training-mode összehasonlító előnézet külön modal, nem csendes kapcsoló.

**Elsődleges / másodlagos műveletek:** Fő: onStartProgramDay és activateProgram. Másodlagos: saveProgram, személyes terv openPersonalPlanner/savePersonalProgram, previewRestoreDay/confirmRestoreDay, deleteProgram, approveTrainingModeCompared/undoTrainingMode. Az editor callbackek változatlan draft/CAS célra kötődnek.

**Scroll / billentyűzet / Back:** Lista scroll visszaállítás; a program szerkesztése modal, IME a címmezőhöz igazodik. Back először az aktuális modalt zárja; mód-/összehasonlítási request epoch védelme megmarad.

**Üres / töltő / hiba / permission és session:** Betöltő/mentési hiba az adott programhoz; stale compare esetén új előnézetet kér. Beépített törlés/reset korlátozását nem oldja fel új menüpont.

**Nézetspecifikus elfogadás:** Kis képernyőn az aktiválás és menü elérhető; DE/RO hosszú címe több sor. TalkBack Aktív jelző + műveletnevek. Nagy fontnál a mode összehasonlítás vertikális, nem levágott kétoszlopos tábla.

**Forrásból bizonyított nyitott kötés:** —

## V04. Naptár

**Meglévő hely:** `calendar`. **Megőrzési ID-k:** E01, E02, E03, E04.

**Meglévő forrás:** [calendar](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L28) · [calendarvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarViewModel.kt#L35) · [calendarplanner](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt#L196)

**WebView folytonosság és kép:** A main naptári napkapcsolat és többhetes ciklus marad. A mockup havi rácsa + napi esemény-idővonala a cél; a jelenlegi napcsík ugyanazon month/day adataiból alakítható rácscsá.

**Felépítés, felülről lefelé:** Hónapváltó → hét napnevek + 7 oszlopos valódi hónaprács → választott dátum → tervezett/teljesített eseménykártyák → többhetes tervezőpanel. A rácspont csak meglévő plan/history eseményt jelez.

**Elsődleges / másodlagos műveletek:** Fő: onStart(row), completed esetben onJournal. Másodlagos: selectDay/changeMonth, save/status, plannerOpen lokális megnyitás, previewPlanner/applyPlanner/cancelPlanner. Terv előnézet és Mégse megtartandó.

**Scroll / billentyűzet / Back:** Hónapváltó és rács fölött nincs vízszintes képernyőscroll. Nagy betűn a rács számcellája48dp; ha7×48dp nem fér a belső szélességbe, a jelenlegi vízszintes napcsík a működő fallback, plusz teljes dátum fejléccel. Editor IME/date picker; Back a preview/edit réteget zárja, nincs mentés.

**Üres / töltő / hiba / permission és session:** Újraolvasási readiness/fallback és busy a VM állapotából; nem indítható idő előtt előnézet. Üres nap külön, üres hónap külön. Cancelled sorok VM-szűrését nem kerüli meg a UI.

**Nézetspecifikus elfogadás:** DE/RO eseménynév wrap, TalkBack teljes dátum + státusz + indítás. DST23/25h nap megtartja a helyi idő szerződését. 4 hét terv cancel/save/reopen és ugyanazon scheduleId indítása kötelező.

**Forrásból bizonyított nyitott kötés:** G09: Settingsből már van Google naptár sync, itt nincs külön onSync callback; nem rajzolható működőnek egy új inline sync gomb. Root nav-hook kell, ha itt is kívánják.

## V05. Gyakorlatkönyvtár

**Meglévő hely:** `exercise_library`. **Megőrzési ID-k:** A11, A12, A13, A14.

**Meglévő forrás:** [library](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L115) · [favorites](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryViewModel.kt#L95) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)

**WebView folytonosság és kép:** A WebView keresője, izomcsoportjai, Kedvencek és saját gyakorlatok maradnak. A mockup kereső/chip/listakártya felépítése javítja az áttekinthetőséget; a106 szám valós aktuális state.rows méretéből jön.

**Felépítés, felülről lefelé:** Kereső → Összes/Kedvencek és izomcsoport-szűrők tördelődő sor → találatszám → teljes lista → saját gyakorlat hozzáadása. Soronként név, főizom, felszerelés és kedvenc; stock anatómiai fotó csak valódi meglévő assetből.

**Elsődleges / másodlagos műveletek:** Fő: selectedId megnyitja a meglévő részletet. Másodlagos: toggleFavorite, saveCustom/deleteCustom editor. A sor szív/csillag önálló48dp cél, nem a kártyanyitás mellékhatása.

**Scroll / billentyűzet / Back:** Keresésnél IME ne takarja a találatlista első akcióját, kereső clear48dp. Back előbb saját editor, majd részlet, majd route. Query/favoritesOnly/muscle már rememberSaveable, megmarad.

**Üres / töltő / hiba / permission és session:** loading/error/reload külön; nincs találat a szűrő miatt nem üres katalógus. WriteFailed a kedvencsoron; nem animálunk sikeres mentést tartós hiba esetén.

**Nézetspecifikus elfogadás:** 320dp és hosszú DE/RO gyakorlatnevek: nincs horizontális elcsúszás; font2.0-n ikon helyett is marad szöveges név. TalkBack kedvenc be/ki állapot és listaindex. Saját elemek nem tűnnek el hardcoded106 limit miatt.

**Forrásból bizonyított nyitott kötés:** —

## V06. Gyakorlat részletei

**Meglévő hely:** `exercise_library / selectedId`. **Megőrzési ID-k:** A12, A13, A14, A15, B03.

**Meglévő forrás:** [librarydetail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryScreen.kt#L165) · [favorites](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ExerciseLibraryViewModel.kt#L95) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)

**WebView folytonosság és kép:** A main leírás, főizmok, súlyértelmezés és videóforrások megmaradnak. A mockup nagy demonstrációs panelje csak rendelkezésre álló tartalmat használ; nem generálunk új, ellenőrzöttnek látszó bemutatót.

**Felépítés, felülről lefelé:** Cím + kedvenc → valódi leírás és felszerelés → célizom/adattípus/előírás → videó vagy külön jelölt keresőhivatkozás → saját elem szerkesztése. Ha nincs asset, leírás és típusszimbólum helyettesíti a dekorációt.

**Elsődleges / másodlagos műveletek:** Fő: NativeExerciseDemo.resolve szerinti VIDEO vagy UNVERIFIED_SEARCH open(link). Másodlagos: toggleFavorite; custom edit. Aktív edzéshez hozzáadás az editor meglévő exercise pickeréből működik.

**Scroll / billentyűzet / Back:** Vertikális részletscroll; Back a kereső listához tér, megőrzött szűréssel. Külső videóból visszatérve nincs új session vagy programmutáció. IME csak custom editorban.

**Üres / töltő / hiba / permission és session:** Videóhiány és linknyitási hiba saját szöveg, nem néma ikon. Nem hitelesített keresés világosan különbözik a demonstrációtól.

**Nézetspecifikus elfogadás:** TalkBack a videó/keresés jellegét és a kedvenc állapotát mondja. Hosszú cím/nagy font mellett a bemutató nem tolja el az első valódi műveletet. Súly/kéz vs összsúly jelentés látható.

**Forrásból bizonyított nyitott kötés:** G03: ebben a screen-signature-ben nincs onAddToWorkout; a mockup Hozzáadás gombját nem lehet új writer/callback nélkül ide bekötni. A meglévő aktív picker út marad, Root dönt az extra entryről.

## V07. Súlyzós aktív edzés

**Meglévő hely:** `active_workout / reps`. **Megőrzési ID-k:** B01, B02, B03, B04, B07, B09, B10, B15, B18.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96) · [setcard](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L717) · [rest](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1078) · [leave](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L683)

**WebView folytonosság és kép:** A megszokott kg/ism/RIR sor és egyértelmű sorozat-OK, undo, ±15 és Kihagyás marad. A mockup egy kiválasztott gyakorlatra koncentrál, nem több teljes kártyára.

**Felépítés, felülről lefelé:** Session/owner állapot → előző/cím/következő → előírás → sorozatrács → visszavonás → opcionális részletek. A pihenőkártya a gyakorlatlistán kívül, a mért alsó vezérlőterületen marad. Befejezés és kamera/galéria külön egyértelmű akció.

**Elsődleges / másodlagos műveletek:** Fő: toggleSetCompleted(renderedTarget). Másodlagos: editSet/setSetRir/toggleSetRirMode, add/removeSet, selectExercise, undoLastSet; rest adjustRestTimer/skipRestTimer. Minden cél workoutId/generation/exerciseId/setId/timerKey a renderelt snapshotból.

**Scroll / billentyűzet / Back:** IME alatt a fókuszos cella és OK elérhető; numerikus megjelenítés nem alakítja át a validátort. Kis szélesség/font2.0: egy sorozat kétsoros blokk (felső set/kg/ism, alsó RIR/OK), nincs tömörítés8sp-re. Back a meglévő leaveWorkout pause út; Settings külön, nem pause.

**Üres / töltő / hiba / permission és session:** Watch-owner csak olvasható tükröt kap; pending ownership/finalization nem oldható fel új gombbal. Timerérték a state-ből, nincs Compose tick motor.

**Nézetspecifikus elfogadás:** Valódi pihenő futása alatt gyakorlatváltáskor marad kijelzés/azonos timerKey. Régi target nem ír. DE/RO labels és TalkBack Sorozat2,16kg,10ism, RIR2, kész/nem kész. Undo és ±15/Skip48dp.

**Forrásból bizonyított nyitott kötés:** G04: a RestTimerFloatingBar csak onAdjust/onSkip paramétert kap; a mockup külön rest-pause vezérlőjét nem szabad a teljes workout pause-nak álcázni.

## V08. Egyoldali időalapú edzés

**Meglévő hely:** `active_workout / timed unilateral`. **Megőrzési ID-k:** B02, B03, B05, B06, B08, B09.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [timer](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L921) · [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A main eredeti alsó sorozatstopperje marad, és az explicit pozitív stop auto-rögzít. A mockup nagy olvasható ideje, státuszcímkéje és csukott Intervallumok sora kerül a valódi stopper köré.

**Felépítés, felülről lefelé:** Gyakorlatváltó → sorozatlista (kész/fut/még nem) → aktuális alsó stopper → Stop/Start és külön Nullázás → csukott intervallum → közös finish/rest terület. Intervallum futásakor nincs duplikált alsó stopper.

**Elsődleges / másodlagos műveletek:** Fő: toggleSetStopwatch(target, side). Másodlagos: resetSetStopwatch, selectExercise; IntervalOptions onSave/onStart/onStop/onSkip a meglévő célokkal.

**Scroll / billentyűzet / Back:** Stop/Start a szám közelében; Nullázás nem piros stopnak álcázott. Számok tabulárisak. Vertikális scroll, nincs billentyűzet a futó méréshez; kézi adat csak lenyitott cellában. Back pause, Settings folyamatos mérés.

**Üres / töltő / hiba / permission és session:** 0mp/reset/implicit pause nem teljesítés. Futó/nincs futó/szüneteltetett/ready/complete külön szöveg+ikon, nem csak progresscsík.

**Nézetspecifikus elfogadás:** 1–59mp és60+mp fixture, explicit pozitív stop után ugyanaz a sorozat egyszer kész. Nagy font/DE a stop és reset nem lóg össze; TalkBack nem szólal meg250ms-onként, állapotváltást jelez.

**Forrásból bizonyított nyitott kötés:** —

## V09. Kétoldali időmérés

**Meglévő hely:** `active_workout / timed bilateral`. **Megőrzési ID-k:** B03, B05, B06, B08.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [timer](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L921) · [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838)

**WebView folytonosság és kép:** A main külön bal/jobb pozitív időfeltétele megmarad. A két mockup-oszlop ugyanazt a meglévő stopper-state-et jeleníti meg, nem két párhuzamos Compose-motort.

**Felépítés, felülről lefelé:** Gyakorlat → sorozatcél → Bal / Jobb külön panel → teljesítési feltétel → csukott pihenő/intervallum. Széles nézeten két oszlop; kis/nagyfonton egymás alatti azonos sorrendű panelek.

**Elsődleges / másodlagos műveletek:** Fő: toggleSetStopwatch(target, "left"/"right"); Másodlagos: resetSetStopwatch a meglévő szerződéssel, gyakorlatváltó, pihenő. Automatikus intervallumléptetés kétoldalinál nem kínálható új funkcióként.

**Scroll / billentyűzet / Back:** Oldalcímke mindig az idő előtt olvasható, oldalváltás nem helycserés UI-trükk. Reset megerősítése csak ha a meglévő út kéri; nem vezetünk be második állapotot. Back/Settings mint V08.

**Üres / töltő / hiba / permission és session:** Csak bal pozitív, jobb0: nem kész. Mindkettő pozitív: meglévő auto-rögzítés. Watch mirror/read-only ugyanazt mutatja más inputengedéllyel.

**Nézetspecifikus elfogadás:** 2.0 font és hosszú oldalcím: vertikális fallback. TalkBack oldal + idő + mérési állapot + művelet. Oldal/gyakorlatváltás és visszatérés nem nulláz rögzített értéket.

**Forrásból bizonyított nyitott kötés:** —

## V10. Kültéri/GPS mérés

**Meglévő hely:** `active_workout / distance`. **Megőrzési ID-k:** B11, B12, B13, B14, E18.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [distance](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L808) · [gpspermission](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt#L48) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A bevált Mérés befejezése, kézi idő/táv és kifejezett GPS-döntés megmarad. A mockup GPS-státuszsáv és2×2 metrikarács szervezése a cél; nincs például148bpm alapérték.

**Felépítés, felülről lefelé:** Aktivitás/owner → GPS-választás/engedélyállapot → táv+idő hangsúly → fix/pontosság és valós pulzus külön → útvonal megnyitás → manuális sorozatmezők → explicit mérés-start/finish. Élő háttértérkép dekoráció nélkül.

**Elsődleges / másodlagos műveletek:** Fő: requestGps(renderedTarget, expectedMeasurementId) → meglévő permission launcher/toggleGpsTracking. Másodlagos: openRouteMap(routeKey), ugyanazon editSet manuális mezők. Permission-settings visszatérés nem hív automatikus GPS-writert.

**Scroll / billentyűzet / Back:** Mérés befejezése nem ugyanaz az Edzés befejezése. A map-close csak modal. IME-nél manuális adatok szerkeszthetők és olvasható a mért vs kézi forrás; futó mérés readonly szabályai maradnak.

**Üres / töltő / hiba / permission és session:** Pontos/hozzávetőleges/megtagadott, központi hely OFF, vár fixre, fixvesztés, hibás/partial route külön. Pulzusforrás nélkül —bpm. Engedélyhiány mellett a workout nem áll le és nincs kitalált pont.

**Nézetspecifikus elfogadás:** Kis nézeten a táv/idő és explicit stop minimum48dp. TalkBack fixállapot és forrás, élő érték nincs zajos folyamatos bejelentéssel. Pozitív explicit GPS-stop külön sorozat-OK nélkül kész; mapX/back/hiba nem.

**Forrásból bizonyított nyitott kötés:** G05: DistanceMeasurementPanel jelenleg nem rendereli liveHeartRateBpm és gpsAccuracyMeters mezőt, noha a state tartalmazza. Bemutatásuk vizuális kötés, új pulzus/pace számítás nem. G06: nincs alaptérkép-provider.

## V11. Intervallum beállítása

**Meglévő hely:** `active_workout / IntervalOptions; settings timer`. **Megőrzési ID-k:** B08, B09, E17.

**Meglévő forrás:** [interval](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L838) · [timerdefaults](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeTimerDefaultsDialog.kt#L35) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A munka/pihenő/kör, hang/rezgés és explicit automatikus léptetés a mainből megmarad. A mockup rendezett beállítássorai a csukott, opcionális aktív panel lenyitásában kapnak helyet.

**Felépítés, felülről lefelé:** Először aktuális mentett preset összefoglaló → lenyitva munka/pihenő/kör → hang/rezgés → autoAdvance (csak támogatott egyoldali mód) → csak erre/mentés alapnak → Mentés → Indítás. Globális timerdialog ugyanazon mezőhierarchia.

**Elsődleges / másodlagos műveletek:** Fő: configureIntervals / IntervalOptions onSave, majd startIntervals. Másodlagos: stopIntervals és skipRestTimer a meglévő célhoz; Settingsben beginTimerDefaults/saveTimerDefaults. A csukás nem mentési gomb.

**Scroll / billentyűzet / Back:** Számbillentyűzet, IME-padding, Next mezősorrend. Módosított és mentett preset külön; start csak az eddig is engedett feltételekkel. Back csak a panel/editor állapotot kezeli, nem állít le rejtetten szolgáltatást.

**Üres / töltő / hiba / permission és session:** Invalid vagy24h feletti konfiguráció a meglévő validátoron marad. intervalSaving/writeFailed látható, nincs hamis Saved. Kétoldali módban nincs új egyoldali automata ígéret.

**Nézetspecifikus elfogadás:** Kis/nagyfont: mezők oszlopba, plusz/mínusz48dp. TalkBack érték+egység és checkboxállapot. Csak a megadott scope változik; más aktív frame/global config nem íródik felül stale mentésből.

**Forrásból bizonyított nyitott kötés:** —

## V12. Edzés befejezése

**Meglévő hely:** `active_workout / showFinishModal`. **Megőrzési ID-k:** B16, B17, B10.

**Meglévő forrás:** [finish](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L1128) · [finishwriter](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L1005) · [leave](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L683) · [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105)

**WebView folytonosság és kép:** A hat effort-állapot, fájdalom és külön pause/discard megmarad. A mockup „Mentésre vár” hierarchiája a cél: az effort választás önmagában ne tűnjön végleges mentésnek.

**Felépítés, felülről lefelé:** Valódi összegzés → hat értékelés mint választható mező → megjegyzés, ha lesz meglévő writable state → elsődleges Mentés → Szüneteltetés és kilépés → elkülönített Elvetés. Nincs nyerttrófea/mentve állítás a writer sikere előtt; kalória csak valós forrásból.

**Elsődleges / másodlagos műveletek:** Jelenlegi fő út: FinishWorkoutDialog.onConfirm(effort) → closeFinishModal → finishWorkout(effort,onSaved). Pause: leaveWorkout. Discard: openDiscardModal/discardWorkout. Tervben a kiválasztás vizuális draft, a valódi finish csak explicit Mentésre hívható; note callback jelenleg nincs.

**Scroll / billentyűzet / Back:** Back/Mégse bezárja az összegzést, nem ment; siker előtt nincs navigate. Nagy font/IME esetén külön görgethető form és hozzáférhető mentés. Finish/restore/owner lockot a UI nem oldja fel.

**Üres / töltő / hiba / permission és session:** Íráshiba vagy finalize/discard hiba megtartandó szerkesztési értékekkel, külön retry. Nincs „mentve” nyugta részleges natív lezárásnál.

**Nézetspecifikus elfogadás:** 2.0font, DE/RO: hat válasz legalább48dp, nem42dp magas buttonok. TalkBack kiválasztott effort külön a Mentéstől. Pause nem hoz naplósort vagy naptári teljesítést; discard külön confirmation.

**Forrásból bizonyított nyitott kötés:** G07: a jelenlegi modal azonnal ment és nincs megjegyzés vagy összegző input contract. A note/fájdalom részletes writer Root-funkciócsomag; nem új ViewModel API dizájnindokból. A phase2 nem nevezheti késznek ezt a teljes képet e hiány rendezése nélkül.

## V13. Napló

**Meglévő hely:** `journal`. **Megőrzési ID-k:** C01, C07.

**Meglévő forrás:** [journal](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L55) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65)

**WebView folytonosság és kép:** A main kereső, hónap/időszak és PR-jelzés marad; a mockup lapozott, jól szkennelhető kártyáihoz illesztjük. Nem csak a képernyőre már betöltött lapban keresünk.

**Felépítés, felülről lefelé:** Kereső → hónap/időszak → szűrő összefoglaló → dátumos workoutkártyák (név, teljesítés, idő, sorozat/PR) → append loading/retry. Útvonalthumbnail csak ténylegesen betöltött meglévő adatból, nincs listaszintű route-hydrate.

**Elsődleges / másodlagos műveletek:** Fő: openDetails(row.id). Másodlagos: setSearch/setMonth, Paging retry. Az ID stabil; a kártya nem tölt részletet vagy fotófájlt csak a díszítésért.

**Scroll / billentyűzet / Back:** LazyPagingItems stabil kulcs; query/month és scroll vissza részletből. IME keresőhöz, Back előbb szerkesztő/részlet réteg, majd főoldal. Részletdialog nem hagy két aktív focus-rootot.

**Üres / töltő / hiba / permission és session:** Üres napló / nincs keresési találat / appenderror külön. PR hiány nem „0PR rekord”. Betöltő kártyákhoz nincs kamu edzésnév vagy mérés.

**Nézetspecifikus elfogadás:** Kis kijelzőn legalább két áttekinthető rekord, nagy betűn egy is elfogadható, minden művelet elérhető. DE/RO hosszú név wrap. TalkBack dátum → cím → státusz/PR → megnyitás; mini térkép dekoráció csak igazolt meglévő geometryből.

**Forrásból bizonyított nyitott kötés:** G06: listában nincs alaptérkép/route geometry betöltés; a látványterv várostérképes thumbja nem kész funkció. Ne hozzunk létre N+1 route-queryt a dizájnért.

## V14. Naplózott edzés részletei

**Meglévő hely:** `journal / selectedWorkoutId modal`. **Megőrzési ID-k:** C02, C03, C04, C05, C07.

**Meglévő forrás:** [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65) · [journaledit](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalEditorDialog.kt#L33) · [sources](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalHealthPanel.kt#L23) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257)

**WebView folytonosság és kép:** A main részletes sorozatok, Statisztikák/Forrás és utólagos szerkesztés marad. A mockup három füle, időtartam-hierarchiája és külön fotó/útvonal elérése a cél.

**Felépítés, felülről lefelé:** Edzésazonosító, dátum/név → időmutatók → scrollozható fülek Gyakorlatok/Statisztikák/Forrás → teljes gyakorlat/sorozatlista → fotó/útvonal → Szerkesztés/Calendar intent → megerősített törlés.

**Elsődleges / másodlagos műveletek:** Fő: openEditor / szerkesztő save callback. Másodlagos: openRouteMap, refreshWorkoutHealth(activity), createCalendarIntent, requestDeleteWorkout/confirmDeleteWorkout; privát fotó modal. A két health source külön jelenik meg.

**Scroll / billentyűzet / Back:** Full-screen dialog jelenlegi close/back szerződése megmarad, a közös gear hozzáférhetőségét a dialogon belüli guardolt nav-hook nélkül nem ígérjük. Tab/scroll workout-ID-hez kötött. IME csak editorban, cancel nem ír.

**Üres / töltő / hiba / permission és session:** Loading/missing/detailError retry; photosMissing és health partial saját állapot. Delete vagy mentés közben close/back csak meglévő engedéllyel. Az alkalmazott feedback nem válik módosíthatóvá új chip miatt.

**Nézetspecifikus elfogadás:** 2.0font hosszú edzésnév/tabs nem fedik a listát. TalkBack sorozatonként súly/ism/idő/RIR és forrás. Ugyanaz az edzés-ID marad edit után. Fotó törlés és workout törlés külön jelentésű.

**Forrásból bizonyított nyitott kötés:** G08 megengedett modal-kivétel: a JournalDetailDialog külön Window; a NATIVE_MOCKUP_REVIEW szerint beágyazott dialógusra nem szükséges új fogaskerék. Nem kell új nav-hookot vagy hatodik fület bevezetni emiatt.

## V15. Privát útvonaltérkép

**Meglévő hely:** `WorkoutRouteMapDialog modal`. **Megőrzési ID-k:** B13, C06.

**Meglévő forrás:** [map](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L48) · [mapcanvas](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/WorkoutRouteMapDialog.kt#L79) · [journalvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalViewModel.kt#L65)

**WebView folytonosság és kép:** A bevált piros X és a privát útvonalszakaszok maradnak. A mockup teljes képernyős útvonal és alsó adatpanel kompozíciója a cél, nem a generált Budapest háttérképe.

**Felépítés, felülről lefelé:** Cím + piros veszélytokennel megkülönböztetett X → route point/partial állapot → nagy saját geometry canvas → start/path/finish legenda. Csak létező összesített adat lehet alsó panelen. Nincs fogaskerék vagy bottom nav a térképmodalban.

**Elsődleges / másodlagos műveletek:** Fő/másodlagos: onDismiss (X, rendszer-Back), minden aktív/journal belépő ugyanazt a canvasadatot adja. Nincs stop/finish writer a close útján.

**Scroll / billentyűzet / Back:** Window teljes méret, rendszer insetek, X48dp. Route bounding box elfér, szakaszok nincsenek összekötve hiányzó pontokon. Nincs IME; Back csak ezt a modalt zárja.

**Üres / töltő / hiba / permission és session:** Offline saját route továbbra rajzolható; hiányzó rész explicit jelzés. Alaptérkép hiánya nem routehiba és nincs új offline tile-ígéret.

**Nézetspecifikus elfogadás:** TalkBack route szakasz/pont összefoglaló és X címke. Nagy font mellett a címke/legenda nem fedheti a close-t. X után GPS/stopper/session állapota változatlan.

**Forrásból bizonyított nyitott kötés:** G06: MercatorRouteCanvas saját Path-rajz; nincs tile provider, zoom/recenter callback vagy térképcache. Ezeket a mockupból nem szabad működőként átvenni.

## V16. Fejlődés

**Meglévő hely:** `progress; weight_journal közös elérés`. **Megőrzési ID-k:** C07, C08, C09, C10.

**Meglévő forrás:** [progress](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt#L61) · [progressstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt#L40) · [cardio](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/NativeCardioProgressPanel.kt#L35) · [weight](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt#L48) · [weightvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalViewModel.kt#L40)

**WebView folytonosság és kép:** A main gyakorlatválasztó, 1RM/volumen, izomterhelés, kardió-időszakok és testsúly megmarad. A mockup belső fülei az információkat csoportosítják, nem tüntetik el az összetett régi statisztikát.

**Felépítés, felülről lefelé:** Áttekintő cím → Erőnlét/Mennyiség/Kardió/Testsúly helyi panelek → az adott meglévő queryhez tartozó választók → valódi grafikon + táblás összefoglaló → testsúlynapló gyorselérés. A meglévő state-ben nem létező időszakszűrőhöz nem rajzolunk placebo chipet.

**Elsődleges / másodlagos műveletek:** Fő: selectExercise, selectCardioActivity, onWeightJournal. Másodlagos: retryCardio/loadProgressData, WeightJournalViewModel.setRange/retryReads. Mérésaggregátumot a UI nem számol újra.

**Scroll / billentyűzet / Back:** Azonos selection/scroll/panel visszaállítás; nagy grafikonhoz nincs vízszintes oldalhúzás. IME csak testsúly editorban. Back a közös weight naplóból ugyanide.

**Üres / töltő / hiba / permission és session:** Loading és cardioError/trenderror külön a panelekben; nincs minta e1RM/history pont. A napi/zóna-váltás meglévő refresh collectora megmarad.

**Nézetspecifikus elfogadás:** TalkBack grafikonalternatíva mértérték/lista és mintaösszeg, nem puszta canvas. Kis/nagyfont: kártyák egy oszlop, hosszú DE/RO periódus felirat wrap. Same-count history edit frissítése megmarad.

**Forrásból bizonyított nyitott kötés:** G10: progressPhotos listát betölt a VM, de a ProgressScreen nem renderel fotógalériát vagy callbacket. A mockup Testsúly/Fotó gyorseléréshez Root dönt a hiányzó közös fotó-entryről; a strength időszakszűrés sem található a publikus state-ben.

## V17. Egészség és részletes napló

**Meglévő hely:** `health; HealthRecordBrowser; weight_journal`. **Megőrzési ID-k:** D01, D02, D03, D04, D05, D06, D07, D08, D09, D10, D11, D12, D13, D14, E09, E10.

**Meglévő forrás:** [health](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) · [healthvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L89) · [metrics](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/NativeHealthMetrics.kt#L10) · [records](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthRecordBrowser.kt#L63) · [weight](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/progress/WeightJournalScreen.kt#L48)

**WebView folytonosság és kép:** A main teljes naplója, forrásai, 26 metrikakulcsa és alvásrészletei megmaradnak. A mockup hat csempéje áttekintő hierarchia, nem metrikalimit. Lépés/aktív/összes energia nem összeadás.

**Felépítés, felülről lefelé:** Szolgáltató/lastSync/partial → normál/full frissítés és progress/cancel/retry → választott hónap/nap/csatorna → fontos metrikák → teljes metrikacsoportok → alvás/rekord részletek → testsúly → Eszközök és adatforrásbeállítások. Statisztika és permission külön.

**Elsődleges / másodlagos műveletek:** Fő: syncHealth, record detail megnyitás és HealthRecordBrowser range/source/paging. Másodlagos: setProvider, setDailySourcePreference, setAutoOnOpen, toggleHcBackground/toggleBleBackground, onWeightJournal; kézi Samsung consent megmarad.

**Scroll / billentyűzet / Back:** Metrikarács2oszlop alapból,1oszlop nagyfontnál. A részletbrowser scroll/query megmarad bezárás után; új sync nem állítja át a kiválasztott napot. Dátum natív picker, Back a browserből azonosnapra.

**Üres / töltő / hiba / permission és session:** Engedélyhiány/providerhiány/partial/syncerror/üresnap külön, no data=—. Jövőbeli/régi adat nem élő. HC/Samsung/BLE külön forrás és időbélyeg; a timestamp nélküli forrásbadge nem lehet „friss”.

**Nézetspecifikus elfogadás:** Kis nézet és DE/RO: minden metrikakulcs/részlet elérhető. TalkBack szám+egység+forrás+dátum; progress oldalszámból/napból jön. 30 nap/full/incremental és régi nap/source selection megmarad. Kézi testsúly update nem ír provider-recordot.

**Forrásból bizonyított nyitott kötés:** —

## V18. Coach

**Meglévő hely:** `coach`. **Megőrzési ID-k:** C11, C12, C13, C14, A09, A10.

**Meglévő forrás:** [coach](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/CoachScreen.kt#L23) · [coachvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/CoachViewModel.kt#L26) · [readiness](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachReadiness.kt#L14)

**WebView folytonosság és kép:** A main tanácsok, terhelés/fájdalom korlátozás és külön Samsung/saját/Coach érték marad. A mockup nagy readiness körét az egyetlen NativeCoachReadiness eredmény tölti ki.

**Felépítés, felülről lefelé:** Nagy Coach score + korlátozó ok → Samsung energia és TrainPilot becslés két névvel → frissesség/tényezők → javasolt programnap CTA → gyakorlatonkénti lenyitható tanács → megerősített variációelőnézet.

**Elsődleges / másodlagos műveletek:** Fő: meglévő onStart vagy upcoming esetben onCalendar; másodlagos: refreshHealth, previewVariation/saveVariation/closeVariation, onHealth/onProgress/onPrograms. Read-only tanács nem alkalmazott terv.

**Scroll / billentyűzet / Back:** Egy scroll, számpanel csak addig nagy, amíg az első ajánlás elérhető. Nagyfonton két pontszám vertikális. Back variációt zár/route-ra lép, nem fogad el programot.

**Üres / töltő / hiba / permission és session:** Nincs score: —, nem0. Elavult/hiányzó Samsung és fallback külön. Loading/error/reload megtartja az engedélyezett tanácsokat, nem jelzi késznek a félkész új readet.

**Nézetspecifikus elfogadás:** Azonos inputnál Home/Health recovery nem veheti át a terheléssel csökkentett Coach score-t. TalkBack három külön skála és indok, nem „87+61=54”. DE/RO a körtől jobbra szűk szöveg helyett alatta jelenik meg.

**Forrásból bizonyított nyitott kötés:** —

## V19. Beállítások

**Meglévő hely:** `settings`. **Megőrzési ID-k:** A02, C14, E11, E12, E13, E14, E15, E16, E17, E19, E20.

**Meglévő forrás:** [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [themepicker](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeThemePicker.kt#L31) · [lang](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeLanguagePicker.kt#L32) · [timerdefaults](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeTimerDefaultsDialog.kt#L35) · [google](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt#L26)

**WebView folytonosság és kép:** A main szekciók, 32paletta és négy nyelv marad; a mockup soros beállításkártyái a hosszú Material-listát tagolják. A user mentett témáját a grafit/arany bemutató kedvéért nem reseteljük.

**Felépítés, felülről lefelé:** Megjelenés (nyelv/témák) → Profil → Edzés/időzítő/haptika/mód → Kapcsolatok/Google → Mentés-visszaállítás → Adatvédelem/karbantartás. Szekciócímek és fogaskerék kontextus megmarad. Veszélyes műveletek nem elsődleges arany főgombok.

**Elsődleges / másodlagos műveletek:** Fő: meglévő editor Save callbackek. Másodlagos: setTheme/setLanguage, beginTimerDefaults/saveTimerDefaults, saveProfile, training-mode compare, connect/sync/disconnect, három Google maintenance. Nincs új verzióadat vagy backup-lista lekérés.

**Scroll / billentyűzet / Back:** IME profile/timerben a formnak foglal helyet. Back először megnyitott panel, majd eredeti route/session; Settingsbe lépés önmagában nem pause. Dialog input local draft nem kerül újra inicializálásra háttérrecompozíciótól.

**Üres / töltő / hiba / permission és session:** Versengő restore/sync/writer busy és hibák panelspecifikusak; új felület nem kerüli meg a guardot. Google confirmation account/scope/cancel megmarad, token nem UI.

**Nézetspecifikus elfogadás:** 32 palette választható és persistált, HU/EN/DE/RO az egész felületet frissíti.48dp switchsor, hosszú német label-wrap. TalkBack szekció és be/ki/mentési állapot. Aktív időmérés Settings roundtrip során folytatódik.

**Forrásból bizonyított nyitott kötés:** G09: globális Permissions/Watch külön route nincs; a mockup ezekre mutató sorait csak meglévő kontextusos entryvel vagy Root nav-bekötéssel lehet megvalósítani. Verziósort csak valós BuildConfigból, ha Root jóváhagyja a hiányzó kötést.

## V20. Óra és szinkron

**Meglévő hely:** `active_workout / ownership dialog + Home status`. **Megőrzési ID-k:** E05, E06, E07, E08.

**Meglévő forrás:** [workout](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L105) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96) · [ownership](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L805) · [home](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt#L62) · [nav](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt#L92)

**WebView folytonosság és kép:** A main kétirányú ownership/protokoll és offline saját folytatás marad. A mockup státuszkártyáit a meglévő session-owner felület köré tervezzük; nem „következő edzésnél” ígéretet.

**Felépítés, felülről lefelé:** Valódi kapcsolat → aktuális workout tulajdonos → pending/ack/blocked állapot → engedélyezett átadás/visszavétel → offline magyarázat. Akkumulátor csak valódi adatból, kapcsolat nem következik pusztán egy régi profilból.

**Elsődleges / másodlagos műveletek:** Fő: requestOwnershipBackFromWatch vagy startWatchMeasurementRemotely(activity), a jelenlegi UI permission/owner szabályaival. Másodlagos: meglévő ownership dialog zárás/megtekintés; Home csak státusz, nem hamis mutatós órafotó.

**Scroll / billentyűzet / Back:** Őrizze az aktív session nézetét és a bottom eredetfület. Back csak a panelt zárja. Nincs IME; ownership közben a globális navigation guard megmarad.

**Üres / töltő / hiba / permission és session:** Disconnected/ack-pending/phone-owner/watch-owner és recovery blokk külön szöveg. Reconnect nem indít automatikus átadást vagy két stoppert.

**Nézetspecifikus elfogadás:** TalkBack kimondja a tulajdonost és az átadás függő állapotát, a zöld pont nem elég. Kis kijelzőn a manuális átadás és a figyelmeztetés együtt látható. Offline→online nem lesz néma ownercsere.

**Forrásból bizonyított nyitott kötés:** G09: standalone watch_sync route és settings callback nincs; az aktuális aktív editorba szervezett panel megvalósítható. Teljes külön oldalhoz Root prezentációs nav-hook szükséges; Wear-kód nem módosítható.

## V21. Mentés és visszaállítás

**Meglévő hely:** `settings / SAF + recovery`. **Megőrzési ID-k:** E11, E12, E13, E14, E15.

**Meglévő forrás:** [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74) · [settingsvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L54) · [recovery](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/ReplacementRecoveryPanel.kt#L19) · [google](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt#L26)

**WebView folytonosság és kép:** A bevált JSON/ZIP/Drive és explicit egészségadat-beleegyezés megmarad. A mockup három formátumkártyáját a már meglévő SAF/Drive callbackekhez kötjük, kitalált mentéslista nélkül.

**Felépítés, felülről lefelé:** Formátum és tényleges tartalom → health consent → Export → Import külön blokk → interrupted recovery + Folytatás/alternatív JSON/ZIP → Google recoveryEarlier és maintenance külön veszélyzóna. Restore tiltásszöveg: aktív edzés KÖZBEN, nem ELŐTT.

**Elsődleges / másodlagos műveletek:** Fő: meglévő JSON/ZIP launcherek + onSaf*Completed; Google syncDriveCloud. Másodlagos: resumeReplacement/onJson/onZip recovery; explicit erasure sajátconfirm. SAF cancel nem hív import/export writert.

**Scroll / billentyűzet / Back:** Scrollos szekció Settingsen belül, nem új route. Rendszer fájlválasztóból Back ugyanoda és meglévő restore-fence mellett tér vissza. Nem adunk dummy fájllistát vagy kijelölt visszaállítás CTA-t hiányzó API-val.

**Üres / töltő / hiba / permission és session:** Hiba/részleges ZIP/restore pending explicit recovery. Műveleti siker csak repository eredményből. Busy alatt a felhasználó tudja, mely művelet fut; aktív session nem vész el.

**Nézetspecifikus elfogadás:** Kis/nagyfont és DE/RO: tartalommagyarázat nem sűrített apróbetű. TalkBack formátum+tartalom+scope és megerősítés. Import hiba után továbbra van retry/alternatíva, nem dekorációs zöld pipa.

**Forrásból bizonyított nyitott kötés:** G11: nincs mentések listázására/preview-lista választására state a SettingsUiState-ben. A mintabeli dátumok/méretek/sorok nem jeleníthetők meg valós funkcióként. Adatintegritási QA-ból jövő jövőbeli baseline-t Root adja.

## V22. Engedélyek

**Meglévő hely:** `kontextusos panelek: GPS/health/BLE/settings`. **Megőrzési ID-k:** E18, E09, E10, D09, B11.

**Meglévő forrás:** [gpspermission](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/NativeGpsPermissionRecovery.kt#L48) · [health](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt#L99) · [healthvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L89) · [settings](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt#L74)

**WebView folytonosság és kép:** A main kontextusos engedélykérés és rendszerlinkek maradnak. A mockup állapotsoros elrendezése átvehető a létező panelekben, nem új második permission-managerrel.

**Felépítés, felülről lefelé:** Funkcióhoz szükséges hozzáférés → rendszerhely külön sor → hiány oka → konkrét Android prompt/settings elérés → explicit visszatérés/újrapróba. Külön HC/Samsung/BLE/Wear és értesítési indokok.

**Elsődleges / másodlagos műveletek:** Fő: NativeGpsPermissionRecovery existing request/retry/settingsIntent; health permission launcherek; BLE háttér permission launcherek. Másodlagos: cancel/back és állapotfrissítés. ON_RESUME csak olvas, nem GPS-start.

**Scroll / billentyűzet / Back:** Rendszerablak visszatérési út megmarad; eredeti workout/generation/exercise/set requestid nem cserélődhet újra. Nincs IME. Back elveti a pending felhasználói szándékot, nem teljesít sorozatot.

**Üres / töltő / hiba / permission és session:** Approximate vs precise, permanently denied vs egyszer denied, location OFF, SDK unsupported és app-szintű Samsung korlát külön. Általános engedélylista nem állíthat minden platformtámogatást zöldre.

**Nézetspecifikus elfogadás:** TalkBack pontos engedély+státusz+következmény. Kis képernyőn a „rendszerhely” nem rövidül „GPS engedély”-re. Permissiongrant után sem startol háttérben GPS; explicit felhasználói retry kell.

**Forrásból bizonyított nyitott kötés:** G09: önálló permissions route és közös permission-list state nem létezik. A meglévő panelek vizuális rendezése készíthető; a kép szerinti közös oldal Root-adaptert igényel.

## V23. Privát edzésfotók

**Meglévő hely:** `active_workout attachedPhotos + journal photo modal`. **Megőrzési ID-k:** C05, C10, E13, E15.

**Meglévő forrás:** [activephotos](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt#L418) · [photo](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L257) · [detail](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/journal/JournalScreen.kt#L121) · [workoutstate](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt#L96)

**WebView folytonosság és kép:** A main kamera/galéria, privát tárolás és tudatos törlés marad. A mockup két képkártyája valódi mentett fotót jelenthet, nem súlyzós stock-képet.

**Felépítés, felülről lefelé:** Privát tartalom/backup consent magyarázat → meglévő fotók (szükség szerint lapozott decode) → camera/gallery aktív edzésben → full-view meglévő fotómodal → egyértelmű adott-ID törlés a támogatott kontextusban.

**Elsődleges / másodlagos műveletek:** Fő: meglévő cameraLauncher/galleryLauncher és onPhotoCaptured. Másodlagos: ActiveWorkoutViewModel.deletePhoto(photo.id), journal photo megnyitás/bezárás. Fotó készítése nem állít meg edzést.

**Scroll / billentyűzet / Back:** Scroll/thumbs és full-view elválasztva; Back csak kép/modal. Nagy kép dekódolása privát repositoryból, nincs teljes archívum hydrate. Törlés célja látható,48dp gomb.

**Üres / töltő / hiba / permission és session:** Üres, filemissing, decodeerror, restore-fence, consent-/feltöltési állapot a valós adatok szerint. Nem ígérjük, hogy „soha nem kerül felhőbe”, ha user ZIP/Drive fotómentést engedélyez.

**Nézetspecifikus elfogadás:** TalkBack fotó címke/dátum/kontextus, törlési scope. Nagyfontnál a grid1oszlopra áll. Thumbnail maxedge és aktív mérés melletti scroll nem foghatja az UI-threadet. Képbezárás nem finish.

**Forrásból bizonyított nyitott kötés:** G12: aktív attachedPhotos sorban jelenleg üres Box +24dp delete ikon, nincs bitmapmegjelenítés vagy fotó-view callback. Journal PrivateJournalPhoto működő megjelenítő. Standalone progress photos út hiány G10; új mutáció nincs dizájncsomagban.

## V24. Program szerkesztése

**Meglévő hely:** `programs / ProgramEditorDialog`. **Megőrzési ID-k:** A07, A08, A09, A14, C14.

**Meglévő forrás:** [programeditor](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramEditorDialog.kt#L72) · [programvm](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt#L138) · [rx](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/ProgramPrescriptionDialog.kt#L32) · [custom](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/programs/CustomExerciseDialog.kt#L57)

**WebView folytonosság és kép:** A main napok/gyakorlatok sorrendje, prescriptions és saját elemek védelme marad. A mockup fókuszált napfül + egy lenyitott gyakorlat + külön Mentés/Mégse jó vizuális minta.

**Felépítés, felülről lefelé:** Név → programnapválasztó → nap neve/sorrend → gyakorlatkártyák, kijelölt részletben sets/reps/load/rest és típusspecifikus mezők → hozzáadás → Mentés/Mégse → külön veszélyes törlés/visszaállítás.

**Elsődleges / másodlagos műveletek:** Fő: onSave(draft) → saveProgram(editing,draft,language,editorToken). Másodlagos: onCancel, prescriptions meglévő editor, sorrend/move és exercise chooser, delete/restore csak ahol eredeti szerződés engedi. Nincs új két szerkesztő ugyanazon draftnak.

**Scroll / billentyűzet / Back:** IME fókuszhoz bring-into-view, min48dp sorrendkezelők. A 3×gomb és input nem fér minden sorba: kis/nagyfonton címkézett vertikális mezőpárok. Back/Mégse nem ment, megnyitott editorToken/CAS érvényben marad.

**Üres / töltő / hiba / permission és session:** Saving/invalid/stale/writefailed a draftban marad, nem defaultokat tölt újra. A template-reset/copy működés meglévő domain-policyt követ, nem adunk új globális reset ígéretet.

**Nézetspecifikus elfogadás:** DE/RO hosszú gyakorlat + kg/kéz érték együtt olvasható. TalkBack sorrendindex és explicit Fel/Le fallback drag mellett. Mégse után DB változatlan; siker után ugyanazon program és megfelelő nap marad elérhető.

**Forrásból bizonyított nyitott kötés:** —
