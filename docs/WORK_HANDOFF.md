# TrainPilot – aktuális átadás

## PR #271 – P1h programnézet, 2026-10-29

Indulás: `d3ad5fc7` dokumentációs HEAD, az `a81a32c1` alkalmazás-/tesztforrás Quick/Performance/6 Chromium/Phone gate sikeres. A P1g 393px CI-képén a beállítások és programfejléc még egy nagy, egymásba ágyazott kártyán voltak.

P1h: kizárólag `?tp271=1` scoped CSS. A külső programkeret és a második belső keret megszűnik; a programfejléc a canvasra kerül. Az eredeti edzésbeállítások önálló kártyán, a teljes szélességű napok külön kártyákon maradnak. Nem módosul `www/app.js`, HTML, storage, edzésmotor, natív forrás vagy verzió (1.3.71/2735).

A meglévő `p271-webview-workout-entry.cjs` 320/360/393/412px × basic/vivid próbái a keret nélküli szekció mellett a beállítások és program összecsukását/nyitását, az eredeti öt pihenő/progressziómező elérhetőségét, a Mentés callbacket és a programválasztó valódi útvonalát is ellenőrzik. Minden korábbi gyakorlatválasztó/Quick/kültéri/edzésindítás ellenőrzés megmarad. Új kép: `pr271-workout-settings-expanded-393.png`.

Helyi ellenőrzés: Node szintaxis és diff. **Az új P1h kód CI- és képi eredménye még ellenőrzendő**; a P1g PASS nem az új változat bizonyítéka. Következő: pontos forrású kapuk és CI-képek, majd a további képi eltérések. PR OPEN/DRAFT marad; nincs merge, release, telepítés vagy alapértelmezett UI-váltás.


## PR #271 – WebView mockup folytatás, 2026-10-29

**Végső ellenőrzés:** a tesztelt alkalmazás-/tesztforrás `a81a32c14613f1698e3acede2a2587ed636748f1`. Quick `40820138769`, Performance `40820138806`, Phone `40820133396` SUCCESS. Mind a 6 Chromium-shard és az aláírt APK release-gate sikeres; a letöltött natív XML 251 teszt / 0 failure / 0 error / 0 skipped. APK source/package és signer continuity SUCCESS, release-publikálás SKIPPED. A jelen dokumentációs checkpoint nem új alkalmazáskód, kapuismétlés nem szükséges.

P1g első Phone-futásán (`4aeb53e3`, `40819698307`) az új 360px próba a 873px magas viewporthoz tévesen 740px határt használt. A teszt-only `a81a32c1` javítás tényleges `innerHeight`-et ellenőriz; minden elhelyezési assertion megmaradt. A CSS byteazonos a `4aeb53e3` verzióval.

Valóban megnyitott új CI-képek: shard-1 artifact `12874740849`, `pr271-new-header-home-393.png` és `pr271-workout-top-393.png`. A különálló grafitkártyák és teljes szélességű alsó navigáció igazoltan megjelent. Ez nem 1:1 képi elfogadás: az aktív program belső szerkesztője és a Home onboarding sűrűbb a referenciánál. **Következő legkisebb lépés:** az aktív program egymásba ágyazott vizuális kereteinek célzott egyszerűsítése az eredeti szerkesztési/indítási callbackekkel, majd azonos-forrású screenshot és regresszió. Fizikai telefonos elfogadás és alapértelmezett UI-váltás nincs.

Az alábbi audit az induláskor ellenőrzött állapotot és a csomag terjedelmét rögzíti:

- Élő GitHub-audit: PR #271 OPEN/DRAFT, `feat/webview-mockup-redesign`, HEAD `f3215d58dcc3cf55676fe779cfbe57e3bbe37e1c`; a PR és a branch ref egyezett. A korábbi beszélgetésben említett későbbi commitokat a most lekért ref nem igazolta, ezért nem ezekből indulunk.
- Ugyanezen HEAD-en Quick `40767575070` és Performance `40767574914` SUCCESS. Phone `40767565488`: Chromium 6/6 SUCCESS és `release-gate` job `123098145306` SUCCESS, benne regresszió, aláírt APK-build, source/package és signer continuity ellenőrzés. `Publish GitHub Release` SKIPPED. A külön push Quick megszakítása nem írja felül a sikeres PR Quick-futást.
- Ez nem a külön PR #270 Compose-gate elfogadása, és nem fizikai készülékes vagy teljes képi elfogadás. Nincs igazolt új Wear/platform/upgrade teszt erre a WebView-dizájnra; a natív forrás változatlan.
- Következő legkisebb lépés: a meglévő CI-képeket összevetni az eredeti 01-es mockuppal, és csak a látható felületi eltéréseket javítani. A teljes, már zöld gate újraindítása önmagában nem szükséges.
- P1g kód: csak `?tp271=1` alatt sötétebb grafitfelületek, különálló Home/Edzés kártyák, finom fémes keret, a külső oldalkártya vizuális eltávolítása és teljes szélességű alsó navigáció. Az eredeti hat metrika, CTA-k, onboarding, mentett színválasztás megmarad. Basic témák mattak.
- A meglévő bottom-dock browser regresszió 320/360/393/412 px szélességet és az új canvas/rail megjelenést ellenőrzi. A CI továbbra is a meglévő hat shardot használja, új workflow nélkül. Helyi ellenőrzés: Node szintaxis + diff. Az új kód képi/CI eredménye még ellenőrzendő, a korábbi PASS nem vonatkozik rá.
- Kanonikus `www/app.js`, natív motor, main és PR #270 érintetlen; nincs merge, release, verzióemelés vagy telepítés. A végleges UI-elfogadás és az alapértelmezett bekapcsolás továbbra is nyitott.


Frissítve: **2026-10-22**. **#235/#237 mainen, készüléken elfogadva, változatlan 1.3.71 /2735.** Új teljesítménykiadás közzétéve: `v1.3.71-perf-20261022` (Latest) / `wear-v1.3.71-perf-20261022`, az eredeti 1.3.71 kiadás megőrzésével. #214 karbantartási issue marad. [Takarítási szabályzat](REPOSITORY_CLEANUP_POLICY.md).
Először [AGENTS.md](../AGENTS.md), majd ez a fájl.
GitHub az elsődleges; a lentiek pontos forráshoz kötött eredmények.

## Közzétett gyorsított kiadás – 2026-10-22

