# Külön chat B – edzésstílusok és valódi progressziós módok

## Kötelező munkakeret

- Repo: fuloplevente1998/TrainPilot. Bázis **0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20** a
  feat/native-android-migration ágról. App/tesztforrás880026f; benne a Home/HC,
  #243, javított Gemini-kardiómag és a programeditor IO-guard. Ne mainből vagy
  a régi9a18ca6/1803892 bázisból indulj.
- Olvasd el: AGENTS.md, docs/NATIVE_RESUME.md, docs/WORK_HANDOFF.md teteje,
  a feladathoz megadott konkrét források/reviewk. A teljes exportot ne töltsd be
  szükségtelenül. Meglévő canonical com.repforge.app kódot használj;
  com.repforge prototípusok/hamis szolgáltatók nem köthetők be.
- Csak az alább megadott új saját ág írható. Main és migrációs ág írása, merge,
  force push, issue/PR-lezárás tiltott. Ez a konkrét külső részfeladat kivétel
  az AGENTS fő migrációs munkására vonatkozó ágkorlátozás alól.
- Verzió/build/package változatlan:1.3.71-native-test /2736 /com.repforge.app.
  Nincs új workflow, függőség/toolchain/séma önkényes cseréje vagy új adatmotor.
- Kis `[skip ci]` commitok, push a saját ágra, **draft PR célja
  feat/native-android-migration**. A meglévő natív workflow/közös CI a fő
  munkásnál marad; helyben nincs Gradle, APK vagy emulátor, ne indíts CI-t.
- Funkció+érdemi JVM/Compose UI-tesztkód készüljön. Új külön tesztfájlokat írj;
  a közös NativePhoneNavigationTest-et és a másik munkás tesztjeit ne módosítsd.
  Meglévő tesztet ne gyengíts működési hiba elfedésére. Statikus és tényleges
  futási eredmény külön: jelen bázis580 JVM+52 UI új kódja NEM FUTOTT, a régi
  dd4cc27 zöld kapu nem az új változtatásod igazolása.
- HU/EN/DE/RO feliratok új, feladatspecifikus kis XML-fájlokban, a nagy strings.xml
  változtatása nélkül. Meglévő tématokenek/32 paletta/48dp és nyelvi fallback;
  nincs kitalált mérés, mintaadat a termékben vagy hardcoded téma.
- Más munkás írási területére ne nyúlj. Ha további meglévő fájl szükséges,
  a határon túli javaslatot külön diffként add át, ne írj másik ágra és ne hagyj
  késznek jelölt, bekötetlen gombot/placeholdert.
- Átadás: exact SHA/PR, fájllista, tényleges main-hívásiút és elfogadási pontok,
  megírt/futtatott ellenőrzések külön, korlátok. Nincs automatikus issue-close.
  Magyarul röviden kommunikálj. Javasolt High; komoly debugnál jelezd a szükségét.

## Feladat és saját ág

Ág: **feat/native-training-modes**.
A mainben már megvalósult, a natív migrációból még hiányzó trainingModes197
viselkedést kösd be teljesen. A #197 mainen lezárt; nem újranyitandó és nem
újra lezárandó. A feladat a main adott funkciójának natív paritása.
Olvasd el az issue tényleges szövegét és a pinned app.js VÉGSŐ felülírásait.
HIT stílus és progressziós szabály külön fogalom; nem Wear szenzorjavítás!

### Elfogadási feltételek

1. Normál/HIT-inspirált stílus és dupla/lineáris/RIR progresszió; globális
   választás +programoverride. Pontos main schemaVersion1/defaults/byProgram/
   explicitChoice/revision/undo formátum, legacy és malformed konzervatív
   fallback. Ne csak dropdown és eltárolt szöveg készüljön.
2. Forrásutak: tp197TrainingModes, tp197ComputePreview, tp197ExercisePreview,
   tp197UiPreview/Apply/Undo és a VÉGSŐ tp197CoachInfo/Allowed/Request/Apply
   bekötések. Keresd meg a későbbi azonos név-felülírásokat is.
