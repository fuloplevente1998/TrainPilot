# Natív migráció – véglegesítési státusz

Frissítve: **2026-10-24**. Munkaág: `feat/native-android-finalization`.
Legutóbbi teljes kapu a `40123422503` (`378e3cd`): a telefonos/Wear debug build,
natív unit suite, signed update és aláírás-folytonosság sikeres; a Wear UI és a
régi kiadásról adatmegőrző upgrade is zöld. A telefonos Compose UI emulátor
`adb: device offline` hibája miatt nem kezdte el az instrumentációt. Az aktuális
appforrás `68d667a`-val már tartalmazza a testsúlynaplót és az új Health
részletezőt; ezen a SHA-n teljes CI még nem futott.
A
`main` (`8a36502`, alkalmazásbaseline `7473b30`) érintetlen; az
egyirányú forráság aktuális HEAD-je `f494383`. Az upstream `e3e79d3`
súlynapló-funkciója `116a9ab`-ként, a `606b8c0` Health-részletezője
`68d667a`-ként került át.
A `36462aa`, `c941413`, `04dba76` és `a026f03` újratervezett képtáblákat, teljes
main-funkcióleltárt és összevont natív elhelyezési tervet tartalmaz; az A01–E20
lista **81 megőrzési követelmény**, nem kész funkció- vagy tesztállapot.
Az `033386e` katalógus/migráció tartalma a finalization ágon már megjelent
`8ead84a`-ként. A `edc8f52` 26 metrikakulcsos, dátum-/csatornaválasztós
Egészségnaplója és az `1e4783b` BLE-csatorna javítása ellenőrzötten
cherry-pickelve `b7a82ef`/`a0e7393` lett. A Coach-advice és a korábbi
edzés-végrehajtási javítások kódja már egyezik; a `dee1199` hozzáadta a
Coach-cél lokalizált programnevét a Kezdőlap ajánlásához. A pontos
`git diff HEAD origin/feat/native-android-migration` felülvizsgálata után a
finalization ág `87bb0ae`, `43598b2` és `ec2c65e` Wear-/tesztjavításait
megőriztük; az upstream eltérő Wear-fájlverziójának vak átvétele ezeket
visszavonná. A `main` Capacitor/WebView felülete és Node/npm manifesztje nincs
átemelve; a cél a natív Compose.

## Státuszjelölések és bizonyíték

- **KÉSZ** – a konkrét, megnevezett viselkedésre van futtatott tesztbizonyíték.
- **RÉSZBEN KÉSZ** – működő részek mellett hiányzó paritás vagy integrált út maradt.
- **HIÁNYZIK** – a main megfelelő működéséhez nincs natív megfelelő.
- **ELLENŐRIZENDŐ** – forrás/próbakód létezik, de a jelenlegi SHA-n nem futott le.

Legutóbbi korábbi teljes natív Actions-kapu: [40072751592](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40072751592),
**SUCCESS, 6/6 job** a `dd4cc27` commiton: telefon **393/393**, Wear **167/167**,
kerek Wear UI **183/183**, aláírt, szintetikus upgrade **2/2**. Ez nem igazolja
az azóta hozzáadott kódot. A `43598b2`-n futott
[40118525270](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40118525270):
telefon/Wear debug APK-k sikeresen fordultak, az app unit suite **567 tesztből
2-t** elbukott (`NativeMetadataGateTest` virtuális időzítésű IO-várakozása;
`NativeWorkoutDiscardTest` elavult execution revision miatt elutasított rekordja).
A két teszt célzott javítása `ec2c65e`. A
[40119184559](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40119184559)
futás debug buildje és telefonos unit suite-ja sikeres lett, de a következő
push után a futás **cancelled** állapotba került, mielőtt az aláírt update/
verziókapu befejeződött volna. Az új, katalógus-integrációt is tartalmazó
`8ead84a` CI-futása:
[40119749853](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40119749853)
**failure** lett. Az SDK-telepítés sikerült, de a telefonos Kotlin-fordítás
`BackupRestoreManager.kt:542` sorban elbukott: a seedelő hívás egy nem elérhető
`context` nevet használt. Ez forráskódhiba, nem Windows TLS/SDK környezeti hiba.
A javítás `appContext`-re vált; az új UI- és palettabatch-szel együtt még nincs
CI-vel igazolva. A teszt-, aláírt/update- és UI-kapuk, illetve az APK-pár továbbra
nyitottak.
Fizikai telefonos/Watch7-próba sem történt.

