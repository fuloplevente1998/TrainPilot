# Google-karbantartás – kötelező külső átadás

2026-10-26. Draft [PR255](https://github.com/fuloplevente1998/TrainPilot/pull/255).
Saját ág: `feat/native-google-maintenance-ui`; PR-cél: `feat/native-android-migration`.
Exact base: `e28ab41887a976ee380f63f465dbdcb51f494329` (app f72c9cb0).
Exact kód/teszt HEAD: **`834a7863dbef9d72001f582694e7940d2c983194`**.
Ezt követi a jelen docs-only commit; a végső teljes HEAD és a push-ellenőrzés a
PR-leírásban és a végső átadási üzenetben szerepel. A branch nem lett rebased/merged.

Feladatforrás: `32547e8a:docs/plans/NATIVE_GOOGLE_MAINTENANCE_CODEX_HANDOFF.md`.
Verzió/csomag változatlan: com.repforge.app /1.3.71-native-test /2736 Phone/Wear.
Nincs új dependency, séma, workflow, toolchain, Gradle/script vagy közös tesztmódosítás.
AGENTS/RESUME/WORK_HANDOFF/PR238 és másik munkás riportja nem változott.
Nincs belső subagent, force-push, merge, rebase, main/migration írás, PR/issue zárás,
GitHub-komment vagy valódi Google-adat törlése/visszavonása.

## 1. Kód – UI → caller → writer → tartós eredmény

| Követelmény | Bekötés és eredmény | Státusz |
|---|---|---|
| 1. Három külön megerősített művelet | SettingsScreen kapcsolt fiók kártyája → NativeGoogleMaintenanceControls → VM.openGoogleMaintenance → [previewMaintenance](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/GoogleSyncRepository.kt#L266); [NativeGoogleMaintenanceConfirmation](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt#L41) konkrét emailt, eltérő scope-ot és következményt mutat. Revoke: engedély visszavonás + account lekapcsolás; Drive: csak APP_FILE_REGEX által engedett appData mentés/fotó; Calendar: kizárólag owner + eredeti MANAGED_CALENDAR_DESC. Mégse csak a processz-lokális preview-t ejti el. Helyi history/photo táblát/fájlt egyik út sem töröl. | IMPLEMENTED, runtime UNRUN |
| 2. Meglévő authorization/consent/gate | [SettingsViewModel.confirmGoogleMaintenance](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt#L77) → [authorizeAndRun](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/GoogleSyncRepository.kt#L167) opcionális maintenance requesttel → eredeti NativeGoogleConsentBridge + Identity AuthorizationRequest → ugyanazon egy processzszintű NativeGoogleOperationGate → [performMaintenance](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/GoogleSyncRepository.kt#L321). Nem a public, új gate-et nyitó revoke/delete wrapper hívódik belül. GoogleSyncAction eredeti enumja és protokollja megmaradt; completion opcionális reporttal bővült. Token kizárólag repository-lokális változó, nincs UI/log/prefs/backup token. | IMPLEMENTED, OAuth UNRUN |
| 3. Account/dataset/admission | Preview rövid operation-gate alatt account/sub/email + processz accountEpoch + NativeDataReplacement continuation. [requireMaintenance](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/GoogleSyncRepository.kt#L278) consent előtt/után, userinfo után, remote lapok/törlések között és lokális publish előtt. Meglévő NativeHealthForegroundAdmission Main execution probe + IO-safe workout/paused/finalization/discard/ownership/backup/restore admission újrahasználva, read-only. Main-probe minden metadata lease előtt; SDK/network kívül marad. | IMPLEMENTED, verseny/restore runtime UNRUN |
| 4. Partial és feltámadásvédelem | [prepareMaintenance](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/GoogleSyncRepository.kt#L285) a remote művelet előtt fail-closed privát pause-t és külön Drive/Calendar pause-t ment; ugyanazon eredeti metadata lease alatt Room-tranzakcióban cloudPrefs drive/calendar=false, cloudStatus üres, cloudBase/cloudHeads/cloudCalendarSent owner cache törlés. [deleteSequentially](https://github.com/fuloplevente1998/TrainPilot/blob/834a7863dbef9d72001f582694e7940d2c983194/android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenance.kt#L26) az első hibánál megáll, ismert visszaigazolt darabszámmal és bizonytalan utolsó HTTP-eredménnyel tér vissza. Nincs rollback ígéret. Explicit új megnyitás+megerősítés kell retryhoz. | IMPLEMENTED, remote fault runtime UNRUN |
| 5. Tesztforrás | Új NativeGoogleMaintenanceTest 11 JVM és NativeGoogleMaintenanceUiTest 3 valódi Compose eset; alább külön. Eredeti Google consent/account/gate/repository admission tesztek érintetlenek. | MEGÍRVA, UNRUN |

### Routing és tartós állapot

- A három régi public maintenance API megmaradt. Ezek is először a közös gate-et
  foglalják, csak azután rögzítik az account/datasetet, ellenőriznek és futtatnak;
  busy/disconnected esetben nem indul SDK/network, Result.failure marad a protokoll.
- A produktív UI minden maintenance-hez az authorizeAndRun útját használja,
  beleértve a revoke-hoz szükséges scope-okat. Userinfo a meglévő
  NativeGoogleAccountPolicy révén a stabil subjecthez kötött.
- accountEpoch megváltozik connect/disconnect/revoke után: A→B→A vagy ugyanazon
  fiók le-/újracsatlakoztatása sem teszi újra érvényessé a régi megerősítést.
- A settings request processz-lokális, nem serialized; process death után nincs
  replayelhető megerősítés. A consent bridge meglévő attempt ID/cancel védelme marad.
- A rövid lokális metadata/Room műveletet nem tartjuk consent, Google SDK vagy
  HTTP alatt. Az operation-gate-et viszont igen, az eredeti szerződés szerint.
- A törlési lépések 404/410 válasza idempotens siker. Más hiba után nincs további
  automatikus törlés. Az ismert sikeres szám nem vész el későbbi admission-hibánál.
- A pause a távoli kérés előtt durable. Ha a DB-előkészítés hibás, a privát pause
  akkor is fail-closed maradhat; remote törlés nem indul, nincs korai siker.
- Későbbi tokenes/unattended Drive/Calendar entry nem indulhat a pause alatt.
  Csak egy új, kifejezett UI-s authorizeAndRun sync törölheti az adott szolgáltatás
  pause-ját, friss userinfo + dataset ellenőrzés után. Drive manuális újraindítása
  nem oldja fel a Calendar pause-ját. A legacy cloudPrefs mindkét ága tiltva marad.
- Revoke sikerénél a kapcsolt profil/calendar-cache eltűnik, pause megmarad.
  A helyi Room napló/tervek/programok és fotófájlok érintetlenek maradnak.
- Meglévő JSON/ZIP, timer, profil, theme és aktív edzés kezelését nem cseréltük le.
  Új UI 48dp minimum kontrollokat és meglévő tématokeneket használ; HU/EN/DE/RO
  külön native_google_maintenance.xml, nem közös strings.xml.

## 2. Megírt tesztek – nem futtatott eredmény

Exact forrás: `834a7863dbef9d72001f582694e7940d2c983194`, natív2736. Nincs hozzá teszt-XML/PASS/artifact.

**NativeGoogleMaintenanceTest – 11 JVM/Robolectric eset:**

- account/sub/email váltás elutasítása;
- azonos fiók reconnect epochja invalidálja a preview-t;
- replacement fence elutasítás;
- denied admission nem indít remote törlést;
- cancel nem válik sikerré;
- részleges törlés megőrzi a visszaigazolt számot, jelzi az uncertain eredményt;
- későbbi guardhiba megőrzi az előző sikeres darabszámot;
- retry csak külön hívásra fut, ID-dedupe;
- minden public maintenance ugyanazon gate-nél busy-t ad, network nélkül;
- disconnected maintenance nem jut az SDK-ig;
- durable pause mellett a tokenes sync nem töltheti azonnal vissza a törölt adatot.

**NativeGoogleMaintenanceUiTest – 3 Compose eset:**

- három külön gomb, konkrét fiók, Mégse nem futtat callbacket, explicit megerősítés;
- partial szöveg nem completion, ismert szám és incomplete jelzés;
- valódi SettingsScreen→VM→repository: preview után induló backup-admission miatt
  confirm failure még Identity.authorize előtt; nincs OAuth/transport fake, nincs
  pause/identity-írás vagy adatvesztés. Csak debug, izolált emulator fixture;
  a teszt átmenetileg example.invalid profilt használ, majd a régi prefs-et visszaírja.

Az első kettő Compose komponens-teszt, nem OAuth E2E. A harmadik valódi VM/caller
elutasítási út. Sikeres Google/OAuth és tényleges távoli törlés nem szimulált
termékfunkció vagy fizikai elfogadás. A meglévő consent cancel/launch/late result
és repository admission teszteket nem lazítottuk vagy módosítottuk.

## 3. Ténylegesen futtatott ellenőrzések

Exact app/tesztforrás: `834a7863dbef9d72001f582694e7940d2c983194`.

- git diff base --check: statikus PASS.
- Hét módosult/új Kotlin-fájl string/comment-aware delimiter ellenőrzése:
  lexikai PASS. Nem Kotlin parser, compiler, típuselemzés vagy runtime.
- 4 locale ×13 resource kulcs XML parse/key/format-placeholder egyezés:
  statikus PASS.
- Kijelölt fájlhatár ellenőrizve: kizárólag a megadott existing/new területek.
- Main exact7473b30f387eda54186c1e31830304ca12bd104f:www/app.js,
  Git blob b5567576bed9a8f93608a6bf1ed8db1dee4723f4. A végső
  tp105ManageData L14998–15017 három ágát Node vm-ben ténylegesen futtattuk
  izolált bridge/db/localStorage stubokkal: disable a remote kérés előtt,
  owner cache cleanup, helyi history sentinel megmarad, busy lezárul.
  JS referencia PASS, nem Kotlin runtime vagy valódi Google-törlés.
- A40317771457 közös run API szerint a vizsgálatkor IN_PROGRESS,
  head e28ab41887a976ee380f63f465dbdcb51f494329. Nem állítunk róla teljes sikert,
  és nem a jelen kódra vonatkozik. Nem cancel/re-run/dispatch-eltük.

**Nem futott:** Kotlin/Gradle fordítás, JVM/Robolectric, Compose/emulátor,
APK, signed upgrade vagy valódi Google OAuth/permission/delete. Nincs artifact,
APK-hash vagy teszt-XML; ezek hiányában nincs runtime PASS. A meglévő canonical
native-android.yml nem kínál külön maintenance filter inputot: saját új teljes
futást vagy workflow-t nem indítottunk a Root koordinációjának megkerülésére.

## 4. Korlátok és integrátori következő lépés

- HTTP timeout/cancel/process death mellett a remote eredmény bizonytalan lehet;
  nincs remote rollback. A tartós pause megmarad. Process death után a részleges
  számláló nem tartós auditnapló: új megnyitás/explicit megerősítés és a távoli
  fiók ellenőrzése kell. Nem állítunk meg nem történt törlést vagy korai sikert.
- A rövid Main execution és IO ownership probe nem tehető közös tranzakcióba
  a Google szerverével. Meglevő guardok kérésenként ismétlődnek; egy már kiküldött
  remote kérést egy utólagos workout/restore kezdés nem tud visszavonni.
- Másik készülék későbbi feltöltését helyi pause nem tiltja meg. Ez az eredeti
  main helyi disable/clear szerződése, nem fiókszintű felhőzár.
- A main a cache-eket siker után törli, itt a pause-zal együtt remote előtt:
  konzervatív helyi eltérés a részleges hiba utáni automatikus visszatöltés ellen.
  Helyi felhasználói adatot nem törlünk; explicit következő sync új alapot képezhet.
- Valódi OAuth, account-rename/switch, scope-deny/expiry, törlés és revoke:
  **DEVICE_PENDING**. Teljes natív fordítás és futási bizonyíték:
  **UNRUN / a fő integrátor közös kapujára vár**. Ez nem DONE/PASS státusz.

Javasolt központi ellenőrzés: exact saját HEAD review, canonical native workflow
Root által koordinálva azonos source-on, NativeGoogleMaintenanceTest és meglévő
NativeGoogle* unitok, három új Compose case izolált fixture-rel; XML fail/error/skip,
artifact és SHA ellenőrzés. Külön tesztfiókon részleges Drive/Calendar törlés,
consent-cancel, account és restore váltás; helyi napló/fotó megőrzés, pause és
explicit retry/resume. Élő saját Google-adatokat ne használjanak erre.

## 5. Teljes változott fájllista

- `android/app/src/main/java/com/repforge/app/GoogleSyncRepository.kt`
- `android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenance.kt`
- `android/app/src/main/java/com/repforge/app/ui/settings/NativeGoogleMaintenanceUi.kt`
- `android/app/src/main/java/com/repforge/app/ui/settings/SettingsScreen.kt`
- `android/app/src/main/java/com/repforge/app/ui/settings/SettingsViewModel.kt`
- `android/app/src/main/res/values-de/native_google_maintenance.xml`
- `android/app/src/main/res/values-en/native_google_maintenance.xml`
- `android/app/src/main/res/values-ro/native_google_maintenance.xml`
- `android/app/src/main/res/values/native_google_maintenance.xml`
- `android/app/src/nativePhoneUiTest/java/com/repforge/app/NativeGoogleMaintenanceUiTest.kt`
- `android/app/src/test/java/com/repforge/app/NativeGoogleMaintenanceTest.kt`
- `docs/reviews/NATIVE_GOOGLE_MAINTENANCE_HANDOFF.md`

A végső docs-only push után a saját távoli/lokális HEAD egyezését a PR-leírás és
az átadási üzenet rögzíti. Root main/migration munkájába nem írtunk.
