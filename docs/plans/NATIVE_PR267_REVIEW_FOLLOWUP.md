# PR267 – a paritásaudit célzott befejezése

Ugyanazon saját ág/PR267 folytatása. Review exact HEAD:
0f8cae57060a4b1b8fd7bee673a5ef6741641041.
Egyetlen írható fájl továbbra docs/reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md.
Main/migration/app/tests/workflow írás, új ág, CI, merge/close tilos.
A docs-only baseline5703883c eltérés a pin3297e82b-től elfogadható, nincs benne
új appkód. Nem kell újrafuttatni Androidot vagy újraprioritizálni kitalált hibákat.

## Root review

A 11natív sor és38pinned fájlhivatkozás használható; G10javítva,
G08/G09elfogadott, G06/G11újfeature osztályozás jó kiindulás.
De a kért main-live-callsite mező mindenhol bizonyítatlan, ezért ez még nem teljes
main↔native paritási következtetés. A hat evidence-missing pontot (G02/G03/G04/
G05/G07/G12) ne nevezd automatikusan hat kötelező implementációnak.

## Hiányzó források könnyen olvashatóan

Exact main7473b30f387eda54186c1e31830304ca12bd104f, app.js blob
b5567576bed9a8f93608a6bf1ed8db1dee4723f4.
Nem kell egy2MB-os MCP-válaszból dolgozni: van byte-azonos93részre vágott forrás,
24000byte/rész, eredeti sorszámokkal és181szekciós indexszel:
https://github.com/fuloplevente1998/TrainPilot/blob/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/INDEX.md
Raw:
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/INDEX.md
Ugyanitt manifest.json és app-part-001.txt…app-part-093.txt; a szükséges részeket
olvasd. Végső override/caller érvényes, az első függvénydefiníció nem elég.

## Befejezési feltételek

- Mind11sornál mainUI → tényleges handler → végső implementáció, exact path/line.
  Ha mainben nincs ilyen út, dokumentáld a megvizsgált releváns szekciókat és azt,
  hogy ez mockupból származó igény; ne találj ki hiányzó végfelhasználói funkciót.
- A6evidence-missing pontnál különítsd el a main-paritást a felhasználó új
  mockup-/accessibility követelményétől. Például24dp delete48dp-re alakítása lehet
  kötelező újUIkritérium akkor is, ha a régi main másként nézett ki.
- Státusz/prior/patch owner az összevetés eredménye alapján frissüljön.
- G05élőpulzus a headerben már létezik; ezt tartsd meg, ne nevezd teljes HR-hiánynak.
- G10integrált scoped-green; közösruntime nem ebből az auditból következik.
- Amelyik pont tényleges hozzáférési hiba miatt nem dönthető el: konkrét forrás/
  sikertelen elérési kísérlet/hiányzó adat legyen, ne általános „minified” indok.
- Ugyanazon report frissítése, exact újSHA és rövid delta a PRleírásban.
  Ez auditbefejezés, nem felhatalmazás új funkció kódolására.
