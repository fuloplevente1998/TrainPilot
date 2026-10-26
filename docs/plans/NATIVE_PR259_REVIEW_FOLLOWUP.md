# PR259 – célzott második review-kör

Repo fuloplevente1998/TrainPilot, saját ág fix/native-training-mode-approval-ui.
Review exact HEAD: 9b1a2e2265117825d9b5c7d4396d61639a8855ba.

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

A program dialog/összehasonlítás tulajdonosának kivitele a lazy item életciklusából,
a teljes before/after identity, a pending fogyasztása és a külön összehasonlítási
hibaállapot jó irány. A korábbi timeout kizárólagos okát nem bizonyítja új runtime.

## Javítandó: ellenőrzés és írás közti verseny

TrainingProgramsViewModel.approveTrainingModeCompared jelenleg:
  isCurrent(context, preview, snapshot) && NativeTrainingModeChange.approve(...)
Az első művelet elvégzi a program raw/day ellenőrzést, de a második csak később
lép be a metadata-admission/CAS-ba. A kettő között egy szabályos programszerkesztés
módosíthatja a recepteket. A settings raw/activeProgram CAS ezt nem feltétlen
veszi észre. Tehát az új programrecept-guard jelenleg nem egy védett művelet a mentéssel.

1. A read-only revalidáció és meglévő approve közös NativeDataReplacement.mutate
   alatt fusson a megengedett VM-fájlban. Olvasd el a mutate implementációját:
   coroutine Access alapján reentráns, ezért az approve belső mutate-ját ne
   cseréld saját writerre és ne készíts új mutexet. Ellenőrizd a programírók
   ugyanilyen metadata lease használatát; ha valamelyik nem így ír, konkrétan
   jelöld a korlátot, ne ígérj globális atomikusságot.
2. A snapshot ne élhesse túl egy adatcsere epochját úgy, hogy restore után azonos
   raw/settings miatt ismét érvényesnek látszik. A meglévő Continuation adatgeneráció-
   kötését használd az engedélyezett Comparison Snapshot/VM útban, ne sémát változtass.
3. Új kontrollált regressziók: program raw/day módosítás a comparison után;
   ugyanilyen bytesra restore/generációváltás; active/paused workout tiltás;
   dupla approval. Nincs settings/recept írás elutasításkor. Kontrollált coroutine/
   barrier sorrend, nem időzítésre épülő sleep.
4. A meglévő három új tesztet és a négy eredeti módválasztó UI tesztet tartsd meg.
   Külön ellenőrizd a globális picker előnézet/cancel/approve/undo ágát is.
   A program öröklés visszaállítása továbbra külön összehasonlítás/jóváhagyás;
   teljesen üres vagy hibás comparison nem teheti feltétel nélkül aktívvá a gombot.
5. Az új snapshot program recipe identityjét teszteld a UI-n keresztül is, ne csak
   kézzel összeállított objektum isCurrent hívásával. Ha Android runner hiányzik,
   a tesztforrás készüljön el és a runtime maradjon egyértelmű UNRUN.

Nincs SettingsStore/NativeTrainingModeChange/motor átírás; a reentráns külső lease
és a Continuation kötés a most engedélyezett fájlokban megoldandó.
Ezután exact új SHA, célzott tesztlista és aktualizált saját report; draft marad.
