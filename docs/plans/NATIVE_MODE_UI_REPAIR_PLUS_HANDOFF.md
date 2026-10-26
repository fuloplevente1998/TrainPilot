# Chat 2 – Programonkénti edzésmód előnézet/jóváhagyás javítása

Saját ág: `fix/native-training-mode-approval-ui`.

## Cél és ismert hiba

`NativeTrainingModesUiTest.programPickerApprovalOverridesGlobalWithoutChangingProgramRecipes`:
30s timeout kt:159, a `training_modes_program_approve` engedélyezésére várva.
A navigáció már valódi `shortcut_programs` útvonal; ne cseréld nemlétező alsó tabra.

Tárd fel: a program és katalógus beolvasása, comparison/proposal identity, Main/IO
váltás, lazy composition és enabled állapot közül mi akadályozza az előnézetet.
Ellenőrizd a változó raw/settings/program id miatti recomposition/cancellationt.
A hiányos összehasonlításra nem engedélyezheted feltétel nélkül az Approve gombot.
Javítsd a hibát; jóváhagyás kizárólag megfelelő, friss, kész összehasonlításból írhat.

Bizonyítandó: preview/Mégse nem ír; programhit/rir override jóváhagyás után tartós;
a globális default és a másik program öröklése változatlan; program receptek/rawJson
változatlanok; öröklés visszaállítása külön preview+confirmation; stale/active-workout/
restore tiltás és dupla jóváhagyás nem kerülhető meg. Használd a meglévő
NativeTrainingModeChange admission/CAS útját, ne készíts új settings-writert.
A négy meglévő NativeTrainingModesUiTest és a kapcsolódó JVM assertjei maradjanak.

## Írható fájlok

- android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt
- android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt
- android/app/src/main/java/com/repforge/app/ui/settings/NativeTrainingModePicker.kt
- android/app/src/main/java/com/repforge/app/ui/settings/NativeTrainingModeComparison.kt
- android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeTrainingModesUiTest.kt
- Új saját célzott tesztek `NativeTrainingModeApproval*Test.kt` névvel a megfelelő test/nativePhoneUiTest könyvtárban.
- docs/reviews/NATIVE_TRAINING_MODE_APPROVAL_UI_FIX_HANDOFF.md

Minden más csak olvasható: különösen NativeTrainingModeChange, SettingsStore,
SettingsScreen/ViewModel, megosztott fixture, NavHost, program-actions motor, Calendar,
GPS, Wear, workflow-k. A közös picker globális ágát se rontsd el; meglévő globális
preview/cancel/approve/undo tesztek regresszióvédelme kötelező. Új szöveg csak saját
`native_modes_approval_recovery.xml` négy locale-ban; ne módosíts nagy közös szótárat.

Repository: https://github.com/fuloplevente1998/TrainPilot
Indulási forrás: `98ef71b2e4f2fa4f08b415c3d0a94d113b62303d` a `feat/native-android-migration` ágról.

Olvasd az ezen a commiton található AGENTS.md-t, docs/NATIVE_RESUME.md és
docs/WORK_HANDOFF.md aktuális tetejét; utána csak a feladat fájljait.
Új saját ág az exact kiindulási commitból, draft PR célnak
`feat/native-android-migration`. Ne a régi #252/#245 ág folytatása legyen.
Main ÉS migrációs főág csak olvasható: nincs oda push, merge, rebase, force push,
verzió/csomag/séma/dependency módosítás. Nincs PR/issue-zárás vagy kommentelés.
Saját PR leírását és saját handoffját frissítheted. Nincs belső subagent/Codex-indítás.
A másik chat más fájlokon dolgozik; Root a közös integrátor és CI-felelős.

## Valós hibabizonyíték

A közös Actions40317771457 checkoutja e28ab418 volt:
https://github.com/fuloplevente1998/TrainPilot/actions/runs/40317771457
Phone artifact12649155506, instrumentation.txt:75 terminális,35PASS/40FAIL,
0screenshot, no process crash. Ez nem az új forrásra futtatott teszt.
Olvasd a teljes érintett stack-et a nyers artifactból; ha a csatlakozóval nem érhető
el, mondd ki, és dolgozz a lent idézett tünetből és a megadott exact forrásból.
Root már javította a közös screenshot handshaket, néhány lazy scrollt és a
Programs home-shortcut navigációt. Ezeket ne másold vissza régi forrásból.

## Munkamód és tesztelési szabály

Ez javítási feladat, nem újabb általános audit. Keresd a konkrét okot, javítsd a
valós adatúton és/vagy a hibás teszt-előkészítésen. Ne legyen új párhuzamos motor.
Nincs Thread.sleep, timeout puszta emelése, teszttörlés/@Ignore, assert gyengítése,
közvetlen adatbázis-manipulációval kiváltott UI-mentés, vagy hibát elnyelő catch.
Tényleges ready/enabled állapotra várj, LazyColumn esetén a valódi konténert görgesd.
A meglévő assert üzleti jelentését tartsd meg; téves tesztforgatókönyvet indokoltan
javíts, ne a hibás mellékhatást tedd elvárássá.

Köztes commit `[skip ci]`. Ne indíts teljes native-android.yml kaput, új workflow-t,
APK/release vagy más ág futását; azokat Root egy közös forráson koordinálja.
Helyi teljes Gradle/emulátor tilos. Ha már rendelkezésre áll kifejezetten saját ági
célzott GitHub-runner, azon csak az érintett tesztosztályok futtathatók, de ehhez ne
írj main/migration ágat és ne állítsd át a canonical workflow-t. Ha nincs ilyen
runner, készítsd el a tesztforrást és az exact futtatandó osztálylistát, runtime=UNRUN.
A környezeti korlátot ne keverd kódhibával vagy PASS-szal. Statikus ellenőrzés,
Kotlin fordítás, JVM és UI runtime külön státusz legyen.

## Átadás

Saját draft PR, pontos HEAD/indulási SHA, csak engedélyezett fájldelta.
Írj rövid saját docs/reviews/ átadót: reprodukció/gyökérok, javítás, megmaradt guardok,
tesztek pontos neve és tényleges futási bizonyítéka vagy UNRUN. Ne becsülj tesztszámot.
Ha a helyes javításhoz a fájlhatáron kívüli módosítás kell, írd le konkrét fájl/sor/
javasolt patch formájában; ne módosítsd önhatalmúan.
Nincs automatikus merge vagy mainre telepíthetőség állítása. Root review-zik és integrál.
