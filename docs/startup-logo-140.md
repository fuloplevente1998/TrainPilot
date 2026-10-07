# Folyamatos logónagyítás Android-indításkor (#140, #141 r3)

Az ikon megérintése után az app saját indítási felületén a meglévő arany súlyzólogó kicsiből folyamatosan nő. A helyi Kezdőoldal elkészülésekor a nagyítás az aktuális mérettől folytatódik: 480 ms alatt a logó túlnő a teljes képernyőn, az átmenet utolsó 45%-ában a háttérrel együtt áttűnik a Kezdőoldalba. A súlyzó részei együtt mozognak, nincs összeállítás, méret-visszaugrás vagy ismétlődő animáció.

## Natív megvalósítás

A rendszer splash csak az első natív képkockáig marad. A `MainActivity` a WebView létrehozásakor saját, teljes ablakos `LogoView` réteget tesz a tartalom fölé. A rendszer kilépési visszahívása átadja az ikon képernyőbeli középpontját, majd eltávolítja a rendszer splash-t. A saját réteg az eredeti launcher-vektort rajzolja: a kezdeti nagyítás 0,55, ugyanúgy, mint a rendszer kezdőikonján.

A növekedés a helyi betöltés idejétől függ: `0,55 + 2,45 × elapsed / (elapsed + 2200)`. Hosszabb WebView-indulásnál is folytatódik, nincs rögzített végméretnél álló betöltőlogó. Az első végleges DOM-render és a következő render-frame után az `AppFeedback.startupReady` elindítja a képernyőt kitöltő kilépési zoomot az aktuális méretről. A célméret a teljes ablak hosszabb oldalából számolódik, álló és fekvő nézetben is. Egy réteg alpha értéke halványítja a logót és a hátteret együtt; nincs külön SurfaceView, rendszerikon-kivágás vagy SurfaceControl-kezelés. A vektor egyszer, legfeljebb 2048 px-es képpé alakul; a képkockák csak ezt az egy textúrát méretezik. A réteg eltávolításakor a bitmap referencia felszabadul, nincs képkockánkénti vektorraszterizálás.

Az Android/OEM launcher legelső ikonból nyíló animációját továbbra is a rendszer vezérli. Az app a saját első natív képkockájától vezérli a folyamatos mozgást; a natív folyamat létrejötte előtti rendszerképkockák időzítését nem módosítja.

## Működés és hibakezelés

- Nincs mesterséges minimum betöltési idő vagy hálózati várakozás. A 480 ms-os átmenet már elkészült helyi képernyő fölött fut; a felhőműveletek továbbra is elhalasztva indulnak.
- Android animációskála kikapcsolásakor és webes `prefers-reduced-motion` mellett nincs nagyítás vagy áttűnés. Visszatérés és appon belüli nézetváltás nem ismétli az indítást.
- Hibás/hiányzó app.js esetén a natív réteg azonnal elengedi az elérhető Újrapróbálás felületet. A bridge készenléti jelzésének elmaradásakor a 8 másodperces natív maximum elengedi a webes boot/hiba-felületet. Activity-megszűnéskor megszűnik a rajzolás és az animáció, az eltávolítás egyszer történik.
- A webes inline SVG ugyanennek a teljes logót nagyító mozgásnak az előnézete. A hosszú betöltés CSS-kulcsképei közelítik a natív időfüggvényt; a kilépés méretezése, exponenciális zoomja és alpha görbéje megegyezik a natívval. A service worker új r3 cache-t használ.
- Az r3 böngészős videó 2 másodperces, kizárólag előnézeti app.js kézbesítéssel szemlélteti a felvételen látott helyi betöltést. Ez a késleltetés nincs a kész appban. A launcher és a valódi WebView indítás végső ellenőrzése a telefonos APK-teszt.

## Ellenőrzés

120 funkcionális regresszió és 4 UI-runner teszt; a 73 elemes böngészős gate része az indítás 320/393/412 px-en, a növekvő méret, a készenléti határon megmaradó méret, a képernyőt meghaladó végméret és áttűnés, natív első-render jelzés, reduced motion, visszatérés/adatmegőrzés, valamint hibás runtime és valódi újrapróbálás. Android/JVM-regresszió ellenőrzi az elhúzódó betöltés alatti növekedést, az álló/fekvő képernyő kitöltését, a méretfolytonosságot és a megszakított animáció egyszeri felszabadítását. A CI lefordítja az Android-erőforrásokat és futtatja a natív teszteket az aláírt APK előtt.

Az r1 kis összeállítási mozgása és az r2 külön rendszerikon-rétegre adott halványítása helyett az r3 a 2026-10-07 09:50-es felvételhez tartozó pontosított igényt valósítja meg: egyetlen, nagyra növő logó tűnik át a Kezdőoldalba. A #141 draft és a #140 nyitott marad a telefonos elfogadásig; a main és a v1.2.8 release nem változik.
