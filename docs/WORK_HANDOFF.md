# TrainPilot – aktuális átadás

Frissítve: **2026-10-23**. A main legutóbb ellenőrzött HEAD-je `7473b30`; **#235/#237 mainen, készüléken elfogadva, változatlan 1.3.71 /2735**. Latest: `v1.3.71-perf-20261022`, Wear párja `wear-v1.3.71-perf-20261022`; az október 21-i eredeti kiadás megmaradt. A natív migráció külön ágon folyamatban; #214 karbantartási issue marad. [Takarítási szabályzat](REPOSITORY_CLEANUP_POLICY.md).
Először [AGENTS.md](../AGENTS.md), majd ez a fájl.
GitHub az elsődleges; a lentiek pontos forráshoz kötött eredmények.

## Folyamatban – natív editor és motor egyeztetése (2026-10-23)

- Legújabb **termékkód `9492755797171fe728423050f0295f91a48244bb`**, [CI 39957648783](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39957648783) még fut. **Legutóbbi teljes zöld/APK-forrás továbbra is `1a0cc94`**, lent részletes bizonyítékkal. Új APK nem átadott, élő telepítés továbbra is STOP.
- `bf219eb` új `NativeWorkoutEditing`: teljes natív frame az editor tartós forrása; `PhoneWorkoutExecutionStore.projection()` egy SQLite-tranzakcióban ad frame+eseménykurzort. Snapshot commit előtt nincs nyugta, sikertelen UI-publikálás után a natív szerkesztés visszaállítható; stale UI nem írja felül a natív rögzítést. Complete/undo a natív parancsmotoron halad, foreground controller csak a látható/resumed editorból armolódik; háttérbe kerülés nem pause. `ownerEpoch193` átadva. Lezárt snapshot nem éleszthető újra.
- Első `bf219eb` CI **39956933123**: fordítás zöld, telefon unit **291 összes /8 hiba**. Hét korábbi API-teszt jogosan függő eseményt várt a régi háttérprotokollnál; az új publisher túl korán ackolt. Egy új teszt nem armolta a rögzítéshez előírt controllert. **9492755 javítja:** csak az új, teljes `nativeEditorProjection=1` frame optál be a tartós natív fogyasztóba, a régi protokoll korábbi szemantikája változatlan. Új tesztek valódi Robolectric service ARM-mal, nincs gyengített régi elvárás. 8 új editorpróba várható; a teljes eredményt nem szabad előre zöldnek mondani.
- Érintett források: `NativeWorkoutEditing.kt`, natív store `projection()`, `PhoneWorkoutExecution.publish`, `NativeWorkoutSession.installPrepared`, `ActiveWorkoutViewModel` és lifecycle-bekötés; teszt `NativeWorkoutEditingTest.kt`. A régi `com.repforge` prototípus nem lett launcher vagy második kanonikus motor.
- Következő az aktuális CI hibáinak szükség szerinti javítása/ellenőrzése; utána alsó stopper/GPS explicit measured completion, gyakorlatváltás/undo, gyors/outdoor és importált draft. Native-editor jelző nélküli régi session/draft folytatása még külön egyeztetendő; ne dobjuk el, ne „javítsuk” törléssel. Main/kiadás változatlan, nincs verzióemelés vagy saját telefonos tesztfeladat.

## Natív edzésindítás és lezárás – legutóbbi fejlesztés (2026-10-23)

