# Végleges UI/UX – részletes specifikáció, első szakasz

**Állapot: a részletes előkészítési csomag elkészült; alkalmazás-UI még nem változott.**
Ez a külön dizájnmunka első, ütközésmentes szakasza. Nem a24képernyő kész
megvalósítása, nem futó app screenshot-csomag és nem natív runtime-elfogadás.

## Exact források és hatókör

- Saját ág: `feat/native-final-ui-ux`; draft cél: `feat/native-android-migration`.
- Induló/olvasási referencia: **c116a042229f61b86babbbeb23c5d2c360486e20**.
- Feladat: [5ad4b5bc NATIVE_FINAL_UI_UX_CODEX_HANDOFF.md](https://github.com/fuloplevente1998/TrainPilot/blob/5ad4b5bc/docs/plans/NATIVE_FINAL_UI_UX_CODEX_HANDOFF.md).
- Elolvasva a teljes303soros [NATIVE_DESIGN_HANDOFF.md](../NATIVE_DESIGN_HANDOFF.md),
  az AGENTS, NATIVE_RESUME és WORK_HANDOFF aktuális teteje, NATIVE_MOCKUP_REVIEW.
- Main alkalmazás: **7473b30f387eda54186c1e31830304ca12bd104f**, `www/app.js`
  blob **b5567576bed9a8f93608a6bf1ed8db1dee4723f4**. A késői planner, nextPlanned,
  health/provider, indoor/stopwatch, ownership, theme és maintenance szekciók
  is részei az összevetésnek; nem az első azonos nevű függvényből indultunk.
- Mind a négy tényleges PNG-t megnyitottuk és vizuálisan áttekintettük. A táblák
 1024×1536 generált referenciák, nem alkalmazásfotók. Hash/dimenzió a MANIFEST-tel egyezik.
- Most kizárólag `docs/design/final-ui-ux/**` változik. UI/VM/nav/theme/közös
  resource, teszt, screenshot/evidence script és workflow nincs módosítva.
  Main/migration és másik saját ág nem kap push-t. Merge/rebase/version change nincs.

## Megtekintett képtáblák

| Kép | Ellenőrzött SHA256 kezdete |
|---|---|
| [01-main-pages-and-planning.png](../mockups/01-main-pages-and-planning.png) | `9cce817d7c158c0e` |
| [02-workouts-and-measurement.png](../mockups/02-workouts-and-measurement.png) | `89fc888fb731f1fa` |
| [03-journal-maps-and-health.png](../mockups/03-journal-maps-and-health.png) | `50de2e9abbdce370` |
| [04-editing-and-settings.png](../mockups/04-editing-and-settings.png) | `3b927da28ad69eda` |

Teljes hash és méret: [eredeti MANIFEST](../mockups/MANIFEST.json).

## Kivitelezőnek átadandó fájlok

1. [01 – Vizuális rendszer](01-visual-system.md): a négy tábla konkrét olvasata;
   meglévő tokenekből származó szerepek, tipográfia, méretek/térközök/sarkok,
   állapotok, input és adaptív viselkedés. Nincs minden témán fix arany/fehér.
2. [02 – Mind a24nézet részletes szerződése](02-screen-contracts.md): megőrzött
   WebView-elem → mockupba illesztés, felülről lefelé oldalfelépítés, fő/másodlagos
   művelet, scroll/IME/Back, üres/loading/hiba/permission/session és egyedi elfogadás.
3. [03 – Pontos meglévő forrásbekötések](03-source-bindings.md): route/Compose →
   tervezett vizuális komponens → létező state/callback → kanonikus adat/writer;
   minden nézethez meglévő tesztforrás, **futási siker állítása nélkül**.
4. [04 – Nyitott kötések és kivitelezési/elfogadási terv](04-implementation-and-acceptance.md):
   12 bizonyított eltérés/korlát, köztük megengedett modal-kivétel; pontos későbbi
   fájlhatárok és lépcsőzetes sorrend. Nyolc globális small/font/locale/TalkBack/
   kontraszt/input/session kritérium mind24nézethez. Raw screenshot bizonyítékszerződés.
5. [05 – Teljes A–E megőrzési leltár](05-feature-preservation.md): mind81követelmény
   nézethez/bindinghoz rendelve, Wear és hiányzó bekötés külön jelölve.
6. Géppel olvasható mellékletek: [screen-contracts.json](screen-contracts.json),
   [source-bindings.json](source-bindings.json), [source-evidence.json](source-evidence.json),
   [palette-inventory.json](palette-inventory.json), [acceptance-register.json](acceptance-register.json).
   Ezek specifikációs és forrásindexek, nem futási riportok. Az acceptance minden sora NOT_RUN.

## Kiemelt döntések

Az öt alsó fül és globális Settings megmarad. A főoldalakon a Folytatás/Indítás,
aktív mérésnél az aktuális sorozat és a mérési állapot az elsődleges. A kiválasztott
programday, keresés, szűrő, scroll és szerkesztési draft nem vész el az új elrendezéstől.
Az aktív workout négy típusa ugyanazt a kanonikus targetet/state-et/writert használja.

A24nézet nem24új route. A Beállítások/mentés/engedélyek/óra megjelenítés több meglévő
panelbe rendezhető. A térképmodal piros X-e megmarad; nem zár edzést. Az illusztrált
háttértérkép, backupfájllista, könyvtári Add callback és finish-note nem létező kötését
nem lehet rajzolt demóval késznek jelölni. A konkrét forráseltérés a04dokumentumban van.

Mind32paletta megmarad:26main Basic/Vivid +6korábbi natív kompatibilitási kulcs.
A26main kulcs utolsó literális accent/accent2 hozzárendelését az exact JS-ben
összevetettük a natív katalógussal, egyeznek; a sort a palette-inventory rögzíti.
HU/EN/DE/RO és a teljes81követelmény marad, nem csak a képeken látszó metrikák.

## Amit ténylegesen ellenőriztünk

- Négy PNG: SHA256 és IHDR dimenzió egyezik az eredeti MANIFEST-tel; mind megtekintve.
- Exact native source:64szimbólumhivatkozás valóban megtalálható a rögzített fájlban,
  fájl SHA256 és GitHub commit+line link a source-evidence indexben.
- Mind24nézet kapott layout/action/input/back/state/egyedi elfogadási szerződést,
  bindingot és NOT_RUN acceptance bejegyzést; mind81feature-ID lefedett.
- Mind32téma leltára,26main literális palette-egyezés; közös token/kontraszt policy olvasva.
- Saját dokumentáció JSON parse, lokális/exact forráslink, fájlhatár és diff whitespace
  ellenőrzése végrehajtva. Ez dokumentációs/source-check, nem Kotlin/Android teszt.

**Nem írtunk vagy futtattunk új Android/JVM/Compose tesztet.** Nem készült APK,
emulátoros kép, vizuális before/after, kontraszt/TalkBack runtime-mérés vagy új CI.
A meglévő tesztosztályok hivatkozása kizárólag regressziós kiindulás. A dokumentumbeli
méretek célértékek, nem már megépített felület mérési eredményei.

## A második szakasz tényleges indítása

Az átadó feltétele: **„Második szakasz – csak az UI-QA átadása és Root
területfeloldása után”**. A `fix/native-phone-ui-final-gate` chat MOST a teljes
ui/**, phoneUItesztek és host screenshot/evidence út tulajdonosa. A megtekintett
PR261 HEAD-je835e533f030c3d6e5f8bd3656570d71a72f5d6e2 volt; ezt nem kezeltük
feloldásként vagy bizonyított zöld QA-ként. A migration korábbi run40342319155
API szerint FAILURE volt; ebből új baseline-ra nem következik siker.

Következő lépés a fő integrátornál: az első szakasz dokumentumainak review-ja,
a G01–G12 döntések triage-ja, majd az UI-QA tényleges átadása után **exact integrált
implementációs baseline és ágfrissítési mód** kijelölése. Csak ezután P0–P6 végrehajtás,
ugyanazon fixture-rel valódi raw PNG és futó üzleti/assertion bizonyíték. A feltételt
nem helyettesíti egy köztes zöld job, PR státusz vagy a mostani docs csomag.

A végső docs HEAD, draft PR link, teljes diff lista és távoli egyezés a PR-leírásban
és a végső átadásban szerepel. A csomag saját átadója ez a README; közös memória-
vagy másik munkás riportfájl nem változott.
