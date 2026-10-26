# PR250 – integrátori review és következő javítások

2026-10-26. Külső exact `fb64307f67459a00def6b81890bc4ba337da00d6`,
beemelve `85f09350`. A két eredeti auditfájl byteazonos; nem runtime-jelentés.

- 107 ID/sorrend megmaradt, JSON státuszösszeg: 77 IMPLEMENTED_UNVERIFIED,
  9 PARTIAL, 21 PLATFORM_DEVICE_PENDING. Nincs igazolt natív runtime elfogadás.
- A main teljes app.js már elérhető a pinned, byteazonos split forrásból.
  A régi nyers UNRESOLVED mezők megszűntek. Ez hivatkozásjavulás, nem minden
  caller/final override és működés független igazolása.
- A07 program duplikálás/reset, A12 kedvencek, C10 fotóhoz vezető gyorslink,
  E15 törlési main handler és E19 rendszerlinkek még célzott összevetést kérnek.
  Bizonytalan elérési útból nem készítünk automatikusan új funkciót.
- #249 téma már benne a vizsgált appban. #251 Health szinkron későbbi integráció:
  `4b1711b1`, Samsung SDK interoperabilitás javítva `cfa113be`. A régi audit
  OWNED_BY_OTHER / régi CI állapota történeti pillanatkép, nem mai hiánylista.
- Valódi kódszintű hiányok: első indítási welcome/skip/continue, utólagos
  napló-feedback szerkesztés, többhetes naptártervezés és közvetlen globális
  időzítőbeállítás. Az E18 engedély-visszaút csak a hiányzó helyeken pótlandó;
  a meglévő GPS/HC/BLE kontextus-kéréseket nem duplikáljuk.

## B08/E17 – globális időzítőbeállítás bekötése

Root hozzáadta a Settings közvetlen, hatmezős szerkesztőjét. Munka/pihenő/kör,
hang, rezgés és automatikus léptetés az eredeti NativeIntervalPreset validátoron
és TrainPilotSettingsStore-on megy át. HU/EN/DE/RO feliratok, téma-tokenek,
explicit Mentés/Mégse. Hibás vagy 24 órán túli preset nem menthető.

A mentés metadata lease + megnyitott konfiguráció összehasonlítás + replacement
epoch ellenőrzést használ. Visszaállítás vagy másik szerkesztés után a régi
ablak nem írhatja felül az új adatot. A meglévő aktív frame, stopper és workout
override érintetlen; a NativeWorkoutSession már létező timerConfiguration útja
az új edzésekhez olvassa a mentett alapértéket. Nem készült második időzítő.

Megírt, még nem futott ellenőrzések: 4 JVM (hat mező/aktív frame változatlanság,
stale editor, azonos konfigurációjú restore epoch, atomikus hibás preset),
2 valódi Settings→VM→DataStore emulátoros UI (cancel/save/reopen és invalid).
Helyi lexical/XML/diff ellenőrzés nem Gradle vagy runtime PASS.

## Közös CI elakadás

`40285918450` app `cfa113be`: telefon/Wear debug és telefonos teszt-APK
fordítás sikeres. Unit lépés 12 perces diagnosztikai timeout. 184 per-case
SUCCESS után a NativeCoachVariationTest.confirmationCreatesSeparateCoachProgramEvenForAnOwnSourceProgram
nem fejeződik be. Ez nem teljes telefonos suite PASS; R8/UI/upgrade nem futott.

`1b3e440f`: csak a tesztharness kap StandardTestDispatcher + runTest main pumpot;
a sikeres mentési próba valódi IO hívóból indul. Room transaction → Main.immediate
check visszatérését nem blokkolja tovább a runBlocking/paused looper. Minden
termékkódbeli main-thread guard, forrásbizonyíték és tesztassert megmaradt.
Az új közös CI igazolja vagy cáfolja a javítást, nincs tesztkihagyás.

Main/verzió/csomag változatlan, nincs PR-zárás vagy élő adatos telepítés.

## Utóellenőrzés – E15/E19 és felhő-karbantartás

A main tényleges erase/privacy caller most azonosítva, nem maradt pusztán
kulcsszókeresési hiány: app.js L10759/L11177 Settings → tp105DataPanel L15019–15021
→ tp105ManageData L14998–15017, kind local/drive/calendar/revoke. Privacy:
tp105OpenPrivacy L15023 → BackupArchive.privacy (natív) vagy privacy.html (web).
Exact main7473b30, eredeti sorszámok. A helyi törlési és privacy funkció a natív
Settingsben már elérhető; erre nem készül második törlőmotor vagy privacy oldal.

**További bizonyított bekötési hiány:** GoogleSyncRepository.revokeAccess,
deleteAllDriveAppData és deleteManagedCalendars megvan, de kizárólag deklaráció/
belső SDK-call találatuk van az alkalmazásforrásban. SettingsScreen/ViewModel
nem hívja őket. A main fiókhoz kötött három megerősített karbantartási műveletének
UI/consent/account/operation gate bekötése későbbi root-csomag. Ez kódparitás-hiány,
nem csak runtime-bizonyítékhiány. Valódi Google-adatokon semmi nem lett törölve/
visszavonva. Külön tesztelendő a cancel, account switch, token expiry, restore
epoch, aktív edzés/szinkron admission és részben végrehajtott távoli törlés.

A12 main rf144FavoriteButtons/toggleExerciseFavorite144 L1542–1552 és portable
exerciseFavorites L14896/L14905/L14920 forrása azonosítva, de a végső library
override caller fennmaradása továbbra külön vizsgálat. Native favorites nem hiányzik.
C10 a main rf215WeightScreen L2791 testsúlyadatokat/diagramot mutat; közvetlen
testsúly→haladásfotó shortcut hiánya önmagában nem igazolt main regresszió.
E19 TRAINPILOT_VERSION L127 és backup/diagnosztikai használat nem bizonyítja
egy külön verzióinformációs Settings-képernyő kötelező paritását.

## C03 – utólagos napló-feedback bekötése

`bc18d019`: az eredeti JournalEditor draft/mentési úthoz hat explicit visszajelzés
és a Statisztikák fülről is szerkesztőgomb került. Mégse nem ír. Mentés az eredeti
workout/sync/key azonosítót tartja; a már alkalmazott program-feedback nem írható
át (main accept applied guard paritás). A main kompatibilis effort/1–5 vagy null
score/rating/source/updatedAt mezők frissülnek. Régi skipped/suggestion kikerül,
ismeretlen feedback kiterjesztés megmarad. Nincs új finalizálás vagy programemelés.
Megírt 3 JVM+1 valódi editor→VM→Room UI teszt még nem futott.

Közös40289777728 valós XML: telefon1093/4fail, Wear167/0fail. TimerDefaults4,
HealthSync18+11 és SamsungGrantScope3 PASS. A négy hiba és b28672bf javítása az
aktuális NATIVE_RESUME-ban. Új közös40291546367 indításkor IN_PROGRESS, a post-hoc
feedback és a négy javítás új tesztelfogadása ebből következhet, régi runból nem.