3. Módválasztás preview/explicit jóváhagyás és korlátozott egyszeri undo.
   A módbeállítás mentése kizárólag settings-változás: nem írhatja át automatikusan
   a programreceptet, múltbeli naplót, importált/native draftot vagy aktív frame-et.
4. Valódi Coach-ajánlás: kiválasztott szabály hatása ténylegesen jelenjen meg,
   érthető indokkal. Súlyalkalmazás külön explicit preview/jóváhagyás a main
   szerint; aktuális workout/generation/owner epoch/exercise/set/config/előzmény
   frissességét ellenőrizd. Csak engedett, még nem szerkesztett/teljesített célra,
   a meglévő NativeWorkoutEditing/kanonikus motor útján. Nincs autoApply.
5. Hiányzó RIR, fájdalom/rossz readiness, equipment/step/terheléshatár,
   bodyweight/time/distance és HIT összetett gyakorlat konzervatív védelmei.
   Nincs automatikus bukásig erőltetés vagy későbbi/haladó mód kitalálása.
6. RIR tényleges0..10 és missing≠0 megmarad. Legacy mód változatlan meglévő
   NativeCoachExerciseAdvice számítást használjon; ne legyen két versengő motor.
7. Elérhető globális/program UI és aktív edzés ajánlási művelet, négy nyelv,
   normál tématokenek. Settings/backup/restore oda-vissza őrizze a konfigurációt
   és ismeretlen mezőket. A meglévő napló/photo/route/Wear invariánsok maradnak.
8. Érdemi tesztkód: módhatások, legacy/malformed fallback, override, explicit
   preview/cancel/approve/undo, stale config és owner/frame, done/user-edited
   cél tiltása, missing RIR/pain/readiness, portable roundtrip, valódi UI-flow.

### Meglévő írható fájlok

A canonical com.repforge.app alatt:

- data/store/TrainPilotSettingsStore.kt (mode JSON/preferences, nem időzítőátírás).
- data/migration/LegacyWebViewMigrator.kt KIZÁRÓLAG settings mode import/export;
  shared workoutJson/journal/photo/restore kódhoz ne nyúlj.
- ui/settings/SettingsScreen.kt, SettingsViewModel.kt.
- ui/programs/TrainingProgramsScreen.kt, TrainingProgramsViewModel.kt.
- ui/coach/NativeCoachExerciseAdvice.kt, NativeCoachAdviceRepository.kt,
  NativeCoachAdviceText.kt (legacy path megőrzéssel).
- ui/workout/ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt és
  NativeWorkoutEditing.kt: csak explicit proposal/approval bekötés és őrök,
  nem ownership/timer/undo-188/finish algoritmus újraírása.

Új mode helper/dialog/policy fájlok és külön native_training_modes.xml négy
nyelven; új NativeTrainingModesTest/NativeTrainingModesUiTest osztályok.
Más meglévő fájl, különösen DAO/Entities/Progress/Health/Home/Journal,
NativeProgramEditor/Prescription és finalizer/ownership/Wear nem írható.
Szükséges további hookot külön patchben adj, ne telepíts finish wrappert.


## Rögzített forráslinkek

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/AGENTS.md)
- [docs/NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/NATIVE_RESUME.md)
- [docs/WORK_HANDOFF.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/WORK_HANDOFF.md)
- [docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md)
- [android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachExerciseAdvice.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachExerciseAdvice.kt)
- [android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachAdviceRepository.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/coach/NativeCoachAdviceRepository.kt)
- [android/app/src/main/java/com/repforge/app/NativeWorkoutEditing.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/NativeWorkoutEditing.kt)
- [android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt)
- [android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt)
- [android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt)
- [Main – végleges app.js](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js)
- [Issue197 – eredeti követelmények és lezárási megjegyzés](https://github.com/fuloplevente1998/TrainPilot/issues/197)
