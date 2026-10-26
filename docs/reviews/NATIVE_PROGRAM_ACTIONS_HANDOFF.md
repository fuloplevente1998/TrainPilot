# Native Program Actions – külön átadás (PR250 A07)

Dátum: 2026-10-26. Saját fejlesztési ág: `feat/native-program-actions-parity`.
**Exact pinned base:** `7043fc7d5fac91383f583332fd8fc0e9397f4a1c`.
**Kóddelta HEAD az átadódokumentum megírása előtt:** `2a069ac6d1899500070473e5bb23e7df057fd878`.
Cél PR-base: `feat/native-android-migration`. Sem main-, sem migration-írás/merge, rebase, force push vagy teljes CI nem történt.
Csomag/verzió maradt `com.repforge.app`, `1.3.71-native-test`, `2736`.

## Main bizonyíték és a tudatosan nem portolt út

- `7473b30f387eda54186c1e31830304ca12bd104f:www/app.js`, byteazonos pinned
  split: `docs/audit-input/main-7473b30/app-part-070.txt` eleje,
  `tp106SaveProgram` (eredeti kb. L15131): új saját programot hoz létre új
  `custom-${crypto.randomUUID()}` azonosítóval; ez **create**, nem bizonyított
  tetszőleges programot duplikáló végső UI-caller. A natív saját program létrehozása
  már létezett, builtin-copy-on-edit már működött. Új, bizonyíték nélküli általános
  duplikáló gomb nem készült.
- `app-part-011.txt` `rf153ResetDay(w)` (eredeti kb. L1905–1959):
  a **régi** `plan().A/B` tömböt tölti fel `DEFAULT_PLAN`-ből
  (a `dead-bug` helyett `crunch`), megerősítéssel.
  `app-part-029.txt` későbbi `tp2629Confirm` wrapper
  megőrzi a Full Body A/B napra korlátozást.
- `app-part-005.txt` `resetPlan` (eredeti kb. L1022):
  egyszerre visszaírja a **globális gyakorlatkönyvtárat** és az
  `home-basic` builtint, a naplót megőrzi; `app-part-029.txt`
  megerősítő wrap. A végső Settings-caller és az összes későbbi
  override útja nem bizonyított teljesen. A mai natív gyári catalog
  védett; a reset teljes katalógus-módosítása az írási szerződésen kívül van.
  **Nem** készült univerzális / globális reset.

## Követelmény → UI → writer → tartós eredmény

| Követelmény | UI/entry | Writer, CAS és védelem | Tartós eredmény |
|---|---|---|---|
| Forrásigazolt másolási viselkedés | Meglévő `program_edit_<id>` / builtin „copy & edit”; nincs új univerzális gomb | `TrainingProgramsViewModel.saveProgram` egy megnyitásra állandó `actionToken`-t ad át a `NativeProgramEditor.save`-nek; új UUID alapú program ID és `programId:dayId` összetett kulcs, Room tranzakció | Egy saját program; eredeti builtin érintetlen, új másolat önálló program/day azonosítókkal és `sourceTemplateId`-vel; nincs automatikus aktiválás vagy edzésállapot-öröklés |
| Ismételt mentés/retry | Azonos szerkesztőablak ugyanazon request tokennel | A token a `rawJson.nativeActionToken` mezőben, tranzakción belüli egyező mentés visszaadja az előző program ID-t | Nem keletkezik újabb példány ugyanazon request retry-jára |
| Kizárólag igazolt gyári A/B nap | Saját, `home-basic` sablonból másolt program A/B napján `program_restore_<programId>_<A/B>` | `NativeProgramActions.preview`: konkrét programnap, eddigi/gyári sorrend, érintett receptazonosítók, több napra kiható recept figyelmeztetés | Előnézet **nem ment**, más programokon nincs resetgomb |
| Megerősítés / Mégse | HU/EN/DE/RO `native_program_actions.xml`; `program_restore_confirm/cancel` | Explicit felhasználói jóváhagyás után `NativeProgramActions.restore` → egyetlen meglévő `NativeProgramEditor.save` Room tranzakció; Mégse bezár és nem ír | A kiválasztott A/B nap gyári elemei, kizárólag a hozzá tartozó szerkeszthető receptmezők állnak vissza; más napok és napló/fotó/Health/GPS adatok nem törlődnek |
| Stale/guard | Hiba marad a preview-n, újranyitás szükséges | `NativeDataReplacement.mutate`, replacement epoch ticket, `opened` és factory `ProgramWithDays` teljes CAS tranzakción belül, Main thread workout snapshot és ownership handoff/finalizer guard előtte **és** commitnál | Régi preview nem írhat restore vagy programváltozás után; aktív/finalizáló/átadó workout blokkol |
| Ismeretlen raw mező megőrzése | Régi név/nap/gyakorlat mezőkhöz nincs külön UI | Szerkeszthető receptkulcsok: `sets,reps,weight,rest,trial`; ismeretlen recept-kiterjesztések és nem célzott program/day raw mezők érintetlenek | Meglevő custom program és más nap raw mezői megmaradnak; nem hozunk létre második adatmotort |