- **Termékkód `cab522a1b9fabf7a31fec294c57eab98644b5c3d`; legutóbbi CI-forrás `1a0cc941e515f9ad68c10c8c5e50a41330f67198` (azonos `android` fa: `add906cc76607965c9c0f0106f801d3d7dc6f0d7`)**, kizárólag `feat/native-android-migration`; main továbbra is `7473b30`. Nincs verzióemelés, merge, release vagy élő adatokra telepítési kérés.
- Commitok: `92172eb` tartós telefonos session és #228 előtöltés; `a0511e2` közös telefon/óra lezárás; `c063d32` tulajdonos-/restore-/törlési védelem; `cab522a` atomi snapshot-mentési kerítés. Dokumentációs HEAD később eltérhet az APK forrásától.
- A Home programnapi és a Programok indítógombja `NativeWorkoutSession.startWorkout` után navigál. Stabil workout/set UUID, natív execution előbb tartós, aztán aktív snapshot. Ismételt indítás a meglévő telefonos vagy órás edzést nyitja; publikálási megszakításból a meglévő natív frame áll helyre. Nem írja felül az importált `draft` dokumentumot: annak natív folytatása még külön feladat. **Home „quick” és „outdoor-gps” művelete még külön bekötendő**, nem létező programnapként nem indítható.
- `NativeWorkoutStartValues`: elfogadott main `tp228*` port. Naplópozíciónként legfrissebb érvényes súly/ismétlés, másik nap/program/gyorsedzés is forrás lehet; nincs készre jelölés/RIR/GPS/idő/korábbi set-ID öröklés. 32-es adatbázislapok, megfelelő edzésekre szűkítve; érvénytelen/törölt/jövőbeli/dupla gyakorlatazonosítós forrás elutasítva.
- **Egyetlen `TrainPilotWorkoutFinalization.finishWorkout`:** UI és Wear finish ugyanazt használja. Tartós pending draft + natív parancsok felfüggesztése, timer/GPS leállítás, natív események és kurzor együttes tartós mentése a nyugta előtt; Room napló+naptár tranzakció; valódi `closeExecution`, majd drafttakarítás. Nincs mentve nyugta lezárási/takarítási hiba mellett. Stabil azonosító és első finish idő megmarad újrapróbáláskor; régi órás lezárás nem törölheti az új telefonos sessiont. UI csak siker után navigál, mentés alatt visszalépés/ismételt indítás blokkolva.
- `ActiveWorkoutStore.saveOwned` atomi szinten elutasítja a lezárás alatti módosító snapshotokat, akkor is, ha azok lemásolták a pending jelzőt. Csak a külön belső `saveFinalizing` írhatja tovább ezt a draftot. Handoff/restore alatt nincs lezárás; régi, 2000 tombstone ablakon túli törlési alias sem éleszthető újra. Függő lezárásnál retry kell, nem discard. Telefonos mutációk az órás tükröt védik.
- [CI 39944821598](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39944821598): debug/release, csomag/verzió/signer ellenőrzés sikeres; letöltött XML **telefon 284/284, Wear 167/167**, 0 failure/error/skipped, benne **19 új teszt**. Eredeti és október 22-i gyorsított release → signed native `adb install -r` **2/2 sikeres**, nyers seed/verify státusz és telepített verzió ellenőrizve. Az első Wear UI próbán **182/183**, a 160dp/font1 korona-teszt 5s fókusz-timeoutot kapott; logcat szerint az emulator setupwizard/sysui/charging activity előtérbe jött. A változatlan forrású második próbán ugyanaz az 1 fókuszhiba volt. `3d92afc` / `39953204714`: termékbuild/unit/upgrade zöld, az emulátorscript védett komponens tiltásán SecurityExceptiont kapott, ezért nem futottak az UI-próbák. `1a0cc94` / [39953886817](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39953886817): a tiltó komponensparancsok eltávolítva; `dumpsys battery unplug` és ébren tartás modellezi a töltőről leválasztott órát. **39953886817 teljes SUCCESS**, új Wear UI **183/183**, 183 egyedi eset, 0 failure/error/skipped (68+59+56). Letöltött friss XML telefon **284/284**, Wear **167/167**; friss raw upgrade **2/2** ellenőrizve. A tesztek és a termék Wear fókuszfeltételei változatlanok; a töltőről leválasztott emulátor megoldotta a környezeti fókuszhibát.
- Legfrissebb bevizsgált APK-/CI-forrás **`1a0cc941e515f9ad68c10c8c5e50a41330f67198`** (termékkód Android-fája azonos a `cab522a` fájával). Eredeti aláírás, **1.3.71-native-test /2736**. [Phone artifact 12500999530](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39953886817/artifacts/12500999530), SHA256 `908e3f33df6115483d4549d6e52dd8ff6eddf5b132e383243bcb9f9310efc7da`; [Wear 12502025977](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39953886817/artifacts/12502025977), SHA256 `3a9e6ac9f14fe143b4dea0fad9252f8cd866cfd7800db9e07640f23ad8e0323b`. Csomag/verzió/cert/hash függetlenül letöltve ellenőrizve. **CI-bizonylat, nem élő telepítési ajánlás.**
- [Gépi bizonylat, 19 regressziós eset és kapuk](builds/2026-10-23-native-workout-start-finalization.json). A gépi bizonylat a friss `1a0cc94` kaput és a korábbi emulátorhibákat is rögzíti. A Wear/upgrade XML származtatott és így megjelölt; eredeti nyers runnerlog az artifactokban.
- Következő: folyamatos Compose editor–natív motor egyeztetés (edit/complete/undo még részben közvetlen snapshot-mutáció; controller foreground arm/pihenő nincs bekötve), gyors/outdoor indítás és draft folytatás, alsó sorozatstopper/GPS explicit stop automatikus teljesítése és natív undo, majd további képernyők/paritás. Naptár/Coach, teljes katalógus, nyelvek, nagy napló teljesítménye és tényleges natív UI/backup-visszaállítás még nincs lezárva. Saját telefon/Watch7 és valódi kéteszközös szinkron külön későbbi kapu.

## Korábbi teljes technikai kapu – 134adcd (2026-10-23)

