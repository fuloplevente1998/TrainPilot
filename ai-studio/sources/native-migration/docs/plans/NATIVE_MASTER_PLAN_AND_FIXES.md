# TrainPilot Natív Android — Teljes Vizuális Terv (V01–V24 / P0–P6) és Kódjavítási Csomag

Alapállapot:
- Utolsó teljesen zöld funkcionális + P0 alap: `31dbcde2` / `2f7cd835` (1333 JVM teszt PASS, 112/112 telefonos UI PASS, 3 Wear shard PASS, 2 signed upgrade PASS).
- Jelenlegi kísérleti P1 jelölt: `67f2889f` (CI run `40464028059` — 99 PASS, 3 FAIL, 12 MISSING / `Process crashed`).

---

## I. RÉSZ — MIT ÉS HOGYAN JAVÍTUNK A PIROS TELEFONOS UI-KAPUN (`67f2889f`)

### 1. Hiba #1: `NativeLocalizationLanguageUiTest.trainingCalendarAndCoachFollowChangedAppLanguageOnRealNavigation`
- **Tünet:** `navigation_back` nem létezik kattintáskor.
- **Gyökérok:**
  1. A P1-es `HomeScreen.kt`-ban a `NativePlanningShortcuts` (`shortcut_calendar`, `shortcut_coach`, `shortcut_programs`) a `TodayHeroCard`, a `QuickStartRow` (és annak redundáns szekciófejléce) valamint a 3×100dp-s `TodayMetricsGrid` alá (~750dp mélyre, a látható képernyőn kívülre) csúszott, miközben `uiState.isLoading == true` alatt egyáltalán nem is volt a Compose-fában.
  2. A `NativeLocalizationLanguageUiTest.kt` 90., 93. és 99. sora `.performScrollTo()` **nélkül** hív `.performClick()`-et a `shortcut_calendar` és `shortcut_coach` gombokra, így a kattintás nem vált ki navigációt (az app a `"home"` route-on marad).
  3. A rákövetkező `awaitText("Calendar")` és `awaitText("Kalender")` azért adott false-positive találatot, mert a `HomeScreen`-en lévő `shortcut_calendar` gomb felirata románul szintén `"Calendar"`, németül pedig `"Kalender"`.
- **Javítás:**
  - `NativeLocalizationLanguageUiTest.kt`: `.performScrollTo().performClick()` használata + `compose.waitUntil(15_000) { nodesWithTag("navigation_back") }` ellenőrzés.
  - `HomeScreen.kt`: a navigációs gyorsgombok (`QuickStartRow` és `NativePlanningShortcuts`) mindig a Compose-fában maradnak (nem tűnnek el `isLoading` alatt), a redundáns `"GYORS INDÍTÁS"` fejléc kikerül, a `TodayMetricsGrid` pedig `min = 100.dp` helyett `IntrinsicSize.Max` alapú kompakt (~76dp/sor) magasságot kap.

### 2. Hiba #2: `NativePhoneNavigationTest.coachVariationPreviewCancelsWithoutWritingAndSavesOnlyAnExplicitCopy` (1276. sor)
- **Tünet:** `ComposeTimeoutException` 15 000 ms után.
- **Gyökérok:**
  1. A P1-ben a `NativeHomeRepository.loadTodayMetrics` minden egyes Home-állapotfrissítéskor meghívta a teljes **`NativeCoachEngine.load(context, db)`** motort `Dispatchers.IO`-n, mielőtt a `HomeViewModel` az `isLoading`-ot `false`-ra állította volna.
  2. Amikor a teszt előre betöltött edzésnaplóval elindul vagy a `CoachScreen`-en variációt ment (`saveVariation`), a NavController backstackjén (`"home"`) élő `HomeViewModel` a háttérben újra és újra ráindítja a `NativeCoachEngine.load`-ot, versengve a `CoachViewModel.reload()` Room-műveleteivel, miközben a `HomeScreen` `isLoading` alatt elrejti a `shortcut_*` gombokat.
  3. Ráadásul ez megsérti a `03-source-bindings.md` V01 szerződését is, amely szerint a `home_recovery_card` hiteles forrása a `NativeHomeRecoveryPresentation` (amit a `HomeViewModel` már kiszámol, de a `HomeScreen` eddig figyelmen kívül hagyott).
- **Javítás:**
  - `HomeViewModel.kt` + `NativeHomeRepository.kt`: a `NativeCoachEngine.load(context, db)` hívást teljesen eltávolítjuk a `NativeHomeRepository.loadTodayMetrics`-ből. A `HomeViewModel` a már feloldott `NativeHomeRecoveryPresentation.resolve(appContext)` eredményét adja át a `loadTodayMetrics`-nek, így a `"recovery"` csempe (`home_recovery_card`) közvetlenül a hiteles `recovery` modellt mutatja, a `HomeViewModel` pedig ~10× gyorsabb és nem zavarja a `CoachScreen`-t.

