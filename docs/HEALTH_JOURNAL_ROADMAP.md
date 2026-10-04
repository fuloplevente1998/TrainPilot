# Health Connect-egészségnapló — 1.2.6 /2709

A felhasználó 2026-10-04-én jóváhagyta a PR #114 main merge-jét és az 1.2.6 /2709 stabil kiadást. Előzmények: #110, #113; végrehajtási állapot: [#119](https://github.com/fuloplevente1998/TrainPilot/issues/119).

## Megvalósult működés

- A Health Connect engedélyezett eredeti rekordjai privát natív SQLite-tárolóba kerülnek, típussal, eredeti azonosítóval, módosítási idővel és eredeti alkalmazással. Ismételt olvasás, módosítás és törlés nem duplikál; a korábbi helyi napló átállása idempotens.
- A felület dátumonként egy napi összesítést mutat. Automatikus módban a legteljesebb napi lépésszámú eredet adja a lépés-, távolság- és kalóriaösszesítést. A Health Connect prioritás külön választható; átfedő eredeteket nem adunk össze.
- Az eredeti mérési rekordok külön lenyithatók és lapozhatók; technikai forrásazonosító csak a részleteknél jelenik meg. Samsung Health-adatot is Health Connectből olvasunk, nincs közvetlen Samsung-integráció.
- A felület legfeljebb 30 napi összesítést és korlátozott rekordlapot tart memóriában. Az alvás a befejezés napjához tartozik; hiányzó adat és valódi nulla különbözik.
- Appnyitáskori frissítés külön engedélyezhető. A háttérfrissítés külön Health Connect-rendszerengedélyt és megfelelő Android-verziót igényel; Android ütemezi, késleltetheti. Kényszerleállítás után újra meg kell nyitni az alkalmazást.
- Az edzéshez tartozó egészségadatok továbbra is a stabil edzésazonosítóhoz és tényleges aktív időszakhoz kapcsolódnak, szünetek nélkül. Napi összegből nem állítunk elő pontos edzésértéket.
- A teljes natív napló csak külön egészségmentési hozzájárulással indított kézi JSON/ZIP-be kerül, Drive-pillanatképbe nem. Visszaállítás és helyi törlés tranzakciós, a régi rekordok megőrzése és a késői írások védelme megmarad.

## HRV-követés — #113

Csak a Health Connectnek valóban átadott, engedélyezett HRV-adat olvasható. Ha Samsung Health nem osztja meg, a TrainPilot nem tudja azt Health Connectből előállítani. Átlagpulzusból nem képzünk mesterséges HRV-t. További hozzáférés csak igazolt adatforrással, mértékegységgel, időbélyeggel és kompatibilitással tervezhető.

## Ellenőrzés

119 Node-regresszió, 4 futtatóteszt, 62 Chromium-szkript, Android/JVM release-tesztek és a változatlan teljesítménykapuk; a kiadott APK pontos forrásának, verziójának és eredeti aláírásának ellenőrzése. Telefonos main/release-jóváhagyás rögzítve a #119-ben. [Kiadási megjegyzések](releases/v1.2.6.md).
