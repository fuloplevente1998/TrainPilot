# Natív migráció – funkcióegyezési ellenőrzés

Állapot: 2026-10-23, az `eb1d21f` alkalmazásforrás és a tesztmanifesteket javító
`7c73ebe` alapján. Ez forrásellenőrzés, nem készülékes elfogadás.
Összehasonlítási alap: a készüléken elfogadott gyorsított 1.3.71 /2735,
`v1.3.71-perf-20261022`, forrás `5d27aa9` (appkód: `715dc5d`).
A teljes natív kiváltás **még nincs kész**. A build, migráció és termékfunkciók
külön kapuk; a források megléte vagy a debug-fordítás nem funkcióegyezés.

| Terület | Megfigyelés | Következő igazolás/javítás |
| --- | --- | --- |
| Build | `eb1d21f`: mindkét debug APK és a JVM tesztek forrása lefordult. Wear XML: 167/167, nincs hiba/kihagyás. Telefonos unit futást tesztmanifest állította meg. | `7c73ebe` CI; teljes telefonos unit, aláírt release és emulátor. |
| Aláírt frissítés | Eredeti csomagnév, magasabb kód, eredeti kulcs és tanúsítványegyezés előírva a workflow-ban. | Sikeres kiadási build és tényleges `adb install -r` mindkét 1.3.71 alap-APK-ról. |
| Adatmigráció | Aktuális WebView localStorage olvasás, helyi archívum, Room-tranzakció, sérült forrásnál nincs kész nyugta. | Unicode/nagy napló, törlés, GPS, fotó, program, beállítás, draft, régi health DB tényleges upgrade-próbája. A draft archiválása még nem működő natív folytatás. |
| Telefonos edzésindítás | `TrainPilotNavigation.kt`: az `onStartWorkout` és `onStartProgramDay` callback csak navigál. Nem készít sessiont; az aktív képernyő önmagában sem indít új edzést. | Kanonikus edzésindítás bekötése; meglévő session/draft védelme, stabil azonosítók, utolsó értékek átvétele. |
| Lezárás | `ActiveWorkoutScreen.kt` rögtön visszanavigál az aszinkron finish után. `ActiveWorkoutViewModel.finishWorkout` nem teljesíti a naptárat, `discardExecution`-t hív és hibát nyel el. Nincs igazolt natív eseményegyeztetés. | Közös, újrapróbálható lezárás; események mentése a nyugta előtt; valódi close, naptár, timer/GPS, hibavisszajelzés; navigáció csak siker után. Kapcsolódó Wear finish ág is vizsgálandó. |
| Stopper / GPS teljesítés | Az aktív Compose képernyőn nincs bekötött eredeti alsó sorozatstopper. A GPS stop csak leállítja a szolgáltatást. | Pozitív explicit stop készre jelöl; implicit stop/pause/0 nem; kétoldali gyakorlat mindkét oldal; undo és GPS útvonalkötés megőrzése. |
| Naptár / Coach | A `NavHost` nem tartalmaz külön naptár/Coach útvonalat. A Coach gomb a training útvonalra visz. | Eredeti funkciók átültetése és valós UI-próbája; programképernyő nem helyettesíti őket. |
| Napló teljesítmény | Compose LazyColumn van, de az adatréteg minden edzést részletekkel figyel. A Wear mini-napló az összes részlet betöltése után vesz csak nyolcat. | Korlátozott lekérdezés/lapozás, teljes adathalmazra működő szűrés; 0/200/1500 minta. Nem állítunk igazolt natív gyorsulást. |
| Health | Valós Samsung SDK adapter és explicit foreground frissítés helyreállítva; az onResume munka IO-n fut. Az eredeti felesleges preferenciavisszaírás-gyorsítás átvéve. | Tesztek, Samsung/HC készülékes engedély- és háttérváltási próba; hiányzó adat nem friss siker. |
| Wear | Az eredeti teljes natív launcher/szolgáltatások/protokoll visszakötve; az alternatív távirányítós prototípus nem launcher. A main gyorsított kiadása nem módosított Wear/shared appkódot. | Megőrzött kerek UI teszt, Watch7, valódi telefon–óra szinkron és offline átadás/visszavétel. |
| Nyelvek / UI | Számos Compose szöveg közvetlenül magyar; a térkép semleges Bezárás gombot használ. | HU/EN/DE/RO, nagy betű/kis kijelző, elfogadott piros X, használható érintési célok. |

## Ellenőrzési sorrend

1. Fordítás és meglévő JVM tesztek; ugyanazon forrású, eredeti kulcsos APK-pár.
2. Régi tényleges kiadásokra telepítő, adatmegőrzést vizsgáló emulátorkapu.
3. Kritikus edzésindítás/stop/lezárás és szinkron viselkedés helyreállítása,
   célzott hibainjektálás és UI-próbák; az első két kapu újra az érintett forrásra.
4. Többi főképernyő, funkcióegyezés, fordítások és teljesítmény.
5. Élő adatokra csak külön teljes mentés és a releváns kapuk után; fizikai telefon
   és Galaxy Watch7 teszt. Nincs automatikus main merge vagy verzióemelés.
