# TrainPilot Wear OS 1.2.11 /2718

Megkezdett edzésnél az órás kezdőlap alsó gombsora Folytatás – Törlés – Menü. A Törlés a meglévő megerősítő képernyőt nyitja meg. Edzés nélkül marad az Indítás/Napok – Naptár – Menü gombsor és a következő edzés megjelenítése. A Telefon gomb mindkét állapotban megmarad, a naptár aktív edzés közben a Menüben elérhető.

Az órás mérések indításánál és lezárásánál a Health Services sikeres, adat nélküli (Void/null) válasza korábban Kotlin nullparaméter-hibát okozott. Ezeket a parancsokat a kód most külön kezeli az adatot visszaadó lekérdezésektől. A hibás vagy megszakított parancs továbbra is hibának számít; a már leállított szolgáltatás késői válasza nem módosít állapotot.

A korábbi verzió konkrét nullparaméter-hibájával elakadt mérés az aktív edzéshez újrapróbálható automatikusan, ha a mérések engedélyezve vannak és az engedélyek megvannak. Ugyanaz az edzés és az addigi mért összesítések maradnak meg. Más mérési hibák nem indítanak automatikus újrapróbálást.

Nyolc célzott natív regresszió ellenőrzi a sikeres nullos indítás/lezárás válaszát, a végrehajtó használatát, a hiba és megszakítás kezelését, a szolgáltatás leállítása utáni késői választ, a normál és hiányzó lekérdezési adatot, valamint a régi hiba szűk felismerését.

Kizárólag órás kiadás, a meglévő kiadási aláírással. A telefonos TrainPilot 1.2.10 kompatibilis; ehhez a javításhoz nem szükséges telefonos frissítés. Az órás verzió és kiadás önálló a `Wear Release Gate` folyamatban, a kiadási címke `wear-v1.2.11`.
