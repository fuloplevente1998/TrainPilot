# Natív migráció – funkcióegyezési ellenőrzés

Állapot: 2026-10-23, a teljesen zöld `134adcd` technikai kapu alapján. Ez forrásellenőrzés, nem készülékes elfogadás.
Összehasonlítási alap: a készüléken elfogadott gyorsított 1.3.71 /2735,
`v1.3.71-perf-20261022`, forrás `5d27aa9` (appkód: `715dc5d`).
A teljes natív kiváltás **még nincs kész**. A build, migráció és termékfunkciók
külön kapuk; a források megléte vagy a debug-fordítás nem funkcióegyezés.

| Terület | Megfigyelés | Következő igazolás/javítás |
| --- | --- | --- |
| Build | `134adcd`: debug/release sikeres, telefon unit 265/265, Wear unit 167/167, kerek UI 183/183; nincs hiba/kihagyás. | Készülékes és teljes funkcióegyezési kapuk. A technikai kapu zöld. |
| Aláírt frissítés | Eredeti csomagnév, magasabb kód, eredeti kulcs és tanúsítványegyezés előírva a workflow-ban. | `134adcd`: kiadási build, tanúsítvány/hash, tényleges `adb install -r` és szintetikus adatmigráció mindkét alap-APK-ról igazolva. Élő adatok és készülékes elfogadás még külön szükséges. |
| Adatmigráció | Aktuális WebView localStorage olvasás, helyi archívum, Room-tranzakció, sérült forrásnál nincs kész nyugta. | Unicode/nagy napló, törlés, GPS, fotó, program/nap, testsúly, naptár, beállítás, draft, régi health DB, eredeti tár és idempotencia tényleges upgrade-próbája 2/2 sikeres. A draft archiválása még nem működő natív folytatás. |
| Telefonos edzésindítás | `TrainPilotNavigation.kt`: az `onStartWorkout` és `onStartProgramDay` callback csak navigál. Nem készít sessiont; az aktív képernyő önmagában sem indít új edzést. | Kanonikus edzésindítás bekötése; meglévő session/draft védelme, stabil azonosítók, utolsó értékek átvétele. |
| Lezárás | `ActiveWorkoutScreen.kt` rögtön visszanavigál az aszinkron finish után. `ActiveWorkoutViewModel.finishWorkout` nem teljesíti a naptárat, `discardExecution`-t hív és hibát nyel el. Nincs igazolt natív eseményegyeztetés. | Közös, újrapróbálható lezárás; események mentése a nyugta előtt; valódi close, naptár, timer/GPS, hibavisszajelzés; navigáció csak siker után. Kapcsolódó Wear finish ág is vizsgálandó. |
| Stopper / GPS teljesítés | Az aktív Compose képernyőn nincs bekötött eredeti alsó sorozatstopper. A GPS stop csak leállítja a szolgáltatást. | Pozitív explicit stop készre jelöl; implicit stop/pause/0 nem; kétoldali gyakorlat mindkét oldal; undo és GPS útvonalkötés megőrzése. |
| Naptár / Coach | A `NavHost` nem tartalmaz külön naptár/Coach útvonalat. A Coach gomb a training útvonalra visz. | Eredeti funkciók átültetése és valós UI-próbája; programképernyő nem helyettesíti őket. |
| Napló teljesítmény | Compose LazyColumn van, de az adatréteg minden edzést részletekkel figyel. A Wear mini-napló az összes részlet betöltése után vesz csak nyolcat. | Korlátozott lekérdezés/lapozás, teljes adathalmazra működő szűrés; 0/200/1500 minta. Nem állítunk igazolt natív gyorsulást. |
| Health | Valós Samsung SDK adapter és explicit foreground frissítés helyreállítva; az onResume munka IO-n fut. Az eredeti felesleges preferenciavisszaírás-gyorsítás átvéve. | Tesztek, Samsung/HC készülékes engedély- és háttérváltási próba; hiányzó adat nem friss siker. |
| Wear | Az eredeti teljes natív launcher/szolgáltatások/protokoll visszakötve; az alternatív távirányítós prototípus nem launcher. A main gyorsított kiadása nem módosított Wear/shared appkódot. | Kerek UI 183/183 sikeres. Watch7, valódi telefon–óra szinkron és offline átadás/visszavétel még készülékes kapu. |
| Nyelvek / UI | Számos Compose szöveg közvetlenül magyar; a térkép semleges Bezárás gombot használ. | HU/EN/DE/RO, nagy betű/kis kijelző, elfogadott piros X, használható érintési célok. |

## Ellenőrzési sorrend

1. Fordítás és meglévő JVM tesztek; ugyanazon forrású, eredeti kulcsos APK-pár.
2. Régi tényleges kiadásokra telepítő, adatmegőrzést vizsgáló emulátorkapu.
3. Kritikus edzésindítás/stop/lezárás és szinkron viselkedés helyreállítása,
   célzott hibainjektálás és UI-próbák; az első két kapu újra az érintett forrásra.
4. Többi főképernyő, funkcióegyezés, fordítások és teljesítmény.
5. Élő adatokra csak külön teljes mentés és a releváns kapuk után; fizikai telefon
   és Galaxy Watch7 teszt. Nincs automatikus main merge vagy verzióemelés.
