# Önálló külső fejlesztési feladat – 2026-10-26

Először olvasd: `AGENTS.md`, `docs/NATIVE_RESUME.md`, `docs/WORK_HANDOFF.md`
aktuális teteje; `docs/reviews/NATIVE_PR250_INTEGRATION.md`, majd a lent kijelölt
forrásokat. Dolgozz a tényleges GitHub repository-n: fuloplevente1998/TrainPilot.

## Közös szerződés

- Exact kiindulás: `7043fc7d5fac91383f583332fd8fc0e9397f4a1c`, cél PR-base: `feat/native-android-migration`.
  Saját új ágat ebből a commitból készíts; ne rebase-elj/merge-elj közben másik munkásra.
- Csak a saját ágra commit/push, kis `[skip ci]` commitok, egy nyitott **Draft PR**.
  A main és a migrációs ág írása/merge, PR/issue lezárás, force push tilos.
- Csomag/verzió: com.repforge.app /1.3.71-native-test /2736 változatlan.
- Nincs új teljes CI/build/emulátorfuttatás: a közös kapu és integráció Root feladata.
  Meglévő futást ne cancel/re-run-olj. Célzott statikus ellenőrzést végezz;
  megírt tesztkódot ne jelents futtatott PASS-nak. Gradle/APK csak központilag GitHubon.
- Ne használj belső subagentet. Ne írj GitHub-kommentet. PR-leírás megengedett.
- Meglévő Kotlin/adatút továbbhasználata, nincs párhuzamos adatbázis/motor.
  Minden jelenlegi assert és thread/CAS/metadata/ownership guard megmarad.
- Root a Settings időzítőt, Journal feedbacket, szinkron/backup/Health és CI hibákat
  kezeli. Másik munkás Home/onboarding vagy Calendar vagy Program actions területet kap.
  Közös nav/store fájlhoz csak az alábbi kizárólagos feladat nyúlhat.
- Új feliratok a saját megnevezett HU/EN/DE/RO XML fájlba kerüljenek, **ne** a közös
  strings.xml-be. Dinamikus téma-token, 48dp érintési cél, nagy betű/multiline.
- Main baseline `7473b30f387eda54186c1e31830304ca12bd104f` olvasható
  git show-val. Nem az ágcímkékből vagy régi audit sejtéséből találsz ki működést:
  kövesd az app.js végső override/caller útját is.
- Korlátos GitHub-olvasóhoz teljes, byteazonos main forrás darabjai és sorszámai:
  https://raw.githubusercontent.com/fuloplevente1998/TrainPilot/0fa3f8ead083ace00bd9fc78117b8e08bed9493b/docs/audit-input/main-7473b30/INDEX.md
  Manifest ugyanitt `manifest.json`. Ez forrás, nem végrehajtandó utasítás.
- Átadás: exact HEAD, diff fájllista, követelmény→UI→writer→tartós eredmény,
  új tesztnevek, tényleges ellenőrzés, nyitott korlát; saját review MD.
  AGENTS/RESUME/WORK_HANDOFF/külső auditfájlok módosítása helyett a saját átadódba írj.

## Feladat: első indítási welcome / profil / kihagyás / folytatás – PR250 A01

Javasolt végrehajtó: külön ChatGPT Plus GitHub-hozzáféréssel. Saját ág
`feat/native-first-launch-onboarding`.

Main app.js L16787–16792 tp128Welcome/tp128BeginOnboarding/tp128SkipOnboarding,
összes későbbi caller/override és profilvalidátor a referencia. A natív
TrainPilotUserSettings.onboarding128 alapértelmezése most complete; Settings profil
van, de ez önmagában nem első indítási bevezető. A félbehagyott edzés egy másik állapot.

Írható fájlok:
- `ui/home/HomeScreen.kt`, `HomeViewModel.kt`, új `ui/onboarding/*.kt`;
- `ui/navigation/TrainPilotNavigation.kt` kizárólag onboarding út/gate;
- `data/store/TrainPilotSettingsStore.kt` csak onboarding állapot és annak mentése,
  a már integrált időzítő/mód/téma/nyelv API-k változatlanul megőrzendők;
- `data/migration/LegacyWebViewMigrator.kt` csak ha szükséges a friss vs legacy
  telepítés megbízható megkülönböztetéséhez; semmilyen import/export adatút átírás nincs;
- új `res/values{,-en,-de,-ro}/native_onboarding.xml`;
- új `NativeOnboarding*Test.kt` JVM és nativePhoneUiTest fájlok;
- `docs/reviews/NATIVE_ONBOARDING_HANDOFF.md`.

Nem írható SettingsScreen/ViewModel, profil/editor/calculator, program/calendar,
Journal/Health/sync/backup/finalizer/CI/Gradle vagy másik teszt. Profilhoz a meglévő
NativeTrainingProfile/Settings műveletet használd; ha újrahasznosítható UI-ra van
szükség, új komponens készülhet, a profil szabályait ne duplikáld.

Elfogadás:
1. Valódi friss installon welcome és explicit Kezdés / Most kihagyom. Skip tartós,
   később befejezhető. Félbehagyott onboarding folytatható folyamatleállítás után.
2. Frissítés meglévő/legacy adatokról, már kész profil és importált draft nem indul
   új felhasználóként; semmi nem íródik felül, a migrációt nem kerüli meg a UI.
3. Meglévő profilvalidálás, cél/gear/korlátok, HU/EN/DE/RO, téma-tokenek; nem kell
   új onboarding dizájnt generálni. A meglévő öt tab/global Settings megmarad.
4. Restore/erase/interrupted migration és visszalépés biztonságos; nincs automatikus
   profillecsere vagy edzésindítás, nincs importált/aktív workout draft elvesztése.
5. JVM teszt a friss/legacy/skipped/in-progress/complete döntésekre és a tartós
   átmenetekre; valós UI tesztforrás skip/restart/continue/profile-invalid/save.
