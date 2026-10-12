# Wear OS kezdőlap — #170 látványterv

![Kezdőlap edzés előtt és edzés közben](mockups/home-revamp-170.png)

Ez generált látványterv, nem a futó alkalmazás képernyőképe. A számok mintaadatok. A felhasználó sűrű, kör alakú saját számlapját adta referenciának, majd kifejezetten a telefonos appal megegyező ikonokat kért.

## Vizuális irány

- Fekete kijelző, nagy központi adat, finom íves oldalsó feliratok és szegmentált állapotjelző.
- Edzés előtt dátum/idő, következő edzés neve, program és időpont, gyakorlatszám, adatfrissítés ideje.
- Edzés közben eltelt idő, gyakorlat neve, sorozatállapot és előrehaladás, az adott edzéshez tartozó pulzus/kcal, valamint elérhető mozgásadatok.
- Három alsó körgomb: Napló, Indítás/Folytatás, Beállítások/Órás mérések. Külön Telefon és Menü elérés ívesen; az oldalra húzás továbbra is menüt nyit.
- Óra-akkumulátor és mérési állapot a peremen. A kapcsolat és a helyben elérhető programadat külön kezelendő; a korábbi pillanatkép nem bizonyít élő telefonkapcsolatot.

## Az ikonok forrása

A végleges implementáció a `www/index.html` `icons` objektumának eredeti SVG geometriáját használja, nem emojit vagy újrarajzolt alternatív ikonokat:

| Wear cél | Telefonos kulcs |
| --- | --- |
| Edzés indítása/folytatása | `plan` — ferde súlyzó |
| Napló | `history` — spirálos jegyzetfüzet |
| Beállítások | `settings` — fogaskerék |
| Pulzus/mérés | `health` — szív |
| Naptár | `calendar` |

A generált kép ezeket csak közelíti; az alkalmazásban az eredeti SVG-k kerülnek natív vektoros megjelenítésre.

## Megvalósítási korlátok

A mért értékek kizárólag az aktuális edzés azonosítójához illesztve jelenhetnek meg. Mérés nélküli kezdőlapon nincs kitalált pulzus, kcal vagy lépésszám. A szegmentált sor a tervezett/rögzített gyakorlatok vagy sorozatok száma, nem kitalált egészségügyi grafikon. Az akkumulátorérték az óra rendszeréből, a kapcsolatjelzés a tényleges Data Layer kapcsolatból származik majd.

A kisebb kijelzőn és nagyobb rendszerbetűn a tartalom sűrűségét csökkenteni kell; a vezérlők és feliratok maradjanak a körön belül, elkülönített érintési területekkel. A #177-ben elfogadott szerkesztő és többi rövid oldal elrendezése megmarad. A natív megvalósítást kerek kijelzős képernyőképekkel és funkcionális útvonalpróbákkal kell ellenőrizni.
