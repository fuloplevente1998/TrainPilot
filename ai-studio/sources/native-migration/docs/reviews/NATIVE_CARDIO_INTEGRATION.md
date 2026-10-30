# Gemini kardiószámítás – forrásreview és integráció

2026-10-25. Felhasználótól két teljes Kotlin-fájl és egy patch érkezett.
Exact eredeti fájlok importja **5aacc8a**, majd main-paritás javítás **0f8bb70**.
Külső GitHub-ág/PR nem érkezett, nem állítunk külső push-t/CI-t.

Eredeti feltöltések SHA256:

- NativeCardioModel.kt: `ffc5a214c453c7cb42a9e75c57be07a74ccc6a3bfc2d0c1b61b215771ba40a12`
- NativeCardioModelTest.kt: `3c7d34efe5b19bcf7edf7fa524e569a94651c6760fabe0ae624d3671ad6f890c`
- native-cardio-model.patch.txt: `7ea18d3aeda197ccdab3c0efa1fe2963c2bdcc14d91faebf698ba9730741f540`

A patch nem alkalmazható: git apply --check „corrupt patch at line867”. Hibás
hunkhossz és csonka tesztvég; a teljes Kotlin-fájlokból dolgoztunk. A modell
patchben lévő törzse sorvégtől eltekintve egyezik a teljes fájllal, a teszt nem.
Nem töltöttük fel a hibás patch-et végrehajtható átadóként.

Meglévő mag: streaming add/addPage, all-time és8 helyi naptári hét, páros
idő/táv súlyozott tempó, time-only külön, aktivitás/azonos kerek métertáv
rekordok legjobb+második+count, stabil idő/seconds/input tie, forrás/route/set ID.
Nincs Room/Context/provider/hálózat vagy új naplótároló.

Javítások a tényleges main7473b30:www/app.js alapján:

- Route key nem mérési forrás: source = distanceSource vagy manual, a route ID
  külön megmarad. Nincs GPS-forrás kitalálása pusztán útvonalkulcsból.
- Rekord nélkül a legutóbbi attempt a best, nem az input első sora.
- Hibás finished nem esik vissza started-re; nincs dátumprefix levágásból
  kitalált időpont. ISO date-only UTC-t jelent, nem helyi éjfélt.
- Nincs tizedesvesszőből vagy left/right mezőből kitalált seconds. Az opcionális
  durationSeconds csak előre dekódolt set.reps alias lehet, nem Wear/wall-time.
- A main az egész exercise összes setje alapján minősít distance-nek, az undone
  testvérseteket is figyelembe véve. Két opcionális exercise-wide projekciós flag
  került be, a majdani lapozott adapternek ezeket SQL-ből kell biztosítania.
- Nincs alapértelmezett distance típus/ismeretlen esetre kitalált mérési mód,
  case-insensitive típus vagy all/* wildcard; aktivitásazonosítók szó szerintiek.
- A tiszta aggregátor nem talál ki DAO-kurzort. addPage csak aggregál; valódi
  SQL-oldal utolsó nyers sorából az adapter lép tovább akkor is, ha sorokat kihagytunk.

Ténylegesen futott célzott referencia: a main pontos tp106/tp107 függvényeit
Node-ban kiemelve6 eset: bestWithoutDistance=new, routeSource=manual,
commaSeconds=0, sideSeconds=0, badDateRows=0, mixed-sibling exercise distance=true.
Ez a main referenciaeredménye, **nem az új Kotlin futási igazolása**.

9 külső JVM-metódus+6 saját regressziós metódus megírva, **NEM FUTOTT**.
Lexical/diff/API/szabálysorrend ellenőrzés volt. Nincs Kotlin fordítás/APK/emulátor.

## Nyitott bekötés és korlátok

- **Nincs még Room-projekció vagy Fejlődés UI.** P2 kardió auditpont nem lezárt.
- Az adapter adjon stabil sorrendet, valódi set/workout/source/route mezőket,
  whole-exercise flaget és cancellationt; ne töltsön teljes history gráfot.
- A program emberi neveinek/aktivitáscímkéinek helyes HU-fallbackját a meglévő
  nativeLocalizedName útján biztosítsa; a modell üres címkére ID-t is tárolhat.
- Pozitív epoch és kanonikus ISO dátumok/decimális másodpercek támogatottak.
  JS Date nem szabványos alakjai és Number hex/bin/octal időszövegek nem
  támogatottak; ezek nem érvényes natív recorded seconds bemenetek. Teljes
  tetszőleges JS-koerció-egyezést nem állítunk. Az adapter ne adjon ellentmondó
  epoch/ISO vagy reps/durationSeconds értékeket. Az ID-deduplikálás upstream.
- Memória a workout-ID/aktivitás/rekordcsoportok számával nő, nem konstans;
  a modell nem tart teljes mérési sorlistát/GPS-pontokat, két attemptet őriz/csoport.
- A kanonikus adattartományra sincs futtatott paritás/performance elfogadás;
  publikus UI-ra kötés és közös native gate külön szükséges.