### 3. Hiba #3: `NativeTrainingModeApprovalUiTest.uiApprovalRejectsProgramRawChangeAfterShownComparison` + 12 MISSING teszt (`Process crashed`)
- **Tünet:** Compose `SlotWriter` `ArrayIndexOutOfBoundsException` a composition dispose közben, ami megöli az instrumentation folyamatot, ezért a hátralévő 12 teszt `MISSING` lesz.
- **Gyökérok:**
  1. A `TrainingProgramsScreen.kt` (183–207. sor) a `training_modes_program_approve` elutasításakor bezárja az első `AlertDialog` subcompositiont (`training_modes_program_dialog`), és **ugyanabban a lépésben** `state.modeStatus` beállításával megnyit egy második `AlertDialog` subcompositiont.
  2. Az `awaitRejected()` csak a szemantikai fa szövegét várja meg (`fetchSemanticsNodes().isNotEmpty()`), majd `compose.waitForIdle()` nélkül azonnal ráfut a `@After fun close() = fixture.close(scenario)` hívásra.
  3. A `NativeLocalizationEmulatorFixture.close(scenario)` közvetlenül meghívja a `scenario?.close()`-t (`instrumentation.waitForIdleSync()` csak utána, a `stopServices()`-ben fut!). Így az Activity lebontása (`disposeComposition`) pont a két `AlertDialog` subcomposition váltása közben fut le, inkonzisztens `SlotWriter` állapotot okozva.
- **Javítás:**
  - `NativeLocalizationEmulatorFixture.kt`: `instrumentation.waitForIdleSync()` hívása közvetlenül a `scenario?.close()` **előtt**.
  - `NativeTrainingModeApprovalUiTest.kt` (`awaitRejected`) és `NativeHomeMetricLayoutUiTest.kt`: `compose.waitForIdle()` hívása a várakozás és a tesztek végén.

---

## II. RÉSZ — TELJES VIZUÁLIS ÉS FUNKCIONÁLIS TERV AZ EGÉSZ ALKALMAZÁSRA (P0–P6 / V01–V24)

### P0 Csomag — Globális vizuális rendszer és közös komponenskönyvtár (`ui/theme/*`)
1. **Szín- és felületi hierarchia (32 paletta támogatásával):**
   - `bg` (fő háttér) -> `surface` (fejléc/alsó sáv) -> `card` (elsődleges szekciókártya, `18dp` rádiusz, `1dp borderSubtle`) -> `cardElevated` (kiemelt/hero kártya, `22dp` rádiusz, `1dp borderStrong`) -> `surface2` (belső metrika-csempe, chip, beviteli mező, `12–14dp` rádiusz).
   - **Tilos** a kártyák egymásba ágyazása azonos háttérszínnel: `card` belsejében mindig `surface2` vagy `cardElevated` áll.
2. **Tipográfia és számfegyelem:**
   - Minden óra, súly, ismétlés, RIR, távolság, tempó, pulzus, százalék és dátum kötelezően `fontFeatureSettings = "tnum"` (`tabular-nums`).
   - Egyetlen szekción belül legfeljebb 3 szövegszint: főcím/érték (`text`), másodlagos címke (`muted`), szemantikus/akcentus jelölés (`accentText` / `success` / `warning` / `danger` / `info`).
3. **Érintési célok és kis kijelző / nagy betű védelem:**
   - Minden interaktív elem minimum `48×48dp`, elsődleges CTA gombok `52–56dp` magasak.
   - Kétoszlopos rácsokban (`MetricTile`, `ActionCard`) tilos a fix magasságú üres doboz (`min = 100.dp`); helyette `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max))` + kártyánként `.weight(1f).fillMaxHeight()` használandó, így `1.0x`, `1.3x` és `2.0x` fontScale-en is pontosan egyforma magasak maradnak a szomszédos kártyák.
4. **Közös P0 építőelemek (`TrainPilotSurfaces.kt`):**
   - `TrainPilotHeroSurface` (opcionális bal oldali `3dp` fókuszcsíkkal: `accent` / `warning` / `info` / `success`),
   - `TrainPilotSectionSurface`, `TrainPilotActionSurface`, `TrainPilotSubtleSurface`,
   - `TrainPilotSectionHeader` (cím + opcionális alcím + jobb oldali szöveges akció-link `48dp` érintési céllal),
   - `TrainPilotStatusBadge`, `TrainPilotMetaPill`, `TrainPilotMetricTile`,
   - `TrainPilotInlineIssue` (egységes hiba/figyelmeztetés sáv újrapróbálás gombbal),
   - `TrainPilotEmptyState` (ikon badge + cím + 1–2 soros leírás + opcionális CTA).

---

### P1 Csomag — Az 5 Főoldal (`V01`–`V05`)

#### `V01. Kezdőlap` (`HomeScreen.kt`) — Mockup-kompozíció + a `main` funkciói
1. **Feltételes állapotkártyák (ha releváns):**
   - `OnboardingResumeCard` (`home_onboarding_resume`, bal oldali `3dp info/accent` csík) és `ImportedDraftCard` (`home_resume_draft`, bal oldali `3dp warning` csík).
