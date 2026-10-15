# TrainPilot #199 — aktuális main release gate és készülékes ellenőrzés

## Cél
A #191 mentési és szinkronstátusz, #200 pihenőkihagyás, #201 telefonos explicit kilépéskor közös óra/telefon szüneteltetés **azonos main commitból épített** telefonos és Wear APK-val ellenőrizendő. A felhasználó a korábbi P0 #162/#157 kör után jóváhagyta a #199 main integrációját, #191/#200 lezárását és a következő P1 #163 GPS/térkép munkát.

## Release gate átadás
- Telefonos és Wear Release Gate mindig a **pontosan azonos aktuális main HEAD** forrásra mutasson. Külön ellenőrizendő a Quick, Chromium, natív telefon/Wear, körkijelzős UI és teljesítmény kapu.
- Aláírás, package/versionCode (`1.3.4 /2730`), forrás SHA és a kimeneti APK-k SHA-256 értékei legyenek rögzítve az átadáshoz. A régi c503887 APK pár #201-et nem tartalmazza; a 2ce7288 már igen.
- A #199 app-kód korábban sikeres telefonos ([38612855231](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38612855231)) és Wear ([38587157455](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38587157455)) gate-et kapott, de **nem az aktuális main SHA-n**; ezért főági gate szükséges.
- A korábbi, dokumentációs main [38612264974](https://github.com/fuloplevente1998/TrainPilot/actions/runs/38612264974) telefonos gate egy Chromium UI shard `json-restore-lifecycle.cjs` időtúllépése miatt bukott; később a #199 forrásának ugyanazon tesztshardja sikeres. Az eredményt új főági futáson kell igazolni, nem szabad automatikusan zöldnek minősíteni.

## Rövid, valós készülékes ellenőrzés
1. Telefonon induló edzés → órás sorozatrögzítés → mentési/szinkron státusz: helyi nyugta != telefon tartós nyugtája; offline és ismételt küldés után pontosan egy napló.
2. Telefonos kihagyott pihenő → az órán a következő sorozat rögzíthető, nem egy lejárt időzítő `Folytatás` gombja fogadja.
3. Telefonon kifejezett kilépés → órán a Health Services és aktív időmérés szünetel; a vázlat telefonon folytatható, a szünet nem dupláz aktív időt/kalóriát.
4. Órás önálló átvétel/folytatás nem része e release scope-nak (külön #202); a #201 teljes, többperces szünet/újraindítás/késői frame próbáit az Issue lezárása előtt külön igazoljuk.

A napló, mérési források és korábbi programok nem veszhetnek el, verziót és release taget automatikusan nem emelünk.

## #206 – közös TrainPilot 1.3.51 /2732 gate

A [#208 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/208) alapján a felhasználó a GPS-es távolsági gyakorlatok **explicit natív GPS STOP utáni tulajdonoshoz kötött automatikus teljesítését** és a két egymás melletti **GPS-mérés / Beltéri időmérés** gombot **egy közös 1.3.51 /2732** verzióban hagyta jóvá. Az 1.3.5 /2731-ben működő Wear Health Services távolság-/sebességmérés és telefonos szinkron változatlan; a telefonos saját GPS-koordinátákból rajzolt élő/naplótérkép megmarad. Nincs önálló órás GPS nyomvonal állítás (#193 külön).

**Ugyanazon SHA kapu:** az aktuális `feat/206-distance-auto-complete-compact-1351` fejlesztési commiton Quick, Performance, 6/6 Chromium, natív JUnit, telefonos aláírt Release APK, Wear aláírt Release APK és wear round UI teszt sikeressége szükséges. Ez a dokumentációs változtatás szándékosan újraindítja a Wear Release Gate-et és a körkijelzőteszteket, hogy ne egy régi SHA-n készült órás APK-t adjunk a telefonos 1.3.51 mellé. Mindkét APK-nál versionName **1.3.51**, versionCode **2732**, a kézbesítésnél külön egyező forrás-SHA és aláírás ellenőrzendő. Tilos main merge vagy kész kiadásként való átadás valamennyi fenti gate nélkül; a készülékes elfogadás marad a felhasználó tesztje.
