# Natív migráció – funkcióegyezési ellenőrzés

Állapot: 2026-10-24, `dd4cc27` importált draft-átvétel, `9da639d` gyors/kültéri indítás és GPS-választás, `40384e7` kézi measured/interval/időnapló, `922b6b7` CI / `e164ef3` GPS/pihenő/gyakorlatváltás, `b6e8158` stabil UI-célzás/alsó stopper, `9492755` editor/motor és `cab522a` indítás/lezárás alapján. A korábbi teljes technikai zöld alap `134adcd`. Ez forrásellenőrzés, nem készülékes elfogadás.
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
| Adatmigráció | Aktuális WebView localStorage olvasás, helyi archívum, Room-tranzakció, sérült forrásnál nincs kész nyugta. | Unicode/nagy napló, törlés, GPS, fotó, program/nap, testsúly, naptár, beállítás, draft, régi health DB, eredeti tár és idempotencia tényleges upgrade-próbája 2/2 sikeres. `dd4cc27`: draft-preflight és tartós natív átvétel, Home Folytatás, szünetben helyreállítás; forrásarchívum, hash-kötésű lezárás/elvetés és SQL-hiba/retry. 20 új unit zöld, teljes kapu telefon 393/Wear 167, kerek UI 183, signed upgrade 2. A valós telefonos Compose folytatás/mentés/elvetés próba még hiányzik. |
| Telefonos edzésindítás | `cab522a`: tartós programnapi indítás, stabil ID, ismételt start/recovery védett; #228 előtöltés lapozott forrással. `9da639d`: valódi gyors/kültéri könyvtárválasztó és programfüggetlen quick session, közös előtöltés, aktivitásonként látható és megjegyzett GPS-választás. Csak új explicit start ad egyszeri memóriabeli GPS-szándékot; megmarad az aktív/watch/draft védelem. | Új 15 core-próba zöld, mind a korábbi 525 unit-eset megmaradt; friss teljes kapu zöld, telefon 373/Wear 167, kerek UI 183, signed upgrade 2. Az új telefonos Compose indítás tényleges UI-próbája, programnapi kezdeti GPS-választás még hátra van. A draft core-átvétel `dd4cc27`-ben bekötve, UI-próbája külön hiányzik. |
| Lezárás | `cab522a`: UI és Wear közös `TrainPilotWorkoutFinalization`; esemény+kurzor tartós a nyugta előtt; napló+naptár tranzakció, valódi close, timer/GPS stop, takarítás, navigáció csak siker után. Mentési kerítés és stabil retry. | 9 új teszt zöld, köztük valódi SQLite-hibainjektálás, újrapróbálás, késői watch finish, handoff/restore, régi törlés. Tényleges natív UI és két készülékes próbák még szükségesek; az elvetési út külön további vizsgálatot igényel. |
| Telefonos editor–natív motor | `9492755`: a teljes natív frame a tartós szerkesztési forrás, atomi frame/kurzor projekció; UI-commit előtt nincs ack. Complete/undo valódi natív parancs, tulajdonosi epoch megőrizve; foreground controller csak resumed editorból armolódik. Régi protokollok nem optálnak be automatikusan az új fogyasztóba. | 8 új teszt zöld; az előző 451 unit-eset mind megmaradt. `b6e8158`: a UI callback megjelenítéskori workout/generation/exercise/set célpontot őriz, új set UUID-t kap, ismételt gyakorlat listakulcsa és undo célzása javítva. Az új kapu teljesen zöld. Az új GPS/pihenő kapu lent. Valós integrált telefonos UI-próba még szükséges. |
| Stopper / GPS teljesítés | `b6e8158`: az alsó sorozatstopper a natív frame-ben, explicit pozitív stop natív record/undo, kétoldali pozitív pár szükséges; nulla/reset/pause nem teljesítés. Nyugvó clock nem ír folyamatosan SQLite-t; ownership-retirement szüneteltet. `e164ef3`: GPS set/route kötés és tartós explicit pozitív stop a natív record/undo útján; implicit stop csak részadatot őriz. | 19 új célzott teszt mind zöld, a korábbi 459 unit-eset megmaradt. Az új 17 GPS-próba mind zöld, köztük SQL-hiba/retry, régi callback, engedély/rendszerhely, privát naplóútvonal és közös pause. `40384e7`: kézi measured OK/undo, közös megfigyelt időnapló és opcionális automatikus intervallum; 20 új próba zöld, a korábbi 505 eset megmaradt. Integrált telefonos UI, nem-autoAdvance mód és globális preset még hátra van. |
| Pihenő / gyakorlatváltás | `e164ef3`: valódi skip/±15 mp stabil workout/generation/key célzással, tartós rest-only terv és hibánál rollback; gyakorlatváltás őrzi a pihenőt és a kiválasztást. | 10 új service-próba zöld, tényleges időelszámolás és ownership/frozen védelem. Integrált UI még külön szükséges. |
| Időösszesítés | `40384e7`: megfigyelt stopper/GPS és interval munka/pihenő/szünet közös naplóban; kézi idő nem hamisít megfigyelt időt. Folytatás/undo/reset nem dupláz; folyamat/tulajdonos védett. Élő összesítés csak memóriában. | Új célzott tesztek és Room-GPS időnaplóállítások zöldek. Tényleges telefonos megjelenítés/készülékes mérés még külön igazolandó. |
| Naptár / Coach | Új batchben `e05ff57`–`9b09af7`: Room naptár CRUD/bounded nézet, Coach valós helyi HealthJournalStore-adatokkal és célprogramnappal; önálló Calendar/Coach útvonal bekötve. `4f60bc0`: ötfüles navigáció, globális Settings és külön gyorsbelépések. Még nem bevizsgált forrás. | Per-gyakorlat tanácsok, teljes mockup, provider-frissítés, timezone-paritás és integrált UI-próba. Nem új screen-implementáció kell a meglévő helyett. |
| Napló teljesítmény | `f105003`: Wear legfeljebb8 szülő Room-ban; `7d5ac83`: telefon Paging, 32-es összegző lapok, reaktív explicit részlet, globális Unicode-keresés bounded szövegprojekciókon és SQL-hónaphatárok. Új tesztkód megírva, nem futtatva. | Tényleges 0/200/1500 minta emulátoros késleltetés/memória/frame mérés; integrált UI és adatváltozásos regresszió. Nem állítunk igazolt gyorsulást. |
| Health | Valós Samsung SDK adapter és explicit foreground frissítés helyreállítva; az onResume munka IO-n fut. Az eredeti felesleges preferenciavisszaírás-gyorsítás átvéve. | Tesztek, Samsung/HC készülékes engedély- és háttérváltási próba; hiányzó adat nem friss siker. |
| Wear | Az eredeti teljes natív launcher/szolgáltatások/protokoll visszakötve; az alternatív távirányítós prototípus nem launcher. A main gyorsított kiadása nem módosított Wear/shared appkódot. | Kerek UI 183/183 sikeres. Watch7, valódi telefon–óra szinkron és offline átadás/visszavétel még készülékes kapu. |
| Nyelvek / UI | Számos Compose szöveg közvetlenül magyar; a privát aktív/napló térkép piros X-et használ. Watch→Phone folytatás vegyes útvonalszakaszainak UI-ja még vizsgálandó. | HU/EN/DE/RO, nagy betű/kis kijelző, elfogadott piros X, használható érintési célok. |

