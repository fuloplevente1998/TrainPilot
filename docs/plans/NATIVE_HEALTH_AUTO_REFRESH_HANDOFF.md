# Külön chat C – automatikus foreground egészségfrissítés

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

Ág: **feat/native-health-auto-refresh**.
A main autoOnOpen működését kösd be a natív Egészség felülethez. Ez valódi
providerfrissítés, nem csak Room/SQLite újraolvasás. A Home helyi re-read
(e915bd9) és a Journal explicit workout HC-refresh (0a69035) már kész kód,
azokat ne írd újra és ne tekintsd az autoOnOpen megoldásának.

### Elfogadási feltételek

1. A HealthJournalStore meglévő preferences().autoOnOpen kulcsát és
   setPreferences útját használd. Van már tartós kulcs/export, defaultfalse;
   ne hozz létre második DataStore/preference/store mezőt ugyanarra.
2. Elérhető HU/EN/DE/RO kapcsoló az Egészség felületen. Bekapcsolt állapotban
   megfelelő foreground/on-open eseménynél valós providerfrissítés a meglévő
   NativeHealthRefresh/HealthConnectRepository/SamsungHealthRepository útján.
3. Kövesd az app.js végső auto()/rf250Sync útját: opt-in,10perces automatikus
   throttle, in-flight dedupe, nincs auto olvasás aktív/paused session, ownership/
   finalization/restore/backup védelem mellett. Engedély nélkül nincs kéretlen
   rendszerablak, hamis siker vagy adattörlés; tárolt adat látható marad.
4. Manuális frissítés nem10perces tiltás, de ne fusson párhuzamosan ugyanaz az
   SDK-művelet. Tartsd meg a manuális Samsung-permission folyamatot. A selected
   historical day/filter ne ugorjon át másik napra a háttéreredménytől.
5. A képernyő lifecycle-ja is számít, nem csak a MainActivity RESUMED állapota:
   elnavigálás/rotation/process-recreation, provider- és napváltás, revoke/offline/
   részleges grant esetén nincs régi válasz/new datasetbe írás vagy új dialógus.
6. Provider IPC/permission várakozás ne tartson hosszú metadata mutexet vagy
   Room/snapshot lockot. Rövid prepare +dataset/health generation ticket és
   őrzött commit a meglévő journalba. Restore alatt indult/régi providereredmény
   nem írhatja vissza a lecserélt adatokat; a provider saját belső write pontjait
   is ellenőrizd, nem elég a válasz utáni UI-token.
7. Ne vezess be új háttérszolgáltatást/workert/SDK-klienst. A valódi main sync
   hatását egyeztesd az elérhető natív provider/journal funkciókkal; az esetleg
   hiányzó inkrementális/full-sync paritást külön jelezd, ne nevezd késznek egy
   lokális reload vagy üres callback alapján.
8. Érdemi tesztkód: opt-out/opt-in, első megnyitás,10perc és manuális bypass,
   párhuzamos trigger, no permission/partial/failure, aktív session/restore,
   late response/generation/provider/date váltás, valódi kapcsoló és lifecycle.
   Szimulált SDK-válasz nem fizikai Samsung/HC-siker; külön jelöld.

### Meglévő írható fájlok

A canonical com.repforge.app alatt:

- ui/health/HealthDashboardScreen.kt, HealthDashboardViewModel.kt.
- NativeHealthRefresh.kt: meglévő readDay/Today koordináció és biztonság,
  nem új párhuzamos frissítőmotor.
- HealthConnectRepository.kt és SamsungHealthRepository.kt CSAK ha a napi/
  journal read foreground/epoch-fence biztosításához szükséges. Meglévő hívások
  forráskompatibilisek maradjanak; readTrainingWindow/writeWorkout ne változzon.

Új kis coordinator/gate helper, külön native_health_auto_refresh.xml négy
nyelven, új NativeHealthAutoRefreshTest/NativeHealthAutoRefreshUiTest.
HealthJournalStore.kt csak olvasható: a meglévő prefs/generation/write guard
szerződését használd, séma/record paging/backup formátum nem változhat.
Más meglévő fájl (különösen SettingsStore/Screen, Legacy mapper, Progress,
Coach, workout/Journal/Home, NativeWorkoutHealth, NativeHealthRecords és
HealthRecordBrowser) nem írható. Határon túli szükséges diff külön átadás.


## Rögzített forráslinkek

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/AGENTS.md)
- [docs/NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/NATIVE_RESUME.md)
- [docs/WORK_HANDOFF.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/WORK_HANDOFF.md)
- [docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md)
- [android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt)
- [android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt)
- [android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/NativeHealthRefresh.kt)
- [android/app/src/main/java/com/repforge/app/NativeWorkoutHealth.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/NativeWorkoutHealth.kt)
- [android/app/src/main/java/com/repforge/app/NativeDataReplacement.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/NativeDataReplacement.kt)
- [android/app/src/main/java/com/repforge/app/HealthJournalStore.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/HealthJournalStore.kt)
- [android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/HealthConnectRepository.kt)
- [android/app/src/main/java/com/repforge/app/SamsungHealthRepository.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/SamsungHealthRepository.kt)
- [Main – végleges app.js](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js)
