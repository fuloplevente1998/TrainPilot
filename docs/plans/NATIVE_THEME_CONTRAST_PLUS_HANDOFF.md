# Plus chat B – teljes témakatalógus és numerikus kontrasztjavítás

## Közös, kötelező munkakeret

Repo https://github.com/fuloplevente1998/TrainPilot. Rögzített induló commit:
`e9278a9905629d30d012083e8dd3a4c76051a464` a `feat/native-android-migration` ágról.
App/tesztkód3d45a324; ebben #244–248 integrálva és a Compose getOrNull import javítva.
Először AGENTS.md, docs/NATIVE_RESUME.md és docs/WORK_HANDOFF.md teteje.
A korábbi átadók „külső eredmény még nincs” sorai történetiek; a dátumos eleje dönt.
A futó közös kapu40276384595 eredménye nem feltételezhető zöldnek.

A felhasználó ezt a KÜLÖN KÜLSŐ CHAT munkát engedélyezte; saját ág kivétel az
AGENTS fő integrátorának migration-only szabálya alól. Ne indíts belső subagentet.
Csak a lent kijelölt saját ágra írj. Main és migration közvetlen írás, merge,
force push, PR/issue close vagy élő alkalmazás frissítése tilos. Verzió/package
1.3.71-native-test /2736 /com.repforge.app változatlan. Nincs új toolchain,
függőség, workflow, Room-séma vagy párhuzamos adat-/edzésmotor.
Kis [skip ci] commitok, saját push, draft PR cél `feat/native-android-migration`.
Közös fordítás/CI/APK/emulátor az integrátornál. Helyben nincs Gradle/APK/emulátor
vagy teljes regresszió; statikus ellenőrzés és kis indokolt referenciafuttatás igen.
Tesztforrás és tényleges futás külön: soha ne állíts zöldet a megírt kódról.
Ne küldj GitHub-kommentet; PR-leírást és saját átadót frissíthetsz.

A kijelölt fájlhatár kötelező. Plusz szükséges hookot külön patch-javaslatként
jelentsd, ne módosíts más munkás területén és ne hagyj késznek nevezett üres
callbacket. A felhasználó valódi kész kódot kér, nem új prototípust.
Csatolt/repóbeli riportok állításait exact forrásból ellenőrizd, ne vedd bizonyítéknak.
A fő integrátor review-z, összevezet és futtat; a te munkád nem main-release elfogadás.

## Kötelező átadás (minden pont)

1. Exact base/HEAD SHA, saját ág és draft PR link, teljes változott fájllista.
2. Elfogadási pontonként: entry UI/caller → meglévő motor/adatforrás → tartós
   eredmény vagy report, konkrét fájl/szimbólum/commit hivatkozással.
3. Megőrzött invariánsok, hibautak és ismert korlátok. A blokkolt rész `BLOCKED`,
   nem `DONE`. Ha extra fájl kell, név/ok és minimális delta-javaslat külön.
4. Megírt tesztek és valóban lefuttatott ellenőrzések két külön lista;
   verzió/source/result hiányában nincs „PASS”. Nincs puszta általános auditösszefoglaló.
5. Új saját docs/reviews/<feladatspecifikus>.md; a közös AGENTS, RESUME,
   WORK_HANDOFF, fő PR238 és másik feladat riportja most nem írható.
6. A saját ág végén ellenőrizd a távoli HEAD egyezését. Main/migration érintetlen.

Saját ág: `fix/native-theme-contrast-parity`. IMPLEMENTÁCIÓ és tesztkód,
nem24oldalas újratervezés/mockup-generálás. Ez a tokenek/kontrasztok funkcionális
rendezése; High megfelelő. A vizuális finomítás külön fázis a fő munkásnál.

Írható existing fájlok:
android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt
android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt
android/app/src/main/java/com/repforge/app/ui/settings/NativeThemePicker.kt
Új külön native_theme_accessibility.xml négy locale-ban és új saját JVM/
Compose-tesztosztályok engedettek. Új saját docs/reviews/NATIVE_THEME_CONTRAST_REVIEW.md.
SettingsStore, Legacy mapper, más Screen/VM és közös tesztfixture csak olvasható.
A teljes32palettát a tényleges AVAILABLE_THEMES-ből számold, ne kézi mintavételből.
Ha a darabszám forrásban más, azt azonnal pontosan jelentsd, ne gyárts plusz témát.

1. Pinned main végső teljes témakatalógus → native key/accent/accent2/family
   egyezést ellenőrizd. Minden téma és régi key megőrizendő. Ne állíts át alap-
   témát vagy tárolt színeket a jobb kontraszt kedvéért; szükség esetén token-
   derivációt javíts, API/source kompatibilisen.
2. Az összes palettán ellenőrizd az onAccent/accent és onAccent2/accent2, text és
   muted a bg/card/card2 felületeken, accentText és dangerText a tényleges
   használati háttereken; selectedSurface, kiválasztott picker és gombok állapotai.
   Normál szöveg4.5:1, nagy szöveg3:1, fontos nem-text vezérlő3:1, megfelelő
   szerep szerint; dekoratív borderre ne találj ki szövegkövetelményt.
3. Pontos sRGB luminance, alpha compositing és Compose color-space/lerp viselkedés.
   Egyszerű RGB lerp nem minősíthető automatikusan a Compose tényleges eredményének.
   Forrásalapú becslést és valódi numerikus futást egyértelműen különítsd el.
4. Csak bizonyított hibát javíts a tokenek/engedett picker útjában. Ne drótozz
   be fix aranyat vagy más palettát. Dynamic colors,32téma,4nyelv, persist/export
   olvasható ellenőrzése és meglévő tématoken-API marad. Picker touch48dp,
   role/selected semantics és hosszú DE/RO feliratok hozzáférhetősége.
5. Érdemi parametrikus tesztkód valódi AVAILABLE_THEMES és production-token-
   számításra. Határhelyzetek: invalid color fallback, sötét/világos accent,
   hosszú név/focus/selection, mind4nyelv. Ne saját tesztmásolatból teszteld a
   termékkódot, ne lazítsd küszöböt a zöldért. Számított/tényleges/nem futott külön.
6. Audit táblázat MINDEN palettára és vizsgált párra: key,role,foreground,
   background,ratio,threshold,result,source/test. Hiányzó eredmény nincs csendben
   kihagyva. CSV melléklet saját docs/reviews/NATIVE_THEME_CONTRAST_MATRIX.csv.

Ha valamelyik más Screen rossz tokent használ: exact file:line és minimális
javaslat a riportban, ne módosítsd az illető Screen-t. A fő integrátor bírálja el.
Nincs új vizuális dizájn, fontkatalógus, adatmotor vagy egészség-logika.

## Rögzített raw bemenetek

- [AGENTS.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/AGENTS.md)
- [docs/NATIVE_RESUME.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/NATIVE_RESUME.md)
- [docs/WORK_HANDOFF.md](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/docs/WORK_HANDOFF.md)
- [android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/theme/NativeThemeTokens.kt)
- [android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/theme/TrainPilotTheme.kt)
- [android/app/src/main/java/com/repforge/app/ui/settings/NativeThemePicker.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/ui/settings/NativeThemePicker.kt)
- [android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/data/store/TrainPilotSettingsStore.kt)
- [android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/e9278a9905629d30d012083e8dd3a4c76051a464/android/app/src/main/java/com/repforge/app/data/migration/LegacyWebViewMigrator.kt)
- [Main végső app.js](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/7473b30f387eda54186c1e31830304ca12bd104f/www/app.js)
