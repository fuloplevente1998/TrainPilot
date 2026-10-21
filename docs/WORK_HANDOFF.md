# TrainPilot – aktuális átadás

Frissítve: **2026-10-21**. **#234 mainen, 1.3.71 /2735 telefon/Wear kiadva; v1.3.71 a GitHub Latest.** A #225 audit PR beolvasztás nélkül lezárva, #230/#234 által felváltva; #235 draft, mentési kör készüléken elfogadva; UI/stopper és görgetési kör készüléken elfogadva; első oldalváltások lassúsága még vizsgálat alatt; #214 karbantartási issue marad;  [takarítási szabályzat](REPOSITORY_CLEANUP_POLICY.md).
Először [AGENTS.md](../AGENTS.md), majd ez a fájl.
GitHub az elsődleges; a lentiek pontos forráshoz kötött eredmények.

## Folyamatban – #235, első oldalváltások lassúsága

- Új visszajelzés a `1b5e488` teszt-APK-ról: „Rendben, jo lett.” A stopper- és
  görgetési javítás elfogadott. Maradék: appindítás után az első oldalváltások
  kifejezetten lassúak, később gyorsak. Ennek külön célzott profilozása folyamatban;
  main merge/release/verzióemelés nincs engedélyezve.
- Célzott első/második oldalnyitási profil: Chromium 4× CPU, 0/200/1500 szintetikus
  edzés. A készülék konkrét hidegindítási késését nem reprodukáltuk pontosan.
  Layout/stílus költség és nagy háttér-Napló DOM kimutatható, de a geometriai
  műveletek összevonása nem adott következetes teljes javulást. A kísérlet app/index
  módosítása visszaállítva, nincs belőle új alkalmazáskód vagy APK.
- Függő célzott kérdés: minden teljes appbezárás/újraindítás után visszatér-e,
  vagy csak APK-telepítés utáni első indításnál volt. Ez választja szét a rendszer
  egyszeri bemelegedését a visszatérő hidegindítási útvonaltól. Ne tekintsd ezt
  megoldottnak a korábbi zöld CI vagy a stopper elfogadása miatt.
- Az első kör `655183c` APK-járól felhasználói visszajelzés: nincs dupla edzés,
  törlés után nem tér vissza a bejegyzés; a többi kért ellenőrzés működik.
  Ez készülékes mentési elfogadás, **nem main merge-engedély**.
- Új hibák: Naptár/Coach/Beállítások görgetése 1–1,5 s-ra akad; planknél dupla
  stopper; stopper leállítása után felesleges sorozat-OK; készenléti pont nem érthető.
- Döntés: **a felső intervallumos stopper kerül ki, az eredeti alsó marad**.
  Az intervallumbeállítás az alsó kártyában összecsukott, választható rész;
  nem készül új stopper vagy új globális render-/finish-wrapper.
- Explicit pozitív stopper-stop az eredeti `toggleSet` folyamaton jelöl készre,
  megőrizve visszavonást/haptikát/natív rögzítést. Nulla, implicit pause/halt,
  navigáció és nullázás nem teljesítés. Oldalsó planknél két pozitív oldal kell.
- A Wear háttérkatalógus publikálásakor 130 history-olvasás helyett 2: közös
  publikációs pillanatkép és előtöltési index; törlési ellenőrzés egyszer soronként,
  üres tombstone-listán nincs teljes legacy hash. Nincs tartós, elemszám alapú cache:
  azonos hosszúságú szerkesztés/törlés a következő publikációban megjelenik.
  A már törölt sor a friss órás előzménylistából is kimarad.
- Panelgörgetés/touchmove alatt csak a közönséges home/katalógus küldés vár
  180 ms nyugalomra. Kifejezett force és az aktív edzés parancs-/állapotszinkronja
  nem vár; parancsfeldolgozás közben sincs katalógus miatti halasztás. Folyamatos
  görgetés + nyugtázott parancs célzott próbája PASS; az eredeti frissítés
  automatikusan folytatódik.