- **Élő appra telepítési STOP:** a felhasználó adatvédelmi aggálya jogos. A `134adcd` APK-pár technikai CI-bizonylat, nem az adatokat tartalmazó napi alkalmazás frissítésére átadott build. A teljes telefonos funkcióegyezés hiányzik. Előbb elkülönített emulátoron/teszteszközön működés- és migrációvizsgálat, mentés-visszaállítás ellenőrzése, majd csak a releváns kapuk után készülékes frissítési próba. Azonos csomagnévvel a tesztbuild az élő app közös adatterét használja; a régi tár megőrzése és a szintetikus upgrade-siker nem teljes adatbiztonsági garancia. A régi APK visszatelepítése önmagában nem visszaállítás. Ne kérj eltávolítást, adattörlést vagy downgrade-et. Külön alkalmazásazonosítójú párhuzamos tesztkiadás még nem készült; ilyet ne állíts elérhetőnek, és a végleges azonosítót ne változtasd meg.
- **Kizárólag `feat/native-android-migration`; main merge/kiadás nem engedélyezett.** Munkafa: `/workspace/TrainPilot-native-migration`. Más/Codespaces munkafákat ne írj felül. Ellenőrizd a GitHub valódi HEAD-et; a dokumentációs commit nem új APK.
- **Bevizsgált APK-/alkalmazásforrás `134adcdb76480a9c26a22cc17df6ecc9a8561f0c`.** [CI 39937602813](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39937602813) **teljes SUCCESS**. Telefon/Wear debug és eredeti kulcsos R8 release, csomagnév/verzió/tanúsítvány ellenőrzése sikeres. Letöltött XML: telefon **265/265**, Wear **167/167**, 0 failure/error/skipped.
- **Kerek Wear UI 183/183**, 183 egyedi eset, 0 failure/error/skipped, API30 x86 emulátoron (68+59+56). Az eredeti fókuszvédelem helyes volt: a Wear setup wizard `EmulatorStatusActivity`-ja a teszt közben előtérbe ugrott. A tesztemulátorban letiltva, a forgatási teszt megvárja a tényleges fókuszt. Termék Wear fókuszműködés nincs megkerülve. Képek és logok az artifactokban.
- **Valódi aláírt frissítés 2/2 sikeres:** az eredeti `v1.3.71` és a **gyorsított `v1.3.71-perf-20261022`** tényleges release APK-ról `adb install -r`, eltávolítás/adattörlés nélkül. A közös szintetikus fixture Unicode/nagy napló, törlés, program/nap, testsúly, naptár, nyelv/haptika, draft/ismeretlen kulcs, változatlan eredeti localStorage/mentéshash, GPS, fotó, régi health DB és idempotencia ellenőrzése mind sikeres. **Nem minden valós felhasználói adatváltozat garanciája.**
- Mindkét APK: `com.repforge.app`, **`1.3.71-native-test /2736`**, eredeti signer `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`. [Telefon signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39937602813/artifacts/12494139412), [Wear signed ZIP](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39937602813/artifacts/12494159370). Telefon SHA256 `bdcaedac58a56aa20bd46cfc0870146d7d3f04cf97dc0ca45bae6bd63fa3bd82`; Wear `8ae386e71995ab92daba5a66dea0c8d4403d078b3c57e84375244cbfdbdd7e72`.
- [Bizonylat/artifactazonosítók és korábbi hibák](builds/2026-10-23-native-migration-checkpoint.json). A naplókból **származtatott**, így megjelölt [Wear XML](builds/native-134adcd/wear-round-derived.xml) és [upgrade XML](builds/native-134adcd/signed-upgrade-derived.xml) nem a futtató natív JUnit-riportja; az eredeti nyers eredmények is artifactban maradtak. Unit XML-ek közvetlen Gradle-riportok.
- **A teljes natív kiváltás nincs kész; az élő app cseréje még nem elfogadott.** [Konkrét funkcióhiányok](NATIVE_PARITY_AUDIT.md): telefonos edzésindítás callback csak navigál, lezárás hiányos és túl korán navigál; alsó stopper/GPS teljesítés, Naptár/Coach, nyelvek és nagy napló teljesítménye hátra van. A zöld technikai kapu ezeket nem zárja le. Fizikai telefon/Watch7 és kétirányú szinkron szintén külön kapu.
- **Következő implementáció:** a kanonikus telefonos edzésindítás, natív eseményegyeztetés és egyetlen biztonságos lezárás; a már elfogadott stopper/GPS készre jelölés megőrzésével. Utána Naptár/Coach és további paritás a 2026-10-22-i gyorsított mainhez. Csak megfelelő mentés és releváns sikeres próbák után élő készülékes teszt; ne kérj eltávolítást vagy adattörlést.
- Migráció: aktuális `https://localhost` WebView localStorage chunkolt olvasása, eredeti tár változatlan, helyi archívum `noBackupFilesDir/native-migration-v1371/legacy-localstorage.json`, Room-tranzakció, sérült forrásnál nincs kész nyugta. Destruktív Room downgrade nincs. A natív UI indulása a migráció sikeréig vár.
- A korábbi AndroidJUnit release-ellenőrzés a közös R8/Kotlin ABI miatt még a teszt előtt omlott össze. Helyette `:migration-seed` **CI-segéd** (nem termékapp) használ platform Instrumentationt. A natív APK-ban **csak `-PnativeUpgradeTests` esetén** szerepel a vele együtt optimalizált `src/nativeUpgrade/.../NativeMigrationProbe.kt`, egyetlen megtartott belépési ponttal. Minden korábbi adatellenőrző feltétel megmaradt. A régi hybrid AndroidJUnit források változatlanok; a natív upgrade lane nem használja őket. A fixture a segéd APK assetje, nem a telefonos termék UI-ja.
- Aktív **Groovy** Gradle: JDK21/JVM17, AGP8.13.0/Gradle8.13, Kotlin/Compose/serialization **2.2.21**, Room2.7 KAPT, compile/target SDK36, Wear Fragment1.8.6. Az inaktív `.gradle.kts` fájlok később rendezendők. [Toolchain](../NATIVE_TOOLCHAIN_POLICY.md). Gradle heap 3GB, két worker, Kotlin daemon1GB; a korábbi D8 1,5GB-os OOM megszűnt. Ez buildmemória, nem telefonos igény. Forgó cache megőrzi a tényleges build-cache frissítéseit.
- Minden Gradle/emulátor/APK a meglévő `native-android.yml` Actionsben. A natív ágon a használhatatlan Capacitor Quick/Phone/Wear auto kapuk kizárva; natív kapuk helyettesítik. Nincs main merge/force push/új termékág vagy a jóváhagyott native-test verzión túli emelés. Main ellenőrzött HEAD továbbra is `7473b30f387eda54186c1e31830304ca12bd104f`.
- Ha a CLI joblog üres/403, `github_fetch_workflow_job_logs` ad szöveget, az artifact connector letöltése működik. Ne írd ki a teljes aláírt URL-t/környezetet. A felületi távoli hibajelzés után a kapcsolat/környezet működött és minden commit/APK megmaradt. Figyeléshez a `gh run watch` mellől elhagyható az `--exit-status`: a workflow piros volta ettől még az API-ban ellenőrzendő.

## Aktuális kiadási alap és megőrzendő gyorsítások – 2026-10-22

