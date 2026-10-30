# AI Studio – jóváhagyott képernyőreferencia, 2026-10-28

A felhasználó a [Drive-mappa](https://drive.google.com/drive/folders/1G-s2Llme03Wah_7J_TDrS_xs5go9Ut9V)
18 valódi AI Studio-appképe alapján kérte: „ilyen legyen az appunk”. Mind a 18 képet
megnyitottuk. Ezek az elsődleges vizuális elfogadási referenciák; a korábbi 18:49-es
export a komponensek visszakereshető forrása. A működés forrása a PR270 kanonikus
natív rendszere marad. A képek nem igazolják a Studio demó adatkezelését.

## Pontosan mit kell követni?

- Sötét háttér, finoman elkülönülő kártyák, vékony arany kiemelések, lekerekítések.
- Kompakt fejléc: az eredeti arany súlyzó, TrainPilot, nyelv, paletta és beállítások.
- Öt felső főfül: Kezdőlap, Edzés, Napló, Fejlődés, Egészség.
- A kártyák sorrendje, arányai, a belső lapok és a lenyitható részletek felépítése.
- Home 2×2 állapotkártyák, gyorshivatkozások és Coach-ajánlat; Fejlődés volumenes
  főpanel, négy mutató és anatómiai kártya; Egészség napi áttekintés és részletsorok.
- A részletes eredeti natív anatómia és a launcher saját súlyzóvektora megmarad.

A Chrome címsora, az AI Studio kerete, az emulátor oldalsó gombjai és az alsó
Preview/Chat sáv a fejlesztőeszközhöz tartoznak. Az összehasonlítás az alkalmazás
belső nézetére vonatkozik. A teljes JPEG 1080×2392 mérete nem bizonyítja a belső
Studio-emulátor dp-szélességét, DPI-jét vagy fontScale értékét.

A natív összehasonlítás telefonprofilja: **1080×2392 px, 450 logikai dpi,
384 dp szélesség, fontScale 0.90**. A 450 a felhasználó 384 dp-s rendszerbeállításából
számolt logikai sűrűség, nem a panel fizikai PPI-je. Nagy betűnél és fordításoknál
a feliratok olvashatósága, görgethetősége és a 48 dp-s érintési célok megmaradnak.

## Képindex

A [gépileg olvasható index](STUDIO_DRIVE_VISUAL_REFERENCE_2026-10-28.json) minden
eredeti fájl nevét, Drive-azonosítóját, linkjét, byte-méretét, felbontását és SHA256
hashét rögzíti. Az eredeti Drive-fájlokat nem másoltuk a GitHubra; hozzáférésük a
Drive jogosultságait követi. Az index nem tartalmaz fiókadatot vagy képpontokat.

| Sorszám | Fájlnév időrésze | Megjelenített nézet |
|---|---|---|
| 01 | 10-06-51 | Kezdőlap: fejléc, tervező, következő edzés, gyors edzés, óra |
| 02 | 10-06-56 | Kezdőlap alsó része: 2×2 állapot, Programok/Naptár/Coach, célok, ajánlat |
| 03 | 10-07-04 | Edzés: gyorsindító alsó választópanel |
| 04 | 10-07-07 | Gyakorlatkönyvtár: kereső, izomcsoportok, képes sorok, videó |
| 05 | 10-07-11 | Kültéri mérés: GPS-kártya, térkép, idő és mutatók |
| 06 | 10-07-20 | Napló: hónap-/napválasztó párbeszédablak |
| 07 | 10-07-25 | Fejlődés: belső lapok, volumen, négy mutató, anatómia |
| 08 | 10-07-29 | Statisztika: egyéni csúcsok / kardió választó, üres tartalom |
| 09 | 10-07-35 | Erő és 1RM: gyakorlatválasztó, görbe, rekord és legjobb sorozat |
| 10 | 10-07-39 | Egészség: napi 3×2 áttekintés, pulzus, testsúly és kapcsolatok |
| 11 | 10-07-41 | Egészség alsó része: Wear-sor, szinkron, engedélyek, forrás |
| 12 | 10-07-48 | Testsúly lenyitva: test/fitnesz, változás, új mérés, időszakok |
| 13 | 10-07-52 | További egészségadatok lenyitott 2×3 rácsa |
| 14 | 10-07-55 | Egészségnapló lenyitott napi bejegyzései |
| 15 | 10-08-01 | Kapcsolt eszközök: Wear-vezérlés, Health Connect, Samsung Health |
| 16 | 10-08-05 | Beállítások: profil, téma, nyelv, időzítő, hang, progresszió |
| 17 | 10-08-11 | Beállítások: eszközök, mentés, engedélyek, privát fotók |
| 18 | 10-08-14 | Fejléc nyelvválasztó legördülő menü |

## Igazolt eltérések a natív a27cc2fe képekhez képest

Összehasonlítási alap: [változatlan natív képek](previews/studio-a27cc2fe-owner/README.md),
[teljes tesztkör](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40660277579).
A tesztek zöldek, a teljes vizuális egyezés **még nincs kész**.

| Terület | Megfigyelés | Következő megvalósítási cél |
|---|---|---|
| Közös felületek | A natív #1 Graphite/Gold kép is világosabb és erősebben keretezett kártyákat mutat. A Home több helyet foglal a belső nézet arányaihoz képest. | Az exportból ellenőrzött szín-/térköz-/tipográfiaértékek következetes használata; azonos belső nézeten új kép. A JPEG-ből nem becsülünk pontos színkódot. |
| Felső fülek | Fent vannak és valódi útvonalra vezetnek, de 384 dp-n a sor görgethető; a referencián mind az öt látszik. | Alap betűméretnél öt olvasható fül elhelyezését külön vizsgálni; nagy betűnél megőrizni a kipróbált görgetést és teljes feliratokat. |
| Home | A tervező/edzéshős/gyorsindító/óra/állapotkártyák sorrendje már felismerhető, a sűrűség és a részletek eltérnek. | A teljes Home szerkezetének és színes állapotikonjainak átültetése valós mérésekkel és meglévő műveletekkel. |
| Fejlődés | A natív képen régi „Utolsó 30 nap” blokk látszik a működő új izomkártya előtt. A Studio három belső lapot, volumenes főpanelt és négy kompakt mutatót mutat. | Teljes képernyőhierarchia átültetése, natív volume/strength/cardio/PR állapotokra kötve. Az izomkártya már valós időszakokat és sorozatokat használ. |
| Egészség | A natív napi panel 2 oszlopos, külön nyers forrásazonosítókkal és régi felső lapokkal; a referencián 3×2 áttekintés és lenyitható sorok vannak. | Studio felépítés, a teljes meglévő egészségtartalommal, forrás-/engedély-/üres állapotokkal. |
| BLE | A natív képen még külön „BLE Okosóra (#111)” fül van. A felhasználó korábban kifejezetten nem kért ilyen külön felületet. | A végső főfelület Wear OS / Health Connect / Samsung Health irányát követni; a régi BLE-próbát nem önálló termékoldalként átvenni. |
| Részletes képernyők | A Drive-képek megadják a könyvtár, választók, Health-részletek, eszközök és beállítások kívánt stílusát. | Ezeket külön portolni a kanonikus műveletekhez; jelen dokumentum nem állít róluk kész natív vizuális egyezést. |

## Adatok és állapotok

A képeken szereplő számok, rögzített dátumok, példautak, előre jelzett kapcsolatok
és nézetváltó demógombok nem éles alkalmazáskövetelmények. Például a 0 bpm nem
„normális”, a hiányzó alvás nem „jó minőségű”, a 0 kg nem mentett testsúly, és
kapcsolat/visszaigazolás nélkül nincs sikeres óraátadás. Az üres/hiba/betöltés
állapotok azonos vizuális nyelven, valós jelentéssel jelenjenek meg.

Wear OS, Health Connect és Samsung Health a támogatott integrációs irány.
A meglévő session-, migrációs-, adatcsere-, jogosultság- és mentési védelmek,
valamint a meglévő testTag-ek megőrzendők. Ez a referencia-rögzítés dokumentációs
változás; nem új appbuild vagy felhasználói készülékes elfogadás.