2. **Mai edzés Hero (`TodayHeroCard`):**
   - `TrainPilotHeroSurface` bal oldali `3dp accent` csíkkal.
   - Felső sor: `TrainPilotStatusBadge` (`MAI EDZÉS` / `FOLYAMATBAN`) + aktív edzésnél élő státusz.
   - `home_program_name` (`22sp Bold`), alatta `home_day_name` (`15sp SemiBold`, `accentText`).
   - Meta-sor: `TrainPilotMetaPill` chipek (`X gyakorlat`, `~55 perc`, vagy `X befejezett sorozat`).
   - CTA: `home_start_workout` (`52dp` magas, teljes szélességű `accent` gomb `PlayArrow` ikonnal).
3. **Gyorsindító sor (`QuickStartRow` — külön szekciófejléc nélkül!):**
   - `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max))` 2 egyforma magas kártyával (`home_quick_workout`, `home_outdoor_workout`), `36×36dp` lekerekített ikon-badge-dzsel, `15sp SemiBold` címmel és 1 soros `12sp muted` alcímmel.
4. **Napi áttekintés 6 kártya (`TodayMetricsGrid`):**
   - `TrainPilotSectionHeader("NAPI ÁTTEKINTÉS")` + 3×2-es `IntrinsicSize.Max` rács (`home_metric_card_workouts`, `duration`, `recovery` + `home_recovery_card`, `sleep`, `weight`, `streak`).
   - Kompakt `12dp` belső padding; felső sorban kis ikon + nagybetűs címke (`11sp SemiBold muted`); középen érték (`20sp Bold tabular-nums`) és egység (`12sp muted`) közös alapvonalon (`Alignment.Bottom`); alul 1 soros szemantikus státusz. A `"recovery"` kártya a `NativeHomeRecoveryPresentation` hiteles adatát mutatja.
5. **Tervezés és edző (`NativePlanningShortcuts`):**
   - `TrainPilotSectionHeader("TERVEZÉS ÉS EDZŐ")` + 3 egyforma magas kártya (`shortcut_calendar`, `shortcut_coach`, `shortcut_programs`), amelyek `uiState.isLoading` alatt is a Compose-fában maradnak.

#### `V02. Edzés főoldal` (`TrainingLandingScreen.kt`) — Mockup-kompozíció + a `main` funkciói
1. **Aktív / folytatható edzés vagy piszkozat kártya (ha van):** `training_resume_active` / `training_resume_draft` (bal oldali `3dp accent/warning` csíkkal).
2. **Következő programnap Hero (`NextProgramDayHeroCard`):**
   - `TrainPilotHeroSurface` bal oldali `3dp accent` csíkkal, `KÖVETKEZŐ PROGRAMNAP` badge-dzsel, programnévvel (`13sp muted`), a nap nevével (`22sp Bold`), 1–2 soros gyakorlat-előnézettel (`Fekvenyomás · Ferde nyomás · Tolódzkodás`), `TrainPilotMetaPill` sorral és `52dp`-s `Edzés indítása` CTA-val.
3. **Gyorsindító kártyapár (`QuickActionsRow` — külön szekciófejléc nélkül!):**
   - 2 oszlopos `IntrinsicSize.Max` rács: `training_quick_workout` (`Gyors edzés`) és `training_outdoor_workout` (`Kültéri edzés`).
4. **Aktív program napjai szekció (`ProgramDaysSection`):**
   - Fejléc: `AKTÍV PROGRAM NAPJAI` + `Programok` (`training_open_programs`) és `Gyakorlatkönyvtár` (`training_open_exercises`) elérése.
   - Programnapok listája (`training_day_${day.id}`):
     - Bal: `36×36dp` lekerekített sorszám-badge (`1`, `2`, `3` — a következő napnál `accentSoft` + `accentText`, a többinél `cardElevated` + `muted`).
     - Közép: Nap címe (`16sp SemiBold`) + 1 soros gyakorlat-előnézet (`13sp muted`, `Ellipsis`) + gyakorlatszám.
     - Jobb: A következő napnál telített `accent` `Indítás` gomb, a többinél szekunder (`cardElevated` + `borderStrong`) `Indítás` gomb (`training_start_day_${day.id}`).

#### `V03. Napló` (`WorkoutHistoryScreen.kt`) — Mockup-szerkezet + a `main` összes funkciója
1. **Kereső + szűrő fej (`SearchAndFilterHeader`):**
   - `TrainPilotSectionSurface` keresőmező (`history_search_input`, `history_clear_search`) + vízszintes szűrő-chip sor (`history_filter_row`: `all`, `strength`, `outdoor`, `cardio`, `imported`) `accentSoft` aktív kiemeléssel.