- A gyorsított telefon/Wear kiadás tag-/APK-forrása **`5d27aa9c5ebf68d1c206c88a74dea89e660a4937`**, alkalmazáskódja az elfogadott `715dc5d` builddel egyezik. Változatlan `1.3.71 /2735`, `com.repforge.app`, eredeti aláírás. Main ellenőrzött HEAD: `7473b30f387eda54186c1e31830304ca12bd104f` (kiadási dokumentáció).
- Telefon SHA256 `850b5e1e597e574dbf5cf55cf82721a3f77879447db9852fa056d2b889d50570`; Wear SHA256 `c145afd729f4a9bf91d7cef48f2e9c82ddb265b73dc2b5bb8a15b12a9839e2ea`. A natív frissítési teszt most mindkét telefonos 1.3.71 APK-ról indul; a verziószám önmagában nem különbözteti meg őket.
- A mainhez képesti áttekintésből megőrzendő: egyetlen stabil azonosítós, újrapróbálható lezárási folyamat; nincs mentve nyugta lezárási/takarítási hiba esetén; naptári teljesítés és stopperleállítás; eredeti alsó stopper és explicit pozitív stop utáni automatikus teljesítés; mindkét oldal szükséges oldalsó planknél. A natív importban ezen viselkedések egyezése **még nincs igazolva**.
- Teljesítmény: a gyorsított WebView már csak 32 naplókártyát rajzol először, startup olvasásokat közösít, elrendezésméréseket csökkent, R8-at használ, a Health/Wear munkát az inputszálról leveszi. A Compose megfelelőit külön kell vizsgálni: Lazy-lista/adatbázislekérdezés mérete, háttér-IO, Room/Flow frissítés, képernyőmegjelenés és navkijelölés egyezése. A natív kódban az onResume Health munka már IO-n fut; a felesleges health-preferenciavisszaírást `eb1d21f` kerüli. WebView startup baseline-profilt nem kell vakon átmásolni Compose-ra.
- A main ág működő UI-ját nem keverjük automatikus merge-gel a teljesen natív ág eltérő Gradle/Activity struktúrájába. A forrásellenőrzés nem teljes funkcionális elfogadás. [Gyorsított kiadás](https://github.com/fuloplevente1998/TrainPilot/releases/tag/v1.3.71-perf-20261022).

## #214 – GitHub-bemutató frissítve (2026-10-21)

- Kifejezett felhasználói kérésre a GitHub About rövid angol leírása és 11 Topics címkéje ténylegesen elmentve, böngészőben visszaellenőrizve.
- Angol/magyar README: csoportosított aktuális funkciók, 8 telefonos + 2 natív Wear-kép. Új [magyar funkcióáttekintés](FEATURES.hu.md); [képeredet](screenshots/README.md) és SHA-256 jegyzék.
- Képek változtatás nélkül a kiadott `60fe647a` forrás main CI-jéből: Phone **39519510921**, Wear round **39519510953**. Szintetikus fixture-k, nem személyes adatok és nem Play-készülékfotók.
- Kizárólag dokumentáció/képek és repository metaadatok változtak. App-/natív kód, 1.3.71 /2735 verzió és kiadott APK-k változatlanok. Új APK-/emulátorkaput nem indítunk ugyanarra a forrásra.
- #214 nyitva marad a korábban megőrzött Play/partneri/licencelési feladatokkal.

## Lezárva – #234 / 1.3.71 kiadva (2026-10-21)

- #234 merged mainre: `60fe647a98ccdcc7f102d647a6a1cc8db40bbbfa`; elfogadott PR-head `be76bf3753e821cab0d97390fa84fcb5c53b1108`. HEAD, PR teszt-merge és main fája azonos: `4943c34a3aa135e78ed823d54b4206bde25c8c22`.
- Végleges verzió **1.3.71 /2735** mindkét platformon. A lentebbi 1.3.7 teszt-APK a készülékes elfogadás előzménye.
- Készülékes visszajelzés a `cbff3509` APK-ról: a rejtett rekordrészletező megnyílik, de az Új PR jelvénnyel nem zárható.
- A click handler most a korábbi nyitott állapotot váltja; második koppintás teljesen elrejti a panelt és nyitva hagyja az edzéskártyát. A részletek címével is összecsukható.
- A jelvény `aria-expanded` állapota és nyila követi a panelt; bezáráskor a fókusz visszatér a látható jelvényre.
- A meglévő Chromium PR-teszt bővítve nyitás–zárás–újranyitásra, külön cím általi zárásra és HU/EN/DE/RO kis kijelzős, nagy betűs változatokra.
- További készülékes kérés: az Edzésstílus/progresszió zárt panel legyen laposabb; a Mégse láthatóan dobja el a szerkesztést; a sorozat-haptika pipája egyezzen az appéval.
- A belső módpanel 46px-es kompakt fejléccel, dupla függőleges padding nélkül jelenik meg. A Mégse eldobja a függőben lévő választást, visszaállítja a mentett mezőket és becsukja a belső panelt, a külső Edzés beállításai nyitva marad.
- A haptika ugyanazt a `tp155-sync-check` témázott 22px-es négyzet/pipa vezérlőt használja, mint a többi szinkronbeállítás. A mentett kapcsoló működése változatlan.
- A módválasztó/mentés tesztek és a haptika integrált UI-tesztje kibővítve; a Mégse modellteszt helyben sikeres. A PR-részletező célzott tesztje bekerült a kötelező Chromium suite-ba.
- Végső teszt/APK-forrás: `924664ef7882aa59cbedfbcebe6c95605511d9b1`. Quick **159/159**, Performance **SUCCESS**, telefonos Release Gate **39515146159 SUCCESS**, Chromium **6/6, 107/107**, natív XML **251/251, 0 failure/error/skipped**.
- Aláírt, kicsomagolt telefonos APK átadva; verzió **1.3.7 /2734**. SHA-256 `2d2ba088f61cf1c34a9da0252a66d0e8caee41449bf817860aacfc2aaedaaa3a`. A csomagolt app.js/CSS blobazonos a forrással, a source stamp a fenti commit. Aláírás és signer-folytonosság kapu sikeres.
- [Korábbi készülékes teszt-APK bizonylata](builds/2026-10-21-234-pr-toggle-training-ui.json). A felhasználó az alábbi üzenettel elfogadta és lezárta ezt a kört.
- Felhasználói elfogadás: „Zárható, mehet mainre 1.3.71, ez lesz a legfrissebb verzió, release gate stb. leírásban is frissítsük.”
- Jóváhagyott kiadásjelölt: **1.3.71 /2735** telefon és Wear; metaadatok, cache, verziótesztek és kiadási jegyzetek frissítve. Helyi verzió/kiadási tesztek **11/11**, app.js szintaxis sikeres.
- Az 1.3.71 merge előtti kapui: Quick **39518365505: 159/159**, Performance **39518365457 SUCCESS**, metadata **39518365472 SUCCESS**; telefon **39518361442**, Chromium **6/6, 107/107**, natív XML **251/251**; Wear **39518361372**, XML **167/167**; kerek UI **39518361440**, XML **183/183** három shardban (68+59+56). Minden XML: **0 failure/error/skipped**. Aláírás/signer-folytonosság sikeres.
- Main Quick **39519510907**, Performance **39519510934**, telefon **39519510921**, Wear **39519510922**: SUCCESS. Telefon XML **251/251**, Wear XML **167/167**, 0 failure/error/skipped.
- Publikus APK-pár, forrás mindkettőnél a fenti main merge, aláírás/signer-folytonosság sikeres, letöltött artifact és release digest egyezik:
  - [Telefon 1.3.71](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.71/TrainPilot-1.3.71.apk), SHA-256 `f3fc054d9e2142fc7919b0e7beee8c2db35527de2fcb85406c23ab23141e5e68`.
  - [Wear 1.3.71](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.71/TrainPilot-1.3.71-wear.apk), SHA-256 `ea963e7393bb29748fdd3325d3e83e6a715261e5a93bdd8ed38ab5876773e97a`.
- `v1.3.71` explicit Latest, `wear-v1.3.71` latest=false; mindkét tag a main merge-re mutat. Telefon app.js/CSS blobazonos; mindkét APK forrásjelölése ellenőrizve. [Kiadási bizonylat](builds/2026-10-21-release-1371.json).
- Main kerek UI **39519510953 SUCCESS**, mindhárom shard és teljes **183/183** lefedettség sikeres. A mentett snapshot hiánya miatti cold boot figyelmeztetés nem teszthiba.
- Következő: nincs további #234 fejlesztés vagy készülékes jóváhagyásra váró pont. #225 lezárva (történeti audit, #230/#234 által felváltva); #231 külön marad; Play-előkészítés #214 változatlan.

## Elfogadva – #230 / 1.3.7 közzétéve

- **Felhasználói jóváhagyás (2026-10-20):** „Rendben, zárható :) mehet mainre 1.3.7 verzioszamon, a release gatenel, teszteknel ezt vegyü figyelembe.”
- #226 mentett PR-jelvény, #227 kézi sorozat haptika, #228 Napló-alapú gyakorlatonkénti súly- és ismétlés-előtöltés és #229 tényleges sorozat-RIR: felhasználó által elfogadott #230 csomag.
- #228 korábbi készülékes hibajelzés: a tervezett edzés rendben volt, a gyors edzésnél a célismétlés felülírta a Napló alapján előtöltött tényleges ismétléseket. Javítva mindkét gyorsedzés-indítási úton; a hat Chromium shard integrált tesztet futtat.
- **1.3.7 /2734** összehangolt telefon/Wear verzió: `package.json`, `package-lock.json`, két SOURCE_VERSION, két Gradle, `www/app.js`. A verziópolitikai tesztek az 1.3.7 címkéket és a 2734 kódot várják.
- **Merge előtti kötelező kapuk:** forrásazonos Quick/Performance, 6/6 Chromium, telefonos és Wear aláírt release gate, óra kerek kijelző teszt. Az előző `49784cb` 1.3.6 /2733 sikeres kapui a funkciót igazolták, de nem bizonyítják az 1.3.7-es buildet.
- **Main merge és kiadás kizárólag sikeres 1.3.7 kapuk után.** A mainre kerülő azonosított forrásból az önálló telefon/Wear workflow-k külön `v1.3.7` / `wear-v1.3.7` kiadást állítanak elő. Verifikáció és #226–#229 lezárás a tényleges merge/kiadás után.