- [Telefon](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.71-perf-20261022) **Latest**, [Wear](https://github.com/fuloplevente1998/TrainPilot/releases/tag/wear-v1.3.71-perf-20261022) külön stabil kiadás; mindkettő publikált, nem prerelease. Közös tag-/APK-forrás **`5d27aa9c5ebf68d1c206c88a74dea89e660a4937`**, alkalmazáskód az elfogadott `715dc5d` builddel azonos. Verzió változatlan **1.3.71 /2735**, appid `com.repforge.app`, eredeti v2 signer.
- Telefon SHA256 **`850b5e1e597e574dbf5cf55cf82721a3f77879447db9852fa056d2b889d50570`**, méret 3 294 865 byte. Wear SHA256 **`c145afd729f4a9bf91d7cef48f2e9c82ddb265b73dc2b5bb8a15b12a9839e2ea`**, 4 226 963 byte. GitHub asset-digestek egyeznek a letöltött/ellenőrzött CI-csomagokkal. Telefonos forrás ZIP, checksum, mindkét platform buildadatai mellékelve.
- A pontos main-forráson: Quick `39833146328` és Performance `39833146319` sikeres; Phone `39833146419` **112/112 UI, 251/251 natív release**, Wear `39833146349` **167/167**, kerek UI `39833146411` **183/183**. Letöltött XML/JSON szerint 0 failure/error/skipped. Előzetes WebView `39830952962` **3/3**, alkalmazáskód-azonos forráson. [Rövid kiadási bizonylat](builds/2026-10-22-release-1371-performance.json).
- Feltöltés a meglévő `refresh-release-assets.yml` új, opcionális dátumozott címkepár-bemeneteivel, újrafordítás nélkül: sikeres `39834820959`. A helyi uploads API 401-e után a GitHub saját tokenjét használja. Az első publisher-futás draft tag-keresése 404-et adott; `082f51b` javította. A forrás/verzió/checksum/signer/tag ellenőrzések megmaradtak.
- README HU/EN, kiadásjegyzék, változáslista, aktuális fejlesztési fejlécek, tervállapotok és #235 leírása frissítve. A korábbi `v1.3.71` / `wear-v1.3.71` tag **`60fe647`** maradt, eredeti asset ID/méret változatlan; leírásuk az új kiadásra mutat. Nincs visszamenőleges bináris-/tagcsere vagy Play-publikálás.

## Készülékes elfogadás és main merge – 2026-10-22

- Felhasználó a `715dc5d` teszt-APK után: „Elfogadjuk, akkor ez az lehet az új main valtozatlan 1.3.71 / 2735”. A navigációs szín/oldal együttváltási javítás és az azt megelőző, együttes tesztbuild készüléken elfogadva; main merge kifejezetten engedélyezve.
- #237 közvetlenül mainre célozva, hogy az elfogadott #235 előzményt és a #236 teljesítményjavításokat egyszerre vezesse át. Merge commit **`5d27aa9c5ebf68d1c206c88a74dea89e660a4937`**, merge-fa és a bevizsgált ág dokumentációs HEAD-je (`b63cc79`) byteazonos. GitHub #235 és #237 állapota MERGED. Az alkalmazás-/teszt-APK forrása továbbra is **`715dc5d4353ed86116b4596f0cc13916efbe4888`**, a különbség csak handoff-dokumentáció és merge-metaadat.
- Telefon + Wear **1.3.71 /2735**, version-policy ellenőrzés sikeres. Meglévő kiadási tag/asset nincs felülírva, új hivatalos release nincs létrehozva. A main push szokásos CI-kapui külön futnak; a készüléken elfogadott, pontos APK és zöld előzetes kapuk alább.
- A korábbi szakaszok Draft/elfogadás-függő megjegyzései történeti pillanatképek. A fizikai felhasználói elfogadás nem helyettesíti a még el nem végzett ötismétléses medián/R8-okozati mérést; a P1–P4 további tervpontjai külön folytathatók.

## #236 – navigáció lenyomási színe és oldalváltás (2026-10-22)

- Fizikai visszajelzés a `40edb0a` APK-ról: „Sokkal jobb”, de a gomb színváltása még gyorsabb az oldal megjelenésénél. Ez részleges javulás, nem végleges teljesítményelfogadás.
- Új alkalmazásforrás `715dc5d4353ed86116b4596f0cc13916efbe4888`: a meglévő navigációs stílusban a fő nav `:hover/:active` filter/opacity hatása semleges. Az általános gomb-press effekt korábban már lenyomáskor világosított, míg a route a click/felengedéskor indult. A fő nav kijelölése így a megjelenő oldallal együtt változik; nincs új router/wrapper vagy pointer-down navigáció.
- A meglévő `settings-immediate-navigation-236.cjs` valós CDP touchStart/hold/touchCancel/touchEnd próbákkal bővült, basic/vivid témán és 4× CPU-val. A korábbi szinkron Settings/nav, második tap és 320/393 px ellenőrzés megmaradt.
- A pontos `715dc5d` forráson sikeres: Quick `39830962591`, Performance `39830952866`, Phone Release Gate `39830952727` (6/6 shard, 112/112 UI-modul, 251/251 natív release), WebView `39830952962` (3/3). Letöltött UI JSON és natív XML szerint nincs hiba/kihagyás; a 4× CPU érintés/hold/cancel/release regresszió sikeres.
- Készüléken elfogadott [telefonos teszt-APK+forrás ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39830952727/artifacts/12448513131), **1.3.71/2735**, `com.repforge.app`, SHA256 `dd475589160e3073bc91e0294ee995627ec358a1efc149b9f77bc6fccaa830cd`. Source stamp `715dc5d`, csomagolt app.js byteazonos, baseline.prof/profm jelen, v2 signer az eredeti `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`. Fizikai együttváltás és az általános lassulás javítása elfogadva. Verzió/Wear változatlan.

## #236 – átvétel a 2026-10-22-i beszélgetésexportból

- Átvett alkalmazásforrás: `4037e3b`; az export utáni P1.2-javítások is bekerültek. P1.2 egy indulási fitet céloz, valódi útvonalváltáskor szinkron fitet és csak resize esetén RAF-összevonást. A Beállítások panel és a fő navigáció kijelölője azonnal vált; P1.5 konzervatív telefonos R8/shrink és kézi baseline-profil készült.
- A `4037e3b` Performance és natív WebView kapuja sikeres, a telefonos release kapu nem jutott el APK-ig. A 2/6 UI-shardban a `home-coach-startup-consistency` valódi redundáns naplóolvasást jelzett; az 1/6 shard megszakadt. Ezek nem sikeres végső kapuk.
- Új alkalmazáscommit: `40edb0aca9e1439cc19a9943ec5729276834b337`. A régi időzített `rf221CompactHome` nem írja felül a már elkészült `tp166-home-coach` kártyát. Ezzel elmarad a késői readiness/pain naplóolvasás és a visszajavító render. A szigorú meglévő startup-teszt változatlan; nincs új wrapper vagy meghosszabbított cache.
- Helyi célzott ellenőrzés: JS-szintaxis, diff, `runtime-120.cjs`, `release-optimization-236.cjs` sikeres. A pontos `40edb0a` forráson mind sikeres: Quick `39829034572` **163/163**, Performance `39829026605`, Phone Release Gate `39829026707` **6/6 shard, 112/112 UI-modul, 251/251 natív release**, natív WebView `39829026592` **3/3**. A letöltött UI JSON és natív XML jelentésekben 0 failure/error/skipped; a startup, egy-fit és azonnali Settings regressziók is lefutottak.
- Aláírt telefonos [APK+forrás ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39829026707/artifacts/12448441574), **1.3.71/2735**, `com.repforge.app`, **3 294 809 byte**. APK SHA256 `cf40f60c54a4a0118611062075440115b074f03991c846d0b8a7073e82d8cce2`; v2 signer `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`, CI szerint v1.0.0-folytonosság sikeres. A source stamp `40edb0a`, a csomagolt `app.js` byteazonos; baseline.prof (1533 byte) és baseline.profm (244 byte) jelen van, összes DEX 2 394 608 byte. Ez csomagolási bizonyíték, nem készülékes profiltelepítési/gyorsulási mérés.
- [Nyers profil](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39829026605/artifacts/12447809029): mindhárom 0/200/1500 fixture **1 startup history-olvasás**. Egyszeri 4× Chromium boot 159/269/467 ms; első Settings két-RAF proxy 100/134/206 ms. Nem ötismétléses medián és nem fizikai telefonmérés. Wear alkalmazáskód változatlan; a korábbi [Wear APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39683190611/artifacts/12384958699) marad, SHA256 `883ee593d510ff8afcadb0a276c5b6c1ef66df25b3a329779cb9e0e329ccbd7f`.
- A régi workspace 3 nem commitolt startup-statisztikai módosítása megőrizve Git stashben (`preserve earlier uncommitted startup statistics before continuing remote 4037e3b`); nem került vissza az újabb forrásra.
- Következő: fizikai telefonon a friss APK rátelepítése törlés nélkül, teljes bezárás/újranyitás, azonnali Settings és második érintéses bezárás, Naptár/Coach/Programok/Napló váltások, valamint natív plugin/Health/Wear működés ellenőrzése. Az R8 gyorsulása, P1.1 50%-os cél és az ötismétléses mediánok továbbra sem igazoltak. Nincs main merge/verzióemelés/release.

## #236 – P1.1 panel-reflow első célzott javítása (2026-10-22)

- **Alkalmazáskód és APK forrása:** `eae37c62bbc596f5cd98efb4320395a6a7b77c9c`, ág `perf/236-journal-dom`, Draft #237; ezen a ponton verzió **1.3.71/2735**. A korábbi `60fe647` a 2026-10-21-én közzétett eredeti 1.3.71, telefon és Wear kiadási tagja erre mutat.
- Közvetlen `tp155R4RefreshPanel` javítás: `positionPanel()` nav-geometriaolvasása a panel tartalmi DOM-csere elé került; a `scrollTop=y` böngésző-általi korlátozás kiváltja a csere utáni `panel.scrollHeight/clientHeight` kényszerített méréseket. Nincs új globális render/go/refresh wrapper. Célzott `tests/browser/panel-reflow-236.cjs` + UI suite regisztráció; 320/393 px naptár/Coach/beállítás/scroll/bezárás regresszió.
- Pontos forrású **sikeres** CI: [Quick](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39693537448), [Phase 7/CPU](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39693530788), [Phone Release Gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39693530807) (6/6 UI + natív kapu/aláírásfolytonosság), [valódi natív WebView](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39693530858). Aláírt telefonos [APK+forrás ZIP artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39693530807/artifacts/12388909425), APK SHA-256 `0c87aae0ecc172b0cd6366797e2993533d59ca596ad67caf4e857b989f828cd8`. Wear-kód és Wear APK nincs megváltoztatva.
- CI 4× CPU 1500 fixture egyszeri első két-RAF proxy: `96a31a7` naptár 253, Coach 222, beállítások 225 ms; `eae37c6` naptár 253, Coach 194, beállítások 190 ms. **Nem 5 futás mediánja**, futtatási ingadozás miatt nincs P1.1 50%-os elfogadás vagy fizikai készülékes gyorsulás állítva.
- A `60fe647` eredeti kiadás [Phase7](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39519510934) és `96a31a7` [Phase7](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39689250994) külön CI-futásában üres adatnál a boot 208.2→242 ms, Home render 13.3→18.2 ms, Health render 35.1→50.6 ms; Napló 65.8→31.2 ms. Ez nem fizikai telefon mérés.
- **Natív hidegindítási diagnosztika zajos:** `96a31a7` Android emulátor process force-stop után `activityToJsReadyMs` 8008 ms, `speed` fordítással 4891 ms; `eae37c6` kontrollfutásban 4860 ms, `speed` fordítással 6272 ms. A két egymás utáni eltérő futásból nem vonható le következetes R8/profilelőny; P1.5 előtt 5+ azonos körülményű, reprodukálható minta szükséges.
- **Felhasználói fizikai visszajelzés:** az óráról a telefonos app megnyílik és minden adat frissül; a megmaradt probléma **indulási/oldalváltási lassúság**. Új APK fizikai elfogadása és P1.1 50%-os célja nyitott. A következő irány: szétválasztani a natív process cold start, WebView JS ready és valós első nav latency értékeit, és csak igazoltan gyorsabb P1.2–P1.5 változásokat bevezetni. Nincs main-merge/release/verzióemelés.


## Új jóváhagyott P1–P4 javítási sorrend (2026-10-22)

- Az átadott `TrainPilot-javitasi-prompt.md` alapján a **[részletes P1–P4 javítási terv](plans/TRAINPILOT_JAVITASI_TERV_2026-10-22.md)** az irányadó. A dokumentációs tervrögzítés nem új alkalmazás- vagy APK-build.
- Első következő implementáció: P1.1 – a panel-frissítés és az elrendezésolvasások mérése, majd direkt javítása új `render`/`go` wrapper nélkül. 0/200/1500 workout, 4× CPU és **legalább 5 futás medián**, előtte/utána összehasonlítás szükséges.
- P1.4 csak részben készült el: a PR-kártyákhoz az aliasindex van, de a `workout-records-226` `data()` minden paint során még megvizsgálja a teljes history/tombstone raw sztringeket. Azonos elemszámú módosítások frissessége maradjon garantált.
- 2026-10-21-i aktuális `96a31a7` [profilfutás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39689250994) egyetlen 1500-as ciklusában a Naptár ~253, Coach ~222, Beállítások ~225 ms két-RAF proxy; ezek **nem** az ötismétléses végleges baseline-ok. A prompt régebbi 490 ms állítása újramérendő.
- Main merge/release/issue-zárás nincs engedélyezve; a #237 továbbra is Draft. Az `AGENTS.md` és `APP_REFACTOR.md` biztonsági/tesztelési szabályai érvényesek.


## #236 – Napló DOM, PR-alias és navigációs gyorsítás (Draft PR #237, 2026-10-21)

- Ág: `perf/236-journal-dom`; base `refactor/workout-finalization` (#235), [Draft PR #237](https://github.com/fuloplevente1998/TrainPilot/pull/237), [issue #236](https://github.com/fuloplevente1998/TrainPilot/issues/236). Az alkalmazáskód végső teszt-/APK-commitja: `7a12846301553b65a24fd5f1762e4498f85c4f99`. Ezután kizárólag átadási dokumentáció frissül; az APK forrását ne keverd össze a dokumentációs HEAD-del.
- Valódi javítás: 32 kártya első Napló-renderkor, további 32-es inkrementális DOM-hozzáfűzés a teljes adathalmaz dátumszűrésével; régi bejegyzés, részletek, fotók, PR-jelvény és fókusz megőrizve. Dátumszűrő, eltérő history-elemszám és más útvonalról visszatérés a kezdeti keretet visszaállítja. PR kártyák és jelvény-kattintások `cachedRows.find`/alias-újrakeresés helyett változásérzékeny közös indexet használnak. Nincs új `render`/`go` globális felülíró lánc.
- Rögzített Chromium mérés, 4× CPU, azonos runner és 1500 edzés, **#235 `1f1f98d` → #236 `7a128463`**: DOM **21 268 → 718**, Napló első két-RAF paint-proxy **3759 → 594 ms**, második **3311 → 434 ms**. Script első megnyitás **2031,8 → 512,5 ms**, layout **665,6 → 25,0 ms**. Ezek szintetikus értékek, nem fizikai telefonos input- vagy FPS-mérések. [Profil, CPU-nyersadatok](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39684298983/artifacts/12384089792).
- Végső forráson: [Quick sikeres](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39684305244), [Performance sikeres](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39684298983), [telefonos Release Gate sikeres](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39684298953) (Chromium 6/6 + natív tesztek, v2 aláírás és v1.0.0 signer-folytonosság), [Wear kerek UI sikeres](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39684298935) (3/3 shard). A [valódi natív telefonos WebView regresszió](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39684113040) is sikeres a **változatlan végső alkalmazáskódot** tartalmazó `d677339b` commiton; az első hibát sérült emulátorkép-letöltés okozta.
- Teszt-telefon APK 1.3.71 /2735, [ZIP / GitHub artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39684298953/artifacts/12386141046), SHA256 `12917d37b5b934bd25f84fc163676a1ba4c9485a3f5caf2f48016a7ac81bc2d6`. Wear-kód változatlan, [aláírt Wear artifact](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39683190611/artifacts/12384958699) `fa5e18c` CI-forráson, SHA256 `883ee593d510ff8afcadb0a276c5b6c1ef66df25b3a329779cb9e0e329ccbd7f`; végső kerek UI ugyanazon webalkalmazásforráson zöld.
- **Nyitott:** fizikai telefonos hideg első és ismételt Napló/Naptár/Coach/Beállítások navigáció, régi bejegyzések/fotók/PR és telefon/Wear készülékes elfogadás. #235 és #236 továbbra is Draft/nyitott; nincs main merge, verzióemelés vagy új release. A telepített teszt-APK után, explicit jóváhagyással mérlegelhető lezárás/összevezetés.

## Új követési issue – #236, Napló és oldalváltások

- Felhasználói kérésre megnyitva: [#236 – Napló és oldalváltások gyorsítása nagy edzéselőzménynél](https://github.com/fuloplevente1998/TrainPilot/issues/236).
- Tartalma: a küldött képen jelzett okok, a már elkészült #235 indulási javítás,
  az `1f1f98d` forráshoz kötött saját mérés és a fennmaradó ~21 268 elemes Napló.
  A külső kép számai nem jelenlegi APK-mérésként szerepelnek.
- Következő javítási sorrend: korlátozott kezdeti Napló-lista és teljes adatokon
  működő szűrés; igazolt naplóindexelési, panel-/dekorációs és layout-költségek
  csökkentése; adatfrissesség, előtte/utána mérés és készülékes elfogadás.
- A jegy nyitva, implementáció ebben az issue-rögzítési körben nem készült.
  A #235 teszt-APK, main, verzió és kiadás változatlan.

## Folyamatban – #235, hidegindítás és első oldalváltások

- Új készülékes visszajelzés: a korábbi javítással gyorsabb, de még nem megfelelő;
  a maradék főleg **indításkor és az első néhány váltáskor** érezhető.
- A hideg első render a DCL-modulok előtt futott, a közös naplóolvasás nélkül.
  Most a végső modulok telepítése után, a meglévő olvasási scope-ban indul.
  Korai natív finish nem előzheti meg ezt; a restore-barrier megmarad. A helyes
  Mai javaslat kártya ismételt startup-javítása 0 újraszámítás; valódi régi
  kártya javítása megmarad. A profilmodul a tényleges restore utáni adatokból
  választ welcome/Home képernyőt az első renderben, külön korai render nélkül.
- **Végső alkalmazás-/APK-/tesztforrás: `1f1f98d03873282d27e20dc9c9d2c25ae14d2e20`.**
  PR teszt-merge és HEAD fa azonos: `5c5575b1770a79dd50128e27623563a180d32621`.
  Az ezt követő dokumentációs commit nem új alkalmazásbuild.
- Összehasonlító profil: 4× lassított Chromium, ugyanazon runneren `8a49c04`
  ellen. 0/200/1500 edzés indulása **331→144 / 1213→299 / 4975→555 ms**;
  teljes startup history-olvasás **56/74/74→6/6/6**. Egy app-root írás, DCL előtt
  0; első renderben legfeljebb egy journal-olvasás. Nem fizikai telefonmérés.
  A későbbi navigáció idejében nincs igazolt következetes gyorsulás.
- Forrásazonos kapuk: Quick **39658073740: 162/162**; Performance **39658088051**;
  telefon **39658078365: 6/6 Chromium, 109/109 külön modul, 251/251 natív**;
  natív WebView **39658083652: 3/3**; Wear **39658092578: 167/167**;
  kerek UI **39658096720: 183/183** (68+59+56). Minden letöltött XML/JSON-ban
  0 failure/error/skipped. A WebView 200 edzéses Activity-újranyitásai 127/87 ms;
  ez nem folyamat-force-stop vagy fizikai érintési késés/FPS mérése.
- **Új teszt-APK-pár, 1.3.71 /2735:**
  [Telefon](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39658078365/artifacts/12372449697),
  SHA-256 `bc45410b3d4de1135490004ab196c73157da234155a31342837fdc06ca5a140c`;
  [Wear](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39658092578/artifacts/12371179081),
  SHA-256 `50ebcda68e52aae5590caa6b2c19380307e1652121e65008fc000119b9e90f91`.
  Telefon: 31 byteazonos webasset, 805 Git-azonos forrásfájl és pontos fájllista,
  helyes source stamp/verzió/SHA256SUMS. Mindkét APK eredeti v2 signerhash-e
  ellenőrzött; CI apksigner és signer-folytonosság PASS. Wear-kód nem változott.
- Köztes hibák: `c4e6583` friss welcome-regresszióját javítottuk; korábbi natív
  fókuszhiba pontos oka nem igazolt, az elvárás változatlan, végső 3/3 PASS.
  A telefon release első kísérlete Maven-letöltésen állt meg; kizárólag a hibás
  release job újrafuttatása sikeres, forrás változatlan. [Bizonylat](builds/2026-10-21-235-cold-startup.json).
- **Következő:** az új telefon-APK teljes bezárás/újranyitás utáni azonnali
  készülékes próbája, első néhány oldalváltással. Elfogadás még nincs; #235 draft,
  main `bc43d4e`, verzió és kiadás változatlan. Nincs merge vagy issue-lezárás.

### Előző kör – háttérterhelés javítása


- **Új készülékes visszajelzés (2026-10-21):** a `3199528` teszt-APK-val az indulás
  továbbra is nagyon lassú; 15 s várakozás csak részben segít, az oldalváltások
  között még késleltetés érezhető. Pontosítás: **végig megmarad, csak kisebb**.
  A katalógus-optimalizálás nem oldotta meg a hibát.
- Új célzott natív javítás: `MainActivity.onResume` eddig a főszálon várt a
  `HealthJournalStore` közös zárára, SQLite-ot olvasott/írt és háttérfeladatot
  ütemezett. Az egészségnapló-karbantartás most soros háttérszálon fut, csak az
  application contextet tartja meg. A már kikapcsolt régi órás beállításokat
  `retireWatchUi` nem írja újra minden megnyitáskor/inicializáláskor.
- Új natív WebView-regresszió: szándékosan foglalt egészségnapló-zár mellett a
  valódi Activity pause/resume és a UI-szál visszatér a zár felengedése előtt.
  SQLite-teszt méri, hogy a második `retireWatchUi` 0 adatváltozást végez.
  Ezek a konkrét blokkolási okot ellenőrzik; a készülék teljes késésének
  megszűnését még nem igazolják. A végső alkalmazásforrás WebView XML-je
  **39613275281: 3/3, 0 failure/error/skipped**.
- Folyamatos terhelés javítása: a 900 ms-os Wear-poll Task-visszahívása (JSON,
  tartós parancssor) a Capacitor háttérvégrehajtóján fut; a tulajdonosváltás
  meglévő explicit main-looper átadása megmarad. A 3 s-os home-poll a katalógus
  építése előtt a tényleges tárolt bemeneteket, helyi napot/időzónát és nyelvet
  hasonlítja össze. Változatlan bemenetnél nincs history-parszolás/indexépítés.
  Nem elemszám-alapú: közvetlen storage-visszaállítás, azonos hosszúságú módosítás,
  tombstone, program/gyakorlat, draft, beállítás és éjfél érvényteleníti.
  Force/foreground/requestHome újraépít; sikertelen küldés nem lépteti a bemenetet.
- Új célzott Node-regresszió: függőséglefedés a tényleges storage-olvasásokból,
  5 változatlan pollnál 0 history-hívás, direkt azonos hosszúságú szerkesztés,
  nyelv/pihenő/draft/tombstone, force, újrapróbálás és éjfél PASS. Meglévő teljes
  runtime journal-resync és Wear lifecycle PASS. Chromium 200/1500 edzéses
  teszt kiegészítve valós tombstone + szerkesztési frissességre és 0 új indexre
  5 változatlan poll alatt. Mindkét adathosszon 0 új index, friss szerkesztés/törlés;
  az öt üres poll együtt 0–0,2 ms ezen a Chromium runneren. Nem készülékes időmérés.
- **Végső alkalmazásforrás: `6d28ade8a39577995eeedb8b04a6a7fbe2bc6d6e`.**
  Telefonos build-/tesztforrás **`3e373a8e1f67d678beb5c121f37dfa2f09c219bd`**;
  közte csak kanonikus tesztregisztráció és a halasztott küldés tesztfixture-ja
  változott. HEAD és PR teszt-merge fa azonos: `d74ae93c70da46300af6117c10573225d92953b8`.
- Forrásazonos kapuk: Quick **39614621725: 162/162**, Performance **39614621671 SUCCESS**;
  telefon **39614613499 SUCCESS**, Chromium **6/6, 109/109 külön modul**, natív XML
  **251/251**. Wear **39613284803: 167/167**, kerek UI **39613289878: 183/183**
  (68+59+56); utóbbi két forrás `6d28ade`, alkalmazáskód azonos. Minden letöltött
  XML/JSON-ban 0 failure/error/skipped. A köztes tesztfuttatókör-/fixture-hibák
  és javításuk a bizonylatban, nem alkalmazásbeli sikerként szerepelnek.
- **Új teszt-APK-pár, 1.3.71 /2735:**
  [Telefon](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39614613499/artifacts/12351489638),
  SHA-256 `4255a214e1955493fd28666d3a35c5a57681621bcb9936b9b37ebc8bb80fccd8`;
  [Wear](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39613284803/artifacts/12352131837),
  SHA-256 `7a04e1ab9c4a376786eff11e09944bc02dc56c3ac79bca7d7e7059c987781797`.
  A telefon 31 webes assetje byteazonos, a source ZIP 803 fájlja Git-azonos,
  a source stamp/WEAR sidecar és SHA256SUMS helyes. CI apksigner/badging és
  eredeti signer-folytonosság sikeres; letöltött APK-k v2 tanúsítványhash-e ellenőrzött.
  Wear alkalmazáskód nem változott. [Részletes bizonylat](builds/2026-10-21-235-startup-background.json).
- **Következő:** az új telefon-APK készülékes próbája teljes bezárás után azonnal,
  majd külön újranyitáskor 15 s várakozással, több percnyi oldalváltás közben is.
  Készülékes elfogadás még nincs. #235 draft, main `bc43d4e` és a kiadás változatlan;
  nincs merge, release, verzióemelés vagy issue-lezárás.

### Korábbi #235 körök és előzmények

- Új visszajelzés a `1b5e488` teszt-APK-ról: „Rendben, jo lett.” A stopper- és
  görgetési javítás elfogadott. Maradék: appindítás után az első oldalváltások
  kifejezetten lassúak, később gyorsak. Ennek külön célzott profilozása folyamatban;
  main merge/release/verzióemelés nincs engedélyezve.
- Célzott első/második oldalnyitási profil: Chromium 4× CPU, 0/200/1500 szintetikus
  edzés. A készülék konkrét hidegindítási késését nem reprodukáltuk pontosan.
  Layout/stílus költség és nagy háttér-Napló DOM kimutatható, de a geometriai
  műveletek összevonása nem adott következetes teljes javulást. A kísérlet app/index
  módosítása visszaállítva, nincs belőle új alkalmazáskód vagy APK.
- Felhasználói pontosítás: **minden bezárás után visszatér**; nem csak telepítés után.
  Ez visszatérő indulási hiba. Ne tekintsd megoldottnak a korábbi zöld CI vagy a
  stopper elfogadása miatt.
- Új, célzott javítás: a renderen kívül induló órás katalógusépítés is a meglévő
  szinkron `tp7WithHistorySnapshot` olvasási hatókörében fut. A teljes gyakorlat-
  és programkönyvtár egyszer olvasódik, a cache a publikáció végén megszűnik;
  nincs új globális felülíró lánc vagy tartós ajánlás-cache.
  200 teljes metaadatú edzéses 4× CPU-probában 3318 → 346 storage-olvasás,
  history 2 → 1; mért komponensidő 222,5 → 163,7 ms. Ezek Chromium-mérések,
  nem a telefon teljes késésének igazolt megoldása.
- Célzott regresszió 200/1500 edzéssel: history/gyakorlat-/programkönyvtár egy-egy
  olvasás; azonos elemszámú szerkesztés/törlés frissesség és meglévő stopperes
  feltételek PASS. Wear lifecycle, Quick Workout és journal-resync PASS.
- **Új alkalmazásforrás: `3199528a830651d9f459cecf40de1e7b6ec1294f`.** Az utána
  következő `1c93ed1`–`0ae22b8` csak CI/helper/Android teszt; appkód byteazonos.
  Quick **39601996215: 161/161**, Performance **39601996296 SUCCESS**;
  telefon **39602051798 SUCCESS**, Chromium **6/6, 109/109**, natív **251/251**;
  Wear **39602061552: 167/167**, kerek UI **39602066211: 183/183**.
  Letöltött XML/JSON: 0 failure/error/skipped, 109 külön UI-modul.
- Új Android WebView kapu **39606934806: 2/2**, forrás `0ae22b8` (csak teszteszköz
  különbség): 200 edzés, két Activity/WebView újranyitás, 32 navigáció, 8 katalógusminta;
  teljes JSON export bevizsgálva. Azonos katalóguskimenet: **4133 → 213 olvasás**;
  scoped 19,3–52,5 ms, unscoped 104,7–267,3 ms a debug emulátoron.
  A navigáció ingadozó, következetes első köri lassulás itt nem igazolt.
  Szintetikus Android érintéspárok közvetlenül a WebView-ba; két-RAF proxy;
  nem process-force-stop, hardveres input-késés vagy készülék-FPS mérés.
  A korábbi hibás érintés/export futásokat és a javításukat a bizonylat külön rögzíti.
- [Új telefon APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39602051798/artifacts/12346632971),
  SHA-256 `4f8496d6b9c0cbce307b3bb3e2c437e55eaee7c5d886b60a07732f4a114be0d0`;
  [azonos forrású Wear APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39602061552/artifacts/12346686323),
  SHA-256 `49d7f5cff5dc518cd8395a284621678025e484ba4b7185da355e5acdc47cc243`.
  **1.3.71 /2735**; app asset/source ZIP és Wear sidecar commit, SHA256SUMS,
  aláírás/signer-folytonosság ellenőrzött. Wear alkalmazáskód nem változott.
- [Hideg navigációs bizonylat](builds/2026-10-21-235-cold-navigation.json).
  **A korábbi készülékes próba eredménye:** teljes bezárás/újranyitás után lassú,
  15 s várakozás után kisebb, de végig megmaradó oldalváltási késés. A fenti
  új javítás erre a negatív visszajelzésre készült; készülékes elfogadása még nincs.
  A katalógus javulása nem a teljes készülékes hiba megoldásának bizonyítéka.
- Az első kör `655183c` APK-járól felhasználói visszajelzés: nincs dupla edzés,
  törlés után nem tér vissza a bejegyzés; a többi kért ellenőrzés működik.
  Ez készülékes mentési elfogadás, **nem main merge-engedély**.
- Új hibák: Naptár/Coach/Beállítások görgetése 1–1,5 s-ra akad; planknél dupla
  stopper; stopper leállítása után felesleges sorozat-OK; készenléti pont nem érthető.
- Döntés: **a felső intervallumos stopper kerül ki, az eredeti alsó marad**.
  Az intervallumbeállítás az alsó kártyában összecsukott, választható rész;
  nem készül új stopper vagy új globális render-/finish-wrapper.
- Explicit pozitív stopper-stop az eredeti `toggleSet` folyamaton jelöl készre,
  megőrizve visszavonást/haptikát/natív rögzítést. Nulla, implicit pause/halt,
  navigáció és nullázás nem teljesítés. Oldalsó planknél két pozitív oldal kell.
- A Wear háttérkatalógus publikálásakor 130 history-olvasás helyett 2: közös
  publikációs pillanatkép és előtöltési index; törlési ellenőrzés egyszer soronként,
  üres tombstone-listán nincs teljes legacy hash. Nincs tartós, elemszám alapú cache:
  azonos hosszúságú szerkesztés/törlés a következő publikációban megjelenik.
  A már törölt sor a friss órás előzménylistából is kimarad.
- Panelgörgetés/touchmove alatt csak a közönséges home/katalógus küldés vár
  180 ms nyugalomra. Kifejezett force és az aktív edzés parancs-/állapotszinkronja
  nem vár; parancsfeldolgozás közben sincs katalógus miatti halasztás. Folyamatos
  görgetés + nyugtázott parancs célzott próbája PASS; az eredeti frissítés
  automatikusan folytatódik.
- Visszavonás utáni oldalsó stopper: az aktuális sorozat mentett bal/jobb idejét
  mutatja akkor is, ha nincs futó helyi óratulajdonos. A megállított óracache nem
  írhatja felül a kézi szerkesztést. Automatikus stop → visszavonás → nullázás/
  szerkesztés → újrarajzolás és fordított oldalsorrend célzott próbája PASS.
- Mai készenlét képe: Samsung 87, effort-load 45 → meglévő <55 korlát miatt 54;
  külön TP-becslés `70 + (45−70)×0,35`, kerekítve 61. Nem összeadás/átlag.
  Az alacsony load-pont nagyobb jelentett megterhelést jelent. A képlet változatlan,
  a részletezőben HU/EN/DE/RO magyarázat jelenik meg a konkrét 87 → 54 korlátra.
- Helyi célzott eredmény: előtöltési index kompatibilitás PASS; időzítő 8 nyelv/méret
  kombináció, explicit/implicit/0 stop és 200/1500 edzéses publikálás PASS;
  kompakt egy-/kétoldali UI PASS; korábbi mentési integritás 14/14 PASS;
  egyetlen finish-belépő strukturális ellenőrzése PASS.
- **Előző, készüléken elfogadott alkalmazásforrás: `1b5e488b35ca39dfe79ab17e6347b38daf0401a8`.**
  PR-merge és head fa azonos: `1577cb0e9fbc7d2534e3e2bbc4f25bc7f95a4471`.
  Utána csak dokumentációs átadás, nem új alkalmazásbuild.
- Friss, forrásazonos GitHub-kapuk: Quick **39552869084: 161/161**, Performance
  **39552869065 SUCCESS**; telefon **39552881285 SUCCESS**, Chromium **6/6, 109/109**,
  natív XML **251/251**; valódi WebView/háttér **39552884885: 1/1**;
  Wear **39552887694: 167/167**; kerek UI **39552891381: 183/183** (68+59+56).
  Letöltött shard-JSON és XML: **0 failure/error/skipped**, minden UI-modul egyszer.
  A régi, külön OK-ra építő stoppertesztek az új szerződést és valódi visszavonást
  ellenőrzik; a Napló UI-teszt tíz kijelzőméreten is sikeres.
- [Új telefonos teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39552881285/artifacts/12324696670),
  SHA-256 `f9a87c24621131a3b743ba5273f426d29cdc091c9159a51afe8889d542052ad8`.
  APK app.js/index.html/training-107.css byteazonos, forrás-ZIP stampje a fenti commit.
- [Azonos forrású Wear teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39552887694/artifacts/12324303353),
  SHA-256 `922e8b936da0b58b5824a3cc34601b5ea31d923d4cccf2baa90f432a33da8936`.
  Wear alkalmazáskód nem változott. Mindkettő **1.3.71 /2735**, SHA256SUMS,
  aláírás és signer-folytonosság ellenőrzött. Nincs APK-ba ágyazott JSON source stamp;
  telefonos forrásarchívum és Wear sidecar igazolja a commitot.
- [Új bizonylat](builds/2026-10-21-235-ui-scroll-stopwatch.json).
  Következő készülékes próba: Naptár/Coach/Beállítások tartós görgetése; plank explicit
  Stop → kész pipa külön OK nélkül → Visszavonás; oldalanként két pozitív idő;
  visszavonás utáni szerkesztés/újranyitás. A Samsung 87 → 54 magyarázat legyen látható.
  **Stopper/görgetési kör készüléken elfogadva; első navigáció lassúsága külön marad.** #235 draft, main `bc43d4e` változatlan,
  nincs merge, release vagy verzióemelés. A teljes Napló/PR lapozás és általános
  tárolási helyreállítás későbbi kör marad.

## Előző bevizsgált #235 alkalmazásforrás – mentési kör

- [#235 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/235), ág
  `refactor/workout-finalization`. Alap main `bc43d4e`; változatlan verzió **1.3.71 /2735**.
- Tesztelt alkalmazásforrás **`655183c93ba9d63b30d59dd4bea89117ae7b94cb`**;
  PR teszt-merge fa és head fa azonos: `33eb07a5ec273d2d3fd44fa15c32f7f754859edf`.
  Az ezt követő átadási commit kizárólag dokumentáció, nem új alkalmazásbuild.
- A finish korábbi 11 definíciós/hozzárendelési helye és dinamikus wrapperei
  helyett egy koordinátor. Natív reconcile/close, sorozat, stopperek, GPS, naptár,
  részleges megerősítés és értékelés a közös folyamat része.
- Idempotens history-upsert, rollback, tartós draft-jelző; sikertelen natív close
  vagy drafttakarítás nem küldhet „mentve” nyugtát. A teljes lezárás előtt érkező
  natív sorozat bekerül. Sérült history/scheduled JSON lezáráskor nem írható felül;
  általános db.get-helyreállítás nincs még kész. A végleges naplóból az előtöltési
  UI-segédmező kimarad, a draftból nem töröljük idő előtt.
- Friss alkalmazásazonos GitHub-kapuk: Quick **39545876527: 160/160**, Performance
  **39545876543 SUCCESS**; telefon **39545881760 SUCCESS**, Chromium **6/6, 108/108**,
  natív XML **251/251**; tényleges WebView/háttér **39545884307: 1/1**;
  Wear **39545887200: 167/167**; kerek UI **39545889547: 183/183** (68+59+56).
  Minden XML-ben **0 failure/error/skipped**. A 14 új integritási eset GitHubon is PASS.
- [Telefon teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39545881760/artifacts/12321955384),
  SHA-256 `45a642464cd5dc15c308dc38e55b1aecfb36fdcdd55c86be623f6d5b8e36ccf3`.
  A csomagolt app.js byte-azonos; a forrás-ZIP stampje a fenti commit.
- [Wear teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39545887200/artifacts/12320794234),
  SHA-256 `9247c5fd2b09103efe7cc531af3bcb7c0e3a826307a12d3c624a83a14c660e87`.
  Wear alkalmazáskód nem változott; az új build ugyanahhoz a commitpárhoz tartozik.
  Mindkét APK **1.3.71 /2735**, aláírás és signer-folytonosság sikeres.
- [CI/APK bizonylat](builds/2026-10-21-235-workout-finalization.json).
  Main merge/release/verzióemelés nem történt; a mentési kör készülékes elfogadása a fenti új szakaszban szerepel.
- Készülékes smoke: telefonos és órás befejezés egyszeri naplóbejegyzéssel;
  részleges mentés Mégse/Mentés; oldalsó stopper; GPS sikeres Mérés befejezése
  külön sorozat-OK nélkül; tervezett edzés naptárban teljesült.
- Következő kör: [APP_REFACTOR](plans/APP_REFACTOR.md) szerint **Napló/PR indexelés
  és fokozatos megjelenítés**, 200/1500 edzéses méréssel. Ez az 1. kör még nem
  oldja meg a nagy napló megnyitásának teljesítményét vagy az összes auditpontot.

## #214 – GitHub-bemutató frissítve (2026-10-21)

- Kifejezett felhasználói kérésre a GitHub About rövid angol leírása és 11 Topics címkéje ténylegesen elmentve, böngészőben visszaellenőrizve.
- Angol/magyar README: csoportosított aktuális funkciók, 8 telefonos + 2 natív Wear-kép. Új [magyar funkcióáttekintés](FEATURES.hu.md); [képeredet](screenshots/README.md) és SHA-256 jegyzék.
- Képek változtatás nélkül a kiadott `60fe647a` forrás main CI-jéből: Phone **39519510921**, Wear round **39519510953**. Szintetikus fixture-k, nem személyes adatok és nem Play-készülékfotók.
- Kizárólag dokumentáció/képek és repository metaadatok változtak. App-/natív kód, 1.3.71 /2735 verzió és kiadott APK-k változatlanok. Új APK-/emulátorkaput nem indítunk ugyanarra a forrásra.
- #214 nyitva marad a korábban megőrzött Play/partneri/licencelési feladatokkal.

## Lezárva – #234 / 1.3.71 kiadva (2026-10-21)

- #234 merged mainre: `60fe647a98ccdcc7f102d647a6a1cc8db40bbbfa`; elfogadott PR-head `be76bf3753e821cab0d97390fa84fcb5c53b1108`. HEAD, PR teszt-merge és main fája azonos: `4943c34a3aa135e78ed823d54b4206bde25c8c22`.
- Végleges verzió **1.3.71 /2735** mindkét platformon. A lentebbi 1.3.7 teszt-APK a készülékes elfogadás előzménye.
- Készülékes visszajelzés a `cbff3509` APK-ról: a rejtett rekordrészletező megnyílik, de az Új PR jelvénnyel nem zárható.
- A click handler most a korábbi nyitott állapotot váltja; második koppintás teljesen elrejti a panelt és nyitva hagyja az edzéskártyát. A részletek címével is összecsukható.
- A jelvény `aria-expanded` állapota és nyila követi a panelt; bezáráskor a fókusz visszatér a látható jelvényre.
- A meglévő Chromium PR-teszt bővítve nyitás–zárás–újranyitásra, külön cím általi zárásra és HU/EN/DE/RO kis kijelzős, nagy betűs változatokra.
- További készülékes kérés: az Edzésstílus/progresszió zárt panel legyen laposabb; a Mégse láthatóan dobja el a szerkesztést; a sorozat-haptika pipája egyezzen az appéval.
- A belső módpanel 46px-es kompakt fejléccel, dupla függőleges padding nélkül jelenik meg. A Mégse eldobja a függőben lévő választást, visszaállítja a mentett mezőket és becsukja a belső panelt, a külső Edzés beállításai nyitva marad.
- A haptika ugyanazt a `tp155-sync-check` témázott 22px-es négyzet/pipa vezérlőt használja, mint a többi szinkronbeállítás. A mentett kapcsoló működése változatlan.
- A módválasztó/mentés tesztek és a haptika integrált UI-tesztje kibővítve; a Mégse modellteszt helyben sikeres. A PR-részletező célzott tesztje bekerült a kötelező Chromium suite-ba.
- Végső teszt/APK-forrás: `924664ef7882aa59cbedfbcebe6c95605511d9b1`. Quick **159/159**, Performance **SUCCESS**, telefonos Release Gate **39515146159 SUCCESS**, Chromium **6/6, 107/107**, natív XML **251/251, 0 failure/error/skipped**.
- Aláírt, kicsomagolt telefonos APK átadva; verzió **1.3.7 /2734**. SHA-256 `2d2ba088f61cf1c34a9da0252a66d0e8caee41449bf817860aacfc2aaedaaa3a`. A csomagolt app.js/CSS blobazonos a forrással, a source stamp a fenti commit. Aláírás és signer-folytonosság kapu sikeres.
- [Korábbi készülékes teszt-APK bizonylata](builds/2026-10-21-234-pr-toggle-training-ui.json). A felhasználó az alábbi üzenettel elfogadta és lezárta ezt a kört.
- Felhasználói elfogadás: „Zárható, mehet mainre 1.3.71, ez lesz a legfrissebb verzió, release gate stb. leírásban is frissítsük.”
- Jóváhagyott kiadásjelölt: **1.3.71 /2735** telefon és Wear; metaadatok, cache, verziótesztek és kiadási jegyzetek frissítve. Helyi verzió/kiadási tesztek **11/11**, app.js szintaxis sikeres.
- Az 1.3.71 merge előtti kapui: Quick **39518365505: 159/159**, Performance **39518365457 SUCCESS**, metadata **39518365472 SUCCESS**; telefon **39518361442**, Chromium **6/6, 107/107**, natív XML **251/251**; Wear **39518361372**, XML **167/167**; kerek UI **39518361440**, XML **183/183** három shardban (68+59+56). Minden XML: **0 failure/error/skipped**. Aláírás/signer-folytonosság sikeres.
- Main Quick **39519510907**, Performance **39519510934**, telefon **39519510921**, Wear **39519510922**: SUCCESS. Telefon XML **251/251**, Wear XML **167/167**, 0 failure/error/skipped.
- Publikus APK-pár, forrás mindkettőnél a fenti main merge, aláírás/signer-folytonosság sikeres, letöltött artifact és release digest egyezik:
  - [Telefon 1.3.71](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.71/TrainPilot-1.3.71.apk), SHA-256 `f3fc054d9e2142fc7919b0e7beee8c2db35527de2fcb85406c23ab23141e5e68`.
  - [Wear 1.3.71](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.71/TrainPilot-1.3.71-wear.apk), SHA-256 `ea963e7393bb29748fdd3325d3e83e6a715261e5a93bdd8ed38ab5876773e97a`.
- `v1.3.71` explicit Latest, `wear-v1.3.71` latest=false; mindkét tag a main merge-re mutat. Telefon app.js/CSS blobazonos; mindkét APK forrásjelölése ellenőrizve. [Kiadási bizonylat](builds/2026-10-21-release-1371.json).
- Main kerek UI **39519510953 SUCCESS**, mindhárom shard és teljes **183/183** lefedettség sikeres. A mentett snapshot hiánya miatti cold boot figyelmeztetés nem teszthiba.
- Következő: nincs további #234 fejlesztés vagy készülékes jóváhagyásra váró pont. #225 lezárva (történeti audit, #230/#234 által felváltva); #231 külön marad; Play-előkészítés #214 változatlan.

## Elfogadva – #230 / 1.3.7 közzétéve

- **Felhasználói jóváhagyás (2026-10-20):** „Rendben, zárható :) mehet mainre 1.3.7 verzioszamon, a release gatenel, teszteknel ezt vegyü figyelembe.”
- #226 mentett PR-jelvény, #227 kézi sorozat haptika, #228 Napló-alapú gyakorlatonkénti súly- és ismétlés-előtöltés és #229 tényleges sorozat-RIR: felhasználó által elfogadott #230 csomag.
- #228 korábbi készülékes hibajelzés: a tervezett edzés rendben volt, a gyors edzésnél a célismétlés felülírta a Napló alapján előtöltött tényleges ismétléseket. Javítva mindkét gyorsedzés-indítási úton; a hat Chromium shard integrált tesztet futtat.
- **1.3.7 /2734** összehangolt telefon/Wear verzió: `package.json`, `package-lock.json`, két SOURCE_VERSION, két Gradle, `www/app.js`. A verziópolitikai tesztek az 1.3.7 címkéket és a 2734 kódot várják.
- **Merge előtti kötelező kapuk:** forrásazonos Quick/Performance, 6/6 Chromium, telefonos és Wear aláírt release gate, óra kerek kijelző teszt. Az előző `49784cb` 1.3.6 /2733 sikeres kapui a funkciót igazolták, de nem bizonyítják az 1.3.7-es buildet.
- **Main merge és kiadás kizárólag sikeres 1.3.7 kapuk után.** A mainre kerülő azonosított forrásból az önálló telefon/Wear workflow-k külön `v1.3.7` / `wear-v1.3.7` kiadást állítanak elő. Verifikáció és #226–#229 lezárás a tényleges merge/kiadás után.

## Aktuális – #223 mainen, #214 külső előkészítése nyitott

- Felhasználói merge-jóváhagyás 2026-10-20 18:21 (Europe/Budapest): „Minden mehet mainre”.
- **#223 merged mainre: `c9a395e56259849a2182c8b7c8b4e0d9c5d1ab7c`**; merge fa `6b978991c22ae20380bb6cab322678082c171686`, byteazonos az elfogadott `7ea45f3305366f5253fefef7df9f42ec77dfc1a0` PR-head fájával.
- Ág: `maintenance/214-repository-hygiene`, alap main `a3a4ddce2250c32ef5b55e9ed8733b6a3aa85bda`.
- Angol + magyar README, 37 release dátum/platform szerinti jegyzéke, friss családos változáslista és Play-checklist, proprietary csomagjelölés.
- Új verzióegyezési ellenőrzés és célzott tesztek; csak leírásfejlécet módosító release-karbantartás, védett eredeti szöveg/tag/melléklet-metaadatokkal.
- Helyi célzott teszt: 8/8; verzió 1.3.6 /2733, eredeti kiadási feljegyzések és alkalmazás-/natív forrás változatlan.
- [#223 – beolvasztott PR](https://github.com/fuloplevente1998/TrainPilot/pull/223), ellenőrzött kód/metaadat HEAD: `3275b97cc2a6d1342ed866e523d24c807f73da60`; fa `c72d127e14c3d15ddfc0b9d5654da6f079e05f13`. A PR-merge tesztfája byteazonos.
- Quick [39454775410](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454775410): **154/154 + 8/8 karbantartási teszt**, Performance [39454775235](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454775235): **SUCCESS**, metadata PR-validálás [39454774998](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454774998): **SUCCESS**.
- A tényleges release-leírás rendezés [39454564328](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454564328) sikeres a `58bd1b1` forráson. **37/37 eredeti szöveg hash-azonos; 37 tag és 154 melléklet metaadata/digestje változatlan**, független GitHub/ls-remote utóellenőrzéssel. [Riport](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454564328/artifacts/12281085093).
- Végső PR-head (`7ea45f3`) Quick **39455207276**, Performance **39455207248**, metadata-validálás **39455207238**: mind SUCCESS. A tesztelt PR-merge fa egyezik a HEAD-del és a tényleges main merge fájával.
- Automatikus main kapuk a merge `c9a395e` forrásán: Quick **39456946831**, Performance **39456946849**, Phone Gate **39456946879**, Wear Gate **39456946991**, Wear round UI **39456946769**; az átadás rögzítésekor még folyamatban. Ez a kizárólag átadást rögzítő dokumentációs commit nem indít duplikált kapukat; a karbantartási és alkalmazáskód változatlan.
- Verzió **1.3.6 /2733**; a merge nem módosított taget vagy közzétett APK-mellékletet és nem publikált Playre. Következő: a #214 megőrzött külső Play/Samsung feladatai; külön #224 audit Draft PR is nyitott, nem része a #223 csomagnak.
- #214 nyitva marad a [rögzített külső Play/Samsung/aláírás/licenc feladatokkal](MAINTENANCE_214.md).
- A felhasználó #184/#197 main engedélyét 2026-10-20-án újra megerősítette; GitHub szerint mindkettő már completed, #222 merged `f413a3a`.

## Elfogadott alkalmazás – #222 mainen, funkcionális kör lezárva

- Felhasználói elfogadás/lezárás 2026-10-20 17:12 (Europe/Budapest):
  „Zarhato ez is, igy akkor kb minden mar nem ?”
  Ez a legutolsó Samsung/Regeneráció-javítás elfogadása és a korábban feltételesen
  jóváhagyott RIR utáni #222 integráció lezárási pontja; nem kitalált részletes mérési napló.
- **#222 merged mainre: `f413a3a475cf31a4e596429fe1c8dcf3e1e135df`. #184 és #197 completed.**
  Main merge fa `791db4a8608a27bdcbfc31490a8cbf427b3fd307`, byteazonos a PR-head fájával.
  A korábbi dokumentációs eltérésen túl a bevizsgált alkalmazásforrás változatlan:
  `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`.
- Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A végső dokumentációs HEAD Quick
  **39441673066** és Performance **39441673033** is sikeres.
  A merge után automatikusan induló main futások külön futások; eredményüket nem találjuk ki.
- Verzió **1.3.6 /2733**, Wear változatlan. Nem történt verzióemelés vagy új release.
  A már telepített, elfogadott [telefonos APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379)
  alkalmazásforrása azonos a mainre került kóddal.
  [CI/APK bizonylat](builds/2026-10-20-222-recovery-tiles.json);
  [lezárási bizonylat](builds/2026-10-20-222-accepted.json).
- **Egyetlen nyitott issue: #214**, karbantartás és Google Play-előkészítés.
  A Samsung nyilvános/production partner-hozzáférés ellenőrzése is ott marad;
  saját készülékes siker nem partnerregisztrációs igazolás. Nyitott PR nincs.
- A jelenlegi funkcionális fejlesztési sor lezárult. A dizájnmódosítások lehetnek
  a következő külön feladat; még nem kezdődtek el. Rögzített ötletek:
  [UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md), külön későbbi tesztágra.

## Történeti #222 – Samsung-pontszám olvasható, Regeneráció-csempe egységesítése

- Felhasználó 16:24-es képe **sikeres készülékes Samsung-olvasást igazol** az e97fe1e APK-n.
  Ez nem teljes PR/main elfogadás és nem külön egyezésigazolás a Samsung Health kijelzésével.
- Kérés: a Regeneráció csempe is ugyanazt a mai külső pontszámot mutassa.
  A csempe eddig friss saját alvás/HRV rekordhoz kötötten, Coach-korláttal számolt.
  Most a hiteles mai Samsung-pontszámot mutatja Home/Egészség oldalon akkor is,
  ha külön alvás/HRV-rekord nincs. Külső adat hiányában a korábbi saját becslés marad.
- Közös egész számú kijelzés: a képen látott SDK-tizedes érték 55/100-ként jelenik meg.
  A nyers SDK-pontszám és a Coach fájdalom/terhelés védelmei változatlanok.
  Kártya/csempe/Coach forrássor azonos formázást használ. Natív és Wear kód érintetlen.
- Az új teszt elkapta a nyitott panel mögött elmaradó csempefrissítést; javítva csak
  a read-only pontszám közvetlen DOM-frissítésével, panel/draft/scroll megőrzése mellett.
- **Végső alkalmazásforrás:** `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`;
  PR-merge fája is `8332fd6ffa21d0281af7bc5f702f928ecdcef6e7`.
  Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**. Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A Samsung-teszt egész számú kártya/Health/Home
  csempe-egyezést igazol frissítéskor, nyitott panel és külön alvásrekord hiánya mellett.
- **Telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379),
  1.3.6 /2733, SHA-256 `3798a8e679f9a5a16b013be774f918e62eca24e0070b36084a7d2e1349cf5e28`.
  app.js/index.html byteazonos, forrásjelölés/SHA256SUMS/aláírás és signer-folytonosság helyes.
  [Bizonylat](builds/2026-10-20-222-recovery-tiles.json). Wear APK változatlan.
- **Következő:** új APK-n csempe/kártya egyezésének készülékes visszajelzése.
  #222 Draft, main/verzió változatlan, nincs merge/kiadás/issue-lezárás.

## Korábbi #222 – 1001-es napi lekérdezés javítása

- Felhasználó 16:07-kor az új APK képernyőképét küldte: **SAMSUNG_HEALTH_1001**.
  Samsung dokumentáció: ERR_INVALID_INPUT (érvénytelen bemenet); nem igazolt partnerhiba.
- Konkrét kódhiba: a napi startDate és endDate azonos dátum volt, mindkettő inclusive.
  A valódi Samsung SDK ezt azonos éjféli kezdő/végű LocalTimeFilterré alakítja.
  Javítás: pozitív [mai éjfél, következő éjfél) helyi dátumintervallum; a holnapi pont
  továbbra is kizárt. Nincs másnapi vagy saját becslés Samsung-pontként felcímkézve.
- Natív teszt a tényleges buildelt SDK-kérés LocalTimeFilterét vizsgálja; 1001-et okozó
  nulla hosszúságot minden energiaolvasó teszt elutasít. Új regresszió 23/24/25 órás
  budapesti napokra és a holnapi éjféli pont kizárására. Korábban a mock ezt nem nézte.
- **Alkalmazásforrás:** `e97fe1e31a824640ea78c1cf39b45c1c572456db`;
  Quick/Performance PR-merge fa is `d5f7d46679560ee44fcf7615d66c393b9ad25a0c`.
  Quick **39436835835: 154/154**, Performance **39436835798: SUCCESS**,
  Phone Gate **39436826354: SUCCESS**. Chromium **6/6, 105/105**, letöltött JSON-ok
  forrása/duplikációmentessége ellenőrizve. Natív XML **249/249, 0 failure/error/skipped**;
  valódi SDK-szűrő/DST regresszió és a régi nulla hosszúságot igazoló teszt sikeres.
- **Új telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39436826354/artifacts/12271587790),
  1.3.6 /2733, SHA-256 `52eecad1878da78d20d773cb35193ed27482c6464cb14215de75e8c499b98fc8`.
  app.js/index.html és a forrásarchívum natív kódja byteazonos, forrásjelölés és SHA256SUMS
  helyes; apksigner és eredeti signer-folytonosság sikeres.
  [Bizonylat](builds/2026-10-20-222-samsung1001.json).
- Forrásváltozás csak telefonos Samsung-olvasó és natív teszt; felület/Wear/verzió azonos.
  **Következő:** új APK telepítése, Regeneráció → Samsung-pontszám frissítése;
  a mai Samsung-érték egyezésének készülékes visszajelzése. Készülékes siker még nem
  igazolt; #222 Draft, main `1393982d`, nincs merge/verzióemelés/issue-lezárás.

## Korábbi #222 – Regeneráció alá rendezés

- Felhasználó az új APK-ról képet küldött: Samsung Healthen telefonon/órán van mai
  pontszám, TrainPilot az általános „olvasás nem sikerült” állapotot mutatja.
  **Ez sikertelen készülékes teszt**, a korábbi zöld CI nem igazolja a Samsung-hozzáférést.
- Kérés szerint a Samsung-kártya a Regeneráció megnyitható részébe került;
  külön felső szintű kártya nincs. Frissítés után a panel nyitva marad.
- Az önálló olvasás állapotellenőrzése is kizárólag ENERGY_SCORE engedélyre kérdez;
  más adattípus nem akadályozhatja. Natív SDK-hibakód megőrizve aszinkron és belső
  platform-kivételen át; részleges olvasásnál is strukturált mezőhiba jut a felületre.
- A felület külön jelzi a 2000 engedélyhiányt, 2003 app-hozzáférési korlátot,
  3001 frissítési igényt és 3000/3002/3003 Samsung-beállítási akadályt. Nyers
  kivétel/adat nem kerül ki; diagnózishoz hibakód látszik. Hiánynál marad a TP-becslés.
- Samsung hivatalos API-feltétel: csomagnév + release aláírás + engedélyezett
  adattípusok partnerregisztrációja kell normál felhasználói hozzáféréshez.
  https://developer.samsung.com/health/data/process.html
  https://developer.samsung.com/health/data/api-reference/-shd/com.samsung.android.sdk.health.data.error/-error-code/index.html
  A konkrét készülékhibát kód nélkül **nem** minősítjük biztos partnerhibának.
- **Bevizsgált alkalmazásforrás:** `ae82d481b31d8d01394ce41d08ef003c8847d0c1`.
  PR-merge fája is `4056ab3898c1386ea61969c330c06287b612380f`, azonos alkalmazásforrás.
  Quick **39423620810: 154/154**, Performance **39423620964: SUCCESS**.
  Phone Gate **39423616111: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **248/248, 0 failure/error/skipped**. Új engedélyhatár- és kivételláncteszt sikeres;
  Samsung Chromium: Regeneráción belüli kártya, nyitva maradó panel és hibakód rendben.
- **Új telefonos teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39423616111/artifacts/12264688934),
  1.3.6 /2733, SHA-256 `49426605ba22046bdba3051004528e70814c0b62de9714c372c488ef67ff752d`.
  APK app.js/index.html byteazonos, forrásjelölés és SHA256SUMS helyes, signer
  az eredeti v1.0.0-val egyezik. Wear változatlan. [Bizonylat](builds/2026-10-20-222-recovery.json).
- **Következő készülékes próba:** Egészség → Regeneráció → Samsung energiapontszám,
  egyszeri frissítés/engedélyezés; ha hiba marad, a konkrét hibakód szükséges.
  Ez a build az adatolvasás és hibaazonosítás javítása, nem igazolt készülékes megoldás.
  #222 Draft, main/verzió változatlan, issue-k nyitva.

## Korábbi #222 – teljes CI sikeres, teszt-APK kész

- Integráció: `feat/184-197-coach-integration`, **#222 nyitott / Draft**.
  **Bevizsgált alkalmazásforrás:** `a8bec13630136d89128f259bd065f6c2f57b2cea`.
  Quick/Performance PR-merge fája byteazonos: `889b18da87d94f3dc38cbb37b9629ecab2927e71`.
  A későbbi dokumentációs commit nem új alkalmazásbuild.
- **Quick 39391181566: 154/154; Performance 39391181636: SUCCESS.**
  **Phone Release Gate 39391176400: SUCCESS; Chromium 6/6, 105/105**,
  mind a 6 letöltött shard-JSON pontos forrásra és duplikáció nélkül ellenőrizve.
  Natív XML: **246/246, 0 failure/error/skipped**, benne a Samsung SDK olvasó 8 esete.
- **Telefon teszt-APK: 1.3.6 /2733**, [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39391176400/artifacts/12249603397).
  SHA-256: `0352cbfc68b6f46dc402f82a267878bc1f0aaec2035e3514e782606c2bf7fb80`.
  A letöltött APK app.js/index.html byteazonos a bevizsgált forrással; source stamp helyes,
  apksigner és eredeti v1.0.0 signer-folytonosság rendben. Wear forrás/APK változatlan.
  [Ellenőrzési bizonylat](builds/2026-10-20-222-rir-samsung.json).
- **#197:** opcionális valódi 0–10 RIR, közvetlen számmezőn; azonnali draftmentés
  újrarajzolás nélkül; hibás szerkesztés nem hagy korábbi mérést. Reload, edzéslezárás,
  a tényleges lazy Napló és a következő konzervatív Coach-ajánlás E2E sikeres.
  A cél-RIR nem mérés, hiányzó cél nem implicit 0; nincs automatikus súlyátírás.
- **#184 felhasználói hiba:** Samsung Healthben van pontszám, TrainPilotban nem látszik.
  Javítás: külön napi ENERGY_SCORE olvasás, külön engedély, a lépés-/kalóriaforrástól
  független automatikus frissítés; mai hiteles Samsung-pont elsődleges, hiány/hiba/
  engedélymegvonás esetén saját becslés. Nincs kettős pontozás, fájdalomkorlát megmarad.
  Az Egészség oldalon adat nélkül is látszik a pontszámkártya és az ok/frissítés.
  Az önálló olvasás nem írja felül a teljes Samsung-napi rekordot. Éjfél kizáró végpont javítva.
- **Felhasználói felhatalmazás:** „ha a RIR javítás bekerül, mehet mainre” feltételes
  jóváhagyás, majd kifejezett #222 feltöltési engedély. Ez nem sikeres készülékes
  Samsung-tesztbeszámoló. A közben jelentett pontszámhiba új APK-n még visszaellenőrzendő.
- **Következő:** APK telepítés; Egészség → Samsung energiapontszám, szükség esetén
  egyszeri engedélyezés. Egyezzen a Samsung Health mai értékével; hiánynál fallback.
  Ellenőrizendő a pihenőidők megőrzése APK-frissítés/Drive-visszaállítás után és releváns
  telefon/Wear használat. A készülékes eredményt ne találd ki.
  Main továbbra is `1393982d`, nincs merge, verzióemelés, release vagy Issue-lezárás.
  Dizájnmódosítások a funkcionális lezárás után, külön későbbi feladatként jönnek.

## #216 lezárva, 1.3.6 mainen

Felhasználói engedély: **„Rendben, akkor mehet, zarhatjuk ezt a teljes részt es akkor 1.3.6.lesz”**.
Ez lezárási/main/kiadási jóváhagyás; részletes fizikai tesztbeszámolót nem találunk ki.

- **#218 merged `2059770c0eadf9c9e66929635865861667404d41`; #216 completed.**
  Órás menü: aktív edzés/Gyors edzés elöl; Naptár/Programnapok közös csoportban.
  Új kültéri gyors/tervezett edzés előtt látható, aktivitásonként megjegyzett GPS-választás,
  futó edzésről közvetlen állapot/kezelés. Reconnect/render nem indít rögzítést.
  Kézi gyakorlatváltás megőrzi a pihenő eredeti határidejét és automata fázis/cél/tulajdonosát.
  Telefonos működés és GPS-adatút megmaradt. HU/EN/DE/RO.
- **#219 merged `98be66221430202715aafa9a71583bbbd22d4fd3`; 1.3.6 /2733.**
  Mindkét app metaadatai és appfelirat, SW cache, verzióelvárások és kiadási jegyzetek frissítve.
  Main teljes fája byteazonos a bevizsgált `76fa539` PR-head fájával:
  `d0f5487aa3338369c879126b7d2ca15cbbd4e3b3`.
- #216 alkalmazáskód `c04851f`; utána csak tesztjavítások. A home fixture törli az aktív edzést,
  ezért a felfelé gesztus után **Gyors edzés** az első látható menügomb, nem Napló/Folytatás.
  Az eredeti azonnali láthatósági, gesztus- és geometriai feltételek megmaradtak.

## Ellenőrzött kiadási forrás

- Quick **39231417696**, Performance **39231417699**: sikeres (`76fa539`).
- Telefon **39231412495**: Chromium **6/6**, JVM **243/243** (`76fa539`).
- Wear **39231317332**: JVM **159/159** (`7b600e4`).
- Ownership **39231417723**: **81/81**; kerek UI/platform **39231317394: 183/183**.
- Minden vizsgált XML: **0 failure/error/skipped**. Külön #216 kerek UI
  **39231315228: 183/183**; új GPS/pihenő/menü tesztek mind az öt kijelző/betűméreten.
- `7b600e4..76fa539` csak AGENTS/átadás/bizonylat; alkalmazás- és tesztkód byteazonos.
- Aláírás folytonos a nyilvános v1.0.0-val; signer SHA-256:
  `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- [Bizonylat, APK-hashek és források](builds/2026-10-19-release-136.json).

## Végleges, publikált 1.3.6 APK-pár

Mindkét kiadás/tag forrása **`98be66221430202715aafa9a71583bbbd22d4fd3`**, 1.3.6 /2733:
- [Telefon APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.6/TrainPilot-1.3.6.apk),
  SHA-256 `4b3b2fd90b38912a7a51157ed18c337f361f5e3f5521df69d10a64697c4cc987`.
- [Wear APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.6/TrainPilot-1.3.6-wear.apk),
  SHA-256 `ac9d16066df7f63290bca20656bf883391e41bbd1399cde397b9292176233958`.

Main telefon **39233098299**: 6/6 UI, JVM XML 243/243; Wear **39233098238**: JVM XML 159/159;
0 failure/error/skipped. Mindkét apksigner és eredeti signer-folytonosság sikeres.
A letöltött main artifact APK-hashek egyeznek a nyilvános release asset digestjeivel,
a telefonos forrásarchívum és Wear SOURCE_VERSION egyaránt `98be6622`-t igazol.
Main Quick **39233098560**, Performance **39233098199** sikeres.
Kerek UI **39233098292** további automatikus main futás; az azonos teljes forrásfa
183/183 kapuja a fenti **39231317394** futáson XML-ben igazolt. Élő státuszát GitHubon ellenőrizd.
A későbbi dokumentációs HEAD nem új alkalmazásbuild; a két APK tényleges forrása a fenti commit.

## Külön maradó backlog / lezárt előzmények

- **#206**: maradék aktivitásnézet/UI. Az explicit sikeres telefonos GPS „Mérés befejezése”
  külön sorozat-OK nélkül már elfogadott; térképbezárás/hibás/implicit stop nem teljesítés.
- **#197**: választható edzésstílusok/progresszió; **#184**: Samsung Energy Score;
  **#214**: karbantartás/Play. Az 1.3.6 ezeket nem zárja le.
- #193/#202 önálló órás GPS és telefonos naplótérkép + offline átadás/visszavétel elfogadva;
  #213 main `311f507`, #193 completed. Az órán nincs térképrajzoló: az óra rögzít, a telefon rajzol.
- #215 kompakt Intervallumok elfogadva/main `c67006b`; kétoldali UI nem engedélyez egyórás motort,
  két pozitív idő kell a kész állapothoz. #212 korona/lünetta #217 main `102eb366`, elfogadva.
- #188 visszavonás és #189 folyamatos 3 mp feloldás elfogadva/mainen. #192/#211 sztornózott.
- [Külső UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md): csak rögzítésre engedélyezve,
  későbbi megvalósítás külön tesztágon. [Részletes #193 terv](wear/P2_193_ONALLO_ORAS_EDZES.md).
- Régi hosszú átadás: `885b6df:docs/WORK_HANDOFF.md` a Git-történetben; történeti státuszai elavultak.

## Történeti környezeti megjegyzés (új munkamenetben ellenőrizendő)

Korábban: CLI auth lejárt (gh 401, git fetch/push credential nélkül), GitHub connector működik.
Ne írj ki credentialt, ne force-pusholj. Contents/Git tree/commit + expected_sha CAS használható.
Helyi `local-release-136` más commitazonosítókat tartalmaz, alkalmazásforrás azonos; **ne pushold
vakon a helyi feature ágat**. A távoli main/PR-head a kanonikus. APK-hoz GitHub artifact permalink
(ZIP, benne APK) vagy valódi release letöltés kell, belső /workspace link nem használható.
