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

## Feladat: programmásolás és gyári visszaállítás forrásigazolt paritása – PR250 A07

Javasolt végrehajtó: külön ChatGPT Plus GitHub-hozzáféréssel. Saját ág
`feat/native-program-actions-parity`.

Először célzottan bizonyítsd a main tényleges felhasználói másolás/reset műveleteit.
Az audit A07 nem állítja, hogy a teljes programeditor hiányzik: create/edit/delete,
builtin-copy-on-edit már működik; NativeProgramEditor.save(clone=true) megvan.
Main app.js tp106SaveProgram L15131 körül, resetPlan L1022 és L6237 körüli wrap,
programnap gyári visszaállítás és későbbi override/caller a referencia.

Írható fájlok:
- `NativeProgramEditor.kt`, új `NativeProgramActions*.kt`;
- `ui/programs/TrainingProgramsScreen.kt`, `TrainingProgramsViewModel.kt`,
  `ProgramEditorDialog.kt` (csak program action UI);
- új `res/values{,-en,-de,-ro}/native_program_actions.xml`;
- új `NativeProgramActions*Test.kt` JVM és nativePhoneUiTest fájlok;
- `docs/reviews/NATIVE_PROGRAM_ACTIONS_HANDOFF.md`.

Nem írható profil/plannergenerátor/prescription számítás, program/catalog seed,
SettingsStore, Home/NavHost/Calendar, Journal/Health/Wear/sync/finalizer/CI/Gradle,
régi közös tesztek. DAO-only olvasás; ha új íróművelet/API feltétlen kell a meglevő
tranzakcióhoz, előbb a handoffban nevezd meg, ne módosíts önkényesen más felületet.

Elfogadás:
1. Csak forrásigazolt fő app-műveletet pótolj. A nem bizonyított művelet kapjon
   konkrét bizonyítékhiányt; ne találj ki univerzális reset gombot.
2. Másolat saját stabil program/day azonosítót kap, forrás változatlan, nincs
   automatikus aktiválás vagy örökölt sorozatteljesítés/timer/workout state.
3. Visszaállítás előtt konkrét preview/megerősítés: pontosan mely gyári nap/
   recept/gyakorlat érintett. Naplót/fotót/Health/GPS-t nem töröl.
4. Aktív/finalizáló/handoff edzés mellett meglévő guard megmarad. Metadata lease,
   IO→Main snapshot check, programforrás CAS és atomikus Room mentés kötelező.
5. Mégse semmit nem ír; stale forrás és restore után nem írhat régi preview.
   Ismételt Save/retry nem csinál kontrollálatlan duplikátumot.
6. Új JVM és valódi UI tesztforrás a copy/reset/guard/cancel/stale/unknown raw
   mezőmegőrzésre. Régi editor/prescription/HIT-RIR viselkedés változatlan.