## Aktuális – #223 mainen, #214 külső előkészítése nyitott

- Felhasználói merge-jóváhagyás 2026-10-20 18:21 (Europe/Budapest): „Minden mehet mainre”.
- **#223 merged mainre: `c9a395e56259849a2182c8b7c8b4e0d9c5d1ab7c`**; merge fa `6b978991c22ae20380bb6cab322678082c171686`, byteazonos az elfogadott `7ea45f3305366f5253fefef7df9f42ec77dfc1a0` PR-head fájával.
- Ág: `maintenance/214-repository-hygiene`, alap main `a3a4ddce2250c32ef5b55e9ed8733b6a3aa85bda`.
- Angol + magyar README, 37 release dátum/platform szerinti jegyzéke, friss családos változáslista és Play-checklist, proprietary csomagjelölés.
- Új verzióegyezési ellenőrzés és célzott tesztek; csak leírásfejlécet módosító release-karbantartás, védett eredeti szöveg/tag/melléklet-metaadatokkal.
- Helyi célzott teszt: 8/8; verzió 1.3.6 /2733, eredeti kiadási feljegyzések és alkalmazás-/natív forrás változatlan.
- [#223 – beolvasztott PR](https://github.com/fuloplevente1998/TrainPilot/pull/223), ellenőrzött kód/metaadat HEAD: `3275b97cc2a6d1342ed866e523d24c807f73da60`; fa `c72d127e14c3d15ddfc0b9d5654da6f079e05f13`. A PR-merge tesztfája byteazonos.
- Quick [39454775410](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454775410): **154/154 + 8/8 karbantartási teszt**, Performance [39454775235](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454775235): **SUCCESS**, metadata PR-validálás [39454774998](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454774998): **SUCCESS**.
- A tényleges release-leírás rendezés [39454564328](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454564328) sikeres a `58bd1b1` forráson. **37/37 eredeti szöveg hash-azonos; 37 tag és 154 melléklet metaadata/digestje változatlan**, független GitHub/ls-remote utóellenőrzéssel. [Riport](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39454564328/artifacts/12281085093).
- Végső PR-head (`7ea45f3`) Quick **39455207276**, Performance **39455207248**, metadata-validálás **39455207238**: mind SUCCESS. A tesztelt PR-merge fa egyezik a HEAD-del és a tényleges main merge fájával.
- Automatikus main kapuk a merge `c9a395e` forrásán: Quick **39456946831**, Performance **39456946849**, Phone Gate **39456946879**, Wear Gate **39456946991**, Wear round UI **39456946769**; az átadás rögzítésekor még folyamatban. Ez a kizárólag átadást rögzítő dokumentációs commit nem indít duplikált kapukat; a karbantartási és alkalmazáskód változatlan.
- Verzió **1.3.6 /2733**; a merge nem módosított taget vagy közzétett APK-mellékletet és nem publikált Playre. Következő: a #214 megőrzött külső Play/Samsung feladatai; külön #224 audit Draft PR is nyitott, nem része a #223 csomagnak.
- #214 nyitva marad a [rögzített külső Play/Samsung/aláírás/licenc feladatokkal](MAINTENANCE_214.md).
- A felhasználó #184/#197 main engedélyét 2026-10-20-án újra megerősítette; GitHub szerint mindkettő már completed, #222 merged `f413a3a`.

## Elfogadott alkalmazás – #222 mainen, funkcionális kör lezárva

- Felhasználói elfogadás/lezárás 2026-10-20 17:12 (Europe/Budapest):
  „Zarhato ez is, igy akkor kb minden mar nem ?”
  Ez a legutolsó Samsung/Regeneráció-javítás elfogadása és a korábban feltételesen
  jóváhagyott RIR utáni #222 integráció lezárási pontja; nem kitalált részletes mérési napló.
- **#222 merged mainre: `f413a3a475cf31a4e596429fe1c8dcf3e1e135df`. #184 és #197 completed.**
  Main merge fa `791db4a8608a27bdcbfc31490a8cbf427b3fd307`, byteazonos a PR-head fájával.
  A korábbi dokumentációs eltérésen túl a bevizsgált alkalmazásforrás változatlan:
  `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`.
- Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A végső dokumentációs HEAD Quick
  **39441673066** és Performance **39441673033** is sikeres.
  A merge után automatikusan induló main futások külön futások; eredményüket nem találjuk ki.
- Verzió **1.3.6 /2733**, Wear változatlan. Nem történt verzióemelés vagy új release.
  A már telepített, elfogadott [telefonos APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379)
  alkalmazásforrása azonos a mainre került kóddal.
  [CI/APK bizonylat](builds/2026-10-20-222-recovery-tiles.json);
  [lezárási bizonylat](builds/2026-10-20-222-accepted.json).
- **Egyetlen nyitott issue: #214**, karbantartás és Google Play-előkészítés.
  A Samsung nyilvános/production partner-hozzáférés ellenőrzése is ott marad;
  saját készülékes siker nem partnerregisztrációs igazolás. Nyitott PR nincs.
- A jelenlegi funkcionális fejlesztési sor lezárult. A dizájnmódosítások lehetnek
  a következő külön feladat; még nem kezdődtek el. Rögzített ötletek:
  [UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md), külön későbbi tesztágra.

## Történeti #222 – Samsung-pontszám olvasható, Regeneráció-csempe egységesítése

- Felhasználó 16:24-es képe **sikeres készülékes Samsung-olvasást igazol** az e97fe1e APK-n.
  Ez nem teljes PR/main elfogadás és nem külön egyezésigazolás a Samsung Health kijelzésével.
- Kérés: a Regeneráció csempe is ugyanazt a mai külső pontszámot mutassa.
  A csempe eddig friss saját alvás/HRV rekordhoz kötötten, Coach-korláttal számolt.
  Most a hiteles mai Samsung-pontszámot mutatja Home/Egészség oldalon akkor is,
  ha külön alvás/HRV-rekord nincs. Külső adat hiányában a korábbi saját becslés marad.
- Közös egész számú kijelzés: a képen látott SDK-tizedes érték 55/100-ként jelenik meg.
  A nyers SDK-pontszám és a Coach fájdalom/terhelés védelmei változatlanok.
  Kártya/csempe/Coach forrássor azonos formázást használ. Natív és Wear kód érintetlen.
- Az új teszt elkapta a nyitott panel mögött elmaradó csempefrissítést; javítva csak
  a read-only pontszám közvetlen DOM-frissítésével, panel/draft/scroll megőrzése mellett.
- **Végső alkalmazásforrás:** `c14e70778fdb81d42a49fe4b2ef8aa58b3a5292b`;
  PR-merge fája is `8332fd6ffa21d0281af7bc5f702f928ecdcef6e7`.
  Quick **39440379202: 154/154**, Performance **39440379183: SUCCESS**,
  Phone Gate **39440372153: SUCCESS**. Chromium **6/6, 105/105**, natív XML
  **249/249, 0 failure/error/skipped**. A Samsung-teszt egész számú kártya/Health/Home
  csempe-egyezést igazol frissítéskor, nyitott panel és külön alvásrekord hiánya mellett.
- **Telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39440372153/artifacts/12273816379),
  1.3.6 /2733, SHA-256 `3798a8e679f9a5a16b013be774f918e62eca24e0070b36084a7d2e1349cf5e28`.
  app.js/index.html byteazonos, forrásjelölés/SHA256SUMS/aláírás és signer-folytonosság helyes.
  [Bizonylat](builds/2026-10-20-222-recovery-tiles.json). Wear APK változatlan.
