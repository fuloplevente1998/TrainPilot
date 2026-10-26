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

## Feladat: többhetes naptártervezés – PR250 E02

Javasolt végrehajtó: külön Codex. Saját ág `feat/native-calendar-multiweek`.

A natív Calendar jelenleg egy eseményt szerkeszt. Valósítsd meg a main tényleges
TrainPilot110Core.planWeeks paritását: napi/váltott/heti mód, kijelölt hétköznapok,
kezdőnap/idő, időtartam, hetek, első programnap és programnapok ciklusa.
Először forrásból tisztázd a pontos támogatott módneveket, korlátokat, ütközés/
csere/szabadnap/completed/aktív nap kezelését; ne az audit megfogalmazását másold.

Main caller: app.js L13063–13075 rf2211PlanWeeks, tényleges motor L15651 körül
planWeeks és későbbi felülírásai. A tervbeállítások mentése is része a műveletnek.

Írható fájlok:
- `ui/calendar/NativeCalendarRepository.kt`, `CalendarViewModel.kt`, `CalendarScreen.kt`
  a `android/app/src/main/java/com/repforge/app/` alatt;
- új `NativeCalendarPlanner*.kt` ugyanebben a calendar package-ben;
- ha kell, a meglévő CalendarSchedule DAO kizárólag szükséges célzott tranzakciós
  API-ja; új adatbázis/séma/migráció nincs. Meglévő key-value dokumentum használható.
- új `res/values{,-en,-de,-ro}/native_calendar_planner.xml`;
- új `src/test/.../NativeCalendarPlannerTest.kt`, új `src/nativePhoneUiTest/.../NativeCalendarPlannerUiTest.kt`;
- `docs/reviews/NATIVE_CALENDAR_MULTIweek_HANDOFF.md`.

Más DAO/adatmodell, SettingsStore, programeditor, Home/NavHost, GoogleSync,
Health/Journal/GPS/finalizer/workflow/Gradle és meglévő közös teszt nem írható.
Új query szükségességét igazold; nincs teljes history/részletbetöltés.

Elfogadás:
1. Négy hét terv több programnap ciklusával; pontos helyi dátum/idő és DST-kezelés.
2. Végrehajtás előtt áttekinthető preview, Mégse nem ír; explicit jóváhagyás atomikus.
3. Tervbeállítás és ütemezés egy Room tranzakcióban, az eredeti metadata lease alatt;
   stale program/settings/schedule preview vagy replacement után újratervezést kér.
4. Aktív/finalizáló és befejezett esemény nem sérül. Törlés/skip és távoli Google
   identity/tombstone megmarad; nincs dupla esemény ismételt Save/retry után.
5. Újranyitáskor a terv látszik és indítható; a jelenlegi egyesemény-editor megmarad.
6. Új célzott JVM és valódi Compose UI tesztforrás: mode/rotáció, ütközés,
   stale/rollback, DST, cancel/save/reopen. Ne lazíts régi tesztet.
