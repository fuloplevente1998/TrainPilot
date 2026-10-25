# Gemini AI Studio – elkülönített kardiószámítás

2026-10-25. A felhasználó kérte a külön Gemini-chatnek adható promptot és raw
linkeket. Az átadó elkészült; az elindítás/eredmény még nincs visszaigazolva.
Nem belső subagent-felhatalmazás. A fő munkás naplószerkesztést és integrációt végez.
A többnapos Health-böngészőre külön ChatGPT-prompt is átadva; annak elindítása
szintén nem igazolt, a fájlhatárokat fenntartjuk az eredményig vagy lemondásig.

## Másolható feladat

TrainPilot – kizárólag a natív kardióstatisztikák számítási részét készítsd el.
A felületi és Room-bekötés a fő fejlesztőnél marad.

Natív referencia: **9a18ca60a04a17715298993d432c80c63f587cc3**.
Main referencia: **7473b30f387eda54186c1e31830304ca12bd104f**.

Először olvasd el az AGENTS.md-t, NATIVE_RESUME.md-t és az alább megadott
forrásokat. Ha a linkeket nem tudod ténylegesen megnyitni, írd le, melyik fájl
kell feltöltve. Ne találj ki hiányzó kódot, SDK-t vagy futtatott eredményt.

A main www/app.js tényleges függvényeiből indulj ki:
`tp106IsDistance`, `tp107WeekStart`, `tp107Pace`, `tp107CardioModel`,
`tp107DistanceStats`. A teljes fájlban keresd ezeket, ne az ág nevéből következtess.

Elkészítendő két új fájl:

- `android/app/src/main/java/com/repforge/app/ui/progress/NativeCardioModel.kt`
- `android/app/src/test/java/com/repforge/app/NativeCardioModelTest.kt`

Tiszta Kotlin számítás, Context/Room/hálózat nélkül, explicit tesztelhető `now`
és `ZoneId` paraméterrel. Egyszerű dokumentált sorprojekciós bemenetet tervezz,
amelyet később lapozott Room-lekérdezés táplálhat. Ne igényelje a teljes napló
részletgráfjának, fotóinak vagy GPS-pontjainak egyidejű betöltését; ne vezess be
második naplót, tárolót vagy adatbázist. A bemenetet ne módosítsd.

Funkciók:

- Összes táv és idő; aktuális/előző hét és a main szerinti nyolchetes bontás.
- Helyi hétfői hétkezdés; naptári hetek és DST, nem fix168 óra.
- Tempó csak pozitív idővel ÉS távolsággal rendelkező sorokból:
  összegzett másodperc / összegzett kilométer. Az idő távolság nélkül külön
  time-only adat; hiányzó tempót nem becsülünk meg.
- Azonos aktivitás és main szerint kerekített azonos métertáv rekordcsoportok:
  legjobb, második eredmény, darabszám, stabil holtverseny.
- Aktivitásszűrés, kész/nem kész sorozatok, hiányzó/nulla/hibás/nem véges adatok,
  túl nagy táv és jövőbeli edzés kezelése a main tényleges szabályai szerint.
- Rekordok workoutId/setId és mérési forrás azonosítóinak megtartása.
- Ha a main egy esetben nem ad teljes szabályt, dokumentáld a választást;
  megváltoztatott policyt ne nevezz igazolt paritásnak.

Tesztkód: üres bemenet; több hét és aktivitás; súlyozott tempó; time-only;
azonos távú rekord/holtverseny; hibás/jövőbeli adatok; időzónás hétváltás;
DST;200/1500 edzésből képzett bemenet. Ne írj puszta mező-visszaolvasási tesztet.

A meglévő fájlok, DAO-k, képernyők, szinkron, backup, main/migrációs ág,
verzió és buildkonfiguráció módosítása tilos. Nincs helyi Gradle/APK-futtatás.
Ez a számítási rész átadása, nem a teljes kardiófunkció megvalósítása/elfogadása.

Ha van GitHub-írási hozzáférésed: kizárólag új `test/native-cardio-model` ágon,
a megadott natív commitból dolgozz, köztes commit `[skip ci]`. Ez a konkrét
feladat ágkivétel az AGENTS fő migrációs munkásának általános ágkorlátozása alól.
A main és `feat/native-android-migration` ágakat ne írd/merge-eld. Ha készül PR,
draft legyen, célja a migrációs ág; csak a két engedélyezett fájl változzon.
Ha nincs GitHub-hozzáférésed: a két teljes fájlt és alkalmazható unified diffet add.

Átadás: fájlok/diff, bemeneti-kimeneti API, mainhez képesti eltérések/korlátok,
megírt és ténylegesen futtatott ellenőrzések külön. Gyökér-AGENTS vagy teljes
WORK_HANDOFF átírása, main merge és új feladat kiosztása nem része a munkának.

## Rögzített raw források

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/AGENTS.md)
- [NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/docs/NATIVE_RESUME.md)
- [NativeProgressQuery.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/android/app/src/main/java/com/repforge/app/ui/progress/NativeProgressQuery.kt)
- [ProgressViewModel.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/android/app/src/main/java/com/repforge/app/ui/progress/ProgressViewModel.kt)
- [TrainPilotEntities.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/android/app/src/main/java/com/repforge/app/data/db/TrainPilotEntities.kt)
- [WorkoutSharedContracts.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/9a18ca60a04a17715298993d432c80c63f587cc3/android/shared/src/main/java/com/repforge/shared/WorkoutSharedContracts.kt)
- [Main www/app.js](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js)

Mindegyik útvonalat a megadott commit Git-objektumában ellenőriztük.
A raw HTTP elérés ettől függetlenül a Gemini környezetének képessége.