- **Következő:** új APK-n csempe/kártya egyezésének készülékes visszajelzése.
  #222 Draft, main/verzió változatlan, nincs merge/kiadás/issue-lezárás.

## Korábbi #222 – 1001-es napi lekérdezés javítása

- Felhasználó 16:07-kor az új APK képernyőképét küldte: **SAMSUNG_HEALTH_1001**.
  Samsung dokumentáció: ERR_INVALID_INPUT (érvénytelen bemenet); nem igazolt partnerhiba.
- Konkrét kódhiba: a napi startDate és endDate azonos dátum volt, mindkettő inclusive.
  A valódi Samsung SDK ezt azonos éjféli kezdő/végű LocalTimeFilterré alakítja.
  Javítás: pozitív [mai éjfél, következő éjfél) helyi dátumintervallum; a holnapi pont
  továbbra is kizárt. Nincs másnapi vagy saját becslés Samsung-pontként felcímkézve.
- Natív teszt a tényleges buildelt SDK-kérés LocalTimeFilterét vizsgálja; 1001-et okozó
  nulla hosszúságot minden energiaolvasó teszt elutasít. Új regresszió 23/24/25 órás
  budapesti napokra és a holnapi éjféli pont kizárására. Korábban a mock ezt nem nézte.
- **Alkalmazásforrás:** `e97fe1e31a824640ea78c1cf39b45c1c572456db`;
  Quick/Performance PR-merge fa is `d5f7d46679560ee44fcf7615d66c393b9ad25a0c`.
  Quick **39436835835: 154/154**, Performance **39436835798: SUCCESS**,
  Phone Gate **39436826354: SUCCESS**. Chromium **6/6, 105/105**, letöltött JSON-ok
  forrása/duplikációmentessége ellenőrizve. Natív XML **249/249, 0 failure/error/skipped**;
  valódi SDK-szűrő/DST regresszió és a régi nulla hosszúságot igazoló teszt sikeres.
- **Új telefon teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39436826354/artifacts/12271587790),
  1.3.6 /2733, SHA-256 `52eecad1878da78d20d773cb35193ed27482c6464cb14215de75e8c499b98fc8`.
  app.js/index.html és a forrásarchívum natív kódja byteazonos, forrásjelölés és SHA256SUMS
  helyes; apksigner és eredeti signer-folytonosság sikeres.
  [Bizonylat](builds/2026-10-20-222-samsung1001.json).
- Forrásváltozás csak telefonos Samsung-olvasó és natív teszt; felület/Wear/verzió azonos.
  **Következő:** új APK telepítése, Regeneráció → Samsung-pontszám frissítése;
  a mai Samsung-érték egyezésének készülékes visszajelzése. Készülékes siker még nem
  igazolt; #222 Draft, main `1393982d`, nincs merge/verzióemelés/issue-lezárás.

## Korábbi #222 – Regeneráció alá rendezés

