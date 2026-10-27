# TrainPilot Natív Android — Végleges Vizuális Mesterterv (`main` + 4 Mockup Tábla Alapján) és Kódjavítási Csomag

Alapállapot:
- Utolsó teljesen zöld funkcionális + P0 alap: `31dbcde2` / `2f7cd835` (1333 JVM teszt PASS, 112/112 telefonos UI PASS, 3 Wear shard PASS, 2 signed upgrade PASS).
- Jelenlegi kísérleti P1 jelölt: `67f2889f` (CI run `40464028059` — 99 PASS, 3 FAIL, 12 MISSING / `Process crashed`).
- **Kötelező vizuális mérce:** A GitHub `main` ág 17 képernyőképén látható prémium arany-karbon felépítés (simított Bézier-grafikonok, helyben lenyitható részletező kártyák, anatómiai izomtérkép, 3×2-es ikonos állapotrács, meleg borostyán kiemelések, sötét szövegű arany CTA-gombok) kombinálva a 4 új natív mockup-tábla (`01`–`04`) natív navigációjával és kártyakompozíciójával.

---

## I. RÉSZ — A PIROS TELEFONOS UI-KAPU (`67f2889f`) 3 HIBÁJÁNAK ÉS A RUNNER-CRASHNEK A JAVÍTÁSA

1. **Hiba #1 (`NativeLocalizationLanguageUiTest.trainingCalendarAndCoachFollowChangedAppLanguageOnRealNavigation`):**
   - **Ok:** A `shortcut_calendar` és `shortcut_coach` gombok a P1-es `HomeScreen`-en a látható képernyő alá csúsztak, a teszt pedig `.performScrollTo()` nélkül kattintott rájuk. Mivel a főoldali gomb felirata románul (`"Calendar"`) és németül (`"Kalender"`) azonos a célképernyő címével, az `awaitText` false-positive találatot adott a főoldalon maradva, majd a `navigation_back` elbukott.
   - **Javítás:** `.performScrollTo().performClick()` + `compose.waitUntil(15_000) { nodesWithTag("navigation_back") }` a tesztben; a `HomeScreen.kt`-ban pedig a `shortcut_*` gombok `isLoading` alatt is a Compose-fában maradnak.
2. **Hiba #2 (`NativePhoneNavigationTest.coachVariationPreviewCancelsWithoutWritingAndSavesOnlyAnExplicitCopy`, 1276. sor):**
   - **Ok:** A `NativeHomeRepository.loadTodayMetrics` minden Home-állapotfrissítéskor meghívta a teljes `NativeCoachEngine.load(context, db)` motort a háttérben, lefoglalva a Roomot és a Coach-motort, miközben a `HomeScreen` figyelmen kívül hagyta a `uiState.recovery` (`NativeHomeRecoveryPresentation`) hiteles modellt.
   - **Javítás:** A `NativeCoachEngine.load` hívást töröljük a `NativeHomeRepository.loadTodayMetrics`-ből; a `HomeViewModel` a már feloldott `NativeHomeRecoveryPresentation.resolve(appContext)` modellt adja át a `loadTodayMetrics`-nek.
3. **Hiba #3 (`NativeTrainingModeApprovalUiTest.uiApprovalRejectsProgramRawChangeAfterShownComparison` + 12 MISSING teszt / `Process crashed`):**
   - **Ok:** A `TrainingProgramsScreen.kt` elutasításkor bezárja az első `AlertDialog` subcompositiont és megnyit egy másodikat (`state.modeStatus`). Az `awaitRejected()` után azonnal lefutó `scenario?.close()` félúton kapta el a két `AlertDialog` subcomposition váltását, ami `SlotWriter` `ArrayIndexOutOfBoundsException`-t okozott.
   - **Javítás:** `instrumentation.waitForIdleSync()` közvetlenül a `scenario?.close()` **előtt** a `NativeLocalizationEmulatorFixture.kt`-ban + `compose.waitForIdle()` az `awaitRejected()` végén.

---

## II. RÉSZ — GLOBÁLIS VIZUÁLIS DNS (`main` + MOCKUPOK KÖTELEZŐ SZABÁLYAI)

1. **Gomb-kontraszt szabály (`onAccent` — TILOS a fehér szöveg sárga/arany gombon!):**
   - Világos és meleg akcentusszíneken (különösen az Arany/Sárga `#EAB308` / `#F2B93B` és világos neon palettákon, ahol `colors.accent.luminance() > 0.40f`) minden elsődleges CTA gombon (`Indítás`, `Folytatás`, `Mentés`, `Aktiválás`, `Drive-szinkron most`, `Terv frissítése`) és aktív időszak-gombon (`3 hó`, `3m`, `HU`) a szöveg és ikon színe **kötelezően mélysötét (`#0E1116`)**, soha nem `Color.White`!
2. **Kártya-keretek, meleg borostyán kiemelések és rétegzettség:**
   - **Fő szekciókonténer (`TrainPilotHeroSurface` / kiemelt csoport):** `#11151C` háttér, `22dp` rádiusz, finom arany-bronz keret (`colors.accent.copy(alpha = 0.28f)`).
   - **Aktív / kiválasztott / következő kártya (pl. következő `A nap`, lenyitott metrikakártya, aktív program, kiválasztott naptári nap):** `1.5dp` vastag arany keret (`colors.accent.copy(alpha = 0.78f)`) + meleg sötét-borostyán belső tónus (`colors.accentSoft`).
   - **Belső metrika-csempe (`surface2` / `cardElevated`):** `#161B24` háttér, `14–16dp` rádiusz, `1dp` `#252D3B` (`borderSubtle`) keret.
   - **Meleg borostyán tanácsadó doboz (`MAI JAVASLAT` a Kezdőlapon és a Coach-ban):** `colors.accentSoft` (`#2A2212`) háttér, `1dp` `colors.accent.copy(alpha = 0.45f)` keret, arany `MAI JAVASLAT` felirat (`12sp Bold`), fehér főcím (`20sp Bold`) és másodlagos leírás.
3. **Kötelező ikon-badge-ek és grafikonok:**
   - **Tilos az ikon nélküli, csupasz szövegdoboz!** Minden állapotcsempén (`Alvás`, `Aktív kalória`, `Pulzus`, `Lépések`, `Mai edzés`, `Regeneráció`) kötelező az akcentusszínű vektoros ikon, a `Fejlődés`, `Egészség` és `Beállítások` kártyáin pedig a `36×36dp`-s lekerekített `accentSoft` ikon-badge.
   - Kötelező vizuális elemek: `Canvas` alapú **simított arany Bézier-görbék** (`Pulzustrend`, `Testsúlynapló`), **interaktív arany oszlopdiagram** (`Volumen trend`), **anatómiai izomcsoport-terhelés térkép** (`Fejlődés`), és **kör-ív (circular arc gauge)** (`Coach` készenlét, `Pihenőidő` stopper).

---

## III. RÉSZ — KÉPERNYŐNKKÉNTI VÉGLEGES TERV (`V01`–`V24` / `P1`–`P6`)

### P1 Csomag — Az 5 Főoldal (`V01`–`V05`)

