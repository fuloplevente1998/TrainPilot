# Natív migráció – véglegesítési státusz

Frissítve: **2026-10-24**. Munkaág: `feat/native-android-finalization`,
legutóbbi commit `8ead84a`. A `main` (`8a36502`) és a forráság
(`feat/native-android-migration`, `033386e`) változatlan; egyikre sem történt
írás. A `29ac3a2` és `ccc5c9a` alkalmazáskód-változásai már patch-egyezők
(`572a10b`, `38e38df`); a friss `033386e` katalógus-/migrációs javítása
`8ead84a` commitként egyirányúan átkerült. A `69bc1fd` és `910cdf4`
dokumentációs checkpointok új alkalmazáskódot nem tartalmaznak. A migrációs ág
eltérő, korábbi tesztváltozatait nem vettük át; az `ec2c65e` tesztjavításai
megmaradtak. A `main` Capacitor/WebView felülete és
Node/npm manifesztje nincs átemelve: a natív ágon a Compose felület a cél.
Ez a forrás-/architektúra-csere nem bizonyít termékfunkció-paritást; az alábbi
funkciók külön vannak értékelve. Az ág létrehozásakor rögzített összevetés:
`main` 69 commit-tal előrébb, a migrációs ág 151 commit-tal előrébb állt.

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
ellenőrzéskor **in progress**; az SDK-telepítés sikeres, a phone/Wear debug build
futott, a unit suite még nem indult el. Az új katalógusteszt és a teljes
aláírt/update/UI kapu ezért még nincs igazolva; nincs friss APK-pár vagy
frissítési bizonyíték.
Fizikai telefonos/Watch7-próba sem történt.

| Main-funkció / natív terület | Státusz | Meglévő bizonyíték és fennmaradó rés |
| --- | --- | --- |
| Edzéstervek, gyakorlatok, sorozatok, előzmény, RIR | **RÉSZBEN KÉSZ** | Natív terv-/sorozatmotor, Room, editor és lapozott napló létezik. `8ead84a` a rögzített main-katalógus 106 gyakorlatát/13 programját hiánypótló seedeléssel vezeti be; a test suite még nem futott ezen a forráson. Teljes editor-, Coach- és RIR-paritás továbbra nyitott. |
| Aktív és gyors edzés | **RÉSZBEN KÉSZ** | Programnapi és quick/outdoor indítás, stabil sorozatazonosítók, natív editor és lezárás létezik. A draft/indítás/mentés/elvetés teljes Compose útja még nem kapott készülékes próbát. |
| GPS-táv, útvonal, térkép | **RÉSZBEN KÉSZ** | `922b6b7`/`e164ef3` GPS-kötési és explicit stop-regressziói CI-ben zöldek; a valós helyengedély-, szenzor-, térkép- és útvonalviselkedés készüléken nyitott. |
| Beltéri, GPS nélküli időmérés | **KÉSZ** | A `40384e7` kapu 20 célzott tesztet és a telefonos teljes unit-csomagot igazolta. Az új intervallummód ezzel nem tekinthető igazoltnak. |
| Automatikus rögzítés és intervallumidőzítés | **ELLENŐRIZENDŐ** | Korábbi stopper/GPS record/undo viselkedés tesztelt; a `9ea2ddf` single-set módja és hordozható presetje új, még nem futtatott tesztekkel érkezett. |
| Pihenő, egyedi beállítások, tartós mentés | **RÉSZBEN KÉSZ** | Skip/±15 mp és Room rollback tesztelve; a friss globális timer-default/preset és importált draft paritása nyitott. |
| Health Connect és Samsung Health | **ELLENŐRIZENDŐ** | Natív adapterek és foreground frissítés jelen van; a `036a2cd` frissítése és az engedély-/háttérváltási út nincs újra CI-vel igazolva, fizikai Health-próba nincs. |
| Telefon–Wear szinkron és óráról vezérelt edzés | **RÉSZBEN KÉSZ** | A natív ownership/command út és unit tesztjei léteznek; offline tulajdonosváltás, két eszközös adatkonfliktus és Watch7 próba még nyitott. |
| Wear szenzorok, GPS, értesítés, komplikáció, Tile | **ELLENŐRIZENDŐ** | A natív Wear komponensek visszaállítva, a kerek UI 183/183 korábban zöld. Ez nem bizonyít valós szenzor-, GPS-, Tile- vagy értesítési működést. |
| Naptár, fejlődési statisztika, edzésnapló | **RÉSZBEN KÉSZ** | Room naptár, Coach-adatok és paginált napló van; per-gyakorlat Coach és 32 main-referencia eset, 6 Room- és 1 hordozható-beállítási unit teszt kódja bekerült (`38e38df`), de az aktuális futás még nem igazolta. Timezone-paritás, teljes nézet és friss Coach-sorrendezés újratesztje is nyitott. |
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
dizájnreferenciái. A négy, felhasználó által megadott natív tervlap most már
verziókezelt formában a `docs/design/mockups/native-ui-board-1.png`–`-4.png`
fájlokban van. Ezek 24 képernyő vizuális referenciái, nem működési bizonyítékok;
a Compose-felület még nem lett hozzájuk igazítva.

## Következő feladatok

1. A `40119184559` aláírt-frissítési kapuját és a `40119749853` teljes futását
   ellenőrizni; a signer/version folytonosságot, UI-kapukat és katalógusteszteket
   csak a ténylegesen zöld eredmény után jelölni igazoltnak.
2. A fennmaradó teszthibákat a környezet- és forráskódhibáktól különválasztani,
   majd az érintett teszteket újrafuttatni.
3. Kritikus Compose telefonos indítás/folytatás/mentés/elvetés és az interval/
   preset, Health-refresh, Coach-order utak integrációs UI-próbái.
4. Teljes main-paritás: Coach/progression, napló/naptár/statisztika, lokális
   mentés-visszaállítás, fotók/Health/Drive és HU/EN/DE/RO UX.
5. A négy eltárolt natív tervlap alapján a Compose UI célzott igazítása,
   először a közös grafit/arany theme és az alap navigáció/főoldal, a
   felhasználói beállítások és mind a hat működési feltétel megőrzésével.
6. Pontos azonos forrású, meglévő eredeti aláírókulcsos release gate; régi
   telefonos/Wear release-ről adatot tartó szintetikus upgrade; végül külön
   teszteszközön telefon + Watch7. Készülékes elfogadás és külön jóváhagyás
   nélkül nincs main merge, release vagy élő adatos telepítés.
