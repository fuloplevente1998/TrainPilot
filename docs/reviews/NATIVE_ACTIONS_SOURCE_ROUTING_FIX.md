# Natív és WebView GitHub Actions útválasztás – 2026-10-26

A screenshoton látható hiba: actions/setup-node cache npm lockfile-t keres egy
Kotlin/Compose-only ágon. Ez nem végrehajtott app-teszt hibája: a folyamat setupban
leáll, az összes teszt kimarad.

## Gyökérok és változás
A root migration branch név szerinti kizárása nem fedte az abból fork-olt feat/fix
ágakat, és a pull_request/manual trigger is elindíthatta a régi workflow-kat.
A classify-app-source.yml reusable workflow checkout UTÁN választ forrástípust:
- package.json + package-lock.json + www/app.js: legacy npm/Chromium/Capacitor kapu;
- mindhárom hiányzik + migration manifest/state + Compose MainActivity + app Gradle:
  natív forrás, legacy kapu nem alkalmazható, summary a native-android.yml-re mutat;
- vegyes/sérült/hiányos legacy forrás, például csak lockfile törlése: FAIL.
A helper hét policy-selfchecket futtat GitHubon a besorolás előtt. Ez nem natív
app-teszt, és nem ad natív működésre PASS állítást.

11workflow jobjai kaptak app-target függést/guardot: Quick Validation, Phone APK,
Wear round UI/release/build, Phase7, ownership, phone execution, P0, Play bundle és
release-metadata. Eredeti job-függőségek, matrix, script, teszt, keystore/signing,
publikálási feltételek és npm ci változatlanok. A Wear complete always-guardja
natív forrás esetén nem állít hamis hiányzó-shard hibát.
A native-android.yml nem módosult; natív JVM/build/signature/upgrade/UI kötelező marad.

## Ellenőrzési státusz
A módosított források és jobgráf textual review-ja elkészült; a forráspolicy
hét esetének selfcheckje az új helperbe került, runtime GitHubon igazolandó.
Helyi környezet exec-server unavailable, ezért nem futott helyi YAML/actionlint/
Python. Ne állíts ezekre PASS-t. Az új futás ellenőrzése még hátra.
A korábbi runokhoz tartozó piros history nem törlődik és nem színezzük át.

## Integráció és folytatás
Root migration branchön 4db52d97(helper) →a7f0e758(11consumer guard) commitok.
Main/verzió/appforrás nem változott. A 263/264 és más régebbi saját ágak saját
régi checkoutja ettől nem változik meg. A munkás saját ágára kizárólag ezt a
CI-deltát vegye át; saját scoped native-android.yml QA fájlját ne írja felül.
Ne merge-elje a teljes rootot vagy másik munkást csak a lockfile hibáért.
Root később a teljes integrált kaput külön ellenőrzi; natív öt UI-hiba nyitott.

Ha a helyi workspace újra elérhető, először fetch/read a távoli migration HEAD:
a connectoron létrehozott commitokat ne írja felül régi helyi checkoutból.
