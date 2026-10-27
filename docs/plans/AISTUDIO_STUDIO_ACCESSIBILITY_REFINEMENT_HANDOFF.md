# AI Studio feladat: a meglévő Studio UI végső hozzáférhetőségi és nyelvi finomítása

Ez a feladat a felhasználó AI Studio Android/Compose PROTOTÍPUSÁRA vonatkozik.
A natív beépítést Root közben végzi a PR270-ben; erre a munkára nem várunk.
A következő blokk egyben bemásolható az eredeti AI Studio projekt beszélgetésébe.

---

Folytasd a már meglévő TrainPilot Android/Jetpack Compose dizájnprojektet.
Ne tervezz új alkalmazást: a jóváhagyott látványt finomítsd, a következő hibákat
javítsd végig, és adj ellenőrizhető kódot. Ne csak javaslatlistát vagy látványképet.

**Pontos bázis:** az általad készített `trainpilot-oct-27-2026-3-58-04-pm.zip`,
SHA256 `574f17f639b91037a6e0bdea428f350d4c5950307227511eafc32e7f64bcefcc`.
Ez a `com.example` forrású, `com.aistudio.trainpilot.tpux01` demóprojekt.
A valódi alkalmazás másik projekt (`com.repforge.app`): azt nem írhatod át.

## Kötelezően megőrzendő

- Az öt főfül FELÜL marad: Kezdőlap, Edzés, Napló, Fejlődés, Egészség.
- A grafit/arany látvány, kompozíciók, meglévő32paletta és HU/EN/DE/RO maradnak.
- A prototípus DEMO értékei, demófeliratai és edzés nélküli állapot-/nézetváltói
  maradjanak használhatók: ezek a felhasználó tudatos ellenőrző eszközei.
- Valós funkciógombot ne törölj azért, mert most demócallbacket használ.
- Meglévő testTag, callback, képernyő, állapot és interakció nem tűnhet el.
- Ne hozz létre új workout/timer/recovery/GPS/Health/backup/migrációs motort,
  ne módosíts modellsémát vagy palettaazonosítót, ne pusholj GitHubra.

## 1. Rendszerbetűméret és keskeny kijelző

A `ui/theme/Theme.kt:67` jelenleg ezt teszi:
`(currentDensity.fontScale * fontScaleMultiplier).coerceIn(0.85f, 1.6f)`.
A felső korlát elrejti a valódi2.0-s rendszerbetűméretet. Javítsd úgy, hogy
alapértelmezett multiplierrel a rendszer teljes méretezése érvényesüljön.
A prototípus saját méretválasztója maradhat; ne használd a rendszerérték levágására.

A `TrainPilotCommonComponents.kt:377–423` TopNavPillItem egysoros10sp-s
feliratai ellipsisszel vágnak.320dp-n és fontScale2.0-val a német/román címkéknek
is teljesen elérhetőnek kell lenniük. Lehet adaptív, vízszintesen görgethető
felső fülsor vagy más, a jóváhagyott normál nézetet megtartó megoldás.
Ne zsugorítsd vissza a betűt, ne rejtsd el a feliratot, ne tedd vissza alulra a sávot.
A kiválasztott fül legyen elérhető görgetéssel, fókusz és kijelölés maradjon helyes.

Járd végig a hero-kártyákat,2/3oszlopos metrikákat, edzésnapokat, szerkesztőket,
aktív mérési módokat, összegzést és dialógusokat. Hosszú név/érték/egység/forrás
ne fedje egymást. Szükség esetén intrinsic egyenmagasság, wrap,1oszlop vagy scroll.
Egy dialognak legyen átgondolt scrolltulajdonosa; hosszú bevezető ne nyomja0magasságra
az összehasonlító/adattáblát. A fő műveletek és a bezárás elérhetők maradjanak.

## 2. Érintési cél és akadálymentesség

- Minden valódi interaktív gomb/ikon/sor érintési célja legalább48×48dp legyen.
  A dekoratív ikon rajzolata maradhat18/24/32dp. Ne növeld indokolatlanul az egész
  kompozíciót; a vizuális méret és az érintési cél külön kezelhető.
- Konkrét ellenőrzési pontok: HomeScreen.kt:212 `.height(44.dp)`;
  WorkoutAndPlanningScreens.kt:240/426 `.height(42.dp)`;
  ugyanott:1107/1120 a32dp-s mínusz/plusz gombok;
  CommonComponents.kt:1301 a32dp-s bezáró. Vizsgáld a többi valódi vezérlőt is.
- A főfülek `Role.Tab`, selected és enabled/disabled szemantikát kapjanak,
  a csoport selectableGroup legyen. Puszta kattintás/szín nem elég.
- Műveleti ikonhoz pontos lokalizált contentDescription, dekoratívhoz null.
  Az ikon+felirat ne mondja ugyanazt kétszer a képernyőolvasóban.
- Szám/egység/forrás ne vesszen el a képernyőolvasóból; a státuszt a szín mellett
  szöveg is jelezze. Csak indokolt szemantika-összevonás, megmaradó testTag-ekkel.

