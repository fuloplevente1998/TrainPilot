# Native Actions source-routing verification

## Rögzített munkaterv

### Kiindulási ág

A közös natív migrációs ág:

`feat/native-android-migration`

A workflow-routing kiindulási commitja:

`a075a21956ea4bd2f1089a9bc106dbe7a598c264`

A `main` ágat ez a munkacsomag nem módosítja.

### Ellenőrző ág

`fix/native-actions-source-routing-verification`

Draft PR: #265 → `feat/native-android-migration`

A G10 külön marad:

- ág: `feat/native-progress-photo-range-binding`
- draft PR: #264
- tesztelt commit: `0ad6698641b2717973febf42c1b70740b4b0ee7d`

A CI-routing munka nem írhatja felül a G10 tesztelt forrását vagy saját scoped workflow-ját.

## Gyökérok

A korábbi piros futások:

- Quick TrainPilot Validation: `40389899406`
- Phone Release Gate + TrainPilot APK: `40389895449`

A legacy npm/Chromium workflow-k natív Kotlin/Compose forráson is elindultak, majd az `actions/setup-node` npm-cache beállítása root lockfile-t keresett. A natív migrációs forrásban szándékosan nincs `package.json`, `package-lock.json` és `www/app.js`, ezért ezek setup-hibák voltak, nem alkalmazásteszt-hibák.

## Bevezetett routing

A `.github/workflows/classify-app-source.yml` a checkout után forrástípus szerint választ:

- teljes legacy WebView/npm forrás → legacy gate;
- teljes natív Kotlin/Compose migrációs forrás → legacy npm/Chromium/Capacitor gate-ek skip;
- kevert vagy hiányos forrás → fail.

A policy self-check 7 esetet fed le.

11 legacy workflow kapott `app-target` függést és `legacy == 'true'` guardot:

1. Quick Validation
2. Phone Release Gate + TrainPilot APK
3. Play Bundle
4. ownership-native-193
5. P0 native validation legacy része
6. Phase7 Performance
7. phone-execution UI
8. release-metadata maintenance
9. Wear build
10. Wear round UI
11. Wear release

## Már bizonyított

Quick TrainPilot Validation:

- run: `40395802353`
- eredmény: SUCCESS
- `app-target / classify`: SUCCESS
- source-routing self-check: 7/7 PASS
- legacy `quick` job: SKIPPED

Ez bizonyítja, hogy natív source esetén a Quick már nem jut el a korábbi `setup-node` / missing lockfile hibáig.

## Következő lépések – rögzített sorrend

1. Phone Release Gate routing runtime bizonyíték megszerzése.
2. A többi legacy consumer smoke/routing ellenőrzése.
3. A `native-android.yml` natív kapuinak ellenőrzése.
4. Saját hibák javítása zöldig, ha valamelyik natív kapu hibázik.
5. Pontos commit SHA-k, Actions run ID-k, joberedmények és korlátok dokumentálása.
6. A #265 verification PR nem kerül merge-re pusztán az ellenőrzés miatt; a tényleges routing-javítás a migration ágon van.
7. A workflow munkacsomag lezárása után a következő natív MD/handoff feladat külön, a migration ágból fork-olt feature ágon indul.

## Tiltások

- nincs main merge;
- nincs verzióemelés;
- nincs release;
- nincs G10 source felülírás;
- nincs hamis PASS állítás futási bizonyíték nélkül;
- legacy WebView forrást nem keverünk vissza a natív migrációs ágba.

## Lezárási feltétel

A workflow munkacsomag akkor tekinthető késznek, ha:

- a natív source minden érintett legacy workflow-ban route-olódik vagy skipelődik;
- a Phone routinghoz van runtime bizonyíték;
- a Quick routing zöld marad;
- a `native-android.yml` alkalmazandó kapui lefutnak;
- a saját hibák zöldre vannak javítva;
- a végső bizonyítékok dokumentálva vannak.

## Verification branch jelenlegi fej

A terv rögzítése előtti head:

`619b6453f6996ebc763e6c663ce0e70b69e9c6cd`

A jelen dokumentum frissítésének commitja lesz a következő verification head.