A [40122692736](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40122692736)
futás a `:app:mergeDebugResources` lépésén bukott el, mert az angol
`home_today_status` és `home_recommendation_error` nyers ASCII aposztrófja
hibás AAPT2 escape-szekvenciát adott. A két értéket `\'`-re javítottuk. Ezt a
[40123033007](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40123033007)
futás igazolta: az angol erőforrások fordultak, és a telefonos, illetve Wear
debug APK-k is elkészültek. A futás ezután a
`:app:compileDebugAndroidTestKotlin` lépésben bukott el, mert a
`NativeHomeScreenTest.kt` nem feloldható `assertDoesNotExist` importot adott.
Az importot eltávolítottuk; a szomszédos `NativePhoneNavigationTest` ugyanezt az
assertiont import nélkül használja. A tesztfordítás és minden ezt követő
unit/UI, signed-upgrade és APK-artifact kapu még nincs igazolva. Mindkét hiba
forráskódbeli volt; TLS-ellenőrzést nem lazítottunk.

A [40123422503](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40123422503)
futás `378e3cd` forrásán a phone/Wear debug build, natív unit suite, meglévő
kulccsal készített update-ek és a csomag-/aláírás-folytonossági ellenőrzések
sikeresek; a 3 Wear UI és 2 régi kiadásról natív adatra upgrade-emulátorkapu is
sikeres. A phone Compose UI job infrahibával bukott: az emulator
`adb: device offline` maradt, install/instrumentation nem indult és screenshot
sem készült. A tesztet a jelenlegi forráson újra kell próbálni. Ez a run nem
tartalmazza sem az `116a9ab` súlynapló-, sem a `68d667a` Health-részletező
bekötést; a jelenlegi forráson teljes CI szükséges.

