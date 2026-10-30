# PR256 visszaellenőrzés, javítás és célzott tesztelés

Repository: fuloplevente1998/TrainPilot. Kizárólag meglévő saját ág:
`feat/native-gps-permission-recovery`, meglévő draft PR **#256**.
Átadott HEAD: `a3ee639cfbbce5251c89ff6d9f8a6148860ff0ce`.
Ne kezdj új ágat, ne add át újra változatlanul a korábbi statikus jelentést.

Olvasd AGENTS.md-t és saját docs/reviews/NATIVE_GPS_PERMISSION_RECOVERY_HANDOFF.md-t.
A mostani kérés: független szemmel ellenőrizd, reprodukálható hibát javíts, és futtasd
az eddig csak megírt célzott teszteket. A forrásszintű jelenlét nem runtime PASS.

## Szigorú határ

Main ÉS feat/native-android-migration kizárólag olvasható. Nem pusholhatsz rájuk,
nem merge/rebase-elsz róluk vagy rájuk, nincs force push, verzió/csomag/séma/dependency
változtatás, PR/issue zárás vagy GitHub-komment. PR256 draft marad. Minden saját
javítás csak saját ágra, a meglévő GPS recovery fájlhatáron; root a review/integrátor.
Nincs Wear/GPS-motor/útvonal/sync/backup/Settings/NavHost átírás vagy új rendszer.

## Ellenőrzés és javítás

- A tényleges permission/service állapotot olvasod, approximate nem precise.
- Denial/permanent denial és központi helyszolgáltatás OFF külön állapot/gomb.
- Activity/Settings ON_RESUME nem indít mérést. Csak explicit retry, eredeti stabil
  workout/generation/exercise/set cél és aktuális phone-owner mellett.
- Permission dialog közben edzésváltás/finish/pause/handoff/restore: régi callback
  nem indíthat új célon mérést. Back/cancel/deny nem hívhatja a GPS writert.
- Forgatás/recomposition/gyors ismételt Retry nem dupláz launchert/mérést;
  missing Activity és beállítási intent handler hiba nem okoz összeomlást.
- HU/EN/DE/RO, 48dp/nagy betű, meglévő tokenek; nem fordítatlan SDK-hiba a UI-n.

## Tényleges futtatás: saját ág, GitHub

1. Kotlin + phone debug és Android test-APK fordítás.
2. Célzott JVM: NativeGpsPermissionRecoveryDecisionTest mind11 esete.
3. Izolált API35 emulátoron kizárólag NativeGpsPermissionRecoveryUiTest mind4 esete.
   Ne rövidíts assertiont, ne hagyj ki tesztet, ne állíts át fail→skipre.
4. Ha nincs megfelelő szűrhető workflow, EHHEZ a QA-folytatáshoz engedélyezett egy
   ÚJ, CSAK workflow_dispatch-os `.github/workflows/native-gps-recovery-qa.yml`
   a saját ágon, az eredeti toolchainnel. Ne módosítsd a canonical native-android.yml-t,
   ne adj push/PR trigger-t, ne indíts teljes közös gate-et, Wear/R8/upgrade sorokat.
   A QA workflow külön reviewer-fájl, root nem köteles a migrációba átvenni.
5. A targeted Gradle filter pontos osztálynév; csak azt a JVM/UI osztályt futtatod,
   nem az összes75korábbi UI tesztet. A régi teljes gate baseline-hibáit nem javítod
   scope-on kívül. Root aktuális UI-javításait nem merge-eled be.
6. Artifact: XML / teljes instrumentation log / logcat / pontos commit / tesztszám;
   ha kép készül, valódi adb screencap az éppen futó appból. Nincs mockup.
   Debug APK csak izolált teszttelepítéshez; nem eredeti release fölötti frissítés.
   A felhasználó nem törli a meglévő appját. Saját adatos installt ne hajts végre.

Ha hiba van, konkrét okot javíts, célzott kaput újra ugyanazon végső commiton.
Átadás: exact HEAD, új diff a fenti SHA óta, konkrét futási link és artifact,
11JVM/4UI tényleges eredménye (vagy hiányának oka), nyitott fizikai GPS/OEM teszt.
Frissítsd a saját átadót és PR256 leírását. Nincs merge vagy készülékes PASS ígéret.
