# Natív migráció – funkcióegyezési ellenőrzés

Állapot: 2026-10-23, `cab522a` edzésindítás/lezárás és annak CI-eredménye alapján. A korábbi teljes technikai zöld alap `134adcd`. Ez forrásellenőrzés, nem készülékes elfogadás.
Összehasonlítási alap: a készüléken elfogadott gyorsított 1.3.71 /2735,
`v1.3.71-perf-20261022`, forrás `5d27aa9` (appkód: `715dc5d`).
A teljes natív kiváltás **még nincs kész**. A build, migráció és termékfunkciók
külön kapuk; a források megléte vagy a debug-fordítás nem funkcióegyezés.

**Telepítési korlát:** az aktuális natív teszt-APK-párt nem szabad a felhasználó
napi, adatokat tartalmazó alkalmazásának frissítéseként ajánlani. Előbb külön
emulátoron/teszteszközön kell lezárni a funkcióhiányokat, az adatmegőrzési és
mentés-visszaállítási vizsgálatot. Azonos csomagnévvel az élő adatteret használja;
a régi APK visszatelepítése nem állítja automatikusan vissza a korábbi adatállapotot.
Külön azonosítójú párhuzamos tesztkiadás jelenleg nem áll rendelkezésre.

| Terület | Megfigyelés | Következő igazolás/javítás |
| --- | --- | --- |
| Build | `134adcd`: debug/release sikeres, telefon unit 265/265, Wear unit 167/167, kerek UI 183/183; nincs hiba/kihagyás. | Készülékes és teljes funkcióegyezési kapuk. A technikai kapu zöld. |
| Aláírt frissítés | Eredeti csomagnév, magasabb kód, eredeti kulcs és tanúsítványegyezés előírva a workflow-ban. | `134adcd`: kiadási build, tanúsítvány/hash, tényleges `adb install -r` és szintetikus adatmigráció mindkét alap-APK-ról igazolva. Élő adatok és készülékes elfogadás még külön szükséges. |
| Adatmigráció | Aktuális WebView localStorage olvasás, helyi archívum, Room-tranzakció, sérült forrásnál nincs kész nyugta. | Unicode/nagy napló, törlés, GPS, fotó, program/nap, testsúly, naptár, beállítás, draft, régi health DB, eredeti tár és idempotencia tényleges upgrade-próbája 2/2 sikeres. A draft archiválása még nem működő natív folytatás. |
| Telefonos edzésindítás | `cab522a`: programnapi Home/Programok indítás tartós, stabil azonosítós natív sessiont készít; ismételt indítás/resume védett. Elfogadott main #228 súly/ismétlés-előtöltés portolva, lapozott adatforrással. | 10 új célzott teszt zöld. Gyors/outdoor gomb, importált draft tényleges natív folytatása és valós képernyőteszt még hátra van. |
| Lezárás | `cab522a`: UI és Wear közös `TrainPilotWorkoutFinalization`; esemény+kurzor tartós a nyugta előtt; napló+naptár tranzakció, valódi close, timer/GPS stop, takarítás, navigáció csak siker után. Mentési kerítés és stabil retry. | 9 új teszt zöld, köztük valódi SQLite-hibainjektálás, újrapróbálás, késői watch finish, handoff/restore, régi törlés. Tényleges natív UI és két készülékes próbák még szükségesek; az elvetési út külön további vizsgálatot igényel. |
| Telefonos editor–natív motor | A programnapi start már előkészíti a natív frame-et, a finish egyezteti a pending eseményeket. A Compose editor közbeni szerkesztés/complete/undo még részben közvetlen snapshot-mutáció, az automatikus controller foreground arm nincs bekötve. | Folyamatos közös editor/motor egyeztetés, valódi complete/undo parancsok, foreground arm és pihenő; a kézi módosítás nem veszhet el natív publikálás vagy handoff közben. Valós integrált UI-próba kell. |
| Stopper / GPS teljesítés | Az aktív Compose képernyőn nincs bekötött eredeti alsó sorozatstopper. A GPS stop csak leállítja a szolgáltatást. | Pozitív explicit stop készre jelöl; implicit stop/pause/0 nem; kétoldali gyakorlat mindkét oldal; undo és GPS útvonalkötés megőrzése. |
| Naptár / Coach | A `NavHost` nem tartalmaz külön naptár/Coach útvonalat. A Coach gomb a training útvonalra visz. | Eredeti funkciók átültetése és valós UI-próbája; programképernyő nem helyettesíti őket. |
| Napló teljesítmény | Compose LazyColumn van, de az adatréteg minden edzést részletekkel figyel. A Wear mini-napló az összes részlet betöltése után vesz csak nyolcat. | Korlátozott lekérdezés/lapozás, teljes adathalmazra működő szűrés; 0/200/1500 minta. Nem állítunk igazolt natív gyorsulást. |
| Health | Valós Samsung SDK adapter és explicit foreground frissítés helyreállítva; az onResume munka IO-n fut. Az eredeti felesleges preferenciavisszaírás-gyorsítás átvéve. | Tesztek, Samsung/HC készülékes engedély- és háttérváltási próba; hiányzó adat nem friss siker. |
| Wear | Az eredeti teljes natív launcher/szolgáltatások/protokoll visszakötve; az alternatív távirányítós prototípus nem launcher. A main gyorsított kiadása nem módosított Wear/shared appkódot. | Kerek UI 183/183 sikeres. Watch7, valódi telefon–óra szinkron és offline átadás/visszavétel még készülékes kapu. |
| Nyelvek / UI | Számos Compose szöveg közvetlenül magyar; a térkép semleges Bezárás gombot használ. | HU/EN/DE/RO, nagy betű/kis kijelző, elfogadott piros X, használható érintési célok. |

Friss teljes technikai kapu: **`39953886817` / `1a0cc94` SUCCESS**,
az Android-termékforrás a `cab522a` fájával azonos. Letöltött XML: telefon
**284/284**, Wear **167/167**; nyers kerek UI **183/183** egyedi eset,
aláírt szintetikus upgrade **2/2**, nincs hiba/kihagyás. A két korábbi fókuszhibát
az emulátor késői rendszerfelülete okozta; a leválasztott töltés szimulációja
helyreállította a környezetet. Védett komponens tiltása nincs a végső scriptben,
a teszt és termék fókuszfeltételek változatlanok.
[Gépi bizonylat](builds/2026-10-23-native-workout-start-finalization.json).

## Ellenőrzési sorrend

1. Fordítás és meglévő JVM tesztek; ugyanazon forrású, eredeti kulcsos APK-pár.
2. Régi tényleges kiadásokra telepítő, adatmegőrzést vizsgáló emulátorkapu.
3. Kritikus edzésindítás/stop/lezárás és szinkron viselkedés helyreállítása,
   célzott hibainjektálás és UI-próbák; az első két kapu újra az érintett forrásra.
4. Többi főképernyő, funkcióegyezés, fordítások és teljesítmény.
5. Élő adatokra csak külön teljes mentés és a releváns kapuk után; fizikai telefon
   és Galaxy Watch7 teszt. Nincs automatikus main merge vagy verzióemelés.