2. **Összesítő metrikasáv:**
   - 3 oszlopos kompakt `TrainPilotSubtleSurface` sáv (`Edzések`: `totalCount`, `Idő`: összegzett perc, `Sorozatok`: összegzett sorozat) `tabular-nums` értékekkel.
3. **Strukturált edzéskártyák (`history_item_${workout.id}`) + lapozás (`history_load_more`):**
   - Felső sor: `36×36dp` típus-ikon badge + cím (`16sp SemiBold`) + dátum (`12sp muted`) + jobb oldalon max. 2 `TrainPilotStatusBadge` + `ChevronRight`.
   - `HorizontalDivider(color = colors.borderSubtle)` + 3–4 oszlopos `tabular-nums` metrikasor (Súlyzós: `Idő`, `Gyakorlatok`, `Sorozatok`, `Volumen`; Kültéri: `Idő`, `Táv`, `Tempó`, `GPS`).

#### `V04. Egészség` (`HealthDashboardScreen.kt`) — A `main` teljes, bevált felépítése P0 vizuális minőségben
- **Nem a vázlatos 4-kártyás mockupot követi**, hanem a `main` összes funkcióját és szekcióját megtartja:
  1. **Dátum- és szinkron-fejléc (`TrainPilotHeroSurface`):** `health_selected_day`, `health_day_prev`, `health_day_next`, `health_day_today`, forrás-jelvények (`Health Connect`, `Samsung Health`), `health_refresh_action`, `health_connect_action`, `health_samsung_action`, `health_add_sleep`, `health_add_weight`.
  2. **Napi egészség-csempék (`IntrinsicSize.Max` 2 oszlopos rács):** Regeneráció, Alvás, Nyugalmi pulzus / HRV, Lépések / Kalória, Testsúly (`TrainPilotSectionSurface` + `tabular-nums` + forrás-badge).
  3. **D06 Testsúlynapló (`health_weight_section`):** Időszak-chipek (`health_weight_range_*`), összegző sáv (`health_weight_summary`: legutóbbi, változás, átlag, min–max `tabular-nums`-szal), súlybejegyzés-sorok (`health_weight_row_*`) szerkesztés/törlés dialógusokkal.
  4. **Egyedi egészségrekordok (`health_records_section`) és D03–D05 részletpanel (`HealthRecordDetailSheet`):** Kategória-szűrők, lapozható rekordlista (`health_record_row_*`, `health_records_load_more`) és a típus-specifikus (alvás / súly / edzés / pulzus / lépés / kalória / HRV / SpO2 / VO2Max) `ModalBottomSheet` (`health_record_detail_sheet`).

#### `V05. Fejlődés` (`ProgressAnalyticsScreen.kt`) — A `main` teljes, bevált felépítése P0 vizuális minőségben
- **A `main` teljes tartalmát viszi tovább:**
  1. **Időszakválasztó (`progress_range_row`):** `7d`, `30d`, `90d`, `365d`, `Összes` (`progress_range_${days}`) kiemelt aktív chippel.
  2. **6 összegző metrikakártya (`SummaryCard`):** 3×2-es `IntrinsicSize.Max` rács (`TrainPilotSectionSurface`, `22sp Bold tabular-nums`).
  3. **Heti sorozatok (`WeeklyBarsCard`) és Edzéstípus-megoszlás (`DistributionCard`):** `TrainPilotSectionSurface` kártyák `8dp` magas lekerekített (`CircleShape`) `surface2` sávokkal és `accent` kitöltéssel, jobb oldalon `tabular-nums` számokkal.
  4. **Egyéni csúcsok (`ExercisePrCard`) és G10 Privát fotógaléria (`progress_photo_gallery`):** Strukturált PR-sorok kiemelt `1RM` badge-dzsel (`accentSoft` + `accentText`), valamint a privát edzésfotók vízszintes galériája (`progress_photo_item_*`) nagyított előnézeti dialógussal.

---

### P2 Csomag — Aktív edzésmódok és befejezés (`V06`–`V11`)

1. **Közös Aktív Edzés Fejrész (`SessionHero` — `V06`–`V10`):**
   - Bal oldalon: aktív edzéstípus badge + edzéscím (`18sp Bold`) + `active_workout_progress` (`Gyakorlat X/Y · Sorozat A/B`).
   - Jobb oldalon: élő összidő (`28sp Bold tabular-nums`, `accentText`) + `active_workout_pause` és `active_workout_finish` gomb.
   - Ha az óra-szinkron aktív vagy hiba van: kompakt `WatchSyncBanner` (`active_watch_banner`).