- Visszavonás utáni oldalsó stopper: az aktuális sorozat mentett bal/jobb idejét
  mutatja akkor is, ha nincs futó helyi óratulajdonos. A megállított óracache nem
  írhatja felül a kézi szerkesztést. Automatikus stop → visszavonás → nullázás/
  szerkesztés → újrarajzolás és fordított oldalsorrend célzott próbája PASS.
- Mai készenlét képe: Samsung 87, effort-load 45 → meglévő <55 korlát miatt 54;
  külön TP-becslés `70 + (45−70)×0,35`, kerekítve 61. Nem összeadás/átlag.
  Az alacsony load-pont nagyobb jelentett megterhelést jelent. A képlet változatlan,
  a részletezőben HU/EN/DE/RO magyarázat jelenik meg a konkrét 87 → 54 korlátra.
- Helyi célzott eredmény: előtöltési index kompatibilitás PASS; időzítő 8 nyelv/méret
  kombináció, explicit/implicit/0 stop és 200/1500 edzéses publikálás PASS;
  kompakt egy-/kétoldali UI PASS; korábbi mentési integritás 14/14 PASS;
  egyetlen finish-belépő strukturális ellenőrzése PASS.
- **Bevizsgált alkalmazásforrás: `1b5e488b35ca39dfe79ab17e6347b38daf0401a8`.**
  PR-merge és head fa azonos: `1577cb0e9fbc7d2534e3e2bbc4f25bc7f95a4471`.
  Utána csak dokumentációs átadás, nem új alkalmazásbuild.
- Friss, forrásazonos GitHub-kapuk: Quick **39552869084: 161/161**, Performance
  **39552869065 SUCCESS**; telefon **39552881285 SUCCESS**, Chromium **6/6, 109/109**,
  natív XML **251/251**; valódi WebView/háttér **39552884885: 1/1**;
  Wear **39552887694: 167/167**; kerek UI **39552891381: 183/183** (68+59+56).
  Letöltött shard-JSON és XML: **0 failure/error/skipped**, minden UI-modul egyszer.
  A régi, külön OK-ra építő stoppertesztek az új szerződést és valódi visszavonást
  ellenőrzik; a Napló UI-teszt tíz kijelzőméreten is sikeres.
- [Új telefonos teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39552881285/artifacts/12324696670),
  SHA-256 `f9a87c24621131a3b743ba5273f426d29cdc091c9159a51afe8889d542052ad8`.
  APK app.js/index.html/training-107.css byteazonos, forrás-ZIP stampje a fenti commit.
- [Azonos forrású Wear teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39552887694/artifacts/12324303353),
  SHA-256 `922e8b936da0b58b5824a3cc34601b5ea31d923d4cccf2baa90f432a33da8936`.
  Wear alkalmazáskód nem változott. Mindkettő **1.3.71 /2735**, SHA256SUMS,
  aláírás és signer-folytonosság ellenőrzött. Nincs APK-ba ágyazott JSON source stamp;
  telefonos forrásarchívum és Wear sidecar igazolja a commitot.
- [Új bizonylat](builds/2026-10-21-235-ui-scroll-stopwatch.json).
  Következő készülékes próba: Naptár/Coach/Beállítások tartós görgetése; plank explicit
  Stop → kész pipa külön OK nélkül → Visszavonás; oldalanként két pozitív idő;
  visszavonás utáni szerkesztés/újranyitás. A Samsung 87 → 54 magyarázat legyen látható.
  **Stopper/görgetési kör készüléken elfogadva; első navigáció lassúsága külön marad.** #235 draft, main `bc43d4e` változatlan,
  nincs merge, release vagy verzióemelés. A teljes Napló/PR lapozás és általános
  tárolási helyreállítás későbbi kör marad.

## Előző bevizsgált #235 alkalmazásforrás – mentési kör

