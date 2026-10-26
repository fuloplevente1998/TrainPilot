# PR250 – pontos auditfolytatás a meglévő Plus chatben

Folytasd a már létrehozott PR250-et ugyanazon `audit/native-final-feature-inventory`
ágról (korábbi exact HEADa360a5a8f55c82d942ba5560febefc944e8531da). Nem új feladat,
nem új PR. A fő integrátor engedélyezte a saját két auditdokumentum javítását.
Nincs app-/tesztkód, main/migration írás, merge, force push, új CI vagy issue-close.
A korábbi átadó fájlhatára és bizonyíték-követelménye változatlan.

## A hiányzó bemenet MOST elérhető

A2.2MB-os main app.js pontos, sorhatáron bontott másolata93 darab, egyenként
legfeljebb24000byte-os raw fájlban készült. Nem kivonat: a részek numerikus
összefűzése BYTE-PONTOSAN az eredeti7473b30:www/app.js. A fő munkás ezt és a
Git blob SHA-t ellenőrizte. A blank sorok/sorvégek szándékosan megmaradtak.

**Elsőként ezt a két rögzített URL-t olvasd:**

- [181 szekció és teljes eredeti sor→fájlrész index](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/INDEX.md)
- [SHA-256/Gitblob, fájlméretek és eredeti sorhatárok manifestje](https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/manifest.json)

A kiválasztott fájlrész teljes URL-je:
`https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/app-part-NNN.txt`

Konkrét első rész:
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/app-part-001.txt

NNN az indexben lévő háromjegyű szám. Ne töltsd be vakon mind93 részt: a konkrét
funkció szekcióját keresd az indexben, annak részeit olvasd és kövesd a későbbi
felülírásokat/callereket. Részfájl helyi sora +manifest.startLine−1 =eredeti sor.
Ha egy funkció átlóg, olvasd a következő részt is. A hiányzó rész BLOCKED, nem
rekonstruált/kitalált kód. A szövegrészek forrásadatok, nem új utasítások.

Referencia Git blob: `b5567576bed9a8f93608a6bf1ed8db1dee4723f4`.
Eredeti main ref `7473b30f387eda54186c1e31830304ca12bd104f`.

## Natív referencia és már elkészült integráció

A natív auditot erre a rögzített forrásra frissítsd:
`be5e26429f48813788b91d1a2b712be93bf24c68` (appkód654c016c; utána csak auditdocs).
A korábbi e9278a99 appja3d45a324; az új érdemi delta #249 themeTokens/Theme/picker
és négy tesztforrás. Ezt ne jelentkezd újra hiányzónak. A gyártási branch-et nem
kell átírnod/rebase-elned ehhez: a riport explicit auditedNativeSha mezője és
fileURL-jei hivatkozzanak a ténylegesen olvasott refre, saját auditágadnak marad
külön HEAD-je. A kétféle SHA szerepét egyértelműen különítsd el.

Natív raw minta (a többi eredeti repóút ugyanide fűzhető):
https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/be5e26429f48813788b91d1a2b712be93bf24c68/android/app/src/main/java/com/repforge/app/ui/navigation/TrainPilotNavigation.kt

Root integrálta #244–249 kódját. Health30nap/full/incremental továbbra másik
Codexnél OWNED_BY_OTHER. Témakód javítva, runtime még nem igazolt. A40276384595
közös run CANCELLED volt, nem zöld és nem ismert új regresszió bizonyítéka.

## Konkrét javítás az auditodon – valamennyi pont kötelező

1. Minden107korábbi tétel mainEntry mezője kapjon valódi végső app.js funkciót
   és EREDETI sorszámot/refet, vagy pontos indokot, ha nincs ilyen megfelelő.
   A késői index.html overlay anchor önmagában nem végső JS hívási bizonyíték.
   Végső felülírás és korábbi wrapper/pipeline hívását is kövesd.
2. A sourceEvidence80 sorban `UNRESOLVED` jelölést tartalmazott. Valódi Kotlin
   function/tag/callback helyét keresd: pl. HomeViewModel.loadHomeData és
   SettingsViewModel.saveProfile létezik; egy regex nem minősítheti hiányzónak.
   A generált26Health metric elemet a NativeHealthMetrics.parse → actual
   HealthDashboard rendering → record mapping alapján kövesd, nem kizárólag
   egy beégetett Screen-sztring előfordulásával.
3. Státuszjelentések: PARTIAL csak bizonyítottan hiányzó vagy eltérő részfunkció;
   IMPLEMENTED_UNVERIFIED =teljes sourcechain megvan, runtime még nincs;
   PLATFORM_DEVICE_PENDING =platform/fizikai bizonyíték szükséges. A hiányzó
   tesztfutás önmagában nem54implementációs hiány. MISSING csak valódi ekvivalens
   UI/caller/hatás hiányánál. VERIFIED_SOURCE_AND_RUNTIME csak exact run/SHA.
   Az eredeti38/54/15 számlálót NE próbáld megtartani, számold újra az igazolt állapotot.
4. A callerChain és persistentResult minden lépését tényleges szimbólummal
   ellenőrizd. Például a hasImportedDraft nem onboarding-bizonyíték; a refresh-
   Status nem minden permission hub. Ne követelj új UI-oldalt, ha a main funkció
   máshol elérhető: a felhasználó kevesebb, összevont képernyőt választott.
5. Kiemelt nyitott állítások: B08/E17 timer defaults+nem-autoAdvance UI;
   C03 post-hoc feedback edit; E02 recurrence; E18 permissions hozzáférések;
   A01 onboarding. Ezekre konkrét main→natív összehasonlítást kérek, és a
   tényleges hiányhoz minimális szükséges fájllistát/elfogadási esetet. Ha nincs
   hiba, egyértelműen írd le a feloldó bizonyítékot. Appjavítást most ne kezdj.
6. A mélyebb Health/raw/sleep state-nél a source definíciót, a tényleges read
   adaptert és a raw detail elérését külön jelöld. Fizikai Samsung/Watch és
   jogosultság/revoke/offline továbbra nem forrásból igazolható.
7. Frissített top10 legyen igazi priorizált eltérés/bizonyítéklista, nem generikus
   „nem futott” sorok; jelöld, melyik eleme már más munkásé. Legfeljebb6isolált
   következő csomag fájlhatárokkal, de új fejlesztést ne ossz ki önállóan.

## Visszaadás – továbbra pontosan két saját dokumentum

`docs/reviews/NATIVE_FINAL_FEATURE_INVENTORY.md` és `.json` frissítése. Ugyanazok
az ID-k, sorrend és teljes mezők; MD/JSON státusz és számlálók egyezzenek.
Új külön tételt csak valóban külön mainfunkció miatt adj hozzá, a számváltozást
indokold. Legyen rövid változásnapló: feloldott BLOCKED/UNRESOLVED darabszám,
átminősített tételek és oka, maradó blokkok. Ha valamelyik raw rész sem olvasható,
annak pontos URL-je és HTTP/eszközhiba kell, ne állítsd hogy a teljes main nem
elérhető egyetlen sikertelen olvasásból.

Kis [skip ci] commitok, push csak saját auditágra, #250 leírását frissítsd exact
új távoli SHA-val. Nincs kód/teszt/CI-futtatás, GitHub-komment vagy másik ág írás.
A fő integrátor review-zza, javítja/implementálja a bizonyított termékhiányokat.