2. **`V06. Aktív súlyzós edzés`:**
   - **Gyakorlat-fejléc (`ExerciseHeader`):** gyakorlat neve (`20sp Bold`), célzott izomcsoport + eszköz badge, előző teljesítmény, jobb oldalon gyakorlatcsere (`active_swap_exercise`), előzmény és megjegyzés gomb.
   - **Sorozattábla (`SetRow`):** `#` (`28dp`), `ELŐZŐ` (`12sp muted tabular-nums`), `KG` (`surface2` mező, `18sp Bold tabular-nums`), `ISM.` (`surface2` mező), `RIR` (chip), és `48×48dp` pipa gomb (`active_complete_set_${index}`). Befejezett sorozat: `success` 12%-os háttér; aktív sorozat: `cardElevated` + bal oldali `3dp accent` sáv.
   - **G12 — Aktív privát fotósor:** Valódi korlátozott méretű (`72×72dp`, `inSampleSize` dekódolású) privát fotó-thumbnail (`PrivatePhotoStore`), hiányzó/sérült fájlnál kulturált fallback kártya, és **`48×48dp` érintési célú törlés gomb** (`active_delete_photo_${id}`).
   - **Alsó dokkolt `RestPanel` (G04 — Rest-only pause/resume):**
     - Egyetlen nagy visszaszámláló (`30sp Bold tabular-nums`), `+15 mp` / `+30 mp` (`active_rest_add`), **Pihenő szüneteltetése / folytatása (`active_rest_pause_resume` — csak a pihenőórát állítja meg, a `workoutPaused` nem változik!)**, és `Kihagyás` (`active_rest_skip`).
3. **`V07. Aktív egyoldali időalapú edzés`:**
   - `MeasurementCard` központi `48sp Bold tabular-nums` stopperrel (`active_uni_timer`), `Cél: 01:00` sávval, `Indítás / Szünet` (`active_uni_start_pause`), `+15 mp`, és `Sorozat rögzítése` (`active_uni_record`) gombbal.
4. **`V08. Aktív kétoldali időalapú edzés` (Bal / Jobb):**
   - Kétoszlopos oldalválasztó (`active_bi_side_left`, `active_bi_side_right`) az adott oldal mért idejével (`tabular-nums`) és státuszával (`Kész` / `Aktív` / `Hátravan`), központi stopperrel és `Pár rögzítése` (`active_bi_record_pair`) CTA-val.
5. **`V09. Aktív kültéri GPS edzés` (G05 + G06 bekötéssel):**
   - **G06 — Térkép kártya (`active_gps_map`):** Explicit beleegyezés (`mapTileConsent`) előtt zéró hálózati forgalom (offline `Canvas` poligon); beleegyezés esetén online térképcsempék + `+` / `-` zoom gombok (`48×48dp`) + `© OpenStreetMap` attribúció + offline fallback.
   - **G05 — Őszinte GPS-pontosság:** Élő `TrainPilotStatusBadge` a tényleges `accuracyMeters` értékkel (`GPS ±4 m`), vagy no-fix / stale esetén őszinte figyelmeztetés (**a `0` soha nem jelenhet meg hiteles fixként!**).
6. **`V10. Aktív intervallum / HIIT / Tabata edzés`:**
   - Fázis-hero (`MUNKA` / `PIHENŐ`), `Kör X / Y`, `52sp Bold tabular-nums` visszaszámláló, `Szünet`, `Következő fázis` (`active_hiit_skip_phase`) és `Befejezés`.
7. **`V11. Edzés befejezése és összegzés modal` (G07 bekötéssel):**
   - 2×2-es összegző metrikacsoport, Effort / RPE választó chipek (`finish_effort_${level}`).
   - **G07 — Valódi megjegyzés és fájdalom/érzet tartós mentése:** `finish_notes_input` és fájdalom-részletek bekötve a finalizerbe; explicit `Edzés mentése` CTA (`finish_confirm_save`), amely csak a writer nyugtája után jelez sikert.

---

### P3 Csomag — Programok, Gyakorlatkönyvtár, Naptár és Szerkesztők (`V12`–`V15`)

1. **`V12. Programok és edzésmódok` (`TrainingProgramsScreen.kt` + A07):**
   - `NativeTrainingModePicker` (`training_modes_program_dialog`, `training_modes_program_approve`), Aktív program Hero + Programlista (`program_card_${id}`).
   - **A07 — Globális alapértelmezések visszaállítása:** Explicit előnézet és megerősítő dialógus (`reset_defaults_confirm`), amely CAS/tranzakcióban állítja vissza a beépített sablonokat úgy, hogy a felhasználó egyedi gyakorlatait, egyedi programjait és a programokból/kedvencekből hivatkozott elemeket megvédi.
2. **`V13. Gyakorlatkönyvtár és részletek` (`ExerciseLibraryScreen.kt` + A12 + G03):**
   - Kereső + szűrő-chip sor **`★ Kedvencek` (`exercise_filter_favorites`)** szűrővel, csillag gombbal (`exercise_favorite_${id}`).
   - **G03 — Inline Quick indítás a részletpanelből:** Közvetlen `Gyors edzés indítása ezzel` / `Hozzáadás` gomb (`exercise_detail_quick_start`) inline konfigurációval és az összes meglévő session-guarddal.