## 3. Teljes négynyelvű felület

A `localization/TrainPilotStrings.kt` és a screenfájlok alapján járd végig az
összes funkcionális feliratot, tooltipet, hibát, állapotot és tartalomleírást.
Példa jelenlegi beégetett magyarra: HomeScreen.kt:608
`Aktív kalória ma: ... • Mai edzés: ...`; WorkoutAndPlanningScreens.kt:685 `Aktív`.
A helyes `LocalizedStrings`/AppLanguage típust használd, ne találj ki új natívAPI-t.
Az EN/DE/RO módnak ténylegesen fordított szöveget kell mutatnia, nem csak egy
nyelvjelzőt. A demóadat lehet ugyanaz a négy nyelven, felirat és formátum ne legyen
véletlenül magyar. A direkt DEMO állapotváltókat is lokalizálhatod, de ne távolítsd el.
Meglévő lokalizációs kulcsot ne nevezz át; az új kulcsok mind4nyelven készüljenek el.

## 4. Kontraszt, megőrzött megjelenés

Ellenőrizd mind32paletta normál/muted szövegét, kiválasztott füleit,
CTA/secondary/danger/disabled állapotait és grafikonjelmagyarázatát.
Normál szöveg4.5:1, nagy szöveg3:1, funkcionális ikon/kontroll3:1 a cél.
Ha mérsz arányt, add meg a valódi előtér/háttér értékeket és az eredményt.
Ne állíts kontrasztPASS-t puszta szemrevételezésből; áttetszőséget kompozitálj.
Meglévő palettaID vagy a teljes színrendszer cseréje helyett célzott javítás kell.

## Fájlhatár

Módosítható a Studio projektben:
- ui/theme/Theme.kt, Type.kt, TrainPilotPalettes.kt (csak szükséges megjelenítési fix),
- ui/components/TrainPilotCommonComponents.kt,
- localization/TrainPilotStrings.kt,
- az5meglévő ui/screens/*.kt,
- szükséges, kis célzott app/src/androidTest teszt.

A DemoDataProvider, TrainPilotModels, TrainPilotViewModel és Android package/build/
manifest maradjon változatlan. Ha bizonyíthatóan kell más fájl, nevezd meg az okát,
és a legkisebb UI-módosítást készítsd el; ne építs új motort vagy állapotfolyamatot.

## Ellenőrzés és pontos átadás

1. Mind5főoldal + súlyzós/időalapú/kétoldali/GPS/összegző nézet és hosszú dialog.
2. HU/EN/DE/RO;320dp és normál telefon; fontScale1.0 és2.0.
3. Célzott Compose-próba a nagyfontos német fülfelirat tényleges
   `TextLayoutResult.hasVisualOverflow`,48dp cél, selected/disabled állapot és
   hosszú dialógus utolsó műveletének elérésére. Ne csak a szemantikafában létezést mérd.
4. Ha van valódi Android build/emulátor hozzáférésed, fordíts/tesztelj és javíts.
   Ha nincs, a kód készülhet, de pontosan `NOT_RUN` státusszal add át. A böngészős
   render, screenshot vagy Preview nem az Android instrumentáció eredménye.
5. Átadás: módosítottfájl-lista, teljes javított kis Kotlinfájlok VAGY unified patch
   (eredeti paths), új kulcsok, BEFORE/AFTER tételek és futtatott/NOT_RUN tesztek.
   Opcionálisan kis ZIP CSAK a megváltozott fájlokkal és manifesttel;
   ne csomagolj gradlecache/build/media/.aistudio/.git vagy teljes TrainPilot repót.
6. `AISTUDIO_REFINEMENT_REPORT.md`: mi készült el, hol van még valódi hiba,
   milyen eszközön/forráson mit futtattál; tényleges képek csak helyes eredetjelzéssel.

A feladat végéig haladj a saját Studio projekteden; ne állj meg javaslatoknál.
A natív appba Root illeszti a javításokat ellenőrzés után.

---

## Pontos eredeti raw források

A Studio projekt általad már ismert, ezért nem kell újra feltölteni az egész ZIP-et.
Ha referencia szükséges, ezek byteazonosan az első exportból valók:

- Theme:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/ui/theme/Theme.kt
- CommonComponents:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/ui/components/TrainPilotCommonComponents.kt
- Strings:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/localization/TrainPilotStrings.kt
- Home:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/ui/screens/HomeScreen.kt
- Training/Planning:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/ui/screens/WorkoutAndPlanningScreens.kt
- Health/Coach/Settings:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/ui/screens/HealthCoachAndSettingsScreens.kt
- Journal/Progress:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/ui/screens/LogAndProgressScreens.kt
- Active measurements:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/com/example/ui/screens/ActiveMeasurementScreens.kt
- Minden eredeti Kotlinfájl rawindexe és hashmanifestje:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/8ba6a388/docs/ai-studio/export-20261027/README.md