| Main-funkció / natív terület | Státusz | Meglévő bizonyíték és fennmaradó rés |
| --- | --- | --- |
| Kezdőlap, ajánlott programnap és főoldali gyorselérések | **ELLENŐRIZENDŐ** | Az új kezdőlap az aktív/importált állapotot, a kanonikus `NativeCoachTargets` szerinti ajánlott napot, az indítási útvonalat és Programok/Naptár/Coach gyorselérést köti be; a Compose-próbakód megírva, de ezen a forráson nem futott. |
| Edzéstervek, gyakorlatok, sorozatok, előzmény, RIR | **RÉSZBEN KÉSZ** | Natív terv-/sorozatmotor, Room, editor és lapozott napló létezik. `8ead84a` a rögzített main-katalógus 106 gyakorlatát/13 programját hiánypótló seedeléssel vezeti be; a test suite még nem futott ezen a forráson. Teljes editor-, Coach- és RIR-paritás továbbra nyitott. |
| Aktív és gyors edzés | **RÉSZBEN KÉSZ** | Programnapi és quick/outdoor indítás, stabil sorozatazonosítók, natív editor és lezárás létezik. A draft/indítás/mentés/elvetés teljes Compose útja még nem kapott készülékes próbát. |
| GPS-táv, útvonal, térkép | **RÉSZBEN KÉSZ** | `922b6b7`/`e164ef3` GPS-kötési és explicit stop-regressziói CI-ben zöldek; a valós helyengedély-, szenzor-, térkép- és útvonalviselkedés készüléken nyitott. |
| Beltéri, GPS nélküli időmérés | **KÉSZ** | A `40384e7` kapu 20 célzott tesztet és a telefonos teljes unit-csomagot igazolta. Az új intervallummód ezzel nem tekinthető igazoltnak. |
| Automatikus rögzítés és intervallumidőzítés | **ELLENŐRIZENDŐ** | Korábbi stopper/GPS record/undo viselkedés tesztelt; a `9ea2ddf` single-set módja és hordozható presetje új, még nem futtatott tesztekkel érkezett. |
| Pihenő, egyedi beállítások, tartós mentés | **RÉSZBEN KÉSZ** | Skip/±15 mp és Room rollback tesztelve; a friss globális timer-default/preset és importált draft paritása nyitott. |
| Health Connect, Samsung Health és egészségnapló | **ELLENŐRIZENDŐ** | A `b7a82ef`/`a0e7393` napi napló mellé `68d667a` olvasható, nap/csatorna/típus/eredet szerint SQL-ben szűrt, kurzoros 25-ös egyedi méréslapot, valamint alvásfázis- és mintarészleteket ad. Nincs aggregátumból gyártott rekord vagy összeadott forrásadat; a restore utáni régi kurzor tiltott. A 16 unit + 3 UI próbakód érkezett, de a jelenlegi SHA-n még nem futott. Provider-engedély/háttérváltás, forráspreferencia és fizikai próba nyitott. |
| Telefon–Wear szinkron és óráról vezérelt edzés | **RÉSZBEN KÉSZ** | A natív ownership/command út és unit tesztjei léteznek; offline tulajdonosváltás, két eszközös adatkonfliktus és Watch7 próba még nyitott. |
| Wear szenzorok, GPS, értesítés, komplikáció, Tile | **ELLENŐRIZENDŐ** | A natív Wear komponensek visszaállítva, a kerek UI 183/183 korábban zöld. Ez nem bizonyít valós szenzor-, GPS-, Tile- vagy értesítési működést. |
| Naptár, fejlődési statisztika, edzésnapló | **RÉSZBEN KÉSZ** | Room naptár, Coach-adatok és paginált napló van; `116a9ab` a Fejlődés/Health felől közös kézi súlynapló képernyőt, dátumos add/edit/megerősített exact-ID delete utat és adatírási védelmet köt be. 13 unit- és 2 UI-próbakód még nem futott ezen a commiton. Korábbi Coach-, Room- és main-referencia tesztek, timezone-paritás, teljes nézet és friss Coach-sorrendezés továbbra nyitott. |
| Mentés, visszaállítás, Drive/felhőszinkron | **RÉSZBEN KÉSZ** | JSON/ZIP redo, Room import, draft és Drive merge guard implementálva; teljes fotó/Health roundtrip, minden crash-fázis és az utolsó tesztbatch nem igazolt. |
| Témák, nyelvek, beállítások | **RÉSZBEN KÉSZ** | Compose paletta és több nyelvi erőforrás létezik; HU/EN/DE/RO teljes lefedettsége, nagy betűk és kisebb kijelzők integrált ellenőrzése nyitott. |
| Telepített app frissítése és adatmegőrzés | **ELLENŐRIZENDŐ** | Azonos `com.repforge.app`, **2736** verziókód (az elfogadott **2735** után), meglévő kulcsos Actions release és két szintetikus upgrade a `dd4cc27`-en igazolt. A mostani SHA és élő adatos készülékfrissítés nincs igazolva; eltávolítás nélküli fizikai próba előtt tilos napi készülékre telepíteni. |
| Main Capacitor/WebView felületi megvalósítása | **HIÁNYZIK** | Szándékosan nincs visszaállítva a natív ágon; a Compose felület helyettesíti. Ez nem jelenti a main minden üzleti funkciójának natív paritását. |

Részletes, korábbi forráshoz kötött bontás: [NATIVE_PARITY_AUDIT.md](NATIVE_PARITY_AUDIT.md),
[aktuális átadás](WORK_HANDOFF.md), [folytatási terv](plans/NATIVE_FINAL_DELIVERY.md).
Ezek a státuszok nem állítják, hogy a forráság a main összes viselkedését már
natívan helyettesíti; a main funkcióinak visszaállítása WebView-ként nem cél.