A napi gyári visszaállítás **szűkített natív megfelelő**: a tényleges main
`rf153ResetDay` legacy `plan` adatra ír, nem egy saját Room programra;
a teljes main `resetPlan` globális gyakorlatkönyvtár-írása nem szerepel.
Ez **nem állítható teljes A07 main-paritásnak** készülékes és végső caller audit nélkül.

## Fájldelta (csak kijelölt fájlok)

1. `android/app/src/main/java/com/repforge/app/NativeProgramEditor.kt`
2. `android/app/src/main/java/com/repforge/app/NativeProgramActions.kt` (új)
3. `android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsScreen.kt`
4. `android/app/src/main/java/com/repforge/app/ui/programs/TrainingProgramsViewModel.kt`
5. `android/app/src/main/res/values/native_program_actions.xml` (új)
6. `android/app/src/main/res/values-en/native_program_actions.xml` (új)
7. `android/app/src/main/res/values-de/native_program_actions.xml` (új)
8. `android/app/src/main/res/values-ro/native_program_actions.xml` (új)
9. `android/app/src/test/java/com/repforge/app/NativeProgramActionsTest.kt` (új)
10. `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeProgramActionsUiTest.kt` (új)
11. Jelen átadó `docs/reviews/NATIVE_PROGRAM_ACTIONS_HANDOFF.md` (új)

## Új tesztforrások – **megírva, nem futtatva**

JVM / Robolectric (`NativeProgramActionsTest`, 6):
- `repeatCopyRequestReusesItsOwnIdentityAndNeverActivatesSourceOrCreatesWorkoutState`
- `previewAndCancelAreReadOnlyAndShowOnlyTheFactoryABDay`
- `restoreSelectedDayAndRecipePreservesOtherDayAndUnknownRawFields`
- `sourceProgramChangeAndFactoryChangeBlockStaleRestoreWithoutWrite`
- `activeWorkoutAndReplacementFenceBlockRestore`
- `unrelatedCustomProgramCannotClaimFactoryDayRestore`

Valódi natív Phone UI (`NativeProgramActionsUiTest`, 3):
- `builtinCopyOnEditPublishesExactlyOneCustomProgramWithoutActivatingIt`
- `cancelPreservesDayThenConfirmedResetPersistsOnlyChosenDay`
- `staleProgramAfterPreviewIsRejectedAndLeavesLaterEditUntouched`

## Valóban elvégzett ellenőrzések és nyitott korlátok

- A pinned GitHub ref és exact main split célzottan elolvasva.
- GitHub compare: a fenti kóddelta a pinned base-hez képest 18 `[skip ci]`
  commitban, 10 kijelölt kód/resource/teszt fájlban; nem érintett shared
  Store/DAO/Gradle/workflow/CI/finalizer/seed/Settings/NavHost.
- 4 locale XML-ben **9/9 azonos kulcs**, minden képernyőbeli
  `R.string.native_program_actions_*` hivatkozás definiált, kijelölt fájlhatár egyezik.
  Ez **statikus R-hivatkozás- és tartalomvizsgálat**, nem aapt/Kotlin compiler.
- **UNRUN:** Gradle compile, JVM, instrumentált UI/emulátor, teljes CI,
  APK/release, fizikai készülék, backup/restore end-to-end.
  Ezek a fő integrátorhoz tartoznak; a megírt teszteket nem jelöltük PASS-nak.
- **Nyitott / részleges:** nincs forrásigazolt tetszőleges program duplikáló main
  UI-gomb; a régi Settings reset pontos utolsó UI-caller útja és a globális gyakorlat-
  reset a saját írási határon kívül; a gyári nap restore csak a
  `home-basic`-eredetű **saját** A/B példányra elérhető; nem töröl naplót.
- További review során ellenőrizendő: meglévő editor/prescription/HIT-RIR
  regresszió, programnap/nyers JSON portable export–reimport, metadata IO→Main
  deadlockmentesség valós integrált CI-ben, és a 48dp/nagy betű/multiline
  tényleges emulátoros elrendezése.

**Integrátori teendő:** exact diff review, a meglévő közös natív gate futtatása,
hibák javítása, készülékes elfogadás és kizárólag külön jóváhagyott merge.