Friss teljes technikai kapu: **`40011226981` / `40384e7` SUCCESS**.
Letöltött XML: telefon **358/358**, Wear **167/167**; mind a korábbi 505 unit-eset
megmaradt, +20 új próba. Nyers kerek UI **183/183** azonos esetazonosítóval,
aláírt szintetikus upgrade **2/2**, nincs hiba/kihagyás. Letöltött release APK-k
hash/csomag/verzió/signer ellenőrizve; a measured protokoll és az időnaplómezők a telefonos R8 DEX-ben is jelen vannak.
[Gépi bizonylat](builds/2026-10-23-native-measured-intervals.json).

**Aktuális nem bevizsgált batch:** `87b593b`, **505 új JVM +48 Phone UI**
tesztkód megírva, NEM FUTOTT. #24082883ab és #241ab25d84 squash integrálva,
Home nevek/egységek javítva; mind a hat külön UI-próba valódi pickert kattint.
[Integrációs review](reviews/NATIVE_LOCALIZATION_INTEGRATION.md).
Google egyetlen auth→consent→profil→régi sync-mag út bekötve, per-attempt result,
cancel/rotation/process-recreation és restore/erase epoch-védelem kódban, nem
valódi Google-fiókkal igazolva. Privát staging/ZIP-redo/post-acceptance publish,
bounded napló/súly/PR-adatút, Health-részletek és tervezőblokkok kódban; valódi
platform/megőrzés/UX és teljes funkcióegyezés továbbra közös kapu.
[Aktuális átadó](WORK_HANDOFF.md), [rövid checkpoint](NATIVE_RESUME.md).
Exact-ID súlytombstone JSON/ZIP/Drive replay-védelem kódban, nem futott;
cloud fotó privát staging/MD5/lease utáni ZIP-redo install+törlés kódban;
remote/local atomikusság és valódi Google próba még nyitott.
A finalizer streaming scoped PR-adatbemenete (eredeti lezárási tranzakcióval),
a testsúlynapló lapozás és teljes adathalmazos skalár/bounded grafikon bekötése,
valamint a személyes programelőnézet teljes nap/practiced-kivétel, friss elfogadási időhatár
és portable bizonyítékbekötés kódja is elkészült; Kotlin/Room/UI még nem futott.
A régebbi táblázati zöldek történeti bizonyítékok, nem az új kód igazolásai.
Utolsó teljes zöld forrás `dd4cc27`; teljes UI/lokalizáció/katalógus/programszerkesztő,
integrált futtatás, fizikai készülékes elfogadás és a terv többi pontja még nyitott.

