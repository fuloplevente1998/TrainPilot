# PR267 – a paritásaudit célzott befejezése

Ugyanazon saját ág/PR267 folytatása. Review exact HEAD:
0f8cae57060a4b1b8fd7bee673a5ef6741641041.
Egyetlen írható fájl továbbra docs/reviews/NATIVE_G02_G12_CURRENT_BASELINE_TRIAGE.md.
Main/migration/app/tests/workflow írás, új ág, CI, merge/close tilos.
A docs-only baseline5703883c eltérés a pin3297e82b-től elfogadható, nincs benne
új appkód. Nem kell újrafuttatni Androidot vagy újraprioritizálni kitalált hibákat.

## Második Root review — 2026-10-26, exact `7aece643`

A frissítés hasznos: a 11 sor most main hivatkozásokat is ad, és külön kezeli
az új mockupigényeket. A teljes audit még **nem elfogadott / nem integrált**.
Root a teljes exact main fájlokból ellenőrizte az alábbi konkrét eltéréseket.
Ugyanazon saját PR267/ág és ugyanazon egyetlen auditreport írható; sem app,
sem teszt, sem CI, sem main/migration írás nem engedélyezett.

1. **G03: nem a végső Quick lánc szerepel.**
   `www/app.js:10638` később felülíródik `:10909`-nél:
   `tp150QuickAction → tp152OpenQuickExercise` (`:10908`) → picker és inline
   konfiguráció (`:10888–10907`) → `tp152StartQuickInline`.
   Ez utóbbit a GPS-védő wrapper is felülírja `:15251–15252`-nél, majd a
   rögzített base `:10892–10902` végzi a persistDraft/phoneWorkoutStarted hívást.
   A `:10266–10268` régi planned-session rejection nem érvényes végső guard:
   a későbbi inline út `tp150IsQuick()` esetén bővít, egyébként
   `tp150QuickConfirmReplace` (`:10258–10262`) a meglévő draft felülírására kérdez rá.
   Pontosan ezt írd le; a natív adat/ownership védelem marad követelmény,
   de ne tulajdoníts a mainnek már felülírt elutasítást.

2. **G06: a provider forrása elérhető, nem csak app.js-ből kell következtetni.**
   Exact `7473b30f387eda54186c1e31830304ca12bd104f:www/index.html`:
   - `:2118–2132` tileProvider: konfigurált HTTPS provider vagy feltételes OSM;
   - `:2297–2317` Leaflet map zoomControl és tényleges tileLayer;
   - `:2335` openRoute, `:2380–2400` offline alapnézet, külön tájékoztatás/
     online gomb és renderLeaflet hívás.
   Tehát az opcionális, explicit beleegyezéses online térképréteg és zoom
   létező main funkció. Ne nevezd teljes egészében új feature-nek. A recenter
   külön vizsgálandó, nem következik automatikusan a zoom meglétéből.
   Vizsgáld a tényleges natív provider/online elérhetőséget, és pontosítsd
   a parity hiányt vagy elfogadott eltérést. Automatikus hálózati tile-lekérés
   ebből nem engedélyezett. Ha kis fájlblokkok kellenek, GitHub contents API
   exact ref, illetve az index.html releváns sorszámai rendelkezésre állnak.

3. **G04: a végső Wear-owned dispatch is része a láncnak.**
   `www/app.js:19018–19021` menti a base dockCommandot, majd external ownership
   esetén remote pause/resume parancsra cseréli. A helyi base `:18871–18874`
   megmarad local ágon. Mindkét végső elágazást dokumentáld.

4. **G07/G11: pontosítsd az override-jegyzéket.**
   nextExercise későbbi wrapperjei `:15248`, `:15495`, `:19032`;
   backupStatus végső lokalizált override `:8100–8105`, exportData `:9602`.
   A végső effect lehet azonos, de a „végső caller ellenőrizve” állítás csak
   teljes lánccal legyen. G05 útvonalpont-pontosság a napló térképén
   `index.html:2378–2379` különbözik a kért élő GPS accuracy paneltől.

Elvárt átadás: ugyanazon report javított táblázata és crosswalkja,
exact új SHA, rövid delta. G03/G04/G06 besorolást a tényleges végső kód alapján
végezd; ne őrizd meg pusztán az előző audit következtetését. Ez továbbra
forrásaudit, futtatott Android/JVM/CI bizonyítékot nem kér és nem igazol.

## Első Root review

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
