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

## A többi hasonló nézet közös védelme

A felhasználó a többi hasonló ablakra is rákérdezett. A javítás előtti Chromium-próbán az Edzés oldal nyitott edzésbeállításai és edzésnapja is összecsukódtak automatikus frissítéskor. A különálló hét panel és a gyorsedzés-választó korábbi védelme már működött.

A közös háttérfrissítési belépési pont ezért elhalasztja a teljes oldal lecserélését, ha látható nyitott részben olvas vagy szerkeszt a felhasználó. Ez a szokásos `<details>` elemekre és az `aria-expanded` állapottal működő részekre is vonatkozik. A háttéradatok beolvasása és tárolása folytatódik. A naplózott edzés a külön, edzéskulcshoz kötött állapot-visszaállítást használja, így az új mérési értékei nyitott nézetben is megjelennek.

Az alapból nyitott aktívprogram-sáv és az összecsukott szülőben rejtett elemek nem akadályozzák a nyugalmi oldal frissítését. A felhasználó saját műveletei továbbra is azonnal újrarajzolhatják a nézetet.

A böngészős ellenőrzés nyolc teljes oldali esetet fed le: edzésbeállítások, edzésnap, edzésgyakorlat szerkesztője, programkártya, programnap, pulzus/regenerációs előzmény, fókusz nélküli testsúlypiszkozat és statisztikai részlet. Minden esetben megmarad a DOM, a nyitott állapot és a görgetés; a natív projekció beolvasásszáma közben tovább nő. Külön próba ellenőrzi a nyugalmi oldal és a kifejezett felhasználói újrarajzolás működését.

Ehhez új telefonos teszt-APK készül, az órás íves elrendezés az előző ellenőrzött APK-val használható. A készülékes elfogadás továbbra is szükséges.

## További órás képernyők átvizsgálása

A készülékes fotó a félbehagyott edzés törlésének megerősítésénél levágott gombot mutatott. A törlés/befejezés kérdése és az összesítés ezért a szövegtől független, rögzített 48 dp alsó vezérlőket kap. Hiba esetén az újrapróbálás is ebben a rögzített sorban van.

A Főmenü, Mini napló és részletei, Gyors edzés, Programnapok, napi naptárrészlet, Órás mérések, Edzésmenü, súly/ismétlés/idő szerkesztő és stoppereszközök közös íves fejlécet és a fizikai körbe teljes egészében beférő görgetési téglalapot használnak. A cím rögzített, a hosszabb lista vagy nagyobb rendszerbetű görgethető. A teljes cím akadálymentes leírásként is elérhető. A súly/ismétlés szerkesztőben az érték és a csökkentés/növelés gombjai külön sorba kerülnek; a stopper vezérlői két sorra oszlanak. A listás szerkesztőkben a teljes 48 dp vezérlő görgethető a látótérbe, 160 dp kijelzőn is. A listás gombok felirata legfeljebb 135%-os fizikai betűméretet használ, két sorban ellipszissel; a teljes felirat akadálymentes leírásként megmarad.

A főoldali középső szöveg szélessége szintén a kör sarkaihoz igazodik. A meglévő heti naptár külön, hétgombos elrendezését a saját geometriai tesztje ellenőrzi. A korábban javított aktuális sorozatnézet íves címe, alsó íves vezérlői és mérései megmaradnak.

A geometriai JVM-próbák a listák teljes sarkait, a cím és tartalom elkülönítését, a megerősítés/összesítés egy- és kétgombos láblécét, a teljes gombköröket és feliratokat ellenőrzik 160–320 dp kerek és eltérő oldalarányú nézeteken. A tényleges órán a törlés/befejezés, mentési hiba/újrapróba, nagy betűméret, a listák végén lévő visszagombok és a szerkesztők még ellenőrzendők.

A teljes Wear kezdőlap dizájnmegújítása külön #170 fejlesztési pont, a telefonos lenyíló nézetek lassulása külön #171 hiba. Ez a javítás a meglévő órás képernyők hozzáférhetőségét és elrendezését kezeli.
