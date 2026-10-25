# #244 Health foreground refresh – integráció előtti review

2026-10-25. Külső ág feat/native-health-auto-refresh, draft célmigration.
Átvett HEAD `8c00b46dbee2a818fa5acf2de44899748a2817dd`; alkalmazás/tesztforrás
`5d89db49f57eb0867a526f522e201b4d3d72c8f9`. E két ref között csak két átadó-
dokumentum módosult. Bázis0a5abbe;34 fájl,+1506/−100.

**Döntés: két javítás szükséges integráció előtt.** A célzott JVM/UI bizonyíték
valós és zöld, de nem fedi a teljes valódi engedélyezett providerfrissítési utat.
Root most csak forrást és meglévő eredményeket ellenőrzött; nem írt appkódot,
nem indított új CI-t/Gradle-t, nem merge-elt vagy telepített.

## 1. P1 – a tényleges frissítés az IO-s main-only admission miatt letiltódik

[HealthDashboardViewModel.kt:330](https://github.com/fuloplevente1998/TrainPilot/blob/5d89db49f57eb0867a526f522e201b4d3d72c8f9/android/app/src/main/java/com/repforge/app/ui/health/HealthDashboardViewModel.kt#L330)
`withContext(Dispatchers.IO)` alatt hívja
`permitted && NativeHealthForegroundAdmission.safe(context)`-et (346.sor).
[NativeHealthForegroundAdmission.kt:34](https://github.com/fuloplevente1998/TrainPilot/blob/5d89db49f57eb0867a526f522e201b4d3d72c8f9/android/app/src/main/java/com/repforge/app/ui/health/NativeHealthForegroundAdmission.kt#L34)
közvetlenül `PhoneWorkoutExecution.store(context)`-ot kér.
A kanonikus PhoneWorkoutEngine.kt:451–454 IO-ról determinisztikusan
`Execution requires main looper` kivételt dob. A safe teljes try/catch-e ezt
false-ra fordítja. A NativeDataReplacement.mutate nem diszpécsert vált.

Ezért a csendes, engedélyezett, inaktív állapot is unsafe: a gate.begin nullt ad,
az SDK-hívásig sem jut el. A MANUAL trigger is ugyanezen feltételen megy át,
tehát a meglévő kézi frissítést is érinti, nem csak az új automatikusat.

**A publish út is javítandó:** a VM requireScreenGuard callbackje újra safe()-et
hív. Ezt a HC és Samsung adapter a saját withContext(IO) olvasó/író pontjairól
is meghívja. Nem elég kizárólag a prepare hívást Mainre vinni: minden tényleges
hívási szálra helyes admission kell. A publication/commit callback szinkron,
ezt a megoldásnál számításba kell venni; ne kerüljön blokkoló Main→IO→Main
kör vagy SDK IPC a globális lockba.

Javítási irány: a main-looperes kanonikus állapotot rövid Main.immediate művelet
ellenőrizze, metadata/epoch/ownership/backup védelem és publishkori újraellenőrzés
megőrzésével; az SDK munka maradjon IO-n/lockon kívül. A globális store looper-
védelmét eltávolítani, vagy safe() hibáját általánosan true-ra cserélni tilos.

Hiányzó érdemi regresszió: a production `safe` és a valódi launchRefresh/prepare/
callback lánc IO-entry útja, csendes állapotban engedélyezett és aktív/restore/
backup helyzetben elutasított. Provider-határ injektálható teszteléshez, de egy
hamis SDK-válasz nem fizikai HC/Samsung elfogadás.

Miért nem vette észre a zöld suite: AdmissionTest csak a tiszta `eligible` függvényt
hívja, explicit Booleanokkal. A UI-case a kapcsoló mentését és Activity újranyitás
utáni állapotát ellenőrzi, tényleges SDK-olvasás/publish sikerét nem állítja.
Ez a teszt hasznos, de a fenti út lefedését nem helyettesíti.

## 2. P2 – ZIP staging hiba után beragadhat a backup admission

[BackupRestoreManager.kt:347](https://github.com/fuloplevente1998/TrainPilot/blob/5d89db49f57eb0867a526f522e201b4d3d72c8f9/android/app/src/main/java/com/repforge/app/BackupRestoreManager.kt#L347):
`NativeHealthBackupGate.begin()` után a `transactionDir(token)` a try/finally
**előtt** fut. Ez a helper `transactionsDir()`-en keresztül könyvtárat hoz létre;
a mkdir IO-hibánál IOExceptiont dob. Ilyenkor a finally/end soha nem fut le,
a processz-szintű active számláló pozitív marad. Következő egészségolvasás vagy
publish folyamatosan Backup in progress miatt tiltott, amíg a processz él.

Javítás: a teljes begin utáni, hibaképes előkészítés a biztos finally alatt legyen;
a cleanup null/nem létrejött staging dir mellett is biztonságos. Regresszió:
`archive_transactions` helyén fájl vagy injektált mkdirhiba; sikertelen import
után gate.available true, következő érvényes művelet működik. A hibakezelési
elvárást ne gyengítsük és ne rejtsük el a valódi importhibát.

## Ellenőrzött jó részek és határok

- Egyetlen meglévő HealthJournalStore autoOnOpen pref, defaultfalse,4 locale;
  opt-in UI és destination lifecycle, manuális Samsung consent megőrzése.
- Processz-szintű10perces elapsed-time cooldown provider+olvasott nap szerint,
  in-flight gate, manuális bypass, historical selection megőrzése.
- Prepare/continuation/health-generation/provider/day/zone/grant és lokális
  publish guard elkülönítése, SDK-adapter per-write hookok. Az elv jó; az1.pont
  miatt a valódi dispatcher-hívási út jelenleg hibás.
- Részleges HC első nap megtartása +partial/dirty jelzés; Samsung denied-field
  megőrzés; DST23/25h és mai végpont now-ig korlátozva. A store változása
  határon túli termékváltozás, ezért külön regresszióval/fizikai próbával kezelendő.
- A külön main-thread NativeCoachVariation fix az előző review egyik közös
  termékhibáját célozza; a Google consent teszt típus/üzenet és original-or-cause
  ellenőrzése megtartja a lényegi szerződést a coroutine exception recovery mellett.
- Több baseline-fordítási és fixture-fix átfed #246-tal. Ezeket csak egyszer,
  szemantikusan egyeztetve integráljuk: különösen WearSync brace/Unit, backup
  appContext, Compose rule/import és a planner fixture kétféle új fájlneve.
- A `.github/workflows/native-health-auto-refresh.yml` új célzott CI a külső
  ágban; nem a canonical native release gate helyettesítője. Ne vegyük át
  automatikusan a fő ágba: a meglévő natív workflow a beépített JVM/UI-teszteket
  már kiválasztja, párhuzamos teljes emulátorfutás nem cél. A baseline átadó
  eredetileg új workflow-t és határon túli módosítást nem kért; a kibővített diff
  feladatátadását külön kezeljük, nem vak merge-gel.

## Tényleges bizonyítékok

[Health Actions40239961158](https://github.com/fuloplevente1998/TrainPilot/actions/runs/40239961158)
**SUCCESS**. PR-head5d89db4, checkout17eedd739b0e5840720201cc80af4a684d9e1df5,
szülei448e435 és5d89db4. Az app/Wear/shared/.github diff e két forrás között üres;
8c00b46 pedig csak docsdelta, tehát az appkódbizonyíték arra is vonatkoztatható.

Root letöltötte és ténylegesen feldolgozta:

- artifact12616966543: **29 JVM/0 fail/0 error/0 skip**.5 Admission+2 Backup+
  5 Publication+9 Gate+4 Window =25 új;4 meglévő NativeHealthRefresh.
- artifact12616857420: **1 UI/0 fail/0 error/0 skip**, valódi Compose kapcsoló/
  tartós preference scenario Android35 emulátoron. Nem fizikai SDK-olvasás.
-4 resourcekulcs4nyelven XML/key/format PASS; git diff --check PASS.
- Közös natív Actions40239961291 a vizsgálatkor **IN_PROGRESS**; nincs teljes
  zöld/fizikai próba vagy aláírt upgrade elfogadás jelentve.
- rf250Sync30napos/full/incremental visszatöltési paritás továbbra külön nyitott;
  a napi foregroundfrissítés nem a teljes egészségszinkron lezárása.

## Külső munkásnak visszaadható pontos feladat

A saját #244 ágon javítsd az1–2.pontot, érdemi production dispatcher-entry és
ZIP-előkészítési hibainjektálásos regresszióval. A már zöld29+1 eset maradjon.
A célzott Health-kaput ellenőrizd, a közös hat baseline-hibát ne javítsd újra
párhuzamosan. Exact új HEAD és XML-bizonyíték kell; nincs main/migration írás,
merge vagy készülékes/SDK-siker kitalálása. Az eredmény újabb exact delta-review-ra
érkezzen vissza; közös integráció és natív gate a fő munkásnál.
