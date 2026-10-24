# Natív migráció – véglegesítési státusz

Frissítve: **2026-10-24**. Munkaág: `feat/native-android-finalization`, a
`feat/native-android-migration` távoli ág **`036a2cdb930fb03bce370a5c55d64f29d5945d67`**
commitjából létrehozva és közzétéve. A `main` (`8a36502`) és a forráság
változatlan; egyik sincs összeolvasztva a munkágba. A két ágat a Git-előzményből
és a `docs/NATIVE_PARITY_AUDIT.md` fájlból olvastuk össze, a hivatkozott
legacy-forrás visszaállítása nélkül. A frissítéskori összevetés: `main` 69
commit-tal előrébb, a migrációs ág 151 commit-tal előrébb állt.

## Státuszjelölések és bizonyíték

- **KÉSZ** – a konkrét, megnevezett viselkedésre van futtatott tesztbizonyíték.
- **RÉSZBEN KÉSZ** – működő részek mellett hiányzó paritás vagy integrált út maradt.
- **HIÁNYZIK** – a main megfelelő működéséhez nincs natív megfelelő.
- **ELLENŐRIZENDŐ** – forrás/próbakód létezik, de a jelenlegi SHA-n nem futott le.

Legutóbbi teljes natív Actions-kapu: [40072751592](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40072751592),
**SUCCESS, 6/6 job** a `dd4cc27` commiton: telefon **393/393**, Wear **167/167**,
kerek Wear UI **183/183**, aláírt, szintetikus upgrade **2/2**. A jelenlegi
`036a2cd` ezt követő három commitja (`6af1d79`, `9ea2ddf`, `036a2cd`) nem része
ennek az eredménynek. A handoff **122 új unit- és 6 instrumentációs
próbakódot** jelez, amelyek még nem futottak le. Nincs igazolt fizikai
telefonos vagy Watch7-próba.

| Main-funkció / natív terület | Státusz | Meglévő bizonyíték és fennmaradó rés |
| --- | --- | --- |
| Edzéstervek, gyakorlatok, sorozatok, előzmény, RIR | **RÉSZBEN KÉSZ** | Natív terv-/sorozatmotor, Room, editor és lapozott napló létezik. Teljes editor-, Coach- és RIR-paritás, valamint az utolsó batch integrációs futtatása hiányzik. |
| Aktív és gyors edzés | **RÉSZBEN KÉSZ** | Programnapi és quick/outdoor indítás, stabil sorozatazonosítók, natív editor és lezárás létezik. A draft/indítás/mentés/elvetés teljes Compose útja még nem kapott készülékes próbát. |
| GPS-táv, útvonal, térkép | **RÉSZBEN KÉSZ** | `922b6b7`/`e164ef3` GPS-kötési és explicit stop-regressziói CI-ben zöldek; a valós helyengedély-, szenzor-, térkép- és útvonalviselkedés készüléken nyitott. |
| Beltéri, GPS nélküli időmérés | **KÉSZ** | A `40384e7` kapu 20 célzott tesztet és a telefonos teljes unit-csomagot igazolta. Az új intervallummód ezzel nem tekinthető igazoltnak. |
| Automatikus rögzítés és intervallumidőzítés | **ELLENŐRIZENDŐ** | Korábbi stopper/GPS record/undo viselkedés tesztelt; a `9ea2ddf` single-set módja és hordozható presetje új, még nem futtatott tesztekkel érkezett. |
| Pihenő, egyedi beállítások, tartós mentés | **RÉSZBEN KÉSZ** | Skip/±15 mp és Room rollback tesztelve; a friss globális timer-default/preset és importált draft paritása nyitott. |
| Health Connect és Samsung Health | **ELLENŐRIZENDŐ** | Natív adapterek és foreground frissítés jelen van; a `036a2cd` frissítése és az engedély-/háttérváltási út nincs újra CI-vel igazolva, fizikai Health-próba nincs. |
| Telefon–Wear szinkron és óráról vezérelt edzés | **RÉSZBEN KÉSZ** | A natív ownership/command út és unit tesztjei léteznek; offline tulajdonosváltás, két eszközös adatkonfliktus és Watch7 próba még nyitott. |
| Wear szenzorok, GPS, értesítés, komplikáció, Tile | **ELLENŐRIZENDŐ** | A natív Wear komponensek visszaállítva, a kerek UI 183/183 korábban zöld. Ez nem bizonyít valós szenzor-, GPS-, Tile- vagy értesítési működést. |
| Naptár, fejlődési statisztika, edzésnapló | **RÉSZBEN KÉSZ** | Room naptár, Coach-adatok és paginált napló van; per-gyakorlat Coach, timezone-paritás, teljes nézet és friss coach-sorrendezés újratesztje nyitott. |
| Mentés, visszaállítás, Drive/felhőszinkron | **RÉSZBEN KÉSZ** | JSON/ZIP redo, Room import, draft és Drive merge guard implementálva; teljes fotó/Health roundtrip, minden crash-fázis és az utolsó tesztbatch nem igazolt. |
| Témák, nyelvek, beállítások | **RÉSZBEN KÉSZ** | Compose paletta és több nyelvi erőforrás létezik; HU/EN/DE/RO teljes lefedettsége, nagy betűk és kisebb kijelzők integrált ellenőrzése nyitott. |
| Telepített app frissítése és adatmegőrzés | **ELLENŐRIZENDŐ** | Azonos `com.repforge.app`, **2736** verziókód (az elfogadott **2735** után), meglévő kulcsos Actions release és két szintetikus upgrade a `dd4cc27`-en igazolt. A mostani SHA és élő adatos készülékfrissítés nincs igazolva; eltávolítás nélküli fizikai próba előtt tilos napi készülékre telepíteni. |

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
[`scripts/codex-cloud-verify.sh`](../scripts/codex-cloud-verify.sh). A jelen
Windows munkamenetben nincs Java, Node/npm, Bash, Android SDK vagy `sdkmanager`,
ezért itt helyi Android build nem futtatható. A natív migrációs ágban nincs
`package.json`/`package-lock.json`; `npm ci` nem alkalmazható. A Node 22 és npm
előkészíthető, a lock nélküli telepítést nem találjuk ki. Az új natív Actions
workflow-ágon Node 22-es, csomagtelepítést nem igénylő Node-tesztek is futnak.

## Következő feladatok

1. Codex Cloudban a dokumentált setup scriptet kijelölni, hivatalos hostok
   hálózatát engedélyezni, majd a `codex-cloud-verify.sh` teljes build/unit/Node
   futását és az új SHA Actions-kapuját ellenőrizni.
2. Új CI-n futtatni a `6af1d79`/`9ea2ddf`/`036a2cd` óta hozzáadott 122 unit és
   6 UI-tesztet; a hibákat a környezet- és forráskódhibáktól külön választani.
3. Kritikus Compose telefonos indítás/folytatás/mentés/elvetés és az interval/
   preset, Health-refresh, Coach-order utak integrációs UI-próbái.
4. Teljes main-paritás: Coach/progression, napló/naptár/statisztika, lokális
   mentés-visszaállítás, fotók/Health/Drive és HU/EN/DE/RO UX.
5. Pontos azonos forrású, meglévő eredeti aláírókulcsos release gate; régi
   telefonos/Wear release-ről adatot tartó szintetikus upgrade; végül külön
   teszteszközön telefon + Watch7. Készülékes elfogadás és külön jóváhagyás
   nélkül nincs main merge, release vagy élő adatos telepítés.
