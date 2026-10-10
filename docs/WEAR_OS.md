# TrainPilot Wear OS — működés és állapot

**Aktuális verziócsalád: 1.3.** A telefon és az óra a [#156](https://github.com/fuloplevente1998/TrainPilot/pull/156) jóváhagyott párosított változata alapján a mainen van. Az 1.2 Wear-alapfunkcióit és az 1.3 közös fejlesztéseit az [összevont változáslista](../RELEASE_NOTES.md) foglalja össze.

A telefonról indított órás appmegnyitás az 1.3 javítási sorozatának része a [#160 tesztágon](https://github.com/fuloplevente1998/TrainPilot/pull/160); még nincs a main release-ekben. A felhasználó a megnyílást 2026. október 10-én visszaigazolta. A kijelző kikapcsolása melletti tényleges mérés és a végső naplóadat külön ellenőrzés marad.

## Architektúra és adatok

A telefon Capacitor/WebView/Java alapú, és őrzi a mentett naplót. Az óra natív Kotlin, Compose és Wear Material 3 alkalmazás, minimum API 30. Az alkalmazásazonosító `com.repforge.app`, az órás névtér `com.repforge.app.wear`; a párosított kiadások aláírója egyezik.

| Szerep | Megvalósítás |
| --- | --- |
| Telefonos adatátadás és parancsfeldolgozás | `WearSyncPlugin`, `ActiveWorkoutStore` és a telefon meglévő edzés-/naplófolyamata |
| Órás aktív edzés és kezdőlapi/napló-cache | `WorkoutSnapshotStore`, `WatchHomeStore` |
| Tartós offline parancsok és lezárás | `WearCommandOutbox`, `WearClosureStore` |
| Edzéshez tartozó mérés | `WearHealthService`, `WearHealthStore` |

A Data Layer útvonalai: `/trainpilot/active-workout`, `/trainpilot/watch-home`, `/trainpilot/workout-command/<commandId>`. Az óra a helyi pillanatképből jeleníti meg az adatokat, a módosító parancsok tartós sorból, visszaigazolással jutnak a telefonra.

Egy edzés ugyanazt a stabil azonosítót használja indításkor, offline folytatáskor, befejezéskor és késői méréskor. Ismételt parancs vagy befejezés nem készít új naplóbejegyzést. Másik aktív telefonos edzés/piszkozat nem cserélhető le régi órás paranccsal. Későn érkező indításnál a végső edzésadat is segítheti az offline alkalom helyreállítását; törölt alkalmat a lezárási/törlési állapot nem enged visszahozni.

## Órás funkciók

- Kezdőlapi Indítás/Folytatás, megerősített Törlés, Menü és Telefon megnyitása.
- Programos edzés és valódi Gyors edzés a telefonról kapott beépített/saját gyakorlatkatalógusból; edzés közbeni gyakorlat-hozzáadás.
- Sorozat, ismétlés és terhelés rögzítése, meglévő pihenőidőzítő, befejezés és elvetés.
- Heti mini naptár, Mini napló, Órás mérések és mérési előzmények.
- Tile és számlapi kiegészítő, a megfelelő kezdő-/edzésoldal megnyitásával.

A kezdőlap körkijelzőhöz igazított, görgethető adatmezőkkel és elérhető vezérlőkkel. Az OLED-felület sötét/arany palettát használ. A teljes programszerkesztés, fotókezelés és mentés a telefonon marad.

## Mini naptár

A heti nézet 4+3 körgombot, hónap-/hétjelzést, mai kiemelést és állapotpontokat mutat. A cache-ben elérhető hetek közt vízszintes lapozás használható; napra koppintva program, nap, gyakorlatok száma, időpont és állapot látható.

A meglévő telefonos csomag a mai nap előtti három és az utána következő tíz napot tartalmazza, naponta egy összesített bejegyzéssel. Hiányzó adat nem minősül pihenőnapnak; több napi edzést vagy távoli történeti hetet nem szabad kitalálni. Ezek bővítéséhez új telefonos adatcsomag szükséges.

Tervezett edzés külön Indítás művelettel indul. Meglévő edzés, telefonos piszkozat vagy függő szinkron mellett a védelmek megmaradnak.

## Mini napló és mérési előzmények

A **Főmenü → Mini napló** legfeljebb nyolc különböző, már mentett telefonos edzést mutat. A részletekben a gyakorlatok, teljesített sorozatok/ismétlések/terhelések és külön egészségforrások láthatók. Függő parancs vagy félkész órás edzés nem kerül a mentett listába. A cache offline olvasható.

Az **Órás mérések** a három legutóbbi mentett, TrainPilot Wear-méréssel rendelkező edzést jeleníti meg: név, dátum, időtartam, rövid gyakorlatinformáció és rendelkezésre álló kalória/átlagpulzus. A részleges mérés jelölt, a nulla megmarad, a hiányzó érték üres marad.

Előzményre koppintva a Mini napló meglévő részlete nyílik meg; a Vissza az eredeti mérési nézetre vezet. Ez az edzés közbeni mérési oldalról nyitott előzménynél is érvényes.

A nézet megnyitása és a kézi frissítés `requestHome` parancsot küld. A telefon előtérbe kerülése változatlan tartalomnál is újraküldi a friss pillanatképet; rutinszerű polling nem küldi újra a változatlant. A parancs csak sikeres közzététel után kap visszaigazolást. A küldési idő alapján régebbi adat nem írhat felül új cache-t.

A csomag teljes UTF-8 mérete korlátozott; nem másol fotókat vagy teljes napi egészségnaplót az órára. Aktív program hiánya nem blokkolhatja a mentett edzések átadását. A telefonverzió és naplótámogatási jelzés megkülönbözteti a régi adatcsomagot az üres naplótól.

## Egészségadatok és kijelző kikapcsolása

A TrainPilot Wear a Health Services edzésmérését használja, külön felhasználói engedélyezéssel és szenzorjogosultságokkal. Csak az eszköz által támogatott adatot kéri: például pulzus/átlag/max, összes energia, lépés, időtartam és mozgásnál távolság/sebesség. A GPS külön választható.

A már elindult mérés előtérszolgáltatásban folytatódik a kijelző kikapcsolásakor. Az adatfrissítés nem feltétlenül folyamatos képernyős ütemű; a szolgáltató csoportosítva is adhat mintákat. A működést friss szenzoradattal és végső mentett összegzéssel kell ellenőrizni. Az app megnyílása vagy egy megmaradt értesítés önmagában nem bizonyít mérést.

Újrainduló szolgáltatás a saját edzés típusát és beállításait állítja helyre. A függő lezárás megmarad; a leállítás végső Health Services-frissítést vár, határolt várakozás után jelölt részleges eredménnyel. Meglévő összesítések megőrzendők. Más alkalmazás futó edzésmérését a TrainPilot nem szakíthatja meg. Rendszerszintű kényszerleállítás vagy újraindítás utáni folyamatos mérés nem következik a képernyő kikapcsolása melletti működésből.

| Adatút | Jelentés és megjelenítés |
| --- | --- |
| TrainPilot Wear (`healthWear129`) | A pontos edzésazonosítóhoz kapcsolt saját Health Services-mérés; érvényes adat esetén a telefon Naplójának kiemelt alapforrása. |
| Health Connect / Samsung Health (`health240`) | A kiválasztott szolgáltató időablakos jelentése, külön aktív, összes és edzéshez kötött energiával. |
| Edzés saját Wear-mérés nélkül | A telefon a kiválasztott szolgáltatóból olvas, ha megosztott adat rendelkezésre áll; közvetlen Wear-mérést nem állít elő utólag. |

Ugyanaz az óra különböző alkalmazásoknak eltérő edzésidőszakot és energiaösszesítést adhat. A Samsung Health saját adata és a Health Connectbe átadott rekord nem garantáltan azonos időben érkezik meg. A három adatút nem összeadható kalóriaforrás.

Ha az óra nincs a kézen, szenzormérés nem feltételezhető. Ha a kézen van, de a TrainPilot nem indított mérési munkamenetet, más alkalmazás megosztott adata még elérhető lehet. Hiányzó pulzus, HRV, ECG vagy vérnyomás nem becsülhető; ez nem egész napos passzív mérést végző alkalmazás.

A 20 guggolásos készülékpróba helyes órás adatátadást igazolt. A korábbi **4 órás kcal / 1 aktív és 2 összes telefonos kcal** eltérés továbbra is forrás-, időablak- és szinkronellenőrzést igényel. A már naplózott értékeket és késői frissítéseket meg kell őrizni.

## Telefonos órás appindítás — 1.3 tesztjavítás

A #160 javítás célja, hogy új telefonos edzéshez az órát ne kelljen előbb kézzel megnyitni.

Új programos vagy gyors edzés indításakor a telefon előbb közzéteszi az aktív pillanatképet, majd az AndroidX `RemoteActivityHelper` segítségével megnyitja a csatlakoztatott órás Activity-t. A handoff útvonala: `trainpilot://wear/start-measurement?workoutId=...&revision=...`. Az óra a megfelelő azonosító/frissítés beérkezése után indítja a meglévő mérést, ha a kapcsoló és jogosultságok engedik.

A kezdeti kijelzőébrenlét és a függő indítás határolt. Telefonos háttérbe kerülés, edzésváltás vagy lejárat megszakítja az indítási kérést. Visszaállított piszkozat, rutinszinkron és óráról indított parancs nem nyitja újra az órás appot. Több csatlakoztatott óra esetén nincs párhuzamos automatikus indítás. Sikertelen megnyitás után a telefonos edzés megmarad és kézi órás megnyitás használható.

**Állapot:** a telefonos megnyitást a felhasználó visszaigazolta; mindkét aláírt build ellenőrzése sikeres. A PR még draft, a háttérben keletkező mérés és a lezárt naplóadat készülékes elfogadása hátravan. A main release-jei még a javítás előtti állapotot tartalmazzák.

## Készülékes ellenőrzés

| Próba | Ellenőrizendő eredmény |
| --- | --- |
| Telefonos programos/gyors indítás, óra érintése nélkül | Appmegnyílás, a megfelelő edzés új mérése, friss pulzus/kalória és mérési értesítés. |
| Kijelző kikapcsolása | Mérési adat tovább keletkezik; befejezéskor nem csak régi érték érkezik. |
| Befejezés telefonon és órán | Egy mentett edzés, hozzá kapcsolt végső órás összegzés a telefonon és a Mini naplóban. |
| Mérés kikapcsolva vagy engedély elutasítva | Nem indul jogosulatlan mérés; a telefonos edzés használható marad. |
| Kapcsolatvesztés és újracsatlakozás | Offline sor megmarad, ismétlés nem duplikál, késői mérés a megfelelő edzéshez kerül. |
| Mini napló/előzmény/részlet/Vissza | Friss lista, helyes források, eredeti nézetre visszatérés és offline cache. |
| Körkijelző és nagy betűméret | Olvasható adatok, görgethető részletek és elérhető műveletek. |
| Saját Wear-mérés nélküli telefonos edzés | Csak a választott szolgáltató ténylegesen elérhető adata látszik. |

## Build és későbbi bővítések

A Wear modul saját natív teszt- és aláírt kiadási kaput használ. A kizárólag órás változás nem feltétlenül igényel új telefonos APK-t; adatátadási protokoll változásakor mindkét app ellenőrzése és megfelelő párja kell. A dokumentációban összevont verziócsalád nem módosítja az APK-k technikai verzióját.

További bővítés a részletesebb történeti naptár és több esemény egy napon. Ezekhez a telefonos pillanatképet is bővíteni kell; a jelenlegi cache korlátait nem szabad több adatként bemutatni.

[Modul és helyi parancsok](../android/wear/README.md) · [Build és signing](BUILD.md) · [Aktuális fejlesztési állapot](../DEVELOPMENT_STATUS.md) · [Korábbi részletes feljegyzés](https://github.com/fuloplevente1998/TrainPilot/blob/2fdcf4fde7021fb234bd85fff0a59b698d36c808/docs/WEAR_OS.md).
