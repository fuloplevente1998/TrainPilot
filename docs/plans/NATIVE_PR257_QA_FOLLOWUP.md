# PR257 visszaellenőrzés, hibajavítás és bizonyítékpróba

Repository: fuloplevente1998/TrainPilot. Kizárólag meglévő saját ág:
`test/native-ci-evidence-summary`, meglévő draft PR **#257**.
Átadott HEAD: `0ae38c3c555bdc226458b44e8c9a6b288052786b`.

A korábbi14stdlib teszt és valós artifact-próba már PASS-nak jelentett. Most
végezz célzott ellenőrzést a saját implementációd feltételezéseivel szemben;
valódi hibát javíts, ne pusztán növeld a tesztszámot vagy ismételd a régi jelentést.
Olvasd AGENTS.md-t és saját usage/handoff dokumentumaidat.

## Szigorú határ

Main ÉS feat/native-android-migration csak olvasható; nincs push/merge/rebase,
force push, PR/issue zárás, GitHub-komment vagy alkalmazás/Gradle/workflow/gate
változtatás. PR257 draft marad. Csak saját4új fájl, szükséges új saját parser
fixture/teszt módosítható. A futó canonical CI-t ne indítsd újra vagy állítsd le.
Nincs Android build/emulátor/APK: ez offline stdlib Python eszköz.

## Célzott ellenaudit

- Declared vs actual unique terminal: csonka log, halál végső státusz nélkül,
  duplicate class/test, eltérő numtests, skip/assumption, null/hiányos testcase.
  A 35PASS/40FAIL sem teljes siker; az üres/falsított OK marker sem elegendő.
- XML root/gyermek számlálók ellentmondása, errors vs failures vs skipped,
  parametrizált esetelemek, hibás XML/JSON/UTF8. Ismeretlen eredet ne legyen PASS.
- Shard grouping nem épülhet csak az összeadott183-ra: minden index pontosan egyszer,
  azonos commit, nincs duplikált unique test; missing/mixed shard fail-closed.
- PNG CRC/zlib/hash mellett valós IHDR/dimenzió/sorrend/IEND és váratlan trailing
  adat; nem teljes PNG ne legyen elfogadott screenshot. Saját bonyolult képmotort
  ne írj: minimális strukturális ellenőrzés elég, új dependency nincs.
- Manifest commit/APK bytes/hash és külső artifact filename eredet külön állítás.
  Egy fájlnév önmagában nem kriptográfiai bizonyítás, ezt a report jelezze.
- Adatvédelem: multiline exception/record/URI/email/token/GPS ne kerüljön a rövid
  kimenetbe; raw fájlra hivatkozhatsz. Hibaüzenetben se jelenjen meg titkos URL.
- CLI hibás/üres bemenet, bemenetre mutató output, hiányzó kötelező gate,
  unwritable output és részleges artifact: ne legyen exit0 vagy inputfelülírás.

## Futás és friss valós referencia

- py_compile + összes saját stdlib unittest ugyanazon végső forráson.
- Régi b286 példa mellett használd read-only az új Actions **40317771457** futást:
  phone artifact **12649155506**, 75terminális/35PASS/40FAIL/0screenshot, normál
  FAILURES!!! lezárás (nincs korábbi nav process crash). AppJVM1120/Wear167 zöld.
- JVM artifact **12647857246** ugyanebből a futásból. Wear és upgrade artifactok
  opcionálisan olvashatók; hiányukat ne alakítsd automatikus teljes PASS-szá.
- Elvárt összkép új phone+JVM bemenetnél: phone FAILED, JVM PASS a tényleges XML
  alapján; minden hiányzó kért bizonyíték INCOMPLETE. Nincs app UI sikerállítás.
- A root phone-ui-gate.report parser időközben szigorodott5b88e07e-ben; csak
  olvasd, ne írd/merge-eld. Az eredeti4fájlos eszközöd továbbra önálló maradjon.

Adj át egy futtatható pontos parancsot, megismételhető rövid JSON/MD eredményt,
tesztszámot és pontos kijavított hibákat. Amely vizsgált sarokeset már helyes,
azt jelezd bizonyítékkal, ne csinálj mesterséges kódváltozást. Végső exact SHA,
diff a fenti átadott SHA óta, saját átadó/PR-leírás frissítve. Merge nincs.
