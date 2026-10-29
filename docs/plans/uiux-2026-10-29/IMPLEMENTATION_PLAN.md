# TrainPilot – meglévő oldalak belső UI/UX finomítása

> **Dátum:** 2026-10-29  
> **Állapot:** felhasználó által elfogadott dizájnterv – a dokumentáció rögzítése **nem jelenti a UI-kód megvalósítását**.  
> **Repó:** `fuloplevente1998/TrainPilot`  
> **Új branch:** `feat/uiux-page-content-20261029` → `main`  
> **Ellenőrzött kiinduló main commit:** `8a36502da5832a42f1cb91708ecfed5cf4f9ed62`.

## 0. Kiemelt döntések, hatókör, kizárások

- **Kizárólag a main aktuális állapotából dolgozz!** A külön, nyitott PR #271 (`feat/webview-mockup-redesign`) **NEM** a kiindulópont; annak kísérleti és visszavont navigációs megoldásait ne merge-öld, ne cherry-pickeld, ne másold át.
- **Hét meglévő képernyőterület belső** layoutja, információhierarchiája, kis ikonja és UX-részlete változhat. **Nincs új főoldali navigáció, alsó dock vagy teljes redesign.** Az aktuális főoldali 2×4 navigáció és a Naptár/Programok/Coach közvetlen elérése marad.
- A TrainPilot meglevő arculata, komponensrendszere, világos/sötét témája, lokalizációi, betűkészlete és a felhasználó **választott témaszíne** az elsődleges. A mockupok arany/zöld/lila/türkiz kiemelései **nem kötelező színek**, csak szemantikai és vizuális inspirációk. Ikonok, grafikonok, aktív gombok, keretek, linkek a jelenlegi theme tokenekből kapják színüket; hiba/siker/figyelmeztetés eltérő, hozzáférhető szemantikus jelzést is kaphat.
- A mintaképekben szereplő edzésnevek, GPS-adatok, PR-értékek, pulzusok, időpontok, verziók, aktivitások és kapcsolódási státuszok **nem valós adatok**; ezeket soha ne hardcode-old a felületbe.
- A már meglévő navigáció, edzésindítás, napló/szerkesztés/törlés, videó megnyitás, Wear-adatátadás és vezérlés, GPS-rögzítés, pihenőidő, időzítő, sorozatmentés, automatikus kitöltés, RIR, egészség- és Drive-szinkron, fájlmentés és visszaállítás **funkcionálisan nem változhat**.
- A felhasználó jóváhagyta a lentebb megjelölt UI/UX-javaslatokat; a Health Connect/Samsung Health kapcsolatjelzés **opcionális**, csak igazolt adatokkal valósítható meg.
- **A meglévő AGENTS.md, docs/WORK_HANDOFF.md és tényleges kód/tesztek az irányadók.** Nem szabad később megváltozott `main` forrásra feltételezések nélkül ráépíteni.

## 1. Közös megjelenési és animációs szabályok

1. A `www/app.js`, `www/index.html` és célzott `www/*.css` a WebView forrás; a natív `android/app`, `android/wear`, `android/shared` csak akkor érinthető, ha a meglévő viselkedés megőrzéséhez szükséges. A jelenlegi CSS theme tokeneket kell újrahasznosítani.
2. Kártyák, chip-ek, státuszsorok, fejléc- és listatávolságok legyenek kompaktak; az érintőterület ne szűküljön használhatatlanra (célozz kb. 44–48 CSS px interaktív célméretet, illetve a platform követelményeit). Külön próba 320/360/393 px-en és nagy betűmérettel.
3. **Fade oldalváltáskor:** 150–200 ms körüli, visszafogott `opacity` áttűnés **csak a váltott belső tartalomra**, úgy, hogy a fő nav kijelölése és a megjelenő oldal együtt reagáljon. Ne legyen animációs késleltetés, üres képernyő, villogás, timer/GPS újraindítás, teljes WebView újratöltés vagy új globális `render/go` wrapper.
4. Gyors dupla érintés, visszaút, háttérbe küldés és félbeszakadt átmenet ne hagyjon eltűnt tartalmat vagy blokkolt pointer-eventet. `prefers-reduced-motion: reduce` esetén animáció nélkül működjön.
5. A Napló / Fejlődés / Statisztikák meglévő sticky belső fülsora, a korábban elfogadott piros X és a videóikon minden érintett helyen maradjon. Az animáció/kompakt mód ne törje meg a scroll-pozíciót vagy a képernyőolvasós használatot.
6. Ne készíts külön alkalmazás-szintű színsémát, párhuzamos route-rendszert, új alkalmazás shellt vagy felesleges másolatoldalakat.

