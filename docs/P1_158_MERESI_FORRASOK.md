# P1 #158 — telefonos napló mérési forrásai

Feladat: [#158](https://github.com/fuloplevente1998/TrainPilot/issues/158). Alap: `cac8937559f064b5141b068a9687b71a704f760a`, a készülékesen elfogadott P0 #195 main-egyesítése. Fejlesztési ág: `fix/158-health-source-provenance`. Verzió: **1.3.4 /2730**.

## Működés

- Az adott edzéshez érkezett, ellenőrzött saját órás mérés az alapértelmezett forrás. Külön választható a telefon által olvasott adat. Ha nincs órás összesítés, a hiány egyértelműen szerepel.
- A Health Connect olvasási csatornája és az adatot közlő alkalmazás külön információ. Például `Health Connect · Samsung Health` csak ténylegesen közölt Samsung-eredetnél; a globális provider-preferencia ezt nem bizonyítja. Közvetlen Samsung SDK-válasz és vegyes provider külön jelölést kap.
- Ha a Samsung hiányzó edzésadatait teljes egészében Health Connect pótolja, a csatorna Health Connect marad; a pótolt edzésenergia is megtartja a saját providerét. Több aktív időablak eltérő csatornái együtt megmaradnak a forrásjelölésben.
- A Wear és a telefon által olvasott energia nem adódik össze. A hiányzó értékből nincs nulla; a valódi nulla megmarad. Az órás átlag/min/max pulzus, mintaszám, kalória, lépés, mérési idő, távolság és max. sebesség csak érvényes, rendelkezésre álló értéknél jelenik meg.
- A késői órás revízió ugyanahhoz az edzéshez tartozik. Megmarad a nyitott napló és a kézzel kiválasztott adatforrás, listaátrendezés esetén is. A meglévő Mini Napló-publikáció a frissített összesítést küldi tovább.
- Az idegen edzéshez vagy más időablakhoz tartozó cache elutasítva. Sikertelen olvasás nem rejti el a korábbi, ugyanarra az időablakra mentett érvényes adatot; mellette megjelenik a hiba. A részleges eredmény külön jelölést kap.
- Órás forrás mellett a Frissítés a függő órás parancsokat olvassa be; nem kér le Health Connect adatot. Telefonos forrás mellett az adott edzés saját időablaka frissül.

## Automatizált bizonyíték

`tests/browser/health-source-provenance-158.cjs` a teljes szállított `app.js` és az `index.html` végső inline felülírásai mellett tesztel: 45:36 **szintetikus** edzés, Wear 390 vs. HC 49 kcal, késői 400 kcal revízió, valós nulla, külön eredet/csatorna, listaátrendezés, stabil Frissítés/összecsukás, megőrzött forrás és naplónézet, Mini Napló-publikáció, idegen/elavult revízió, idegen/eltérő időablak cache, olvasási hiba és 320/393 px nagyított betűs nézet. A régi mainen ugyanaz a teszt a feltétel nélküli Samsung-feliratot reprodukálja.

Meglévő `health-workout-energy.cjs`: külön edzés-, aktív- és nyugalmi energiát is tartalmazó kalória, aktív időablakok/éjfél, hiányzó kalória. Meglévő `phase6-journal-media-health-42-43.cjs`: nyitott napló, scroll és frissítés megőrzése 320/360/390/412 px méreten. A Node-forrásválasztó teszt HU/EN/DE/RO szöveget, nullát, invalid pulzust, tényleges provider/origin metaadatot ellenőriz.

## Ellenőrzött tesztbuild — 2026. október 14.

- Fejlesztési PR: [#196, draft](https://github.com/fuloplevente1998/TrainPilot/pull/196).
- APK forrása: `c800b4ee49ba2aeb566eab3cba347349c5a784d7`; verzió **1.3.4 /2730**.
- [Teljes telefonos gate](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38557899870): **140/140 Node-csomag, 93/93 Chromium-csomag, natív unit tesztek és aláírt APK-build sikeres**.
- [Quick Validation](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38557899869) és [Performance Baseline](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38557896097): sikeres.
- [APK + forrás + ellenőrzőösszegek](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38557899870/artifacts/11891970699). Az Actions artifact 14 napig elérhető; a forráscommit és a hash tartós azonosító.
- APK SHA-256: `17ec80ee73ce6e9b86b6679a32025d7a76671efc701a4fbd8bcbf6d5fd7cc2e5`; **5817681 bájt**.
- Eredeti tanúsítvány SHA-256: `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`. A csomag kriptográfiai aláírása, package/verzió, sourceCommit, forrás-archívum, becsomagolt webes fájlok és érintett natív források külön ellenőrizve.

Az APK a fenti kódcommitból készült; a későbbi átadási dokumentáció commitjai nem változtatják meg az alkalmazás forrását. [Gépi buildbizonylat](builds/2026-10-14-p1-158.json). **Fizikai mérési elfogadás még nincs**; az órán továbbra is a készülékesen elfogadott `9b94a10` APK használható.


## Készülékes elfogadás

1. Az új telefonos APK és az elfogadott Wear `9b94a10` használatával először végezz rövid, például **3–5 perces** edzést. Ez az új forrásjelölés, mentés és késői frissítés első készülékes ellenőrzése; nincs 45 perces technikai minimum. Azonosítsd a konkrét edzést, írd fel az óra befejezéskori kcal/pulzus/idő értékeit.
2. Telefonos Napló → ugyanaz az edzés → **TrainPilot óra**. A beérkezett saját órás összesítés legyen az alapértelmezés, az adatok az órás összesítéssel egyezzenek; a kijelző kerekítése számíthat. HC aktív/összes energia nem helyettesítheti ezeket.
3. Válts **Health Connect** forrásra. Külön adatokat és a ténylegesen közölt alkalmazáseredetet kell látni; nem kell a két külön mérés számainak azonosnak lenniük. Ne történjen összeadás vagy adatnullázás.
4. Rövid kapcsolatmegszakítás után a nyitott naplóban érkezzen meg a végső órás összesítés. Maradjon egyetlen edzés; a nyitott rész és a kézi forrásválasztás megmaradjon. Az órás Mini Napló ugyanazt a friss összesítést kapja.
5. Kapcsolat/olvasási hiba mellett a már beérkezett érvényes adat maradjon meg; hiányzó/részleges mérés ne váljon teljesnek vagy nullának. Ellenőrizd a Frissítés működését mindkét forrásnál.

A #158 rövid készülékes ellenőrzése még nincs visszaigazolva, ezért nyitott. A hosszabb összehasonlítás az eredeti, 45:36 hosszú edzés kcal-eltéréséhez kapcsolódó utóellenőrzés: a következő rendes edzésben végezhető, külön 45 perces tesztedzés és az emiatti fejlesztési megállás nélkül. Main-egyesítés és release külön felhasználói elfogadással. Következő feladat #159 → #191 → #163; jelen körben nincs új Wear UI vagy térképfunkció.