- [#235 draft PR](https://github.com/fuloplevente1998/TrainPilot/pull/235), ág
  `refactor/workout-finalization`. Alap main `bc43d4e`; változatlan verzió **1.3.71 /2735**.
- Tesztelt alkalmazásforrás **`655183c93ba9d63b30d59dd4bea89117ae7b94cb`**;
  PR teszt-merge fa és head fa azonos: `33eb07a5ec273d2d3fd44fa15c32f7f754859edf`.
  Az ezt követő átadási commit kizárólag dokumentáció, nem új alkalmazásbuild.
- A finish korábbi 11 definíciós/hozzárendelési helye és dinamikus wrapperei
  helyett egy koordinátor. Natív reconcile/close, sorozat, stopperek, GPS, naptár,
  részleges megerősítés és értékelés a közös folyamat része.
- Idempotens history-upsert, rollback, tartós draft-jelző; sikertelen natív close
  vagy drafttakarítás nem küldhet „mentve” nyugtát. A teljes lezárás előtt érkező
  natív sorozat bekerül. Sérült history/scheduled JSON lezáráskor nem írható felül;
  általános db.get-helyreállítás nincs még kész. A végleges naplóból az előtöltési
  UI-segédmező kimarad, a draftból nem töröljük idő előtt.
- Friss alkalmazásazonos GitHub-kapuk: Quick **39545876527: 160/160**, Performance
  **39545876543 SUCCESS**; telefon **39545881760 SUCCESS**, Chromium **6/6, 108/108**,
  natív XML **251/251**; tényleges WebView/háttér **39545884307: 1/1**;
  Wear **39545887200: 167/167**; kerek UI **39545889547: 183/183** (68+59+56).
  Minden XML-ben **0 failure/error/skipped**. A 14 új integritási eset GitHubon is PASS.
- [Telefon teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39545881760/artifacts/12321955384),
  SHA-256 `45a642464cd5dc15c308dc38e55b1aecfb36fdcdd55c86be623f6d5b8e36ccf3`.
  A csomagolt app.js byte-azonos; a forrás-ZIP stampje a fenti commit.
- [Wear teszt-APK](https://github.com/fuloplevente1998/TrainPilot/actions/runs/39545887200/artifacts/12320794234),
  SHA-256 `9247c5fd2b09103efe7cc531af3bcb7c0e3a826307a12d3c624a83a14c660e87`.
  Wear alkalmazáskód nem változott; az új build ugyanahhoz a commitpárhoz tartozik.
  Mindkét APK **1.3.71 /2735**, aláírás és signer-folytonosság sikeres.
- [CI/APK bizonylat](builds/2026-10-21-235-workout-finalization.json).
  Main merge/release/verzióemelés nem történt; a mentési kör készülékes elfogadása a fenti új szakaszban szerepel.
- Készülékes smoke: telefonos és órás befejezés egyszeri naplóbejegyzéssel;
  részleges mentés Mégse/Mentés; oldalsó stopper; GPS sikeres Mérés befejezése
  külön sorozat-OK nélkül; tervezett edzés naptárban teljesült.
- Következő kör: [APP_REFACTOR](plans/APP_REFACTOR.md) szerint **Napló/PR indexelés
  és fokozatos megjelenítés**, 200/1500 edzéses méréssel. Ez az 1. kör még nem
  oldja meg a nagy napló megnyitásának teljesítményét vagy az összes auditpontot.

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

## Történeti környezeti megjegyzés (új munkamenetben ellenőrizendő)

Korábban: CLI auth lejárt (gh 401, git fetch/push credential nélkül), GitHub connector működik.
Ne írj ki credentialt, ne force-pusholj. Contents/Git tree/commit + expected_sha CAS használható.
Helyi `local-release-136` más commitazonosítókat tartalmaz, alkalmazásforrás azonos; **ne pushold
vakon a helyi feature ágat**. A távoli main/PR-head a kanonikus. APK-hoz GitHub artifact permalink
(ZIP, benne APK) vagy valódi release letöltés kell, belső /workspace link nem használható.
