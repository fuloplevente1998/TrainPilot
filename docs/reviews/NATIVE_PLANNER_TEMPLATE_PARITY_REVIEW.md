# Natív planner main-plan-7 paritás – külön ág átadó

Dátum: 2026-10-26. Repo: `fuloplevente1998/TrainPilot`.
Feladat: `docs/plans/NATIVE_PLANNER_PARITY_FIX_HANDOFF.md` (a `981d1e06` dokumentumverzióból).
Saját ág: `fix/native-planner-template-parity`.
**Rögzített alap:** `4eae08c8b917893ac5da37346201872828783814`
(`feat/native-android-migration` állapota az átadó írásakor).
Draft PR cél: `feat/native-android-migration`; nem merge, nem main.
Az exact végső saját HEAD-et a draft PR head SHA-ja adja meg.

## Igazolt ok – hibás natív validáció, nem hibás main-orákulum

Main referencia: `7473b30f387eda54186c1e31830304ca12bd104f`,
`www/app.js` Git blob `b5567576bed9a8f93608a6bf1ed8db1dee4723f4`.
A **legutolsó** `validateProfile` felülírás az app.js 8208–8217. sorában
az `excluded` minden elemét ellenőrzi a valós `exercises()` azonosítói ellen,
kivéve a külön backup-import út `rf148BackupValidationIds` azonosítóit.
Normál tervgeneráláskor utóbbi `null`. Az ismeretlen ID hibája
`planner.errorExclude`.

A `generatePersonalProgram` utolsó felülírása (16639–16673) előbb
`validateProfile(input)`-ot, majd `tp128GenerateBase(profile)`-t hív,
és **csak ezután** lép a `home-basic` templateslot-helyettesítő ágra.
Tehát a rossz kizárásazonosítót nem lehet helyettesítővel elfogadni.

A `native-planner-cases-main-7473.json` `main-plan-7` kizárásai:
`pushup`, `modified-pushup`, `incline-pushup`, `squat`, `air-squat`.
Az utolsó kettő **nem exercise ID**, hanem az `squat` például mozgásminta;
sem a rögzített main forrásban összegyűjtött ID-k, sem a forrásból készült
`native-planner-main-7473.json` `needs/meta` azonosítói között nincs.
A natív preview korábban `keys(profile,"excluded")` után csak az ismert
gyakorlatokat szűrte, az ismeretlen ID-kat észrevétlenül figyelmen kívül hagyta.
Ezért volt képes elfogadni a main által elutasított profilt.
**A referencia `rejected: true` helyes; a fixture byte-pontosan változatlan.**

## Minimális módosítás és invariánsok

- `NativeProfileProgramPreview.kt`: az ismert `catalogById` ellen
  ellenőrzi a nyers `excluded` JSONArray minden elemét, még az eligible/pool/
  helyettesítés előtt. Hibája: `IllegalArgumentException("Invalid exercise exclusion")`.
  Nem elég a korábban normalizált halmazt vizsgálni, mert az üres elem kiesne.
- `NativePlannerTemplateParityTest.kt`: `main-plan-7` nem akármilyen
  kivétellel, hanem pontosan a helyes okkal utasítandó el. A többi main-plan
  paritási sorrend/gyakorlatsor/becslés assertion változatlan.
- `NativePlannerExclusionSafetyTest.kt`: három külön ellenőrzés:
  ismeretlen ID + ismert ID-s helyettesíthető profil;
  minden **valós** gyakorlat kizárása továbbra is elutasít;
  egy érvényes pushup-kizárás B-napi biztonságos helyettesítést kap,
  megtartva A/B-t, legalább 3 gyakorlatot és a perc-budgetet.
- `android/app/src/test/reference/planner-main-7473-exclusion.cjs`: külön,
  opcionálisan reprodukálható, a valódi, hash-ellenőrzött main végső
  validátorszakaszát futtató szűk Node-próba. Nem a generátor újraírása.

**Nem módosult:** production `native-planner-main-7473.json`, tesztfixture,
`NativePlannerPolicy.kt`, katalogus/DB/schema, programeditor, Health,
modes, edzésmotor, workflow, verzió és bármely más ág.
Az equipment/avoid-area/safety, minimum 3, B-nap, time-estimate guard
ugyanazon változatlan natív forrásútban maradt; a tesztkódjuk elkészült,
de natív futásuk **nem bizonyított** ezen a commiton.

## Ténylegesen elvégzett ellenőrzés

1. Rögzített main blobot lekérve a **valódi utolsó** validator-szakaszt
   (8208–8217) izolált V8-környezetben végrehajtva:
   `main-plan-7` → `planner.errorExclude`;
   csak a három létező pushup-ID-ra javított exclusions →
   validator elfogadja. **PASS.**
2. A környezet stubjai nyíltan: `exercises()` 115 azonosítója a pinned
   main `id:...` literáljaiból és a forráseredetű natív policy `needs`
   kulcsaiból; `GEAR132={}` (a profil gear üres);
   `profileGear132` az átadott üres `gear` mezőt használta;
   `TP149_PLANNER_ROWS.focus` csak `balanced`,
   `area={}`; `tp149T` visszaadta a kulcsot;
   `rf148BackupValidationIds=null`.
   E próba kizárólag a main **validátorának** hibáját bizonyítja,
   nem a teljes main generátor, böngésző, aktuális user catalog,
   Room vagy history futását. Az üres synthetic history nincs beolvasva.
3. A Node-referenciafájl JS-szintaxisa V8 `Function` parse ellenőrzéssel
   **PASS**. GitHub `compare_commits` a pinned SHA-hoz: csak
   a négy kijelölt app/test/reference fájl változott a review létrehozása előtt.
4. Natív Kotlin guard és assertion helye forrásvizsgálattal ellenőrizve.
   A bázis produkciós policy hash `3264b822c1e11cbca0ba0cd4110a00ccc703cedd`;
   app-forrás blob `b5567576bed9a8f93608a6bf1ed8db1dee4723f4`.

**Megírva, de NEM futtatva:** 8 paraméterezett JVM-es main-plan eset
(erősített 7. elutasítás), 3 új JVM-regresszió;
a külön Node-script nincs helyi repó/git mainobjektum hiányában futtatva.
Nincs Gradle/JVM/XML, teljes CI, emulátor, APK vagy készülékes teszteredmény.
A V8 validator-próba nem egyenértékű ezekkel.

## Integrátori teendők / nyitott pontok

1. Teljes Git historyval rendelkező repo munkafán:
   `node android/app/src/test/reference/planner-main-7473-exclusion.cjs`.
   A script `git show 7473b30f387eda54186c1e31830304ca12bd104f:www/app.js`
   tartalmának valódi Git blob SHA-1-ét ellenőrzi, csak utána fut.
   Shallow checkout esetén előbb a hivatkozott main commitot le kell kérni.
2. Kizárólag az integrátor indítson célzott Android JVM-tesztet:
   `NativePlannerTemplateParityTest` és
   `NativePlannerExclusionSafetyTest`. A pontos Gradle/XML PASS/FAIL
   és ha szükséges, a közös CI csak az összeépített migrációs forráson.
3. Integráció előtt a jelenlegi `feat/native-android-migration` HEAD-del
   diff/rebase-review szükséges. Más PR-k újabb változtatása nem része ennek.
4. Main merge, PR-close, release, verzióváltás vagy élő install nincs engedélyezve.
