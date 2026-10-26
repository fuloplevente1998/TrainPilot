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