- Felhasználó az új APK-ról képet küldött: Samsung Healthen telefonon/órán van mai
  pontszám, TrainPilot az általános „olvasás nem sikerült” állapotot mutatja.
  **Ez sikertelen készülékes teszt**, a korábbi zöld CI nem igazolja a Samsung-hozzáférést.
- Kérés szerint a Samsung-kártya a Regeneráció megnyitható részébe került;
  külön felső szintű kártya nincs. Frissítés után a panel nyitva marad.
- Az önálló olvasás állapotellenőrzése is kizárólag ENERGY_SCORE engedélyre kérdez;
  más adattípus nem akadályozhatja. Natív SDK-hibakód megőrizve aszinkron és belső
  platform-kivételen át; részleges olvasásnál is strukturált mezőhiba jut a felületre.
- A felület külön jelzi a 2000 engedélyhiányt, 2003 app-hozzáférési korlátot,
  3001 frissítési igényt és 3000/3002/3003 Samsung-beállítási akadályt. Nyers
  kivétel/adat nem kerül ki; diagnózishoz hibakód látszik. Hiánynál marad a TP-becslés.
- Samsung hivatalos API-feltétel: csomagnév + release aláírás + engedélyezett
  adattípusok partnerregisztrációja kell normál felhasználói hozzáféréshez.
  https://developer.samsung.com/health/data/process.html
  https://developer.samsung.com/health/data/api-reference/-shd/com.samsung.android.sdk.health.data.error/-error-code/index.html
  A konkrét készülékhibát kód nélkül **nem** minősítjük biztos partnerhibának.
- **Bevizsgált alkalmazásforrás:** `ae82d481b31d8d01394ce41d08ef003c8847d0c1`.
  PR-merge fája is `4056ab3898c1386ea61969c330c06287b612380f`, azonos alkalmazásforrás.
  Quick **39423620810: 154/154**, Performance **39423620964: SUCCESS**.
  Phone Gate **39423616111: SUCCESS**, Chromium **6/6, 105/105**, natív XML
  **248/248, 0 failure/error/skipped**. Új engedélyhatár- és kivételláncteszt sikeres;
  Samsung Chromium: Regeneráción belüli kártya, nyitva maradó panel és hibakód rendben.
- **Új telefonos teszt-APK:** [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39423616111/artifacts/12264688934),
  1.3.6 /2733, SHA-256 `49426605ba22046bdba3051004528e70814c0b62de9714c372c488ef67ff752d`.
  APK app.js/index.html byteazonos, forrásjelölés és SHA256SUMS helyes, signer
  az eredeti v1.0.0-val egyezik. Wear változatlan. [Bizonylat](builds/2026-10-20-222-recovery.json).
- **Következő készülékes próba:** Egészség → Regeneráció → Samsung energiapontszám,
  egyszeri frissítés/engedélyezés; ha hiba marad, a konkrét hibakód szükséges.
  Ez a build az adatolvasás és hibaazonosítás javítása, nem igazolt készülékes megoldás.
  #222 Draft, main/verzió változatlan, issue-k nyitva.

## Korábbi #222 – teljes CI sikeres, teszt-APK kész

- Integráció: `feat/184-197-coach-integration`, **#222 nyitott / Draft**.
  **Bevizsgált alkalmazásforrás:** `a8bec13630136d89128f259bd065f6c2f57b2cea`.
  Quick/Performance PR-merge fája byteazonos: `889b18da87d94f3dc38cbb37b9629ecab2927e71`.
  A későbbi dokumentációs commit nem új alkalmazásbuild.
- **Quick 39391181566: 154/154; Performance 39391181636: SUCCESS.**
  **Phone Release Gate 39391176400: SUCCESS; Chromium 6/6, 105/105**,
  mind a 6 letöltött shard-JSON pontos forrásra és duplikáció nélkül ellenőrizve.
  Natív XML: **246/246, 0 failure/error/skipped**, benne a Samsung SDK olvasó 8 esete.
- **Telefon teszt-APK: 1.3.6 /2733**, [ZIP, benne APK és forrás](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39391176400/artifacts/12249603397).
  SHA-256: `0352cbfc68b6f46dc402f82a267878bc1f0aaec2035e3514e782606c2bf7fb80`.
  A letöltött APK app.js/index.html byteazonos a bevizsgált forrással; source stamp helyes,
  apksigner és eredeti v1.0.0 signer-folytonosság rendben. Wear forrás/APK változatlan.
  [Ellenőrzési bizonylat](builds/2026-10-20-222-rir-samsung.json).
- **#197:** opcionális valódi 0–10 RIR, közvetlen számmezőn; azonnali draftmentés
  újrarajzolás nélkül; hibás szerkesztés nem hagy korábbi mérést. Reload, edzéslezárás,
  a tényleges lazy Napló és a következő konzervatív Coach-ajánlás E2E sikeres.
  A cél-RIR nem mérés, hiányzó cél nem implicit 0; nincs automatikus súlyátírás.
- **#184 felhasználói hiba:** Samsung Healthben van pontszám, TrainPilotban nem látszik.
  Javítás: külön napi ENERGY_SCORE olvasás, külön engedély, a lépés-/kalóriaforrástól
  független automatikus frissítés; mai hiteles Samsung-pont elsődleges, hiány/hiba/
  engedélymegvonás esetén saját becslés. Nincs kettős pontozás, fájdalomkorlát megmarad.
  Az Egészség oldalon adat nélkül is látszik a pontszámkártya és az ok/frissítés.
  Az önálló olvasás nem írja felül a teljes Samsung-napi rekordot. Éjfél kizáró végpont javítva.
- **Felhasználói felhatalmazás:** „ha a RIR javítás bekerül, mehet mainre” feltételes
  jóváhagyás, majd kifejezett #222 feltöltési engedély. Ez nem sikeres készülékes
  Samsung-tesztbeszámoló. A közben jelentett pontszámhiba új APK-n még visszaellenőrzendő.
- **Következő:** APK telepítés; Egészség → Samsung energiapontszám, szükség esetén
  egyszeri engedélyezés. Egyezzen a Samsung Health mai értékével; hiánynál fallback.
  Ellenőrizendő a pihenőidők megőrzése APK-frissítés/Drive-visszaállítás után és releváns
  telefon/Wear használat. A készülékes eredményt ne találd ki.
  Main továbbra is `1393982d`, nincs merge, verzióemelés, release vagy Issue-lezárás.
  Dizájnmódosítások a funkcionális lezárás után, külön későbbi feladatként jönnek.