## 2. Oldalankénti specifikáció

### A. Beállítások

**Kép:** `references/01-settings.png` – háromrészű Beállítások / Óra és szinkron / Mentés és visszaállítás prototípus.

- A meglevő tartalom átrendezése jól tagolt, tömör csoportokba: profil, téma/nyelv, edzésidőzítő, hangjelzés, eszközkapcsolat, mentés-visszaállítás, adatvédelem, névjegy/verzió – ami valóban létezik, marad.
- A Wear OS óra és szinkron beállításai innen egyértelműen elérhetők: párosítás/csatlakozási állapot, valóban ismert akku, edzésátadás, legutolsó igazolt szinkron, offline helyzet, jogosultságok. **Elsősorban meglévő kapcsolódási/átadási felület megnyitása vagy funkcionálisan biztonságos átvezetése**, ne két külön státuszlogika.
- Biztonsági mentés: a meglévő JSON/ZIP/Drive lehetőségek és visszaállítási folyamat rendezett UI-ban. Destruktív visszaállításnál a már működő megerősítések/hibakezelés maradjanak.
- Nincs hamis 'Csatlakoztatva', akkuszint, szinkronidő vagy 'Sikeres mentés' üzenet.

**Elfogadás:** minden eredeti beállítás megtalálható, a témák/nyelvek váltáskor valós működés, Wear és backup útvonalak ugyanarra a forrásra futnak.

### B. Edzés indítása / Órára küldés

**Kép:** `references/02-workout-start.png`.

- **Minimális változtatás.** Kompakt indítókártya: programnap/ciklus, edzésnév, becsült idő, gyakorlat- és munkaszettadatok, néhány előnézeti gyakorlat.
- A már működő **Edzés indítása** és **Órára / Wear OS** műveletek legyenek felismerhetők, eltérő rendeltetéssel.
- A már meglévő **videóikon, videómegnyitás és hivatkozások maradjanak** ugyanott, ahol jelenleg működnek. A videókhoz ne adj kitalált URL-t vagy új lejátszási mechanikát.
- A választott témaszín váltsa az akcentust; külön sárga fix gomb nem kell.

**Elfogadás:** telefonos és órás indítás ugyanúgy működik, a meglévő videógombokat nem takarja/veszíti el a kompakt layout.

### C. Egészség / kapcsolatállapot (opcionális)

**Kép:** `references/03-health-sources.png`.

- Opcionális, egyszerű összefoglaló kártya: ténylegesen kapcsolható **Samsung Health / Health Connect** adatforrás, engedély állapota, utolsó igazolt frissítés, frissítés vagy beállítás megnyitása.
- **Előbb auditáld a `main` valódi Health implementációját.** A kapcsolat-, engedély- és adattovábbítási állapot nem ugyanaz; ne mossuk össze.
- Ha a szükséges API/állapot nem áll rendelkezésre, inkább **ne jelenjen meg az új kártya**, vagy csak a tényleges beállítási átjáró; ne legyen hamis 'kapcsolódva', nem létező frissítési gomb vagy értelmetlen dekoráció.
- Nem feladat új Health integráció, engedélykérés vagy adatforrás-egyesítési logika.

**Elfogadás:** csak valós státusz; kapcsolat nélkül is érthető; funkcióhiánynál opcionális elem elhagyható.

### D. Aktív edzés / indított workout

**Kép:** `references/04-active-workout.png`.

- **Elsődleges UX-prioritás:** kevesebb függőleges görgetés a tényleges aktív sessionben.
- Keskeny összegzősáv: edzésnév, eltelt idő, tényleges pulzus (ha ismert), meglévő befejezés.
- Kicsi, de jól kezelhető pihenőblokk: hátralévő idő, +/- lépések, kihagyás, vizuális progress; **ugyanaz a meglévő pihenőidőzítő**, nem új timer-owner.
- Tömörebb sorozattábla: munkasor/bemelegítés, előző érték, súly, ismétlés, RIR, kész jel. **Szerkeszthető mezők és készrejelölés továbbra is biztonságosan megnyomhatók**, mobil billentyűzettel és nagyobb fonttal.
- Aktuális gyakorlat és következő gyakorlat egyszerű, kisméretű előnézete. Meglévő automatikus előtöltés, draft, Undo, RIR, videó, órás tükör-/tulajdonos-állapot változatlan.
- Ne zsúfold az egész folyamatot egy nem görgethető panelbe: inkább **kevesebb szükségtelen görgetés**, szükség esetén továbbra is lehet scroll.

