# Folyamatos logónagyítás Android-indításkor (#140, #141 r4)

Az ikon megérintése után az app saját indítási felületén a meglévő arany súlyzólogó kicsiből folyamatosan nő. A helyi Kezdőoldal tényleges WebView-képkockájának elkészülésekor a nagyítás az aktuális mérettől folytatódik: 480 ms alatt a logó túlnő a teljes képernyőn, az átmenet utolsó 45%-ában a háttérrel együtt áttűnik a Kezdőoldalba. A súlyzó részei együtt mozognak, nincs összeállítás, méret-visszaugrás vagy ismétlődő animáció.

## Natív megvalósítás

A rendszer splash csak az első natív képkockáig marad. A `MainActivity` a WebView létrehozásakor saját, teljes ablakos `LogoView` réteget tesz a tartalom fölé. A rendszer kilépési visszahívása átadja az ikon képernyőbeli középpontját, majd eltávolítja a rendszer splash-t. A saját réteg az eredeti launcher-vektort rajzolja: a kezdeti nagyítás 0,55, ugyanúgy, mint a rendszer kezdőikonján.

A növekedés a helyi betöltés idejétől függ: `0,55 + 2,45 × elapsed / (elapsed + 2200)`. Hosszabb WebView-indulásnál is folytatódik, nincs rögzített végméretnél álló betöltőlogó. Az első végleges DOM-render és a következő render-frame után az `AppFeedback.startupReady` a natív `WebView.postVisualStateCallback` jelzését kéri. A kilépési zoom az aktuális méretről csak a már elkészült WebView-képkocka után indul. A célméret a teljes ablak hosszabb oldalából számolódik, álló és fekvő nézetben is. A natív View alpha értéke végig 1; a bitmap és a hátteret kitöltő, egymást nem fedő téglalapok Paint alpha értéke halványul együtt. Az egyszeri logótextúra kitöltött, fedő hátteret is tartalmaz a vektor áttetsző helyein, így minden pixel egyszer kap halványítást. Nincs teljes ablakos köztes alpha-réteg, külön SurfaceView, rendszerikon-kivágás vagy SurfaceControl-kezelés. A vektor egyszer, legfeljebb 2048 px-es képpé alakul; a képkockák csak ezt az egy textúrát méretezik. A réteg eltávolításakor a bitmap referencia felszabadul, nincs képkockánkénti vektorraszterizálás.

Az Android/OEM launcher legelső ikonból nyíló animációját továbbra is a rendszer vezérli. Az app a saját első natív képkockájától vezérli a folyamatos mozgást; a natív folyamat létrejötte előtti rendszerképkockák időzítését nem módosítja.

## Működés és hibakezelés

- Nincs mesterséges minimum betöltési idő vagy hálózati várakozás. A 480 ms-os átmenet már elkészült helyi képernyő fölött fut; a felhőműveletek a natív átmenet befejezése után indulnak. A plugin Promise-ja csak a fedőréteg eltávolítása után teljesül; hiba, watchdog, Activity-megszűnés és kikapcsolt animáció esetén is elengedi a hívót.
- Android animációskála kikapcsolásakor és webes `prefers-reduced-motion` mellett nincs nagyítás vagy áttűnés. Visszatérés és appon belüli nézetváltás nem ismétli az indítást.
- Hibás/hiányzó app.js esetén a natív réteg azonnal elengedi az elérhető Újrapróbálás felületet. A bridge készenléti jelzésének elmaradásakor a 8 másodperces natív maximum elengedi a webes boot/hiba-felületet. Activity-megszűnéskor megszűnik a rajzolás és az animáció, az eltávolítás egyszer történik.
- A webes inline SVG ugyanennek a teljes logót nagyító mozgásnak az előnézete. A hosszú betöltés CSS-kulcsképei közelítik a natív időfüggvényt; a kilépés méretezése, exponenciális zoomja és alpha görbéje megegyezik a natívval. A service worker új r4 cache-t használ.
- Az r3 böngészős videó 2 másodperces, kizárólag előnézeti app.js kézbesítéssel szemlélteti a felvételen látott helyi betöltést. Ez a késleltetés nincs a kész appban. A launcher és a valódi WebView indítás végső ellenőrzése a telefonos APK-teszt.

## Ellenőrzés

120 funkcionális regresszió és 4 UI-runner teszt; a 73 elemes böngészős gate része az indítás 320/393/412 px-en, a növekvő méret, a készenléti határon megmaradó méret, a képernyőt meghaladó végméret és áttűnés, natív első-render jelzés, reduced motion, visszatérés/adatmegőrzés, valamint hibás runtime és valódi újrapróbálás. Android/JVM-regresszió ellenőrzi az elhúzódó betöltés alatti növekedést, az álló/fekvő képernyő kitöltését, a méretfolytonosságot, az egységes pixel-alpha értéket natív Skia-rajzolással, a fedés nélküli rajzolást, az egyszeri raszterizálást és a megszakított animáció egyszeri felszabadítását. A WebView-frame visszahívás tesztje valódi kész képkockáig nem indít zoomot, a késői/ismételt callback és a levált WebView kezelését is ellenőrzi. A böngészős natív bridge-regresszió a felhős státuszlekérést az átmenet végéig visszatartja, majd pontosan egyszer indítja. A CI lefordítja az Android-erőforrásokat és futtatja a natív teszteket az aláírt APK előtt.

Az r1 kis összeállítási mozgása és az r2 külön rendszerikon-rétegre adott halványítása helyett az r3 a 2026-10-07 09:50-es felvételhez tartozó pontosított igényt valósítja meg: egyetlen, nagyra növő logó tűnik át a Kezdőoldalba. A #141 draft és a #140 nyitott marad a telefonos elfogadásig; a main és a v1.2.8 release nem változik.


## R4: a végső áttűnés akadásának javítása

A 2026-10-07 10:28:59-es telefonos felvételen az r3 nagyítása már megfelel az igénynek, de a halványulás elején a kép közel 0,3 másodpercre megáll, majd a Kezdőoldal hirtelen megjelenik. A felvétel kb. 24 fps-es és egyenletes képkockaidőket tartalmaz; önmagában nem GPU-/főszálprofil.

Az r4 a kódban látható két költséges átadási pontot kezeli: megszünteti a View-szintű alpha miatt a halványítás kezdetekor létrejövő teljes ablakos köztes rajzréteget, és DOM-készültség helyett a WebView már elkészült képkockájához köti a zoomot. A felhős indítás csak a fedőréteg eltávolítása után kezdődik. A jóváhagyott növekedési görbe, méret, 480 ms és utolsó 45%-os halványítás megmarad. A készülékes folyamatosságot az új APK telefonos tesztje igazolhatja; a böngészős előnézet nem méri az Android GPU-terhelését.


## Elfogadás — 2026-10-07

A felhasználó az r6 tesztet enyhe megmaradt akadás mellett elfogadta mainre emelésre. Kiadás: 1.2.8-rc1 /2712, TRACE=false. Az enyhe akadás ismert korlát; a teljes folyamatosság nem bizonyított.
