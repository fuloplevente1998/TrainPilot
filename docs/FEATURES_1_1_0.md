# TrainPilot 1.1.0 / 2698 — célok, újratervezés és intervallumok

Az [#103](https://github.com/fuloplevente1998/TrainPilot/issues/103) változásai a legutóbbi stabil mainre (`81032d9b3ec697680e7fdb1210e89f94e584b299`, 1.0.9 / 2697) épülnek. A funkciók az 1.2.0 /2703 elfogadott kiadásban kerülnek mainre.

## Használat

- **Célok:** Az 1.2.0-ban: Kezdőlap → Célok beállítása. A korábbi Mai állapot / Beállítások hivatkozásokat a kompakt gomb váltotta fel. Heti edzésdarabszám és havi kardiókilométer állítható; a 0 kikapcsolja az adott célt. A fejlődés a célpanelen sávokkal, a kezdőlapon rövid számmal jelenik meg. A hét hétfőn kezdődik, a hónap helyi idő szerint vált. Mentett, teljesített sorozatot tartalmazó edzések számítanak; a kilométer a naplózott sorozatok távolsága, külön Health-import hozzáadása nélkül.
- **Újratervezés:** Naptár → Tervezési beállítások → Újratervezés. Válassz programot és új kezdőnapot, ellenőrizd a régi → új időpontokat, majd frissítsd a tervet. A kihagyott és következő tervezett alkalmak mozognak, sorrendjük/azonosítójuk megmarad. A teljesített, naplózott, törölt és megkezdett alkalmak védettek; foglalt napra nem kerül új edzés. Az időközben megváltozott terv új előnézetet kér.
- **Intervallumok:** idő+távolság típusú gyakorlatnál Beltéri időmérés → Intervallumok. Munka, pihenő, körök, hang és rezgés állítható. Az utolsó kör után nincs extra pihenő. A meglévő Indítás/Szünet/Folytatás/Nullázás vezérlők működnek; az elkészült intervallum nem pipálja ki automatikusan a sorozatot.
- **Coach:** a „Mi alapján?” részen látható az alvás, saját HRV-átlag, legutóbbi terhelés és fájdalom tényleges pontszámhatása, valamint az adatok dátuma/hiánya. A meglévő képlet változatlan: 70 kiindulópont, 25–100 korlát, friss jel nélkül nincs pontszám.
- **Felületi visszajelzés:** az üres napló, szűrt napló, keresés és kardióállapot következő lépést kínál. A sikeres mentés nem szakítja meg a munkát felugró ablakkal; a sikertelen mentés jelzett hiba marad. Nagy rendszerbetűméretnél a Home/Egészség/Naptár függőlegesen görgethető.

## Időmérés és adatok

A sorozat idejébe a munka és az intervallumpihenő is beleszámít, a kézzel indított szünet nem. GPS és helyhozzáférés nélkül használható; futó GPS-mérés közben intervallum nem indítható. A hang- és rezgésjelzéshez megnyitott app szükséges. Háttérből vagy újraindításból visszatérve az eltelt idő visszaáll, és a tervezett végidőre korlátozódik; háttérbeli ébresztést nem ígér.

A célok és a mentett intervallumbeállítások a meglévő settings/backup/Drive adatútban maradnak. Az aktív időzítő a mentett edzéstervezet része, befejezett naplóba nem kerül technikai időzítőállapot. Nincs adatbázis-törlés vagy kötelező adatmigráció.

## Ellenőrzések

- `npm test`: 118 regresszió; az új teszt helyi hét/hónap, duplikáció és különálló régi bejegyzés, dátumütközés, A/B sorrend, teljesített/aktív védelem, DST, intervallumhatár és hibás konfiguráció eseteket fed le.
- `npm run test:ui`: 57 szkript; az új teszt 320/360/393/412 px, HU/EN/DE/RO és matte/vivid témák mellett ellenőrzi a célokat, sikertelen tárolást, előnézet frissülését, valódi fázisváltást/rezgést, szünetet, újraindítást és nagy betűs elérhetőséget.
- A Phase 7 mérése azonos 240 × 8 × 4 adathalmazon szükséges előtte/utána. Az eredeti strukturális/időzítési limitek változatlanok. Chromium-idők nem helyettesítik a telefonos mérést.
- Aláírt APK: azonos `com.repforge.app`, 1.1.0 / 2698, eredeti signer; csomagolt webfájlok és forrás ZIP összehasonlítása a tényleges buildcommittal.

## Telefonos elfogadás

1. Rátelepítés a meglévő 2697-es appra, napló/program/backup megőrzése; célok mentése, újranyitás és biztonsági mentés.
2. Kihagyott A/B edzések előnézete és alkalmazása; teljesített és megkezdett edzés változatlan marad.
3. Rövid munka/pihenő/kör próbája: hang, rezgés, szünet, folytatás, nullázás, háttérből visszatérés és app-újraindítás; kézi sorozatbefejezés.
4. Coach-indoklás valós Health-adatokkal; üres állapotok és mentési jelzések.
5. Alap és nagy Android-betűméret: a kezdőlap, Egészség és Naptár alja, a lenyílók és a Mentés gomb elérhető; normál betűméretnél a fix áttekintés megmarad.

A [kiadási szabály](PERFORMANCE_REGRESSION_POLICY.md) szerint új UX-változás telefonos jóváhagyás után merge-elhető mainre. A felhasználó az összes fejlesztést tartalmazó 1.2.0 /2703 kiadás main merge-jét 2026. október 4-én jóváhagyta.
