# Külön chat A – kardió teljes adat- és felületi bekötése

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

Ág: **feat/native-cardio-progress-ui**.
A már javított NativeCardioModel magot kösd a valódi Room-naplóhoz és a
Fejlődés felülethez. Nem új számítási rendszert kérünk. A math magot és annak
15 JVM-regresszióját most ne írd át; eltérést külön jelezz.

### Elfogadási feltételek

1. Lapozott, könnyű Room-projekció és IO-adapter: minden releváns mentett
   edzés/sorozat teljes adathalmazán működjön, ne csak az utolsó32/100 soron.
   Törlések/tombstone-kizárás, stabil nyers DB keyset és cancellation.
2. Az addPage csak aggregál; az adapter saját DAO-sorrendjének utolsó nyers
   sorával lépjen akkor is, ha a modell kihagyott minden sort. Ne feltételezd,
   hogy a régi strength trend started-kurzora megegyezik a finished-alapú kardióval.
3. Add át a valós workout/set/route/source adatokat és a teljes exercise összes
   setjét (undone testvérsetet is) összegző exerciseHasDistance/Positive flags-t.
   Seconds = tényleges set.reps, nem a teljes edzés durationje vagy Wear ideje.
   Nincs lépésekből becsült GPS-táv vagy route ID-ből kitalált mérési forrás.
4. Ne tölts teljes history gráfot/fotókat/GPS-pontokat. Ha source-meta csak raw
   JSON-ban van, legfeljebb korlátozott, lehetőleg egy szülőt tartó cache/scalar
   lookup; ne duplikáld egy nagy workout raw JSON-ját minden setsor mellé.
   SQLite JSON1 támogatását ne feltételezd igazolás nélkül; séma nem módosulhat.
5. Fejlődésen elérhető Kardió rész: aktivitásválasztó, összesített/heti táv és
   idő, súlyozott tempó, nyolchetes valódi grafikon, azonos távú rekordok,
   time-only/üres/hiba állapotok. Az erő/1RM/izomcsoport/testsúly nézet maradjon.
6. Adatváltozáskor frissüljön azonos darabszám mellett is; ne count-cache legyen.
   Nyelv/aktivitásváltáskor régi IO-válasz ne írja felül az új kiválasztást.
   Emberi nevek HU-fallbackját a közös nativeLocalizedName alapján kezeld;
   modellben az ID-re visszaesett címke nem automatikusan valódi fordítás.
7. JVM-kód:0/200/1500, több lap, legrégebbi rekord, deleted/source/time-only,
   testvérset másik oldalon, hibás/jövőbeli sor és haladó cursor, same-count edit.
   Valódi UI-kód: elérés/aktivitásváltás/adatfrissülés, meglévő strength megmarad,
   HU/EN/DE/RO/téma, nem generált mockup. Teljes APK/gate most nem fut.

### Meglévő írható fájlok

- ui/progress/NativeProgressQuery.kt, ProgressViewModel.kt, ProgressScreen.kt
  a canonical android/app/src/main/java/com/repforge/app alatt.
- data/db/TrainPilotDaos.kt: kizárólag az új kardióprojekció/lekérdezések.
- data/db/TrainPilotEntities.kt: csak új nem-@Entity projekciós DTO-k.

Új fájlok lehetnek ui/progress alatt adapter/panel formában, új
native_cardio_progress.xml négy locale-ban, NativeCardioBindingTest és
NativeCardioProgressUiTest. Más meglévő fájl – különösen settings, Coach,
Health, program/mode, finalizer, mapper, NativeCardioModel/Test – nem írható.


## Rögzített forráslinkek

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/AGENTS.md)
- [docs/NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/NATIVE_RESUME.md)
- [docs/WORK_HANDOFF.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/WORK_HANDOFF.md)
- [docs/reviews/NATIVE_CARDIO_INTEGRATION.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/reviews/NATIVE_CARDIO_INTEGRATION.md)
- [docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/docs/reviews/NATIVE_FEATURE_PARITY_TRIAGE.md)
- [android/app/src/main/java/com/repforge/app/ui/progress/NativeCardioModel.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/progress/NativeCardioModel.kt)
- [android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressQuery.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressQuery.kt)
- [android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt)
- [android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/ui/progress/ProgressScreen.kt)
- [android/app/src/main/java/com/repforge/app/data/db/TrainPilotDaos.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/data/db/TrainPilotDaos.kt)
- [android/app/src/main/java/com/repforge/app/data/db/TrainPilotEntities.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0a5abbeb3b95cede671df39a3f8d09dc4ab8ec20/android/app/src/main/java/com/repforge/app/data/db/TrainPilotEntities.kt)
- [Main – végleges app.js](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js)