3. **`V14. Naptár és ütemezés` (`TrainingCalendarScreen.kt`):**
   - Hónap-fejléc, 7 oszlopos `tabular-nums` naprács színkódolt pont-indikátorokkal, kiválasztott nap részletkártyája (`calendar_selected_day_card`).
4. **`V15. Program- és gyakorlatszerkesztő`:**
   - Strukturált `FormSection` kártyák (`surface2` mezők), cél-sorozat/ismétlés/RIR/pihenőidő szerkesztése (`tabular-nums`), alsó rögzített `Mégse` + `Mentés` sáv.

---

### P4 Csomag — Naplórészlet, Egészségrészlet és Coach (`V16`–`V19`)

1. **`V16. Edzésnapló részletek` (`WorkoutHistoryDetailScreen.kt`):**
   - 4 oszlopos összegző sáv, `Gyakorlatok` | `Statisztikák` | `Forrás és megjegyzés` fülek, GPS térkép zöld rajtponttal és **piros `X` végponttal**, privát fotók és sorozattáblák (`tabular-nums`).
2. **`V17. Egészségnapló részletek` (`HealthRecordDetailSheet` — D03–D05):**
   - Típus-specifikus részletpanel minden kategóriára + 26 ismert szolgáltatói csomagnév (`NativeProviderPackageLabels`).
3. **`V18. Testsúly-analitika részletek` (D06):**
   - Időszak-szűrők, 4-es összegző metrikasáv (`Legutóbbi`, `Változás`, `Átlag`, `Min–Max`), súlylista és szerkesztő.
4. **`V19. Coach és AI elemzés` (`CoachScreen.kt`):**
   - `Readiness` Hero (`32sp Bold tabular-nums`) + 2×3 jelzés-rács, javasolt programnap (`coach_start_workout`), gyakorlatszintű javaslatok (`coach_advice_${index}`) és variáció-előnézet (`coach_variation_save` — csak explicit másolatot ment).

---

### P5 Csomag — Beállítások, Óra és szinkron, Mentés, Engedélyek, Privát fotók (`V20`–`V24`)

1. **`V20. Globális Beállítások` (`SettingsScreen.kt`):** Profil, 32 paletta (`settings_theme_${id}`), 4 nyelv (`settings_lang_${code}`), aloldal-navigáció.
2. **`V21. Óra és szinkron` (`WatchSyncSettingsScreen.kt` — G02):** Kitalált óramodell vagy akkumulátor-százalék tilos; csak a hiteles kapcsolatállapot, szinkronidő, üzenetsor és edzés-birtoklás (`watch_takeover` / `watch_handback`) jelenik meg.
3. **`V22. Mentés és visszaállítás` (`BackupRestoreScreen.kt`):** ZIP export (`backup_export`), visszaállítás előnézettel (`backup_import`), `LegacyWebViewMigrator` státusz.
4. **`V23. Engedélyek és integrációk` (`PermissionsSettingsScreen.kt`):** Health Connect, Samsung Health, GPS, Értesítések és Térképcsempe hozzájárulás kártyák.
5. **`V24. Privát fotók és helyi adatvédelem` (`PrivatePhotosSettingsScreen.kt`):** `PrivatePhotoStore` tárhely-összesítő, árva fájlok tisztítása, összes privát fotó törlése.

---

## III. RÉSZ — BEMÁSOLÁSRA KÉSZ KÓDJAVÍTÁSOK (.KT FÁJLOK)

