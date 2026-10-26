# PR251 review és integráció – 2026-10-26

Külső HEAD91fb333872abc2481f7f56dc2d93ee98b6736dae; app/teszt01bf93da;
bázise9278a99. A két ref különbsége csak az átadó. A14fájlos app/teszt/resource
delta integrálva **4b1711b1**-ben;14byteazonos fájl a review-zott HEAD-hez.
Root forrást vizsgált, új natív runtime-sikert nem állít.

Ellenőrzött út: HealthDashboard normal/full/retry/cancel → VM single-flight →
NativeHealthRefresh.sync/execute → NativeHealthSync.run → meglévő HC/Samsung
adapter → guarded local publication → HealthJournalStore és privát checkpoint.
HC első/full30helyi nap, incremental lapozott2000record/page és törlések/dirty;
Samsung30/2nap, nincs hamis change-log. DST/now és history selection megmarad.
A candidate token snapshot előtt készül, full után catch-up poll, checkpoint
csak sikeres napi olvasások és durable apply után, dataset+revision CAS-szal.
Grant/provider/zone/scene/admission védelmek, permission és SDK IPC lockon kívül;
change-revision elutasítja a change log előtti régi napi eredmény visszaírását.
Raw record/nap/health-summary a kanonikus store-ban, új adatmotor/séma nincs.

Kivétel-/cancel utak: a checkpoint nem lép partial/hiba után; retry az előző
állapotból indul, dirty/deletion tárolás tartós. Token csak privát meta, export/
projection whitelistek nem viszik. A toString nem teszi ki a tokent.

Statikus: diff-check,10Kotlin lexical,4locale×11key/format PASS. Mind14integrált
fájl byteazonos az exact külső HEAD-del. TrainingWindow/write API törzse a külső
átadó szerint változatlan; a diff nem ezeket a funkciókat módosította.
Új29JVM és1Compose-komponens tesztforrás NEM FUTOTT. A UI-case nem teljes
Activity/VM/SDK siker; a hamis provider csak teszthatáron szerepel.

## Nyitott, nem rejtett korlátok

-500change-page /530dirty nap foreground budget: túlcsordulás explicit hiba,
 nincs csonkítás/nyugta. Nagyobb archivált dirty-készlethez külön batch-folytatás
 szükséges; „retry smaller batches” mögött nincs felhasználói batch-selector.
-Részleges grant/napi warnings mellett nincs teljes-sync nyugta, candidate-token
 nem kerül előreléptetésre; következő alkalom újra full lehet. Ezt készülékes
 partial-grant esetben teljesítmény/sikerjelentés szempontból is ellenőrizni kell.
-SDK Future megszakítás után a meglévő timeoutig unwinding, publication tiltott;
 a repó nem igazol fizikai Android/Samsung callback és token-expiry szemantikát.
-A szerverállapot változhat a napi snapshot és a poll közt; catch-up és retry
 invariánsok futtatott fault-injection tesztje kötelező a release előtt.

A review alapján nincs azonnal bizonyított további kötelező source-fix ebben a
csomagban; az integrált compiler/JVM/UI/platform kapu szükséges. A fenti korlátok
nem jelölhetők eltűntnek a kódintegrációval. Közös CI Rootnál, #251 draft marad.
#250 javítása a külön chatben folyik; Root nem írja a két auditdokumentumot.
Main/verzió/package/élő install változatlan.
