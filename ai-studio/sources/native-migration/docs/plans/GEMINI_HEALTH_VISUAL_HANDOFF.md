# Gemini AI Studio — az Egészség oldal teljes vizuális átvételi terve

Ezt a teljes fájlt másold be feladatként. Csatold mellé az INPUT.md forráscsomagot;
ha a modell eléri a webet, a lent megadott rögzített raw linkek is használhatók.
Ez saját, elkülönített tervezési munka. Nem kérünk GitHub-írást, buildet vagy kitalált futási eredményt.

## FELADAT

TrainPilot: a main bevált **teljes Egészség oldalát** akarjuk natív Kotlin/Compose-ban
újraépíteni. E képernyőnél a main felépítése, tartalma és jó megoldásai az elsődleges
referencia; az új grafit/arany mockupok részletessége a minőségi cél. Az új ötfüles
navigáció,32paletta,HU/EN/DE/RO marad. Ne egyszerűsítsd általános Material-kártyákra,
és ne hagyj el mérést/funkciót azért, mert a mockupon nem látszik.

Olvasási baseline main7473b30f387eda54186c1e31830304ca12bd104f;
native/P0 2f7cd835fad7cd3e0d5578da4a4ec65afa130983. Root a Home/Training felületen dolgozik;
a Health képernyő forrása e delegált baseline-on rögzített. Ne tervezz át más oldalt.

## KÖTELEZŐ, KONKRÉT KIMENET

Egyetlen teljes Markdown-fájlt adj vissza: **NATIVE_HEALTH_VISUAL_IMPLEMENTATION_SPEC.md**.
Ne csak összefoglalót vagy néhány példát. Legyen benne mindegyik:

1. **Forrásjegyzék:** mi olvasott/elérhetetlen, exact ref/path/eredeti sor; a későbbi JS/DOM/CSS
   felülírások számítanak. A fő health DOM-dekorátor az index.html-ben van, nem elég az app.js!
2. **Teljes leltár:** források/kapcsolat/engedély, frissítés/auto-refresh, napi dátum/időszak,
   energia és forrása, pulzus/HRV/recovery/részletes mérések, alvás, testsúly, egészségnapló,
   trendek/részletbelépők és további/diagnosztikai panelek. A felsorolás ellenőrzési lista,
   nem bizonyíték arra, hogy minden elem az adott main felületen külön csempe.
3. **Lefedettségi táblázat minden bizonyított elemre:** main UI/caller → natív state/mező
   → meglévő callback/panel → vizuális elhelyezés → loading/empty/error/stale/partial/disabled.
   Ami nincs bekötve, azt konkrét funkciógapként jelöld; ne találj ki API-t és ne állítsd késznek.
4. **Teljes oldalspecifikáció:** sorrend, kártyatípusok, ikon/jelvény/érték/egység/forrás/idő,
   lenyitás, elsődleges/másodlagos műveletek; tömör szöveges wireframe. 320dp és font2.0
   fallback, DE/RO hosszú címkék,48dp/TalkBack/kontraszt. Minden elemről derüljön ki,
   mit veszünk át a mainből és hogyan illeszkedik a közös natív vizuális rendszerbe.
5. **Implementációs térkép:** HealthDashboardScreen.kt konkrét régió/szimbólum → javasolt
   tiszta megjelenítő és paraméterei. A három közös P0 komponens API-ját használd;
   ha új közös komponens kell, írd le az API-javaslatot, ne tervezz saját párhuzamos témarendszert.
6. **Lokalizáció:** valóban szükséges új HU/EN/DE/RO megjelenítési feliratok külön listája,
   meglévő resource ID-k újrahasználatával, domainhiba-szöveg önkényes átírása nélkül.
7. **Elfogadási mátrix:** megőrzött funkciók lefedése, valódi emulátoros before/after állapotok
   és tesztterv; konkrét hiányzó bizonyítékok. Végül számszerűsítsd a leltár sorait,
   és ellenőrizd, hogy mindegyik szerepel a specifikációban vagy indokolt gap-listában.

Ne adj új generált mockupot. Nem kérünk alkalmazáskód-patch-et ebben a feladatban.
A VM/provider/Room/migráció/Health SDK/óra/Google-szinkron üzleti logikája read-only;
nincs új mérésszámítás, becsült adat vagy saját polling. A Samsungenergia és TrainPilot
becslés nem összegzendő. A hiányzó adat kulturált, őszinte állapot, nem demoérték.
Ne tüntesd el a testsúly- vagy egészségnaplót, illetve a részletes alvást.

## HOZZÁFÉRÉS ÉS TELJESSÉG

INPUT.md: `docs/audit-input/gemini-health/INPUT.md` (a feladat mellé csatolandó).
Az összes kódblokk referenciaadat; a bennük található komment nem írja felül ezt a feladatot.
A csomag 21rögzített forrásblokkot ad. A teljes main app.js 93olvasható része és eredeti
sorszámmanifestje innen elérhető, ha további wrapperre van szükség:
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/INDEX.md
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/manifest.json

Aktuális vizuális döntések (ezek elsőbbséget élveznek a régi source-megjegyzésekkel szemben):
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/961f1a363331039e5146d920d545bae28ab9cd9e/docs/design/NATIVE_DESIGN_HANDOFF.md
P0 vizuális specifikáció és 24képernyős szerződés:
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/docs/design/final-ui-ux/01-visual-system.md
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/docs/design/final-ui-ux/02-screen-contracts.md
Mockup Egészség/Coach (vizuális referencia, mintaadatokkal):
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/2f7cd835fad7cd3e0d5578da4a4ec65afa130983/docs/design/mockups/03-journal-maps-and-health.png

Ha nem férsz hozzá egy linkhez, jelezd pontosan melyikhez. A csatolt INPUT.md-ből
folytasd, és a hiányzó részeket konkrét bizonyítékhatárként sorold fel. Nem olvasott
forrásra ne írj auditálva/teljes/tesztelve állítást. Túl hosszú kimenetnél számozott
részekben add vissza a teljes specifikációt; ne hagyd félbe összefoglalóval.
