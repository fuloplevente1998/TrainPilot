# Többhetes naptár – PR252 átadás

Base: `7043fc7d5fac91383f583332fd8fc0e9397f4a1c`. Saját ág: `feat/native-calendar-multiweek`.
Draft PR: https://github.com/fuloplevente1998/TrainPilot/pull/252
Kód/teszt HEAD: `a4ef63d3de1ae84102adf392e54a6c4f59b920f8`. E riport utána docs-only commit;
a végső HEAD és a távoli egyezés a PR-leírásban szerepel.
Cél: feat/native-android-migration. Main/migration push, merge, force-push,
komment, CI-dispatch/cancel/rerun, verzióemelés nem történt.
Csomag/verzió változatlan: com.repforge.app /1.3.71-native-test /2736.

## Forrásból igazolt main szerződés

Main7473b30f387eda54186c1e31830304ca12bd104f:www/app.js végső
rf2211PlanWeeks caller L13063–13075 és TrainPilot110Core.planWeeks L15651–15683.
Nincs későbbi planWeeks motorfelülírás. daily/alternate/weekly, 1–12 hét,
10–240 perc, első index és ciklikus programnapok. A custom mód kézi naptáras
kijelölés, nem negyedik automatikus mód; a meglévő egyesemény-editor megmaradt.
A caller replaceOtherPrograms:true értékkel hív; ezt követi a natív tervező.
A teljesített/aktív/rest foglalt napok kimaradnak, nem léptetik a programnapciklust.
Megmaradó dátumokon az ID megmarad, más dátumokra a maradék ID-k kerülnek;
a fölösleges tervek cancelled tombstone-ok, nem fizikai törlések.

## Követelmény → UI → writer → tartós eredmény

Az alábbi szimbólumok mind a fenti exact kód-HEAD-ben találhatók.

1. CalendarScreen többhetes gomb → CalendarPlannerDialog napi/váltott/heti,
   hétköznap, dátum/idő, időtartam/hetek/első nap → NativeCalendarPlanner.plan.
   Legfeljebb 84 helyi dátum; programnapok sortOrder szerint ciklikusak.
   Helyi dátumot léptetünk, nem 24 órás időtartamot. Átfedő őszi időnél a Java
   ZoneId korábbi offsetje; nem létező tavaszi idő elutasítva a meglévő natív
   Calendar-editor szabályával. A JS ilyen gapet előretolna: tudatos natív
   biztonsági eltérés, nem byteazonos DST-gap paritás.
2. Előnézet → CalendarViewModel.previewPlanner → repository.preview csak olvas.
   Látható dátum/idő/programnap lista, alkalmak és törlendők száma. Mégse nem ír.
   Mezőváltozás után a korábbi előnézet nem jóváhagyható.
3. Jóváhagyás → VM.applyPlanner → eredeti NativeDataReplacement.mutate lease,
   megnyitáskori continuation.requireCurrent és teljes SettingsStore identity
   összehasonlítás → repository.applyPreview Room-tranzakció. Újraolvasott
   program/days, plannerSettings dokumentum, range schedule, history és védett
   ID-k egyezése kell. Stale esetben a meglévő hibajelzés után új előnézet kell.
   Ütemezés és plannerSettings egy tranzakcióban íródik; nincs sémamódosítás.
4. Repository preview védi az active/paused/finalization scheduleId-t és minden
   hasScheduleHistory rekordot; completed/rest események foglalják a napot.
   A natív megőrzési szabály konzervatívabb a JS done-set szűrésénél: bármely
   mentett history sor foglal/protektál. Raw ismeretlen mezők és managed Google
   identity megmaradnak; removed sorok tombstone-ok. Ugyanazon preview retry
   nativePlannerReceipt alapján nem ad új eseményeket. Régi cancelled sor nem
   kerül a mozgatható sorba, és nem éled fel. Nincs második naptármotor/adattár.
