# Teljes kerek kijelző — #176

A felhasználó 2026. október 12-i főmenüfotója alapján a kiadott 1.3.4 órás app csak egy középső, rövid sávban mutatta a görgethető listát. A korábbi körbe írt téglalap a 200 dp nézetből kb. 98 dp magasságot és 136 dp szélességet engedett használni. Az edzésnézetet egy további belső kör szűkítette. A régi geometriai próbák ezt az elhelyezési szabályt ellenőrizték; a készülékes fotó megmutatta annak használhatósági hibáját.

## Elrendezés

Minden hosszabb oldal teljes képernyős Wear `ScalingLazyColumn`-t használ. A kör közepe 8 dp oldalsó margót kap; a tartalom magassága a teljes képernyőmagasság. A sorokat a Wear könyvtár a perem közelében méretezi és halványítja. A fejléc csak a felső körperemen kap keskeny hátteret, a lista előtt nincs levágó téglalap.

A gombok, feliratok és mérési sorok külön lazy elemek. A felső/alsó félképernyős tartalmi padding miatt az első és utolsó elemek, illetve hosszabb kártyák végei is a kör közepére görgethetők. Ez tartalmi padding; a görgető nézet továbbra is az egész képernyőt használja. Az alap kezdőpozíció a második elemet középre helyezi, így a lista felső és alsó része is látható.

Az érintett oldalak: főmenü, Mini napló és részletei, gyors edzés/gyakorlatkeresés, programnapok, naptári napi részletek és üres állapot, órás mérések/előzmények, súly/ismétlés/idő szerkesztő, stoppereszközök, edzésmenü, kezdőlap, aktuális sorozat/stopper, törlés/befejezés megerősítése és mentési összesítő/újrapróba. A heti naptár már teljes képernyős saját elrendezése megmarad.

A három 48 dp vezérlő egy külön görgethető íves elem: a középső lejjebb kerül, az érintési felületek legalább 4 dp-re vannak egymástól. A kisebb súly/ismétlés mezők, a pulzus/kcal és az elérhető mozgásmérések megmaradnak. A képernyő teljes tartalma görgethető; a vezérlőknek nincs külön, állandó középső vagy alsó ablaka. A hosszú cím továbbra is az ívben kaphat ellipszist; a teljes felirat az akadálymentes leírásban szerepel. A teljes kezdőlap-revamp külön #170.

## Ellenőrzés

- JVM: teljes magasság és legalább 90% középső körszélesség 160–320 dp és eltérő oldalarányú nézeteken; középre görgetett ív/gombfeliratok körön belül; elkülönített 48 dp célok és kompakt Telefon gomb.
- Kerek Wear OS 30 emulátor: tényleges Compose méretek, érintéses görgetés, főmenü első/utolsó gombjának megnyomása, összes listás útvonal és heti/napi naptár, naplórészletek, három edzéslap, ismétléses és időméréses nézet.
- Emulátor-beállítások: 160, 192, 225 dp; normál, 150% és 200% betűméret. A képernyőképek MediaStore-ba kerülnek, így a teszt-APK automatikus eltávolítása után is átadhatók CI-artifactként.
- A telefonos naptári próbában a rögzített böngészős tesztidő mellett egy dátumfeltétel a Node valós idejét használta. A feltétel most ugyanahhoz a rögzített böngészős időhöz viszonyít; a telefonos alkalmazás kódja nem módosul.

A futásazonosítók és az APK lenyomata a PR-ben szerepelnek. Az aláírt próba továbbra is 1.3.4 /2730, külön fájlnévvel; a korábbi telefonos 1.3.4-gyel kompatibilis. A készülékes elfogadás még szükséges, a nyilvános 1.3.4 APK-k nem kerülnek automatikusan lecserélésre.
