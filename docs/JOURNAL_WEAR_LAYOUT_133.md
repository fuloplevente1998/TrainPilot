# Napló háttérfrissítése és órás gyakorlatnézet — 1.3.3

A felhasználó a kibővített 1.3.3 kiadásban két készülékes hibát jelzett:

- A megnyitott naplózott edzés idővel összecsukódik, a Napló listája marad a képernyőn.
- Az órán a gyakorlatnév és az alsó gombok felirata levágódik; a súly/ismétlés mezők túl nagyok. Az alsó három vezérlőt ismét ívesen kéri, a pulzus/kcal megőrzésével.

## Javítás

A natív Health Journal késői inicializálása és automatikus frissítése újrarajzolhatja a telefonos oldalt. A háttérfrissítés megőrzi a naplózott edzés stabil kulcsához tartozó nyitott állapotot, az egészségadat-kártya és a belső részletek állapotát, valamint a látható edzés görgetési helyét. Az újonnan beszúrt bejegyzés nem örökli egy régi bejegyzés állapotát. A mérési értékek továbbra is frissülnek, a befejezetlen szerkesztőt elhalasztott újrarajzolás védi. A lecserélt DOM-elemek késői `toggle` eseménye nem módosíthatja az aktuális nézet állapotát.

Az ismétléses és időméréses órás gyakorlatnézet a meglévő Wear Compose Foundation íves szövegét használja a felső peremnél. A gyakorlatnévre koppintva továbbra is megnyílik az edzésmenü. Az alsó három, 48 dp méretű vezérlő külön pozícióban követi az alsó ívet; a középső lejjebb van. A cím, görgethető középső tartalom, vezérlők és lapjelző egymástól külön kapnak helyet. A súly/ismétlés mezők kisebbek, a gombfeliratok tényleges mért szélességükhöz igazodnak. A hosszú cím az íven belül ellipszist kaphat; a teljes név a menüben és az akadálymentes leírásban elérhető. Nagy betűméret vagy kis kijelző esetén a középső tartalom görgethető, az alsó vezérlők helye rögzített.

Az élő mérés, szinkronizálás és végső összesítés protokollja változatlan. A telefon és az óra látható verziója továbbra is **1.3.3 /2729**; az új próba-APK-k egyedi fájlnevet kapnak.

## Ellenőrzés

- Chromium regresszió: késői natív inicializálás, automatikus import és cooldown, összecsukott Health-kártya, nyitott fotórész, új mérési érték, új szinkronizált bejegyzés, stabil görgetési hely, sorhoz kötött vezérlők, befejezetlen szerkesztő, azonos feladatban nyitás/frissítés, aktív edzésből Naplóba lépés.
- Wear JVM geometriai regresszió: kerek és téglalap alakú nézetek, 160–254 dp; mindhárom teljes kör és gombfelirat a fizikai kijelzőn belül, érintési felületek elkülönítve, cím/tartalom/vezérlők/lapjelző számára külön hely.
- A készülékes elfogadás még szükséges: megnyitott telefonos Napló várakozás és új mérési adat után; órán rövid/hosszú gyakorlatnév, súly/ismétlés és időmérés, pihenő, rögzítés, előző/következő/befejezés, pulzus/kcal és elérhető lépés/távolság.

A korábbi #165 mérésjavítás sikeres készülékes elfogadása megmarad; ez a kijelzési és nézetállapot-javítás külön próba.
