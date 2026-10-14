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