## #216 lezárva, 1.3.6 mainen

Felhasználói engedély: **„Rendben, akkor mehet, zarhatjuk ezt a teljes részt es akkor 1.3.6.lesz”**.
Ez lezárási/main/kiadási jóváhagyás; részletes fizikai tesztbeszámolót nem találunk ki.

- **#218 merged `2059770c0eadf9c9e66929635865861667404d41`; #216 completed.**
  Órás menü: aktív edzés/Gyors edzés elöl; Naptár/Programnapok közös csoportban.
  Új kültéri gyors/tervezett edzés előtt látható, aktivitásonként megjegyzett GPS-választás,
  futó edzésről közvetlen állapot/kezelés. Reconnect/render nem indít rögzítést.
  Kézi gyakorlatváltás megőrzi a pihenő eredeti határidejét és automata fázis/cél/tulajdonosát.
  Telefonos működés és GPS-adatút megmaradt. HU/EN/DE/RO.
- **#219 merged `98be66221430202715aafa9a71583bbbd22d4fd3`; 1.3.6 /2733.**
  Mindkét app metaadatai és appfelirat, SW cache, verzióelvárások és kiadási jegyzetek frissítve.
  Main teljes fája byteazonos a bevizsgált `76fa539` PR-head fájával:
  `d0f5487aa3338369c879126b7d2ca15cbbd4e3b3`.
- #216 alkalmazáskód `c04851f`; utána csak tesztjavítások. A home fixture törli az aktív edzést,
  ezért a felfelé gesztus után **Gyors edzés** az első látható menügomb, nem Napló/Folytatás.
  Az eredeti azonnali láthatósági, gesztus- és geometriai feltételek megmaradtak.

## Ellenőrzött kiadási forrás

- Quick **39231417696**, Performance **39231417699**: sikeres (`76fa539`).
- Telefon **39231412495**: Chromium **6/6**, JVM **243/243** (`76fa539`).
- Wear **39231317332**: JVM **159/159** (`7b600e4`).
- Ownership **39231417723**: **81/81**; kerek UI/platform **39231317394: 183/183**.
- Minden vizsgált XML: **0 failure/error/skipped**. Külön #216 kerek UI
  **39231315228: 183/183**; új GPS/pihenő/menü tesztek mind az öt kijelző/betűméreten.
- `7b600e4..76fa539` csak AGENTS/átadás/bizonylat; alkalmazás- és tesztkód byteazonos.
- Aláírás folytonos a nyilvános v1.0.0-val; signer SHA-256:
  `4abcf50041e99aef2c100e989103ef426002db464c7963ab02d3230c67356688`.
- [Bizonylat, APK-hashek és források](builds/2026-10-19-release-136.json).

## Végleges, publikált 1.3.6 APK-pár

Mindkét kiadás/tag forrása **`98be66221430202715aafa9a71583bbbd22d4fd3`**, 1.3.6 /2733:
- [Telefon APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/v1.3.6/TrainPilot-1.3.6.apk),
  SHA-256 `4b3b2fd90b38912a7a51157ed18c337f361f5e3f5521df69d10a64697c4cc987`.
- [Wear APK](https://github.com/fuloplevente1998/TrainPilot/releases/download/wear-v1.3.6/TrainPilot-1.3.6-wear.apk),
  SHA-256 `ac9d16066df7f63290bca20656bf883391e41bbd1399cde397b9292176233958`.

Main telefon **39233098299**: 6/6 UI, JVM XML 243/243; Wear **39233098238**: JVM XML 159/159;
0 failure/error/skipped. Mindkét apksigner és eredeti signer-folytonosság sikeres.
A letöltött main artifact APK-hashek egyeznek a nyilvános release asset digestjeivel,
a telefonos forrásarchívum és Wear SOURCE_VERSION egyaránt `98be6622`-t igazol.
Main Quick **39233098560**, Performance **39233098199** sikeres.
Kerek UI **39233098292** további automatikus main futás; az azonos teljes forrásfa
183/183 kapuja a fenti **39231317394** futáson XML-ben igazolt. Élő státuszát GitHubon ellenőrizd.
A későbbi dokumentációs HEAD nem új alkalmazásbuild; a két APK tényleges forrása a fenti commit.

## Külön maradó backlog / lezárt előzmények

- **#206**: maradék aktivitásnézet/UI. Az explicit sikeres telefonos GPS „Mérés befejezése”
  külön sorozat-OK nélkül már elfogadott; térképbezárás/hibás/implicit stop nem teljesítés.
- **#197**: választható edzésstílusok/progresszió; **#184**: Samsung Energy Score;
  **#214**: karbantartás/Play. Az 1.3.6 ezeket nem zárja le.
- #193/#202 önálló órás GPS és telefonos naplótérkép + offline átadás/visszavétel elfogadva;
  #213 main `311f507`, #193 completed. Az órán nincs térképrajzoló: az óra rögzít, a telefon rajzol.
- #215 kompakt Intervallumok elfogadva/main `c67006b`; kétoldali UI nem engedélyez egyórás motort,
  két pozitív idő kell a kész állapothoz. #212 korona/lünetta #217 main `102eb366`, elfogadva.
- #188 visszavonás és #189 folyamatos 3 mp feloldás elfogadva/mainen. #192/#211 sztornózott.
- [Külső UI/funkció backlog](plans/UI_TEST_BRANCH_BACKLOG.md): csak rögzítésre engedélyezve,
  későbbi megvalósítás külön tesztágon. [Részletes #193 terv](wear/P2_193_ONALLO_ORAS_EDZES.md).
- Régi hosszú átadás: `885b6df:docs/WORK_HANDOFF.md` a Git-történetben; történeti státuszai elavultak.

## Környezet

CLI auth lejárt (gh 401, git fetch/push credential nélkül), GitHub connector működik.
Ne írj ki credentialt, ne force-pusholj. Contents/Git tree/commit + expected_sha CAS használható.
Helyi `local-release-136` más commitazonosítókat tartalmaz, alkalmazásforrás azonos; **ne pushold
vakon a helyi feature ágat**. A távoli main/PR-head a kanonikus. APK-hoz GitHub artifact permalink
(ZIP, benne APK) vagy valódi release letöltés kell, belső /workspace link nem használható.
