# Logós Android-indítás (#140)

A launcher és az alkalmazás első képernyője közötti üres szürke/fekete felületet a meglévő arany súlyzólogó váltja fel. A natív és a webes logó ugyanazokat a launcher-útvonalakat, színeket és középpontot használja; az alkalmazásikon nem változik.

- Android 12-től a rendszer splash képernyőjének vektora 280 ms alatt nyílik meg: a két súlyzóoldal és a középrész finoman összeér. Android 7–11-en a kompatibilitási képernyő statikus logót mutat, ugyanazzal a rövid kilépési áttűnéssel.
- Az `AppFeedback.startupReady` jelzés az első végleges helyi DOM renderelése és a következő render-frame után oldja fel a natív splash-t. A kilépés 180 ms-os opacity/scale átmenet; nincs mesterséges minimum betöltési idő, hálózati feltétel vagy ismétlődő animáció.
- Az Android animációskála kikapcsolásakor, illetve a böngésző `prefers-reduced-motion` beállításánál az átmenet azonnali. Háttérből visszatérés és egyszerű nézetváltás nem indít új splash-t.
- Indítási hibánál a natív réteg azonnal elengedi az elérhető Újrapróbálás képernyőt. A hiányzó/hibás app.js külön korai hibajelzést kap. Ha a bridge nem tud jelzést küldeni, 8 másodperces biztonsági maximum engedi megjelenni a webes indítási/hiba-felületet; ez nem minimális várakozás. Activity-megszűnéskor a watchdog törlődik.
- A webes SVG és a kritikus stílus az index.html-ben van, ezért a logóhoz nincs külön kép-/hálózati kérés. A webes áttűnés nem késlelteti a Boot.finished jelzést vagy a felhőműveletek elhalasztását.

A legelső ikonból nyíló launcher-animációt Android és a gyártói launcher vezérli. A böngészős mozgó előnézet az app betöltési logóját és a tényleges Kezdőoldalra való áttűnést mutatja; a videóban 200 ms-os, kizárólag előnézeti forráskézbesítés szemlélteti a telefon rövid helyi betöltését. A kész alkalmazásban ez a késleltetés nincs jelen.

Ellenőrzés: 120 funkcionális regresszió; új Chromium-teszt 320/393/412 px-en, natív első-render jelzéssel, reduced motionnel, nézetváltás/visszatérés és adatok megőrzésével, hiányzó runtime-mal és valódi újrapróbálással. A GitHub gate az Android-erőforrásokat és Java-kódot is lefordítja, futtatja a JVM-teszteket. A telefonos launcher/splash összhatás külön készülékes elfogadással igazolható.

## Második telefonos változat — külön rajzolt ikonréteg (#141 r2)

A 2026-10-07 09:05-ös telefonos felvétel az első #141 buildet mutatja. Képkockánként a 280 ms-os logómegnyílás látható; a hibás kilépésnél a háttér már a Kezdőoldalra áttűnik, miközben az ikon még fedően rajta marad, és az eltávolításkor hirtelen eltűnik.

Android 12-től az animált splash ikon külön SurfaceView/kompozitorrétegen is futhat. A szülő View halványítása önmagában nem kezeli ezt, és a SurfaceView.setAlpha a korábbi Android-verziókon figyelmen kívül maradhat. Az r2 egyetlen időzítéssel halványítja a hátteret és az ikont, külön SurfaceControl alpha-frissítéssel, ha az ikon ilyen felületen van. A kilépési idő 180 ms marad; a logómegnyílás és a tényleges készenléti feltétel nem változik. Megszűnő Activity vagy már levált felület esetén az animáció és az eltávolítás egyszer szabadítja fel a natív réteget.

Új Android/JVM-regresszió ellenőrzi a két alpha-érték együtt változását, az eltávolítást, valamint a csatlakozatlan SurfaceView és a megszakított Activity kilépésének felszabadítását. A tényleges kompozitoros áttűnés végső ellenőrzése az új telefonos teszt.