**Elfogadás:** edzésindítás → sorozatok, súly/ismétlés, pihenő, Undo, mentés/beállítás, háttér-visszatérés, órás szinkron nincs regresszió; legalább 320 px-en használható.

### E. Kültéri mérés + minitérkép + teljes képernyős térkép

**Képek:** `references/05-outdoor-measurement.png` és `references/06-fullscreen-map.png`.

- **Kompakt mérés:** tevékenység, GPS-útvonal választás/valódi GPS állapot, pontszám és minőség ha mérhető, távolság/idő/tempó/pulzus (ha van), rövid minitérkép.
- **Térkép teljes képernyőn:** útvonal valós pontokból, helyzet ha elérhető, visszaút az aktív mérés megszakítása nélkül, kompakt lebegő statisztika és csak támogatott térképréteg-vezérlők.
- **TILOS** nézetváltáskor leállítani/újraindítani a GPS-t, útvonalat vagy edzésidőzítőt; a térkép komponens lehet újrarenderelt, a mögöttes rögzítés/azonosítók nem.
- Magán GPS-koordináták kezelése változatlan, nincs felhőbe kerülő koordináta vagy új telemetria.
- Megmarad az első GPS-engedélyezés kifejezett döntése, a pontos helyengedély/Location rendszerfeltétel külön kezelése, valamint az explicit `Mérés befejezése` és a meglévő sorozat-készrejelölési szabály. Térkép bezárása **nem** fejezi be a mérést.
- Opcionális offline térképjelzés csak ha valóban létezik és ellenőrizhető.

**Elfogadás:** mozgó GPS útvonal / megszakadt GPS / helyengedély visszavonás / map oda-vissza / sikeres és hibás STOP esetén nincs hamis útvonal vagy edzésbefejezés.

### F. Edzésnapló és PR-archívum

**Kép:** `references/07-journal-pr.png`.

- A napló legyen vizuálisan tagoltabb, de nem harsány: kis **PR-jelvények**, témaszínes kiemelések, visszafogott státuszcímkék, következetes gyakorlat/volumen/RIR/HR összefoglaló.
- Kártyák alaphelyzetben kompaktak, kattintásra/megnyitásra meglévő részletekkel bővülnek; valódi értékek a naplóadatból/rekordlogikából.
- Összes / PR-edzések / GPS-kardió szűrés, **ha illeszthető a meglévő naplóarchitektúrához**, a jelenlegi szűrés/keresés és szerkesztés/törlés megőrzésével.
- Kültéri GPS edzések külön könnyen felismerhető összefoglalókkal, nem kitalált pont/útvonal/pulzus adatokkal.
- Napló / Fejlődés / Statisztikák meglévő **sticky** fülkezelése változatlanul működjön hosszú listával is.
- PR-adatok számítása és mentése nem része ennek a feladatnak; a vizuális jelzés csak meglévő rekordokból jön.

**Elfogadás:** 20+ naplóbejegyzés, részletnyitás, PR és GPS szűrés, szerkesztés/törlés, sticky tabs, scroll 320–393 px-en helyes.

### G. Programok

**Kép:** `references/08-programs.png`.

- Programlista kompaktabb, több program látszódjon. Csak az aktuális elemeket mutassuk; az adatmodell és kiválasztás változatlan.
- **Programtípusonként felismerhető, egységes méretű kis ikon**: erő, izomépítés, funkcionális edzés, állóképesség stb. Ezek témaszín-alapú / diszkrét semleges színek, nem kötelező sokszínű paletta. Ismeretlen típust értelmes általános ikon jelöljön.
- Aktív program finom témaszínű kiemeléssel és szöveges `Aktív` állapottal. Saját / Közösségi fülek és meglévő programlétrehozás / szerkesztés / választás marad.
- Kisebb hézagok; kisebb tipográfia csak addig, amíg a magyar és többi fordítás olvasható, nem csonkol lényeges adatot.

**Elfogadás:** sok programnál a jelenlegihez képest több kártya látható, programváltás/létrehozás/szerkesztés tovább működik; az ikonok konzisztensen ugyanazt jelentik.

## 3. Ajánlott implementációs sorrend

