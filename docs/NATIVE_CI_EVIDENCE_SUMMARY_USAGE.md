# Native CI evidence summary — offline használat

A `scripts/native/evidence-summary.py` kizárólag **már letöltött és kibontott** GitHub Actions artifact-könyvtárakat olvas. A Python 3.11+ standard library elegendő. Nem kér tokent, nem hív hálózatot, nem indít buildet, emulátort vagy workflow-t, és nem ír a bemeneti artifactokba.

Az eredeti artifactnevek (az SHA-val együtt) **könyvtárnévként megőrzendők**. Például az `artifacts/` alatt:

- `TrainPilot-Native-Phone-UI-Evidence/manifest.json`, `instrumentation.txt`, `screenshots/*.png`
- `TrainPilot-Native-Wear-UI-Results-<full40SHA>-0/`, `-1/`, `-2/`: `instrumentation.txt`, opcionális Wear PNG-k
- `TrainPilot-Native-Tests-<full40SHA>/app/build/test-results/.../TEST-*.xml`, illetve `wear/...`
- `TrainPilot-Native-Update-Verification/native-apk-sha256.txt` (ha letöltötték)

Példa a kiválasztott kapukhoz (az APK-k a ZIP-ekből külön, változatlanul kibontva):

```bash
python3 scripts/native/evidence-summary.py artifacts \
  --expected-commit "${COMMIT_FULL_SHA}" \
  --require-gate jvm --require-gate phone --require-gate wear \
  --wear-shards 3 --wear-total 183 \
  --require-screenshot home \
  --phone-apk extracted-phone-debug/app-debug.apk \
  --phone-test-apk extracted-phone-ui-test/app-debug-androidTest.apk \
  --json-out /tmp/native-ci-summary.json \
  --markdown-out /tmp/native-ci-summary.md
```

Több bemeneti könyvtár is megadható. A `--require-gate` ismételhető: `jvm`, `phone`, `wear`, `apk`; hiányzó kötelező kapu `INCOMPLETE`. `--require-screenshot` ismételhető, a manifestben név szerint meglévő telefonos checkpointot követel meg. A `--apk-file 'app/build/outputs/apk/release/app-release.apk=/path/to/app-release.apk'` (szintén ismételhető) az `apk` gate checksum-sorát valódi APK-byte-hoz ellenőrzi. A forráskönyvtárban nem lehet a `--json-out`/`--markdown-out`; külön fájlra mutassanak.

Kilépési kód: `0` = az átadott és megkövetelt kapuk bizonyítéka PASS, `1` = FAILED (teszthiba, ellentmondás, eltérő SHA/sérült hash/duplikáció), `2` = UNKNOWN/INCOMPLETE vagy érvénytelen CLI-argumentum. A statusokat **ne értelmezd új CI-futásként**: az eszköz csupán az átadott anyagot értékeli.

## JSON v1

`schema_version`, `expected_commit`, `status`, `complete`, `gates`, `limitations`; a `gates` kulcsai: `phone`, `wear`, `jvm`, `apk`. A telefon mezői: `declared`, `executed`, `passed`, `failed`, `errors`, `skipped`, `missing`, `crashed`, `complete`, `screenshots`, `commit`, `failure_excerpts`, `issues`, `raw`, `status`. A Wear: `expected_shards`, `shards[]` (index, commit, declared/executed/passed/fail/errors/skipped/missing/status, screenshot-szám, raw és hibalista), `unique_cases`, összesített számlálók. JVM: `modules.app|wear|shared.{tests,failures,errors,skips}`, `tests`, `xml_files`, `failures`, `skips`; APK metadata: `records`, `verified`, `issues`, `status`. `issues[]` típusos rövid diagnózisok, `failure_excerpts[]` csak testcase-azonosító, kiválasztott kivételtípus és relatív raw fájlút; nyers stack, GPS, személyes adat, token vagy URL nincs a kimenetben.

## Konzervatív bizonyítási korlátok

Az Android runner `numtests` nem azonos a végrehajtott tesztekkel. Egy eset csak a terminális `INSTRUMENTATION_STATUS_CODE` után számít; a hiányzó eseteket **nem találja ki**. A végső runner kód, `OK (N tests)` és az egyedi esetek egyezése kötelező. A `instrumentation-derived.xml` nem helyettesíti a raw naplót. Wear esetén a teljes, három külön indexű shard és az egyedi class+method párok szükségesek; a sharding megnevezése a könyvtárnévben van, nincs beágyazott SHA-attesztáció. JVM esetén a `TEST-*.xml` fájlok nyers testcase-elemeit számolja, és összeveti a deklarált suite-számmal. A `--wear-total` explicit globális elvárás; nincs varázslatosan feltételezett 183 minden jövőbeli változathoz.

Telefonos sikerhez a két debug APK valódi byte-jainak egyezése, nem üres screenshot-manifest és érvényes PNG/hash szükséges. Az APK checksum metadata önmagában kevés; `--apk-file` nélkül `INCOMPLETE`. A valós futtatás és a screenshot eredetének kriptográfiai igazolása külső bizonyítékot igényel. Sem egy parser-PASS, sem egyetlen zöld gate nem natív készülékes elfogadás.

Gyors szintetikus regresszió:

```bash
python3 -m unittest discover -s tests/native -p 'test_evidence_summary.py' -v
```
