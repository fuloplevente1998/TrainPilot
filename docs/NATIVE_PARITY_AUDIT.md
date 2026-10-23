# Natív migráció – funkcióegyezési ellenőrzés

Állapot: 2026-10-23, `922b6b7` CI / `e164ef3` GPS/pihenő/gyakorlatváltás, `b6e8158` stabil UI-célzás/alsó stopper, `9492755` editor/motor és `cab522a` indítás/lezárás alapján. A korábbi teljes technikai zöld alap `134adcd`. Ez forrásellenőrzés, nem készülékes elfogadás.
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
| Telefonos editor–natív motor | `9492755`: a teljes natív frame a tartós szerkesztési forrás, atomi frame/kurzor projekció; UI-commit előtt nincs ack. Complete/undo valódi natív parancs, tulajdonosi epoch megőrizve; foreground controller csak resumed editorból armolódik. Régi protokollok nem optálnak be automatikusan az új fogyasztóba. | 8 új teszt zöld; az előző 451 unit-eset mind megmaradt. `b6e8158`: a UI callback megjelenítéskori workout/generation/exercise/set célpontot őriz, új set UUID-t kap, ismételt gyakorlat listakulcsa és undo célzása javítva. Az új kapu teljesen zöld. Az új GPS/pihenő kapu lent. Valós integrált telefonos UI-próba még szükséges. |
| Stopper / GPS teljesítés | `b6e8158`: az alsó sorozatstopper a natív frame-ben, explicit pozitív stop natív record/undo, kétoldali pozitív pár szükséges; nulla/reset/pause nem teljesítés. Nyugvó clock nem ír folyamatosan SQLite-t; ownership-retirement szüneteltet. `e164ef3`: GPS set/route kötés és tartós explicit pozitív stop a natív record/undo útján; implicit stop csak részadatot őriz. | 19 új célzott teszt mind zöld, a korábbi 459 unit-eset megmaradt. Az új 17 GPS-próba mind zöld, köztük SQL-hiba/retry, régi callback, engedély/rendszerhely, privát naplóútvonal és közös pause. Integrált telefonos UI, kézi measured OK/undo, opcionális intervallum és teljes időösszesítés még hátra van. |
| Pihenő / gyakorlatváltás | `e164ef3`: valódi skip/±15 mp stabil workout/generation/key célzással, tartós rest-only terv és hibánál rollback; gyakorlatváltás őrzi a pihenőt és a kiválasztást. | 10 új service-próba zöld, tényleges időelszámolás és ownership/frozen védelem. Integrált UI még külön szükséges. |
| Naptár / Coach | A `NavHost` nem tartalmaz külön naptár/Coach útvonalat. A Coach gomb a training útvonalra visz. | Eredeti funkciók átültetése és valós UI-próbája; programképernyő nem helyettesíti őket. |
| Napló teljesítmény | Compose LazyColumn van, de az adatréteg minden edzést részletekkel figyel. A Wear mini-napló az összes részlet betöltése után vesz csak nyolcat. | Korlátozott lekérdezés/lapozás, teljes adathalmazra működő szűrés; 0/200/1500 minta. Nem állítunk igazolt natív gyorsulást. |
| Health | Valós Samsung SDK adapter és explicit foreground frissítés helyreállítva; az onResume munka IO-n fut. Az eredeti felesleges preferenciavisszaírás-gyorsítás átvéve. | Tesztek, Samsung/HC készülékes engedély- és háttérváltási próba; hiányzó adat nem friss siker. |
| Wear | Az eredeti teljes natív launcher/szolgáltatások/protokoll visszakötve; az alternatív távirányítós prototípus nem launcher. A main gyorsított kiadása nem módosított Wear/shared appkódot. | Kerek UI 183/183 sikeres. Watch7, valódi telefon–óra szinkron és offline átadás/visszavétel még készülékes kapu. |
| Nyelvek / UI | Számos Compose szöveg közvetlenül magyar; a privát aktív/napló térkép piros X-et használ. Watch→Phone folytatás vegyes útvonalszakaszainak UI-ja még vizsgálandó. | HU/EN/DE/RO, nagy betű/kis kijelző, elfogadott piros X, használható érintési célok. |

Friss teljes technikai kapu: **`40000812477` / `922b6b7` SUCCESS**, termékkód `e164ef3`.
Letöltött XML: telefon **338/338**, Wear **167/167**; mind a korábbi 478 unit-eset
megmaradt, +27 új próba. Nyers kerek UI **183/183** azonos esetazonosítóval,
aláírt szintetikus upgrade **2/2**, nincs hiba/kihagyás. Letöltött release APK-k
hash/csomag/verzió/signer ellenőrizve; a measured protokoll a telefonos R8 DEX-ben is jelen van.
[Gépi bizonylat](builds/2026-10-23-native-gps-rest.json).

## Ellenőrzési sorrend

1. Fordítás és meglévő JVM tesztek; ugyanazon forrású, eredeti kulcsos APK-pár.
2. Régi tényleges kiadásokra telepítő, adatmegőrzést vizsgáló emulátorkapu.
3. Kritikus edzésindítás/stop/lezárás és szinkron viselkedés helyreállítása,
   célzott hibainjektálás és UI-próbák; az első két kapu újra az érintett forrásra.
4. Többi főképernyő, funkcióegyezés, fordítások és teljesítmény.
5. Élő adatokra csak külön teljes mentés és a releváns kapuk után; fizikai telefon
   és Galaxy Watch7 teszt. Nincs automatikus main merge vagy verzióemelés.
