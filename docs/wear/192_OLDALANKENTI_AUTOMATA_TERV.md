# #192 — oldalankénti automata időzítés: egyeztetendő működés

Állapot: **konkrét javaslat, alkalmazáskód még nem készült**. Dátum: 2026. október 17.

Alap: `91889326056ac257975360644ead39ded1b5a4d6` main, a készülékesen elfogadott #189 / #210 után. Az érintészár egyetlen folyamatos 3 másodperces tartásra felold. A #189 lezárva. Új ág: `feat/192-bilateral-automatic`.

A [#192 issue](https://github.com/fuloplevente1998/TrainPilot/issues/192) az átmeneti szabályok és a naplókompatibilitás jóváhagyását kéri kódolás előtt. A következő javaslat ezt a két döntést teszi konkréttá. Verzió marad 1.3.51 /2732.

## 1. Beállítás és menetrend

- Csak a `mp/oldal` egységű gyakorlat új, választható automata működése.
- Gyakorlatonként beállítható kezdő oldal: bal → jobb vagy jobb → bal.
- A bal és jobb munkaidő külön állítható; alapból azonos. A közös idő beállítása mindkettőt kitölti, az eltérő idő külön kapcsolható.
- Külön oldalcsere-pihenő és a meglévő sorozat-/körpihenő. Az oldalcsere-pihenő alapból 0 másodperc.
- Egy kör teljes bal+jobb pár. Megmarad a meglévő sorozat- és körszám: egy sorozat csak az előírt körök összes oldalának teljesítésekor kész.
- A páron belül az oldalcsere-pihenő, két teljes pár között a sorozat-/körpihenő következik; a két pihenő nem adódik össze ugyanazon határon.
- A kezdő oldal minden új körben ugyanaz. Az első munkaszakaszt a felhasználó indítja; utána a menetrend automatikus.
- Nulla pihenő közvetlen átmenetet jelent. Az edzés utolsó oldala után nincs záró pihenő; a következő gyakorlatra váltás a meglévő szabályt követi. Végső edzésmentés továbbra is tudatos művelet.

Példa: bal 30 mp, jobb 40 mp, oldalcsere 10 mp, sorozatpihenő 60 mp, két sorozat, sorozatonként egy kör:

| Szakasz | Idő | Következő |
| --- | ---: | --- |
| 1. sorozat, bal munka | 30 mp | Oldalcsere-pihenő |
| Oldalcsere-pihenő | 10 mp | Jobb oldal |
| 1. sorozat, jobb munka | 40 mp | Sorozatpihenő |
| Sorozatpihenő | 60 mp | 2. sorozat bal oldala |
| 2. sorozat, bal munka | 30 mp | Oldalcsere-pihenő |
| Oldalcsere-pihenő | 10 mp | Jobb oldal |
| 2. sorozat, jobb munka | 40 mp | Kész, nincs záró pihenő |

Teljes menetrend: **220 mp = 140 mp munka + 20 mp oldalcsere-pihenő + 60 mp sorozatpihenő**. Fordított sorrendben az oldalak és a hozzájuk tartozó munkaidők cserélődnek, az összidő ugyanaz.

## 2. Szünet, kihagyás, módosítás és visszavonás

- Szünet/folytatás megőrzi az oldalt, kört, sorozatot, hátralévő időt és a pihenő típusát. A kézi szünet külön idő, nem munka vagy tervezett pihenő.
- Az oldalcsere-pihenő kihagyása a másik oldalt, a sorozatpihenő kihagyása a következő párt indítja. A kihagyott pihenőidő nem ténylegesen eltöltött pihenő.
- Pihenő módosítása csak az éppen futó pihenőt érinti; a menetrend beállítása változatlan marad. A kétféle pihenő egyértelmű feliratot kap.
- Kézi oldal-/gyakorlatléptetés nem jelöl teljesítettnek kihagyott munkát. A már végzett rész megmarad, a részleges/kimaradt oldal állapota külön tárolódik, a sorozat nem lesz automatikusan kész.
- Automata kikapcsolásakor az elvégzett bal/jobb idő megmarad, az időzített léptetés megáll, és a meglévő kézi oldalas időmérés használható.
- A #188 Visszavonás az utolsó teljes sorozat rögzítésének meglévő, védett inverzét használja. A mért egészségadat és az eltöltött időnapló megmarad; nincs új párhuzamos visszavonási rendszer.
- A futó munkaszakasz tervének módosítása nem írja át visszamenőleg az aktuális oldalt. A módosítás szüneteltetett állapotban, a következő még nem indított munkaszakasztól alkalmazható.

## 3. Új naplóadat és régi adatok

Javasolt új időbontás kizárólag az új oldalankénti automatához:

| Adat | Tartalom |
| --- | --- |
| Bal/jobb munkaidő | Kizárólag az adott oldalon ténylegesen végzett munka; pihenő és kézi szünet nélkül |
| Oldalcsere-pihenő | Ténylegesen eltöltött idő a két oldal között |
| Sorozat-/körpihenő | Ténylegesen eltöltött idő két teljes pár között |
| Kézi szünet | Felhasználói megállítás tényleges ideje |
| Állapot | Oldalanként kész / részleges / kihagyott; a teljes sorozat állapota ettől külön |

A meglévő sorozat `leftSeconds` / `rightSeconds` mezői őrzik a tényleges oldalas munkát. Több kör esetén az adott oldal köreinek végzett munkaideje összeadódik. A meglévő `reps` kompatibilitási érték a két oldal kisebb munkaideje marad; nem tartalmaz pihenőt. A sorozat `done` állapota csak az összes előírt oldal/kör sikeres teljesítésekor igaz.

A meglévő kanonikus időnapló bővül verziózott oldalas részletekkel; nem készül második összesítendő napló. A tervezett 2-es séma oldalankénti és pihenőtípus szerinti részleteket, valamint a meglévő összesítő mezőket tartalmazza:

```text
workMs = leftWorkMs + rightWorkMs
restMs = sideRestMs + setRestMs
actualTotalMs = workMs + restMs + pausedMs
```

A részletek az összesítő mezők bontásai, nem hozzáadandó további idők. Minden szakasz stabil edzés-/gyakorlat-/sorozat-/kör-/oldal-/szakaszazonosítóhoz és az időzítőtulajdonoshoz kötött. Az idővonal pozíciója kihagyáskor előre ugorhat; az eltöltött idő nem.

Az 1-es naplóséma és a korábbi `reps` értékek változatlanul olvashatók. A régi telefonos munka+pihenő és órás tiszta munka értékeiből nem következtetünk utólag bal/jobb vagy pihenőbontásra. A hiányzó részlet ismeretlen, nem mért nulla. A hagyományos `mp`, ismétléses és távmérős gyakorlatok működése nem változik ezzel.

## 4. Telefon, óra és megszakítás

- Egy időzítőtulajdonos marad: telefonos indításnál telefon, órás indításnál óra. A másik eszköz megjelenítő/vezérlő; a kapcsolatvesztés nem hoz létre új időzítőt.
- A szakaszok, részleges oldalállapot és napló natívan, tartósan mentődnek. A lezárt kijelző és a felületi frissítés nem vezérli az idővonalat.
- Egyeztetett protokollképesség jelzi az oldalas támogatást. Nem támogatott telefon/óra párosnál az új automata nem indul el félreértelmezett menetrenddel; az eltérés látható, a kézi használat elérhető.
- Meglévő egyszeri 3–2–1 előjelzés és szakaszváltási jelzés; a tulajdonos kezeli a jelzés idővonalát, ismételt szinkron nem ismétli meg.
- Képernyőváltás, normál háttérműködés és kapcsolatvesztés alatt a tulajdonos tovább fut. Szolgáltatás-/eszközújraindításnál a meglévő helyreállítható szüneteltetés őrzi a pontos oldalt és hátralévő időt; nincs utólag kitalált teljesítés.
- Telefonon az aktív számláló az alsó vezérlősorban marad. Órán az aktuális és következő oldal, szakasz, hátralévő idő és sorozat/kör kis kerek kijelzőn is olvasható. Az érintészár és a kézi stopper meglévő védelmei megmaradnak.

## 5. Feltárt módosítási helyek

- `www/app.js`: utolsó aktív `automatic-workout-162.js`, `intervals-162.js`, `automatic-time-162.js`, `phone-execution-162.js`, `wear-automatic-162.js` szakaszok; beállítás, kijelzés és szinkron.
- Telefon: `PhoneWorkoutPlans`, `IntervalTimeline`, `IntervalTimeLedger`, `PhoneWorkoutExecutionStore` és az időzítőszolgáltatás. Jelenleg a sima `mp` időzített, az új támogatás kifejezett bővítés.
- Wear: `WearAutomatic`, `WearAutomaticEngine`, `WearAutomaticService`, pillanatkép/protokoll és kijelzés. A `WearAutomaticEngine.timed` jelenleg a sima `mp` egységet fogadja el.
- A telefonos meglévő `PhoneWorkoutTimeTest` szándékosan igazolja a régi munka+pihenő sorozatértéket. Ez kompatibilitási regresszió marad, az új oldalas tesztek külön, tiszta munkaidőt várnak.

## 6. Megvalósítás és ellenőrzés

1. A fenti menetrend és új naplóbontás felhasználói egyeztetése. Ez a dokumentum még javaslat.
2. Közös példaadatok és azonos szakaszhatárok tesztje JS/Java/Kotlin alatt; új protokoll és régi adatok kompatibilitása.
3. Natív telefonos és Wear időzítés, tartós részleges oldalállapot, kihagyás/szünet/visszavonás, egyetlen tulajdonos és pontosan egyszeri rögzítés.
4. Telefonos és kerek Wear-beállítás/kijelzés, magyar/angol/német/román szövegek, témák és nagy betűméret.
5. Azonos forrású Quick, teljesítmény, telefonos UI/natív/build, Wear Kotlin/build és teljes kerek UI-kapuk GitHub Actionsben. Helyben csak célzott rövid ellenőrzés.
6. Ellenőrzött, azonos forrású telefon+Wear teszt-APK-pár, változatlan verzióval. Külön készülékes elfogadás után main és #192 lezárása.

Kötelező regresszió: mindkét oldalsorrend, azonos/eltérő munkaidő, több sorozat/kör, nulla/eltérő pihenő, utolsó oldal, szünet mind a négy időtípusban, pihenő kihagyása/módosítása, kézi léptetés, részleges oldal, késői/dupla parancs, offline/újracsatlakozás, restart, mentés/backup/import, hagyományos időzítés és #188/#189.
