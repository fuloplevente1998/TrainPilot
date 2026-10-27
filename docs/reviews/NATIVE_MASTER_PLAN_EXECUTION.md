# Native master plan – végrehajtás és forrásillesztés

Bázis: 937e66099da37bfa12ac7d48cf54660199d9bc8f, feat/native-android-migration.
Felhasználói sorrend: 1 stabilitás/V01–V02 → zöld ellenőrzés → 2 V03–V05 →
3 P2 → P3 → P4 → P5, csomagonként ellenőrzés. A mellékelt eredeti terv változatlanul:
`docs/plans/NATIVE_MASTER_PLAN_AND_FIXES.md`. A forráskódblokkok bemeneti javaslatok;
a tényleges API-hoz igazítás szükséges, funkciót vagy guardot nem törölhetnek.

## Ellenőrzött eltérések a javaslat és a repository között

- NativeHomeRepository valós helye ui/home, osztály és bounded Room snapshot.
  Nem data/repository objektum; data/local/TrainPilotDatabase és WorkoutLauncher
  importok, TrainingLandingViewModel, HomeMetricCardUiModel nem a jelenlegi API-k.
- A valós HomeViewModel már NativeHomeRecoveryPresentation.select-et hív;
  nincs Home-ból NativeCoachEngine.load és nincs loadTodayMetrics/rebuildState.
  E két fájl megtartva, a korábbi bounded/epoch/restore admission megmarad.
  A feltételezett 10× gyorsulás nincs mérve és nem állítjuk.
- A jelenlegi hat Home-metrika sleep/calories/pulse/steps/workouts/recovery.
  Nem cserélhető duration/weight/streak új listára a meglévők elvesztésével.
- A valós oldalak JournalScreen.kt és ProgressScreen.kt; WorkoutHistoryScreen és
  ProgressAnalyticsScreen néven nem készül párhuzamos implementáció.
- A training mode elutasítás valódi resource-ja native_modes_error, aktuális
  appnyelvvel. A tervben native_modes_compare_stale/hu nem az itteni szerződés.
- A quick/planning entry már loading alatt is létezik. Ezt megtartjuk; a két
  hibás tesztcsalád közvetlen offscreen kattintását tényleges scroll előzi meg.
- A runner-crash pontos oka még nem bizonyított. A main-looper/Compose idle
  szinkronizálás forrásjavítási jelölt, amíg a teljes futás nem igazolja.

## 1. lépés – első javítási jelölt

- Locale calendar/coach scroll és valódi navigation_back ellenőrzés;
  Coach variation tesztben szintén valódi görgetés. Assertion nem gyengült.
- Fixture close előtt main idle; mode approval elutasítás és teardown előtt
  Compose idle. Metric layout teszt teardown előtt is Compose idle.
- Közös Hero/Section/Subtle surface és StatusBadge, meglévő palettából.
- Home: meglévő state/callbackok, óra és minden metrika/forrás/hiba megmarad;
  nincs redundáns Quick cím; quick és metric párok intrinsic/equal height,
  nagy fontnál egymás alatti layout; quick műveleteknél busy admission.
- Training: ugyanazok a start/quick/library/planning callbackok, kompakt intrinsic
  quick gombpár (nagy betűnél egymás alatt), közös felületek/48–52dp/testTag-ek.
  Nincs kitalált következő nap, új launcher vagy második VM/session motor.
- Statikus ellenőrzés: módosított Kotlin-fájlok meglévő testTag-jei és UI @Test
  metódusai megmaradtak. Ez nem Android-fordítás vagy UI PASS.

Állapot: forrás kész, CI/JVM/UI még nem futott ezen a jelölten. Az 1. lépés nem
lezárt, a 2–3. lépés még nem kezdődött. Main/verzió/adat/PR-merge változatlan.
