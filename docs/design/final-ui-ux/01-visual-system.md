# Vizuális rendszer és megvalósítható komponensspecifikáció

Ez **tervezési specifikáció**, nem végrehajtott theme/UI patch. A jelenlegi tokenek:
[theme](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt#L28), [tokens](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt#L10), tipográfia [typography](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt#L55); palette katalógus [palettes](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt#L113).
A nyers képszínek nem válnak univerzális hex konstansokká. A bemutató-fixture
`amberGold` (basic) vagy `yellow` (vivid); a mentett `themeKey/themeAccent/themeAccent2`
választást nem állítjuk át. A további31téma egyenrangú, a grafit/arany csak referencia.

## A négy tényleges kép vizuális olvasata

- **01**: a sötét alapon a Folytatás/Indítás egyértelmű nagy hangsúly, másodlagos
  műveletek sötét felületűek. Az aktuális program kijelölt kontúrt + „Aktív” jelzést
  kap; finom vonalak és12–16egységnyi ritmus választja el a listát. A kézi fejléc,
  indokolatlan dupla oldalcím és nagy üres top-margin a natívban kerülendő.
- **02**: a gyakorlat azonosító fejléc és a mérés/sorozat a fókusz; nincs általános
  dashboard dekoráció. Nagy időszám, jól látható state és explicit Stop, alatta
  kompakt pihenő. A kétoldali panelek és a kis ±15célok nem másolhatók mechanikusan
  keskeny/font2.0 nézetre: tördelődő fallback és48dp szükséges.
- **03**: edzéslista és napi mérési kártyák erős név/szám, visszafogott metaadat.
  A térkép teljes felületű modal, piros X-szel. A három Coach-pontszám külön
  jelentésű; a csinos háromszámos panel nem indokol új átlagolást.
- **04**: beállítások szekcionált sorokból, egy kijelölt/lenyitott editorsorból
  és jól elkülönített veszélyes műveletekből állnak. A mockup restore- és
  handoff-feliratát a dokumentált működés írja felül; a bemutató fotó nem useradat.

A fájlok SHA256/dimenzió egyezését a MANIFEST-tel ténylegesen ellenőriztük;
az eredmény a source-evidence.json-ban van. Mind4kép 1024×1536 kompozit tábla,
nem1024dp széles natív telefon. Nem exportálunk belőlük UI bitmapgombokat.

## Színek és állapotok

| Szerep | Meglévő token / alkalmazás | Kötelező megkülönböztetés |
|---|---|---|
| Háttér | bg (jelenleg0E1015) | Scaffold és összefüggő lap; nincs minden komponens mögött külön keret |
| Fő kártya | card (1A1E25), card2 (232933) | card a tartalom; card2 a beviteli/másodlagos felület |
| Szöveg | text, muted | fő adat text; címke/időbélyeg muted; nem .4alpha pici szöveg |
| Fő művelet | accent kitöltés + onAccent | onFill derivált kontrasztot használ; nem fix fehér vagy fix fekete |
| Kiegészítő hangsúly | accent2 + onAccent2 | nem helyettesíti a második elsődleges CTA-t |
| Kis hangsúlyos cím | accentText | a nyers accent helyett a4.5:1-re derivált szövegtoken |
| Kijelölt sor/fül | selectedSurface + selectedBorder + text/state label | border≥3:1, ikon/szöveg jelzi a kiválasztást is |
| Dekoratív elválasztó | line / emphasisBorder | a line nem funkcionális focus vagy hibajelző, mert a3:1 nem garantált |
| Siker | success + kész ikon/szöveg | szín önmagában nem bizonyít mérést/mentést |
| Veszély / hiba | danger felület + dangerText felirat/ikon | normál destructív outline, explicit megerősítés; X a route modalon a kivételes hangsúly |
| Betöltés / részleges / offline | muted/text + ismert progress vagy indeterminate | nem kapnak jóváhagyást jelző pipát; ismeretlen százalék nem rajzolható |

A raw `accent` szövegként sötét Basic témákban nem garantáltan olvasható.
A [tokens](https://github.com/fuloplevente1998/TrainPilot/blob/c116a042229f61b86babbbeb23c5d2c360486e20/android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt#L10) jelenlegi Oklab-lerp és alpha-composite kontrasztpolitikája marad;
nem írjuk újra RGB-közelítéssel. Tényleges renderben4.5:1 normál szöveg,
3:1 nagy szöveg és funkcionális kontúr a minimum. A kiválasztás/fókusz nem csak
opacity-váltás. Semleges/dangerText szöveg nem rendelhető véletlenül danger
háttértokenhez foregroundként (pl. most több errorlabel `colors.danger`).

## Tipográfia (tervezett szerepek, nem mind létező jelenlegi Style nevek)

| Szint | Méret / sorköz sp | Súly | Használat |
|---|---|---|---|
| Screen title | 24 /30 |600 | ha a közös topbar már tartalmazza a nevet, nem duplikáljuk még egyszer |
| Hero név | 24 /30 |700 | következő/folytatott edzés; több sor engedett |
| Szekció | 18 /24 |600 | aktuális program, mai adatok |
| Kártya címe | 16 /22 |600 | név és fő állapot |
| Body/input | 16 /24 |400 | beviteli érték és hiba; nem11sp |
| Meta / címke | 13 /18 |400–500 | forrás, dátum, egység |
| Micro | 12 /16 |500 | csak másodlagos rövid badge, nem fő művelet |
| Fő mérés | 36 /42 |700 | stopper/táv, tabular numerals, egység13sp a szám mellett/alatt |
| Másodlagos mérés | 24 /30 |600 | kétoldali vagy rácsérték |
| Button | 14 /20 |600 | teljes címke, fontscale tördelődik; label rövidítése nem információvesztés |

A mostani TrainPilotTypography30/22/18/15/13/11/10sp szintjei a baseline;
phase2-ben a meglévő Material szerepekhez igazított hunkokkal egységesítjük.
Egy glyph se legyen színben fixTextColor azért, mert a TextStyle onnan származik:
a komponens explicit témaszerepet használ. Rendszerfontot használunk, nincs új
fontdependency. Nincs all-caps hosszú DE/RO cím vagy túlzott letterSpacing.

## Méret, térköz és sarkok

-4dp alapegység; belső mikro-gap4, kapcsolódó sor8, kártya-gap12, oldal padding16,
 szekcióköz24. Kártya padding16 (sűrű adatnál12); a card-title és fő adat8.
- Sarki sugarak: fő card16, hero20, input/vezérlő12, badge8. Chip nem teljes
 pill mindenen. Funkcionális kontúr1–2dp; kiválasztott panel2dp. A nyers képen
 látszó glow/shadow nem kell a jó adat-hierarchiához.
- Vezérlő **minimum48×48dp**, fő CTA minimum52dp magas, tartalommal növekszik.
 `height(42.dp)` és24dp foto-X nem marad. Ikon24dp vizuálisan, touch48dp.
 Soros +/- komponens teljes egységfeliratot tart, nincs9dp széles cél.
-320dp szélesség és1.0font mellett az oldal belseje288dp; két kártya csak ha
 mindkettő értéke/címkéje elfér. Font>1.3 vagy keskeny constraint esetén one-column.
 A layout valódi constraints/fontScale alapján vált, nem eszközmodell-nevekből.
- Footernek és aktív bannernek mért layout hely kell. Nem elég a mostani
 sok képernyőn80/96dp Spacer, ha a footer+font ennél magasabb. Phase2-ben közös
 inset/contentPadding szerződés; a Scaffold és IME inset nem alkalmazandó kétszer.
- Alsó nav öt célja megmarad. A címke legfeljebb két sor, de2.0fontnál új
 mérés/célhossz szükséges; beszédes TalkBack címke mindig teljes.

## Tervezett, újrahasznosítható megjelenítők

Ezek **új vizuális composable-javaslatok**, nem új motor/state/writer:

| Tervezett komponens | Paraméterek forrása | Interakció |
|---|---|---|
| SectionHeader / SurfaceCard | cím, opcionális state text, colors | opcionális meglévő callback, nincs implicit load |
| ActionCard / StatusBadge | meglévő title/subtitle/status/enabled | meglévő onClick; szerep+selected semantics |
| MetricTile | valueText/unit/source/time/isAvailable | opcionális létező részletnyitás; nincs adatkonverziós képlet |
| SessionHero | kanonikus snapshot jelenlegi célja + meglévő resume callback | nem startol új sessiont |
| ExerciseHeader / SetRow | ActiveExerciseUiModel/ActiveSetUiModel, renderedTargets | ugyanezen targetre ugyanaz az editor callback |
| MeasurementCard / RestPanel | timer/gps state + NativeTimerTarget | meglévő toggle/adjust/skip, önálló tick nélkül |
| SourceAndFreshness | meglévő source/date/partial | csak megjelenít, nem dönti újra el a hitelességet |
| InlineIssue / EmptyState | meglévő loading/error/permission/empty + retry | retry csak explicit user action |
| FormSection / FieldRow | meglévő draft mező és save/error | IME/tördelés/fókusz; domain-validátor változatlan |

Képek: csak létező privát fotó, ellenőrzött demo/video vagy meglévő asset.
Nincs új stockfotó élő mérést jelképezve, nincs generált „after” appképernyő.
A ritmus és az ikonográfia a fő vezérlő és adat elkülönítését szolgálja.
Iconok a meglévő Material készletből, azonos24dp stroke/térfoglalással; a belső
entitynevek helyett lokalizált jelentés. A mockupnak nincs új asset-licencígérete.

## Űrlap, navigáció és visszajelzés

Módosított local draft → explicit mentés → meglévő writer eredmény → siker.
Mégse/Back nem ír; pending save közben nem szabad bezárni és késznek jelezni.
Egy sikertelen mentés megtartja az inputot és az értelmezhető hibát; stale szerkesztő
újraolvasást kér. Rotationt megőrző UI Saver csak prezentációs draftot őrizhet,
nem kerülhet token/consent/activity vagy privát bitmap a Bundle-be.
Selection/scroll/query visszaáll; onResume nem új mentés vagy GPS-start.
A mérés-/hibaállapot-frissítés TalkBacknek nem250ms-onkénti live region.

A fájlhatárok és a tényleges callbackek a03-binding dokumentumban vannak.