1. **Közös alap:** audit, theme tokenek, kompakt kártya/ikon minták, fade a jelenlegi navigációt megtartva; regressziós tesztek.
2. **Alacsonyabb kockázat:** Programok → Edzés indítása → Beállítások.
3. **Közepes kockázat:** Napló/PR-archívum → opcionális Egészség státusz.
4. **Magasabb kockázat, külön tesztek:** Aktív edzés → Kültéri mérés + mini/teljes térkép.

Ezt a sorrendet a tényleges kód felmérése alapján finomíthatod, de a megvalósítást **kis, ellenőrizhető commitokra** bontsd. Ne keverd meg a natív Wear/GPS állapotgépet puszta vizuális módosítás miatt.

## 4. Munkamenet az agentnek

1. Nyisd meg `AGENTS.md`, `docs/WORK_HANDOFF.md`, majd ezt a specifikációt és a kapcsolódó referenciaképeket; ellenőrizd az **aktuális branch/HEAD és a main változásait**. Csak az itt kért képernyők **belső** UI-ját módosítsd.
2. Először auditáld a meglévő útvonalakat, CSS theme-értékeket, eventkezelőket, perzisztencia/szinkron szerződéseket és teszteket. A képernyőneveket/DOM-azonosítókat forrásból állapítsd meg, ne a mockup alapján találd ki.
3. Első lépésként javíts egy-egy oldalt kicsi diff-fel; közös design elemeket a meglévő CSS-ben implementálj, de ne írd át feleslegesen a teljes `styles.css`-t vagy a komplett `www/app.js`-t.
4. Tartsd meg a meglévő videóikonokat, a 2×4 navigációt, a sticky Napló-füleket, a témaállapotot és minden edzés/GPS/Wear működést. Teljesítményromlást és felesleges újrarenderelést kerülj.
5. Célzott JS-szintaxis/diff és UI-regressziót minden rész után. Legalább 320/360/393 px, világos/sötét és két eltérő témaszín, HU/EN/DE/RO hosszabb feliratok; nagy betűméret, reduced motion, dupla-tap, gyors oda-vissza. Aktív edzés, GPS, offline helyzet és Wear tükör külön tesztek.
6. Teljes Quick/Performance/Chromium/natív phone+Wear/UI kapuk és szükség esetén aláírt APK-k a **meglévő GitHub Actions** munkafolyamaton keresztül; ne hozz létre második release/build workflow-t. A dokumentációs PR önmagában **nem kész APK és nem tesztelt UI**.
7. Nincs verzióemelés, `main` merge vagy release külön készülékes elfogadás nélkül. PR marad draft, amíg a megvalósítás és elfogadás nincs igazolva.

## 5. Képmappa

A képek az oldal-specifikáció **vizuális referenciái**, nem pontos pixel-/színmásolási kötelezettségek. A forrás chatben **nyolc eredeti kép** érkezett:
- `references/01-settings.png` – Beállítások (3 panel)
- `references/02-workout-start.png` – Edzés indítása / Wear OS
- `references/03-health-sources.png` – Egészség / kapcsolat (opcionális)
- `references/04-active-workout.png` – Aktív edzés
- `references/05-outdoor-measurement.png` – Kültéri mérés / minitérkép
- `references/06-fullscreen-map.png` – Teljes képernyős térkép
- `references/07-journal-pr.png` – Edzésnapló / PR
- `references/08-programs.png` – Programok

> **Fontos:** ha valamely kép még nincs ténylegesen a GitHub mappába feltöltve, a hivatkozását **nem szabad teljesült képfeltöltésnek tekinteni**. Az agent a képek tényleges létezését ellenőrizze; a specifikáció képektől függetlenül is irányadó.

## 6. Definition of Done (minden oldal)

- [ ] Választott témaszín és világos/sötét működik, új fix színpaletta nélkül.
- [ ] Nincs funkcióvesztés, hiányzó videógomb, elrejtett fő navigáció vagy nem kattintható kártya.
- [ ] Kevesebb fölösleges whitespace, ugyanakkor olvasható, akadálymentes és nagy betűnél is kezelhető.
- [ ] Fade finom és megszakítható, háttér/timer/Wear/GPS állapotot nem nullázza.
- [ ] Célzott regressziós teszt(ek) az oldalt érintő működésre, forráshoz kötött eredménnyel.
- [ ] PR-diff csak megállapodott belső UI/UX területet érint; main/release nem változik jóváhagyás nélkül.