## Ellenőrzési sorrend

1. Fordítás és meglévő JVM tesztek; ugyanazon forrású, eredeti kulcsos APK-pár.
2. Régi tényleges kiadásokra telepítő, adatmegőrzést vizsgáló emulátorkapu.
3. Kritikus edzésindítás/stop/lezárás és szinkron viselkedés helyreállítása,
   célzott hibainjektálás és UI-próbák; az első két kapu újra az érintett forrásra.
4. Többi főképernyő, funkcióegyezés, fordítások és teljesítmény.
5. Élő adatokra csak külön teljes mentés és a releváns kapuk után; fizikai telefon
   és Galaxy Watch7 teszt. Nincs automatikus main merge vagy verzióemelés.

## Egészségnapló és mockuplefedettség – 2026-10-24 pontosítás

A felhasználó kifejezetten kéri a teljes egészségnapló/testsúly/pulzus és minden
main-funkció megtartását. A 24 mockup csak főnézet, nem végleges funkciólista.
Forrásellenőrzés: HealthDashboardViewModel jelenleg20 metrikadefiníciót és mai
adatbetöltést tartalmaz, miközben a UI26 metrikát ír. Ez a felirat nem igazolt
lefedettség. HealthJournalStore időszakos/lapozott adatút létezik, de a teljes
historikus metrika-UI bekötése külön feladat. ProgressViewModel testsúlybevitelt,
időszakszűrést és testsúlytörténetet tartalmaz; ez még nem friss futtatott igazolás.
Következő: teljes main-metrikakészlet és historikus útvonal megfeleltetése, a hamis
fix darabszám helyett tényleges lefedettség, részletoldalak/előzmények és tesztek.


Egészség-folytatás **edc8f52**: a korábbi20/26 eltérés kódban rendezve, mind26
provider-kulcsnak van lokalizált megjelenítése. Havi/nap/csatorna választás bekötve,
régi adat nem helyettesíti a hiányzó kiválasztott napot. 7 új unit+1 UI tesztkód
még nem futott; ez nem teljes egészségnapló-paritás. Nyers rekordok/részletek,
eredet- és mérésidő-szűrés, alvásrészletek és minden testsúlyművelet még külön tétel.
