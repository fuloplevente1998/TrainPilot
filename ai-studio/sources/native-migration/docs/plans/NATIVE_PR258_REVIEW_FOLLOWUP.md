# PR258 – célzott második review-kör

Repo fuloplevente1998/TrainPilot, saját ág fix/native-calendar-planner-ui-readiness.
Review exact HEAD: b5cefd4e776f830c2b15745462f82a422f5aacf4.

Saját meglévő ág és PR marad; main és feat/native-android-migration csak olvasható.
Nincs merge/rebase/force push, verzió/séma/dependency/workflow módosítás vagy más
PR lezárása. Az eredeti fájlhatár érvényes; ne nyúlj közös fixture/NavHost fájlhoz.
A root e12ed8a5 commitban már feltöltötte a közös fixture két hiányzó Compose
importját (performScrollTo/assertIsDisplayed). Ne másold át az egész fixture-t;
az önálló ág fordításakor ezt a baseline-hiányt külön nevezd meg, és szükség
esetén csak a kétimportos javítás célzott átvételét kérd Root-tól.
Ne indíts teljes CI-t vagy új workflow-t. A közös Android runtime Rootnál,
ha nincs saját rendelkezésre álló célzott runner, runtime=UNRUN, nem PASS.
Kis [skip ci] commitok, exact új HEAD, diff az alábbi review SHA óta és aktualizált
saját handoff/PR-leírás szükséges. A forrásbeli javítás nem futtatott bizonyíték.
A meglévő teszteket ne gyengítsd, ne töröld; nincs sleep/timeout-emelés mint javítás.

## Ami rendben van a forrásreview szerint

A plannerLoaded + nem üres programnapok gate, érvényes program/elsőnap fallback,
tesztizoláció és a Mégse/mentendő4hét különválasztása jó irány.
A repository CAS-t és a tényleges UI→VM→Room utat megtartottad.

## Pótolandó védelem és célzott tesztek

1. CalendarViewModel.cancelPlanner csak a már elkészült preview-t/continuationt
   törli. A previewPlanner korábban elindult repository.preview hívása később
   visszatérhet, és Mainen újra publikálhatja a megszakított preview-t.
   Nincs kérésazonosság/epoch összevetés a késői publish előtt.
   Ez forrásból azonosított VM-interleaving, nem emulatoron reprodukált UI-hiba:
   a busy UI-gate-et ne bontsd le, és ne állítsd, hogy normál letiltott gombbal
   már bizonyítottan kiváltható.
   Adj preview-request identity/invalidálási védelmet cancel/új kérés/VM-clear
   esetére, és kontrollált sorrendű regressziót: olvasás indul → cancel →
   késői eredmény érkezik → nincs preview és nincs alkalmazható régi continuation.
   A mostani cancelInvalidatesPreviewBeforeAnyLateApply már kész preview UTÁN
   cancelál; ezt tartsd meg, de nem fedi a folyamatban lévő kérés esetét.
2. A settings/program betöltés még nem kész állapotát ténylegesen fedje teszt:
   a létező, de letiltott gomb nem nyit dialogot; kész állapot után egy explicit
   kattintás megnyitja. Nincs null/üres programmal történő mentés.
   A törölt/rövidített korábbi program fallbackjét is ellenőrizd.
3. Meglévő11JVM és három új UI metódus státusza továbbra UNRUN. Új tesztkód,
   fordítás és tényleges futás külön státusz, konkrét osztály/metóduslista.
   A pontos Slot- és settings-egyezés, duplicateApply és új Activity/VM roundtrip
   assertjei maradjanak. Mentés hibájánál ne tűnjön el sikeresként a dialog.
4. A harness rows(from)45napos olvasási ablaka a jelenlegi4hetes próbához elég;
   ha bővíted több hétre, a preview teljes időtartamára ellenőrizz, ne legyen
   a tesztből kimaradó későbbi sor.

Kizárólag a saját Calendar-fájlok és saját teszt/handoff módosítható.
Ezután pontos új SHA-val add vissza Rootnak; ne merge-elj.