5. Beállítások → meglévő kv_documents/plannerSettings → VM init és következő
   megnyitás visszatölti az értékeket. A megnyitás addig tiltott, amíg a mentett
   konfiguráció be nem töltődött. A meglévő hónap observer látja a mentett sorokat;
   az eredeti CalendarScreen onStart és NativeCalendarRepository.requireStart
   út változatlan. Az egyesemény-editor továbbra elérhető.
6. Új NativeCalendarPlannerTest: 11 JVM/Room/Robolectric eset; új
   NativeCalendarPlannerUiTest: 1 valódi Calendar→VM→Room Compose scenario.
   Ezek megírt tesztek, **UNRUN**, nem fizikai vagy emulátoros PASS.

## Megírt tesztek (nem futottak)

Négyhetes ciklus; alternate/weekly helyi napok; preview/cancel/save/reopen/retry;
stale schedule; tranzakciórollback kv-write hibánál; őszi DST 25 óra és pontos
45 perc; active/completed napvédelem; stale plannerSettings; stale program;
ID/Google identity megőrzés és cancellation tombstone; tavaszi DST-gap.
Compose: valódi képernyőn előnézet, Mégse, új előnézet, mentés és újranyitás.
A UI-teszt futtatása elkülönített tesztadatbázisú emulátoron szükséges, mivel
valódi tervet ment. A meglévő közös fixture-t és teszteket nem módosítottuk.
Restore-fence és globális Settings-váltás integrált futási bizonyítéka még nincs;
a védelmük kódban a meglévő continuation/lease és settings-CAS útját használja.

## Tényleges ellenőrzés és korlátok

- Base diff --check: statikusan rendben.
- 4 locale XML parse és 14 erőforráskulcs egyezése: statikusan rendben.
- Fájlhatár ellenőrizve; nincs DAO-/modell-/SettingsStore-/nav-/workflow-módosítás.
- A valódi pinned main features-110.js pure core Node vm alatt futott Budapest
  zónában: daily28, alternate14, weekly12 és B első programnap rotációja rendben.
  Ez JS-referencia, nem Kotlin runtime/fordítás vagy teljes paritásteszt.
- Nem futott Gradle, Kotlin-fordító, JVM, Compose, emulátor, teljes CI vagy APK.
  Kódmegvalósítás és tesztforrás kész; runtime/fordítás elfogadása PENDING.
- Nincs új history query: meglévő dátumintervallumos CalendarHistorySummary
  projection és célzott EXISTS schedule-history vizsgálat. Nincs set/photo/route
  részletbetöltés. A legfeljebb 12 hetes naptárintervallumot olvassuk.
- A meglévő általános calendar hibaablak jelzi a stale/hibás esetet. A felhasználó
  új előnézetet kérhet; a régi adatok nem íródnak felül. Érintési célok min48dp,
  új feliratok külön HU/EN/DE/RO XML-ben, dinamikus téma-tokenekkel.

## Teljes diff fájllista

- `android/app/src/main/java/com/repforge/app/ui/calendar/CalendarScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/calendar/CalendarViewModel.kt`
- `android/app/src/main/java/com/repforge/app/ui/calendar/NativeCalendarPlanner.kt`
- `android/app/src/main/java/com/repforge/app/ui/calendar/NativeCalendarRepository.kt`
- `android/app/src/main/res/values-de/native_calendar_planner.xml`
- `android/app/src/main/res/values-en/native_calendar_planner.xml`
- `android/app/src/main/res/values-ro/native_calendar_planner.xml`
- `android/app/src/main/res/values/native_calendar_planner.xml`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeCalendarPlannerUiTest.kt`
- `android/app/src/test/java/com/repforge/app/NativeCalendarPlannerTest.kt`
- `docs/reviews/NATIVE_CALENDAR_MULTIweek_HANDOFF.md`

## Integrátori következő lépés

Exact diff review, különösen main vs natív DST-gap eltérés és a konzervatív
history-foglalás; utána közös canonical fordítás/JVM/Compose futás a fő
integrátornál. A 11+1 teszt XML fail/error/skip eredményét ellenőrizni kell.
Nincs zöld CI vagy készülékes elfogadás állítva. Main merge és release nem része
ennek az átadásnak.
