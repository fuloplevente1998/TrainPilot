# P0 #162 → #157 — közös tesztpár és készülékes elfogadás

Frissítve: **2026. október 13.** Közös draft PR: [#195](https://github.com/fuloplevente1998/TrainPilot/pull/195). Forráság: `fix/p0-162-157-safe-wear-sync`. Verzió mindkét appban **1.3.4 /2730**. A main és a nyilvános kiadások nem változnak; #162 és #157 a tényleges telefonos/órás eredményig nyitva marad.

## Tesztpár

A gate-ek befejezése után ide kerül a forráscommit, a telefonos és Wear artifact linkje, az APK-k SHA-256 értéke és a csomag/aláírás ellenőrzésének eredménye. A régebbi, azonos verziószámú APK-k nem azonosítják ezt a P0 forrást.

Mindkét friss APK-t a meglévő telepítés fölé kell telepíteni. Az alkalmazás eltávolítása nem része a próbának. Az órán az Órás mérések legyen bekapcsolva, a korábban kért szenzorengedélyek megadva; másik alkalmazás ne mérjen párhuzamos edzést. A két eszköz legyen kapcsolódva. Sikertelen indításkor a pontos órás/telefonos állapotot is rögzítsük; az app megnyílása önmagában nem bizonyít szenzormérést.

## Rövid próbamenet

| Próba | Teendő | Elfogadás |
| --- | --- | --- |
| #157 gyorsindítás | A lezárt órát hagyd érintetlenül; telefonon indíts új gyors, súlyzós edzést. | Az órás TrainPilot egyszer megnyílik, ugyanaz a gyakorlat/edzés jelenik meg, majd tényleges friss HR/kcal érkezik. A puszta „Mérés indul” nem siker. |
| #162 lezárt telefon | Automata módban két kézi, ismétléses sorozat; az elsőt az órán rögzítsd, miközben a telefon már lezárt. | Rögzítés és pihenő a telefon felélesztése nélkül működik. A pihenő lejár, a következő sorozat készen áll; ismétlést továbbra is te rögzítesz. |
| Hosszú pihenő / rezgés | Válassz 120 mp pihenőt. Figyeld a teljes kört és az utolsó másodperceket. | A kör a teljes 120 mp alatt fogy. A végén három rövid előjelzés és egy 1 mp-es rezgés; ugyanarra a váltásra nincs kettős jelzés. |
| Időalapú automata | Rövid időalapú gyakorlat több sorozattal, utána ismétléses gyakorlat; a telefon kijelzője lezárt. | Munka/pihenő/sorozatléptetés működik, az időalapú kész sorozat nem akadályozza a következő kézi rögzítést. Felnyitáskor nincs eltérő vagy ismételt eredmény. |
| Szünet / kihagyás | Pihenőnél szüneteltesd, várj néhány másodpercet, folytasd; később hagyd ki a maradék pihenőt. | A kézi szünet külön idő; a kihagyott pihenő nem lesz elvégzett pihenő vagy munka. |
| Szerkesztés | Pihenő közben javíts egy előző kész sorozatot. Később egy kézi sorozat végzése közben javíts korábbi adatot. | A pihenő nem számolódik munkának; az aktuális kézi sorozat eltelt ideje nem veszik el. A súly/ismétlés javítása megmarad. |
| Kapcsolatvesztés | Rövid telefon–óra megszakadás, egy órás rögzítés, majd visszakapcsolódás és telefonos megnyitás. | Nincs elveszett vagy kétszer rögzített sorozat; a függő szinkron látható és helyreáll. A telefonos timer kapcsolat nélkül is a saját vezérlőjén fut. |
| Mentés / napló | Telefonon fejezd be és mentsd el az edzést. | Egyetlen naplóbejegyzés, stabil kész sorozatok, helyes mérési forrás. A TrainPilot Wear adatok és a Health Connect adatok elkülönülnek. |
| További irányok | Rövid tervezett edzés, külön önálló órás és önálló telefonos próba. | A meglévő indítási és automata működés megmarad. Telefon nélkül nem követelünk telefonos szinkront. |

A háttérben ténylegesen elvégzett munka-, pihenő- és szünetidő a session külön `automaticTime162` naplójába kerül. Ezt a belső adatot a UI jelenleg nem feltétlenül mutatja külön bontásban; bizonytalan összegnél konkrét edzésazonosítóval/exporttal ellenőrizzük, ne a legacy időalapú `reps` mezőt adjuk hozzá újra. GPS és oldalankénti automata bővítés nem része ennek a körnek.

## Eredményrögzítés

Készülékes eredmény még nincs. A visszajelzés tartalmazza: melyik fenti próba, indítás eszköze, telefon/óra lezárása, mi történt, látható HR/kcal, valamint hogy a naplóban egyetlen edzés és helyes sorozatok maradtak-e. Hibánál a pontos képernyőállapot vagy hibaüzenet és a hozzávetőleges idő segít.

Sikeres CI vagy telepíthető APK nem zárja automatikusan az Issue-kat. Mainre emelés és publikált release a felhasználó későbbi elfogadása szerint történhet.
