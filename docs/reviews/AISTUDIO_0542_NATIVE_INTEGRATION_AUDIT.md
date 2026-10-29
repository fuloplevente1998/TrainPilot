# AI Studio 2026-10-28 05:42 — összehasonlító audit

## Forrás és hatókör

Forrásrepo: `fuloplevente1998/Native-AI-Studio`, exact ref
`2db71628c604fb361e7197a7c41b2a0ed9299cea`,
`trainpilot-oct-28-2026-5-42-24-am.zip`.

Pinned letöltés:
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/2db71628c604fb361e7197a7c41b2a0ed9299cea/trainpilot-oct-28-2026-5-42-24-am.zip

SHA256 `8f91e79d604de2415ee57bd6094c453f1512a0f2b1c93caf354da31f75b6a308`.
8 861 301 byte, 479 ZIP-bejegyzés, CRC PASS.
Összehasonlítás: 21:02 export SHA256 `db5ce026e2559366a7d3366332ab64affef7448839a7727f0dbfce447d7d65d9`.
Mindössze négy fájl tartalma változott: Studio TrainPilotViewModel, natív MainActivity,
LegacyLocalStorageReader és AISTUDIO_REFINEMENT_REPORT. A képernyők és dizájnfájlok byteazonosak a 21:02 exporttal.
Statikus review; nincs helyi Gradle/build/telepítés/CI vagy alkalmazáskód-módosítás.
Az export riportjának PASS kijelentése nem független futási bizonyíték.
PR270 natív összehasonlítási pont: `3ad2fa1c`.

## Ellenőrzött javulások

- A LegacyLocalStorageReader visszaállítja a 60 másodperces timeoutot, eltávolítja a WebView-hiba/timeout -> emptyMap fallbacket és az app_webview létezésvizsgálatát. A korábbi kritikus hibaelnyelés ezen a kódúton megszűnt. A finally cleanup még runCatchingot használ; ez nem sikeres olvasássá alakítja az eredeti hibát.
- MainActivity visszaállítja a recovery / migrationReady / migrationFailure / loading ágakat, lokalizált hibaképernyővel és retry gombbal. Ezek forrásban ellenőrzött javítások, még nem emulátoros elfogadás.
- A settings írások már valóban hívják a setTheme/setLanguage/setTimerDefaults API-kat. A testsúly hozzáadása már valódi Room DAO-hívás, és az induló adapter valódi naplókat/testsúlyt is próbál olvasni.

## Három elsődleges blokkoló

A következő sorszámok az új ZIP fájljaira vonatkoznak, nem a PR270 azonos nevű natív fájljaira.

### 1. P0 — mesterséges edzés kerülhet a valódi adatbázisba

`app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt:624–675`:
finishAndSaveWorkout új véletlen UUID-t generál minden híváskor, nem a kanonikus
aktív session azonosítóját használja. Fix 38 perces kezdőidőt, rögzített
program/nap azonosítót és üres exercises tömböt ír a migrátor Room-import API-jával.
A UI ettől eltérő fix volument, sorozatszámot és PR-t állít. Már a tartós írás
előtt sikert jelez; az írási hiba csak logba kerül. Újabb kattintás új rekordot
hozhat létre. A migrátor belső metadata fence-e létezik, de ez nem helyettesíti az
edzésmotor session-, ownership-, idempotencia- és finalization szerződését.

Teendő: a meglévő natív finish/summary/finalization útvonalat hívni a valódi
sessionnel; csak tartós siker után mutatni mentési nyugtát. A prototípus
edzésadatait nem szabad éles naplóként perzisztálni.

### 2. P1 — a Health/Wear visszajelzés továbbra nem a tényleges eredményt jelzi

Ugyanott `:830–867`: a Health gomb továbbra 120-szal növeli a lépésszámot,
majd sikeres Samsung/Health Connect szinkront ír ki. Ezután csak háttérfeladatot
ütemez és watch-home snapshotot publikál; nem olvasott új provider-adatot és
nem vár szinkroneredményt. Az óraátadás csak lokális booleant vált és ugyanazt
a home snapshot publikálást hívja; nincs ownership request/ack/nyugtázott átadás.
A korábbi DEMO címkék eltávolítása nem teszi ezeket valódi mérési eredménnyé.

Teendő: a meglévő natív Health refresh és Wear ownership folyamatainak állapotát
megjeleníteni; loading/error/partial/ack állapotokkal. A 120 lépés hozzáadását
eltávolítani az éles adapterből.

### 3. P1 — közvetlen adapterírások megkerülik a kanonikus adatkezelést

Ugyanott `:789–815`: testsúly közvetlen bodyWeightDao.upsertWeight hívással kerül
be, NativeDataReplacement.mutate és a meglévő NativeWeightJournalRepository
validálása/normalizálása nélkül. Restore-versenyben ez nincs az eredeti metadata
beléptetéssel összehangolva. A rawJson nem tartalmazza a sor id/updatedAt mezőit;
a kanonikus repository ezeket is tárolja. A UI már írás előtt sikert mutat.

`:870–917`: hang vagy pihenőidő módosítása hardcoded workSeconds=30 és rounds=3
értékeket is visszaír, tehát más meglévő intervallumbeállítást felülírhat.
`:314–315`: a színek packed Color.value alsó bitjeiből készülnek, nem toArgb()
ARGB-értékből; ez hibás accentet tárolhat (sRGB packed értéknél a szín a felső
32 bitben van). A toArgb importálva van, de nincs használva.
`:211–250`: az induló getAllWorkoutsWithDetails nincs lapozva; bind a migráció és
recovery readiness előtt fut. A valódi rekordok mellé hozzáfűzi a DEMO listát,
fix 30:00/15:00 munka/pihenő értékeket tesz a valós naplókra, és megismételt
Activity-bind újra hozzáfűzheti ugyanazokat. A hibákat catch(Throwable) elnyeli.

Teendő: a meglévő bounded, invalidációt figyelő natív repository/ViewModel
adatút újrahasználata, megtartott continuation/restore fence-ekkel; a settings
módosítást meglévő állapotra épülő kanonikus művelettel menteni.

## Bizonyítékhatárok és folytatás

Az applicationId változatlanul `com.aistudio.trainpilot.tpux01`; külön Studio
sandbox, nem a kanonikus signed-upgrade identitás. A ZIP tesztjei változatlan
Example tesztek; nincs új célzott regressziós próba, tesztlog vagy APK a javításokhoz.
A jelentésben említett toggleHapticVibration nem található a Studio ViewModelben.
Az összes P0/P1 javítás befejezését ezért a forrás nem támasztja alá.

A javított migrációs hozzáállás és a Studio megjelenés használható. A teljes ZIP
nem merge-elhető éles implementációként. PR270-en az elfogadott Studio UI-t a
kanonikus motor/repository/state/callback réteghez kell kötni; az új adapter
fake-record és fake-success viselkedését nem átemelni. A jelenlegi elfogadott
18:49 vizuális kánon, eredeti arany súlyzó és valódi anatómiai grafika kivételei
változatlanok. A megjelenítés ezzel a ZIP-pel nem változott a 21:02-höz képest.