### 1. FÁJL: `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeLocalizationEmulatorFixture.kt`
Cseréld le a `close` metódust erre (megakadályozza a `SlotWriter` `ArrayIndexOutOfBoundsException` crasht Activity-lebontáskor):

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
                unit = if (recoveryScore != null) "%" else "",
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
                unit = if (sleepMinutes != null) tr(language, "óra", "h", "Std", "h") else "",
                status = when {
                    sleepMinutes == null -> tr(language, "Egészségnapló", "Health log", "Gesundheitslog", "Jurnal sănătate")
                    sleepMinutes >= 420 -> tr(language, "Optimális", "Optimal", "Optimal", "Optim")
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

    private fun formatHours(minutes: Int): String {
        val hours = minutes / 60.0
        return String.format(java.util.Locale.US, "%.1f", hours)
    }

    private fun formatOneDecimal(value: Double): String {
        return String.format(java.util.Locale.US, "%.1f", value)
    }

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

~~~kotlin
package com.repforge.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.repforge.app.ui.theme.TrainPilotMetaPill
import com.repforge.app.ui.theme.TrainPilotSectionHeader
import com.repforge.app.ui.theme.TrainPilotSectionSurface
import com.repforge.app.ui.theme.TrainPilotStatusBadge
import com.repforge.app.ui.theme.TrainPilotTheme
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayHeroCard(
    uiState: HomeUiState,
    language: String,
    busy: Boolean,
    onStartOrContinue: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val badgeLabel = when {
        uiState.hasActiveWorkout -> tr(language, "FOLYAMATBAN", "IN PROGRESS", "AKTIV", "ÎN CURS")
        else -> tr(language, "MAI EDZÉS", "TODAY'S WORKOUT", "HEUTIGES WORKOUT", "ANTRENAMENTUL DE AZI")
    }
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
        else -> tr(language, "Válassz programot vagy indíts gyors edzést", "Select a program or start a quick workout", "Programm wählen oder Schnelltraining starten", "Alege un program sau începe un antrenament rapid")
    }
    val primaryLabel = when {
        uiState.isFinishingWorkout -> tr(language, "Mentés folyamatban…", "Saving workout…", "Workout wird gespeichert…", "Se salvează antrenamentul…")
        uiState.hasActiveWorkout -> tr(language, "Edzés folytatása", "Continue Workout", "Training fortsetzen", "Continuă antrenamentul")
        else -> tr(language, "Edzés indítása", "Start Workout", "Training starten", "Începe antrenamentul")
    }

    TrainPilotHeroSurface(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRect(
                    color = colors.accent,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height)
                )
            }
    ) {
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("home_program_name")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = dayTitle,
            color = if (uiState.hasActiveWorkout) colors.accentText else colors.muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("home_day_name")
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.hasActiveWorkout) {
                TrainPilotMetaPill(
                    text = tr(
                        language,
                        "Aktív munkamenet",
                        "Live session",
                        "Aktive Einheit",
                        "Sesiune activă"
                    )
                )
            } else if (uiState.nextDayExerciseCount > 0) {
                TrainPilotMetaPill(
                    text = tr(
                        language,
                        "${uiState.nextDayExerciseCount} gyakorlat",
                        "${uiState.nextDayExerciseCount} exercises",
                        "${uiState.nextDayExerciseCount} Übungen",
                        "${uiState.nextDayExerciseCount} exerciții"
                    )
                )
            }
            TrainPilotMetaPill(
                text = tr(
                    language,
                    "${uiState.totalCompletedWorkouts} naplózott edzés",
                    "${uiState.totalCompletedWorkouts} logged workouts",
                    "${uiState.totalCompletedWorkouts} gespeicherte Workouts",
                    "${uiState.totalCompletedWorkouts} antrenamente salvate"
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onStartOrContinue,
            enabled = !busy && (uiState.hasActiveWorkout || uiState.nextDayId.isNotBlank()),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = Color.White,
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
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    val title = tr(language, "NAPI ÁTTEKINTÉS", "TODAY OVERVIEW", "TAGESÜBERSICHT", "REZUMAT ZILNIC")

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
            Text(
                text = metric.label,
                color = colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

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
private fun OnboardingResumeCard(
    language: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val title = tr(language, "Edzésprofil befejezése", "Complete training profile", "Trainingsprofil abschließen", "Finalizează profilul de antrenament")
    val body = tr(language, "Állítsd be a céljaidat és a heti edzésszámot a pontosabb ajánlásokhoz.", "Set your goals and weekly schedule for tailored guidance.", "Ziele und Wochenplan für präzisere Empfehlungen festlegen.", "Setează obiectivele și frecvența săptămânală.")
    val action = tr(language, "Folytatás", "Continue", "Fortsetzen", "Continuă")

    TrainPilotSectionSurface(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = body, color = colors.muted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("home_onboarding_resume")
        ) {
            Text(text = action, fontWeight = FontWeight.SemiBold)
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
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("home_resume_draft")
        ) {
            Text(text = action, fontWeight = FontWeight.SemiBold)
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

~~~kotlin
package com.repforge.app.ui.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.repforge.app.ui.theme.TrainPilotMetaPill
import com.repforge.app.ui.theme.TrainPilotSectionHeader
import com.repforge.app.ui.theme.TrainPilotSectionSurface
import com.repforge.app.ui.theme.TrainPilotStatusBadge
import com.repforge.app.ui.theme.TrainPilotTheme
import kotlinx.coroutines.launch

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
        "en" -> "ACTIVE PROGRAM DAYS"
        "de" -> "AKTIVE PROGRAMMTAGE"
        "ro" -> "ZILELE PROGRAMULUI ACTIV"
        else -> "AKTÍV PROGRAM NAPJAI"
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

            val nextDay = uiState.days.firstOrNull { it.id == uiState.nextProgramDayId }
                ?: uiState.days.firstOrNull()
            if (nextDay != null && !uiState.hasActiveWorkout) {
                item {
                    NextProgramDayHeroCard(
                        programName = uiState.activeProgramName,
                        day = nextDay,
                        language = language,
                        enabled = !busy,
                        onStart = {
                            if (busy) return@NextProgramDayHeroCard
                            launchingDayId = nextDay.id
                            scope.launch {
                                try {
                                    val result = WorkoutLauncher.startProgramDay(context, nextDay.id)
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

            item {
                QuickActionsRow(
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
                val highlightedId = nextDay?.id
                itemsIndexed(uiState.days, key = { _, d -> d.id }) { index, day ->
                    ProgramDayRow(
                        index = index + 1,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NextProgramDayHeroCard(
    programName: String,
    day: ProgramDaySummaryUiModel,
    language: String,
    enabled: Boolean,
    onStart: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val badge = when (language) {
        "en" -> "NEXT PROGRAM DAY"
        "de" -> "NÄCHSTER PROGRAMMTAG"
        "ro" -> "URMĂTOAREA ZI DE PROGRAM"
        else -> "KÖVETKEZŐ PROGRAMNAP"
    }
    val startText = when (language) {
        "en" -> "Start Program Day"
        "de" -> "Programmtag starten"
        "ro" -> "Începe ziua de program"
        else -> "Programnap indítása"
    }
    val exLabel = when (language) {
        "en" -> "${day.exerciseCount} exercises"
        "de" -> "${day.exerciseCount} Übungen"
        "ro" -> "${day.exerciseCount} exerciții"
        else -> "${day.exerciseCount} gyakorlat"
    }

    TrainPilotHeroSurface(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRect(
                    color = colors.accent,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height)
                )
            }
    ) {
        TrainPilotStatusBadge(
            text = badge,
            containerColor = colors.accentSoft,
            contentColor = colors.accentText,
            icon = Icons.Default.FitnessCenter
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (programName.isNotBlank()) {
            Text(
                text = programName,
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = day.title,
            color = colors.text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrainPilotMetaPill(text = exLabel)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onStart,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = Color.White,
                disabledContainerColor = colors.surface2,
                disabledContentColor = colors.muted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = startText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
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

    TrainPilotHeroSurface(modifier = Modifier.fillMaxWidth()) {
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
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
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
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("training_resume_draft")
        ) {
            Text(text = action, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickActionsRow(
    language: String,
    enabled: Boolean,
    onQuickWorkout: (Boolean) -> Unit
) {
    val colors = TrainPilotTheme.colors
    val spacing = TrainPilotTheme.spacing
    val quickLabel = when (language) {
        "en" -> "Quick Workout"
        "de" -> "Schnelltraining"
        "ro" -> "Antrenament rapid"
        else -> "Gyors edzés"
    }
    val quickSub = when (language) {
        "en" -> "Custom session"
        "de" -> "Freie Einheit"
        "ro" -> "Sesiune liberă"
        else -> "Szabad gyakorlatválasztás"
    }
    val outdoorLabel = when (language) {
        "en" -> "Outdoor GPS"
        "de" -> "Outdoor-GPS"
        "ro" -> "Exterior GPS"
        else -> "Kültéri GPS"
    }
    val outdoorSub = when (language) {
        "en" -> "Route & pace"
        "de" -> "Route & Tempo"
        "ro" -> "Traseu & ritm"
        else -> "Útvonal és tempó"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(spacing.gridGap)
    ) {
        TrainPilotActionSurface(
            onClick = { onQuickWorkout(false) },
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("training_quick_workout")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = quickLabel,
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
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("training_outdoor_workout")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = outdoorLabel,
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
private fun PlanningToolsRow(
    language: String,
    enabled: Boolean,
    onOpenPrograms: () -> Unit,
    onOpenExercises: () -> Unit
) {
    val colors = TrainPilotTheme.colors
    val spacing = TrainPilotTheme.spacing
    val programsLabel = when (language) {
        "en" -> "Programs"
        "de" -> "Programme"
        "ro" -> "Programe"
        else -> "Programok"
    }
    val programsSub = when (language) {
        "en" -> "Templates & modes"
        "de" -> "Vorlagen & Modi"
        "ro" -> "Șabloane & moduri"
        else -> "Sablonok és edzésmódok"
    }
    val exercisesLabel = when (language) {
        "en" -> "Exercise Library"
        "de" -> "Übungsbibliothek"
        "ro" -> "Bibliotecă exerciții"
        else -> "Gyakorlatkönyvtár"
    }
    val exercisesSub = when (language) {
        "en" -> "Search & favorites"
        "de" -> "Suche & Favoriten"
        "ro" -> "Căutare & favorite"
        else -> "Keresés és kedvencek"
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
            Text(text = programsLabel, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = programsSub, color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TrainPilotActionSurface(
            onClick = onOpenExercises,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("training_open_exercises")
        ) {
            Text(text = exercisesLabel, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = exercisesSub, color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            .testTag("training_day_${day.id}"),
        contentPadding = 14.dp
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isNextDay) colors.accentSoft else colors.cardElevated)
                        .border(
                            width = 1.dp,
                            color = if (isNextDay) colors.accent.copy(alpha = 0.45f) else colors.borderSubtle,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        color = if (isNextDay) colors.accentText else colors.muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = day.title,
                        color = colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countLabel,
                        color = colors.muted,
                        fontSize = 12.sp,
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
                    contentColor = if (isNextDay) Color.White else colors.text,
                    disabledContainerColor = colors.surface2,
                    disabledContentColor = colors.muted
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("training_start_day_${day.id}")
            ) {
                Text(text = startLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
~~~