1. **`V01. Kezdőlap` (`HomeScreen.kt` — `main` `11-24-03` + Mockup `01` #1):**
   - Feltételes kártyák: `Személyes edzéstervező` (`home_onboarding_resume`, arany `Profil és új terv` gomb sötét szöveggel) és `Mentett edzéspiszkozat` (`home_resume_draft`).
   - **Aktív program & Következő edzés Hero (`TodayHeroCard`):** Arany-bronz keretes kártya; bal oldalon `Aktív program` badge + `home_program_name` (`20sp Bold`); jobb oldalon (függőleges elválasztóval) `Következő` + `home_day_name` (`16sp SemiBold`) + gyakorlatszám; alul teljes szélességű `52dp` arany CTA gomb sötét szöveggel (`home_start_workout`).
   - **Gyorsindító sor (`QuickStartRow`):** 2 oszlopos `IntrinsicSize.Max` kártyapár (`home_quick_workout`, `home_outdoor_workout`) `36×36dp` arany ikon-badge-dzsel.
   - **`Mai állapot` 6-os ikon-rács (`TodayMetricsGrid`):** 3×2-es `IntrinsicSize.Max` rács arany ikonokkal, `tabular-nums` értékekkel és folyamatjelző sávval (`home_metric_card_*` + `home_recovery_card` a hiteles `NativeHomeRecoveryPresentation` alapján).
   - **`MAI JAVASLAT` Coach előnézeti kártya + `Tervezés és edző` (`NativePlanningShortcuts`):** Meleg borostyán `MAI JAVASLAT` kártya (`Mai készenlét X/100`), alatta a 3 tervező kártya (`shortcut_calendar`, `shortcut_coach`, `shortcut_programs`).

2. **`V02. Edzés főoldal` (`TrainingLandingScreen.kt` — `main` `11-24-07` + Mockup `01` #2):**
   - Felül kiemelt `Gyors edzés` kártya (`training_quick_workout`) + 2 oszlopban `Gyakorlatok / Izomcsoportok` (`training_open_exercises`) és `Kültéri GPS` (`training_outdoor_workout`).
   - **Aktuális program szekció:** `AKTÍV PROGRAM` badge + programnév + `Másik program választása >` (`training_open_programs`).
   - **Programnapok (`A nap` | `B nap` — `training_day_${day.id}`):** 2 oszlopos (`IntrinsicSize.Max`) kártyapárban (vagy sorokban), ahol a soron következő nap **`1.5dp` arany keretet**, arany keretes `A` betű/szám badge-et és **telített arany `▶ Indítás` gombot (sötét szöveggel)** kap (`training_start_day_${day.id}`), a többi nap pedig szekunder `cardElevated` gombot.

3. **`V03. Napló` (`WorkoutHistoryScreen.kt` — `main` `11-24-54` + Mockup `03` #1):**
   - Keresőmező (`history_search_input`), `Szűrő dátum szerint / típus-chipek` (`history_filter_row`), `X / Y edzés` számláló.
   - Strukturált edzéskártyák (`history_item_${workout.id}`): bal oldalon típus-ikon badge + edzés címe (`16sp Bold`) + dátum/idő (`tabular-nums`) + gyakorlat-előnézet; jobb oldalon pill-badge (`6 gyakorlat • 1 fotó` vagy kültéri mini-útvonal) + arany `>` chevron.

4. **`V04. Egészség` (`HealthDashboardScreen.kt` — `main` `11-24-11`..`11-24-43` TELJES FELÉPÍTÉSE!):**
   - **Fejléc kártya:** Arany szív ikon-badge + `Mai állapot` + kiválasztott dátum (`health_selected_day`) + léptetők (`health_day_prev`, `health_day_next`, `health_day_today`).
   - **3×2-es `Mai állapot` ikon-rács:** `Alvás` (🌙 `6,2 h`), `Aktív kalória` (🔥 `164 kcal`), `Pulzus` (♡ `65 bpm`), `Lépések` (👟 `3916`), `Mai edzés` (🏋️ `0`), `Regeneráció` (🌿 `84/100`).
   - **Lenyitható `Pulzustrend` kártya:** Zárt állapotban jobb oldali arany Bézier-sparkline-nal; lenyitva `Mai pulzus` 2×2-es rács (`Átlagpulzus`, `Nyugalmi pulzus`, `Max. pulzus`, `Min. pulzus`), `7 napos trend` nagy simított arany Bézier-görbével és 7 napi érték-csempével (`Sze..K`), valamint `Regenerációs előzmények >`.
   - **Lenyitható `Testsúlynapló` kártya (D06 — `health_weight_section`):** Zárt állapotban sparkline-nal; lenyitva `Test és fittség` (`Testsúly`, `Testzsír %`, `SpO2 %`, `VO2max`), `Változás` és `Minimum / Maximum` kártyapár, `+ Új mérés >` (`health_add_weight`), `7d / 30d / 3m / 1y / All` (`health_weight_range_*`) választó, simított arany súly-trendvonal és szerkeszthető súlysorok (`health_weight_row_*`).
   - **Lenyitható alsó szekciók:** `További Health-adatok`, `Egészségnapló` (`Ettől` / `Eddig` + 2×4-es `Napi összesítések` + egyedi rekordok `health_records_section` és D03–D05 `HealthRecordDetailSheet`), `Health Connect` (forrás-választó + 17 adattípus engedély-státusz listája), alul pedig `Szinkronizálás` (`health_refresh_action`) és `Engedélyek` (`health_connect_action`) gombpár.

5. **`V05. Fejlődés és Statisztikák` (`ProgressAnalyticsScreen.kt` — `main` `11-24-59`..`11-25-54` TELJES FELÉPÍTÉSE!):**
   - Felső választó: `Fejlődés` | `Statisztikák` (`Személyes rekordok` | `Kardióstatisztika`).
   - **`Fejlődés` nézet:**
     1. **`Összes volumen` & `Volumen trend` Hero kártya:** `26sp Bold tabular-nums` összvolumen, `1 nap / 7 nap / 30 nap / 3 hó` (`progress_range_*`) gombok (aktív gomb arany háttérrel és sötét szöveggel!), és interaktív arany oszlopdiagram vízszintes rácsvonalakkal.
     2. **2×2-es Lenyitható (Expandable) Metrika-rács:** `PR rekordok` (🏆), `Edzések száma` (📅), `Átlagos volumen` (↗), `Átl. edzésidő` (🕒). Koppintásra a kiválasztott kártya `1.5dp` arany keretet kap, és közvetlenül a rács alatt lenyílik a hozzá tartozó részletes lista (`PR rekordok` korábbi -> új értékekkel; `Edzések száma` / `Átlagos volumen` / `Átl. edzésidő` edzésenkénti listával).
     3. **`Izomcsoport-terhelés` kártya:** Bal oldalon elülső és hátsó anatómiai izomtérkép; jobb oldalon `Mell`, `Hát`, `Váll`, `Láb`, `Kar`, `Core` terhelési sávok (`Kisebb` sárga, `Közepes` narancs, `Nagyobb` piros színkódolással és százalékkal).
     4. **`Fejlődési javaslat` kártya** + **G10 Privát fotógaléria (`progress_photo_gallery`)**.
   - **`Statisztikák` nézet:**
     1. `Személyes rekordok`: minden naplózott gyakorlat kártyája a legjobb súllyal / ismétléssel (`ExercisePrCard`).
     2. `Kardióstatisztika`: `Minden mozgás` (`Heti kilométer`, `Előző hét`, `Átlagtempó`, `Rögzített idő` + heti km sávok) és `Napi aktivitás / lépések` (`Mai összes lépés`, `Edzésen kívül`, `Heti lépések`, `Előző hét` + `Lépésadatok frissítése` + napi bontás).

---

### P2 Csomag — Aktív edzésmódok és befejezés (`V06`–`V11` — Mockup `02` #1–#6)

1. **`V06. Súlyzós edzés` (Mockup `02` #1):**
   - Gyakorlat-fejléc (`<` és `>` léptető nyilakkal, `56×56dp` gyakorlat-illusztrációval, izomcsoporttal és cél-sorozattal `3 × 10 ism.`).
   - Sorozattábla (`#`, `Kg`, `Ism.`, `RIR`, `48×48dp` zöld pipa `active_complete_set_${index}`), alatta `Visszavonás` sáv.
   - **G12 — Aktív privát fotók:** Valódi `72×72dp` privát thumbnail, hiányzó/sérült fájlnál fallback kártya, **`48×48dp` törlés gomb** (`active_delete_photo_${id}`).
   - **G04 — Alsó `Pihenőidő` panel:** Kör-ívvel (`00:52`), arany `||` pause/resume gombbal (`active_rest_pause_resume` — csak a pihenőórát állítja meg, a `workoutPaused` nem változik!), `-15 mp`, `+15 mp` (`active_rest_add`) és `Kihagyás` (`active_rest_skip`) gombokkal, alul `52dp` arany `Edzés befejezése` CTA-val (sötét szöveggel).
2. **`V07. Egyoldali időalapú edzés` (Mockup `02` #2):** Sorozatsorok (`Kész` / `Folyamatban`), nagy `00:38` (`44sp Bold tabular-nums`) stopper arany folyamatjelző sávval, piros `Leállítás` és `Nullázás` gombpárral.
3. **`V08. Kétoldali időalapú edzés` (Mockup `02` #3):** Kétoszlopos `Bal oldal` | `Jobb oldal` kártyapár saját stopperrel (`00:25` | `00:18`), folyamatsávval, `Leállítás`/`Indítás` és `Nullázás` gombokkal (`active_bi_record_pair`).
4. **`V09. Kültéri GPS mérés` (Mockup `02` #4 — G05 + G06):**
   - `GPS-útvonal` kapcsoló + **G05 őszinte GPS-állapot sáv** (`GPS: rögzítés • X pont • ±Y m` vagy no-fix/stale jelzés; a `0` soha nem hiteles fix!).
   - 2×2-es élő mérési rács (`Távolság`, `Idő`, `Tempó`, `Pulzus`).
   - **G06 térkép (`active_gps_map`):** Explicit beleegyezés előtt zéró hálózati forgalom (offline `Canvas` poligon zöld rajtponttal és piros végponttal); beleegyezés esetén online csempék + zoom + `© OpenStreetMap` attribúció.
5. **`V10. Intervallumok` (Mockup `02` #5):** `Munkaidő`, `Pihenőidő`, `Ismétlések száma` `- / +` léptetőkkel, `Hangjelzés`, `Rezgés`, `Automatikus léptetés` kapcsolókkal és arany `Intervallumok indítása` CTA-val.
6. **`V11. Edzés befejezése` (Mockup `02` #6 — G07):** Trófea ikon, 3 oszlopos összegző sáv (`Időtartam`, `Összes ism.`, `Kalória`), 3×2-es `Hogyan érezted magad?` választó (`Túl könnyű`, `Kicsit könnyű`, `Pont jó`, `Kicsit nehéz`, `Túl nehéz`, `Fájdalom`), **G07 valódi `Megjegyzés` szövegmező (`finish_notes_input`) tartós bekötése a finalizerre**, arany `✓ Mentés` (`finish_confirm_save`), `Szüneteltetés és kilépés`, és piros keretes `Edzés elvetése`.

---

### P3–P5 Csomagok (`V12`–`V24` — `main` + Mockup `01`, `03`, `04`)

1. **`V12. Programok` (`TrainingProgramsScreen.kt` — `main` `11-24-48` + Mockup `01` #3 + A07):**
   - Felül arany `Új saját program létrehozása` CTA (sötét szöveggel), `+ Saját gyakorlat` (arany keretes gomb), `Gyakorlatok / Izomcsoportok`.
   - `Edzésprogramok` lista (`program_card_${id}`): chipek (`Beépített sablon`, `Otthon`, `Kezdő`), programcím (`18sp Bold`), ciklus/gyakorlatszám, jobb oldalon arany `Aktiválás` gomb (sötét szöveggel, `program_activate_${id}`) és `>` chevron.
   - **A07 (`Alapértékek visszaállítása`):** Explicit előnézet és megerősítés CAS/tranzakcióval, amely megvédi az egyedi gyakorlatokat, egyedi programokat és a hivatkozott elemeket.
2. **`V13. Gyakorlatkönyvtár és részletek` (`ExerciseLibraryScreen.kt` — Mockup `01` #5–#6 + A12 + G03):**
   - Kereső + szűrő-chipek (`Összes`, **`★ Kedvencek` (`exercise_filter_favorites`)**, `Láb`, `Mell`, `Hát`, `Váll`, `Kar`, `Core`).
   - Gyakorlatrészlet: izomtérkép-illusztráció, `Eszköz`, `Fő izmok`, `Másodlagos izmok`, számozott `Végrehajtás` lépések, alul `▶ Bemutató` és **G03 arany `+ Hozzáadás az edzéshez / Gyors edzés indítása` (`exercise_detail_quick_start`)** inline konfigurációval és session-guardokkal.
3. **`V14. Naptár és Újratervezés` (`TrainingCalendarScreen.kt` — `main` `11-25-57`, `11-26-05` + Mockup `01` #4):**
   - Hónap-fejléc (`<`, `2026. Október`, `>`), 7 oszlopos naprács (befejezett napokon arany `✓`, tervezett napokon arany `A` / `B` jelölés, a mai napon `1.5dp` arany keret).
   - Alatta az aktív program ütemező kártyája (`Újratervezés` gomb, `Tervezési ritmus`, `Első edzésnap`, `Kezdőnap`, `Időpont`, `Időtartam`, `Hetek száma` + arany `Edzések megtervezése` CTA) és az `Edzések újratervezése` modal (`Terv frissítése`).
4. **`V15. Program- és gyakorlatszerkesztő` (Mockup `04` #6):** `A nap` | `B nap` fülek, gyakorlatsorok helyben lenyitható arany keretes szerkesztővel (`Sorozat`, `Ismétlés`, `Súly`, `Pihenő` `- / +` léptetőkkel).
5. **`V16. Edzésnapló részletek és Edzésútvonal` (`WorkoutHistoryDetailScreen.kt` — Mockup `03` #2–#3):**
   - `Gyakorlatok (X)` | `Statisztikák` | `Forrás` fülek, lenyitható gyakorlat-akkordok sorozatsorokkal (`60 kg × 8 ✓`), `Fotók (X)` és `Útvonal megtekintése` gombok, `Egészségügyi forrás` chipek (`TrainPilot óra`, `Health Connect`), teljes képernyős GPS térkép zöld rajtponttal és **piros `X` bezáró/végponttal**.
6. **`V19. Coach` (`CoachScreen.kt` — `main` `11-29-18` + Mockup `03` #6):**
   - Felső arany-bronz keretes Hero: belsejében a meleg borostyán `MAI JAVASLAT` kártya (`Könnyített nap` + részletes indoklás), alatta 2 oszlopban `Mai készenlét` (`54/100` kör-ívvel) és `Következő edzés` (`Otthoni A/B • A`), alul `52dp` arany `Edzés indítása` CTA (`coach_start_workout`).
   - Alatta `Gyakorlatonkénti javaslat` lista (`coach_advice_${index}`) státusz-pill-lel (`Könnyebb kezdés` / `Normál terhelés`) és variáció-előnézettel (`coach_variation_${id}`, `coach_variation_save`).
7. **`V20–V24. Beállítások, Óra, Mentés, Engedélyek, Privát fotók` (`main` `11-29-28`..`11-29-37` + Mockup `04` #1–#5):**
   - `Beállítások`: `Nyelv` (`HU`, `EN`, `DE`, `RO`), `Témaszín` (32 paletta), `Biztonsági mentés` (`Mentés fájlba` | `Visszatöltés`), `Google-fiók és szinkron` (`Automatikus Drive-szinkron`, `Automatikus edzésnaptár`, arany `Drive-szinkron most`, `Naptárszinkron most`), `Edzésprofil` (`Kímélő kezdőprofil`, pirosas `Alapértékek visszaállítása`), és `Edzésstílus és progresszió` (`Előnézet: Jelenlegi vs Javasolt` + arany `Mentés` / `Mégse`).
   - `Óra és szinkron` (G02): tilos a kitalált modell/akku; csak hiteles kapcsolatállapot, szinkronidő és átadás (`watch_takeover` / `watch_handback`).

---

## IV. RÉSZ — BEMÁSOLÁSRA KÉSZ KÓDJAVÍTÁSOK (7 ÉRINTETT .KT FÁJL)

### 1. FÁJL: `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationEmulatorFixture.kt`
Cseréld le a `close` metódust erre:

~~~kotlin
    fun close(scenario: ActivityScenario<MainActivity>?) {
        instrumentation.waitForIdleSync()
        scenario?.close()
        stopServices()
    }
~~~

### 2. FÁJL: `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeTrainingModeApprovalUiTest.kt`
Cseréld le az `awaitRejected` metódust erre:

~~~kotlin
    private fun awaitRejected() {
        val label = nativeResources(fixture.context, "hu").getString(R.string.native_modes_compare_stale)
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }
~~~

### 3. FÁJL: `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationLanguageUiTest.kt`
Cseréld le a `trainingCalendarAndCoachFollowChangedAppLanguageOnRealNavigation` metódust erre:

~~~kotlin
    @Test fun trainingCalendarAndCoachFollowChangedAppLanguageOnRealNavigation() {
        launch()
        compose.onNodeWithTag("navigation_settings").performClick()
        selectLanguageInSettings("ro", "Profil de antrenament")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("navigation_training").performClick()
        awaitText("Antrenament rapid")
        compose.onNodeWithTag("navigation_home").performClick()
        compose.onNodeWithTag("shortcut_calendar").performScrollTo().performClick()
        compose.waitUntil(15_000) { nodesWithTag("navigation_back") }
        awaitText("Calendar")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("shortcut_coach").performScrollTo().performClick()
        compose.waitUntil(15_000) { nodesWithTag("coach_ready") }
        awaitText("Coach")
        compose.onNodeWithTag("navigation_back").performClick()

        compose.onNodeWithTag("navigation_settings").performClick()
        selectLanguageInSettings("de", "Trainingsprofil")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("shortcut_calendar").performScrollTo().performClick()
        compose.waitUntil(15_000) { nodesWithTag("navigation_back") }
        awaitText("Kalender")
        compose.onNodeWithTag("navigation_back").performClick()
        compose.onNodeWithTag("navigation_training").performClick()
        awaitText("Schnelltraining")
        assertEquals("de", fixture.selectedLanguage())
    }
~~~

### 4. FÁJL: `android/app/src/main/java/com/repforge/app/ui/home/HomeViewModel.kt`
A `rebuildState` metódusban a `todayMetrics` és `recovery` betöltését cseréld erre:

~~~kotlin
        val recovery = withContext(Dispatchers.IO) {
            NativeHomeRecoveryPresentation.resolve(appContext)
        }
        val todayMetrics = withContext(Dispatchers.IO) {
            NativeHomeRepository.loadTodayMetrics(appContext, appDb, currentLanguage, recovery)
        }
~~~

### 5. FÁJL (TELJES): `android/app/src/main/java/com/repforge/app/data/repository/NativeHomeRepository.kt`

~~~kotlin
package com.repforge.app.data.repository

import android.content.Context
import com.repforge.app.data.local.TrainPilotDatabase
import com.repforge.app.data.local.entity.WorkoutHistoryEntity
import com.repforge.app.data.store.HealthJournalStore
import com.repforge.app.ui.home.HomeRecoveryTone
import com.repforge.app.ui.home.HomeRecoveryUiModel
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class HomeMetricCardUiModel(
    val key: String,
    val label: String,
    val value: String,
    val unit: String = "",
    val status: String = "",
    val tone: String = "neutral"
)

object NativeHomeRepository {

    fun loadTodayMetrics(
        context: Context,
        db: TrainPilotDatabase,
        language: String,
        recovery: HomeRecoveryUiModel? = null
    ): List<HomeMetricCardUiModel> {
        val recentWorkouts = runCatching {
            db.workoutHistoryDao().getRecentWorkouts(limit = 40)
        }.getOrDefault(emptyList())
        val healthSnapshot = runCatching { HealthJournalStore.get(context).snapshot() }.getOrNull()
        val resolvedRecovery = recovery ?: runCatching {
            com.repforge.app.ui.home.NativeHomeRecoveryPresentation.resolve(context)
        }.getOrNull()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekStart = today.minusDays(6)

        val weekWorkouts = recentWorkouts.filter { entity ->
            val date = Instant.ofEpochMilli(entity.completedAtEpochMs).atZone(zone).toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(today)
        }
        val weekMinutes = weekWorkouts.sumOf { parseDurationMinutes(it) }
        val streakDays = computeStreakDays(recentWorkouts, today, zone)

        val recoveryScore = resolvedRecovery?.score
        val sleepMinutes = healthSnapshot?.latestSleep?.durationMinutes
        val latestWeight = healthSnapshot?.latestWeight

        return listOf(
            HomeMetricCardUiModel(
                key = "workouts",
                label = tr(language, "Edzések (7 nap)", "Workouts (7d)", "Workouts (7T)", "Antrenamente (7z)"),
                value = weekWorkouts.size.toString(),
                unit = tr(language, "alkalom", "sessions", "Einheiten", "sesiuni"),
                status = if (weekWorkouts.isNotEmpty()) {
                    tr(language, "Aktív hét", "Active week", "Aktive Woche", "Săptămână activă")
                } else {
                    tr(language, "Indíts edzést ma", "Start today", "Heute starten", "Începe azi")
                },
                tone = if (weekWorkouts.isNotEmpty()) "accent" else "neutral"
            ),
            HomeMetricCardUiModel(
                key = "duration",
                label = tr(language, "Edzésidő (7 nap)", "Training time (7d)", "Trainingszeit (7T)", "Timp antrenament (7z)"),
                value = weekMinutes.toString(),
                unit = tr(language, "perc", "min", "Min", "min"),
                status = if (weekMinutes >= 90) {
                    tr(language, "Jó volumen", "Good volume", "Gutes Volumen", "Volum bun")
                } else {
                    tr(language, "Heti összesítés", "Weekly total", "Wochenschnitt", "Total săptămânal")
                },
                tone = if (weekMinutes >= 90) "success" else "neutral"
            ),
            HomeMetricCardUiModel(
                key = "recovery",
                label = tr(language, "Regeneráció", "Recovery", "Erholung", "Recuperare"),
                value = recoveryScore?.toString() ?: "—",
                unit = if (recoveryScore != null) "/100" else "",
                status = resolvedRecovery?.headline ?: when {
                    recoveryScore == null -> tr(language, "Nincs friss adat", "No recent data", "Keine Daten", "Fără date")
                    recoveryScore >= 75 -> tr(language, "Edzésre kész", "Ready to train", "Bereit", "Pregătit")
                    recoveryScore >= 50 -> tr(language, "Mérsékelt", "Moderate", "Mäßig", "Moderat")
                    else -> tr(language, "Pihenés ajánlott", "Rest advised", "Ruhe empfohlen", "Odihnă recomandată")
                },
                tone = when (resolvedRecovery?.tone) {
                    HomeRecoveryTone.READY -> "success"
                    HomeRecoveryTone.MODERATE -> "warning"
                    HomeRecoveryTone.LOW -> "danger"
                    else -> "neutral"
                }
            ),
            HomeMetricCardUiModel(
                key = "sleep",
                label = tr(language, "Alvás", "Sleep", "Schlaf", "Somn"),
                value = sleepMinutes?.let { formatHours(it) } ?: "—",
                unit = if (sleepMinutes != null) tr(language, "h", "h", "Std", "h") else "",
                status = when {
                    sleepMinutes == null -> tr(language, "Egészségnapló", "Health log", "Gesundheitslog", "Jurnal sănătate")
                    sleepMinutes >= 420 -> tr(language, "Jó minőség", "Optimal", "Optimal", "Optim")
                    else -> tr(language, "Rövid alvás", "Short sleep", "Kurzer Schlaf", "Somn scurt")
                },
                tone = when {
                    sleepMinutes == null -> "neutral"
                    sleepMinutes >= 420 -> "success"
                    else -> "warning"
                }
            ),
            HomeMetricCardUiModel(
                key = "weight",
                label = tr(language, "Testsúly", "Body weight", "Körpergewicht", "Greutate"),
                value = latestWeight?.weightKg?.let { formatOneDecimal(it) } ?: "—",
                unit = if (latestWeight != null) "kg" else "",
                status = latestWeight?.date ?: tr(language, "Nincs rögzítve", "Not logged", "Nicht erfasst", "Neînregistrat"),
                tone = if (latestWeight != null) "info" else "neutral"
            ),
            HomeMetricCardUiModel(
                key = "streak",
                label = tr(language, "Sorozat", "Streak", "Serie", "Serie"),
                value = streakDays.toString(),
                unit = tr(language, "nap", "days", "Tage", "zile"),
                status = if (streakDays > 0) {
                    tr(language, "Folyamatos aktivitás", "Active streak", "Aktive Serie", "Serie activă")
                } else {
                    tr(language, "Ma indítható", "Start today", "Heute starten", "Începe azi")
                },
                tone = if (streakDays > 0) "accent" else "neutral"
            )
        )
    }

    private fun parseDurationMinutes(entity: WorkoutHistoryEntity): Int {
        return runCatching {
            val json = JSONObject(entity.rawWorkoutJson)
            val sec = json.optLong("durationSeconds", 0L)
            if (sec > 0L) (sec / 60.0).roundToInt().coerceAtLeast(1)
            else json.optInt("durationMinutes", 0).coerceAtLeast(0)
        }.getOrDefault(0)
    }

    private fun computeStreakDays(
        workouts: List<WorkoutHistoryEntity>,
        today: LocalDate,
        zone: ZoneId
    ): Int {
        val activeDays = workouts.map {
            Instant.ofEpochMilli(it.completedAtEpochMs).atZone(zone).toLocalDate()
        }.toSet()
        if (activeDays.isEmpty()) return 0
        var cursor = if (today in activeDays) today else today.minusDays(1)
        if (cursor !in activeDays) return 0
        var count = 0
        while (cursor in activeDays) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private fun formatHours(minutes: Int): String =
        String.format(java.util.Locale.US, "%.1f", minutes / 60.0)

    private fun formatOneDecimal(value: Double): String =
        String.format(java.util.Locale.US, "%.1f", value)

    private fun tr(language: String, hu: String, en: String, de: String, ro: String): String =
        when (language) {
            "en" -> en
            "de" -> de
            "ro" -> ro
            else -> hu
        }
}
~~~

### 6. FÁJL (TELJES): `android/app/src/main/java/com/repforge/app/ui/home/HomeScreen.kt`
(`main` + Mockup `01` #1 szerinti felépítés: sötét szövegű arany CTA gombokkal, arany keretes Hero kártyával, ikonos `Mai állapot` ráccsal, meleg borostyán `MAI JAVASLAT` Coach kártyával és `isLoading` alatt is elérhető navigációval):

~~~kotlin
package com.repforge.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.data.repository.HomeMetricCardUiModel
import com.repforge.app.data.workout.WorkoutLaunchResult
import com.repforge.app.data.workout.WorkoutLauncher
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.theme.TrainPilotActionSurface
import com.repforge.app.ui.theme.TrainPilotHeroSurface
import com.repforge.app.ui.theme.TrainPilotSectionHeader
import com.repforge.app.ui.theme.TrainPilotSectionSurface
import com.repforge.app.ui.theme.TrainPilotStatusBadge
import com.repforge.app.ui.theme.TrainPilotTheme
import kotlinx.coroutines.launch

private fun onAccentColor(accent: Color): Color =
    if (accent.luminance() > 0.40f) Color(0xFF0E1116) else Color.White

@Composable
fun HomeScreen(
    language: String = "hu",
    offerOnboarding: Boolean = false,
    onOpenActiveWorkout: () -> Unit = {},
    onQuickWorkout: (Boolean) -> Unit = {},
    onResumeDraft: () -> Unit = {},
    onOnboarding: () -> Unit = {},
    onCalendar: () -> Unit = {},
    onCoach: () -> Unit = {},
    onPrograms: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = TrainPilotTheme.colors
    val spacing = TrainPilotTheme.spacing
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var launching by remember { mutableStateOf(false) }
    val busy = launching || uiState.isFinishingWorkout

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        if (offerOnboarding) {
            OnboardingResumeCard(language = language, enabled = !busy, onClick = onOnboarding)
        }
        if (uiState.hasImportedDraft && !uiState.hasActiveWorkout) {
            ImportedDraftCard(
                title = uiState.importedDraftTitle,
                language = language,
                enabled = !busy,
                onResume = onResumeDraft
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag("home_loading"),
                    color = colors.accent
                )
            }
        } else {
            TodayHeroCard(
                uiState = uiState,
                language = language,
                busy = busy,
                onStartOrContinue = {
                    if (busy) return@TodayHeroCard
                    if (uiState.hasActiveWorkout) {
                        onOpenActiveWorkout()
                    } else {
                        launching = true
                        scope.launch {
                            try {
                                val result = WorkoutLauncher.startNextProgramDay(context)
                                if (result is WorkoutLaunchResult.Ready) {
                                    onOpenActiveWorkout()
                                }
                            } finally {
                                launching = false
                            }
                        }
                    }
                }
            )
        }

        QuickStartRow(
            language = language,
            enabled = !busy,
            hasActiveWorkout = uiState.hasActiveWorkout,
            onQuickWorkout = onQuickWorkout
        )

        if (!uiState.isLoading) {
            TodayMetricsGrid(metrics = uiState.todayMetrics, language = language)
            DailyRecommendationCard(
                recovery = uiState.recovery,
                language = language,
                onClick = onCoach
            )
        }

        NativePlanningShortcuts(
            language = language,
            enabled = !busy,
            onCalendar = onCalendar,
            onCoach = onCoach,
            onPrograms = onPrograms
        )
    }
}

@Composable
private fun TodayHeroCard(
    uiState: HomeUiState,
    language: String,
    busy: Boolean,
    onStartOrContinue: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val onAccent = onAccentColor(colors.accent)
    val badgeLabel = when {
        uiState.hasActiveWorkout -> tr(language, "Folyamatban", "In progress", "Aktiv", "În curs")
        else -> tr(language, "Aktív program", "Active program", "Aktives Programm", "Program activ")
    }
    val nextLabel = tr(language, "Következő", "Next up", "Nächstes", "Următorul")
    val programName = when {
        uiState.hasActiveWorkout && uiState.activeWorkoutTitle.isNotBlank() -> uiState.activeWorkoutTitle
        uiState.activeProgramName.isNotBlank() -> uiState.activeProgramName
        else -> tr(language, "Nincs aktív program", "No active program", "Kein aktives Programm", "Niciun program activ")
    }
    val dayTitle = when {
        uiState.hasActiveWorkout -> tr(
            language,
            "${uiState.activeCompletedSets} befejezett sorozat",
            "${uiState.activeCompletedSets} completed sets",
            "${uiState.activeCompletedSets} abgeschlossene Sätze",
            "${uiState.activeCompletedSets} serii finalizate"
        )
        uiState.nextDayTitle.isNotBlank() -> uiState.nextDayTitle
        else -> tr(language, "Gyors edzés indítható", "Quick workout ready", "Schnelltraining bereit", "Antrenament rapid")
    }
    val primaryLabel = when {
        uiState.isFinishingWorkout -> tr(language, "Mentés folyamatban…", "Saving workout…", "Workout wird gespeichert…", "Se salvează antrenamentul…")
        uiState.hasActiveWorkout -> tr(language, "Edzés folytatása", "Continue Workout", "Training fortsetzen", "Continuă antrenamentul")
        else -> tr(language, "Tervezett edzés indítása", "Start Planned Workout", "Geplantes Training starten", "Începe antrenamentul planificat")
    }

    TrainPilotHeroSurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, colors.accent.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TrainPilotStatusBadge(
                    text = badgeLabel,
                    containerColor = colors.accentSoft,
                    contentColor = colors.accentText,
                    icon = Icons.Default.FitnessCenter
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = programName,
                    color = colors.text,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("home_program_name")
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.borderSubtle)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nextLabel,
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayTitle,
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("home_day_name")
                )
                if (uiState.nextDayExerciseCount > 0 && !uiState.hasActiveWorkout) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr(
                            language,
                            "${uiState.nextDayExerciseCount} gyakorlat",
                            "${uiState.nextDayExerciseCount} exercises",
                            "${uiState.nextDayExerciseCount} Übungen",
                            "${uiState.nextDayExerciseCount} exerciții"
                        ),
                        color = colors.muted,
                        fontSize = 12.sp,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStartOrContinue,
            enabled = !busy && (uiState.hasActiveWorkout || uiState.nextDayId.isNotBlank()),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = onAccent,
                disabledContainerColor = colors.surface2,
                disabledContentColor = colors.muted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("home_start_workout")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = primaryLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickStartRow(
    language: String,
    enabled: Boolean,
    hasActiveWorkout: Boolean,
    onQuickWorkout: (Boolean) -> Unit
) {
    val colors = TrainPilotTheme.colors
    val spacing = TrainPilotTheme.spacing
    val quickTitle = tr(language, "Gyors edzés", "Quick Workout", "Schnelltraining", "Antrenament rapid")
    val quickSub = tr(language, "Szabad gyakorlatválasztás", "Free exercise selection", "Freie Übungsauswahl", "Selecție liberă")
    val outdoorTitle = tr(language, "Kültéri edzés", "Outdoor Workout", "Outdoor-Training", "Antrenament exterior")
    val outdoorSub = tr(language, "GPS útvonal és élő mérés", "GPS route & live stats", "GPS-Route & Live-Messung", "Traseu GPS & date live")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(spacing.gridGap)
    ) {
        TrainPilotActionSurface(
            onClick = { onQuickWorkout(false) },
            enabled = enabled && !hasActiveWorkout,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("home_quick_workout")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentSoft)
                        .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = quickTitle,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = quickSub,
                        color = colors.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        TrainPilotActionSurface(
            onClick = { onQuickWorkout(true) },
            enabled = enabled && !hasActiveWorkout,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("home_outdoor_workout")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentSoft)
                        .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = outdoorTitle,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = outdoorSub,
                        color = colors.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun TodayMetricsGrid(
    metrics: List<HomeMetricCardUiModel>,
    language: String
) {
    if (metrics.isEmpty()) return
    val spacing = TrainPilotTheme.spacing
    val title = tr(language, "Mai állapot", "Today's Status", "Heutiger Status", "Starea de azi")

    Column(verticalArrangement = Arrangement.spacedBy(spacing.cardGap)) {
        TrainPilotSectionHeader(title = title)

        val rows = metrics.chunked(2)
        rows.forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(spacing.gridGap)
            ) {
                pair.forEach { metric ->
                    MetricCard(
                        metric = metric,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (metric.key == "recovery") {
                                    Modifier.testTag("home_recovery_card")
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: HomeMetricCardUiModel,
    modifier: Modifier = Modifier
) {
    val colors = TrainPilotTheme.colors
    val icon = when (metric.key) {
        "sleep" -> Icons.Default.Bedtime
        "duration" -> Icons.Default.LocalFireDepartment
        "recovery" -> Icons.Default.Eco
        "weight" -> Icons.Default.MonitorWeight
        "streak" -> Icons.Default.Bolt
        else -> Icons.Default.FitnessCenter
    }
    val statusColor = when (metric.tone) {
        "success" -> colors.success
        "warning" -> colors.warning
        "danger" -> colors.danger
        "info" -> colors.info
        "accent" -> colors.accentText
        else -> colors.muted
    }

    TrainPilotSectionSurface(
        modifier = modifier.testTag("home_metric_card_${metric.key}"),
        contentPadding = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = metric.label,
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = metric.value,
                    color = colors.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                if (metric.unit.isNotBlank()) {
                    Text(
                        text = metric.unit,
                        color = colors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = metric.status.ifBlank { " " },
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyRecommendationCard(
    recovery: HomeRecoveryUiModel,
    language: String,
    onClick: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val header = tr(language, "MAI JAVASLAT", "TODAY'S ADVICE", "HEUTIGE EMPFEHLUNG", "SFATUL DE AZI")
    val readinessLabel = tr(language, "Mai készenlét", "Today's readiness", "Heutige Bereitschaft", "Pregătire azi")
    val scoreText = recovery.score?.let { "$it/100" } ?: "—"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.accentSoft)
            .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = header,
                color = colors.accentText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = recovery.headline,
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (recovery.detail.isNotBlank()) {
            Text(
                text = recovery.detail,
                color = colors.muted,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(text = readinessLabel, color = colors.muted, fontSize = 11.sp)
                Text(
                    text = scoreText,
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }
    }
}

@Composable
private fun OnboardingResumeCard(
    language: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val onAccent = onAccentColor(colors.accent)
    val title = tr(language, "Személyes edzéstervező", "Personal Training Planner", "Persönlicher Trainingsplaner", "Planificator personal")
    val body = tr(language, "Állítsunk össze egy hozzád illő, követhető tervet.", "Let's build a tailored plan that fits your goals.", "Erstellen wir einen passenden Trainingsplan für dich.", "Să construim un plan adaptat obiectivelor tale.")
    val action = tr(language, "Profil és új terv", "Profile & new plan", "Profil & neuer Plan", "Profil și plan nou")

    TrainPilotHeroSurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
    ) {
        Text(text = title, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = body, color = colors.muted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = onAccent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("home_onboarding_resume")
        ) {
            Text(text = action, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ImportedDraftCard(
    title: String,
    language: String,
    enabled: Boolean,
    onResume: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val onAccent = onAccentColor(colors.accent)
    val label = tr(language, "Mentett edzéspiszkozat", "Saved workout draft", "Gespeicherter Entwurf", "Ciornă de antrenament")
    val action = tr(language, "Piszkozat folytatása", "Resume draft", "Entwurf fortsetzen", "Continuă ciorna")

    TrainPilotSectionSurface(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = colors.warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (title.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onResume,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = onAccent),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("home_resume_draft")
        ) {
            Text(text = action, fontWeight = FontWeight.Bold)
        }
    }
}

private fun tr(language: String, hu: String, en: String, de: String, ro: String): String =
    when (language) {
        "en" -> en
        "de" -> de
        "ro" -> ro
        else -> hu
    }
~~~

### 7. FÁJL (TELJES): `android/app/src/main/java/com/repforge/app/ui/programs/TrainingLandingScreen.kt`
(`main` `11-24-07` + Mockup `01` #2 szerinti felépítés: sötét szövegű arany `Gyors edzés indítása` CTA-val, 2 oszlopos eszközkártyákkal, arany keretes következő programnappal és `A / B` betű-badge-ekkel):

~~~kotlin
package com.repforge.app.ui.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.data.workout.WorkoutLaunchResult
import com.repforge.app.data.workout.WorkoutLauncher
import com.repforge.app.ui.theme.TrainPilotActionSurface
import com.repforge.app.ui.theme.TrainPilotHeroSurface
import com.repforge.app.ui.theme.TrainPilotSectionHeader
import com.repforge.app.ui.theme.TrainPilotSectionSurface
import com.repforge.app.ui.theme.TrainPilotStatusBadge
import com.repforge.app.ui.theme.TrainPilotTheme
import kotlinx.coroutines.launch

private fun onAccentColor(accent: Color): Color =
    if (accent.luminance() > 0.40f) Color(0xFF0E1116) else Color.White

@Composable
fun TrainingLandingScreen(
    language: String = "hu",
    onOpenActiveWorkout: () -> Unit = {},
    onQuickWorkout: (Boolean) -> Unit = {},
    onResumeDraft: () -> Unit = {},
    onOpenPrograms: () -> Unit = {},
    onOpenExercises: () -> Unit = {},
    viewModel: TrainingLandingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = TrainPilotTheme.colors
    val spacing = TrainPilotTheme.spacing
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var launchingDayId by remember { mutableStateOf<String?>(null) }
    val busy = launchingDayId != null || uiState.isFinishingWorkout

    val daysTitle = when (language) {
        "en" -> "ACTIVE PROGRAM"
        "de" -> "AKTIVES PROGRAMM"
        "ro" -> "PROGRAM ACTIV"
        else -> "AKTÍV PROGRAM"
    }
    val emptyDays = when (language) {
        "en" -> "No program days found in the active program."
        "de" -> "Keine Programmtage im aktiven Programm gefunden."
        "ro" -> "Nu există zile în programul activ."
        else -> "Az aktív program nem tartalmaz edzésnapokat."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = spacing.screenHorizontal),
        contentPadding = PaddingValues(vertical = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("training_loading"),
                        color = colors.accent
                    )
                }
            }
        } else {
            if (uiState.hasActiveWorkout) {
                item {
                    ActiveWorkoutBanner(
                        title = uiState.activeWorkoutTitle,
                        completedSets = uiState.activeCompletedSets,
                        isFinishing = uiState.isFinishingWorkout,
                        language = language,
                        onContinue = onOpenActiveWorkout
                    )
                }
            } else if (uiState.hasImportedDraft) {
                item {
                    DraftBanner(
                        title = uiState.importedDraftTitle,
                        language = language,
                        enabled = !busy,
                        onResume = onResumeDraft
                    )
                }
            }

            item {
                QuickActionsSection(
                    language = language,
                    enabled = !busy && !uiState.hasActiveWorkout,
                    onQuickWorkout = onQuickWorkout
                )
            }

            item {
                PlanningToolsRow(
                    language = language,
                    enabled = !busy,
                    onOpenPrograms = onOpenPrograms,
                    onOpenExercises = onOpenExercises
                )
            }

            item {
                TrainPilotSectionHeader(
                    title = daysTitle,
                    subtitle = uiState.activeProgramName.ifBlank { null }
                )
            }

            if (uiState.days.isEmpty()) {
                item {
                    TrainPilotSectionSurface(modifier = Modifier.fillMaxWidth()) {
                        Text(text = emptyDays, color = colors.muted, fontSize = 14.sp)
                    }
                }
            } else {
                val nextDay = uiState.days.firstOrNull { it.id == uiState.nextProgramDayId }
                    ?: uiState.days.firstOrNull()
                val highlightedId = nextDay?.id
                itemsIndexed(uiState.days, key = { _, d -> d.id }) { index, day ->
                    ProgramDayRow(
                        index = index,
                        day = day,
                        isNextDay = day.id == highlightedId,
                        language = language,
                        enabled = !busy && !uiState.hasActiveWorkout,
                        onStart = {
                            if (busy) return@ProgramDayRow
                            launchingDayId = day.id
                            scope.launch {
                                try {
                                    val result = WorkoutLauncher.startProgramDay(context, day.id)
                                    if (result is WorkoutLaunchResult.Ready) {
                                        onOpenActiveWorkout()
                                    }
                                } finally {
                                    launchingDayId = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutBanner(
    title: String,
    completedSets: Int,
    isFinishing: Boolean,
    language: String,
    onContinue: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val onAccent = onAccentColor(colors.accent)
    val label = when {
        isFinishing -> when (language) {
            "en" -> "Saving workout…"
            "de" -> "Workout wird gespeichert…"
            "ro" -> "Se salvează antrenamentul…"
            else -> "Edzés mentése folyamatban…"
        }
        else -> when (language) {
            "en" -> "Active workout in progress"
            "de" -> "Aktives Training läuft"
            "ro" -> "Antrenament activ în desfășurare"
            else -> "Folyamatban lévő edzés"
        }
    }
    val action = when (language) {
        "en" -> "Continue"
        "de" -> "Fortsetzen"
        "ro" -> "Continuă"
        else -> "Folytatás"
    }

    TrainPilotHeroSurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
    ) {
        TrainPilotStatusBadge(
            text = label,
            containerColor = colors.accentSoft,
            contentColor = colors.accentText
        )
        if (title.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$completedSets",
            color = colors.muted,
            fontSize = 13.sp,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            enabled = !isFinishing,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = onAccent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("training_resume_active")
        ) {
            Text(text = action, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DraftBanner(
    title: String,
    language: String,
    enabled: Boolean,
    onResume: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val onAccent = onAccentColor(colors.accent)
    val label = when (language) {
        "en" -> "Saved workout draft"
        "de" -> "Gespeicherter Entwurf"
        "ro" -> "Ciornă de antrenament"
        else -> "Mentett edzéspiszkozat"
    }
    val action = when (language) {
        "en" -> "Resume draft"
        "de" -> "Entwurf fortsetzen"
        "ro" -> "Continuă ciorna"
        else -> "Piszkozat folytatása"
    }

    TrainPilotSectionSurface(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = colors.warning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (title.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onResume,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = onAccent),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("training_resume_draft")
        ) {
            Text(text = action, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickActionsSection(
    language: String,
    enabled: Boolean,
    onQuickWorkout: (Boolean) -> Unit
) {
    val colors = TrainPilotTheme.colors
    val onAccent = onAccentColor(colors.accent)
    val quickLabel = when (language) {
        "en" -> "Quick Workout"
        "de" -> "Schnelltraining"
        "ro" -> "Antrenament rapid"
        else -> "Gyors edzés indítása"
    }
    val quickSub = when (language) {
        "en" -> "Choose exercises and start immediately."
        "de" -> "Übung wählen und sofort starten."
        "ro" -> "Alege un exercițiu și începe imediat."
        else -> "Válassz egy gyakorlatot, és kezdd el."
    }
    val outdoorLabel = when (language) {
        "en" -> "Outdoor GPS"
        "de" -> "Outdoor-GPS"
        "ro" -> "Exterior GPS"
        else -> "Kültéri GPS edzés"
    }
    val outdoorSub = when (language) {
        "en" -> "Route, distance & live pace"
        "de" -> "Route, Distanz & Live-Tempo"
        "ro" -> "Traseu, distanță & ritm live"
        else -> "Útvonal, távolság és élő tempó"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TrainPilotHeroSurface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
        ) {
            Button(
                onClick = { onQuickWorkout(false) },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = onAccent,
                    disabledContainerColor = colors.surface2,
                    disabledContentColor = colors.muted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("training_quick_workout")
            ) {
                Text(text = quickLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quickSub,
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        TrainPilotActionSurface(
            onClick = { onQuickWorkout(true) },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("training_outdoor_workout")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentSoft)
                        .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(text = outdoorLabel, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = outdoorSub, color = colors.muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PlanningToolsRow(
    language: String,
    enabled: Boolean,
    onOpenPrograms: () -> Unit,
    onOpenExercises: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val spacing = TrainPilotTheme.spacing
    val programsLabel = when (language) {
        "en" -> "Choose Another Program"
        "de" -> "Anderes Programm wählen"
        "ro" -> "Alege alt program"
        else -> "Másik program választása"
    }
    val exercisesLabel = when (language) {
        "en" -> "Exercises / Muscle Groups"
        "de" -> "Übungen / Muskelgruppen"
        "ro" -> "Exerciții / Grupe musculare"
        else -> "Gyakorlatok / Izomcsoportok"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(spacing.gridGap)
    ) {
        TrainPilotActionSurface(
            onClick = onOpenPrograms,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("training_open_programs")
        ) {
            Text(text = programsLabel, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        TrainPilotActionSurface(
            onClick = onOpenExercises,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("training_open_exercises")
        ) {
            Text(text = exercisesLabel, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProgramDayRow(
    index: Int,
    day: ProgramDaySummaryUiModel,
    isNextDay: Boolean,
    language: String,
    enabled: Boolean,
    onStart: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val onAccent = onAccentColor(colors.accent)
    val codeLetter = ('A' + (index % 26)).toString()
    val startLabel = when (language) {
        "en" -> "Start"
        "de" -> "Starten"
        "ro" -> "Start"
        else -> "Indítás"
    }
    val countLabel = when (language) {
        "en" -> "${day.exerciseCount} exercises"
        "de" -> "${day.exerciseCount} Übungen"
        "ro" -> "${day.exerciseCount} exerciții"
        else -> "${day.exerciseCount} gyakorlat"
    }

    TrainPilotSectionSurface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNextDay) {
                    Modifier.border(1.5.dp, colors.accent.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .testTag("training_day_${day.id}"),
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accentSoft)
                        .border(
                            width = 1.dp,
                            color = colors.accent.copy(alpha = if (isNextDay) 0.75f else 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = codeLetter,
                        color = colors.accentText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = day.title,
                        color = colors.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countLabel,
                        color = colors.muted,
                        fontSize = 13.sp,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            Button(
                onClick = onStart,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNextDay) colors.accent else colors.cardElevated,
                    contentColor = if (isNextDay) onAccent else colors.text,
                    disabledContainerColor = colors.surface2,
                    disabledContentColor = colors.muted
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("training_start_day_${day.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = startLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
~~~