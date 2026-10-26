# Plus chat 2 — native CI evidence summary, saját átadás

Dátum: 2026-10-26. Pinned kiindulás: `e28ab41887a976ee380f63f465dbdcb51f494329` (appforrás f72c9cb0). Saját ág `test/native-ci-evidence-summary`, cél `feat/native-android-migration`, csak draft PR. Módosítás kizárólag új offline Python eszköz/stdlib unittest/használati dokumentum és ez a review. GitHub Actions/workflow, Gradle, alkalmazáskód, csomag/séma/verzió változatlan. Teljes CI dispatch és merge nincs.

## Tartalom és szerződés

- `scripts/native/evidence-summary.py`: több kibontott artifact helyi vizsgálata, exact teljes SHA; csak lokális fájlolvasás. Raw instrumentation terminális class+method, `numtests` vs tényleges teljesített, runner-záró `OK`, crash és hiány; szűrt, személyes adatot nem tartalmazó failure-excerpt + raw relatív hivatkozás. JVM XML számlálók, sérült/többértelmű XML elutasítása. Wear 0/1/2 shard, unique tesztazonosítók, deklarált/globális összegek, commitnév-egyezés. Telefonos manifest `commit` és két APK-SHA, kötelező képek + stdlib PNG/CRC/zlib és SHA/bytes. APK metadata SHA-check külön opcionális kapu. Össz státusz PASS/FAILED/INCOMPLETE/UNKNOWN, exit 0/1/2, JSON schema v1 és rövid Markdown.
- `tests/native/test_evidence_summary.py`: csak szintetikus, izolált stdlib-fixture; korrupt és hiányos evidence nem válhat zölddé.
- `docs/NATIVE_CI_EVIDENCE_SUMMARY_USAGE.md`: pontos parancs, artifact névmegőrzés, kimeneti szerződés és korlát.

## Valóban végrehajtott ellenőrzés

1. `python3 -m py_compile scripts/native/evidence-summary.py`: **PASS** (szintaxis).
2. `python3 -m unittest discover -s tests/native -p 'test_evidence_summary.py' -v`: **14/14 PASS** helyi szintetikus próbán (nem emulátoros/native runtime).
3. Read-only valós artifact próba: Actions [40291546367](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40291546367), telefon [12636867224](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40291546367/artifacts/12636867224), mellette JVM és három Wear artifact. A parancs (tényleges helyi célút változhat):

   ```bash
   python3 scripts/native/evidence-summary.py /mnt/data/native-ci-real \
     --expected-commit b28672bffe574506ed6c808c88d6b7a42c4ecd70 \
     --require-gate phone --require-gate jvm --require-gate wear \
     --wear-total 183 --json-out /tmp/native-ci-real.json \
     --markdown-out /tmp/native-ci-real.md
   ```

   **Exit 1 / FAILED**: phone `declared=69`, `executed=18`, `passed=12`, `failed=6`, `missing=51`, `crashed=true`, screenshot `0`; JVM app `1096/0/0/0`, Wear JVM `167/0/0/0` PASS; Wear UI `68+56+59=183`, unique `183`, három azonos nevű SHA-shard PASS; APK metadata csak checklist-fájlt tartalmaz, valódi APK byte nélkül **INCOMPLETE**. Ez NEM teljes telefon UI PASS és nem új CI-futás.

## Korlátok / rootnak hátra

- A Wear/JVM SHA az eredeti artifact-könyvtár nevéből ered; GitHub run metadata nélkül önmagában nem kriptográfiai forrásigazolás. A konkrét Android APK és screenshot származása csak a meglévő CI-/készülékes szerződéssel hitelesíthető.
- A teljes natív CI és készülékes elfogadás Root feladata. A teszt-parser local PASS-ja nem native testsuite PASS.
- A valós hibás telefonos artifactban nincs screenshot és a folyamat crash miatt hiányos; nincs kitalált 51 eset.
- Valódi felhasználói adatot, titkot, GPS-koordinátát, token/URL-t a generált kimenetbe nem írunk.