## Környezet és tesztek

Az aktív build-konfiguráció a `android/variables.gradle` és `android/build.gradle`:
Temurin 21, Gradle wrapper 8.13, AGP 8.13.0, Kotlin/Compose plugin 2.2.21,
compile/target SDK 36, Build Tools 36.0.0, Java/Kotlin bytecode 17. A
`android/settings.gradle` tartalmazza a `:app`, `:wear`, `:shared` és
`:migration-seed` modulokat. Az alternatív Kotlin DSL buildfájlok nem az aktív
Gradle-belépési pontok.

Új Codex Cloud telepítő és ellenőrző: [környezetleírás](CODEX_CLOUD_ENVIRONMENT.md),
[`scripts/codex-cloud-setup.sh`](../scripts/codex-cloud-setup.sh),
[`scripts/codex-cloud-verify.sh`](../scripts/codex-cloud-verify.sh). A Windows
munkamenetben Temurin JDK 21.0.8, Node.js 22.18.0, npm 10.9.3 és Android
Command-line Tools található (`C:\Android\Sdk`). Az API 36 platform/Build Tools
telepítését a Windows CRL/TLS hálózati hiba akadályozza; TLS-tanúsítvány-ellenőrzést
nem kapcsoltunk ki, helyi Android build ezért nem fut. A Linux Actions sikeresen
telepítette az API 36 platformot és elvégezte a debug buildet. A migrációs ágon
nincs `package.json`/`package-lock.json`; `npm ci` nem alkalmazható. A workflow
Node 22-es, csomagtelepítést nem igénylő Node-ellenőrzéseket futtat.

A `docs/screenshots` képei a régi main alkalmazást mutatják, nem a natív app
dizájnreferenciái. A felhasználó által jelzett frissített tervlapok a
`docs/design/mockups/01-...png`–`04-...png` fájlokban vannak; az eredeti négy
`native-ui-board-*` fájl is megmaradt. A 24 képernyő vizuális referenciáit most
kezdtük Compose-kódra átvezetni; a képek önmagukban nem működési bizonyítékok.
Az aktuális funkcióleltár a `docs/design/NATIVE_DESIGN_HANDOFF.md` A01–E20
szakasza. A metrika-/funkció útvonalankénti tesztfedettség még nincs végig
feltérképezve; a megőrzendő lista nem tekinthető kész lefedettségi mérésnek.

## Következő feladatok

1. Teljes natív Actions-kapu a testsúlynaplót és Health-részletezőt tartalmazó
   aktuális finalization SHA-n;
   a phone Compose UI emulator bootját újra kell próbálni a `40123422503`
   `adb: device offline` infrastruktúrahibája után.
2. A következő CI-hibákat forrás-/környezet-/tesztelvárási ok szerint szétválasztani,
   és csak igazolt hibát javítani.
3. Kritikus Compose telefonos indítás/folytatás/mentés/elvetés és az interval/
   preset, Health-refresh, Coach-order utak integrációs UI-próbái.
4. Az A01–E20 mind a 81 funkciójára natív útvonal/adatforrás/teszt megfeleltetés:
   különösen Coach/progression, napló/naptár/statisztika, testsúly/egészségnapló,
   lokális mentés-visszaállítás, fotók/Health/Drive és HU/EN/DE/RO UX.
5. A frissített 24 képernyős átadó alapján a Compose UI célzott igazítása;
   a jelenlegi kezdőlap csak az első, még ellenőrzendő UI-batch.
6. Azonos forrású, meglévő eredeti aláírókulcsos release gate; régi telefonos/
   Wear release-ről adatot tartó szintetikus upgrade; végül külön teszteszközön
   telefon + Watch7. Készülékes elfogadás és külön jóváhagyás nélkül nincs main
   merge, release vagy élő adatos telepítés.
