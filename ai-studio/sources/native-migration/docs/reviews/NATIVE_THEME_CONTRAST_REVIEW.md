# Native theme contrast — Plus B átadás (2026-10-26)

## Állapot, forrás, fájlhatár

- **Pinned BASE:** `e9278a9905629d30d012083e8dd3a4c76051a464` (`feat/native-android-migration`). A dokumentumot átadó külön SHA (`5224ada0`) **nem** a kódbázis induló commitja.
- **Saját ág:** `fix/native-theme-contrast-parity` — **Draft PR [#249](https://github.com/fuloplevente1998/TrainPilot/pull/249)** → `feat/native-android-migration`.
- A report megírását megelőző kód+CSV HEAD: `a67a51077e01e0b38d8cab3de01eed1e465a5085`. **Végső remote HEAD:** a PR naprakész leírásában és távoli ref ellenőrzésében; a riport saját commitja megváltoztatja a HEAD-et.
- **Módosított vagy új fájlok (csak engedélyezett):**
  1. `android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt` — alpha-composite WCAG-kontraszt, kijelölt szöveg és fókuszkeret származtatása.
  2. `android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt` — a közvetlenül konstruált `TrainPilotExtendedColors` alapértelmezett kiválasztási kerete is olvasható.
  3. `android/app/src/main/java/com/repforge/app/ui/settings/NativeThemePicker.kt` — látható billentyűzetfókusz, törésre képes hosszú felirat, változatlan radio/selected szemantika.
  4. `android/app/src/test/java/com/repforge/app/ui/theme/NativeThemeContrastParityTest.kt` — új izolált JVM-tesztforrás.
  5. `docs/reviews/NATIVE_THEME_CONTRAST_MATRIX.csv` — 800 soros teljes numerikus mátrix.
  6. `docs/reviews/NATIVE_THEME_CONTRAST_REVIEW.md` — ez az átadó.
- **Nem módosult:** a Store/legacy mapper, más Screen/VM, közös fixture, meglévő tesztek, AGENTS/RESUME/WORK_HANDOFF, CI/workflow, Gradle/dependency, Room, verzió és main/migration ág. Nincs merge/CI/APK/készülékfrissítés. Az alkalmazás szerződéses verziója `1.3.71-native-test` / `2736` / `com.repforge.app` változatlan.

## 1. Teljes katalógus, főági forrásparitás — DONE (source)

**Végső main**: `7473b30f387eda54186c1e31830304ca12bd104f:www/app.js`, blob `b5567576bed9a8f93608a6bf1ed8db1dee4723f4`.
- Vivid alapszín-készlet `RF200_THEMES` `app.js:2107+`, illetve `TP1511_VIVID` `app.js:10720`.
- Végleges Basic felülírások az **utolsó hatályos** `Object.assign(RF200_THEMES,...)`-ben: `app.js:11718–11730`, 10 Basic; `TP1511_BASIC` `11731–11732`.
- Native `TrainPilotSettingsStore.AVAILABLE_THEMES`: `android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt:112–145`.
- Forráson végrehajtott valódi beolvasás/összevetés: **26/26 main key + accent + accent2 egyezik**; family a main Basic/Vivid listából egyezik. Plusz **6** korábban menthető natív key változatlan (`amberGold, emerald, slateCoral, vividCyan, vividGold, vividEmerald`): **32** összes, nincs új téma.
- Korábbi tesztbizonyíték **tesztforrás, nem mostani futás**: `NativeThemeParityTest.allMainThemesKeepTheirKeysColorsAndFamilies` a `main-7473b30-themes.json` fixture-rel; `previouslySavedNativeKeysAreStillSelectable`. A Store importja `importedTheme` `149–152`, default `classicBlue` `47–50`, adatmentése `setTheme` `228–235`. Nem módosult.

## 2. Konkrét UI → adatforrás → tartós eredmény, javítások

| Acceptance | Bejárható caller → termékmotor → eredmény / bizonyíték | Állapot |
|---|---|---|
| Katalógus, perzisztencia | `SettingsScreen.kt:134` → `NativeThemePicker` (`onSelect(theme)`, `Role.RadioButton`, `selected`, `theme_<key>` tag) → `SettingsViewModel.setTheme:152` → `TrainPilotSettingsStore.setTheme:228–235` → 4 DataStore kulcs → `settingsFlow:161–164` → `TrainPilotTheme` `NativeThemeTokens.resolve`. Restore a `importedTheme:149–152` útban; `themeJson:76–81` az exportálható adat. | **DONE source; runtime UNVERIFIED** |
| Szöveg/fill | `NativeThemeTokens.resolve` → változatlan `accent,accent2`, változatlan `onFill` fekete/fehér választás; `readableAccent` a card+card2+**selectedSurface** kontrasztját méri, 4.5:1. `TrainPilotTheme` Material3 `onPrimary/onSecondary`. | **DONE source; runtime UNVERIFIED** |
| Selected/focus | `selectedSurface` ugyanaz a Basic matte / Vivid Compose-Oklab tint, mint korábban. A `readableControlAccent` 3:1-et keres card/card2/selectedSurface ellen. `NativeThemePicker` a 2dp színnel kijelölést **és** fókuszt jelez, radio/selected semantics marad. | **DONE source; UI runtime BLOCKED (Root)** |
| Más szövegek | `TrainPilotTheme.kt` `BgColor,CardColor,Card2Color,TextColor,MutedColor,DangerTextColor` változatlan; háttérenként mérve. `dangerText` / `danger` páros is mérve. | **DONE source; runtime UNVERIFIED** |
| HU/EN/DE/RO | A `ThemePaletteOption.displayName` valódi négy locale-ját hívja a picker; explicit `softWrap=true`, `fillMaxWidth`, `widthIn(112..200.dp)`, `heightIn(min=64.dp)` biztosítja a felirat törését és 48dp-nél nagyobb célpontot. Szemantika: `selectableGroup` + `selectable(role=RadioButton,selected=...)`; fókusz `onFocusChanged`. | **DONE source; TalkBack/DE/RO UI próba BLOCKED (Root)** |

### Bizonyított hiba → minimális delta

- **Bázis**: Basic selected keret `LineColor` / `Card2Color` csak ~**1.164:1**; a Basic mind a 13 kulcsára 3:1 alatti. Ugyanez kártyához ~1.331:1. 13×2=26 hibás indikátorpár.
- **Bázis**: `readableAccent` korábban csak `CardColor` és `Card2Color` ellen olvasott. A tényleges Vivid `selectedSurface` hátteren az öt `purple`, `indigo`, `red`, `magenta`, `lavender` kiválasztott felirata a referenciafuttatásban **4.5:1 alatt** maradt (~3.992–4.325). Ez 5 hibás szövegpár.
- **Javítás**: csak *származtatott* `accentText` és `selectedBorder`, illetve picker focus-megjelenítés módosult; a tárolt hex, family, key, default, Base/Vivid csoportosítás nem. `onFill` szabálya nem lazult. Nincs fix arany szín.

## 3. Numerikus audit: teljes CSV

**[NATIVE_THEME_CONTRAST_MATRIX.csv](NATIVE_THEME_CONTRAST_MATRIX.csv)**: valódi `AVAILABLE_THEMES`-ből kiolvasott 32 paletta × 25 szereppár = **800 adatsor**; **768 küszöbös** és **32 INFO_ONLY** (a dekoratív `selectedSurface` önálló, keret nélküli összehasonlítása). Oszlopok: `key,role,foreground_srgb,background_srgb,ratio,threshold,result,source_test`. Minden kulcshoz `onAccent/accent`, `onAccent2/accent2`, `text/muted/accentText/dangerText` három alapfelületen, `accentText/selectedSurface`, `dangerText/danger`, `selectedBorder` három felületen, picker selected/unselected/focus és primary/secondary button szöveg tartozik.

**Ténylegesen végrehajtott, de független JavaScript sRGB+Oklab referencia:** 768/768 `REFERENCE_PASS`, nulla `REFERENCE_FAIL`; a legkisebb számított 4.5:1 szerep **4.5106:1**, 3:1 szerep **3.1831:1**. Ez **nem Compose/Android-teszt PASS**; a CSV `PRODUCTION_TOKEN_VIA_JS_OKLAB_REFERENCE` jelölést használ. A sRGB luminanciához IEC/WCAG transfer függvény és 0.2126/0.7152/0.0722 súlyok, Oklab átmenethez független lebegőpontos referencia transzformáció készült. A production `androidx.compose.ui.graphics.lerp` maga Oklab térben interpolál és `Color.luminance` sRGB EOTF-et alkalmaz; **nem** tévesztettük össze kódolt RGB-channel lerppel. A kiválasztott termékjelölt tesztelése production Compose kontraszttal fut majd a JVM-tesztben. `NativeThemeTokens.contrast` most `compositeOver`-rel előbb az alpha-t is figyelembe veszi; az alpha regresszió tesztforrása megvan, **nem futott**. Oklab Float/kerekítés, kijelző és tényleges pixel-kompozit eltérhet: a referencia nem pixel-screenshot.

A fényességi/luminancia/fókusz küszöböt szándékosan **nem** csökkentettük; a 3:1 a funkcionális körvonal, a 4.5:1 a normál szöveg követelménye. Nagy szöveg esetén 3:1 lenne megengedett, de konzervatívan 4.5:1-gyel auditáltuk. Pusztán dekoratív keretre/swatchra nem állítottunk külön 4.5:1 szövegküszöböt.

## 4. Megírt tesztek (NEM azonos a futással)

Új `android/app/src/test/java/com/repforge/app/ui/theme/NativeThemeContrastParityTest.kt`:
1. `everyStoredPaletteKeepsItsPaintAndAllTextAndSelectionThresholds`: **termék** `AVAILABLE_THEMES` → **termék** `NativeThemeTokens.resolve`; mind 32× több role/threshold, veszteség nélküli accent ellenőrzés; nem duplikált tesztpaletta.
2. `invalidAndLegacyColorFormatsCannotReplaceSavedAccentsSilently`: hibás HEX, dark/light/boundary onFill.
3. `translucentPaintIsCompositedBeforeContrastRatherThanTreatedAsOpaque`: félig átlátszó sRGB kompozíció.
4. `allFourLocalesKeepRealThemeNamesIncludingLongGermanAndRomanianNames`: mind32×4 valódi `displayName`, hosszú DE/RO.
Előző, nem módosított tesztek: `NativeThemeParityTest` és `NativeThemeTokensTest`; ezek lefutása jelen átadásban nem igazolt. **Compose UI/TalkBack fókusz- és felirattörési instrumentation teszt most nincs lefuttatva; a teljes UI gate Root feladata.** A valós futás eredménye csak forrás-SHA/jobreport mellett fogadható el.

## 5. Ténylegesen végrehajtott ellenőrzések vs. nyitott

| Ellenőrzés | Forrás / futtatás | Eredmény |
|---|---|---|
| Main katalogus-extract | GitHub pinned main blob `b5567576...`, final `RF200_THEMES` és natív Store pinned `e9278a...` beolvasás + JS key/hex összevetés | **26/26 matched**, +6 őrzött natív |
| Bázis kontraszthibák számszerűsítése | Pinned Kotlin token logika → JS referencia sRGB+Oklab program tényleges futtatása | **31** küszöb alatti pár (26 border +5 selected text) |
| Javított teljes CSV | Pinned production katalógus és az engedélyezett módosított token-számítás szerinti JS referencia | **800 sor, 768 reference pass, 32 info, 0 reference fail** |
| Kotlin JVM/JUnit, Compose UI, Gradle/Android compile, R8, APK, screenshot, fizikai telefon/Wear | **NEM FUTOTT / NOT RUN**; közös CI és integráció kizárólag Root | **BLOCKED – közös gate** |
| Dinamikus/locale/settings/backup runtime körjárat | Csak kód útvonal és tesztforrás ellenőrzött, nem történt telepítés | **BLOCKED – készülékes** |

## 6. Invariánsok, hibautak, BLOCKED és javasolt integrátori lépések

**Invariánsok:** megőrzött 32 key/hex/family; default `classicBlue`; DataStore key/érték séma; backup/restore/legacy key mapping; HU/EN/DE/RO szövegforrás; Material3 darkColorScheme szerepkiosztás; nincs dinamikus témaforrás csere; nincs új dependency, Gradle, workflow, GPS/Health/edzésmotor-kód, verzió, main/migration write.

**Hibautak:** hibás hex a régi fallback (nem okoz összeomlást); black/white fill a kontraszt nagyobbik értéke; alpha compositing a megjelenő háttéren; ha a keresés 100 lépésben nem talál értéket, fehér szöveg/körvonal az opaque sötét felületeken. A sötét felületek fixek, a palette input csak sRGB `#RRGGBB`, így a fallback nem néma felülírt tárolt szín.

**BLOCKED**:
- Natív **unit/Compose test tényleges lefutása**, TalkBack/key focus/DE+RO legszűkebb kijelző és fontscale UI, eszközszintű 48dp-touch vizsgálat: Root közös CI/emulátor/fizikai próba. Nem jelölhető késznek a JVM-teszt megírásától.
- **Más screen, nem írható terület:** `android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardScreen.kt:700` `ProviderChoiceChip` `.height(42.dp)`; ez a 48dp minimum alatti vezérlő a picker követelményein kívül. **Minimális külön patch-javaslat:** `.heightIn(min = 48.dp)` és képernyős klikkelhetőségi vizsgálat; **nem módosítottuk**. A chipek `onAccent` használata az `accent` kitöltésen helyes (`HealthDashboardScreen.kt:700–711`); a `ProgressScreen.kt:193–203` selected chip ugyanezt a korrekt párt használja.
- Ha a közös gate más Screen felületen tényleges mismatch-et talál, arra külön exact file:line, token és minimális patch szükséges. Most nincs bizonyított szélesebb hibafeltárás vagy runtime PASS.

**Átadás Rootnak**: review-zza a `#249` kizárólag hat fájlját, futtassa a Compose/Android/JVM céltesztet és a közös CI-t az *integrált SHA-n*; csak a kapu bizonyítékával és valódi pixel/TalkBack ellenőrzéssel döntsön integrációról. Egyik jelenlegi `REFERENCE_PASS` sem állít release-ready/minden kész állapotot.
